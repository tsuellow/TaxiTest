package com.dale.viaje.nicaragua.data;

import androidx.room.Entity;
import androidx.room.Ignore;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({"taxiId","latitude","longitude","locationTime","rotation","seats","extra","destinationLatitude","destinationLongitude","isActive"})
@Entity(tableName = "clientNew")
public class ClientNew extends ClientObject {

    private double pseudoDistance=0.0;

    public ClientNew(@JsonProperty("taxiId") int taxiId, @JsonProperty("latitude") double latitude, @JsonProperty("longitude") double longitude,
                   @JsonProperty("locationTime") long locationTime, @JsonProperty("rotation") float rotation, @JsonProperty("seats") int seats, @JsonProperty("extra") String extra,
                   @JsonProperty("destinationLatitude") double destinationLatitude, @JsonProperty("destinationLongitude") double destinationLongitude,
                   @JsonProperty("isActive") int isActive) {
        super(taxiId, latitude, longitude, locationTime, rotation, seats, extra, destinationLatitude, destinationLongitude, isActive);
        //this.pseudoDistance=pseudoDistance;
    }

    @Ignore
    public ClientNew(){}

    public double getPseudoDistance() {
        return pseudoDistance;
    }

    public void setPseudoDistance(double pseudoDistance) {
        this.pseudoDistance = pseudoDistance;
    }
}
