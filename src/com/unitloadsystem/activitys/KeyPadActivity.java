package com.unitloadsystem.activitys;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class KeyPadActivity extends Activity{
	
	int iID;
	TextView tText;
	Button bBtn;
	boolean g_bFirstClick = true;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.keypad);
		
		tText = (TextView) findViewById(R.id.showInputNum);
		
		Intent intent = getIntent();
		iID = intent.getIntExtra("BtnID", -1);
		String text = intent.getStringExtra("TextIn");
		
		if (!text.equals(""))
			tText.setText(text);
		else
			tText.setText("0");
	}
	
	public void btnInputNum(View v){
		bBtn = (Button) findViewById(v.getId());
		String sBtnText = ((CharSequence)bBtn.getText()).toString();
		String sValueText = tText.getText().toString();
		
		if(sBtnText.equals("."))
			if(sValueText.contains("."))
				return;
		
		if(Double.parseDouble(sValueText + sBtnText) >= 100000)
			return;
		
		if(sValueText.length() > 10)
			return;
		
		if(g_bFirstClick){
			tText.setText(sBtnText);
			g_bFirstClick = false;
		}
		else if(sValueText.equals("0") && !sBtnText.equals("."))
			tText.setText(sBtnText);
		else
			tText.setText(tText.getText() + sBtnText);
	}
	
	public void btnClear(View v){
		tText.setText("0");
	}
	
	public void btnSend(View v){
		Intent intent = new Intent(getApplicationContext(), UnitCalculationActivity.class);
		intent.putExtra("Value", tText.getText().toString());
		intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
		setResult(RESULT_OK, intent);
		finish();
	}
}
