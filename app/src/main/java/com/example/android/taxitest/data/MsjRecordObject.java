package com.example.android.taxitest.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import com.example.android.taxitest.CommunicationsRecyclerView.MetaMessageObject;


@Entity(tableName = "msjTable", foreignKeys = @ForeignKey(entity = CommRecordObject.class,
        parentColumns = "commId",
        childColumns = "commId",
        onDelete = ForeignKey.NO_ACTION),
        indices = {@Index(value = {"commId"})})
public class MsjRecordObject {

    @Ignore
    public static final int OK=1;
    @Ignore
    public static final int FAILED=0;

    private String commId;
    @PrimaryKey @NonNull
    private String msjId="0_0";
    private int isOutgoing;
    private int intentCode;
    private String filePath;
    private long timestamp;
    private long received;
    private long heard;
    private long played;
    private int msjStatus=OK;

    @Ignore
    public MsjRecordObject(MetaMessageObject metaMsj){
        commId=metaMsj.comm.commId;
        msjId=metaMsj.msjObject.getMsgId();
        isOutgoing=metaMsj.isOutgoing?1:0;
        intentCode=metaMsj.msjObject.getIntentCode();
        filePath=metaMsj.audioFile!=null?metaMsj.audioFile.getAbsolutePath():null;
        timestamp=metaMsj.msjObject.getTimestamp();
    }

    public MsjRecordObject() {
    }

    public String getCommId() {
        return commId;
    }

    public void setCommId(String commId) {
        this.commId = commId;
    }

    public String getMsjId() {
        return msjId;
    }

    public void setMsjId(String msjId) {
        this.msjId = msjId;
    }

    public int getIsOutgoing() {
        return isOutgoing;
    }

    public void setIsOutgoing(int isOutgoing) {
        this.isOutgoing = isOutgoing;
    }

    public int getIntentCode() {
        return intentCode;
    }

    public void setIntentCode(int intentCode) {
        this.intentCode = intentCode;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public long getReceived() {
        return received;
    }

    public void setReceived(long received) {
        this.received = received;
    }

    public long getHeard() {
        return heard;
    }

    public void setHeard(long heard) {
        this.heard = heard;
    }

    public long getPlayed() {
        return played;
    }

    public void setPlayed(long played) {
        this.played = played;
    }

    public int getMsjStatus() {
        return msjStatus;
    }

    public void setMsjStatus(int msjStatus) {
        this.msjStatus = msjStatus;
    }
}
