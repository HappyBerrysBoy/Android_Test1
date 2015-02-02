package com.unitloadsystem.activity;

import java.util.ArrayList;

import com.unitloadsystem.beans.PalletViewBean;

import android.app.Activity;
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
import android.widget.LinearLayout;

public class CalculationResultActivity extends Activity{

	float g_fLength;        // Box 세로
	float g_fWidth;         // Box 가로
	float g_fHeight;        // Box 높이
    float g_fBoxWeight;     // Box 무게
    int g_iBoxQuantity;     // Box 수량
    int g_iBoxLayers;       // Box 단적 수

	private final float RATE_SIZE = 0.30f;          // 사이즈 축소율
    private final int LEFT_MARGIN = 50;             // 좌측 마진

    private final int TOP_INIT_MARGIN = 90;         // 최초 상단 마진
    private final int BIG_TEXT_MARGIN = 120;        // 큰 텍스트 마진

    private final int TEXT_MARGIN = 70;             // Text와 Text 사이 간격
    private final int TEXT_LAYOUT_INTERVAL = 40;    // Text와 Pallet Layout 사이 간격
    private final int LINE_INTERVAL = 50;           // 라인 좌측 및 우측 간격
    private final int INTERVAL = 2;
    private final int CNTR_MARGIN = 5;
    private final int LINE_SIZE = 2;

    private final int BIG_TEXT_SIZE = 80;           // 큰 텍스트 크기
    private final int TEXT_SIZE = 50;               // 일반 텍스크 크기

	// 색깔
	private final int BORDER_COLOR = Color.BLACK;
	private final int PALLET_COLOR = Color.BLUE;
    private final int BOX_COLOR = Color.GREEN;
    private final int TEXT_COLOR = Color.BLACK;
    private final int LINE_COLOR = Color.GRAY;

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
        g_fBoxWeight = intent.getFloatExtra("BoxWeight", 0f);
        g_iBoxQuantity = intent.getIntExtra("BoxQuantity", 0);
        g_iBoxLayers = intent.getIntExtra("BoxLayer", 0);
	}
	
	class MyView extends View {
        int screenWidth;
        int totalTop = 0;
        boolean bFirst = true;

        private final String STR_PALLET_SIZE = getString(R.string.palletSize);
        private final String STR_STACK_RULE = getString(R.string.stackRule);
        private final String STR_OCCUPANCY_RATE = getString(R.string.occupancyRate);
        private final String STR_BOXES = getString(R.string.boxes);

        private final String STACK_HEIGHT = getString(R.string.stackHeight);
        private final String TOTAL_WEIGHT = getString(R.string.totalWeight);
        private final String NEED_PALLET = getString(R.string.needPalletCnt);
        private final String UNIT_CNT = getString(R.string.unitCnt);

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

                // Pallet Info Text
                sText = STR_PALLET_SIZE + "(" + palletName + ") ";
                drawText(canvas, paint, sText, iTop, LEFT_MARGIN, TEXT_COLOR, BIG_TEXT_SIZE, true);

                iTop += TEXT_MARGIN;
                sText = STACK_HEIGHT + " : " + (g_fHeight * g_iBoxLayers) + "mm";
                drawText(canvas, paint, sText, iTop, LEFT_MARGIN, TEXT_COLOR, TEXT_SIZE, false);

                iTop += TEXT_MARGIN;
                sText = TOTAL_WEIGHT + " : " + (g_fBoxWeight * g_iBoxQuantity) + "kg";
                drawText(canvas, paint, sText, iTop, LEFT_MARGIN, TEXT_COLOR, TEXT_SIZE, false);

                // Split Stack Drawing..
                iTop += TEXT_MARGIN * 2;
                sText = "<" + STR_STACK_RULE + "1> " + split.size() + STR_BOXES + "/ " + STR_OCCUPANCY_RATE + Math.round(layout.getDouble("SplitStackShare") * 100) + "%";
                drawText(canvas, paint, sText, iTop, LEFT_MARGIN, TEXT_COLOR, TEXT_SIZE, false);

                iTop += TEXT_MARGIN;
                sText = NEED_PALLET + " : " + (int)(Math.ceil((double)g_iBoxQuantity / (split.size() * g_iBoxLayers))) + UNIT_CNT;
                drawText(canvas, paint, sText, iTop, LEFT_MARGIN, TEXT_COLOR, TEXT_SIZE, false);

//                iTop += TEXT_MARGIN;
//                sText = "적재된 팔레트 무게(팔렛트 무게 제외) : " + (split.size() * g_iBoxLayers * g_fBoxWeight);
//                drawText(canvas, paint, sText, iTop, LEFT_MARGIN, TEXT_COLOR, TEXT_SIZE, false);

                iTop += TEXT_LAYOUT_INTERVAL;
                drawPallet(canvas, paint, iTop, fContainerWidth, fContainerLength, PALLET_COLOR);
                drawSplitStack(canvas, paint, split, iTop, LEFT_MARGIN, fWidth, fLength, RATE_SIZE, INTERVAL, BOX_COLOR);

                iTop += (int)(RATE_SIZE * fContainerLength) + TEXT_MARGIN;

                // Pin Wheel Stack Drawing...
                sText = "<" + STR_STACK_RULE + "2> " + pinWheel.size() + STR_BOXES + "/ " + STR_OCCUPANCY_RATE + Math.round(layout.getDouble("PinWheelStackShare") * 100) + "%";
                drawText(canvas, paint, sText, iTop, LEFT_MARGIN, TEXT_COLOR, TEXT_SIZE, false);

                iTop += TEXT_MARGIN;
                sText = NEED_PALLET + " : " + (int)(Math.ceil((double)g_iBoxQuantity / (pinWheel.size() * g_iBoxLayers))) + UNIT_CNT;
                drawText(canvas, paint, sText, iTop, LEFT_MARGIN, TEXT_COLOR, TEXT_SIZE, false);

//                iTop += TEXT_MARGIN;
//                sText = "적재된 팔레트 무게(팔렛트 무게 제외) : " + (pinWheel.size() * g_iBoxLayers * g_fBoxWeight);
//                drawText(canvas, paint, sText, iTop, LEFT_MARGIN, TEXT_COLOR, TEXT_SIZE, false);

                iTop += TEXT_LAYOUT_INTERVAL;
                drawPallet(canvas, paint, iTop, fContainerWidth, fContainerLength, PALLET_COLOR);

                int iPinWheelRowCount = layout.getInt("PinWheelStackRowCount");
                int iPinWheelColCount = layout.getInt("PinWheelStackColCount");
                drawPinWheel(canvas, paint, pinWheel, iTop, LEFT_MARGIN, fWidth, fLength, RATE_SIZE, INTERVAL, iPinWheelRowCount, iPinWheelColCount, BOX_COLOR);

                paint.setColor(LINE_COLOR);
                iTop += (int)(RATE_SIZE * fContainerLength) + TEXT_LAYOUT_INTERVAL;
                canvas.drawLine(LINE_INTERVAL, iTop, screenWidth - LINE_INTERVAL, iTop, paint);
                iTop += BIG_TEXT_MARGIN;
                paint.setColor(TEXT_COLOR);
            }

            totalTop = iTop;
		}

        protected  void onMeasure(int a, int b){
            if(bFirst || totalTop == 0){
                bFirst = false;
                setMeasuredDimension(screenWidth, 3000);
            }else{
                setMeasuredDimension(screenWidth, totalTop);
            }
        }

        private void drawText(Canvas canvas, Paint paint, String text, int top, int left, int color, int textSize, boolean isBold){
            paint.setTextSize(textSize);
            paint.setColor(color);
            if(isBold){
                paint.setTypeface(Typeface.create((String)null, Typeface.BOLD));
            }else{
                paint.setTypeface(Typeface.create((String)null, Typeface.NORMAL));
            }
            canvas.drawText(text, left, top, paint);
        }

        private void drawPallet(Canvas canvas, Paint paint, int top, float containerWidth, float containerHeight, int color){
            Rect splitRect = new Rect(LEFT_MARGIN - CNTR_MARGIN, top - CNTR_MARGIN,
                    (int)(RATE_SIZE * containerWidth + LEFT_MARGIN + CNTR_MARGIN),
                    (int)(RATE_SIZE * containerHeight + top + CNTR_MARGIN));
            paint.setColor(color);
            canvas.drawRect(splitRect, paint);
        }

        private void drawSplitStack(Canvas canvas, Paint paint, ArrayList<PalletViewBean> stackView, int top, int left, float fWidth, float fLength, float rate, int interval, int color){
            paint.setColor(color);
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

        private void drawPinWheel(Canvas canvas, Paint paint, ArrayList<PalletViewBean> stackView, int top, int left, float fWidth, float fLength, float rate, int interval, int row, int col, int color){
            paint.setColor(color);
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
