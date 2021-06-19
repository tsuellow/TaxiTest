package com.dale.viaje.nicaragua.utils;

import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.dale.viaje.nicaragua.R;

public class GenericDialogUtils {

    public static String testText="Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod " +
            "tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis " +
            "nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis " +
            "aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat " +
            "nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui " +
            "officia deserunt mollit anim id est laborum.";

    public static Dialog makeScrollableTextDialog(Context context, String title, String text){
        final Dialog dialog=new Dialog(context);
        dialog.setContentView(R.layout.dialog_scrollable_text);
        TextView titleView=dialog.findViewById(R.id.tv_title_dialog);
        TextView textView=dialog.findViewById(R.id.tv_text);
        Button closeBtn=dialog.findViewById(R.id.bt_close);

        titleView.setText(title);
        textView.setText(text);

        closeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });
        return dialog;
    }
}
