package com.unitloadsystem.activitys;

import com.unitloadsystem.beans.StackEvalutorBean;
import com.unitloadsystem.common.CommonFunction;
import com.unitloadsystem.db.BoxSpec;
import com.unitloadsystem.db.MySQLiteOpenHelper;
import com.unitloadsystem.db.Pallet;
import com.unitloadsystem.fragments.Fragments.TitleFragment;
import com.unitloadsystem.fragments.Fragments.UnitCalcFragment;
import com.unitloadsystem.stackcalculation.CalcPinWheelStackforPallet;
import com.unitloadsystem.stackcalculation.CalcSplitStackforPallet;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DecimalFormat;
import java.util.ArrayList;

public class UnitCalculationActivity extends LocalizedActivity {

    SQLiteDatabase db;
    MySQLiteOpenHelper helper;

	Button bBtn;
	private Pallet selectedPallet;
	private String selectedBoxName;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		FragmentManager fragManagr = getFragmentManager();
		FragmentTransaction fragTransaction = fragManagr.beginTransaction();

		if (savedInstanceState == null) {
			fragTransaction.add(R.id.container, new TitleFragment());
			fragTransaction.add(R.id.container, new UnitCalcFragment());

			fragTransaction.commit();
		}

        helper = new MySQLiteOpenHelper(getApplicationContext(), "pallet.db", null, MySQLiteOpenHelper.DB_VERSION);
	}

	@Override
	protected void onStart() {
		// TODO Auto-generated method stub
		super.onStart();
//		Toast.makeText(getApplicationContext(), "onStart()", Toast.LENGTH_SHORT).show();
			TextView textTitle = (TextView) findViewById(R.id.title);
			textTitle.setText(R.string.calc);
			refreshPalletSelection();
		}

	    public ArrayList<Pallet> getPallets(){
        db = helper.getReadableDatabase(); // db객체를 얻어온다. 읽기 전용
	        Cursor c = db.query("palletdb", null, null, null, null, null, "name");

        ArrayList<Pallet> aResult = new ArrayList<Pallet>();

        try {
            int nameIndex = c.getColumnIndexOrThrow("name");
            int widthIndex = c.getColumnIndexOrThrow("width");
	            int heightIndex = c.getColumnIndexOrThrow("height");
	            int palletHeightIndex = c.getColumnIndexOrThrow("pallet_height");
	            int unitIndex = c.getColumnIndexOrThrow("unit");

            while (c.moveToNext()) {
                // c의 int가져와라 ( c의 컬럼 중 id) 인 것의 형태이다.
                String name = c.getString(nameIndex);
                int width = c.getInt(widthIndex);
                int height = c.getInt(heightIndex);
                String unit = c.getString(unitIndex);

                Pallet pallet = new Pallet();
                pallet.setName(name);
	                pallet.setWidth(width);
	                pallet.setLength(height);
	                pallet.setHeight(c.getInt(palletHeightIndex));
	                pallet.setUnit(unit);
                aResult.add(pallet);
            }
        } finally {
            c.close();
        }

	        return aResult;
	    }

        public ArrayList<BoxSpec> getBoxSpecs(){
            db = helper.getReadableDatabase();
            Cursor c = db.query("boxdb", null, null, null, null, null, "name");

            ArrayList<BoxSpec> aResult = new ArrayList<BoxSpec>();

            try {
                int nameIndex = c.getColumnIndexOrThrow("name");
                int lengthIndex = c.getColumnIndexOrThrow("box_length");
                int widthIndex = c.getColumnIndexOrThrow("box_width");
                int heightIndex = c.getColumnIndexOrThrow("box_height");
                int unitIndex = c.getColumnIndexOrThrow("unit");
                int weightIndex = c.getColumnIndexOrThrow("weight");
                int weightUnitIndex = c.getColumnIndexOrThrow("weight_unit");

                while (c.moveToNext()) {
                    BoxSpec boxSpec = new BoxSpec();
                    boxSpec.setName(c.getString(nameIndex));
                    boxSpec.setLength(c.getFloat(lengthIndex));
                    boxSpec.setWidth(c.getFloat(widthIndex));
                    boxSpec.setHeight(c.getFloat(heightIndex));
                    boxSpec.setUnit(c.getString(unitIndex));
                    boxSpec.setWeight(c.getFloat(weightIndex));
                    boxSpec.setWeightUnit(c.getString(weightUnitIndex));
                    aResult.add(boxSpec);
                }
            } finally {
                c.close();
            }

            return aResult;
        }

	public void btnInputNum(View v){

		bBtn = (Button) findViewById(v.getId());

		Intent intent = new Intent(getApplicationContext(), KeyPadActivity.class);
		intent.putExtra("BtnID", v.getId());
		intent.putExtra("TextIn", bBtn.getText());
		intent.putExtra("InputUnit", getNumberInputUnit(v.getId()));
		intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
		startActivityForResult(intent, v.getId());
	}

	private String getNumberInputUnit(int viewId) {
		if (viewId == R.id.length || viewId == R.id.width || viewId == R.id.height) {
			return ((Button) findViewById(R.id.dimension)).getText().toString();
		}
		if (viewId == R.id.weight) {
			return ((Button) findViewById(R.id.weightType)).getText().toString();
		}
		if (viewId == R.id.quantity) {
			return getString(R.string.keypadUnitPieces);
		}
		if (viewId == R.id.boxLayers) {
			return getString(R.string.keypadUnitLayers);
		}
		return "";
	}

	protected void onActivityResult (int requestCode, int resultCode, Intent data) {

		if (resultCode == RESULT_OK) {
			bBtn = (Button) findViewById(requestCode);
			bBtn.setText(data.getStringExtra("Value"));
			updateEstimatedStackHeight();
		}
	}

	public void btnDimension(View v){
		showButtonChoice((Button) v, R.array.dimensions, R.string.chooseLengthUnit);
		}

	private void showButtonChoice(final Button target, int arrayId, int titleResId) {
		final String[] items = getResources().getStringArray(arrayId);
		ModernChoiceDialog.show(this, titleResId, items,
				findSelectedIndex(items, target.getText().toString()),
				new ModernChoiceDialog.OnChoiceSelected() {
					@Override
					public void onChoiceSelected(int index) {
						target.setText(items[index]);
						updateEstimatedStackHeight();
					}
				});
	}

	private int findSelectedIndex(String[] items, String selectedValue) {
		for (int i = 0; i < items.length; i++) {
			if (items[i].equals(selectedValue)) {
				return i;
			}
		}
		return -1;
	}

		public void btnWeightType(View v){
		showButtonChoice((Button) v, R.array.weights, R.string.chooseWeightUnit);
		}

	        public void btnLoadBox(View v){
            final ArrayList<BoxSpec> boxSpecs = getBoxSpecs();
            if(boxSpecs.size() == 0){
                Toast.makeText(this, getString(R.string.noSavedBox), Toast.LENGTH_SHORT).show();
                return;
            }

	            String[] names = new String[boxSpecs.size()];
	            int selectedIndex = -1;
	            for(int i=0; i<boxSpecs.size(); i++){
	                BoxSpec box = boxSpecs.get(i);
	                names[i] = getString(R.string.boxChoiceRow, box.getName(),
	                        formatNumber(box.getLength()), formatNumber(box.getWidth()),
	                        formatNumber(box.getHeight()), box.getUnit(),
                        formatNumber(box.getWeight()), box.getWeightUnit());
	                if (box.getName().equals(selectedBoxName)) {
	                    selectedIndex = i;
	                }
	            }

	            ModernChoiceDialog.show(this, R.string.loadBox, names, selectedIndex,
	                    new ModernChoiceDialog.OnChoiceSelected() {
	                        @Override
	                        public void onChoiceSelected(int index) {
	                            applyBoxSpec(boxSpecs.get(index));
	                            selectedBoxName = boxSpecs.get(index).getName();
	                            ((TextView)findViewById(R.id.selectedBoxStatus)).setText(
	                                    getString(R.string.currentBoxSelected,
	                                            boxSpecs.get(index).getName()));
	                        }
	                    });
        }

	        public void btnDirectInput(View v){
	            selectedBoxName = null;
            ((Button)findViewById(R.id.length)).setText(R.string.initBoxWidth);
            ((Button)findViewById(R.id.width)).setText(R.string.initBoxHeight);
            ((Button)findViewById(R.id.height)).setText(R.string.initBoxDepth);
            ((Button)findViewById(R.id.dimension)).setText(R.string.unitMm);
            ((Button)findViewById(R.id.weight)).setText(R.string.initialFive);
            ((Button)findViewById(R.id.weightType)).setText(R.string.unitKg);
	            resetCalculationInputs();
	            ((TextView)findViewById(R.id.selectedBoxStatus)).setText(R.string.currentBoxDirect);
	            updateEstimatedStackHeight();
	        }

	public void btnSelectPallet(View view) {
		final ArrayList<Pallet> pallets = getPallets();
		if (pallets.isEmpty()) {
			Toast.makeText(this, R.string.registerPalletMsg, Toast.LENGTH_LONG).show();
			return;
		}

		String[] choices = new String[pallets.size()];
		int selectedIndex = -1;
		for (int i = 0; i < pallets.size(); i++) {
			Pallet pallet = pallets.get(i);
			choices[i] = getString(R.string.palletChoiceRow, pallet.getName(),
					pallet.getWidth(), pallet.getLength(), pallet.getHeight(), pallet.getUnit());
			if (selectedPallet != null && pallet.getName().equals(selectedPallet.getName())) {
				selectedIndex = i;
			}
		}

		ModernChoiceDialog.show(this, R.string.chooseSavedPallet, choices, selectedIndex,
				new ModernChoiceDialog.OnChoiceSelected() {
					@Override
					public void onChoiceSelected(int index) {
						selectedPallet = pallets.get(index);
						showSelectedPallet();
					}
				});
	}

	private void refreshPalletSelection() {
		ArrayList<Pallet> pallets = getPallets();
		Pallet current = null;
		if (selectedPallet != null) {
			for (Pallet pallet : pallets) {
				if (pallet.getName().equals(selectedPallet.getName())) {
					current = pallet;
					break;
				}
			}
		}
		selectedPallet = current;
		if (selectedPallet == null && pallets.size() == 1) {
			selectedPallet = pallets.get(0);
		}
		showSelectedPallet();
	}

	private void showSelectedPallet() {
		TextView status = (TextView) findViewById(R.id.selectedPalletStatus);
		if (status == null) {
			return;
		}
		if (selectedPallet == null) {
			status.setText(R.string.currentPalletNone);
			updateEstimatedStackHeight();
			return;
		}
		status.setText(getString(R.string.currentPalletSelected, selectedPallet.getName(),
				selectedPallet.getWidth(), selectedPallet.getLength(), selectedPallet.getHeight(),
				selectedPallet.getUnit()));
		updateEstimatedStackHeight();
	}

	public void btnSelectType(View v){
        // 2015. 2. 2. DB에 등록된 Pallet 모두 표시
//		SetDialogItem(v.getId(), R.array.containerType);
//
//		Button btnDetailSpec = (Button) findViewById(R.id.detailSpec);
//		btnDetailSpec.setText("");
	}

        private String formatNumber(float value){
            DecimalFormat decimalFormat = new DecimalFormat("0.##");
            return decimalFormat.format(value);
        }

        private void applyBoxSpec(BoxSpec boxSpec){
            ((Button)findViewById(R.id.length)).setText(formatNumber(boxSpec.getLength()));
            ((Button)findViewById(R.id.width)).setText(formatNumber(boxSpec.getWidth()));
            ((Button)findViewById(R.id.height)).setText(formatNumber(boxSpec.getHeight()));
            ((Button)findViewById(R.id.dimension)).setText(boxSpec.getUnit());
            ((Button)findViewById(R.id.weight)).setText(formatNumber(boxSpec.getWeight()));
            ((Button)findViewById(R.id.weightType)).setText(boxSpec.getWeightUnit());
			resetCalculationInputs();
			updateEstimatedStackHeight();
        }

		private void resetCalculationInputs() {
			((Button) findViewById(R.id.quantity)).setText("");
			((Button) findViewById(R.id.boxLayers)).setText("");
		}

		private void updateEstimatedStackHeight() {
			TextView summary = (TextView) findViewById(R.id.estimatedStackHeight);
			if (summary == null) {
				return;
			}
			if (selectedPallet == null) {
				summary.setText(R.string.estimatedStackHeightEmpty);
				return;
			}

			try {
				String unit = ((Button) findViewById(R.id.dimension)).getText().toString();
				float boxHeight = Float.parseFloat(
						((Button) findViewById(R.id.height)).getText().toString());
				int layers = Integer.parseInt(
						((Button) findViewById(R.id.boxLayers)).getText().toString());
				if (boxHeight <= 0 || layers <= 0) {
					throw new NumberFormatException();
				}

				float boxHeightMm = CommonFunction.changeToMM(boxHeight, unit);
				float palletHeightMm = CommonFunction.changeToMM(
						selectedPallet.getHeight(), selectedPallet.getUnit());
				float totalHeightMm = boxHeightMm * layers + palletHeightMm;
				float displayTotal = CommonFunction.changeToInch(totalHeightMm, unit);
				float displayBoxes = CommonFunction.changeToInch(boxHeightMm * layers, unit);
				float displayPallet = CommonFunction.changeToInch(palletHeightMm, unit);
				summary.setText(getString(R.string.estimatedStackHeight,
						formatNumber(displayTotal), unit, formatNumber(displayBoxes),
						formatNumber(displayPallet)));
			} catch (NumberFormatException ignored) {
				summary.setText(R.string.estimatedStackHeightEmpty);
			}
		}

	    public void btnCalc(View v){
	        ArrayList<Pallet> pallets = getPallets();
	        if(pallets.size() == 0){
	            Toast.makeText(this, R.string.registerPalletMsg, Toast.LENGTH_LONG).show();
	            return;
	        }
	        if (selectedPallet == null) {
	            Toast.makeText(this, R.string.selectPalletForCalculation, Toast.LENGTH_SHORT).show();
	            return;
	        }

        String unit = ((Button)findViewById(R.id.dimension)).getText().toString();
        String wgtUnit = ((Button)findViewById(R.id.weightType)).getText().toString();

        float fBoxLength;
        float fBoxWidth;
        float fBoxHeight;
        int iBoxQuantity;
        float fBoxWeight;
        int iBoxLayer;
        try {
            fBoxLength = CommonFunction.changeToMM(Float.parseFloat(((Button)findViewById(R.id.length)).getText().toString()), unit);
            fBoxWidth = CommonFunction.changeToMM(Float.parseFloat(((Button)findViewById(R.id.width)).getText().toString()), unit);
            fBoxHeight = CommonFunction.changeToMM(Float.parseFloat(((Button)findViewById(R.id.height)).getText().toString()), unit);
            iBoxQuantity = Integer.parseInt(((Button)findViewById(R.id.quantity)).getText().toString());
            fBoxWeight = Float.parseFloat(((Button)findViewById(R.id.weight)).getText().toString());
            iBoxLayer = Integer.parseInt(((Button)findViewById(R.id.boxLayers)).getText().toString());
            if(fBoxLength <= 0 || fBoxWidth <= 0 || fBoxHeight <= 0 || iBoxQuantity <= 0 || fBoxWeight < 0 || iBoxLayer <= 0){
                throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, R.string.invalidBoxSpec, Toast.LENGTH_SHORT).show();
            return;
        }

		Intent intent = new Intent(getApplicationContext(), CalculationResultActivity.class);
		intent.putExtra("Length", fBoxLength);
		intent.putExtra("Width", fBoxWidth);
		intent.putExtra("Height", fBoxHeight);
        intent.putExtra("BoxQuantity", iBoxQuantity);
        intent.putExtra("BoxWeight", fBoxWeight);
        intent.putExtra("BoxLayer", iBoxLayer);
	        intent.putExtra("BoxDimension", unit);
	        intent.putExtra("BoxWeightUnit", wgtUnit);
	        intent.putExtra("BoxName", selectedBoxName == null
	                ? getString(R.string.directInput) : selectedBoxName);

	        Bundle layouts = new Bundle();
	        Pallet pallet = selectedPallet;
	        int palletWidth = (int)CommonFunction.changeToMM(pallet.getWidth(), pallet.getUnit()) + 1;
	        int palletLength = (int)CommonFunction.changeToMM(pallet.getLength(), pallet.getUnit()) + 1;
	        int palletHeight = (int)CommonFunction.changeToMM(pallet.getHeight(), pallet.getUnit());

	        Bundle layout = new Bundle();
	        layout.putString("PalletName", pallet.getName());
	        layout.putInt("ContainerWidth", palletWidth);
	        layout.putInt("ContainerLength", palletLength);
	        layout.putInt("PalletHeight", palletHeight);
	        layout.putString("PalletDimen", pallet.getUnit());

	        StackEvalutorBean splitStack = getSplitStackResult(palletWidth, palletLength, fBoxWidth, fBoxLength);
	        StackEvalutorBean pinWheelStack = getPinWheelStackResult(palletWidth, palletLength, fBoxWidth, fBoxLength);

	        layout.putBundle("SplitStack", getBundleResult(splitStack));
	        layout.putBundle("PinWheelStack", getBundleResult(pinWheelStack));
	        layout.putDouble("SplitStackShare", splitStack.getShare());
	        layout.putDouble("PinWheelStackShare", pinWheelStack.getShare());
	        layout.putInt("PinWheelStackRowCount", pinWheelStack.getRowCount());
	        layout.putInt("PinWheelStackColCount", pinWheelStack.getColCount());
	        layouts.putBundle("Layout0", layout);

        intent.putExtra("Layouts", layouts);

        startActivity(intent);
	}

	private StackEvalutorBean getSplitStackResult(int pContainerWidth, int pContainerLength, float pWidth, float pLength){
		CalcSplitStackforPallet calcSplitStack = new CalcSplitStackforPallet();

		StackEvalutorBean stackHorizontal = calcSplitStack.CalcSplitStackRule("H", "V", pContainerWidth, pContainerLength, pWidth, pLength, 0, 0);
		StackEvalutorBean stackVertical = calcSplitStack.CalcSplitStackRule("V", "H", pContainerWidth, pContainerLength, pLength, pWidth, 0, 0);

		if(stackHorizontal.getShare() > stackVertical.getShare()){
			return stackHorizontal;
		}else{
			return stackVertical;
		}
	}

	private StackEvalutorBean getPinWheelStackResult(int pContainerWidth, int pContainerLength, float pWidth, float pLength){
		CalcPinWheelStackforPallet calcPinWheelStack = new CalcPinWheelStackforPallet();

		StackEvalutorBean stackHorizontal = calcPinWheelStack.CalcPinWheelStackRule("H", "V", pContainerWidth, pContainerLength, pWidth, pLength);

        return stackHorizontal;
	}
	
	private Bundle getBundleResult(StackEvalutorBean stack){
		Bundle b = new Bundle();
		b.putParcelableArrayList("Layout", stack.getPalletView());

		return b;
	}

    public void btnBack(View v){
        finish();
    }
}
