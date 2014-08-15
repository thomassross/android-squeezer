package org.robolectric.shadows;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

import android.support.v7.internal.view.SupportMenuInflater;
import android.view.Menu;

/**
 * https://stackoverflow.com/questions/21264849/android-content-res-resourcesnotfoundexception-in-robolectric-2-2
 */
@Implements(SupportMenuInflater.class)
public class ShadowSupportMenuInflater extends ShadowMenuInflater {
    @Implementation
    public void inflate(int menuRes, Menu menu) {
        super.inflate(menuRes, menu);
    }
}