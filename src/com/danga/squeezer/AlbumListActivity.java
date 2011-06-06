
package com.danga.squeezer;

import java.util.HashMap;
import java.util.Map;

import android.app.ListActivity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.danga.squeezer.itemlists.IServiceSongListCallback;
import com.danga.squeezer.model.SqueezerAlbum;
import com.danga.squeezer.model.SqueezerSong;
import com.danga.squeezer.service.AlbumCache.Albums;
import com.danga.squeezer.service.ISqueezeService;
import com.danga.squeezer.service.SqueezeService;

public class AlbumListActivity extends ListActivity {
    @SuppressWarnings("unused")
    private static final String TAG = AlbumsListActivity.class.getName();
    private ListView mListView;
    private ProgressBar mProgressBar;

    private Uri mAlbumUri;
    private Cursor mCursor;

    private SqueezerAlbum mAlbum;
    private String mAlbumId;
    private String mAlbumName;
    private String mAlbumArtist;

    private ISqueezeService service;
    private final ServiceConnection serviceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName name, IBinder binder) {
            service = ISqueezeService.Stub.asInterface(binder);
            try {
                service.registerSongListCallback(songListCallback);
                AlbumListActivity.this.onServiceConnected();
            } catch (RemoteException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        public void onServiceDisconnected(ComponentName name) {
            service = null;
        };
    };

    private final SongListAdapater mSongListAdapter = new SongListAdapater();

    /*
     * @see android.app.Activity#onCreate(android.os.Bundle)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.album_list_activity);

        mListView = getListView();
        mProgressBar = (ProgressBar) findViewById(R.id.progress);

        // Get the URI for the album to display
        Intent intent = getIntent();
        mAlbumUri = intent.getData();

        // Fetch the album information
        mCursor = managedQuery(mAlbumUri, new String[] {
                Albums._ID, Albums.COL_ALBUMID, Albums.COL_NAME, Albums.COL_ARTIST,
                Albums.COL_ARTWORK_PATH
        }, null, null, null);

        mCursor.moveToFirst();
        mAlbumId = mCursor.getString(mCursor.getColumnIndex(Albums.COL_ALBUMID));
        mAlbumName = mCursor.getString(mCursor.getColumnIndex(Albums.COL_NAME));
        mAlbumArtist = mCursor.getString(mCursor.getColumnIndex(Albums.COL_ARTIST));

        mAlbum = new SqueezerAlbum(mAlbumId, mAlbumName);
        mAlbum.setArtist(mAlbumArtist);

        // Set the header with information about this album
        TextView t = (TextView) findViewById(R.id.text1);
        t.setText(mAlbumName);
        t = (TextView) findViewById(R.id.text2);
        t.setText(mAlbumArtist);

        ImageView img = (ImageView) findViewById(R.id.icon);
        img.setImageURI(Uri.parse(mCursor.getString(mCursor.getColumnIndex(Albums.COL_ARTWORK_PATH))));

        setListAdapter(mSongListAdapter);
    }

    @Override
    public void onResume() {
        super.onResume();
        bindService(new Intent(this, SqueezeService.class), serviceConnection,
                Context.BIND_AUTO_CREATE);
    }

    protected void onServiceConnected() {
        runOnUiThread(new Runnable() {
            public void run() {
                try {
                    fetchSongInformation();
                } catch (RemoteException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * Fetch song information for the current album.
     * 
     * @throws RemoteException
     */
    protected void fetchSongInformation() throws RemoteException {
        service.songs(0, "tracknum", null, mAlbum, null, null, null);
    }

    /**
     * Play the displayed album.
     * 
     * @param v The view that was clicked.
     */
    public void onPlayNow(View v) {
        try {
            service.playlistControl("load", mAlbum.getClass().getName(), mAlbum.getId());
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * Add the displayed album to the end of the current playlist.
     * 
     * @param v
     */
    public void onAddToPlaylist(View v) {
        try {
            service.playlistControl("add", mAlbum.getClass().getName(), mAlbum.getId());
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (service != null)
            unbindService(serviceConnection);
    }

    private final IServiceSongListCallback songListCallback = new IServiceSongListCallback.Stub() {
        /**
         * Process the list of songs received from the server. Add each song to
         * the mSongListAdapter at it's given position in the results.
         */
        public void onSongsReceived(int count, int start,
                java.util.List<com.danga.squeezer.model.SqueezerSong> items) {
            int i = start;

            for (SqueezerSong song : items) {
                mSongListAdapter.setItem(i, song);
                i++;
            }
        }

        /**
         * Turn off the progress spinner.
         */
        public void onItemsFinished() {
            runOnUiThread(new Runnable() {
                public void run() {
                    mProgressBar.setVisibility(View.GONE);
                }
            });
        }
    };

    private class SongListAdapater extends BaseAdapter {
        private int count;
        private final Map<Integer, SqueezerSong> songs = new HashMap<Integer, SqueezerSong>();

        public int getCount() {
            return count;
        }

        public Object getItem(int position) {
            return songs.get(position);
        }

        public long getItemId(int position) {
            return position; // TODO: Maybe the song ID instead?
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            View songView = getLayoutInflater()
                    .inflate(R.layout.album_list_entry, mListView, false);

            SqueezerSong song = songs.get(position);
            TextView t;
            String name = song.getName(); // TODO: Seen an NPE here. How is that
                                          // possible?
            String artist = song.getArtist();

            if (name != null) {
                t = (TextView) songView.findViewById(R.id.text1);
                t.setText(name);
            }

            if (artist != null) {
                t = (TextView) songView.findViewById(R.id.text2);
                t.setText(artist);
            }

            return songView;
        }

        public void setItem(int position, SqueezerSong song) {
            songs.put(position, song);
            count++;

            // Updates the view, so has to run on the UI thread
            runOnUiThread(new Runnable() {
                public void run() {
                    notifyDataSetChanged();
                }
            });
        }
    }
}
