package com.unitloadsystem.activity;

import com.unitloadsystem.activity.R;
import com.unitloadsystem.fragments.Fragments.TitleFragment;
import com.unitloadsystem.fragments.Fragments.MenuFragment;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

public class MainActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		FragmentManager fragManagr = getFragmentManager();
		FragmentTransaction fragTransaction = fragManagr.beginTransaction();
		
		if (savedInstanceState == null) {
//			getFragmentManager().beginTransaction().add(R.id.container, new PlaceholderFragment()).commit();
			fragTransaction.add(R.id.container, new TitleFragment());
			fragTransaction.add(R.id.container, new MenuFragment());
			
			fragTransaction.commit();
		}
	}
	
	@Override
	protected void onStart() {
		// TODO Auto-generated method stub
		super.onStart();
//		Toast.makeText(getApplicationContext(), "onStart()", Toast.LENGTH_SHORT).show();
		TextView textTitle = (TextView) findViewById(R.id.title);
		textTitle.setText(R.string.menu);
	}
	
	public void btnCalcClick(View v){
		Intent intent = new Intent(getApplicationContext(), UnitCalculationActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
		startActivity(intent);
	}

	public void btnPalletInfo(View v){
		Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://kpp.logisall.com/"));
		startActivity(intent); 
	}
	
	public void btnCodeManager(View v){
		Intent intent = new Intent(getApplicationContext(), PalletManagerActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
		startActivity(intent);
	}
}
