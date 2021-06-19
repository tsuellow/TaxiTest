package com.dale.viaje.nicaragua;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;

import com.dale.viaje.nicaragua.CommunicationsRecyclerView.CommsObject;
import com.dale.viaje.nicaragua.data.ClientObject;
import com.dale.viaje.nicaragua.utils.MiscellaneousUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.Date;

public class CustomUtils {

    public static final String apiExtension="client/";
    public static final int WS_PORT=4000;
    public static final int UDP_PORT=44444;

    public static void interpretJson(JSONObject data, CommsObject comm){
        try {
            String firstName = data.getString("firstName");
            String lastName = data.getString("lastName");
            String collar = ((ClientObject)comm.taxiMarker.taxiObject).getSeats()+" pers.";
            String dateStr = data.getString("dob");
            Date dob = MiscellaneousUtils.String2Date(dateStr);
            String genStr = data.getString("gender");
            String gender = genStr.equals("m") ? "male" : "female";
            double reputation = data.getDouble("repAvg");
            long timestamp=data.getLong("timestamp");
//            String base64 = data.getString("photo");
//            byte[] biteOutput = Base64.decode(base64, 0);
//            Bitmap photo = BitmapFactory.decodeByteArray(biteOutput, 0, biteOutput.length);
            comm.commCardData.setData(firstName,collar,firstName,lastName,reputation,dob,gender,timestamp);
            //save thumb to gallery
            //File thumbFile=MiscellaneousUtils.makeFile(comm.mContext,"thumbs",getOtherStringId(comm.taxiMarker.taxiObject.getTaxiId())+".jpg");
            //MiscellaneousUtils.saveBitmapToFile(thumbFile,photo);
        }catch (JSONException e){
            e.printStackTrace();
        }
    }

    public static String getOwnStringId(int id){
        return "t"+id;
    }
    public static String getOtherStringId(int id){
        return "c"+id;
    }

    public static String getThumbUrl(int taxiId){
        return Constants.S3_SERVER_URL+"clients/thumb/"+taxiId+".jpg";
    }

    public static String getFaceUrl(int taxiId, long profileTimestamp){
        return Constants.S3_SERVER_URL+"clients/photos/"+taxiId+"/face/"+profileTimestamp+".jpg";
    }
}
