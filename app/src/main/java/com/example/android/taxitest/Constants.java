package com.example.android.taxitest;

import org.oscim.core.GeoPoint;

public class Constants {
    //should be mostly in shared prefs
    public static int myId=3;
    public static final String MAP_FILE = "nicaragua_modified_2021_00.map";
    public static final int MAP_RESOURCE=R.raw.nicaragua_modified_2021_00;
    public static final String POI_FILE ="nicaragua_modified_2021_00_poi.poi";
    public static final int POI_RESOURCE=R.raw.nicaragua_modified_2021_00_poi;
    public static String userType="taxi";
    public static GeoPoint lastLocation=new GeoPoint(0.0,0.0);
    public static double filterDegrees=45;
    public static int barriosFile=R.raw.barrios_esteli;
    public static GeoPoint lowerLeftLabelLimit=new GeoPoint(13.041135,-86.406090 );
    public static GeoPoint upperRightLabelLimit=new GeoPoint(13.124386, -86.309543);
    public static String SERVER_URL="https://www.awicon.de/theostaxi/php/";


}
