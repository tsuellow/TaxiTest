package com.example.android.taxitest.vtmExtension;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.example.android.taxitest.CommunicationsRecyclerView.AcknowledgementObject;
import com.example.android.taxitest.CommunicationsRecyclerView.CommsObject;
import com.example.android.taxitest.CommunicationsRecyclerView.CommunicationsAdapter;
import com.example.android.taxitest.CommunicationsRecyclerView.MessageObject;
import com.example.android.taxitest.CommunicationsRecyclerView.MetaMessageObject;
import com.example.android.taxitest.R;
import com.example.android.taxitest.connection.WebSocketDriverLocations;
import com.example.android.taxitest.data.SocketObject;
import com.example.android.taxitest.utils.MiscellaneousUtils;
import com.example.android.taxitest.utils.PaintUtils;
import com.example.android.taxitest.utils.ZoomUtils;
import com.example.android.taxitest.vectorLayer.BarrioPolygonDrawable;
import com.example.android.taxitest.vectorLayer.BarriosLayer;
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
import static com.example.android.taxitest.utils.ZoomUtils.getDrawableSize;

public class OtherTaxiLayer extends ItemizedLayer<TaxiMarker> implements Map.UpdateListener
{

    public VectorMasterDrawable drawable;
    public BarriosLayer barriosLayer;
    public Context context;
    public boolean isBillboard;

    public List<DrawableBitmapCorrespondence> bitmapReferenceList= new ArrayList<>();

    //helper vars
    PathModel pathModelArrow;
    PathModel pathModelCircle;
    double mapScale;
    public Bitmap[] scaledGrayedSymbols = new Bitmap[40];
    public Bitmap[] appearDisappearAnim = new Bitmap[40];
    private WebSocketDriverLocations mWebSocketConnection;
    private ConnectionLineLayer2 mConnectionLines;
    private CommunicationsAdapter mCommunicationsAdapter;



    public OtherTaxiLayer(Context context, BarriosLayer barriosLayer, Map map, List<TaxiMarker> list,
                          WebSocketDriverLocations webSocketConnection,
                          ConnectionLineLayer2 connectionLineLayer, CommunicationsAdapter commsAdapter) {
        super(map,
                list,
                (MarkerSymbol) null,//new MarkerSymbol(AndroidGraphicsCustom.drawableToBitmap(defaultDrawable, ZoomUtils.getDrawableSize(map.getMapPosition().getZoom())), MarkerSymbol.HotspotPlace.CENTER, false),
                null);
        this.context = context;
        loadVectorDrawable();
        this.barriosLayer = barriosLayer;
        this.mWebSocketConnection=webSocketConnection;
        initializeWebSocket();
        mConnectionLines =connectionLineLayer;
        mCommunicationsAdapter=commsAdapter;
        mCommunicationsAdapter.setConnectionLines(mConnectionLines);
        initializeCommsInvitationsProcessor();

        setOnItemGestureListener(customListener);


        mapScale=map.getMapPosition().getScale();

        prepareScaleAndAppearanceTransitions();
        isBillboard=false;
    }

    public void loadVectorDrawable(){
        drawable=new VectorMasterDrawable(context, R.drawable.icon_taxi);
        pathModelArrow = drawable.getPathModelByName("arrow");
        pathModelCircle = drawable.getPathModelByName("circle");
    }

    boolean hasPayload=false;
    public void initializeCommsInvitationsProcessor(){
        mCommunicationsAdapter.setMessageInvitationListener(new CommunicationsAdapter.MessageInvitationListener() {
            @Override
            public void onInvitationReceived(MessageObject msj) {
                final MessageObject msj1=msj;
                int id= MiscellaneousUtils.getNumericId(msj.getSendingId());
                final TaxiMarker tm=findTaxi(id);
                Log.d("socketTest","incomming id"+tm.taxiObject.getTaxiId());
                if (msj.getIntentCode()==CommsObject.REQUEST_SENT) { //only mind new, not jet initialized invitations
                    if (tm != null) {
                        //normally when a new message is received from a taxi with which no comm is established yet we simply do as if we were clicking it and adding the msj
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                CommsObject comm = doClick(tm);
                                //you need to do the following as both the msj and the ack arrive/are sent before the commsObject exists properly
                                MetaMessageObject metaMsj=new MetaMessageObject(msj1, comm);
                                metaMsj.addAckAtTopOfList(new AcknowledgementObject(msj1,CommsObject.RECEIVED));
                                comm.addAtTopOfMsjList(metaMsj);
                            }
                        });

                    } else {
                        //if a taxi is not visible because it was filtered out we first have to make it appear
                        hasPayload = true;
                        if (!mWebSocketConnection.getProcessIsRunning()) {
                            mWebSocketConnection.startAccumulationTimer();
                        }
                    }
                }
            }
        });
        mCommunicationsAdapter.setMessageCancellationListener(new CommunicationsAdapter.MessageCancellationListener() {
            @Override
            public void onCancellationReceived(MessageObject msj) {
                int id= MiscellaneousUtils.getNumericId(msj.getSendingId());
                final TaxiMarker tm=findTaxi(id);
                if (tm!=null){
                    //normally when a new message is received from a taxi with which no comm is established yet we simply do as if we were clicking it and adding the msj
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(context,tm.taxiObject.getTaxiId()+" te mando a la verga",Toast.LENGTH_LONG).show();
                            doUnClick(tm);
                        }
                    });

                }
            }
        });
        mCommunicationsAdapter.setCommAcceptedListener(new CommunicationsAdapter.CommAcceptedListener() {
            @Override
            public void onCommAccepted(CommsObject comm) {
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(context,"comm was just accepted", Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }

    private void runPostAnimationTasks(){
        if (hasPayload){
            Iterator<MessageObject> i = mCommunicationsAdapter.newIncomingComms.iterator();
            while (i.hasNext()) {
                MessageObject msj = i.next();
                int id= MiscellaneousUtils.getNumericId(msj.getSendingId());
                TaxiMarker tm=findTaxi(id);
                if (tm!=null){
                    if (!tm.getIsClicked()) {
                        //when this is the first message we receive from this taxi, we first click the taxi and then add the msg
                        CommsObject comm=doClick(tm);
                        comm.addAtTopOfMsjList(new MetaMessageObject(msj,comm));
                    }else{
                        //if taxi was already clicked we just find the relevant comm and add the msj
                        CommsObject comm=mCommunicationsAdapter.getItemList().get(mCommunicationsAdapter.getCommIndex(tm.taxiObject.getTaxiId()));
                        comm.addAtTopOfMsjList(new MetaMessageObject(msj,comm));
                    }
                    i.remove();
                }
            }
            if (mCommunicationsAdapter.newIncomingComms.size()==0){
                hasPayload=false;
            }
//            if (hasPayload){
//                mWebSocketConnection.startAccumulationTimer();
//            }
        }
    }

    public void initializeWebSocket(){
        mWebSocketConnection.initializeSocketListener();
        mWebSocketConnection.startAccumulationTimer();
        mWebSocketConnection.connectSocket();
        //listener for websocket data accumulation result every 3 seconds
        mWebSocketConnection.setAnimationDataListener(new WebSocketDriverLocations.AnimationDataListener() {
            @Override
            public void onAnimationParametersReceived(List<? extends SocketObject> baseTaxis, List<? extends SocketObject> newTaxis) {
                //give existing taxis a purpose
                boolean allOk=true;
                if (baseTaxis.size()>0) {
                    for (int i = 0; i < baseTaxis.size(); i++) {
                        SocketObject taxiObject = baseTaxis.get(i);
                        if (taxiObject.getIsActive() != 1) {
                            mItemList.get(i).setPurpose(TaxiMarker.Purpose.DISAPPEAR);
                        } else {
                            //TODO figure out how to fix this bug
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
                            mItemList.get(i).setRotatedSymbol(fetchBitmap(mItemList.get(i)));
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
                    for (SocketObject tO:baseTaxis){
                        addNewTaxi(tO);
                    }
                }
                //add new taxis
                for (SocketObject tO:newTaxis){
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

    public CommsObject doClick(TaxiMarker item){
        item.setIsClicked(true);
        item.setRotatedSymbol(fetchBitmap(item));
        CommsObject comm=new CommsObject(item,context);
        mCommunicationsAdapter.addItem(comm);
        mConnectionLines.addLine(item);

        update();
        mMap.updateMap(true);
        mMap.render();
        return comm;
    }

    public void doUnClick(TaxiMarker item){
        item.setIsClicked(false);
        //mConnectionLines.removeLine(item.taxiObject.getTaxiId());
        mConnectionLines.remove(item.taxiObject.getTaxiId());
        mCommunicationsAdapter.cancelById(item.taxiObject.getTaxiId());
        CommunicationsAdapter.soundPool.play(CommunicationsAdapter.soundCanceled,1,1,0,0,1);
        item.setRotatedSymbol(fetchBitmap(item));
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
            return true;
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
        MarkerSymbol symbol = new MarkerSymbol(appearDisappearAnim[0], MarkerSymbol.HotspotPlace.CENTER, isBillboard);
        item.setRotatedSymbol(symbol);
        mItemList.add(item);
        populate();
    }

    public TaxiMarker findTaxi(int taxiId){
        for(TaxiMarker tm:mItemList){
            if (tm.taxiObject.getTaxiId()==taxiId){
                return tm;
            }
        }
        return null;
    }

    public void addNewTaxi(SocketObject taxiObject){
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
    public MarkerSymbol fetchBitmap(TaxiMarker taxiMarker) {
        BarrioPolygonDrawable barrio = barriosLayer.getContainingBarrio(taxiMarker.destGeoPoint);
        taxiMarker.color=barrio.getStyle().fillColor;
        taxiMarker.barrio=barrio.getBarrioName();
        if (!taxiMarker.getIsClicked()) {
            for (DrawableBitmapCorrespondence item : bitmapReferenceList) {
                if (barrio.getBarrioId() == item.barrioId) {
                    return new MarkerSymbol(item.barrioBitmap, MarkerSymbol.HotspotPlace.CENTER,isBillboard);
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
        return new MarkerSymbol(bitmap, MarkerSymbol.HotspotPlace.CENTER,isBillboard);
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
    public class DrawableBitmapCorrespondence {
        public int barrioId;
        public int barrioColor;
        public Drawable barrioDrawable;
        public Bitmap barrioBitmap;

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

                        if (marker.getPurpose()== TaxiMarker.Purpose.MOVE){
                            marker.executeFrame(a, frames);
                        }else if (marker.getPurpose()== TaxiMarker.Purpose.APPEAR){
                            //play appear animation
                            Bitmap appearDisappear=getAppearDisappearFrame(a,frames,true);
                            MarkerSymbol symbol=new MarkerSymbol(appearDisappear, MarkerSymbol.HotspotPlace.CENTER,isBillboard);
                            marker.setRotatedSymbol(symbol);
                        }else{
                            //play disappear animation
                            marker.setRotatedSymbol(new MarkerSymbol(getAppearDisappearFrame(a,frames,false), MarkerSymbol.HotspotPlace.CENTER,isBillboard));
                        }
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
                        if(m.isClicked){
                            doUnClick(m);
                        }
                        i.remove();
                    }else if (m.getPurpose() == TaxiMarker.Purpose.APPEAR){
                        m.setRotatedSymbol(fetchBitmap(m));
                    }
                }
                //assume all values from purpose at the end of amin
                for (TaxiMarker marker:mItemList){
                    marker.setTaxiObject(marker.getPurposeTaxiObject());
                }
                populate();
                update();
                mMap.updateMap(false);
                mWebSocketConnection.setProcessIsRunning(false);
                runPostAnimationTasks();
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
            otherTaxiMarker.setRotatedSymbol(new MarkerSymbol(otherSymbol, MarkerSymbol.HotspotPlace.CENTER, isBillboard));
            if (zoom!=mZoom2){
                return;
            }
        }
        mMap.updateMap(false);

    }

    public void prepareScaleAndAppearanceTransitions(){
        pathModelArrow.setFillColor(Color.GRAY);
        pathModelArrow.setFillAlpha(0.5f);
        pathModelArrow.setStrokeAlpha(0.0f);
        pathModelCircle.setStrokeAlpha(0.0f);
        for (int i = 0; i < 40; i++) {
            scaledGrayedSymbols[i] = AndroidGraphicsCustom.drawableToBitmap(drawable, 41 + i);
            appearDisappearAnim[i]=AndroidGraphicsCustom.drawableToBitmap(drawable,2+2*i);
        }
    }

    //TODO this is causing fatal exceptions it was trying to access frame 100 where 99 is the max value. rethink this
    private Bitmap getAppearDisappearFrame(int frame, int totalFrames, boolean appear){

        int maxSizeIndex=getDrawableIndex(mZoom2);
        int currSizeIndex=maxSizeIndex*frame/totalFrames;
        Log.d("wtf bitmap"," maxSize:"+maxSizeIndex+" currSizeInd:"+currSizeIndex);
        Bitmap bitmap;
        if (appear){
            bitmap= appearDisappearAnim[currSizeIndex];
        }else{
            bitmap= appearDisappearAnim[maxSizeIndex-currSizeIndex];
        }
        return bitmap;
    }



    public double mZoom;
    public synchronized void zoomAdjust(final double zoom){
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
