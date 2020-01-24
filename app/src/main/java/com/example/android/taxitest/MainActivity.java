package com.example.android.taxitest;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Vibrator;
import android.util.Log;
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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static com.example.android.taxitest.utils.ZoomUtils.getDrawableIndex;
import static com.example.android.taxitest.utils.ZoomUtils.getDrawableSize;

public class MainActivity extends Activity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,SensorEventListener{

    private MapView mapView;

    private MapScaleBar mapScaleBar;
    private Context mContext;
    private Location mLocation;
    private Compass mCompass;
    private ImageView compassImage;
    private ImageView backToCenterImage;

    private AlertDialog dialogCalibrate;
    Vibrator mVibrator;


    private float mTilt;
    private double mScale;

    MarkerSymbol symbol;
    MarkerSymbol otherSymbol;
    private SensorManager mSensorManager;
    private Sensor mSensorAccelerometer;
    private Sensor mSensorMagnetometer;

    MarkerSymbol mFocusMarker;
    ItemizedLayer<TaxiMarker> mMarkerLayer;
    OtherTaxiLayer mOtherTaxisLayer;

    private LocationManager locationManager;
    private final MapPosition mapPosition = new MapPosition();
    MarkerSymbol[] mClickAnimationSymbols = new MarkerSymbol[19];
    Drawable[] mClickAnimationDrawables = new Drawable[19];
    private MarkerSymbol[] mScaleSymbols = new MarkerSymbol[100];
    private Bitmap[] mOtherScaleSymbols = new Bitmap[100];

    private Location endLocation=new Location("");
    private Location mCurrLocation=new Location("");;


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

    //private ArrayList<TaxiMarker> mOtherTaxiList;

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




        // Tile source

        MapFileTileSource tileSource = new MapFileTileSource();
        copyFileToExternalStorage(R.raw.nicaragua);//put in async task
        File file=new File(Environment.getExternalStorageDirectory(), Constants.MAP_FILE);
        String mapPath = file.getAbsolutePath();
        //tileSource.setMapFile(mapPath);

        if (!file.exists()) {
            Log.d("stordir","not exists");
        } else if (!file.isFile()) {
            Log.d("stordir","is not file");
        } else if (!file.canRead()) {
            Log.d("stordir","cant read");
        }
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
                                    mLocation = location;
                                    mLocation.setLatitude(mLocation.getLatitude()-0.0);
                                    mLocation.setLongitude(mLocation.getLongitude()-0.0);
                                    setOwnMarker(mLocation);
                                    mapView.map().setMapPosition(mLocation.getLatitude(), mLocation.getLongitude(), mScale);
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
                    if (mCurrLocation != null && mLocation != null && !mClicked) {
                        MapPosition mapPosition=mapView.map().getMapPosition();
                        mapPosition.setPosition(new GeoPoint(endLocation.getLatitude(),endLocation.getLongitude()));
                        //use regular smoothen function
                        if (mCurrLocation != null && mLocation != null && !mClicked) {
                            smoothenMapMovement(mCurrLocation, mLocation, endLocation);

                        }
                        //mapView.map().animator().animateTo(mapPosition);
                        mLocation = endLocation;

                    }
                    //emit current position
                    TaxiObject ownTaxiObject=new TaxiObject(Constants.myId,endLocation.getLatitude(),endLocation.getLongitude(),endLocation.getTime(),mCompass.getRotation(),"taxi",0.0,0.0,1);
                    mWebSocketConnection.attemptSend(ownTaxiObject.taxiObjectToCsv());
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

        mMarkerLayer = new ItemizedLayer<TaxiMarker>(mapView.map(), new ArrayList<TaxiMarker>(), symbol, null);
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






        mapView.map().layers().add(mMarkerLayer);
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
        //get sensor manager
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        //get accelerometer and magnetometer for compass and orientation
        assert mSensorManager != null;
        mSensorAccelerometer = mSensorManager.getDefaultSensor(
                Sensor.TYPE_ACCELEROMETER);
        mSensorMagnetometer = mSensorManager.getDefaultSensor(
                Sensor.TYPE_MAGNETIC_FIELD);
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


        // Listeners for the sensors are registered in this callback and
        // can be unregistered in onStop().
        //
        // Check to ensure sensors are available before registering listeners.
        // Both listeners are registered with a "normal" amount of delay
        // (SENSOR_DELAY_NORMAL).
        if (mSensorAccelerometer != null) {
            mSensorManager.registerListener(this, mSensorAccelerometer,
                    SensorManager.SENSOR_DELAY_NORMAL);
        }
        if (mSensorMagnetometer != null) {
            mSensorManager.registerListener(this, mSensorMagnetometer,
                    SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        // Unregister all sensor listeners in this callback so they don't
        // continue to use resources when the app is stopped.
        mSensorManager.unregisterListener(this);
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
            if (g instanceof Gesture.LongPress) {
                GeoPoint p = mMap.viewport().fromScreenPoint(e.getX(), e.getY());
                Toast.makeText(mContext,mVectorLayer.getContainingBarrio(p).getBarrioId(),Toast.LENGTH_LONG).show();
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

                //Toast.makeText(mContext,"currPos: "+mapPosition.getLatitude(),Toast.LENGTH_LONG).show();
                mCurrLocation.setLatitude(mapView.map().getMapPosition().getLatitude());
                mCurrLocation.setLongitude(mapView.map().getMapPosition().getLongitude());
                // Toast.makeText(mContext,"currPos: "+mapPosition.getLatitude()+" "+mCurrLocation.getLatitude(),Toast.LENGTH_LONG).show();



            }
            if (e==Map.MOVE_EVENT || e == Map.SCALE_EVENT || e==Map.ROTATE_EVENT){
                backToCenterImage.setVisibility(ImageView.VISIBLE);
                bullseyeAnim();
                //mCompass.setMode(Compass.Mode.OFF);
                wasMoved=true;
                mCompass.controlView(false);
                rescheduleTimer();
            }
            if(e == Map.SCALE_EVENT || e == Map.ANIM_END|| e==Map.ROTATE_EVENT) {
                double scale = mapPosition.getZoom();
                Log.d("scale_test", "scale:" + scale);
                if (mScale!=mapPosition.getScale()){
                    //own anim
                    mScale=mapPosition.getScale();
                    int currSize=getDrawableIndex(scale);
                    scaleIcon(currSize);
                    //scaleAnimation(scale);

                    //others anim
                    //scaleOtherIcons(currSize);
                    //resetOriginalIcon(currSize);
                }
            }
            if (e==Map.TILT_EVENT){
                mTilt=mapPosition.getTilt();
            }
        }

    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        Log.d("rotat",""+mCompass.getRotation());
            if (mMarkerLayer.getItemList().size()!=0) {


                TaxiMarker taxiMarker=mMarkerLayer.getItemList().get(0);
                taxiMarker.setRotation(mCompass.getRotation());

                mMarkerLayer.getItemList().set(0,taxiMarker);
                mMarkerLayer.update();

                mapView.map().updateMap(true);
            }




    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    public void scaleIcon(int scale){
        symbol=mScaleSymbols[scale];
        TaxiMarker taxiMarker = mMarkerLayer.getItemList().get(0);
        taxiMarker.setRotatedSymbol(symbol);
        mMarkerLayer.getItemList().set(0,taxiMarker);
        mapView.map().updateMap(false);
    }

    public void scaleOtherIcons(int scale){

        Bitmap otherSymbol = mOtherScaleSymbols[scale];
        for (TaxiMarker otherTaxiMarker : mOtherTaxisLayer.getItemList()) {
            otherTaxiMarker.setRotatedSymbol(new MarkerSymbol(otherSymbol, MarkerSymbol.HotspotPlace.CENTER, false));
        }
        mapView.map().updateMap(false);

    }

    double mZoom;
    public void scaleAnimation(final double zoom){
        if (zoom!=mZoom) {
            mZoom=zoom;
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (mZoom == zoom) {
                        PathModel pathModel=otherIcon.getPathModelByName("arrow");
                        for (TaxiMarker item:mOtherTaxisLayer.getItemList()) {

                            if (clickedItems.contains((Integer)item.taxiObject.getTaxiId())){
                                //clickedItems.remove((Integer)item.getTaxiId());
                                pathModel.setStrokeWidth(2);
                                item.setRotatedSymbol(new MarkerSymbol(AndroidGraphicsCustom.drawableToBitmap(otherIcon,101+getDrawableSize(zoom)), MarkerSymbol.HotspotPlace.CENTER,false));
                                //Toast.makeText(mContext, "is contained", Toast.LENGTH_LONG).show();
                            }else{
                                //clickedItems.add(item.getTaxiId());
                                pathModel.setStrokeWidth(0);
                                item.setRotatedSymbol(new MarkerSymbol(AndroidGraphicsCustom.drawableToBitmap(otherIcon,101+getDrawableSize(zoom)), MarkerSymbol.HotspotPlace.CENTER,false));
                                //Toast.makeText(mContext, "is not contained", Toast.LENGTH_LONG).show();
                            }
                        }

                        for (int i = 0; i < mClickAnimationSymbols.length; i++) {
                            mClickAnimationSymbols[i] = new MarkerSymbol(AndroidGraphicsCustom.drawableToBitmap(mClickAnimationDrawables[i], 101 + getDrawableSize(zoom)), MarkerSymbol.HotspotPlace.CENTER, false);
                        }
                        mOtherTaxisLayer.update();
                        mapView.map().updateMap(true);

                    }
                }
            }, 100);
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
                    //mCompass.setEnabled(true);

                }
            });

        }
    };

    private void backToCenter(){
        wasMoved=false;
        mCompass.setMapRotation(-mapView.map().getMapPosition().getBearing());
        smoothenMapMovement(mCurrLocation,mLocation,endLocation);
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

    private void smoothenMapMovement(Location initialMap, Location initialTaxi, final Location end){
        double latDiffMap=end.getLatitude()-initialMap.getLatitude();
        double lonDiffMap=end.getLongitude()-initialMap.getLongitude();
        //float rotDiffMap=-mCompass.getRotation()-mapView.map().getMapPosition().getBearing();
        //mCompass.setMapRotation(mapView.map().getMapPosition().getBearing());

        double latDiff=end.getLatitude()-initialTaxi.getLatitude();
        double lonDiff=end.getLongitude()-initialTaxi.getLongitude();
        final int frames=19;
        for (int a = 0; a<frames ;a++) {
            final double latMap = initialMap.getLatitude() + latDiffMap * (a + 1) / frames;
            final double lonMap = initialMap.getLongitude() + lonDiffMap * (a + 1) / frames;
            final int i=a;

            final double lat = initialTaxi.getLatitude() + latDiff * (a + 1) / frames;
            final double lon = initialTaxi.getLongitude() + lonDiff * (a + 1) / frames;
            final Location loc=new Location(initialTaxi);
            loc.setLatitude(lat);
            loc.setLongitude(lon);

            mapView.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (end==endLocation) {

                        if (!wasMoved) {
                            //move map
                            mapView.map().viewport().setMapPosition(new MapPosition(latMap, lonMap, mScale));
                            mapView.map().viewport().setTilt(mTilt);
                            mapView.map().viewport().setRotation(-mCompass.getMapRotation());
                            //reset anim initial values
                            mCurrLocation.setLatitude(latMap);
                            mCurrLocation.setLongitude(lonMap);
                            if(i==frames-1){
                                mCompass.controlView(true);
                                mCompass.setMode(Compass.Mode.C2D);
                            }
                        }

                        //move taxi
                        setOwnMarker(loc);
                        Log.d("marker_error", "potential issue location changed"+end.getTime()+" acc:"+end.getAccuracy());
                        mapView.map().updateMap(true);

                        //reset anim initial values
                        mLocation=loc;

                    }

                }
            }, 25 * a);
        }
    }

    void setOwnMarker(Location location) {
        TaxiMarker taxiMarker;
        if (mMarkerLayer.getItemList().size() == 1) {
            //maker already exists
            taxiMarker = mMarkerLayer.getItemList().get(0);
        } else {
              mMarkerLayer.removeAllItems();
              TaxiObject taxiObject=new TaxiObject(Constants.myId,location.getLatitude(),location.getLongitude(),location.getTime(),mCompass.getRotation(),"taxi",0.0,0.0,1);
              taxiMarker = new TaxiMarker(taxiObject);

        }
        taxiMarker.setLatitude(location.getLatitude());
        taxiMarker.setLongitude(location.getLongitude());
        taxiMarker.setRotatedSymbol(symbol);
        if (mMarkerLayer.getItemList().size() > 0) {
            mMarkerLayer.getItemList().set(0, taxiMarker);
        } else {
            mMarkerLayer.addItem(taxiMarker);
        }
        mMarkerLayer.populate();//???
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
