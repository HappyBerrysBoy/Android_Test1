package com.unitloadsystem.mixed;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class TierPlan implements Serializable {
    private static final long serialVersionUID = 1L;

    private final int normalizedHeightMm;
    private final int zMm;
    private final List<Placement> placements;
    private final int usedAreaMm2;
    private final double totalWeightKg;

    public TierPlan(int normalizedHeightMm, int zMm, List<Placement> placements,
                    int usedAreaMm2, double totalWeightKg) {
        if (normalizedHeightMm <= 0) {
            throw new IllegalArgumentException("normalizedHeightMm must be > 0");
        }
        if (zMm < 0) {
            throw new IllegalArgumentException("zMm must be >= 0");
        }
        this.normalizedHeightMm = normalizedHeightMm;
        this.zMm = zMm;
        this.placements = Collections.unmodifiableList(new ArrayList<Placement>(placements));
        this.usedAreaMm2 = usedAreaMm2;
        this.totalWeightKg = totalWeightKg;
    }

    public int getNormalizedHeightMm() {
        return normalizedHeightMm;
    }

    public int getZMm() {
        return zMm;
    }

    public List<Placement> getPlacements() {
        return placements;
    }

    public int getUsedAreaMm2() {
        return usedAreaMm2;
    }

    public double getTotalWeightKg() {
        return totalWeightKg;
    }

    public int getBoxCount() {
        return placements.size();
    }
}
