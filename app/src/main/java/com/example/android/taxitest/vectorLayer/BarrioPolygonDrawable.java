package com.example.android.taxitest.vectorLayer;

import android.graphics.Color;

import org.locationtech.jts.geom.Geometry;
import org.oscim.core.GeoPoint;
import org.oscim.layers.vector.geometries.PolygonDrawable;
import org.oscim.layers.vector.geometries.Style;

import java.util.List;

public class BarrioPolygonDrawable extends PolygonDrawable {
    private String barrioName;
    private int barrioId;

    static Style.Builder sb = Style.builder()
            .strokeColor(Color.BLACK)
            .fillColor(Color.BLACK)
            .strokeWidth(1.5f);

    public String getBarrioName() {
        return barrioName;
    }

    public void setBarrioName(String barrioName) {
        this.barrioName = barrioName;
    }

    public int getBarrioId() {
        return barrioId;
    }

    public void setBarrioId(int barrioId) {
        this.barrioId = barrioId;
    }

    public BarrioPolygonDrawable(Geometry polygon, Style style) {
        super(polygon, style);
    }

    public BarrioPolygonDrawable(List<GeoPoint> points, String barrioName) {
        super(points);
        this.barrioName=barrioName;
    }

    public BarrioPolygonDrawable(Style style, GeoPoint... points) {
        super(style, points);
    }

    public BarrioPolygonDrawable(List<GeoPoint> points, Style style, String barrioName, int barrioId) {
        super(points, style);
        this.barrioName=barrioName;
        this.barrioId=barrioId;
    }

    public BarrioPolygonDrawable(GeoPoint[] points, GeoPoint[] holePoints, float lineWidth, int lineColor, int fillColor, float fillAlpha) {
        super(points, holePoints, lineWidth, lineColor, fillColor, fillAlpha);
    }

    public BarrioPolygonDrawable(List<GeoPoint> points, List<GeoPoint> holePoints, float lineWidth, int lineColor, int fillColor, float fillAlpha) {
        super(points, holePoints, lineWidth, lineColor, fillColor, fillAlpha);
    }

    public BarrioPolygonDrawable(List<GeoPoint> points, List<GeoPoint> holePoints, Style style) {
        super(points, holePoints, style);
    }

    public Style getStyle(){
        return this.style;
    }
}
