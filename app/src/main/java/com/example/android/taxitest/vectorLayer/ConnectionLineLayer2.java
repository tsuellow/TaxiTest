package com.example.android.taxitest.vectorLayer;

import android.graphics.Color;

import com.example.android.taxitest.MainActivity;
import com.example.android.taxitest.vtmExtension.TaxiMarker;

import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.oscim.core.Box;
import org.oscim.core.GeoPoint;
import org.oscim.layers.vector.VectorLayer;
import org.oscim.layers.vector.geometries.Drawable;
import org.oscim.layers.vector.geometries.Style;
import org.oscim.map.Map;
import org.oscim.utils.SpatialIndex;
import org.oscim.utils.ThreadUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.example.android.taxitest.utils.MiscellaneousUtils.locToGeo;

public class ConnectionLineLayer2 extends VectorLayer {

    protected final HashMap<Integer,Drawable> mappedDrawables = new HashMap<>(128);

    public ConnectionLineLayer2(Map map, SpatialIndex<Drawable> index) {
        super(map, index);
    }

    public ConnectionLineLayer2(Map map) {
        super(map);
    }

    static Style.Builder sb = Style.builder()
            .strokeColor(Color.RED)
            .strokeWidth(2);
    //static Style mStyle = sb.build();

    public void addLine(TaxiMarker taxiMarker){
        ConnectionLineDrawable2 drawable=new ConnectionLineDrawable2(taxiMarker);
        add(drawable);
        mappedDrawables.put(taxiMarker.taxiObject.getTaxiId(),drawable);
        update();
        ThreadUtils.assertMainThread();
        mMap.render();
    }

    public synchronized void remove(Integer key) {
        Drawable toRemove = mappedDrawables.get(key);
        if (toRemove != null) {
            Box bbox = bbox(toRemove.getGeometry());
            mDrawables.remove(bbox, toRemove);
            update();
            mMap.updateMap(true);
        }else{
            return;
        }
    }

    public synchronized void updateLines(){
        for (Drawable drawable:tmpDrawables){
            ((ConnectionLineDrawable2) drawable).setGeometry();
        }
        update();
        mMap.render();
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

    public synchronized void resetStyle(){
        for (Drawable drawable:tmpDrawables){
                ((ConnectionLineDrawable2) drawable).setStyle();
                update();
                //mMap.updateMap(true);
        }

    }
}
