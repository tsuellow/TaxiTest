package com.dale.viaje.nicaragua.vectorLayer;

import com.dale.viaje.nicaragua.data.SocketObject;

import org.oscim.core.GeoPoint;
import org.oscim.utils.FastMath;

import java.util.ArrayList;

public class HexagonUtils {

    public static double[] quadProfileDriver={600,450,300,250,200};
    public static double[] quadProfileDriverEmpty={450,450,450,450,450};
    public static double[] quadProfileClient={300,300,400,500,600};
    public static double[] quadProfileComms={100,100,100,100,100};

    //this creates a halo of 8 marker-points around the taximarker so that the user can receive broadcasts from these points' quadrants
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
            double deviation=i*45.0;
            if (i>0&&i<4){
               geos.addAll(getPoints(geo,theta,deviation,profile[i]));
            }else{
                if (i==0) {
                    geos.add(getSinglePoint(geo, FastMath.clampDegree(theta+0), profile[i]));
                }

                if (i==4) {
                    geos.add(getSinglePoint(geo, FastMath.clampDegree(theta+180), profile[i]));
                }
            }
        }
        return geos;
    }

    public static ArrayList<GeoPoint> getPoints(GeoPoint geoPoint, double bearing, double deviation, double h){
        ArrayList<GeoPoint> geos=new ArrayList<>();
        geos.add(getSinglePoint(geoPoint, FastMath.clampDegree(bearing+deviation),h));
        geos.add(getSinglePoint(geoPoint,FastMath.clampDegree(bearing-deviation),h));
        if (h>300){
            geos.add(getSinglePoint(geoPoint,FastMath.clampDegree(bearing+deviation),h/2));
            geos.add(getSinglePoint(geoPoint,FastMath.clampDegree(bearing-deviation),h/2));
        }
        return geos;
    }

    public static GeoPoint getSinglePoint(GeoPoint geoPoint, double degs, double h){
        double catOp=GeoPoint.latitudeDistance((int)(Math.sin(Math.toRadians(degs))*h));
        double catAd=GeoPoint.longitudeDistance((int)(Math.cos(Math.toRadians(degs))*h),geoPoint.getLatitude());
        return  new GeoPoint(geoPoint.getLatitude()+catOp,geoPoint.getLongitude()+catAd);
    }
}
