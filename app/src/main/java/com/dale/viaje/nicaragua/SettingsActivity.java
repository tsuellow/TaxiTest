package com.dale.viaje.nicaragua;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.os.Bundle;

public class SettingsActivity extends AppCompatActivity {

    public Toolbar mToolbar;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        mToolbar = (Toolbar) findViewById(R.id.toolbar_settings);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle(R.string.settingsactivity_title);
    }

    @Override
    public void onBackPressed(){
        super.onBackPressed();
    }
}