package com.example.android.taxitest;

import android.location.Location;
import android.view.View;

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

public class MainActivityDriver extends MainActivity{

    @Override
    public void setupOtherMarkerLayer() {
        mOtherTaxisLayer=new OtherTaxiLayer(mContext, mBarriosLayer,mapView.map(),new ArrayList<TaxiMarker>(), mWebSocketDriverLocs, mConnectionLineLayer, rvCommsAdapter);
    }

    @Override
    public void setOwnMarkerLayer() {
        ownIcon=new VectorMasterDrawable(this,R.drawable.icon_taxi);
        mOwnMarkerLayer= new OwnMarkerLayer(mContext, mBarriosLayer,mapView.map(),new ArrayList<OwnMarker>(),ownIcon, Constants.lastLocation,destGeo,mCompass);

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
