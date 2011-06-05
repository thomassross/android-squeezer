
package com.danga.squeezer;

import android.app.ListActivity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

public class AlbumListActivity extends ListActivity {
    private static final String TAG = AlbumsListActivity.class.getName();
    private ListView mListView = null;

    /*
     * (non-Javadoc)
     * @see android.app.Activity#onCreate(android.os.Bundle)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.album_list_activity);

        // Get the URI for the album to display
        Intent intent = getIntent();
        Uri albumUri = intent.getData();

        // Create a static header showing album information
        mListView = getListView();
        View headerView = getLayoutInflater().inflate(R.layout.album_list_header, mListView, false);
        mListView.addHeaderView(headerView, null, false);

        Toast.makeText(getApplicationContext(),
                "In AlbumListActivity " + albumUri,
                Toast.LENGTH_SHORT).show();
    }

}
