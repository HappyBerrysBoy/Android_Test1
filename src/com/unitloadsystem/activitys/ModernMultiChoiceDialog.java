package com.unitloadsystem.activitys;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public final class ModernMultiChoiceDialog {
    public interface OnSelectionApplied {
        void onSelectionApplied(boolean[] selected);
    }

    private ModernMultiChoiceDialog() {
    }

    public static void show(final Activity activity, int titleResId, String[] items,
                            boolean[] initialSelection, int minimumSelection,
                            final OnSelectionApplied listener) {
        final boolean[] selected = new boolean[items.length];
        System.arraycopy(initialSelection, 0, selected, 0,
                Math.min(initialSelection.length, selected.length));

        final Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.modern_multi_choice_dialog);
        dialog.setCanceledOnTouchOutside(true);
        ((TextView) dialog.findViewById(R.id.multiChoiceDialogTitle)).setText(titleResId);

        final TextView count = (TextView) dialog.findViewById(R.id.multiChoiceCount);
        final LinearLayout list = (LinearLayout) dialog.findViewById(R.id.multiChoiceDialogList);
        final LayoutInflater inflater = activity.getLayoutInflater();
        final TextView[] rows = new TextView[items.length];

        View.OnClickListener rowClick = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int index = (Integer) view.getTag();
                selected[index] = !selected[index];
                updateRow(activity, rows[index], items[index], selected[index]);
                count.setText(activity.getString(R.string.mixedSelectedCount, countSelected(selected)));
            }
        };

        for (int i = 0; i < items.length; i++) {
            TextView row = (TextView) inflater.inflate(
                    R.layout.modern_choice_dialog_item, list, false);
            row.setTag(i);
            row.setOnClickListener(rowClick);
            updateRow(activity, row, items[i], selected[i]);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 0, 0, dp(activity, 8));
            list.addView(row, params);
            rows[i] = row;
        }

        count.setText(activity.getString(R.string.mixedSelectedCount, countSelected(selected)));
        ((ImageButton) dialog.findViewById(R.id.multiChoiceDialogClose)).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        dialog.dismiss();
                    }
                });
        ((Button) dialog.findViewById(R.id.multiChoiceDone)).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (countSelected(selected) < minimumSelection) {
                            Toast.makeText(activity,
                                    activity.getString(R.string.mixedSelectAtLeast, minimumSelection),
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }
                        dialog.dismiss();
                        listener.onSelectionApplied(selected.clone());
                    }
                });

        dialog.show();
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            WindowManager.LayoutParams params = new WindowManager.LayoutParams();
            params.copyFrom(window.getAttributes());
            params.width = Math.min(activity.getResources().getDisplayMetrics().widthPixels
                    - dp(activity, 32), dp(activity, 560));
            params.height = Math.min(activity.getResources().getDisplayMetrics().heightPixels
                    - dp(activity, 72), dp(activity, 720));
            window.setAttributes(params);
        }
    }

    private static void updateRow(Activity activity, TextView row, String text, boolean selected) {
        row.setSelected(selected);
        row.setText(selected ? activity.getString(R.string.selectedListItem, text) : text);
        row.setTextColor(activity.getResources().getColor(
                selected ? R.color.headerText : R.color.textPrimary));
        row.setContentDescription(selected
                ? activity.getString(R.string.selectedListItemDescription, text) : text);
    }

    private static int countSelected(boolean[] selected) {
        int count = 0;
        for (boolean value : selected) {
            if (value) {
                count++;
            }
        }
        return count;
    }

    private static int dp(Activity activity, int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }
}
