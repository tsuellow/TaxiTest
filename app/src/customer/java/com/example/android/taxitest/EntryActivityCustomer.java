package com.example.android.taxitest;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;

public class EntryActivityCustomer extends EntryActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppTheme);
        //check if this is first interaction
        SharedPreferences preferences= PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        boolean isFirstInteraction=(preferences.getString("taxiId", null)==null);
        if (isFirstInteraction){
            Intent i=new Intent(EntryActivityCustomer.this, RegistrationActivity.class);
            startActivity(i);
        }
        //else load normal launching activity
        super.onCreate(savedInstanceState);
        searchTaxi.setText("find taxis");


    }

    @Override
    public void setOnClickListeners(final Button searchTaxi, final Button justWatch) {
        searchTaxi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                searchTaxi.setVisibility(View.GONE);
                justWatch.setVisibility(View.GONE);
                //parentLayout.setVisibility(View.GONE);
                //setTheme(R.style.AppTheme_Launcher);
                Handler handler=new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Intent intent = new Intent(EntryActivityCustomer.this, ChooseDestination.class);
                        startActivity(intent);
                    }
                },300);

            }
        });

        justWatch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                searchTaxi.setVisibility(View.GONE);
                justWatch.setVisibility(View.GONE);
                Intent intent = new Intent(EntryActivityCustomer.this, MainActivityCustomer.class);
                startActivity(intent);
            }
        });
    }
}
