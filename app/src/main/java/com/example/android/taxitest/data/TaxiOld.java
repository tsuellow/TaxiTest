package com.example.android.taxitest.data;

import androidx.room.Entity;

@Entity(tableName = "taxiOld")
public class TaxiOld extends TaxiObject {


    public TaxiOld(int taxiId, double latitude, double longitude, long locationTime, float rotation, String type, double destinationLatitude, double destinationLongitude, int isActive) {
        super(taxiId, latitude, longitude, locationTime, rotation, type, destinationLatitude,destinationLongitude, isActive);
    }
}
