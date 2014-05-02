package com.unitloadsystem.activity;

import com.unitloadsystem.activity.R;
import com.unitloadsystem.fragments.Fragments.TitleFragment;
import com.unitloadsystem.fragments.Fragments.UnitCalcFragment;

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
import android.widget.Toast;

public class UnitCalculationActivity extends Activity {
	
	Button bBtn;
	TextView tView;
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
	}
	
	public void btnDetail(View v){
		SetDialogItem(v.getId(), R.array.detailContainer);
	}
}
