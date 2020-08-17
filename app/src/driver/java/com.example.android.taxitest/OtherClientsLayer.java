package com.example.android.taxitest;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.util.Log;

import com.example.android.taxitest.CommunicationsRecyclerView.CommsObject;
import com.example.android.taxitest.CommunicationsRecyclerView.CommunicationsAdapter;
import com.example.android.taxitest.connection.WebSocketDriverLocations;
import com.example.android.taxitest.data.ClientObject;
import com.example.android.taxitest.data.CommRecordObject;
import com.example.android.taxitest.utils.PaintUtils;
import com.example.android.taxitest.utils.ZoomUtils;
import com.example.android.taxitest.vectorLayer.BarrioPolygonDrawable;
import com.example.android.taxitest.vectorLayer.BarriosLayer;
import com.example.android.taxitest.vectorLayer.ConnectionLineLayer2;
import com.example.android.taxitest.vtmExtension.AndroidGraphicsCustom;
import com.example.android.taxitest.vtmExtension.OtherTaxiLayer;
import com.example.android.taxitest.vtmExtension.TaxiMarker;
import com.sdsmdg.harjot.vectormaster.VectorMasterDrawable;
import com.sdsmdg.harjot.vectormaster.models.PathModel;

import org.oscim.backend.canvas.Bitmap;
import org.oscim.layers.marker.MarkerSymbol;
import org.oscim.map.Map;

import java.util.ArrayList;
import java.util.List;

public class OtherClientsLayer extends OtherTaxiLayer {

    PathModel pathModelArrow, pathModelCircle, pathModelNumber;
    VectorMasterDrawable drawable1, drawable2, drawable3, drawable4;
    List<DrawableBitmapCorrespondence> bitmapReferenceList= new ArrayList<>();

    public OtherClientsLayer(Context context, BarriosLayer barriosLayer, Map map, List<TaxiMarker> list,
                             WebSocketDriverLocations webSocketConnection,
                             ConnectionLineLayer2 connectionLineLayer, CommunicationsAdapter commsAdapter) {
        super(context, barriosLayer, map, list, webSocketConnection, connectionLineLayer, commsAdapter);
        isBillboard=true;
        placement=MarkerSymbol.HotspotPlace.BOTTOM_CENTER;
    }



    @Override
    public void loadVectorDrawable() {
        drawable1=new VectorMasterDrawable(context, R.drawable.icon_client_1);
        drawable2=new VectorMasterDrawable(context, R.drawable.icon_client_2);
        drawable3=new VectorMasterDrawable(context, R.drawable.icon_client_3);
        drawable4=new VectorMasterDrawable(context, R.drawable.icon_client_4);
        pathModelArrow = drawable1.getPathModelByName("arrow");
        pathModelCircle = drawable1.getPathModelByName("circle");
        pathModelNumber = drawable1.getPathModelByName("number");

    }

    @Override
    public MarkerSymbol fetchBitmap(TaxiMarker taxiMarker) {
        BarrioPolygonDrawable barrio = barriosLayer.getContainingBarrio(taxiMarker.destGeoPoint);
        taxiMarker.color=barrio.getStyle().fillColor;
        taxiMarker.barrio=barrio.getBarrioName();
        int seats=((ClientObject) taxiMarker.taxiObject).getSeats();
        if (!taxiMarker.getIsClicked()) {
            for (DrawableBitmapCorrespondence item : bitmapReferenceList) {
                if (barrio.getBarrioId() == item.barrioId && seats==item.seats) {
                    return new MarkerSymbol(item.barrioBitmap, placement,isBillboard);
                }
            }
            Log.d("referenceList",""+bitmapReferenceList.size());
        }
        int color = barrio.getStyle().fillColor;
        VectorMasterDrawable drawable = modifyDrawable(color, seats, taxiMarker.getIsClicked());
        Bitmap bitmap = AndroidGraphicsCustom.drawableToBitmap(drawable, ZoomUtils.getDrawableSize(mMap.getMapPosition().getZoom()));
        if (!taxiMarker.getIsClicked()) {
            bitmapReferenceList.add(new DrawableBitmapCorrespondence(barrio.getBarrioId(), color, seats, drawable, bitmap));
        }
        return new MarkerSymbol(bitmap, placement,isBillboard);
    }


    public VectorMasterDrawable modifyDrawable(int color, int seats, boolean isClicked) {
        VectorMasterDrawable drawable;
        switch (seats){
            case 1:
                drawable=drawable1;
                break;
            case 2:
                drawable=drawable2;
                break;
            case 3:
                drawable=drawable3;
                break;
            default:
                drawable=drawable4;
                break;
        }
        VectorMasterDrawable result = new VectorMasterDrawable(context,drawable.getResID());
        PathModel pathModelArrow=result.getPathModelByName("arrow");
        pathModelArrow.setFillColor(color);
        pathModelArrow.setFillAlpha(1.0f);
        pathModelArrow.setStrokeAlpha(1.0f);
        if (isClicked){
            pathModelArrow.setStrokeWidth(2.0f);
        }
        return result;
    }

    public class DrawableBitmapCorrespondence {
        public int barrioId;
        public int barrioColor;
        public int seats;
        public Drawable barrioDrawable;
        public Bitmap barrioBitmap;

        public DrawableBitmapCorrespondence(int barrioId, int barrioColor, int seats, Drawable barrioDrawable, Bitmap barrioBitmap) {
            this.barrioId = barrioId;
            this.barrioColor = barrioColor;
            this.seats=seats;
            this.barrioDrawable = barrioDrawable;
            this.barrioBitmap = barrioBitmap;
        }
    }

    @Override
    public void prepareScaleAndAppearanceTransitions() {
        pathModelArrow.setFillColor(Color.GRAY);
        pathModelArrow.setFillAlpha(0.5f);
        pathModelArrow.setStrokeAlpha(0.0f);
        pathModelCircle.setStrokeAlpha(0.0f);
        pathModelNumber.setStrokeAlpha(0.0f);
        pathModelCircle.setFillAlpha(0.0f);
        pathModelNumber.setFillAlpha(0.0f);
        for (int i = 0; i < 40; i++) {
            scaledGrayedSymbols[i] = AndroidGraphicsCustom.drawableToBitmap(drawable1, 41 + i);
            appearDisappearAnim[i]=AndroidGraphicsCustom.drawableToBitmap(drawable1,2+2*i);
        }
    }

    @Override
    public synchronized void zoomAdjust(final double zoom) {
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
                            item.setRotatedSymbol(fetchBitmap(item));
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
