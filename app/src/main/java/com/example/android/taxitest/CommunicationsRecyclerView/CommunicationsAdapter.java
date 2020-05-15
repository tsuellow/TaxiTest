package com.example.android.taxitest.CommunicationsRecyclerView;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;


import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.android.taxitest.Constants;
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
    private MediaPlayer player = null;
    ConnectionLineLayer2 connectionLines;
    public List<MessageObject> newIncomingComms=new ArrayList<>();


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

        //Log.d("socketTest",mSocket.id());

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
            CommsObject m = i.next();
            if (m.taxiMarker.taxiObject.getTaxiId() == taxiId) {
                int index=mComms.indexOf(m);
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

    boolean isPlaying=false;
    private void startPlaying(final MetaMessageObject msj, final CircularProgressBar circularProgressBar) {
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

                CommsObject comm=mComms.get(getCommIndex(taxiId));
                stopPlaying();
                isPlaying=false;
                circularProgressBar.setProgress(0.0f);
                circularProgressBar.setVisibility(View.INVISIBLE);
                msj.setWasPlayed(true);
                msj.comm.performUpdates();

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
            Log.e(LOG_TAG, "prepare() failed");
            isPlaying=false;
        }

    }

    private void stopPlaying() {
        player.release();
        player = null;
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
        private ImageView ackCheck, ackPlayed, ackHeard, ackRecording;

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
        repaintButton(comm,holder.confirmOrPlay);


        comm.setMsjUpdateListener(new CommsObject.MsjUpdateListener() {
            @Override
            public void onMsjUpdateReceived(int intentCode) {
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        Log.d("socketTest","is being executed");
                        CommsObject currComm=mComms.get(getCommIndex(taxiId));
                        repaintButton(currComm,holder.confirmOrPlay);
                    }
                });
                //notifyItemChanged(getCommIndex(taxiId));
            }
        });

        holder.recordButton.setRecordingFinishedListener(new RecordButton.RecordingFinishedListener() {
            @Override
            public void onRecordingFinished(File file) {
                CommsObject currComm=mComms.get(getCommIndex(taxiId));
                MetaMessageObject metaMsj=new MetaMessageObject(CommsObject.REQUEST_SENT,file,currComm);
                currComm.addAtTopOfMsjList(metaMsj);
                MessageObject msj=metaMsj.getMsjObject();
                attemptSend(msj);
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
                        MessageObject msj=metaMsj.getMsjObject();
                        attemptSend(msj);
                        connectionLines.playCommAnim(taxiId);
                        break;
                    case  CommsObject.AWAITING:
                        //do nothing, only toast/play own msg
                        Toast.makeText(mContext,"awaiting response from other party", Toast.LENGTH_LONG).show();
                        break;
                    case  CommsObject.PLAY:
                        //start playing
                        startPlaying(currComm.getNextUnplayedMsj(),holder.progressBar);
                        break;
                    case  CommsObject.ACCEPT:
                        //send acceptance
                        MetaMessageObject metaMsjAccept=new MetaMessageObject(CommsObject.ACCEPTED,null,currComm);
                        currComm.addAtTopOfMsjList(metaMsjAccept);
                        MessageObject msjAccept=metaMsjAccept.getMsjObject();
                        attemptSend(msjAccept);
                        connectionLines.playCommAnim(taxiId);
                        break;
                    case  CommsObject.BT_ACCEPTED:
                        //paint btn black
                        Toast.makeText(mContext,"taxi was already accepted", Toast.LENGTH_LONG).show();
                        break;
                }
            }
        });


    }


    public void repaintButton(CommsObject currComm, FloatingActionButton fab){
        switch (currComm.getButtonCode()){
            case  CommsObject.AWAITING:
                //do nothing, only toast/play own msg
                fab.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(mContext,R.color.colorDeselected)));
                fab.setImageResource(R.drawable.waiting);
                break;
            case  CommsObject.PLAY:
                //start playing
                fab.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(mContext,R.color.colorGreen)));
                fab.setImageResource(R.drawable.play_button);
                break;
            case  CommsObject.ACCEPT:
                //send acceptance
                fab.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(mContext,R.color.colorGreen)));
                fab.setImageResource(R.drawable.confirm);
                break;
            case  CommsObject.BT_ACCEPTED:
                //paint btn black
                fab.setImageResource(R.drawable.confirm);
                fab.setBackgroundTintList(ColorStateList.valueOf(Color.BLACK));
                break;
            default:
                fab.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(mContext,R.color.colorGreen)));
                fab.setImageResource(R.drawable.send);
        }
        fab.hide();
        fab.show();
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
    private Socket mSocket;
    Emitter.Listener onChatReceived;
    Emitter.Listener onSocketsConnected;

    public void connectSocket(){
        mSocket.on("audio chat",onChatReceived);
        mSocket.on("sockets connected",onSocketsConnected);
        mSocket.connect();
    }

    public void disconnectSocket(){
        mSocket.off("audio chat",onChatReceived);
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


    public void attemptSend(MessageObject msj){
        JSONObject payload=msj.generateJson();
        mSocket.emit("audio chat", payload);

        //Log.d("socketTest",audioString.substring(0,100));
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
}
