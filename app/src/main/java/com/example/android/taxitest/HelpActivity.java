package com.example.android.taxitest;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;
import android.widget.Toolbar;

import com.example.android.taxitest.utils.CustomTextView;
import com.example.android.taxitest.utils.GenericDialogUtils;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.YouTubePlayerListener;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.ui.views.YouTubePlayerSeekBar;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.ui.views.YouTubePlayerSeekBarListener;

import org.jetbrains.annotations.NotNull;

public class HelpActivity extends AppCompatActivity {

    Context mContext;
    YouTubePlayer player;
    ImageButton fullScreen, closeScreen, whatsApp, googlePlay;
    CardView videoScreen;
    CustomTextView screenTitle;
    YouTubePlayerView youTubePlayerView;
    YouTubePlayerSeekBar seekBar;
    boolean isFullScreen=false;
    HelpVideoAdapter adapter;
    RecyclerView recyclerViewVideos;
    Button readTerms, readDataUse;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help);

        mContext=this;
        readTerms=findViewById(R.id.bt_terms);
        readDataUse=findViewById(R.id.bt_data_use);
        whatsApp=findViewById(R.id.ib_whastapp);
        googlePlay=findViewById(R.id.ib_googleplay);



        setupScreen();
        setupDocuments();
        setupSuggestions();
//        Display display=getWindowManager().getDefaultDisplay();
//        Point output=new Point();
//        display.getSize(output);

        recyclerViewVideos=(RecyclerView) findViewById(R.id.rv_help_videos);
        adapter=new HelpVideoAdapter(new HelpVideos().getVideoList(),this);
        GridLayoutManager manager=new GridLayoutManager(this,2,GridLayoutManager.VERTICAL,false);
        recyclerViewVideos.setLayoutManager(manager);
        recyclerViewVideos.setAdapter(adapter);


        adapter.setVideoClickedListener(new HelpVideoAdapter.VideoClickedListener() {
            @Override
            public void onVideoClicked(HelpVideoObject videoObject) {
                if (player!=null){
                    videoScreen.setVisibility(View.VISIBLE);
                    player.loadVideo(videoObject.youtubeUrlExt,0);
                    adapter.setVideoCurrentlyPlaying(videoObject.youtubeUrlExt);
                    screenTitle.setText(videoObject.shortName);
                    makeScreenSmall();
                }else{
                    Toast.makeText(getApplicationContext(),"wait a moment while player loads and try again", Toast.LENGTH_LONG).show();
                }

            }
        });



//        fullScreen.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                if (!isFullScreen){
//                    ViewGroup.LayoutParams params= videoScreen.getLayoutParams();
//                    params.width=ViewGroup.LayoutParams.MATCH_PARENT;
//                    params.height=ViewGroup.LayoutParams.MATCH_PARENT;
//                    videoScreen.setLayoutParams(params);
////                    int[] point = new int[2];
////                    cardView.getLocationOnScreen(point); // or getLocationInWindow(point)
////                    int x = point[0];
////                    int y = point[1];
////                    cardView.setPivotX(x+cardView.getWidth()/(2*1.5f));
////                    cardView.setPivotY(y+cardView.getHeight()/(2*1.5f));
////                    Log.d("pivot",""+cardView.getPivotX()+", "+cardView.getPivotY());
////                    cardView.setLayoutParams(new ConstraintLayout.LayoutParams(cardView.getLayoutParams().MATCH_PARENT,cardView.getLayoutParams().MATCH_PARENT));
//                    isFullScreen=true;
//                }else{
//                    Display display=getWindowManager().getDefaultDisplay();
//                    Point output=new Point();
//                    display.getSize(output);
//                    int w=(int) (70*87*output.x/10000);
//                    int h=(int) (70*output.y/100);
//                    ViewGroup.LayoutParams params= videoScreen.getLayoutParams();
//                    params.width=w;
//                    params.height=h;
//                    videoScreen.setLayoutParams(params);
////                    cardView.setScaleX(1f);
////                    cardView.setScaleY(1f);
//                    isFullScreen=false;
//                }
//                Log.d("isfull",""+isFullScreen);
//
//            }
//        });






    }

    private  void setupScreen(){
        videoScreen =(CardView) findViewById(R.id.cv_video_container);
        screenTitle=(CustomTextView) findViewById(R.id.ct_title);
        fullScreen=(ImageButton) findViewById(R.id.ib_fullscreen);
        closeScreen=(ImageButton) findViewById(R.id.ib_close);
        youTubePlayerView = findViewById(R.id.yp_view);
        seekBar= (YouTubePlayerSeekBar) findViewById(R.id.sb_video_progress);
        getLifecycle().addObserver(youTubePlayerView);



        youTubePlayerView.addYouTubePlayerListener(new AbstractYouTubePlayerListener() {
            @Override
            public void onReady(@NonNull YouTubePlayer youTubePlayer) {

                player =youTubePlayer;
                player.addListener(seekBar);
                seekBar.setYoutubePlayerSeekBarListener(new YouTubePlayerSeekBarListener() {
                    @Override
                    public void seekTo(float v) {
                        player.seekTo(v);
                    }
                });

                player.addListener(new YouTubePlayerListener() {
                    @Override
                    public void onReady(@NotNull YouTubePlayer youTubePlayer) {

                    }

                    @Override
                    public void onStateChange(@NotNull YouTubePlayer youTubePlayer, @NotNull PlayerConstants.PlayerState playerState) {
                        if (playerState==PlayerConstants.PlayerState.ENDED){
                            makeScreenClose();
                        }
                    }

                    @Override
                    public void onPlaybackQualityChange(@NotNull YouTubePlayer youTubePlayer, @NotNull PlayerConstants.PlaybackQuality playbackQuality) {

                    }

                    @Override
                    public void onPlaybackRateChange(@NotNull YouTubePlayer youTubePlayer, @NotNull PlayerConstants.PlaybackRate playbackRate) {

                    }

                    @Override
                    public void onError(@NotNull YouTubePlayer youTubePlayer, @NotNull PlayerConstants.PlayerError playerError) {

                    }

                    @Override
                    public void onCurrentSecond(@NotNull YouTubePlayer youTubePlayer, float v) {

                    }

                    @Override
                    public void onVideoDuration(@NotNull YouTubePlayer youTubePlayer, float v) {

                    }

                    @Override
                    public void onVideoLoadedFraction(@NotNull YouTubePlayer youTubePlayer, float v) {

                    }

                    @Override
                    public void onVideoId(@NotNull YouTubePlayer youTubePlayer, @NotNull String s) {

                    }

                    @Override
                    public void onApiChange(@NotNull YouTubePlayer youTubePlayer) {

                    }
                });

            }
        });

        fullScreen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isFullScreen){
                    makeScreenLarge();
                }else{
                    makeScreenSmall();
                }
            }
        });

        closeScreen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                makeScreenClose();
            }
        });
    }

    void makeScreenSmall(){
        //make small
        Display display=getWindowManager().getDefaultDisplay();
        Point output=new Point();
        display.getSize(output);
        int w=(int) (70*87*output.x/10000);
        int h=(int) (70*output.y/100);
        ViewGroup.LayoutParams params= videoScreen.getLayoutParams();
        params.width=w;
        params.height=h;
        videoScreen.setLayoutParams(params);
        //videoScreen.bringToFront();
        isFullScreen=false;
        fullScreen.setImageResource(R.drawable.ic_fullscreen);
    }

    void makeScreenLarge(){
        //make as big as parent
        ViewGroup.LayoutParams params= videoScreen.getLayoutParams();
        params.width=ViewGroup.LayoutParams.MATCH_PARENT;
        params.height=ViewGroup.LayoutParams.MATCH_PARENT;
        videoScreen.setLayoutParams(params);
        //videoScreen.bringToFront();
        isFullScreen=true;
        fullScreen.setImageResource(R.drawable.ic_minimize);
    }

    void makeScreenClose(){
        player.pause();
        videoScreen.setVisibility(View.GONE);
        adapter.setVideoCurrentlyPlaying(null);
    }

    void setupDocuments(){
        readTerms.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GenericDialogUtils.makeScrollableTextDialog(mContext,"Terms of use",GenericDialogUtils.testText).show();
            }
        });

        readDataUse.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GenericDialogUtils.makeScrollableTextDialog(mContext,"Data policy",GenericDialogUtils.testText).show();
            }
        });
    }

    void setupSuggestions(){
        whatsApp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("http://api.whatsapp.com/send?phone=50587786917&text=Hola"));
                intent.setPackage("com.whatsapp");
                if (intent.resolveActivity(getPackageManager())!=null)
                startActivity(intent);
            }
        });

        googlePlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.coffeestainstudios.goatsimulator")));
                } catch (android.content.ActivityNotFoundException anfe) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.coffeestainstudios.goatsimulator")));
                }
            }
        });
    }



}