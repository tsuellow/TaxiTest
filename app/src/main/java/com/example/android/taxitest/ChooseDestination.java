package com.example.android.taxitest;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;

import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.FrameLayout;
import android.widget.NumberPicker;
import android.widget.Toast;

import com.example.android.taxitest.vtmExtension.AndroidGraphicsCustom;
import com.google.android.material.textfield.TextInputLayout;

import org.oscim.android.MapView;
import org.oscim.backend.canvas.Bitmap;
import org.oscim.core.GeoPoint;
import org.oscim.core.MercatorProjection;
import org.oscim.core.Point;
import org.oscim.core.Tag;
import org.oscim.core.Tile;
import org.oscim.event.Gesture;
import org.oscim.event.MotionEvent;
import org.oscim.layers.marker.ItemizedLayer;
import org.oscim.layers.marker.MarkerItem;
import org.oscim.layers.marker.MarkerSymbol;
import org.oscim.tiling.OverzoomTileDataSource;
import org.oscim.tiling.source.mapfile.MapDatabase;
import org.oscim.tiling.source.mapfile.MapReadResult;
import org.oscim.tiling.source.mapfile.PointOfInterest;
import org.slf4j.Marker;

import java.util.ArrayList;
import java.util.List;


public class ChooseDestination extends AppCompatActivity {


    public static GeoPoint destGeo;
    DestinationFragment fragment;
    AutoCompleteTextView chooseBarrio;
    TextInputLayout loChooseBarrio;
    AutoCompleteTextView chooseReference;
    NumberPicker amountPassengers;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_destination);

        fragment=new DestinationFragment();
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.map_container, fragment)
                    .commit();
        }


        chooseBarrio= (AutoCompleteTextView) findViewById(R.id.actv_barrio);
        chooseReference= (AutoCompleteTextView) findViewById(R.id.actv_reference);
        String[] array={"uno","dos" ,"tres", "cuatro","cinco","seis"};
        //List<String> pois=fragment.getPoiList();
        ArrayAdapter<String> adapter=new ArrayAdapter<String>(ChooseDestination.this,
                android.R.layout.simple_expandable_list_item_1,array);
        chooseBarrio.setAdapter(adapter);
        chooseBarrio.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View view, android.view.MotionEvent motionEvent) {
                ((AutoCompleteTextView) view).showDropDown();
                return false;
            }


        });
        loChooseBarrio=(TextInputLayout) findViewById(R.id.lo_barrio);

        chooseReference.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                List<String> pois=fragment.getPoiList();
                ArrayAdapter<String> adapter=new ArrayAdapter<String>(ChooseDestination.this,
                        android.R.layout.simple_expandable_list_item_1,pois);
                chooseBarrio.setAdapter(adapter);
            }
        });



        destGeo=new GeoPoint(0.0,0.0);





    }




    public static void someMethodOutside(GeoPoint geoPoint){
        destGeo=geoPoint;
    }

    public static class DestinationFragment extends BasicMapFragment{

        List<String> poiList;
        public List<String> getPoiList(){
            List<String> result=new ArrayList<>();
            long mapSize = MercatorProjection.getMapSize((byte) 18);

            int tileLRLat = MercatorProjection.latitudeToTileY(Constants.lowerRightLabelLimit.getLatitude(),(byte)18);
            int tileLRLon = MercatorProjection.longitudeToTileX(Constants.lowerRightLabelLimit.getLongitude(),(byte)18);
            int tileULLat = MercatorProjection.latitudeToTileY(Constants.upperLeftLabelLimit.getLatitude(),(byte)18);
            int tileULLon = MercatorProjection.longitudeToTileX(Constants.upperLeftLabelLimit.getLongitude(),(byte)18);
            //Tile upperLeft = new Tile(tileULLon, tileULLat, (byte) 18);
            //Tile lowerRight = new Tile(tileLRLon, tileLRLat, (byte) 18);
            Tile lowerRight = new Tile(68189,121454, (byte) 18);
            Tile upperLeft = new Tile(68186,121453, (byte) 18);
            Tile entire = new Tile(1065,1897, (byte) 12);
            //Log.d("pois",""+upperLeft.tileX+","+upperLeft.tileY+","+(int)upperLeft.zoomLevel);

            MapReadResult mapReadResult = ((MapDatabase) ((OverzoomTileDataSource) mTileSource.getDataSource()).getDataSource()).readMapData(upperLeft,lowerRight);

            Log.d("pois",mapReadResult.pointOfInterests.size()+"size");
            // Filter POI
            //sb.append("*** POI ***");
            for (PointOfInterest pointOfInterest : mapReadResult.pointOfInterests) {
                List<Tag> tags = pointOfInterest.tags;
                Log.d("pois",tags.size()+"size");
                for (Tag tag1 : tags) {
                    //if (tag1.key.contains("name")) {
                        String entry = tag1.key+ "=" + tag1.value;
                        result.add(entry);

                    //}
                }
            }
            return result;
        }

        @Override
        public void onActivityCreated(@Nullable Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);
            poiList=getPoiList();
        }

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
