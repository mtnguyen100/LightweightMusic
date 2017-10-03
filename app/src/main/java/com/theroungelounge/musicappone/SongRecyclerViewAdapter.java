package com.theroungelounge.musicappone;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.theroungelounge.musicappone.SongListFragment.OnListFragmentInteractionListener;

import java.util.ArrayList;

/**
 * {@link RecyclerView.Adapter} that can display a {@link Song} and makes a call to the
 * specified {@link OnListFragmentInteractionListener}.
 */
public class SongRecyclerViewAdapter extends RecyclerView.Adapter<SongRecyclerViewAdapter.ViewHolder> {

    private ArrayList<Song> mValues;
    private final OnListFragmentInteractionListener mListener;

    public SongRecyclerViewAdapter(ArrayList<Song> theSongs, OnListFragmentInteractionListener listener) {
        mValues = theSongs;
        mListener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.song, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        final int songPos = position;
        holder.mItem = mValues.get(position);
        holder.titleView.setText(mValues.get(position).getTitle());
        holder.artistView.setText(mValues.get(position).getArtist());
        holder.lengthView.setText(formatSongLength(mValues.get(position).getLength()));

        holder.mView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (null != mListener) {
                    // Notify the active callbacks interface (the activity, if the
                    // fragment is attached to one) that an item has been selected.
                    mListener.onSongPicked(songPos);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }

    public void swapValues(ArrayList<Song> values) {
        mValues = values;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final View mView;
        public final TextView titleView;
        public final TextView artistView;
        public final TextView lengthView;
        public Song mItem;

        public ViewHolder(View view) {
            super(view);
            mView = view;
            titleView = (TextView)view.findViewById(R.id.song_title);
            artistView = (TextView)view.findViewById(R.id.song_artist);
            lengthView = (TextView)view.findViewById(R.id.song_length);
        }
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
