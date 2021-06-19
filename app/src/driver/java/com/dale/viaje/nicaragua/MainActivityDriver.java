package com.dale.viaje.nicaragua;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.dale.viaje.nicaragua.CommunicationsRecyclerView.CommsObject;
import com.dale.viaje.nicaragua.CommunicationsRecyclerView.CommunicationsAdapter;
import com.dale.viaje.nicaragua.vtmExtension.OwnMarker;
import com.dale.viaje.nicaragua.vtmExtension.OwnMarkerLayer;
import com.dale.viaje.nicaragua.vtmExtension.TaxiMarker;
import com.sdsmdg.harjot.vectormaster.VectorMasterDrawable;

import org.oscim.core.GeoPoint;
import org.oscim.layers.marker.ItemizedLayer;

import java.util.ArrayList;

import static android.content.ContentValues.TAG;

public class MainActivityDriver extends MainActivity{
    private static final String TAG = "MainActivityDriver";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        defaultPivot=0.75f;
        super.onCreate(savedInstanceState);

        rvCommsAdapter.setCommAcceptedListener(new CommunicationsAdapter.CommAcceptedListener() {
            @Override
            public void onCommAccepted(CommsObject comm) {

                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        showChangeDestDialog(comm.taxiMarker.destGeoPoint,true);
                    }
                });

            }
        });

        mOwnMarkerLayer.setOnItemGestureListener(new ItemizedLayer.OnItemGestureListener<OwnMarker>() {
            @Override
            public boolean onItemSingleTapUp(int index, OwnMarker item) {
                showOwnDialog();
                return true;
            }

            @Override
            public boolean onItemLongPress(int index, OwnMarker item) {
                return false;
            }
        });
    }

    @Override
    public void setupOtherMarkerLayer() {
        mOtherTaxisLayer=new OtherClientsLayer(mContext, mBarriosLayer,mapView.map(),new ArrayList<TaxiMarker>(), mUdpInConnection, mConnectionLineLayer, rvCommsAdapter);
    }

    @Override
    public void setOwnMarkerLayer() {
        ownIcon=new VectorMasterDrawable(this,R.drawable.icon_taxi);
        mOwnMarkerLayer= new OwnMarkerLayer(mContext, mBarriosLayer,mapView.map(),new ArrayList<OwnMarker>(),ownIcon, Constants.lastLocation,destGeo,mCompass);
        //mWebSocketDriverLocs.mSocket.connect();

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
////                adjustedLocation.setLatitude(adjustedLocation.getLatitude()-39.25471545); //hannover hubertus
////                adjustedLocation.setLongitude(adjustedLocation.getLongitude()-96.11757698); //hannover hubertus
////                adjustedLocation.setLatitude(adjustedLocation.getLatitude()-28.03179311); //istanbul
////                adjustedLocation.setLongitude(adjustedLocation.getLongitude()-115.3572729); //istanbul
//                endLocation=adjustedLocation;
//                mCompass.setCurrLocation(endLocation);
//                if (mCurrMapLoc != null && mMarkerLoc != null ) {
//                    startMoveAnim(500);
//                }
//                //emit current position
//                mOwnTaxiObject=new TaxiObject(MiscellaneousUtils.getNumericId(myId),endLocation.getLatitude(),endLocation.getLongitude(),endLocation.getTime(),mCompass.getRotation(),"t",destGeo.getLatitude(),destGeo.getLongitude(),1);
//                //this should be different websocket
//                mWebSocketDriverLocs.attemptSend(mOwnTaxiObject.objectToCsv());
//                Log.d(TAG, "onLocationResult: "+mOwnTaxiObject.objectToCsv());
//            }
//
//            ;
//        };
//    }

//    @Override
//    public void setupFilterButton() {
//        //filter button
//        filterImage.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                if (!filterOn){
//                    filterOn=true;
//                    mWebSocketClientLocs.setFilter(true);
//                    filterImage.setImageAlpha(255);
//                }else{
//                    filterOn=false;
//                    mWebSocketClientLocs.setFilter(false);
//                    filterImage.setImageAlpha(100);
//                }
//            }
//        });
//    }



    public void showOwnDialog(){
        final Dialog dialog=new Dialog(this);
        dialog.setContentView(R.layout.dialog_own_status);
        TextView title=dialog.findViewById(R.id.tv_title_dialog);
        Button emptyBtn=dialog.findViewById(R.id.bt_taxi_empty);
        Button fullBtn=dialog.findViewById(R.id.bt_taxi_full);
        Button closeBtn=dialog.findViewById(R.id.bt_close);


        emptyBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setDestGeo(new GeoPoint(0.0,0.0));
                setFilter(false);
                dialog.dismiss();
            }
        });

        fullBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setIsActive(2,mContext);
                dialog.dismiss();
            }
        });

        closeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });

        dialog.show();

    }

    public void setStatusText(int code, Context context){
        switch (code){
            case 1:
                status.setText(R.string.mainactivitydriver_searching);
                status.setTextColor(ContextCompat.getColor(context,R.color.colorGreen));
                statusDot.setColorFilter(ContextCompat.getColor(context, R.color.colorGreen), android.graphics.PorterDuff.Mode.SRC_IN);
                break;
            case 2:
                status.setText(R.string.mainactivitydriver_full);
                status.setTextColor(ContextCompat.getColor(context,R.color.colorBlue));
                statusDot.setColorFilter(ContextCompat.getColor(context, R.color.colorBlue), android.graphics.PorterDuff.Mode.SRC_IN);
                break;
            default:
                status.setText(R.string.mainactivitydriver_inactive);
                status.setTextColor(ContextCompat.getColor(context,R.color.colorRed));
                statusDot.setColorFilter(ContextCompat.getColor(context, R.color.colorRed), android.graphics.PorterDuff.Mode.SRC_IN);
        }
    }

    @Override
    public Intent getCloseIntent() {
        Intent intent = new Intent(MainActivityDriver.this, EntryActivityDriver.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        return intent;
    }

//    @Override
//    public void doOnDestroy() {
//        setIsActive(0,mContext);
//        mOwnTaxiObject=new TaxiObject(MiscellaneousUtils.getNumericId(myId),endLocation.getLatitude(),
//                endLocation.getLongitude(),endLocation.getTime(),mCompass.getRotation(),"t",
//                destGeo.getLatitude(),destGeo.getLongitude(),isActive);
//        mWebSocketDriverLocs.attemptSend(mOwnTaxiObject.objectToCsv());
//        super.doOnDestroy();
//    }
}
