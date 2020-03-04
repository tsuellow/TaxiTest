package com.example.android.taxitest.vectorLayer;



import android.graphics.Color;

import com.example.android.taxitest.MainActivity;

import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.oscim.core.Box;
import org.oscim.core.GeoPoint;
import org.oscim.layers.vector.VectorLayer;
import org.oscim.layers.vector.geometries.Drawable;
import org.oscim.layers.vector.geometries.LineDrawable;
import org.oscim.layers.vector.geometries.Style;
import org.oscim.map.Map;
import org.oscim.utils.ThreadUtils;

import java.util.ArrayList;
import java.util.List;

import static com.example.android.taxitest.utils.MiscellaneousUtils.locToGeo;

public class ConnectionLineLayer extends VectorLayer {


    public ConnectionLineLayer(Map map) {
        super(map);
    }

    static Style.Builder sb = Style.builder()
            .strokeColor(Color.RED)
            .strokeWidth(2);
    static Style mStyle = sb.build();

    public void addLine(int id, int color, GeoPoint otherPoint){
        List<GeoPoint> line=new ArrayList<>();
        line.add(locToGeo(MainActivity.mMarkerLoc));
        line.add(otherPoint);
        Style style=sb.strokeColor(color).build();
        add(new ConnectionLineDrawable(line,style,id));
        update();
        ThreadUtils.assertMainThread();
        mMap.render();
    }

    //put remove and move in single synchronized method to avoid concurrent modification exception
    // TODO try to implement hashmap or at least sync  methods
    public void removeLine(int id){
        ConnectionLineDrawable toRemove = null;
        Box bbox=null;
        synchronized (this) {
            for (Drawable drawable : tmpDrawables) {
                if (id == ((ConnectionLineDrawable) drawable).getId()) {
                    toRemove = (ConnectionLineDrawable) drawable;
                    break;
                }
            }
        }
        bbox = bbox(toRemove.getGeometry());
        mDrawables.remove(bbox,toRemove);
        update();
        mMap.updateMap(true);
    }

    private static Box bbox(Geometry geometry) {
        Envelope e = geometry.getEnvelopeInternal();
        Box bbox = new Box(e.getMinX(), e.getMinY(), e.getMaxX(), e.getMaxY());
        //if ("Point".equals(geometry.getGeometryType())){
        //    bbox.
        //}

        bbox.scale(1E6);
        return bbox;
    }

    public synchronized void moveLineFromOtherLayer(int id, GeoPoint otherPoint){
        for (Drawable drawable:tmpDrawables){
            if (id==((ConnectionLineDrawable) drawable).getId()){
                List<GeoPoint> points=new ArrayList<>();
                points.add(0,locToGeo(MainActivity.mMarkerLoc));
                points.add(1,otherPoint);
                ((ConnectionLineDrawable) drawable).setGeometry(points);
                update();
                mMap.updateMap(true);
                break;
            }
        }
    }

    public synchronized void moveLineFromOwnLayer(GeoPoint ownPoint){
        for (Drawable drawable:tmpDrawables){
            ((ConnectionLineDrawable) drawable).resetOwnEnd(ownPoint);
            update();
            mMap.updateMap(true);
        }
    }

    public synchronized void resetStyle(int id, int color){
        for (Drawable drawable:tmpDrawables){
            if (id==((ConnectionLineDrawable) drawable).getId()){
                ((ConnectionLineDrawable) drawable).setStyle(sb.strokeColor(color).build());
                update();
                mMap.updateMap(true);
            }
        }
    }


}
