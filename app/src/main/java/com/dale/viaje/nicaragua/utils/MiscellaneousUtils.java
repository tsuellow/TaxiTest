package com.dale.viaje.nicaragua.utils;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.location.Location;
import android.os.Environment;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;

import androidx.core.app.NotificationCompat;


import com.dale.viaje.nicaragua.MainActivity;

import com.dale.viaje.nicaragua.R;
import com.dale.viaje.nicaragua.RegistrationActivityBasic;
import com.google.android.gms.common.util.IOUtils;

import org.oscim.core.GeoPoint;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.Format;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import static java.util.Calendar.DATE;
import static java.util.Calendar.MONTH;
import static java.util.Calendar.YEAR;

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

    public static void showExitNotification(Context context,String title, String text, Intent closeIntent) {

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(context, MainActivity.NOTIFICATION_CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_notification)
                        .setContentTitle(title)
                        .setContentText(text)
                        .setStyle(new NotificationCompat.BigTextStyle()
                                .bigText(text));

        mBuilder.setDefaults(Notification.DEFAULT_SOUND);
        mBuilder.setAutoCancel(true);

        PendingIntent piClose = PendingIntent.getActivity(context, 0, closeIntent, 0);
        mBuilder.addAction(0,context.getString(R.string.miscutils_exitapp),piClose);

        Intent goToAppIntent = context.getPackageManager()
                .getLaunchIntentForPackage(context.getPackageName())
                .setPackage(null)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);

        PendingIntent piGoToApp = PendingIntent.getActivity(context, 0, goToAppIntent, 0);
        mBuilder.addAction(0,context.getString(R.string.miscutils_openapp),piGoToApp);
        mBuilder.setContentIntent(piGoToApp);

        NotificationManager mNotificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        assert mNotificationManager != null;
        mNotificationManager.notify(2, mBuilder.build());
    }

    public static String convertTime(long time){
        Date date = new Date(time);
        Format format = new SimpleDateFormat("h:mm:ss a");
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

    public static String getDateStringPast(Date date){
        String dateString=null;
        if(date!=null){
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM - h:mm a");
            dateString= sdf.format(date);
        }
        return dateString;
    }

    public static String getDateStringGeneric(Date date, String pattern){
        String dateString=null;
        if(date!=null){
            SimpleDateFormat sdf = new SimpleDateFormat(pattern);
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

    public static int getDiffYears(Date first, Date last) {
        Calendar a = getCalendar(first);
        Calendar b = getCalendar(last);
        int diff = b.get(YEAR) - a.get(YEAR);
        if (a.get(MONTH) > b.get(MONTH) ||
                (a.get(MONTH) == b.get(MONTH) && a.get(DATE) > b.get(DATE))) {
            diff--;
        }
        return diff;
    }

    public static Calendar getCalendar(Date date) {
        Calendar cal = Calendar.getInstance(Locale.US);
        cal.setTime(date);
        return cal;
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

    public static File makeFile(Context context, String dir, String filename){
        File dirFile=new File(context.getExternalFilesDir(null),dir);
        if (!dirFile.exists()){
            dirFile.mkdirs();
        }
        return new File(dirFile,filename);
    }

    public static File makeThumbFile(Context context, String taxiId){
        return MiscellaneousUtils.makeFile(context,"thumbs", taxiId+".jpg");

    }

    public static File makeAudioFile(Context context, String commId, String senderId){
        return makeFile(context,commId+"/audio",senderId+"_"+(new Date().getTime())+".aac");
    }

    public static File makePhotoFile(Context context, String commId, String taxiId){
        return makeFile(context,commId+"/photo",taxiId+".jpg");
    }

    public static void saveBitmapToFile(File file, Bitmap bitmap){
        try{
            if (file.exists()){
                file.delete();
            }
            FileOutputStream fileOut = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fileOut);
            fileOut.flush();
            fileOut.close();
        }catch (Exception e){
            Log.d("saveBitmapToFile", "saveBitmapToFile: failed");
        }
    }

    public static String replaceNull(String text){
        if (text==null){
            return "-";
        }else{
            return text;
        }
    }

    public static byte[] readFileToBytes(File file) {
        byte[] bytes = new byte[(int) file.length()];
        try (FileInputStream fis = new FileInputStream(file)) {
            //read file into bytes[]
            //fis.read(bytes);
            bytes = IOUtils.toByteArray(fis);
        }catch (IOException e){
            Log.d("byteerror",""+e.getMessage());
            e.printStackTrace();
        }

        return bytes;

    }

    public static boolean isClick(MotionEvent event, Long startTime) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                startTime=event.getEventTime();
                return false;
            }
            case MotionEvent.ACTION_UP: {
                long clickDuration = event.getEventTime() - startTime;
                return clickDuration < 200;
            }
            default:
                return false;
        }
    }

    public static File imageFile(int requestType, RegistrationActivityBasic.Size size, Context context){
        String name=requestType==RegistrationActivityBasic.REQUEST_TAKE_FACE?"face":"car";
        name=size== RegistrationActivityBasic.Size.FULL?name+"_full":size== RegistrationActivityBasic.Size.MED?name+"_med":name+"_thumb";
        File photoFile;
        photoFile = new File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), ""+name+".jpg");
        return photoFile;
    }

    public static double reducePrecision(double a){
        return Math.round(a * 100000.0) / 100000.0;
    }



}
