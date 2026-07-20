package com.unitloadsystem.activitys;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.unitloadsystem.common.CommonFunction;
import com.unitloadsystem.db.BoxSpec;
import com.unitloadsystem.db.MySQLiteOpenHelper;
import com.unitloadsystem.db.Pallet;
import com.unitloadsystem.mixed.BoxDemand;
import com.unitloadsystem.mixed.MixedLoadOptimizer;
import com.unitloadsystem.mixed.PalletSpec;
import com.unitloadsystem.mixed.PlanResult;

import java.util.ArrayList;
import java.util.Locale;

public class MixedCalculationActivity extends LocalizedActivity {
    private static final int REQUEST_BOX_QUANTITY_BASE = 5000;
    private static final int REQUEST_BOX_LAYERS_BASE = 6000;

    private final ArrayList<Pallet> pallets = new ArrayList<Pallet>();
    private final ArrayList<BoxSpec> boxes = new ArrayList<BoxSpec>();
    private boolean[] selectedPallets;
    private boolean[] selectedBoxes;
    private int[] boxQuantities;
    private int[] boxLayers;
    private MySQLiteOpenHelper helper;
    private boolean calculating;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.mixedcalculation_layout);
        helper = new MySQLiteOpenHelper(this, "pallet.db", null, MySQLiteOpenHelper.DB_VERSION);
        loadSavedSpecs();
        selectedPallets = new boolean[pallets.size()];
        selectedBoxes = new boolean[boxes.size()];
        boxQuantities = new int[boxes.size()];
        boxLayers = new int[boxes.size()];
        for (int i = 0; i < boxQuantities.length; i++) {
            boxQuantities[i] = 100;
            boxLayers[i] = 5;
        }
        renderSelections();
    }

    public void chooseMixedPallets(View view) {
        String[] rows = new String[pallets.size()];
        for (int i = 0; i < pallets.size(); i++) {
            Pallet pallet = pallets.get(i);
            rows[i] = getString(R.string.palletChoiceRow, pallet.getName(), pallet.getWidth(),
                    pallet.getLength(), pallet.getHeight(), pallet.getUnit());
        }
        ModernMultiChoiceDialog.show(this, R.string.mixedChoosePallets, rows,
                selectedPallets, 1, selected -> {
                    selectedPallets = selected;
                    renderSelections();
                });
    }

    public void chooseMixedBoxes(View view) {
        String[] rows = new String[boxes.size()];
        for (int i = 0; i < boxes.size(); i++) {
            BoxSpec box = boxes.get(i);
            rows[i] = getString(R.string.boxChoiceRow, box.getName(), format(box.getLength()),
                    format(box.getWidth()), format(box.getHeight()), box.getUnit(),
                    format(box.getWeight()), box.getWeightUnit());
        }
        ModernMultiChoiceDialog.show(this, R.string.mixedChooseBoxes, rows,
                selectedBoxes, 1, selected -> {
                    selectedBoxes = selected;
                    renderSelections();
                });
    }

    private void editBoxQuantity(int boxIndex) {
        Intent intent = new Intent(this, KeyPadActivity.class);
        intent.putExtra("BtnID", -1);
        intent.putExtra("TextIn", String.valueOf(boxQuantities[boxIndex]));
        intent.putExtra("InputUnit", getString(R.string.mixedBoxQuantityUnit));
        intent.putExtra("InputLabel", getString(R.string.mixedBoxQuantityLabel,
                boxCode(boxIndex) + " " + boxes.get(boxIndex).getName()));
        startActivityForResult(intent, REQUEST_BOX_QUANTITY_BASE + boxIndex);
    }

    private void editBoxLayers(int boxIndex) {
        Intent intent = new Intent(this, KeyPadActivity.class);
        intent.putExtra("BtnID", -1);
        intent.putExtra("TextIn", String.valueOf(boxLayers[boxIndex]));
        intent.putExtra("InputUnit", getString(R.string.mixedBoxLayersUnit));
        intent.putExtra("InputLabel", getString(R.string.mixedBoxLayersLabel,
                boxCode(boxIndex) + " " + boxes.get(boxIndex).getName()));
        startActivityForResult(intent, REQUEST_BOX_LAYERS_BASE + boxIndex);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null) {
            return;
        }
        String value = data.getStringExtra("Value");
        int boxIndex = requestCode - REQUEST_BOX_QUANTITY_BASE;
        if (boxIndex >= 0 && boxIndex < boxQuantities.length) {
            try {
                boxQuantities[boxIndex] = Math.max(1, (int) Float.parseFloat(value));
                renderSelections();
            } catch (NumberFormatException ignored) {
                Toast.makeText(this, R.string.invalidBoxSpec, Toast.LENGTH_SHORT).show();
            }
            return;
        }
        boxIndex = requestCode - REQUEST_BOX_LAYERS_BASE;
        if (boxIndex >= 0 && boxIndex < boxLayers.length) {
            try {
                boxLayers[boxIndex] = Math.max(1, (int) Float.parseFloat(value));
                renderSelections();
            } catch (NumberFormatException ignored) {
                Toast.makeText(this, R.string.invalidBoxSpec, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void renderSelections() {
        LinearLayout palletContainer = (LinearLayout) findViewById(R.id.mixedPalletsContainer);
        LinearLayout boxContainer = (LinearLayout) findViewById(R.id.mixedBoxesContainer);
        palletContainer.removeAllViews();
        boxContainer.removeAllViews();

        int palletOrdinal = 0;
        int boxOrdinal = 0;
        int totalQuantity = 0;
        for (int i = 0; i < selectedPallets.length; i++) {
            if (selectedPallets[i]) {
                palletContainer.addView(createPalletRow(++palletOrdinal, pallets.get(i)), rowParams());
            }
        }
        for (int i = 0; i < selectedBoxes.length; i++) {
            if (selectedBoxes[i]) {
                boxContainer.addView(createBoxRow(++boxOrdinal, i, boxes.get(i)), rowParams());
                totalQuantity += boxQuantities[i];
            }
        }
        findViewById(R.id.mixedEmptyPallets).setVisibility(
                palletOrdinal == 0 ? View.VISIBLE : View.GONE);
        findViewById(R.id.mixedEmptyBoxes).setVisibility(
                boxOrdinal == 0 ? View.VISIBLE : View.GONE);
        ((TextView) findViewById(R.id.mixedSelectionSummary)).setText(
                getString(R.string.mixedSummaryFormat, palletOrdinal, boxOrdinal, totalQuantity));
    }

    private View createPalletRow(int ordinal, Pallet pallet) {
        LinearLayout row = baseRow();
        row.addView(createBadge("P" + ordinal));
        TextView text = createDescription();
        text.setText(getString(R.string.palletListRow, pallet.getName(), pallet.getWidth(),
                pallet.getLength(), pallet.getHeight(), pallet.getUnit()));
        row.addView(text, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        return row;
    }

    private View createBoxRow(int ordinal, final int boxIndex, BoxSpec box) {
        LinearLayout row = baseRow();
        row.addView(createBadge("B" + ordinal));
        TextView text = createDescription();
        text.setText(box.getName() + "\n" + format(box.getLength()) + " x "
                + format(box.getWidth()) + " x " + format(box.getHeight()) + " "
                + box.getUnit() + " · " + format(box.getWeight()) + " "
                + box.getWeightUnit());
        row.addView(text, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        row.addView(createBoxInputButton(String.format(Locale.getDefault(), "%,d\n%s",
                        boxQuantities[boxIndex], getString(R.string.mixedBoxQuantityUnit)),
                view -> editBoxQuantity(boxIndex)));
        row.addView(createBoxInputButton(String.format(Locale.getDefault(), "%d\n%s",
                        boxLayers[boxIndex], getString(R.string.mixedBoxLayersUnit)),
                view -> editBoxLayers(boxIndex)));
        return row;
    }

    private Button createBoxInputButton(String value, View.OnClickListener listener) {
        Button button = new Button(this);
        button.setMinWidth(0);
        button.setMinHeight(0);
        button.setText(value);
        button.setTextColor(getResources().getColor(R.color.primaryColor));
        button.setTextSize(14);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setBackgroundResource(R.drawable.secondary_button);
        button.setOnClickListener(listener);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(68), dp(58));
        params.setMargins(dp(6), 0, 0, 0);
        button.setLayoutParams(params);
        return button;
    }

    private LinearLayout baseRow() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(12), dp(11), dp(12), dp(11));
        row.setBackgroundResource(R.drawable.dashboard_action);
        return row;
    }

    private TextView createBadge(String value) {
        TextView badge = new TextView(this);
        badge.setText(value);
        badge.setTextColor(getResources().getColor(R.color.headerText));
        badge.setTextSize(14);
        badge.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        badge.setGravity(Gravity.CENTER);
        badge.setBackgroundResource(R.drawable.mixed_badge);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(42), dp(42));
        params.setMargins(0, 0, dp(12), 0);
        badge.setLayoutParams(params);
        return badge;
    }

    private TextView createDescription() {
        TextView text = new TextView(this);
        text.setTextColor(getResources().getColor(R.color.textPrimary));
        text.setTextSize(14);
        text.setLineSpacing(0, 1.08f);
        return text;
    }

    private LinearLayout.LayoutParams rowParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, dp(8));
        return params;
    }

    private void loadSavedSpecs() {
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor palletCursor = db.query("palletdb", null, null, null, null, null, "name");
        try {
            while (palletCursor.moveToNext()) {
                Pallet pallet = new Pallet();
                pallet.setName(palletCursor.getString(palletCursor.getColumnIndexOrThrow("name")));
                pallet.setWidth(palletCursor.getInt(palletCursor.getColumnIndexOrThrow("width")));
                pallet.setLength(palletCursor.getInt(palletCursor.getColumnIndexOrThrow("height")));
                pallet.setHeight(palletCursor.getInt(palletCursor.getColumnIndexOrThrow("pallet_height")));
                pallet.setUnit(palletCursor.getString(palletCursor.getColumnIndexOrThrow("unit")));
                pallets.add(pallet);
            }
        } finally {
            palletCursor.close();
        }

        Cursor boxCursor = db.query("boxdb", null, null, null, null, null, "name");
        try {
            while (boxCursor.moveToNext()) {
                BoxSpec box = new BoxSpec();
                box.setName(boxCursor.getString(boxCursor.getColumnIndexOrThrow("name")));
                box.setLength(boxCursor.getFloat(boxCursor.getColumnIndexOrThrow("box_length")));
                box.setWidth(boxCursor.getFloat(boxCursor.getColumnIndexOrThrow("box_width")));
                box.setHeight(boxCursor.getFloat(boxCursor.getColumnIndexOrThrow("box_height")));
                box.setUnit(boxCursor.getString(boxCursor.getColumnIndexOrThrow("unit")));
                box.setWeight(boxCursor.getFloat(boxCursor.getColumnIndexOrThrow("weight")));
                box.setWeightUnit(boxCursor.getString(boxCursor.getColumnIndexOrThrow("weight_unit")));
                boxes.add(box);
            }
        } finally {
            boxCursor.close();
        }
    }

    public void calculateMixedLoad(View view) {
        if (calculating) {
            return;
        }
        if (countSelected(selectedPallets) < 1 || countSelected(selectedBoxes) < 1) {
            Toast.makeText(this, R.string.mixedInvalidSelection, Toast.LENGTH_LONG).show();
            return;
        }
        try {
            ArrayList<PalletSpec> palletSpecs = buildPalletSpecs();
            ArrayList<BoxDemand> demands = buildBoxDemands();
            int tallestPallet = 0;
            int tallestBoxStack = 0;
            for (PalletSpec palletSpec : palletSpecs) {
                tallestPallet = Math.max(tallestPallet, palletSpec.getHeightMm());
            }
            for (BoxDemand demand : demands) {
                tallestBoxStack = Math.max(tallestBoxStack, demand.getMaxStackHeightMm());
            }
            int maxHeight = tallestPallet + tallestBoxStack;
            if (tallestBoxStack <= 0) {
                Toast.makeText(this, R.string.mixedInvalidHeight, Toast.LENGTH_LONG).show();
                return;
            }

            setCalculating(true);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        final PlanResult result = new MixedLoadOptimizer().optimize(
                                palletSpecs, demands, maxHeight);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (isFinishing() || isDestroyed()) {
                                    return;
                                }
                                setCalculating(false);
                                if (result.getPalletLoads().isEmpty()) {
                                    Toast.makeText(MixedCalculationActivity.this,
                                            R.string.mixedNoPlan, Toast.LENGTH_LONG).show();
                                    return;
                                }
                                Intent intent = new Intent(MixedCalculationActivity.this,
                                        MixedCalculationResultActivity.class);
                                intent.putExtra(MixedCalculationResultActivity.EXTRA_PLAN_RESULT,
                                        result);
                                intent.putExtra(MixedCalculationResultActivity.EXTRA_BOX_DEMANDS,
                                        demands);
                                startActivity(intent);
                            }
                        });
                    } catch (final IllegalArgumentException exception) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (isFinishing() || isDestroyed()) {
                                    return;
                                }
                                setCalculating(false);
                                Toast.makeText(MixedCalculationActivity.this,
                                        R.string.mixedInvalidSelection, Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                }
            }, "mixed-load-optimizer").start();
        } catch (IllegalArgumentException exception) {
            Toast.makeText(this, R.string.mixedInvalidSelection, Toast.LENGTH_LONG).show();
        }
    }

    private void setCalculating(boolean value) {
        calculating = value;
        Button button = findButton(R.id.mixedCalculateButton);
        button.setEnabled(!value);
        button.setAlpha(value ? 0.72f : 1f);
        button.setText(value ? R.string.mixedCalculating : R.string.mixedRunCalculation);
    }

    private ArrayList<PalletSpec> buildPalletSpecs() {
        ArrayList<PalletSpec> result = new ArrayList<PalletSpec>();
        int ordinal = 0;
        for (int i = 0; i < selectedPallets.length; i++) {
            if (!selectedPallets[i]) {
                continue;
            }
            Pallet pallet = pallets.get(i);
            result.add(new PalletSpec("P" + (++ordinal), pallet.getName(),
                    toMm(pallet.getWidth(), pallet.getUnit()),
                    toMm(pallet.getLength(), pallet.getUnit()),
                    toMm(pallet.getHeight(), pallet.getUnit())));
        }
        return result;
    }

    private ArrayList<BoxDemand> buildBoxDemands() {
        ArrayList<BoxDemand> result = new ArrayList<BoxDemand>();
        int ordinal = 0;
        for (int i = 0; i < selectedBoxes.length; i++) {
            if (!selectedBoxes[i]) {
                continue;
            }
            BoxSpec box = boxes.get(i);
            result.add(new BoxDemand("B" + (++ordinal), box.getName(),
                    toMm(box.getWidth(), box.getUnit()),
                    toMm(box.getLength(), box.getUnit()),
                    toMm(box.getHeight(), box.getUnit()),
                    toKg(box.getWeight(), box.getWeightUnit()), boxQuantities[i],
                    boxLayers[i]));
        }
        return result;
    }

    private int toMm(float value, String unit) {
        return Math.max(1, Math.round(CommonFunction.changeToMM(value, unit)));
    }

    private double toKg(float value, String unit) {
        return "lb".equalsIgnoreCase(unit) ? value * 0.45359237d : value;
    }

    private int countSelected(boolean[] values) {
        int count = 0;
        for (boolean value : values) {
            if (value) {
                count++;
            }
        }
        return count;
    }

    private String boxCode(int boxIndex) {
        int ordinal = 0;
        for (int i = 0; i <= boxIndex; i++) {
            if (selectedBoxes[i]) {
                ordinal++;
            }
        }
        return "B" + ordinal;
    }

    private String format(float value) {
        return value == Math.round(value)
                ? String.valueOf(Math.round(value))
                : String.format(Locale.getDefault(), "%.1f", value);
    }

    private Button findButton(int id) {
        return (Button) findViewById(id);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    public void btnBack(View view) {
        finish();
    }
}
