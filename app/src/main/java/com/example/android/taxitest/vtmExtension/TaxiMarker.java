package com.example.android.taxitest.vtmExtension;


import android.util.Log;

import com.example.android.taxitest.data.SocketObject;
import com.example.android.taxitest.data.TaxiObject;
import org.oscim.core.GeoPoint;
import org.oscim.layers.marker.MarkerInterface;
import org.oscim.layers.marker.MarkerSymbol;
import org.oscim.utils.FastMath;

public class TaxiMarker implements MarkerInterface, Comparable<TaxiMarker> {

    public enum Purpose {
        APPEAR, DISAPPEAR, MOVE, NULL
    }

    public SocketObject taxiObject;
    public GeoPoint geoPoint;
    public GeoPoint destGeoPoint;
    protected MarkerSymbol markerSymbol;
    public String barrio;
    public int color;
    public int age=0;


    private Purpose purpose=Purpose.NULL;
    private SocketObject purposeTaxiObject;
    public boolean isClicked=false;

    public TaxiMarker(SocketObject taxiObject){
        this.taxiObject=taxiObject;
        geoPoint=new GeoPoint(taxiObject.getLatitude(),taxiObject.getLongitude());
        destGeoPoint=new GeoPoint(taxiObject.getDestinationLatitude(),taxiObject.getDestinationLongitude());
        //set marker automatically  computeSymbol
    }

    public void setTaxiObject(SocketObject taxiObject){
        this.taxiObject=taxiObject;
        geoPoint=new GeoPoint(taxiObject.getLatitude(),taxiObject.getLongitude());
        destGeoPoint=new GeoPoint(taxiObject.getDestinationLatitude(),taxiObject.getDestinationLongitude());
        //set marker automatically  computeSymbol
    }

    public synchronized void setRotatedSymbol(MarkerSymbol marker) {
        markerSymbol = marker;
        markerSymbol.setRotation(taxiObject.getRotation());
    }

    public Purpose getPurpose() {
        return purpose;
    }

    public void setPurpose(Purpose purpose) {
        this.purpose = purpose;
    }

    public SocketObject getPurposeTaxiObject() {
        return purposeTaxiObject;
    }

    public void setPurposeTaxiObject(SocketObject purposeTaxiObject) {
        this.purposeTaxiObject = purposeTaxiObject;
        if (movementListener!=null)
        movementListener.onMarkerMoved(purposeTaxiObject);
    }

    public void setDestColor(int color, String barrio) {
        if (barrio!=this.barrio){
            this.color = color;
            this.barrio=barrio;
            //Log.d("colorchange", "got triggered0");
            if (colorChangeListener!=null){
                Log.d("colorchange", "got triggered");
                colorChangeListener.onColorChanged();
            }
        }
        this.color = color;
    }

    //TODO execute this before setting purposetaxiobject to taxiobject and use result to repaint ta
    public boolean doesAlphaChange(){
        int alphaOld=age/3;
        int alphaNew;
        if (taxiObject.getLocationTime()==purposeTaxiObject.getLocationTime()){
            age=age+1;
        }else{
            age=0;
        }
        alphaNew=age/3;
        return alphaNew != alphaOld;
    }

    public float getAlphaValue(){
        int step=age/3;
        float rawAlpha=1.0f-0.2f*step;
        return Math.max(0.2f,rawAlpha);
    }

    public void setIsClicked(boolean clicked){
        isClicked=clicked;
    }

    public boolean getIsClicked(){
        return isClicked;
    }

    public void setLatitude(double latitude){
        this.taxiObject.setLatitude(latitude);
        this.geoPoint=new GeoPoint(latitude,this.taxiObject.getLongitude());
    }

    public void setLongitude(double longitude){
        this.taxiObject.setLongitude(longitude);
        this.geoPoint=new GeoPoint(this.taxiObject.getLatitude(),longitude);
    }

    public void setRotation(float rotation){
        this.taxiObject.setRotation(rotation);
        markerSymbol.setRotation(rotation);
    }

    public void setGeoPoint(GeoPoint geoPoint){
        this.geoPoint=geoPoint;
        this.taxiObject.setLatitude(geoPoint.getLatitude());
        this.taxiObject.setLongitude(geoPoint.getLongitude());
    }


    @Override
    public MarkerSymbol getMarker() {
        return markerSymbol;
    }

    @Override
    public GeoPoint getPoint() {
        return geoPoint;
    }

    @Override
    public int compareTo(TaxiMarker taxiMarker) {
        return Integer.compare(this.taxiObject.getTaxiId(),taxiMarker.taxiObject.getTaxiId());
    }

    /**
     * If a MarkerItem is created using this convenience class instead of TaxiMarker,
     * this specific item will not be clusterable.
     */
    public static class NonClusterable extends TaxiMarker {
        public NonClusterable(TaxiObject taxiObject) {
            super(taxiObject);
        }
    }

    public void executeFrame(int i, int frames){
        float shift=1.0f/(frames-i);

        double latShift=(this.purposeTaxiObject.getLatitude()-this.taxiObject.getLatitude())*(double)shift;
        double lonShift=(this.purposeTaxiObject.getLongitude()-this.taxiObject.getLongitude())*(double)shift;
        float rotChange=this.purposeTaxiObject.getRotation()-this.taxiObject.getRotation();
        rotChange=(float) FastMath.clampDegree(rotChange);
        float rotShift=rotChange*shift;

        this.setLatitude(this.taxiObject.getLatitude()+latShift);
        this.setLongitude(this.taxiObject.getLongitude()+lonShift);
        this.setRotation(this.taxiObject.getRotation()+rotShift);

    }

    //movement listener
    public interface MovementListener{
        void onMarkerMoved(SocketObject newPoint);
    }

    MovementListener movementListener;

    public void setMovementListener(MovementListener movementListener){
        this.movementListener=movementListener;
    }

    //color change listener
    public interface ColorChangeListener{
        void onColorChanged();
    }

    ColorChangeListener colorChangeListener;

    public void setColorChangeListener(ColorChangeListener colorChangeListener){
        this.colorChangeListener=colorChangeListener;
    }
}
