package com.dale.viaje.nicaragua.CommunicationsRecyclerView;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class SmoothLinearLayoutManager extends LinearLayoutManager {


    public SmoothLinearLayoutManager(Context context) {
        super(context);
    }

    private  int h=0;
    private  int w=getWidth();
    private boolean autoMeasureEnabled=true;

    @Override
    public boolean isAutoMeasureEnabled(){
        return autoMeasureEnabled;
    }

    @Override
    public void onItemsRemoved(@NonNull final RecyclerView recyclerView, int positionStart, int itemCount) {
        super.onItemsRemoved(recyclerView, positionStart, itemCount);
        autoMeasureEnabled = false;
        h=recyclerView.getHeight();
        w=recyclerView.getWidth();
        //h=Math.min(h,280);
        postOnAnimation(new Runnable() {
            @Override
            public void run() {
                recyclerView.getItemAnimator().isRunning(new RecyclerView.ItemAnimator.ItemAnimatorFinishedListener() {
                    @Override
                    public void onAnimationsFinished() {
                        autoMeasureEnabled = true;
                        requestLayout();
                    }
                });
            }
        });
    }

    @Override
    public void onMeasure(@NonNull RecyclerView.Recycler recycler, @NonNull RecyclerView.State state, int widthSpec, int heightSpec) {
        super.onMeasure(recycler, state, widthSpec, heightSpec);

        if (!isAutoMeasureEnabled()) {
            // we should perform measuring manually
            // so request animations
            requestSimpleAnimationsInNextLayout();
            //keep size until remove animation will be completed
            setMeasuredDimension(w, h);
        }
    }

}
