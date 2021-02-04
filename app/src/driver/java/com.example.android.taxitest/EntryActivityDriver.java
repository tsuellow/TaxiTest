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
            Intent i=new Intent(EntryActivityDriver.this, RegistrationActivityDriver.class);
            startActivity(i);
        }
        //else load normal launching activity
        super.onCreate(savedInstanceState);
        searchTaxi.setText("find clients");



    }

    public void setCurrentContext(){
        mContext=EntryActivityDriver.this;
        mNextActivitySearch=MainActivityDriver.class;
    }


}
