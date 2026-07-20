package com.unitloadsystem.mixed;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class MixedLoadOptimizer implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String REASON_BOX_TOO_TALL =
            "BOX_TOO_TALL_FOR_MAX_TOTAL_HEIGHT";
    public static final String REASON_BOX_FOOTPRINT_TOO_LARGE =
            "BOX_FOOTPRINT_EXCEEDS_ALL_PALLETS";
    public static final String REASON_BOX_INCOMPATIBLE_WITH_ALL_PALLETS =
            "BOX_HEIGHT_AND_FOOTPRINT_DO_NOT_FIT_SAME_PALLET";
    public static final String REASON_UNPLACED_AFTER_EXHAUSTING_CANDIDATES =
            "UNPLACED_AFTER_EXHAUSTING_PALLET_CANDIDATES";

    private static final int MODE_LAYERED = 0;
    private static final int MODE_TOWER = 1;
    private static final int MODE_HYBRID = 2;

    private static final int TOWER_HEURISTIC_NONE = -1;
    private static final int TOWER_HEURISTIC_VOLUME = 0;
    private static final int TOWER_HEURISTIC_WASTE = 1;
    private static final int TOWER_HEURISTIC_BALANCED = 2;

    private static final StrategySpec LAYERED_STRATEGY = new StrategySpec(
            0, "layered-equal-height", "Layered equal-height mixing",
            MODE_LAYERED, TOWER_HEURISTIC_NONE, false);
    private static final StrategySpec TOWER_VOLUME_STRATEGY = new StrategySpec(
            1, "tower-volume-first", "Tower packed-volume-first",
            MODE_TOWER, TOWER_HEURISTIC_VOLUME, true);
    private static final StrategySpec TOWER_WASTE_STRATEGY = new StrategySpec(
            2, "tower-footprint-waste-first", "Tower footprint/waste-first",
            MODE_TOWER, TOWER_HEURISTIC_WASTE, true);
    private static final StrategySpec TOWER_BALANCED_STRATEGY = new StrategySpec(
            3, "tower-height-balanced", "Tower height-balanced",
            MODE_TOWER, TOWER_HEURISTIC_BALANCED, true);
    private static final StrategySpec HYBRID_STRATEGY = new StrategySpec(
            4, "hybrid-best-per-load", "Hybrid layered/tower per-load",
            MODE_HYBRID, TOWER_HEURISTIC_NONE, false);

    private static final StrategySpec[] STRATEGIES = new StrategySpec[] {
            LAYERED_STRATEGY,
            TOWER_VOLUME_STRATEGY,
            TOWER_WASTE_STRATEGY,
            TOWER_BALANCED_STRATEGY,
            HYBRID_STRATEGY
    };

    public PlanResult optimize(List<PalletSpec> palletCandidates,
                               List<BoxDemand> boxDemands,
                               int maxTotalHeightMm) {
        if (palletCandidates == null || palletCandidates.isEmpty()) {
            throw new IllegalArgumentException("At least one pallet candidate is required");
        }
        if (boxDemands == null) {
            throw new IllegalArgumentException("boxDemands must not be null");
        }
        if (maxTotalHeightMm <= 0) {
            throw new IllegalArgumentException("maxTotalHeightMm must be > 0");
        }

        List<PalletSpec> sortedPallets = new ArrayList<PalletSpec>(palletCandidates);
        Collections.sort(sortedPallets, PALLET_ORDER);

        List<BoxDemand> sortedDemands = new ArrayList<BoxDemand>();
        for (int i = 0; i < boxDemands.size(); i++) {
            BoxDemand demand = boxDemands.get(i);
            if (demand == null) {
                throw new IllegalArgumentException("boxDemands contains null");
            }
            if (demand.getQuantity() > 0) {
                sortedDemands.add(demand);
            }
        }
        Collections.sort(sortedDemands, DEMAND_ORDER);

        Map<String, Integer> initialRemaining = new LinkedHashMap<String, Integer>();
        List<PlanResult.LeftoverDemand> infeasibleLeftovers =
                new ArrayList<PlanResult.LeftoverDemand>();
        List<BoxDemand> feasibleDemands = new ArrayList<BoxDemand>();

        for (int i = 0; i < sortedDemands.size(); i++) {
            BoxDemand demand = sortedDemands.get(i);
            String reason = findInfeasibleReason(sortedPallets, demand, maxTotalHeightMm);
            if (reason != null) {
                infeasibleLeftovers.add(new PlanResult.LeftoverDemand(
                        demand, demand.getQuantity(), reason));
            } else {
                feasibleDemands.add(demand);
                initialRemaining.put(demand.getId(), Integer.valueOf(demand.getQuantity()));
            }
        }

        List<PlanCandidate> evaluatedCandidates = new ArrayList<PlanCandidate>();
        for (int i = 0; i < STRATEGIES.length; i++) {
            evaluatedCandidates.add(buildPlanCandidate(STRATEGIES[i], sortedPallets,
                    feasibleDemands, initialRemaining, infeasibleLeftovers, maxTotalHeightMm));
        }

        PlanCandidate best = null;
        for (int i = 0; i < evaluatedCandidates.size(); i++) {
            PlanCandidate candidate = evaluatedCandidates.get(i);
            if (best == null || PLAN_ORDER.compare(candidate, best) < 0) {
                best = candidate;
            }
        }

        List<PlanResult.StrategySummary> summaries =
                new ArrayList<PlanResult.StrategySummary>(evaluatedCandidates.size());
        for (int i = 0; i < evaluatedCandidates.size(); i++) {
            summaries.add(evaluatedCandidates.get(i).toSummary());
        }

        return best.toPlanResult(sortedPallets, summaries);
    }

    private PlanCandidate buildPlanCandidate(StrategySpec strategy,
                                             List<PalletSpec> sortedPallets,
                                             List<BoxDemand> feasibleDemands,
                                             Map<String, Integer> initialRemaining,
                                             List<PlanResult.LeftoverDemand> infeasibleLeftovers,
                                             int maxTotalHeightMm) {
        Map<String, Integer> remaining = new LinkedHashMap<String, Integer>(initialRemaining);
        List<LoadBuild> loads = new ArrayList<LoadBuild>();
        Set<String> selectedPalletIds = new LinkedHashSet<String>();

        while (hasRemaining(remaining)) {
            LoadBuild best = null;
            for (int i = 0; i < sortedPallets.size(); i++) {
                PalletSpec pallet = sortedPallets.get(i);
                LoadBuild candidate = buildLoadForStrategy(strategy, pallet, feasibleDemands,
                        remaining, maxTotalHeightMm, loads.size() + 1);
                if (candidate.totalBoxes <= 0) {
                    continue;
                }
                if (best == null || LOAD_ORDER.compare(candidate, best) < 0) {
                    best = candidate;
                }
            }

            if (best == null || best.totalBoxes <= 0) {
                break;
            }

            for (Map.Entry<String, Integer> entry : best.usedByDemand.entrySet()) {
                String demandId = entry.getKey();
                int updated = remaining.get(demandId).intValue() - entry.getValue().intValue();
                remaining.put(demandId, Integer.valueOf(updated));
            }
            loads.add(best);
            selectedPalletIds.add(best.palletSpec.getId());
        }

        List<PlanResult.LeftoverDemand> leftovers =
                new ArrayList<PlanResult.LeftoverDemand>(infeasibleLeftovers);
        int leftoverQuantity = 0;
        for (int i = 0; i < feasibleDemands.size(); i++) {
            BoxDemand demand = feasibleDemands.get(i);
            int qty = remaining.get(demand.getId()).intValue();
            if (qty > 0) {
                leftovers.add(new PlanResult.LeftoverDemand(
                        demand, qty, REASON_UNPLACED_AFTER_EXHAUSTING_CANDIDATES));
                leftoverQuantity += qty;
            }
        }
        for (int i = 0; i < infeasibleLeftovers.size(); i++) {
            leftoverQuantity += infeasibleLeftovers.get(i).getRemainingQuantity();
        }

        List<PalletSpec> selectedPallets = new ArrayList<PalletSpec>();
        for (int i = 0; i < sortedPallets.size(); i++) {
            PalletSpec pallet = sortedPallets.get(i);
            if (selectedPalletIds.contains(pallet.getId())) {
                selectedPallets.add(pallet);
            }
        }

        int totalBoxesLoaded = 0;
        long totalBoxVolumeMm3 = 0L;
        long totalCapacityVolumeMm3 = 0L;
        for (int i = 0; i < loads.size(); i++) {
            LoadBuild load = loads.get(i);
            totalBoxesLoaded += load.totalBoxes;
            totalBoxVolumeMm3 += load.totalBoxVolumeMm3;
            totalCapacityVolumeMm3 += (long) load.palletSpec.getFootprintAreaMm2()
                    * (long) Math.max(0, maxTotalHeightMm - load.palletSpec.getHeightMm());
        }

        return new PlanCandidate(strategy, maxTotalHeightMm, selectedPallets, loads,
                leftovers, totalBoxesLoaded, leftoverQuantity,
                totalBoxVolumeMm3, totalCapacityVolumeMm3);
    }

    private LoadBuild buildLoadForStrategy(StrategySpec strategy, PalletSpec pallet,
                                           List<BoxDemand> demands,
                                           Map<String, Integer> remaining,
                                           int maxTotalHeightMm, int loadNumber) {
        if (strategy.mode == MODE_LAYERED) {
            return buildLayeredLoad(LAYERED_STRATEGY, pallet, demands, remaining,
                    maxTotalHeightMm, loadNumber);
        }
        if (strategy.mode == MODE_TOWER) {
            return buildTowerLoad(strategy, pallet, demands, remaining,
                    maxTotalHeightMm, loadNumber);
        }

        LoadBuild layered = buildLayeredLoad(LAYERED_STRATEGY, pallet, demands, remaining,
                maxTotalHeightMm, loadNumber);
        LoadBuild bestTower = null;
        for (int i = 0; i < STRATEGIES.length; i++) {
            StrategySpec candidateStrategy = STRATEGIES[i];
            if (candidateStrategy.mode != MODE_TOWER) {
                continue;
            }
            LoadBuild tower = buildTowerLoad(candidateStrategy, pallet, demands, remaining,
                    maxTotalHeightMm, loadNumber);
            if (tower.totalBoxes <= 0) {
                continue;
            }
            if (bestTower == null || LOAD_ORDER.compare(tower, bestTower) < 0) {
                bestTower = tower;
            }
        }

        if (layered.totalBoxes <= 0) {
            return bestTower == null ? LoadBuild.empty(pallet, loadNumber, LAYERED_STRATEGY)
                    : bestTower;
        }
        if (bestTower == null || bestTower.totalBoxes <= 0) {
            return layered;
        }
        return LOAD_ORDER.compare(layered, bestTower) <= 0 ? layered : bestTower;
    }

    private String findInfeasibleReason(List<PalletSpec> pallets, BoxDemand demand,
                                        int maxTotalHeightMm) {
        boolean fitsHeight = false;
        boolean fitsFootprint = false;
        for (int i = 0; i < pallets.size(); i++) {
            PalletSpec pallet = pallets.get(i);
            boolean fitsThisHeight = demand.getHeightMm()
                    <= maxTotalHeightMm - pallet.getHeightMm()
                    && demand.getMaxStackLayers() > 0;
            boolean fitsThisFootprint = fitsFootprint(pallet, demand);
            if (fitsThisHeight) {
                fitsHeight = true;
            }
            if (fitsThisFootprint) {
                fitsFootprint = true;
            }
            if (fitsThisHeight && fitsThisFootprint) {
                return null;
            }
        }
        if (!fitsHeight) {
            return REASON_BOX_TOO_TALL;
        }
        if (!fitsFootprint) {
            return REASON_BOX_FOOTPRINT_TOO_LARGE;
        }
        return REASON_BOX_INCOMPATIBLE_WITH_ALL_PALLETS;
    }

    private boolean fitsFootprint(PalletSpec pallet, BoxDemand demand) {
        return fits(pallet.getWidthMm(), pallet.getLengthMm(),
                demand.getWidthMm(), demand.getLengthMm())
                || fits(pallet.getWidthMm(), pallet.getLengthMm(),
                demand.getLengthMm(), demand.getWidthMm());
    }

    private LoadBuild buildLayeredLoad(StrategySpec strategy, PalletSpec pallet,
                                       List<BoxDemand> demands,
                                       Map<String, Integer> remaining,
                                       int maxTotalHeightMm, int loadNumber) {
        int availableHeight = maxTotalHeightMm - pallet.getHeightMm();
        if (availableHeight <= 0) {
            return LoadBuild.empty(pallet, loadNumber, strategy);
        }

        Map<String, Integer> snapshot = new LinkedHashMap<String, Integer>(remaining);
        List<TierBuild> tiers = new ArrayList<TierBuild>();
        int currentZ = pallet.getHeightMm();
        int remainingHeight = availableHeight;

        while (remainingHeight > 0) {
            TierBuild bestTier = null;
            List<Placement> supportingPlacements = tiers.isEmpty()
                    ? null : tiers.get(tiers.size() - 1).placements;
            for (int i = 0; i < demands.size(); i++) {
                BoxDemand demand = demands.get(i);
                if (snapshot.get(demand.getId()).intValue() <= 0) {
                    continue;
                }
                int normalizedHeight = demand.getHeightMm();
                if (normalizedHeight > remainingHeight
                        || currentZ + normalizedHeight
                        > pallet.getHeightMm() + demand.getMaxStackHeightMm()) {
                    continue;
                }
                TierBuild candidate = buildTier(pallet, demands, snapshot,
                        normalizedHeight, currentZ, supportingPlacements);
                if (candidate.boxCount <= 0) {
                    continue;
                }
                if (bestTier == null || TIER_ORDER.compare(candidate, bestTier) < 0) {
                    bestTier = candidate;
                }
            }

            if (bestTier == null || bestTier.boxCount <= 0) {
                break;
            }

            tiers.add(bestTier);
            for (Map.Entry<String, Integer> entry : bestTier.usedByDemand.entrySet()) {
                String demandId = entry.getKey();
                int updated = snapshot.get(demandId).intValue() - entry.getValue().intValue();
                snapshot.put(demandId, Integer.valueOf(updated));
            }
            currentZ += bestTier.normalizedHeightMm;
            remainingHeight -= bestTier.normalizedHeightMm;
        }

        return LoadBuild.fromTiers(pallet, loadNumber, strategy, tiers);
    }

    private TierBuild buildTier(PalletSpec pallet, List<BoxDemand> demands,
                                Map<String, Integer> remaining, int normalizedHeightMm,
                                int zMm, List<Placement> supportingPlacements) {
        TierBuild best = null;
        for (int strategy = 0; strategy < 3; strategy++) {
            TierBuild candidate = buildTierWithStrategy(pallet, demands, remaining,
                    normalizedHeightMm, zMm, strategy, supportingPlacements);
            if (best == null || TIER_ORDER.compare(candidate, best) < 0) {
                best = candidate;
            }
        }
        return best;
    }

    private TierBuild buildTierWithStrategy(PalletSpec pallet, List<BoxDemand> demands,
                                            Map<String, Integer> remaining,
                                            int normalizedHeightMm, int zMm,
                                            int strategy,
                                            List<Placement> supportingPlacements) {
        List<Rect> freeRects = new ArrayList<Rect>();
        freeRects.add(new Rect(0, 0, pallet.getWidthMm(), pallet.getLengthMm()));

        List<Placement> placements = new ArrayList<Placement>();
        Map<String, Integer> usedByDemand = new LinkedHashMap<String, Integer>();
        int usedAreaMm2 = 0;
        double totalWeightKg = 0d;
        long totalVolumeMm3 = 0L;

        while (!freeRects.isEmpty()) {
            PlacementCandidate best = null;
            for (int demandIndex = 0; demandIndex < demands.size(); demandIndex++) {
                BoxDemand demand = demands.get(demandIndex);
                if (demand.getHeightMm() != normalizedHeightMm) {
                    continue;
                }
                if (zMm + demand.getHeightMm()
                        > pallet.getHeightMm() + demand.getMaxStackHeightMm()) {
                    continue;
                }
                if (remaining.get(demand.getId()).intValue()
                        - valueOrZero(usedByDemand, demand.getId()) <= 0) {
                    continue;
                }
                best = pickBetterPlacement(best, demand, demandIndex, freeRects, zMm, false,
                        strategy, supportingPlacements);
                if (demand.getWidthMm() != demand.getLengthMm()) {
                    best = pickBetterPlacement(best, demand, demandIndex, freeRects, zMm, true,
                            strategy, supportingPlacements);
                }
            }

            if (best == null) {
                break;
            }

            placements.add(best.placement);
            usedAreaMm2 += best.boxWidthMm * best.boxLengthMm;
            totalWeightKg += best.demand.getWeightKg();
            totalVolumeMm3 += best.demand.getVolumeMm3();
            usedByDemand.put(best.demand.getId(),
                    Integer.valueOf(valueOrZero(usedByDemand, best.demand.getId()) + 1));

            splitFreeRects(freeRects, new Rect(best.placement.getXMm(),
                    best.placement.getYMm(), best.boxWidthMm, best.boxLengthMm));
            sortFreeRects(freeRects);
        }

        return new TierBuild(normalizedHeightMm, zMm, placements, usedAreaMm2,
                totalWeightKg, totalVolumeMm3, usedByDemand);
    }

    private LoadBuild buildTowerLoad(StrategySpec strategy, PalletSpec pallet,
                                     List<BoxDemand> demands,
                                     Map<String, Integer> remaining,
                                     int maxTotalHeightMm, int loadNumber) {
        int availableHeight = maxTotalHeightMm - pallet.getHeightMm();
        if (availableHeight <= 0) {
            return LoadBuild.empty(pallet, loadNumber, strategy);
        }

        List<Rect> freeRects = new ArrayList<Rect>();
        freeRects.add(new Rect(0, 0, pallet.getWidthMm(), pallet.getLengthMm()));

        List<TowerColumnBuild> columns = new ArrayList<TowerColumnBuild>();
        Map<String, Integer> usedByDemand = new LinkedHashMap<String, Integer>();
        List<Integer> columnHeights = new ArrayList<Integer>();
        long totalVolumeMm3 = 0L;
        double totalWeightKg = 0d;

        while (!freeRects.isEmpty()) {
            int targetHeightMm = computeTowerTargetHeight(columnHeights, availableHeight);
            TowerColumnCandidate best = null;

            for (int demandIndex = 0; demandIndex < demands.size(); demandIndex++) {
                BoxDemand demand = demands.get(demandIndex);
                int availableQuantity = remaining.get(demand.getId()).intValue()
                        - valueOrZero(usedByDemand, demand.getId());
                if (availableQuantity <= 0) {
                    continue;
                }
                best = pickBetterTowerColumn(best, demand, demandIndex, freeRects,
                        availableQuantity, pallet.getHeightMm(), availableHeight,
                        targetHeightMm, false, strategy.towerHeuristic);
                if (demand.getWidthMm() != demand.getLengthMm()) {
                    best = pickBetterTowerColumn(best, demand, demandIndex, freeRects,
                            availableQuantity, pallet.getHeightMm(), availableHeight,
                            targetHeightMm, true, strategy.towerHeuristic);
                }
            }

            if (best == null || best.stackCount <= 0) {
                break;
            }

            columns.add(new TowerColumnBuild(best.demand, best.boxWidthMm, best.boxLengthMm,
                    best.freeRect.xMm, best.freeRect.yMm, best.stackCount, best.rotated));
            totalVolumeMm3 += best.demand.getVolumeMm3() * (long) best.stackCount;
            totalWeightKg += best.demand.getWeightKg() * best.stackCount;

            usedByDemand.put(best.demand.getId(),
                    Integer.valueOf(valueOrZero(usedByDemand, best.demand.getId())
                            + best.stackCount));
            columnHeights.add(best.columnHeightMm);

            splitFreeRects(freeRects, new Rect(best.freeRect.xMm, best.freeRect.yMm,
                    best.boxWidthMm, best.boxLengthMm));
            sortFreeRects(freeRects);
        }

        if (usesAllRemaining(remaining, usedByDemand)) {
            spreadFinalTowerColumns(columns, freeRects);
        }
        List<Placement> placements = buildTowerPlacements(columns, pallet.getHeightMm());

        return LoadBuild.fromPlacements(pallet, loadNumber, strategy,
                Collections.<TierBuild>emptyList(), placements,
                totalWeightKg, totalVolumeMm3, usedByDemand);
    }

    private boolean usesAllRemaining(Map<String, Integer> remaining,
                                     Map<String, Integer> usedByDemand) {
        for (Map.Entry<String, Integer> entry : remaining.entrySet()) {
            if (valueOrZero(usedByDemand, entry.getKey()) < entry.getValue().intValue()) {
                return false;
            }
        }
        return true;
    }

    private void spreadFinalTowerColumns(List<TowerColumnBuild> columns,
                                         List<Rect> freeRects) {
        while (!freeRects.isEmpty()) {
            SpreadCandidate best = null;
            for (int columnIndex = 0; columnIndex < columns.size(); columnIndex++) {
                TowerColumnBuild source = columns.get(columnIndex);
                if (source.stackCount <= 1) {
                    continue;
                }
                best = pickBetterSpreadCandidate(best, source, columnIndex,
                        freeRects, false);
                if (source.demand.getWidthMm() != source.demand.getLengthMm()) {
                    best = pickBetterSpreadCandidate(best, source, columnIndex,
                            freeRects, true);
                }
            }
            if (best == null) {
                break;
            }

            TowerColumnBuild source = columns.get(best.sourceColumnIndex);
            int movedCount = source.stackCount / 2;
            source.stackCount -= movedCount;
            columns.add(new TowerColumnBuild(source.demand,
                    best.boxWidthMm, best.boxLengthMm,
                    best.freeRect.xMm, best.freeRect.yMm,
                    movedCount, best.rotated));

            splitFreeRects(freeRects, new Rect(best.freeRect.xMm, best.freeRect.yMm,
                    best.boxWidthMm, best.boxLengthMm));
            sortFreeRects(freeRects);
        }
    }

    private SpreadCandidate pickBetterSpreadCandidate(SpreadCandidate currentBest,
                                                       TowerColumnBuild source,
                                                       int sourceColumnIndex,
                                                       List<Rect> freeRects,
                                                       boolean rotated) {
        int boxWidthMm = rotated
                ? source.demand.getLengthMm() : source.demand.getWidthMm();
        int boxLengthMm = rotated
                ? source.demand.getWidthMm() : source.demand.getLengthMm();
        for (int rectIndex = 0; rectIndex < freeRects.size(); rectIndex++) {
            Rect rect = freeRects.get(rectIndex);
            if (!fits(rect.widthMm, rect.lengthMm, boxWidthMm, boxLengthMm)) {
                continue;
            }
            SpreadCandidate candidate = new SpreadCandidate(source, sourceColumnIndex,
                    rect, rectIndex, boxWidthMm, boxLengthMm, rotated);
            if (currentBest == null
                    || compareSpreadCandidates(candidate, currentBest) < 0) {
                currentBest = candidate;
            }
        }
        return currentBest;
    }

    private int compareSpreadCandidates(SpreadCandidate left, SpreadCandidate right) {
        int heightCompare = compareInts(
                right.source.columnHeightMm(), left.source.columnHeightMm());
        if (heightCompare != 0) {
            return heightCompare;
        }
        long leftRisk = (long) left.source.columnHeightMm()
                * right.source.narrowSideMm();
        long rightRisk = (long) right.source.columnHeightMm()
                * left.source.narrowSideMm();
        int riskCompare = compareLongs(rightRisk, leftRisk);
        if (riskCompare != 0) {
            return riskCompare;
        }
        int wasteCompare = compareInts(left.wasteAreaMm2(), right.wasteAreaMm2());
        if (wasteCompare != 0) {
            return wasteCompare;
        }
        int sourceCompare = compareInts(left.sourceColumnIndex, right.sourceColumnIndex);
        if (sourceCompare != 0) {
            return sourceCompare;
        }
        if (left.rotated != right.rotated) {
            return left.rotated ? 1 : -1;
        }
        return compareInts(left.freeRectIndex, right.freeRectIndex);
    }

    private List<Placement> buildTowerPlacements(List<TowerColumnBuild> columns,
                                                 int palletHeightMm) {
        List<Placement> placements = new ArrayList<Placement>();
        for (int columnIndex = 0; columnIndex < columns.size(); columnIndex++) {
            TowerColumnBuild column = columns.get(columnIndex);
            for (int stackIndex = 0; stackIndex < column.stackCount; stackIndex++) {
                placements.add(new Placement(
                        column.demand.getId(),
                        column.demand.getName(),
                        column.boxWidthMm,
                        column.boxLengthMm,
                        column.demand.getHeightMm(),
                        column.demand.getWeightKg(),
                        column.xMm,
                        column.yMm,
                        palletHeightMm + (stackIndex * column.demand.getHeightMm()),
                        column.rotated));
            }
        }
        return placements;
    }

    private int computeTowerTargetHeight(List<Integer> columnHeights, int availableHeight) {
        if (columnHeights.isEmpty()) {
            return availableHeight;
        }
        long total = 0L;
        for (int i = 0; i < columnHeights.size(); i++) {
            total += columnHeights.get(i).intValue();
        }
        return (int) (total / (long) columnHeights.size());
    }

    private PlacementCandidate pickBetterPlacement(PlacementCandidate currentBest,
                                                   BoxDemand demand,
                                                   int demandIndex,
                                                   List<Rect> freeRects,
                                                   int zMm,
                                                   boolean rotated,
                                                   int strategy,
                                                   List<Placement> supportingPlacements) {
        int boxWidthMm = rotated ? demand.getLengthMm() : demand.getWidthMm();
        int boxLengthMm = rotated ? demand.getWidthMm() : demand.getLengthMm();

        for (int rectIndex = 0; rectIndex < freeRects.size(); rectIndex++) {
            Rect rect = freeRects.get(rectIndex);
            if (!fits(rect.widthMm, rect.lengthMm, boxWidthMm, boxLengthMm)) {
                continue;
            }
            Rect footprint = new Rect(rect.xMm, rect.yMm, boxWidthMm, boxLengthMm);
            if (!isFullySupported(footprint, zMm, supportingPlacements)) {
                continue;
            }
            Placement placement = new Placement(
                    demand.getId(),
                    demand.getName(),
                    boxWidthMm,
                    boxLengthMm,
                    demand.getHeightMm(),
                    demand.getWeightKg(),
                    rect.xMm,
                    rect.yMm,
                    zMm,
                    rotated);
            PlacementCandidate candidate = new PlacementCandidate(
                    demand, demandIndex, rect, rectIndex, placement, boxWidthMm, boxLengthMm);
            if (currentBest == null
                    || comparePlacementCandidates(candidate, currentBest, strategy) < 0) {
                currentBest = candidate;
            }
        }
        return currentBest;
    }

    private boolean isFullySupported(Rect footprint, int zMm,
                                     List<Placement> supportingPlacements) {
        if (supportingPlacements == null) {
            return true;
        }

        long supportedAreaMm2 = 0L;
        for (int i = 0; i < supportingPlacements.size(); i++) {
            Placement support = supportingPlacements.get(i);
            if (support.getZMm() + support.getHeightMm() != zMm) {
                continue;
            }

            int overlapWidthMm = Math.min(footprint.xMm + footprint.widthMm,
                    support.getXMm() + support.getWidthMm())
                    - Math.max(footprint.xMm, support.getXMm());
            int overlapLengthMm = Math.min(footprint.yMm + footprint.lengthMm,
                    support.getYMm() + support.getLengthMm())
                    - Math.max(footprint.yMm, support.getYMm());
            if (overlapWidthMm > 0 && overlapLengthMm > 0) {
                supportedAreaMm2 += (long) overlapWidthMm * overlapLengthMm;
            }
        }
        return supportedAreaMm2 == (long) footprint.widthMm * footprint.lengthMm;
    }

    private TowerColumnCandidate pickBetterTowerColumn(TowerColumnCandidate currentBest,
                                                       BoxDemand demand, int demandIndex,
                                                       List<Rect> freeRects,
                                                       int availableQuantity,
                                                       int palletHeightMm,
                                                       int availableHeightMm,
                                                       int targetHeightMm,
                                                       boolean rotated,
                                                       int heuristic) {
        int boxWidthMm = rotated ? demand.getLengthMm() : demand.getWidthMm();
        int boxLengthMm = rotated ? demand.getWidthMm() : demand.getLengthMm();
        int maxStackCount = Math.min(availableQuantity,
                Math.min(availableHeightMm / demand.getHeightMm(), demand.getMaxStackLayers()));
        if (maxStackCount <= 0) {
            return currentBest;
        }

        int[] candidateCounts = buildTowerStackCounts(maxStackCount, demand.getHeightMm(),
                targetHeightMm, heuristic);
        for (int rectIndex = 0; rectIndex < freeRects.size(); rectIndex++) {
            Rect rect = freeRects.get(rectIndex);
            if (!fits(rect.widthMm, rect.lengthMm, boxWidthMm, boxLengthMm)) {
                continue;
            }
            for (int i = 0; i < candidateCounts.length; i++) {
                int stackCount = candidateCounts[i];
                if (stackCount <= 0) {
                    continue;
                }
                TowerColumnCandidate candidate = new TowerColumnCandidate(
                        demand, demandIndex, rect, rectIndex, boxWidthMm, boxLengthMm,
                        stackCount, palletHeightMm, rotated, targetHeightMm);
                if (currentBest == null
                        || compareTowerColumnCandidates(candidate, currentBest, heuristic) < 0) {
                    currentBest = candidate;
                }
            }
        }
        return currentBest;
    }

    private int[] buildTowerStackCounts(int maxStackCount, int boxHeightMm,
                                        int targetHeightMm, int heuristic) {
        if (heuristic != TOWER_HEURISTIC_BALANCED) {
            return new int[] { maxStackCount };
        }

        int targetCount = Math.max(1, targetHeightMm / boxHeightMm);
        int targetCountPlus = Math.max(1, (targetHeightMm + boxHeightMm - 1) / boxHeightMm);
        int[] raw = new int[] {
                1,
                clamp(targetCount, 1, maxStackCount),
                clamp(targetCountPlus, 1, maxStackCount),
                maxStackCount
        };
        int uniqueCount = 0;
        int[] unique = new int[raw.length];
        for (int i = 0; i < raw.length; i++) {
            boolean seen = false;
            for (int j = 0; j < uniqueCount; j++) {
                if (unique[j] == raw[i]) {
                    seen = true;
                    break;
                }
            }
            if (!seen) {
                unique[uniqueCount++] = raw[i];
            }
        }
        int[] result = new int[uniqueCount];
        for (int i = 0; i < uniqueCount; i++) {
            result[i] = unique[i];
        }
        return result;
    }

    private int comparePlacementCandidates(PlacementCandidate left,
                                           PlacementCandidate right,
                                           int strategy) {
        if (strategy == 1) {
            int boxAreaCompare = compareInts(right.boxAreaMm2(), left.boxAreaMm2());
            if (boxAreaCompare != 0) {
                return boxAreaCompare;
            }
        } else if (strategy == 2) {
            int leftShort = Math.min(
                    left.freeRect.widthMm - left.boxWidthMm,
                    left.freeRect.lengthMm - left.boxLengthMm);
            int rightShort = Math.min(
                    right.freeRect.widthMm - right.boxWidthMm,
                    right.freeRect.lengthMm - right.boxLengthMm);
            int shortCompare = compareInts(leftShort, rightShort);
            if (shortCompare != 0) {
                return shortCompare;
            }
        }
        return PLACEMENT_ORDER.compare(left, right);
    }

    private int compareTowerColumnCandidates(TowerColumnCandidate left,
                                             TowerColumnCandidate right,
                                             int heuristic) {
        if (heuristic == TOWER_HEURISTIC_VOLUME) {
            int volumeCompare = compareLongs(right.columnVolumeMm3(), left.columnVolumeMm3());
            if (volumeCompare != 0) {
                return volumeCompare;
            }
            int heightCompare = compareInts(right.columnHeightMm, left.columnHeightMm);
            if (heightCompare != 0) {
                return heightCompare;
            }
            int countCompare = compareInts(right.stackCount, left.stackCount);
            if (countCompare != 0) {
                return countCompare;
            }
        } else if (heuristic == TOWER_HEURISTIC_WASTE) {
            int wasteCompare = compareInts(left.wasteAreaMm2(), right.wasteAreaMm2());
            if (wasteCompare != 0) {
                return wasteCompare;
            }
            int shortWasteCompare = compareInts(left.shortWasteMm(), right.shortWasteMm());
            if (shortWasteCompare != 0) {
                return shortWasteCompare;
            }
            int volumeCompare = compareLongs(right.columnVolumeMm3(), left.columnVolumeMm3());
            if (volumeCompare != 0) {
                return volumeCompare;
            }
        } else if (heuristic == TOWER_HEURISTIC_BALANCED) {
            int balanceCompare = compareInts(left.balanceDistanceMm(), right.balanceDistanceMm());
            if (balanceCompare != 0) {
                return balanceCompare;
            }
            int wasteCompare = compareInts(left.wasteAreaMm2(), right.wasteAreaMm2());
            if (wasteCompare != 0) {
                return wasteCompare;
            }
            int volumeCompare = compareLongs(right.columnVolumeMm3(), left.columnVolumeMm3());
            if (volumeCompare != 0) {
                return volumeCompare;
            }
        }

        int areaCompare = compareInts(right.boxAreaMm2(), left.boxAreaMm2());
        if (areaCompare != 0) {
            return areaCompare;
        }
        int demandOrderCompare = compareInts(left.demandIndex, right.demandIndex);
        if (demandOrderCompare != 0) {
            return demandOrderCompare;
        }
        if (left.rotated != right.rotated) {
            return left.rotated ? 1 : -1;
        }
        int rectIndexCompare = compareInts(left.freeRectIndex, right.freeRectIndex);
        if (rectIndexCompare != 0) {
            return rectIndexCompare;
        }
        return left.demand.getId().compareTo(right.demand.getId());
    }

    private void splitFreeRects(List<Rect> freeRects, Rect used) {
        List<Rect> split = new ArrayList<Rect>();
        for (int i = 0; i < freeRects.size(); i++) {
            Rect free = freeRects.get(i);
            if (!intersects(free, used)) {
                split.add(free);
                continue;
            }
            addRect(split, free.xMm, free.yMm,
                    used.xMm - free.xMm, free.lengthMm);
            addRect(split, used.xMm + used.widthMm, free.yMm,
                    free.xMm + free.widthMm - used.xMm - used.widthMm,
                    free.lengthMm);
            addRect(split, free.xMm, free.yMm,
                    free.widthMm, used.yMm - free.yMm);
            addRect(split, free.xMm, used.yMm + used.lengthMm,
                    free.widthMm,
                    free.yMm + free.lengthMm - used.yMm - used.lengthMm);
        }
        freeRects.clear();
        freeRects.addAll(split);
        pruneContainedRects(freeRects);
    }

    private boolean intersects(Rect left, Rect right) {
        return left.xMm < right.xMm + right.widthMm
                && left.xMm + left.widthMm > right.xMm
                && left.yMm < right.yMm + right.lengthMm
                && left.yMm + left.lengthMm > right.yMm;
    }

    private void pruneContainedRects(List<Rect> freeRects) {
        for (int i = freeRects.size() - 1; i >= 0; i--) {
            for (int j = freeRects.size() - 1; j >= 0; j--) {
                if (i != j && contains(freeRects.get(j), freeRects.get(i))) {
                    freeRects.remove(i);
                    break;
                }
            }
        }
    }

    private void addRect(List<Rect> freeRects, int xMm, int yMm, int widthMm, int lengthMm) {
        if (widthMm <= 0 || lengthMm <= 0) {
            return;
        }
        Rect candidate = new Rect(xMm, yMm, widthMm, lengthMm);
        for (int i = freeRects.size() - 1; i >= 0; i--) {
            Rect existing = freeRects.get(i);
            if (contains(existing, candidate)) {
                return;
            }
            if (contains(candidate, existing)) {
                freeRects.remove(i);
            }
        }
        freeRects.add(candidate);
    }

    private boolean contains(Rect outer, Rect inner) {
        return inner.xMm >= outer.xMm
                && inner.yMm >= outer.yMm
                && inner.xMm + inner.widthMm <= outer.xMm + outer.widthMm
                && inner.yMm + inner.lengthMm <= outer.yMm + outer.lengthMm;
    }

    private void sortFreeRects(List<Rect> freeRects) {
        Collections.sort(freeRects, RECT_ORDER);
    }

    private static boolean hasRemaining(Map<String, Integer> remaining) {
        for (Integer value : remaining.values()) {
            if (value.intValue() > 0) {
                return true;
            }
        }
        return false;
    }

    private static boolean fits(int areaWidthMm, int areaLengthMm,
                                int boxWidthMm, int boxLengthMm) {
        return boxWidthMm <= areaWidthMm && boxLengthMm <= areaLengthMm;
    }

    private static int valueOrZero(Map<String, Integer> values, String key) {
        Integer value = values.get(key);
        return value == null ? 0 : value.intValue();
    }

    private static int clamp(int value, int min, int max) {
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }

    private static final Comparator<PalletSpec> PALLET_ORDER = new Comparator<PalletSpec>() {
        @Override
        public int compare(PalletSpec left, PalletSpec right) {
            int areaCompare = compareInts(left.getFootprintAreaMm2(), right.getFootprintAreaMm2());
            if (areaCompare != 0) {
                return areaCompare;
            }
            int widthCompare = compareInts(left.getWidthMm(), right.getWidthMm());
            if (widthCompare != 0) {
                return widthCompare;
            }
            int lengthCompare = compareInts(left.getLengthMm(), right.getLengthMm());
            if (lengthCompare != 0) {
                return lengthCompare;
            }
            int heightCompare = compareInts(left.getHeightMm(), right.getHeightMm());
            if (heightCompare != 0) {
                return heightCompare;
            }
            int idCompare = left.getId().compareTo(right.getId());
            if (idCompare != 0) {
                return idCompare;
            }
            return left.getName().compareTo(right.getName());
        }
    };

    private static final Comparator<BoxDemand> DEMAND_ORDER = new Comparator<BoxDemand>() {
        @Override
        public int compare(BoxDemand left, BoxDemand right) {
            int heightCompare = compareInts(right.getHeightMm(), left.getHeightMm());
            if (heightCompare != 0) {
                return heightCompare;
            }
            int areaCompare = compareInts(right.getFootprintAreaMm2(), left.getFootprintAreaMm2());
            if (areaCompare != 0) {
                return areaCompare;
            }
            int idCompare = left.getId().compareTo(right.getId());
            if (idCompare != 0) {
                return idCompare;
            }
            return left.getName().compareTo(right.getName());
        }
    };

    private static final Comparator<Rect> RECT_ORDER = new Comparator<Rect>() {
        @Override
        public int compare(Rect left, Rect right) {
            int yCompare = compareInts(left.yMm, right.yMm);
            if (yCompare != 0) {
                return yCompare;
            }
            int xCompare = compareInts(left.xMm, right.xMm);
            if (xCompare != 0) {
                return xCompare;
            }
            int areaCompare = compareInts(left.areaMm2(), right.areaMm2());
            if (areaCompare != 0) {
                return areaCompare;
            }
            int widthCompare = compareInts(left.widthMm, right.widthMm);
            if (widthCompare != 0) {
                return widthCompare;
            }
            return compareInts(left.lengthMm, right.lengthMm);
        }
    };

    private static final Comparator<PlacementCandidate> PLACEMENT_ORDER =
            new Comparator<PlacementCandidate>() {
                @Override
                public int compare(PlacementCandidate left, PlacementCandidate right) {
                    int leftWaste = left.freeRect.areaMm2() - left.boxAreaMm2();
                    int rightWaste = right.freeRect.areaMm2() - right.boxAreaMm2();
                    int wasteCompare = compareInts(leftWaste, rightWaste);
                    if (wasteCompare != 0) {
                        return wasteCompare;
                    }
                    int leftShort = Math.min(
                            left.freeRect.widthMm - left.boxWidthMm,
                            left.freeRect.lengthMm - left.boxLengthMm);
                    int rightShort = Math.min(
                            right.freeRect.widthMm - right.boxWidthMm,
                            right.freeRect.lengthMm - right.boxLengthMm);
                    int shortCompare = compareInts(leftShort, rightShort);
                    if (shortCompare != 0) {
                        return shortCompare;
                    }
                    int leftLong = Math.max(
                            left.freeRect.widthMm - left.boxWidthMm,
                            left.freeRect.lengthMm - left.boxLengthMm);
                    int rightLong = Math.max(
                            right.freeRect.widthMm - right.boxWidthMm,
                            right.freeRect.lengthMm - right.boxLengthMm);
                    int longCompare = compareInts(leftLong, rightLong);
                    if (longCompare != 0) {
                        return longCompare;
                    }
                    int demandOrderCompare = compareInts(left.demandIndex, right.demandIndex);
                    if (demandOrderCompare != 0) {
                        return demandOrderCompare;
                    }
                    if (left.placement.isRotated() != right.placement.isRotated()) {
                        return left.placement.isRotated() ? 1 : -1;
                    }
                    int rectOrderCompare = compareInts(left.freeRectIndex, right.freeRectIndex);
                    if (rectOrderCompare != 0) {
                        return rectOrderCompare;
                    }
                    return left.demand.getId().compareTo(right.demand.getId());
                }
            };

    private static final Comparator<TierBuild> TIER_ORDER = new Comparator<TierBuild>() {
        @Override
        public int compare(TierBuild left, TierBuild right) {
            int boxCompare = compareInts(right.boxCount, left.boxCount);
            if (boxCompare != 0) {
                return boxCompare;
            }
            int areaCompare = compareInts(right.usedAreaMm2, left.usedAreaMm2);
            if (areaCompare != 0) {
                return areaCompare;
            }
            int volumeCompare = compareLongs(right.totalVolumeMm3, left.totalVolumeMm3);
            if (volumeCompare != 0) {
                return volumeCompare;
            }
            int heightCompare = compareInts(left.normalizedHeightMm, right.normalizedHeightMm);
            if (heightCompare != 0) {
                return heightCompare;
            }
            return comparePlacementLists(left.placements, right.placements);
        }
    };

    private static final Comparator<LoadBuild> LOAD_ORDER = new Comparator<LoadBuild>() {
        @Override
        public int compare(LoadBuild left, LoadBuild right) {
            int boxCompare = compareInts(right.totalBoxes, left.totalBoxes);
            if (boxCompare != 0) {
                return boxCompare;
            }
            int volumeCompare = compareLongs(right.totalBoxVolumeMm3, left.totalBoxVolumeMm3);
            if (volumeCompare != 0) {
                return volumeCompare;
            }
            int palletAreaCompare = compareInts(
                    left.palletSpec.getFootprintAreaMm2(), right.palletSpec.getFootprintAreaMm2());
            if (palletAreaCompare != 0) {
                return palletAreaCompare;
            }
            int heightCompare = compareInts(left.totalLoadedHeightMm, right.totalLoadedHeightMm);
            if (heightCompare != 0) {
                return heightCompare;
            }
            if (left.towerBased != right.towerBased) {
                return left.towerBased ? 1 : -1;
            }
            int strategyCompare = compareInts(left.strategySpec.priority, right.strategySpec.priority);
            if (strategyCompare != 0) {
                return strategyCompare;
            }
            int palletIdCompare = left.palletSpec.getId().compareTo(right.palletSpec.getId());
            if (palletIdCompare != 0) {
                return palletIdCompare;
            }
            return comparePlacementLists(left.placements, right.placements);
        }
    };

    private static final Comparator<PlanCandidate> PLAN_ORDER =
            new Comparator<PlanCandidate>() {
                @Override
                public int compare(PlanCandidate left, PlanCandidate right) {
                    int leftoverCompare = compareInts(left.leftoverQuantity, right.leftoverQuantity);
                    if (leftoverCompare != 0) {
                        return leftoverCompare;
                    }
                    int loadCompare = compareInts(left.loads.size(), right.loads.size());
                    if (loadCompare != 0) {
                        return loadCompare;
                    }
                    int volumeCompare = compareLongs(right.totalBoxVolumeMm3, left.totalBoxVolumeMm3);
                    if (volumeCompare != 0) {
                        return volumeCompare;
                    }
                    int utilizationCompare = compareVolumeRatios(
                            left.totalBoxVolumeMm3, left.totalCapacityVolumeMm3,
                            right.totalBoxVolumeMm3, right.totalCapacityVolumeMm3);
                    if (utilizationCompare != 0) {
                        return utilizationCompare;
                    }
                    int boxCompare = compareInts(right.totalBoxesLoaded, left.totalBoxesLoaded);
                    if (boxCompare != 0) {
                        return boxCompare;
                    }
                    int strategyCompare = compareInts(
                            left.strategy.priority, right.strategy.priority);
                    if (strategyCompare != 0) {
                        return strategyCompare;
                    }
                    return compareLoadLists(left.loads, right.loads);
                }
            };

    private static int compareVolumeRatios(long leftNumerator, long leftDenominator,
                                           long rightNumerator, long rightDenominator) {
        if (leftDenominator <= 0L && rightDenominator <= 0L) {
            return 0;
        }
        if (leftDenominator <= 0L) {
            return 1;
        }
        if (rightDenominator <= 0L) {
            return -1;
        }
        long leftScaled = leftNumerator * rightDenominator;
        long rightScaled = rightNumerator * leftDenominator;
        return compareLongs(rightScaled, leftScaled);
    }

    private static int compareLoadLists(List<LoadBuild> left, List<LoadBuild> right) {
        int sizeCompare = compareInts(left.size(), right.size());
        if (sizeCompare != 0) {
            return sizeCompare;
        }
        for (int i = 0; i < left.size(); i++) {
            int loadCompare = LOAD_ORDER.compare(left.get(i), right.get(i));
            if (loadCompare != 0) {
                return loadCompare;
            }
        }
        return 0;
    }

    private static int comparePlacementLists(List<Placement> left, List<Placement> right) {
        int sizeCompare = compareInts(left.size(), right.size());
        if (sizeCompare != 0) {
            return sizeCompare;
        }
        for (int i = 0; i < left.size(); i++) {
            Placement l = left.get(i);
            Placement r = right.get(i);
            int idCompare = l.getBoxId().compareTo(r.getBoxId());
            if (idCompare != 0) {
                return idCompare;
            }
            int zCompare = compareInts(l.getZMm(), r.getZMm());
            if (zCompare != 0) {
                return zCompare;
            }
            int xCompare = compareInts(l.getXMm(), r.getXMm());
            if (xCompare != 0) {
                return xCompare;
            }
            int yCompare = compareInts(l.getYMm(), r.getYMm());
            if (yCompare != 0) {
                return yCompare;
            }
            int widthCompare = compareInts(l.getWidthMm(), r.getWidthMm());
            if (widthCompare != 0) {
                return widthCompare;
            }
            int lengthCompare = compareInts(l.getLengthMm(), r.getLengthMm());
            if (lengthCompare != 0) {
                return lengthCompare;
            }
            int heightCompare = compareInts(l.getHeightMm(), r.getHeightMm());
            if (heightCompare != 0) {
                return heightCompare;
            }
            if (l.isRotated() != r.isRotated()) {
                return l.isRotated() ? 1 : -1;
            }
        }
        return 0;
    }

    private static int compareInts(int left, int right) {
        return left < right ? -1 : (left == right ? 0 : 1);
    }

    private static int compareLongs(long left, long right) {
        return left < right ? -1 : (left == right ? 0 : 1);
    }

    private static final class StrategySpec {
        final int priority;
        final String id;
        final String label;
        final int mode;
        final int towerHeuristic;
        final boolean towerBased;

        StrategySpec(int priority, String id, String label,
                     int mode, int towerHeuristic, boolean towerBased) {
            this.priority = priority;
            this.id = id;
            this.label = label;
            this.mode = mode;
            this.towerHeuristic = towerHeuristic;
            this.towerBased = towerBased;
        }
    }

    private static final class Rect {
        final int xMm;
        final int yMm;
        final int widthMm;
        final int lengthMm;

        Rect(int xMm, int yMm, int widthMm, int lengthMm) {
            this.xMm = xMm;
            this.yMm = yMm;
            this.widthMm = widthMm;
            this.lengthMm = lengthMm;
        }

        int areaMm2() {
            return widthMm * lengthMm;
        }
    }

    private static final class PlacementCandidate {
        final BoxDemand demand;
        final int demandIndex;
        final Rect freeRect;
        final int freeRectIndex;
        final Placement placement;
        final int boxWidthMm;
        final int boxLengthMm;

        PlacementCandidate(BoxDemand demand, int demandIndex, Rect freeRect, int freeRectIndex,
                           Placement placement, int boxWidthMm, int boxLengthMm) {
            this.demand = demand;
            this.demandIndex = demandIndex;
            this.freeRect = freeRect;
            this.freeRectIndex = freeRectIndex;
            this.placement = placement;
            this.boxWidthMm = boxWidthMm;
            this.boxLengthMm = boxLengthMm;
        }

        int boxAreaMm2() {
            return boxWidthMm * boxLengthMm;
        }
    }

    private static final class TowerColumnBuild {
        final BoxDemand demand;
        final int boxWidthMm;
        final int boxLengthMm;
        final int xMm;
        final int yMm;
        final boolean rotated;
        int stackCount;

        TowerColumnBuild(BoxDemand demand, int boxWidthMm, int boxLengthMm,
                         int xMm, int yMm, int stackCount, boolean rotated) {
            this.demand = demand;
            this.boxWidthMm = boxWidthMm;
            this.boxLengthMm = boxLengthMm;
            this.xMm = xMm;
            this.yMm = yMm;
            this.stackCount = stackCount;
            this.rotated = rotated;
        }

        int columnHeightMm() {
            return stackCount * demand.getHeightMm();
        }

        int narrowSideMm() {
            return Math.min(boxWidthMm, boxLengthMm);
        }
    }

    private static final class SpreadCandidate {
        final TowerColumnBuild source;
        final int sourceColumnIndex;
        final Rect freeRect;
        final int freeRectIndex;
        final int boxWidthMm;
        final int boxLengthMm;
        final boolean rotated;

        SpreadCandidate(TowerColumnBuild source, int sourceColumnIndex,
                        Rect freeRect, int freeRectIndex,
                        int boxWidthMm, int boxLengthMm, boolean rotated) {
            this.source = source;
            this.sourceColumnIndex = sourceColumnIndex;
            this.freeRect = freeRect;
            this.freeRectIndex = freeRectIndex;
            this.boxWidthMm = boxWidthMm;
            this.boxLengthMm = boxLengthMm;
            this.rotated = rotated;
        }

        int wasteAreaMm2() {
            return freeRect.areaMm2() - (boxWidthMm * boxLengthMm);
        }
    }

    private static final class TowerColumnCandidate {
        final BoxDemand demand;
        final int demandIndex;
        final Rect freeRect;
        final int freeRectIndex;
        final int boxWidthMm;
        final int boxLengthMm;
        final int stackCount;
        final int columnHeightMm;
        final boolean rotated;
        final int targetHeightMm;

        TowerColumnCandidate(BoxDemand demand, int demandIndex, Rect freeRect, int freeRectIndex,
                             int boxWidthMm, int boxLengthMm, int stackCount,
                             int palletHeightMm, boolean rotated, int targetHeightMm) {
            this.demand = demand;
            this.demandIndex = demandIndex;
            this.freeRect = freeRect;
            this.freeRectIndex = freeRectIndex;
            this.boxWidthMm = boxWidthMm;
            this.boxLengthMm = boxLengthMm;
            this.stackCount = stackCount;
            this.columnHeightMm = stackCount * demand.getHeightMm();
            this.rotated = rotated;
            this.targetHeightMm = targetHeightMm;
        }

        int boxAreaMm2() {
            return boxWidthMm * boxLengthMm;
        }

        int wasteAreaMm2() {
            return freeRect.areaMm2() - boxAreaMm2();
        }

        int shortWasteMm() {
            return Math.min(freeRect.widthMm - boxWidthMm, freeRect.lengthMm - boxLengthMm);
        }

        int balanceDistanceMm() {
            return Math.abs(columnHeightMm - targetHeightMm);
        }

        long columnVolumeMm3() {
            return demand.getVolumeMm3() * (long) stackCount;
        }
    }

    private static final class TierBuild {
        final int normalizedHeightMm;
        final int zMm;
        final List<Placement> placements;
        final int boxCount;
        final int usedAreaMm2;
        final double totalWeightKg;
        final long totalVolumeMm3;
        final Map<String, Integer> usedByDemand;

        TierBuild(int normalizedHeightMm, int zMm, List<Placement> placements,
                  int usedAreaMm2, double totalWeightKg, long totalVolumeMm3,
                  Map<String, Integer> usedByDemand) {
            this.normalizedHeightMm = normalizedHeightMm;
            this.zMm = zMm;
            this.placements = Collections.unmodifiableList(new ArrayList<Placement>(placements));
            this.boxCount = placements.size();
            this.usedAreaMm2 = usedAreaMm2;
            this.totalWeightKg = totalWeightKg;
            this.totalVolumeMm3 = totalVolumeMm3;
            this.usedByDemand = new LinkedHashMap<String, Integer>(usedByDemand);
        }

        TierPlan toTierPlan() {
            return new TierPlan(normalizedHeightMm, zMm, placements, usedAreaMm2, totalWeightKg);
        }
    }

    private static final class LoadBuild {
        final PalletSpec palletSpec;
        final int loadNumber;
        final StrategySpec strategySpec;
        final List<TierBuild> tiers;
        final List<Placement> placements;
        final boolean towerBased;
        final int totalBoxes;
        final int totalLoadedHeightMm;
        final double totalWeightKg;
        final long totalBoxVolumeMm3;
        final Map<String, Integer> usedByDemand;

        LoadBuild(PalletSpec palletSpec, int loadNumber, StrategySpec strategySpec,
                  List<TierBuild> tiers, List<Placement> placements,
                  double totalWeightKg, long totalBoxVolumeMm3,
                  Map<String, Integer> usedByDemand) {
            this.palletSpec = palletSpec;
            this.loadNumber = loadNumber;
            this.strategySpec = strategySpec;
            this.tiers = Collections.unmodifiableList(new ArrayList<TierBuild>(tiers));
            this.placements = Collections.unmodifiableList(new ArrayList<Placement>(placements));
            this.towerBased = strategySpec.towerBased;
            this.totalBoxes = placements.size();
            this.totalLoadedHeightMm = computeTotalLoadedHeightMm(palletSpec, placements);
            this.totalWeightKg = totalWeightKg;
            this.totalBoxVolumeMm3 = totalBoxVolumeMm3;
            this.usedByDemand = new LinkedHashMap<String, Integer>(usedByDemand);
        }

        static LoadBuild empty(PalletSpec palletSpec, int loadNumber, StrategySpec strategySpec) {
            return new LoadBuild(palletSpec, loadNumber, strategySpec,
                    Collections.<TierBuild>emptyList(), Collections.<Placement>emptyList(),
                    0d, 0L, Collections.<String, Integer>emptyMap());
        }

        static LoadBuild fromTiers(PalletSpec palletSpec, int loadNumber,
                                   StrategySpec strategySpec, List<TierBuild> tiers) {
            List<Placement> placements = new ArrayList<Placement>();
            double totalWeightKg = 0d;
            long totalVolumeMm3 = 0L;
            Map<String, Integer> usedByDemand = new LinkedHashMap<String, Integer>();
            for (int i = 0; i < tiers.size(); i++) {
                TierBuild tier = tiers.get(i);
                placements.addAll(tier.placements);
                totalWeightKg += tier.totalWeightKg;
                totalVolumeMm3 += tier.totalVolumeMm3;
                for (Map.Entry<String, Integer> entry : tier.usedByDemand.entrySet()) {
                    String key = entry.getKey();
                    usedByDemand.put(key,
                            Integer.valueOf(valueOrZero(usedByDemand, key)
                                    + entry.getValue().intValue()));
                }
            }
            return new LoadBuild(palletSpec, loadNumber, strategySpec, tiers, placements,
                    totalWeightKg, totalVolumeMm3, usedByDemand);
        }

        static LoadBuild fromPlacements(PalletSpec palletSpec, int loadNumber,
                                        StrategySpec strategySpec, List<TierBuild> tiers,
                                        List<Placement> placements,
                                        double totalWeightKg, long totalVolumeMm3,
                                        Map<String, Integer> usedByDemand) {
            return new LoadBuild(palletSpec, loadNumber, strategySpec, tiers, placements,
                    totalWeightKg, totalVolumeMm3, usedByDemand);
        }

        private static int computeTotalLoadedHeightMm(PalletSpec palletSpec,
                                                      List<Placement> placements) {
            int maxTopMm = palletSpec.getHeightMm();
            for (int i = 0; i < placements.size(); i++) {
                Placement placement = placements.get(i);
                int topMm = placement.getZMm() + placement.getHeightMm();
                if (topMm > maxTopMm) {
                    maxTopMm = topMm;
                }
            }
            return maxTopMm;
        }

        PalletLoad toPalletLoad() {
            List<TierPlan> tierPlans = new ArrayList<TierPlan>();
            for (int i = 0; i < tiers.size(); i++) {
                tierPlans.add(tiers.get(i).toTierPlan());
            }
            return new PalletLoad(loadNumber, palletSpec, tierPlans, placements, totalBoxes,
                    totalLoadedHeightMm, totalWeightKg, totalBoxVolumeMm3,
                    strategySpec.id, strategySpec.label, towerBased);
        }
    }

    private static final class PlanCandidate {
        final StrategySpec strategy;
        final int maxTotalHeightMm;
        final List<PalletSpec> selectedPallets;
        final List<LoadBuild> loads;
        final List<PlanResult.LeftoverDemand> leftovers;
        final int totalBoxesLoaded;
        final int leftoverQuantity;
        final long totalBoxVolumeMm3;
        final long totalCapacityVolumeMm3;

        PlanCandidate(StrategySpec strategy, int maxTotalHeightMm,
                      List<PalletSpec> selectedPallets, List<LoadBuild> loads,
                      List<PlanResult.LeftoverDemand> leftovers,
                      int totalBoxesLoaded, int leftoverQuantity,
                      long totalBoxVolumeMm3, long totalCapacityVolumeMm3) {
            this.strategy = strategy;
            this.maxTotalHeightMm = maxTotalHeightMm;
            this.selectedPallets = Collections.unmodifiableList(
                    new ArrayList<PalletSpec>(selectedPallets));
            this.loads = Collections.unmodifiableList(new ArrayList<LoadBuild>(loads));
            this.leftovers = Collections.unmodifiableList(
                    new ArrayList<PlanResult.LeftoverDemand>(leftovers));
            this.totalBoxesLoaded = totalBoxesLoaded;
            this.leftoverQuantity = leftoverQuantity;
            this.totalBoxVolumeMm3 = totalBoxVolumeMm3;
            this.totalCapacityVolumeMm3 = totalCapacityVolumeMm3;
        }

        PlanResult.StrategySummary toSummary() {
            return new PlanResult.StrategySummary(strategy.id, strategy.label,
                    strategy.towerBased, loads.size(), leftoverQuantity,
                    totalBoxesLoaded, totalBoxVolumeMm3, totalCapacityVolumeMm3);
        }

        PlanResult toPlanResult(List<PalletSpec> allPallets,
                                List<PlanResult.StrategySummary> summaries) {
            List<PalletLoad> palletLoads = new ArrayList<PalletLoad>(loads.size());
            for (int i = 0; i < loads.size(); i++) {
                palletLoads.add(loads.get(i).toPalletLoad());
            }
            return new PlanResult(maxTotalHeightMm, allPallets, selectedPallets,
                    palletLoads, leftovers, totalBoxesLoaded,
                    strategy.id, strategy.label, summaries);
        }
    }
}
