package com.example.android.taxitest;

import java.util.ArrayList;

public class HelpVideos {

    ArrayList<HelpVideoObject> helpVideos=new ArrayList<>();

    public HelpVideos() {
        helpVideos.add(new HelpVideoObject("NYleIH94hbM","intro", "Introductory app tour","learn basic usage of the app"));
        helpVideos.add(new HelpVideoObject("Dlpvqe1Q8FQ","security", "How to travel safely","learn basic usage of the app"));
        helpVideos.add(new HelpVideoObject("LQRJg2cUtS4","reputation", "Make the service better","learn basic usage of the app"));
        helpVideos.add(new HelpVideoObject("SFMG4MWV4Io","past trips", "View your travel history","learn basic usage of the app"));
        helpVideos.add(new HelpVideoObject("tHdjxYBqU18","settings", "Explore advanced options","learn basic usage of the app"));
    }

    public ArrayList<HelpVideoObject> getVideoList(){
        return helpVideos;
    }
}
