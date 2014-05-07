package com.unitloadsystem.beans;

import android.os.Parcel;
import android.os.Parcelable;

public class PalletViewBean implements Parcelable {
    
    private String direction;
    private float x;
    private float y;
    
    public String getdirection() {
        return direction;
    }

    public void setdirection(String direction) {
        this.direction = direction;
    }

    public float getx() {
        return x;
    }

    public void setx(float x) {
        this.x = x;
    }
    
    public float gety() {
        return y;
    }

    public void sety(float y) {
        this.y = y;
    }
    
    public PalletViewBean(String direction, float x, float y){
        this.direction = direction;
        this.x = x;
        this.y = y;
    }
    
    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(direction);
        dest.writeFloat(x);
        dest.writeFloat(y);
    }
    
    public static final Parcelable.Creator<PalletViewBean> CREATOR = new Creator<PalletViewBean>(){
        public PalletViewBean createFromParcel(Parcel source){
            String direction = source.readString();
            float x = source.readFloat();
            float y = source.readFloat();
            return new PalletViewBean(direction, x, y);
        }
        
        public PalletViewBean[] newArray(int size){
            return new PalletViewBean[size];
        }
    };
}