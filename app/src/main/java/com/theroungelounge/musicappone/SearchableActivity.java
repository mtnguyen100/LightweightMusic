package com.theroungelounge.musicappone;

import android.app.SearchManager;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import java.util.ArrayList;

public class SearchableActivity extends AppCompatActivity
        implements LoaderManager.LoaderCallbacks<Cursor> {

    public static final String EXTRA_SEARCH_SONG_LIST = "search_song_list";
    public static final String EXTRA_SEARCH_SONG_LIST_POSITION = "search_song_list_position";

    private ListView searchResultsListView;
    private SongCursorAdapter songCursorAdapter;

    private static final int SEARCH_LOADER = 0;
    private static final String SEARCH_QUERY = "query";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_searchable);

        songCursorAdapter = new SongCursorAdapter(this, null, 0);
        searchResultsListView = (ListView) findViewById(R.id.search_song_list);

        Intent intent = getIntent();
        if(Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            query = "'%" + query + "%'";

            //performSearch(query);
            Bundle bundle = new Bundle();
            bundle.putString(SEARCH_QUERY, query);
            getSupportLoaderManager().initLoader(SEARCH_LOADER, bundle, this);
            searchResultsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    Intent intent = new Intent(SearchableActivity.this, MainActivity.class);
                    ArrayList<Song> songList = getSongList(songCursorAdapter.getCursor());
                    intent.putExtra(EXTRA_SEARCH_SONG_LIST, songList);
                    intent.putExtra(EXTRA_SEARCH_SONG_LIST_POSITION, position);
                    MainActivity.searchSongPlayed = false;
                    startActivity(intent);
                }
            });
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_searchable, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.action_search:
                onSearchRequested();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private ArrayList<Song> getSongList(Cursor musicCursor) {
        ArrayList<Song> songs = new ArrayList<Song>();

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
        musicCursor.moveToFirst();
        do {
            long thisId = musicCursor.getLong(idColumn);
            String thisTitle =
                    MainActivity.getTitle(musicCursor.getString(titleColumn), musicCursor, titleColumn)
                            .trim();
            String thisArtist =
                    MainActivity.getArtist(musicCursor.getString(titleColumn), musicCursor, artistColumn)
                            .trim();
            int thisLength = musicCursor.getInt(lengthColumn);
            long thisAlbumId = musicCursor.getLong(albumIdColumn);
            songs.add(new Song(thisId, thisTitle, thisArtist, thisLength, thisAlbumId));
        } while(musicCursor.moveToNext());
        return songs;
    }

    /*private void performSearch(String query) {
        Uri musicUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Cursor musicCursor = getContentResolver().query(
                musicUri,
                null,
                MediaStore.Audio.Media.IS_MUSIC + " <> 0"
                        + " AND " +
                        MediaStore.Audio.Media.TITLE + " NOT LIKE '+%'"
                        + " AND " + "(" +
                        MediaStore.Audio.Media.TITLE + " LIKE " + query
                        + " OR " +
                        MediaStore.Audio.Media.ARTIST + " LIKE " + query + ")",
                null,
                null);
        songCursorAdapter.swapCursor(musicCursor);
        searchResultsListView.setAdapter(songCursorAdapter);
    }*/

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Uri musicUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String query = args.getString(SEARCH_QUERY);
        String sortOrder = MediaStore.Audio.Media.ARTIST + " ASC";

        return new CursorLoader(this,
                musicUri,
                new String[]{BaseColumns._ID,
                        MediaStore.Audio.AudioColumns.TITLE,
                        MediaStore.Audio.AudioColumns.ARTIST,
                        MediaStore.Audio.AudioColumns.DURATION,
                        MediaStore.Audio.AudioColumns.ALBUM_ID},
                MediaStore.Audio.Media.IS_MUSIC + " <> 0"
                        + " AND " +
                        MediaStore.Audio.Media.TITLE + " NOT LIKE '+%'"
                        + " AND " + "(" +
                        MediaStore.Audio.Media.TITLE + " LIKE " + query
                        + " OR " +
                        MediaStore.Audio.Media.ARTIST + " LIKE " + query + ")",
                null,
                sortOrder);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if(searchResultsListView.getAdapter() == null) {
            searchResultsListView.setAdapter(songCursorAdapter);
        }
        songCursorAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        songCursorAdapter.swapCursor(null);
    }
}
