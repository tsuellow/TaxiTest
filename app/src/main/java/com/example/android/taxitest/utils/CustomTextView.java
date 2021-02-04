package com.example.android.taxitest.utils;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;

import com.example.android.taxitest.R;

public class CustomTextView extends androidx.appcompat.widget.AppCompatTextView {

    float mStroke;

    public CustomTextView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.obtainStyledAttributes(attrs,
                R.styleable.CustomTextView);
        mStroke=a.getFloat(R.styleable.CustomTextView_stroke,1.0f);
        a.recycle();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        TextPaint paint = this.getPaint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(mStroke);
        //this.setTypeface(ResourcesCompat.getFont(getContext(), R.font.dale_viaje_font));
        super.onDraw(canvas);
    }
}
