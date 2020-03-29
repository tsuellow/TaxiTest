package com.example.android.taxitest.connection;

import android.content.Context;
import android.util.Log;

import com.example.android.taxitest.CommunicationsRecyclerView.CommunicationsAdapter;
import com.example.android.taxitest.data.SqlLittleDB;
import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;

public class CommsConnection {

    private Socket mSocket;
    private Context mContext;

    Emitter.Listener onLocationUpdate;

    public CommsConnection(String url, Context context){
        try {
            mSocket = IO.socket(url);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        mContext=context;

    }

    public void connectSocket(){
        mSocket.on("location update",onLocationUpdate);
        mSocket.connect();
    }

    public void disconnectSocket(){
        mSocket.off("location update",onLocationUpdate);
        mSocket.disconnect();
    }

    public void initializeSocketListener(){
        onLocationUpdate=new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                final String jsonString=(String) args[0];
                try {
                    JSONObject jsonObject = new JSONObject(jsonString);

                }catch (JSONException err){
                    Log.d("Error", err.toString());
                }

            }
        };
    }
}
