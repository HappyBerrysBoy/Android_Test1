package com.unitloadsystem.activity;

import com.unitloadsystem.activity.R;
import com.unitloadsystem.db.MySQLiteOpenHelper;
import com.unitloadsystem.db.Pallet;
import com.unitloadsystem.fragments.Fragments.TitleFragment;
import com.unitloadsystem.fragments.Fragments.MenuFragment;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;

public class MainActivity extends Activity {
    SQLiteDatabase db;
    MySQLiteOpenHelper helper;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		FragmentManager fragManagr = getFragmentManager();
		FragmentTransaction fragTransaction = fragManagr.beginTransaction();

		if (savedInstanceState == null) {
			fragTransaction.add(R.id.container, new TitleFragment());
			fragTransaction.add(R.id.container, new MenuFragment());

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
		textTitle.setText(R.string.menu);
	}

	public void btnCalcClick(View v){
        ArrayList aList = getPallets();
        if(aList.size() == 0){
            String msg = getString(R.string.registerPalletMsg);
            Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
            return;
        }

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
}
