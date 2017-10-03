package com.theroungelounge.musicappone;

import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import java.util.ArrayList;

public class PlaylistsActivity extends AppCompatActivity {

    private ArrayList<Playlist> playlists;
    private ListView playlistView;
    private Uri musicUri;
    private Intent intent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playlists);

        playlistView = (ListView) findViewById(R.id.playlist_list);
        playlists = getPlayLists();

        intent = new Intent();
        PlaylistAdapter playlistAdapter = new PlaylistAdapter(this, playlists);
        playlistView.setAdapter(playlistAdapter);
        playlistView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                intent.putExtra(Intent.EXTRA_TEXT, getSongsFromPlaylist(playlists.get(position)));
                intent.putExtra("PLAYLIST_ID", playlists.get(position).getId());
                setResult(RESULT_OK, intent);
                finish();
            }
        });
    }

    public ArrayList<Playlist> getPlayLists() {
        ArrayList<Playlist> thePlaylists = new ArrayList<Playlist>();
        ContentResolver contentResolver = getContentResolver();
        Uri playlistUri =  MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI;
        Cursor playlistCursor = contentResolver.query(playlistUri, null, null, null, null);
        if (playlistCursor != null && playlistCursor.moveToFirst()) {
            //get columns
            int titleColumn = playlistCursor.getColumnIndex
                    (MediaStore.Audio.Playlists.NAME);
            int idColumn = playlistCursor.getColumnIndex
                    (MediaStore.Audio.Playlists._ID);
            //add songs to list
            do {
                long thisId = playlistCursor.getLong(idColumn);
                String thisName = playlistCursor.getString(titleColumn);
                Cursor membersCursor = contentResolver.query(MediaStore.Audio.Playlists.Members
                        .getContentUri("external", thisId), null, null, null, null);
                int thisNumSongs;
                if(membersCursor != null) {
                    thisNumSongs = membersCursor.getCount();
                    if(thisNumSongs > 0) {
                        thePlaylists.add(new Playlist(thisId, thisName, thisNumSongs));
                    }
                    membersCursor.close();
                }
            } while (playlistCursor.moveToNext());
            playlistCursor.close();
        }
        return thePlaylists;
    }

    private ArrayList<Song> getSongsFromPlaylist(Playlist playlist) {
        ArrayList<Song> theSongs = new ArrayList<Song>();
        Cursor playlistCursor = getContentResolver().query(MediaStore.Audio.Playlists.Members
                        .getContentUri("external", playlist.getId()),
                new String[]{
                        BaseColumns._ID,
                        MediaStore.Audio.AudioColumns.TITLE,
                        MediaStore.Audio.AudioColumns.ARTIST,
                        MediaStore.Audio.AudioColumns.DURATION,
                        MediaStore.Audio.AudioColumns.ALBUM_ID},
                null, null, null);
        if(playlistCursor != null) {
            int idIndex = playlistCursor.getColumnIndex(BaseColumns._ID);
            int titleIndex = playlistCursor.getColumnIndex(MediaStore.Audio.AudioColumns.TITLE);
            int artistIndex = playlistCursor.getColumnIndex(MediaStore.Audio.AudioColumns.ARTIST);
            int durationIndex = playlistCursor.getColumnIndex(
                    MediaStore.Audio.AudioColumns.DURATION);
            int albumIdIndex = playlistCursor.getColumnIndex(MediaStore.Audio.AudioColumns.ALBUM_ID);
            while(playlistCursor.moveToNext()) {
                Song song = new Song(
                        playlistCursor.getLong(idIndex),
                        playlistCursor.getString(titleIndex),
                        playlistCursor.getString(artistIndex),
                        playlistCursor.getInt(durationIndex),
                        playlistCursor.getLong(albumIdIndex));
                theSongs.add(song);
            }
            playlistCursor.close();
        }
        return theSongs;
    }
}