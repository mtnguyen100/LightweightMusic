package com.theroungelounge.musicappone;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.MediaController.MediaPlayerControl;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class MainActivity extends AppCompatActivity implements MediaPlayerControl,
        SongListFragment.OnListFragmentInteractionListener,
        MusicControllerFragment.OnControllerFragmentInteractionListener {

    private static final String LOG_TAG = MainActivity.class.getSimpleName();
    private final String SONGLISTFRAGMENT_TAG = "SLFTAG";
    private final String CONTROLLERFRAGMENT_TAG = "CFTAG";
    private final String LIST_FOCUSED = "list_focused";
    private final String CURR_SONG = "curr_song";
    private final String SONG_LIST = "song_list";

    //Model
    private ArrayList<Song> songList;                //Stores songs from the hard drive
    private Uri musicUri;

    //Controller
    private MusicService musicSrv;                   //Service used to stream music
    private MusicUpdateReceiver musicUpdateReceiver; //Receiver to update the controller views
    private Intent playIntent;                       //Binds the MusicService to MainActivity
    private boolean musicBound = false;              //Checks binding between MainActivity and MusicService
    private boolean playbackPaused = true;           //Checks if player is currently paused
    private boolean songsNotSynced;                  //Checks if songList is synced with MusicService
    public static boolean searchSongPlayed;          //Checks if a SearchableActivity song is playing

    //View
    private SongListFragment songListFragment;
    private MusicControllerFragment musicControllerFragment;
    private LinearLayout musicControllerTextLinearLayout;
    private ImageButton playPauseButton;
    private ImageButton prevButton;
    private ImageButton nextButton;
    private TextView songTitleTextView;
    private TextView songArtistTextView;
    private MusicController controller;              //The media controller for playing/pausing music



    /* Controller methods */
    /**
     * Notifies the MusicService to play the selected Song
     * Updated by Matthew Nguyen on 6/8/2016
     *
     * @param songIndex
     */
    @Override
    public void onSongPicked(int songIndex) {
        if(songIndex < 0) { songIndex = 0; }
        if(songsNotSynced) {
            musicSrv.setList(songList);
            musicUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
            songsNotSynced = false;
        }
        if (playbackPaused) {
            playbackPaused = false;
            playPauseButton.setImageResource(android.R.drawable.ic_media_pause);
        }
        musicSrv.setSong(songIndex);
        musicSrv.playSong();
    }

    /**
     * Retrieves music files from the hard drive using musicUri, going through them with
     * the musicResolver and the musicCursor.
     * The title, ID, artist, and duration of the song is retrieved.
     */
    public ArrayList<Song> getSongList() {
        ArrayList<Song> songs = new ArrayList<Song>();
        ContentResolver musicResolver = getContentResolver();
        Uri musicUri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Cursor musicCursor = musicResolver.query(
                musicUri,
                new String[]{BaseColumns._ID,
                        MediaStore.Audio.AudioColumns.TITLE,
                        MediaStore.Audio.AudioColumns.ARTIST,
                        MediaStore.Audio.AudioColumns.DURATION,
                        MediaStore.Audio.AudioColumns.ALBUM_ID},
                MediaStore.Audio.Media.IS_MUSIC + " <> 0"
                        + " AND " +
                        MediaStore.Audio.Media.TITLE + " NOT LIKE '+%'",
                null,
                null);
        if (musicCursor != null && musicCursor.moveToFirst()) {
            //get columns
            int titleColumn = musicCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media.TITLE);
            int idColumn = musicCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media._ID);
            int artistColumn = musicCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media.ARTIST);
            int lengthColumn = musicCursor.getColumnIndex
                    (MediaStore.Audio.Media.DURATION);
            int albumIdColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID);
            //add songs to list
            do {
                long thisId = musicCursor.getLong(idColumn);
                String thisTitle =
                        getTitle(musicCursor.getString(titleColumn), musicCursor, titleColumn)
                                .trim();
                String thisArtist =
                        getArtist(musicCursor.getString(titleColumn), musicCursor, artistColumn)
                                .trim();
                int thisLength = musicCursor.getInt(lengthColumn);
                long thisAlbumId = musicCursor.getLong(albumIdColumn);
                songs.add(new Song(thisId, thisTitle, thisArtist, thisLength, thisAlbumId));
            }
            while (musicCursor.moveToNext());
        }
        return songs;
    }

    /**
     * Attempts to obtain the song name from the title's words after a dash(-).
     * If there is no dash, the method returns the default song title.
     */
    public static String getTitle(String songTitle, Cursor musicCursor, int titleColumn) {
        if (songTitle.contains("-")) {
            return songTitle.substring(songTitle.indexOf('-') + 2);
        } else {
            return musicCursor.getString(titleColumn);
        }
    }

    /**
     * Attempts to obtain the artist name from the title's words before a dash(-).
     * If there is no dash, the method returns the default artist title.
     */
    public static String getArtist(String songTitle, Cursor musicCursor, int artistColumn) {
        if (songTitle.contains("-")) {
            return songTitle.substring(0, songTitle.indexOf("-"));
        } else {
            return musicCursor.getString(artistColumn);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == RESULT_OK && requestCode == 2404) {
            if(data != null) {
                long playlistId = data.getLongExtra("PLAYLIST_ID", 0);
                songList = (ArrayList<Song>) data.getSerializableExtra(Intent.EXTRA_TEXT);
                sortByName();
                if (musicControllerFragment != null) {
                    songListFragment = SongListFragment.newInstance(songList);
                    musicControllerFragment = null;
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.main_activity_fragment_container,
                                    songListFragment, SONGLISTFRAGMENT_TAG)
                            .commit();
                }
                musicUri = MediaStore.Audio.Playlists.Members.getContentUri("external", playlistId);
                musicSrv.setContentUri(musicUri);
            }
        }
    }

    private void setShuffle() {
        Collections.shuffle(songList);
        songsNotSynced = true;
        if (songListFragment != null) {
            songListFragment = SongListFragment.newInstance(songList);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.main_activity_fragment_container,
                            songListFragment, SONGLISTFRAGMENT_TAG)
                    .commit();
        }
        onSongPicked(0);
    }

    public void sortByName() {
        Collections.sort(songList, new Comparator<Song>() {
            public int compare(Song a, Song b) {
                return a.getTitle()
                        .compareTo(b.getTitle());
            }
        });
        songsNotSynced = true;
        if(songListFragment != null) {
            songListFragment.setSongList(songList);
        }
    }

    public void sortByArtist() {
        Collections.sort(songList, new Comparator<Song>() {
            public int compare(Song a, Song b) {
                return a.getArtist()
                        .compareTo(b.getArtist());
            }
        });
        songsNotSynced = true;
        if(songListFragment != null) {
            songListFragment.setSongList(songList);
        }
    }

    public void sortByLength() {
        Collections.sort(songList, new Comparator<Song>() {
            public int compare(Song a, Song b) {
                return ((Integer) b.getLength()).compareTo(a.getLength());
            }
        });
        songsNotSynced = true;
        if(songListFragment != null) {
            songListFragment.setSongList(songList);
        }
    }

    @Override
    public boolean isPlaying() {
        if (musicSrv != null && musicBound)
            return musicSrv.isPng();
        return false;
    }

    public boolean isPaused() {
        if (musicSrv != null && musicBound && playbackPaused) {
            return true;
        }
        return false;
    }

    /**
     * Connects the MusicService to the MainActivity.
     * Determines whether the MainActivity is started
     * from a SearchableActivity and plays the searched song
     * if true.
     */
    private ServiceConnection musicConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.MusicBinder binder = (MusicService.MusicBinder) service;
            musicSrv = binder.getService();
            Intent mainActivityIntent = getIntent();
            if(mainActivityIntent.hasExtra(SearchableActivity.EXTRA_SEARCH_SONG_LIST)
                    && !searchSongPlayed) {
                musicSrv.setContentUri(musicUri);
                onSongPicked(mainActivityIntent
                        .getIntExtra(SearchableActivity.EXTRA_SEARCH_SONG_LIST_POSITION, 0));
                playPauseButton.setImageResource(android.R.drawable.ic_media_pause);
                searchSongPlayed = true;
            } else {
                updateControllerTextViews(musicSrv.getSongIndex());
                if(musicSrv.isPng()) {
                    playPauseButton.setImageResource(android.R.drawable.ic_media_pause);
                } else {
                    playPauseButton.setImageResource(android.R.drawable.ic_media_play);
                }
            }
            musicBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            musicBound = false;
        }
    };

    /**
     * This receiver updates the information on the
     * MusicController LinearLayout whenever a new
     * song begins to play.
     */
    private class MusicUpdateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals(MusicService.SONG_PLAYING_TAG)) {
                updateControllerTextViews(musicSrv.getSongIndex());
            }
        }
    }

    /* View methods */
    @SuppressWarnings("unchecked")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /* Initialize Model */
        musicUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Intent mainActivityIntent = getIntent();
        //Sets the songList to the result list from a search from the SearchableActivity
        //If there was no search, set the songList to the default list sorted by name
        if (mainActivityIntent.hasExtra(SearchableActivity.EXTRA_SEARCH_SONG_LIST)) {
            songList = (ArrayList<Song>) getIntent()
                    .getSerializableExtra(SearchableActivity.EXTRA_SEARCH_SONG_LIST);
            songsNotSynced = true;
        } else {
            songList = getSongList();
            sortByName();
        }

        //Retrieves the previous state of the MainActivity via savedInstanceState, if it exists
        //Otherwise, initialize the default listFragment
        if(savedInstanceState != null && savedInstanceState.containsKey(LIST_FOCUSED)) {
            if(savedInstanceState.containsKey(SONG_LIST)) {
                songList = (ArrayList<Song>) savedInstanceState.getSerializable(SONG_LIST);
            }
            if(savedInstanceState.getBoolean(LIST_FOCUSED)) {
                songListFragment = SongListFragment.newInstance(songList);
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.main_activity_fragment_container,
                                songListFragment, SONGLISTFRAGMENT_TAG)
                        .commit();
            } else {
                musicControllerFragment = MusicControllerFragment.newInstance(
                        (Song)savedInstanceState.getSerializable(CURR_SONG), musicUri);
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.main_activity_fragment_container,
                                musicControllerFragment,
                                CONTROLLERFRAGMENT_TAG)
                        .commit();
            }
        } else {
            songListFragment = SongListFragment.newInstance(songList);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.main_activity_fragment_container,
                            songListFragment, SONGLISTFRAGMENT_TAG)
                    .commit();
        }

        /* Initialize Views */
        musicControllerTextLinearLayout = findViewById(R.id.music_controller_info_linearlayout);
        playPauseButton = findViewById(R.id.music_controller_play_imagebutton);
        prevButton = findViewById(R.id.music_controller_prev_imagebutton);
        nextButton = findViewById(R.id.music_controller_next_imagebutton);
        songTitleTextView = findViewById(R.id.music_controller_title_textview);
        songArtistTextView = findViewById(R.id.music_controller_artist_textview);

        /* Event Listeners */
        playPauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isPlaying()) {
                    pause();
                    playPauseButton.setImageResource(android.R.drawable.ic_media_play);
                } else {
                    play();
                    playPauseButton.setImageResource(android.R.drawable.ic_media_pause);
                }
            }
        });
        prevButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(songsNotSynced) {
                    musicSrv.setList(songList);
                    songsNotSynced = false;
                }
                playPrev();
                playPauseButton.setImageResource(android.R.drawable.ic_media_pause);
            }
        });
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(songsNotSynced) {
                    musicSrv.setList(songList);
                    songsNotSynced = false;
                }
                playNext();
                playPauseButton.setImageResource(android.R.drawable.ic_media_pause);
            }
        });
        musicControllerTextLinearLayout.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if(songsNotSynced) {
                            musicSrv.setList(songList);
                            songsNotSynced = false;
                        }
                        Fragment currFragment =
                                getSupportFragmentManager()
                                        .findFragmentByTag(CONTROLLERFRAGMENT_TAG);
                        if(currFragment == null) {
                            musicControllerFragment = MusicControllerFragment.newInstance(
                                    songList.get(musicSrv.getSongIndex()), musicUri);
                            songListFragment = null;
                            getSupportFragmentManager().beginTransaction()
                                    .replace(R.id.main_activity_fragment_container,
                                            musicControllerFragment,
                                            CONTROLLERFRAGMENT_TAG)
                                    .commit();
                        } else {
                            songListFragment = SongListFragment.newInstance(songList);
                            musicControllerFragment = null;
                            getSupportFragmentManager().beginTransaction()
                                    .replace(R.id.main_activity_fragment_container,
                                            songListFragment,
                                            SONGLISTFRAGMENT_TAG)
                                    .commit();
                        }
                    }
                }
        );
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (playIntent == null) {
            playIntent = new Intent(this, MusicService.class);
            bindService(playIntent, musicConnection, Context.BIND_AUTO_CREATE);
            startService(playIntent);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(musicUpdateReceiver == null) { musicUpdateReceiver = new MusicUpdateReceiver(); }
        IntentFilter intentFilter = new IntentFilter(MusicService.SONG_PLAYING_TAG);
        registerReceiver(musicUpdateReceiver, intentFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (musicUpdateReceiver != null) { unregisterReceiver(musicUpdateReceiver); }
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if(songsNotSynced) {
            musicSrv.setList(songList);
        }
        boolean is_list_focused =
                getSupportFragmentManager().findFragmentByTag(SONGLISTFRAGMENT_TAG) != null;
        outState.putBoolean(LIST_FOCUSED, is_list_focused);
        outState.putSerializable(CURR_SONG, songList.get(musicSrv.getSongIndex()));
        outState.putSerializable(SONG_LIST, songList);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        stopService(playIntent);
        musicSrv = null;
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    private void updateControllerTextViews(int songIndex) {
        songTitleTextView.setText(songList.get(songIndex).getTitle());
        songArtistTextView.setText(songList.get(songIndex).getArtist());
        if(musicControllerFragment != null) {
            musicControllerFragment.updateControllerViews(songList.get(songIndex));
        }
    }

    /**
     * Controls the overflow menu of the program.
     *
     * @param item The MenuItem regarding the overflow menu,
     *             including options such as shuffling and ending
     *             the program.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //menu item selected
        switch (item.getItemId()) {
            case R.id.action_shuffle:
                setShuffle();
                break;
            case R.id.action_search:
                onSearchRequested();
                break;
            case R.id.action_all_songs:
                songList = getSongList();
                sortByName();
                musicSrv.setContentUri(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI);
                songsNotSynced = true;
                break;
            case R.id.goto_playlists:Intent gotoPlaylists = new Intent(this, PlaylistsActivity.class);
                startActivityForResult(gotoPlaylists, 2404);
                break;
            case R.id.action_end:
                stopService(playIntent);
                musicSrv = null;
                System.exit(0);
                break;
            case R.id.action_sort_name:
                if (item.isChecked())
                    item.setChecked(false);
                else {
                    item.setChecked(true);
                    sortByName();
                }
                break;
            case R.id.action_sort_artist:
                if (item.isChecked())
                    item.setChecked(false);
                else {
                    item.setChecked(true);
                    sortByArtist();
                }
                break;
            case R.id.action_sort_length:
                if (item.isChecked())
                    item.setChecked(false);
                else {
                    item.setChecked(true);
                    sortByLength();
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    public void play() {
        playbackPaused = false;
        if(songsNotSynced && musicSrv.getSongIndex() == 0) {
            musicSrv.setList(songList);
            musicUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
            songsNotSynced = false;
            musicSrv.setSong(0);
            musicSrv.playSong();
        } else {
            musicSrv.go();
        }
    }

    @Override
    public void pause() {
        playbackPaused = true;
        musicSrv.pausePlayer();
    }

    public void playNext() {
        musicSrv.playNext();
        updateControllerTextViews(musicSrv.getSongIndex());
        if (playbackPaused) {
            playbackPaused = false;
        }
    }

    public void playPrev() {
        musicSrv.playPrev();
        updateControllerTextViews(musicSrv.getSongIndex());
        if (playbackPaused) {
            playbackPaused = false;
        }
    }

    @Override
    public void start() {
        musicSrv.go();
    }

    @Override
    public void rewind() {
        //Optional feature - to be implemented at a later date
    }

    @Override
    public void fastForward() {
        //Optional feature - to be implemented at a later date
    }

    @Override
    public int getDuration() {
        if (isPlaying() || isPaused())
            return musicSrv.getDur();
        else return 0;
    }

    @Override
    public int getCurrentPosition() {
        if (isPlaying() || isPaused())
            return musicSrv.getPosition();
        else return 0;
    }

    @Override
    public void seekTo(int pos) {
        musicSrv.seek(pos);
    }

    @Override
    public int getBufferPercentage() {
        return 0;
    }

    @Override
    public boolean canPause() {
        return true;
    }

    @Override
    public boolean canSeekBackward() {
        return true;
    }

    @Override
    public boolean canSeekForward() {
        return true;
    }

    @Override
    public int getAudioSessionId() {
        return 0;
    }
}