package com.example.android.taxitest.vectorLayer;

import android.graphics.Color;

import com.example.android.taxitest.MainActivity;
import com.example.android.taxitest.vtmExtension.OwnMarker;
import com.example.android.taxitest.vtmExtension.TaxiMarker;

import org.locationtech.jts.geom.LineString;
import org.oscim.core.GeoPoint;
import org.oscim.layers.vector.geometries.LineDrawable;
import org.oscim.layers.vector.geometries.Style;

import java.util.Arrays;
import java.util.List;

import static com.example.android.taxitest.utils.MiscellaneousUtils.locToGeo;

public class ConnectionLineDrawable2 extends LineDrawable {

    TaxiMarker taxiMarker;
    //static GeoPoint ownGeo= locToGeo(MainActivity.mMarkerLoc);

    static Style.Builder sb = Style.builder()
            .strokeColor(Color.RED)
            .strokeWidth(2);

    public ConnectionLineDrawable2(List<GeoPoint> points, Style style) {
        super(points, style);

    }

    public ConnectionLineDrawable2 (TaxiMarker other){
        super(Arrays.asList(locToGeo(MainActivity.mMarkerLoc),other.geoPoint),sb.strokeColor(other.color).build());
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
}
