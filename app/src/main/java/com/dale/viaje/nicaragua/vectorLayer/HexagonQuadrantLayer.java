package com.dale.viaje.nicaragua.vectorLayer;

import android.content.Context;
import android.util.Log;

import com.dale.viaje.nicaragua.CommunicationsRecyclerView.CommsObject;
import com.dale.viaje.nicaragua.CommunicationsRecyclerView.CommunicationsAdapter;
import com.dale.viaje.nicaragua.data.SocketObject;
import com.dale.viaje.nicaragua.vtmExtension.TaxiMarker;

import org.geojson.FeatureCollection;
import org.locationtech.jts.geom.Point;
import org.oscim.core.GeoPoint;
import org.oscim.layers.vector.VectorLayer;
import org.oscim.map.Map;
import org.oscim.utils.geom.GeomBuilder;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class HexagonQuadrantLayer extends VectorLayer {
    Context mContext;
    int mResource;
    private List<HexagonQuadrantDrawable> fullDrawables=new ArrayList<>();

//    private BitSet sendingBits=new BitSet(64);
//    private BitSet receivingBits=new BitSet(64);


    private HashSet<HexagonQuadrantDrawable> sendingDrawables=new HashSet<>();
    private HashSet<HexagonQuadrantDrawable> receivingDrawables=new HashSet<>();

    public HashSet<HexagonQuadrantDrawable> getSendingDrawables() {
        return sendingDrawables;
    }

    public HashSet<HexagonQuadrantDrawable> getReceivingDrawables() {
        return receivingDrawables;
    }

//    public BitSet getSendingBits() {
//        return sendingBits;
//    }
//
//    public BitSet getReceivingBits() {
//        return receivingBits;
//    }

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
//  public BitSet getSendingBits(CommunicationsAdapter adapter, SocketObject taxiObject){
    public HashSet<Integer> getSendingChannels(CommunicationsAdapter adapter, SocketObject taxiObject){
        //create empty vessel sets
        //BitSet bits=new BitSet(64);
        HashSet<Integer> channels=new HashSet<>();
        HashSet<HexagonQuadrantDrawable> areaSet=new HashSet<>();

        GeoPoint geo=new GeoPoint(taxiObject.getLatitude(),taxiObject.getLongitude());
        //add own barrio
        HexagonQuadrantDrawable ownHex=this.getContainingQuadrant(geo);
        //bits.set(ownHex.getBit());
        channels.add(ownHex.getBit());
        areaSet.add(ownHex);

        //add hexs of comms that have not yet been received
        //no need for surrounding points as other party is receiving around himself
        for (CommsObject comm:adapter.getCommsAwaitingConfirmation()){
            HexagonQuadrantDrawable commHex=this.getContainingQuadrant(comm.taxiMarker.geoPoint);
            //bits.set(commHex.getBit());
            channels.add(commHex.getBit());
            areaSet.add(commHex);
        }

        //change sending bits in case there is a difference
//        if (!bits.equals(sendingBits)){
//            sendingBits=bits;//consider not setting until later so you can check for changes;
            sendingDrawables=areaSet;
//        }
        //return sendingBits;
        return channels;
    }

    //public BitSet getReceivingBits(CommunicationsAdapter adapter, SocketObject taxiObject, double[] profile, int layerDepth){
    public HashSet<Integer> getReceivingChannels(CommunicationsAdapter adapter, SocketObject taxiObject, double[] profile, int layerDepth){
        //create empty vessel sets
        //BitSet bits=new BitSet(64);
        HashSet<Integer> channels=new HashSet<>();
        HashSet<HexagonQuadrantDrawable> areaSet=new HashSet<>();

        //add hexs immediately around my current position
        for (GeoPoint geo:HexagonUtils.getSurroundingMarkers(profile,taxiObject)){
            Log.d("hexagon:",geo.toString());
            HexagonQuadrantDrawable surroundingHex=this.getContainingQuadrant(geo);
            //bits.set(surroundingHex.getBit());
            channels.add(surroundingHex.getBit());
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
                        if (quadrant!=null){
                            areaSubSet.add(quadrant);
                            //bits.set(quadrant.getBit());
                            channels.add(quadrant.getBit());
                            ids.add(id);
                        }
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
                //bits.set(commHex.getBit());
                channels.add(commHex.getBit());
                areaSet.add(commHex);
            }
        }

//        if (!bits.equals(receivingBits)){
//            receivingBits=bits;//consider not setting until later so you can check for changes;
            receivingDrawables=areaSet;
//        }
//        return bits;
        return channels;
    }
}
