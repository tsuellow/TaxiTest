package com.dale.viaje.nicaragua.data;

import androidx.room.Entity;

@Entity(tableName = "clientOld")
public class ClientOld extends ClientObject {


    public ClientOld(int taxiId, double latitude, double longitude, long locationTime, float rotation, int seats, String extra, double destinationLatitude, double destinationLongitude, int isActive) {
        super(taxiId, latitude, longitude, locationTime, rotation, seats, extra, destinationLatitude,destinationLongitude, isActive);
    }
}
