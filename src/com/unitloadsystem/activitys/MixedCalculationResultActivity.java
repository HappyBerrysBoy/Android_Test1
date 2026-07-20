package com.unitloadsystem.activitys;

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Button;
import android.widget.TextView;

import com.unitloadsystem.container.ContainerLoadPlanner;
import com.unitloadsystem.container.ContainerLoadCalculator;
import com.unitloadsystem.mixed.BoxDemand;
import com.unitloadsystem.mixed.PalletLoad;
import com.unitloadsystem.mixed.PalletSpec;
import com.unitloadsystem.mixed.Placement;
import com.unitloadsystem.mixed.PlanResult;
import com.unitloadsystem.mixed.TierPlan;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MixedCalculationResultActivity extends LocalizedActivity {
    public static final String EXTRA_PLAN_RESULT = "MixedPlanResult";
    public static final String EXTRA_BOX_DEMANDS = "MixedBoxDemands";
    private static final String STATE_RESULT_MODE = "mixed_state_result_mode";
    private static final String STATE_CONTAINER_TYPE = "mixed_state_container_type";
    private static final int MODE_2D = 0;
    private static final int MODE_3D = 1;
    private static final int MODE_CONTAINER = 2;

    private static final int[] BOX_COLORS = {
            Color.rgb(0, 114, 178), Color.rgb(213, 94, 0), Color.rgb(0, 121, 92),
            Color.rgb(167, 87, 145), Color.rgb(216, 143, 0), Color.rgb(74, 149, 181)
    };

    private PlanResult planResult;
    private ArrayList<BoxDemand> boxDemands;
    private int resultMode;
    private String selectedContainerTypeName;
    private final LinkedHashMap<String, Integer> boxTypeIndices =
            new LinkedHashMap<String, Integer>();

    @Override
    @SuppressWarnings("unchecked")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.mixedcalculationresult_layout);
        resultMode = savedInstanceState == null
                ? MODE_2D : savedInstanceState.getInt(STATE_RESULT_MODE, MODE_2D);
        selectedContainerTypeName = savedInstanceState == null
                ? null : savedInstanceState.getString(STATE_CONTAINER_TYPE);
        if (resultMode == MODE_CONTAINER && selectedContainerTypeName == null) {
            resultMode = MODE_2D;
        }
        planResult = (PlanResult) getIntent().getSerializableExtra(EXTRA_PLAN_RESULT);
        boxDemands = (ArrayList<BoxDemand>) getIntent().getSerializableExtra(EXTRA_BOX_DEMANDS);
        if (planResult == null || boxDemands == null) {
            finish();
            return;
        }
        for (int i = 0; i < boxDemands.size(); i++) {
            boxTypeIndices.put(boxDemands.get(i).getId(), i);
        }
        renderResult();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(STATE_RESULT_MODE, resultMode);
        outState.putString(STATE_CONTAINER_TYPE, selectedContainerTypeName);
        super.onSaveInstanceState(outState);
    }

    private void renderResult() {
        int sectionCount = 0;
        for (PalletLoad load : planResult.getPalletLoads()) {
            sectionCount += load.isTowerBased() ? 1 : load.getTiers().size();
        }
        String resultSummary = getString(R.string.mixedResultHeader,
                planResult.getTotalBoxesLoaded(), planResult.getPalletLoads().size(), sectionCount);
        String strategySummary = planResult.getChosenStrategyId().length() == 0
                ? "" : getString(R.string.mixedStrategySummary,
                strategyLabel(planResult.getChosenStrategyId()),
                planResult.getEvaluatedStrategyCount());
        ((TextView) findViewById(R.id.mixedResultHeaderSummary)).setText(
                strategySummary.length() == 0
                        ? resultSummary : resultSummary + "\n" + strategySummary);
        renderLegend();
        renderLeftovers();

        renderPalletResults();
        updateModeVisibility();
        updateModeButtons();
    }

    private void renderPalletResults() {
        LinearLayout container = (LinearLayout) findViewById(R.id.mixedResultPallets);
        container.removeAllViews();
        if (resultMode == MODE_CONTAINER) {
            List<ContainerLoadPlanner.UnitLoad> loads =
                    new ArrayList<ContainerLoadPlanner.UnitLoad>();
            for (int i = 0; i < planResult.getPalletLoads().size(); i++) {
                PalletLoad palletLoad = planResult.getPalletLoads().get(i);
                String label = "P" + (i + 1);
                loads.add(new ContainerLoadPlanner.UnitLoad(
                        label + "-" + palletLoad.getPalletSpec().getId(), label,
                        palletLoad.getPalletSpec().getWidthMm(),
                        palletLoad.getPalletSpec().getLengthMm(),
                        palletLoad.getTotalLoadedHeightMm()));
            }
            List<ContainerResultRenderer.Section> sections =
                    new ArrayList<ContainerResultRenderer.Section>();
            sections.add(new ContainerResultRenderer.Section(
                    getString(R.string.mixedContainerPlanTitle), loads));
            ContainerLoadCalculator.ContainerType type =
                    ContainerResultRenderer.findContainerType(selectedContainerTypeName);
            if (type != null) {
                ContainerResultRenderer.render(this, container, sections, type);
            }
            return;
        }
        for (PalletGroup group : groupAdjacentPalletLoads()) {
            container.addView(createPalletCard(group), blockParams(dp(14)));
        }
    }

    private void renderLegend() {
        LinearLayout legend = (LinearLayout) findViewById(R.id.mixedResultLegend);
        for (int i = 0; i < boxDemands.size(); i++) {
            BoxDemand demand = boxDemands.get(i);
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.addView(createBadge(demand.getId(), i));
            TextView text = baseText(13, R.color.textPrimary, false);
            text.setText(getString(R.string.mixedLegendRow, demand.getId(), demand.getName(),
                    demand.getWidthMm(), demand.getLengthMm(), demand.getHeightMm(),
                    demand.getWeightKg()));
            LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
            textParams.setMargins(dp(10), 0, 0, 0);
            row.addView(text, textParams);
            legend.addView(row, blockParams(dp(8)));
        }
    }

    private void renderLeftovers() {
        if (planResult.getLeftovers().isEmpty()) {
            return;
        }
        StringBuilder summary = new StringBuilder();
        for (int i = 0; i < planResult.getLeftovers().size(); i++) {
            PlanResult.LeftoverDemand leftover = planResult.getLeftovers().get(i);
            if (summary.length() > 0) {
                summary.append(" · ");
            }
            summary.append(leftover.getDemand().getId())
                    .append(" x ").append(leftover.getRemainingQuantity());
        }
        TextView view = (TextView) findViewById(R.id.mixedResultUnplaced);
        view.setText(getString(R.string.mixedUnplaced, summary.toString()));
        view.setVisibility(View.VISIBLE);
    }

    private View createPalletCard(PalletGroup group) {
        PalletLoad load = group.load;
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));
        card.setBackgroundResource(R.drawable.result_card);

        TextView title = baseText(18, R.color.textPrimary, true);
        title.setText(group.count == 1
                ? getString(R.string.mixedPalletResultTitle, group.startOrdinal,
                load.getPalletSpec().getName())
                : getString(R.string.mixedRepeatedPalletTitle, group.startOrdinal,
                group.startOrdinal + group.count - 1, group.count,
                load.getPalletSpec().getName()));
        card.addView(title);

        TextView meta = baseText(13, R.color.textSecondary, false);
        int sectionCount = load.isTowerBased() ? 1 : load.getTiers().size();
        int metaString = load.isTowerBased()
                ? R.string.mixedPalletTowerResultMeta : R.string.mixedPalletResultMeta;
        String metaText = getString(metaString, sectionCount, load.getTotalBoxes(),
                load.getTotalLoadedHeightMm(), loadUtilization(load));
        if (group.count > 1) {
            metaText += "\n" + getString(R.string.mixedRepeatedPalletMeta,
                    group.count, load.getTotalBoxes() * group.count);
        }
        meta.setText(metaText + "\n"
                + getString(R.string.mixedLoadStrategy, strategyLabel(load.getStrategyId())));
        LinearLayout.LayoutParams metaParams = blockParams(dp(12));
        card.addView(meta, metaParams);

        if (resultMode == MODE_3D) {
            Mixed3DView drawing = new Mixed3DView(this);
            drawing.setLoad(load.getPalletSpec(), load.getPlacements(), boxTypeIndices,
                    getString(R.string.mixed3dAccessibility, group.startOrdinal,
                            load.getTotalBoxes()));
            card.addView(drawing, blockParams(0));
        } else if (load.isTowerBased()) {
            card.addView(createTowerBlock(load), blockParams(dp(12)));
        } else {
            for (TierGroup tierGroup : groupAdjacentTiers(load.getTiers())) {
                card.addView(createTierBlock(load.getPalletSpec(), tierGroup),
                        blockParams(dp(12)));
            }
        }
        return card;
    }

    private View createTowerBlock(PalletLoad load) {
        LinearLayout block = new LinearLayout(this);
        block.setOrientation(LinearLayout.VERTICAL);
        block.setPadding(dp(12), dp(12), dp(12), dp(12));
        block.setBackgroundResource(R.drawable.selection_summary);

        TextView title = baseText(16, R.color.textPrimary, true);
        title.setText(R.string.mixedTowerPlanTitle);
        block.addView(title);
        TextView meta = baseText(12, R.color.textSecondary, false);
        meta.setText(R.string.mixedTowerPlanMeta);
        block.addView(meta, blockParams(dp(10)));

        LinkedHashMap<String, TowerColumn> columns = new LinkedHashMap<String, TowerColumn>();
        for (Placement placement : load.getPlacements()) {
            String key = placement.getBoxId() + ":" + placement.getXMm() + ":"
                    + placement.getYMm() + ":" + placement.getWidthMm() + ":"
                    + placement.getLengthMm();
            TowerColumn column = columns.get(key);
            if (column == null) {
                column = new TowerColumn(placement);
                columns.put(key, column);
            }
            column.count++;
        }

        ArrayList<MixedTierView.BoxCell> cells = new ArrayList<MixedTierView.BoxCell>();
        StringBuilder description = new StringBuilder();
        for (TowerColumn column : columns.values()) {
            Placement placement = column.base;
            int typeIndex = boxTypeIndices.containsKey(placement.getBoxId())
                    ? boxTypeIndices.get(placement.getBoxId()) : 0;
            String label = placement.getBoxId() + "×" + column.count;
            cells.add(new MixedTierView.BoxCell(label, typeIndex,
                    placement.getXMm(), placement.getYMm(), placement.getWidthMm(),
                    placement.getLengthMm()));
            if (description.length() > 0) {
                description.append(" · ");
            }
            description.append(label);
        }
        MixedTierView drawing = new MixedTierView(this);
        drawing.setPadding(dp(4), dp(4), dp(4), dp(4));
        drawing.setTier(load.getPalletSpec().getWidthMm(),
                load.getPalletSpec().getLengthMm(), cells,
                getString(R.string.mixedTowerAccessibility, description.toString()));
        block.addView(drawing, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        return block;
    }

    private View createTierBlock(PalletSpec pallet, TierGroup group) {
        TierPlan tier = group.tier;
        LinearLayout block = new LinearLayout(this);
        block.setOrientation(LinearLayout.VERTICAL);
        block.setPadding(dp(12), dp(12), dp(12), dp(12));
        block.setBackgroundResource(R.drawable.selection_summary);

        TextView title = baseText(16, R.color.textPrimary, true);
        title.setText(group.count == 1
                ? getString(R.string.mixedTierTitle, group.startOrdinal,
                tier.getNormalizedHeightMm())
                : getString(R.string.mixedRepeatedTierTitle, group.startOrdinal,
                group.startOrdinal + group.count - 1, tier.getNormalizedHeightMm(), group.count));
        block.addView(title);

        String mix = boxMix(tier);
        int occupancy = Math.round(tier.getUsedAreaMm2() * 100f
                / pallet.getFootprintAreaMm2());
        TextView meta = baseText(12, R.color.textSecondary, false);
        meta.setText(group.count == 1
                ? getString(R.string.mixedTierMeta, tier.getBoxCount(), occupancy, mix)
                : getString(R.string.mixedRepeatedTierMeta, tier.getBoxCount(),
                tier.getBoxCount() * group.count, occupancy, mix));
        block.addView(meta, blockParams(dp(10)));

        ArrayList<MixedTierView.BoxCell> cells = new ArrayList<MixedTierView.BoxCell>();
        for (Placement placement : tier.getPlacements()) {
            int typeIndex = boxTypeIndices.containsKey(placement.getBoxId())
                    ? boxTypeIndices.get(placement.getBoxId()) : 0;
            cells.add(new MixedTierView.BoxCell(placement.getBoxId(), typeIndex,
                    placement.getXMm(), placement.getYMm(), placement.getWidthMm(),
                    placement.getLengthMm()));
        }
        MixedTierView drawing = new MixedTierView(this);
        drawing.setPadding(dp(4), dp(4), dp(4), dp(4));
        drawing.setTier(pallet.getWidthMm(), pallet.getLengthMm(), cells,
                getString(R.string.mixedTierAccessibility, group.startOrdinal, mix));
        block.addView(drawing, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        return block;
    }

    private List<PalletGroup> groupAdjacentPalletLoads() {
        List<PalletGroup> groups = new ArrayList<PalletGroup>();
        PalletGroup current = null;
        for (int i = 0; i < planResult.getPalletLoads().size(); i++) {
            PalletLoad load = planResult.getPalletLoads().get(i);
            String signature = loadSignature(load);
            if (current != null && current.signature.equals(signature)) {
                current.count++;
            } else {
                current = new PalletGroup(load, i + 1, signature);
                groups.add(current);
            }
        }
        return groups;
    }

    private List<TierGroup> groupAdjacentTiers(List<TierPlan> tiers) {
        List<TierGroup> groups = new ArrayList<TierGroup>();
        TierGroup current = null;
        for (int i = 0; i < tiers.size(); i++) {
            TierPlan tier = tiers.get(i);
            String signature = tierSignature(tier);
            if (current != null && current.signature.equals(signature)) {
                current.count++;
            } else {
                current = new TierGroup(tier, i + 1, signature);
                groups.add(current);
            }
        }
        return groups;
    }

    private String loadSignature(PalletLoad load) {
        StringBuilder builder = new StringBuilder();
        builder.append(load.getPalletSpec().getId()).append('|')
                .append(load.getStrategyId()).append('|');
        for (Placement placement : load.getPlacements()) {
            appendPlacementSignature(builder, placement, true);
        }
        return builder.toString();
    }

    private String tierSignature(TierPlan tier) {
        StringBuilder builder = new StringBuilder();
        builder.append(tier.getNormalizedHeightMm()).append('|');
        for (Placement placement : tier.getPlacements()) {
            appendPlacementSignature(builder, placement, false);
        }
        return builder.toString();
    }

    private void appendPlacementSignature(StringBuilder builder, Placement placement,
                                          boolean includeHeight) {
        builder.append(placement.getBoxId()).append(':')
                .append(placement.getXMm()).append(':')
                .append(placement.getYMm()).append(':')
                .append(placement.getWidthMm()).append(':')
                .append(placement.getLengthMm());
        if (includeHeight) {
            builder.append(':').append(placement.getZMm()).append(':')
                    .append(placement.getHeightMm());
        }
        builder.append('|');
    }

    private int averageOccupancy(PalletLoad load) {
        if (load.getTiers().isEmpty()) {
            return 0;
        }
        int total = 0;
        for (TierPlan tier : load.getTiers()) {
            total += Math.round(tier.getUsedAreaMm2() * 100f
                    / load.getPalletSpec().getFootprintAreaMm2());
        }
        return total / load.getTiers().size();
    }

    private int loadUtilization(PalletLoad load) {
        if (!load.isTowerBased() && !load.getTiers().isEmpty()) {
            return averageOccupancy(load);
        }
        long cargoHeight = Math.max(1,
                load.getTotalLoadedHeightMm() - load.getPalletSpec().getHeightMm());
        long capacity = (long) load.getPalletSpec().getWidthMm()
                * load.getPalletSpec().getLengthMm() * cargoHeight;
        return capacity <= 0 ? 0
                : (int) Math.min(100, Math.round(load.getTotalBoxVolumeMm3() * 100d / capacity));
    }

    private String boxMix(TierPlan tier) {
        LinkedHashMap<String, Integer> counts = new LinkedHashMap<String, Integer>();
        for (Placement placement : tier.getPlacements()) {
            Integer count = counts.get(placement.getBoxId());
            counts.put(placement.getBoxId(), count == null ? 1 : count + 1);
        }
        StringBuilder summary = new StringBuilder();
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            if (summary.length() > 0) {
                summary.append(" · ");
            }
            summary.append(entry.getKey()).append(" x ").append(entry.getValue());
        }
        return summary.toString();
    }

    private String strategyLabel(String strategyId) {
        if ("layered-equal-height".equals(strategyId)) {
            return getString(R.string.mixedStrategyLayered);
        }
        if ("tower-volume-first".equals(strategyId)) {
            return getString(R.string.mixedStrategyTowerVolume);
        }
        if ("tower-footprint-waste-first".equals(strategyId)) {
            return getString(R.string.mixedStrategyTowerWaste);
        }
        if ("tower-height-balanced".equals(strategyId)) {
            return getString(R.string.mixedStrategyTowerBalanced);
        }
        if ("hybrid-best-per-load".equals(strategyId)) {
            return getString(R.string.mixedStrategyHybrid);
        }
        return strategyId;
    }

    private TextView createBadge(String value, int typeIndex) {
        TextView badge = baseText(13, R.color.headerText, true);
        badge.setText(value);
        badge.setGravity(Gravity.CENTER);
        GradientDrawable background = new GradientDrawable();
        background.setColor(BOX_COLORS[Math.floorMod(typeIndex, BOX_COLORS.length)]);
        background.setStroke(dp(2), getResources().getColor(R.color.textPrimary));
        background.setCornerRadius(dp(4));
        badge.setBackground(background);
        badge.setLayoutParams(new LinearLayout.LayoutParams(dp(42), dp(38)));
        return badge;
    }

    private TextView baseText(int sizeSp, int colorRes, boolean bold) {
        TextView view = new TextView(this);
        view.setTextColor(getResources().getColor(colorRes));
        view.setTextSize(sizeSp);
        view.setTypeface(Typeface.DEFAULT, bold ? Typeface.BOLD : Typeface.NORMAL);
        view.setLineSpacing(0, 1.08f);
        return view;
    }

    private LinearLayout.LayoutParams blockParams(int bottomMargin) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, bottomMargin);
        return params;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    public void showMixed2d(View view) {
        setResultMode(MODE_2D);
    }

    public void showMixed3d(View view) {
        setResultMode(MODE_3D);
    }

    public void showMixedContainer(View view) {
        ContainerResultRenderer.chooseContainerType(this, selectedContainerTypeName,
                new ContainerResultRenderer.OnContainerTypeSelected() {
                    @Override
                    public void onContainerTypeSelected(ContainerLoadCalculator.ContainerType type) {
                        selectedContainerTypeName = type.name;
                        resultMode = MODE_CONTAINER;
                        renderPalletResults();
                        updateModeVisibility();
                        updateModeButtons();
                    }
                });
    }

    private void setResultMode(int mode) {
        if (resultMode == mode) {
            return;
        }
        resultMode = mode;
        renderPalletResults();
        updateModeVisibility();
        updateModeButtons();
    }

    private void updateModeVisibility() {
        boolean containerMode = resultMode == MODE_CONTAINER;
        findViewById(R.id.mixedLegendCard).setVisibility(
                containerMode ? View.GONE : View.VISIBLE);
        findViewById(R.id.mixedResultUnplaced).setVisibility(
                containerMode || planResult.getLeftovers().isEmpty()
                        ? View.GONE : View.VISIBLE);
        findViewById(R.id.mixedHeuristicNote).setVisibility(
                containerMode ? View.GONE : View.VISIBLE);
    }

    private void updateModeButtons() {
        Button mode2d = (Button) findViewById(R.id.mixedMode2d);
        Button mode3d = (Button) findViewById(R.id.mixedMode3d);
        Button modeContainer = (Button) findViewById(R.id.mixedModeContainer);
        updateModeButton(mode2d, resultMode == MODE_2D);
        updateModeButton(mode3d, resultMode == MODE_3D);
        updateModeButton(modeContainer, resultMode == MODE_CONTAINER);
    }

    private void updateModeButton(Button button, boolean selected) {
        button.setBackgroundResource(selected
                ? R.drawable.mode_segment_selected : R.drawable.mode_segment_unselected);
        button.setTextColor(getResources().getColor(
                selected ? R.color.headerText : R.color.textSecondary));
        button.setSelected(selected);
    }

    public void btnBack(View view) {
        finish();
    }

    private static final class TowerColumn {
        final Placement base;
        int count;

        TowerColumn(Placement base) {
            this.base = base;
        }
    }

    private static final class PalletGroup {
        final PalletLoad load;
        final int startOrdinal;
        final String signature;
        int count = 1;

        PalletGroup(PalletLoad load, int startOrdinal, String signature) {
            this.load = load;
            this.startOrdinal = startOrdinal;
            this.signature = signature;
        }
    }

    private static final class TierGroup {
        final TierPlan tier;
        final int startOrdinal;
        final String signature;
        int count = 1;

        TierGroup(TierPlan tier, int startOrdinal, String signature) {
            this.tier = tier;
            this.startOrdinal = startOrdinal;
            this.signature = signature;
        }
    }
}
