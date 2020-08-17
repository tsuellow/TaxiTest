package com.example.android.taxitest;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.android.taxitest.CommunicationsRecyclerView.CommsDialogAdapter;
import com.example.android.taxitest.CommunicationsRecyclerView.CommsRecDialogAdapter;
import com.example.android.taxitest.data.CommRecordObject;
import com.example.android.taxitest.data.MsjRecordObject;
import com.example.android.taxitest.utils.CurvedTextView;
import com.example.android.taxitest.utils.MiscellaneousUtils;
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
        Bitmap bitmap=BitmapFactory.decodeFile(thumbFile.getAbsolutePath());
        holder.photo.setImageBitmap(bitmap);
        holder.collar.setText(comm.getCollar());
        holder.dateTime.setText(MiscellaneousUtils.getDateStringPast(new Date(comm.getTimestamp())));
        if (comm.getCommStatus() == CommRecordObject.ACCEPTED) {
            holder.statusText.setText("accepted");
        } else {
            if (comm.getCommStatus() == CommRecordObject.CONTACTED) {
                holder.statusText.setText("contacted");
            } else {
                holder.statusText.setText("observing");
            }
        }


        holder.profile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(mContext, "open online profile "+comm.getCollar()+" "+comm.getCommStatus(), Toast.LENGTH_SHORT).show();
            }
        });

        holder.info.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(mContext, "show trip info", Toast.LENGTH_SHORT).show();
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
        TextView noMsgs=dialog.findViewById(R.id.tv_no_msgs);
        title.setText("Chat with "+comm.getFirstName());
        RecyclerView commsRV=(RecyclerView) dialog.findViewById(R.id.rv_comms_dialog);
        Button closeBtn=(Button) dialog.findViewById(R.id.bt_dialog_close);
        CommsRecDialogAdapter adapter=new CommsRecDialogAdapter(mContext,comm);
        populateDataSource(comm.getCommId(),adapter);
        commsRV.setAdapter(adapter);
        LinearLayoutManager layoutManager = new LinearLayoutManager(dialog.getContext());
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        commsRV.setLayoutManager(layoutManager);
        closeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });
        dialog.show();
    }

    private static void populateDataSource(String commId, CommsRecDialogAdapter adapter) {
        AppExecutors.getInstance().diskIO().execute(new Runnable() {
            @Override
            public void run() {
                List<MsjRecordObject> msjs=PastTripsActivity.mDb.commsDao().getCommMsjs(commId);
                adapter.setMsjs(msjs);
            }
        });
    }

}
