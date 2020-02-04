package com.example.android.taxitest;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.os.Environment;
import android.os.Vibrator;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat;

import com.example.android.taxitest.connection.WebSocketConnection;
import com.example.android.taxitest.data.SqlLittleDB;
import com.example.android.taxitest.data.TaxiObject;
import com.example.android.taxitest.vectorLayer.BarriosLayer;
import com.example.android.taxitest.vectorLayer.GeoJsonUtils;
import com.example.android.taxitest.vtmExtension.AndroidGraphicsCustom;
import com.example.android.taxitest.vtmExtension.OtherTaxiLayer;
import com.example.android.taxitest.vtmExtension.OwnMarker;
import com.example.android.taxitest.vtmExtension.OwnMarkerLayer;
import com.example.android.taxitest.vtmExtension.TaxiMarker;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.sdsmdg.harjot.vectormaster.VectorMasterDrawable;
import com.sdsmdg.harjot.vectormaster.models.PathModel;

import org.geojson.FeatureCollection;
import org.oscim.android.MapView;
import org.oscim.backend.CanvasAdapter;
import org.oscim.backend.canvas.Bitmap;
import org.oscim.backend.canvas.Color;
import org.oscim.core.GeoPoint;
import org.oscim.core.MapPosition;
import org.oscim.event.Event;
import org.oscim.event.Gesture;
import org.oscim.event.GestureListener;
import org.oscim.event.MotionEvent;
import org.oscim.layers.Layer;
import org.oscim.layers.marker.ItemizedLayer;
import org.oscim.layers.marker.MarkerSymbol;
import org.oscim.layers.tile.buildings.BuildingLayer;
import org.oscim.layers.tile.vector.VectorTileLayer;
import org.oscim.layers.tile.vector.labeling.LabelLayer;
import org.oscim.map.Map;
import org.oscim.renderer.GLViewport;
import org.oscim.scalebar.DefaultMapScaleBar;
import org.oscim.scalebar.MapScaleBar;
import org.oscim.scalebar.MapScaleBarLayer;
import org.oscim.theme.VtmThemes;
import org.oscim.tiling.source.mapfile.MapFileTileSource;
import org.oscim.utils.ThreadUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static com.example.android.taxitest.utils.ZoomUtils.getDrawableSize;
import static org.oscim.utils.FastMath.clamp;

public class MainActivity extends Activity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener{

    private MapView mapView;

    private MapScaleBar mapScaleBar;
    private Context mContext;
    private Location mMarkerLoc;
    private Compass mCompass;
    private ImageView compassImage;
    private ImageView backToCenterImage;

    Vibrator mVibrator;


    private float mTilt;
    private double mScale;

    MarkerSymbol symbol;
    MarkerSymbol otherSymbol;

    TaxiObject mOwnTaxiObject;

    ItemizedLayer<TaxiMarker> mMarkerLayer;
    OwnMarkerLayer mOwnMarkerLayer;
    OtherTaxiLayer mOtherTaxisLayer;

    private MarkerSymbol[] mScaleSymbols = new MarkerSymbol[100];
    private Bitmap[] mOtherScaleSymbols = new Bitmap[100];

    private Location endLocation=new Location("");
    private Location mCurrMapLoc =new Location("");;


    //experiment
    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;

    //consider deleting
    private boolean mClicked=false;


    WebSocketConnection mWebSocketConnection;
    SqlLittleDB mDb;


    List<Integer> clickedItems=new ArrayList<Integer>();
    Drawable clickedIcon;
    VectorMasterDrawable otherIcon;


    private InputStream geoJsonIs;

    BarriosLayer mVectorLayer;

    AnimatedVectorDrawableCompat advCompat;
    AnimatedVectorDrawable adv;

    boolean wasMoved=false;
    int mCurrSize=99;



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_tilemap);
        ActivityCompat.requestPermissions(MainActivity.this, new String[]{READ_EXTERNAL_STORAGE,ACCESS_FINE_LOCATION, WRITE_EXTERNAL_STORAGE},111);

        //initialize multiple sensors and permissions
        initializeServices();

        mDb= SqlLittleDB.getInstance(getApplicationContext());

        // we build google api client
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(3000);
        locationRequest.setFastestInterval(500);

        // Map view
        mapView = (MapView) findViewById(R.id.mapView);
        compassImage = (ImageView) findViewById(R.id.compass);
        backToCenterImage = (ImageView) findViewById(R.id.back_to_center);

        mOwnTaxiObject=new TaxiObject(Constants.myId,0.0,0.0,new Date().getTime(),0.0f,Constants.userType,0.0,0.0,1);




        // Tile source

        MapFileTileSource tileSource = new MapFileTileSource();
        copyFileToExternalStorage(R.raw.nicaragua);//put in async task
        File file=new File(Environment.getExternalStorageDirectory(), Constants.MAP_FILE);
        String mapPath = file.getAbsolutePath();
        //tileSource.setMapFile(mapPath);


        if (tileSource.setMapFile(mapPath)) {
            Toast.makeText(this,"success",Toast.LENGTH_LONG).show();
            // Vector layer
            VectorTileLayer tileLayer = mapView.map().setBaseMap(tileSource);
            // Building layer
            mapView.map().layers().add(new BuildingLayer(mapView.map(), tileLayer));
            // Label layer
            mapView.map().layers().add(new LabelLayer(mapView.map(), tileLayer));
            // Render theme
            mapView.map().setTheme(VtmThemes.DEFAULT);
            //add set pivot
            mapView.map().viewport().setMapViewCenter(0.0f, 0.75f);
            // Scale bar
            mapScaleBar = new DefaultMapScaleBar(mapView.map());
            MapScaleBarLayer mapScaleBarLayer = new MapScaleBarLayer(mapView.map(), mapScaleBar);
            mapScaleBarLayer.getRenderer().setPosition(GLViewport.Position.BOTTOM_LEFT);
            mapScaleBarLayer.getRenderer().setOffset(5 * CanvasAdapter.getScale(), 0);
            mapView.map().layers().add(mapScaleBarLayer);
            mTilt = mapView.map().viewport().getMinTilt();
            mScale = 1 << 17;
            //mMarkerLayer = new ItemizedLayer<TaxiMarker>(mapView.map(), new ArrayList<TaxiMarker>(), symbol, null);
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ) {
                fusedLocationProviderClient.getLastLocation()
                        .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                            @Override
                            public void onSuccess(Location location) {
                                // Got last known location. In some rare situations this can be null.
                                if (location != null) {
                                    mMarkerLoc = location;
                                    mMarkerLoc.setLatitude(mMarkerLoc.getLatitude()-0.0);
                                    mMarkerLoc.setLongitude(mMarkerLoc.getLongitude()-0.0);
                                    //setOwnMarker(mMarkerLoc);
                                    mOwnMarkerLayer.moveMarker(new GeoPoint(mMarkerLoc.getLatitude(),mMarkerLoc.getLongitude()));
                                    mapView.map().setMapPosition(mMarkerLoc.getLatitude(), mMarkerLoc.getLongitude(), mScale);
                                }
                            }
                        });
            }

            locationCallback = new LocationCallback() {
                @Override
                public void onLocationResult(LocationResult locationResult) {
                    if (locationResult == null) {
                        return;
                    }
                    //smoothen transition to new spot
                    Location adjustedLocation = locationResult.getLastLocation();
                    adjustedLocation.setLatitude(adjustedLocation.getLatitude()-0.0);
                    adjustedLocation.setLongitude(adjustedLocation.getLongitude()-0.0);
                    endLocation=adjustedLocation;
                    mCompass.setCurrLocation(endLocation);
                    if (mCurrMapLoc != null && mMarkerLoc != null && !mClicked) {
//                        MapPosition mapPosition=mapView.map().getMapPosition();
//                        mapPosition.setPosition(new GeoPoint(endLocation.getLatitude(),endLocation.getLongitude()));
                        //use regular smoothen function
                        if (mCurrMapLoc != null && mMarkerLoc != null && !mClicked) {
                            //smoothenMapMovement(mCurrMapLoc, mMarkerLoc, endLocation);
                            startMoveAnim(500);

                        }
                        //mapView.map().animator().animateTo(mapPosition);
                        //mMarkerLoc = endLocation;

                    }
                    //emit current position
                    mOwnTaxiObject=new TaxiObject(Constants.myId,endLocation.getLatitude(),endLocation.getLongitude(),endLocation.getTime(),mCompass.getRotation(),"taxi",0.0,0.0,1);
                    mWebSocketConnection.attemptSend(mOwnTaxiObject.taxiObjectToCsv());
                }

                ;
            };


        }
        //add compass to map
        mCompass = new Compass(this, mapView.map(), compassImage);
        mCompass.setEnabled(true);
        mCompass.setMode(Compass.Mode.C2D);
        mapView.map().layers().add(mCompass);


        mapView.map().layers().add(new MapEventsReceiver(mapView));
        Drawable icon = getResources().getDrawable(R.drawable.frame00);
        otherIcon=new VectorMasterDrawable(this,R.drawable.frame00);
        //otherIcon.setTint(Color.BLUE);
        Drawable grayedIcon= icon.getConstantState().newDrawable().mutate();
        grayedIcon.setTint(Color.GRAY);
        grayedIcon.setAlpha(75);
        Bitmap bitmapPoi = AndroidGraphicsCustom.drawableToBitmap(icon, 200);
        final Bitmap otherBitmap=AndroidGraphicsCustom.drawableToBitmap(otherIcon, 200);

//        for (int i = 0; i <= 18; i++) {
//            String name = (i < 10) ? "frame0" : "frame";
//            Drawable drawable = getResources().getDrawable(this.getResources().getIdentifier(name + i, "drawable", this.getPackageName()));
//            mClickAnimationDrawables[i] = drawable;
//            mClickAnimationSymbols[i] = new MarkerSymbol(AndroidGraphicsCustom.drawableToBitmap(drawable, 200), MarkerSymbol.HotspotPlace.CENTER, false);
//        }

        //prepare bitmaps
        for (int i = 0; i < 100; i++) {
            mScaleSymbols[i] = new MarkerSymbol(AndroidGraphicsCustom.drawableToBitmap(icon, 101 + i), MarkerSymbol.HotspotPlace.CENTER, false);
        }

        //prepare others bitmaps
        for (int i = 0; i < 100; i++) {
            mOtherScaleSymbols[i] = AndroidGraphicsCustom.drawableToBitmap(grayedIcon, 101 + i);
        }

        //TextureItem t=new TextureItem(bitmapPoi);
        //TextureRegion tr=new TextureRegion(t,new TextureAtlas.Rect(0,0,10,10));

        Drawable baseIcon=getResources().getDrawable(R.drawable.frame00);
        clickedIcon= baseIcon.getConstantState().newDrawable().mutate();
        clickedIcon.setTint(Color.BLACK);
        ItemizedLayer.OnItemGestureListener<TaxiMarker> listener= new ItemizedLayer.OnItemGestureListener<TaxiMarker>() {
            @Override
            public boolean onItemSingleTapUp(int index, TaxiMarker item) {
                double zoom=mapView.map().getMapPosition().getZoom();
                int currSize=getDrawableSize(zoom);
                PathModel pathModel=otherIcon.getPathModelByName("arrow");

                if (clickedItems.contains((Integer)item.taxiObject.getTaxiId())){
                    clickedItems.remove((Integer)item.taxiObject.getTaxiId());
                    pathModel.setStrokeWidth(0);
                    item.setRotatedSymbol(new MarkerSymbol(AndroidGraphicsCustom.drawableToBitmap(otherIcon,101+currSize), MarkerSymbol.HotspotPlace.CENTER,false));
                    //Toast.makeText(mContext, "is contained", Toast.LENGTH_LONG).show();
                }else{
                    clickedItems.add(item.taxiObject.getTaxiId());
                    pathModel.setStrokeWidth(2);
                    item.setRotatedSymbol(new MarkerSymbol(AndroidGraphicsCustom.drawableToBitmap(otherIcon,101+currSize), MarkerSymbol.HotspotPlace.CENTER,false));
                    //Toast.makeText(mContext, "is not contained", Toast.LENGTH_LONG).show();
                }
                mOtherTaxisLayer.update();
                mapView.map().updateMap(true);

                Toast.makeText(mContext, item.taxiObject.getTaxiId() + " id", Toast.LENGTH_LONG).show();
                return false;
            }

            @Override
            public boolean onItemLongPress(int index, TaxiMarker item) {
                return false;
            }
        };

        symbol = new MarkerSymbol(bitmapPoi, MarkerSymbol.HotspotPlace.CENTER, false);


        otherSymbol = new MarkerSymbol(otherBitmap, MarkerSymbol.HotspotPlace.CENTER, false);

        //project geojson
        geoJsonIs=getResources().openRawResource(R.raw.barrios);
        mVectorLayer=new BarriosLayer(mapView.map(),mContext);
        FeatureCollection fc= GeoJsonUtils.loadFeatureCollection(geoJsonIs);
        GeoJsonUtils.addBarrios(mVectorLayer,fc);
        mapView.map().layers().add(mVectorLayer);

        //mMarkerLayer = new ItemizedLayer<TaxiMarker>(mapView.map(), new ArrayList<TaxiMarker>(), symbol, null);
        mOwnMarkerLayer= new OwnMarkerLayer(mContext,mVectorLayer,mapView.map(),new ArrayList<OwnMarker>(),otherIcon,Constants.lastLocation, new GeoPoint(0.0,0.0),mCompass);
        mOtherTaxisLayer=new OtherTaxiLayer(mContext,mVectorLayer,mapView.map(),new ArrayList<TaxiMarker>(),otherIcon);
        //mOtherTaxisLayer.setOnItemGestureListener(listener);




        backToCenterImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                backToCenter();
            }
        });
        backToCenterImage.setVisibility(ImageView.INVISIBLE);

        Drawable d = backToCenterImage.getDrawable();
        if (d instanceof AnimatedVectorDrawableCompat) {
            advCompat = (AnimatedVectorDrawableCompat) d;
        } else if (d instanceof AnimatedVectorDrawable) {
            adv = (AnimatedVectorDrawable) d;
        }






        //mapView.map().layers().add(mMarkerLayer);
        mapView.map().layers().add(mOwnMarkerLayer);
        mapView.map().layers().add(mOtherTaxisLayer);


        //prepareCompassRecalibrateDialog();

        mWebSocketConnection=new WebSocketConnection("https://id-ex-theos-taxi-test1.herokuapp.com/",this, mContext);
        mWebSocketConnection.initializeSocketListener();
        mWebSocketConnection.startAccumulationTimer();
        mWebSocketConnection.connectSocket();

        AppExecutors.getInstance().diskIO().execute(new Runnable() {
            @Override
            public void run() {
                mDb.taxiDao().clearTaxiBase();
                mDb.taxiDao().clearTaxiOld();
            }
        });

        mWebSocketConnection.setAnimationDataListener(new WebSocketConnection.AnimationDataListener() {
            @Override
            public void onAnimationParametersReceived(List<TaxiObject> baseTaxis, List<TaxiObject> newTaxis) {
                //give existing taxis a purpose
                boolean allOk=true;
                if (baseTaxis.size()>0) {
                    for (int i = 0; i < baseTaxis.size(); i++) {
                        TaxiObject taxiObject = baseTaxis.get(i);
                        if (taxiObject.getIsActive() == 0) {
                            mOtherTaxisLayer.getItemList().get(i).setPurpose(TaxiMarker.Purpose.DISAPPEAR);
                            //mOtherTaxisLayer.removeItem(i);
                        } else {
                            mOtherTaxisLayer.getItemList().get(i).setPurpose(TaxiMarker.Purpose.MOVE);
                        }
                        mOtherTaxisLayer.getItemList().get(i).setPurposeTaxiObject(taxiObject);
                        if(mOtherTaxisLayer.getItemList().get(i).taxiObject.getTaxiId()!=mOtherTaxisLayer.getItemList().get(i).getPurposeTaxiObject().getTaxiId()){
                            allOk=false;
                        }
                    }
                }
                //reset taxis in case of correspondence error
                if(!allOk){
                    //remove all taxis and add them anew
                    mOtherTaxisLayer.removeAllItems();
                    for (TaxiObject tO:baseTaxis){
                        mOtherTaxisLayer.addNewTaxi(tO);
                    }
                }
                //add new taxis
                for (TaxiObject tO:newTaxis){
                    mOtherTaxisLayer.addNewTaxi(tO);
                }
                //sort the new array to ensure future correspondence
                if (mOtherTaxisLayer.getItemList().size()>0){
                    //sort itemlist
                    Collections.sort(mOtherTaxisLayer.getItemList());
                }
                //set off animation process
                mOtherTaxisLayer.setOffAnimation(18);
            }
        });

        mCurrSize=getDrawableSize(mapView.map().getMapPosition().getZoom());

    }



    private void initializeServices(){
        //initialize vibrator
        mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        //set context
        mContext = getApplicationContext();
    }

    //check if map file is in external storage and else load it there from resources
    private void copyFileToExternalStorage(int resourceId){
        File sdFile = new File(Environment.getExternalStorageDirectory(), Constants.MAP_FILE);
        if (!sdFile.exists()) {
            try {
                InputStream in = getResources().openRawResource(resourceId);
                FileOutputStream out = new FileOutputStream(sdFile);
                byte[] buff = new byte[1024];
                int read = 0;
                try {
                    while ((read = in.read(buff)) > 0) {
                        out.write(buff, 0, read);
                    }
                } finally {
                    in.close();
                    out.close();
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    @Override
    protected void onStart() {
        super.onStart();

    }

    @Override
    protected void onStop() {
        super.onStop();

        AppExecutors.getInstance().diskIO().execute(new Runnable() {
            @Override
            public void run() {
                mDb.taxiDao().clearTaxiBase();
                mDb.taxiDao().clearTaxiOld();
            }
        });
    }

    class MapEventsReceiver extends Layer implements GestureListener, Map.UpdateListener {

        MapEventsReceiver(MapView mapView) {
            super(mapView.map());
        }

        @Override
        public boolean onGesture(Gesture g, MotionEvent e) {

//            if (g instanceof Gesture.Tap) {
//                Toast.makeText(mContext,mVectorLayer.getContainingBarrio(new GeoPoint(13.09,-86.36)).getBarrioName(),Toast.LENGTH_LONG).show();
//                return true;
//            }
            if (g instanceof Gesture.Tap) {
                GeoPoint p = mMap.viewport().fromScreenPoint(e.getX(), e.getY());
                endLocation.setLatitude(p.getLatitude());
                endLocation.setLongitude(p.getLongitude());
                startMoveAnim(500);
                //mapView.map().animator().animateTo(new GeoPoint(endLocation.getLatitude(),endLocation.getLongitude()));
                return true;
            }
//            if (g instanceof Gesture.TripleTap) {
//                GeoPoint p = mMap.viewport().fromScreenPoint(e.getX(), e.getY());
//                Toast.makeText(mContext, "Map triple tap\n" + p, Toast.LENGTH_SHORT).show();
//                return true;
//            }
            return false;
        }

        @Override
        public void onMapEvent(Event e, MapPosition mapPosition) {
            if (e == Map.POSITION_EVENT || e == Map.SCALE_EVENT || e == Map.ANIM_END|| e==Map.ROTATE_EVENT) {
                mCurrMapLoc.setLatitude(mapView.map().getMapPosition().getLatitude());
                mCurrMapLoc.setLongitude(mapView.map().getMapPosition().getLongitude());
            }
            if (e==Map.MOVE_EVENT || e == Map.SCALE_EVENT || e==Map.ROTATE_EVENT){
                backToCenterImage.setVisibility(ImageView.VISIBLE);
                bullseyeAnim();
                wasMoved=true;
                mCompass.controlView(false);
                rescheduleTimer();
            }
            if(e == Map.SCALE_EVENT || e == Map.ANIM_END|| e==Map.ROTATE_EVENT) {
                mScale=mapPosition.getScale();
            }
            if (e==Map.TILT_EVENT){
                mTilt=mapPosition.getTilt();
            }
        }

    }

    public void rescheduleTimer(){
        mTimer.cancel();
        mTimer=new Timer("movementTimer",true);
        MyTimerClass timerTask=new MyTimerClass();
        mCompass.setMode(Compass.Mode.OFF);
        mTimer.schedule(timerTask,15000);
    }

    private Timer mTimer=new Timer("movementTimer",true);
    private class MyTimerClass extends TimerTask {
        @Override
        public void run() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    backToCenter();
                }
            });

        }
    };

    private void backToCenter(){
        wasMoved=false;
        mCompass.setMapRotation(-mapView.map().getMapPosition().getBearing());
        //smoothenMapMovement(mCurrMapLoc, mMarkerLoc,endLocation);
        startMoveAnim(500);
        backToCenterImage.setVisibility(ImageView.INVISIBLE);
        Toast.makeText(this,""+endLocation.getLatitude(),Toast.LENGTH_LONG).show();
        //mCompass.setMode(Compass.Mode.C2D);
        mTimer.cancel();
    }



    private void bullseyeAnim(){
        Drawable d = backToCenterImage.getDrawable();
        if (d instanceof AnimatedVectorDrawableCompat){
            //AnimatedVectorDrawableCompat advCompat = (AnimatedVectorDrawableCompat) d;
            advCompat.stop();
            advCompat.start();
        } else if (d instanceof AnimatedVectorDrawable){
            //AnimatedVectorDrawable adv = (AnimatedVectorDrawable) d;
            adv.stop();
            adv.start();
        }
    }


    public static final int ANIM_NONE = 0;
    public static final int ANIM_MOVE = 1 << 0;

    int mState = ANIM_NONE;
    long mAnimEnd = -1;
    double mRemainingDuration = 0;

    public void startMoveAnim(float duration){
        mRemainingDuration=duration;
        mState=ANIM_MOVE;
        mAnimEnd=System.currentTimeMillis() + (long) duration;
        updateMoveAnim();
    }

    public void updateMoveAnim(){
        ThreadUtils.assertMainThread();
        if (mState == ANIM_NONE)
            return;

        long millisLeft = mAnimEnd - System.currentTimeMillis();

        double adv = clamp(1.0f - millisLeft / mRemainingDuration, 1E-6f, 1);

        mRemainingDuration=(float) millisLeft;

        if ((mState & ANIM_MOVE) != 0) {
            //do the moving
            //of marker
            double latDiffMark=endLocation.getLatitude()-mMarkerLoc.getLatitude();
            double lonDiffMark=endLocation.getLongitude()-mMarkerLoc.getLongitude();
            double lat = mMarkerLoc.getLatitude() + latDiffMark*adv;
            double lon = mMarkerLoc.getLongitude() + lonDiffMark*adv;
            mMarkerLoc.setLatitude(lat);
            mMarkerLoc.setLongitude(lon);
            mOwnMarkerLayer.moveMarker(new GeoPoint(lat,lon));

            //of map
            if (!wasMoved) {
                //move map
                double latDiffMap = endLocation.getLatitude() - mCurrMapLoc.getLatitude();
                double lonDiffMap = endLocation.getLongitude() - mCurrMapLoc.getLongitude();
                double latMap = mCurrMapLoc.getLatitude() + latDiffMap * adv;
                double lonMap = mCurrMapLoc.getLongitude() + lonDiffMap * adv;
                mapView.map().viewport().setMapPosition(new MapPosition(latMap, lonMap, mScale));
                mapView.map().viewport().setTilt(mTilt);
                mapView.map().viewport().setRotation(-mCompass.getMapRotation());
                //reset anim initial values
                mCurrMapLoc.setLatitude(latMap);
                mCurrMapLoc.setLongitude(lonMap);
                if(millisLeft<=0){
                    mCompass.controlView(true);
                    mCompass.setMode(Compass.Mode.C2D);
                }
            }
            mapView.map().updateMap(true);
        }

        if (millisLeft <= 0) {
            //log.debug("animate END");
            cancel();
        }

        mapView.postDelayed(updateTask, 25);

    }

    Runnable updateTask=new Runnable() {
        @Override
        public void run() {
            if (mState == ANIM_MOVE){
                updateMoveAnim();
            }
        }
    };

    public void cancel() {
        mState = ANIM_NONE;
    }


    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            //what to do if permissions are not granted
            return;
        }
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, null);
    }

    @Override
    protected void onPause() {
        mapView.onPause();
        super.onPause();
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }
}
