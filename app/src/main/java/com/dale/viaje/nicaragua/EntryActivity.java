package com.dale.viaje.nicaragua;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.Toast;

import com.dale.viaje.nicaragua.utils.CustomTextView;
import com.dale.viaje.nicaragua.utils.MapUtilsCustom;
import com.dale.viaje.nicaragua.utils.MiscellaneousUtils;
import com.dale.viaje.nicaragua.utils.ProfileUtils;
import com.dale.viaje.nicaragua.vtmExtension.CitySupport;

import java.io.File;

public class EntryActivity extends AppCompatActivity {

    Button searchTaxi, justWatch;
    AutoCompleteTextView chooseCity;
    ConstraintLayout parentLayout;
    LinearLayout llContainer, llUserSalute;
    CitySupport cities;
    ImageView settings, photo;
    CustomTextView name;
    SharedPreferences preferences;
    Context mContext;
    Class mNextActivitySearch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppTheme);
        super.onCreate(savedInstanceState);
        getSupportActionBar().hide();
        setContentView(R.layout.activity_entry);
        preferences= PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        MapUtilsCustom.copyFileToExternalStorage(Constants.POI_RESOURCE,Constants.POI_FILE,getApplicationContext());
        MapUtilsCustom.copyFileToExternalStorage(Constants.MAP_RESOURCE,Constants.MAP_FILE,getApplicationContext());

        llUserSalute=(LinearLayout) findViewById(R.id.ll_user);
        photo=(ImageView) findViewById(R.id.iv_photo_face);
        name=(CustomTextView) findViewById(R.id.tv_name);
        settings=(ImageView) findViewById(R.id.settings);
        searchTaxi=(Button) findViewById(R.id.search_taxi);
        parentLayout=(ConstraintLayout) findViewById(R.id.entry_parent_view);
        llContainer=(LinearLayout) findViewById(R.id.ll_button_container);
        chooseCity=(AutoCompleteTextView) findViewById(R.id.actv_city);

        setCurrentContext();
        File faceFile= MiscellaneousUtils.imageFile(RegistrationActivityBasic.REQUEST_TAKE_FACE,
                RegistrationActivityBasic.Size.MED,getApplicationContext());
        Bitmap faceBitmap= BitmapFactory.decodeFile(faceFile.getAbsolutePath());
        photo.setImageBitmap(faceBitmap);
        name.setText(preferences.getString("firstname","Fulanito"));
        Animation slideAnimation = AnimationUtils.loadAnimation(mContext, R.anim.slide_anim);
        llUserSalute.startAnimation(slideAnimation);
        //showLayout(true,llUserSalute);
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

        settings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openSettings();
            }
        });

        setOnClickListeners();
    }

    public void onBackPressed(){
        Intent a = new Intent(Intent.ACTION_MAIN);
        a.addCategory(Intent.CATEGORY_HOME);
        a.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(a);
    }

    public void setCurrentContext(){
        mContext=EntryActivity.this;
        mNextActivitySearch=MainActivity.class;
    }

    private void showLayout(boolean show, LinearLayout linearLayout){
        if (show){
            linearLayout.setVisibility(View.VISIBLE);
            linearLayout.animate()
                    .translationX(0)
                    .setDuration(300)
                    .alpha(1)
                    .start();
        } else {
            linearLayout.animate()
                    .translationX(linearLayout.getWidth())
                    .alpha(0)
                    .setDuration(300)
                    .start();
        }
    }

    public boolean checkCity(){
        if (chooseCity.getText().toString().trim().isEmpty()){
            chooseCity.setHint(R.string.entryactivity_choosecity);
            chooseCity.setHintTextColor(ColorStateList.valueOf(ContextCompat.getColor(this,R.color.colorRed)));
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
                        ProfileUtils.displayProfileDialog(mContext,"My Profile","http://api.daleviaje.net:3001/driver/2?id=1&token=12345").show();
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

    public void setOnClickListeners(){
        searchTaxi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (checkCity()){
                    saveCity();
                    llContainer.setVisibility(View.GONE);
                    llUserSalute.clearAnimation();
                    llUserSalute.setVisibility(View.GONE);
                    settings.setVisibility(View.GONE);

                    Handler handler=new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            Intent intent = new Intent(mContext, mNextActivitySearch);
                            startActivity(intent);
                        }
                    },300);
                }
            }
        });

//        when new buttons are added use this space to add new onclicklisteners that will leave only the logo visible
    }
}
