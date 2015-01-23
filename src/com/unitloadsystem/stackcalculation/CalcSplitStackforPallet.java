package com.unitloadsystem.stackcalculation;

import java.util.ArrayList;

import com.unitloadsystem.beans.PalletViewBean;
import com.unitloadsystem.beans.StackEvalutorBean;

public class CalcSplitStackforPallet {

//	private String firstDir;
//	private String secondDir;
//	private int containerWidth;
//	private int containerLength;
//	private float width;
//	private float length;
	
	void CalcSplitStackforPallet(){
//		this.firstDir = firstDir;
//		this.secondDir = secondDir;
//		this.containerWidth = containerWidth;
//		this.containerLength = containerLength;
//		this.width = width;
//		this.length = length;
	}
	
	public StackEvalutorBean CalcSplitStackRule(String firstDir, String secondDir, int containerWidth, int containerLength, float width, float length, int startX, int startY){
		StackEvalutorBean result = new StackEvalutorBean();
		
		int iAvailCount = (int)(containerWidth / width);
		
		if(iAvailCount <= 0){
			result.setShare(0.0);
			result.setPalletView(new ArrayList<PalletViewBean>());
			return result;
		}
		
		StackEvalutorBean[] stackCase = new StackEvalutorBean[iAvailCount];
		
		for(int cnt = 1; cnt < iAvailCount + 1; cnt++){
			stackCase[cnt - 1] = new StackEvalutorBean();
			ArrayList<PalletViewBean> aPalletView = new ArrayList<PalletViewBean>();
		
			int iLengthCount = (int)(containerLength / length);
			double dUnitShare = (length * width) / (containerLength * containerWidth);
			double palletShare = dUnitShare * cnt * iLengthCount;
			
			AddBox(aPalletView, cnt, iLengthCount, firstDir, startX, startY, width, length);
			
			if(cnt * width + length <= containerWidth && width <= containerLength){
				int iRemainWidthCnt = (int) ((containerWidth - width * cnt) / length);
				int iAvailLengthCnt = (int)(containerLength / width);
				palletShare += dUnitShare * iRemainWidthCnt * iAvailLengthCnt;
				
				AddBox(aPalletView, iRemainWidthCnt, iAvailLengthCnt, secondDir, startX + width * cnt, startY, length, width);
				
				if(iAvailLengthCnt * width + length <= containerLength){
					if(cnt * width + width <= containerWidth){
						int iRemainLengthCnt = (int)((containerWidth - width * cnt) / width);
						int iAvailWidthCnt = (int)((containerLength - width * iAvailLengthCnt) / length);
						palletShare += dUnitShare * iRemainLengthCnt * iAvailWidthCnt;
						
						AddBox(aPalletView, iRemainLengthCnt, iAvailWidthCnt, firstDir, startX + width * cnt, startY + width * iAvailLengthCnt, width, length);
					}
				}
			}else if(iLengthCount * length + width <= containerLength && length <= containerWidth){
				int iRemainWidthCnt = (int) (containerWidth / length);
				int iAvailLengthCnt = (int)((containerLength - length * iLengthCount) / width);
				palletShare += dUnitShare * iRemainWidthCnt * iAvailLengthCnt;
				
				AddBox(aPalletView, iRemainWidthCnt, iAvailLengthCnt, secondDir, startX, startY + length * iLengthCount, length, width);
			}
			
			stackCase[cnt - 1].setId(cnt - 1);
			stackCase[cnt - 1].setShare(palletShare);
			stackCase[cnt - 1].setPalletView(aPalletView);
		}
		
		int iBestIdx = GetBestShare(stackCase);
		
		result.setShare(stackCase[iBestIdx].getShare());
		result.setPalletView(stackCase[iBestIdx].getPalletView());
		
		return result;
	}
	
	private void AddBox(ArrayList<PalletViewBean> palletView, int row, int col, String dir, float startrow, float startcol, float addrow, float addcol){
		for(int i=0; i<row; i++){
			for(int j=0; j<col; j++){
				palletView.add(new PalletViewBean(dir, (int)(startrow + addrow * i), (int)(startcol + addcol * j)));
			}
		}
	}
	
	private int GetBestShare(StackEvalutorBean[] beans){
		int iResult = 0;
		double bestShare = 0.0d;
		
		for(int i=0; i<beans.length; i++){
			if(beans[i].getShare() > bestShare){
				iResult = i;
				bestShare = beans[i].getShare();
			}
		}
		
		return iResult;
	}
}
