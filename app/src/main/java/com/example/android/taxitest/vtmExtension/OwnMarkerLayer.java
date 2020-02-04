package com.example.android.taxitest.vtmExtension;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.location.Location;
import android.util.Log;

import com.example.android.taxitest.Compass;
import com.example.android.taxitest.data.TaxiObject;
import com.example.android.taxitest.utils.PaintUtils;
import com.example.android.taxitest.utils.ZoomUtils;
import com.example.android.taxitest.vectorLayer.BarrioPolygonDrawable;
import com.example.android.taxitest.vectorLayer.BarriosLayer;
import com.sdsmdg.harjot.vectormaster.VectorMasterDrawable;
import com.sdsmdg.harjot.vectormaster.models.PathModel;

import org.oscim.backend.canvas.Bitmap;
import org.oscim.core.GeoPoint;
import org.oscim.core.MapPosition;
import org.oscim.event.Event;
import org.oscim.layers.marker.ItemizedLayer;
import org.oscim.layers.marker.MarkerSymbol;
import org.oscim.map.Map;
import org.oscim.utils.ThreadUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static com.example.android.taxitest.utils.ZoomUtils.getDrawableIndex;
import static org.oscim.utils.FastMath.clamp;

public class OwnMarkerLayer extends ItemizedLayer<OwnMarker> implements Map.UpdateListener {

    private OwnMarker ownMarker;
    private Compass mCompass;
    private VectorMasterDrawable drawable;
    private BarriosLayer barriosLayer;
    private Context context;
    private int destId;

    public List<OwnMarkerLayer.DrawableBitmapCorrespondence> bitmapReferenceList= new ArrayList<>();

    //helper vars
    PathModel pathModelArrow;
    PathModel pathModelCircle;
    double mapScale;
    private Bitmap[] scaledGrayedSymbols = new Bitmap[100];



    public OwnMarkerLayer(Context context, BarriosLayer barriosLayer, Map map, List<OwnMarker> list, VectorMasterDrawable defaultDrawable, GeoPoint location, GeoPoint dest, Compass compass) {
        super(map,
                list,
                new MarkerSymbol(AndroidGraphicsCustom.drawableToBitmap(defaultDrawable, ZoomUtils.getDrawableSize(map.getMapPosition().getZoom())), MarkerSymbol.HotspotPlace.CENTER, false),
                null);
        drawable = defaultDrawable;
        this.context = context;
        this.barriosLayer = barriosLayer;
        setOnItemGestureListener(customListener);
        pathModelArrow = drawable.getPathModelByName("arrow");
        pathModelCircle = drawable.getPathModelByName("circle");
        mapScale=map.getMapPosition().getScale();
        mCompass=compass;
        ownMarker=new OwnMarker(location,dest);

        addTaxi(ownMarker);
        prepareScaledBitmapArray();

        mCompass.setCompassUpdateListener(new Compass.CompassUpdateListener() {
            @Override
            public void onCompassChanged(float rotation) {
                mItemList.get(0).setRotation(rotation);
                populate();
                mMap.updateMap(true);
            }
        });
    }

    private ItemizedLayer.OnItemGestureListener<OwnMarker> customListener=new ItemizedLayer.OnItemGestureListener<OwnMarker>() {
        @Override
        public boolean onItemSingleTapUp(int index, OwnMarker item) {

            return false;
        }

        @Override
        public boolean onItemLongPress(int index, OwnMarker item) {
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
    private void addTaxi(OwnMarker item) {
        removeAllItems();
        MarkerSymbol symbol = new MarkerSymbol(fetchBitmap(item.destGeoPoint, item.getIsClicked()), MarkerSymbol.HotspotPlace.CENTER, false);
        item.setRotatedMarker(symbol,mCompass.getRotation());
        mItemList.add(item);
        populate();
    }

    public void moveMarker(GeoPoint point){
        mItemList.get(0).setGeoPoint(point);
        if(!checkDest(mItemList.get(0).destGeoPoint)){
            mItemList.get(0).setRotatedMarker(new MarkerSymbol(fetchBitmap(mItemList.get(0).destGeoPoint,false), MarkerSymbol.HotspotPlace.CENTER,false),mCompass.getRotation());
        }
        populate();
    }


    //checks if bitmap for geopoint already exists else it calculates a new one
    public Bitmap fetchBitmap(GeoPoint geoPoint, boolean isClicked) {
        BarrioPolygonDrawable barrio = barriosLayer.getContainingBarrio(geoPoint);
        this.destId=barrio.getBarrioId();
        if (!isClicked) {
            for (OwnMarkerLayer.DrawableBitmapCorrespondence item : bitmapReferenceList) {
                if (barrio.getBarrioId() == item.barrioId) {
                    return item.barrioBitmap;
                }
            }
            Log.d("referenceList",""+bitmapReferenceList.size());
        }
        int color = barrio.getStyle().fillColor;
        VectorMasterDrawable drawable = modifyDrawable(color, isClicked);
        Bitmap bitmap = AndroidGraphicsCustom.drawableToBitmap(drawable, ZoomUtils.getDrawableSize(mMap.getMapPosition().getZoom()));
        if (!isClicked) {
            bitmapReferenceList.add(new OwnMarkerLayer.DrawableBitmapCorrespondence(barrio.getBarrioId(), color, drawable, bitmap));
        }
        return bitmap;
    }

    private boolean checkDest(GeoPoint geoPoint){
        if (barriosLayer.getContainingBarrio(geoPoint).getBarrioId()==destId){
            return true;
        }else{
            return false;
        }
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

    //animation logic stays in main
    public void move(GeoPoint geoPoint) {
        mItemList.get(0).setGeoPoint(geoPoint);
    }




    @Override
    public void onMapEvent(Event e, MapPosition mapPosition) {

        if(e == Map.SCALE_EVENT || e == Map.ANIM_END|| e==Map.ROTATE_EVENT) {
            double scale = mapPosition.getZoom();

            if (mapScale!=mapPosition.getScale()){
                //own anim
                mapScale=mapPosition.getScale();
                int currSize=getDrawableIndex(scale);
                //others anim
                scaleIcon(currSize);
            }
        }
    }

    public void scaleIcon(int scale){
        Bitmap otherSymbol = scaledGrayedSymbols[scale];
        mItemList.get(0).setRotatedMarker(new MarkerSymbol(otherSymbol, MarkerSymbol.HotspotPlace.CENTER, false),mCompass.getRotation());
        mMap.updateMap(false);

    }


    private void prepareScaledBitmapArray(){
        for (int i = 0; i < 100; i++) {
            scaledGrayedSymbols[i] = AndroidGraphicsCustom.drawableToBitmap(drawable, 101 + i);
        }
    }


}
