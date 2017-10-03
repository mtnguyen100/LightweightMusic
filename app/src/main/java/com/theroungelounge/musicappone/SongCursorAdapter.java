package com.theroungelounge.musicappone;

import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * Created by Rounge on 8/12/2017.
 */

public class SongCursorAdapter extends CursorAdapter {

    public SongCursorAdapter(Context context, Cursor cursor, int flags) {
        super(context, cursor, flags);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return LayoutInflater.from(context).inflate(R.layout.song, parent, false);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        TextView titleView = (TextView) view.findViewById(R.id.song_title);
        TextView artistView = (TextView) view.findViewById(R.id.song_artist);
        TextView lengthView = (TextView) view.findViewById(R.id.song_length);

        titleView.setText(cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE)));
        artistView.setText(cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)));
        lengthView.setText(
                SongAdapter.formatSongLength(
                        cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION))));
    }
}
