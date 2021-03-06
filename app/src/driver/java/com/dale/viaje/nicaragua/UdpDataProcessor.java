package com.dale.viaje.nicaragua;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.preference.PreferenceManager;

import com.dale.viaje.nicaragua.CommunicationsRecyclerView.CommunicationsAdapter;
import com.dale.viaje.nicaragua.data.ClientNew;
import com.dale.viaje.nicaragua.data.ClientObject;
import com.dale.viaje.nicaragua.data.SocketObject;
import com.dale.viaje.nicaragua.data.SqlLittleDB;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class UdpDataProcessor {
    //this class processes data incoming as clientObjects

    //part inherited from old socket.io websocket


    public UdpDataProcessor(CommunicationsAdapter communicationsAdapter, Context context) {
        mDb=SqlLittleDB.getInstance(context);
        this.preferences= PreferenceManager.getDefaultSharedPreferences(context);
        this.commsInfo = communicationsAdapter;
    }

    public List<ClientNew> mNewPositionsList=new ArrayList<ClientNew>();
    protected CommunicationsAdapter commsInfo;
    protected boolean filterOn=false;
    boolean processIsRunning=false;
    boolean isFirstTime=true;
    SharedPreferences preferences;
    double phi;
    int limit;
    public SqlLittleDB mDb;

    public void processContent(String content){
        //do csv shit
        try{
            ClientNew clientObject=r.readValue(content);
            addOrReset(false,clientObject);
        }catch(Exception e){
            e.printStackTrace();
            Log.d("CSV","not reading");
        }
    }

    public  void doOnDisconnect(){
        mDataAccumulateTimer.cancel();
    }


    public void setProcessIsRunning(boolean processIsRunning) {
        this.processIsRunning = processIsRunning;
    }

    public boolean getProcessIsRunning() {
        return processIsRunning;
    }

    private CsvMapper mapper=new CsvMapper();
    private CsvSchema schema = mapper.schemaFor(ClientNew.class).withColumnSeparator('|');
    private ObjectReader r=mapper.readerFor(ClientNew.class).with(schema);

    public synchronized void addOrReset(boolean reset, ClientNew clientObject) {
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
            mNewPositionsList.clear();
        }else{
            mNewPositionsList.add(clientObject);
        }
    }

    private class ExecuteReset extends TimerTask {
        @Override
        public void run() {
            addOrReset(true, (ClientNew) null);
        }
    };

    Timer mDataAccumulateTimer=new Timer("accumulationTimer",true);
    ExecuteReset executeReset=new ExecuteReset();

    public void startAccumulationTimer(){
        mDataAccumulateTimer.schedule(executeReset,100,3000);
    }

    public void processReceivedData() {
        phi = (double) preferences.getFloat("filteramplitude", 45.0f);
        limit = preferences.getInt("taxiamount",20);
        mDb.clientDao().runNewPreOutputTransactions(mNewPositionsList,filterOn,commsInfo.getCommIds(),phi,limit);

        //output received data
        final List<ClientObject> baseArray=mDb.clientDao().getMatchingTaxiBase();
        final List<ClientObject> newArray=mDb.clientDao().getNewTaxis();

        Log.d("clients in base",baseArray.size()+"");
        Log.d("clients in new",newArray.size()+"");

        mAnimationDataListener.onAnimationParametersReceived(baseArray,newArray);

        mDb.clientDao().runPostOutputTransactions(new Date().getTime());
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
