package com.example.android.taxitest;

import android.os.Bundle;

public class EntryActivityCustomer extends EntryActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        searchTaxi.setText("test customer");
    }
}
