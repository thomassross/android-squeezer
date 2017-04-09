package uk.org.ngo.squeezer;

import android.os.Bundle;

/**
 * Created by nik on 09/04/2017.
 */

public class SettingsFragment extends android.support.v7.preference.PreferenceFragmentCompat {
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public void onCreatePreferences(final Bundle savedInstanceState, final String rootKey) {
        addPreferencesFromResource(R.xml.preferences);
    }
}
