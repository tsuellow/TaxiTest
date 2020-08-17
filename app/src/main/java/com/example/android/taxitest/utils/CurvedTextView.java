package com.example.android.taxitest.utils;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;


public class CurvedTextView  extends View {

    Paint paintFill;
    Paint paintStroke;
    String plate="ES00000";
    float pxs;
    int textSize;
    int offset;

    public void setText(String text){
        if (text!=null){
            plate=text;
            invalidate();
        }
    }

    public CurvedTextView(Context context, AttributeSet attrs) {
        super(context,attrs);
        setFocusable(true);

        pxs = 60 * getResources().getDisplayMetrics().density;
        Log.d("pxs size",""+pxs);

        textSize=Math.round(pxs/7);
        offset=Math.round(pxs/13);

        paintFill = new Paint();
        paintFill.setAntiAlias(true);
        paintFill.setTextSize(textSize);
        paintFill.setColor(Color.WHITE);

        paintStroke=new Paint();
        paintStroke.setAntiAlias(true);
        paintStroke.setTextSize(textSize);
        paintStroke.setStyle(Paint.Style.STROKE);
        paintStroke.setStrokeWidth(2);
        paintStroke.setColor(Color.BLACK);


    }

    @Override
    protected void onDraw(Canvas canvas)
    {
        Path path = new Path();
        //path.addCircle(60, 60, 52, Path.Direction.CCW);
        path.addCircle(pxs/2, pxs/2, 8*pxs/20, Path.Direction.CCW);
        canvas.drawTextOnPath(plate, path, Math.round(pxs*3.1416*0.5), offset, paintStroke);
        canvas.drawTextOnPath(plate, path, Math.round(pxs*3.1416*0.5), offset, paintFill);
    }
}

