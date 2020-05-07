package com.example.android.taxitest.CommunicationsRecyclerView;

import android.util.Log;

import com.example.android.taxitest.Constants;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MetaMessageObject{
    boolean isOutgoing;
    MessageObject msjObject;
    File audioFile;
    List<AcknowledgementObject> ackList=new ArrayList<>();
    CommsObject comm;

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
            } catch (Exception e) {
                audioFile = null;
                e.printStackTrace();
            }
        }
    }

    public MetaMessageObject(int intendCode, File audioFile, CommsObject comm) {
        isOutgoing=true;
        this.comm=comm;
        this.msjObject = new MessageObject("t"+ Constants.myId,"t"+comm.taxiMarker.taxiObject.getTaxiId(),intendCode,audioFile,new Date().getTime());
        this.audioFile = audioFile;
    }

    public void addAckAtTopOfList(AcknowledgementObject ack){
        ackList.add(0,ack);
        if (ack.getMsgId().equals(comm.mLatestMsjId)) {
            comm.ackUpdateListener.onAckUpdateReceived(ack.getAckCode());
        }
    }

    public AcknowledgementObject getAckAtTopOfList(){
        return ackList.get(0);
    }


}
