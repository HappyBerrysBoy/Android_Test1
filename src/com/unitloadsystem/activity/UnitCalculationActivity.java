package com.unitloadsystem.activity;

import com.unitloadsystem.activity.R;
import com.unitloadsystem.beans.StackEvalutorBean;
import com.unitloadsystem.db.MySQLiteOpenHelper;
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
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;

public class UnitCalculationActivity extends Activity {

    SQLiteDatabase db;
    MySQLiteOpenHelper helper;

	Button bBtn;
	TextView tView;
	int g_iBtnID;
	int g_iArrayID;
	int g_iContainerLength;
	int g_iContainerWidth;
	
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

    public ArrayList getPallets(){
        db = helper.getReadableDatabase(); // db객체를 얻어온다. 읽기 전용
        Cursor c = db.query("palletdb", null, null, null, null, null, null);

        ArrayList aResult = new ArrayList();

        while (c.moveToNext()) {
            // c의 int가져와라 ( c의 컬럼 중 id) 인 것의 형태이다.
            String name = c.getString(c.getColumnIndex("name"));
            int width = c.getInt(c.getColumnIndex("width"));
            int height = c.getInt(c.getColumnIndex("height"));
            String unit = c.getString(c.getColumnIndex("unit"));

            HashMap hMap = new HashMap();
            hMap.put("name", name);
            hMap.put("width", width);
            hMap.put("height", height);
            hMap.put("unit", unit);
            aResult.add(hMap);
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
		SetDialogItem(v.getId(), R.array.containerType);
		
		Button btnDetailSpec = (Button) findViewById(R.id.detailSpec);
		btnDetailSpec.setText("");
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
        ArrayList aPalletList = getPallets();

		Button btnSize;
		btnSize = (Button) findViewById(R.id.length);
		float fBoxLength = Float.parseFloat(btnSize.getText().toString());
		btnSize = (Button) findViewById(R.id.width);
		float fBoxWidth = Float.parseFloat(btnSize.getText().toString());
		btnSize = (Button) findViewById(R.id.height);
		float fBoxHeight = Float.parseFloat(btnSize.getText().toString());
		
		Intent intent = new Intent(getApplicationContext(), CalculationResultActivity.class);
		intent.putExtra("Length", fBoxLength);
		intent.putExtra("Width", fBoxWidth);
		intent.putExtra("Height", fBoxHeight);
		
		btnSize = (Button) findViewById(R.id.detailSpec);
		SetContainerSize(btnSize.getText().toString());
		intent.putExtra("ContainerLength", g_iContainerLength);
		intent.putExtra("ContainerWidth", g_iContainerWidth);
		intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
		
		StackEvalutorBean splitStack = getSplitStackResult(g_iContainerWidth, g_iContainerLength, fBoxWidth, fBoxLength);
		StackEvalutorBean pinWheelStack = getPinWheelStackResult(g_iContainerWidth, g_iContainerLength, fBoxWidth, fBoxLength);
		
		intent.putExtra("SplitStack", getBundleResult(splitStack));
		intent.putExtra("PinWheelStack", getBundleResult(pinWheelStack));
		intent.putExtra("SplitStackShare", splitStack.getShare());
		intent.putExtra("PinWheelStackShare", pinWheelStack.getShare());
		intent.putExtra("PinWheelStackRowCount", pinWheelStack.getRowCount());
		intent.putExtra("PinWheelStackColCount", pinWheelStack.getColCount());
		startActivity(intent);
	}
	
	private void SetContainerSize(String pSize){
		String[] sSize = pSize.split("X");
		
		if(sSize.length > 1){
			g_iContainerWidth = Integer.parseInt(sSize[0].trim());
			g_iContainerLength = Integer.parseInt(sSize[1].trim());
		}else{
			g_iContainerLength = 1100;
			g_iContainerWidth = 1100;
		}
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
//		StackEvalutorBean stackVertical = calcPinWheelStack.CalcSplitStackRule("V", "H", pContainerWidth, pContainerLength, pWidth, pLength);
		
//		if(stackHorizontal.getShare() > stackVertical.getShare()){
//		if(stackHorizontal.getShare() > 0){
			return stackHorizontal;
//		}else{
//			return stackVertical;
//		}
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
