package com.unitloadsystem.activity;

import java.util.ArrayList;
import java.util.HashMap;

import com.unitloadsystem.activity.R;
import com.unitloadsystem.beans.PalletViewBean;
import com.unitloadsystem.beans.StackEvalutorBean;
import com.unitloadsystem.fragments.Fragments.TitleFragment;
import com.unitloadsystem.fragments.Fragments.UnitCalcFragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Parcelable.Creator;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class UnitCalculationActivity extends Activity {
	
	Button bBtn;
	TextView tView;
	int g_iBtnID;
	int g_iArrayID;
	int g_iContainerLength;
	int g_iContainerWidth;
	
	float g_fBoxLength;
	float g_fBoxWidth;
	
	double g_dPalletShare;
	
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
//		new AlertDialog.Builder(this)
//		.setTitle("Select Item")
////		.setIcon(R.drawable.ic_launcher)
//		.setItems(R.array.dimensions, 
//			new DialogInterface.OnClickListener() {
//			public void onClick(DialogInterface dialog, int which) {
//				String[] dimensions = getResources().getStringArray(R.array.dimensions);
//				Button btn = (Button)findViewById(R.id.dimension);
//				btn.setText(dimensions[which]);
//			}
//		})
//		.setNegativeButton("Cancel", null)
//		.show();
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
		g_dPalletShare = 0.0d;
		
		Intent intent = new Intent(getApplicationContext(), CalculationResultActivity.class);
		btnSize = (Button) findViewById(R.id.length);
		intent.putExtra("Length", btnSize.getText());
		g_fBoxLength = Float.parseFloat(btnSize.getText().toString());
		
		btnSize = (Button) findViewById(R.id.width);
		g_fBoxWidth = Float.parseFloat(btnSize.getText().toString());
		
		intent.putExtra("Width", btnSize.getText());
		btnSize = (Button) findViewById(R.id.height);
		intent.putExtra("Height", btnSize.getText());
		
		btnSize = (Button) findViewById(R.id.detailSpec);
		SetContainerSize(btnSize.getText().toString());
		intent.putExtra("ContainerLength", g_iContainerLength);
		intent.putExtra("ContainerWidth", g_iContainerWidth);
		intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
		intent.putExtra("Layout", getBoxArray());
		intent.putExtra("PalletShare", g_dPalletShare);
		startActivity(intent);
	}
	
	private void SetContainerSize(String p_sSize){
		String[] sSize = p_sSize.split("X");
		
		if(sSize.length > 1){
			g_iContainerWidth = Integer.parseInt(sSize[0].trim());
			g_iContainerLength = Integer.parseInt(sSize[1].trim());
		}else{
			g_iContainerLength = 1100;
			g_iContainerWidth = 1100;
		}
	}
	
	private double GetShareRate(){
		double dReturn = 0.0d;
		
		return dReturn;
	}
	
	
	
	private Bundle getBoxArray(){
		Bundle b = new Bundle();
		ArrayList<ArrayList<PalletViewBean>> aTotalList = new ArrayList<ArrayList<PalletViewBean>>();

		StackEvalutorBean stackHorizontal = CalcHorizontalSplitStackRule();
		StackEvalutorBean stackVertical = CalcVerticalSplitStackRule();
		
		
		if(stackHorizontal.getShare() > stackVertical.getShare()){
			b.putParcelableArrayList("Layout", stackHorizontal.getPalletView());
			g_dPalletShare = stackHorizontal.getShare();
		}else{
			b.putParcelableArrayList("Layout", stackVertical.getPalletView());
			g_dPalletShare = stackVertical.getShare();
		}
		
		return b;
	}
	
	private StackEvalutorBean CalcHorizontalSplitStackRule(){
		StackEvalutorBean result = new StackEvalutorBean();
		ArrayList<PalletViewBean> aList = new ArrayList<PalletViewBean>();
		
		int iWidthCount = (int)(g_iContainerWidth / g_fBoxWidth);
		int iLengthCount = (int)(g_iContainerLength / g_fBoxLength);
		double dUnitShare = (g_fBoxLength * g_fBoxWidth) / (g_iContainerLength * g_iContainerWidth);
		double palletShare = dUnitShare * iWidthCount * iLengthCount;
		
		for(int i=0; i<iWidthCount; i++){
			for(int j=0; j<iLengthCount; j++){
				aList.add(new PalletViewBean("H", (int)(g_fBoxWidth * i), (int)(g_fBoxLength * j)));
			}
		}
		
		if(iWidthCount * g_fBoxWidth + g_fBoxLength <= g_iContainerWidth){
			int iRemainCount = (int)(g_iContainerLength / g_fBoxWidth);
			int iRemainInterval = 0;
			if(iRemainCount > 1){
				iRemainInterval = (int)((g_iContainerLength - g_fBoxWidth * 2 - g_fBoxWidth * (iRemainCount - 2)) / (iRemainCount - 1));
			}
			
			for(int i=0; i<iRemainCount; i++){
				aList.add(new PalletViewBean("V", (int)(g_fBoxWidth * iWidthCount), (int)((g_fBoxWidth + iRemainInterval) * i)));
				palletShare += dUnitShare;
			}
		}
		
		result.setShare(palletShare);
		result.setPalletView(aList);
		
		return result;
	}
	
	private StackEvalutorBean CalcVerticalSplitStackRule(){
		StackEvalutorBean result = new StackEvalutorBean();
		ArrayList<PalletViewBean> aList = new ArrayList<PalletViewBean>();
		
		int iWidthCount = (int)(g_iContainerWidth / g_fBoxLength);
		int iLengthCount = (int)(g_iContainerLength / g_fBoxWidth);
		double dUnitShare = (g_fBoxLength * g_fBoxWidth) / (g_iContainerLength * g_iContainerWidth);
		double palletShare = dUnitShare * iWidthCount * iLengthCount;
		
		for(int i=0; i<iWidthCount; i++){
			for(int j=0; j<iLengthCount; j++){
				aList.add(new PalletViewBean("V", (int)(g_fBoxLength * i), (int)(g_fBoxWidth * j)));
			}
		}
		
		if(iLengthCount * g_fBoxWidth + g_fBoxLength  <= g_iContainerLength){
			int iRemainCount = (int)(g_iContainerWidth / g_fBoxWidth);
			int iRemainInterval = 0;
			if(iRemainCount > 1){
				iRemainInterval = (int)((g_iContainerWidth - g_fBoxWidth * 2 - g_fBoxWidth * (iRemainCount - 2)) / (iRemainCount - 1));
			}
			
			for(int i=0; i<iRemainCount; i++){
				aList.add(new PalletViewBean("H", (int)((g_fBoxWidth + iRemainInterval) * i), (int)(g_fBoxWidth * iLengthCount)));
				palletShare += dUnitShare;
			}
		}
		
		result.setShare(palletShare);
		result.setPalletView(aList);
		
		return result;
	}
}
