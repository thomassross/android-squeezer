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

package uk.org.ngo.squeezer.framework;

import java.lang.reflect.Field;

import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.ReflectUtil;
import uk.org.ngo.squeezer.Util;
import uk.org.ngo.squeezer.actionbarcompat.PopupMenuAdapter;
import uk.org.ngo.squeezer.actionbarcompat.SimpleMenu;
import uk.org.ngo.squeezer.itemlists.SqueezerAlbumListActivity;
import uk.org.ngo.squeezer.itemlists.SqueezerArtistListActivity;
import uk.org.ngo.squeezer.itemlists.SqueezerSongListActivity;
import android.os.Parcelable.Creator;
import android.os.RemoteException;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

public abstract class SqueezerBaseItemView<T extends SqueezerItem> implements SqueezerItemView<T> {
	protected static final int CONTEXTMENU_BROWSE_SONGS = 0;
	protected static final int CONTEXTMENU_BROWSE_ALBUMS = 1;
	protected static final int CONTEXTMENU_BROWSE_ARTISTS = 2;
	protected static final int CONTEXTMENU_PLAY_ITEM = 3;
	protected static final int CONTEXTMENU_ADD_ITEM = 4;
	protected static final int CONTEXTMENU_INSERT_ITEM = 5;
	protected static final int CONTEXTMENU_BROWSE_ALBUM_SONGS = 6;
	protected static final int CONTEXTMENU_BROWSE_ARTIST_ALBUMS = 7;
	protected static final int CONTEXTMENU_BROWSE_ARTIST_SONGS = 8;

	private final SqueezerItemListActivity activity;
    private final LayoutInflater layoutInflater;
	private SqueezerItemAdapter<T> adapter;
	private Class<T> itemClass;
	private Creator<T> creator;

	public SqueezerBaseItemView(SqueezerItemListActivity activity) {
		this.activity = activity;
        layoutInflater = activity.getLayoutInflater();
	}

    protected String getTag() {
        return getClass().getSimpleName();
    }

	public SqueezerItemListActivity getActivity() {
		return activity;
	}

	public LayoutInflater getLayoutInflater() {
        return layoutInflater;
    }

    public SqueezerItemAdapter<T> getAdapter() {
		return adapter;
	}

	public void setAdapter(SqueezerItemAdapter<T> adapter) {
		this.adapter = adapter;
	}

	@SuppressWarnings("unchecked")
	public Class<T> getItemClass() {
		if (itemClass  == null) {
			itemClass = (Class<T>) ReflectUtil.getGenericClass(getClass(), SqueezerItemView.class, 0);
			if (itemClass  == null)
				throw new RuntimeException("Could not read generic argument for: " + getClass());
		}
		return itemClass;
	}

	@SuppressWarnings("unchecked")
	public Creator<T> getCreator() {
		if (creator == null) {
			Field field;
			try {
				field = getItemClass().getField("CREATOR");
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
			try {
				creator = (Creator<T>) field.get(null);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		return creator;
	}

    public View getAdapterView(View convertView, int index, T item) {
        return getDefaultAdapterView(convertView, index, item);
    }

    public View getDefaultAdapterView(View convertView, final int index, T item) {
        ViewHolder viewHolder;

        if (convertView == null || convertView.getTag() == null) {
            convertView = layoutInflater.inflate(R.layout.list_row, null);

            viewHolder = new ViewHolder();
            viewHolder.label = (TextView) convertView.findViewById(R.id.label);
            viewHolder.contextMenu = (ImageView) convertView.findViewById(R.id.contextMenu);

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        viewHolder.label.setText(item.getName());
        setupContextMenu(viewHolder.contextMenu, index, item);

        return convertView;
    }

	public View getAdapterView(View convertView, String label) {
		return Util.getListItemView(layoutInflater, R.layout.list_item, convertView, label);
	}

    private static class ViewHolder {
        TextView label;
        ImageView contextMenu;
    }


	/**
	 * The default context menu handler handles some common actions.
	 * Each action must be set up in {@link #setupContextMenu(android.view.ContextMenu, int, SqueezerItem)}
	 */
	public boolean doItemContext(MenuItem menuItem, int index, T selectedItem) throws RemoteException {
		switch (menuItem.getItemId()) {
		case CONTEXTMENU_BROWSE_SONGS:
			SqueezerSongListActivity.show(activity, selectedItem);
			return true;
		case CONTEXTMENU_BROWSE_ALBUMS:
			SqueezerAlbumListActivity.show(activity, selectedItem);
			return true;
		case CONTEXTMENU_BROWSE_ARTISTS:
			SqueezerArtistListActivity.show(activity, selectedItem);
			return true;
		case CONTEXTMENU_PLAY_ITEM:
			activity.play((SqueezerPlaylistItem) selectedItem);
			return true;
		case CONTEXTMENU_ADD_ITEM:
			activity.add((SqueezerPlaylistItem) selectedItem);
			return true;
		case CONTEXTMENU_INSERT_ITEM:
			activity.insert((SqueezerPlaylistItem) selectedItem);
			return true;
		}
		return false;
	}

	/**
	 * Setup the handler for the context menu
	 * @param button
	 * @param index
	 * @param item
	 */
    protected void setupContextMenu(final View button, final int index, final T item) {
        button.setVisibility(View.VISIBLE);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                showContextMenu(button, index, item);
            }
        });
    }
    
    private void showContextMenu(final View button, final int index, final T selectedItem) {
        Menu contextMenu = new SimpleMenu(activity);
        setupContextMenu(contextMenu, index, selectedItem);
        PopupMenuAdapter.show(button, activity, contextMenu, new PopupMenuAdapter.PopupMenuListener() {
            public void onMenuItemSelected(MenuItem menuItem) {
                try {
                    doItemContext(menuItem, index, selectedItem);
                } catch (RemoteException e) {
                    Log.e(getTag(), "Error performing context menu action '"+ menuItem + "' for '" + selectedItem + "': " + e);
                }
            }
        });
    }

}
