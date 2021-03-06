package com.unitloadsystem.activitys;

import java.util.ArrayList;
import java.util.HashMap;
import com.unitloadsystem.db.MySQLiteOpenHelper;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class PalletManagerActivity extends Activity{

    Button bBtn;
	Activity thisAct = this;
    SQLiteDatabase db;
    MySQLiteOpenHelper helper;

    ListAdapter la;
    ListView listView;

    String strSelectedPallet;
    int g_iBtnID;
    int g_iArrayID;

    View.OnClickListener ocl = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            TextView tv = (TextView) v;
            tv.setBackgroundColor(getResources().getColor(R.color.keypadSendButtonBack));
            strSelectedPallet = (String)tv.getText();
        }
    };

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
//		setContentView(R.layout.activity_main);
		setContentView(R.layout.palletmanager_layout);

//		FragmentManager fragManagr = getFragmentManager();
//		FragmentTransaction fragTransaction = fragManagr.beginTransaction();
//
//		if (savedInstanceState == null) {
//			fragTransaction.add(R.id.container, new PalletManagerFragment());
//			fragTransaction.commit();
//		}

        helper = new MySQLiteOpenHelper(getApplicationContext(), "pallet.db", null, 1);

        listRefresh();
	}

    public void btnInputNum(View v){
        bBtn = (Button) findViewById(v.getId());

        Intent intent = new Intent(getApplicationContext(), KeyPadActivity.class);
        intent.putExtra("BtnID", v.getId());
        intent.putExtra("TextIn", bBtn.getText());
        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivityForResult(intent, v.getId());
    }

    public void btnDimenPallet(View v){
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

    protected void onActivityResult (int requestCode, int resultCode, Intent data) {

        if (resultCode == RESULT_OK) {
            bBtn = (Button) findViewById(requestCode);
            bBtn.setText(data.getStringExtra("Value"));
        }
    }

    public ArrayList getPallets(){
        // 1) db의 데이터를 읽어와서, 2) 결과 저장, 3)해당 데이터를 꺼내 사용

        db = helper.getReadableDatabase(); // db객체를 얻어온다. 읽기 전용
        Cursor c = db.query("palletdb", null, null, null, null, null, null);

        /*
         * 위 결과는 select * from student 가 된다. Cursor는 DB결과를 저장한다. public Cursor
         * query (String table, String[] columns, String selection, String[]
         * selectionArgs, String groupBy, String having, String orderBy)
         */

        ArrayList aResult = new ArrayList();

        while (c.moveToNext()) {
            // c의 int가져와라 ( c의 컬럼 중 id) 인 것의 형태이다.
            String name = c.getString(c.getColumnIndex("name"));
            int width = c.getInt(c.getColumnIndex("width"));
            int height = c.getInt(c.getColumnIndex("height"));
            String unit = c.getString(c.getColumnIndex("unit"));
            Log.i("db", "name: " + name + ", width : " + width + ", height : " + height + ", unit : " + unit);

            HashMap hMap = new HashMap();
            hMap.put("name", name);
            hMap.put("width", width);
            hMap.put("height", height);
            hMap.put("unit", unit);
            aResult.add(hMap);
        }

        return aResult;
    }

    public void addPallet(View v){
        String widthBtn = (String)((Button)findViewById(R.id.widthPalletforAdd)).getText();
        String heightBtn = (String)((Button)findViewById(R.id.heightPalletforAdd)).getText();
        String dimenBtn = (String)((Button)findViewById(R.id.dimentPalletforAdd)).getText();

        if(widthBtn.equals("") || Float.parseFloat(widthBtn) < 10){
            Toast.makeText(getApplicationContext(), "Please Input Width or Input Width over 100mm", Toast.LENGTH_SHORT).show();
            return;
        }

        if(heightBtn.equals("") || Float.parseFloat(heightBtn) < 10){
            Toast.makeText(getApplicationContext(), "Please Input Height or Input Height over 100mm", Toast.LENGTH_SHORT).show();
            return;
        }

        db = helper.getWritableDatabase();

        if(Float.parseFloat(widthBtn) < Float.parseFloat(heightBtn)){
            String strTemp = heightBtn;
            heightBtn = widthBtn;
            widthBtn = strTemp;
        }

        ContentValues values = new ContentValues();
        values.put("name", widthBtn + "X" + heightBtn);
        values.put("width", Integer.parseInt(widthBtn));
        values.put("height", Integer.parseInt(heightBtn));
        values.put("unit", dimenBtn);

        db.insert("palletdb", null, values);

        listRefresh();
    }

    // update
//    public void update (String name, int age) {
//        db = helper.getWritableDatabase(); //db 객체를 얻어온다. 쓰기가능
//
//        ContentValues values = new ContentValues();
//        values.put("age", age);    //age 값을 수정
//        db.update("student", values, "name=?", new String[]{name});
//        /*
//         * new String[] {name} 이런 간략화 형태가 자바에서 가능하다
//         * 당연하지만, 별도로 String[] asdf = {name} 후 사용하는 것도 동일한 결과가 나온다.
//         */
//
//        /*
//         * public int update (String table,
//         * ContentValues values, String whereClause, String[] whereArgs)
//         */
//    }

    public void delPallet(View v){
        if(strSelectedPallet.equals(null) || strSelectedPallet.equals(""))
            return;

        Button widthBtn = (Button)findViewById(R.id.widthPalletforAdd);
        Button heightBtn = (Button)findViewById(R.id.heightPalletforAdd);
        db = helper.getWritableDatabase();

        db.delete("palletdb", "name=?", new String[]{strSelectedPallet});
        listRefresh();
    }

    public void listRefresh(){
        ArrayList aList = getPallets();

        la = new ListAdapter(this, R.id.palletListView, aList);
        listView = (ListView) findViewById(R.id.palletListView);
        listView.setAdapter(la);
    }

    private class ListAdapter extends ArrayAdapter<HashMap> {
        LayoutInflater inflater;
        private ArrayList<HashMap> items;

        public ListAdapter(Context context, int textViewResourceId, ArrayList<HashMap> items) {
            super(context, textViewResourceId, items);
            inflater = (LayoutInflater) thisAct.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            this.items = items;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.pallet_code, parent, false);

                HashMap hPallet = items.get(position);

                if (hPallet != null) {
                    TextView tvPalletName = (TextView) convertView.findViewById(R.id.palletCodeName);
                    TextView tvPalletWidth = (TextView) convertView.findViewById(R.id.palletCodeWidth);
                    TextView tvPalletHeight = (TextView) convertView.findViewById(R.id.palletCodeHeight);
                    TextView tvPalletUnit = (TextView) convertView.findViewById(R.id.palletCodeUnit);

                    tvPalletName.setText((String) hPallet.get("name"));
                    tvPalletWidth.setText(String.valueOf((Integer)hPallet.get("width")));
                    tvPalletHeight.setText(String.valueOf((Integer)hPallet.get("height")));
                    tvPalletUnit.setText((String) hPallet.get("unit"));

                    tvPalletName.setOnClickListener(ocl);
                    }
                }

            return convertView;
        }
    }

    public void btnBack(View v){
        this.finish();
    }
}
