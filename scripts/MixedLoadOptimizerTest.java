import com.unitloadsystem.mixed.BoxDemand;
import com.unitloadsystem.mixed.MixedLoadOptimizer;
import com.unitloadsystem.mixed.PalletLoad;
import com.unitloadsystem.mixed.PalletSpec;
import com.unitloadsystem.mixed.PlanResult;
import com.unitloadsystem.mixed.Placement;
import com.unitloadsystem.mixed.TierPlan;

import java.util.Arrays;
import java.util.List;

public final class MixedLoadOptimizerTest {
    public static void main(String[] args) {
        MixedLoadOptimizer optimizer = new MixedLoadOptimizer();

        testExistingMixedFixture(optimizer);
        testSinglePalletAndSingleBox(optimizer);
        testDenseMixedDemandUsesSevenTiers(optimizer);
        testLayeredTiersNeverOverhangLowerSupport(optimizer);
        testReportedMixedFixtureHasNoOverlap(optimizer);
        testTowerPackingSharesSinglePallet(optimizer);
        testHeightAndFootprintMustFitSamePallet(optimizer);
        testDeterministicResults(optimizer);

        System.out.println("MixedLoadOptimizer tests passed");
    }

    private static void testExistingMixedFixture(MixedLoadOptimizer optimizer) {
        List<PalletSpec> pallets = Arrays.asList(
                new PalletSpec("EU-1", "Euro Pallet", 800, 1200, 144),
                new PalletSpec("STD-1", "Standard Pallet", 1000, 1200, 150)
        );

        List<BoxDemand> demands = Arrays.asList(
                new BoxDemand("B1", "Widget Alpha", 400, 600, 200, 10.5d, 2),
                new BoxDemand("B2", "Widget Beta", 300, 800, 200, 12.25d, 2),
                new BoxDemand("B4", "Flat Medium", 800, 300, 150, 8.0d, 2),
                new BoxDemand("B3", "Oversize Case", 850, 1100, 250, 30.0d, 1),
                new BoxDemand("B5", "Too Tall", 300, 300, 600, 4.0d, 1)
        );

        PlanResult result = optimizer.optimize(pallets, demands, 700);

        assertEquals("existing fixture chosen strategy", "layered-equal-height",
                result.getChosenStrategyId());
        assertEquals("existing fixture strategy count", 5, result.getEvaluatedStrategyCount());
        assertEquals("total boxes loaded", 7, result.getTotalBoxesLoaded());
        assertEquals("load count", 2, result.getPalletLoads().size());
        assertEquals("selected pallet count", 2, result.getSelectedPalletCandidates().size());
        assertEquals("first pallet selected", "EU-1",
                result.getSelectedPalletCandidates().get(0).getId());
        assertEquals("second pallet selected", "STD-1",
                result.getSelectedPalletCandidates().get(1).getId());

        PalletLoad firstLoad = result.getPalletLoads().get(0);
        assertEquals("first load strategy", "layered-equal-height", firstLoad.getStrategyId());
        assertFalse("first load not tower-based", firstLoad.isTowerBased());
        assertEquals("first load placement count", 6, firstLoad.getPlacements().size());
        assertEquals("first load pallet", "EU-1", firstLoad.getPalletSpec().getId());
        assertEquals("first load total boxes", 6, firstLoad.getTotalBoxes());
        assertEquals("first load total height includes pallet", 494,
                firstLoad.getTotalLoadedHeightMm());
        assertEquals("first load tier count", 2, firstLoad.getTiers().size());

        TierPlan mixedTier = firstLoad.getTiers().get(0);
        assertEquals("mixed tier normalized height", 200, mixedTier.getNormalizedHeightMm());
        assertEquals("mixed tier z starts at pallet height", 144, mixedTier.getZMm());
        assertEquals("mixed tier box count", 4, mixedTier.getBoxCount());
        assertTrue("mixed tier contains both B1 and B2",
                containsBox(mixedTier, "B1") && containsBox(mixedTier, "B2"));
        assertEquals("mixed tier B1 count", 2, countBoxes(mixedTier, "B1"));
        assertEquals("mixed tier B2 count", 2, countBoxes(mixedTier, "B2"));
        assertTrue("rotation used for B2 placements", hasRotatedPlacement(mixedTier, "B2"));
        assertTrue("B1 stable name preserved",
                hasPlacement(mixedTier, "B1", "Widget Alpha", 10.5d));
        assertTrue("B2 stable name preserved",
                hasPlacement(mixedTier, "B2", "Widget Beta", 12.25d));

        TierPlan secondTier = firstLoad.getTiers().get(1);
        assertEquals("second tier normalized height", 150, secondTier.getNormalizedHeightMm());
        assertEquals("second tier z offset", 344, secondTier.getZMm());
        assertEquals("second tier only B4 boxes", 2, countBoxes(secondTier, "B4"));

        PalletLoad secondLoad = result.getPalletLoads().get(1);
        assertEquals("second load strategy", "layered-equal-height", secondLoad.getStrategyId());
        assertEquals("second load pallet", "STD-1", secondLoad.getPalletSpec().getId());
        assertEquals("second load boxes", 1, secondLoad.getTotalBoxes());
        assertEquals("second load total height includes pallet", 400,
                secondLoad.getTotalLoadedHeightMm());
        assertEquals("second load tier count", 1, secondLoad.getTiers().size());
        assertEquals("B3 placed on its own height tier", 250,
                secondLoad.getTiers().get(0).getNormalizedHeightMm());
        assertEquals("B3 count", 1, countBoxes(secondLoad.getTiers().get(0), "B3"));

        assertEquals("leftover count", 1, result.getLeftovers().size());
        PlanResult.LeftoverDemand leftover = result.getLeftovers().get(0);
        assertEquals("leftover id", "B5", leftover.getDemand().getId());
        assertEquals("leftover quantity", 1, leftover.getRemainingQuantity());
        assertEquals("leftover reason",
                MixedLoadOptimizer.REASON_BOX_TOO_TALL, leftover.getReason());

        assertNoOverlap3d(firstLoad);
        assertNoOverlap3d(secondLoad);
        assertEveryElevatedBoxFullySupported(firstLoad);
        assertEveryElevatedBoxFullySupported(secondLoad);
    }

    private static void testSinglePalletAndSingleBox(MixedLoadOptimizer optimizer) {
        List<PalletSpec> pallets = Arrays.asList(
                new PalletSpec("P1", "Only pallet", 1000, 1200, 150));
        List<BoxDemand> demands = Arrays.asList(
                new BoxDemand("B1", "Only box", 400, 300, 200, 7d, 12));

        PlanResult result = optimizer.optimize(pallets, demands, 1000);

        assertEquals("single selection boxes loaded", 12, result.getTotalBoxesLoaded());
        assertEquals("single selection leftovers", 0, result.getLeftovers().size());
        assertTrue("single selection creates a load", !result.getPalletLoads().isEmpty());
        for (PalletLoad load : result.getPalletLoads()) {
            assertEquals("single selection pallet id", "P1", load.getPalletSpec().getId());
            assertNoOverlap3d(load);
            assertEveryElevatedBoxFullySupported(load);
        }
    }

    private static void testDenseMixedDemandUsesSevenTiers(MixedLoadOptimizer optimizer) {
        List<PalletSpec> pallets = Arrays.asList(
                new PalletSpec("P1", "Small", 1100, 1100, 150),
                new PalletSpec("P2", "Square", 1200, 1200, 150));
        List<BoxDemand> demands = Arrays.asList(
                new BoxDemand("B1", "Small box", 300, 100, 300, 5d, 100),
                new BoxDemand("B2", "Large box", 300, 200, 300, 5d, 100));

        PlanResult result = optimizer.optimize(pallets, demands, 1800);
        int tierCount = 0;
        for (int i = 0; i < result.getPalletLoads().size(); i++) {
            PalletLoad load = result.getPalletLoads().get(i);
            tierCount += load.getTiers().size();
            assertNoOverlap3d(load);
            assertEveryElevatedBoxFullySupported(load);
        }
        assertEquals("dense fixture chosen strategy", "layered-equal-height",
                result.getChosenStrategyId());
        assertEquals("dense mixed load count", 2, result.getPalletLoads().size());
        assertEquals("dense mixed tier count", 7, tierCount);
        assertEquals("dense mixed boxes loaded", 200, result.getTotalBoxesLoaded());
    }

    private static void testLayeredTiersNeverOverhangLowerSupport(
            MixedLoadOptimizer optimizer) {
        List<PalletSpec> pallets = Arrays.asList(
                new PalletSpec("P1", "1200 x 1600", 1200, 1600, 150));
        List<BoxDemand> demands = Arrays.asList(
                new BoxDemand("B1", "300 x 100 x 200", 300, 100, 200, 5d, 100),
                new BoxDemand("B2", "300 x 200 x 300", 300, 200, 300, 5d, 100),
                new BoxDemand("B3", "200 x 600 x 400", 200, 600, 400, 5d, 100));

        PlanResult result = optimizer.optimize(pallets, demands, 1800);

        assertEquals("reported mixed fixture boxes loaded", 300,
                result.getTotalBoxesLoaded());
        assertEquals("reported mixed fixture pallet count", 3,
                result.getPalletLoads().size());
        for (int i = 0; i < result.getPalletLoads().size(); i++) {
            PalletLoad load = result.getPalletLoads().get(i);
            assertNoOverlap3d(load);
            assertEveryElevatedBoxFullySupported(load);
        }

        PalletLoad finalLoad = result.getPalletLoads().get(
                result.getPalletLoads().size() - 1);
        assertTrue("final partial pallet spreads boxes across more floor positions",
                countGroundColumns(finalLoad) >= 40);
        assertTrue("final partial pallet stays low after spreading",
                finalLoad.getTotalLoadedHeightMm() <= 950);
    }

    private static void testTowerPackingSharesSinglePallet(MixedLoadOptimizer optimizer) {
        List<PalletSpec> pallets = Arrays.asList(
                new PalletSpec("T1", "Tower pallet", 1000, 1000, 150),
                new PalletSpec("T2", "Large tower pallet", 1100, 1100, 150));
        List<BoxDemand> demands = Arrays.asList(
                new BoxDemand("B1", "Tall stack", 500, 500, 250, 10d, 2),
                new BoxDemand("B2", "Mid stack", 500, 500, 200, 8d, 2),
                new BoxDemand("B3", "Short stack", 500, 500, 150, 6d, 2));

        PlanResult result = optimizer.optimize(pallets, demands, 650);

        assertEquals("tower fixture chosen strategy", "tower-volume-first",
                result.getChosenStrategyId());
        assertEquals("tower fixture load count", 1, result.getPalletLoads().size());
        assertEquals("tower fixture total boxes", 6, result.getTotalBoxesLoaded());
        assertEquals("tower fixture leftovers", 0, result.getLeftovers().size());

        PalletLoad load = result.getPalletLoads().get(0);
        assertTrue("tower fixture load is tower-based", load.isTowerBased());
        assertEquals("tower load strategy id", "tower-volume-first", load.getStrategyId());
        assertEquals("tower load tiers are compatibility-only", 0, load.getTiers().size());
        assertEquals("tower load placement count", 6, load.getPlacements().size());
        assertEquals("tower load is spread below the previous maximum height",
                550, load.getTotalLoadedHeightMm());
        assertTrue("multiple box heights share one physical pallet",
                countDistinctHeights(load) >= 3);

        PlanResult.StrategySummary layeredSummary = findSummary(
                result.getEvaluatedStrategies(), "layered-equal-height");
        assertTrue("tower load count beats or equals layer-only candidate",
                loadCount(result) <= layeredSummary.getPalletLoadCount());

        assertNoOverlap3d(load);
        assertTowerSupport(load);
    }

    private static void testReportedMixedFixtureHasNoOverlap(MixedLoadOptimizer optimizer) {
        List<PalletSpec> pallets = Arrays.asList(
                new PalletSpec("P1", "Korea Export (1210)", 1200, 1000, 120));
        List<BoxDemand> demands = Arrays.asList(
                new BoxDemand("B1", "300x700x200", 300, 700, 200, 3d, 500, 5),
                new BoxDemand("B2", "600x300x300", 600, 300, 300, 5d, 1000, 5));

        PlanResult result = optimizer.optimize(pallets, demands, 1620);

        assertEquals("reported fixture loads every requested box", 1500,
                result.getTotalBoxesLoaded());
        assertEquals("reported fixture has no leftovers", 0, result.getLeftovers().size());
        for (int i = 0; i < result.getPalletLoads().size(); i++) {
            PalletLoad load = result.getPalletLoads().get(i);
            assertNoOverlap3d(load);
            assertEveryElevatedBoxFullySupported(load);
        }
    }

    private static void testDeterministicResults(MixedLoadOptimizer optimizer) {
        List<PalletSpec> pallets = Arrays.asList(
                new PalletSpec("D1", "Deterministic pallet", 1000, 1000, 150),
                new PalletSpec("D2", "Deterministic pallet 2", 1100, 1000, 150));
        List<BoxDemand> demands = Arrays.asList(
                new BoxDemand("B1", "Tall stack", 500, 500, 250, 10d, 2),
                new BoxDemand("B2", "Mid stack", 500, 500, 200, 8d, 2),
                new BoxDemand("B3", "Short stack", 500, 500, 150, 6d, 2),
                new BoxDemand("B4", "Flat filler", 250, 500, 100, 4d, 4));

        PlanResult left = optimizer.optimize(pallets, demands, 650);
        PlanResult right = optimizer.optimize(pallets, demands, 650);

        assertEquals("deterministic strategy", left.getChosenStrategyId(),
                right.getChosenStrategyId());
        assertEquals("deterministic load count",
                left.getPalletLoads().size(), right.getPalletLoads().size());
        for (int i = 0; i < left.getPalletLoads().size(); i++) {
            assertSamePlacements("deterministic load " + i,
                    left.getPalletLoads().get(i).getPlacements(),
                    right.getPalletLoads().get(i).getPlacements());
        }
    }

    private static void testHeightAndFootprintMustFitSamePallet(
            MixedLoadOptimizer optimizer) {
        List<PalletSpec> pallets = Arrays.asList(
                new PalletSpec("C1", "Low but narrow", 400, 400, 100),
                new PalletSpec("C2", "Wide but tall", 1000, 1000, 500));
        List<BoxDemand> demands = Arrays.asList(
                new BoxDemand("B1", "Cross-pallet trap", 800, 800, 400, 10d, 1));

        PlanResult result = optimizer.optimize(pallets, demands, 800);

        assertEquals("cross-pallet fixture loads", 0, result.getPalletLoads().size());
        assertEquals("cross-pallet fixture leftovers", 1, result.getLeftovers().size());
        assertEquals("cross-pallet fixture reason",
                MixedLoadOptimizer.REASON_BOX_INCOMPATIBLE_WITH_ALL_PALLETS,
                result.getLeftovers().get(0).getReason());
    }

    private static int loadCount(PlanResult result) {
        return result.getPalletLoads().size();
    }

    private static int countDistinctHeights(PalletLoad load) {
        int distinct = 0;
        int[] seen = new int[load.getPlacements().size()];
        for (int i = 0; i < load.getPlacements().size(); i++) {
            int height = load.getPlacements().get(i).getHeightMm();
            boolean alreadySeen = false;
            for (int j = 0; j < distinct; j++) {
                if (seen[j] == height) {
                    alreadySeen = true;
                    break;
                }
            }
            if (!alreadySeen) {
                seen[distinct++] = height;
            }
        }
        return distinct;
    }

    private static int countGroundColumns(PalletLoad load) {
        int count = 0;
        for (int i = 0; i < load.getPlacements().size(); i++) {
            if (load.getPlacements().get(i).getZMm()
                    == load.getPalletSpec().getHeightMm()) {
                count++;
            }
        }
        return count;
    }

    private static void assertNoOverlap3d(PalletLoad load) {
        List<Placement> placements = load.getPlacements();
        for (int i = 0; i < placements.size(); i++) {
            Placement left = placements.get(i);
            assertTrue("placement x within pallet",
                    left.getXMm() >= 0
                            && left.getXMm() + left.getWidthMm()
                            <= load.getPalletSpec().getWidthMm());
            assertTrue("placement y within pallet",
                    left.getYMm() >= 0
                            && left.getYMm() + left.getLengthMm()
                            <= load.getPalletSpec().getLengthMm());
            assertTrue("placement z above pallet",
                    left.getZMm() >= load.getPalletSpec().getHeightMm());
            for (int j = i + 1; j < placements.size(); j++) {
                Placement right = placements.get(j);
                boolean separated = left.getXMm() + left.getWidthMm() <= right.getXMm()
                        || right.getXMm() + right.getWidthMm() <= left.getXMm()
                        || left.getYMm() + left.getLengthMm() <= right.getYMm()
                        || right.getYMm() + right.getLengthMm() <= left.getYMm()
                        || left.getZMm() + left.getHeightMm() <= right.getZMm()
                        || right.getZMm() + right.getHeightMm() <= left.getZMm();
                assertTrue("placements do not overlap in 3D", separated);
            }
        }
    }

    private static void assertTowerSupport(PalletLoad load) {
        List<Placement> placements = load.getPlacements();
        for (int i = 0; i < placements.size(); i++) {
            Placement placement = placements.get(i);
            if (placement.getZMm() == load.getPalletSpec().getHeightMm()) {
                continue;
            }
            boolean supported = false;
            for (int j = 0; j < placements.size(); j++) {
                Placement below = placements.get(j);
                if (below.getZMm() + below.getHeightMm() == placement.getZMm()
                        && below.getXMm() == placement.getXMm()
                        && below.getYMm() == placement.getYMm()
                        && below.getWidthMm() == placement.getWidthMm()
                        && below.getLengthMm() == placement.getLengthMm()
                        && below.getBoxId().equals(placement.getBoxId())) {
                    supported = true;
                    break;
                }
            }
            assertTrue("elevated tower box supported by identical footprint below", supported);
        }
    }

    private static void assertEveryElevatedBoxFullySupported(PalletLoad load) {
        List<Placement> placements = load.getPlacements();
        for (int i = 0; i < placements.size(); i++) {
            Placement upper = placements.get(i);
            if (upper.getZMm() == load.getPalletSpec().getHeightMm()) {
                continue;
            }

            long supportedAreaMm2 = 0L;
            for (int j = 0; j < placements.size(); j++) {
                Placement lower = placements.get(j);
                if (lower.getZMm() + lower.getHeightMm() != upper.getZMm()) {
                    continue;
                }
                supportedAreaMm2 += overlapAreaMm2(upper, lower);
            }
            long footprintAreaMm2 = (long) upper.getWidthMm() * upper.getLengthMm();
            assertTrue("elevated box footprint fully supported",
                    supportedAreaMm2 == footprintAreaMm2);
        }
    }

    private static long overlapAreaMm2(Placement left, Placement right) {
        int overlapWidthMm = Math.min(left.getXMm() + left.getWidthMm(),
                right.getXMm() + right.getWidthMm()) - Math.max(left.getXMm(), right.getXMm());
        int overlapLengthMm = Math.min(left.getYMm() + left.getLengthMm(),
                right.getYMm() + right.getLengthMm()) - Math.max(left.getYMm(), right.getYMm());
        if (overlapWidthMm <= 0 || overlapLengthMm <= 0) {
            return 0L;
        }
        return (long) overlapWidthMm * overlapLengthMm;
    }

    private static PlanResult.StrategySummary findSummary(
            List<PlanResult.StrategySummary> summaries, String strategyId) {
        for (int i = 0; i < summaries.size(); i++) {
            PlanResult.StrategySummary summary = summaries.get(i);
            if (strategyId.equals(summary.getStrategyId())) {
                return summary;
            }
        }
        throw new AssertionError("Missing strategy summary: " + strategyId);
    }

    private static void assertSamePlacements(String name, List<Placement> expected,
                                             List<Placement> actual) {
        assertEquals(name + " size", expected.size(), actual.size());
        for (int i = 0; i < expected.size(); i++) {
            Placement left = expected.get(i);
            Placement right = actual.get(i);
            assertEquals(name + " box id " + i, left.getBoxId(), right.getBoxId());
            assertEquals(name + " x " + i, left.getXMm(), right.getXMm());
            assertEquals(name + " y " + i, left.getYMm(), right.getYMm());
            assertEquals(name + " z " + i, left.getZMm(), right.getZMm());
            assertEquals(name + " width " + i, left.getWidthMm(), right.getWidthMm());
            assertEquals(name + " length " + i, left.getLengthMm(), right.getLengthMm());
            assertEquals(name + " height " + i, left.getHeightMm(), right.getHeightMm());
            assertEquals(name + " rotated " + i, left.isRotated(), right.isRotated());
        }
    }

    private static boolean containsBox(TierPlan tierPlan, String boxId) {
        return countBoxes(tierPlan, boxId) > 0;
    }

    private static int countBoxes(TierPlan tierPlan, String boxId) {
        int count = 0;
        for (int i = 0; i < tierPlan.getPlacements().size(); i++) {
            if (boxId.equals(tierPlan.getPlacements().get(i).getBoxId())) {
                count++;
            }
        }
        return count;
    }

    private static boolean hasRotatedPlacement(TierPlan tierPlan, String boxId) {
        for (int i = 0; i < tierPlan.getPlacements().size(); i++) {
            Placement placement = tierPlan.getPlacements().get(i);
            if (boxId.equals(placement.getBoxId()) && placement.isRotated()) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasPlacement(TierPlan tierPlan, String boxId,
                                        String name, double weightKg) {
        for (int i = 0; i < tierPlan.getPlacements().size(); i++) {
            Placement placement = tierPlan.getPlacements().get(i);
            if (boxId.equals(placement.getBoxId())
                    && name.equals(placement.getBoxName())
                    && Double.compare(weightKg, placement.getWeightKg()) == 0) {
                return true;
            }
        }
        return false;
    }

    private static void assertEquals(String name, int expected, int actual) {
        if (expected != actual) {
            throw new AssertionError(name + ": expected " + expected + ", got " + actual);
        }
    }

    private static void assertEquals(String name, boolean expected, boolean actual) {
        if (expected != actual) {
            throw new AssertionError(name + ": expected " + expected + ", got " + actual);
        }
    }

    private static void assertEquals(String name, String expected, String actual) {
        if (!expected.equals(actual)) {
            throw new AssertionError(name + ": expected " + expected + ", got " + actual);
        }
    }

    private static void assertTrue(String name, boolean condition) {
        if (!condition) {
            throw new AssertionError(name);
        }
    }

    private static void assertFalse(String name, boolean condition) {
        if (condition) {
            throw new AssertionError(name);
        }
    }
}
