package com.dale.viaje.nicaragua.data;


import androidx.room.Entity;
import androidx.room.Ignore;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({"taxiId","latitude","longitude","locationTime","rotation","type","destinationLatitude","destinationLongitude","isActive"})
@Entity(tableName = "taxiNew")
public class TaxiNew extends TaxiObject {

    private double pseudoDistance=0.0;

    public TaxiNew(@JsonProperty("taxiId") int taxiId, @JsonProperty("latitude") double latitude, @JsonProperty("longitude") double longitude,
                   @JsonProperty("locationTime") long locationTime, @JsonProperty("rotation") float rotation, @JsonProperty("type") String type,
                   @JsonProperty("destinationLatitude") double destinationLatitude, @JsonProperty("destinationLongitude") double destinationLongitude,
                   @JsonProperty("isActive") int isActive) {
        super(taxiId, latitude, longitude, locationTime, rotation, type, destinationLatitude,destinationLongitude, isActive);
        //this.pseudoDistance=pseudoDistance;
    }

    @Ignore
    public TaxiNew(){}

    public double getPseudoDistance() {
        return pseudoDistance;
    }

    public void setPseudoDistance(double pseudoDistance) {
        this.pseudoDistance = pseudoDistance;
    }
}
