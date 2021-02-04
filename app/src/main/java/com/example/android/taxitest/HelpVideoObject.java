package com.example.android.taxitest;

public class HelpVideoObject {
    public String youtubeUrlExt, shortName, longName, description;
    boolean isPlaying=false;

    public HelpVideoObject(String youtubeUrlExt, String shortName, String longName, String description) {
        this.youtubeUrlExt = youtubeUrlExt;
        this.shortName = shortName;
        this.longName = longName;
        this.description = description;
    }
}
