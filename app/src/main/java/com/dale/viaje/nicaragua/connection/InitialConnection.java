package com.dale.viaje.nicaragua.connection;

import android.content.Context;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.dale.viaje.nicaragua.Constants;
import com.dale.viaje.nicaragua.MainActivity;
import com.dale.viaje.nicaragua.R;

import org.json.JSONException;
import org.json.JSONObject;

public class InitialConnection {
    Context context;
    public static String locationsUrl;
    public static String commsUrl;

    public InitialConnection(Context context) {
        this.context=context;
    }

    public  void requestInitialConnectionAddresses(){

        JSONObject json=new JSONObject();
        try {
            json.put("taxiId", MainActivity.myId);
            json.put("token",MainActivity.myToken);
        }catch (JSONException e){
            e.printStackTrace();
        }

        JsonObjectRequest jsonObjectRequestDriver;

        jsonObjectRequestDriver = new JsonObjectRequest
                (Request.Method.POST, Constants.INITIAL_CONN_URL, json, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            String res = response.getString("response");
                            if (res.equals("OK")) {
                                Constants.LOC_IP =response.getString("locUrl");
                                Constants.SERVER_IP= response.getString("serverUrl");
                                mOnConnDataReceivedListener.onConnDataReceived();
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                            failureMsg();
                        }

                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        error.printStackTrace();
                        failureMsg();
                    }
                });
        MySingleton.getInstance(context).addToRequestQueue(jsonObjectRequestDriver);
    }

    private void failureMsg(){
        Toast.makeText(context, R.string.initialconnection_couldnotgetconndata,Toast.LENGTH_SHORT).show();
    }

    //callback
    public interface OnConnDataReceivedListener{
        void onConnDataReceived();
    }

    OnConnDataReceivedListener mOnConnDataReceivedListener;

    public void setOnConnDataReceivedListener(OnConnDataReceivedListener listener){
        mOnConnDataReceivedListener=listener;
    }
}
