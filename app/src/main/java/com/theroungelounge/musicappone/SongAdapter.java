package com.theroungelounge.musicappone;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by Rounge on 6/1/2016.
 */
public class SongAdapter extends BaseAdapter {

    private ArrayList<Song> songs;  //The list of songs on the hard drive
    private LayoutInflater songInf; //Maps a song's title and artist to the TextViews in song.xml

    public SongAdapter(Context c, ArrayList<Song> theSongs){
        songs=theSongs;
        songInf=LayoutInflater.from(c);
    }



    @Override
    public int getCount() {
        return songs.size();
    }

    @Override
    public Object getItem(int index) {
        return songs.get(index);
    }

    @Override
    public long getItemId(int arg0) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        //map to song layout
        LinearLayout songLay = (LinearLayout)songInf.inflate
                (R.layout.song, parent, false);
        //get title and artist TextViews
        TextView songView = (TextView)songLay.findViewById(R.id.song_title);
        TextView artistView = (TextView)songLay.findViewById(R.id.song_artist);
        TextView lengthView = (TextView)songLay.findViewById(R.id.song_length);
        //get song using position
        Song currSong = songs.get(position);
        //get title and artist strings
        songView.setText(currSong.getTitle());
        artistView.setText(currSong.getArtist());
        lengthView.setText(formatSongLength(currSong.getLength()));
        //set position as tag
        songLay.setTag(position);
        return songLay;
    }

    /**
     * Created by Rounge on 6/9/2016
     * @param songLength The length of the song in the songs list.
     * @return The formatted length of the song.
     */
    public static String formatSongLength(int songLength) {
        double minutes = (double)(songLength/1000)/60;
        double seconds = (minutes - ((int)minutes))*60;
        if (seconds < 10.0)
            return ((int)minutes) + ":0" + ((int)seconds);
        else
            return ((int)minutes) + ":" + ((int)seconds);
    }
}
