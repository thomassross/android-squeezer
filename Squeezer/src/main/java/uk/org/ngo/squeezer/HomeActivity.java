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


import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.Toolbar;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;

import com.crashlytics.android.Crashlytics;

import java.util.ArrayList;
import java.util.List;

import de.cketti.library.changelog.ChangeLog;
import uk.org.ngo.squeezer.dialog.AboutDialog;
import uk.org.ngo.squeezer.dialog.TipsDialog;
import uk.org.ngo.squeezer.framework.BaseActivity;
import uk.org.ngo.squeezer.itemlist.AlarmsActivity;
import uk.org.ngo.squeezer.itemlist.AlbumListActivity;
import uk.org.ngo.squeezer.itemlist.ApplicationListActivity;
import uk.org.ngo.squeezer.itemlist.ArtistListActivity;
import uk.org.ngo.squeezer.itemlist.FavoriteListActivity;
import uk.org.ngo.squeezer.itemlist.GenreListActivity;
import uk.org.ngo.squeezer.itemlist.MusicFolderListActivity;
import uk.org.ngo.squeezer.itemlist.PlayerListActivity;
import uk.org.ngo.squeezer.itemlist.PlaylistsActivity;
import uk.org.ngo.squeezer.itemlist.RadioListActivity;
import uk.org.ngo.squeezer.itemlist.SongListActivity;
import uk.org.ngo.squeezer.itemlist.YearListActivity;
import uk.org.ngo.squeezer.itemlist.dialog.AlbumViewDialog;
import uk.org.ngo.squeezer.model.Player;
import uk.org.ngo.squeezer.service.event.HandshakeComplete;

import com.mikepenz.iconics.IconicsDrawable;
import com.mikepenz.materialdrawer.AccountHeader;
import com.mikepenz.materialdrawer.AccountHeaderBuilder;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.holder.BadgeStyle;
import com.mikepenz.materialdrawer.holder.StringHolder;
import com.mikepenz.materialdrawer.interfaces.OnCheckedChangeListener;
import com.mikepenz.materialdrawer.model.DividerDrawerItem;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.ProfileDrawerItem;
import com.mikepenz.materialdrawer.model.ProfileSettingDrawerItem;
import com.mikepenz.materialdrawer.model.SecondaryDrawerItem;
import com.mikepenz.materialdrawer.model.SectionDrawerItem;
import com.mikepenz.materialdrawer.model.SwitchDrawerItem;
import com.mikepenz.materialdrawer.model.ToggleDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IProfile;
import com.mikepenz.fontawesome_typeface_library.FontAwesome;
import com.mikepenz.google_material_typeface_library.GoogleMaterial;
import com.mikepenz.materialdrawer.model.interfaces.Nameable;
import com.mikepenz.octicons_typeface_library.Octicons;
import com.mikepenz.iconics.IconicsDrawable;


public class HomeActivity extends BaseActivity {

    private final String TAG = "HomeActivity";

    private static final int ARTISTS = 0;

    private static final int ALBUMS = 1;

    private static final int SONGS = 2;

    private static final int GENRES = 3;

    private static final int YEARS = 4;

    private static final int NEW_MUSIC = 5;

    private static final int MUSIC_FOLDER = 6;

    private static final int RANDOM_MIX = 7;

    private static final int PLAYLISTS = 8;

    private static final int INTERNET_RADIO = 9;

    private static final int FAVORITES = 10;

    private static final int MY_APPS = 11;

    private boolean mCanFavorites = false;

    private boolean mCanMusicfolder = false;

    private boolean mCanMyApps = false;

    private boolean mCanRandomplay = false;

    private ListView listView;

    private GoogleAnalyticsTracker tracker;

    private static final int PROFILE_SETTING = 1;

    //save our header or result
    private AccountHeader headerResult = null;
    private Drawer result = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!BuildConfig.DEBUG) {
            Crashlytics.start(this);
        }

        setContentView(R.layout.item_list);
        listView = (ListView) findViewById(R.id.item_list);

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        final SharedPreferences preferences = getSharedPreferences(Preferences.NAME, 0);

        // Enable Analytics if the option is on, and we're not running in debug
        // mode so that debug tests don't pollute the stats.
        if (preferences.getBoolean(Preferences.KEY_ANALYTICS_ENABLED, true)) {
            if ((getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) == 0) {
                Log.v("NowPlayingActivity", "Tracking page view 'HomeActivity");
                // Start the tracker in manual dispatch mode...
                tracker = GoogleAnalyticsTracker.getInstance();
                tracker.startNewSession("UA-26457780-1", this);
                tracker.trackPageView("HomeActivity");
            }
        }

        // Show the change log if necessary.
        ChangeLog changeLog = new ChangeLog(this);
        if (changeLog.isFirstRun()) {
            if (changeLog.isFirstRunEver()) {
                changeLog.skipLogDialog();
            } else {
                changeLog.getLogDialog().show();
            }
        }

        android.support.v7.widget.Toolbar toolbar = (android.support.v7.widget.Toolbar) findViewById(R.id.toolbar);
//        setSupportActionBar(toolbar);
//        getSupportActionBar().setTitle("HOME");

        // Create a few sample profile
        // NOTE you have to define the loader logic too. See the CustomApplication for more details
        final IProfile profile = new ProfileDrawerItem().withName("Mike Penz").withEmail("mikepenz@gmail.com").withIcon("https://avatars3.githubusercontent.com/u/1476232?v=3&s=460");
        final IProfile profile2 = new ProfileDrawerItem().withName("Bernat Borras").withEmail("alorma@github.com").withIcon(Uri.parse("https://avatars3.githubusercontent.com/u/887462?v=3&s=460"));
        final IProfile profile3 = new ProfileDrawerItem().withName("Max Muster").withEmail("max.mustermann@gmail.com").withIcon(getResources().getDrawable(R.drawable.profile2));
        final IProfile profile4 = new ProfileDrawerItem().withName("Felix House").withEmail("felix.house@gmail.com").withIcon(getResources().getDrawable(R.drawable.profile3));
        final IProfile profile5 = new ProfileDrawerItem().withName("Mr. X").withEmail("mister.x.super@gmail.com").withIcon(getResources().getDrawable(R.drawable.profile4)).withIdentifier(4);
        final IProfile profile6 = new ProfileDrawerItem().withName("Batman").withEmail("batman@gmail.com").withIcon(getResources().getDrawable(R.drawable.profile5));

        // Create the AccountHeader
        headerResult = new AccountHeaderBuilder()
                .withActivity(this)
                .withHeaderBackground(R.drawable.header)
                .addProfiles(
//                        profile,
//                        profile2,
//                        profile3,
//                        profile4,
//                        profile5,
                        profile6,
                        //don't ask but google uses 14dp for the add account icon in gmail but 20dp for the normal icons (like manage account)
                        //new ProfileSettingDrawerItem().withName("Add Account").withDescription("Add new GitHub Account").withIcon(new IconicsDrawable(this, GoogleMaterial.Icon.gmd_add).actionBarSize().paddingDp(5).colorRes(R.color.material_drawer_primary_text)).withIdentifier(PROFILE_SETTING),
                        new ProfileSettingDrawerItem().withName("Manage Players").withIcon(GoogleMaterial.Icon.gmd_settings).withIdentifier(200)
                )
                .withOnAccountHeaderListener(new AccountHeader.OnAccountHeaderListener() {
                    @Override
                    public boolean onProfileChanged(View view, IProfile profile, boolean current) {
                        //sample usage of the onProfileChanged listener
                        //if the clicked item has the identifier 1 add a new profile ;)
                        if (profile instanceof IDrawerItem && ((IDrawerItem) profile).getIdentifier() == PROFILE_SETTING) {
                            IProfile newProfile = new ProfileDrawerItem().withNameShown(true).withName("Batman").withEmail("batman@gmail.com").withIcon(getResources().getDrawable(R.drawable.profile5));
                            if (headerResult.getProfiles() != null) {
                                //we know that there are 2 setting elements. set the new profile above them ;)
                                headerResult.addProfile(newProfile, headerResult.getProfiles().size() - 2);
                            } else {
                                headerResult.addProfiles(newProfile);
                            }
                        }else if (profile instanceof IDrawerItem && ((IDrawerItem) profile).getIdentifier() == 200) {
                            Intent intent = new Intent(HomeActivity.this, PlayerListActivity.class);
                            HomeActivity.this.startActivity(intent);
                        }

                        //false if you have not consumed the event and it should close the drawer
                        return false;
                    }
                })
                .withSavedInstance(savedInstanceState)
                .build();

        //new PrimaryDrawerItem().withDescription("A more complex sample").withName("NAAM").withIcon(FontAwesome.Icon.faw_music).withIdentifier(5).withSelectable(false),
        //new PrimaryDrawerItem().withName(R.string.home_item_genres).withIcon(FontAwesome.Icon.faw_music).withIdentifier(4).withSelectable(false).withBadgeStyle(new BadgeStyle().withTextColor(Color.WHITE).withColorRes(R.color.md_red_700)), //NEE

        //Create the drawer
        result = new DrawerBuilder().addDrawerItems()
                .withActivity(this)
                .withToolbar(toolbar)
                .withAccountHeader(headerResult) //set the AccountHeader we created earlier for the header
                .addDrawerItems(
                        new PrimaryDrawerItem().withName(R.string.home_item_songs).withIcon(FontAwesome.Icon.faw_music).withIdentifier(1).withSelectable(false),
                        new PrimaryDrawerItem().withName(R.string.home_item_artists).withIcon(FontAwesome.Icon.faw_home).withIdentifier(2).withSelectable(false), //NEE
                        new PrimaryDrawerItem().withName(R.string.home_item_albums).withIcon(GoogleMaterial.Icon.gmd_album).withIdentifier(3).withSelectable(false),
                        new PrimaryDrawerItem().withName(R.string.home_item_genres).withIcon(FontAwesome.Icon.faw_music).withIdentifier(4).withSelectable(false), //NEE
                        new PrimaryDrawerItem().withName(R.string.home_item_years).withIcon(FontAwesome.Icon.faw_music).withIdentifier(5).withSelectable(false), //NEE
                        new PrimaryDrawerItem().withName(R.string.home_item_new_music).withIcon(FontAwesome.Icon.faw_music).withIdentifier(6).withSelectable(false),
                        new PrimaryDrawerItem().withName(R.string.home_item_random_mix).withIcon(FontAwesome.Icon.faw_random).withIdentifier(7).withSelectable(false),
                        new PrimaryDrawerItem().withName(R.string.home_item_playlists).withIcon(FontAwesome.Icon.faw_music).withIdentifier(8).withSelectable(false), //NEE
                        new PrimaryDrawerItem().withName(R.string.home_item_music_folder).withIcon(FontAwesome.Icon.faw_folder).withIdentifier(9).withSelectable(false),
                        new PrimaryDrawerItem().withName(R.string.home_item_radios).withIcon(GoogleMaterial.Icon.gmd_radio).withIdentifier(10).withSelectable(false),
                        new PrimaryDrawerItem().withName(R.string.home_item_favorites).withIcon(GoogleMaterial.Icon.gmd_favorite).withIdentifier(11).withSelectable(false),
                        new PrimaryDrawerItem().withName(R.string.home_item_my_apps).withIcon(GoogleMaterial.Icon.gmd_apps).withIdentifier(12).withSelectable(false),
                        new DividerDrawerItem(),
                        new PrimaryDrawerItem().withName(R.string.alarm_clock).withIcon(FontAwesome.Icon.faw_clock_o).withIdentifier(20).withSelectable(false),
                        new PrimaryDrawerItem().withName(R.string.menu_item_settings_label).withIcon(FontAwesome.Icon.faw_cog).withIdentifier(21).withSelectable(false),
                        new PrimaryDrawerItem().withName(R.string.menu_item_about_label).withIcon(FontAwesome.Icon.faw_cog).withIdentifier(22).withSelectable(false)

//                        new SwitchDrawerItem().withName("Switch").withIcon(Octicons.Icon.oct_tools).withChecked(true).withOnCheckedChangeListener(onCheckedChangeListener),
//                        new SwitchDrawerItem().withName("Switch2").withIcon(Octicons.Icon.oct_tools).withChecked(true).withOnCheckedChangeListener(onCheckedChangeListener),
//                        new ToggleDrawerItem().withName("Toggle").withIcon(Octicons.Icon.oct_tools).withChecked(true).withOnCheckedChangeListener(onCheckedChangeListener)
                ) // add the items we want to use with our Drawer
                .withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
                    @Override
                    public boolean onItemClick(View view, int position, IDrawerItem drawerItem) {
                        //check if the drawerItem is set.
                        //there are different reasons for the drawerItem to be null
                        //--> click on the header
                        //--> click on the footer
                        //those items don't contain a drawerItem

                        if (drawerItem != null) {
                            Intent intent = null;
                            if (drawerItem.getIdentifier() == 1) {
                                intent = new Intent(HomeActivity.this, SongListActivity.class);
                            } else if (drawerItem.getIdentifier() == 2) {
                                intent = new Intent(HomeActivity.this, ArtistListActivity.class);
                            } else if (drawerItem.getIdentifier() == 3) {
                                intent = new Intent(HomeActivity.this, AlbumListActivity.class);
                            } else if (drawerItem.getIdentifier() == 4) {
                                intent = new Intent(HomeActivity.this, GenreListActivity.class);
                            } else if (drawerItem.getIdentifier() == 5) {
                                intent = new Intent(HomeActivity.this, YearListActivity.class);
                            } else if (drawerItem.getIdentifier() == 6) {
                                intent = new Intent(HomeActivity.this, AlbumListActivity.class);
                                intent.putExtra(AlbumViewDialog.AlbumsSortOrder.class.getName(), AlbumViewDialog.AlbumsSortOrder.__new.name());
                            } else if (drawerItem.getIdentifier() == 7) {
                                intent = new Intent(HomeActivity.this, RandomplayActivity.class);
                            } else if (drawerItem.getIdentifier() == 8) {
                                intent = new Intent(HomeActivity.this, PlaylistsActivity.class);
                            } else if (drawerItem.getIdentifier() == 9) {
                                intent = new Intent(HomeActivity.this, MusicFolderListActivity.class);
                            } else if (drawerItem.getIdentifier() == 10) {
                                intent = new Intent(HomeActivity.this, RadioListActivity.class);
                            } else if (drawerItem.getIdentifier() == 11) {
                                intent = new Intent(HomeActivity.this, FavoriteListActivity.class);
                            } else if (drawerItem.getIdentifier() == 12) {
                                intent = new Intent(HomeActivity.this, ApplicationListActivity.class);


                            } else if (drawerItem.getIdentifier() == 20) {
                                intent = new Intent(HomeActivity.this, AlarmsActivity.class);
                            } else if (drawerItem.getIdentifier() == 21) {
                                intent = new Intent(HomeActivity.this, SettingsActivity.class);
                            } else if (drawerItem.getIdentifier() == 22) {
                                intent = new Intent(HomeActivity.this, AboutDialog.class);
                            }

                            if (intent != null) {
                                HomeActivity.this.startActivity(intent);
                            }
                        }

                        return false;
                    }
                })
                .withSavedInstance(savedInstanceState)
                .withShowDrawerOnFirstLaunch(true)
                .build();

        //only set the active selection or active profile if we do not recreate the activity
        if (savedInstanceState == null) {
            // set the selection to the item with the identifier 11
            result.setSelection(21, false);

            //set the active profile
            headerResult.setActiveProfile(profile3);
        }

        result.updateBadge(4, new StringHolder(10 + ""));



    }

    private OnCheckedChangeListener onCheckedChangeListener = new OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(IDrawerItem drawerItem, CompoundButton buttonView, boolean isChecked) {
            if (drawerItem instanceof Nameable) {
                Log.i("material-drawer", "DrawerItem: " + ((Nameable) drawerItem).getName() + " - toggleChecked: " + isChecked);
            } else {
                Log.i("material-drawer", "toggleChecked: " + isChecked);
            }
        }
    };

    public void onEventMainThread(HandshakeComplete event) {
        int[] icons = new int[]{
                R.drawable.ic_artists,
                R.drawable.ic_albums, R.drawable.ic_songs,
                R.drawable.ic_genres, R.drawable.ic_years, R.drawable.ic_new_music,
                R.drawable.ic_music_folder, R.drawable.ic_random,
                R.drawable.ic_playlists, R.drawable.ic_internet_radio,
                R.drawable.ic_favorites, R.drawable.ic_my_apps
        };

        Log.d("players", String.valueOf(getService().getPlayers()));

        List<Player> players = getService().getPlayers();

        for (int i=0; i<players.size(); i++) {
            IProfile newProfile = new ProfileDrawerItem().withNameShown(true).withName(players.get(i).getName()).withEmail(players.get(i).getIp()).withIcon(getResources().getDrawable(R.drawable.profile5));

            if (headerResult.getProfiles() != null) {
                //we know that there are 2 setting elements. set the new profile above them ;)
                headerResult.addProfile(newProfile, headerResult.getProfiles().size() - 2);
            } else {
                headerResult.addProfiles(newProfile);
            }

            if(players.get(i).getConnected()){
                headerResult.setActiveProfile(newProfile);
            }
        }

        String[] items = getResources().getStringArray(R.array.home_items);

        if (getService() != null) {
            mCanFavorites = event.canFavourites;
            mCanMusicfolder = event.canMusicFolders;
            mCanMyApps = event.canMyApps;
            mCanRandomplay = event.canRandomPlay;
        }

        List<IconRowAdapter.IconRow> rows = new ArrayList<IconRowAdapter.IconRow>(MY_APPS + 1);
        for (int i = ARTISTS; i <= MY_APPS; i++) {
            if (i == MUSIC_FOLDER && !mCanMusicfolder) {
                continue;
            }

            if (i == RANDOM_MIX && !mCanRandomplay) {
                continue;
            }

            if (i == FAVORITES && !mCanFavorites) {
                continue;
            }

            if (i == MY_APPS && !mCanMyApps) {
                continue;
            }

            rows.add(new IconRowAdapter.IconRow(i, items[i], icons[i]));
        }

        listView.setAdapter(new IconRowAdapter(this, rows));
        listView.setOnItemClickListener(onHomeItemClick);

        // Show a tip about volume controls, if this is the first time this app
        // has run. TODO: Add more robust and general 'tips' functionality.
        PackageInfo pInfo;
        try {
            final SharedPreferences preferences = getSharedPreferences(Preferences.NAME,
                    0);

            pInfo = getPackageManager().getPackageInfo(getPackageName(),
                    PackageManager.GET_META_DATA);
            if (preferences.getLong("lastRunVersionCode", 0) == 0) {
                new TipsDialog().show(getSupportFragmentManager(), "TipsDialog");
                SharedPreferences.Editor editor = preferences.edit();
                editor.putLong("lastRunVersionCode", pInfo.versionCode);
                editor.commit();
            }
        } catch (PackageManager.NameNotFoundException e) {
            // Nothing to do, don't crash.
        }
    }

    private final OnItemClickListener onHomeItemClick = new OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

            switch ((int) id) {
                case ARTISTS:
                    ArtistListActivity.show(HomeActivity.this);
                    break;
                case ALBUMS:
                    AlbumListActivity.show(HomeActivity.this);
                    break;
                case SONGS:
                    SongListActivity.show(HomeActivity.this);
                    break;
                case GENRES:
                    GenreListActivity.show(HomeActivity.this);
                    break;
                case YEARS:
                    YearListActivity.show(HomeActivity.this);
                    break;
                case NEW_MUSIC:
                    AlbumListActivity.show(HomeActivity.this,
                            AlbumViewDialog.AlbumsSortOrder.__new);
                    break;
                case MUSIC_FOLDER:
                    MusicFolderListActivity.show(HomeActivity.this);
                    break;
                case RANDOM_MIX:
                    RandomplayActivity.show(HomeActivity.this);
                    break;
                case PLAYLISTS:
                    PlaylistsActivity.show(HomeActivity.this);
                    break;
                case INTERNET_RADIO:
                    // Uncomment these next two lines as an easy way to check
                    // crash reporting functionality.
                    //String sCrashString = null;
                    //Log.e("MyApp", sCrashString);
                    RadioListActivity.show(HomeActivity.this);
                    break;
                case FAVORITES:
                    FavoriteListActivity.show(HomeActivity.this);
                    break;
                case MY_APPS:
                    ApplicationListActivity.show(HomeActivity.this);
                    break;
            }
        }
    };

    @Override
    public void onDestroy() {
        super.onDestroy();

        // Send analytics stats (if enabled).
        if (tracker != null) {
            tracker.dispatch();
            tracker.stopSession();
        }
    }

    public static void show(Context context) {
        final Intent intent = new Intent(context, HomeActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        context.startActivity(intent);
    }

}
