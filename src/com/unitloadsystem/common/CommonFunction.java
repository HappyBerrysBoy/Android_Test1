package com.unitloadsystem.common;

/**
 * Created by KSC on 2015-09-22.
 */
public class CommonFunction {

    private static final float INCH_TO_MM = (float) 25.4;

    public static float changeToMM(float value, String kind){
        if(kind.equals("mm")){
            return value;
        }else if(kind.equals("inch")){
            return value * INCH_TO_MM;
        }
        return value;
    }

    public static float changeToInch(float value, String kind){
        if(kind.equals("mm")){
            return value;
        }else if(kind.equals("inch")){
            return value / INCH_TO_MM;
        }
        return value;
    }
}
