package com.unitloadsystem.activitys;

import com.unitloadsystem.beans.StackEvalutorBean;
import com.unitloadsystem.db.MySQLiteOpenHelper;
import com.unitloadsystem.db.Pallet;
import com.unitloadsystem.fragments.Fragments.TitleFragment;
import com.unitloadsystem.fragments.Fragments.UnitCalcFragment;
import com.unitloadsystem.stackcalculation.CalcPinWheelStackforPallet;
import com.unitloadsystem.stackcalculation.CalcSplitStackforPallet;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;

public class UnitCalculationActivity extends Activity {

    SQLiteDatabase db;
    MySQLiteOpenHelper helper;

	Button bBtn;
	int g_iBtnID;
	int g_iArrayID;

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

        helper = new MySQLiteOpenHelper(getApplicationContext(), "pallet.db", null, 1);
	}

	@Override
	protected void onStart() {
		// TODO Auto-generated method stub
		super.onStart();
//		Toast.makeText(getApplicationContext(), "onStart()", Toast.LENGTH_SHORT).show();
		TextView textTitle = (TextView) findViewById(R.id.title);
		textTitle.setText(R.string.calc);
	}

    public ArrayList<Pallet> getPallets(){
        db = helper.getReadableDatabase(); // db객체를 얻어온다. 읽기 전용
        Cursor c = db.query("palletdb", null, null, null, null, null, null);

        ArrayList<Pallet> aResult = new ArrayList<Pallet>();

        while (c.moveToNext()) {
            // c의 int가져와라 ( c의 컬럼 중 id) 인 것의 형태이다.
            String name = c.getString(c.getColumnIndex("name"));
            int width = c.getInt(c.getColumnIndex("width"));
            int height = c.getInt(c.getColumnIndex("height"));
            String unit = c.getString(c.getColumnIndex("unit"));

            Pallet pallet = new Pallet();
            pallet.setName(name);
            pallet.setWidth(width);
            pallet.setLength(height);
            pallet.setUnit(unit);
            aResult.add(pallet);
        }

        return aResult;
    }

	public void btnInputNum(View v){

		bBtn = (Button) findViewById(v.getId());

		Intent intent = new Intent(getApplicationContext(), KeyPadActivity.class);
		intent.putExtra("BtnID", v.getId());
		intent.putExtra("TextIn", bBtn.getText());
		intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
		startActivityForResult(intent, v.getId());
	}

	protected void onActivityResult (int requestCode, int resultCode, Intent data) {

		if (resultCode == RESULT_OK) {
			bBtn = (Button) findViewById(requestCode);
			bBtn.setText(data.getStringExtra("Value"));
		}
	}

	public void btnDimension(View v){
        // 2015. 1. 23. 일단 센치미터만 가능하도록 설정
//		SetDialogItem(v.getId(), R.array.dimensions);
	}

	private String SetDialogItem(int id, int arrayId){
		String sReturn = "";

		g_iBtnID = id;
		g_iArrayID = arrayId;

		new AlertDialog.Builder(this)
		.setTitle("Select Item")
//		.setIcon(R.drawable.ic_launcher)
		.setItems(g_iArrayID,
			new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				String[] dimensions = getResources().getStringArray(g_iArrayID);
				Button btn = (Button)findViewById(g_iBtnID);
				btn.setText(dimensions[which]);
			}
		})
		.setNegativeButton("Cancel", null)
		.show();

		return sReturn;
	}

    private String SetDialogItem(int id, String[] names){
        String sReturn = "";
        final String[] items = names;

        g_iBtnID = id;

        new AlertDialog.Builder(this)
                .setTitle("Select Item")
                .setItems(names,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                Button btn = (Button)findViewById(g_iBtnID);
                                btn.setText(items[which]);
                            }
                        })
                .setNegativeButton("Cancel", null)
                .show();

        return sReturn;
    }

	public void btnWeightType(View v){
        // 2015. 1. 23. 일단 kg만 가능하도록 설정
//		SetDialogItem(v.getId(), R.array.weights);
	}

	public void btnSelectType(View v){
        // 2015. 2. 2. DB에 등록된 Pallet 모두 표시
//		SetDialogItem(v.getId(), R.array.containerType);
//
//		Button btnDetailSpec = (Button) findViewById(R.id.detailSpec);
//		btnDetailSpec.setText("");
	}

	public void btnDetail(View v){
		ArrayList aList = getPallets();
        String[] strPallets = new String[aList.size()];
        for(int i=0; i<aList.size(); i++){
            strPallets[i] = (String)((HashMap)aList.get(i)).get("name");
        }

        SetDialogItem(v.getId(), strPallets);
	}

	public void btnCalc(View v){
        ArrayList<Pallet> aPalletList = getPallets();

		Button btnSize;
		btnSize = (Button) findViewById(R.id.length);
		float fBoxLength = Float.parseFloat(btnSize.getText().toString());
		btnSize = (Button) findViewById(R.id.width);
		float fBoxWidth = Float.parseFloat(btnSize.getText().toString());
		btnSize = (Button) findViewById(R.id.height);
		float fBoxHeight = Float.parseFloat(btnSize.getText().toString());
        btnSize = (Button) findViewById(R.id.quantity);
        int iBoxQuantity = Integer.parseInt(btnSize.getText().toString());
        btnSize = (Button) findViewById(R.id.weight);
        float fBoxWeight = Float.parseFloat(btnSize.getText().toString());
        btnSize = (Button) findViewById(R.id.boxLayers);
        int iBoxLayer = Integer.parseInt(btnSize.getText().toString());

		Intent intent = new Intent(getApplicationContext(), CalculationResultActivity.class);
		intent.putExtra("Length", fBoxLength);
		intent.putExtra("Width", fBoxWidth);
		intent.putExtra("Height", fBoxHeight);
        intent.putExtra("BoxQuantity", iBoxQuantity);
        intent.putExtra("BoxWeight", fBoxWeight);
        intent.putExtra("BoxLayer", iBoxLayer);

        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        Bundle layouts = new Bundle();

        for(int i=0; i<aPalletList.size(); i++){
            Pallet pallet = aPalletList.get(i);
            Bundle layout = new Bundle();
            layout.putString("PalletName", pallet.getName());
            layout.putInt("ContainerWidth", pallet.getWidth());
            layout.putInt("ContainerLength", pallet.getLength());

            StackEvalutorBean splitStack = getSplitStackResult(pallet.getWidth(), pallet.getLength(), fBoxWidth, fBoxLength);
            StackEvalutorBean pinWheelStack = getPinWheelStackResult(pallet.getWidth(), pallet.getLength(), fBoxWidth, fBoxLength);

            layout.putBundle("SplitStack", getBundleResult(splitStack));
            layout.putBundle("PinWheelStack", getBundleResult(pinWheelStack));
            layout.putDouble("SplitStackShare", splitStack.getShare());
            layout.putDouble("PinWheelStackShare", pinWheelStack.getShare());
            layout.putInt("PinWheelStackRowCount", pinWheelStack.getRowCount());
            layout.putInt("PinWheelStackColCount", pinWheelStack.getColCount());

            layouts.putBundle("Layout" + i, layout);
        }

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
