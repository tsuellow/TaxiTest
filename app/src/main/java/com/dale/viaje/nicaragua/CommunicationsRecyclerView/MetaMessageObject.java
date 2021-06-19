package com.dale.viaje.nicaragua.CommunicationsRecyclerView;

import android.util.Log;

import com.dale.viaje.nicaragua.AppExecutors;
import com.dale.viaje.nicaragua.CustomUtils;
import com.dale.viaje.nicaragua.MainActivity;
import com.dale.viaje.nicaragua.data.MsjRecordObject;
import com.dale.viaje.nicaragua.utils.MiscellaneousUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MetaMessageObject{
    public boolean isOutgoing;
    public boolean wasPlayed=false;
    public MessageObject msjObject;
    public File audioFile;
    public List<AcknowledgementObject> ackList=new ArrayList<>();
    public CommsObject comm;

    public boolean isWasPlayed() {
        return wasPlayed;
    }

    public void setWasPlayed(boolean wasPlayed) {
        this.wasPlayed = wasPlayed;
    }



    public MessageObject getMsjObject() {
        return msjObject;
    }

    public void setMsjObject(MessageObject msjObject) {
        this.msjObject = msjObject;
    }

    public File getAudioFile() {
        return audioFile;
    }

    public void setAudioFile(File audioFile) {
        this.audioFile = audioFile;
    }

    public MetaMessageObject(MessageObject msjObject, CommsObject comm) {
        isOutgoing=false;
        this.comm=comm;
        this.msjObject = msjObject;
        Log.d("socketTest",msjObject.getMsgId()+"__2");
        if (msjObject.getAudioBytes()!=null) {
            try {
                audioFile = MiscellaneousUtils.makeAudioFile(comm.mContext,comm.commId,msjObject.sendingId);
                Log.d("socketTest",audioFile.getAbsolutePath());
                FileOutputStream fos = new FileOutputStream(audioFile);
                fos.write(msjObject.getAudioBytes());
                wasPlayed=false;
            } catch (Exception e) {
                audioFile = null;
                wasPlayed=true;
                e.printStackTrace();
            }
        }else{
            audioFile = null;
            wasPlayed=true;
        }
        archiveMsj();
    }

    public MetaMessageObject(int intendCode, File audioFile, CommsObject comm) {
        isOutgoing=true;
        wasPlayed=true;
        this.comm=comm;
        this.msjObject = new MessageObject(MainActivity.myId, CustomUtils.getOtherStringId(comm.taxiMarker.taxiObject.getTaxiId()),intendCode,audioFile,new Date().getTime());
        this.audioFile = audioFile;
        archiveMsj();
    }

    public List<AcknowledgementObject> getAckList() {
        return ackList;
    }

    public void addAckAtTopOfList(AcknowledgementObject ack){
        ackList.add(0,ack);
        updateMsj(ack);//archive ack
        if (ack.getMsgId().equals(comm.mLatestMsjId) && isOutgoing) {
            comm.ackUpdateListener.onAckUpdateReceived(ack);
        }
        //confirm that communication is enabeled
        if (!comm.isCommEngaged()) {
            if (ack.getAckCode() != CommsObject.SENT && ack.getAckCode() != CommsObject.FAILED) {
                comm.setCommEngaged(true);
            }
        }
    }

    public AcknowledgementObject getAckAtTopOfList(){
        return ackList.get(0);
    }

    public int getTopAck(){
        int topAck=CommsObject.SENT;
        for (AcknowledgementObject ack:ackList){
            if (ack.getAckCode()>0 && ack.getAckCode()>topAck){
                topAck=ack.getAckCode();
            }
        }
        return topAck;
    }

    public void archiveMsj(){
        Log.d("msjRec", "archiveMsj: success");
        MsjRecordObject msjRec=new MsjRecordObject(this);
        AppExecutors.getInstance().diskIO().execute(new Runnable() {
            @Override
            public void run() {
                CommsObject.mDb.commsDao().insertMsj(msjRec);
            }
        });
    }

    public void updateMsj(AcknowledgementObject ack){
        switch (ack.getAckCode()){
            case CommsObject.RECEIVED:
               updateReceived(ack);
               break;
            case CommsObject.PLAYED:
                updatePlayed(ack);
                break;
            case CommsObject.HEARD:
                updateHeard(ack);
                break;
            case CommsObject.FAILED:
                updateMsjStatus(ack,MsjRecordObject.FAILED);
                break;
        }
        Log.d("msjRec", "archiveMsj: "+ack.getAckCode());
    }



    private void updateReceived(AcknowledgementObject ack){
        AppExecutors.getInstance().diskIO().execute(new Runnable() {
            @Override
            public void run() {
                CommsObject.mDb.commsDao().updateReceived(ack.msgId,ack.timestamp);
            }
        });
    }
    private void updatePlayed(AcknowledgementObject ack){
        AppExecutors.getInstance().diskIO().execute(new Runnable() {
            @Override
            public void run() {
                CommsObject.mDb.commsDao().updatePlayed(ack.msgId,ack.timestamp);
            }
        });
    }
    private void updateHeard(AcknowledgementObject ack){
        AppExecutors.getInstance().diskIO().execute(new Runnable() {
            @Override
            public void run() {
                CommsObject.mDb.commsDao().updateHeard(ack.msgId,ack.timestamp);
            }
        });
    }
    private void updateMsjStatus(AcknowledgementObject ack, int status){
        AppExecutors.getInstance().diskIO().execute(new Runnable() {
            @Override
            public void run() {
                CommsObject.mDb.commsDao().updateMsjStatus(ack.msgId,status);
            }
        });
    }
}
