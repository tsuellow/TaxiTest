package com.example.android.taxitest.CommunicationsRecyclerView;

import android.animation.TimeInterpolator;
import android.app.Instrumentation;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
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
import androidx.transition.Visibility;

import com.example.android.taxitest.Constants;
import com.example.android.taxitest.R;
import com.example.android.taxitest.RecordButtonUtils.RecordButton;
import com.example.android.taxitest.utils.MiscellaneousUtils;
import com.example.android.taxitest.vectorLayer.ConnectionLineLayer2;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;
import com.google.android.gms.common.util.IOUtils;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.mikhaellopez.circularprogressbar.CircularProgressBar;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Date;
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

    public void addItem(CommsObject commsObject){
        mComms.add(commsObject);
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
    private void startPlaying(File file, final CircularProgressBar circularProgressBar) {
        if (isPlaying){
            player.stop();
            circularProgressBar.setProgress(0.0f);
            circularProgressBar.setVisibility(View.INVISIBLE);
        }
        //Toast.makeText(mContext,""+file.length()/1024.0+" kb",Toast.LENGTH_LONG).show();
        player = new MediaPlayer();
        player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                stopPlaying();
                isPlaying=false;
                circularProgressBar.setProgress(0.0f);
                circularProgressBar.setVisibility(View.INVISIBLE);
            }
        });
        try {
            player.setDataSource(file.getAbsolutePath());
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
        CommsObject comm=mComms.get(position);
        final int taxiId=comm.taxiMarker.taxiObject.getTaxiId();
        holder.name.setText("ID: "+comm.taxiMarker.taxiObject.getTaxiId());
        holder.numberPlate.setText("T"+comm.taxiMarker.taxiObject.getTaxiId());
        holder.destination.setText("barrio: "+comm.taxiMarker.color);
        holder.destColor.setCardBackgroundColor(comm.taxiMarker.color);

        comm.setMsjUpdateListener(new CommsObject.MsjUpdateListener() {
            @Override
            public void onMsjUpdateReceived(int intentCode) {
                //blablabla
                Log.d("socketTest","code2 "+intentCode);
                holder.confirmOrPlay.setImageResource(R.drawable.play_button);
                holder.confirmOrPlay.refreshDrawableState();
            }
        });

        holder.recordButton.setRecordingFinishedListener(new RecordButton.RecordingFinishedListener() {
            @Override
            public void onRecordingFinished(File file) {
                CommsObject currComm=mComms.get(getCommIndex(taxiId));
                MetaMessageObject metaMsj=new MetaMessageObject(CommsObject.REQUEST_SENT,file,currComm);
                currComm.addAtTopOfMsjList(metaMsj);
                MessageObject msj=metaMsj.getMsjObject();
                JSONObject payload=msj.generateJson();
                attemptSend(payload);
            }
        });

        holder.confirmOrPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Toast.makeText(mContext,"confirm or play media"+mComms.get(getCommIndex(taxiId)).getTopOfList().getAbsolutePath(),Toast.LENGTH_LONG).show();
                CommsObject currComm=mComms.get(getCommIndex(taxiId));
                startPlaying(currComm.getTopOfMsjList().getAudioFile(),holder.progressBar);
                connectionLines.playCommAnim(taxiId);
            }
        });


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
                    String idString=msj.getSendingId();
                    int id = Integer.parseInt(idString.substring(1));
                    if (getCommIndex(id)==-1){
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


    public void attemptSend(JSONObject audioString){
        mSocket.emit("audio chat", audioString);

        //Log.d("socketTest",audioString.substring(0,100));
    }

    public interface MessageInvitationListener{
        void onInvitationReceived(MessageObject msj);
    }

    MessageInvitationListener messageInvitationListener;

    public void setMessageInvitationListener(MessageInvitationListener messageInvitationListener) {
        this.messageInvitationListener = messageInvitationListener;
    }
}
