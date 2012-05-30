package uk.org.ngo.squeezer.actionbarcompat;

import java.util.ArrayList;
import java.util.List;

import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.Util;
import android.app.Activity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnKeyListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.PopupWindow;

/**
 * An adapter for items in a popup menu.
 */
public class PopupMenuAdapter extends BaseAdapter {
    private List<MenuItem> items = new ArrayList<MenuItem>();
    private LayoutInflater layoutInflater;

    private PopupMenuAdapter(LayoutInflater layoutInflater, Menu menu) {
        for (int i = 0; i < menu.size(); i++) {
            MenuItem item = menu.getItem(i);
            if (item.isVisible())
                this.items.add(item);
        }
        this.layoutInflater = layoutInflater;
    }


    public int getCount() {
        return items.size();
    }

    public MenuItem getItem(int position) {
        return items.get(position);
    }
 
    public long getItemId(int position) {
        return items.get(position).getItemId();
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        return getView(items.get(position), convertView, parent);
    }

    public View getView(MenuItem item, View convertView, ViewGroup parent) {
        View itemView = Util.getListItemView(layoutInflater, R.layout.list_item, convertView, item.getTitle());
        itemView.setEnabled(item.isEnabled());
        return itemView;
    }
    
    @Override
    public boolean areAllItemsEnabled() { return false; }

    @Override
    public boolean isEnabled(int position) {
        return items.get(position).isEnabled();
    }
    
    /**
     * Calculate the width of this this popup, as the maximum width of the visible items
     * 
     * @return The calculated witdh
     */
    private int getWidth() {
        View convertView = null;
        int width = 0;
        for (MenuItem item: items) {
            convertView = getView(item, null, null); // Apparently we can't reuse view, when measure is called, ... strange
            convertView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
            width = Math.max(width, convertView.getMeasuredWidth());
        }
        return width;
    }
    
    /**
     * Show a popup menu for the supplied menu.
     * 
     * @param button The action bar button which invoked the popup menu
     * @param menu
     * @param activity
     */
    public static void show(final View button, final Activity activity, final Menu menu, final PopupMenuListener callback) {
        List<MenuItem> menuItems = new ArrayList<MenuItem>();
        for (int i = 0; i < menu.size(); i++) menuItems.add(menu.getItem(i));

        final PopupMenuAdapter menuAdapter = new PopupMenuAdapter(activity.getLayoutInflater(), menu);
        int width = menuAdapter.getWidth();
        
        //TODO Android complains that listView is leaked on orientation change with the popup menu showing
        final ListView listView = (ListView) activity.getLayoutInflater().inflate(R.layout.overflowmenu_compat, null, false);
        final PopupWindow popupWindow = new PopupWindow(listView, width, LayoutParams.WRAP_CONTENT, true);
        listView.setAdapter(menuAdapter);
        listView.setOnTouchListener(new OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN && (event.getX() < 0 || event.getY() < 0 || event.getY() > listView.getHeight())) {
                    popupWindow.dismiss();
                    return true;
                }
                return false;
            }
        });
        listView.setOnKeyListener(new OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_BACK)) {
                    popupWindow.dismiss();
                    return true;
                }
                return false;
            }
        });
        listView.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> arg0, View view, int position, long id) {
                popupWindow.dismiss();
                callback.onMenuItemSelected(menuAdapter.getItem(position));
            }
        });
        popupWindow.showAsDropDown(button);
    }

    public interface PopupMenuListener {
        void onMenuItemSelected(MenuItem menuItem);
    }

}