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

import com.google.common.base.Joiner;

import java.lang.reflect.Field;
import java.util.EnumSet;

import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.ReflectUtil;
import uk.org.ngo.squeezer.itemlists.SqueezerAlbumListActivity;
import uk.org.ngo.squeezer.itemlists.SqueezerArtistListActivity;
import uk.org.ngo.squeezer.itemlists.SqueezerSongListActivity;
import uk.org.ngo.squeezer.util.ImageFetcher;
import uk.org.ngo.squeezer.widgets.ListItemImageButton;
import uk.org.ngo.squeezer.widgets.SquareImageView;

import android.os.Parcelable.Creator;
import android.os.RemoteException;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Represents the view hierarchy for a single {@link SqueezerItem} subclass, suitable for displaying
 * in a {@link SqueezerItemListActivity}.
 * <p/>
 * This class supports views that have a {@link TextView} to display the primary information about
 * the {@link SqueezerItem} and can optionally enable additional views.  The layout is defined in
 * {@code res/layout/list_item.xml}.
 * <ul>
 *     <li>A {@link SquareImageView} suitable for displaying icons</li>
 *     <li>A second, smaller {@link TextView} for additional item information</li>
 *     <li>A {@link ListItemImageButton} that shows a disclosure triangle for a context menu</li>
 * </ul>
 * The view can display an item in one of two states.  The primary state is when the data to be
 * inserted in to the view is known, and represented by a complete {@link SqueezerItem} subclass.
 * The loading state is when the data type is known, but has not been fetched from the server yet.
 * <p/>
 * To customise the view's display create an {@link EnumSet} of {@link ViewParams} and pass it to
 * {@link #setViewParams(EnumSet)} or {@link #setLoadingViewParams(EnumSet)} depending on whether
 * you want to change the layout of the view in its primary state or the loading state. For example,
 * if the primary state should show a context button you may not want to show that button while
 * waiting for data to arrive.
 * <p/>
 * Override {@link #bindView(View, SqueezerItem, ImageFetcher)} and {@link #bindView(View, String)}
 * to control how data from the item is inserted in to the view.
 * <p/>
 * If you need a completely custom view hierarchy then override {@link #getAdapterView(View,
 * ViewGroup, EnumSet)} and {@link #getAdapterView(View, ViewGroup, String)}.
 *
 * @param <T> the SqueezerItem subclass this view represents.
 */
public abstract class SqueezerBaseItemView<T extends SqueezerItem> implements SqueezerItemView<T> {
    protected static final int BROWSE_ALBUMS = 1;

    private final SqueezerItemListActivity mActivity;
    private final LayoutInflater mLayoutInflater;

    private SqueezerItemAdapter<T> mAdapter;
	private Class<T> mItemClass;
    private Creator<T> mCreator;

    /**
     * Parameters that control which additional views will be enabeld in the item view.
     */
    public enum ViewParams {
        /**
         * Adds a {@link SquareImageView} for displaying album
         * artwork or other iconography.
         */
        ICON,

        /** Adds a second line for detail information ({@code R.id.text2}). */
        TWO_LINE,

        /** Adds a button (with click handler) to display the context menu. */
        CONTEXT_BUTTON
    }

    /** View parameters for a filled-in view.  One primary line with context button. */
    private EnumSet<ViewParams> mViewParams = EnumSet.of(ViewParams.CONTEXT_BUTTON);

    /** View parameters for a view that is loading data.  Primary line only. */
    private EnumSet<ViewParams> mLoadingViewParams = EnumSet.noneOf(ViewParams.class);

    /**
     * A ViewHolder for the views that make up a complete list item.
     */
    public static class ViewHolder {
        public ImageView icon;
        public TextView text1;
        public TextView text2;
        public ImageButton btnContextMenu;
        public EnumSet<ViewParams> viewParams;
    }

    /** Joins elements together with ' - ', skipping nulls. */
    protected static final Joiner mJoiner = Joiner.on(" - ").skipNulls();

    public SqueezerBaseItemView(SqueezerItemListActivity activity) {
        this.mActivity = activity;
        mLayoutInflater = activity.getLayoutInflater();
    }

    @Override
    public SqueezerItemListActivity getActivity() {
        return mActivity;
    }

    public SqueezerItemAdapter<T> getAdapter() {
        return mAdapter;
    }

    public void setAdapter(SqueezerItemAdapter<T> adapter) {
        mAdapter = adapter;
    }

    public LayoutInflater getLayoutInflater() {
        return mLayoutInflater;
    }

    /** Set the view parameters to use for the view when data is loaded. */
    protected void setViewParams(EnumSet<ViewParams> viewParams) {
        mViewParams = viewParams;
    }

    /** Set the view parameters to use for the view while data is being loaded. */
    protected void setLoadingViewParams(EnumSet<ViewParams> viewParams) {
        mLoadingViewParams = viewParams;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Class<T> getItemClass() {
        if (mItemClass == null) {
            mItemClass = (Class<T>) ReflectUtil.getGenericClass(getClass(), SqueezerItemView.class,
                    0);
            if (mItemClass == null)
                throw new RuntimeException("Could not read generic argument for: " + getClass());
        }
        return mItemClass;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Creator<T> getCreator() {
        if (mCreator == null) {
            Field field;
            try {
                field = getItemClass().getField("CREATOR");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            try {
                mCreator = (Creator<T>) field.get(null);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return mCreator;
    }

    protected String getTag() {
        return getClass().getSimpleName();
    }

    /**
     * Returns a view suitable for displaying the data of item in a list. Item may not be null.
     * <p>
     * Override this method and {@link #getAdapterView(View, ViewGroup, String)}
     * if your subclass uses a different layout.
     */
    @Override
    public View getAdapterView(View convertView, ViewGroup parent, T item, ImageFetcher imageFetcher) {
        View view = getAdapterView(convertView, parent, mViewParams);
        bindView(view, item, imageFetcher);
        return view;
    }

    /**
     * Binds the item's name to {@link ViewHolder#text1}.
     * <p>
     * OVerride this instead of
     * {@link #getAdapterView(View, ViewGroup, SqueezerItem, ImageFetcher)}
     * if the default layouts are sufficient.
     *
     * @param view The view that contains the {@link ViewHolder}
     * @param item The item to be bound
     * @param imageFetcher An {@link ImageFetcher} (may be <code>null</code>)
     */
    public void bindView(View view, T item, ImageFetcher imageFetcher) {
        ViewHolder viewHolder = (ViewHolder) view.getTag();

        viewHolder.text1.setText(item.getName());
    }

    /**
     * Returns a view suitable for displaying the "Loading..." text.
     * <p>
     * Override this method and {@link #getAdapterView(View, ViewGroup, SqueezerItem, ImageFetcher)}
     * if your extension uses a different layout.
     */
    @Override
    public View getAdapterView(View convertView, ViewGroup parent, String text) {
        View view = getAdapterView(convertView, parent, mLoadingViewParams);
        bindView(view, text);
        return view;
    }

    /**
     * Binds the text to {@link ViewHolder#text1}.
     *
     * Override this instead of {@link #getAdapterView(View, ViewGroup, String)} if the default
     * layout is sufficient.
     *
     * @param view The view that contains the {@link ViewHolder}
     * @param text The text to set in the view.
     */
    public void bindView(View view, String text) {
        ViewHolder viewHolder = (ViewHolder) view.getTag();

        viewHolder.text1.setText(text);
    }

    /**
     * Creates a view from {@code convertView} and the {@code viewParams} using the default
     * layout {@link R.layout#list_item}
     *
     * @param convertView View to reuse if possible.
     * @param parent The {@link ViewGroup} to inherit properties from.
     * @param viewParams A set of 0 or more {@link ViewParams} to customise the view.
     * @return convertView if it can be reused, or a new view
     */
    public View getAdapterView(View convertView, ViewGroup parent, EnumSet<ViewParams> viewParams) {
        return getAdapterView(convertView, parent, viewParams, R.layout.list_item);
    }

    /**
     * Creates a view from {@code convertView} and the {@code viewParams}.
     * 
     * @param convertView View to reuse if possible.
     * @param parent The {@link ViewGroup} to inherit properties from.
     * @param viewParams A set of 0 or more {@link ViewParams} to customise the view.
     * @param layoutResource The layout resource defining the item view
     * @return convertView if it can be reused, or a new view
     */
    public View getAdapterView(View convertView, ViewGroup parent, EnumSet<ViewParams> viewParams, int layoutResource) {
        ViewHolder viewHolder = (convertView != null && convertView.getTag().getClass() == ViewHolder.class)
                ? (ViewHolder) convertView.getTag()
                : null;

        if (viewHolder == null) {
            convertView = getLayoutInflater().inflate(layoutResource, parent, false);
            viewHolder = new ViewHolder();
            viewHolder.text1 = (TextView) convertView.findViewById(R.id.text1);
            viewHolder.text2 = (TextView) convertView.findViewById(R.id.text2);
            viewHolder.icon = (ImageView) convertView.findViewById(R.id.icon);
            viewHolder.btnContextMenu = (ImageButton) convertView.findViewById(R.id.context_menu);
            convertView.setTag(viewHolder);
        }

        // If the view parameters are different then reset the visibility of child views and hook
        // up any standard behaviours.
        if (! viewParams.equals(viewHolder.viewParams)) {
            viewHolder.icon.setVisibility(viewParams.contains(ViewParams.ICON) ? View.VISIBLE : View.GONE);
            viewHolder.text2.setVisibility(viewParams.contains(ViewParams.TWO_LINE) ? View.VISIBLE : View.GONE);

            if (viewParams.contains(ViewParams.CONTEXT_BUTTON)) {
                viewHolder.btnContextMenu.setVisibility(View.VISIBLE);
                viewHolder.btnContextMenu.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        v.showContextMenu();
                    }
                });
            } else {
                viewHolder.btnContextMenu.setVisibility(View.GONE);
            }

            viewHolder.viewParams = viewParams;
        }

        return convertView;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
            SqueezerItemView.ContextMenuInfo menuInfo) {
        menu.setHeaderTitle(menuInfo.item.getName());
    }

    /**
     * The default context menu handler handles some common actions. Each action
     * must be set up in
     * {@link #setupContextMenu(android.view.ContextMenu, int, SqueezerItem)}
     */
    @Override
    public boolean doItemContext(MenuItem menuItem, int index, T selectedItem)
            throws RemoteException {
        switch (menuItem.getItemId()) {
            case R.id.browse_songs:
                SqueezerSongListActivity.show(mActivity, selectedItem);
                return true;

            case BROWSE_ALBUMS:
                SqueezerAlbumListActivity.show(mActivity, selectedItem);
                return true;

            case R.id.browse_artists:
                SqueezerArtistListActivity.show(mActivity, selectedItem);
                return true;

            case R.id.play_now:
                mActivity.play((SqueezerPlaylistItem) selectedItem);
                return true;

            case R.id.add_to_playlist:
                mActivity.add((SqueezerPlaylistItem) selectedItem);
                return true;

            case R.id.play_next:
                mActivity.insert((SqueezerPlaylistItem) selectedItem);
                return true;
        }
        return false;
    }
}
