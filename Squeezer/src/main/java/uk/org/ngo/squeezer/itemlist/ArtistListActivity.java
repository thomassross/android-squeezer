/*
 * Copyright (c) 2011 Kurt Aaholst <kaaholst@gmail.com>
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

package uk.org.ngo.squeezer.itemlist;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import uk.org.ngo.squeezer.framework.BaseListActivity;
import uk.org.ngo.squeezer.framework.Item;
import uk.org.ngo.squeezer.framework.ItemView;
import uk.org.ngo.squeezer.itemlist.GenreSpinner.GenreSpinnerCallback;
import uk.org.ngo.squeezer.itemlist.dialog.ArtistFilterDialog;
import uk.org.ngo.squeezer.menu.BaseMenuFragment;
import uk.org.ngo.squeezer.menu.FilterMenuFragment;
import uk.org.ngo.squeezer.menu.FilterMenuFragment.FilterableListActivity;
import uk.org.ngo.squeezer.model.Album;
import uk.org.ngo.squeezer.model.Artist;
import uk.org.ngo.squeezer.model.Genre;
import uk.org.ngo.squeezer.service.ISqueezeService;

public class ArtistListActivity extends BaseListActivity<Artist> implements
        GenreSpinnerCallback, FilterableListActivity {

    private String searchString = null;

    public String getSearchString() {
        return searchString;
    }

    public void setSearchString(String searchString) {
        this.searchString = searchString;
    }

    private Album album;

    public Album getAlbum() {
        return album;
    }

    public void setAlbum(Album album) {
        this.album = album;
    }

    private Genre genre;

    /** Name to use for intent data for filtering the view. */
    private static final String FILTER_KEY = ArtistListActivity.class.getName();

    @Override
    public Genre getGenre() {
        return genre;
    }

    @Override
    public void setGenre(Genre genre) {
        this.genre = genre;
    }

    @Override
    public ItemView<Artist> createItemView() {
        return new ArtistView(this);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        BaseMenuFragment.add(this, FilterMenuFragment.class);

        Intent intent = getIntent();
        Parcelable item = intent.getParcelableExtra(FILTER_KEY);
        if (item != null) {
            if (item instanceof Album) {
                album = (Album) item;
            } else if (item instanceof Genre) {
                genre = (Genre) item;
            } else {
                Log.e(getTag(), "Unexpected class as filter key: " + item.getClass());
            }
        }
    }

    @Override
    protected void orderPage(@NonNull ISqueezeService service, int start) {
        service.artists(this, start, searchString, album, genre);
    }

    @Override
    public boolean onSearchRequested() {
        showFilterDialog();
        return false;
    }

    @Override
    public void showFilterDialog() {
        new ArtistFilterDialog().show(getSupportFragmentManager(), "ArtistFilterDialog");
    }

    /**
     * Start the activity to show all artists.
     *
     * @param context the context to use to start the activity.
     */
    public static void show(Context context) {
        show(context, null);
    }

    /**
     * Start the activity to show all artists that match the given item.
     * @param context the context to use to start the activity.
     * @param item the item to filter by. If it's an {@link Album} then only artists with songs
     * on that album are shown. If it's a {@link Genre} then only artists with songs in that
     * genre are shown.
     */
    public static void show(Context context, @Nullable Item item) {
        final Intent intent = new Intent(context, ArtistListActivity.class);

        if (item != null) {
            intent.putExtra(FILTER_KEY, item);
        }

        context.startActivity(intent);
    }
}
