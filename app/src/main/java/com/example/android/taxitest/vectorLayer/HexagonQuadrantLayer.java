package com.example.android.taxitest.vectorLayer;

import android.content.Context;
import android.graphics.Color;

import com.example.android.taxitest.CommunicationsRecyclerView.CommunicationsAdapter;
import com.example.android.taxitest.data.TaxiObject;

import org.geojson.FeatureCollection;
import org.locationtech.jts.geom.Point;
import org.oscim.core.GeoPoint;
import org.oscim.layers.vector.VectorLayer;
import org.oscim.map.Map;
import org.oscim.utils.geom.GeomBuilder;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class HexagonQuadrantLayer extends VectorLayer {
    Context mContext;
    int mResource;

    private List<HexagonQuadrantDrawable> fullDrawables=new ArrayList<>();

    public HexagonQuadrantLayer(Map map, Context context, int barriosResourceId) {
        super(map);
        mContext=context;
        mResource=barriosResourceId;
        InputStream geoJsonIs=mContext.getResources().openRawResource(mResource);
        FeatureCollection fc= GeoJsonUtils.loadFeatureCollection(geoJsonIs);
        if (fc != null) {
            GeoJsonUtils.addHexQuadrants(this,fc);
        }
    }

    public void addHex(HexagonQuadrantDrawable drawable){
        add(drawable);
        fullDrawables.add(drawable);
    }

    public synchronized HexagonQuadrantDrawable getContainingQuadrant(GeoPoint geoPoint){
        Point point = new GeomBuilder().point(geoPoint.getLongitude(), geoPoint.getLatitude()).toPoint();
        for (HexagonQuadrantDrawable drawable : fullDrawables) {
            if (drawable.getGeometry().contains(point)) {
                return drawable;
            }
        }
        return fullDrawables.get(0);
    }

    public int[] getSendingQuadrants(CommunicationsAdapter adapter, TaxiObject taxiObject){
        return new int[]{1, 2};
    }

    public int[] getReceivingQuadrants(CommunicationsAdapter adapter, TaxiObject taxiObject, int layerDepth){
        return new int[]{1, 2};
    }
}
