package com.example.android.taxitest;

import android.Manifest;
import android.content.pm.PackageManager;

import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.example.android.taxitest.data.TaxiObject;
import com.example.android.taxitest.vectorLayer.BarriosLayer;
import com.example.android.taxitest.vtmExtension.AndroidGraphicsCustom;
import com.example.android.taxitest.vtmExtension.OwnMarker;
import com.example.android.taxitest.vtmExtension.OwnMarkerLayer;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.sdsmdg.harjot.vectormaster.VectorMasterDrawable;

import org.oscim.android.MapView;
import org.oscim.android.theme.AssetsRenderTheme;
import org.oscim.backend.CanvasAdapter;
import org.oscim.backend.canvas.Bitmap;
import org.oscim.core.GeoPoint;
import org.oscim.core.MapPosition;
import org.oscim.event.Event;
import org.oscim.event.Gesture;
import org.oscim.event.GestureListener;
import org.oscim.event.MotionEvent;
import org.oscim.layers.Layer;
import org.oscim.layers.marker.ItemizedLayer;
import org.oscim.layers.marker.MarkerItem;
import org.oscim.layers.marker.MarkerSymbol;
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

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.Objects;

import static org.oscim.utils.FastMath.clamp;

public class BasicMapFragment extends Fragment implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener{
    MapView mapView;
    private ImageView compassImage;
    private ImageView backToCenterImage;

    MapFileTileSource mTileSource;
    private BuildingLayer mBuildingLayer;
    private LabelLayer mLabelLayer;
    private MapScaleBar mapScaleBar;
    private MapScaleBarLayer mMapScaleBarLayer;
    private Compass mCompass;
    MapEventsReceiver mMapEventsReceiver;
    BarriosLayer mBarriosLayer;
    OwnMarkerLayer mOwnMarkerLayer;
    ItemizedLayer<MarkerItem> customItemLayer;

    VectorMasterDrawable ownIcon;
    Drawable blur;
    Drawable dest;

    //google fused location provider variables
    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;

    private   Location mMarkerLoc;
    Location endLocation=new Location("");
    Location mCurrMapLoc =new Location("");
    private  GeoPoint destGeo;
    float mTilt;
    double mScale;

    boolean wasMoved=false;
    private static final String TAG = "BasicMapFragment";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        //return super.onCreateView(inflater, container, savedInstanceState);
        Log.d(TAG, "onCreateView: executed");

        //set content view assets and multiple components
        View rootView=inflater.inflate(R.layout.fragment_basic_map,container,false);

        compassImage = (ImageView) rootView.findViewById(R.id.simple_compass);
        backToCenterImage = (ImageView) rootView.findViewById(R.id.simple_back_to_center);
        mapView = (MapView) rootView.findViewById(R.id.simple_map);

        //initialize map
        mTileSource = new MapFileTileSource();
        //copyFileToExternalStorage(R.raw.result);//put in async task
        File file=new File(getContext().getExternalFilesDir(null), Constants.MAP_FILE);
        String mapPath = file.getAbsolutePath();
        if (!mTileSource.setMapFile(mapPath)) {
            //Toast.makeText(mContext,"could not read map",Toast.LENGTH_LONG).show();
            Log.d("First Launch","could not read map");
        }
        // Vector layer
        VectorTileLayer tileLayer = mapView.map().setBaseMap(mTileSource);
        // Render theme
        mapView.map().setTheme(new AssetsRenderTheme(Objects.requireNonNull(getContext()).getAssets(),"", "vtm/day_mode.xml"));
        //add set pivot
        mapView.map().viewport().setMapViewCenter(0.0f, 0.0f);
        //set important variables
        mTilt = mapView.map().viewport().getMinTilt();
        mScale = 1 << 14;
        destGeo=new GeoPoint(0.0,0.0);
        mapView.map().setMapPosition(Constants.lastLocation.getLatitude(),Constants.lastLocation.getLongitude(), mScale);

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
        mCompass = new Compass(getContext(), mapView.map(), compassImage);
        mCompass.setEnabled(true);
        mCompass.setMode(Compass.Mode.OFF);
        // BarriosLayer
        mBarriosLayer =new BarriosLayer(mapView.map(), getContext(), Constants.barriosFile);
        // OwnMarkerLayer
        ownIcon=new VectorMasterDrawable(getContext(),R.drawable.location_dot);
        mOwnMarkerLayer= new OwnMarkerLayer(getContext(), mBarriosLayer,mapView.map(),new ArrayList<OwnMarker>(),ownIcon,Constants.lastLocation, new GeoPoint(13.0923151,-86.3609919),mCompass);
        //set customLayer
        Bitmap exampleBitmap= AndroidGraphicsCustom.drawableToBitmap(getContext().getResources().getDrawable(R.drawable.location_pin,null),100);
        customItemLayer=new ItemizedLayer<MarkerItem>(mapView.map(), new MarkerSymbol(exampleBitmap,MarkerSymbol.HotspotPlace.BOTTOM_CENTER,true));
        // MapEventsReceiver
        setMapEventsReceiver(mapView);


        addMapLayers();





        //google api client for location services
        //settings
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(getContext());
        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(3000);
        locationRequest.setFastestInterval(500);


        //last known location
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ) {
            fusedLocationProviderClient.getLastLocation()
                    .addOnSuccessListener(Objects.requireNonNull(getActivity()), new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            // Got last known location. In some rare situations this can be null.
                            if (location != null) {
                                mMarkerLoc = location;
                                //use tis to shift location (dev only)
                                mMarkerLoc.setLatitude(mMarkerLoc.getLatitude()-0.0);
                                mMarkerLoc.setLongitude(mMarkerLoc.getLongitude()-0.0);
                                mapView.map().setMapPosition(mMarkerLoc.getLatitude(), mMarkerLoc.getLongitude(), mScale);
                                mOwnMarkerLayer.moveMarker(new GeoPoint(mMarkerLoc.getLatitude(),mMarkerLoc.getLongitude()));
                                endLocation=location;
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
            }

            ;
        };

        backToCenterImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                wasMoved=false;
                mCompass.setMapRotation(-mapView.map().getMapPosition().getBearing());
                startMoveAnim(500);
            }
        });

        doBeforeInflation();

        return rootView;
    }

    public void doBeforeInflation(){
        //custom actions to be taken before inflation
    }

    public void addMapLayers(){
        mapView.map().layers().add(mBarriosLayer);
        mapView.map().layers().add(mBuildingLayer);
        mapView.map().layers().add(mLabelLayer);
        mapView.map().layers().add(mMapEventsReceiver);
        mapView.map().layers().add(mOwnMarkerLayer);
        mapView.map().layers().add(customItemLayer);
        mapView.map().layers().add(mMapScaleBarLayer);
        mapView.map().layers().add(mCompass);
    }

    public void setMapEventsReceiver(MapView mapView){
        mMapEventsReceiver=new MapEventsReceiver(mapView);
    }



    //MAP EVENTS RECEIVER
    public class MapEventsReceiver extends Layer implements GestureListener, Map.UpdateListener {

        MapEventsReceiver(MapView mapView) {
            super(mapView.map());
        }

        @Override
        public boolean onGesture(Gesture g, MotionEvent e) {

//            if (g instanceof Gesture.LongPress) {
//            }
            if (g instanceof Gesture.Tap) {
                GeoPoint p = mMap.viewport().fromScreenPoint(e.getX(), e.getY());
                mOwnMarkerLayer.setDest(p);
                //mapView.map().animator().animateTo(new GeoPoint(endLocation.getLatitude(),endLocation.getLongitude()));
                return true;
            }
//            if (g instanceof Gesture.TripleTap) {
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
                wasMoved=true;
                mCompass.controlView(false);
            }
            if(e == Map.SCALE_EVENT || e == Map.ANIM_END|| e==Map.ROTATE_EVENT) {
                mScale=mapPosition.getScale();
            }
            if (e==Map.TILT_EVENT){
                mTilt=mapPosition.getTilt();
            }
        }
    }


    //ANIMATION FRAMEWORK FOR MAP AND OWN MARKER
    private static final int ANIM_NONE = 0;
    private static final int ANIM_MOVE = 1;

    private int mState = ANIM_NONE;
    private long mAnimEnd = -1;
    private double mRemainingDuration = 0;

    private void startMoveAnim(float duration){
        mRemainingDuration=duration;
        mState=ANIM_MOVE;
        mAnimEnd=System.currentTimeMillis() + (long) duration;
        updateMoveAnim();
    }

    private void updateMoveAnim(){
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
            }
            mapView.map().updateMap(true);
        }

        if (millisLeft <= 0) {
            //log.debug("animate END");
            cancel();
        }

        mapView.postDelayed(updateTask, 25);

    }

    private Runnable updateTask=new Runnable() {
        @Override
        public void run() {
            if (mState == ANIM_MOVE){
                updateMoveAnim();
            }
        }
    };

    private void cancel() {
        mState = ANIM_NONE;
    }


    //Lifecycle and other methods to override
    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
        if (ActivityCompat.checkSelfPermission(Objects.requireNonNull(getContext()), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            //what to do if permissions are not granted
            return;
        }
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, null);
    }

    @Override
    public void onPause() {
        mapView.onPause();
        super.onPause();
        //here we need this
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        Log.d(TAG, "onPause: happened");
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
