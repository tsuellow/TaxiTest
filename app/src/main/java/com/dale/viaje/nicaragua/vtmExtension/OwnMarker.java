package com.dale.viaje.nicaragua.vtmExtension;

import com.dale.viaje.nicaragua.data.TaxiObject;

import org.oscim.core.GeoPoint;
import org.oscim.layers.marker.MarkerInterface;
import org.oscim.layers.marker.MarkerSymbol;

public class OwnMarker implements MarkerInterface {

    public GeoPoint geoPoint;
    public GeoPoint destGeoPoint;
    protected MarkerSymbol markerSymbol;

    public boolean isClicked=false;

    public OwnMarker(GeoPoint geoPoint, GeoPoint dest){
        this.geoPoint=geoPoint;
        this.destGeoPoint=dest;
    }

    public void setDestination(GeoPoint dest){
        this.destGeoPoint=dest;
    }

    public void setRotatedMarker(MarkerSymbol marker, float rotation){
        markerSymbol=marker;
        markerSymbol.setRotation(rotation);
    }

    public void setIsClicked(boolean clicked){
        isClicked=clicked;
    }

    public boolean getIsClicked(){
        return isClicked;
    }

    public void setLatitude(double latitude){
        this.geoPoint=new GeoPoint(latitude,geoPoint.getLongitude());
    }

    public void setLongitude(double longitude){
        this.geoPoint=new GeoPoint(geoPoint.getLatitude(),longitude);
    }

    public void setRotation(float rotation){
        markerSymbol.setRotation(rotation);
    }

    public void setGeoPoint(GeoPoint geoPoint){
        this.geoPoint=geoPoint;
    }


    @Override
    public MarkerSymbol getMarker() {
        return markerSymbol;
    }

    @Override
    public GeoPoint getPoint() {
        return geoPoint;
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

    }
}
