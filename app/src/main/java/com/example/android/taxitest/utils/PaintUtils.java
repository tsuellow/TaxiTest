package com.example.android.taxitest.utils;


import android.graphics.Color;
import android.graphics.ColorSpace;

public class PaintUtils {

    public static int getHue(int color) {
        float[] hsbVals = new float[3];
        Color.colorToHSV(color, hsbVals);
        //hsbVals[1] = 0f;
        hsbVals[2] = 0.5f;
        return Color.HSVToColor(hsbVals);
    }

    public static int getSaturation(int color) {
        float[] hsbVals = new float[3];
        Color.colorToHSV(color, hsbVals);
        hsbVals[0] = 0f;
        hsbVals[1] = 0f;
        //the secret lies in under exagerating lightness
        hsbVals[2] = hsbVals[2]*hsbVals[2];
        return Color.HSVToColor(hsbVals);
    }
}
