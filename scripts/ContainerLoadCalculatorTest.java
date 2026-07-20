package com.unitloadsystem.container;

public final class ContainerLoadCalculatorTest {
    public static void main(String[] args) {
        ContainerLoadCalculator.Estimate twenty = ContainerLoadCalculator.estimate(
                ContainerLoadCalculator.DRY_20, 1100, 1100, 900, 150, 21);
        assertEquals("20ft floor capacity", 10, twenty.floorCapacity);
        assertEquals("20ft single-stack containers", 3, twenty.singleStackContainers);
        assertEquals("20ft double-stack tiers", 2, twenty.verticalTiers);
        assertEquals("20ft double-stack containers", 2, twenty.stackedContainers);

        ContainerLoadCalculator.Estimate forty = ContainerLoadCalculator.estimate(
                ContainerLoadCalculator.DRY_40, 1100, 1100, 900, 150, 21);
        assertEquals("40ft floor capacity", 20, forty.floorCapacity);
        assertEquals("40ft single-stack containers", 2, forty.singleStackContainers);
        assertEquals("40ft double-stack containers", 1, forty.stackedContainers);

        ContainerLoadCalculator.Estimate tooTall = ContainerLoadCalculator.estimate(
                ContainerLoadCalculator.DRY_20, 1100, 1100, 1500, 150, 21);
        assertEquals("tall load tiers", 1, tooTall.verticalTiers);
        assertEquals("tall load capacity", 10, tooTall.stackedCapacity);

        ContainerLoadCalculator.Estimate euro = ContainerLoadCalculator.estimate(
                ContainerLoadCalculator.DRY_20, 1200, 800, 800, 150, 22);
        assertEquals("20ft euro pallet mixed orientation", 11, euro.floorCapacity);

        ContainerLoadCalculator.Estimate tallPallet = ContainerLoadCalculator.estimate(
                ContainerLoadCalculator.DRY_20, 1100, 1100, 1000, 250, 21);
        assertEquals("actual pallet height limits double stacking", 1, tallPallet.verticalTiers);

        assertEquals("20ft usable height includes top clearance", 2240,
                ContainerLoadCalculator.DRY_20.getUsableHeightMm());
        ContainerLoadCalculator.Estimate clearanceLimited = ContainerLoadCalculator.estimate(
                ContainerLoadCalculator.DRY_20, 1100, 1100, 971, 150, 2);
        assertEquals("pallet plus cargo respects top clearance", 1,
                clearanceLimited.verticalTiers);

        System.out.println("ContainerLoadCalculator tests passed");
    }

    private static void assertEquals(String name, int expected, int actual) {
        if (expected != actual) {
            throw new AssertionError(name + ": expected " + expected + ", got " + actual);
        }
    }
}
