package com.dale.viaje.nicaragua.vectorLayer;

import android.graphics.Color;

import com.dale.viaje.nicaragua.MainActivity;
import com.dale.viaje.nicaragua.vtmExtension.TaxiMarker;

import org.locationtech.jts.geom.LineString;
import org.oscim.core.GeoPoint;
import org.oscim.layers.vector.geometries.LineDrawable;
import org.oscim.layers.vector.geometries.Style;

import java.util.Arrays;
import java.util.List;

import static com.dale.viaje.nicaragua.utils.MiscellaneousUtils.locToGeo;


/**
 * Predefined class for drawing lines from own marker to taxi
 */
public class ConnectionLineDrawable2 extends LineDrawable {

    public TaxiMarker taxiMarker;
    //static GeoPoint ownGeo= locToGeo(MainActivity.mMarkerLoc);

    static Style.Builder sb = Style.builder()
            .strokeColor(Color.RED)
            .strokeWidth(1.5f);


    public ConnectionLineDrawable2(List<GeoPoint> points, Style style) {
        super(points, style);

    }

    public ConnectionLineDrawable2 (TaxiMarker other){
        super(Arrays.asList(locToGeo(MainActivity.mMarkerLoc),other.geoPoint),sb.strokeColor(other.color).strokeWidth(1.5f).build());
        taxiMarker=other;
    }



    public void setGeometry(){
        List<GeoPoint> list=Arrays.asList(locToGeo(MainActivity.mMarkerLoc),taxiMarker.geoPoint);
        double[] coords = new double[list.size() * 2];
        int c = 0;
        for (GeoPoint p : list) {
            coords[c++] = p.getLongitude();
            coords[c++] = p.getLatitude();
        }
        this.geometry = new LineString(coordFactory.create(coords, 2), geomFactory);
    }

    public void setStyle() {
        this.style = sb.strokeColor(taxiMarker.color).build();
    }

    public void setStyle(int color) {
        this.style = sb.strokeColor(color).build();
    }


}
