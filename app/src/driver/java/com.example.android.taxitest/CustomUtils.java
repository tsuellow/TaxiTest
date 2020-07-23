package com.example.android.taxitest;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;

import com.example.android.taxitest.CommunicationsRecyclerView.CommsObject;
import com.example.android.taxitest.data.ClientObject;
import com.example.android.taxitest.utils.MiscellaneousUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

public class CustomUtils {

    public static final String phpFile="get_client.php";

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
            String base64 = data.getString("photo");
            byte[] biteOutput = Base64.decode(base64, 0);
            Bitmap photo = BitmapFactory.decodeByteArray(biteOutput, 0, biteOutput.length);
            comm.setCommCardData(comm.new CardData(firstName,collar,firstName,lastName,reputation,dob,gender,photo));
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
}
