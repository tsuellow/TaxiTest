package com.dale.viaje.nicaragua;

import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import com.dale.viaje.nicaragua.CommunicationsRecyclerView.CommunicationsAdapter;
import com.dale.viaje.nicaragua.connection.IncomingUdpSocket;
import com.dale.viaje.nicaragua.data.ClientObject;
import com.dale.viaje.nicaragua.utils.ZoomUtils;
import com.dale.viaje.nicaragua.vectorLayer.BarrioPolygonDrawable;
import com.dale.viaje.nicaragua.vectorLayer.BarriosLayer;
import com.dale.viaje.nicaragua.vectorLayer.ConnectionLineLayer2;
import com.dale.viaje.nicaragua.vtmExtension.AndroidGraphicsCustom;
import com.dale.viaje.nicaragua.vtmExtension.OtherTaxiLayer;
import com.dale.viaje.nicaragua.vtmExtension.TaxiMarker;
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
//    List<DrawableBitmapCorrespondence> bitmapReferenceList= new ArrayList<>();

    public OtherClientsLayer(Context context, BarriosLayer barriosLayer, Map map, List<TaxiMarker> list,
                             IncomingUdpSocket webSocketConnection,
                             ConnectionLineLayer2 connectionLineLayer, CommunicationsAdapter commsAdapter) {
        super(context, barriosLayer, map, list, webSocketConnection, connectionLineLayer, commsAdapter);

    }



    @Override
    public void loadVectorDrawable() {
        drawable1=new VectorMasterDrawable(context, R.drawable.icon_client_1_big);
        drawable2=new VectorMasterDrawable(context, R.drawable.icon_client_2_big);
        drawable3=new VectorMasterDrawable(context, R.drawable.icon_client_3_big);
        drawable4=new VectorMasterDrawable(context, R.drawable.icon_client_4_big);
        pathModelArrow = drawable1.getPathModelByName("arrow");
        pathModelCircle = drawable1.getPathModelByName("circle");
        pathModelNumber = drawable1.getPathModelByName("number");

    }

    @Override
    public MarkerSymbol fetchBitmap(TaxiMarker taxiMarker) {
        BarrioPolygonDrawable barrio = barriosLayer.getContainingBarrio(taxiMarker.destGeoPoint);
        taxiMarker.setDestColor(barrio.getStyle().fillColor,barrio.getBarrioName());
//        if (!taxiMarker.getIsClicked()) {
//            for (DrawableBitmapCorrespondence item : bitmapReferenceList) {
//                if (barrio.getBarrioId() == item.barrioId && seats==item.seats) {
//                    return new MarkerSymbol(item.barrioBitmap, placement,isBillboard);
//                }
//            }
//            Log.d("referenceList",""+bitmapReferenceList.size());
//        }
        VectorMasterDrawable drawable = modifyDrawable(taxiMarker);
        Bitmap bitmap = AndroidGraphicsCustom.drawableToBitmap(drawable, ZoomUtils.getDrawableSize(mMap.getMapPosition().getZoom()));
//        if (!taxiMarker.getIsClicked()) {
//            bitmapReferenceList.add(new DrawableBitmapCorrespondence(barrio.getBarrioId(), color, seats, drawable, bitmap));
//        }
        return new MarkerSymbol(bitmap, placement,isBillboard);
    }


    public VectorMasterDrawable modifyDrawable(TaxiMarker taxiMarker) {
        VectorMasterDrawable drawable;
        int seats=((ClientObject)taxiMarker.taxiObject).getSeats();
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
        PathModel pathModelCircle=result.getPathModelByName("circle");
        pathModelArrow.setFillColor(taxiMarker.color);
        pathModelArrow.setFillAlpha(taxiMarker.getAlphaValue());
        //pathModelCircle.setFillAlpha(taxiMarker.getAlphaValue());
        if (taxiMarker.isClicked){
            pathModelArrow.setStrokeWidth(2.0f);
        }
        return result;
    }

//    public class DrawableBitmapCorrespondence {
//        public int barrioId;
//        public int barrioColor;
//        public int seats;
//        public Drawable barrioDrawable;
//        public Bitmap barrioBitmap;
//
//        public DrawableBitmapCorrespondence(int barrioId, int barrioColor, int seats, Drawable barrioDrawable, Bitmap barrioBitmap) {
//            this.barrioId = barrioId;
//            this.barrioColor = barrioColor;
//            this.seats=seats;
//            this.barrioDrawable = barrioDrawable;
//            this.barrioBitmap = barrioBitmap;
//        }
//    }

    @Override
    public void prepareScaleAndAppearanceTransitions() {
        isBillboard=true;
        placement=MarkerSymbol.HotspotPlace.BOTTOM_CENTER;

        pathModelArrow.setFillColor(Color.GRAY);
        pathModelArrow.setFillAlpha(0.5f);
        pathModelArrow.setStrokeAlpha(0.0f);
        pathModelCircle.setStrokeAlpha(0.0f);
        pathModelNumber.setStrokeAlpha(0.0f);
        pathModelCircle.setFillAlpha(0.0f);
        pathModelNumber.setFillAlpha(0.0f);
        for (int i = 0; i < 40; i++) {
            scaledGrayedSymbols[i] = new MarkerSymbol(AndroidGraphicsCustom.drawableToBitmap(drawable1, 41 + i),placement,isBillboard);
            appearDisappearAnim[i]=new MarkerSymbol(AndroidGraphicsCustom.drawableToBitmap(drawable1,2+2*i),placement,isBillboard);
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
//                        for(DrawableBitmapCorrespondence item:bitmapReferenceList){
//                            item.barrioBitmap=AndroidGraphicsCustom.drawableToBitmap(item.barrioDrawable, ZoomUtils.getDrawableSize(zoom));
//                            if (zoom!=mZoom){
//                                return;
//                            }
//                        }
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
