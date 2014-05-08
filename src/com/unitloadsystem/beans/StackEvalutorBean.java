package com.unitloadsystem.beans;

import java.util.ArrayList;

public class StackEvalutorBean {

	private double share;
	private ArrayList<PalletViewBean> palletView;
	
	void StackEvalutorBean(){
		
	}
	
	public double getShare() {
		return share;
	}
	public void setShare(double share) {
		this.share = share;
	}
	public ArrayList<PalletViewBean> getPalletView() {
		return palletView;
	}
	public void setPalletView(ArrayList<PalletViewBean> palletView) {
		this.palletView = palletView;
	}
}
