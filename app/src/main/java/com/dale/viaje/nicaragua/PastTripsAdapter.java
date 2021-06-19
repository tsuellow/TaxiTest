package com.dale.viaje.nicaragua;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.dale.viaje.nicaragua.CommunicationsRecyclerView.CommsRecDialogAdapter;
import com.dale.viaje.nicaragua.data.CommRecordObject;
import com.dale.viaje.nicaragua.data.MsjRecordObject;
import com.dale.viaje.nicaragua.utils.CurvedTextView;
import com.dale.viaje.nicaragua.utils.MiscellaneousUtils;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.util.Date;
import java.util.List;

public class PastTripsAdapter extends RecyclerView.Adapter<PastTripsAdapter.ViewHolder> {

    public class ViewHolder extends RecyclerView.ViewHolder{

        private int adapterPosition;
        private TextView name;
        private CurvedTextView collar;
        private TextView origin;
        private TextView destination;
        private ImageView photo;
        private CardView statusColor;
        private FloatingActionButton profile;
        public ImageButton info;
        private TextView reputation;
        private ImageView reputationStar;
        private TextView dateTime;
        private TextView statusText;
        //commsMenu
        private ImageButton commHistoryMenu;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            name = (TextView) itemView.findViewById(R.id.tv_name);
            collar = (CurvedTextView) itemView.findViewById(R.id.ct_collar);
            origin = (TextView) itemView.findViewById(R.id.tv_origin);
            destination = (TextView) itemView.findViewById(R.id.tv_destination);
            statusColor = (CardView) itemView.findViewById(R.id.cv_border_color);
            profile = (FloatingActionButton) itemView.findViewById(R.id.bt_profile);
            info = (ImageButton) itemView.findViewById(R.id.bt_info);
            dateTime = (TextView) itemView.findViewById(R.id.tv_date_time);
            statusText = (TextView) itemView.findViewById(R.id.tv_status_text);
            photo = (ImageView) itemView.findViewById(R.id.iv_photo);
            reputation = (TextView) itemView.findViewById(R.id.tv_rep);
            reputationStar = (ImageView) itemView.findViewById(R.id.iv_rep);
            commHistoryMenu = (ImageButton) itemView.findViewById(R.id.bt_comm_history);
        }
    }

    private List<CommRecordObject> mCommRecs;
    private Context mContext;

    public PastTripsAdapter(Context context){
        mContext=context;
    }

    public void setCommRecs(List<CommRecordObject> commRecs) {
        this.mCommRecs = commRecs;
        notifyDataSetChanged();
    }



    @NonNull
    @Override
    public PastTripsAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater=LayoutInflater.from(mContext);

        //now inflate the view
        View clientView=inflater.inflate(R.layout.adapter_past_comms,parent,false);

        return new ViewHolder(clientView);
    }

    @Override
    public void onBindViewHolder(@NonNull PastTripsAdapter.ViewHolder holder, int position) {
        final  CommRecordObject comm=mCommRecs.get(position);
        holder.name.setText(comm.getFirstName());
        //holder.reputation.setText(comm.getReputation()+"");
        holder.origin.setText(comm.getBarrioFrom());
        holder.origin.setTextColor(comm.getColorFrom());
        holder.destination.setText(comm.getBarrioTo());
        holder.destination.setTextColor(comm.getColorTo());
        File thumbFile=new File(mContext.getExternalFilesDir(null),"thumbs/"+comm.getTaxiId()+".jpg");
        if (thumbFile.exists()){
            Bitmap bitmap=BitmapFactory.decodeFile(thumbFile.getAbsolutePath());
            holder.photo.setImageBitmap(bitmap);
        }
        holder.collar.setText(comm.getCollar());
        holder.dateTime.setText(MiscellaneousUtils.getDateStringPast(new Date(comm.getTimestamp())));
        if (comm.getCommStatus() == CommRecordObject.ACCEPTED) {
            holder.statusText.setText(R.string.pasttripsadapter_accepted);
        } else {
            if (comm.getCommStatus() == CommRecordObject.CONTACTED) {
                holder.statusText.setText(R.string.pasttripsadapter_contacted);
            } else {
                holder.statusText.setText(R.string.pasttripsadapter_observing);
            }
        }


        holder.profile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(mContext, R.string.pasttripsadapter_onprofileclick, Toast.LENGTH_SHORT).show();
            }
        });

        holder.info.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showInfoDialog(comm);
            }
        });

        holder.commHistoryMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showCommsDialog(comm);
            }
        });




        //populate basic shit and test
        //switch statement for status
        //recycle existing adapter for audio at least xml
        //create info dialog
    }

    @Override
    public int getItemCount() {
        if (mCommRecs==null){
            return  0;
        }else {
            return mCommRecs.size();
        }
    }

    //Comms Object dialog inflater
    public void showCommsDialog(CommRecordObject comm){
        final Dialog dialog=new Dialog(mContext);
        dialog.setContentView(R.layout.comm_dialog);
        TextView title=dialog.findViewById(R.id.tv_title_dialog);
        TextView date=dialog.findViewById(R.id.tv_date);
        TextView noMsjs=dialog.findViewById(R.id.tv_no_msgs);
        TextView name=dialog.findViewById(R.id.tv_other);
        title.setText(mContext.getString(R.string.pasttripsadapter_chatwith)+comm.getFirstName());
        date.setText(MiscellaneousUtils.getDateStringGeneric(new Date(comm.getTimestamp()),"EEEE dd. MMMM yyyy"));
        date.setVisibility(View.VISIBLE);
        name.setText(comm.getFirstName()+",");
        RecyclerView commsRV=(RecyclerView) dialog.findViewById(R.id.rv_comms_dialog);
        Button closeBtn=(Button) dialog.findViewById(R.id.bt_dialog_close);
        CommsRecDialogAdapter adapter=new CommsRecDialogAdapter(mContext,comm);
        populateDataSource(comm.getCommId(),adapter, noMsjs);
        commsRV.setAdapter(adapter);
        LinearLayoutManager layoutManager = new LinearLayoutManager(dialog.getContext());
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        layoutManager.setReverseLayout(true);
        commsRV.setLayoutManager(layoutManager);
        commsRV.scrollToPosition(0);
        closeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });
        dialog.show();
    }

    private static void populateDataSource(String commId, CommsRecDialogAdapter adapter, TextView placeHolderText) {
        AppExecutors.getInstance().diskIO().execute(new Runnable() {
            @Override
            public void run() {
                List<MsjRecordObject> msjs=PastTripsActivity.mDb.commsDao().getCommMsjs(commId);
                if (msjs.size()>0){
                    placeHolderText.setVisibility(View.GONE);
                }
                adapter.setMsjs(msjs);
            }
        });
    }

    public void showInfoDialog(CommRecordObject comm){
        final Dialog dialog=new Dialog(mContext);
        dialog.setContentView(R.layout.dialog_trip_details);
        ImageView photo=dialog.findViewById(R.id.iv_photo);
        TextView name=dialog.findViewById(R.id.tv_name);
        TextView ageGenderId=dialog.findViewById(R.id.tv_age_gender_id);
        TextView plate=dialog.findViewById(R.id.tv_plate);
        TextView date=dialog.findViewById(R.id.tv_date);
        TextView origin=dialog.findViewById(R.id.tv_origin);
        TextView destination=dialog.findViewById(R.id.tv_destination);
        TextView seats=dialog.findViewById(R.id.tv_seats);
        TextView status=dialog.findViewById(R.id.tv_status);
        ImageButton originPoint=dialog.findViewById(R.id.iv_pin_from);
        ImageButton destinationPoint=dialog.findViewById(R.id.iv_pin_to);
        //TODO alternatively use thumb or photo in this instance
        File thumbFile=new File(mContext.getExternalFilesDir(null),"thumbs/"+comm.getTaxiId()+".jpg");
        if (thumbFile.exists()){
            Bitmap bitmap=BitmapFactory.decodeFile(thumbFile.getAbsolutePath());
            photo.setImageBitmap(bitmap);
        }
        String nameStr=comm.getFirstName()+" "+MiscellaneousUtils.replaceNull(comm.getLastName());
        name.setText(nameStr);
        String otherPersonalData=""+MiscellaneousUtils.getDiffYears(new Date(comm.getDob()),new Date())+" ("+comm.getGender().charAt(0)+") / ID: "+comm.getTaxiId();
        ageGenderId.setText(otherPersonalData);
        if (!comm.getCollar().equals(comm.getSeats())){
            plate.setText(mContext.getString(R.string.pasttripsadapter_plate)+" "+comm.getCollar());
        }else {
            plate.setVisibility(View.GONE);
        }
        date.setText(MiscellaneousUtils.getDateStringGeneric(new Date(comm.getTimestamp()),"dd MMM yyyy - h:mm a"));
        origin.setText(comm.getBarrioFrom());
        origin.setTextColor(comm.getColorFrom());
        destination.setText(comm.getBarrioTo());
        destination.setTextColor(comm.getColorTo());
        seats.setText(comm.getSeats());
        if (comm.getCommStatus() == CommRecordObject.ACCEPTED) {
            status.setText(R.string.pasttripsadapter_accepted);
        } else {
            if (comm.getCommStatus() == CommRecordObject.CONTACTED) {
                status.setText(R.string.pasttripsadapter_contacted);
            } else {
                status.setText(R.string.pasttripsadapter_observing);
            }
        }
        originPoint.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Uri uri=Uri.parse("geo:"+comm.getLatFrom()+","+comm.getLonFrom()+"?q="+comm.getLatFrom()+","+comm.getLonFrom()+"(Point of origin)");
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                mContext.startActivity(intent);
            }
        });

        destinationPoint.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Uri uri=Uri.parse("geo:"+comm.getLatTo()+","+comm.getLonTo()+"?q="+comm.getLatTo()+","+comm.getLonTo()+"(Point of destination)");
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                mContext.startActivity(intent);
            }
        });


        Button closeBtn=(Button) dialog.findViewById(R.id.bt_dialog_close);

        closeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });
        dialog.show();
    }

}
