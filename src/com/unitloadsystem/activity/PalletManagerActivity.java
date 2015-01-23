package com.unitloadsystem.activity;

import java.util.ArrayList;
import java.util.HashMap;

import com.unitloadsystem.db.PalletDB;
import com.unitloadsystem.fragments.Fragments.TitleFragment;
import com.unitloadsystem.fragments.Fragments.PalletManagerFragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
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

	Activity thisAct = this;
	private PalletDB dbAdapter;
    private static final String TAG = "PalletsDbAdapter";
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		FragmentManager fragManagr = getFragmentManager();
		FragmentTransaction fragTransaction = fragManagr.beginTransaction();
		
		if (savedInstanceState == null) {
			fragTransaction.add(R.id.container, new TitleFragment());
			fragTransaction.add(R.id.container, new PalletManagerFragment());
			
			fragTransaction.commit();
		}
		
		// SQLite
		Log.d(TAG, "Database Test : onCreate()");
		dbAdapter = new PalletDB(this);
		dbAdapter.open();
		
		Button bt = (Button) findViewById(R.id.addPallet);
		bt.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				dbAdapter.createNote("title", "body");
			}
		});
		
		Button btnGetPallet = (Button) findViewById(R.id.getPallet);
		btnGetPallet.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				Cursor result = dbAdapter.fetchAllNotes();
				result.moveToFirst();
				while(!result.isAfterLast()){
					String title = result.getString(1);
					String body = result.getString(2);
					TextView tvName = (TextView)findViewById(R.id.palletName);
					TextView tvWidth = (TextView)findViewById(R.id.palletWidth);
					tvName.setText(title);
					tvWidth.setText(body);
				}
				
				result.close();
			}
		});
		
//		listRefresh();
	}
	
	private void listRefresh(){
		try {
			HashMap<String, String> hMap = new HashMap<String, String>();
			ArrayList aList = new ArrayList();
			
			android.widget.ListAdapter la = new ListAdapter(this, R.id.palletListView, aList);
			ListView listView = (ListView) findViewById(R.id.palletListView);
			listView.setAdapter(la);
			
			listView.setOnItemClickListener(mItemClickListener);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
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
						Toast.makeText(getApplicationContext(), "이미 마감 되었습니다.", Toast.LENGTH_SHORT).show();
						return;
					}
					
					final AlertDialog.Builder adb = new AlertDialog.Builder(PalletManagerActivity.this);
                    adb.setTitle("견적 정보를 입력하시오.");
                    adb.setView(linear);
                    adb.setPositiveButton("확인", new DialogInterface.OnClickListener() {
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
                    adb.setNegativeButton("취소", new DialogInterface.OnClickListener() {
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
					// 2개의 텍스트뷰를 셋팅해준다.
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
//					// 셋팅한 텍스트뷰의 텍스트에 이름과 전화번호를 넣어준다.
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
