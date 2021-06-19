package com.dale.viaje.nicaragua;

import android.content.Context;

import java.util.ArrayList;

public class HelpVideos {
    
    Context context;


    ArrayList<HelpVideoObject> helpVideos=new ArrayList<>();

    public HelpVideos(Context context) {
        this.context=context;
        helpVideos.add(new HelpVideoObject("NYleIH94hbM",context.getString(R.string.helpvideos_intro), context.getString(R.string.helpvideos_introlong),context.getString(R.string.helpvideos_introdesc)));
        helpVideos.add(new HelpVideoObject("Dlpvqe1Q8FQ",context.getString(R.string.helpvideos_security), context.getString(R.string.helpvideos_securitylong),context.getString(R.string.helpvideos_securitydesc)));
        helpVideos.add(new HelpVideoObject("LQRJg2cUtS4",context.getString(R.string.helpvideos_reputation), context.getString(R.string.helpvideos_reputationlong),context.getString(R.string.helpvideos_reputationdesc)));
        helpVideos.add(new HelpVideoObject("SFMG4MWV4Io",context.getString(R.string.helpvideos_pasttrips), context.getString(R.string.helpvideos_pasttripslong),context.getString(R.string.helpvideos_pasttripsdesc)));
        helpVideos.add(new HelpVideoObject("tHdjxYBqU18",context.getString(R.string.helpvideos_settings), context.getString(R.string.helpvideos_settingslong),context.getString(R.string.helpvideos_settingsdesc)));
    }

    public ArrayList<HelpVideoObject> getVideoList(){
        return helpVideos;
    }
}
