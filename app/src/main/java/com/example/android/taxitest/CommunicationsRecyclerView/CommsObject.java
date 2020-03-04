package com.example.android.taxitest.CommunicationsRecyclerView;

import android.widget.Filter;

import com.example.android.taxitest.vtmExtension.TaxiMarker;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class CommsObject {
    static final int OBSERVING=0;
    static final int REQUEST_SENT=1;
    static final int REQUEST_RECEIVED=2;
    static final int ACCEPTED=3;
    static final int REJECTED=4;

    private List<File> audioList=new ArrayList<>();

    public void addAtTopOfList(File file){
        audioList.add(0,file);
    }

    public File getTopOfList(){
        return audioList.get(0);
    }



    public TaxiMarker taxiMarker;

    //driver info driverObject from room DB
    //list of individual communications
    //Comms info
    int messageStatus=OBSERVING;

    public CommsObject(TaxiMarker taxiMarker){
        this.taxiMarker=taxiMarker;
    }



}
