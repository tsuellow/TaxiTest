package com.example.android.taxitest.connection;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import com.example.android.taxitest.AppExecutors;
import com.example.android.taxitest.CommunicationsRecyclerView.CommunicationsAdapter;
import com.example.android.taxitest.Constants;
import com.example.android.taxitest.MainActivity;
import com.example.android.taxitest.data.SocketObject;
import com.example.android.taxitest.data.SqlLittleDB;
import com.example.android.taxitest.data.TaxiDao;
import com.example.android.taxitest.data.TaxiNew;
import com.example.android.taxitest.data.TaxiObject;
import com.example.android.taxitest.utils.MiscellaneousUtils;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;
import org.json.JSONObject;
import org.oscim.core.GeoPoint;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static android.content.ContentValues.TAG;
import static com.example.android.taxitest.utils.MiscellaneousUtils.locToGeo;


public class WebSocketDriverLocations {

    private static final String TAG = "WebSocketDriverLocation";

    public Socket mSocket;
    private List<TaxiNew> mNewPositionsList=new ArrayList<TaxiNew>();
    protected CommunicationsAdapter commsInfo;
    protected boolean filterOn=false;
    boolean processIsRunning=false;
    boolean isFirstTime=true;

    public SqlLittleDB mDb;


    public Emitter.Listener onLocationUpdate;

    public WebSocketDriverLocations(String url, Context context, CommunicationsAdapter communicationsAdapter){
        try {
            mSocket = IO.socket(url);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        mDb=SqlLittleDB.getInstance(context);
        commsInfo=communicationsAdapter;
    }

    public void connectSocket(){
        mSocket.on("location update",onLocationUpdate);
        mSocket.connect();
    }

    public void disconnectSocket(){
        mDataAccumulateTimer.cancel();
        mSocket.off("location update",onLocationUpdate);
        mSocket.disconnect();
    }

    public void setProcessIsRunning(boolean processIsRunning) {
        this.processIsRunning = processIsRunning;
    }

    public boolean getProcessIsRunning() {
        return processIsRunning;
    }

    private CsvMapper mapper=new CsvMapper();
    private CsvSchema schema = mapper.schemaFor(TaxiNew.class).withColumnSeparator('|');
    private ObjectReader r=mapper.readerFor(TaxiNew.class).with(schema);

    int msjs=0;
    public void initializeSocketListener(){  //add target object itemizedlayer
        onLocationUpdate=new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                final String csvString=(String) args[0];
                msjs++;
                try{
                    TaxiNew taxiObject=r.readValue(csvString);
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
        Log.d(TAG, "attemptSend: worked"+mSocket.toString());

    }

    public synchronized void addOrReset(boolean reset, TaxiNew taxiObject){
        if (isFirstTime){
            mDb.taxiDao().clearTaxiBase();
            mDb.taxiDao().clearTaxiOld();
            mDb.taxiDao().clearTaxiNew();
            mDb.clientDao().clearTaxiBase();
            mDb.clientDao().clearTaxiOld();
            mDb.clientDao().clearTaxiNew();
            isFirstTime=false;
        }
        setProcessIsRunning(true);
        if (reset){
            processReceivedData();
            Log.d("timing",mNewPositionsList.size()+" arrived taxis");
            Log.d("timing",msjs+" arrived msjs");
            msjs=0;
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

    Timer mDataAccumulateTimer=new Timer("accumulationTimer",true);
    ExecuteReset executeReset=new ExecuteReset();

    public void startAccumulationTimer(){

        mDataAccumulateTimer.schedule(executeReset,100,3000);
    }

    public void processReceivedData(){
        mDb.taxiDao().runNewPreOutputTransactions(mNewPositionsList,filterOn,commsInfo.getCommIds());
//        mDb.taxiDao().runPreOutputTransactions(mNewPositionsList, MiscellaneousUtils.getNumericId(MainActivity.myId));

//        if (filterOn) {
//            SocketFilter sf = new SocketFilter(locToGeo(MainActivity.mMarkerLoc), MainActivity.destGeo, 45.0);
//            mDb.taxiDao().applyDirectionalFilter(sf.bLeft, sf.mLeft, sf.bRight, sf.mRight, sf.signLeft, sf.signRight, commsInfo.getCommIds());
//        }

        //output received data
        final List<TaxiObject> baseArray=mDb.taxiDao().getMatchingTaxiBase();
        final List<TaxiObject> newArray=mDb.taxiDao().getNewTaxis();

        Log.d("timing",baseArray.size()+" base taxis");
        Log.d("timing",newArray.size()+" new taxis");

        mAnimationDataListener.onAnimationParametersReceived(baseArray,newArray);

        mDb.taxiDao().runPostOutputTransactions(new Date().getTime());
    }

    public interface AnimationDataListener{
        void onAnimationParametersReceived(List<? extends SocketObject> baseTaxis, List<? extends SocketObject> newTaxis);
    }

    protected AnimationDataListener mAnimationDataListener;

    public void setAnimationDataListener(AnimationDataListener listener){
        mAnimationDataListener=listener;
    }

    public void setFilter(boolean set){
        filterOn=set;
    }


}
