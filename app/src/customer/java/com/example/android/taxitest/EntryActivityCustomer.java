package com.example.android.taxitest;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class EntryActivityCustomer extends EntryActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppTheme);
        //check if this is first interaction
        preferences= PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        boolean isFirstInteraction=(preferences.getString("taxiId", null)==null);
        if (isFirstInteraction){
            Intent i=new Intent(EntryActivityCustomer.this, RegistrationActivityClient.class);
            startActivity(i);
        }
        //else load normal launching activity
        super.onCreate(savedInstanceState);
        searchTaxi.setText("find taxis");


    }

    public void setCurrentContext(){
        mContext=EntryActivityCustomer.this;
        mNextActivitySearch=ChooseDestination.class;
    }




}
