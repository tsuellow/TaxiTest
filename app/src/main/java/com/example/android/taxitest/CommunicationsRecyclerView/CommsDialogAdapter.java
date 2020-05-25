package com.example.android.taxitest.CommunicationsRecyclerView;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.android.taxitest.R;
import com.example.android.taxitest.utils.MiscellaneousUtils;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.mikhaellopez.circularprogressbar.CircularProgressBar;

import java.io.IOException;
import java.util.List;

public class CommsDialogAdapter extends RecyclerView.Adapter<CommsDialogAdapter.ViewHolder> {

    Context mContext;
    CommsObject comm;

    public CommsDialogAdapter(Context context, CommsObject comm) {
        mContext=context;
        this.comm=comm;
    }

    public class ViewHolder extends RecyclerView.ViewHolder{

        private int adapterPosition;
        private TextView time;
        private CircularProgressBar progressBar;
        private FloatingActionButton confirmOrPlay;
        private LinearLayout ackBubble;
        private ImageView ackCheck, ackPlayed, ackHeard, ackRecording, ackFailed;
        RelativeLayout parent;


        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            time = (TextView) itemView.findViewById(R.id.tv_time_of_msg_dialog);
            progressBar=(CircularProgressBar) itemView.findViewById(R.id.play_progress_bar_dialog);
            confirmOrPlay = (FloatingActionButton) itemView.findViewById(R.id.bt_confirm_dialog);

            //acknowledgements
            ackBubble=(LinearLayout) itemView.findViewById(R.id.ack_bubble_dialog);
            ackCheck=(ImageView) itemView.findViewById(R.id.ack_check_dialog);
            ackPlayed=(ImageView) itemView.findViewById(R.id.ack_played_dialog);
            ackHeard=(ImageView) itemView.findViewById(R.id.ack_heard_dialog);
            ackFailed=(ImageView) itemView.findViewById(R.id.ack_failed_dialog);

            parent=(RelativeLayout) itemView.findViewById(R.id.parent_layout_dialog);
        }


    }

    @NonNull
    @Override
    public CommsDialogAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater=LayoutInflater.from(mContext);

        //now inflate the view
        View commsDialogView=inflater.inflate(R.layout.comms_dialog_adapter,parent,false);
        CommsDialogAdapter.ViewHolder viewHolder=new CommsDialogAdapter.ViewHolder(commsDialogView);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull final CommsDialogAdapter.ViewHolder holder, int position) {
        final MetaMessageObject msj=comm.getMsjList().get(position);
//        setNewReproductionListener(new NewReproductionListener() {
//            @Override
//            public void onNewReproduction(String msjId) {
//                if (!msjId.equals(msj.getMsjObject().getMsgId())){
//                    holder.progressBar.setProgress(0.0f);
//                    holder.progressBar.setVisibility(View.INVISIBLE);
//                }
//            }
//        });
        if(msj.isOutgoing){
            holder.parent.setGravity(Gravity.END);
            holder.confirmOrPlay.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(mContext,R.color.colorBlue)));
        }else{
            holder.parent.setGravity(Gravity.START);
            holder.confirmOrPlay.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(mContext,R.color.colorGreen)));
        }

        holder.time.setText(MiscellaneousUtils.convertTime(msj.getMsjObject().getTimestamp()));

        paintButtons(msj,holder.confirmOrPlay,holder.ackCheck,holder.ackPlayed,holder.ackHeard,holder.ackFailed);
        paintAck(msj,holder.ackCheck,holder.ackPlayed,holder.ackHeard,holder.ackFailed);

        holder.confirmOrPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (msj.getAudioFile()!=null){
                    startPlaying(msj,holder.progressBar);
                }
            }
        });

    }

    private void paintButtons(MetaMessageObject msj, FloatingActionButton fab, ImageView ackCheck, ImageView ackPlayed, ImageView ackHeard, ImageView ackFailed){
        ackFailed.setVisibility(View.GONE);
        if (msj.getAudioFile()==null){
            ackCheck.setVisibility(View.VISIBLE);
            ackPlayed.setVisibility(View.GONE);
            ackHeard.setVisibility(View.GONE);
            if (msj.getMsjObject().getIntentCode()!=CommsObject.ACCEPT){
                fab.setImageResource(R.drawable.send);
            }else{
                fab.setImageResource(R.drawable.accepted);
                fab.setBackgroundTintList(ColorStateList.valueOf(Color.BLACK));
            }
        }else{
            fab.setImageResource(R.drawable.play_button);
            ackCheck.setVisibility(View.VISIBLE);
            ackPlayed.setVisibility(View.VISIBLE);
            ackHeard.setVisibility(View.VISIBLE);
        }

    }

    private  void paintAck(MetaMessageObject msj, ImageView ackCheck, ImageView ackPlayed, ImageView ackHeard, ImageView ackFailed){
        ackCheck.setColorFilter(Color.WHITE);
        ackPlayed.setColorFilter(Color.WHITE);
        ackHeard.setColorFilter(Color.WHITE);
        switch (msj.getTopAck()){
            case CommsObject.RECEIVED:
                ackCheck.setColorFilter(ContextCompat.getColor(mContext, R.color.colorBlueAcc));
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
                ackFailed.setVisibility(View.VISIBLE);
                break;
        }
    }

    @Override
    public int getItemCount() {
        return comm.getMsjList().size();
    }


    //CommsObject media player
    private MediaPlayer player = null;
    boolean isPlaying=false;
    public void startPlaying(final MetaMessageObject msj, final CircularProgressBar circularProgressBar) {
        if (isPlaying){
            player.stop();
            circularProgressBar.setProgress(0.0f);
            circularProgressBar.setVisibility(View.INVISIBLE);
            newReproductionListener.onNewReproduction(msj.getMsjObject().getMsgId());
        }

        player = new MediaPlayer();
        player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                stopPlaying();
                circularProgressBar.setProgress(0.0f);
                circularProgressBar.setVisibility(View.INVISIBLE);

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

    public interface NewReproductionListener{
        void onNewReproduction(String msjId);
    }

    NewReproductionListener newReproductionListener;

    public void setNewReproductionListener(NewReproductionListener newReproductionListener) {
        this.newReproductionListener = newReproductionListener;
    }
}


