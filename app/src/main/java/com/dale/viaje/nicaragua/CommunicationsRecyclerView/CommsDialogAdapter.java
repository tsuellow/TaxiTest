package com.dale.viaje.nicaragua.CommunicationsRecyclerView;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.recyclerview.widget.RecyclerView;

import com.dale.viaje.nicaragua.R;
import com.dale.viaje.nicaragua.utils.MiscellaneousUtils;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.mikhaellopez.circularprogressbar.CircularProgressBar;

import java.io.IOException;

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
        LinearLayout group;


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
            group=(LinearLayout) itemView.findViewById(R.id.group_layout);
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
        if(msj.isOutgoing){
            holder.parent.setGravity(Gravity.END);
            holder.confirmOrPlay.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(mContext,R.color.colorBlue)));
            holder.group.getBackground().setTint(ColorUtils.setAlphaComponent(ContextCompat.getColor(mContext,R.color.colorBlue),100));
            holder.progressBar.setProgressBarColor(ContextCompat.getColor(mContext,R.color.colorBlueDark));
        }else{
            holder.parent.setGravity(Gravity.START);
            holder.confirmOrPlay.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(mContext,R.color.colorGreen)));
            holder.group.getBackground().setTint(ColorUtils.setAlphaComponent(ContextCompat.getColor(mContext,R.color.colorGreen),100));
            holder.progressBar.setProgressBarColor(ContextCompat.getColor(mContext,R.color.colorGreenDark));
        }

        holder.time.setText(MiscellaneousUtils.convertTime(msj.getMsjObject().getTimestamp()));
        holder.time.setTypeface(Typeface.defaultFromStyle(Typeface.ITALIC));

        final String toastButton=paintButtons(msj,holder.confirmOrPlay,holder.ackCheck,holder.ackPlayed,holder.ackHeard,holder.ackFailed);
        final String toastAck=paintAck(msj,holder.ackCheck,holder.ackPlayed,holder.ackHeard,holder.ackFailed);




        holder.confirmOrPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (msj.getAudioFile()!=null){
                    startPlaying(msj,holder.progressBar);
                    holder.progressBar.setVisibility(View.VISIBLE);
                }
                Toast.makeText(mContext,toastButton,Toast.LENGTH_SHORT).show();
            }
        });

        holder.ackBubble.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(mContext,toastAck.trim(),Toast.LENGTH_LONG).show();
            }
        });

    }

    private String paintButtons(MetaMessageObject msj, FloatingActionButton fab, ImageView ackCheck, ImageView ackPlayed, ImageView ackHeard, ImageView ackFailed){
        String toast="";
        ackFailed.setVisibility(View.GONE);
        if (msj.getAudioFile()==null){
            ackCheck.setVisibility(View.VISIBLE);
            ackPlayed.setVisibility(View.GONE);
            ackHeard.setVisibility(View.GONE);
            if (msj.getMsjObject().getIntentCode()!=CommsObject.ACCEPT){
                fab.setImageResource(R.drawable.send);
                toast=msj.isOutgoing?mContext.getString(R.string.commsdialogadapter_invitationbyyou):mContext.getString(R.string.commsdialogadapter_invitationbyother)+" "+comm.commCardData.firstName;
            }else{
                fab.setImageResource(R.drawable.accepted);
                fab.setBackgroundTintList(ColorStateList.valueOf(Color.BLACK));
                toast=msj.isOutgoing?mContext.getString(R.string.commsdialogadapter_acceptedbyyou):mContext.getString(R.string.commsdialogadapter_acceptedbyother)+" "+comm.commCardData.firstName;
            }
        }else{
            fab.setImageResource(R.drawable.play_button);
            ackCheck.setVisibility(View.VISIBLE);
            ackPlayed.setVisibility(View.VISIBLE);
            ackHeard.setVisibility(View.VISIBLE);
            toast=msj.isOutgoing?mContext.getString(R.string.commsdialogadapter_audiobyyou):mContext.getString(R.string.commsdialogadapter_audiobyother)+" "+comm.commCardData.firstName;
        }
        return toast;

    }

    private String paintAck(MetaMessageObject msj, ImageView ackCheck, ImageView ackPlayed, ImageView ackHeard, ImageView ackFailed){
        StringBuilder toast= new StringBuilder(msj.getAckList().size() == 0 ? mContext.getString(R.string.commsdialogadapter_nothingtoshow) : "");
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

        for (AcknowledgementObject ack:msj.getAckList()){
            String part="";
            switch (ack.getAckCode()){
                case CommsObject.RECEIVED:
                    part=mContext.getString(R.string.commsdialogadapter_received)+" "+MiscellaneousUtils.convertTime(ack.getTimestamp());
                    break;
                case CommsObject.PLAYED:
                    part=mContext.getString(R.string.commsdialogadapter_played)+" "+MiscellaneousUtils.convertTime(ack.getTimestamp());
                    break;
                case CommsObject.HEARD:
                    part=mContext.getString(R.string.commsdialogadapter_heard)+" "+MiscellaneousUtils.convertTime(ack.getTimestamp());
                    break;
                case CommsObject.FAILED:
                    part=mContext.getString(R.string.commsdialogadapter_failed)+" "+MiscellaneousUtils.convertTime(ack.getTimestamp());
                    break;
            }
            toast.append(part).append("\n");
        }
        return toast.toString();
    }

    @Override
    public int getItemCount() {
        return comm.getMsjList().size();
    }


    //CommsObject media player
    private MediaPlayer player = null;
    boolean isPlaying=false;
    CircularProgressBar currentlyMovingBar=null;
    public void startPlaying(final MetaMessageObject msj, final CircularProgressBar circularProgressBar) {
        if (isPlaying){
            player.stop();
            currentlyMovingBar.setProgress(0.0f);
            currentlyMovingBar.setVisibility(View.INVISIBLE);
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
            circularProgressBar.setProgress(0.0f);
            circularProgressBar.setVisibility(View.VISIBLE);
            player.start();
            circularProgressBar.setProgressWithAnimation(100.0f,(long) player.getDuration(),new LinearInterpolator());
            isPlaying=true;
            currentlyMovingBar=circularProgressBar;
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


}


