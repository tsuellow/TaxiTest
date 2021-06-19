package com.dale.viaje.nicaragua.connection;

import android.util.Log;

import com.dale.viaje.nicaragua.MainActivity;
import com.dale.viaje.nicaragua.utils.MiscellaneousUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashSet;

public class WsJsonMsg{
    static int type;
    static int taxiId= MiscellaneousUtils.getNumericId(MainActivity.myId);
    static String city=MainActivity.city.name;
    static HashSet<Integer> targetChannels=new HashSet<Integer>();
    static HashSet<Integer> receptionChannels=new HashSet<Integer>();
    String payload="";

    public WsJsonMsg(String payload) {
        this.payload = payload;
    }

    public JSONObject jsonify(){
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


    public String getInitialMsg(){
        String result;
        type=0;
        receptionChannels=new HashSet<Integer>();
        result=jsonify().toString();
        return result;
    }

    public  String createLocationMsg(boolean isConnected,HashSet<Integer> newTargetChannels, HashSet<Integer> newReceivingChannels){
        String msg="";
        targetChannels=newTargetChannels;
        type=newReceivingChannels.equals(receptionChannels)&&isConnected?1:2;
        if (isConnected){
            Log.d("createLocationMsg", " changed the reception channels "+type);
            receptionChannels = newReceivingChannels;
        }
        msg=jsonify().toString();

        return  msg;
    }


}
