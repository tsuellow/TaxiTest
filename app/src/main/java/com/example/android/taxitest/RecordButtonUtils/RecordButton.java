package com.example.android.taxitest.RecordButtonUtils;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.media.MediaRecorder;
import android.media.SoundPool;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.AppCompatImageView;

import com.example.android.taxitest.R;

import java.io.File;
import java.io.IOException;
import java.util.Date;


public class RecordButton extends AppCompatImageView implements View.OnTouchListener{

    private ScaleAnim scaleAnim;
    private boolean listenForRecord = true;
    private String commId;
    private MediaRecorder recorder = null;
    private boolean isRecording;

    private static SoundPool soundPool = null;
    private static int soundStart,soundEnd;


    public RecordButton(Context context, int id) {
        super(context);
        init(context, null);
    }

    public void setCommId(String commId) {
        this.commId = commId;
    }

    public RecordButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public RecordButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);


    }


    private void init(Context context, AttributeSet attrs) {
        if (attrs != null) {
            TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.RecordButton);

            int imageResource = typedArray.getResourceId(R.styleable.RecordButton_mic_icon, -1);


            if (imageResource != -1) {
                setTheImageResource(imageResource);
            }

            typedArray.recycle();
        }

        if (soundPool==null){
            soundPool = new SoundPool.Builder()
                    .setMaxStreams(5)
                    .build();
            //these are just two wav files with short intro and exit sounds
            soundStart=soundPool.load(getContext(),R.raw.start_recording_sound,0);
            soundEnd=soundPool.load(getContext(),R.raw.end_recording_sound,0);
        }

        scaleAnim = new ScaleAnim(this);
        this.setOnTouchListener(this);

    }

    private void setTheImageResource(int imageResource) {
        Drawable image = AppCompatResources.getDrawable(getContext(), imageResource);
        setImageDrawable(image);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        setClip(this);
    }

    public void setClip(View v) {
        if (v.getParent() == null) {
            return;
        }

        if (v instanceof ViewGroup) {
            ((ViewGroup) v).setClipChildren(false);
            ((ViewGroup) v).setClipToPadding(false);
        }

        if (v.getParent() instanceof View) {
            //setClip((View) v.getParent());
        }
    }



    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (isListenForRecord()) {
            switch (event.getAction()) {

                case MotionEvent.ACTION_DOWN:
                    ((RecordButton) v).startScale();
                    startRecording();
                    break;
                case MotionEvent.ACTION_CANCEL:
                case MotionEvent.ACTION_UP:
                    stopRecording();
                    ((RecordButton) v).stopScale();
                    break;

            }

        }
        return isListenForRecord();
    }

    File file;
    //audio recording tools
    private void startRecording() {
        if (isRecording){
            stopRecording();
        }
        String fileName=getContext().getExternalCacheDir().getAbsolutePath()+"/"+ commId +"_"+new Date().getTime()+".aac";
        file=new File(fileName);
        recorder = new MediaRecorder();
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.AAC_ADTS);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        recorder.setAudioSamplingRate(8000);
        recorder.setAudioChannels(1);
        recorder.setAudioEncodingBitRate(12000);
        recorder.setOutputFile(fileName);
        try {
            recorder.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
        //Toast.makeText(getContext(),fileName,Toast.LENGTH_LONG).show();
        soundPool.play(soundStart,1,1,0,0,1);
        recorder.start();
        setIsRecording(true);
        recordingStartedListener.onRecordingStarted();
    }

    public void stopRecording() {
        if (isRecording) {
            soundPool.play(soundEnd,1,1,0,0,1);
            Handler handler=new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    try {
                        recorder.stop();
                        recordingFinishedListener.onRecordingFinished(file);
                    }catch (RuntimeException e){
                        e.printStackTrace();
                        Toast.makeText(getContext(),"recording failed",Toast.LENGTH_LONG).show();
                        recordingFinishedListener.onRecordingFinished(null);
                    }
                    recorder.release();
                    setIsRecording(false);
                    recorder = null;
                }
            },500);

        }
    }

    private void setIsRecording(boolean isRecording){
        this.isRecording=isRecording;
    }


    protected void startScale() {
        scaleAnim.start();
    }

    protected void stopScale() {
        scaleAnim.stop();
    }

    public void setListenForRecord(boolean listenForRecord) {
        this.listenForRecord = listenForRecord;
    }

    public boolean isListenForRecord() {
        return listenForRecord;
    }

    //callback for when a recording has been made
    public interface RecordingFinishedListener{
        void onRecordingFinished(File file);
    }

    RecordingFinishedListener recordingFinishedListener;

    public void setRecordingFinishedListener(RecordingFinishedListener recordingFinishedListener) {
        this.recordingFinishedListener = recordingFinishedListener;
    }

    //callback for when a recording has started
    public interface RecordingStartedListener{
        void onRecordingStarted();
    }

    RecordingStartedListener recordingStartedListener;

    public void setRecordingStartedListener(RecordingStartedListener recordingStartedListener) {
        this.recordingStartedListener = recordingStartedListener;
    }
}

