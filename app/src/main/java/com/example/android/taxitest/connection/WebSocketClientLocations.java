package com.example.android.taxitest.connection;

import android.content.Context;
import android.util.Log;

import com.example.android.taxitest.CommunicationsRecyclerView.CommunicationsAdapter;
import com.example.android.taxitest.MainActivity;
import com.example.android.taxitest.data.ClientObject;
import com.example.android.taxitest.utils.MiscellaneousUtils;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.github.nkzawa.emitter.Emitter;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static com.example.android.taxitest.utils.MiscellaneousUtils.locToGeo;

public class WebSocketClientLocations extends WebSocketDriverLocations {

    public List<ClientObject> mNewPositionsList=new ArrayList<ClientObject>();

    public WebSocketClientLocations(String url, Context context, CommunicationsAdapter communicationsAdapter) {
        super(url, context, communicationsAdapter);
    }

    @Override
    public void initializeSocketListener() {
        onLocationUpdate=new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                final String csvString=(String) args[0];
                try{
                    ClientObject clientObject=r.readValue(csvString);
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
            addOrReset(true, (ClientObject) null);
        }
    };

    Timer mDataAccumulateTimer=new Timer("accumulationTimer",true);
    ExecuteReset executeReset=new ExecuteReset();


    private CsvMapper mapper=new CsvMapper();
    private CsvSchema schema = mapper.schemaFor(ClientObject.class).withColumnSeparator('|');
    private ObjectReader r=mapper.readerFor(ClientObject.class).with(schema);

    @Override
    public void startAccumulationTimer() {
        mDataAccumulateTimer.schedule(executeReset,100,5000);
    }

    public synchronized void addOrReset(boolean reset, ClientObject clientObject) {
        setProcessIsRunning(true);
        if (reset){
            processReceivedData();
            mNewPositionsList.clear();
        }else{
            mNewPositionsList.add(clientObject);
        }
    }

    @Override
    public void processReceivedData() {
        mDb.clientDao().runPreOutputTransactions(mNewPositionsList, MiscellaneousUtils.getNumericId(MainActivity.myId));

        if (filterOn) {
            //this applies a filter to the data coming through the websocket and leaves only relevant
            // taxis in the view plus the ones with which communications are already underway
            SocketFilter sf = new SocketFilter(locToGeo(MainActivity.mMarkerLoc), MainActivity.destGeo, 45.0);
            mDb.taxiDao().applyDirectionalFilter(sf.bLeft, sf.mLeft, sf.bRight, sf.mRight, sf.signLeft, sf.signRight, commsInfo.getCommIds());
        }

        //output received data
        final List<ClientObject> baseArray=mDb.clientDao().getMatchingTaxiBase();
        final List<ClientObject> newArray=mDb.clientDao().getNewTaxis();

        Log.d("clients in base",baseArray.size()+"");
        Log.d("clients in new",newArray.size()+"");

        mAnimationDataListener.onAnimationParametersReceived(baseArray,newArray);

        mDb.clientDao().runPostOutputTransactions(new Date().getTime());
    }
}
