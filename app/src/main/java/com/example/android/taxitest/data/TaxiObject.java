package com.example.android.taxitest.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import com.example.android.taxitest.vtmExtension.TaxiMarker;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;


@JsonPropertyOrder({"taxiId","latitude","longitude","locationTime","rotation","type","destinationLatitude","destinationLongitude","isActive"})
@Entity(tableName = "taxiBase")
public class TaxiObject implements Comparable<TaxiObject>{
    @PrimaryKey
    private int taxiId;
    private double latitude;
    private double longitude;
    private long locationTime;
    private float rotation;
    private String type;
    private double destinationLatitude;
    private double destinationLongitude;
    private int isActive;

    public TaxiObject(int taxiId, double latitude, double longitude, long locationTime, float rotation, String type, double destinationLatitude, double destinationLongitude, int isActive) {
        this.taxiId = taxiId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.locationTime = locationTime;
        this.rotation = rotation;
        this.type = type;
        this.destinationLatitude = destinationLatitude;
        this.destinationLongitude= destinationLongitude;
        this.isActive = isActive;
    }

    @Ignore
    public TaxiObject(){}

    @Ignore
    public TaxiObject(TaxiMarker taxiMarker) {
        this.taxiId=taxiMarker.taxiObject.getTaxiId();
        this.latitude=taxiMarker.taxiObject.getLatitude();
        this.longitude=taxiMarker.taxiObject.getLongitude();
        this.locationTime=taxiMarker.taxiObject.getLocationTime();
        this.rotation=taxiMarker.taxiObject.getRotation();
        this.type=taxiMarker.taxiObject.getType();
        this.destinationLatitude=taxiMarker.taxiObject.getDestinationLatitude();
        this.destinationLongitude=taxiMarker.taxiObject.getDestinationLongitude();
        this.isActive = 1;
    }

    public String taxiObjectToCsv(){
        String result=null;
        try{
            result=""+this.taxiId+"|"+this.latitude+"|"+this.longitude+"|"+this.locationTime+"|"+this.rotation+"|"+this.type+"|"+this.destinationLatitude+"|"+this.destinationLongitude+"|"+this.isActive;
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
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
    public int compareTo(@NonNull TaxiObject taxiObject) {
        return Integer.compare(this.getTaxiId(),taxiObject.getTaxiId());
    }
}
