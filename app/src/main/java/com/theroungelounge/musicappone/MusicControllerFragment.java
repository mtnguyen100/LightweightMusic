package com.theroungelounge.musicappone;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import java.io.FileNotFoundException;
import java.io.InputStream;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link OnControllerFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link MusicControllerFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class MusicControllerFragment extends Fragment {

    private static final String LOG_TAG = MusicControllerFragment.class.getSimpleName();

    private static final String CONTROLLER_CURR_SONG = "SONG";
    private static final String CONTROLLER_URI = "URI";

    private Song currSong;
    private Uri musicUri;

    private ImageView albumImageView;
    private ImageButton prevButton;
    private ImageButton nextButton;
    private ImageButton rewindButton;
    private ImageButton fastForwardButton;
    private ImageButton pauseButton;
    private TextView titleTextView;
    private TextView artistTextView;
    private SeekBar seekBar;
    private TextView currTimeTextView;
    private TextView totalTimeTextView;

    private OnControllerFragmentInteractionListener mListener;

    public MusicControllerFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param currSong the current song that is playing or queued.
     * @return A new instance of fragment MusicControllerFragment.
     */
    public static MusicControllerFragment newInstance(Song currSong, Uri musicUri) {
        MusicControllerFragment fragment = new MusicControllerFragment();
        Bundle args = new Bundle();
        args.putSerializable(CONTROLLER_CURR_SONG, currSong);
        args.putParcelable(CONTROLLER_URI, musicUri);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle bundle = getArguments();
        if (bundle != null) {
            currSong = (Song) bundle.getSerializable(CONTROLLER_CURR_SONG);
            musicUri = bundle.getParcelable(CONTROLLER_URI);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_music_controller, container, false);

        albumImageView = (ImageView) view.findViewById(R.id.song_focus_album_cover_imageview);
        /*prevButton = (ImageButton) view.findViewById(R.id.prev_button);
        nextButton = (ImageButton) view.findViewById(R.id.next_button);
        rewindButton = (ImageButton) view.findViewById(R.id.rewind_button);
        fastForwardButton = (ImageButton) view.findViewById(R.id.ffwd_button);
        pauseButton = (ImageButton) view.findViewById(R.id.pause_button); */
        titleTextView = (TextView) view.findViewById(R.id.song_focus_title_textview);
        artistTextView = (TextView) view.findViewById(R.id.song_focus_artist_textview);

        updateControllerViews(currSong);
        /*prevButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.playPrev();
            }
        });
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.playNext();
            }
        });*/
        return view;
    }

    public void updateControllerViews(Song currSong) {
        this.currSong = currSong;
        Bitmap albumCover = getAlbumCover(currSong.getAlbumId());
        titleTextView.setText(currSong.getTitle());
        artistTextView.setText(currSong.getArtist());
        if(albumCover != null) {
            albumImageView.setImageBitmap(albumCover);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnControllerFragmentInteractionListener) {
            mListener = (OnControllerFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnListFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    private Bitmap getAlbumCover(long albumId) {
        Bitmap albumCover = null;
        Cursor cursor = null;
        try {
            cursor = getContext().getContentResolver().query(
                    musicUri,
                    new String[] {MediaStore.Audio.Media.ALBUM_ID},
                    null, null, null
            );
            cursor.moveToFirst();

            Uri albumUri = ContentUris.withAppendedId(
                    Uri.parse("content://media/external/audio/albumart"), albumId);
            Log.d(LOG_TAG, albumUri.toString());
            ContentResolver resolver = getContext().getContentResolver();
            try {
                InputStream in = resolver.openInputStream(albumUri);
                albumCover = BitmapFactory.decodeStream(in);
            } catch (FileNotFoundException e) {
                Log.e(LOG_TAG, e.getMessage());
            }
        } finally {
            if(cursor != null) {
                cursor.close();
            }
        }
        return albumCover;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnControllerFragmentInteractionListener {
        void playNext();
        void playPrev();
        void rewind();
        void fastForward();
        void pause();
    }
}
