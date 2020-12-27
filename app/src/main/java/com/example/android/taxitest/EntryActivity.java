package com.example.android.taxitest;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.LinearLayout;

import com.example.android.taxitest.utils.MapUtilsCustom;
import com.example.android.taxitest.vtmExtension.CitySupport;
import com.google.android.material.textfield.TextInputLayout;

public class EntryActivity extends AppCompatActivity {

    Button searchTaxi, justWatch;
    AutoCompleteTextView chooseCity;
    ConstraintLayout parentLayout;
    LinearLayout llContainer;
    CitySupport cities;
    SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppTheme);
        super.onCreate(savedInstanceState);
        getSupportActionBar().hide();
        setContentView(R.layout.activity_entry);
        preferences= PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        MapUtilsCustom.copyFileToExternalStorage(Constants.POI_RESOURCE,Constants.POI_FILE,getApplicationContext());
        MapUtilsCustom.copyFileToExternalStorage(Constants.MAP_RESOURCE,Constants.MAP_FILE,getApplicationContext());

        searchTaxi=(Button) findViewById(R.id.search_taxi);
        //justWatch=(Button) findViewById(R.id.watch_only);
        parentLayout=(ConstraintLayout) findViewById(R.id.entry_parent_view);
        llContainer=(LinearLayout) findViewById(R.id.ll_button_container);
        chooseCity=(AutoCompleteTextView) findViewById(R.id.actv_city);

        cities=new CitySupport();

        ArrayAdapter<String> adapter=new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, cities.getCityDropdown());

        chooseCity.setAdapter(adapter);
        chooseCity.setKeyListener(null);
        getCity();
        chooseCity.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                ((AutoCompleteTextView) view).showDropDown();
                chooseCity.setError(null);
                return false;
            }

        });




        setOnClickListeners();



    }

    public boolean checkCity(){
        if (chooseCity.getText().toString().trim().isEmpty()){
            chooseCity.setHint("choose city!");
            chooseCity.setHintTextColor(ColorStateList.valueOf(ContextCompat.getColor(this,R.color.colorRedError)));
            return false;
        }else{
            return true;
        }
    }

    public void getCity(){
        String cityName=preferences.getString("city",null);
        if (cityName!=null){
            City city=cities.getCityByName(cityName);
            chooseCity.setText(city.prettyName,false);
            //chooseCity.setSelection(chooseCity.getText().length());
        }
    }

    public void saveCity(){
        City city=cities.getCityByName(chooseCity.getText().toString());
        if (city!=null){
            SharedPreferences.Editor editor=preferences.edit();
            editor.putString("city",city.name);
            editor.apply();
        }
    }

    public void setOnClickListeners(){
        searchTaxi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (checkCity()){
                    saveCity();
                    llContainer.setVisibility(View.GONE);

                    Handler handler=new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            Intent intent = new Intent(EntryActivity.this, MainActivity.class);
                            startActivity(intent);
                        }
                    },300);
                }
            }
        });

//        when new buttons are added use this space to add new onclicklisteners that will leave only the logo visible
    }
}
