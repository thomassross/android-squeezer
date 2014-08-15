package uk.org.ngo.squeezer.tests;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;
import org.robolectric.util.ActivityController;

import android.view.View;

import uk.org.ngo.squeezer.HomeActivity;
import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.RobolectricGradleTestRunner;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricGradleTestRunner.class)
public class MyAndroidClassTest {

    @Test @Config(reportSdk = 10)
    public void testWhenActivityCreatedHelloTextViewIsVisible() throws Exception {
        HomeActivity activity = new HomeActivity();

        ActivityController.of(activity).attach().create();

        int visibility = activity.findViewById(R.id.item_list).getVisibility();
        assertEquals(visibility, View.VISIBLE);
    }
}