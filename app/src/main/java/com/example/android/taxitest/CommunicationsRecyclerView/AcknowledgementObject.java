package com.example.android.taxitest.CommunicationsRecyclerView;

public class AcknowledgementObject {
    String sendingId;
    String receivingId;
    String msgId;
    int ackCode;
    long timestamp;

    public AcknowledgementObject(String sendingId, String receivingId, String msgId, int ackCode, long timestamp) {
        this.sendingId = sendingId;
        this.receivingId = receivingId;
        this.msgId = msgId;
        this.ackCode = ackCode;
        this.timestamp = timestamp;
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
