package com.unitloadsystem.activitys;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

public final class ModernChoiceDialog {
    public interface OnChoiceSelected {
        void onChoiceSelected(int index);
    }

    private ModernChoiceDialog() {
    }

    public static void show(final Activity activity, int titleResId, String[] items,
                            int selectedIndex, final OnChoiceSelected listener) {
        final Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.modern_choice_dialog);
        dialog.setCanceledOnTouchOutside(true);

        ((TextView) dialog.findViewById(R.id.choiceDialogTitle)).setText(titleResId);
        ((ImageButton) dialog.findViewById(R.id.choiceDialogClose)).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        dialog.dismiss();
                    }
                });

        LinearLayout list = (LinearLayout) dialog.findViewById(R.id.choiceDialogList);
        LayoutInflater inflater = activity.getLayoutInflater();
        for (int i = 0; i < items.length; i++) {
            final int index = i;
            TextView row = (TextView) inflater.inflate(
                    R.layout.modern_choice_dialog_item, list, false);
            boolean selected = i == selectedIndex;
            row.setSelected(selected);
            row.setText(selected
                    ? activity.getString(R.string.selectedListItem, items[i]) : items[i]);
            row.setTextColor(activity.getResources().getColor(
                    selected ? R.color.headerText : R.color.textPrimary));
            row.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    dialog.dismiss();
                    listener.onChoiceSelected(index);
                }
            });

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 0, 0, dp(activity, 8));
            list.addView(row, params);
        }

        dialog.show();
        final Window window = dialog.getWindow();
        if (window == null) {
            return;
        }
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        WindowManager.LayoutParams attributes = window.getAttributes();
        attributes.dimAmount = 0.48f;
        window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        int width = Math.round(activity.getResources().getDisplayMetrics().widthPixels * 0.9f);
        window.setLayout(width, WindowManager.LayoutParams.WRAP_CONTENT);

        final View content = dialog.findViewById(R.id.choiceDialogRoot);
        content.post(new Runnable() {
            @Override
            public void run() {
                int maxHeight = Math.round(
                        activity.getResources().getDisplayMetrics().heightPixels * 0.72f);
                if (content.getHeight() > maxHeight) {
                    window.setLayout(window.getAttributes().width, maxHeight);
                }
            }
        });
    }

    private static int dp(Activity activity, int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }
}
