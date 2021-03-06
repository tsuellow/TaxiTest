package com.dale.viaje.nicaragua.CommunicationsRecyclerView;

import android.util.Log;

import androidx.room.Ignore;

import com.dale.viaje.nicaragua.MainActivity;
import com.google.android.gms.common.util.IOUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Date;

public class MessageObject {
    String sendingId;
    String receivingId;
    int intentCode;
    byte[] audioBytes;
    long timestamp;
    String msgId;

    public MessageObject() {
    }

    public MessageObject(String receivingId, int intentCode) {
        this.sendingId = MainActivity.myId;
        this.receivingId = receivingId;
        this.intentCode = intentCode;
        this.timestamp = new Date().getTime();
        this.msgId=sendingId+"_"+timestamp;
    }

    @Ignore
    public MessageObject(String sendingId, String receivingId, int intentCode, File audioFile, long timestamp) {
        this.sendingId = sendingId;
        this.receivingId = receivingId;
        this.intentCode = intentCode;
        this.timestamp = timestamp;
        this.msgId=sendingId+"_"+timestamp;

        if (audioFile!=null){
            try {
                InputStream inputStream = new FileInputStream(audioFile.getAbsolutePath());
                audioBytes = IOUtils.toByteArray(inputStream);
//                RandomAccessFile f = new RandomAccessFile(audioFile, "r");
//                audioBytes = new byte[(int) f.length()];
//                f.readFully(audioBytes);
            }catch (Exception e){
                audioBytes=null;
            }
        }else{
            audioBytes=null;
        }
    }

    @Ignore
    public MessageObject(JSONObject jsonObject){
        try {
            sendingId = jsonObject.getString("sendingId");
            receivingId = jsonObject.getString("receivingId");
            intentCode=jsonObject.getInt("intentCode");
            audioBytes=(byte[])jsonObject.get("audioBytes");
            timestamp=jsonObject.getLong("timestamp");
            msgId=jsonObject.getString("msgId");
        }catch (JSONException e){
            e.printStackTrace();
        }
    }
    public static MessageObject readIntoMsj(JSONObject jsonObject){
        MessageObject msj=new MessageObject();
        try {
            String sendingId = jsonObject.getString("sendingId");
            String receivingId = jsonObject.getString("receivingId");
            int intentCode=jsonObject.getInt("intentCode");
            byte[] audioBytes;
            if (jsonObject.get("audioBytes")==JSONObject.NULL){
                audioBytes=null;
            }else{
                audioBytes=(byte[]) jsonObject.get("audioBytes");
            }
            long timestamp=jsonObject.getLong("timestamp");
            String msgId=jsonObject.getString("msgId");

            msj.setSendingId(sendingId);
            msj.setReceivingId(receivingId);
            msj.setIntentCode(intentCode);
            msj.setAudioBytes(audioBytes);
            msj.setMsgId(msgId);
            msj.setTimestamp(timestamp);
        }catch (JSONException e){
            e.printStackTrace();
        }
        return msj;
    }

    public String getSendingId() {
        return sendingId;
    }

    public void setSendingId(String sendingId) {
        this.sendingId = sendingId;
    }

    public String getReceivingId() {
        return receivingId;
    }

    public void setReceivingId(String receivingId) {
        this.receivingId = receivingId;
    }

    public int getIntentCode() {
        return intentCode;
    }

    public void setIntentCode(int intentCode) {
        this.intentCode = intentCode;
    }

    public byte[] getAudioBytes() {
        return audioBytes;
    }

    public void setAudioBytes(byte[] audioBytes) {
        this.audioBytes = audioBytes;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getMsgId() {
        return msgId;
    }

    public void setMsgId(String msgId) {
        this.msgId = msgId;
    }

    public JSONObject generateJson(){
        try {
            JSONObject output = new JSONObject();
            output.put("receivingId", receivingId);
            output.put("sendingId", sendingId);
            output.put("intentCode", intentCode);
            if (audioBytes!=null) {
                output.put("audioBytes", audioBytes);
            }else{
                output.put("audioBytes", JSONObject.NULL);
            }
            output.put("timestamp", timestamp);
            output.put("msgId", msgId);
            return output;
        }catch (Exception e){
            e.printStackTrace();
            Log.d("error_sending","fuuck");
            return null;
        }
    }
}
