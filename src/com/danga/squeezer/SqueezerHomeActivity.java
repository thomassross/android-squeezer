package com.danga.squeezer;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;

import com.danga.squeezer.itemlists.SqueezerRadioListActivity;
import com.danga.squeezer.service.AlbumCache;
import com.danga.squeezer.service.ProviderUri;
import com.danga.squeezer.service.SongCache;

public class SqueezerHomeActivity extends ListActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHomeMenu();
    }

    private void setHomeMenu() {
        int[] icons = new int[] {
                R.drawable.icon_nowplaying,
                R.drawable.icon_mymusic, R.drawable.icon_internet_radio,
                R.drawable.icon_my_apps, R.drawable.icon_favorites,
                R.drawable.icon_ml_albums, R.drawable.icon_ml_songs,
                R.drawable.icon_ml_artist, R.drawable.icon_ml_genres,
                R.drawable.icon_ml_years
        };
        setListAdapter(new IconRowAdapter(this, getResources().getStringArray(R.array.home_items),
                icons));
        getListView().setOnItemClickListener(onHomeItemClick);
    }

    private final OnItemClickListener onHomeItemClick = new OnItemClickListener() {
        private static final int NOW_PLAYING = 0;
        private static final int MUSIC = 1;
        private static final int INTERNET_RADIO = 2;
        private static final int APPS = 3;
        private static final int FAVORITES = 4;
        private static final int TEST_ALBUMS = 5;
        private static final int TEST_SONGS = 6;
        private static final int TEST_ARTISTS = 7;
        private static final int TEST_GENRES = 8;
        private static final int TEST_YEARS = 9;

        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            switch (position) {
                case NOW_PLAYING:
                    SqueezerActivity.show(SqueezerHomeActivity.this);
                    break;
                case MUSIC:
                    SqueezerMusicActivity.show(SqueezerHomeActivity.this);
                    break;
                case INTERNET_RADIO:
                    SqueezerRadioListActivity.show(SqueezerHomeActivity.this);
                    break;
                case APPS:
                    // TODO (kaa) implement
                    // SqueezerApplicationListActivity.show(SqueezerHomeActivity.this);
                    break;
                case FAVORITES:
                    break;
                case TEST_ALBUMS:
                    startActivity(new Intent(Intent.ACTION_VIEW,
                            AlbumCache.Albums.CONTENT_ID_URI_BASE));
                    break;
                case TEST_SONGS:
                    startActivity(new Intent(Intent.ACTION_VIEW,
                            SongCache.Songs.CONTENT_ID_URI_BASE));
                    break;
                case TEST_ARTISTS:
                    startActivity(new Intent(Intent.ACTION_VIEW,
                            ProviderUri.ARTIST.getContentIdUriBase()));
                    break;
                case TEST_GENRES:
                    startActivity(new Intent(Intent.ACTION_VIEW,
                            ProviderUri.GENRE.getContentIdUriBase()));
                    break;
                case TEST_YEARS:
                    startActivity(new Intent(Intent.ACTION_VIEW,
                            ProviderUri.YEAR.getContentIdUriBase()));
                    break;
            }
        }
    };

    public static void show(Context context) {
        final Intent intent = new Intent(context, SqueezerHomeActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        context.startActivity(intent);
    }

}
