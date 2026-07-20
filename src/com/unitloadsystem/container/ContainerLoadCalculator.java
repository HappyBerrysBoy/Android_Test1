package com.unitloadsystem.container;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class ContainerLoadCalculator {
    public static final int WALL_CLEARANCE_MM = 50;
    public static final int END_CLEARANCE_MM = 50;
    public static final int DOOR_CLEARANCE_MM = 50;
    public static final int TOP_CLEARANCE_MM = 150;

    public static final ContainerType DRY_20 =
            new ContainerType("20'", 2350, 5898, 2390, 2340, 2280);
    public static final ContainerType DRY_40 =
            new ContainerType("40'", 2350, 12032, 2390, 2340, 2280);
    public static final ContainerType DRY_40_HIGH_CUBE =
            new ContainerType("40' HC", 2350, 12032, 2695, 2340, 2585);
    public static final ContainerType DRY_45_HIGH_CUBE =
            new ContainerType("45' HC", 2352, 13556, 2697, 2340, 2585);

    private static final List<ContainerType> SUPPORTED_TYPES =
            Collections.unmodifiableList(Arrays.asList(
                    DRY_20, DRY_40, DRY_40_HIGH_CUBE, DRY_45_HIGH_CUBE));

    private ContainerLoadCalculator() {
    }

    public static List<ContainerType> supportedTypes() {
        return SUPPORTED_TYPES;
    }

    public static Estimate estimate(ContainerType container, int palletWidthMm,
                                    int palletLengthMm, float loadedBoxHeightMm,
                                    float palletHeightMm,
                                    int requiredPallets) {
        int floorCapacity = maxFloorCapacity(container.getUsableWidthMm(),
                container.getUsableLengthMm(),
                palletWidthMm, palletLengthMm);
        float unitLoadHeight = Math.max(0f, loadedBoxHeightMm)
                + Math.max(0f, palletHeightMm);
        int verticalTiers = unitLoadHeight > 0f
                && unitLoadHeight <= container.getUsableDoorHeightMm()
                && unitLoadHeight * 2f <= container.getUsableHeightMm()
                ? 2 : 1;
        int stackedCapacity = floorCapacity * verticalTiers;

        return new Estimate(container, floorCapacity,
                containersNeeded(requiredPallets, floorCapacity),
                verticalTiers, stackedCapacity,
                containersNeeded(requiredPallets, stackedCapacity));
    }

    static int maxFloorCapacity(int containerWidth, int containerLength,
                                int palletWidth, int palletLength) {
        if (containerWidth <= 0 || containerLength <= 0
                || palletWidth <= 0 || palletLength <= 0) {
            return 0;
        }

        int best = gridCount(containerWidth, containerLength, palletWidth, palletLength);
        best = Math.max(best,
                gridCount(containerWidth, containerLength, palletLength, palletWidth));

        for (int split = 0; split <= containerWidth; split++) {
            int first = gridCount(split, containerLength, palletWidth, palletLength);
            int second = gridCount(containerWidth - split, containerLength,
                    palletLength, palletWidth);
            best = Math.max(best, first + second);
        }

        for (int split = 0; split <= containerLength; split++) {
            int first = gridCount(containerWidth, split, palletWidth, palletLength);
            int second = gridCount(containerWidth, containerLength - split,
                    palletLength, palletWidth);
            best = Math.max(best, first + second);
        }

        return best;
    }

    private static int gridCount(int areaWidth, int areaLength,
                                 int palletWidth, int palletLength) {
        return (areaWidth / palletWidth) * (areaLength / palletLength);
    }

    private static int containersNeeded(int requiredPallets, int capacity) {
        if (requiredPallets <= 0 || capacity <= 0) {
            return 0;
        }
        return (requiredPallets + capacity - 1) / capacity;
    }

    public static final class ContainerType {
        public final String name;
        public final int widthMm;
        public final int lengthMm;
        public final int heightMm;
        public final int doorWidthMm;
        public final int doorHeightMm;

        ContainerType(String name, int widthMm, int lengthMm, int heightMm,
                      int doorWidthMm, int doorHeightMm) {
            this.name = name;
            this.widthMm = widthMm;
            this.lengthMm = lengthMm;
            this.heightMm = heightMm;
            this.doorWidthMm = doorWidthMm;
            this.doorHeightMm = doorHeightMm;
        }

        public int getUsableWidthMm() {
            return Math.max(0, widthMm - WALL_CLEARANCE_MM * 2);
        }

        public int getUsableLengthMm() {
            return Math.max(0, lengthMm - END_CLEARANCE_MM * 2);
        }

        public int getUsableHeightMm() {
            return Math.max(0, heightMm - TOP_CLEARANCE_MM);
        }

        public int getUsableDoorWidthMm() {
            return Math.max(0, doorWidthMm - DOOR_CLEARANCE_MM * 2);
        }

        public int getUsableDoorHeightMm() {
            return Math.max(0, doorHeightMm - DOOR_CLEARANCE_MM);
        }
    }

    public static final class Estimate {
        public final ContainerType container;
        public final int floorCapacity;
        public final int singleStackContainers;
        public final int verticalTiers;
        public final int stackedCapacity;
        public final int stackedContainers;

        Estimate(ContainerType container, int floorCapacity, int singleStackContainers,
                 int verticalTiers, int stackedCapacity, int stackedContainers) {
            this.container = container;
            this.floorCapacity = floorCapacity;
            this.singleStackContainers = singleStackContainers;
            this.verticalTiers = verticalTiers;
            this.stackedCapacity = stackedCapacity;
            this.stackedContainers = stackedContainers;
        }

        public boolean canDoubleStack() {
            return verticalTiers == 2;
        }
    }
}
