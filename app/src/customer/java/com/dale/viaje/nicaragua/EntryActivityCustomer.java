package com.dale.viaje.nicaragua;

import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;

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
        searchTaxi.setText(R.string.entryactivitycustomer_findtaxis);


    }

    public void setCurrentContext(){
        mContext=EntryActivityCustomer.this;
        mNextActivitySearch=ChooseDestination.class;
    }




}
