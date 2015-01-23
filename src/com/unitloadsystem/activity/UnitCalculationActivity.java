package com.unitloadsystem.activity;

import com.unitloadsystem.activity.R;
import com.unitloadsystem.beans.StackEvalutorBean;
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
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class UnitCalculationActivity extends Activity {
	
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
		
		tView = (TextView) findViewById(R.id.showInputInfo);
	}
	
	@Override
	protected void onStart() {
		// TODO Auto-generated method stub
		super.onStart();
//		Toast.makeText(getApplicationContext(), "onStart()", Toast.LENGTH_SHORT).show();
		TextView textTitle = (TextView) findViewById(R.id.title);
		textTitle.setText(R.string.calc);
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
		SetDialogItem(v.getId(), R.array.dimensions);
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
	
	public void btnWeightType(View v){
		SetDialogItem(v.getId(), R.array.weights);
	}
	
	public void btnSelectType(View v){
		SetDialogItem(v.getId(), R.array.containerType);
		
		Button btnDetailSpec = (Button) findViewById(R.id.detailSpec);
		btnDetailSpec.setText("");
	}
	
	public void btnDetail(View v){
		Button btnContainer = (Button) findViewById(R.id.containerType);
		
		if(btnContainer.getText().equals("Container")){
			SetDialogItem(v.getId(), R.array.detailContainer);
		}else if(btnContainer.getText().equals("Pallet")){
			SetDialogItem(v.getId(), R.array.detailPallet);
		}else{
			SetDialogItem(v.getId(), R.array.detailAirULD);
		}
	}
	
	public void btnCalc(View v){
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
}
