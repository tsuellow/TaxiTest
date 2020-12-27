package com.example.android.taxitest;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;

public class EntryActivityDriver extends EntryActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //check if this is first interaction
        SharedPreferences preferences= PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        boolean isFirstInteraction=(preferences.getString("taxiId", null)==null);
        if (isFirstInteraction){
            Intent i=new Intent(EntryActivityDriver.this, RegistrationActivity.class);
            startActivity(i);
        }
        //else load normal launching activity
        super.onCreate(savedInstanceState);
        searchTaxi.setText("find clients");



    }

    @Override
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
                            Intent intent = new Intent(EntryActivityDriver.this, MainActivityDriver.class);
                            startActivity(intent);
                        }
                    },300);
                }
            }
        });

//        when new buttons are added use this space to add new onclicklisteners that will leave only the logo visible
    }

}
