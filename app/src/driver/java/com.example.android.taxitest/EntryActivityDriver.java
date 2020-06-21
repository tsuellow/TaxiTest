package com.example.android.taxitest;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;

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
        searchTaxi.setText("test driver");
    }
}
