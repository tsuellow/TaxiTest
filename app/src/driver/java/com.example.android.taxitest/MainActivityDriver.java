package com.example.android.taxitest;

import android.location.Location;
import android.util.Log;
import android.view.View;

import com.example.android.taxitest.connection.WebSocketDriverLocations;
import com.example.android.taxitest.data.TaxiObject;
import com.example.android.taxitest.utils.MiscellaneousUtils;
import com.example.android.taxitest.vtmExtension.OtherTaxiLayer;
import com.example.android.taxitest.vtmExtension.OwnMarker;
import com.example.android.taxitest.vtmExtension.OwnMarkerLayer;
import com.example.android.taxitest.vtmExtension.TaxiMarker;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationResult;
import com.sdsmdg.harjot.vectormaster.VectorMasterDrawable;

import java.util.ArrayList;

import static android.content.ContentValues.TAG;

public class MainActivityDriver extends MainActivity{
    private static final String TAG = "MainActivityDriver";

    @Override
    public void setupOtherMarkerLayer() {
        mOtherTaxisLayer=new OtherTaxiLayer(mContext, mBarriosLayer,mapView.map(),new ArrayList<TaxiMarker>(), mWebSocketClientLocs, mConnectionLineLayer, rvCommsAdapter);
    }

    @Override
    public void setOwnMarkerLayer() {
        ownIcon=new VectorMasterDrawable(this,R.drawable.icon_taxi);
        mOwnMarkerLayer= new OwnMarkerLayer(mContext, mBarriosLayer,mapView.map(),new ArrayList<OwnMarker>(),ownIcon, Constants.lastLocation,destGeo,mCompass);
        mWebSocketDriverLocs.mSocket.connect();

    }

    @Override
    public void setupLocationCallback() {
        //callback every 3000ms
        //TODO send old locations if callback  fails to execute. prevent from unclicking on other users
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                //smoothen transition to new spot
                Location adjustedLocation = locationResult.getLastLocation();
//                adjustedLocation.setLatitude(adjustedLocation.getLatitude()-39.2908);
//                adjustedLocation.setLongitude(adjustedLocation.getLongitude()-96.095);
                endLocation=adjustedLocation;
                mCompass.setCurrLocation(endLocation);
                if (mCurrMapLoc != null && mMarkerLoc != null ) {
                    startMoveAnim(500);
                }
                //emit current position
                mOwnTaxiObject=new TaxiObject(MiscellaneousUtils.getNumericId(myId),endLocation.getLatitude(),endLocation.getLongitude(),endLocation.getTime(),mCompass.getRotation(),"t",destGeo.getLatitude(),destGeo.getLongitude(),1);
                //this should be different websocket
                mWebSocketDriverLocs.attemptSend(mOwnTaxiObject.objectToCsv());
                Log.d(TAG, "onLocationResult: "+mOwnTaxiObject.objectToCsv());
            }

            ;
        };
    }

    @Override
    public void setupFilterButton() {
        //filter button
        filterImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!filterOn){
                    filterOn=true;
                    mWebSocketClientLocs.setFilter(true);
                    filterImage.setImageAlpha(255);
                }else{
                    filterOn=false;
                    mWebSocketClientLocs.setFilter(false);
                    filterImage.setImageAlpha(100);
                }
            }
        });
    }
}
