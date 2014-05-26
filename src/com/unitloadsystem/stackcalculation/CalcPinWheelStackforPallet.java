package com.unitloadsystem.stackcalculation;

import java.util.ArrayList;

import com.unitloadsystem.beans.PalletViewBean;
import com.unitloadsystem.beans.StackEvalutorBean;

public class CalcPinWheelStackforPallet {

	public StackEvalutorBean CalcSplitStackRule(String firstDir, String secondDir, int containerWidth, int containerLength, float width, float length){
		StackEvalutorBean result = new StackEvalutorBean();
		
		// PinWheel 한 면적에 박스 배치 수량
		int iAvailCount = 0;
		int iSmallLength = 0;
		
		double dUnitShare = (length * width) / (containerLength * containerWidth);
		
		if(containerWidth <= containerLength){
			iAvailCount = (int)((containerWidth - length) / width);
			iSmallLength = containerWidth;
		}else{
			iAvailCount = (int)((containerLength - length) / width);
			iSmallLength = containerLength;
		}
		
		StackEvalutorBean[] stackCase = new StackEvalutorBean[iAvailCount];
		
		for(int cnt = 1; cnt<iAvailCount + 1; cnt++){
			
			stackCase[cnt - 1] = new StackEvalutorBean();
			ArrayList<PalletViewBean> aPalletView = new ArrayList<PalletViewBean>();
			int iAvailRow = (int)((iSmallLength - cnt * width) / length);
			double palletShare = 0;
			
			if(firstDir.equals("H")){
				for(int i=0; i<cnt; i++){
					for(int j=0; j<iAvailRow; j++){
						palletShare += dUnitShare * 4;
						aPalletView.add(new PalletViewBean(firstDir, width * i, (int)(length * j)));
						aPalletView.add(new PalletViewBean(secondDir, (int)(width * iAvailCount + length * j), width * i));
						aPalletView.add(new PalletViewBean(secondDir, (int)(length * j), (int)(length * iAvailRow + width * i)));
						aPalletView.add(new PalletViewBean(firstDir, (int)(length * iAvailRow + width * i), (int)(width * iAvailCount + length * j)));
					}
				}
			}
			
			int iRemainWidth = (int)(containerWidth - cnt * width - iAvailRow * length);
			
			StackEvalutorBean temp = new StackEvalutorBean();
			
			if(iRemainWidth >= width && iRemainWidth >= length)
			{
				CalcSplitStackforPallet calcSplitStack = new CalcSplitStackforPallet();
				
				StackEvalutorBean stackHorizontal = calcSplitStack.CalcSplitStackRule("H", "V", iRemainWidth, containerLength, width, length);
				StackEvalutorBean stackVertical = calcSplitStack.CalcSplitStackRule("V", "H", iRemainWidth, containerLength, width, length);
				
				if(stackHorizontal.getShare() > stackVertical.getShare()){
					temp = stackHorizontal;
				}else{
					temp = stackVertical;
				}
				
				ArrayList<PalletViewBean> split = temp.getPalletView();
				
				for(int k=0; k<split.size(); k++){
					PalletViewBean box = split.get(k);
					box.setx(box.getx() + iRemainWidth);
					aPalletView.add(box);
					palletShare += dUnitShare;
				}
			}
			
			stackCase[cnt - 1].setId(cnt - 1);
			stackCase[cnt - 1].setRowCount(cnt);
			stackCase[cnt - 1].setColCount(iAvailRow);
			stackCase[cnt - 1].setShare(palletShare);
			stackCase[cnt - 1].setPalletView(aPalletView);
		}
		
		int iBestIdx = GetBestShare(stackCase);
		
//		result.setShare(palletShare);
//		result.setPalletView(aPalletView);
		
		result.setShare(stackCase[iBestIdx].getShare());
		result.setRowCount(stackCase[iBestIdx].getRowCount());
		result.setColCount(stackCase[iBestIdx].getColCount());
		result.setPalletView(stackCase[iBestIdx].getPalletView());
		
//		StackEvalutorBean[] stackCase = new StackEvalutorBean[iAvailCount];
//		
//		for(int cnt = 1; cnt <= iAvailCount; cnt++){
//			stackCase[cnt - 1] = new StackEvalutorBean();
//			ArrayList<PalletViewBean> aPalletView = new ArrayList<PalletViewBean>();
//		
//			int iLengthCount = (int)(containerLength / length);
//			double dUnitShare = (length * width) / (containerLength * containerWidth);
//			double palletShare = dUnitShare * cnt * iLengthCount;
//			
//			AddBox(aPalletView, cnt, iLengthCount, firstDir, 0, 0, width, length);
//			
//			if(cnt * width + length <= containerWidth && width <= containerLength){
//				int iRemainWidthCnt = (int) ((containerWidth - width * cnt) / length);
//				int iAvailLengthCnt = (int)(containerLength / width);
//				palletShare += dUnitShare * iRemainWidthCnt * iAvailLengthCnt;
//				
//				AddBox(aPalletView, iRemainWidthCnt, iAvailLengthCnt, secondDir, width * cnt, 0, length, width);
//				
//				if(iAvailLengthCnt * width + length <= containerLength){
//					if(cnt * width + width <= containerWidth){
//						int iRemainLengthCnt = (int)((containerWidth - width * cnt) / width);
//						int iAvailWidthCnt = (int)((containerLength - width * iAvailLengthCnt) / length);
//						palletShare += dUnitShare * iRemainLengthCnt * iAvailWidthCnt;
//						
//						AddBox(aPalletView, iRemainLengthCnt, iAvailWidthCnt, firstDir, width * cnt, width * iAvailLengthCnt, width, length);
//					}
//				}
//			}else if(iLengthCount * length + width <= containerLength && length <= containerWidth){
//				int iRemainWidthCnt = (int) (containerWidth / length);
//				int iAvailLengthCnt = (int)((containerLength - length * iLengthCount) / width);
//				palletShare += dUnitShare * iRemainWidthCnt * iAvailLengthCnt;
//				
//				AddBox(aPalletView, iRemainWidthCnt, iAvailLengthCnt, secondDir, 0, length * iLengthCount, length, width);
//			}
//			
//			stackCase[cnt - 1].setId(cnt - 1);
//			stackCase[cnt - 1].setShare(palletShare);
//			stackCase[cnt - 1].setPalletView(aPalletView);
//		}
//		
//		int iBestIdx = GetBestShare(stackCase);
//		
//		result.setShare(palletShare);
//		result.setPalletView(aPalletView);
		
		return result;
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
