/*
 * Copyright (c) 2012 Google Inc.  All Rights Reserved.
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

package uk.org.ngo.squeezer.dialog;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.Map.Entry;
import java.util.TreeMap;

import uk.org.ngo.squeezer.Preferences;
import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.Util;
import uk.org.ngo.squeezer.util.ScanNetworkTask;

/**
 * Scans the local network for servers, allow the user to choose one, set it as the preferred server
 * for this network, and optionally enter authentication information.
 * <p>
 * A new network scan can be initiated manually if desired.
 */
public class ServerAddressView extends LinearLayout implements ScanNetworkTask.ScanNetworkCallback {
    private static final String TAG = ServerAddressView.class.getSimpleName();

    private Preferences mPreferences;
    private String mBssId;

    private EditText mServerAddressEditText;
    private TextView mServerName;
    private Spinner mServersSpinner;
    private EditText mUserNameEditText;
    private EditText mPasswordEditText;
    private Button mScanButton;
    private View mScanResults;
    private View mScanProgress;
    private TextView mScanDisabledMessage;

    private ScanNetworkTask mScanNetworkTask;

    /** Map server names to IP addresses. */
    private TreeMap<String, String> mDiscoveredServers;

    private ArrayAdapter<String> mServersAdapter;

    // This doesn't work (yet).  This needs to be in the activity that inflates the layout that
    // contains this view.  The activity will need a reference to this view, so it can do
    // something like:
    //
    //    serverAddressView.toggleScanningUi(...)
    //
    // in the receiver. It will also need to un/register the receiver, with something like
    //
    //    onPause() { unregisterReceiver(broadcastReceiver); }
    //    onResume() { registerReceiver(broadcastReceiver, new IntentFilter(
    //                    ConnectivityManager.CONNECTIVITY_ACTION));
    //
    // DisconnectedActivity and SettingsActivity host this.
    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            ConnectivityManager connMgr = (ConnectivityManager) context
                    .getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = connMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            toggleScanningUi(networkInfo.isConnected());
        }
    };


    public ServerAddressView(final Context context) {
        super(context);
        initialize(context);
    }

    public ServerAddressView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize(context);
    }

    private void initialize(@NonNull final Context context) {
        inflate(context, R.layout.server_address_view, this);
        if (!isInEditMode()) {
            mPreferences = new Preferences(context);
            Preferences.ServerAddress serverAddress = mPreferences.getServerAddress();
            mBssId = serverAddress.bssId;

            mServerAddressEditText = (EditText) findViewById(R.id.server_address);
            mUserNameEditText = (EditText) findViewById(R.id.username);
            mPasswordEditText = (EditText) findViewById(R.id.password);
            setServerAddress(serverAddress.address);

            // Set up the servers spinner.
            mServersAdapter = new ArrayAdapter<String>(context, android.R.layout.simple_spinner_item);
            mServersAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            mServerName = (TextView) findViewById(R.id.server_name);
            mServersSpinner = (Spinner) findViewById(R.id.found_servers);
            mServersSpinner.setAdapter(mServersAdapter);
            mServersSpinner.setOnItemSelectedListener(new MyOnItemSelectedListener());

            // Set up the scanning UI.
            mScanButton = (Button) findViewById(R.id.scan_button);
            mScanButton.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    startNetworkScan(context);
                }
            });
            mScanResults = findViewById(R.id.scan_results);
            mScanProgress = findViewById(R.id.scan_progress);
            mScanProgress.setVisibility(GONE);
            mScanDisabledMessage = (TextView) findViewById(R.id.scan_disabled_msg);

            // Only support network scanning on WiFi.
            ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo ni = connectivityManager.getActiveNetworkInfo();
            boolean isWifi = ni != null && ni.getType() == ConnectivityManager.TYPE_WIFI;
            toggleScanningUi(isWifi);
            if (isWifi) {
                startNetworkScan(context);
            }
        }
    }

    // Enable / disable showing the buttons / results of network scanning.
    private void toggleScanningUi(boolean showScanningUi) {
        if (mScanDisabledMessage == null) {
            return;
        }

        // Enable these if scanning is disabled.
        mScanDisabledMessage.setVisibility(showScanningUi ? GONE : VISIBLE);

        // Enable these if scanning is enabled.
        mScanButton.setVisibility(showScanningUi ? VISIBLE : GONE);
        mScanResults.setVisibility(showScanningUi ? VISIBLE : GONE);
    }

    public void savePreferences() {
        String address = mServerAddressEditText.getText().toString();

        // Append the default port if necessary.
        if (!address.contains(":")) {
            address += ":" + getResources().getInteger(R.integer.DefaultPort);
        }

        Preferences.ServerAddress serverAddress = mPreferences.saveServerAddress(address);

        final String serverName = getServerName(address);
        if (serverName != null) {
            mPreferences.saveServerName(serverAddress, serverName);
        }

        final String userName = mUserNameEditText.getText().toString();
        final String password = mPasswordEditText.getText().toString();
        mPreferences.saveUserCredentials(serverAddress, userName, password);
    }

    public void onDismiss() {
        // Stop scanning
        if (mScanNetworkTask != null) {
            mScanNetworkTask.cancel(true);
        }
    }

    /**
     * Starts scanning for servers.
     */
    void startNetworkScan(Context context) {
        mScanResults.setVisibility(GONE);
        mScanProgress.setVisibility(VISIBLE);
        mScanNetworkTask = new ScanNetworkTask(context, this);
        mScanNetworkTask.execute();
    }

    /**
     * Called when server scanning has finished.
     * @param serverMap Discovered servers, key is the server name, value is the IP address.
     */
    public void onScanFinished(TreeMap<String, String> serverMap) {
        mScanResults.setVisibility(VISIBLE);
        mServerName.setVisibility(GONE);
        mServersSpinner.setVisibility(GONE);
        mScanProgress.setVisibility(GONE);

        if (mScanNetworkTask == null) {
            return;
        }

        mDiscoveredServers = serverMap;
        mScanNetworkTask = null;

        switch (mDiscoveredServers.size()) {
            case 0:
                // Do nothing, no servers found.
                break;

            case 1:
                // Populate the edit text widget with the address found.
                setServerAddress(mDiscoveredServers.get(mDiscoveredServers.firstKey()));
                mServerName.setVisibility(VISIBLE);
                mServerName.setText(mDiscoveredServers.firstKey());
                break;

            default:
                // Show the spinner so the user can choose a server.
                mServersAdapter.clear();
                for (Entry<String, String> e : mDiscoveredServers.entrySet()) {
                    mServersAdapter.add(e.getKey());
                }
                int position = getServerPosition(mServerAddressEditText.getText().toString());
                if (position >= 0) mServersSpinner.setSelection(position);
                mServersSpinner.setVisibility(VISIBLE);
                mServersAdapter.notifyDataSetChanged();
        }
    }

    private void setServerAddress(String address) {
        String currentHostPort = mServerAddressEditText.getText().toString();
        String currentHost = Util.parseHost(currentHostPort);
        int currentPort = Util.parsePort(currentHostPort);

        String host = Util.parseHost(address);
        int port = Util.parsePort(address);

        if (host.equals(currentHost)) {
            port = currentPort;
        }

        Preferences.ServerAddress serverAddress = new Preferences.ServerAddress();
        serverAddress.bssId = mBssId;
        serverAddress.address = host + ":" + port;

        mServerAddressEditText.setText(serverAddress.address);
        mUserNameEditText.setText(mPreferences.getUserName(serverAddress));
        mPasswordEditText.setText(mPreferences.getPassword(serverAddress));
    }

    private String getServerName(String ipPort) {
        if (mDiscoveredServers != null)
            for (Entry<String, String> entry : mDiscoveredServers.entrySet())
                if (ipPort.equals(entry.getValue()))
                    return entry.getKey();
        return null;
    }

    private int getServerPosition(String ipPort) {
        if (mDiscoveredServers != null) {
            String host = Util.parseHost(ipPort);
            int position = 0;
            for (Entry<String, String> entry : mDiscoveredServers.entrySet()) {
                if (host.equals(entry.getValue()))
                    return position;
                position++;
            }
        }
        return -1;
    }

    /**
     * Inserts the selected address in to the edit text widget.
     */
    private class MyOnItemSelectedListener implements OnItemSelectedListener {
        public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
            String serverAddress = mDiscoveredServers.get(parent.getItemAtPosition(pos).toString());
            setServerAddress(serverAddress);
        }

        public void onNothingSelected(AdapterView<?> parent) {
            // Do nothing.
        }
    }

}
