package uk.org.ngo.squeezer;


import android.app.Application;
import android.content.Context;
import android.os.Build;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.UrlConnectionDownloader;

// Trick to make the app context useful available everywhere.
// See http://stackoverflow.com/questions/987072/using-application-context-everywhere

public class Squeezer extends Application {

    private static Squeezer instance;

    public Squeezer() {
        instance = this;
    }

    public static Context getContext() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // Configure Picasso.
        Picasso.Builder builder = new Picasso.Builder(this);
        builder.indicatorsEnabled(BuildConfig.DEBUG);

        // OkHttp is not available on devices < API 9, explicitly use UrlConnectionDownloader.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.GINGERBREAD) {
            builder.downloader(new UrlConnectionDownloader(this));
        }

        Picasso.setSingletonInstance(builder.build());
    }
}

