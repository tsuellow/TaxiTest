package com.example.android.taxitest.CommunicationsRecyclerView;

import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.Filter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.example.android.taxitest.Constants;
import com.example.android.taxitest.MainActivity;
import com.example.android.taxitest.R;
import com.example.android.taxitest.connection.MySingleton;
import com.example.android.taxitest.utils.MiscellaneousUtils;
import com.example.android.taxitest.vtmExtension.TaxiMarker;
import com.mikhaellopez.circularprogressbar.CircularProgressBar;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

public class CommsObject {

    //list of intent codes
    public static final int OBSERVING=0;
    public static final int REQUEST_SENT=1;
    public static final int REQUEST_RECEIVED=2;
    public static final int ACCEPTED=3;
    public static final int REJECTED=4;

    //list of button statuses
    public static final int INVITE=0;
    public static final int AWAITING=1;
    public static final int PLAY=2;
    public static final int ACCEPT=3;
    public static final int BT_ACCEPTED=4;

    //list of ack statuses
    public static final int SENT=0;
    public static final int RECEIVED=1;
    public static final int PLAYED=2;
    public static final int HEARD=3;
    public static final int FAILED=4;
    public static final int RECORDING_STARTED=-1;
    public static final int RECORDING_STOPPED=-2;

    Context mContext;
    String mLatestMsjId;
    int mMsjStatus=OBSERVING;
    public TaxiMarker taxiMarker;
    private List<MetaMessageObject> msjList=new ArrayList<>();
    boolean accepted=false;

    public List<MetaMessageObject> getMsjList() {
        return msjList;
    }

    //driver info driverObject from room DB
    String firstName;
    String lastName;
    String nrPlate;
    double reputation;
    Date dob;
    String gender;
    Bitmap photo;

    //list of individual communications

    public CommsObject(TaxiMarker taxiMarker, Context context) {
        this.taxiMarker=taxiMarker;
        mContext=context;
        sendDataRequest();
    }

    public MetaMessageObject getTopOfList(){
        return msjList.get(0);
    }


    public void performUpdates(){
        if (msjUpdateListener!=null) {
            msjUpdateListener.onMsjUpdateReceived(mMsjStatus);
        }
    }

    public void addAtTopOfMsjList(MetaMessageObject msj){
        msjList.add(0,msj);
        mLatestMsjId=msj.getMsjObject().getMsgId();
        mMsjStatus=msj.msjObject.getIntentCode();
        //logic to deal with the fact that request is sent for sender but received for receiver
        if (mMsjStatus==REQUEST_SENT && !msj.isOutgoing){
            mMsjStatus=REQUEST_RECEIVED;
        }
        //play sound on incoming msgs
        if (!msj.isOutgoing && msj.getMsjObject().getIntentCode()!=REJECTED){
            CommunicationsAdapter.soundPool.play(CommunicationsAdapter.soundMsjArrived,1,1,0,0,1);
            if (!MainActivity.getIsActivityInForeground())
            MiscellaneousUtils.showNotification(mContext,"You have been contacted!", "A client has sent you a message on TaxiTest recently");
        }

        if (msjUpdateListener!=null) {
            Log.d("socketTest","msjStatus:"+mMsjStatus);
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
        return null;
    }



    public int getButtonCode(){
        if (accepted){
            return BT_ACCEPTED;
        }else if (mMsjStatus==REQUEST_RECEIVED && msjList.get(0).getAudioFile()!=null && !msjList.get(0).isWasPlayed()){
            return PLAY;
        }else if (mMsjStatus==REQUEST_RECEIVED){
            return ACCEPT;
        }else if (mMsjStatus==REQUEST_SENT){
            return AWAITING;
        }else{
            return INVITE;
        }
    }

    public MetaMessageObject findMsjById(String msjId){
        for (MetaMessageObject msj:msjList){
            if (msjId.equals(msj.msjObject.getMsgId())){
                return msj;
            }
        }
        return null;
    }

    public boolean isAccepted() {
        return accepted;
    }

    public void setAccepted(boolean accepted) {
        this.accepted = accepted;
    }


    //callback for when a new acknowledgement has arrived
    public interface AckUpdateListener{
        void onAckUpdateReceived(AcknowledgementObject ack);
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


    //CommsObject media player
    private MediaPlayer player = null;
    boolean isPlaying=false;
    public void startPlaying(final MetaMessageObject msj, final CircularProgressBar circularProgressBar) {
        if (isPlaying){
            player.stop();
            circularProgressBar.setProgress(0.0f);
            circularProgressBar.setVisibility(View.INVISIBLE);
        }
        //callback preparation
        final int taxiId=msj.comm.taxiMarker.taxiObject.getTaxiId();

        player = new MediaPlayer();
        player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                    stopPlaying();
                    CommunicationsAdapter.resetMediaProgressBar(circularProgressBar);
                    msj.setWasPlayed(true);
                    msj.comm.performUpdates();
                    //acknowledge that you listened to the end
                    AcknowledgementObject ack=new AcknowledgementObject(msj.getMsjObject(), HEARD);
                    CommunicationsAdapter.attemptSendAck(ack);
                    msj.addAckAtTopOfList(ack);

                    //keep playing msgs if there is more than one audio file to be played
                    if (getNextUnplayedMsj() != null) {
                        startPlaying(getNextUnplayedMsj(), circularProgressBar);
                    }
            }
        });
        try {
            player.setDataSource(msj.getAudioFile().getAbsolutePath());
            player.prepare();
            circularProgressBar.setVisibility(View.VISIBLE);
            player.start();
            circularProgressBar.setProgressWithAnimation(100.0f,(long) player.getDuration(),new LinearInterpolator());
            isPlaying=true;
        } catch (IOException e) {
            isPlaying=false;
        }

    }

    public void stopPlaying() {
        isPlaying = false;
        player.stop();
        player.release();
        player = null;
    }

    //Comms Object dialog inflater
    public void showCommsDialog(Context context){
        final Dialog dialog=new Dialog(context);
        dialog.setContentView(R.layout.comm_dialog);
        TextView title=dialog.findViewById(R.id.tv_title_dialog);
        TextView noMsgs=dialog.findViewById(R.id.tv_no_msgs);
        title.setText("Chat with "+taxiMarker.taxiObject.getTaxiId());
        if (msjList.size()==0){
            noMsgs.setVisibility(View.VISIBLE);
        }else{
            noMsgs.setVisibility(View.GONE);
        }
        RecyclerView commsRV=(RecyclerView) dialog.findViewById(R.id.rv_comms_dialog);
        Button closeBtn=(Button) dialog.findViewById(R.id.bt_dialog_close);
        CommsDialogAdapter adapter=new CommsDialogAdapter(context,this);
        commsRV.setAdapter(adapter);
        LinearLayoutManager layoutManager = new LinearLayoutManager(dialog.getContext());
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        commsRV.setLayoutManager(layoutManager);
//        DividerItemDecoration deco=new DividerItemDecoration(commsRV.getContext(), layoutManager.getOrientation());
//        deco.setDrawable(Objects.requireNonNull(ContextCompat.getDrawable(context, R.drawable.divider)));
//        commsRV.addItemDecoration(deco);
        closeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });
        dialog.show();
    }

    public void sendDataRequest(){
        try {
            JSONObject json=new JSONObject();
            json.put("taxiId" ,taxiMarker.taxiObject.getTaxiId());
            requestCommData(json);
        }catch (JSONException e){
            e.printStackTrace();
        }

    }


    public void requestCommData(JSONObject json){

        JsonObjectRequest jsonObjectRequest;

        jsonObjectRequest = new JsonObjectRequest
                (Request.Method.POST, Constants.SERVER_URL + "get_driver.php", json, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            String res = response.getString("response");
                            if (res.equals("OK")) {
                                JSONObject data=(JSONObject) response.get("data");
                                firstName=data.getString("firstName");
                                lastName=data.getString("lastName");
                                nrPlate=data.getString("nrPlate");
                                String dateStr=data.getString("dob");
                                dob=MiscellaneousUtils.String2Date(dateStr);
                                String genStr=data.getString("gender");
                                gender= genStr.equals("m") ?"male":"female";
                                reputation=data.getDouble("repAvg");
                                String base64=data.getString("photo");
                                byte[] biteOutput = Base64.decode(base64, 0);
                                photo = BitmapFactory.decodeByteArray(biteOutput, 0, biteOutput.length);
                                dataUpdateListener.onDataUpdateReceived();


                                //change comms strings and photo
                            }else{
                                //error on server side fix
                            }
                            //notification
                        } catch (JSONException e) {
                            e.printStackTrace();
                            Toast.makeText(mContext,"json failed",Toast.LENGTH_LONG).show();

                        }

                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        error.printStackTrace();
                        //send request again

                    }
                });
        MySingleton.getInstance(mContext).addToRequestQueue(jsonObjectRequest);
    }

    //callback for when a new message has arrived
    public interface DataUpdateListener{
        void onDataUpdateReceived();
    }

    DataUpdateListener dataUpdateListener;

    public void setDataUpdateListener(DataUpdateListener dataUpdateListener) {
        this.dataUpdateListener = dataUpdateListener;
    }


}
