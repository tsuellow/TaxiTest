package com.example.android.taxitest;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.MenuItemCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.os.Bundle;
import android.view.Menu;

import com.example.android.taxitest.data.CommRecordObject;
import com.example.android.taxitest.data.SqlLittleDB;

import java.util.List;

public class PastTripsActivity extends AppCompatActivity {

    public static final String SEARCH_STRING = "SEARCH_STRING";
    RecyclerView rvTrips;
    public static SqlLittleDB mDb;
    PastTripsAdapter mAdapter;
    Context mContext;
    SearchView searchView;
    Toolbar mToolbar;
    String searchString;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_past_trips);

        mToolbar = (Toolbar) findViewById(R.id.toolbar_trips);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle("Past trips");

        mContext = this;
        rvTrips = (RecyclerView) findViewById(R.id.rv_past_trips);
        mAdapter = new PastTripsAdapter(mContext);
        mDb = SqlLittleDB.getInstance(getApplicationContext());

        rvTrips.setAdapter(mAdapter);
        rvTrips.setLayoutManager(new LinearLayoutManager(this));
        if (savedInstanceState==null){
            searchString = "";
        }else{
            searchString=savedInstanceState.getString(SEARCH_STRING);
        }

        populateDataSource(searchString);


    }

    private void populateDataSource(String s) {
        String str = s + "%";

        final LiveData<List<CommRecordObject>> trips = mDb.commsDao().getPastComms(str);
        trips.observe(this, new Observer<List<CommRecordObject>>() {
            @Override
            public void onChanged(@Nullable List<CommRecordObject> commRecords) {
                mAdapter.setCommRecs(commRecords);
                //mToolbar.setSubtitle(mAdapter.getItemCount()+" past contacts");

            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_search_only, menu);
        searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
        searchView.setSubmitButtonEnabled(true);
        searchView.setOnQueryTextListener(onQueryTextListener);
        searchView.setQuery(searchString,true);
        return super.onCreateOptionsMenu(menu);
    }

    private SearchView.OnQueryTextListener onQueryTextListener =
            new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    populateDataSource(query);
                    searchView.clearFocus();
                    return true;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    populateDataSource(newText);
                    searchString=newText;

                    return true;
                }
            };

    //remember if phone is flipped
    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {

        savedInstanceState.putString(SEARCH_STRING, searchString);
        // Always call the superclass so it can save the view hierarchy state
        super.onSaveInstanceState(savedInstanceState);
    }


}