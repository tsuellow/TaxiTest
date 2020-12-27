package com.example.android.taxitest.vectorLayer;

import android.content.Context;
import android.graphics.Color;

import com.example.android.taxitest.CommunicationsRecyclerView.CommsObject;
import com.example.android.taxitest.CommunicationsRecyclerView.CommunicationsAdapter;
import com.example.android.taxitest.data.SocketObject;
import com.example.android.taxitest.data.TaxiObject;
import com.example.android.taxitest.vtmExtension.OwnMarker;
import com.example.android.taxitest.vtmExtension.OwnMarkerLayer;
import com.example.android.taxitest.vtmExtension.TaxiMarker;

import org.geojson.FeatureCollection;
import org.locationtech.jts.geom.Point;
import org.oscim.core.GeoPoint;
import org.oscim.layers.vector.VectorLayer;
import org.oscim.map.Map;
import org.oscim.utils.geom.GeomBuilder;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashSet;
import java.util.List;

public class HexagonQuadrantLayer extends VectorLayer {
    Context mContext;
    int mResource;
    private List<HexagonQuadrantDrawable> fullDrawables=new ArrayList<>();

    private BitSet sendingBits=new BitSet(64);
    private BitSet receivingBits=new BitSet(64);
    private HashSet<HexagonQuadrantDrawable> sendingDrawables=new HashSet<>();
    private HashSet<HexagonQuadrantDrawable> receivingDrawables=new HashSet<>();

    public HashSet<HexagonQuadrantDrawable> getSendingDrawables() {
        return sendingDrawables;
    }

    public HashSet<HexagonQuadrantDrawable> getReceivingDrawables() {
        return receivingDrawables;
    }

    public BitSet getSendingBits() {
        return sendingBits;
    }

    public BitSet getReceivingBits() {
        return receivingBits;
    }

    public HexagonQuadrantDrawable getQuadById(String id){
        for (HexagonQuadrantDrawable hex:fullDrawables){
            if (hex.getQuadrantId().contentEquals(id)){
                return hex;
            }
        }
        return null;
    }

    public HexagonQuadrantLayer(Map map, Context context, int quadrantsResourceId) {
        super(map);
        mContext=context;
        mResource=quadrantsResourceId;
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

//    public BitSet getSurroundingBits(double[] profile, TaxiMarker taxiMarker){
//        BitSet bits=new BitSet(64);
//        for (GeoPoint geo:HexagonUtils.getSurroundingMarkers(profile,taxiMarker.taxiObject)){
//            HexagonQuadrantDrawable hex=this.getContainingQuadrant(geo);
//            bits.set(hex.getBit());
//        }
//        return bits;
//    }
//
//    public BitSet getCommBits(CommunicationsAdapter adapter){
//        BitSet bits=new BitSet(64);
//        for (CommsObject comm:adapter.mComms){
//            TaxiMarker taxiMarker=comm.taxiMarker;
//            for (GeoPoint geo:HexagonUtils.getSurroundingMarkers(HexagonUtils.quadProfileComms,taxiMarker.taxiObject)){
//                HexagonQuadrantDrawable hex=this.getContainingQuadrant(geo);
//                bits.set(hex.getBit());
//            }
//            HexagonQuadrantDrawable hex=this.getContainingQuadrant(comm.taxiMarker.geoPoint);
//            bits.set(hex.getBit());
//        }
//        return bits;
//    }
//
//    public BitSet getUnconfirmedCommBits(CommunicationsAdapter adapter){
//        BitSet bits=new BitSet(64);
//        //no need for surrounding points as other party is receiving around himself
//        for (CommsObject comm:adapter.getCommsAwaitingConfirmation()){
//            HexagonQuadrantDrawable hex=this.getContainingQuadrant(comm.taxiMarker.geoPoint);
//            bits.set(hex.getBit());
//        }
//        return bits;
//    }

    public BitSet getSendingBits(CommunicationsAdapter adapter, SocketObject taxiObject){
        //create empty vessel sets
        BitSet bits=new BitSet(64);
        HashSet<HexagonQuadrantDrawable> areaSet=new HashSet<>();

        GeoPoint geo=new GeoPoint(taxiObject.getLatitude(),taxiObject.getLongitude());
        //add own barrio
        HexagonQuadrantDrawable ownHex=this.getContainingQuadrant(geo);
        bits.set(ownHex.getBit());
        areaSet.add(ownHex);

        //add hexs of comms that have not yet been received
        //no need for surrounding points as other party is receiving around himself
        for (CommsObject comm:adapter.getCommsAwaitingConfirmation()){
            HexagonQuadrantDrawable commHex=this.getContainingQuadrant(comm.taxiMarker.geoPoint);
            bits.set(commHex.getBit());
            areaSet.add(commHex);
        }

        //change sending bits in case there is a difference
        if (!bits.equals(sendingBits)){
            sendingBits=bits;//consider not setting until later so you can check for changes;
            sendingDrawables=areaSet;
        }
        return sendingBits;
    }

    public BitSet getReceivingBits(CommunicationsAdapter adapter, SocketObject taxiObject, double[] profile, int layerDepth){
        //create empty vessel sets
        BitSet bits=new BitSet(64);
        HashSet<HexagonQuadrantDrawable> areaSet=new HashSet<>();

        //add hexs immediately around my current position
        for (GeoPoint geo:HexagonUtils.getSurroundingMarkers(profile,taxiObject)){
            HexagonQuadrantDrawable surroundingHex=this.getContainingQuadrant(geo);
            bits.set(surroundingHex.getBit());
            areaSet.add(surroundingHex);
        }

        //add hexs around the previous hexs in case too few counterparts are received
        HashSet<String> ids=new HashSet<>();
        HashSet<HexagonQuadrantDrawable> areaSubSet=new HashSet<>();
        for (int i=0;i<layerDepth;i++) {
            for (HexagonQuadrantDrawable hex : areaSet) {
                for (String id : hex.getNeighbors()) {
                    if (!ids.contains(id)) {
                        HexagonQuadrantDrawable quadrant = this.getQuadById(id);
                        areaSubSet.add(quadrant);
                        bits.set(quadrant.getBit());
                        ids.add(id);
                    }
                }
            }
        }
        areaSet.addAll(areaSubSet);

        //add open comms hexs
        for (CommsObject comm:adapter.mComms){
            TaxiMarker taxiMarker=comm.taxiMarker;
            for (GeoPoint geo:HexagonUtils.getSurroundingMarkers(HexagonUtils.quadProfileComms,taxiMarker.taxiObject)){
                HexagonQuadrantDrawable commHex=this.getContainingQuadrant(geo);
                bits.set(commHex.getBit());
                areaSet.add(commHex);
            }
        }

        if (!bits.equals(receivingBits)){
            receivingBits=bits;//consider not setting until later so you can check for changes;
            receivingDrawables=areaSet;
        }
        return bits;
    }
}
