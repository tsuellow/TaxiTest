package com.example.android.taxitest.vtmExtension;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import com.example.android.taxitest.CommunicationsRecyclerView.CommsObject;
import com.example.android.taxitest.CommunicationsRecyclerView.CommunicationsAdapter;
import com.example.android.taxitest.connection.WebSocketConnection;
import com.example.android.taxitest.data.TaxiObject;
import com.example.android.taxitest.utils.PaintUtils;
import com.example.android.taxitest.utils.ZoomUtils;
import com.example.android.taxitest.vectorLayer.BarrioPolygonDrawable;
import com.example.android.taxitest.vectorLayer.BarriosLayer;
import com.example.android.taxitest.vectorLayer.ConnectionLineLayer;
import com.example.android.taxitest.vectorLayer.ConnectionLineLayer2;
import com.sdsmdg.harjot.vectormaster.VectorMasterDrawable;
import com.sdsmdg.harjot.vectormaster.models.PathModel;

import org.oscim.backend.canvas.Bitmap;
import org.oscim.core.GeoPoint;
import org.oscim.core.MapPosition;
import org.oscim.event.Event;
import org.oscim.layers.marker.ItemizedLayer;
import org.oscim.layers.marker.MarkerSymbol;
import org.oscim.map.Map;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static com.example.android.taxitest.utils.ZoomUtils.getDrawableIndex;

public class OtherTaxiLayer extends ItemizedLayer<TaxiMarker> implements Map.UpdateListener
{

    private VectorMasterDrawable drawable;
    private BarriosLayer barriosLayer;
    private Context context;

    public List<DrawableBitmapCorrespondence> bitmapReferenceList= new ArrayList<>();

    //helper vars
    PathModel pathModelArrow;
    PathModel pathModelCircle;
    double mapScale;
    private Bitmap[] scaledGrayedSymbols = new Bitmap[100];
    private WebSocketConnection mWebSocketConnection;
    private ConnectionLineLayer2 mConnectionLines;
    private CommunicationsAdapter mCommunicationsAdapter;



    public OtherTaxiLayer(Context context, BarriosLayer barriosLayer, Map map, List<TaxiMarker> list,
                          VectorMasterDrawable defaultDrawable, WebSocketConnection webSocketConnection,
                          ConnectionLineLayer2 connectionLineLayer, CommunicationsAdapter commsAdapter) {
        super(map,
                list,
                new MarkerSymbol(AndroidGraphicsCustom.drawableToBitmap(defaultDrawable, ZoomUtils.getDrawableSize(map.getMapPosition().getZoom())), MarkerSymbol.HotspotPlace.CENTER, false),
                null);
        drawable = defaultDrawable;
        this.context = context;
        this.barriosLayer = barriosLayer;
        this.mWebSocketConnection=webSocketConnection;
        initializeWebSocket();
        mConnectionLines =connectionLineLayer;
        mCommunicationsAdapter=commsAdapter;
        mCommunicationsAdapter.setConnectionLines(mConnectionLines);

        setOnItemGestureListener(customListener);

        pathModelArrow = drawable.getPathModelByName("arrow");
        pathModelCircle = drawable.getPathModelByName("circle");
        mapScale=map.getMapPosition().getScale();

        prepareScaledBitmapArray();
    }

    public void initializeWebSocket(){
        mWebSocketConnection.initializeSocketListener();
        mWebSocketConnection.startAccumulationTimer();
        mWebSocketConnection.connectSocket();
        //listener for websocket data accumulation result every 3 seconds
        mWebSocketConnection.setAnimationDataListener(new WebSocketConnection.AnimationDataListener() {
            @Override
            public void onAnimationParametersReceived(List<TaxiObject> baseTaxis, List<TaxiObject> newTaxis) {
                //give existing taxis a purpose
                boolean allOk=true;
                if (baseTaxis.size()>0) {
                    for (int i = 0; i < baseTaxis.size(); i++) {
                        TaxiObject taxiObject = baseTaxis.get(i);
                        if (taxiObject.getIsActive() != 1) {
                            mItemList.get(i).setPurpose(TaxiMarker.Purpose.DISAPPEAR);
                        } else {
                            mItemList.get(i).setPurpose(TaxiMarker.Purpose.MOVE);
                        }
                        mItemList.get(i).setPurposeTaxiObject(taxiObject);
                        if(mItemList.get(i).taxiObject.getTaxiId()!=taxiObject.getTaxiId()){
                            allOk=false;
                            break;
                        }
                        //check if destination has changed and if so change destination color right away
                        if (mItemList.get(i).taxiObject.getDestinationLatitude()!=taxiObject.getDestinationLatitude() || mItemList.get(i).taxiObject.getDestinationLongitude()!=taxiObject.getDestinationLongitude()){
                            mItemList.get(i).destGeoPoint=new GeoPoint(taxiObject.getDestinationLatitude(),taxiObject.getDestinationLongitude());
                            mItemList.get(i).setRotatedSymbol(new MarkerSymbol(fetchBitmap(mItemList.get(i)), MarkerSymbol.HotspotPlace.CENTER,false));
//                            if (mItemList.get(i).getIsClicked()){
//                                mConnectionLines.resetStyle(taxiObject.getTaxiId(),mItemList.get(i).color);
//                            }

                        }
                    }
                }
                //reset taxis in case of correspondence error
                if(!allOk){
                    //remove all taxis and add them anew
                    removeAllItems();
                    for (TaxiObject tO:baseTaxis){
                        addNewTaxi(tO);
                    }
                }
                //add new taxis
                for (TaxiObject tO:newTaxis){
                    addNewTaxi(tO);
                }
                //sort the new array to ensure future correspondence
                if (mItemList.size()>0){
                    Collections.sort(mItemList);
                }
                //NEW DELETE MAYBE
                mConnectionLines.resetStyle();
                //set off animation process
                setOffAnimation(18);
            }
        });
    }

    public void doClick(TaxiMarker item){
        item.setIsClicked(true);
        mConnectionLines.addLine(item);
        mCommunicationsAdapter.addItem(new CommsObject(item));
        item.setRotatedSymbol(new MarkerSymbol(fetchBitmap(item), MarkerSymbol.HotspotPlace.CENTER,false));
        update();
        mMap.updateMap(true);
    }

    public void doUnClick(TaxiMarker item){
        item.setIsClicked(false);
        //mConnectionLines.removeLine(item.taxiObject.getTaxiId());
        mConnectionLines.remove(item.taxiObject.getTaxiId());
        mCommunicationsAdapter.cancelById(item.taxiObject.getTaxiId());
        item.setRotatedSymbol(new MarkerSymbol(fetchBitmap(item), MarkerSymbol.HotspotPlace.CENTER,false));
        update();
        mMap.updateMap(true);
    }

    private OnItemGestureListener<TaxiMarker> customListener=new OnItemGestureListener<TaxiMarker>() {
        @Override
        public boolean onItemSingleTapUp(int index, TaxiMarker item) {
            if (item.getIsClicked()){
                doUnClick(item);
            }else{
                doClick(item);
            }
            Toast.makeText(context, item.taxiObject.getTaxiId() + " id", Toast.LENGTH_LONG).show();
            return false;
        }

        @Override
        public boolean onItemLongPress(int index, TaxiMarker item) {
            return false;
        }
    };

    public Bitmap bitmapFromDrawable(Drawable drawable) {
        return AndroidGraphicsCustom.drawableToBitmap(drawable, ZoomUtils.getDrawableSize(mMap.getMapPosition().getZoom()));
    }

    public void setDrawable(VectorMasterDrawable drawable) {
        this.drawable = drawable;
    }

    public void setBarriosLayer(BarriosLayer barriosLayer) {
        this.barriosLayer = barriosLayer;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    //adds a new taxi to the layer
    public void addTaxi(TaxiMarker item) {
        MarkerSymbol symbol = new MarkerSymbol(fetchBitmap(item), MarkerSymbol.HotspotPlace.CENTER, false);
        item.setRotatedSymbol(symbol);
        mItemList.add(item);
        populate();
    }

    public void addNewTaxi(TaxiObject taxiObject){
        TaxiMarker otherTaxiMarker=new TaxiMarker(taxiObject);
        otherTaxiMarker.setPurpose(TaxiMarker.Purpose.APPEAR);
        otherTaxiMarker.setPurposeTaxiObject(taxiObject);
        //otherTaxiMarker.setRotatedSymbol(new MarkerSymbol(AndroidGraphicsCustom.drawableToBitmap(drawable,ZoomUtils.getDrawableSize(mMap.getMapPosition().getZoom())), MarkerSymbol.HotspotPlace.CENTER,false));
        addTaxi(otherTaxiMarker);
    }

    public int getTaxiColor(TaxiMarker taxiMarker){
        BarrioPolygonDrawable barrio = barriosLayer.getContainingBarrio(taxiMarker.destGeoPoint);
        return barrio.getStyle().fillColor;
    }

    //checks if bitmap for geopoint already exists else it calculates a new one
    public Bitmap fetchBitmap(TaxiMarker taxiMarker) {
        BarrioPolygonDrawable barrio = barriosLayer.getContainingBarrio(taxiMarker.destGeoPoint);
        taxiMarker.color=barrio.getStyle().fillColor;
        if (!taxiMarker.getIsClicked()) {
            for (DrawableBitmapCorrespondence item : bitmapReferenceList) {
                if (barrio.getBarrioId() == item.barrioId) {
                    return item.barrioBitmap;
                }
            }
            Log.d("referenceList",""+bitmapReferenceList.size());
        }
        int color = barrio.getStyle().fillColor;
        VectorMasterDrawable drawable = modifyDrawable(color, taxiMarker.getIsClicked());
        Bitmap bitmap = AndroidGraphicsCustom.drawableToBitmap(drawable, ZoomUtils.getDrawableSize(mMap.getMapPosition().getZoom()));
        if (!taxiMarker.getIsClicked()) {
            bitmapReferenceList.add(new DrawableBitmapCorrespondence(barrio.getBarrioId(), color, drawable, bitmap));
        }
        return bitmap;
    }

    // modifies drawable according to destination colors
    public VectorMasterDrawable modifyDrawable(int color, boolean isClicked) {
        VectorMasterDrawable result = new VectorMasterDrawable(context,drawable.getResID());
        PathModel pathModelArrow=result.getPathModelByName("arrow");
        PathModel pathModelCircle=result.getPathModelByName("circle");
        pathModelArrow.setFillColor(color);
        pathModelArrow.setFillAlpha(1.0f);
        pathModelArrow.setStrokeAlpha(1.0f);
        pathModelCircle.setStrokeColor(PaintUtils.getSaturation(color));
        pathModelCircle.setStrokeAlpha(1.0f);
        if (isClicked){
            pathModelArrow.setStrokeWidth(2.0f);
        }
        return result;
    }



    //helper class to store existing bitmaps for taxis
    private class DrawableBitmapCorrespondence {
        int barrioId;
        int barrioColor;
        Drawable barrioDrawable;
        Bitmap barrioBitmap;

        public DrawableBitmapCorrespondence(int barrioId, int barrioColor, Drawable barrioDrawable, Bitmap barrioBitmap) {
            this.barrioId = barrioId;
            this.barrioColor = barrioColor;
            this.barrioDrawable = barrioDrawable;
            this.barrioBitmap = barrioBitmap;
        }
    }

    public void setOffAnimation(final int frames) {
        Collections.sort(mItemList);
        for (int i = 0; i < frames; i++) {
            final int a = i;
            mMap.postDelayed(new Runnable() {
                @Override
                public void run() {
                    for (TaxiMarker marker : mItemList) {
                        marker.executeFrame(a, frames);
//                        if (marker.isClicked){
//                            mConnectionLines.moveLineFromOtherLayer(marker.taxiObject.getTaxiId(),marker.geoPoint);
//                        }
                    }
                    mConnectionLines.updateLines();
                    populate();
                    update();
                    mMap.updateMap(false);
                }
            }, i * 500 / frames);
        }
        mMap.postDelayed(new Runnable() {
            @Override
            public void run() {
                //remove taxis bound to disappear
                Iterator<TaxiMarker> i = mItemList.iterator();
                while (i.hasNext()) {
                    TaxiMarker m = i.next();
                    if (m.getPurpose() == TaxiMarker.Purpose.DISAPPEAR) {
                        //mConnectionLines.removeLine(m.taxiObject.getTaxiId());
                        //mConnectionLines.remove(m.taxiObject.getTaxiId());
                        if(m.isClicked){
                            doUnClick(m);
                        }
                        i.remove();
                    }
                }
                //assume all values from purpose at the end of amin
                for (TaxiMarker marker:mItemList){
                    marker.setTaxiObject(marker.getPurposeTaxiObject());
                }
                populate();
                update();
                mMap.updateMap(false);
            }
        }, 500);
    }


    @Override
    public void onMapEvent(Event e, MapPosition mapPosition) {

        if(e == Map.SCALE_EVENT || e == Map.ANIM_END|| e==Map.ROTATE_EVENT) {
            double scale = mapPosition.getZoom();

            if (mapScale!=mapPosition.getScale()){
                //own anim
                mapScale=mapPosition.getScale();


                //others anim do only when zoom in range

                scaleOtherIcons(scale);
                zoomAdjust(scale);
            }
        }
    }

    private double mZoom2;
    public synchronized void scaleOtherIcons(double zoom){
        mZoom2=zoom;
        int currSize=getDrawableIndex(zoom);
        Bitmap otherSymbol = scaledGrayedSymbols[currSize];
        for (TaxiMarker otherTaxiMarker : mItemList ) {
            otherTaxiMarker.setRotatedSymbol(new MarkerSymbol(otherSymbol, MarkerSymbol.HotspotPlace.CENTER, false));
            if (zoom!=mZoom2){
                return;
            }
        }
        mMap.updateMap(false);

    }

    private void prepareScaledBitmapArray(){
        pathModelArrow.setFillColor(Color.GRAY);
        pathModelArrow.setFillAlpha(0.5f);
        pathModelArrow.setStrokeAlpha(0.0f);
        pathModelCircle.setStrokeAlpha(0.0f);
        for (int i = 0; i < 100; i++) {
            scaledGrayedSymbols[i] = AndroidGraphicsCustom.drawableToBitmap(drawable, 101 + i);
        }
    }

    private double mZoom;
    private synchronized void zoomAdjust(final double zoom){
        if (zoom!=mZoom) {
            mZoom=zoom;
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (mZoom == zoom) {
                        //shrink all cached bitmaps
                        for(DrawableBitmapCorrespondence item:bitmapReferenceList){
                            item.barrioBitmap=AndroidGraphicsCustom.drawableToBitmap(item.barrioDrawable, ZoomUtils.getDrawableSize(zoom));
                            if (zoom!=mZoom){
                                return;
                            }
                        }
                        for (TaxiMarker item:mItemList) {
                            item.setRotatedSymbol(new MarkerSymbol(fetchBitmap(item), MarkerSymbol.HotspotPlace.CENTER,false));
                            //check if this fixes concurrent modification exception
                            if (zoom!=mZoom){
                                return;
                            }
                        }
                        update();
                        mMap.updateMap(true);
                    }
                }
            }, 100);
        }
    }

}
