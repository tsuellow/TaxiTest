package com.example.android.taxitest.utils;

import android.content.Context;
import android.location.Location;
import android.util.DisplayMetrics;
import android.util.Log;

import org.oscim.core.GeoPoint;

import java.io.File;

public class MiscellaneousUtils {

    public static GeoPoint locToGeo(Location location){
        return new GeoPoint(location.getLatitude(),location.getLongitude());
    }

    public static int convertDpToPixel(float dp, Context context){
        float result=dp * ((float) context.getResources().getDisplayMetrics().densityDpi / DisplayMetrics.DENSITY_DEFAULT);
        return (int)result;
    }


    //delete audio files from external Cachedir
    public static void trimCache(Context context) {
        try {
            File dir = context.getExternalCacheDir();
            if (dir != null && dir.isDirectory()) {
                deleteDir(dir);
                Log.d("cache deletion","success");
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.d("cache deletion","failed at trim");
        }
    }

    public static boolean deleteDir(File dir) {
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    Log.d("cache deletion","failed at "+children[i]);
                    return false;
                }
            }
        }

        // The directory is now empty so delete it
        return dir.delete();
    }

}
