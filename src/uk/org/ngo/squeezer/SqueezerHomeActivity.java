/*
 * Copyright (c) 2009 Google Inc.  All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.org.ngo.squeezer;


import uk.org.ngo.squeezer.itemlists.SqueezerRadioListActivity;
import uk.org.ngo.squeezer.service.AlbumCache;
import uk.org.ngo.squeezer.service.ProviderUri;
import uk.org.ngo.squeezer.service.SongCache;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;

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
                // R.drawable.icon_my_apps, R.drawable.icon_favorites,
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
        private static final int APPS = -1;
        private static final int FAVORITES = -2;
        private static final int TEST_ALBUMS = 3;
        private static final int TEST_SONGS = 4;
        private static final int TEST_ARTISTS = 5;
        private static final int TEST_GENRES = 6;
        private static final int TEST_YEARS = 7;

        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            switch (position) {
                case NOW_PLAYING:
                    SqueezerActivity.show(SqueezerHomeActivity.this);
                    break;
                case MUSIC:
                    SqueezerMusicActivity.show(SqueezerHomeActivity.this);
                    break;
                case INTERNET_RADIO:
                    // Uncomment these next two lines as an easy way to check crash
                    // reporting functionality.
                    // String sCrashString = null;
                    // Log.e("MyApp", sCrashString.toString());
                    SqueezerRadioListActivity.show(SqueezerHomeActivity.this);
                    break;
                case APPS:
                    // TODO (kaa) implement
                    // SqueezerApplicationListActivity.show(SqueezerHomeActivity.this);
                    break;
                case FAVORITES:
                    // Currently hidden, by commenting out the entry in strings.xml.
                    // TODO: Implement
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
