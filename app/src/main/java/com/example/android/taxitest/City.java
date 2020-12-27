package com.example.android.taxitest;

import org.oscim.core.GeoPoint;

public class City {
    public String name;
    public String prettyName;
    public int resourceBarrios;
    public int resourceQuadrants;
    GeoPoint latLonMax;
    GeoPoint latLonMin;

    public City(String name, String prettyName, int resourceBarrios, int resourceQuadrants, GeoPoint latLonMax, GeoPoint latLonMin) {
        this.name = name;
        this.prettyName = prettyName;
        this.resourceBarrios = resourceBarrios;
        this.resourceQuadrants = resourceQuadrants;
        this.latLonMax = latLonMax;
        this.latLonMin = latLonMin;
    }

    public boolean isInCity(GeoPoint geo){
        return geo.getLatitude()<latLonMax.getLatitude()&&geo.getLatitude()>latLonMin.getLatitude()
                &&geo.getLongitude()<latLonMax.getLongitude()&&geo.getLongitude()>latLonMin.getLongitude();
    }
}
