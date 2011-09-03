
package com.danga.squeezer;

import java.util.List;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.danga.squeezer.itemlists.IServiceSongListCallback;
import com.danga.squeezer.model.SqueezerSong;
import com.danga.squeezer.service.ISqueezeService;
import com.danga.squeezer.service.SongCache.Songs;
import com.danga.squeezer.service.SongCacheProvider;
import com.danga.squeezer.service.SqueezeService;
import com.danga.squeezer.service.SqueezerServerState;

public class SongsListActivity extends FragmentActivity implements
        SongsListFragment.OnSongSelectedListener {
    @SuppressWarnings("unused")
    private static final String TAG = SongsListActivity.class.getName();

    /** The number of items per page. */
    private final int mPageSize = Squeezer.getContext().getResources()
            .getInteger(R.integer.PageSize);

    private static final int PROGRESS_DIALOG = 0;
    private ProgressDialog progressDialog;

    private SongCacheProvider mProvider;
    private ContentProviderClient mContentProviderClient;

    private ISqueezeService mService;
    private final ServiceConnection serviceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName name, IBinder binder) {
            mService = ISqueezeService.Stub.asInterface(binder);
        }

        public void onServiceDisconnected(ComponentName name) {
            mService = null;
        };
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.songs_list_activity);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Get a reference to the content provider.
        mContentProviderClient = getContentResolver().acquireContentProviderClient(
                Songs.CONTENT_URI);
        mProvider = (SongCacheProvider) mContentProviderClient.getLocalContentProvider();

        // Bind the service.
        Intent intent = new Intent(this, SqueezeService.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onPause() {
        super.onPause();

        mContentProviderClient = null;
        mProvider = null;

        try {
            mService.unregisterSongListCallback(songListCallback);
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        if (serviceConnection != null)
            unbindService(serviceConnection);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.songslist, menu);
        return true;
    }

    /** OnSongSelectedListener methods. */

    public void onSongSelected(Uri songUri) {
        Intent i = new Intent(Intent.ACTION_VIEW, songUri);
        startActivity(i);
    }

    /**
     * Process the options menu.
     *
     * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        Fragment fragment = fragmentManager.findFragmentById(R.id.list);

        switch (item.getItemId()) {
            case R.id.clear_cache:
                /**
                 * Remove the list fragment, clear the cache, and re-add the
                 * list fragment.
                 */
                if (mProvider != null) {
                    FragmentTransaction t = fragmentManager.beginTransaction();
                    t.remove(fragment);
                    t.commit();

                    mProvider.rebuildCache();

                    t = fragmentManager.beginTransaction();
                    t.add(R.id.list, new SongsListFragment());
                    t.commit();
                }

                return true;

            case R.id.preload_cache:
                /**
                 * Set up a callback to catch new songs when they arrive. The
                 * callback posts messages to a handler. The handler updates the
                 * state of the progress dialog on each message, and requests
                 * the next set of songs from the server.
                 */
                try {
                    // Remove the song list fragment, so it doesn't try and
                    // update while the cache is loaded.
                    FragmentTransaction t = fragmentManager.beginTransaction();
                    t.remove(fragment);
                    t.commit();

                    // Register the callback, enable the dialog, and start
                    // fetching songs.
                    mService.registerSongListCallback(songListCallback);
                    showDialog(PROGRESS_DIALOG);
                    mService.songs(0, "tracknum", null, null, null, null, null);
                } catch (RemoteException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

                /*
                 * The other half of this (re-adding the fragment, turning off
                 * the dialog box, etc) is in mProgressHandler.
                 */
                return true;
        }

        return false;
    }

    /**
     * Handle messages posted by songListCallback.
     * <p>
     * Either request the next set of songs if there are more to come, or re-add
     * the list fragment, clean up the dialog, and remove the songListCallback.
     */
    final Handler mProgressHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            int total = msg.arg1;
            progressDialog.setProgress(total);

            if (total >= progressDialog.getMax()) {
                dismissDialog(PROGRESS_DIALOG);

                FragmentTransaction t = getSupportFragmentManager().beginTransaction();
                t.add(R.id.list, new SongsListFragment());
                t.commit();

                try {
                    mService.unregisterSongListCallback(songListCallback);
                } catch (RemoteException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            } else if (total != 0) { // Skip the very first result received.
                try {
                    Log.v(TAG, "Calling songs(" + msg.arg2 + ")");
                    mService.songs(msg.arg2, "tracknum", null, null, null, null, null);
                } catch (RemoteException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
    };

    private final IServiceSongListCallback songListCallback = new IServiceSongListCallback.Stub() {
        /**
         * Process songs returned by the Squeezer Server.
         * <p>
         * Sends a message to the progress dialog handler so that it can update
         * the dialog and fetch the next set of songs.
         *
         * @param count Number of items as reported by squeezeserver.
         * @param start The start position of items in this update.
         * @param items New items to update in the cache
         */
        public void onSongsReceived(int count, int start, List<SqueezerSong> items)
                throws RemoteException {
            int page = start / mPageSize;
            Message msg = mProgressHandler.obtainMessage();
            msg.arg1 = start;
            msg.arg2 = (page + 1) * mPageSize; // The next page of songs to
                                               // fetch.
            mProgressHandler.sendMessage(msg);
        }

        public void onServerStateChanged(SqueezerServerState oldState, SqueezerServerState newState)
                throws RemoteException {
        }

        public void onItemsFinished() throws RemoteException {
            // TODO Auto-generated method stub

        }
    };

    /** Dialog methods */

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case PROGRESS_DIALOG:
                progressDialog = new ProgressDialog(this);
                progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                progressDialog.setMessage("Loading..."); // TODO: Localise.
                return progressDialog;
            default:
                return null;
        }
    }

    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {
        switch (id) {
            case PROGRESS_DIALOG:
                progressDialog.setProgress(0);
                try {
                    progressDialog.setMax(mService.getServerState().getTotalSongs());
                } catch (RemoteException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                break;
        }
    }
}
