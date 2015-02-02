package com.unitloadsystem.activity;

import java.util.ArrayList;

import com.unitloadsystem.beans.PalletViewBean;
import com.unitloadsystem.fragments.Fragments;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;

public class CalculationResultActivity extends Activity{

	float g_fLength;
	float g_fWidth;
	float g_fHeight;

	private final float RATE_SIZE = 0.30f;

    private final int TOP_INIT_MARGIN = 90;
    private final int TITLE_LENGTH = 150;
    private final int LEFT_MARGIN = 50;
    private final int TOP_MARGIN = 80;
    private final int BIG_TEST_MARGIN = 90;
    private final int TEXT_MARGIN = 70;
    private final int TEXT_LAYOUT_INTERVAL = 60;
    private final int INTERVAL = 2;
    private final int CNTR_MARGIN = 5;
    private final int TEXT_SIZE = 30;
    private final int LINE_SIZE = 2;
	
	private final int BORDER_COLOR = Color.BLACK;
	private final int PALLET_COLOR = Color.BLUE;
    private final int BOX_COLOR = Color.GREEN;
    private final int TEXT_COLOR = Color.BLACK;

    Bundle g_Layouts;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        setContentView(R.layout.calculationresult_layout);

        LinearLayout sv = (LinearLayout)findViewById(R.id.layoutResult);
        MyView vw = new MyView(this);
        sv.addView(vw);

		Intent intent = getIntent();
		g_fLength = intent.getFloatExtra("Length", 0f);
		g_fWidth = intent.getFloatExtra("Width", 0f);
		g_fHeight = intent.getFloatExtra("Height", 0f);
        g_Layouts = intent.getBundleExtra("Layouts");
	}
	
	class MyView extends View {
        int screenWidth;
        int totalTop = 0;
        boolean bFirst = true;

        private final String STR_PALLET_SIZE = getString(R.string.palletSize);
        private final String STR_STACK_RULE = getString(R.string.stackRule);
        private final String STR_OCCUPANCY_RATE = getString(R.string.occupancyRate);
        private final String STR_BOXES = getString(R.string.boxes);

		public MyView(Context context) {
			super(context);
            DisplayMetrics metrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(metrics);
			screenWidth = metrics.widthPixels;
		}

		public void onDraw(Canvas canvas) {
			canvas.drawColor(Color.WHITE);
			Paint paint = new Paint();

			paint.setColor(BORDER_COLOR);
			paint.setTextSize(TEXT_SIZE);
			paint.setStrokeWidth(LINE_SIZE);

            float fWidth = RATE_SIZE * g_fWidth;
            float fLength = RATE_SIZE * g_fLength;

            String sText = "";
            int iTop = TOP_INIT_MARGIN;;

            for(int i=0; i<g_Layouts.size(); i++){
                Bundle layout = g_Layouts.getBundle("Layout" + i);

                float fContainerWidth = (float)layout.getInt("ContainerWidth");
                float fContainerLength = (float)layout.getInt("ContainerLength");

                Bundle bunSplitStack = layout.getBundle("SplitStack");
                Bundle bunPinWheelStack = layout.getBundle("PinWheelStack");

                ArrayList<PalletViewBean> split = bunSplitStack.getParcelableArrayList("Layout");
                ArrayList<PalletViewBean> pinWheel = bunPinWheelStack.getParcelableArrayList("Layout");

                String palletName = layout.getString("PalletName");

                // Split Stack Drawing..
                sText = STR_PALLET_SIZE + "(" + palletName + ") ";
                paint.setTextSize(80);
                paint.setTypeface(Typeface.create((String)null, Typeface.BOLD));
                canvas.drawText(sText, LEFT_MARGIN, iTop, paint);
                iTop += TEXT_LAYOUT_INTERVAL;
                paint.setTextSize(60);
                sText = STR_STACK_RULE + "1 : " + split.size() + STR_BOXES + "/ " + STR_OCCUPANCY_RATE + Math.round(layout.getDouble("SplitStackShare") * 100) + "%";

                paint.setColor(TEXT_COLOR);
                canvas.drawText(sText, LEFT_MARGIN, iTop, paint);

                iTop += TEXT_LAYOUT_INTERVAL;

                drawPallet(canvas, paint, sText, iTop, LEFT_MARGIN, fContainerWidth, fContainerLength);
                paint.setColor(BOX_COLOR);

                drawSplitStack(canvas, paint, split, iTop, LEFT_MARGIN, fWidth, fLength, RATE_SIZE, INTERVAL);

                // Pin Wheel Stack Drawing...
                iTop += (int)(RATE_SIZE * fContainerLength) + TEXT_MARGIN;
                sText = STR_STACK_RULE + "2 : " + split.size() + STR_BOXES + "/ " + STR_OCCUPANCY_RATE + Math.round(layout.getDouble("PinWheelStackShare") * 100) + "%";

                paint.setColor(TEXT_COLOR);
                canvas.drawText(sText, LEFT_MARGIN, iTop, paint);

                iTop += TEXT_LAYOUT_INTERVAL;

                drawPallet(canvas, paint, sText, iTop, LEFT_MARGIN, fContainerWidth, fContainerLength);
                paint.setColor(BOX_COLOR);

                int iPinWheelRowCount = layout.getInt("PinWheelStackRowCount");
                int iPinWheelColCount = layout.getInt("PinWheelStackColCount");
                drawPinWheel(canvas, paint, pinWheel, iTop, LEFT_MARGIN, fWidth, fLength, RATE_SIZE, INTERVAL, iPinWheelRowCount, iPinWheelColCount);

                iTop += (int)(RATE_SIZE * fContainerLength) + BIG_TEST_MARGIN;
                paint.setColor(TEXT_COLOR);

                totalTop = iTop;
            }
		}

        protected  void onMeasure(int a, int b){
            if(bFirst || totalTop == 0){
                bFirst = false;
                setMeasuredDimension(screenWidth, 100);
            }else{
                setMeasuredDimension(screenWidth, totalTop);
            }
        }

        private void drawPallet(Canvas canvas, Paint paint, String text, int top, int left, float containerWidth, float containerHeight){
            Rect splitRect = new Rect(LEFT_MARGIN - CNTR_MARGIN, top - CNTR_MARGIN,
                    (int)(RATE_SIZE * containerWidth + LEFT_MARGIN + CNTR_MARGIN),
                    (int)(RATE_SIZE * containerHeight + top + CNTR_MARGIN));
            paint.setColor(PALLET_COLOR);
            canvas.drawRect(splitRect, paint);
        }

        private void drawSplitStack(Canvas canvas, Paint paint, ArrayList<PalletViewBean> stackView, int top, int left, float fWidth, float fLength, float rate, int interval){
            for(int i=0; i<stackView.size(); i++){
                PalletViewBean box = stackView.get(i);
                if(box.getdirection().equals("H")){
                    canvas.drawRect(new RectF(left + box.getx() * rate,
                            top + box.gety() * rate,
                            fWidth + left + box.getx() * rate - interval,
                            fLength + top + box.gety() * rate - interval), paint);
                }else{
                    canvas.drawRect(new RectF(left + box.getx() * rate,
                            top + box.gety() * rate,
                            fLength + left + box.getx() * rate - interval,
                            fWidth + top + box.gety() * rate - interval), paint);
                }
            }
        }

        private void drawPinWheel(Canvas canvas, Paint paint, ArrayList<PalletViewBean> stackView, int top, int left, float fWidth, float fLength, float rate, int interval, int row, int col){

            for(int i=0; i<row; i++){
                for(int j=0; j<col; j++){
                    canvas.drawRect(new RectF(left + fWidth * i, top + fLength * j,
                            left + fWidth * (i + 1) - interval, top + fLength * (j + 1) - interval), paint);
                    canvas.drawRect(new RectF(left + fWidth * row + fLength * j, top + fWidth * i,
                            left + fWidth * row + fLength * (j + 1) - interval, top + fWidth * (i + 1) - interval), paint);
                    canvas.drawRect(new RectF(left + fLength * j, col * fLength + fWidth * i + top,
                            left + fLength * (j + 1) - interval, top + col * fLength + fWidth * (i + 1) - interval), paint);
                    canvas.drawRect(new RectF(left + fLength * col + fWidth * i, top + fWidth * row + fLength * j,
                            left + fLength * col + fWidth * (i + 1) - interval, top + fWidth * row + fLength * (j + 1) - interval), paint);
                }
            }

            for(int i=row * col * 4; i<stackView.size(); i++){
                PalletViewBean box = stackView.get(i);
                if(box.getdirection().equals("H")){
                    canvas.drawRect(new RectF(left + box.getx() * rate,
                            top + box.gety() * rate,
                            fWidth + left + box.getx() * rate - interval,
                            fLength + top + box.gety() * rate - interval), paint);
                }else{
                    canvas.drawRect(new RectF(left + box.getx() * rate,
                            top + box.gety() * rate,
                            fLength + left + box.getx() * rate - interval,
                            fWidth + top + box.gety() * rate - interval), paint);
                }
            }
        }
	}
}
