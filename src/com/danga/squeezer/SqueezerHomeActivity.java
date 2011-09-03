
package com.danga.squeezer;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;

import com.danga.squeezer.service.AlbumCache;
import com.danga.squeezer.service.ArtistCache;
import com.danga.squeezer.service.GenreCache;
import com.danga.squeezer.service.SongCache;
import com.danga.squeezer.service.YearCache;

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
                R.drawable.icon_favorites, R.drawable.icon_ml_albums,
                R.drawable.icon_ml_songs, R.drawable.icon_ml_artist,
                R.drawable.icon_ml_genres, R.drawable.icon_ml_years
        };
        setListAdapter(new IconRowAdapter(this, getResources().getStringArray(R.array.home_items),
                icons));
        getListView().setOnItemClickListener(onHomeItemClick);
    }

    private final OnItemClickListener onHomeItemClick = new OnItemClickListener() {
        private static final int NOW_PLAYING = 0;
        private static final int MUSIC = 1;
        private static final int INTERNET_RADIO = 2;
        private static final int FAVORITES = 3;
        private static final int TEST_ALBUMS = 4;
        private static final int TEST_SONGS = 5;
        private static final int TEST_ARTISTS = 6;
        private static final int TEST_GENRES = 7;
        private static final int TEST_YEARS = 8;

        //
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            switch (position) {
                case NOW_PLAYING:
                    SqueezerActivity.show(SqueezerHomeActivity.this);
                    break;
                case MUSIC:
                    SqueezerMusicActivity.show(SqueezerHomeActivity.this);
                    break;
                case INTERNET_RADIO:
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
                            ArtistCache.Artists.CONTENT_ID_URI_BASE));
                    break;
                case TEST_GENRES:
                    startActivity(new Intent(Intent.ACTION_VIEW,
                            GenreCache.Genres.CONTENT_ID_URI_BASE));
                    break;
                case TEST_YEARS:
                    startActivity(new Intent(Intent.ACTION_VIEW,
                            YearCache.Years.CONTENT_ID_URI_BASE));
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
