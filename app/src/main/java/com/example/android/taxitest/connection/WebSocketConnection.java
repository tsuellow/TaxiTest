package com.example.android.taxitest.connection;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import com.example.android.taxitest.Constants;
import com.example.android.taxitest.data.SqlLittleDB;
import com.example.android.taxitest.data.TaxiObject;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;
import org.json.JSONObject;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;


public class WebSocketConnection {

    private Socket mSocket;
    private Context mContext;
    private Activity mActivity;
    public List<TaxiObject> mNewPositionsList=new ArrayList<TaxiObject>();


    SqlLittleDB mDb;


    Emitter.Listener onLocationUpdate;

    public WebSocketConnection(String url, Activity activity, Context context){
        mActivity=activity;
        try {
            mSocket = IO.socket(url);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        mContext=context;
        mDb=SqlLittleDB.getInstance(context);
    }

    public void connectSocket(){
        mSocket.on("location update",onLocationUpdate);
        mSocket.connect();
    }

    public void disconnectSocket(){
        mSocket.off("location update",onLocationUpdate);
        mSocket.disconnect();
    }


    private CsvMapper mapper=new CsvMapper();
    private CsvSchema schema = mapper.schemaFor(TaxiObject.class).withColumnSeparator('|');
    private ObjectReader r=mapper.readerFor(TaxiObject.class).with(schema);

    public void initializeSocketListener(){  //add target object itemizedlayer
        onLocationUpdate=new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                final String csvString=(String) args[0];
                try{
                    TaxiObject taxiObject=r.readValue(csvString);
                    addOrReset(false,taxiObject);
                }catch(Exception e){
                    e.printStackTrace();
                    Log.d("CSV","not reading");
                }
            }
        };
    }

    public void attemptSend(String locationObject){
        mSocket.emit("location update", locationObject);
    }

    public synchronized void addOrReset(boolean reset, TaxiObject taxiObject){
        if (reset){
            List<TaxiObject> tempList=mNewPositionsList;
            processReceivedData(tempList);
            mNewPositionsList.clear();
        }else{
            mNewPositionsList.add(taxiObject);
        }
    }

    //private Timer mDataAccumulateTimer=new Timer("accumulationTimer",true);
    private class ExecuteReset extends TimerTask {
        @Override
        public void run() {
            addOrReset(true,null);
        }
    };

    public void startAccumulationTimer(){
        Timer mDataAccumulateTimer=new Timer("accumulationTimer",true);
        ExecuteReset executeReset=new ExecuteReset();
        mDataAccumulateTimer.schedule(executeReset,3000,3000);
    }

    private void processReceivedData(final List<TaxiObject> newData){

        mDb.taxiDao().runPreOutputTransactions(mNewPositionsList, Constants.myId);
        //output received data
        final List<TaxiObject> baseArray=mDb.taxiDao().getMatchingTaxiBase();
        final List<TaxiObject> newArray=mDb.taxiDao().getNewTaxis();

        Log.d("taxis in base",baseArray.size()+"");
        Log.d("taxis in end",newArray.size()+"");

        mAnimationDataListener.onAnimationParametersReceived(baseArray,newArray);

        mDb.taxiDao().runPostOutputTransactions(new Date().getTime());
    }

    public interface AnimationDataListener{
        void onAnimationParametersReceived(List<TaxiObject> baseTaxis, List<TaxiObject> newTaxis);
    }

    AnimationDataListener mAnimationDataListener;

    public void setAnimationDataListener(AnimationDataListener listener){
        mAnimationDataListener=listener;
    }


}
