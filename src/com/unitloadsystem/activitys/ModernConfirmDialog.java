package com.unitloadsystem.activitys;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

public final class ModernConfirmDialog {
    public interface OnConfirmed {
        void onConfirmed();
    }

    private ModernConfirmDialog() {
    }

    public static void show(final Activity activity, String message, int confirmLabelResId,
                            final OnConfirmed listener) {
        final Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.modern_confirm_dialog);
        dialog.setCanceledOnTouchOutside(true);

        ((TextView) dialog.findViewById(R.id.confirmDialogMessage)).setText(message);
        ((ImageButton) dialog.findViewById(R.id.confirmDialogClose)).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        dialog.dismiss();
                    }
                });
        ((Button) dialog.findViewById(R.id.confirmDialogCancel)).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        dialog.dismiss();
                    }
                });
        Button confirm = (Button) dialog.findViewById(R.id.confirmDialogConfirm);
        confirm.setText(confirmLabelResId);
        confirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
                listener.onConfirmed();
            }
        });

        dialog.show();
        Window window = dialog.getWindow();
        if (window == null) {
            return;
        }
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        WindowManager.LayoutParams params = new WindowManager.LayoutParams();
        params.copyFrom(window.getAttributes());
        params.dimAmount = 0.48f;
        params.width = Math.min(activity.getResources().getDisplayMetrics().widthPixels
                - dp(activity, 32), dp(activity, 520));
        params.height = WindowManager.LayoutParams.WRAP_CONTENT;
        window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        window.setAttributes(params);
    }

    private static int dp(Activity activity, int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }
}
