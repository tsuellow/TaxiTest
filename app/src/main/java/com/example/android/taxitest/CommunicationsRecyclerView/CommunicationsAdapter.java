package com.example.android.taxitest.CommunicationsRecyclerView;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.media.SoundPool;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;


import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.android.taxitest.CustomUtils;
import com.example.android.taxitest.MainActivity;
import com.example.android.taxitest.R;
import com.example.android.taxitest.RecordButtonUtils.RecordButton;
import com.example.android.taxitest.utils.CurvedTextView;
import com.example.android.taxitest.utils.MiscellaneousUtils;
import com.example.android.taxitest.vectorLayer.ConnectionLineLayer2;
import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.mikhaellopez.circularprogressbar.CircularProgressBar;

import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;
import org.oscim.utils.ThreadUtils;

import java.io.File;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class CommunicationsAdapter extends RecyclerView.Adapter<CommunicationsAdapter.ViewHolder>{

    private static final String LOG_TAG = "AudioRecordTest";

    public List<CommsObject> mComms;
    private Context mContext;
    ConnectionLineLayer2 connectionLines;
    public List<MessageObject> newIncomingComms=new ArrayList<>();
    RecyclerView mRecyclerView;
    public static SoundPool soundPool = null;
    public static int soundMsjArrived,soundCanceled,soundAck;


    public CommunicationsAdapter(Context context, ConnectionLineLayer2 connectionLineLayer){
        mContext=context;
        mComms=new ArrayList<CommsObject>();
        connectionLines=connectionLineLayer;
        CommsObject.initializeCommsDbAccess(mContext);

        //socket code placeholder
        try {
            IO.Options opts=new IO.Options();
            opts.forceNew = true;
            opts.query = "id="+ MainActivity.myId;
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

    @Override
    public void onAttachedToRecyclerView(@NotNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);

        mRecyclerView = recyclerView;
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
        Log.d("testes", "cancelById: is happening");
        Iterator<CommsObject> i = mComms.iterator();
        while (i.hasNext()) {
            CommsObject comm = i.next();
            if (comm.taxiMarker.taxiObject.getTaxiId() == taxiId) {
                int index=mComms.indexOf(comm);
                //send cancellation msj
                if (comm.mMsjStatus!=CommsObject.OBSERVING){
                    MessageObject msj=new MessageObject(CustomUtils.getOtherStringId(taxiId),CommsObject.REJECTED);
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

    public void cancelByIdWithAnim(int taxiId) {
        Log.d("commies", "cancelByIdWithAnim: "+taxiId);
        CommsObject comm=mComms.get(getCommIndex(taxiId));
        int index=mComms.indexOf(comm);
        Animation anim = AnimationUtils.loadAnimation(mContext,
                android.R.anim.slide_out_right);
        anim.setDuration(300);
        View itemView=mRecyclerView.getLayoutManager().findViewByPosition(index);
        if (itemView!=null)
        itemView.startAnimation(anim);

        if (comm.mMsjStatus!=CommsObject.OBSERVING){
            MessageObject msj=new MessageObject(CustomUtils.getOtherStringId(taxiId),CommsObject.REJECTED);
            attemptSendMsj(msj);
        }
        if (comm.isPlaying){
            comm.stopPlaying();
        }

        anim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                int index=mComms.indexOf(comm);
                mComms.remove(comm);
                notifyItemRemoved(index);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
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
        private CurvedTextView numberPlate;
        private TextView destination;
        private ImageView photo;
        private CardView destColor;
        public TextView arrows;
        public RecordButton recordButton;
        private CircularProgressBar progressBar;
        private FloatingActionButton confirmOrPlay;
        public FloatingActionButton cancel;
        private ProgressBar loadingFace;
        private TextView reputation;
        private ImageView reputationStar;
        //acknowledgements
        private LinearLayout ackBubble;
        private ImageView ackCheck, ackPlayed, ackHeard, ackRecording, ackFailed;
        private TextView descriptionText, descriptionPointer, ackFailedText;
        //commsMenu
        private ImageButton commHistoryMenu;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            name = (TextView) itemView.findViewById(R.id.tv_name);
            numberPlate = (CurvedTextView) itemView.findViewById(R.id.ct_plate);
            destination = (TextView) itemView.findViewById(R.id.tv_destination);
            arrows=(TextView) itemView.findViewById(R.id.tv_arrows);
            photo = (ImageView) itemView.findViewById(R.id.iv_photo);
            loadingFace=(ProgressBar) itemView.findViewById(R.id.pb_loading_face);
            reputation=(TextView) itemView.findViewById(R.id.tv_rep);
            reputationStar=(ImageView) itemView.findViewById(R.id.iv_rep);
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
        //holder.name.setText("ID: "+comm.taxiMarker.taxiObject.getTaxiId());

        //holder.numberPlate.setText("T"+comm.taxiMarker.taxiObject.getTaxiId());
        holder.destination.setText(comm.taxiMarker.barrio);
        holder.destination.setTextColor(comm.taxiMarker.color);
        holder.destColor.setCardBackgroundColor(comm.taxiMarker.color);
        holder.recordButton.setCommId(CustomUtils.getOtherStringId(taxiId));

        comm.setDataUpdateListener(new CommsObject.DataUpdateListener() {
            @Override
            public void onDataUpdateReceived() {
                holder.name.setText(comm.commCardData.title);
                holder.photo.setImageBitmap(comm.commCardData.photo);
                holder.loadingFace.setVisibility(View.GONE);
                holder.numberPlate.setText(comm.commCardData.collar);
                double rounded = Math. round(comm.commCardData.reputation * 100.0) / 100.0;
                holder.reputation.setText(String.valueOf(rounded));
            }
        });


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

        final CountDownTimer recordingTimer=new CountDownTimer(20000, 1000) {
            public void onTick(long millisUntilFinished) {
                int secsLeft=(int)millisUntilFinished/1000;
                holder.descriptionText.setText("recording:\n"+secsLeft+"s      ");

            }

            public void onFinish() {
                holder.descriptionText.setText("recording ended");
                holder.recordButton.stopRecording();
            }

        };

        holder.recordButton.setRecordingStartedListener(new RecordButton.RecordingStartedListener() {
            @Override
            public void onRecordingStarted() {
                isRecording=true;
                whoIsRecording=CustomUtils.getOtherStringId(taxiId);
                attemptSendAck(new AcknowledgementObject(comm, CommsObject.RECORDING_STARTED,"rec"));
                holder.descriptionText.setTextColor(ContextCompat.getColor(mContext,R.color.colorBlue));
                holder.descriptionPointer.setTextColor(ContextCompat.getColor(mContext,R.color.colorBlue));
                recordingTimer.start();
            }
        });

        holder.recordButton.setRecordingFinishedListener(new RecordButton.RecordingFinishedListener() {
            @Override
            public void onRecordingFinished(File file) {
                isRecording=false;
                whoIsRecording=null;
                recordingTimer.cancel();
                if (file != null) {
                    attemptSendAck(new AcknowledgementObject(comm, CommsObject.RECORDING_STOPPED,"rec"));
                    CommsObject currComm=mComms.get(getCommIndex(taxiId));
                    MetaMessageObject metaMsj=new MetaMessageObject(CommsObject.REQUEST_SENT,file,currComm);
                    currComm.addAtTopOfMsjList(metaMsj);
                    metaMsj.addAckAtTopOfList(new AcknowledgementObject(metaMsj.getMsjObject(),CommsObject.SENT));
                    MessageObject msj=metaMsj.getMsjObject();
                    attemptSendMsj(msj);
                }
                resolvePendingCancellations();
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
                        if (currComm.getNextUnplayedMsj()==null) {
                            Toast.makeText(mContext, "awaiting response from other party", Toast.LENGTH_LONG).show();
                        }else{
                            currComm.startPlaying(currComm.getNextUnplayedMsj(),holder.progressBar);
                            sendAndRegisterOwnAck(new AcknowledgementObject(currComm.getNextUnplayedMsj().getMsjObject(),CommsObject.PLAYED));
                        }
                        break;
                    case  CommsObject.PLAY:
                        //start playing
                        currComm.startPlaying(currComm.getNextUnplayedMsj(),holder.progressBar);
                        sendAndRegisterOwnAck(new AcknowledgementObject(currComm.getNextUnplayedMsj().getMsjObject(),CommsObject.PLAYED));
                        break;
                    case  CommsObject.ACCEPT:
                        //send acceptance
                        MetaMessageObject metaMsjAccept=new MetaMessageObject(CommsObject.ACCEPTED,null,currComm);
                        currComm.setAccepted(true);
                        currComm.addAtTopOfMsjList(metaMsjAccept);
                        MessageObject msjAccept=metaMsjAccept.getMsjObject();
                        attemptSendMsj(msjAccept);
                        connectionLines.playCommAnim(taxiId);
                        commAcceptedListener.onCommAccepted(currComm);
                        break;
                    case  CommsObject.BT_ACCEPTED:
                        //paint btn black
                        if (currComm.getNextUnplayedMsj()==null){
                            Toast.makeText(mContext,"taxi was already accepted", Toast.LENGTH_LONG).show();
                        }else{
                            currComm.startPlaying(currComm.getNextUnplayedMsj(),holder.progressBar);
                            sendAndRegisterOwnAck(new AcknowledgementObject(currComm.getNextUnplayedMsj().getMsjObject(),CommsObject.PLAYED));
                        }
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
                if (currComm.getNextUnplayedMsj()==null) {
                    fab.setImageResource(R.drawable.waiting);
                }else{
                    fab.setImageResource(R.drawable.awaiting_but_playable);
                }
                fab.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(mContext,R.color.colorDeselected)));
                repaintDescription("awaiting response",ContextCompat.getColor(mContext, R.color.colorDeselected),descriptionText,descriptionPointer);
                break;
            case  CommsObject.PLAY:
                //start playing
                ackBubble.setVisibility(View.GONE);
                fab.setImageResource(R.drawable.play_button);
                fab.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(mContext,R.color.colorGreen)));
                repaintDescription("play audio",ContextCompat.getColor(mContext, R.color.colorGreen),descriptionText,descriptionPointer);
                break;
            case  CommsObject.ACCEPT:
                //send acceptance
                ackBubble.setVisibility(View.GONE);
                fab.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(mContext,R.color.colorGreen)));
                fab.setImageResource(R.drawable.confirm);
                repaintDescription("accept",ContextCompat.getColor(mContext, R.color.colorGreen),descriptionText,descriptionPointer);
                break;
            case  CommsObject.BT_ACCEPTED:
                //paint btn black
                if (currComm.getTopOfList().isOutgoing){
                    currComm.getTopOfList().addAckAtTopOfList(new AcknowledgementObject(currComm.getTopOfList().getMsjObject(),CommsObject.SENT));
                    ackBubble.setVisibility(View.VISIBLE);
                }else {
                    ackBubble.setVisibility(View.GONE);
                }
                fab.setBackgroundTintList(ColorStateList.valueOf(Color.BLACK));
                if (currComm.getNextUnplayedMsj()==null) {
                    fab.setImageResource(R.drawable.accepted);
                }else{
                    fab.setImageResource(R.drawable.accepted_but_playable);
                }
                descriptionText.setTypeface(null, Typeface.BOLD);
                repaintDescription("accepted",Color.BLACK,descriptionText,descriptionPointer);
                break;
            default:
                ackBubble.setVisibility(View.GONE);
                fab.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(mContext,R.color.colorBlueDark)));
                fab.setImageResource(R.drawable.send);
                repaintDescription("invite",ContextCompat.getColor(mContext,R.color.colorBlueDark),descriptionText,descriptionPointer);
        }
        fab.hide();
        fab.show();
    }

    private void repaintDescription(String text, int color, TextView descriptionText, TextView descriptionPointer){
        descriptionText.setText(text);
        descriptionText.setTextColor(color);
        descriptionPointer.setTextColor(color);
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
                    ackRecording.setVisibility(View.VISIBLE);
                    ackFailed.setVisibility(View.GONE);
                    ackFailedText.setVisibility(View.GONE);
                    if (currCom.findMsjById(ack.getMsgId()).getAudioFile()==null){
                        ackPlayed.setVisibility(View.GONE);
                        ackHeard.setVisibility(View.GONE);
                    }else{
                        ackPlayed.setVisibility(View.VISIBLE);
                        ackHeard.setVisibility(View.VISIBLE);
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
                        Log.d("testes","new msj inv "+msj.getIntentCode());
                        newIncomingComms.add(msj);
                        messageInvitationListener.onInvitationReceived(msj);
                    }else{//comm is already there
                        if (msj.getIntentCode()!=CommsObject.REJECTED) {
                            CommsObject comm = mComms.get(getCommIndex(id));
                            if (msj.getIntentCode()==CommsObject.ACCEPTED){
                                comm.setAccepted(true);
                                Log.d("socketTest","codemsj0 "+msj.getIntentCode());
                                commAcceptedListener.onCommAccepted(comm);
                            }
                            Log.d("socketTest","codemsj "+msj.getIntentCode());
                            MetaMessageObject metaMsj = new MetaMessageObject(msj, comm);
                            mComms.get(getCommIndex(id)).addAtTopOfMsjList(metaMsj);
                        }else{
                            processCancellations(msj);
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

    //elegant cancellations framework. (consider just ignoring, alternative isn't that bad)
    List<MessageObject> mToBeCancelled=new ArrayList<>();
    boolean isRecording=false;
    String whoIsRecording=null;
    private void processCancellations(MessageObject msj){
        if (!isRecording || whoIsRecording.equals(msj.getSendingId())){
            messageCancellationListener.onCancellationReceived(msj);
        }else{
            mToBeCancelled.add(msj);
        }
    }

    private void resolvePendingCancellations(){
        if (mToBeCancelled.size()>0){
            Iterator<MessageObject> i = mToBeCancelled.iterator();
            while (i.hasNext()) {
                MessageObject cancellation = i.next();
                messageCancellationListener.onCancellationReceived(cancellation);
                i.remove();
            }
        }
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

    //callback for when new message invitation is received
    public interface MessageInvitationListener{
        void onInvitationReceived(MessageObject msj);
    }

    MessageInvitationListener messageInvitationListener;

    public void setMessageInvitationListener(MessageInvitationListener messageInvitationListener) {
        this.messageInvitationListener = messageInvitationListener;
    }

    //callback for when message is cancelled
    public interface MessageCancellationListener{
        void onCancellationReceived(MessageObject msj);
    }

    MessageCancellationListener messageCancellationListener;

    public void setMessageCancellationListener(MessageCancellationListener messageCancellationListener) {
        this.messageCancellationListener = messageCancellationListener;
    }

    //callback for when comm was accepted
    public interface CommAcceptedListener{
        void onCommAccepted(CommsObject comm);
    }

    CommAcceptedListener commAcceptedListener;

    public void setCommAcceptedListener(CommAcceptedListener commAcceptedListener) {
        this.commAcceptedListener = commAcceptedListener;
    }

    //multiple methods
    public static void resetMediaProgressBar(CircularProgressBar circularProgressBar){
        circularProgressBar.setProgress(0.0f);
        circularProgressBar.setVisibility(View.INVISIBLE);
    }
}
