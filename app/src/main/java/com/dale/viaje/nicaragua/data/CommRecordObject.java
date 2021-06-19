package com.dale.viaje.nicaragua.data;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import com.dale.viaje.nicaragua.CommunicationsRecyclerView.CommsObject;
import com.dale.viaje.nicaragua.CustomUtils;
import com.dale.viaje.nicaragua.MainActivity;
import com.dale.viaje.nicaragua.R;

import org.oscim.core.GeoPoint;

import java.util.Date;

@Entity(tableName = "commsTable")
public class CommRecordObject {
    
    @Ignore
    public static final int OBSERVING=0;
    @Ignore
    public static final int CONTACTED=2;
    @Ignore
    public static final int ACCEPTED=1;

    //comm related
    @PrimaryKey @NonNull
    private String commId="0_0";
    private long timestamp;
    private int commStatus;
    //party related
    private String taxiId;
    private String firstName;
    private String lastName;
    private String gender;
    private long dob;
    private String collar;
    private String seats;
    private String type;
    private String photoFacePath;
    private double reputation;
    //trip related
    private String barrioFrom;
    private String barrioTo;
    private int colorFrom;
    private int colorTo;
    private double latFrom;
    private double lonFrom;
    private double latTo;
    private double lonTo;
    //fix later
    private String city;

    @Ignore
    public CommRecordObject(CommsObject comm, ClientObject clientObject, Context context) {
        commId = comm.commId;
        timestamp = new Date().getTime();
        commStatus = OBSERVING;

        taxiId = CustomUtils.getOtherStringId(comm.taxiMarker.taxiObject.getTaxiId());

        latFrom=clientObject.getLatitude();
        lonFrom=clientObject.getLongitude();
        latTo=clientObject.getDestinationLatitude();
        lonTo=clientObject.getDestinationLongitude();
        barrioFrom=((MainActivity)context).mBarriosLayer.getContainingBarrio(new GeoPoint(latFrom,lonFrom)).getBarrioName();
        barrioTo=((MainActivity)context).mBarriosLayer.getContainingBarrio(new GeoPoint(latTo,lonTo)).getBarrioName();
        colorFrom=((MainActivity)context).mBarriosLayer.getContainingBarrio(new GeoPoint(latFrom,lonFrom)).getStyle().fillColor;
        colorTo=((MainActivity)context).mBarriosLayer.getContainingBarrio(new GeoPoint(latTo,lonTo)).getStyle().fillColor;
        seats=clientObject.getSeats()+" "+context.getString(R.string.commrecordobject_persabbrev);
        type=((TaxiObject)comm.taxiMarker.taxiObject).getType();
        //remember entering city as well

        //default placeholders
        fillPlaceHolders();
    }

    @Ignore
    public CommRecordObject(CommsObject comm, TaxiObject taxiObject, Context context) {
        commId = comm.commId;
        timestamp = new Date().getTime();
        commStatus = 0;

        taxiId = CustomUtils.getOtherStringId(comm.taxiMarker.taxiObject.getTaxiId());

        latFrom=comm.taxiMarker.taxiObject.getLatitude();
        lonFrom=comm.taxiMarker.taxiObject.getLongitude();
        latTo=comm.taxiMarker.taxiObject.getDestinationLatitude();
        lonTo=comm.taxiMarker.taxiObject.getDestinationLongitude();
        barrioFrom=((MainActivity)context).mBarriosLayer.getContainingBarrio(new GeoPoint(latFrom,lonFrom)).getBarrioName();
        barrioTo=((MainActivity)context).mBarriosLayer.getContainingBarrio(new GeoPoint(latTo,lonTo)).getBarrioName();
        colorFrom=((MainActivity)context).mBarriosLayer.getContainingBarrio(new GeoPoint(latFrom,lonFrom)).getStyle().fillColor;
        colorTo=((MainActivity)context).mBarriosLayer.getContainingBarrio(new GeoPoint(latTo,lonTo)).getStyle().fillColor;
        seats=((ClientObject)comm.taxiMarker.taxiObject).getSeats()+" "+context.getString(R.string.commrecordobject_persabbrev);
        type=taxiObject.getType();
        //remember entering city as well

        //default placeholders
        fillPlaceHolders();
    }

    private void fillPlaceHolders(){
        firstName="Fulanito";
        lastName="de Tal";
        dob=new Date().getTime();
        gender="x";
        collar=seats;
    }

    public CommRecordObject() {
    }

    public String getCommId() {
        return commId;
    }

    public void setCommId(String commId) {
        this.commId = commId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public int getCommStatus() {
        return commStatus;
    }

    public void setCommStatus(int commStatus) {
        this.commStatus = commStatus;
    }

    public String getTaxiId() {
        return taxiId;
    }

    public void setTaxiId(String taxiId) {
        this.taxiId = taxiId;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public long getDob() {
        return dob;
    }

    public void setDob(long dob) {
        this.dob = dob;
    }

    public String getCollar() {
        return collar;
    }

    public void setCollar(String collar) {
        this.collar = collar;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getPhotoFacePath() {
        return photoFacePath;
    }

    public void setPhotoFacePath(String photoFacePath) {
        this.photoFacePath = photoFacePath;
    }

    public double getReputation() {
        return reputation;
    }

    public void setReputation(double reputation) {
        this.reputation = reputation;
    }

    public String getBarrioFrom() {
        return barrioFrom;
    }

    public void setBarrioFrom(String barrioFrom) {
        this.barrioFrom = barrioFrom;
    }

    public String getBarrioTo() {
        return barrioTo;
    }

    public void setBarrioTo(String barrioTo) {
        this.barrioTo = barrioTo;
    }

    public int getColorFrom() {
        return colorFrom;
    }

    public void setColorFrom(int colorFrom) {
        this.colorFrom = colorFrom;
    }

    public int getColorTo() {
        return colorTo;
    }

    public void setColorTo(int colorTo) {
        this.colorTo = colorTo;
    }

    public double getLatFrom() {
        return latFrom;
    }

    public void setLatFrom(double latFrom) {
        this.latFrom = latFrom;
    }

    public double getLonFrom() {
        return lonFrom;
    }

    public void setLonFrom(double lonFrom) {
        this.lonFrom = lonFrom;
    }

    public double getLatTo() {
        return latTo;
    }

    public void setLatTo(double latTo) {
        this.latTo = latTo;
    }

    public double getLonTo() {
        return lonTo;
    }

    public void setLonTo(double lonTo) {
        this.lonTo = lonTo;
    }

    public String getSeats() {
        return seats;
    }

    public void setSeats(String seats) {
        this.seats = seats;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }


}
