package com.example.android.taxitest;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.preference.PreferenceManager;

import com.example.android.taxitest.CommunicationsRecyclerView.CommunicationsAdapter;
import com.example.android.taxitest.connection.IncomingUdpSocket;
import com.example.android.taxitest.data.SocketObject;
import com.example.android.taxitest.data.SqlLittleDB;
import com.example.android.taxitest.data.TaxiNew;
import com.example.android.taxitest.data.TaxiObject;
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

    private List<TaxiNew> mNewPositionsList=new ArrayList<TaxiNew>();
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
            TaxiNew taxiObject=r.readValue(content);
            addOrReset(false,taxiObject);
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

    public CsvMapper mapper=new CsvMapper();
    public CsvSchema schema = mapper.schemaFor(TaxiNew.class).withColumnSeparator('|');
    public ObjectReader r=mapper.readerFor(TaxiNew.class).with(schema);

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
            mNewPositionsList.clear();
        }else{
            mNewPositionsList.add(taxiObject);
        }
    }

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
        phi = (double) preferences.getFloat("filteramplitude", 45.0f);
        limit = preferences.getInt("taxiamount",20);
        mDb.taxiDao().runNewPreOutputTransactions(mNewPositionsList,filterOn,commsInfo.getCommIds(),phi,limit);

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
