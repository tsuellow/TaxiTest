package com.dale.viaje.nicaragua.deprecatedClasses;

import android.content.Context;
import android.util.Log;

import com.dale.viaje.nicaragua.CommunicationsRecyclerView.CommunicationsAdapter;
import com.dale.viaje.nicaragua.data.ClientNew;
import com.dale.viaje.nicaragua.data.ClientObject;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.github.nkzawa.emitter.Emitter;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class WebSocketClientLocations extends WebSocketDriverLocations {

    public List<ClientNew> mNewPositionsList=new ArrayList<ClientNew>();

    public WebSocketClientLocations(String url, Context context, CommunicationsAdapter communicationsAdapter) {
        super(url, context, communicationsAdapter);
    }

    private CsvMapper mapper=new CsvMapper();
    private CsvSchema schema = mapper.schemaFor(ClientNew.class).withColumnSeparator('|');
    private ObjectReader r=mapper.readerFor(ClientNew.class).with(schema);

    @Override
    public void initializeSocketListener() {
        onLocationUpdate=new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                final String csvString=(String) args[0];
                Log.d("CSV",csvString);
                try{
                    ClientNew clientObject=r.readValue(csvString);
                    addOrReset(false,clientObject);
                }catch(Exception e){
                    e.printStackTrace();
                    Log.d("CSV","not reading");
                }
            }
        };
    }

    private class ExecuteReset extends TimerTask {
        @Override
        public void run() {
            addOrReset(true, (ClientNew) null);
        }
    };

    Timer mDataAccumulateTimer=new Timer("accumulationTimer",true);
    ExecuteReset executeReset=new ExecuteReset();




    @Override
    public void startAccumulationTimer() {
        mDataAccumulateTimer.schedule(executeReset,100,5000);
    }

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

    @Override
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
}
