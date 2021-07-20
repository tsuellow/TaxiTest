package com.dale.viaje.nicaragua;

import android.Manifest;

import android.app.Activity;
import android.app.Dialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;

import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Vibrator;

import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat;

import com.dale.viaje.nicaragua.CommunicationsRecyclerView.CommunicationsAdapter;
import com.dale.viaje.nicaragua.CommunicationsRecyclerView.SmoothLinearLayoutManager;
import com.dale.viaje.nicaragua.connection.IncomingUdpSocket;
import com.dale.viaje.nicaragua.connection.InitialConnection;
import com.dale.viaje.nicaragua.connection.OutgoingWebSocket;
import com.dale.viaje.nicaragua.connection.WsJsonMsg;
import com.dale.viaje.nicaragua.data.SocketObject;
import com.dale.viaje.nicaragua.data.SqlLittleDB;
import com.dale.viaje.nicaragua.data.TaxiObject;
import com.dale.viaje.nicaragua.utils.MiscellaneousUtils;
import com.dale.viaje.nicaragua.utils.ProfileUtils;
import com.dale.viaje.nicaragua.vectorLayer.BarriosLayer;
import com.dale.viaje.nicaragua.vectorLayer.ConnectionLineLayer2;
import com.dale.viaje.nicaragua.vectorLayer.HexagonQuadrantLayer;
import com.dale.viaje.nicaragua.vectorLayer.HexagonUtils;
import com.dale.viaje.nicaragua.vtmExtension.CitySupport;
import com.dale.viaje.nicaragua.vtmExtension.OtherTaxiLayer;
import com.dale.viaje.nicaragua.vtmExtension.OwnMarker;
import com.dale.viaje.nicaragua.vtmExtension.OwnMarkerLayer;
import com.dale.viaje.nicaragua.vtmExtension.TaxiMarker;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.sdsmdg.harjot.vectormaster.VectorMasterDrawable;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.oscim.android.MapView;
import org.oscim.android.theme.AssetsRenderTheme;
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
import org.oscim.tiling.source.mapfile.MapFileTileSource;
import org.oscim.utils.ThreadUtils;
import org.oscim.utils.animation.Easing;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.RECORD_AUDIO;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static com.dale.viaje.nicaragua.utils.MiscellaneousUtils.trimCache;
import static org.oscim.utils.FastMath.clamp;

public class MainActivity extends Activity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener{

    @NonNls
    public static final String MOVEMENT_TIMER = "movementTimer";
    @NonNls
    public static final String TAXI_TEST_NOTIFICATION_CHANNEL = "taxi_test_notification_channel";
    ConnectivityManager connectivityManager;
    ConnectivityManager.NetworkCallback networkCallback;
    //layout components
    MapView mapView;
    ImageView compassImage;
    ImageView backToCenterImage;
    ImageView barriosImage;
    ImageView nightModeImage;
    ImageView filterImage;
    ImageView settings;
    ImageView exit;
    public TextView destination;
    static TextView status;
    static ImageView statusDot;

    //websocket connection
//    WebSocketDriverLocations mWebSocketDriverLocs;
//    WebSocketClientLocations mWebSocketClientLocs;
    InitialConnection mConnectionInitiator;
    OutgoingWebSocket mWsOutConnection;
    IncomingUdpSocket mUdpInConnection;
    UdpDataProcessor mUdpDataProcessor;

    //database
    SqlLittleDB mDb;

    //map layer variables
    BuildingLayer mBuildingLayer;
    LabelLayer mLabelLayer;
    MapScaleBar mapScaleBar;
    MapScaleBarLayer mMapScaleBarLayer;
    public Compass mCompass;
    Compass.Mode defaultMode=Compass.Mode.C2D;
    public MapEventsReceiver mMapEventsReceiver;
    public BarriosLayer mBarriosLayer;
    public OwnMarkerLayer mOwnMarkerLayer;
    public OtherTaxiLayer mOtherTaxisLayer;
    ConnectionLineLayer2 mConnectionLineLayer;
    public HexagonQuadrantLayer mQuadrantLayer;

    //google fused location provider variables
    FusedLocationProviderClient fusedLocationProviderClient;
    LocationRequest locationRequest;
    public LocationCallback locationCallback;

    //recycler view and co
    RecyclerView rvCommunications;
    public CommunicationsAdapter rvCommsAdapter;

    //other helper and component variables
    public Context mContext;
    Vibrator mVibrator;
    public VectorMasterDrawable otherIcon, ownIcon;
    public float defaultPivot=0.75f;
    float mTilt;
    double mScale;
    public static SocketObject mOwnTaxiObject;
    AnimatedVectorDrawableCompat advCompat;
    AnimatedVectorDrawable adv;
    boolean wasMoved=false;
    boolean barriosVisible=true;
    boolean nightModeOn=false;
    boolean filterOn= false;
    public static final String NOTIFICATION_CHANNEL_ID="999";
    //helper locations
    public static Location mMarkerLoc=new Location("");
    Location endLocation=new Location("");
    Location mCurrMapLoc =new Location("");

    //TODO fix redundancy of destGeo and location in OwnTaxiLayer

    public static boolean isActivityInForeground;

    SharedPreferences preferences;
    SharedPreferences.Editor editor;
    public static String myId;
    public static String myToken;
    public static GeoPoint destGeo=new GeoPoint(0,0);
    public static String destBarrio;
    public static int isActive=0;
    public static City city;

    NotificationManager mNotificationManager;

    @NonNls
    private static final String TAG = "MainActivity";


    @Override
    public void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppTheme);
        super.onCreate(savedInstanceState);
        //basic settings
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        preferences= PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        myId=preferences.getString("taxiId","t0");
        myToken=preferences.getString("token","");
        city=new CitySupport().getCityByName(preferences.getString("city",null));
        Constants.lastLocation=getLastKnownLocation();



        //set content view assets and multiple components
        setContentView(R.layout.activity_tilemap);
        compassImage = (ImageView) findViewById(R.id.compass);
        backToCenterImage = (ImageView) findViewById(R.id.back_to_center);
        barriosImage=(ImageView) findViewById(R.id.barrios);
        nightModeImage= (ImageView) findViewById(R.id.moon);
        nightModeImage.setImageAlpha(100);
        settings= (ImageView) findViewById(R.id.settings);
        filterImage=(ImageView) findViewById(R.id.filter);
        filterImage.setImageAlpha(100);
        mapView = (MapView) findViewById(R.id.mapView);
        rvCommunications = (RecyclerView) findViewById(R.id.rv_comms);
        destination=(TextView) findViewById(R.id.tv_destination);
        status=(TextView) findViewById(R.id.tv_status);
        statusDot=(ImageView) findViewById(R.id.iv_status);
        exit=(ImageView) findViewById(R.id.exit);

        //set context
        mContext = MainActivity.this;

        // check permissions CONSIDER MOVING
        ActivityCompat.requestPermissions(MainActivity.this, new String[]{READ_EXTERNAL_STORAGE,ACCESS_FINE_LOCATION, WRITE_EXTERNAL_STORAGE, RECORD_AUDIO},111);

        //initialize vibrator
        mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        //instantiate and prepare DB
        mDb= SqlLittleDB.getInstance(getApplicationContext());
        AppExecutors.getInstance().diskIO().execute(new Runnable() {
            @Override
            public void run() {
                mDb.taxiDao().clearTaxiBase();
                mDb.taxiDao().clearTaxiOld();
                mDb.clientDao().clearTaxiBase();
                mDb.clientDao().clearTaxiOld();
            }
        });



        //initialize map... see if creating map is still necessary
        MapFileTileSource tileSource = new MapFileTileSource();
        copyFileToExternalStorage(Constants.MAP_RESOURCE);//put in async task
        File file=new File(mContext.getExternalFilesDir(null), Constants.MAP_FILE);
        String mapPath = file.getAbsolutePath();
        if (!tileSource.setMapFile(mapPath)) {
            //Toast.makeText(mContext,"could not read map",Toast.LENGTH_LONG).show();
            Log.d("First Launch","could not read map");
        }
        // Vector layer
        VectorTileLayer tileLayer = mapView.map().setBaseMap(tileSource);
        setupMap();
        // SET LAYERS
        addMapDecorations(tileLayer);
        // Compass
        setupCompass();
        // MapEventsReceiver
        mMapEventsReceiver=new MapEventsReceiver(mapView);
        // BarriosLayer
        mBarriosLayer =new BarriosLayer(mapView.map(), mContext, city.resourceBarrios);
        //quadrants
        mQuadrantLayer=new HexagonQuadrantLayer(mapView.map(),mContext,city.resourceQuadrants);
        //ConnectionLineLayer
        mConnectionLineLayer=new ConnectionLineLayer2(mapView.map());

//        mWebSocketDriverLocs =new WebSocketDriverLocations("http://ec2-3-88-176-60.compute-1.amazonaws.com:3003/", mContext,rvCommsAdapter);
//        mWebSocketClientLocs =new WebSocketClientLocations("http://ec2-3-88-176-60.compute-1.amazonaws.com:3002/", mContext,rvCommsAdapter);


        mConnectionInitiator=new InitialConnection(mContext);
        mConnectionInitiator.setOnConnDataReceivedListener(new InitialConnection.OnConnDataReceivedListener() {
            @Override
            public void onConnDataReceived() {
//                initRecyclerView();
//                initiateServerConnection();
//                // OwnMarkerLayer
//                setOwnMarkerLayer();
//                // OtherMarkerLayer
//                setupOtherMarkerLayer();
//                // ADD ALL LAYERS TO MAP
//                addMapLayers();
//                //get destination from intent
//                getDestination();
//                //taxi is inactive by default
//                setIsActive(0,mContext);
//                //set up the parameters for location callbacks
//                setupFusedLocationProvider();
//                //define what happens when a location fix is established
//                setupLocationCallback();
//                requestLocationUpdates();
//                //set up filter functionality that interacts with the way incoming udp msgs are processed
//                setupFilterButton();
            }
        });
        //mConnectionInitiator.requestInitialConnectionAddresses();
        initRecyclerView();
        mUdpDataProcessor=new UdpDataProcessor(rvCommsAdapter,mContext);
        initiateServerConnection();

        // OwnMarkerLayer
        setOwnMarkerLayer();
        // OtherMarkerLayer
        setupOtherMarkerLayer();
        // ADD ALL LAYERS TO MAP
        addMapLayers();
        //get destination from intent
        getDestination();
        //taxi is inactive by default
        setIsActive(0,mContext);
        //set up the parameters for location callbacks
        setupFusedLocationProvider();
        //define what happens when a location fix is established
        setupLocationCallback();
        requestLocationUpdates();
        //set up filter functionality that interacts with the way incoming udp msgs are processed
        setupFilterButton();

        //MULTIPLE ON CLICK LISTENERS FOLLOW
        //night mode button
        nightModeImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!nightModeOn){
                    nightModeOn=true;
                    mapView.map().setTheme(new AssetsRenderTheme(getAssets(),"", "vtm/night_mode.xml"));
                    setIconColors(Color.parseColor("#cccccc"));
                    nightModeImage.setImageAlpha(255);
                }else{
                    nightModeOn=false;
                    mapView.map().setTheme(new AssetsRenderTheme(getAssets(),"", "vtm/day_mode.xml"));
                    setIconColors(Color.parseColor("#292929"));
                    nightModeImage.setImageAlpha(100);
                }

            }
        });

        //barrios_esteli button
        barriosImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (barriosVisible){
                    barriosVisible=false;
                    mapView.map().layers().remove(2);
                    barriosImage.setImageAlpha(100);
                }else{
                    barriosVisible=true;
                    mapView.map().layers().add(2,mBarriosLayer);
                    barriosImage.setImageAlpha(255);
                }
            }
        });

        //back-to-center animation button setup
        backToCenterImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                backToCenter();
            }
        });

        prepareBackToCenterAnim();

        compassImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (defaultMode==Compass.Mode.OFF){
                    defaultMode= Compass.Mode.C2D;
                    mCompass.setMode(defaultMode);
                    compassImage.setImageAlpha(255);
                }else{
                    defaultMode= Compass.Mode.OFF;
                    mCompass.setMode(defaultMode);
                    MapPosition target=mapView.map().getMapPosition();
                    mCompass.adjustArrow(mCompass.getMapRotation(),0.0f,800);
                    target.setBearing(0.0f);
                    double tinyCorrection=1E-5;
                    target.setPosition(target.getLatitude(),target.getLongitude()+tinyCorrection);
                    mapView.map().animator().animateTo(800,target, Easing.Type.SINE_IN);
                    mCompass.setMapRotation(0.0f);
                    compassImage.setImageAlpha(100);
                }
            }
        });

        settings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openSettings();
            }
        });

        exit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });



        mNotificationManager =
                (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        assert mNotificationManager != null;
        createNotificationChannel();

        listenForNetworkChanges();
    }

    public void getDestination() {
        //get destination from intent
        Intent intent=getIntent();
        if (intent!=null){
            setDestGeo(new GeoPoint(intent.getDoubleExtra("DEST_LAT",0.0), intent.getDoubleExtra("DEST_LON",0.0)));
        }else {
            setDestGeo(new GeoPoint(0.0, 0.0));
        }
    }

    public void setupFusedLocationProvider() {
        //google api client for location services
        //settings
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(3000);
        locationRequest.setFastestInterval(2000);
        locationRequest.setMaxWaitTime(4000);

        //last known location
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ) {
            fusedLocationProviderClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        // Got last known location. In some rare situations this can be null.
                        if (location != null) {
                            mMarkerLoc = location;
                            //use this to shift location (dev only)
//                            mMarkerLoc.setLatitude(mMarkerLoc.getLatitude()-39.2908);hanover
//                            mMarkerLoc.setLongitude(mMarkerLoc.getLongitude()-96.095);hanover
//                            mMarkerLoc.setLatitude(mMarkerLoc.getLatitude()-28.03179311); //istanbul
//                            mMarkerLoc.setLongitude(mMarkerLoc.getLongitude()-115.3572729); //istanbul
                            saveAsLastKnownLocation(mMarkerLoc);
                            mOwnMarkerLayer.moveMarker(new GeoPoint(mMarkerLoc.getLatitude(),mMarkerLoc.getLongitude()));
                            mapView.map().setMapPosition(mMarkerLoc.getLatitude(), mMarkerLoc.getLongitude(), mScale);
                        }
                    }
                });
        }
    }

    public void saveAsLastKnownLocation(Location location) {
        editor=preferences.edit();
        editor.putFloat("lastLat",(float)location.getLatitude());
        editor.putFloat("lastLon",(float)location.getLongitude());
        editor.apply();
    }

    public GeoPoint getLastKnownLocation(){
        return new GeoPoint((double)preferences.getFloat("lastLat",13.0851f),(double)preferences.getFloat("lastLon",86.363f));
    }

    public void initiateServerConnection() {
        URI uri;
        try{
            uri=new URI(Constants.WS_ADDRESS);
            //uri=new URI("ws://34.207.241.98:4000");
        }catch (URISyntaxException e){
            uri=null;
        }
        mUdpInConnection=new IncomingUdpSocket(mUdpDataProcessor);
        mWsOutConnection= new OutgoingWebSocket(uri,mContext,mUdpInConnection);
        mWsOutConnection.connectToServer();
    }

    @Override
    public void onBackPressed() {
        //super.onBackPressed();
        if (rvCommsAdapter!=null){
            if (rvCommsAdapter.getItemCount()==0){
                exitSearch();
            }else{
                showSimpleDialog(getString(R.string.mainactivity_onexit_1),getString(R.string.mainactivity_onexit_2),20);
            }
        }
    }

    public void exitSearch(){
        Intent intent = getCloseIntent();
        startActivity(intent);
    }

    public Intent getCloseIntent(){
        Intent intent = new Intent(MainActivity.this, EntryActivity.class);
        finish();
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return intent;
    }

    public void showSimpleDialog(String titleText, String introText, int timer){

        final Dialog dialog=new Dialog(this);
        dialog.setContentView(R.layout.dialog_yes_no);
        TextView title=dialog.findViewById(R.id.tv_title_dialog);
        TextView intro=dialog.findViewById(R.id.tv_text_intro);
        TextView countdown=dialog.findViewById(R.id.tv_countdown);
        Button yesBtn=dialog.findViewById(R.id.bt_accept);
        Button noBtn=dialog.findViewById(R.id.bt_cancel);
        LinearLayout auto=(LinearLayout) dialog.findViewById(R.id.ll_auto_accept);

        title.setText(titleText);
        intro.setText(introText);

        final CountDownTimer countdownTimer=new CountDownTimer(timer*1000, 1000) {
            public void onTick(long millisUntilFinished) {
                int secsLeft=(int)millisUntilFinished/1000;
                countdown.setText(new StringBuilder().append("").append(secsLeft).toString());

            }
            public void onFinish() {
                //send cancellation msg
                exitSearch();
                //close dialog
                dialog.dismiss();
            }
        };

        yesBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                exitSearch();
                //close dialog
                countdownTimer.cancel();
                dialog.dismiss();
            }
        });

        noBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                countdownTimer.cancel();
                dialog.dismiss();
            }
        });

        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                countdownTimer.cancel();
            }
        });

        if (timer!=0){
            countdownTimer.start();
        }else{
            auto.setVisibility(View.GONE);
        }
        dialog.show();
    }

    public void setDestGeo(GeoPoint geo){
        destGeo=geo;
        destBarrio=mBarriosLayer.getContainingBarrio(geo).getBarrioName();
        destination.setText(destBarrio);
        destination.setTextColor(mBarriosLayer.getContainingBarrio(geo).getStyle().fillColor);
        mOwnMarkerLayer.setDest(geo);
        setIsActive(1,mContext);
    }

    public void setIsActive(int code, Context context){
        Log.d(TAG, "setIsActive: is executed"+code);
        isActive=code;
        mOwnMarkerLayer.setIsActive(code);
        setStatusText(code,context);
    }

    public void setStatusText(int code, Context context){
        switch (code){
            case 1:
                status.setText(R.string.mainactivity_setstatustext_1);
                status.setTextColor(ContextCompat.getColor(context,R.color.colorGreen));
                statusDot.setColorFilter(ContextCompat.getColor(context, R.color.colorGreen), android.graphics.PorterDuff.Mode.SRC_IN);
                break;
            case 2:
                status.setText(R.string.mainactivity_setstatustext_2);
                status.setTextColor(ContextCompat.getColor(context,R.color.colorBlue));
                statusDot.setColorFilter(ContextCompat.getColor(context, R.color.colorBlue), android.graphics.PorterDuff.Mode.SRC_IN);
                break;
            default:
                status.setText(R.string.mainactivity_setstatustext_3);
                status.setTextColor(ContextCompat.getColor(context,R.color.colorRed));
                statusDot.setColorFilter(ContextCompat.getColor(context, R.color.colorRed), android.graphics.PorterDuff.Mode.SRC_IN);
        }
    }

    public void setupFilterButton() {
        //filter button
        filterImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setFilter(!filterOn);
            }
        });
    }

    public void setFilter(boolean on){
        filterOn=on;
        //mWebSocketDriverLocs.setFilter(on);
        mUdpInConnection.dataProcessor.setFilter(on);
        int alpha=on?255:100;
        filterImage.setImageAlpha(alpha);
    }

    public boolean isFirstLocationFix=true;
    public long fixTs=System.currentTimeMillis();
    public void setupLocationCallback() {
        //callback every 3000ms
        //TODO send old locations if callback  fails to execute. prevent from unclicking on other users
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                if (isFirstLocationFix && mWsOutConnection.getConnectionStatus()==OutgoingWebSocket.ESTABLISHED){
                    setIsActive(1,mContext);
                    isFirstLocationFix=false;
                }
                //smoothen transition to new spot
                Location adjustedLocation = locationResult.getLastLocation();
//                adjustedLocation.setLatitude(adjustedLocation.getLatitude()-39.2908); //hannover andres
//                adjustedLocation.setLongitude(adjustedLocation.getLongitude()-96.095); //hannover andres
//                adjustedLocation.setLatitude(adjustedLocation.getLatitude()-39.25441545); //hannover hubertus
//                adjustedLocation.setLongitude(adjustedLocation.getLongitude()-96.11757698); //hannover hubertus
//                adjustedLocation.setLatitude(adjustedLocation.getLatitude()-28.03179311); //istanbul
//                adjustedLocation.setLongitude(adjustedLocation.getLongitude()-115.3572729); //istanbul
                endLocation=adjustedLocation;
                mCompass.setCurrLocation(endLocation);
                if (mCurrMapLoc != null && mMarkerLoc != null ) {
                    startMoveAnim(500);
                }
                //emit current position
//                mOwnTaxiObject=new TaxiObject(MiscellaneousUtils.getNumericId(myId),endLocation.getLatitude(),endLocation.getLongitude(),endLocation.getTime(),
//                        mCompass.getRotation(),"taxi",destGeo.getLatitude(),destGeo.getLongitude(),isActive);
                setOwnSocketObject(endLocation);
                //TODO delete the stuff below later
                Toast.makeText(mContext,"fix: "+(System.currentTimeMillis()-fixTs),Toast.LENGTH_SHORT).show();
                fixTs=System.currentTimeMillis();
                //mWebSocketDriverLocs.attemptSend(mOwnTaxiObject.objectToCsv());
                WsJsonMsg msgInstance=new WsJsonMsg(mOwnTaxiObject.objectToCsv());
                mWsOutConnection.sendMsg(msgInstance.createLocationMsg(mWsOutConnection.isConnected(),mQuadrantLayer.getSendingChannels(rvCommsAdapter,mOwnTaxiObject),
                        mQuadrantLayer.getReceivingChannels(rvCommsAdapter,mOwnTaxiObject,HexagonUtils.quadProfileDriver,0)));

            }

            ;
        };
    }

    public void setupOtherMarkerLayer() {
        mOtherTaxisLayer=new OtherTaxiLayer(mContext, mBarriosLayer,mapView.map(),new ArrayList<TaxiMarker>(), mUdpInConnection,mConnectionLineLayer, rvCommsAdapter);
    }

    public void setOwnSocketObject(Location endLocation){
        mOwnTaxiObject=new TaxiObject(MiscellaneousUtils.getNumericId(myId),endLocation.getLatitude(),endLocation.getLongitude(),endLocation.getTime(),
                mCompass.getRotation(),"taxi",destGeo.getLatitude(),destGeo.getLongitude(),isActive);
    }

    public void setOwnMarkerLayer() {
        ownIcon=new VectorMasterDrawable(this,R.drawable.icon_taxi);
        mOwnMarkerLayer= new OwnMarkerLayer(mContext, mBarriosLayer,mapView.map(),new ArrayList<OwnMarker>(),ownIcon, Constants.lastLocation,destGeo,mCompass);
    }

    public void addMapDecorations(VectorTileLayer tileLayer) {
        // Building layer
        mBuildingLayer=new BuildingLayer(mapView.map(), tileLayer);
        // Label layer
        mLabelLayer=new LabelLayer(mapView.map(), tileLayer);
        // Scale bar
        mapScaleBar = new DefaultMapScaleBar(mapView.map());
        mMapScaleBarLayer = new MapScaleBarLayer(mapView.map(), mapScaleBar);
        mMapScaleBarLayer.getRenderer().setPosition(GLViewport.Position.BOTTOM_LEFT);
        mMapScaleBarLayer.getRenderer().setOffset(5 * CanvasAdapter.getScale(), 0);
    }

    public void setupMap() {
        // Render theme
        mapView.map().setTheme(new AssetsRenderTheme(getAssets(),"", "vtm/day_mode.xml"));
        //set zoom tilt and pivot to defaults
        mapView.map().viewport().setMaxTilt(57.5f);
        setPivotTiltZoom();
        //TODO choose something better than last location from constants
        mapView.map().setMapPosition(Constants.lastLocation.getLatitude(),Constants.lastLocation.getLongitude(), mScale);
    }

    public void setPivotTiltZoom(){
        //pivot
        float pivot=preferences.getFloat("pivot",defaultPivot);
        mapView.map().viewport().setMapViewCenter(0.0f, pivot);
        //tilt
        mTilt = preferences.getFloat("tilt",30.0f);
        mapView.map().viewport().setTilt(mTilt);
        //zoom
        float zoom=preferences.getFloat("zoom",17.0f);
        mScale=Math.pow(2, zoom);
    }

    public void setupCompass() {
        mCompass = new Compass(this, mapView.map(), compassImage);
        mCompass.setEnabled(true);
        mCompass.setMode(defaultMode);
    }

    public void openSettings(){
        PopupMenu popup=new PopupMenu(mContext,settings);
        //inflate the created menu resource
        popup.inflate(R.menu.menu_main);
        //define what to do on each item click
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                switch (menuItem.getItemId()){
                    case R.id.opt_settings:{
                        Intent i = new Intent(mContext,SettingsActivity.class);
                        mContext.startActivity(i);
                        break;
                    }
                    case R.id.opt_past_trips:{
                        //Toast.makeText(mContext,"this guy wants to pay",Toast.LENGTH_LONG).show();
                        Intent i = new Intent(mContext,PastTripsActivity.class);
                        mContext.startActivity(i);
                        break;
                    }
                    case R.id.opt_profile:{
                        ProfileUtils.displayProfileDialog(mContext,"My Profile","https://www.id-ex.de/GymLog/#/login");
                        break;
                    }
                    case R.id.opt_help:{
                        Intent i = new Intent(mContext,HelpActivity.class);
                        mContext.startActivity(i);
                        break;
                    }
                }
                return false;
            }
        });
        popup.show();
    }

    private void setIconColors(int color){
        barriosImage.setColorFilter(color);
        compassImage.setColorFilter(color);
        nightModeImage.setColorFilter(color);
        filterImage.setColorFilter(color);
    }

    private void prepareBackToCenterAnim(){
        Drawable d = backToCenterImage.getDrawable();
        //make back to center logo visible before a location is set
        backToCenterImage.setVisibility(ImageView.VISIBLE);
        if (d instanceof AnimatedVectorDrawableCompat) {
            advCompat = (AnimatedVectorDrawableCompat) d;
        } else if (d instanceof AnimatedVectorDrawable) {
            adv = (AnimatedVectorDrawable) d;
        }
        backToCenterImage.postDelayed(new Runnable() {
            @Override
            public void run() {
                backToCenterImage.setVisibility(ImageView.INVISIBLE);
            }
        },1000);
    }

    //METHOD TO ADD MAP LAYERS
    private void addMapLayers(){
        mapView.map().layers().add(2,mBarriosLayer);
        mapView.map().layers().add(mBuildingLayer);
        mapView.map().layers().add(mLabelLayer);
        mapView.map().layers().add(mMapEventsReceiver);
        mapView.map().layers().add(mConnectionLineLayer);
        mapView.map().layers().add(mOwnMarkerLayer);
        mapView.map().layers().add(mOtherTaxisLayer);
        mapView.map().layers().add(mMapScaleBarLayer);
        mapView.map().layers().add(mCompass);
    }

    //CHECK IF  NICARAGUAN MAP-FILE IS IN EXTERNAL STORAGE AND ELSE LOAD IT THERE FROM RESOURCES
    private void copyFileToExternalStorage(int resourceId){
        File sdFile = new File(mContext.getExternalFilesDir(null), Constants.MAP_FILE);
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

            if (g instanceof Gesture.Tap) {
                GeoPoint p = mMap.viewport().fromScreenPoint(e.getX(), e.getY());
//                String channelsReceiving=mQuadrantLayer.getReceivingChannels(rvCommsAdapter, new TaxiObject(MiscellaneousUtils.getNumericId(myId),p.getLatitude(),p.getLongitude(),endLocation.getTime(),
//                        mCompass.getRotation(),"taxi",destGeo.getLatitude(),destGeo.getLongitude(),isActive), HexagonUtils.quadProfileDriver,1).toString();
//                Toast.makeText(mContext,"receiving on: "+channelsReceiving,Toast.LENGTH_LONG).show();
//                Log.d("loquera","es "+mMap.viewport().fromScreenPoint(e.getX(), e.getY()).toString());
                return true;
            }
            if (g instanceof Gesture.DoubleTap) {
                resetDefaultView();
                return true;
            }
            if (g instanceof Gesture.LongPress) {
                GeoPoint p = mMap.viewport().fromScreenPoint(e.getX(), e.getY());
                showChangeDestDialog(p,false);
                return true;
            }
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
                Log.d(TAG, "tilt: "+mTilt);
            }
        }
    }

    public void resetDefaultView() {
        wasMoved=true;
        mCompass.controlView(false);

        setPivotTiltZoom();
        double tinyCorrection=1E-5;
        double lat=endLocation.getLatitude()+tinyCorrection;
        MapPosition defaultView=new MapPosition(lat,endLocation.getLongitude(),mScale);
        defaultView.setTilt(mTilt);
        defaultView.setBearing(-mCompass.getRotation());
        mapView.map().animator().animateTo(800,defaultView, Easing.Type.LINEAR);
        //reset normal values after animation is done
        mapView.postDelayed(new Runnable() {
            @Override
            public void run() {
                wasMoved=false;
                mCompass.controlView(true);
                mapView.map().updateMap();
            }
        },800);
        backToCenterImage.setVisibility(View.INVISIBLE);
    }

    public void showChangeDestDialog(GeoPoint destGeo, boolean autoAccept){
        final Dialog dialog=new Dialog(MainActivity.this);
        dialog.setContentView(R.layout.dialog_reset_dest);
        TextView intro=dialog.findViewById(R.id.tv_text_intro);
        TextView barrio=dialog.findViewById(R.id.tv_text_barrio);
        TextView countdown=dialog.findViewById(R.id.tv_countdown);
        Button closeBtn=dialog.findViewById(R.id.bt_close);
        Button acceptBtn=dialog.findViewById(R.id.bt_accept);
        LinearLayout auto=(LinearLayout) dialog.findViewById(R.id.ll_auto_accept);

        barrio.setText(mBarriosLayer.getContainingBarrio(destGeo).getBarrioName());
        barrio.setTextColor(mBarriosLayer.getContainingBarrio(destGeo).getStyle().fillColor);

        final CountDownTimer countdownTimer=new CountDownTimer(10000, 1000) {
            public void onTick(long millisUntilFinished) {
                int secsLeft=(int)millisUntilFinished/1000;
                countdown.setText(""+secsLeft);

            }
            public void onFinish() {
                setDestGeo(destGeo);
                setFilter(true);
                dialog.dismiss();
            }
        };

        if (autoAccept){
            intro.setText(R.string.mainactivity_showchangedest_1);
            auto.setVisibility(View.VISIBLE);
            countdownTimer.start();

        }else{
            intro.setText(R.string.mainactivity_showchangedest_2);
            auto.setVisibility(View.GONE);
        }

        closeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                countdownTimer.cancel();
                dialog.dismiss();
            }
        });

        acceptBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                countdownTimer.cancel();
                setDestGeo(destGeo);
                setFilter(true);
                dialog.dismiss();
            }
        });
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                countdownTimer.cancel();
            }
        });

        dialog.show();
    }

    //BACK-TO-CENTER FUNCTIONALITY FRAMEWORK
    public void rescheduleTimer(){
        mTimer.cancel();
        mTimer=new Timer(MOVEMENT_TIMER,true);
        MyTimerClass timerTask=new MyTimerClass();
        mCompass.setMode(Compass.Mode.OFF);
        mTimer.schedule(timerTask,15000);
    }

    private Timer mTimer=new Timer(MOVEMENT_TIMER,true);
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
            GeoPoint point=new GeoPoint(lat,lon);
            mOwnMarkerLayer.moveMarker(point);
            mConnectionLineLayer.updateLines();

            //if there are no orientation sensors also for compass
            if (!mCompass.hasNeededSensors()){
                mCompass.setCompassFromBearing(adv);
            }

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
                    mCompass.setMode(defaultMode);
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

    public void requestLocationUpdates(){
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            //what to do if permissions are not granted
            return;
        }
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, null);
    }

    //LIFECYCLE METHODS TO BE OVERRIDDEN
    @Override
    protected void onStart() {
        super.onStart();

    }

    @Override
    protected void onStop() {
        super.onStop();


    }

    @Override
    protected void onDestroy() {
        doOnDestroy();
        super.onDestroy();
    }

    public void  doOnDestroy(){
        //send disconnection msg
        setIsActive(0,mContext);
        setOwnSocketObject(endLocation);
        WsJsonMsg msgInstance=new WsJsonMsg(mOwnTaxiObject.objectToCsv());
        mWsOutConnection.sendMsg(msgInstance.createLocationMsg(true,mQuadrantLayer.getSendingChannels(rvCommsAdapter,mOwnTaxiObject),
                mQuadrantLayer.getReceivingChannels(rvCommsAdapter,mOwnTaxiObject,HexagonUtils.quadProfileDriver,0)));
        //delete old stuff
        //TODO put trimCache in a different activity if it isnt already there
        try {
            trimCache(mContext);
        } catch (Exception e) {
            e.printStackTrace();
            Log.d("cache deletion","failed");
        }
        //clear DBs
        AppExecutors.getInstance().diskIO().execute(new Runnable() {
            @Override
            public void run() {
                mDb.taxiDao().clearTaxiBase();
                mDb.taxiDao().clearTaxiOld();
                mDb.taxiDao().clearTaxiNew();
                mDb.clientDao().clearTaxiBase();
                mDb.clientDao().clearTaxiOld();
                mDb.clientDao().clearTaxiNew();
            }
        });
        //disconnect compass sensor updates
        mCompass.pause();
        //disconnect location updates
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        //disconnect sockets
        rvCommsAdapter.disconnectSocket();
//        mWebSocketClientLocs.disconnectSocket();
//        mWebSocketDriverLocs.disconnectSocket();
        mWsOutConnection.close(1000,""+MiscellaneousUtils.getNumericId(myId));
        mUdpInConnection.doOnDisconnect();
        //if exists cancel idle countdownTimer
        if (idleCountdownTimer!=null){
            idleCountdownTimer.cancel();
        }
        //get rid of all app related notifications
        mNotificationManager.cancel(1);
        mNotificationManager.cancel(2);
        Log.d(TAG, "doOnDestroy: was executed");
        connectivityManager.unregisterNetworkCallback(networkCallback);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
        isActivityInForeground=true;
        if (idleCountdownTimer!=null){
            idleCountdownTimer.cancel();
            idleCountdownTimer=null;
            mNotificationManager.cancel(2);
            mNotificationManager.cancel(1);
        }
        if (shouldCloseActivity)
            exitSearch();
    }

    public boolean shouldCloseActivity=false;
    public  CountDownTimer idleCountdownTimer;
    public void initializeIdleCountdownTimer(){
        idleCountdownTimer=new CountDownTimer(180000,60000) {
            @Override
            public void onTick(long millisUntilFinished) {
                Log.d(TAG, "onTick: "+millisUntilFinished);
                if (((int)(millisUntilFinished/60000))==1){
                    Intent closeIntent = getCloseIntent();
                    MiscellaneousUtils.showExitNotification(mContext,getString(R.string.mainactivity_countdowntimer_1),getString(R.string.mainactivity_countdowntimer_2),closeIntent);
                }
            }

            @Override
            public void onFinish() {
                doOnDestroy();
                shouldCloseActivity=true;
            }
        };
    }
    @Override
    protected void onPause() {
        mapView.onPause();
        super.onPause();
        //do we really need this?
        //fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        isActivityInForeground=false;
        initializeIdleCountdownTimer();
        idleCountdownTimer.start();
        Log.d(TAG, "onPause: countdown started");
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

    private void initRecyclerView(){
        SmoothLinearLayoutManager linearLayoutManager=new SmoothLinearLayoutManager(mContext);
        rvCommsAdapter =new CommunicationsAdapter(MainActivity.this, mConnectionLineLayer);
        rvCommunications.setAdapter(rvCommsAdapter);
        rvCommunications.setLayoutManager(linearLayoutManager);
        new ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(rvCommunications);
    }

    ItemTouchHelper.SimpleCallback itemTouchHelperCallback= new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
            return false;
        }

        @Override
        public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
            super.clearView(recyclerView, viewHolder);
            ((CommunicationsAdapter.ViewHolder) viewHolder).arrows.setTextColor(getResources().getColor(R.color.colorDeselected));
            ((CommunicationsAdapter.ViewHolder) viewHolder).cancel.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.colorDeselected)));
        }

        @Override
        public void onSelectedChanged(@Nullable RecyclerView.ViewHolder viewHolder, int actionState) {
            super.onSelectedChanged(viewHolder, actionState);
            if (actionState==ItemTouchHelper.ACTION_STATE_SWIPE) {
                assert viewHolder != null;
                ((CommunicationsAdapter.ViewHolder) viewHolder).arrows.setTextColor(getResources().getColor(R.color.colorSelected));
                ((CommunicationsAdapter.ViewHolder) viewHolder).cancel.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(mContext,R.color.colorRed)));
            }
        }

        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
            mOtherTaxisLayer.doUnClick(rvCommsAdapter.getItemList().get(viewHolder.getAdapterPosition()).taxiMarker,false);
        }
    };

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String description = getString(R.string.mainactivity_notificationsdescription);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, TAXI_TEST_NOTIFICATION_CHANNEL, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            mNotificationManager.createNotificationChannel(channel);
        }
    }

    public static boolean getIsActivityInForeground(){
        return isActivityInForeground;
    }

    int currentNetworkHash=0;
    public void listenForNetworkChanges(){
        connectivityManager=(ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkRequest nr=new NetworkRequest.Builder().addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR).addTransportType(NetworkCapabilities.TRANSPORT_WIFI).build();
        networkCallback=new ConnectivityManager.NetworkCallback(){

            @Override
            public void onAvailable(@NonNull @NotNull Network network) {
                super.onAvailable(network);
                if (currentNetworkHash!=0){
                    initiateServerConnection();
                }
                currentNetworkHash=network.hashCode();
                Log.d(TAG, "networr onAvailable: "+network.hashCode());
            }

            @Override
            public void onLost(@NonNull @NotNull Network network) {
                super.onLost(network);
                Log.d(TAG, "networr onLost: "+network.hashCode());
                //Toast.makeText(mContext,"you just disconnected from your network, we will try to reconnect you",Toast.LENGTH_SHORT).show();
            }
        };
        connectivityManager.registerNetworkCallback(nr,networkCallback);

    }
}
