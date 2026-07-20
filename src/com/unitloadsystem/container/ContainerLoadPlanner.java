package com.unitloadsystem.container;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public final class ContainerLoadPlanner {
    public Plan plan(ContainerLoadCalculator.ContainerType containerType,
                     List<UnitLoad> requestedLoads, boolean allowDoubleStack) {
        if (containerType == null) {
            throw new IllegalArgumentException("containerType must not be null");
        }
        if (requestedLoads == null) {
            throw new IllegalArgumentException("requestedLoads must not be null");
        }

        List<UnitLoad> validLoads = new ArrayList<UnitLoad>();
        List<UnitLoad> unplaced = new ArrayList<UnitLoad>();
        for (int i = 0; i < requestedLoads.size(); i++) {
            UnitLoad load = requestedLoads.get(i);
            if (load == null) {
                throw new IllegalArgumentException("requestedLoads contains null");
            }
            if (canEnterAndFit(containerType, load)) {
                validLoads.add(load);
            } else {
                unplaced.add(load);
            }
        }
        Collections.sort(validLoads, UNIT_ORDER);

        List<Column> columns = buildColumns(containerType, validLoads, allowDoubleStack);
        Collections.sort(columns, COLUMN_ORDER);
        List<ContainerBuild> containers = new ArrayList<ContainerBuild>();

        for (int i = 0; i < columns.size(); i++) {
            Column column = columns.get(i);
            PlacementChoice best = null;
            for (int containerIndex = 0; containerIndex < containers.size(); containerIndex++) {
                best = pickBetterPlacement(best, containers.get(containerIndex),
                        containerIndex, column, false);
                if (column.widthMm != column.lengthMm) {
                    best = pickBetterPlacement(best, containers.get(containerIndex),
                            containerIndex, column, true);
                }
            }

            if (best == null) {
                ContainerBuild added = new ContainerBuild(containerType);
                containers.add(added);
                int containerIndex = containers.size() - 1;
                best = pickBetterPlacement(null, added, containerIndex, column, false);
                if (column.widthMm != column.lengthMm) {
                    best = pickBetterPlacement(best, added, containerIndex, column, true);
                }
            }

            if (best == null) {
                unplaced.addAll(column.tiers);
                continue;
            }
            ContainerBuild target = containers.get(best.containerIndex);
            target.add(column, best.freeRect.xMm, best.freeRect.yMm,
                    best.widthMm, best.lengthMm, best.rotated);
            splitFreeRects(target.freeRects, new Rect(best.freeRect.xMm, best.freeRect.yMm,
                    best.widthMm, best.lengthMm));
            sortFreeRects(target.freeRects);
        }

        List<ContainerPlan> plans = new ArrayList<ContainerPlan>();
        for (int i = 0; i < containers.size(); i++) {
            ContainerBuild container = containers.get(i);
            if (!container.placements.isEmpty()) {
                plans.add(container.toPlan(i + 1));
            }
        }
        return new Plan(containerType, allowDoubleStack, plans, unplaced);
    }

    private List<Column> buildColumns(ContainerLoadCalculator.ContainerType containerType,
                                      List<UnitLoad> loads, boolean allowDoubleStack) {
        List<Column> columns = new ArrayList<Column>();
        boolean[] used = new boolean[loads.size()];
        for (int i = 0; i < loads.size(); i++) {
            if (used[i]) {
                continue;
            }
            UnitLoad lower = loads.get(i);
            used[i] = true;
            List<UnitLoad> tiers = new ArrayList<UnitLoad>();
            tiers.add(lower);

            if (allowDoubleStack) {
                int upperIndex = findUpperLoad(containerType, loads, used, lower);
                if (upperIndex >= 0) {
                    tiers.add(loads.get(upperIndex));
                    used[upperIndex] = true;
                }
            }
            columns.add(new Column(lower.widthMm, lower.lengthMm, tiers));
        }
        return columns;
    }

    private int findUpperLoad(ContainerLoadCalculator.ContainerType containerType,
                              List<UnitLoad> loads, boolean[] used, UnitLoad lower) {
        int bestIndex = -1;
        for (int i = 0; i < loads.size(); i++) {
            if (used[i]) {
                continue;
            }
            UnitLoad candidate = loads.get(i);
            if (!sameFootprint(lower, candidate)
                    || lower.heightMm + candidate.heightMm
                    > containerType.getUsableHeightMm()) {
                continue;
            }
            if (bestIndex < 0 || UNIT_ORDER.compare(candidate, loads.get(bestIndex)) < 0) {
                bestIndex = i;
            }
        }
        return bestIndex;
    }

    private boolean sameFootprint(UnitLoad left, UnitLoad right) {
        return left.widthMm == right.widthMm && left.lengthMm == right.lengthMm
                || left.widthMm == right.lengthMm && left.lengthMm == right.widthMm;
    }

    private boolean canEnterAndFit(ContainerLoadCalculator.ContainerType container,
                                   UnitLoad load) {
        if (load.widthMm <= 0 || load.lengthMm <= 0 || load.heightMm <= 0
                || load.heightMm > container.getUsableDoorHeightMm()) {
            return false;
        }
        boolean footprintFits = fits(container.getUsableWidthMm(), container.getUsableLengthMm(),
                load.widthMm, load.lengthMm)
                || fits(container.getUsableWidthMm(), container.getUsableLengthMm(),
                load.lengthMm, load.widthMm);
        boolean passesDoor = Math.min(load.widthMm, load.lengthMm)
                <= container.getUsableDoorWidthMm();
        return footprintFits && passesDoor;
    }

    private PlacementChoice pickBetterPlacement(PlacementChoice currentBest,
                                                ContainerBuild container,
                                                int containerIndex, Column column,
                                                boolean rotated) {
        int widthMm = rotated ? column.lengthMm : column.widthMm;
        int lengthMm = rotated ? column.widthMm : column.lengthMm;
        for (int i = 0; i < container.freeRects.size(); i++) {
            Rect free = container.freeRects.get(i);
            if (!fits(free.widthMm, free.lengthMm, widthMm, lengthMm)) {
                continue;
            }
            PlacementChoice candidate = new PlacementChoice(containerIndex, column,
                    free, i, widthMm, lengthMm, rotated);
            if (currentBest == null || PLACEMENT_ORDER.compare(candidate, currentBest) < 0) {
                currentBest = candidate;
            }
        }
        return currentBest;
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

    private void addRect(List<Rect> freeRects, int xMm, int yMm,
                         int widthMm, int lengthMm) {
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

    private boolean intersects(Rect left, Rect right) {
        return left.xMm < right.xMm + right.widthMm
                && left.xMm + left.widthMm > right.xMm
                && left.yMm < right.yMm + right.lengthMm
                && left.yMm + left.lengthMm > right.yMm;
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

    private static boolean fits(int areaWidthMm, int areaLengthMm,
                                int widthMm, int lengthMm) {
        return widthMm <= areaWidthMm && lengthMm <= areaLengthMm;
    }

    public static final class UnitLoad {
        private final String id;
        private final String label;
        private final int widthMm;
        private final int lengthMm;
        private final int heightMm;

        public UnitLoad(String id, String label, int widthMm, int lengthMm, int heightMm) {
            if (id == null || id.trim().length() == 0) {
                throw new IllegalArgumentException("id must not be blank");
            }
            this.id = id;
            this.label = label == null ? id : label;
            this.widthMm = widthMm;
            this.lengthMm = lengthMm;
            this.heightMm = heightMm;
        }

        public String getId() { return id; }
        public String getLabel() { return label; }
        public int getWidthMm() { return widthMm; }
        public int getLengthMm() { return lengthMm; }
        public int getHeightMm() { return heightMm; }

        long areaMm2() {
            return (long) widthMm * lengthMm;
        }
    }

    public static final class Placement {
        private final int xMm;
        private final int yMm;
        private final int widthMm;
        private final int lengthMm;
        private final boolean rotated;
        private final List<UnitLoad> tiers;

        Placement(int xMm, int yMm, int widthMm, int lengthMm,
                  boolean rotated, List<UnitLoad> tiers) {
            this.xMm = xMm;
            this.yMm = yMm;
            this.widthMm = widthMm;
            this.lengthMm = lengthMm;
            this.rotated = rotated;
            this.tiers = Collections.unmodifiableList(new ArrayList<UnitLoad>(tiers));
        }

        public int getXMm() { return xMm; }
        public int getYMm() { return yMm; }
        public int getWidthMm() { return widthMm; }
        public int getLengthMm() { return lengthMm; }
        public boolean isRotated() { return rotated; }
        public List<UnitLoad> getTiers() { return tiers; }

        public int getTotalHeightMm() {
            int total = 0;
            for (int i = 0; i < tiers.size(); i++) {
                total += tiers.get(i).heightMm;
            }
            return total;
        }

        public int getLoadCount() {
            return tiers.size();
        }
    }

    public static final class ContainerPlan {
        private final int number;
        private final List<Placement> placements;

        ContainerPlan(int number, List<Placement> placements) {
            this.number = number;
            this.placements = Collections.unmodifiableList(
                    new ArrayList<Placement>(placements));
        }

        public int getNumber() { return number; }
        public List<Placement> getPlacements() { return placements; }

        public int getLoadCount() {
            int count = 0;
            for (int i = 0; i < placements.size(); i++) {
                count += placements.get(i).getLoadCount();
            }
            return count;
        }
    }

    public static final class Plan {
        private final ContainerLoadCalculator.ContainerType containerType;
        private final boolean doubleStackAllowed;
        private final List<ContainerPlan> containers;
        private final List<UnitLoad> unplaced;

        Plan(ContainerLoadCalculator.ContainerType containerType,
             boolean doubleStackAllowed, List<ContainerPlan> containers,
             List<UnitLoad> unplaced) {
            this.containerType = containerType;
            this.doubleStackAllowed = doubleStackAllowed;
            this.containers = Collections.unmodifiableList(
                    new ArrayList<ContainerPlan>(containers));
            this.unplaced = Collections.unmodifiableList(new ArrayList<UnitLoad>(unplaced));
        }

        public ContainerLoadCalculator.ContainerType getContainerType() { return containerType; }
        public boolean isDoubleStackAllowed() { return doubleStackAllowed; }
        public List<ContainerPlan> getContainers() { return containers; }
        public List<UnitLoad> getUnplaced() { return unplaced; }

        public int getPlacedLoadCount() {
            int count = 0;
            for (int i = 0; i < containers.size(); i++) {
                count += containers.get(i).getLoadCount();
            }
            return count;
        }
    }

    private static final class Column {
        final int widthMm;
        final int lengthMm;
        final int heightMm;
        final List<UnitLoad> tiers;

        Column(int widthMm, int lengthMm, List<UnitLoad> tiers) {
            this.widthMm = widthMm;
            this.lengthMm = lengthMm;
            this.tiers = Collections.unmodifiableList(new ArrayList<UnitLoad>(tiers));
            int totalHeight = 0;
            for (int i = 0; i < tiers.size(); i++) {
                totalHeight += tiers.get(i).heightMm;
            }
            this.heightMm = totalHeight;
        }

        long areaMm2() {
            return (long) widthMm * lengthMm;
        }

        String stableId() {
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < tiers.size(); i++) {
                if (i > 0) {
                    builder.append('|');
                }
                builder.append(tiers.get(i).id);
            }
            return builder.toString();
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

        long areaMm2() {
            return (long) widthMm * lengthMm;
        }
    }

    private static final class PlacementChoice {
        final int containerIndex;
        final Column column;
        final Rect freeRect;
        final int freeRectIndex;
        final int widthMm;
        final int lengthMm;
        final boolean rotated;

        PlacementChoice(int containerIndex, Column column, Rect freeRect,
                        int freeRectIndex, int widthMm, int lengthMm, boolean rotated) {
            this.containerIndex = containerIndex;
            this.column = column;
            this.freeRect = freeRect;
            this.freeRectIndex = freeRectIndex;
            this.widthMm = widthMm;
            this.lengthMm = lengthMm;
            this.rotated = rotated;
        }

        long wasteAreaMm2() {
            return freeRect.areaMm2() - ((long) widthMm * lengthMm);
        }

        int shortWasteMm() {
            return Math.min(freeRect.widthMm - widthMm,
                    freeRect.lengthMm - lengthMm);
        }
    }

    private static final class ContainerBuild {
        final List<Rect> freeRects = new ArrayList<Rect>();
        final List<Placement> placements = new ArrayList<Placement>();

        ContainerBuild(ContainerLoadCalculator.ContainerType containerType) {
            freeRects.add(new Rect(ContainerLoadCalculator.WALL_CLEARANCE_MM,
                    ContainerLoadCalculator.END_CLEARANCE_MM,
                    containerType.getUsableWidthMm(), containerType.getUsableLengthMm()));
        }

        void add(Column column, int xMm, int yMm, int widthMm, int lengthMm,
                 boolean rotated) {
            placements.add(new Placement(xMm, yMm, widthMm, lengthMm,
                    rotated, column.tiers));
        }

        ContainerPlan toPlan(int number) {
            return new ContainerPlan(number, placements);
        }
    }

    private static final Comparator<UnitLoad> UNIT_ORDER = new Comparator<UnitLoad>() {
        @Override
        public int compare(UnitLoad left, UnitLoad right) {
            int area = Long.compare(right.areaMm2(), left.areaMm2());
            if (area != 0) return area;
            int height = Integer.compare(right.heightMm, left.heightMm);
            if (height != 0) return height;
            int side = Integer.compare(
                    Math.max(right.widthMm, right.lengthMm),
                    Math.max(left.widthMm, left.lengthMm));
            if (side != 0) return side;
            return left.id.compareTo(right.id);
        }
    };

    private static final Comparator<Column> COLUMN_ORDER = new Comparator<Column>() {
        @Override
        public int compare(Column left, Column right) {
            int area = Long.compare(right.areaMm2(), left.areaMm2());
            if (area != 0) return area;
            int height = Integer.compare(right.heightMm, left.heightMm);
            if (height != 0) return height;
            return left.stableId().compareTo(right.stableId());
        }
    };

    private static final Comparator<Rect> RECT_ORDER = new Comparator<Rect>() {
        @Override
        public int compare(Rect left, Rect right) {
            int y = Integer.compare(left.yMm, right.yMm);
            if (y != 0) return y;
            int x = Integer.compare(left.xMm, right.xMm);
            if (x != 0) return x;
            return Long.compare(left.areaMm2(), right.areaMm2());
        }
    };

    private static final Comparator<PlacementChoice> PLACEMENT_ORDER =
            new Comparator<PlacementChoice>() {
                @Override
                public int compare(PlacementChoice left, PlacementChoice right) {
                    int container = Integer.compare(left.containerIndex, right.containerIndex);
                    if (container != 0) return container;
                    int waste = Long.compare(left.wasteAreaMm2(), right.wasteAreaMm2());
                    if (waste != 0) return waste;
                    int shortWaste = Integer.compare(left.shortWasteMm(), right.shortWasteMm());
                    if (shortWaste != 0) return shortWaste;
                    int y = Integer.compare(left.freeRect.yMm, right.freeRect.yMm);
                    if (y != 0) return y;
                    int x = Integer.compare(left.freeRect.xMm, right.freeRect.xMm);
                    if (x != 0) return x;
                    if (left.rotated != right.rotated) return left.rotated ? 1 : -1;
                    return Integer.compare(left.freeRectIndex, right.freeRectIndex);
                }
            };
}
