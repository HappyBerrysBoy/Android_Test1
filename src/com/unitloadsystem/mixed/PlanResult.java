package com.unitloadsystem.mixed;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class PlanResult implements Serializable {
    private static final long serialVersionUID = 1L;

    private final int maxTotalHeightMm;
    private final List<PalletSpec> palletCandidates;
    private final List<PalletSpec> selectedPalletCandidates;
    private final List<PalletLoad> palletLoads;
    private final List<LeftoverDemand> leftovers;
    private final int totalBoxesLoaded;
    private final String chosenStrategyId;
    private final String chosenStrategyLabel;
    private final List<StrategySummary> evaluatedStrategies;

    public PlanResult(int maxTotalHeightMm, List<PalletSpec> palletCandidates,
                      List<PalletSpec> selectedPalletCandidates,
                      List<PalletLoad> palletLoads,
                      List<LeftoverDemand> leftovers,
                      int totalBoxesLoaded) {
        this(maxTotalHeightMm, palletCandidates, selectedPalletCandidates,
                palletLoads, leftovers, totalBoxesLoaded,
                "", "", Collections.<StrategySummary>emptyList());
    }

    public PlanResult(int maxTotalHeightMm, List<PalletSpec> palletCandidates,
                      List<PalletSpec> selectedPalletCandidates,
                      List<PalletLoad> palletLoads,
                      List<LeftoverDemand> leftovers,
                      int totalBoxesLoaded,
                      String chosenStrategyId,
                      String chosenStrategyLabel,
                      List<StrategySummary> evaluatedStrategies) {
        this.maxTotalHeightMm = maxTotalHeightMm;
        this.palletCandidates = immutableCopy(palletCandidates);
        this.selectedPalletCandidates = immutableCopy(selectedPalletCandidates);
        this.palletLoads = immutableCopy(palletLoads);
        this.leftovers = immutableCopy(leftovers);
        this.totalBoxesLoaded = totalBoxesLoaded;
        this.chosenStrategyId = chosenStrategyId == null ? "" : chosenStrategyId;
        this.chosenStrategyLabel = chosenStrategyLabel == null ? "" : chosenStrategyLabel;
        this.evaluatedStrategies = immutableCopy(evaluatedStrategies);
    }

    public int getMaxTotalHeightMm() {
        return maxTotalHeightMm;
    }

    public List<PalletSpec> getPalletCandidates() {
        return palletCandidates;
    }

    public List<PalletSpec> getSelectedPalletCandidates() {
        return selectedPalletCandidates;
    }

    public List<PalletLoad> getPalletLoads() {
        return palletLoads;
    }

    public List<LeftoverDemand> getLeftovers() {
        return leftovers;
    }

    public int getTotalBoxesLoaded() {
        return totalBoxesLoaded;
    }

    public String getChosenStrategyId() {
        return chosenStrategyId;
    }

    public String getChosenStrategyLabel() {
        return chosenStrategyLabel;
    }

    public int getEvaluatedStrategyCount() {
        return evaluatedStrategies.size();
    }

    public List<StrategySummary> getEvaluatedStrategies() {
        return evaluatedStrategies;
    }

    public boolean isFullyAssigned() {
        return leftovers.isEmpty();
    }

    private static <T> List<T> immutableCopy(List<T> values) {
        return Collections.unmodifiableList(new ArrayList<T>(values));
    }

    public static final class StrategySummary implements Serializable {
        private static final long serialVersionUID = 1L;

        private final String strategyId;
        private final String strategyLabel;
        private final boolean towerBased;
        private final int palletLoadCount;
        private final int leftoverQuantity;
        private final int totalBoxesLoaded;
        private final long totalBoxVolumeMm3;
        private final long totalCapacityVolumeMm3;

        public StrategySummary(String strategyId, String strategyLabel,
                               boolean towerBased, int palletLoadCount,
                               int leftoverQuantity, int totalBoxesLoaded,
                               long totalBoxVolumeMm3, long totalCapacityVolumeMm3) {
            this.strategyId = strategyId == null ? "" : strategyId;
            this.strategyLabel = strategyLabel == null ? "" : strategyLabel;
            this.towerBased = towerBased;
            this.palletLoadCount = palletLoadCount;
            this.leftoverQuantity = leftoverQuantity;
            this.totalBoxesLoaded = totalBoxesLoaded;
            this.totalBoxVolumeMm3 = totalBoxVolumeMm3;
            this.totalCapacityVolumeMm3 = totalCapacityVolumeMm3;
        }

        public String getStrategyId() {
            return strategyId;
        }

        public String getStrategyLabel() {
            return strategyLabel;
        }

        public boolean isTowerBased() {
            return towerBased;
        }

        public int getPalletLoadCount() {
            return palletLoadCount;
        }

        public int getLeftoverQuantity() {
            return leftoverQuantity;
        }

        public int getTotalBoxesLoaded() {
            return totalBoxesLoaded;
        }

        public long getTotalBoxVolumeMm3() {
            return totalBoxVolumeMm3;
        }

        public long getTotalCapacityVolumeMm3() {
            return totalCapacityVolumeMm3;
        }

        public double getUtilization() {
            if (totalCapacityVolumeMm3 <= 0L) {
                return 0d;
            }
            return (double) totalBoxVolumeMm3 / (double) totalCapacityVolumeMm3;
        }
    }

    public static final class LeftoverDemand implements Serializable {
        private static final long serialVersionUID = 1L;

        private final BoxDemand demand;
        private final int remainingQuantity;
        private final String reason;

        public LeftoverDemand(BoxDemand demand, int remainingQuantity, String reason) {
            if (demand == null) {
                throw new IllegalArgumentException("demand must not be null");
            }
            if (remainingQuantity <= 0) {
                throw new IllegalArgumentException("remainingQuantity must be > 0");
            }
            if (reason == null || reason.trim().length() == 0) {
                throw new IllegalArgumentException("reason must not be blank");
            }
            this.demand = demand;
            this.remainingQuantity = remainingQuantity;
            this.reason = reason;
        }

        public BoxDemand getDemand() {
            return demand;
        }

        public int getRemainingQuantity() {
            return remainingQuantity;
        }

        public String getReason() {
            return reason;
        }
    }
}
