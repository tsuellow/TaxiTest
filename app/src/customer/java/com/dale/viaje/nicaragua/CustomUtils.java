package com.dale.viaje.nicaragua;

import com.dale.viaje.nicaragua.CommunicationsRecyclerView.CommsObject;
import com.dale.viaje.nicaragua.utils.MiscellaneousUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

public class CustomUtils {

    public static final String apiExtension ="driver/";
    public static final int WS_PORT=3000;
    public static final int UDP_PORT=33333;


    public static void interpretJson(JSONObject data, CommsObject comm){
        try {
            String firstName = data.getString("firstName");
            String lastName = data.getString("lastName");
            String collar = data.getString("nrPlate");
            String dateStr = data.getString("dob");
            Date dob = MiscellaneousUtils.String2Date(dateStr);
            String genStr = data.getString("gender");
            String gender = genStr.equals("m") ? "male" : "female";
            double reputation = data.getDouble("repAvg");
            long timestamp=data.getLong("timestamp");
            //String base64 = data.getString("photo");
            //byte[] biteOutput = Base64.decode(base64, 0);
            //Bitmap photo = BitmapFactory.decodeByteArray(biteOutput, 0, biteOutput.length);
            comm.commCardData.setData(firstName,collar,firstName,lastName,reputation,dob,gender,timestamp);
        }catch (JSONException e){
            e.printStackTrace();
        }
    }

    public static String getOwnStringId(int id){
        return "c"+id;
    }
    public static String getOtherStringId(int id){
        return "t"+id;
    }

    public static String getThumbUrl(int taxiId){
        return Constants.S3_SERVER_URL+"drivers/thumb/"+taxiId+".jpg";
    }

    public static String getFaceUrl(int taxiId, long profileTimestamp){
        return Constants.S3_SERVER_URL+"drivers/photos/"+taxiId+"/face/"+profileTimestamp+".jpg";
    }
}
