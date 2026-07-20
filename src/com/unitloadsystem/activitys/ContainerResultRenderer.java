package com.unitloadsystem.activitys;

import android.app.Activity;
import android.content.Context;
import android.graphics.Typeface;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.unitloadsystem.container.ContainerLoadCalculator;
import com.unitloadsystem.container.ContainerLoadPlanner;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public final class ContainerResultRenderer {
    private ContainerResultRenderer() {
    }

    public interface OnContainerTypeSelected {
        void onContainerTypeSelected(ContainerLoadCalculator.ContainerType type);
    }

    public static void chooseContainerType(Activity activity, String selectedTypeName,
                                           final OnContainerTypeSelected listener) {
        final List<ContainerLoadCalculator.ContainerType> types =
                ContainerLoadCalculator.supportedTypes();
        String[] names = new String[types.size()];
        int selectedIndex = -1;
        for (int i = 0; i < types.size(); i++) {
            ContainerLoadCalculator.ContainerType type = types.get(i);
            names[i] = type.name + " Dry";
            if (type.name.equals(selectedTypeName)) {
                selectedIndex = i;
            }
        }
        ModernChoiceDialog.show(activity, R.string.chooseContainerType, names, selectedIndex,
                new ModernChoiceDialog.OnChoiceSelected() {
                    @Override
                    public void onChoiceSelected(int index) {
                        listener.onContainerTypeSelected(types.get(index));
                    }
                });
    }

    public static ContainerLoadCalculator.ContainerType findContainerType(String name) {
        for (ContainerLoadCalculator.ContainerType type
                : ContainerLoadCalculator.supportedTypes()) {
            if (type.name.equals(name)) {
                return type;
            }
        }
        return null;
    }

    public static void render(Context context, LinearLayout parent, List<Section> sections,
                              ContainerLoadCalculator.ContainerType type) {
        parent.removeAllViews();
        parent.addView(text(context, context.getString(R.string.containerPlanReference),
                12, R.color.textSecondary, false), params(dp(context, 14)));

        ContainerLoadPlanner planner = new ContainerLoadPlanner();
        for (int sectionIndex = 0; sectionIndex < sections.size(); sectionIndex++) {
            Section section = sections.get(sectionIndex);
            ContainerLoadPlanner.Plan single = planner.plan(type, section.loads, false);
            ContainerLoadPlanner.Plan stacked = planner.plan(type, section.loads, true);
            parent.addView(createTypeCard(context, type, single, stacked),
                    params(dp(context, sectionIndex == sections.size() - 1 ? 12 : 18)));
        }
    }

    private static View createTypeCard(Context context,
                                       ContainerLoadCalculator.ContainerType type,
                                       ContainerLoadPlanner.Plan single,
                                       ContainerLoadPlanner.Plan stacked) {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(context, 16), dp(context, 16),
                dp(context, 16), dp(context, 16));
        card.setBackgroundResource(R.drawable.result_card);

        card.addView(text(context, context.getString(R.string.containerPlanTitle, type.name),
                17, R.color.textPrimary, true), params(dp(context, 4)));
        card.addView(text(context, context.getString(R.string.containerPlanDimensions,
                        number(type.lengthMm), number(type.widthMm), number(type.heightMm),
                        number(type.doorHeightMm)),
                11, R.color.textTertiary, false), params(dp(context, 10)));
        card.addView(text(context, context.getString(R.string.containerPlanClearance,
                        number(type.getUsableLengthMm()), number(type.getUsableWidthMm()),
                        number(type.getUsableHeightMm())),
                11, R.color.textTertiary, false), params(dp(context, 10)));

        if (!single.getUnplaced().isEmpty()) {
            TextView warning = text(context,
                    context.getString(R.string.containerPlanUnplaced,
                            single.getUnplaced().size()),
                    12, R.color.dangerColor, true);
            warning.setBackgroundResource(R.drawable.danger_button);
            warning.setPadding(dp(context, 10), dp(context, 9),
                    dp(context, 10), dp(context, 9));
            card.addView(warning, params(dp(context, 10)));
        }

        addPlanSection(context, card, type, single,
                R.string.containerPlanSingleTitle,
                R.string.containerPlanSingleDescription,
                R.string.containerPlanSingleDiagram);
        if (stacked.getContainers().size() < single.getContainers().size()) {
            addPlanSection(context, card, type, stacked,
                    R.string.containerPlanStackedTitle,
                    R.string.containerPlanStackedDescription,
                    R.string.containerPlanStackedDiagram);
        } else {
            card.addView(text(context, context.getString(R.string.containerPlanStackedUnavailable),
                    11, R.color.textTertiary, false), params(0));
        }
        if (single.getContainers().isEmpty()) {
            card.addView(text(context, context.getString(R.string.containerPlanEmpty),
                    13, R.color.textSecondary, false), params(0));
        }
        return card;
    }

    private static void addPlanSection(Context context, LinearLayout card,
                                       ContainerLoadCalculator.ContainerType type,
                                       ContainerLoadPlanner.Plan plan,
                                       int titleRes, int descriptionRes, int diagramTitleRes) {
        TextView title = text(context, context.getString(titleRes), 15,
                R.color.textPrimary, true);
        title.setPadding(0, dp(context, 10), 0, dp(context, 2));
        card.addView(title, params(dp(context, 2)));
        card.addView(text(context, context.getString(descriptionRes), 11,
                R.color.textSecondary, false), params(dp(context, 7)));

        TextView summary = text(context, context.getString(R.string.containerPlanTierSummary,
                        plan.getPlacedLoadCount(), plan.getContainers().size()),
                14, R.color.primaryColor, true);
        summary.setBackgroundResource(R.drawable.selection_summary);
        summary.setPadding(dp(context, 12), dp(context, 9), dp(context, 12), dp(context, 9));
        card.addView(summary, params(dp(context, 8)));
        card.addView(text(context, context.getString(diagramTitleRes),
                11, R.color.textTertiary, true), params(dp(context, 6)));

        for (int i = 0; i < plan.getContainers().size(); i++) {
            ContainerLoadPlanner.ContainerPlan container = plan.getContainers().get(i);
            String label = context.getString(R.string.containerPlanItem,
                    container.getNumber(), container.getLoadCount());
            card.addView(text(context, label, 13, R.color.textPrimary, true), params(dp(context, 2)));
            ContainerPlanView view = new ContainerPlanView(context);
            view.setPlan(type, container, label);
            card.addView(view, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));
        }
    }

    private static TextView text(Context context, String value, float size,
                                 int color, boolean bold) {
        TextView view = new TextView(context);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(context.getResources().getColor(color));
        view.setTypeface(Typeface.DEFAULT, bold ? Typeface.BOLD : Typeface.NORMAL);
        view.setLineSpacing(0, 1.08f);
        return view;
    }

    private static LinearLayout.LayoutParams params(int bottomMargin) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, bottomMargin);
        return params;
    }

    private static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    private static String number(int value) {
        return new DecimalFormat("#,##0").format(value);
    }

    public static final class Section {
        final String title;
        final List<ContainerLoadPlanner.UnitLoad> loads;

        public Section(String title, List<ContainerLoadPlanner.UnitLoad> loads) {
            this.title = title;
            this.loads = new ArrayList<ContainerLoadPlanner.UnitLoad>(loads);
        }
    }
}
