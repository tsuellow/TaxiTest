package com.example.android.taxitest.connection;

import com.example.android.taxitest.MainActivity;
import com.example.android.taxitest.utils.MiscellaneousUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.HashSet;

public class WsJsonMsg{
    static int type;
    static int taxiId= MiscellaneousUtils.getNumericId(MainActivity.myId);
    static String city=MainActivity.city.name;
    static HashSet<Integer> targetChannels=new HashSet<Integer>();
    static HashSet<Integer> receptionChannels=new HashSet<Integer>();
    static String payload="";


    public static JSONObject jsonify(){
        JSONObject json=new JSONObject();
        try{
            json.put("type",type);
            json.put("taxiId",taxiId);
            json.put("city",city);
            json.put("targetChannels",new JSONArray(targetChannels));
            json.put("receptionChannels",new JSONArray(receptionChannels));
            json.put("payloadCSV",payload);
        }
        catch(JSONException e){
            e.printStackTrace();
        }
        return  json;
    }


    public static String getInitialMsg(){
        String result;
        type=0;
        result=jsonify().toString();
        return result;
    }

    public static String createLocationMsg(boolean isConnected,HashSet<Integer> newTargetChannels, HashSet<Integer> newReceivingChannels, String newPayload){
        String msg="";
        targetChannels=newTargetChannels;
        payload=newPayload;
        type=2;//newReceivingChannels.equals(receptionChannels)&&isConnected?1:2;
        receptionChannels=newReceivingChannels;
        msg=jsonify().toString();

        return  msg;
    }


}
