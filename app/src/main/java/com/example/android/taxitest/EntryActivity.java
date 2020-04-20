package com.example.android.taxitest;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;

public class EntryActivity extends AppCompatActivity {

    Button searchTaxi, justWatch;
    ConstraintLayout parentLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppTheme);
        super.onCreate(savedInstanceState);
        getSupportActionBar().hide();
        setContentView(R.layout.activity_entry);

        searchTaxi=(Button) findViewById(R.id.search_taxi);
        justWatch=(Button) findViewById(R.id.watch_only);
        parentLayout=(ConstraintLayout) findViewById(R.id.entry_parent_view);

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
                        Intent intent = new Intent(EntryActivity.this, ChooseDestination.class);
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
                Intent intent = new Intent(EntryActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });
    }
}
