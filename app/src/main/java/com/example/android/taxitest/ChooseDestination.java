package com.example.android.taxitest;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;

import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.example.android.taxitest.vtmExtension.AndroidGraphicsCustom;

import org.oscim.android.MapView;
import org.oscim.backend.canvas.Bitmap;
import org.oscim.core.GeoPoint;
import org.oscim.event.Gesture;
import org.oscim.event.MotionEvent;
import org.oscim.layers.marker.ItemizedLayer;
import org.oscim.layers.marker.MarkerItem;
import org.oscim.layers.marker.MarkerSymbol;
import org.slf4j.Marker;


public class ChooseDestination extends AppCompatActivity {


    public static GeoPoint destGeo;
    DestinationFragment fragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_destination);
        destGeo=new GeoPoint(0.0,0.0);
        fragment=new DestinationFragment();
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.map_container, fragment)
                    .commit();
        }



    }

    public static void someMethodOutside(GeoPoint geoPoint){
        destGeo=geoPoint;
    }

    public static class DestinationFragment extends BasicMapFragment{


        @Override
        public void setMapEventsReceiver(MapView mapView) {
            mMapEventsReceiver=new DestinationReceiver(mapView);
        }

        public class DestinationReceiver extends BasicMapFragment.MapEventsReceiver{

            DestinationReceiver(MapView mapView) {
                super(mapView);
            }

            @Override
            public boolean onGesture(Gesture g, MotionEvent e) {

//            if (g instanceof Gesture.LongPress) {
//            }
                if (g instanceof Gesture.Tap) {
                    GeoPoint p = mMap.viewport().fromScreenPoint(e.getX(), e.getY());
                    mOwnMarkerLayer.setDest(p);
                    customItemLayer.removeAllItems();
                    MarkerItem pin=new MarkerItem("Destination", "picked on touch",p);
                    MarkerItem blur=new MarkerItem("Destination Area", "picked on touch",p);
                    pin.setMarker(new MarkerSymbol(AndroidGraphicsCustom.drawableToBitmap(getContext().getResources().getDrawable(R.drawable.location_pin,null),50), MarkerSymbol.HotspotPlace.BOTTOM_CENTER,true));
                    Bitmap blurBitmap=AndroidGraphicsCustom.drawableToBitmap(getContext().getResources().getDrawable(R.drawable.blur,null),5);
                    blurBitmap.scaleTo(200,200);
                    blur.setMarker(new MarkerSymbol(blurBitmap, MarkerSymbol.HotspotPlace.CENTER,false));
                    customItemLayer.addItem(pin);
                    customItemLayer.addItem(blur);
                    someMethodOutside(p);


                    mapView.map().animator().animateTo(p);
                    return true;
                }
//            if (g instanceof Gesture.TripleTap) {
//            }
                return false;
            }
        }
    }





}
