package com.unitloadsystem.beans;

import java.util.ArrayList;

public class StackEvalutorBean {

	private double share;
	private ArrayList<PalletViewBean> palletView;
	private int id;
	private int rowCount;
	private int colCount;
	
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

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getRowCount() {
		return rowCount;
	}

	public void setRowCount(int rowCount) {
		this.rowCount = rowCount;
	}

	public int getColCount() {
		return colCount;
	}

	public void setColCount(int colCount) {
		this.colCount = colCount;
	}
}
