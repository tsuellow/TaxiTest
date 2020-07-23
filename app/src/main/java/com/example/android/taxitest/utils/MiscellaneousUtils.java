package com.example.android.taxitest.utils;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.example.android.taxitest.EntryActivityCustomer;
import com.example.android.taxitest.MainActivity;
import com.example.android.taxitest.MainActivityCustomer;
import com.example.android.taxitest.R;

import org.oscim.core.GeoPoint;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.text.Format;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
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

    public static void showExitNotification(Context context,String title, String text) {

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(context, MainActivity.NOTIFICATION_CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_notification)
                        .setContentTitle(title)
                        .setContentText(text)
                        .setStyle(new NotificationCompat.BigTextStyle()
                                .bigText(text));

        mBuilder.setDefaults(Notification.DEFAULT_SOUND);
        mBuilder.setAutoCancel(true);

        Intent closeIntent = new Intent((MainActivityCustomer)context, EntryActivityCustomer.class);
        ((MainActivityCustomer)context).finish();
        closeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent piClose = PendingIntent.getActivity(context, 0, closeIntent, 0);
        mBuilder.addAction(0,"exit app",piClose);

        Intent goToAppIntent = context.getPackageManager()
                .getLaunchIntentForPackage(context.getPackageName())
                .setPackage(null)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);

        PendingIntent piGoToApp = PendingIntent.getActivity(context, 0, goToAppIntent, 0);
        mBuilder.addAction(0,"open app",piGoToApp);
        mBuilder.setContentIntent(piGoToApp);

        NotificationManager mNotificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        assert mNotificationManager != null;
        mNotificationManager.notify(2, mBuilder.build());
    }

    public static String convertTime(long time){
        Date date = new Date(time);
        Format format = new SimpleDateFormat("hh:mm:ss a");
        return format.format(date);
    }

    public static String getDateString(Date date){
        String dateString=null;
        if(date!=null){
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
            dateString= sdf.format(date);
        }
        return dateString;
    }

    public static Date String2Date(String dateStr){
        SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        Date date = null;
        if(dateStr!=null) {
            try {
                date = sdf.parse(dateStr);
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        return date;
    }

    public static String getShortDateString(Calendar cal){
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH);
        int day = cal.get(Calendar.DAY_OF_MONTH);
        month = 1 + month;
        return day + "/" + month + "/" + year;
    }

    public static Date getRoundDate(Date date){
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        return calendar.getTime();
    }

    public static Bitmap makeSquaredImage(Bitmap bitmap){
        int width  = bitmap.getWidth();
        int height = bitmap.getHeight();
        int newWidth = (height > width) ? width : height;
        int newHeight = (height > width)? height - ( height - width) : height;
        int cropW = (width - height) / 2;
        cropW = (cropW < 0)? 0: cropW;
        int cropH = (height - width) / 2;
        cropH = (cropH < 0)? 0: cropH;
        Bitmap cropImg = Bitmap.createBitmap(bitmap, cropW, cropH, newWidth, newHeight);
        return cropImg;
    }

    public static String base64Bitmap(Bitmap bitmap){
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG,100,baos);
        byte[] b = baos.toByteArray();
        return Base64.encodeToString(b, 0);
    }

    //TODO make this method country agnostic use libphone
    public static String depuratePhone(String rawPhone){
        String phone=rawPhone.replace(" ","").replace("+","00").replace("-","");
        String depPhone=null;
        if (phone.length()>2){
            if (phone.substring(0,2).contentEquals("00")){
                depPhone=phone;
            }else{
                depPhone="00505"+phone;
            }
        }
        return depPhone;

    }

}
