package com.unitloadsystem.activitys;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.unitloadsystem.db.BoxSpec;
import com.unitloadsystem.db.MySQLiteOpenHelper;

import java.text.DecimalFormat;
import java.util.ArrayList;

public class BoxManagerActivity extends LocalizedActivity {
    private MySQLiteOpenHelper helper;
    private Button activeNumberButton;
    private ArrayList<BoxSpec> boxSpecs = new ArrayList<BoxSpec>();
    private String selectedName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.boxmanager_layout);
        helper = new MySQLiteOpenHelper(getApplicationContext(), "pallet.db", null, MySQLiteOpenHelper.DB_VERSION);
        clearForm(null);
        refreshList();
    }

    public void btnInputNum(View view) {
        activeNumberButton = (Button) view;
        Intent intent = new Intent(this, KeyPadActivity.class);
        intent.putExtra("BtnID", view.getId());
        intent.putExtra("TextIn", activeNumberButton.getText());
        intent.putExtra("InputUnit", getNumberInputUnit(view.getId()));
        startActivityForResult(intent, view.getId());
    }

    private String getNumberInputUnit(int viewId) {
        if (viewId == R.id.boxLength || viewId == R.id.boxWidth || viewId == R.id.boxHeight) {
            return ((Button) findViewById(R.id.boxDimension)).getText().toString();
        }
        if (viewId == R.id.boxWeight) {
            return ((Button) findViewById(R.id.boxWeightUnit)).getText().toString();
        }
        return "";
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null) {
            ((Button) findViewById(requestCode)).setText(data.getStringExtra("Value"));
        }
    }

    public void btnBoxDimension(View view) {
        showChoiceDialog((Button) view, R.array.dimensions, R.string.chooseLengthUnit);
    }

    public void btnBoxWeightType(View view) {
        showChoiceDialog((Button) view, R.array.weights, R.string.chooseWeightUnit);
    }

    private void showChoiceDialog(final Button target, int arrayId, int titleResId) {
        final String[] items = getResources().getStringArray(arrayId);
        int selectedIndex = -1;
        for (int i = 0; i < items.length; i++) {
            if (items[i].equals(target.getText().toString())) {
                selectedIndex = i;
                break;
            }
        }
        ModernChoiceDialog.show(this, titleResId, items, selectedIndex,
                new ModernChoiceDialog.OnChoiceSelected() {
                    @Override
                    public void onChoiceSelected(int index) {
                        target.setText(items[index]);
                    }
                });
    }

    public void saveBox(View view) {
        boolean updating = selectedName != null;
        BoxSpec boxSpec = readForm();
        if (boxSpec == null) {
            return;
        }

        ContentValues values = new ContentValues();
        values.put("name", boxSpec.getName());
        values.put("box_length", boxSpec.getLength());
        values.put("box_width", boxSpec.getWidth());
        values.put("box_height", boxSpec.getHeight());
        values.put("unit", boxSpec.getUnit());
        values.put("weight", boxSpec.getWeight());
        values.put("weight_unit", boxSpec.getWeightUnit());

        SQLiteDatabase db = helper.getWritableDatabase();
        if (selectedName != null && !selectedName.equals(boxSpec.getName())) {
            db.delete("boxdb", "name=?", new String[]{selectedName});
        }
        db.insertWithOnConflict("boxdb", null, values, SQLiteDatabase.CONFLICT_REPLACE);
        selectedName = boxSpec.getName();
        refreshList();
        updateFormMode();
        Toast.makeText(this, updating ? R.string.updateBoxDone : R.string.saveBoxDone,
                Toast.LENGTH_SHORT).show();
    }

    public void deleteBox(View view) {
        if (selectedName == null) {
            Toast.makeText(this, R.string.selectBoxToDelete, Toast.LENGTH_SHORT).show();
            return;
        }

        ModernConfirmDialog.show(this, getString(R.string.confirmDeleteBox, selectedName),
                R.string.deleteBox, new ModernConfirmDialog.OnConfirmed() {
                    @Override
                    public void onConfirmed() {
                        helper.getWritableDatabase().delete("boxdb", "name=?", new String[]{selectedName});
                        selectedName = null;
                        clearForm(null);
                        refreshList();
                        Toast.makeText(BoxManagerActivity.this, R.string.deleteBoxDone, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    public void clearForm(View view) {
        selectedName = null;
        ((EditText) findViewById(R.id.boxName)).setText("");
        ((Button) findViewById(R.id.boxLength)).setText(R.string.initBoxWidth);
        ((Button) findViewById(R.id.boxWidth)).setText(R.string.initBoxHeight);
        ((Button) findViewById(R.id.boxHeight)).setText(R.string.initBoxDepth);
        ((Button) findViewById(R.id.boxDimension)).setText(R.string.unitMm);
        ((Button) findViewById(R.id.boxWeight)).setText(R.string.initialFive);
        ((Button) findViewById(R.id.boxWeightUnit)).setText(R.string.unitKg);
        LinearLayout listContainer = (LinearLayout) findViewById(R.id.boxListContainer);
        for (int i = 0; i < listContainer.getChildCount(); i++) {
            updateRowSelection((TextView) listContainer.getChildAt(i), false);
        }
        updateFormMode();
    }

    private BoxSpec readForm() {
        try {
            BoxSpec boxSpec = new BoxSpec();
            boxSpec.setLength(Float.parseFloat(((Button) findViewById(R.id.boxLength)).getText().toString()));
            boxSpec.setWidth(Float.parseFloat(((Button) findViewById(R.id.boxWidth)).getText().toString()));
            boxSpec.setHeight(Float.parseFloat(((Button) findViewById(R.id.boxHeight)).getText().toString()));
            boxSpec.setUnit(((Button) findViewById(R.id.boxDimension)).getText().toString());
            boxSpec.setWeight(Float.parseFloat(((Button) findViewById(R.id.boxWeight)).getText().toString()));
            boxSpec.setWeightUnit(((Button) findViewById(R.id.boxWeightUnit)).getText().toString());

            String name = ((EditText) findViewById(R.id.boxName)).getText().toString().trim();
            if (name.length() == 0) {
                name = makeBoxName(boxSpec);
            }
            boxSpec.setName(name);

            if (boxSpec.getLength() <= 0 || boxSpec.getWidth() <= 0 || boxSpec.getHeight() <= 0 ||
                    boxSpec.getWeight() < 0) {
                throw new NumberFormatException();
            }
            return boxSpec;
        } catch (NumberFormatException e) {
            Toast.makeText(this, R.string.invalidBoxSpec, Toast.LENGTH_SHORT).show();
            return null;
        }
    }

    private String makeBoxName(BoxSpec boxSpec) {
        DecimalFormat format = new DecimalFormat("0.##");
        return format.format(boxSpec.getLength()) + "X" + format.format(boxSpec.getWidth()) + "X" +
                format.format(boxSpec.getHeight()) + " " + boxSpec.getUnit();
    }

    private void refreshList() {
        boxSpecs = loadBoxSpecs();
        final LinearLayout listContainer = (LinearLayout) findViewById(R.id.boxListContainer);
        listContainer.removeAllViews();
        int horizontalPadding = dp(14);
        int verticalPadding = dp(12);

        for (final BoxSpec boxSpec : boxSpecs) {
            TextView row = new TextView(this);
            CharSequence rowText = getString(R.string.boxListRow,
                    boxSpec.getName(), makeBoxDescription(boxSpec));
            row.setTag(rowText);
            row.setTextSize(15);
            row.setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding);
            row.setBackgroundResource(R.drawable.dashboard_action);
            updateRowSelection(row, boxSpec.getName().equals(selectedName));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 0, 0, dp(6));
            listContainer.addView(row, params);
            row.setOnClickListener(clicked -> {
                selectedName = boxSpec.getName();
                for (int i = 0; i < listContainer.getChildCount(); i++) {
                    updateRowSelection((TextView) listContainer.getChildAt(i),
                            listContainer.getChildAt(i) == clicked);
                }
                applyBoxSpec(boxSpec);
                scrollToForm();
            });
        }

        if (boxSpecs.isEmpty()) {
            findViewById(R.id.emptyBoxList).setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.emptyBoxList).setVisibility(View.GONE);
        }
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

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private ArrayList<BoxSpec> loadBoxSpecs() {
        ArrayList<BoxSpec> result = new ArrayList<BoxSpec>();
        Cursor cursor = helper.getReadableDatabase().query("boxdb", null, null, null, null, null, "name");
        try {
            while (cursor.moveToNext()) {
                BoxSpec boxSpec = new BoxSpec();
                boxSpec.setName(cursor.getString(cursor.getColumnIndexOrThrow("name")));
                boxSpec.setLength(cursor.getFloat(cursor.getColumnIndexOrThrow("box_length")));
                boxSpec.setWidth(cursor.getFloat(cursor.getColumnIndexOrThrow("box_width")));
                boxSpec.setHeight(cursor.getFloat(cursor.getColumnIndexOrThrow("box_height")));
                boxSpec.setUnit(cursor.getString(cursor.getColumnIndexOrThrow("unit")));
                boxSpec.setWeight(cursor.getFloat(cursor.getColumnIndexOrThrow("weight")));
                boxSpec.setWeightUnit(cursor.getString(cursor.getColumnIndexOrThrow("weight_unit")));
                result.add(boxSpec);
            }
        } finally {
            cursor.close();
        }
        return result;
    }

    private String makeBoxDescription(BoxSpec boxSpec) {
        DecimalFormat format = new DecimalFormat("0.##");
        return format.format(boxSpec.getLength()) + " x " + format.format(boxSpec.getWidth()) + " x " +
                format.format(boxSpec.getHeight()) + " " + boxSpec.getUnit() + "  |  " +
                format.format(boxSpec.getWeight()) + " " + boxSpec.getWeightUnit();
    }

    private void applyBoxSpec(BoxSpec boxSpec) {
        DecimalFormat format = new DecimalFormat("0.##");
        ((EditText) findViewById(R.id.boxName)).setText(boxSpec.getName());
        ((Button) findViewById(R.id.boxLength)).setText(format.format(boxSpec.getLength()));
        ((Button) findViewById(R.id.boxWidth)).setText(format.format(boxSpec.getWidth()));
        ((Button) findViewById(R.id.boxHeight)).setText(format.format(boxSpec.getHeight()));
        ((Button) findViewById(R.id.boxDimension)).setText(boxSpec.getUnit());
        ((Button) findViewById(R.id.boxWeight)).setText(format.format(boxSpec.getWeight()));
        ((Button) findViewById(R.id.boxWeightUnit)).setText(boxSpec.getWeightUnit());
        updateFormMode();
    }

    private void updateFormMode() {
        TextView mode = (TextView) findViewById(R.id.boxFormModeTitle);
        Button save = (Button) findViewById(R.id.boxSaveButton);
        Button delete = (Button) findViewById(R.id.boxDeleteButton);
        if (mode == null || save == null || delete == null) {
            return;
        }
        boolean editing = selectedName != null;
        mode.setText(editing
                ? getString(R.string.boxFormModeEditing, selectedName)
                : getString(R.string.boxFormModeNew));
        mode.setTextColor(getResources().getColor(
                editing ? R.color.primaryColor : R.color.textPrimary));
        mode.setBackgroundResource(editing
                ? R.drawable.update_mode_background : R.drawable.selection_summary);
        save.setText(editing ? R.string.updateBox : R.string.saveBox);
        delete.setEnabled(editing);
        delete.setAlpha(editing ? 1f : 0.45f);
    }

    private void scrollToForm() {
        final ScrollView scroll = (ScrollView) findViewById(R.id.boxManagerScroll);
        scroll.post(new Runnable() {
            @Override
            public void run() {
                scroll.smoothScrollTo(0, 0);
            }
        });
    }

    public void btnBack(View view) {
        finish();
    }
}
