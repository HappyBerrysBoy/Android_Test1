package com.unitloadsystem.activitys;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.content.Intent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.unitloadsystem.db.MySQLiteOpenHelper;
import com.unitloadsystem.db.Pallet;

import java.util.ArrayList;

public class PalletManagerActivity extends LocalizedActivity {
    private MySQLiteOpenHelper helper;
    private ArrayList<Pallet> pallets = new ArrayList<Pallet>();
    private String selectedName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.palletmanager_layout);
        helper = new MySQLiteOpenHelper(getApplicationContext(), "pallet.db", null, MySQLiteOpenHelper.DB_VERSION);
        clearForm(null);
        refreshList();
    }

    public void btnDimenPallet(final View view) {
        final String[] dimensions = getResources().getStringArray(R.array.dimensions);
        final Button target = (Button) view;
        int selectedIndex = -1;
        for (int i = 0; i < dimensions.length; i++) {
            if (dimensions[i].equals(target.getText().toString())) {
                selectedIndex = i;
                break;
            }
        }
        ModernChoiceDialog.show(this, R.string.chooseLengthUnit, dimensions, selectedIndex,
                new ModernChoiceDialog.OnChoiceSelected() {
                    @Override
                    public void onChoiceSelected(int index) {
                        target.setText(dimensions[index]);
                    }
                });
    }

    public void btnInputNum(View view) {
        Button input = (Button) view;
        Intent intent = new Intent(this, KeyPadActivity.class);
        intent.putExtra("BtnID", view.getId());
        intent.putExtra("TextIn", input.getText());
        intent.putExtra("InputUnit",
                ((Button) findViewById(R.id.palletUnitInput)).getText().toString());
        startActivityForResult(intent, view.getId());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null) {
            ((Button) findViewById(requestCode)).setText(data.getStringExtra("Value"));
        }
    }

    public void savePallet(View view) {
        boolean updating = selectedName != null;
        Pallet pallet = readForm();
        if (pallet == null) {
            return;
        }

        ContentValues values = new ContentValues();
        values.put("name", pallet.getName());
        values.put("width", pallet.getWidth());
        values.put("height", pallet.getLength());
        values.put("pallet_height", pallet.getHeight());
        values.put("unit", pallet.getUnit());

        SQLiteDatabase db = helper.getWritableDatabase();
        if (selectedName != null && !selectedName.equals(pallet.getName())) {
            db.delete("palletdb", "name=?", new String[]{selectedName});
        }
        db.insertWithOnConflict("palletdb", null, values, SQLiteDatabase.CONFLICT_REPLACE);
        selectedName = pallet.getName();
        hideKeyboard();
        refreshList();
        updateFormMode();
        Toast.makeText(this, updating ? R.string.palletUpdated : R.string.palletSaved,
                Toast.LENGTH_SHORT).show();
    }

    public void deletePallet(View view) {
        if (selectedName == null) {
            Toast.makeText(this, R.string.selectPalletToDelete, Toast.LENGTH_SHORT).show();
            return;
        }

        ModernConfirmDialog.show(this, getString(R.string.confirmDeletePallet, selectedName),
                R.string.deletePallet, new ModernConfirmDialog.OnConfirmed() {
                    @Override
                    public void onConfirmed() {
                        helper.getWritableDatabase().delete("palletdb", "name=?", new String[]{selectedName});
                        clearForm(null);
                        refreshList();
                        Toast.makeText(PalletManagerActivity.this, R.string.palletDeleted, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    public void clearForm(View view) {
        selectedName = null;
        EditText name = (EditText) findViewById(R.id.palletNameInput);
        Button width = (Button) findViewById(R.id.palletWidthInput);
        Button length = (Button) findViewById(R.id.palletLengthInput);
        Button height = (Button) findViewById(R.id.palletHeightInput);
        Button unit = (Button) findViewById(R.id.palletUnitInput);
        if (name == null || width == null || length == null || height == null || unit == null) {
            return;
        }

        name.setText("");
        width.setText(R.string.initialPalletWidth);
        length.setText(R.string.initialPalletLength);
        height.setText(R.string.initialPalletHeight);
        unit.setText(R.string.unitMm);
        LinearLayout listContainer = (LinearLayout) findViewById(R.id.palletListContainer);
        if (listContainer != null) {
            for (int i = 0; i < listContainer.getChildCount(); i++) {
                updateRowSelection((TextView) listContainer.getChildAt(i), false);
            }
        }
        hideKeyboard();
        updateFormMode();
    }

    private Pallet readForm() {
        try {
            int width = Integer.parseInt(((Button) findViewById(R.id.palletWidthInput)).getText().toString().trim());
            int length = Integer.parseInt(((Button) findViewById(R.id.palletLengthInput)).getText().toString().trim());
            int height = Integer.parseInt(((Button) findViewById(R.id.palletHeightInput)).getText().toString().trim());
            if (width <= 0 || length <= 0 || height <= 0) {
                throw new NumberFormatException();
            }

            String unit = ((Button) findViewById(R.id.palletUnitInput)).getText().toString();
            String name = ((EditText) findViewById(R.id.palletNameInput)).getText().toString().trim();
            if (name.length() == 0) {
                name = width + "X" + length;
            }

            Pallet pallet = new Pallet();
            pallet.setName(name);
            pallet.setWidth(width);
            pallet.setLength(length);
            pallet.setHeight(height);
            pallet.setUnit(unit);
            return pallet;
        } catch (NumberFormatException e) {
            Toast.makeText(this, R.string.invalidPalletSpec, Toast.LENGTH_SHORT).show();
            return null;
        }
    }

    private ArrayList<Pallet> loadPallets() {
        ArrayList<Pallet> result = new ArrayList<Pallet>();
        Cursor cursor = helper.getReadableDatabase().query("palletdb", null, null, null, null, null, "name");
        try {
            while (cursor.moveToNext()) {
                Pallet pallet = new Pallet();
                pallet.setName(cursor.getString(cursor.getColumnIndexOrThrow("name")));
                pallet.setWidth(cursor.getInt(cursor.getColumnIndexOrThrow("width")));
                pallet.setLength(cursor.getInt(cursor.getColumnIndexOrThrow("height")));
                pallet.setHeight(cursor.getInt(cursor.getColumnIndexOrThrow("pallet_height")));
                pallet.setUnit(cursor.getString(cursor.getColumnIndexOrThrow("unit")));
                result.add(pallet);
            }
        } finally {
            cursor.close();
        }
        return result;
    }

    private void refreshList() {
        pallets = loadPallets();
        final LinearLayout listContainer = (LinearLayout) findViewById(R.id.palletListContainer);
        listContainer.removeAllViews();

        for (final Pallet pallet : pallets) {
            TextView row = new TextView(this);
            CharSequence rowText = getString(R.string.palletListRow, pallet.getName(), pallet.getWidth(),
                    pallet.getLength(), pallet.getHeight(), pallet.getUnit());
            row.setTag(rowText);
            row.setTextSize(15);
            row.setPadding(dp(14), dp(12), dp(14), dp(12));
            row.setBackgroundResource(R.drawable.dashboard_action);
            updateRowSelection(row, pallet.getName().equals(selectedName));

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 0, 0, dp(6));
            listContainer.addView(row, params);

            row.setOnClickListener(clicked -> {
                selectedName = pallet.getName();
                for (int i = 0; i < listContainer.getChildCount(); i++) {
                    updateRowSelection((TextView) listContainer.getChildAt(i),
                            listContainer.getChildAt(i) == clicked);
                }
                applyPallet(pallet);
                scrollToForm();
            });
        }

        findViewById(R.id.emptyPalletList).setVisibility(pallets.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void updateRowSelection(TextView row, boolean selected) {
        CharSequence rowText = (CharSequence) row.getTag();
        row.setSelected(selected);
        row.setText(selected ? getString(R.string.selectedListItem, rowText) : rowText);
        row.setTextColor(getResources().getColor(
                selected ? R.color.headerText : R.color.textPrimary));
        row.setTypeface(Typeface.DEFAULT, selected ? Typeface.BOLD : Typeface.NORMAL);
        row.setContentDescription(selected
                ? getString(R.string.selectedListItemDescription, rowText) : rowText);
    }

    private void applyPallet(Pallet pallet) {
        ((EditText) findViewById(R.id.palletNameInput)).setText(pallet.getName());
        ((Button) findViewById(R.id.palletWidthInput)).setText(String.valueOf(pallet.getWidth()));
        ((Button) findViewById(R.id.palletLengthInput)).setText(String.valueOf(pallet.getLength()));
        ((Button) findViewById(R.id.palletHeightInput)).setText(String.valueOf(pallet.getHeight()));
        ((Button) findViewById(R.id.palletUnitInput)).setText(pallet.getUnit());
        hideKeyboard();
        updateFormMode();
    }

    private void updateFormMode() {
        TextView mode = (TextView) findViewById(R.id.palletFormModeTitle);
        Button save = (Button) findViewById(R.id.palletSaveButton);
        Button delete = (Button) findViewById(R.id.palletDeleteButton);
        if (mode == null || save == null || delete == null) {
            return;
        }
        boolean editing = selectedName != null;
        mode.setText(editing
                ? getString(R.string.palletFormModeEditing, selectedName)
                : getString(R.string.palletFormModeNew));
        mode.setTextColor(getResources().getColor(
                editing ? R.color.primaryColor : R.color.textPrimary));
        mode.setBackgroundResource(editing
                ? R.drawable.update_mode_background : R.drawable.selection_summary);
        save.setText(editing ? R.string.updatePallet : R.string.savePallet);
        delete.setEnabled(editing);
        delete.setAlpha(editing ? 1f : 0.45f);
    }

    private void scrollToForm() {
        final ScrollView scroll = (ScrollView) findViewById(R.id.palletManagerScroll);
        scroll.post(new Runnable() {
            @Override
            public void run() {
                scroll.smoothScrollTo(0, 0);
            }
        });
    }

    private void hideKeyboard() {
        View focused = getCurrentFocus();
        if (focused != null) {
            InputMethodManager keyboard = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            keyboard.hideSoftInputFromWindow(focused.getWindowToken(), 0);
            focused.clearFocus();
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    public void btnBack(View view) {
        finish();
    }
}
