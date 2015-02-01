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
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;

public class CalculationResultActivity extends Activity{

	float g_fLength;
	float g_fWidth;
	float g_fHeight;
	float g_fContainerLength;
	float g_fContainerWidth;

	private final float RATE_SIZE = 0.20f;

    private final int TITLE_LENGTH = 150;
    private final int g_iNumberOfRowDivision = 2;
    private final int g_iNumberOfColDivision = 2;
	int g_iLengthOfRowDivision;
	int g_iLengthOfColDivision;
    private final int LEFT_MARGIN = 50;
    private final int TOP_MARGIN = 80;
    private final int TEXT_MARGIN = 50;
    private final int INTERVAL = 2;
    private final int CNTR_MARGIN = 5;
    private final int TEXT_SIZE = 30;
    private final int LINE_SIZE = 2;
	
	int g_iBorderColor = Color.BLACK;
	int g_iPalletColor = Color.BLUE;
    int g_iBoxColor = Color.GREEN;
    int g_iTextColor = Color.BLACK;
	
    Bundle g_Layouts;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		MyView vw = new MyView(this);
		setContentView(vw);
		
		Intent intent = getIntent();
		g_fLength = intent.getFloatExtra("Length", 0f);
		g_fWidth = intent.getFloatExtra("Width", 0f);
		g_fHeight = intent.getFloatExtra("Height", 0f);
//		g_fContainerLength = (float)(intent.getIntExtra("ContainerLength", 0));
//		g_fContainerWidth = (float)(intent.getIntExtra("ContainerWidth", 0));
//		g_fSplitStackShare = Math.round(intent.getDoubleExtra("SplitStackShare", -1) * 100);
//		g_fPinWheelStacktShare = Math.round(intent.getDoubleExtra("PinWheelStackShare", -1) * 100);
//		g_bSplitStackResult = intent.getBundleExtra("SplitStack");
//		g_bPinWheelStackResult = intent.getBundleExtra("PinWheelStack");

        g_Layouts = intent.getBundleExtra("Layouts");
	}
	
	class MyView extends View {
		public MyView(Context context) {
			super(context);
		}

		public void onDraw(Canvas canvas) {
			canvas.drawColor(Color.WHITE);
			Paint paint = new Paint();

			DisplayMetrics metrics = new DisplayMetrics();
			getWindowManager().getDefaultDisplay().getMetrics(metrics);
			int screenWidth = metrics.widthPixels;
			int screenHeight = metrics.heightPixels - TITLE_LENGTH;
			
			g_iLengthOfRowDivision = (int)screenHeight / g_iNumberOfRowDivision;
			g_iLengthOfColDivision = (int)screenWidth / g_iNumberOfColDivision;
			
			paint.setColor(g_iBorderColor);
			paint.setTextSize(TEXT_SIZE);
			paint.setStrokeWidth(LINE_SIZE);

            for(int i=0; i<g_Layouts.size(); i++){
                Bundle layout = g_Layouts.getBundle("Layout" + i);

                float fContainerWidth = (float)layout.getInt("ContainerWidth");
                float fContainerLength = (float)layout.getInt("ContainerLength");

                Bundle bunSplitStack = layout.getBundle("SplitStack");
                Bundle bunPinWheelStack = layout.getBundle("PinWheelStack");

                ArrayList<PalletViewBean> split = bunSplitStack.getParcelableArrayList("Layout");
                ArrayList<PalletViewBean> pinWheel = bunPinWheelStack.getParcelableArrayList("Layout");

                for(int j=0; j<2; j++){
                    paint.setColor(g_iTextColor);
                    String sText = "";

                    if(j%2 == 0){
                        sText = "Split Stack Rule : " + (j + 1) + ". " + split.size() + "boxes / 활용율 " + Math.round(bunSplitStack.getDouble("SplitStackShare")) * 100 + "% / 1단 적재 Layout";
                    }else{
                        sText = "Pinwheel Stack Rule : " + (j + 1) + ". " + pinWheel.size() + "boxes / 활용율 " + Math.round(bunPinWheelStack.getDouble("PinWheelStackShare")) * 100 + "% / 1단 적재 Layout";
                    }

                    canvas.drawText(sText, LEFT_MARGIN, TEXT_MARGIN + g_iLengthOfRowDivision * j, paint);

                    Rect rect = new Rect(LEFT_MARGIN - CNTR_MARGIN, TOP_MARGIN - CNTR_MARGIN + g_iLengthOfRowDivision * j,
                            (int)(RATE_SIZE * fContainerWidth + LEFT_MARGIN + CNTR_MARGIN),
                            (int)(RATE_SIZE * fContainerLength + TOP_MARGIN + CNTR_MARGIN + g_iLengthOfRowDivision * j));
                    paint.setColor(g_iPalletColor);
                    canvas.drawRect(rect, paint);
                }

                paint.setColor(g_iBoxColor);

                float fWidth = RATE_SIZE * g_fWidth;
                float fLength = RATE_SIZE * g_fLength;

                // Split Stack.. Block Stack
                drawSplitStack(canvas, paint, split, TOP_MARGIN, LEFT_MARGIN, fWidth, fLength, RATE_SIZE, INTERVAL);

			    // Pin Wheel Stack
                int iPinWheelRowCount = bunPinWheelStack.getInt("PinWheelStackRowCount");
                int iPinWheelColCount = bunPinWheelStack.getInt("PinWheelStackColCount");
                drawPinWheel(canvas, paint, pinWheel, TOP_MARGIN + g_iLengthOfRowDivision, LEFT_MARGIN, fWidth, fLength, RATE_SIZE, INTERVAL, iPinWheelRowCount, iPinWheelColCount);
                break;
            }
			
//			ArrayList<PalletViewBean> split = g_bSplitStackResult.getParcelableArrayList("Layout");
//			ArrayList<PalletViewBean> pinWheel = g_bPinWheelStackResult.getParcelableArrayList("Layout");

//			for(int i=0; i<g_iNumberOfRowDivision; i++){
//				paint.setColor(g_iTextColor);
//				String sText = "";
//
//				if(i%2 == 0){
//					sText = "Split Stack Rule : " + (i + 1) + ". " + split.size() + "boxes / 활용율 " + g_fSplitStackShare + "% / 1단 적재 Layout";
//				}else{
//					sText = "Pinwheel Stack Rule : " + (i + 1) + ". " + pinWheel.size() + "boxes / 활용율 " + g_fPinWheelStacktShare + "% / 1단 적재 Layout";
//				}
//
//				canvas.drawText(sText, g_iLeftMargin, g_iTextMargin + g_iLengthOfRowDivision * i, paint);
//
//                Rect rect = new Rect(g_iLeftMargin - g_iCntrMargin, g_iTopMargin - g_iCntrMargin + g_iLengthOfRowDivision * i,
//						(int)(g_fRateSize * g_fContainerWidth + g_iLeftMargin + g_iCntrMargin),
//						(int)(g_fRateSize * g_fContainerLength + g_iTopMargin + g_iCntrMargin + g_iLengthOfRowDivision * i));
//				paint.setColor(g_iPalletColor);
//				canvas.drawRect(rect, paint);
//			}
			
//			paint.setColor(g_iBoxColor);
//
//            float fWidth = g_fRateSize * g_fWidth;
//            float fLength = g_fRateSize * g_fLength;
//
//            // Split Stack.. Block Stack
//            drawSplitStack(canvas, paint, split, g_iTopMargin, g_iLeftMargin, fWidth, fLength, g_fRateSize, g_iInterval);
//
////			// Pin Wheel Stack
//            drawPinWheel(canvas, paint, pinWheel, g_iTopMargin + g_iLengthOfRowDivision, g_iLeftMargin, fWidth, fLength, g_fRateSize, g_iInterval, g_iPinWheelRowCount, g_iPinWheelColCount);
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
