package com.unitloadsystem.activity;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;

import com.unitloadsystem.db.MySQLiteOpenHelper;
import com.unitloadsystem.db.PalletDB;
import com.unitloadsystem.fragments.Fragments.TitleFragment;
import com.unitloadsystem.fragments.Fragments.PalletManagerFragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class PalletManagerActivity extends Activity{

    Button bBtn;
	Activity thisAct = this;
    SQLiteDatabase db;
    MySQLiteOpenHelper helper;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		FragmentManager fragManagr = getFragmentManager();
		FragmentTransaction fragTransaction = fragManagr.beginTransaction();
		
		if (savedInstanceState == null) {
			fragTransaction.add(R.id.container, new PalletManagerFragment());
			fragTransaction.commit();
		}

        helper = new MySQLiteOpenHelper(getApplicationContext(), "pallet.db", null, 1);


        getPallets();
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

    public void getPallets(){
        // 1) db의 데이터를 읽어와서, 2) 결과 저장, 3)해당 데이터를 꺼내 사용

        db = helper.getReadableDatabase(); // db객체를 얻어온다. 읽기 전용
        Cursor c = db.query("palletdb", null, null, null, null, null, null);

        /*
         * 위 결과는 select * from student 가 된다. Cursor는 DB결과를 저장한다. public Cursor
         * query (String table, String[] columns, String selection, String[]
         * selectionArgs, String groupBy, String having, String orderBy)
         */

        while (c.moveToNext()) {
            // c의 int가져와라 ( c의 컬럼 중 id) 인 것의 형태이다.
            String name = c.getString(c.getColumnIndex("name"));
            int width = c.getInt(c.getColumnIndex("width"));
            int height = c.getInt(c.getColumnIndex("height"));
            String unit = c.getString(c.getColumnIndex("unit"));
            Log.i("db", "name: " + name + ", width : " + width + ", height : " + height
                    + ", unit : " + unit);
        }
    }

    public void addPallet(View v){
        Button widthBtn = (Button)findViewById(R.id.widthPalletforAdd);
        Button heightBtn = (Button)findViewById(R.id.heightPalletforAdd);
        db = helper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("name", widthBtn.getText() + "X" + heightBtn.getText());
        values.put("width", Integer.parseInt((String)widthBtn.getText()));
        values.put("height", Integer.parseInt((String)heightBtn.getText()));
        values.put("unit", "cm");

        db.insert("palletdb", null, values);

        getPallets();
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
//    [출처] Android - SQLite 활용 예제1|작성자 장스

    public void delPallet(View v){
        Button widthBtn = (Button)findViewById(R.id.widthPalletforAdd);
        Button heightBtn = (Button)findViewById(R.id.heightPalletforAdd);
        db = helper.getWritableDatabase();

        db.delete("palletdb", "name=?", new String[]{widthBtn.getText() + "X" + heightBtn.getText()});
    }
	
	AdapterView.OnItemClickListener mItemClickListener =
			new AdapterView.OnItemClickListener() {
				public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
					
					final LinearLayout linear = (LinearLayout)View.inflate(PalletManagerActivity.this, R.layout.palletitem, null);
					
					ListView lv = (ListView)parent;
					final LinearLayout ll = (LinearLayout)lv.getChildAt(position);
					final TextView tvItem = (TextView)ll.getChildAt(0);
					final TextView tvAssign = (TextView)ll.getChildAt(2);
					final TextView tvQuantity = (TextView)ll.getChildAt(4);
					final TextView tvUnitPrice = (TextView)ll.getChildAt(6);
					final TextView tvTotalPrice = (TextView)ll.getChildAt(8);
					final TextView tvMagam = (TextView)ll.getChildAt(12);
					
					if(!tvMagam.getText().toString().trim().equals("")){
						Toast.makeText(getApplicationContext(), "�̹� ���� �Ǿ���ϴ�.", Toast.LENGTH_SHORT).show();
						return;
					}
					
					final AlertDialog.Builder adb = new AlertDialog.Builder(PalletManagerActivity.this);
                    adb.setTitle("���� ������ �Է��Ͻÿ�.");
                    adb.setView(linear);
                    adb.setPositiveButton("Ȯ��", new DialogInterface.OnClickListener() {
            			public void onClick(DialogInterface dialog, int whichButton) {
//            				final RadioGroup rg = (RadioGroup)linear.findViewById(R.id.yesnoGroup);
//            				final RadioButton yesRadio = (RadioButton)linear.findViewById(R.id.yesCheck);
//            				final RadioButton noRadio = (RadioButton)linear.findViewById(R.id.noCheck);
//            				final EditText ev = (EditText)linear.findViewById(R.id.quotationPrice);
//            				
//            				if(yesRadio.getId() == rg.getCheckedRadioButtonId())
//            					tvAssign.setText("Y");
//            				else
//            					tvAssign.setText("N");
//            				
//            				tvUnitPrice.setText(ev.getText());
//            				
//            				if(ev.getText().toString().length() > 0)
//            					tvTotalPrice.setText(String.valueOf(Integer.parseInt(ev.getText().toString()) * Integer.parseInt(tvQuantity.getText().toString())));
//            				else
//            					tvTotalPrice.setText("0");
            			}
            		});
                    adb.setNegativeButton("���", new DialogInterface.OnClickListener() {
            			public void onClick(DialogInterface dialog, int whichButton) {
            			}
            		});
                    adb.show();
				}
			};



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
				convertView = inflater.inflate(R.layout.palletitem, parent, false);
				
				HashMap hItem = items.get(position);

				if (hItem != null) {
//					TextView tvName = (TextView) convertView.findViewById(R.id.productName);
//					TextView tvAssign = (TextView) convertView.findViewById(R.id.assign);
//					TextView tvQuantity = (TextView) convertView.findViewById(R.id.productQuantity);
//					TextView tvPrice = (TextView) convertView.findViewById(R.id.productPrice);
//					TextView tvSubtotal = (TextView) convertView.findViewById(R.id.subTotal);
//					TextView tvItemcode = (TextView) convertView.findViewById(R.id.itemCode);
//					TextView tvItemtype = (TextView) convertView.findViewById(R.id.itemType);
//					TextView tvRemark = (TextView) convertView.findViewById(R.id.pqremark);
//					TextView tvMagam = (TextView) convertView.findViewById(R.id.quotationMagam);
//
//					String sItemName = hItem.get("BI_ITEMNAME") == null ? "" : (String) hItem.get("BI_ITEMNAME");
//					String sAssign = hItem.get("PQ_ARRANGE_CHK") == null ? "" : (String) hItem.get("PQ_ARRANGE_CHK");
//					String sItemQuantity = hItem.get("PQ_QUOT_QTY") == null ? "   " : (String) hItem.get("PQ_QUOT_QTY");
//					String sPrice = hItem.get("PQ_PRICE_LOC") == null ? "" : (String) hItem.get("PQ_PRICE_LOC");
//					String sSubtotal = "";
//					String sItemcode = hItem.get("BI_ITEMCODE") == null ? "" : (String) hItem.get("BI_ITEMCODE");
//					String sItemtype = hItem.get("BI_ITEMTYPE") == null ? "" : (String) hItem.get("BI_ITEMTYPE");
//					String sRemark = hItem.get("PQ_REMARK") == null ? "" : (String) hItem.get("PQ_REMARK");
//					String sMagam = hItem.get("MAGAM") == null ? "" : (String) hItem.get("MAGAM");
//					
//					if(!sAssign.equals("") && !sPrice.equals("")){
//						sSubtotal = String.valueOf(Integer.parseInt(sItemQuantity) * Integer.parseInt(sPrice));
//					}else{
//						sSubtotal = "0";
//					}
//					
//					if(sPrice.equals(""))
//						sPrice = "0";
//					
//					tvName.setText(sItemName);
//					tvAssign.setText(sAssign);
//					tvQuantity.setText(sItemQuantity);
//					tvPrice.setText(sPrice);
//					tvSubtotal.setText(sSubtotal);
//					tvItemcode.setText(sItemcode);
//					tvItemtype.setText(sItemtype);
//					tvRemark.setText(sRemark);
//					tvMagam.setText(sMagam);
//
//					tvName.setOnClickListener(ocl);
				}
			}

			return convertView;
		}
	}
}
