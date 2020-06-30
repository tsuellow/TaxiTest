package com.example.android.taxitest.utils;

public class ZoomUtils {
    public static int getDrawableSize(double zoom){
        int currSize;
        if (zoom>16 && zoom<17){
            currSize=(int) (40*(zoom-16));
        }else if (zoom>=17){
            currSize=39;
        }else{
            currSize=0;
        }
        return currSize+41;
    }
    public static int getDrawableIndex(double zoom){
        int currSize;
        if (zoom>16 && zoom<17){
            currSize=(int) (40*(zoom-16));
        }else if (zoom>=17){
            currSize=39;
        }else{
            currSize=0;
        }
        return currSize;
    }
}
