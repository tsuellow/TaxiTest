package com.example.android.taxitest.vtmExtension;

import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import org.oscim.android.canvas.AndroidBitmap;
import org.oscim.android.canvas.AndroidCanvas;
import org.oscim.backend.canvas.Bitmap;

public final class AndroidGraphicsCustom extends AndroidCanvas {

    public static Bitmap drawableToBitmap(Drawable drawable, int scale) {
        if (drawable instanceof BitmapDrawable) {
            return new AndroidBitmap(((BitmapDrawable) drawable).getBitmap());
        }

        android.graphics.Bitmap bitmap = android.graphics.Bitmap
                .createBitmap(scale,
                        scale,
                        android.graphics.Bitmap.Config.ARGB_8888);

        android.graphics.Canvas canvas = new android.graphics.Canvas(bitmap);
        drawable.setBounds(0, 0, scale, scale);
        drawable.draw(canvas);

        return new AndroidBitmap(bitmap);
    }

}
