package com.example.android.taxitest;

import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.example.android.taxitest.CommunicationsRecyclerView.CommsObject;
import com.example.android.taxitest.connection.MySingleton;
import com.example.android.taxitest.data.ClientObject;
import com.example.android.taxitest.data.TaxiObject;
import com.example.android.taxitest.utils.MiscellaneousUtils;
import com.example.android.taxitest.vectorLayer.BarriosLayer;
import com.example.android.taxitest.vtmExtension.OtherTaxiLayer;
import com.example.android.taxitest.vtmExtension.OwnMarker;
import com.example.android.taxitest.vtmExtension.OwnMarkerLayer;
import com.example.android.taxitest.vtmExtension.TaxiMarker;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationResult;
import com.sdsmdg.harjot.vectormaster.VectorMasterDrawable;

import org.json.JSONException;
import org.json.JSONObject;
import org.oscim.android.theme.AssetsRenderTheme;
import org.oscim.core.GeoPoint;

import java.util.ArrayList;
import java.util.Date;

public class MainActivityCustomer extends MainActivity {

    public static int seatAmount;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        defaultPivot=0.25f;
        super.onCreate(savedInstanceState);
        Intent intent=getIntent();
        if (intent!=null){
            seatAmount=intent.getIntExtra("SEATS",1);
        }else {
            seatAmount=1;
        }

    }


    @Override
    public void setupCompass() {
        mCompass = new Compass(this, mapView.map(), compassImage);
        mCompass.setEnabled(true);
        mCompass.setMode(Compass.Mode.OFF);
    }

    @Override
    public void setupOtherMarkerLayer() {
        mOtherTaxisLayer=new OtherDriversLayer(this, mBarriosLayer,mapView.map(),new ArrayList<TaxiMarker>(), mUdpInConnection, mConnectionLineLayer, rvCommsAdapter);
    }

    @Override
    public void setOwnMarkerLayer() {
        ownIcon=new VectorMasterDrawable(this,R.drawable.icon_client);
        mOwnMarkerLayer= new OwnMarkerLayer(mContext, mBarriosLayer,mapView.map(),new ArrayList<OwnMarker>(),ownIcon, Constants.lastLocation,destGeo,mCompass);
        //mWebSocketClientLocs.mSocket.connect();

    }

//    @Override
//    public void setupLocationCallback() {
//        //callback every 3000ms
//        //TODO send old locations if callback  fails to execute. prevent from unclicking on other users
//        locationCallback = new LocationCallback() {
//            @Override
//            public void onLocationResult(LocationResult locationResult) {
//                if (locationResult == null) {
//                    return;
//                }
//                if (isFirstLocationFix){
//                    setIsActive(1,mContext);
//                    isFirstLocationFix=false;
//                }
//                //smoothen transition to new spot
//                Location adjustedLocation = locationResult.getLastLocation();
////                adjustedLocation.setLatitude(adjustedLocation.getLatitude()-39.2908); //hannover andres
////                adjustedLocation.setLongitude(adjustedLocation.getLongitude()-96.095); //hannover andres
////                adjustedLocation.setLatitude(adjustedLocation.getLatitude()-39.25441545); //hannover hubertus
////                adjustedLocation.setLongitude(adjustedLocation.getLongitude()-96.11757698); //hannover hubertus
////                adjustedLocation.setLatitude(adjustedLocation.getLatitude()-28.03179311); //istanbul
////                adjustedLocation.setLongitude(adjustedLocation.getLongitude()-115.3572729); //istanbul
//                endLocation=adjustedLocation;
//                mCompass.setCurrLocation(endLocation);
//                if (mCurrMapLoc != null && mMarkerLoc != null ) {
//                    startMoveAnim(500);
//                }
//                //emit current position
//                mOwnTaxiObject=new ClientObject(MiscellaneousUtils.getNumericId(myId),endLocation.getLatitude(),
//                        endLocation.getLongitude(),endLocation.getTime(),mCompass.getRotation(),seatAmount,"",
//                        destGeo.getLatitude(),destGeo.getLongitude(),isActive);
//                //this should be different websocket
//                mWebSocketClientLocs.attemptSend(mOwnTaxiObject.objectToCsv());
//            }
//
//            ;
//        };
//    }


    @Override
    public void setOwnSocketObject(Location endLocation) {
        mOwnTaxiObject=new ClientObject(MiscellaneousUtils.getNumericId(myId),endLocation.getLatitude(),
                        endLocation.getLongitude(),endLocation.getTime(),mCompass.getRotation(),seatAmount,"",
                        destGeo.getLatitude(),destGeo.getLongitude(),isActive);
    }

    @Override
    public void doOnDestroy() {
//        setIsActive(0,mContext);
//        mOwnTaxiObject=new ClientObject(MiscellaneousUtils.getNumericId(myId),endLocation.getLatitude(),
//                endLocation.getLongitude(),endLocation.getTime(),mCompass.getRotation(),seatAmount,"",
//                destGeo.getLatitude(),destGeo.getLongitude(),isActive);
//        mWebSocketClientLocs.attemptSend(mOwnTaxiObject.objectToCsv());
        //kill timer that might have survived
        if (((OtherDriversLayer)mOtherTaxisLayer).genericCountdownTimer!=null)
            ((OtherDriversLayer)mOtherTaxisLayer).genericCountdownTimer.cancel();
        super.doOnDestroy();
    }

    @Override
    public Intent getCloseIntent() {
        Intent intent = new Intent(MainActivityCustomer.this, EntryActivityCustomer.class);
        //finish();
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        return intent;
    }


    public static JSONObject getCommBackupJson(CommsObject comm, BarriosLayer barriosLayer){
        JSONObject json=new JSONObject();
        try {
            json.put("commId" , comm.commId);
            json.put("clientId" , MiscellaneousUtils.getNumericId(myId));
            json.put("driverId" ,comm.taxiMarker.taxiObject.getTaxiId());
            json.put("seats" , seatAmount);
            json.put("city" , city.name);
            json.put("barrioFrom" , barriosLayer.getContainingBarrio(new GeoPoint(mOwnTaxiObject.getLatitude(),mOwnTaxiObject.getLongitude())).getBarrioName());
            json.put("barrioTo" , destBarrio);
            json.put("latFrom" , mOwnTaxiObject.getLatitude());
            json.put("lonFrom" , mOwnTaxiObject.getLongitude());
            json.put("latTo" , mOwnTaxiObject.getDestinationLatitude());
            json.put("lonTo" , mOwnTaxiObject.getDestinationLongitude());
            json.put("timestamp" , MiscellaneousUtils.getDateString(new Date()));
            //Log.d("volley",json.toString());

        }catch (Exception e){
            e.printStackTrace();
        }
        return json;
    }


}
