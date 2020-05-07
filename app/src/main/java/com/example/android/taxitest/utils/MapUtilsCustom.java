package com.example.android.taxitest.utils;

import android.content.Context;

import com.example.android.taxitest.Constants;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class MapUtilsCustom {

    //CHECK IF  NICARAGUAN MAP-FILE IS IN EXTERNAL STORAGE AND ELSE LOAD IT THERE FROM RESOURCES
    public static void copyFileToExternalStorage(int resourceId, String fileName, Context context){
        File sdFile = new File(context.getExternalFilesDir(null), fileName);
        if (!sdFile.exists()) {
            try {
                InputStream in = context.getResources().openRawResource(resourceId);
                FileOutputStream out = new FileOutputStream(sdFile);
                byte[] buff = new byte[1024];
                int read = 0;
                try {
                    while ((read = in.read(buff)) > 0) {
                        out.write(buff, 0, read);
                    }
                } finally {
                    in.close();
                    out.close();
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }
}
