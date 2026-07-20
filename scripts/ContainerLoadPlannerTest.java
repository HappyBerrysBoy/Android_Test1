package com.unitloadsystem.container;

import java.util.ArrayList;
import java.util.List;

public final class ContainerLoadPlannerTest {
    public static void main(String[] args) {
        testContainerDimensions();
        testHomogeneousMultiContainerPlan();
        testDoubleStackPlan();
        testDoorHeightAndHighCube();
        testTopClearancePreventsUnsafeDoubleStacking();
        testHeterogeneousPlanHasValidGeometry();
        testPlanningIsDeterministic();

        System.out.println("ContainerLoadPlanner tests passed");
    }

    private static void testContainerDimensions() {
        assertEquals("20ft length", 5898, ContainerLoadCalculator.DRY_20.lengthMm);
        assertEquals("40ft height", 2390, ContainerLoadCalculator.DRY_40.heightMm);
        assertEquals("40ft high cube height", 2695,
                ContainerLoadCalculator.DRY_40_HIGH_CUBE.heightMm);
        assertEquals("45ft high cube length", 13556,
                ContainerLoadCalculator.DRY_45_HIGH_CUBE.lengthMm);
        assertEquals("four supported container types", 4,
                ContainerLoadCalculator.supportedTypes().size());
    }

    private static void testHomogeneousMultiContainerPlan() {
        ContainerLoadPlanner planner = new ContainerLoadPlanner();
        List<ContainerLoadPlanner.UnitLoad> loads = repeatLoads(
                "EU", 22, 1200, 800, 950);

        ContainerLoadPlanner.Plan plan = planner.plan(
                ContainerLoadCalculator.DRY_20, loads, false);

        assertEquals("22 euro pallets need two 20ft containers", 2,
                plan.getContainers().size());
        assertEquals("all homogeneous loads placed", 0, plan.getUnplaced().size());
        assertEquals("first 20ft container has 11 pallets", 11,
                plan.getContainers().get(0).getLoadCount());
        assertValidGeometry(plan);
    }

    private static void testDoubleStackPlan() {
        ContainerLoadPlanner planner = new ContainerLoadPlanner();
        List<ContainerLoadPlanner.UnitLoad> loads = repeatLoads(
                "EU", 22, 1200, 800, 950);

        ContainerLoadPlanner.Plan plan = planner.plan(
                ContainerLoadCalculator.DRY_20, loads, true);

        assertEquals("double-stacked euro pallets fit one 20ft container", 1,
                plan.getContainers().size());
        assertEquals("double-stack places every pallet", 22, plan.getPlacedLoadCount());
        assertTrue("double-stack creates two-tier columns",
                hasTwoTierColumn(plan));
        assertValidGeometry(plan);
    }

    private static void testDoorHeightAndHighCube() {
        ContainerLoadPlanner planner = new ContainerLoadPlanner();
        List<ContainerLoadPlanner.UnitLoad> loads = repeatLoads(
                "TALL", 1, 1100, 1100, 2500);

        ContainerLoadPlanner.Plan standard = planner.plan(
                ContainerLoadCalculator.DRY_40, loads, false);
        ContainerLoadPlanner.Plan highCube = planner.plan(
                ContainerLoadCalculator.DRY_40_HIGH_CUBE, loads, false);

        assertEquals("2500mm load cannot pass standard door", 1,
                standard.getUnplaced().size());
        assertEquals("2500mm load passes high-cube door", 0,
                highCube.getUnplaced().size());
    }

    private static void testTopClearancePreventsUnsafeDoubleStacking() {
        ContainerLoadPlanner planner = new ContainerLoadPlanner();
        List<ContainerLoadPlanner.UnitLoad> loads = repeatLoads(
                "CLEAR", 2, 1100, 1100, 1125);

        ContainerLoadPlanner.Plan plan = planner.plan(
                ContainerLoadCalculator.DRY_20, loads, true);

        assertEquals("two 1125mm pallet loads are not stacked in a 20ft dry", 2,
                plan.getContainers().get(0).getPlacements().size());
        assertTrue("top clearance prevents a two-tier column", !hasTwoTierColumn(plan));
        assertValidGeometry(plan);
    }

    private static void testHeterogeneousPlanHasValidGeometry() {
        ContainerLoadPlanner planner = new ContainerLoadPlanner();
        List<ContainerLoadPlanner.UnitLoad> loads = new ArrayList<ContainerLoadPlanner.UnitLoad>();
        loads.addAll(repeatLoads("A", 8, 1100, 1100, 1200));
        loads.addAll(repeatLoads("B", 9, 1200, 800, 1000));
        loads.addAll(repeatLoads("C", 6, 1000, 1200, 900));

        ContainerLoadPlanner.Plan plan = planner.plan(
                ContainerLoadCalculator.DRY_40_HIGH_CUBE, loads, false);

        assertEquals("heterogeneous loads all placed", 0, plan.getUnplaced().size());
        assertEquals("heterogeneous placed count", 23, plan.getPlacedLoadCount());
        assertValidGeometry(plan);
    }

    private static void testPlanningIsDeterministic() {
        ContainerLoadPlanner planner = new ContainerLoadPlanner();
        List<ContainerLoadPlanner.UnitLoad> loads = new ArrayList<ContainerLoadPlanner.UnitLoad>();
        loads.addAll(repeatLoads("A", 13, 1100, 1100, 1050));
        loads.addAll(repeatLoads("B", 17, 1200, 800, 900));
        loads.addAll(repeatLoads("C", 7, 1000, 1200, 800));

        ContainerLoadPlanner.Plan first = planner.plan(
                ContainerLoadCalculator.DRY_20, loads, false);
        ContainerLoadPlanner.Plan second = planner.plan(
                ContainerLoadCalculator.DRY_20, loads, false);

        assertTrue("same inputs produce the same placement plan",
                planSignature(first).equals(planSignature(second)));
    }

    private static String planSignature(ContainerLoadPlanner.Plan plan) {
        StringBuilder signature = new StringBuilder();
        for (ContainerLoadPlanner.ContainerPlan container : plan.getContainers()) {
            signature.append('C').append(container.getNumber()).append(':');
            for (ContainerLoadPlanner.Placement placement : container.getPlacements()) {
                signature.append(placement.getXMm()).append(',')
                        .append(placement.getYMm()).append(',')
                        .append(placement.getWidthMm()).append(',')
                        .append(placement.getLengthMm()).append(',')
                        .append(placement.isRotated()).append('[');
                for (ContainerLoadPlanner.UnitLoad tier : placement.getTiers()) {
                    signature.append(tier.getId()).append('|');
                }
                signature.append("];\n");
            }
        }
        return signature.toString();
    }

    private static List<ContainerLoadPlanner.UnitLoad> repeatLoads(
            String prefix, int count, int widthMm, int lengthMm, int heightMm) {
        List<ContainerLoadPlanner.UnitLoad> loads =
                new ArrayList<ContainerLoadPlanner.UnitLoad>();
        for (int i = 0; i < count; i++) {
            loads.add(new ContainerLoadPlanner.UnitLoad(
                    prefix + (i + 1), prefix + (i + 1),
                    widthMm, lengthMm, heightMm));
        }
        return loads;
    }

    private static boolean hasTwoTierColumn(ContainerLoadPlanner.Plan plan) {
        for (ContainerLoadPlanner.ContainerPlan container : plan.getContainers()) {
            for (ContainerLoadPlanner.Placement placement : container.getPlacements()) {
                if (placement.getTiers().size() == 2) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void assertValidGeometry(ContainerLoadPlanner.Plan plan) {
        for (ContainerLoadPlanner.ContainerPlan container : plan.getContainers()) {
            List<ContainerLoadPlanner.Placement> placements = container.getPlacements();
            for (int i = 0; i < placements.size(); i++) {
                ContainerLoadPlanner.Placement left = placements.get(i);
                assertTrue("placement x within container",
                        left.getXMm() >= 0
                                && left.getXMm() + left.getWidthMm()
                                <= plan.getContainerType().widthMm);
                assertTrue("placement y within container",
                        left.getYMm() >= 0
                                && left.getYMm() + left.getLengthMm()
                                <= plan.getContainerType().lengthMm);
                assertTrue("placement height within container",
                        left.getTotalHeightMm() <= plan.getContainerType().heightMm);
                for (int j = i + 1; j < placements.size(); j++) {
                    ContainerLoadPlanner.Placement right = placements.get(j);
                    boolean separated = left.getXMm() + left.getWidthMm() <= right.getXMm()
                            || right.getXMm() + right.getWidthMm() <= left.getXMm()
                            || left.getYMm() + left.getLengthMm() <= right.getYMm()
                            || right.getYMm() + right.getLengthMm() <= left.getYMm();
                    assertTrue("container floor placements do not overlap", separated);
                }
            }
        }
    }

    private static void assertEquals(String name, int expected, int actual) {
        if (expected != actual) {
            throw new AssertionError(name + ": expected " + expected + ", got " + actual);
        }
    }

    private static void assertTrue(String name, boolean condition) {
        if (!condition) {
            throw new AssertionError(name);
        }
    }
}
