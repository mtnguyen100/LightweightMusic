package com.theroungelounge.musicappone;

import java.io.Serializable;

/**
 * Created by Rounge on 6/1/2016.
 */
public class Song implements Serializable {
    private long id;
    private String title;
    private String artist;
    private int length;
    private long albumId;


    public Song(long songID, String songTitle, String songArtist, int length, long albumId) {
        id = songID;
        title = songTitle;
        artist = songArtist;
        this.length = length;
        this.albumId = albumId;
    }

    public long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getArtist() {
        return artist;
    }

    public int getLength() {
        return length;
    }

    public long getAlbumId() {
        return albumId;
    }
}
