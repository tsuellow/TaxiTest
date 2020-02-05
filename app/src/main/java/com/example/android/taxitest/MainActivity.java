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

import org.geojson.FeatureCollection;
import org.oscim.android.MapView;
import org.oscim.backend.CanvasAdapter;
import org.oscim.core.GeoPoint;
import org.oscim.core.MapPosition;
import org.oscim.event.Event;
import org.oscim.event.Gesture;
import org.oscim.event.GestureListener;
import org.oscim.event.MotionEvent;
import org.oscim.layers.Layer;
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
import static org.oscim.utils.FastMath.clamp;

public class MainActivity extends Activity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener{

    //layout components
    MapView mapView;
    ImageView compassImage;
    ImageView backToCenterImage;

    //websocket connection
    WebSocketConnection mWebSocketConnection;

    //database
    SqlLittleDB mDb;

    //map layer variables
    BuildingLayer mBuildingLayer;
    LabelLayer mLabelLayer;
    MapScaleBar mapScaleBar;
    MapScaleBarLayer mMapScaleBarLayer;
    Compass mCompass;
    MapEventsReceiver mMapEventsReceiver;
    BarriosLayer mBarriosLayer;
    OwnMarkerLayer mOwnMarkerLayer;
    OtherTaxiLayer mOtherTaxisLayer;

    //google fused location provider variables
    FusedLocationProviderClient fusedLocationProviderClient;
    LocationRequest locationRequest;
    LocationCallback locationCallback;

    //other helper and component variables
    Context mContext;
    Vibrator mVibrator;
    VectorMasterDrawable otherIcon;
    float mTilt;
    double mScale;
    TaxiObject mOwnTaxiObject;
    InputStream geoJsonIs;
    AnimatedVectorDrawableCompat advCompat;
    AnimatedVectorDrawable adv;
    boolean wasMoved=false;
    //helper locations
    Location mMarkerLoc;
    Location endLocation=new Location("");
    Location mCurrMapLoc =new Location("");

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //basic settings
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        //set content view xml and multiple components
        setContentView(R.layout.activity_tilemap);
        compassImage = (ImageView) findViewById(R.id.compass);
        backToCenterImage = (ImageView) findViewById(R.id.back_to_center);
        mapView = (MapView) findViewById(R.id.mapView);

        // check permissions CONSIDER MOVING
        ActivityCompat.requestPermissions(MainActivity.this, new String[]{READ_EXTERNAL_STORAGE,ACCESS_FINE_LOCATION, WRITE_EXTERNAL_STORAGE},111);

        //initialize vibrator
        mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        //set context
        mContext = getApplicationContext();
        //instantiate and prepare DB
        mDb= SqlLittleDB.getInstance(getApplicationContext());
        AppExecutors.getInstance().diskIO().execute(new Runnable() {
            @Override
            public void run() {
                mDb.taxiDao().clearTaxiBase();
                mDb.taxiDao().clearTaxiOld();
            }
        });

        //initialize map
        MapFileTileSource tileSource = new MapFileTileSource();
        copyFileToExternalStorage(R.raw.nicaragua);//put in async task
        File file=new File(Environment.getExternalStorageDirectory(), Constants.MAP_FILE);
        String mapPath = file.getAbsolutePath();
        if (!tileSource.setMapFile(mapPath)) {
        Toast.makeText(mContext,"could not read map",Toast.LENGTH_LONG).show();
        }
        // Vector layer
        VectorTileLayer tileLayer = mapView.map().setBaseMap(tileSource);
        // Render theme
        mapView.map().setTheme(VtmThemes.DEFAULT);
        //add set pivot
        mapView.map().viewport().setMapViewCenter(0.0f, 0.75f);

        // SET LAYERS
        // Building layer
        mBuildingLayer=new BuildingLayer(mapView.map(), tileLayer);
        // Label layer
        mLabelLayer=new LabelLayer(mapView.map(), tileLayer);
        // Scale bar
        mapScaleBar = new DefaultMapScaleBar(mapView.map());
        mMapScaleBarLayer = new MapScaleBarLayer(mapView.map(), mapScaleBar);
        mMapScaleBarLayer.getRenderer().setPosition(GLViewport.Position.BOTTOM_LEFT);
        mMapScaleBarLayer.getRenderer().setOffset(5 * CanvasAdapter.getScale(), 0);
        // Compass
        mCompass = new Compass(this, mapView.map(), compassImage);
        mCompass.setEnabled(true);
        mCompass.setMode(Compass.Mode.C2D);
        // MapEventsReceiver
        mMapEventsReceiver=new MapEventsReceiver(mapView);
        // BarriosLayer
        geoJsonIs=getResources().openRawResource(R.raw.barrios);
        mBarriosLayer =new BarriosLayer(mapView.map(),mContext);
        FeatureCollection fc= GeoJsonUtils.loadFeatureCollection(geoJsonIs);
        if (fc != null) {
            GeoJsonUtils.addBarrios(mBarriosLayer,fc);
        }
        // OwnMarkerLayer
        otherIcon=new VectorMasterDrawable(this,R.drawable.frame00);
        mOwnMarkerLayer= new OwnMarkerLayer(mContext, mBarriosLayer,mapView.map(),new ArrayList<OwnMarker>(),otherIcon,Constants.lastLocation, new GeoPoint(13.0923151,-86.3609919),mCompass);
        // OtherMarkerLayer
        mOtherTaxisLayer=new OtherTaxiLayer(mContext, mBarriosLayer,mapView.map(),new ArrayList<TaxiMarker>(),otherIcon);
        // ADD ALL LAYERS TO MAP
        addMapLayers();

        //set important variables
        mTilt = mapView.map().viewport().getMinTilt();
        mScale = 1 << 17;
        mOwnTaxiObject=new TaxiObject(Constants.myId,0.0,0.0,new Date().getTime(),0.0f,Constants.userType,0.0,0.0,1);

        //google api client for location services
        //settings
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(3000);
        locationRequest.setFastestInterval(500);

        //last known location
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ) {
            fusedLocationProviderClient.getLastLocation()
                    .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            // Got last known location. In some rare situations this can be null.
                            if (location != null) {
                                mMarkerLoc = location;
                                //use tis to shift location (dev only)
                                mMarkerLoc.setLatitude(mMarkerLoc.getLatitude()-0.0);
                                mMarkerLoc.setLongitude(mMarkerLoc.getLongitude()-0.0);
                                mOwnMarkerLayer.moveMarker(new GeoPoint(mMarkerLoc.getLatitude(),mMarkerLoc.getLongitude()));
                                mapView.map().setMapPosition(mMarkerLoc.getLatitude(), mMarkerLoc.getLongitude(), mScale);
                            }
                        }
                    });
        }

        //callback every 3000ms
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
                if (mCurrMapLoc != null && mMarkerLoc != null ) {
                    startMoveAnim(500);
                }
                //emit current position
                mOwnTaxiObject=new TaxiObject(Constants.myId,endLocation.getLatitude(),endLocation.getLongitude(),endLocation.getTime(),mCompass.getRotation(),"taxi",0.0,0.0,1);
                mWebSocketConnection.attemptSend(mOwnTaxiObject.taxiObjectToCsv());
            }

            ;
        };

        //back-to-center animation button setup
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

        //websocket connection setup CONSIDER MOVING TO OTHER MARKER LAYER
        mWebSocketConnection=new WebSocketConnection("https://id-ex-theos-taxi-test1.herokuapp.com/", mContext);
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
                        if (taxiObject.getIsActive() == 0) {
                            mOtherTaxisLayer.getItemList().get(i).setPurpose(TaxiMarker.Purpose.DISAPPEAR);
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
                    Collections.sort(mOtherTaxisLayer.getItemList());
                }
                //set off animation process
                mOtherTaxisLayer.setOffAnimation(18);
            }
        });
    }

    //METHOD TO ADD MAP LAYERS
    private void addMapLayers(){
        mapView.map().layers().add(mBarriosLayer);
        mapView.map().layers().add(mBuildingLayer);
        mapView.map().layers().add(mLabelLayer);
        mapView.map().layers().add(mMapScaleBarLayer);
        mapView.map().layers().add(mCompass);
        mapView.map().layers().add(mMapEventsReceiver);
        mapView.map().layers().add(mOwnMarkerLayer);
        mapView.map().layers().add(mOtherTaxisLayer);
    }

    //CHECK IF  NICARAGUAN MAP-FILE IS IN EXTERNAL STORAGE AND ELSE LOAD IT THERE FROM RESOURCES
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

    //MAP EVENTS RECEIVER
    class MapEventsReceiver extends Layer implements GestureListener, Map.UpdateListener {

        MapEventsReceiver(MapView mapView) {
            super(mapView.map());
        }

        @Override
        public boolean onGesture(Gesture g, MotionEvent e) {

//            if (g instanceof Gesture.Tap) {
//                Toast.makeText(mContext,mBarriosLayer.getContainingBarrio(new GeoPoint(13.09,-86.36)).getBarrioName(),Toast.LENGTH_LONG).show();
//                return true;
//            }
            if (g instanceof Gesture.Tap) {
                GeoPoint p = mMap.viewport().fromScreenPoint(e.getX(), e.getY());
                mOwnMarkerLayer.setDest(p);
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

    //BACK-TO-CENTER FUNCTIONALITY FRAMEWORK
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
        startMoveAnim(500);
        backToCenterImage.setVisibility(ImageView.INVISIBLE);
        mTimer.cancel();
    }

    private void bullseyeAnim(){
        Drawable d = backToCenterImage.getDrawable();
        if (d instanceof AnimatedVectorDrawableCompat){
            advCompat.stop();
            advCompat.start();
        } else if (d instanceof AnimatedVectorDrawable){
            adv.stop();
            adv.start();
        }
    }

    //ANIMATION FRAMEWORK FOR MAP AND OWN MARKER
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

    //LIFECYCLE METHODS TO BE OVERRIDDEN
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

    //GOOGLE API FUSED LOCATION CONNECTION METHODS TO BE OVERRIDDEN
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
