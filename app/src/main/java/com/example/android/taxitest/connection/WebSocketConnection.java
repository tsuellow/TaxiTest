package com.example.android.taxitest.connection;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import com.example.android.taxitest.CommunicationsRecyclerView.CommunicationsAdapter;
import com.example.android.taxitest.Constants;
import com.example.android.taxitest.MainActivity;
import com.example.android.taxitest.data.SqlLittleDB;
import com.example.android.taxitest.data.TaxiDao;
import com.example.android.taxitest.data.TaxiObject;
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

import static com.example.android.taxitest.utils.MiscellaneousUtils.locToGeo;


public class WebSocketConnection {

    private Socket mSocket;
    private Context mContext;
    public List<TaxiObject> mNewPositionsList=new ArrayList<TaxiObject>();
    CommunicationsAdapter commsInfo;
    boolean filterOn=false;


    SqlLittleDB mDb;


    Emitter.Listener onLocationUpdate;

    public WebSocketConnection(String url, Context context,CommunicationsAdapter communicationsAdapter){
        try {
            mSocket = IO.socket(url);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        mContext=context;
        mDb=SqlLittleDB.getInstance(context);
        commsInfo=communicationsAdapter;
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
            processReceivedData();
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

    private void processReceivedData(){

        mDb.taxiDao().runPreOutputTransactions(mNewPositionsList, Constants.myId);

        if (filterOn) {
            //this applies a filter to the data coming through the websocket and leaves only relevant
            // taxis in the view plus the ones with which communications are already underway
            SocketFilter sf = new SocketFilter(locToGeo(MainActivity.mMarkerLoc), MainActivity.destGeo, 45.0);
            mDb.taxiDao().applyDirectionalFilter(sf.bLeft, sf.mLeft, sf.bRight, sf.mRight, sf.signLeft, sf.signRight, commsInfo.getCommIds());
        }

        //output received data
        final List<TaxiObject> baseArray=mDb.taxiDao().getMatchingTaxiBase();
        final List<TaxiObject> newArray=mDb.taxiDao().getNewTaxis();

        Log.d("taxis in base",baseArray.size()+"");
        Log.d("taxis in new",newArray.size()+"");

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

    public class SocketFilter{
        double bRight;
        double mRight;
        double bLeft;
        double mLeft;
        int signRight;
        int signLeft;
        List<Integer> exceptions;

        public SocketFilter(GeoPoint geo, GeoPoint dest, double phi){
            double tinyCorrection=0.0;
            if (dest.getLongitude()-geo.getLongitude()==0.0)
                tinyCorrection=1E-11;

            double slope=(dest.getLatitude()-geo.getLatitude())/(dest.getLongitude()-geo.getLongitude()+tinyCorrection);
            double theta=Math.toDegrees(Math.atan(slope));
            Log.d("theta",""+theta);


            mLeft=Math.tan(Math.toRadians(theta+phi/2));
            mRight=Math.tan(Math.toRadians(theta-phi/2));
            Log.d("slopeR",""+mRight);
            Log.d("slopeL",""+mLeft);

            bLeft=geo.getLatitude()-mLeft*geo.getLongitude();
            bRight=geo.getLatitude()-mRight*geo.getLongitude();

            if (theta<90-phi/2 && theta>270+phi/2){
                signLeft= TaxiDao.BELOW;
                signRight=TaxiDao.ABOVE;
            }else if (theta>90-phi/2 && theta<90+phi/2){
                signLeft=TaxiDao.ABOVE;
                signRight=TaxiDao.ABOVE;
            }else if (theta>90+phi/2 && theta<270-phi/2){
                signLeft=TaxiDao.ABOVE;
                signRight=TaxiDao.BELOW;
            }else{
                signLeft=TaxiDao.BELOW;
                signRight=TaxiDao.BELOW;
            }
        }
    }

    public void setFilter(boolean set){
        filterOn=set;
    }


}
