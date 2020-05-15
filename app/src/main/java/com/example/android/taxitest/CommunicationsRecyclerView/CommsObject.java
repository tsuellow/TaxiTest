package com.example.android.taxitest.CommunicationsRecyclerView;

import android.content.Context;
import android.util.Log;
import android.widget.Filter;

import com.example.android.taxitest.Constants;
import com.example.android.taxitest.vtmExtension.TaxiMarker;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class CommsObject {
    Context mContext;
    String mLatestMsjId;
    int mAckStatus=SENT;
    int mMsjStatus=OBSERVING;
    public TaxiMarker taxiMarker;
    private List<MetaMessageObject> msjList=new ArrayList<>();

    //driver info driverObject from room DB
    //list of individual communications

    public CommsObject(TaxiMarker taxiMarker, Context context){
        this.taxiMarker=taxiMarker;
        mContext=context;
    }

    //list of intent codes
    static final int OBSERVING=0;
    static final int REQUEST_SENT=1;
    static final int REQUEST_RECEIVED=2;
    static final int ACCEPTED=3;
    static final int REJECTED=4;

    //list of message statuses
    static final int SENT=0;
    static final int RECEIVED=1;
    static final int PLAYED=2;
    static final int HEARD=3;
    static final int FAILED=4;
    static final int RECORDING_STARTED=5;
    static final int RECORDING_STOPPED=6;


    //legacy code
    private List<File> audioList=new ArrayList<>();

    public void addAtTopOfList(File file){
        audioList.add(0,file);
    }

    public File getTopOfList(){
        return audioList.get(0);
    }
    //legacy

    public void performUpdates(){
        if (msjUpdateListener!=null) {
            msjUpdateListener.onMsjUpdateReceived(mMsjStatus);
        }
    }

    public void addAtTopOfMsjList(MetaMessageObject msj){
        msjList.add(0,msj);
        Log.d("socketTest","new "+msjList.size());
        mMsjStatus=msj.msjObject.getIntentCode();
        //logic to deal with the fact that request is sent for sender but received for receiver
        if (mMsjStatus==REQUEST_SENT && !msj.isOutgoing){
            mMsjStatus=REQUEST_RECEIVED;
        }
        Log.d("socketTest","code "+mMsjStatus);
        //do something and send it in place of mMsjStatus
        if (msjUpdateListener!=null) {
            msjUpdateListener.onMsjUpdateReceived(mMsjStatus);
        }
    }

    public MetaMessageObject getNextUnplayedMsj(){
        for (int i=msjList.size()-1;i>=0;i--){
            MetaMessageObject msj=msjList.get(i);
            if(!msj.isWasPlayed() && msj.getAudioFile()!=null && !msj.isOutgoing){
                return msjList.get(i);
            }
        }
        return msjList.get(0);
    }

    public static final int INVITE=0;
    public static final int AWAITING=1;
    public static final int PLAY=2;
    public static final int ACCEPT=3;
    public static final int BT_ACCEPTED=4;

    public int getButtonCode(){
        if (mMsjStatus==OBSERVING)
            return INVITE;
        if (mMsjStatus==REQUEST_SENT)
            return AWAITING;
        if (mMsjStatus==REQUEST_RECEIVED && msjList.get(0).getAudioFile()!=null && !msjList.get(0).isWasPlayed())
            return PLAY;
        if (mMsjStatus==REQUEST_RECEIVED)
            return ACCEPT;
        if (mMsjStatus==ACCEPTED)
            return BT_ACCEPTED;

        return INVITE;
    }

    public MetaMessageObject findMsjById(String msjId){
        for (MetaMessageObject msj:msjList){
            if (msjId.equals(msj.msjObject.getMsgId())){
                return msj;
            }
        }
        return null;
    }


    //callback for when a new acknowledgement has arrived
    public interface AckUpdateListener{
        void onAckUpdateReceived(int newAckStatus);
    }

    AckUpdateListener ackUpdateListener;

    public void setAckUpdateListener(AckUpdateListener ackUpdateListener) {
        this.ackUpdateListener = ackUpdateListener;
    }


    //callback for when a new message has arrived
    public interface MsjUpdateListener{
        void onMsjUpdateReceived(int intentCode);
    }

    MsjUpdateListener msjUpdateListener;

    public void setMsjUpdateListener(MsjUpdateListener msjUpdateListener) {
        this.msjUpdateListener = msjUpdateListener;
    }






}
