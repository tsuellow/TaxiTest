package com.example.android.taxitest.vectorLayer;

import android.content.Context;
import android.graphics.Color;
import android.widget.Toast;

import org.geojson.FeatureCollection;
import org.locationtech.jts.geom.Point;
import org.oscim.core.Box;
import org.oscim.core.GeoPoint;
import org.oscim.event.Gesture;
import org.oscim.event.MotionEvent;
import org.oscim.layers.vector.VectorLayer;
import org.oscim.layers.vector.geometries.Drawable;
import org.oscim.map.Map;
import org.oscim.utils.SpatialIndex;
import org.oscim.utils.geom.GeomBuilder;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class BarriosLayer extends VectorLayer {
    Context mContext;
    int mResource;
    BarrioPolygonDrawable noBarrioSelected;

    private List<BarrioPolygonDrawable> fullDrawables=new ArrayList<>();

    public BarriosLayer(Map map, SpatialIndex<Drawable> index) {
        super(map, index);
    }

    public BarriosLayer(Map map, Context context, int barriosResourceId) {
        super(map);
        mContext=context;
        mResource=barriosResourceId;
        InputStream geoJsonIs=mContext.getResources().openRawResource(mResource);
        FeatureCollection fc= GeoJsonUtils.loadFeatureCollection(geoJsonIs);
        if (fc != null) {
            GeoJsonUtils.addBarrios(this,fc);
        }

        //create catch it all drawable
        List<GeoPoint> entireCountry=new ArrayList<>();
        entireCountry.add(new GeoPoint(15.3,-89.0));
        entireCountry.add(new GeoPoint(15.3,-82.0));
        entireCountry.add(new GeoPoint(10.5,-82.0));
        entireCountry.add(new GeoPoint(10.5,-89.0));
        noBarrioSelected=new BarrioPolygonDrawable(entireCountry,BarrioPolygonDrawable.sb.build(),"outside of city",-1);
    }





    public void addBarrio(BarrioPolygonDrawable drawable){
        add(drawable);
        fullDrawables.add(drawable);
    }




    public synchronized String containsBarrio(float x, float y) {
        GeoPoint geoPoint = mMap.viewport().fromScreenPoint(x, y);
        Point point = new GeomBuilder().point(geoPoint.getLongitude(), geoPoint.getLatitude()).toPoint();
        for (Drawable drawable : tmpDrawables) {
            if (drawable.getGeometry().contains(point)) {
                if (drawable.getClass() == BarrioPolygonDrawable.class) {
                    return ((BarrioPolygonDrawable) drawable).getBarrioName();
                }
            }
        }
        return "outside of city";
    }

    public synchronized BarrioPolygonDrawable getContainingBarrio(GeoPoint geoPoint){
        Point point = new GeomBuilder().point(geoPoint.getLongitude(), geoPoint.getLatitude()).toPoint();
        for (BarrioPolygonDrawable drawable : fullDrawables) {
            if (drawable.getGeometry().contains(point)) {
                return drawable;
            }
        }
        return noBarrioSelected;
    }

    @Override
    public boolean onGesture(Gesture g, MotionEvent e) {

//            if (g instanceof Gesture.Tap) {
//                GeoPoint p = mMap.viewport().fromScreenPoint(e.getX(), e.getY());
//                String s=getContainingBarrio(p).getStyle().fillColor+"";
//                Toast.makeText(mContext, "Map tap\n" + s, Toast.LENGTH_SHORT).show();
//                return true;
//            }
            if (g instanceof Gesture.LongPress) {
                String p = getContainingBarrio(mMap.viewport().fromScreenPoint(e.getX(), e.getY())).getBarrioName();
                Toast.makeText(mContext, "ID \n" + p, Toast.LENGTH_SHORT).show();
                return true;
            }
//            if (g instanceof Gesture.TripleTap) {
//                GeoPoint p = mMap.viewport().fromScreenPoint(e.getX(), e.getY());
//                Toast.makeText(mContext, "Map triple tap\n" + p, Toast.LENGTH_SHORT).show();
//                return true;
//            }

        return false;
    }
}
