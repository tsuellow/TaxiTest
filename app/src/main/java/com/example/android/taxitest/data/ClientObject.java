package com.example.android.taxitest.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({"taxiId","latitude","longitude","locationTime","rotation","seats","extra","destinationLatitude","destinationLongitude","isActive"})
@Entity(tableName = "clientBase")
public class ClientObject implements SocketObject, Comparable<ClientObject> {

    @PrimaryKey
    private int taxiId;
    private double latitude;
    private double longitude;
    private long locationTime;
    private float rotation;
    private int seats;
    private String extra;
    private double destinationLatitude;
    private double destinationLongitude;
    private int isActive;

    public ClientObject(int taxiId, double latitude, double longitude, long locationTime, float rotation, int seats, String extra, double destinationLatitude, double destinationLongitude, int isActive) {
        this.taxiId = taxiId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.locationTime = locationTime;
        this.rotation = rotation;
        this.seats = seats;
        this.extra=extra;
        this.destinationLatitude = destinationLatitude;
        this.destinationLongitude= destinationLongitude;
        this.isActive = isActive;
    }

    @Ignore
    public ClientObject(){}

//    @Ignore
//    public TaxiObject(TaxiMarker taxiMarker) {
//        this.taxiId=taxiMarker.taxiObject.getTaxiId();
//        this.latitude=taxiMarker.taxiObject.getLatitude();
//        this.longitude=taxiMarker.taxiObject.getLongitude();
//        this.locationTime=taxiMarker.taxiObject.getLocationTime();
//        this.rotation=taxiMarker.taxiObject.getRotation();
//        this.type=(TaxiObject) taxiMarker.taxiObject.getType();
//        this.destinationLatitude=taxiMarker.taxiObject.getDestinationLatitude();
//        this.destinationLongitude=taxiMarker.taxiObject.getDestinationLongitude();
//        this.isActive = 1;
//    }

    public String objectToCsv(){
        String result=null;
        try{
            result=""+this.taxiId +"|"+this.latitude+"|"+this.longitude+"|"+this.locationTime+"|"+this.rotation+"|"+this.seats+"|"+this.extra+"|"+this.destinationLatitude+"|"+this.destinationLongitude+"|"+this.isActive;
        }catch (Exception e){
            e.printStackTrace();
        }
        return result;
    }

    //getters and setters

    public int getTaxiId() {
        return taxiId;
    }

    public void setTaxiId(int taxiId) {
        this.taxiId = taxiId;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public long getLocationTime() {
        return locationTime;
    }

    public void setLocationTime(long locationTime) {
        this.locationTime = locationTime;
    }

    public float getRotation() {
        return rotation;
    }

    public void setRotation(float rotation) {
        this.rotation = rotation;
    }

    public int getSeats() {
        return seats;
    }

    public void setSeats(int seats) {
        this.seats = seats;
    }

    public String getExtra() {
        return extra;
    }

    public void setExtra(String extra) {
        this.extra = extra;
    }

    public double getDestinationLatitude() {
        return destinationLatitude;
    }

    public void setDestinationLatitude(double destinationLatitude) {
        this.destinationLatitude = destinationLatitude;
    }

    public double getDestinationLongitude() {
        return destinationLongitude;
    }

    public void setDestinationLongitude(double destinationLongitude) {
        this.destinationLongitude = destinationLongitude;
    }

    public int getIsActive() {
        return isActive;
    }

    public void setIsActive(int isActive) {
        this.isActive = isActive;
    }

    @Override
    public int compareTo(@NonNull ClientObject clientObject) {
        return Integer.compare(this.getTaxiId(),clientObject.getTaxiId());
    }
}
