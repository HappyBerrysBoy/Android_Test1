package com.unitloadsystem.activity;

import java.util.ArrayList;
import java.util.HashMap;

import com.unitloadsystem.activity.R;
import com.unitloadsystem.beans.PalletViewBean;
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
//		aList.add(new PalletViewBean("V", 0, 0));
//		aList.add(new PalletViewBean("H", g_fBoxLength, 0));
//		aList.add(new PalletViewBean("H", 0, g_fBoxWidth));
//		aList.add(new PalletViewBean("V", g_fBoxWidth, g_fBoxLength));

		double dUnitShare = (g_fBoxLength * g_fBoxWidth) / (g_iContainerLength * g_iContainerWidth);
		double dPalletShare = 0;
		double dPalletShareWidth = 0;
		double dPalletShareLength = 0;
		ArrayList<PalletViewBean> aList = new ArrayList<PalletViewBean>();
		ArrayList<Rect> aRectList = new ArrayList<Rect>();
		
		boolean bCheck = true;
		
		if(g_fBoxWidth < g_iContainerWidth && g_fBoxLength < g_iContainerLength){
			aList.add(new PalletViewBean("H", 0, 0));
			dPalletShare += dUnitShare;
			
			dPalletShareWidth = g_fBoxWidth;
			dPalletShareLength = g_fBoxLength;
			
			aRectList.add(new Rect(0, 0, (int)(g_fBoxWidth), (int)(g_fBoxLength)));
		}
		
		while(bCheck){
			if(dPalletShareWidth + g_fBoxWidth < g_iContainerWidth 
					&& dPalletShareLength + g_fBoxLength < g_iContainerLength && dPalletShare + dUnitShare < 100){
				aList.add(new PalletViewBean("H", (int)(dPalletShareWidth), (int)(dPalletShareLength)));
				dPalletShare += dUnitShare;
				
				dPalletShareWidth = g_fBoxWidth;
				dPalletShareLength = g_fBoxLength;
				
				aRectList.add(new Rect((int)(dPalletShareWidth), (int)(dPalletShareLength), (int)(g_fBoxWidth), (int)(g_fBoxLength)));
				continue;
			}
			
			
		}
		
		b.putParcelableArrayList("Layout", aList);
		
		return b;
	}
}
