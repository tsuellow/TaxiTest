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
import com.dale.viaje.nicaragua.data.CommRecordObject;
import com.dale.viaje.nicaragua.data.MsjRecordObject;
import com.dale.viaje.nicaragua.utils.MiscellaneousUtils;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.mikhaellopez.circularprogressbar.CircularProgressBar;

import java.io.IOException;
import java.util.List;

public class CommsRecDialogAdapter extends RecyclerView.Adapter<CommsRecDialogAdapter.ViewHolder> {
    Context mContext;
    CommRecordObject mComm;
    List<MsjRecordObject> msjList;

    public CommsRecDialogAdapter(Context context, CommRecordObject comm) {
        mContext=context;
        mComm=comm;
    }

    public void setMsjs(List<MsjRecordObject> msjs){
        msjList=msjs;
        notifyDataSetChanged();
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
    public CommsRecDialogAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater=LayoutInflater.from(mContext);

        //now inflate the view
        View commsDialogView=inflater.inflate(R.layout.comms_dialog_adapter,parent,false);
        CommsRecDialogAdapter.ViewHolder viewHolder=new CommsRecDialogAdapter.ViewHolder(commsDialogView);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull final CommsRecDialogAdapter.ViewHolder holder, int position) {
        final MsjRecordObject msj=msjList.get(position);
        if(msj.getIsOutgoing()==1){
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

        holder.time.setText(MiscellaneousUtils.convertTime(msj.getTimestamp()));
        holder.time.setTypeface(Typeface.defaultFromStyle(Typeface.ITALIC));

        final String toastButton=paintButtons(msj,holder.confirmOrPlay,holder.ackCheck,holder.ackPlayed,holder.ackHeard,holder.ackFailed);
        final String toastAck=paintAck(msj,holder.ackCheck,holder.ackPlayed,holder.ackHeard,holder.ackFailed);




        holder.confirmOrPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (msj.getFilePath()!=null){
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

    private String paintButtons(MsjRecordObject msj, FloatingActionButton fab, ImageView ackCheck, ImageView ackPlayed, ImageView ackHeard, ImageView ackFailed){
        String toast="";
        ackFailed.setVisibility(View.GONE);
        if (msj.getFilePath()==null){
            ackCheck.setVisibility(View.VISIBLE);
            ackPlayed.setVisibility(View.GONE);
            ackHeard.setVisibility(View.GONE);
            if (msj.getIntentCode()!=CommsObject.ACCEPT){
                fab.setImageResource(R.drawable.send);
                toast=msj.getIsOutgoing()==1?mContext.getString(R.string.commsdialogadapter_invitationbyyou):mContext.getString(R.string.commsdialogadapter_invitationbyother)+" "+mComm.getFirstName();
            }else{
                fab.setImageResource(R.drawable.accepted);
                fab.setBackgroundTintList(ColorStateList.valueOf(Color.BLACK));
                toast=msj.getIsOutgoing()==1?mContext.getString(R.string.commsdialogadapter_acceptedbyyou):mContext.getString(R.string.commsdialogadapter_acceptedbyother)+" "+mComm.getFirstName();
            }
        }else{
            fab.setImageResource(R.drawable.play_button);
            ackCheck.setVisibility(View.VISIBLE);
            ackPlayed.setVisibility(View.VISIBLE);
            ackHeard.setVisibility(View.VISIBLE);
            toast=msj.getIsOutgoing()==1?mContext.getString(R.string.commsdialogadapter_audiobyyou):mContext.getString(R.string.commsdialogadapter_audiobyother)+" "+mComm.getFirstName();
        }
        return toast;

    }

    private String paintAck(MsjRecordObject msj, ImageView ackCheck, ImageView ackPlayed, ImageView ackHeard, ImageView ackFailed){
        StringBuilder toast= new StringBuilder(msjList.size() == 0 ? mContext.getString(R.string.commsdialogadapter_nothingtoshow) : "");
        ackCheck.setColorFilter(Color.WHITE);
        ackPlayed.setColorFilter(Color.WHITE);
        ackHeard.setColorFilter(Color.WHITE);
        if (msj.getReceived()!=0) {
            ackCheck.setColorFilter(ContextCompat.getColor(mContext, R.color.colorBlueAcc));
        }
        if (msj.getPlayed()!=0) {
            ackCheck.setColorFilter(ContextCompat.getColor(mContext, R.color.colorBlueAcc));
            ackPlayed.setColorFilter(ContextCompat.getColor(mContext, R.color.colorBlueAcc));
        }
        if (msj.getHeard()!=0) {
            ackCheck.setColorFilter(ContextCompat.getColor(mContext, R.color.colorBlueAcc));
            ackPlayed.setColorFilter(ContextCompat.getColor(mContext, R.color.colorBlueAcc));
            ackHeard.setColorFilter(ContextCompat.getColor(mContext, R.color.colorBlueAcc));
        }
        if (msj.getMsjStatus()==0) {
            ackCheck.setVisibility(View.GONE);
            ackPlayed.setVisibility(View.GONE);
            ackHeard.setVisibility(View.GONE);
            ackFailed.setVisibility(View.VISIBLE);
        }

        if (msj.getMsjStatus()==1) {
            String received=mContext.getString(R.string.commsdialogadapter_received)+" "+MiscellaneousUtils.convertTime(msj.getReceived())+"\n";
            String played=mContext.getString(R.string.commsdialogadapter_played)+" "+MiscellaneousUtils.convertTime(msj.getPlayed())+"\n";
            String heard=mContext.getString(R.string.commsdialogadapter_heard)+" "+MiscellaneousUtils.convertTime(msj.getHeard())+"\n";
            if (msj.getFilePath()==null){
                toast.append(received);
            }else{
                toast.append(received).append(played).append(heard);
            }

        }else {
            String failed=mContext.getString(R.string.commsdialogadapter_failed)+" "+MiscellaneousUtils.convertTime(msj.getTimestamp());
            toast.append(failed);
        }

        return toast.toString();

    }

    @Override
    public int getItemCount() {
        return msjList.size();
    }


    //CommsObject media player
    private MediaPlayer player = null;
    boolean isPlaying=false;
    CircularProgressBar currentlyMovingBar=null;
    public void startPlaying(final MsjRecordObject msj, final CircularProgressBar circularProgressBar) {
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
            player.setDataSource(msj.getFilePath());
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
