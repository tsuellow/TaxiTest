package com.example.android.taxitest.vectorLayer;

import com.example.android.taxitest.data.SocketObject;
import com.example.android.taxitest.vtmExtension.OwnMarker;
import com.example.android.taxitest.vtmExtension.TaxiMarker;

import org.oscim.core.GeoPoint;
import org.oscim.layers.marker.MarkerInterface;

import java.util.ArrayList;

public class HexagonUtils {

    public static double[] quadProfileDriver={600,450,300,250,200};
    public static double[] quadProfileDriverEmpty={450,450,450,450,450};
    public static double[] quadProfileClient={300,300,400,500,600};
    public static double[] quadProfileComms={100,100,100,100,100};

    //this creates a halo of 8 marker-points around the taximarker so that the user can receive broadcasts from these points quadrants
    public static ArrayList<GeoPoint> getSurroundingMarkers(double[] profile, SocketObject socketObject){
        ArrayList<GeoPoint> geos=new ArrayList<>();

        GeoPoint dest=new GeoPoint(socketObject.getDestinationLatitude(),socketObject.getDestinationLongitude());
        GeoPoint geo=new GeoPoint(socketObject.getLatitude(),socketObject.getLongitude());
        //add the location of the marker itself to start
        geos.add(geo);

        double tinyCorrection=0.0;
        if (dest.getLongitude()-geo.getLongitude()==0.0)
            tinyCorrection=1E-11;

        //calculating theta
        double deltaLat=dest.getLatitude()-geo.getLatitude();
        double deltaLon=dest.getLongitude()-geo.getLongitude()+tinyCorrection;
        double slope=(deltaLat)/(deltaLon);
        double theta=Math.toDegrees(Math.atan(slope));
        if (deltaLon<0) theta = 180 + theta;
        for (int i=0;i<5;i++){
            double degs=theta+i*45.0;
            if (i>0&&i<4){
               geos.addAll(getPoints(geo,degs,profile[i]));
            }else{
                if (i==0) {
                    geos.add(getSinglePoint(geo, degs, profile[i], false));
                }

                if (i==4) {
                    geos.add(getSinglePoint(geo, degs, profile[i], true));
                }
            }
        }
        return geos;
    }

    public static ArrayList<GeoPoint> getPoints(GeoPoint geoPoint, double degs, double h){
        ArrayList<GeoPoint> geos=new ArrayList<>();
        geos.add(getSinglePoint(geoPoint,degs,h,false));
        geos.add(getSinglePoint(geoPoint,degs,h,true));
        if (h>300){
            geos.add(getSinglePoint(geoPoint,degs,h/2,false));
            geos.add(getSinglePoint(geoPoint,degs,h/2,true));
        }
        return geos;
    }

    public static GeoPoint getSinglePoint(GeoPoint geoPoint, double degs, double h, boolean mirror){
        int v=mirror?-1:1;
        double catOp=Math.sin(Math.toRadians(degs))*h;
        double catAd=Math.cos(Math.toRadians(degs))*h;
        return  new GeoPoint(geoPoint.getLatitude()+v*catOp,geoPoint.getLongitude()+v*catAd);
    }
}