package com.unitloadsystem.mixed;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class PalletLoad implements Serializable {
    private static final long serialVersionUID = 1L;

    private final int loadNumber;
    private final PalletSpec palletSpec;
    private final List<TierPlan> tiers;
    private final List<Placement> placements;
    private final int totalBoxes;
    private final int totalLoadedHeightMm;
    private final double totalWeightKg;
    private final long totalBoxVolumeMm3;
    private final String strategyId;
    private final String strategyName;
    private final boolean towerBased;

    public PalletLoad(int loadNumber, PalletSpec palletSpec, List<TierPlan> tiers,
                      int totalBoxes, int totalLoadedHeightMm,
                      double totalWeightKg, long totalBoxVolumeMm3) {
        this(loadNumber, palletSpec, tiers, flattenPlacements(tiers), totalBoxes,
                totalLoadedHeightMm, totalWeightKg, totalBoxVolumeMm3,
                "", "", false);
    }

    public PalletLoad(int loadNumber, PalletSpec palletSpec, List<TierPlan> tiers,
                      List<Placement> placements,
                      int totalBoxes, int totalLoadedHeightMm,
                      double totalWeightKg, long totalBoxVolumeMm3,
                      String strategyId, String strategyName, boolean towerBased) {
        if (loadNumber <= 0) {
            throw new IllegalArgumentException("loadNumber must be > 0");
        }
        if (palletSpec == null) {
            throw new IllegalArgumentException("palletSpec must not be null");
        }
        this.loadNumber = loadNumber;
        this.palletSpec = palletSpec;
        this.tiers = Collections.unmodifiableList(new ArrayList<TierPlan>(tiers));
        this.placements = Collections.unmodifiableList(new ArrayList<Placement>(placements));
        this.totalBoxes = totalBoxes;
        this.totalLoadedHeightMm = totalLoadedHeightMm;
        this.totalWeightKg = totalWeightKg;
        this.totalBoxVolumeMm3 = totalBoxVolumeMm3;
        this.strategyId = strategyId == null ? "" : strategyId;
        this.strategyName = strategyName == null ? "" : strategyName;
        this.towerBased = towerBased;
    }

    public int getLoadNumber() {
        return loadNumber;
    }

    public PalletSpec getPalletSpec() {
        return palletSpec;
    }

    public List<TierPlan> getTiers() {
        return tiers;
    }

    public List<Placement> getPlacements() {
        return placements;
    }

    public int getTotalBoxes() {
        return totalBoxes;
    }

    public int getTotalLoadedHeightMm() {
        return totalLoadedHeightMm;
    }

    public double getTotalWeightKg() {
        return totalWeightKg;
    }

    public long getTotalBoxVolumeMm3() {
        return totalBoxVolumeMm3;
    }

    public String getStrategyId() {
        return strategyId;
    }

    public String getStrategyName() {
        return strategyName;
    }

    public boolean isTowerBased() {
        return towerBased;
    }

    private static List<Placement> flattenPlacements(List<TierPlan> tiers) {
        List<Placement> flattened = new ArrayList<Placement>();
        for (int i = 0; i < tiers.size(); i++) {
            flattened.addAll(tiers.get(i).getPlacements());
        }
        return flattened;
    }
}
