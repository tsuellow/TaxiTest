package com.dale.viaje.nicaragua.connection;

import android.content.Context;
import android.util.Log;

import com.dale.viaje.nicaragua.CommunicationsRecyclerView.CommunicationsAdapter;
import com.dale.viaje.nicaragua.Constants;
import com.dale.viaje.nicaragua.CustomUtils;
import com.dale.viaje.nicaragua.UdpDataProcessor;
import com.dale.viaje.nicaragua.utils.MiscellaneousUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;

import static com.dale.viaje.nicaragua.MainActivity.myId;

public class IncomingUdpSocket {

    DatagramSocket udpSocket;
    String ip= Constants.LOC_IP;
    int port= CustomUtils.UDP_PORT;
    JSONObject beaconMsg;
    public UdpDataProcessor dataProcessor;

    boolean keepRunning=true;
    boolean triggerConnectionProcess =true;

    public IncomingUdpSocket(UdpDataProcessor processor) {
        dataProcessor=processor;
        initializeJson(MiscellaneousUtils.getNumericId(myId));
    }

    public void initializeJson(int taxiId) {
        beaconMsg=new JSONObject();
        try{
            beaconMsg.put("type",0);
            beaconMsg.put("taxiId",taxiId);
        }catch(JSONException e){
            e.printStackTrace();
        }
    }

    public void setKeepRunning(boolean keepRunning) {
        this.keepRunning = keepRunning;
    }

    public void setIpAndPort(String ip, int port){
        this.ip=ip;
        this.port=port;
    }

    Thread udpLoop=new Thread(new Runnable() {
        @Override
        public void run() {
            try {
                //setIpAndPort("34.207.241.98",44444);
                setIpAndPort(ip,port);

                //this variable does two things: it retries the initial connection process until it succeeds and it keeps the connection alive if to few msgs are received
                long timeLimit=System.currentTimeMillis();

                udpSocket = new DatagramSocket();
                //DatagramSocket udpTwo= new DatagramSocket();
                InetAddress serverIp=InetAddress.getByName(ip);
                udpSocket.connect(serverIp,port);
                byte[] buffer = new byte[2048];
                DatagramPacket receivedPacket = new DatagramPacket(buffer, buffer.length);

                byte[] beaconBuffer=beaconMsg.toString().getBytes();
                DatagramPacket beaconPacket=new DatagramPacket(beaconBuffer,0,beaconBuffer.length,serverIp,port);

                udpSocket.setSoTimeout(1000);

                while (keepRunning) {

                    if (System.currentTimeMillis()>=timeLimit){
                        if (triggerConnectionProcess){
                            timeLimit=System.currentTimeMillis()+999;//send beacon every second until connection is established
                        }else{
                            timeLimit=System.currentTimeMillis()+10000;//resend beacon whenever connection is inactive for 10 seconds or longer
                        }
                        udpSocket.send(beaconPacket);
                        Log.d("UDPreceived", "we sent this shit out already"+ip+port);
                    }

                    try{
                        udpSocket.receive(receivedPacket);
                        if (receivedPacket.getLength()>0){
                            String content = new String(buffer, 0, receivedPacket.getLength());
                            Log.d("UDPreceived", content);
                            dataProcessor.processContent(content);
                            timeLimit=System.currentTimeMillis()+10000;
                        }
                    }catch (SocketTimeoutException error){
                        error.printStackTrace();
                    }
                }
            } catch (Exception e) {
                System.err.println(e);
                Log.d("UDPreceived", "something failed");
                e.printStackTrace();
            }
        }
    });

    public void startServer(){
        udpLoop.start();
    }

    public void setTriggerConnectionProcess(boolean set){
        triggerConnectionProcess=set;
    }

//    public void processContent(String content){
//        //do csv shit
//        try{
//            TaxiNew taxiObject=r.readValue(content);
//            addOrReset(false,taxiObject);
//        }catch(Exception e){
//            e.printStackTrace();
//            Log.d("CSV","not reading");
//        }
//    }
//
    public  void doOnDisconnect(){
        dataProcessor.doOnDisconnect();
        keepRunning=false;
        if (udpSocket!=null){
            udpSocket.close();
        }
    }
//
//    //part inherited from old socket.io websocket
//
//    private List<TaxiNew> mNewPositionsList=new ArrayList<TaxiNew>();
//    protected CommunicationsAdapter commsInfo;
//    protected boolean filterOn=false;
//    boolean processIsRunning=false;
//    boolean isFirstTime=true;
//    SharedPreferences preferences;
//    double phi;
//    int limit;
//    public SqlLittleDB mDb;
//
//
//    public void setProcessIsRunning(boolean processIsRunning) {
//        this.processIsRunning = processIsRunning;
//    }
//
//    public boolean getProcessIsRunning() {
//        return processIsRunning;
//    }
//
//    public CsvMapper mapper=new CsvMapper();
//    public CsvSchema schema = mapper.schemaFor(TaxiNew.class).withColumnSeparator('|');
//    public ObjectReader r=mapper.readerFor(TaxiNew.class).with(schema);
//
//    public synchronized void addOrReset(boolean reset, TaxiNew taxiObject){
//        if (isFirstTime){
//            mDb.taxiDao().clearTaxiBase();
//            mDb.taxiDao().clearTaxiOld();
//            mDb.taxiDao().clearTaxiNew();
//            mDb.clientDao().clearTaxiBase();
//            mDb.clientDao().clearTaxiOld();
//            mDb.clientDao().clearTaxiNew();
//            isFirstTime=false;
//        }
//        setProcessIsRunning(true);
//        if (reset){
//            processReceivedData();
//            Log.d("timing",mNewPositionsList.size()+" arrived taxis");
//            mNewPositionsList.clear();
//        }else{
//            mNewPositionsList.add(taxiObject);
//        }
//    }
//
//    private class ExecuteReset extends TimerTask {
//        @Override
//        public void run() {
//            addOrReset(true,null);
//        }
//    };
//
//    Timer mDataAccumulateTimer=new Timer("accumulationTimer",true);
//    ExecuteReset executeReset=new ExecuteReset();
//
//    public void startAccumulationTimer(){
//
//        mDataAccumulateTimer.schedule(executeReset,100,3000);
//    }
//
//    public void processReceivedData(){
//        phi = (double) preferences.getFloat("filteramplitude", 45.0f);
//        limit = preferences.getInt("taxiamount",20);
//        mDb.taxiDao().runNewPreOutputTransactions(mNewPositionsList,filterOn,commsInfo.getCommIds(),phi,limit);
////        mDb.taxiDao().runPreOutputTransactions(mNewPositionsList, MiscellaneousUtils.getNumericId(MainActivity.myId));
//
////        if (filterOn) {
////            SocketFilter sf = new SocketFilter(locToGeo(MainActivity.mMarkerLoc), MainActivity.destGeo, 45.0);
////            mDb.taxiDao().applyDirectionalFilter(sf.bLeft, sf.mLeft, sf.bRight, sf.mRight, sf.signLeft, sf.signRight, commsInfo.getCommIds());
////        }
//
//        //output received data
//        final List<TaxiObject> baseArray=mDb.taxiDao().getMatchingTaxiBase();
//        final List<TaxiObject> newArray=mDb.taxiDao().getNewTaxis();
//
//        Log.d("timing",baseArray.size()+" base taxis");
//        Log.d("timing",newArray.size()+" new taxis");
//
//        mAnimationDataListener.onAnimationParametersReceived(baseArray,newArray);
//
//        mDb.taxiDao().runPostOutputTransactions(new Date().getTime());
//    }
//
//    public interface AnimationDataListener{
//        void onAnimationParametersReceived(List<? extends SocketObject> baseTaxis, List<? extends SocketObject> newTaxis);
//    }
//
//    protected AnimationDataListener mAnimationDataListener;
//
//    public void setAnimationDataListener(AnimationDataListener listener){
//        mAnimationDataListener=listener;
//    }
//
//    public void setFilter(boolean set){
//        filterOn=set;
//    }

}
