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
 * Created by Rounge on 6/13/2016.
 */
public class PlaylistAdapter extends BaseAdapter {

    private ArrayList<Playlist> playlists;  //The list of playlists on the hard drive
    private LayoutInflater playlistInflater; //Maps a song's title and artist to the TextViews in song.xml

    public PlaylistAdapter(Context c, ArrayList<Playlist> playlists){
        this.playlists = playlists;
        playlistInflater = LayoutInflater.from(c);
    }

    @Override
    public int getCount() {
        return playlists.size();
    }

    @Override
    public Object getItem(int position) {
        return playlists.get(position);
    }

    @Override
    public long getItemId(int position) {
        return playlists.get(position).getId();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        //map to song layout
        LinearLayout playlistLayout = (LinearLayout) playlistInflater.inflate
                (R.layout.playlist, parent, false);
        //get title and artist TextViews
        TextView titleView = (TextView)playlistLayout.findViewById(R.id.playlist_title);
        TextView numSongsView = (TextView)playlistLayout.findViewById(R.id.num_songs);
        //get song using position
        Playlist currPlaylist = playlists.get(position);
        //get title and number of songs
        titleView.setText(currPlaylist.getTitle());
        numSongsView.setText(getNumSongs(currPlaylist.getNumSongs()));
        //set position as tag
        playlistLayout.setTag(position);
        return playlistLayout;
    }

    public String getNumSongs(int numSongs) {
         return "Songs: " + numSongs;
    }
}
