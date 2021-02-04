package com.example.android.taxitest;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.PorterDuff;
import android.opengl.Visibility;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;

import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.taxitest.vtmExtension.AndroidGraphicsCustom;
import com.example.android.taxitest.vtmExtension.CitySupport;
import com.google.android.material.textfield.TextInputLayout;

import org.mapsforge.core.model.Tag;
import org.mapsforge.poi.android.storage.AndroidPoiPersistenceManagerFactory;
import org.mapsforge.poi.storage.ExactMatchPoiCategoryFilter;
import org.mapsforge.poi.storage.PoiCategoryFilter;
import org.mapsforge.poi.storage.PoiCategoryManager;
import org.mapsforge.poi.storage.PoiPersistenceManager;
import org.oscim.android.MapView;
import org.oscim.backend.canvas.Bitmap;
import org.oscim.core.BoundingBox;
import org.oscim.core.GeoPoint;
import org.oscim.core.MercatorProjection;
import org.oscim.core.Point;

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

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toCollection;


public class ChooseDestination extends AppCompatActivity {


    public GeoPoint destGeo;
    DestinationFragment fragment;
    AutoCompleteTextView chooseBarrio;
    AutoCompleteTextView chooseReference;
    ProgressBar loadingBarrios;
    ProgressBar loadingReference;
    ImageButton btUp;
    ImageButton btDown;
    Button btOk;
    Button btCancel;
    TextView seats;
    LinearLayout chosenDestLayout;
    LinearLayout parentLayout;
    ImageView fakeSplash;
    TextView chosenBarrioName;
    Toolbar mToolbar;

    int seatAmount=1;
    HashMap<String,GeoPoint> barriosList;
    HashMap<String,GeoPoint> ptOfReferenceList;
    private PoiPersistenceManager mPersistenceManager;
    File poiDb;

    SharedPreferences preferences;
    public City city;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //setTheme(R.style.AppTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_destination);
        mToolbar = (Toolbar) findViewById(R.id.toolbar_choose_dest);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle("Choose Destination");

        preferences= PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        city=new CitySupport().getCityByName(preferences.getString("city",null));

        parentLayout=(LinearLayout) findViewById(R.id.parent_layout_choose_dest);
        fakeSplash=(ImageView) findViewById(R.id.logo_fake_splash);

        fragment=new DestinationFragment();
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.map_container, fragment)
                    .commit();
        }

        //set up poi search

        barriosList=new HashMap<>();
        ptOfReferenceList=new HashMap<>();


        poiDb=new File(getApplicationContext().getExternalFilesDir(null).getAbsolutePath()+"/"+Constants.POI_FILE);
        mPersistenceManager = AndroidPoiPersistenceManagerFactory.getPoiPersistenceManager(poiDb.getAbsolutePath());


        seats=(TextView) findViewById(R.id.tv_seats);
        btUp=(ImageButton) findViewById(R.id.bt_up);
        btDown=(ImageButton) findViewById(R.id.bt_down);
        btDown.setColorFilter(getResources().getColor(R.color.colorDeselected),
                PorterDuff.Mode.SRC_ATOP);
        btDown.setClickable(false);

        btOk=(Button) findViewById(R.id.bt_dest_confirm);
        btCancel=(Button) findViewById(R.id.bt_dest_cancel);

        chosenDestLayout=(LinearLayout) findViewById(R.id.confirm_layout);
        chosenBarrioName=(TextView) findViewById(R.id.tv_chosen_barrio);

        chooseBarrio= (AutoCompleteTextView) findViewById(R.id.actv_barrio);
        chooseReference= (AutoCompleteTextView) findViewById(R.id.actv_reference);
        loadingBarrios=(ProgressBar) findViewById(R.id.pb_barrio);
        loadingReference=(ProgressBar) findViewById(R.id.pb_reference);

//        chooseBarrio.setOnFocusChangeListener(new View.OnFocusChangeListener() {
//            @Override
//            public void onFocusChange(View v, boolean hasFocus) {
//                if (!hasFocus){
//                    return;
//                }
//                if (loadingBarrios.getVisibility()==View.VISIBLE){
//                    Toast.makeText(getApplicationContext(),"please wait, content is still loading", Toast.LENGTH_SHORT).show();
//
//                }
//                ((AutoCompleteTextView) v).showDropDown();
//
//            }
//        });

        chooseBarrio.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View view, android.view.MotionEvent motionEvent) {
                if (loadingBarrios.getVisibility()==View.VISIBLE){
                    Toast.makeText(getApplicationContext(),"please wait, content is still loading", Toast.LENGTH_SHORT).show();
                    return false;
                }
                ((AutoCompleteTextView) view).showDropDown();
                return false;
            }


        });

        chooseReference.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View view, android.view.MotionEvent motionEvent) {
                if (loadingReference.getVisibility()==View.VISIBLE){
                    Toast.makeText(getApplicationContext(),"please wait, content is still loading", Toast.LENGTH_SHORT).show();
                    return false;
                }
                ((AutoCompleteTextView) view).showDropDown();
                return false;
            }


        });

        populateBarriosDropdown();
        populateReferenceDropdown();

        chooseBarrio.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                chooseReference.setText(null);
                hideSoftKeyboard(ChooseDestination.this);
                String name=(String) adapterView.getItemAtPosition(i);
                Toast.makeText(getApplicationContext(),name,Toast.LENGTH_LONG).show();
                GeoPoint p = barriosList.get(name);
                fragment.moveDestination(p,false);
            }
        });

        chooseReference.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                chooseBarrio.setText(null);
                hideSoftKeyboard(ChooseDestination.this);
                String name=(String) adapterView.getItemAtPosition(i);
                Toast.makeText(getApplicationContext(),name,Toast.LENGTH_LONG).show();
                GeoPoint p= ptOfReferenceList.get(name);
                fragment.moveDestination(p,false);
            }
        });

        //seat amount logic
        btUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (seatAmount<4){
                    if (seatAmount==1){
                        btDown.setClickable(true);
                        btDown.setColorFilter(getResources().getColor(R.color.colorSelected),PorterDuff.Mode.SRC_ATOP);
                    }
                    seatAmount++;
                    if (seatAmount==4){
                        btUp.setClickable(false);
                        btUp.setColorFilter(getResources().getColor(R.color.colorDeselected),PorterDuff.Mode.SRC_ATOP);
                    }
                    seats.setText(""+seatAmount);
                }
            }
        });

        btDown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (seatAmount>1){
                    if (seatAmount==4){
                        btUp.setClickable(true);
                        btUp.setColorFilter(getResources().getColor(R.color.colorSelected),PorterDuff.Mode.SRC_ATOP);
                    }
                    seatAmount--;
                    if (seatAmount==1){
                        btDown.setClickable(false);
                        btDown.setColorFilter(getResources().getColor(R.color.colorDeselected),PorterDuff.Mode.SRC_ATOP);
                    }
                    seats.setText(""+seatAmount);
                }
            }
        });

        fragment.setDestinationSetListener(new DestinationFragment.DestinationSetListener() {
            @Override
            public void destinationSet(String barrioName, int color, GeoPoint dest, boolean fromMap) {
                destGeo=dest;
                chosenDestLayout.setVisibility(View.VISIBLE);
                chosenBarrioName.setText(barrioName);
                chosenBarrioName.setTextColor(color);
                if (fromMap){
                    chooseBarrio.setText(null);
                    chooseReference.setText(null);
                    chooseBarrio.clearFocus();
                    chooseReference.clearFocus();
                }
            }
        });

        btCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                chooseBarrio.setText(null);
                chooseReference.setText(null);
                chooseBarrio.clearFocus();
                chooseReference.clearFocus();
                fragment.customItemLayer.removeAllItems();
                chosenDestLayout.setVisibility(View.GONE);
            }
        });

        btOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                parentLayout.setVisibility(View.GONE);
                getSupportActionBar().hide();
                fakeSplash.setVisibility(View.VISIBLE);

                //Toast.makeText(getApplicationContext(),"destination chosen",Toast.LENGTH_LONG).show();
                Handler handler=new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Intent intent = new Intent(ChooseDestination.this, MainActivityCustomer.class);
                        intent.putExtra("DEST_LAT",destGeo.getLatitude());
                        intent.putExtra("DEST_LON",destGeo.getLongitude());
                        intent.putExtra("SEATS",seatAmount);
                        startActivity(intent);
                        finishAndRemoveTask();
                    }
                },300);

            }
        });

        destGeo=new GeoPoint(0.0,0.0);
    }

    @Override
    public void onBackPressed() {
        //super.onBackPressed();
        Intent intent=new Intent(this,EntryActivityCustomer.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        fragment.onDestroy();
        finishAffinity();
    }

    public static void hideSoftKeyboard(Activity activity) {
        InputMethodManager inputMethodManager =
                (InputMethodManager) activity.getSystemService(
                        Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(
                activity.getCurrentFocus().getWindowToken(), 0);
    }




//    public static void someMethodOutside(GeoPoint geoPoint){
//        destGeo=geoPoint;
//
//    }

    public static class DestinationFragment extends BasicMapFragment{

        public void moveDestination(final GeoPoint p, boolean fromMap){
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
            //someMethodOutside(p);
            String barrioDest=mBarriosLayer.getContainingBarrio(p).getBarrioName();
            int colorDest=mBarriosLayer.getContainingBarrio(p).getStyle().fillColor;
            destinationSetListener.destinationSet(barrioDest,colorDest,p,fromMap);
            mapView.map().postDelayed(new Runnable() {
                @Override
                public void run() {
                    mapView.map().animator().animateTo(p);
                }
            },500);
        }

        interface DestinationSetListener{
            void destinationSet(String barrioName, int color, GeoPoint dest, boolean fromMap);
        }

        public void setDestinationSetListener(DestinationSetListener destinationSetListener) {
            this.destinationSetListener = destinationSetListener;
        }

        DestinationSetListener destinationSetListener;

        @Override
        public void onActivityCreated(@Nullable Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);
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
                    moveDestination(p, true);
                    return true;
                }
//            if (g instanceof Gesture.TripleTap) {
//            }
                return false;
            }
        }
    }

    private HashMap<String,GeoPoint> getPoiData(GeoPoint geoPoint, final String category, final List<Tag> patterns, String exclusion){
        Collection<org.mapsforge.poi.storage.PointOfInterest> result;
        Log.d("loquera",city.latLonMax.toString());
        try {
            PoiCategoryManager categoryManager = mPersistenceManager.getCategoryManager();
            PoiCategoryFilter categoryFilter = new ExactMatchPoiCategoryFilter();
            if (category != null)
                categoryFilter.addCategory(categoryManager.getPoiCategoryByTitle(category));
            org.mapsforge.core.model.BoundingBox bb = new org.mapsforge.core.model.BoundingBox(
                    city.latLonMin.getLatitude(),city.latLonMin.getLongitude(),
                    city.latLonMax.getLatitude(),city.latLonMax.getLongitude());
            result= mPersistenceManager.findInRect(bb, categoryFilter, patterns, Integer.MAX_VALUE);
        } catch (Throwable t) {
            result=null;
        }
        HashMap<String,GeoPoint> output=new HashMap<>();
        if (result!=null) {
            for (org.mapsforge.poi.storage.PointOfInterest poi : result) {
                //CustomPoi poiCust = new CustomPoi(poi.getName(), new GeoPoint(poi.getLatitude(), poi.getLongitude()));
                if (exclusion==null) {
                    output.put(poi.getName(),new GeoPoint(poi.getLatitude(), poi.getLongitude()));
                }else{
                    boolean isRelevant=true;
                    for(Tag tag:poi.getTags()){
                        if (exclusion.contentEquals(tag.key)){
                            isRelevant=false;
                        }
                    }
                    if (isRelevant){
                        output.put(poi.getName(),new GeoPoint(poi.getLatitude(), poi.getLongitude()));
                    }
                }
            }
        }else{
            output= null;
        }
        return output;
    }

    private void populateBarriosDropdown(){
        AppExecutors.getInstance().diskIO().execute(new Runnable() {
            @Override
            public void run() {
                List<Tag> patterns=new ArrayList<>();
                patterns.add(new Tag("place","suburb"));
                final HashMap<String,GeoPoint> output=getPoiData(null,null, patterns,null);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        barriosList.putAll(output);
                        List<String> barrioNames=new ArrayList<String>(barriosList.keySet());
                        ArrayAdapter<String> adapter=new ArrayAdapter<String>(ChooseDestination.this,
                                android.R.layout.simple_expandable_list_item_1,barrioNames);
                        chooseBarrio.setAdapter(adapter);
                        loadingBarrios.setVisibility(View.GONE);
                    }
                });
            }
        });
    }

    private void populateReferenceDropdown(){
        AppExecutors.getInstance().diskIO().execute(new Runnable() {
            @Override
            public void run() {
                final HashMap<String,GeoPoint> output=getPoiData(null,null, null,"place");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ptOfReferenceList.putAll(output);
                        List<String>  ptOfReferenceNames = new ArrayList<String>(ptOfReferenceList.keySet());
                        ArrayAdapter<String> adapter=new ArrayAdapter<String>(ChooseDestination.this,
                                android.R.layout.simple_expandable_list_item_1,ptOfReferenceNames);
                        chooseReference.setAdapter(adapter);
                        loadingReference.setVisibility(View.GONE);
                    }
                });
            }
        });
    }

    public class CustomPoi{
        String name;
        GeoPoint geoPoint;

        public CustomPoi( String name, GeoPoint geoPoint) {
            this.name = name;
            this.geoPoint = geoPoint;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public GeoPoint getGeoPoint() {
            return geoPoint;
        }

        public void setGeoPoint(GeoPoint geoPoint) {
            this.geoPoint = geoPoint;
        }
    }







}
