package com.example.android.taxitest;

import org.oscim.core.GeoPoint;

public class Constants {
    //should be mostly in shared prefs
    public static int myId=1;
    public static final String MAP_FILE = "result.map";
    public static final String POI_FILE ="db_nica.poi";
    public static String userType="taxi";
    public static GeoPoint lastLocation=new GeoPoint(0.0,0.0);
    public static double filterDegrees=45;
    public static int barriosFile=R.raw.barrios;
    public static GeoPoint lowerLeftLabelLimit=new GeoPoint(13.041135,-86.406090 );
    public static GeoPoint upperRightLabelLimit=new GeoPoint(13.124386, -86.309543);

}
