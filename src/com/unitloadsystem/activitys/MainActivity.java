package com.unitloadsystem.activitys;

import com.unitloadsystem.db.MySQLiteOpenHelper;
import com.unitloadsystem.db.Pallet;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class MainActivity extends LocalizedActivity {
    SQLiteDatabase db;
    MySQLiteOpenHelper helper;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main_dashboard_layout);

        helper = new MySQLiteOpenHelper(getApplicationContext(), "pallet.db", null, MySQLiteOpenHelper.DB_VERSION);
	}

	@Override
	protected void onResume() {
		super.onResume();
		((TextView) findViewById(R.id.palletCount)).setText(String.valueOf(getTableCount("palletdb")));
		((TextView) findViewById(R.id.boxCount)).setText(String.valueOf(getTableCount("boxdb")));
	}

	private int getTableCount(String tableName) {
		db = helper.getReadableDatabase();
		Cursor cursor = db.rawQuery("select count(*) from " + tableName, null);
		try {
			return cursor.moveToFirst() ? cursor.getInt(0) : 0;
		} finally {
			cursor.close();
		}
	}

	public void btnCalcClick(View v){
        ArrayList aList = getPallets();
        if(aList.size() == 0){
            String msg = getString(R.string.registerPalletMsg);
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
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
		startActivity(intent);
	}

	public void btnBoxManager(View v){
		startActivity(new Intent(this, BoxManagerActivity.class));
	}

	public void btnSettings(View view) {
		startActivity(new Intent(this, SettingsActivity.class));
	}

	public void btnMixedCalcClick(View view) {
		int palletCount = getTableCount("palletdb");
		int boxCount = getTableCount("boxdb");
		if (palletCount < 1 || boxCount < 1) {
			Toast.makeText(this, R.string.mixedNeedsSavedSpecs, Toast.LENGTH_LONG).show();
			return;
		}
		startActivity(new Intent(this, MixedCalculationActivity.class));
	}

    public ArrayList<Pallet> getPallets(){
        db = helper.getReadableDatabase(); // db객체를 얻어온다. 읽기 전용
        Cursor c = db.query("palletdb", null, null, null, null, null, null);

        ArrayList<Pallet> aResult = new ArrayList<Pallet>();

        try {
            int nameIndex = c.getColumnIndexOrThrow("name");
            int widthIndex = c.getColumnIndexOrThrow("width");
            int heightIndex = c.getColumnIndexOrThrow("height");
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
                pallet.setUnit(unit);
                aResult.add(pallet);
            }
        } finally {
            c.close();
        }

        return aResult;
    }
}
