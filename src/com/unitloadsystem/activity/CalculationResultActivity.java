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
	double g_fSplitStackShare;
	double g_fPinWheelStacktShare;
	
	int g_iPinWheelRowCount;
	int g_iPinWheelColCount;
	
	float g_fRateSize = 0.20f;
	
	int g_iTitleLength = 150;
	int g_iNumberOfRowDivision = 2;
	int g_iNumberOfColDivision = 2;
	int g_iLengthOfRowDivision;
	int g_iLengthOfColDivision;
	int g_iLeftMargin = 50;
	int g_iTopMargin = 80;
	int g_iTextMargin =50;
	int g_iInterval = 2;
	int g_iCntrMargin = 5;
	int g_iTextSize = 30;
	int g_iLineSize = 2;
	
	int g_iBorderColor = Color.BLACK;
	int g_iPalletColor = Color.BLUE;
    int g_iBoxColor = Color.GREEN;
    int g_iTextColor = Color.BLACK;
	
	Bundle g_bSplitStackResult;
	Bundle g_bPinWheelStackResult;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		MyView vw = new MyView(this);
		setContentView(vw);
		
		Intent intent = getIntent();
		g_fLength = intent.getFloatExtra("Length", 0f);
		g_fWidth = intent.getFloatExtra("Width", 0f);
		g_fHeight = intent.getFloatExtra("Height", 0f);
		g_fContainerLength = (float)(intent.getIntExtra("ContainerLength", 0));
		g_fContainerWidth = (float)(intent.getIntExtra("ContainerWidth", 0));
		g_fSplitStackShare = Math.round(intent.getDoubleExtra("SplitStackShare", -1) * 100);
		g_fPinWheelStacktShare = Math.round(intent.getDoubleExtra("PinWheelStackShare", -1) * 100);
		g_bSplitStackResult = intent.getBundleExtra("SplitStack");
		g_bPinWheelStackResult = intent.getBundleExtra("PinWheelStack");
		g_iPinWheelRowCount = intent.getIntExtra("PinWheelStackRowCount", 0);
		g_iPinWheelColCount = intent.getIntExtra("PinWheelStackColCount", 0);
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
			int screenHeight = metrics.heightPixels - g_iTitleLength;
			
			g_iLengthOfRowDivision = (int)screenHeight / g_iNumberOfRowDivision;
			g_iLengthOfColDivision = (int)screenWidth / g_iNumberOfColDivision; 
			
			paint.setColor(g_iBorderColor);
			paint.setTextSize(g_iTextSize);
			paint.setStrokeWidth(g_iLineSize);
			
			ArrayList<PalletViewBean> split = g_bSplitStackResult.getParcelableArrayList("Layout");
			ArrayList<PalletViewBean> pinWheel = g_bPinWheelStackResult.getParcelableArrayList("Layout");

			for(int i=0; i<g_iNumberOfRowDivision; i++){
				paint.setColor(g_iTextColor);
				String sText = "";
				
				if(i%2 == 0){
					sText = "Split Stack Rule : " + (i + 1) + ". " + split.size() + "boxes / 활용율 " + g_fSplitStackShare + "% / 1단 적재 Layout";
				}else{
					sText = "Pinwheel Stack Rule : " + (i + 1) + ". " + pinWheel.size() + "boxes / 활용율 " + g_fPinWheelStacktShare + "% / 1단 적재 Layout";
				}
				
				canvas.drawText(sText, g_iLeftMargin, g_iTextMargin + g_iLengthOfRowDivision * i, paint);

                Rect rect = new Rect(g_iLeftMargin - g_iCntrMargin, g_iTopMargin - g_iCntrMargin + g_iLengthOfRowDivision * i,
						(int)(g_fRateSize * g_fContainerWidth + g_iLeftMargin + g_iCntrMargin), 
						(int)(g_fRateSize * g_fContainerLength + g_iTopMargin + g_iCntrMargin + g_iLengthOfRowDivision * i));
				paint.setColor(g_iPalletColor);
				canvas.drawRect(rect, paint);
			}
			
			paint.setColor(g_iBoxColor);
			
			float fWidth = g_fRateSize * g_fWidth;
			float fLength = g_fRateSize * g_fLength;
			
			// Split Stack.. Block Stack
            drawSplitStack(canvas, paint, split, g_iTopMargin, g_iLeftMargin, fWidth, fLength, g_fRateSize, g_iInterval);
//			for(int i=0; i<split.size(); i++){
//				PalletViewBean box = split.get(i);
//				if(box.getdirection().equals("H")){
//					canvas.drawRect(new RectF(g_iLeftMargin + box.getx() * g_fRateSize,
//							g_iTopMargin + box.gety() * g_fRateSize,
//							fWidth + g_iLeftMargin + box.getx() * g_fRateSize - g_iInterval,
//							fLength + g_iTopMargin + box.gety() * g_fRateSize - g_iInterval), paint);
//				}else{
//					canvas.drawRect(new RectF(g_iLeftMargin + box.getx() * g_fRateSize,
//							g_iTopMargin + box.gety() * g_fRateSize,
//							fLength + g_iLeftMargin + box.getx() * g_fRateSize - g_iInterval,
//							fWidth + g_iTopMargin + box.gety() * g_fRateSize - g_iInterval), paint);
//				}
//			}

//			// Pin Wheel Stack
            drawPinWheel(canvas, paint, pinWheel, g_iTopMargin + g_iLengthOfRowDivision, g_iLeftMargin, fWidth, fLength, g_fRateSize, g_iInterval, g_iPinWheelRowCount, g_iPinWheelColCount);
//			for(int i=0; i<g_iPinWheelRowCount; i++){
//				for(int j=0; j<g_iPinWheelColCount; j++){
//					canvas.drawRect(new RectF(g_iLeftMargin + fWidth * i, g_iTopMargin + fLength * j + g_iLengthOfRowDivision,
//							g_iLeftMargin + fWidth * (i + 1) - g_iInterval, g_iTopMargin + fLength * (j + 1) - g_iInterval + g_iLengthOfRowDivision), paint);
//					canvas.drawRect(new RectF(g_iLeftMargin + fWidth * g_iPinWheelRowCount + fLength * j, g_iTopMargin + g_iLengthOfRowDivision + fWidth * i,
//							g_iLeftMargin + fWidth * g_iPinWheelRowCount + fLength * (j + 1) - g_iInterval, g_iTopMargin + fWidth * (i + 1) - g_iInterval + g_iLengthOfRowDivision), paint);
//					canvas.drawRect(new RectF(g_iLeftMargin + fLength * j, g_iPinWheelColCount * fLength + fWidth * i + g_iTopMargin + g_iLengthOfRowDivision,
//							g_iLeftMargin + fLength * (j + 1) - g_iInterval, g_iTopMargin + g_iPinWheelColCount * fLength + fWidth * (i + 1) + g_iLengthOfRowDivision - g_iInterval), paint);
//					canvas.drawRect(new RectF(g_iLeftMargin + fLength * g_iPinWheelColCount + fWidth * i, g_iTopMargin + fWidth * g_iPinWheelRowCount + fLength * j + g_iLengthOfRowDivision,
//							g_iLeftMargin + fLength * g_iPinWheelColCount + fWidth * (i + 1) - g_iInterval, g_iTopMargin + fWidth * g_iPinWheelRowCount + fLength * (j + 1) - g_iInterval + g_iLengthOfRowDivision), paint);
//				}
//			}
//
//			for(int i=g_iPinWheelRowCount * g_iPinWheelColCount * 4; i<pinWheel.size(); i++){
//				PalletViewBean box = pinWheel.get(i);
//				if(box.getdirection().equals("H")){
//					canvas.drawRect(new RectF(g_iLeftMargin + box.getx() * g_fRateSize,
//							g_iTopMargin + box.gety() * g_fRateSize + g_iLengthOfRowDivision,
//							fWidth + g_iLeftMargin + box.getx() * g_fRateSize - g_iInterval,
//							fLength + g_iTopMargin + box.gety() * g_fRateSize - g_iInterval + g_iLengthOfRowDivision), paint);
//				}else{
//					canvas.drawRect(new RectF(g_iLeftMargin + box.getx() * g_fRateSize,
//							g_iTopMargin + box.gety() * g_fRateSize + g_iLengthOfRowDivision,
//							fLength + g_iLeftMargin + box.getx() * g_fRateSize - g_iInterval,
//							fWidth + g_iTopMargin + box.gety() * g_fRateSize - g_iInterval + g_iLengthOfRowDivision), paint);
//				}
//			}
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
