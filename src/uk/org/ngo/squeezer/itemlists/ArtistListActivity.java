package uk.org.ngo.squeezer.itemlists;

import uk.org.ngo.squeezer.framework.SqueezerBaseActivity;
import uk.org.ngo.squeezer.framework.SqueezerItem;
import uk.org.ngo.squeezer.menu.MenuFragment;
import uk.org.ngo.squeezer.menu.SqueezerFilterMenuItemFragment;
import uk.org.ngo.squeezer.model.SqueezerAlbum;
import uk.org.ngo.squeezer.model.SqueezerGenre;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;

/**
 * Lists all artists.
 * <p>
 * Possibly filtered to show only artists that match a particular genre or album.
 * <p>
 * Hosts an {@link ArtistListFragment} to do the listing,
 */
public class ArtistListActivity extends SqueezerBaseActivity {

    private SqueezerAlbum mAlbum;
    private SqueezerGenre mGenre;

    /**
     * Shows the activity.
     * 
     * @param context
     * @param items A list of 0, 1, or 2 {@link SqueezerItem}s. Only {@link SqueezerAlbum} or
     *            {@link SqueezerGenre} are acceptable. If present, the list of artists will be
     *            filtered to show only those that match the given album and/or genre.
     */
    public static void show(Context context, SqueezerItem... items) {
        final Intent intent = new Intent(context, SqueezerArtistListActivity.class);
        for (SqueezerItem item : items)
            intent.putExtra(item.getClass().getName(), item);
        context.startActivity(intent);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        MenuFragment.add(this, SqueezerFilterMenuItemFragment.class);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            for (String key : extras.keySet()) {
                if (SqueezerAlbum.class.getName().equals(key)) {
                    mAlbum = extras.getParcelable(key);
                } else if (SqueezerGenre.class.getName().equals(key)) {
                    mGenre = extras.getParcelable(key);
                } else
                    Log.e(getTag(), "Unexpected extra value: " + key + "("
                            + extras.get(key).getClass().getName() + ")");
            }
        }
    }

    @Override
    protected void onServiceConnected() throws RemoteException {
        // TODO Auto-generated method stub

    }

}
