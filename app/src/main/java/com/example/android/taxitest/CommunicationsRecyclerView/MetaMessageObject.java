package com.example.android.taxitest.CommunicationsRecyclerView;

import android.util.Log;

import com.example.android.taxitest.Constants;
import com.example.android.taxitest.MainActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MetaMessageObject{
    boolean isOutgoing;
    boolean wasPlayed=false;
    MessageObject msjObject;
    File audioFile;
    List<AcknowledgementObject> ackList=new ArrayList<>();
    CommsObject comm;

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
                audioFile = new File(comm.mContext.getExternalCacheDir(), "/" + msjObject.msgId + ".aac");
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
    }

    public MetaMessageObject(int intendCode, File audioFile, CommsObject comm) {
        isOutgoing=true;
        wasPlayed=true;
        this.comm=comm;
        this.msjObject = new MessageObject(MainActivity.myId,"t"+comm.taxiMarker.taxiObject.getTaxiId(),intendCode,audioFile,new Date().getTime());
        this.audioFile = audioFile;
    }

    public List<AcknowledgementObject> getAckList() {
        return ackList;
    }

    public void addAckAtTopOfList(AcknowledgementObject ack){
        ackList.add(0,ack);
        if (ack.getMsgId().equals(comm.mLatestMsjId) && isOutgoing) {
            comm.ackUpdateListener.onAckUpdateReceived(ack);
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


}
