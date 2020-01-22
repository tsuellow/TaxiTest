package com.example.android.taxitest.utils;

public class ZoomUtils {
    public static int getDrawableSize(double zoom){
        int currSize;
        if (zoom>16 && zoom<17){
            currSize=(int) (100*(zoom-16));
        }else if (zoom>=17){
            currSize=99;
        }else{
            currSize=0;
        }
        return currSize+101;
    }
    public static int getDrawableIndex(double zoom){
        int currSize;
        if (zoom>16 && zoom<17){
            currSize=(int) (100*(zoom-16));
        }else if (zoom>=17){
            currSize=99;
        }else{
            currSize=0;
        }
        return currSize;
    }
}
