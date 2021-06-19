package com.dale.viaje.nicaragua.CommunicationsRecyclerView;

import android.util.Log;

import com.dale.viaje.nicaragua.CustomUtils;
import com.dale.viaje.nicaragua.MainActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

public class AcknowledgementObject {
    String sendingId;
    String receivingId;
    String msgId;
    int ackCode;
    long timestamp;

    public AcknowledgementObject() {
    }

    public AcknowledgementObject(CommsObject comm, int ackCode, String msgId) {
        sendingId = MainActivity.myId;
        receivingId = CustomUtils.getOtherStringId(comm.taxiMarker.taxiObject.getTaxiId());
        this.ackCode = ackCode;
        this.msgId=msgId;
        timestamp=new Date().getTime();
    }

    public AcknowledgementObject(MessageObject msj, int ackCode) {
        this.sendingId = msj.getReceivingId();
        this.receivingId = msj.getSendingId();
        this.msgId = msj.getMsgId();
        this.ackCode = ackCode;
        this.timestamp = new Date().getTime();
        Log.d("msjissue", "AcknowledgementObject: "+msj.getSendingId()+msj.getReceivingId());
    }

    public static AcknowledgementObject readIntoAck(JSONObject jsonObject){
        AcknowledgementObject ack=new AcknowledgementObject();
        try {
            String sendingId = jsonObject.getString("sendingId");
            String receivingId = jsonObject.getString("receivingId");
            int ackCode=jsonObject.getInt("ackCode");
            long timestamp=jsonObject.getLong("timestamp");
            String msgId=jsonObject.getString("msgId");

            ack.setSendingId(sendingId);
            ack.setReceivingId(receivingId);
            ack.setAckCode(ackCode);
            ack.setMsgId(msgId);
            ack.setTimestamp(timestamp);
        }catch (JSONException e){
            e.printStackTrace();
            ack=null;
        }
        return ack;
    }

    public JSONObject generateJson(){
        try {
            JSONObject output = new JSONObject();
            output.put("receivingId", receivingId);
            output.put("sendingId", sendingId);
            output.put("ackCode", ackCode);
            output.put("timestamp", timestamp);
            output.put("msgId", msgId);
            return output;
        }catch (Exception e){
            e.printStackTrace();
            Log.d("error_sending","fuuck ack");
            return null;
        }
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

    public String getMsgId() {
        return msgId;
    }

    public void setMsgId(String msgId) {
        this.msgId = msgId;
    }

    public int getAckCode() {
        return ackCode;
    }

    public void setAckCode(int ackCode) {
        this.ackCode = ackCode;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
