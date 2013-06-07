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

package uk.org.ngo.squeezer.itemlists;

import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.fragment.SongListFragment;
import uk.org.ngo.squeezer.framework.SqueezerBaseActivity;
import uk.org.ngo.squeezer.itemlists.dialogs.SqueezerSongFilterDialog;
import uk.org.ngo.squeezer.itemlists.dialogs.SqueezerSongOrderDialog;
import uk.org.ngo.squeezer.menu.MenuFragment;
import uk.org.ngo.squeezer.menu.SqueezerFilterMenuItemFragment;
import uk.org.ngo.squeezer.menu.SqueezerOrderMenuItemFragment;
import uk.org.ngo.squeezer.model.QueryParameters;
import uk.org.ngo.squeezer.model.SqueezerAlbum;
import uk.org.ngo.squeezer.model.SqueezerSong;
import uk.org.ngo.squeezer.util.ImageCache.ImageCacheParams;
import uk.org.ngo.squeezer.util.ImageFetcher;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.RemoteException;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Lists all songs on a given album.
 * <p>
 * Hosts a {@link SongListFragment} to do the listing, provides a wrapper UI that includes
 * additional information about the album.
 */
public class AlbumListActivity extends SqueezerBaseActivity
 implements
        SongListFragment.OnSongSelectedListener,
 SongListFragment.GetQueryParameters,
        SqueezerFilterMenuItemFragment.SqueezerFilterableListActivity,
        SqueezerOrderMenuItemFragment.SqueezerOrderableListActivity {

    private QueryParameters mQueryParameters = new QueryParameters();

    /** The album being displayed. */
    private SqueezerAlbum mAlbum;

    /** An ImageFetcher for loading thumbnails. */
    protected ImageFetcher mImageFetcher;

    /** ImageCache parameters for the album art. */
    private ImageCacheParams mImageCacheParams;

    /** ImageView that shows album artwork. */
    private ImageView mIconView;

    /** First line of album information text. */
    private TextView mText1;

    /** Second line of album information text. */
    private TextView mText2;

    /**
     * Shows the activity displaying a single album's details.
     * 
     * @param context
     * @param album The album to show.
     */
    public static void show(Context context, SqueezerAlbum album) {
        final Intent intent = new Intent(context, AlbumListActivity.class);
        intent.putExtra(SqueezerAlbum.class.getName(), album);
        context.startActivity(intent);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.album_list_activity);

        // Get the details for the album to display.
        Bundle extras = getIntent().getExtras();
        mAlbum = extras.getParcelable(SqueezerAlbum.class.getName());
        mQueryParameters.album = mAlbum;

        MenuFragment.add(this, SqueezerFilterMenuItemFragment.class);
        MenuFragment.add(this, SqueezerOrderMenuItemFragment.class);

        // XXX: Until this is fixed the context menus won't be correct -- SqueezerSongView
        // uses this to control aspects of the context menu. This should probably be moved
        // in to SongListFragment.
        // songViewLogic.setBrowseByAlbum(album != null);
        // songViewLogic.setBrowseByArtist(artist != null);

        mIconView = (ImageView) findViewById(R.id.icon);
        mText1 = (TextView) findViewById(R.id.text1);
        mText2 = (TextView) findViewById(R.id.text2);

        mText1.setText(mAlbum.getName());
        mText2.setText(mAlbum.getArtist());

        // Get an ImageFetcher to scale artwork to the size of the icon view.
        // TODO: Refactor, this is an exact duplicate of code in SqueezerBaseListActivity,
        // and the cache should be shared between all activities and fragments.
        Resources resources = getResources();
        int iconSize = (Math.max(resources.getDimensionPixelSize(R.dimen.album_art_icon_height),
                resources.getDimensionPixelSize(R.dimen.album_art_icon_width)));
        mImageFetcher = new ImageFetcher(this, iconSize);
        mImageFetcher.setLoadingImage(R.drawable.icon_pending_artwork);
        mImageCacheParams = new ImageCacheParams(this, "artwork");
        mImageCacheParams.setMemCacheSizePercent(this, 0.12f);
    }

    @Override
    public void onResume() {
        super.onResume();

        mImageFetcher.addImageCache(getSupportFragmentManager(), mImageCacheParams);
    }


    /** OnSongSelectedListener methods. */

    public void onSongSelected(SqueezerSong song) {
        // XXX: Currently this does nothing, because the click logic is in SqueezerSongView.
        // That -- and the other views -- need to be modified to take an On*Selected listener
        // as a parameter, so that the activity can figure out what the right thing to do is
        // (which might change depending on which other fragments are active).

        // Intent i = new Intent(Intent.ACTION_VIEW, songUri);
        // startActivity(i);
    }

    // @Override
    // public SqueezerItemView<SqueezerSong> createItemView() {
    // songViewLogic = new SqueezerSongView(this);
    // return songViewLogic;
    // }


    public void showFilterDialog() {
        new SqueezerSongFilterDialog().show(getSupportFragmentManager(), "SongFilterDialog");
    }

    public void showOrderDialog() {
        new SqueezerSongOrderDialog().show(this.getSupportFragmentManager(), "OrderDialog");
    }

    @Override
    protected void onServiceConnected() throws RemoteException {
        String url;

        url = getService().getAlbumArtUrl(mAlbum.getArtwork_track_id());
        mImageFetcher.loadImage(url, mIconView);
        // registerCallback();
        // orderItems();
    }

    @Override
    public QueryParameters getQueryParameters() {
        return mQueryParameters;
    }
}
