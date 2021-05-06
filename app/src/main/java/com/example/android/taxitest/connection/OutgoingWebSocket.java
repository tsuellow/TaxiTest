package com.example.android.taxitest.connection;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;

public class OutgoingWebSocket extends WebSocketClient {

    private static final String TAG = "OutgoingWebSocket";

    public static int DISCONNECTED =-1;
    public static int IN_PROGRESS=0;
    public static int ESTABLISHED=1;

    private int connectionStatus = DISCONNECTED;
    //-1 --pre initialization, server does not know we exist;
    //0 --building connection to server: we established a ws connection and now have to establish udp connection;
    //1 --Server has all the data on us that it needs we can start sending our location

    public boolean intentToClose=false;
    private Context context;
    public IncomingUdpSocket udpSocket;


    public OutgoingWebSocket(URI serverUri, Context context, IncomingUdpSocket udpSocket){
        super(serverUri);
        this.context=context;
        this.udpSocket=udpSocket;
        //put matching udp class in here
    }

    public void connectToServer(){
        this.connect();
        Log.d(TAG, "connectToServer: triggered");

    }

    public int getConnectionStatus() {
        return connectionStatus;
    }

    public void setConnectionStatus(int connectionStatus) {
        this.connectionStatus = connectionStatus;
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        Log.d(TAG,"connection to WS server established");
        //immediately send the first msg in order to associate our connection with our taxiId and city
        send(WsJsonMsg.getInitialMsg());
    }

    @Override
    public void onMessage(String message) {
        Log.d(TAG,"received msg: "+message);
        JSONObject jsonMsg=new JSONObject();
        try{
            jsonMsg=new JSONObject(message);
            int type=jsonMsg.getInt("type");
            String action=jsonMsg.getString("action");
            if (type==0){
                switch (action){
                    case "SEND UDP":
                        //trigger udp hole punching from udp class
                        setConnectionStatus(IN_PROGRESS);
                        udpSocket.setTriggerConnectionProcess(true);
                        udpSocket.startServer();
                        break;
                    case "SEND LOC":
                        //trigger gate that allows a location fix to translate to a send action
                        setConnectionStatus(ESTABLISHED);
                        udpSocket.setTriggerConnectionProcess(false);
                        break;
                    case "DISCONNECT":
                        //trigger disconnection
                        setConnectionStatus(DISCONNECTED);
                        this.close();
                    default:
                        Log.d(TAG,"undefined msg: "+message);
                        Toast.makeText(context,message,Toast.LENGTH_SHORT).show();
                }
            }

        }catch(Exception e){
            e.printStackTrace();
        }

    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        Log.d(TAG,"you have disconnected. reason: "+reason+". were you remotely disconnected? "+remote);
    }

    @Override
    public void onError(Exception ex) {
        Log.d(TAG,"an error happened with ws: "+ex.getMessage());
    }

    public void sendMsg(String msg){
        if (connectionStatus==ESTABLISHED && isOpen()){
            send(msg);
        }else{
            Toast.makeText(context,"connection failed. could not send location",Toast.LENGTH_LONG).show();
        }
    }


}
