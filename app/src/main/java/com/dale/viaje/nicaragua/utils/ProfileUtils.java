package com.dale.viaje.nicaragua.utils;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.dale.viaje.nicaragua.R;

public class ProfileUtils {

    public static Dialog displayProfileDialog(Context context, String title, String url){
        final Dialog dialog=new Dialog(context);
        dialog.setContentView(R.layout.dialog_profile);
        TextView titleView=dialog.findViewById(R.id.tv_title_dialog);
        WebView webView=dialog.findViewById(R.id.wv_profile);
        Button closeBtn=dialog.findViewById(R.id.bt_close);
        ProgressBar progressBar=dialog.findViewById(R.id.pb_loading);

        titleView.setText(title);
        webView.getSettings().setJavaScriptEnabled(true);

        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        webView.setWebViewClient(new WebViewClient(){
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                progressBar.setVisibility(View.GONE);
            }
        });
        webView.loadUrl(url);

        closeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });
        dialog.getWindow().setFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);
        dialog.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
        return dialog;
    }
}
