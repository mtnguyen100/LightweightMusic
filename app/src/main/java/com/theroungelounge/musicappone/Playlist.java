package com.theroungelounge.musicappone;

/**
 * Created by Rounge on 6/13/2016.
 */
public class Playlist {
    private long id;
    private String title;
    private int numSongs;

    public Playlist (long id, String title, int numSongs) {
        this.id = id;
        this.title = title;
        this.numSongs = numSongs;
    }

    public long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public int getNumSongs() {
        return numSongs;
    }
}