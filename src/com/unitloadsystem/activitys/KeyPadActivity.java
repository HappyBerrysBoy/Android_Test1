package com.unitloadsystem.activitys;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class KeyPadActivity extends LocalizedActivity {
    private static final String STATE_FIRST_CLICK = "state_first_click";
    private static final double MAX_VALUE = 100000d;

    private int inputViewId;
    private TextView valueText;
    private boolean firstClick = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.keypad);

        valueText = (TextView) findViewById(R.id.showInputNum);
        inputViewId = getIntent().getIntExtra("BtnID", -1);
        String inputLabel = getIntent().getStringExtra("InputLabel");
        ((TextView) findViewById(R.id.inputFieldLabel)).setText(
                inputLabel == null || inputLabel.trim().length() == 0
                        ? getInputLabel(inputViewId) : inputLabel);
        showInputUnit(getIntent().getStringExtra("InputUnit"));

        String text = getIntent().getStringExtra("TextIn");
        if (savedInstanceState == null) {
            valueText.setText(text == null || text.trim().length() == 0 ? "0" : text);
        } else {
            firstClick = savedInstanceState.getBoolean(STATE_FIRST_CLICK, false);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(STATE_FIRST_CLICK, firstClick);
        super.onSaveInstanceState(outState);
    }

    public void btnInputNum(View view) {
        String key = ((Button) view).getText().toString();
        String current = valueText.getText().toString();

        if (".".equals(key) && !firstClick && current.contains(".")) {
            return;
        }

        String candidate;
        if (firstClick) {
            candidate = ".".equals(key) ? "0." : key;
        } else if ("0".equals(current) && !".".equals(key)) {
            candidate = key;
        } else {
            candidate = current + key;
        }

        if (candidate.length() > 10 || parseValue(candidate) >= MAX_VALUE) {
            Toast.makeText(this, R.string.keypadValueTooLarge, Toast.LENGTH_SHORT).show();
            return;
        }

        valueText.setText(candidate);
        firstClick = false;
    }

    public void btnBackspace(View view) {
        String current = valueText.getText().toString();
        if (firstClick || current.length() <= 1) {
            valueText.setText("0");
        } else {
            valueText.setText(current.substring(0, current.length() - 1));
        }
        firstClick = false;
    }

    public void btnClear(View view) {
        valueText.setText("0");
        firstClick = true;
    }

    public void btnSend(View view) {
        Intent result = new Intent();
        result.putExtra("Value", valueText.getText().toString());
        setResult(RESULT_OK, result);
        finish();
    }

    private double parseValue(String value) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException ignored) {
            return 0d;
        }
    }

    private void showInputUnit(String unit) {
        TextView unitText = (TextView) findViewById(R.id.inputUnit);
        if (unit == null || unit.trim().length() == 0) {
            unitText.setVisibility(View.GONE);
            return;
        }
        unitText.setText(unit);
        unitText.setVisibility(View.VISIBLE);
    }

    private String getInputLabel(int viewId) {
        if (viewId == R.id.length || viewId == R.id.boxLength) {
            return getString(R.string.keypadInputLength);
        }
        if (viewId == R.id.width || viewId == R.id.boxWidth) {
            return getString(R.string.keypadInputWidth);
        }
        if (viewId == R.id.height || viewId == R.id.boxHeight) {
            return getString(R.string.keypadInputHeight);
        }
        if (viewId == R.id.quantity) {
            return getString(R.string.keypadInputQuantity);
        }
        if (viewId == R.id.weight || viewId == R.id.boxWeight) {
            return getString(R.string.keypadInputWeight);
        }
        if (viewId == R.id.boxLayers) {
            return getString(R.string.keypadInputLayers);
        }
        if (viewId == R.id.palletWidthInput) {
            return getString(R.string.keypadInputPalletWidth);
        }
        if (viewId == R.id.palletLengthInput) {
            return getString(R.string.keypadInputPalletLength);
        }
        if (viewId == R.id.palletHeightInput) {
            return getString(R.string.keypadInputPalletHeight);
        }
        return getString(R.string.keypadValueLabel);
    }
}
