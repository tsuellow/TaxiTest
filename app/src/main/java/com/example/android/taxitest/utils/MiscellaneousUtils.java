package com.example.android.taxitest.utils;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.util.DisplayMetrics;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.example.android.taxitest.MainActivity;
import com.example.android.taxitest.R;

import org.oscim.core.GeoPoint;

import java.io.File;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Date;

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


    //TODO fix issue with the id types  (client, taxi). how to convert between int and string properly.
    public static int getNumericId(String stringId){
        return Integer.parseInt(stringId.substring(1));
    }

    public static String getStringId(int id){
        return "t"+id;
    }


    public static void showNotification(Context context,String title, String text) {

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(context, MainActivity.NOTIFICATION_CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_notification)
                        .setContentTitle(title)
                        .setContentText(text)
                        .setStyle(new NotificationCompat.BigTextStyle()
                                .bigText(text));

        mBuilder.setDefaults(Notification.DEFAULT_SOUND);
        mBuilder.setAutoCancel(true);

        Intent i = context.getPackageManager()
                .getLaunchIntentForPackage(context.getPackageName())
                .setPackage(null)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);

        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, i, 0);
        mBuilder.setContentIntent(pendingIntent);

        NotificationManager mNotificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        assert mNotificationManager != null;
        mNotificationManager.notify(1, mBuilder.build());
    }

    public static String convertTime(long time){
        Date date = new Date(time);
        Format format = new SimpleDateFormat("hh:mm:ss a");
        return format.format(date);
    }

}
