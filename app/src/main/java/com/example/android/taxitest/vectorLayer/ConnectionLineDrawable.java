package com.example.android.taxitest.vectorLayer;

import org.locationtech.jts.geom.LineString;
import org.oscim.core.GeoPoint;
import org.oscim.layers.vector.geometries.LineDrawable;
import org.oscim.layers.vector.geometries.Style;

import java.util.ArrayList;
import java.util.List;

public class ConnectionLineDrawable extends LineDrawable {
    int id;
    GeoPoint ownPoint;
    GeoPoint otherPoint;
    List<GeoPoint> list=new ArrayList<>();

    public void setId(int id) {
        this.id = id;
    }

    public GeoPoint getOwnPoint() {
        return ownPoint;
    }

    public void setOwnPoint(GeoPoint ownPoint) {
        this.ownPoint = ownPoint;
    }

    public GeoPoint getOtherPoint() {
        return otherPoint;
    }

    public void setOtherPoint(GeoPoint otherPoint) {
        this.otherPoint = otherPoint;
    }

    public ConnectionLineDrawable(List<GeoPoint> points, Style style, int id) {
        super(points, style);
        this.id=id;
        setList(points);

    }

    private void setList(List<GeoPoint> points){
        list=points;
        ownPoint=list.get(0);
        otherPoint=list.get(1);
    }

    public void resetOwnEnd(GeoPoint own) {
        list.set(0,own);
        setGeometry(list);
    }



    public int getId(){
        return id;
    }

    public void setGeometry(List<GeoPoint> points){
        setList(points);
        double[] coords = new double[points.size() * 2];
        int c = 0;
        for (GeoPoint p : points) {
            coords[c++] = p.getLongitude();
            coords[c++] = p.getLatitude();
        }
        this.geometry = new LineString(coordFactory.create(coords, 2), geomFactory);
    }

    public void resetGeom(){

    }


}
