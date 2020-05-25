package com.example.android.taxitest.CommunicationsRecyclerView;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;


import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.android.taxitest.Constants;
import com.example.android.taxitest.MainActivity;
import com.example.android.taxitest.R;
import com.example.android.taxitest.RecordButtonUtils.RecordButton;
import com.example.android.taxitest.utils.MiscellaneousUtils;
import com.example.android.taxitest.vectorLayer.ConnectionLineLayer2;
import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.mikhaellopez.circularprogressbar.CircularProgressBar;

import org.json.JSONObject;
import org.oscim.utils.ThreadUtils;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class CommunicationsAdapter extends RecyclerView.Adapter<CommunicationsAdapter.ViewHolder>{

    private static final String LOG_TAG = "AudioRecordTest";

    private List<CommsObject> mComms;
    private Context mContext;
    ConnectionLineLayer2 connectionLines;
    public List<MessageObject> newIncomingComms=new ArrayList<>();

    public static SoundPool soundPool = null;
    public static int soundMsjArrived,soundCanceled,soundAck;


    public CommunicationsAdapter(Context context, ConnectionLineLayer2 connectionLineLayer){
        mContext=context;
        mComms=new ArrayList<CommsObject>();
        connectionLines=connectionLineLayer;

        //socket code placeholder
        try {
            IO.Options opts=new IO.Options();
            opts.forceNew = true;
            opts.query = "id=t"+Constants.myId;
            mSocket = IO.socket("https://id-ex-websocket-audiochat-eu.herokuapp.com",opts);
            Log.d("socketTest","success");
            initializeSocketListener();
            connectSocket();
        } catch (URISyntaxException e) {
            e.printStackTrace();
            Log.d("socketTest","failed");
        }

        if (soundPool==null){
            soundPool = new SoundPool.Builder()
                    .setMaxStreams(5)
                    .build();
            //these are just two wav files with short intro and exit sounds
            soundMsjArrived=soundPool.load(mContext,R.raw.end_recording_sound,0);
            soundCanceled=soundPool.load(mContext,R.raw.swipe_sound,0);
            soundAck=soundPool.load(mContext,R.raw.ack_sound,0);
        }

    }

    public void setConnectionLines(ConnectionLineLayer2 connectionLines){
        this.connectionLines=connectionLines;
    }

    public void addItem(final CommsObject commsObject){
        Log.d("socketTest",""+ThreadUtils.isMainThread());
        mComms.add(commsObject);
        //notifyDataSetChanged();
        int i=mComms.indexOf(commsObject);
        notifyItemInserted(i);
    }

    public synchronized List<CommsObject> getItemList(){
        return mComms;
    }

    public List<Integer> getCommIds(){
        List<Integer> result=new ArrayList<>();
        for (CommsObject comm:mComms){
            result.add(comm.taxiMarker.taxiObject.getTaxiId());
        }
        for (MessageObject msj:newIncomingComms){
            result.add(MiscellaneousUtils.getNumericId(msj.getSendingId()));
        }
        return result;
    }

    public synchronized void cancelById(int taxiId){
        Iterator<CommsObject> i = mComms.iterator();
        while (i.hasNext()) {
            CommsObject comm = i.next();
            if (comm.taxiMarker.taxiObject.getTaxiId() == taxiId) {
                int index=mComms.indexOf(comm);
                //send cancellation msj
                if (comm.mMsjStatus!=CommsObject.OBSERVING){
                    MessageObject msj=new MessageObject(MiscellaneousUtils.getStringId(taxiId),CommsObject.REJECTED);
                    attemptSendMsj(msj);
                }
                if (comm.isPlaying){
                    comm.stopPlaying();
                }
                i.remove();
                notifyItemRemoved(index);
                //notifyDataSetChanged();
                break;
            }
        }
    }

    public int getCommIndex(int taxiId){
        for (CommsObject comm:mComms){
            if (comm.taxiMarker.taxiObject.getTaxiId() == taxiId){
                return mComms.indexOf(comm);
            }
        }
        return -1;//case when comm not found
    }


    public class ViewHolder extends RecyclerView.ViewHolder{

        private int adapterPosition;
        private TextView name;
        private TextView numberPlate;
        private TextView destination;
        private ImageView photo;
        private CardView destColor;
        public TextView arrows;
        public RecordButton recordButton;
        private CircularProgressBar progressBar;
        private FloatingActionButton confirmOrPlay;
        public FloatingActionButton cancel;
        //acknowledgements
        private LinearLayout ackBubble;
        private ImageView ackCheck, ackPlayed, ackHeard, ackRecording, ackFailed;
        private TextView descriptionText, descriptionPointer, ackFailedText;
        //commsMenu
        private ImageButton commHistoryMenu;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            name = (TextView) itemView.findViewById(R.id.tv_name);
            numberPlate = (TextView) itemView.findViewById(R.id.tv_plate);
            destination = (TextView) itemView.findViewById(R.id.tv_destination);
            arrows=(TextView) itemView.findViewById(R.id.tv_arrows);
            photo = (ImageView) itemView.findViewById(R.id.iv_photo);
            destColor = (CardView) itemView.findViewById(R.id.cv_border_color);
            recordButton=(RecordButton) itemView.findViewById(R.id.record_button);
            progressBar=(CircularProgressBar) itemView.findViewById(R.id.play_progress_bar);
            confirmOrPlay = (FloatingActionButton) itemView.findViewById(R.id.bt_confirm);
            cancel = (FloatingActionButton) itemView.findViewById(R.id.bt_cancel);
            //acknowledgements
            ackBubble=(LinearLayout) itemView.findViewById(R.id.ack_bubble);
            ackCheck=(ImageView) itemView.findViewById(R.id.ack_check);
            ackPlayed=(ImageView) itemView.findViewById(R.id.ack_played);
            ackHeard=(ImageView) itemView.findViewById(R.id.ack_heard);
            ackRecording=(ImageView) itemView.findViewById(R.id.ack_recording);
            ackFailed=(ImageView) itemView.findViewById(R.id.ack_failed);
            ackFailedText=(TextView) itemView.findViewById(R.id.ack_failed_text);
            //description of dynamic button
            descriptionText=(TextView) itemView.findViewById(R.id.tv_description_text);
            descriptionPointer=(TextView) itemView.findViewById(R.id.tv_description_pointer);
            commHistoryMenu=(ImageButton) itemView.findViewById(R.id.bt_comm_history);

        }


    }

    @NonNull
    @Override
    public CommunicationsAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater=LayoutInflater.from(mContext);

        //now inflate the view
        View commsView=inflater.inflate(R.layout.adapter,parent,false);
        CommunicationsAdapter.ViewHolder viewHolder=new CommunicationsAdapter.ViewHolder(commsView);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull final CommunicationsAdapter.ViewHolder holder, int position) {
        final CommsObject comm=mComms.get(position);
        final int taxiId=comm.taxiMarker.taxiObject.getTaxiId();
        holder.name.setText("ID: "+comm.taxiMarker.taxiObject.getTaxiId());
        holder.numberPlate.setText("T"+comm.taxiMarker.taxiObject.getTaxiId());
        holder.destination.setText("barrio: "+comm.taxiMarker.color);
        holder.destColor.setCardBackgroundColor(comm.taxiMarker.color);
        holder.recordButton.setComm(taxiId);


        //resetting values for recycling process
        resetMediaProgressBar(holder.progressBar);
        holder.ackBubble.setVisibility(View.GONE);
        holder.descriptionText.setText("invite");
        holder.descriptionText.setTypeface(null, Typeface.NORMAL);
        holder.descriptionText.setTextColor(ContextCompat.getColor(mContext,R.color.colorGreen));
        holder.descriptionPointer.setTextColor(ContextCompat.getColor(mContext,R.color.colorGreen));
        repaintButton(comm,holder.confirmOrPlay, holder.ackBubble, holder.descriptionText, holder.descriptionPointer);


        comm.setMsjUpdateListener(new CommsObject.MsjUpdateListener() {
            @Override
            public void onMsjUpdateReceived(int intentCode) {
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        Log.d("socketTest","is being executed");
                        CommsObject currComm=mComms.get(getCommIndex(taxiId));
                        repaintButton(currComm,holder.confirmOrPlay, holder.ackBubble, holder.descriptionText, holder.descriptionPointer);
                    }
                });
                //notifyItemChanged(getCommIndex(taxiId));
            }
        });

        comm.setAckUpdateListener(new CommsObject.AckUpdateListener() {
            @Override
            public void onAckUpdateReceived(final AcknowledgementObject ack) {
                Log.d("socketTest",ack.msgId);
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        repaintAck(comm,ack,holder.ackCheck,holder.ackPlayed,holder.ackHeard,holder.ackRecording,holder.ackFailed,holder.ackFailedText);
                    }
                });
            }
        });

        holder.recordButton.setRecordingStartedListener(new RecordButton.RecordingStartedListener() {
            @Override
            public void onRecordingStarted() {
                attemptSendAck(new AcknowledgementObject(comm, CommsObject.RECORDING_STARTED,"rec"));
            }
        });

        holder.recordButton.setRecordingFinishedListener(new RecordButton.RecordingFinishedListener() {
            @Override
            public void onRecordingFinished(File file) {
                attemptSendAck(new AcknowledgementObject(comm, CommsObject.RECORDING_STOPPED,"rec"));
                CommsObject currComm=mComms.get(getCommIndex(taxiId));
                MetaMessageObject metaMsj=new MetaMessageObject(CommsObject.REQUEST_SENT,file,currComm);
                currComm.addAtTopOfMsjList(metaMsj);
                metaMsj.addAckAtTopOfList(new AcknowledgementObject(metaMsj.getMsjObject(),CommsObject.SENT));
                MessageObject msj=metaMsj.getMsjObject();
                attemptSendMsj(msj);
            }
        });

        holder.confirmOrPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CommsObject currComm=mComms.get(getCommIndex(taxiId));
                switch (currComm.getButtonCode()){
                    case CommsObject.INVITE:
                        //send request
                        MetaMessageObject metaMsj=new MetaMessageObject(CommsObject.REQUEST_SENT,null,currComm);
                        currComm.addAtTopOfMsjList(metaMsj);
                        metaMsj.addAckAtTopOfList(new AcknowledgementObject(metaMsj.getMsjObject(),CommsObject.SENT));
                        MessageObject msj=metaMsj.getMsjObject();
                        attemptSendMsj(msj);
                        connectionLines.playCommAnim(taxiId);
                        break;
                    case  CommsObject.AWAITING:
                        //do nothing, only toast/play own msg
                        Toast.makeText(mContext,"awaiting response from other party", Toast.LENGTH_LONG).show();
                        break;
                    case  CommsObject.PLAY:
                        //start playing
                        currComm.startPlaying(currComm.getNextUnplayedMsj(),holder.progressBar);
                        sendAndRegisterOwnAck(new AcknowledgementObject(currComm.getNextUnplayedMsj().getMsjObject(),CommsObject.PLAYED));
                        break;
                    case  CommsObject.ACCEPT:
                        //send acceptance
                        MetaMessageObject metaMsjAccept=new MetaMessageObject(CommsObject.ACCEPTED,null,currComm);
                        currComm.addAtTopOfMsjList(metaMsjAccept);
                        MessageObject msjAccept=metaMsjAccept.getMsjObject();
                        attemptSendMsj(msjAccept);
                        connectionLines.playCommAnim(taxiId);
                        break;
                    case  CommsObject.BT_ACCEPTED:
                        //paint btn black
                        Toast.makeText(mContext,"taxi was already accepted", Toast.LENGTH_LONG).show();
                        break;
                }
            }
        });

        holder.commHistoryMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                comm.showCommsDialog(mContext);
            }
        });


    }


    public void repaintButton(CommsObject currComm, FloatingActionButton fab, LinearLayout ackBubble, TextView descriptionText, TextView descriptionPointer){
        //TODO add button animations and compress code
        switch (currComm.getButtonCode()){
            case  CommsObject.AWAITING:
                //do nothing, only toast/play own msg
                ackBubble.setVisibility(View.VISIBLE);
                if (!currComm.isAccepted()) {
                    fab.setImageResource(R.drawable.waiting);
                    fab.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(mContext,R.color.colorDeselected)));
                    descriptionText.setText("awaiting response");
                    descriptionText.setTextColor(ContextCompat.getColor(mContext, R.color.colorDeselected));
                    descriptionPointer.setTextColor(ContextCompat.getColor(mContext, R.color.colorDeselected));
                }
                break;
            case  CommsObject.PLAY:
                //start playing
                ackBubble.setVisibility(View.GONE);
                fab.setImageResource(R.drawable.play_button);
                if (!currComm.isAccepted()) {
                    fab.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(mContext,R.color.colorGreen)));
                    descriptionText.setText("play audio");
                    descriptionText.setTextColor(ContextCompat.getColor(mContext, R.color.colorGreen));
                    descriptionPointer.setTextColor(ContextCompat.getColor(mContext, R.color.colorGreen));
                }
                break;
            case  CommsObject.ACCEPT:
                //send acceptance
                ackBubble.setVisibility(View.GONE);
                fab.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(mContext,R.color.colorGreen)));
                fab.setImageResource(R.drawable.confirm);
                descriptionText.setText("accept");
                descriptionText.setTextColor(ContextCompat.getColor(mContext,R.color.colorGreen));
                descriptionPointer.setTextColor(ContextCompat.getColor(mContext,R.color.colorGreen));
                break;
            case  CommsObject.BT_ACCEPTED:
                //paint btn black
                if (currComm.getTopOfList().isOutgoing){
                    ackBubble.setVisibility(View.VISIBLE);
                    currComm.getTopOfList().addAckAtTopOfList(new AcknowledgementObject(currComm.getTopOfList().getMsjObject(),CommsObject.SENT));
                }else {
                    ackBubble.setVisibility(View.GONE);
                }
                fab.setImageResource(R.drawable.accepted);
                fab.setBackgroundTintList(ColorStateList.valueOf(Color.BLACK));
                descriptionText.setText("accepted");
                descriptionText.setTypeface(null, Typeface.BOLD);
                descriptionText.setTextColor(Color.BLACK);
                descriptionPointer.setTextColor(Color.BLACK);
                break;
            default:
                ackBubble.setVisibility(View.GONE);
                fab.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(mContext,R.color.colorGreen)));
                fab.setImageResource(R.drawable.send);
                descriptionText.setText("invite");
                descriptionText.setTextColor(ContextCompat.getColor(mContext,R.color.colorGreen));
                descriptionPointer.setTextColor(ContextCompat.getColor(mContext,R.color.colorGreen));
        }
        fab.hide();
        fab.show();
    }

    public void repaintAck(CommsObject currCom, AcknowledgementObject ack, ImageView ackCheck, ImageView ackPlayed, ImageView ackHeard, ImageView ackRecording, ImageView ackFailed, TextView ackFailedText){
        if (ack.getMsgId().equals("rec")){
            if (ack.getAckCode() == CommsObject.RECORDING_STARTED) {
                ackRecording.setColorFilter(ContextCompat.getColor(mContext, R.color.colorGreenAcc));
            } else if (ack.getAckCode() == CommsObject.RECORDING_STOPPED){
                ackRecording.setColorFilter(Color.WHITE);
            }
        }else{
            switch (ack.getAckCode()){
                case CommsObject.SENT:
                    ackCheck.setColorFilter(Color.WHITE);
                    ackPlayed.setColorFilter(Color.WHITE);
                    ackHeard.setColorFilter(Color.WHITE);
                    ackRecording.setColorFilter(Color.WHITE);
                    ackCheck.setVisibility(View.VISIBLE);
                    ackPlayed.setVisibility(View.VISIBLE);
                    ackHeard.setVisibility(View.VISIBLE);
                    ackRecording.setVisibility(View.VISIBLE);
                    ackFailed.setVisibility(View.GONE);
                    ackFailedText.setVisibility(View.GONE);
                    if (currCom.findMsjById(ack.getMsgId()).getAudioFile()==null){
                        ackPlayed.setVisibility(View.GONE);
                        ackHeard.setVisibility(View.GONE);
                    }
                    break;
                case CommsObject.RECEIVED:

                    ackCheck.setColorFilter(ContextCompat.getColor(mContext, R.color.colorBlueAcc));
                    soundPool.play(CommunicationsAdapter.soundAck,1,1,0,0,1);
                    break;
                case CommsObject.PLAYED:
                    ackCheck.setColorFilter(ContextCompat.getColor(mContext, R.color.colorBlueAcc));
                    ackPlayed.setColorFilter(ContextCompat.getColor(mContext, R.color.colorBlueAcc));
                    break;
                case CommsObject.HEARD:
                    ackCheck.setColorFilter(ContextCompat.getColor(mContext, R.color.colorBlueAcc));
                    ackPlayed.setColorFilter(ContextCompat.getColor(mContext, R.color.colorBlueAcc));
                    ackHeard.setColorFilter(ContextCompat.getColor(mContext, R.color.colorBlueAcc));
                    break;
                case CommsObject.FAILED:
                    ackCheck.setVisibility(View.GONE);
                    ackPlayed.setVisibility(View.GONE);
                    ackHeard.setVisibility(View.GONE);
                    ackRecording.setVisibility(View.GONE);
                    ackFailed.setVisibility(View.VISIBLE);
                    ackFailedText.setVisibility(View.VISIBLE);
            }

        }

    }


    @Override
    public int getItemCount() {
        if (mComms==null){
            return  0;
        }else {
            return mComms.size();
        }
    }


    //temporary socket code
    private static Socket mSocket;
    Emitter.Listener onChatReceived;
    Emitter.Listener onAckReceived;
    Emitter.Listener onSocketsConnected;

    public void connectSocket(){
        mSocket.on("audio chat",onChatReceived);
        mSocket.on("acknowledgment",onAckReceived);
        mSocket.on("sockets connected",onSocketsConnected);
        mSocket.connect();
    }

    public void disconnectSocket(){
        mSocket.off("audio chat",onChatReceived);
        mSocket.off("acknowledgment",onAckReceived);
        mSocket.off("sockets connected",onSocketsConnected);
        mSocket.disconnect();
    }

    public void initializeSocketListener(){
        onChatReceived=new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                Log.d("socketTest","size "+args[0].toString());
                try {
                    JSONObject jsonObject =(JSONObject)args[0];
                    MessageObject msj=MessageObject.readIntoMsj(jsonObject);
                    int id = MiscellaneousUtils.getNumericId(msj.getSendingId());
                    if (getCommIndex(id)==-1){//comm is new incoming
                        newIncomingComms.add(msj);
                        messageInvitationListener.onInvitationReceived(msj);
                    }else{//comm is already there
                        CommsObject comm=mComms.get(getCommIndex(id));
                        MetaMessageObject metaMsj=new MetaMessageObject(msj,comm);
                        mComms.get(getCommIndex(id)).addAtTopOfMsjList(metaMsj);
                        if (msj.getIntentCode()==CommsObject.REJECTED){
                            messageCancellationListener.onCancellationReceived(msj);
                        }
                    }
                    //acknowledge receipt of msj
                    sendAndRegisterOwnAck(new AcknowledgementObject(msj,CommsObject.RECEIVED));
                }catch (Exception err){
                    Log.d("Error", err.toString());
                }

            }
        };
        onAckReceived=new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                Log.d("socketTest","ack "+args[0].toString());
                try {
                    JSONObject jsonObject =(JSONObject)args[0];
                    AcknowledgementObject ack=AcknowledgementObject.readIntoAck(jsonObject);
                    int id = MiscellaneousUtils.getNumericId(ack.getSendingId());
                    if (getCommIndex(id)!=-1){//comm is already there
                        CommsObject comm=mComms.get(getCommIndex(id));
                        if (!ack.getMsgId().equals("rec")) {
                            MetaMessageObject metaMsj = comm.findMsjById(ack.getMsgId());
                            if (metaMsj != null) {
                                metaMsj.addAckAtTopOfList(ack);
                            }
                        }else{
                            Log.d("socketTest",ack.msgId);
                            comm.ackUpdateListener.onAckUpdateReceived(ack);
                        }
                    }
                }catch (Exception err){
                    Log.d("Error", err.toString());
                }
            }
        };
        onSocketsConnected=new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                Log.d("socketTest", args[0].toString());
            }
        };
    }


    public static void attemptSendMsj(MessageObject msj){
        JSONObject payload=msj.generateJson();
        mSocket.emit("audio chat", payload);
    }

    public static void attemptSendAck(AcknowledgementObject ack){
        JSONObject payload=ack.generateJson();
        mSocket.emit("acknowledgment", payload);
    }

    public void sendAndRegisterOwnAck(AcknowledgementObject ack){
        attemptSendAck(ack);
        if (getCommIndex(MiscellaneousUtils.getNumericId(ack.getReceivingId()))!=-1) {
            CommsObject comm = mComms.get(getCommIndex(MiscellaneousUtils.getNumericId(ack.getReceivingId())));
            if (comm.findMsjById(ack.getMsgId())!=null) {
                MetaMessageObject msj = comm.findMsjById(ack.getMsgId());
                msj.addAckAtTopOfList(ack);
            }
        }
    }

    public interface MessageInvitationListener{
        void onInvitationReceived(MessageObject msj);
    }

    MessageInvitationListener messageInvitationListener;

    public void setMessageInvitationListener(MessageInvitationListener messageInvitationListener) {
        this.messageInvitationListener = messageInvitationListener;
    }

    public interface MessageCancellationListener{
        void onCancellationReceived(MessageObject msj);
    }

    MessageCancellationListener messageCancellationListener;

    public void setMessageCancellationListener(MessageCancellationListener messageCancellationListener) {
        this.messageCancellationListener = messageCancellationListener;
    }

    //multiple methods
    public static void resetMediaProgressBar(CircularProgressBar circularProgressBar){
        circularProgressBar.setProgress(0.0f);
        circularProgressBar.setVisibility(View.INVISIBLE);
    }
}
