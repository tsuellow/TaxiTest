package com.example.android.taxitest.vtmExtension;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.core.graphics.ColorUtils;

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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static com.example.android.taxitest.utils.ZoomUtils.getDrawableIndex;
import static com.example.android.taxitest.utils.ZoomUtils.getDrawableSize;

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



    public OtherTaxiLayer(Context context, BarriosLayer barriosLayer, Map map, List<TaxiMarker> list, VectorMasterDrawable defaultDrawable) {
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
        prepareScaledBitmapArray();
    }

    private OnItemGestureListener<TaxiMarker> customListener=new OnItemGestureListener<TaxiMarker>() {
        @Override
        public boolean onItemSingleTapUp(int index, TaxiMarker item) {
            int currSize=getDrawableSize(mZoom);

            if (item.getIsClicked()){
                item.setIsClicked(false);
            }else{
                item.setIsClicked(true);
            }
            item.setRotatedSymbol(new MarkerSymbol(fetchBitmap(item.taxiObject,item.getIsClicked()), MarkerSymbol.HotspotPlace.CENTER,false));
            update();
            mMap.updateMap(true);

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
        MarkerSymbol symbol = new MarkerSymbol(fetchBitmap(item.taxiObject, item.getIsClicked()), MarkerSymbol.HotspotPlace.CENTER, false);
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

    //checks if bitmap for geopoint already exists else it calculates a new one
    public Bitmap fetchBitmap(TaxiObject taxiObject, boolean isClicked) {
        GeoPoint point=new GeoPoint(taxiObject.getDestinationLatitude(),taxiObject.getDestinationLongitude());
        BarrioPolygonDrawable barrio = barriosLayer.getContainingBarrio(point);
        if (!isClicked) {
            for (DrawableBitmapCorrespondence item : bitmapReferenceList) {
                if (barrio.getBarrioId() == item.barrioId) {
                    return item.barrioBitmap;
                }
            }
        }
        int color = barrio.getStyle().fillColor;
        VectorMasterDrawable drawable = modifyDrawable(color, isClicked);
        Bitmap bitmap = AndroidGraphicsCustom.drawableToBitmap(drawable, ZoomUtils.getDrawableSize(mMap.getMapPosition().getZoom()));
        if (!isClicked) {
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
                    }
                    populate();
                    update();
                    mMap.updateMap(false);
                }
            }, i * 500 / frames);
            mMap.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Iterator<TaxiMarker> i = mItemList.iterator();
                    while (i.hasNext()) {
                        TaxiMarker m = i.next();
                        if (m.getPurpose() == TaxiMarker.Purpose.DISAPPEAR) {
                            i.remove();
                        }
                    }
                    populate();
                    update();
                    mMap.updateMap(false);
                }
            }, 500);
        }
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
                scaleOtherIcons(currSize);
                zoomAdjust(scale);
            }
        }
    }

    public void scaleOtherIcons(int scale){

        Bitmap otherSymbol = scaledGrayedSymbols[scale];
        for (TaxiMarker otherTaxiMarker : mItemList ) {
            otherTaxiMarker.setRotatedSymbol(new MarkerSymbol(otherSymbol, MarkerSymbol.HotspotPlace.CENTER, false));
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
    private void zoomAdjust(final double zoom){
        if (zoom!=mZoom) {
            mZoom=zoom;
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (mZoom == zoom) {

                        //shrink all cached bitmaps
                        for(DrawableBitmapCorrespondence item:bitmapReferenceList){
                            item.barrioBitmap=AndroidGraphicsCustom.drawableToBitmap(item.barrioDrawable, ZoomUtils.getDrawableSize(zoom));
                        }

                        for (TaxiMarker item:mItemList) {

                            item.setRotatedSymbol(new MarkerSymbol(fetchBitmap(item.taxiObject,item.getIsClicked()), MarkerSymbol.HotspotPlace.CENTER,false));
                        }


                        update();
                        mMap.updateMap(true);

                    }
                }
            }, 100);
        }
    }

}
