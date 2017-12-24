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
import android.util.Log;
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

    private ArrayList<Song> songList;           //Stores songs from the hard drive

    private Uri musicUri;

    private SongListFragment songListFragment;
    private MusicControllerFragment musicControllerFragment;
    private LinearLayout musicControllerTextLinearLayout;
    private ImageButton playPauseButton;
    private ImageButton prevButton;
    private ImageButton nextButton;
    private TextView songTitleTextView;
    private TextView songArtistTextView;

    private MusicService musicSrv;              //Service used to stream music
    private MusicUpdateReceiver musicUpdateReceiver; //Receiver to update the controller views
    private Intent playIntent;                  //Binds the MusicService to MainActivity
    private boolean musicBound = false;         //Checks if the MainActivity is still bound to MusicService

    private MusicController controller;         //The media controller for playing/pausing music
    private boolean paused = false, playbackPaused = true, songsNotSynced;
    public static boolean searchSongPlayed;

    /**
     * Connects the MusicService to the MainActivity.
     * Methods check whether or not the MusicService is
     * still bound to the MainActivity, and uses
     * the musicBound boolean to verify.
     */
    private ServiceConnection musicConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.MusicBinder binder = (MusicService.MusicBinder) service;
            //get service
            musicSrv = binder.getService();
            Intent mainActivityIntent = getIntent();
            if(mainActivityIntent.hasExtra(SearchableActivity.EXTRA_SEARCH_SONG_LIST)
                    && !searchSongPlayed) {
                onSongPicked(mainActivityIntent
                        .getIntExtra(SearchableActivity.EXTRA_SEARCH_SONG_LIST_POSITION, 0));
                //TODO: figure out a better solution to determine if a searched song is playing
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

    private class MusicUpdateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals(MusicService.SONG_PLAYING_TAG)) {
                updateControllerTextViews(musicSrv.getSongIndex());
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(LOG_TAG, "MainActivity onCreate called.");
        setContentView(R.layout.activity_main);

        musicUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

        musicControllerTextLinearLayout = findViewById(R.id.music_controller_info_linearlayout);
        playPauseButton = findViewById(R.id.music_controller_play_imagebutton);
        prevButton = findViewById(R.id.music_controller_prev_imagebutton);
        nextButton = findViewById(R.id.music_controller_next_imagebutton);
        songTitleTextView = findViewById(R.id.music_controller_title_textview);
        songArtistTextView = findViewById(R.id.music_controller_artist_textview);

        Intent mainActivityIntent = getIntent();
        if (mainActivityIntent.hasExtra(SearchableActivity.EXTRA_SEARCH_SONG_LIST)) {
            songList = (ArrayList<Song>) getIntent()
                    .getSerializableExtra(SearchableActivity.EXTRA_SEARCH_SONG_LIST);
            songsNotSynced = true;
        } else {
            songList = getSongList();
            sortByName();
        }
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

    /**
     * Tells the MusicService to play the selected Song
     * Updated by Rounge on 6/8/2016
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
        updateControllerTextViews(songIndex);
        musicSrv.setSong(songIndex);
        musicSrv.playSong();
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
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
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
            //TODO: fix the shuffle button so that the controller TextViews get updated
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
     *
     * @param songTitle    title of the current song
     * @param musicCursor  cursor used in getSongList()
     * @param artistColumn artist column used by getSongList
     * @return artist obtained by words before the dash('-')
     */
    public static String getArtist(String songTitle, Cursor musicCursor, int artistColumn) {
        if (songTitle.contains("-")) {
            return songTitle.substring(0, songTitle.indexOf("-"));
        } else {
            return musicCursor.getString(artistColumn);
        }
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
     * Terminates the MusicService which stops
     * Songs from playing, and terminates
     * the program.
     */
    @Override
    protected void onDestroy() {
        stopService(playIntent);
        musicSrv = null;
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        super.onPause();
        paused = true;
        if (musicUpdateReceiver != null) { unregisterReceiver(musicUpdateReceiver); }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(musicUpdateReceiver == null) { musicUpdateReceiver = new MusicUpdateReceiver(); }
        IntentFilter intentFilter = new IntentFilter(MusicService.SONG_PLAYING_TAG);
        registerReceiver(musicUpdateReceiver, intentFilter);
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    /**
     * Sets the MusicController and its click listeners, calling the playNext and playPrev
     * to determine what happens when the MusicController buttons are clicked.
     */
    //TODO: You could use this code to implement focus controller functionality
    private void setController() {
        if (controller == null) {
            controller = new MusicController(MainActivity.this);
            controller.setMediaPlayer(MainActivity.this);
            controller.setAnchorView(findViewById(R.id.mainLayout));
        }
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                controller.setPrevNextListeners(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        playNext(); //not the problem
                    }
                }, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        playPrev(); //not the problem
                    }
                });
                controller.setEnabled(true);
            }
        });
        t.start();
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

    private void updateControllerTextViews(int songIndex) {
        songTitleTextView.setText(songList.get(songIndex).getTitle());
        songArtistTextView.setText(songList.get(songIndex).getArtist());
        if(musicControllerFragment != null) {
            musicControllerFragment.updateControllerViews(songList.get(songIndex));
        }
    }

    //play next
    public void playNext() {
        musicSrv.playNext();
        updateControllerTextViews(musicSrv.getSongIndex());
        if (playbackPaused) {
            playbackPaused = false;
        }
    }

    //play previous
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
    public void pause() {
        playbackPaused = true;
        musicSrv.pausePlayer();
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
    public void rewind() {

    }

    @Override
    public void fastForward() {

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