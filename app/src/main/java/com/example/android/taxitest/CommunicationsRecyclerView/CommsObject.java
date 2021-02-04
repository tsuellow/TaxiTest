package com.example.android.taxitest.CommunicationsRecyclerView;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.util.Log;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.example.android.taxitest.AppExecutors;
import com.example.android.taxitest.Constants;
import com.example.android.taxitest.CustomUtils;
import com.example.android.taxitest.MainActivity;
import com.example.android.taxitest.R;
import com.example.android.taxitest.connection.MySingleton;
import com.example.android.taxitest.data.ClientObject;
import com.example.android.taxitest.data.CommRecordObject;
import com.example.android.taxitest.data.SqlLittleDB;
import com.example.android.taxitest.data.TaxiObject;
import com.example.android.taxitest.utils.MiscellaneousUtils;
import com.example.android.taxitest.vtmExtension.TaxiMarker;
import com.mikhaellopez.circularprogressbar.CircularProgressBar;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class CommsObject {

    private static final String TAG = "CommsObject";
    
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

    public Context mContext;
    public String commId;
    String mLatestMsjId;
    int mMsjStatus=OBSERVING;
    public TaxiMarker taxiMarker;
    private List<MetaMessageObject> msjList=new ArrayList<>();
    boolean accepted=false;
    boolean commEngaged=false;

    public static SqlLittleDB mDb;

    public List<MetaMessageObject> getMsjList() {
        return msjList;
    }

    //driver info driverObject from room DB
    public CardData commCardData=new CardData();
    public void setCommCardData(CardData cardData){
        commCardData=cardData;
    }
    public class CardData{

        public String title="Fulanito";
        public String collar="ES0000";
        public String firstName="Fulanito";
        public String lastName="de Tal";
        public String extra="";
        public long profileTimestamp=0;
        public double reputation=3.0;
        public Date dob=new Date();
        public String gender="male";
        public Bitmap thumb = null;

        public CardData(){};

        public void setData(String title, String collar, String firstName, String lastName, double reputation, Date dob, String gender, long timestamp) {
            this.title=title;
            this.collar = collar;
            this.firstName = firstName;
            this.lastName = lastName;
            this.reputation = reputation;
            this.dob = dob;
            this.gender = gender;
            this.profileTimestamp=timestamp;
        }

        public void setThumb(Bitmap bitmap){
            thumb=bitmap;
        }
    }

    public static void initializeCommsDbAccess(Context context){
        mDb=SqlLittleDB.getInstance(context);
    }

    //list of individual communications

    public CommsObject(TaxiMarker taxiMarker, Context context) {
        this.taxiMarker=taxiMarker;
        commId=MainActivity.myId+"_"+CustomUtils.getOtherStringId(taxiMarker.taxiObject.getTaxiId())+"_"+(new Date().getTime());
        mContext=context;
        archiveComm();
        sendDataRequest();

    }

    public void archiveComm(){
        CommRecordObject commRec;
        if (taxiMarker.taxiObject instanceof ClientObject){
            commRec=new CommRecordObject(this,(TaxiObject) ((MainActivity)mContext).mOwnTaxiObject,mContext);
        }else {
            commRec=new CommRecordObject(this,(ClientObject) ((MainActivity)mContext).mOwnTaxiObject,mContext);
        }
        AppExecutors.getInstance().diskIO().execute(new Runnable() {
            @Override
            public void run() {
                mDb.commsDao().insertComm(commRec);
            }
        });
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
            MiscellaneousUtils.showNotification(mContext,"You have been contacted!", "Someone has sent you a message on TaxiTest recently");
        }
        //change comm status
        if (msjList.size()==1){
            updateCommStatus(CommRecordObject.CONTACTED);
        }


        if (msjUpdateListener!=null) {
            Log.d(TAG,"msjStatus:"+mMsjStatus);
            msjUpdateListener.onMsjUpdateReceived(mMsjStatus);
        }
    }

    public void updateCommStatus(int status){
        Log.d(TAG,"msjStatus:"+status);
        AppExecutors.getInstance().diskIO().execute(new Runnable() {
            @Override
            public void run() {
                mDb.commsDao().updateStatus(commId,status);
            }

        });
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

    public void setAccepted() {
        if(!accepted){
            updateCommStatus(CommRecordObject.ACCEPTED);
            this.accepted = true;
            getImageFile(false);//gets and saves the photo of other party once accepted
        }
    }

    public boolean isCommEngaged() {
        return commEngaged;
    }

    public void setCommEngaged(boolean commEngaged) {
        this.commEngaged = commEngaged;
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
        TextView name=dialog.findViewById(R.id.tv_other);
        if (commCardData==null){
            title.setText("Chat with UNKNOWN");
            name.setText("UNKNOWN");
        }else{
            title.setText("Chat with "+commCardData.firstName);
            name.setText(commCardData.firstName+", ");
        }

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
        layoutManager.setReverseLayout(true);
        commsRV.setLayoutManager(layoutManager);
        commsRV.scrollToPosition(0);
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
        String url=Constants.SERVER_URL + CustomUtils.apiExtension+taxiMarker.taxiObject.getTaxiId();
        requestCommData(url, this);
        getImageFile(true);
    }


    public void requestCommData(String url, final CommsObject commsObject){

        JsonObjectRequest jsonObjectRequestDriver;

        jsonObjectRequestDriver = new JsonObjectRequest
                (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            String res = response.getString("response");
                            if (res.equals("OK")) {
                                try{
                                    JSONObject data=(JSONObject) response.get("data");
                                    CustomUtils.interpretJson(data,commsObject);
                                    updateCommRec();//insert received data into db
                                    dataUpdateListener.onDataUpdateReceived();
                                }catch (Exception e){
                                    Log.d(TAG, "recibimos paja");
                                }
                                //change comms strings and photo
                            }else{
                                //error on server side fix
                                Log.d(TAG, "error in json");
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
        MySingleton.getInstance(mContext).addToRequestQueue(jsonObjectRequestDriver);
    }

    public void getImageFile(boolean isThumb){
        Log.d(TAG, "bitmap requested");
        String url=isThumb?CustomUtils.getThumbUrl(taxiMarker.taxiObject.getTaxiId()):CustomUtils.getFaceUrl(taxiMarker.taxiObject.getTaxiId(),commCardData.profileTimestamp);
        ImageRequest imageRequest=new ImageRequest(url,
                new Response.Listener<Bitmap>() {
            @Override
            public void onResponse(Bitmap response) {
                Log.d(TAG, "bitmap received");
                if (isThumb){
                    commCardData.setThumb(response);
                    photoUpdateListener.onPhotoUpdateReceived();
                    File thumbFile=MiscellaneousUtils.makeThumbFile(mContext,CustomUtils.getOtherStringId(taxiMarker.taxiObject.getTaxiId()));
                    MiscellaneousUtils.saveBitmapToFile(thumbFile,response);
                }else{
                    File faceFile=MiscellaneousUtils.makePhotoFile(mContext,commId,CustomUtils.getOtherStringId(taxiMarker.taxiObject.getTaxiId()));
                    MiscellaneousUtils.saveBitmapToFile(faceFile,response);
                }

            }
        }, 480, 480, ImageView.ScaleType.CENTER_INSIDE, Bitmap.Config.RGB_565,
                new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d(TAG, "error caught"+CustomUtils.getThumbUrl(taxiMarker.taxiObject.getTaxiId()));
                error.printStackTrace();
            }
        });
        MySingleton.getInstance(mContext).addToRequestQueue(imageRequest);
    }


    public  void backUpAcceptedComm(JSONObject json){

        JsonObjectRequest jsonObjectRequestDriver;

        jsonObjectRequestDriver = new JsonObjectRequest
                (Request.Method.POST, Constants.SERVER_URL + "comm-log", json, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            String res = response.getString("response");
                            if (res.equals("OK")) {
                                Log.d(TAG, "successful");
                            }else{
                                Log.d(TAG, "failed at sql");
                            }
                            //notification
                        } catch (JSONException e) {
                            e.printStackTrace();
                            Log.d(TAG, "failed at json");
                        }

                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        error.printStackTrace();
                        Log.d(TAG, "failed at volley");

                    }
                });
        MySingleton.getInstance(mContext).addToRequestQueue(jsonObjectRequestDriver);
    }

    public void updateCommRec(){
        AppExecutors.getInstance().diskIO().execute(new Runnable() {
            @Override
            public void run() {
                mDb.commsDao().updatePersonalData(commId,commCardData.firstName,commCardData.lastName,commCardData.gender,commCardData.dob.getTime(),commCardData.collar,commCardData.reputation);
            }
        });
    }

    //callback for when a new message has arrived
    public interface DataUpdateListener{
        void onDataUpdateReceived();
    }

    DataUpdateListener dataUpdateListener;

    public void setDataUpdateListener(DataUpdateListener dataUpdateListener) {
        this.dataUpdateListener = dataUpdateListener;
    }

    public interface PhotoUpdateListener{
        void onPhotoUpdateReceived();
    }

    PhotoUpdateListener photoUpdateListener;

    public void setPhotoUpdateListener(PhotoUpdateListener photoUpdateListener) {
        this.photoUpdateListener = photoUpdateListener;
    }


}
