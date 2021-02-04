package com.example.android.taxitest;

import android.content.Context;
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.android.taxitest.utils.CustomTextView;

import java.util.ArrayList;

public class HelpVideoAdapter extends RecyclerView.Adapter<HelpVideoAdapter.ViewHolder> {


    ArrayList<HelpVideoObject> videoList;
    Context mContext;
    public HelpVideoAdapter(ArrayList<HelpVideoObject> videoList, Context context) {
        this.videoList=videoList;
        mContext=context;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        HelpVideoObject video;
        CustomTextView nameShort;
        TextView nameLong;
        ImageButton playButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            nameShort=itemView.findViewById(R.id.ct_name_short);
            nameLong=itemView.findViewById(R.id.tv_name_long);
            playButton=itemView.findViewById(R.id.ib_play_button);
        }

    }

    @NonNull
    @Override
    public HelpVideoAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater=LayoutInflater.from(mContext);

        //now inflate the view
        View videoView=inflater.inflate(R.layout.adapter_help_video,parent,false);

        return new ViewHolder(videoView);
    }

    @Override
    public void onBindViewHolder(@NonNull HelpVideoAdapter.ViewHolder holder, int position) {
        holder.video=videoList.get(position);
        holder.nameShort.setText(holder.video.shortName);
        holder.nameLong.setText(holder.video.longName);
        if (holder.video.isPlaying){
            holder.playButton.setAlpha(0.9f);
            holder.playButton.setColorFilter(ContextCompat.getColor(mContext,R.color.colorAccent));
        }else{
            holder.playButton.setAlpha(0.4f);
            holder.playButton.setColorFilter(ContextCompat.getColor(mContext,R.color.colorDeselected));
        }

        holder.playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mVideoClickedListener.onVideoClicked(holder.video);
            }
        });

    }

    @Override
    public int getItemCount() {
        return videoList.size();
    }

    //callback interface
    public interface VideoClickedListener{
        void onVideoClicked(HelpVideoObject videoObject);
    }

    VideoClickedListener mVideoClickedListener;

    public void setVideoClickedListener(VideoClickedListener videoClickedListener){
        mVideoClickedListener=videoClickedListener;
    }

    String videoCurrentlyPlayingUrl=null;

    public HelpVideoObject getVideoByUrl(String url){
        for (HelpVideoObject video:videoList){
            if (video.youtubeUrlExt.contentEquals(url))
                return video;
        }
        return null;
    }

    public void setVideoCurrentlyPlaying(String url){
        if (videoCurrentlyPlayingUrl!=null){
            //flag old video as stopped
            HelpVideoObject oldVideo=getVideoByUrl(videoCurrentlyPlayingUrl);
            oldVideo.isPlaying=false;
            notifyItemChanged(videoList.indexOf(oldVideo));
        }
        if (url!=null){
            //flag new video as starting
            HelpVideoObject newVideo=getVideoByUrl(url);
            newVideo.isPlaying=true;
            notifyItemChanged(videoList.indexOf(newVideo));
        }
        videoCurrentlyPlayingUrl=url;
    }
}
