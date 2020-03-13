package com.example.android.taxitest;

import org.oscim.core.GeoPoint;

public class Constants {
    //should be mostly in shared prefs
    public static int myId=1;
    public static final String MAP_FILE = "result.map";
    public static String userType="taxi";
    public static GeoPoint lastLocation=new GeoPoint(0.0,0.0);
    public static double filterDegrees=45;
}
