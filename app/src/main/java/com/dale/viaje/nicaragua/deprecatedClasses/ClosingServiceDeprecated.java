package com.dale.viaje.nicaragua.deprecatedClasses;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import com.dale.viaje.nicaragua.connection.OutgoingWebSocket;
import com.dale.viaje.nicaragua.data.SocketObject;

public class ClosingServiceDeprecated extends Service {
    private static final String TAG = "ClosingService";
    OutgoingWebSocket mWebSocket;
    String closingMsg;
    private final IBinder mBinder = new LocalBinder();

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        mWebSocket.sendMsg(closingMsg);
        Log.d(TAG, "onTaskRemoved: was excecuted");
        stopSelf();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    //returns the instance of the service
    public class LocalBinder extends Binder {
        public ClosingServiceDeprecated getServiceInstance(){
            return ClosingServiceDeprecated.this;
        }
    }

    public void setWebSocket(OutgoingWebSocket outgoingWebSocket){
        mWebSocket=outgoingWebSocket;
    }

    public void setClosingMsg(SocketObject socketObject){
        int status=socketObject.getIsActive();
        socketObject.setIsActive(0);
        closingMsg=socketObject.objectToCsv();
        socketObject.setIsActive(status);
    }
}
