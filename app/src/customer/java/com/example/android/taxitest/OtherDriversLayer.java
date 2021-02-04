package com.example.android.taxitest;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.cardview.widget.CardView;

import com.example.android.taxitest.CommunicationsRecyclerView.CommsObject;
import com.example.android.taxitest.CommunicationsRecyclerView.CommunicationsAdapter;
import com.example.android.taxitest.connection.WebSocketDriverLocations;
import com.example.android.taxitest.data.SocketObject;
import com.example.android.taxitest.utils.MiscellaneousUtils;
import com.example.android.taxitest.vectorLayer.BarriosLayer;
import com.example.android.taxitest.vectorLayer.ConnectionLineLayer2;
import com.example.android.taxitest.vtmExtension.OtherTaxiLayer;
import com.example.android.taxitest.vtmExtension.TaxiMarker;

import org.json.JSONObject;
import org.oscim.core.GeoPoint;
import org.oscim.map.Map;

import java.util.List;

public class OtherDriversLayer extends OtherTaxiLayer {

    CommsObject mSelectedComm=null;
    boolean autoCloseTriggered=false;

    public OtherDriversLayer(Context context, BarriosLayer barriosLayer, Map map, List<TaxiMarker> list, WebSocketDriverLocations webSocketConnection, ConnectionLineLayer2 connectionLineLayer, CommunicationsAdapter commsAdapter) {
        super(context, barriosLayer, map, list, webSocketConnection, connectionLineLayer, commsAdapter);
    }

    @Override
    public void setCommAcceptedListener() {
        mCommunicationsAdapter.setCommAcceptedListener(new CommunicationsAdapter.CommAcceptedListener() {
            @Override
            public void onCommAccepted(CommsObject comm) {
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(context,"comm was just accepted", Toast.LENGTH_LONG).show();
                        mSelectedComm=comm;
                        mSelectedComm.taxiMarker.setMovementListener(new TaxiMarker.MovementListener() {
                            @Override
                            public void onMarkerMoved(SocketObject newPoint) {
                                double distance=MiscellaneousUtils.locToGeo(MainActivity.mMarkerLoc).sphericalDistance(new GeoPoint(newPoint.getLatitude(),newPoint.getLongitude()));
                                if (distance<100.0 ){
                                    if (!autoCloseTriggered){
                                        autoCloseTriggered=true;
                                        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                            @Override
                                            public void run() {
                                                if (autoCloseTriggered)
                                                showEndSearchDialog(comm,"Did your taxi arrive?", "It seems like your taxi already passed by your position.");
                                            }
                                        },10000);
                                    }
                                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                                        @Override
                                        public void run() {
                                            Toast.makeText(context, "your taxi is only " + Math.round(distance) + " meters away", Toast.LENGTH_SHORT).show();
                                        }
                                    });

                                }
                            }
                        });
                        ((MainActivityCustomer)context).setIsActive(2,context);
                        Log.d("commies", "run: "+mSelectedComm.taxiMarker.taxiObject.getTaxiId()+"   "+(mSelectedComm==null));
                        clearComms(mSelectedComm.taxiMarker.taxiObject.getTaxiId());
                        //backup the accepted comm to server
                        JSONObject json=MainActivityCustomer.getCommBackupJson(comm,barriosLayer);
                        //TODO uncomment next line once corresponding node.js file has been created
                        comm.backUpAcceptedComm(json);
                    }
                });
            }
        });
    }

    @Override
    public void doUnClick(TaxiMarker item, boolean animate) {
        if (mSelectedComm!=null){
            if (item==mSelectedComm.taxiMarker){
                mSelectedComm.taxiMarker.setMovementListener(null);
                autoCloseTriggered=false;
                showEndSearchDialog(mSelectedComm,"You cancelled your selected taxi, did it arrive?", "You just dismissed and accepted communication. " +
                        "If you have not yet found a match you can continue searching");
                mSelectedComm=null;

            }
        }
        super.doUnClick(item, animate);

    }

    public CountDownTimer genericCountdownTimer;
    public void showEndSearchDialog(CommsObject comm, String titleText, String contextText){
        //make notification if not in foreground
        if (!MainActivity.getIsActivityInForeground()){
            Intent closeIntent = new Intent((MainActivityCustomer)context, EntryActivityCustomer.class);
            closeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            MiscellaneousUtils.showExitNotification(context,"Has your taxi arrived?", "If you are already on your way you can" +
                    " close the app, otherwise open it to continue your search. \nIf you ignore this notification your search will end automatically in a few minutes",closeIntent);
        }

        final Dialog dialog=new Dialog((MainActivityCustomer)context);
        dialog.setContentView(R.layout.dialog_end_search);
        TextView title=dialog.findViewById(R.id.tv_title_dialog);
        CardView border=dialog.findViewById(R.id.cv_border_color);
        ImageView photo=dialog.findViewById(R.id.iv_photo);
        TextView name=dialog.findViewById(R.id.tv_name);
        TextView plate=dialog.findViewById(R.id.tv_plate);
        TextView contextIntro=dialog.findViewById(R.id.tv_text_context);
        TextView intro=dialog.findViewById(R.id.tv_text_intro);
        TextView countdown=dialog.findViewById(R.id.tv_countdown);
        Button finishBtn=dialog.findViewById(R.id.bt_finish);
        Button continueBtn=dialog.findViewById(R.id.bt_continue);
        LinearLayout auto=(LinearLayout) dialog.findViewById(R.id.ll_auto_accept);
        LinearLayout commInfo=(LinearLayout) dialog.findViewById(R.id.ll_personal_info);

        title.setText(titleText);
        contextIntro.setText(contextText);

        if (comm==null){
            commInfo.setVisibility(View.GONE);
        }else{
            border.setCardBackgroundColor(comm.taxiMarker.color);
            photo.setImageBitmap(comm.commCardData.thumb);
            name.setText(comm.commCardData.firstName+" "+comm.commCardData.lastName);
            plate.setText("plate: "+comm.commCardData.collar);
        }

        genericCountdownTimer=new CountDownTimer(60000, 1000) {
            public void onTick(long millisUntilFinished) {
                int secsLeft=(int)millisUntilFinished/1000;
                countdown.setText(""+secsLeft);

            }
            public void onFinish() {
                //send cancellation msg
                if (comm!=null)
                    mCommunicationsAdapter.cancelById(comm.taxiMarker.taxiObject.getTaxiId());
                //exit to entry
                ((MainActivityCustomer)context).exitSearch();
                //close dialog
                genericCountdownTimer.cancel();
                if (dialog.isShowing())
                dialog.dismiss();
            }
        };

        finishBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //send cancellation msg
                if (comm!=null)
                    mCommunicationsAdapter.cancelById(comm.taxiMarker.taxiObject.getTaxiId());
                //exit to entry
                ((MainActivityCustomer)context).exitSearch();
                //close dialog
                genericCountdownTimer.cancel();
                dialog.dismiss();
            }
        });

        continueBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mSelectedComm==null)
                    ((MainActivityCustomer)context).setIsActive(1,context);
                genericCountdownTimer.cancel();
                dialog.dismiss();
            }
        });

        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                genericCountdownTimer.cancel();
            }
        });
        genericCountdownTimer.start();
        dialog.show();
    }
}
