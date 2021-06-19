package com.dale.viaje.nicaragua.utils;

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

        textSize=Math.round(pxs/6);
        offset=Math.round(pxs/20);

        paintFill = new Paint();
        paintFill.setAntiAlias(true);
        paintFill.setTextSize(textSize);
        paintFill.setColor(Color.WHITE);

        paintStroke=new Paint();
        paintStroke.setAntiAlias(true);
        paintStroke.setTextSize(textSize);
        paintStroke.setStyle(Paint.Style.STROKE);
        paintStroke.setStrokeWidth(3);
        paintStroke.setColor(Color.BLACK);


    }

    @Override
    protected void onDraw(Canvas canvas)
    {
        Path path = new Path();
        //path.addCircle(60, 60, 52, Path.Direction.CCW);
        Log.d("texttest", "once"+pxs);
        path.addCircle(pxs/2, pxs/2, 8*pxs/19, Path.Direction.CCW);
        canvas.drawTextOnPath(plate, path, Math.round(pxs*3.1416*0.53), offset, paintStroke);
        canvas.drawTextOnPath(plate, path, Math.round(pxs*3.1416*0.53), offset, paintFill);
    }
}

