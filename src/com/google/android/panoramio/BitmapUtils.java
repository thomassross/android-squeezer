/*
 * Copyright (C) 2008 Google Inc.
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

package com.google.android.panoramio;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 *  Utilities for loading a bitmap from a URL
 *
 */
public class BitmapUtils {
    
    private static final String TAG = "Panoramio";
    
    private static final int IO_BUFFER_SIZE = 32 * 1024;
    
    /**
     * Loads a byte array from the specified url.  This can take a while, so it
     * should not be called from the UI thread.
     * 
     * @param url The location of the bitmap asset
     * 
     * @return The byte array, or null if it could not be loaded
     */
    public static byte[] loadByteArrayFromUrl(String url) {
        InputStream in = null;
        BufferedOutputStream out = null;
        byte[] data = null;
    	
        try {
        	HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        	//in = new BufferedInputStream(new URL(url).openStream(), IO_BUFFER_SIZE);
        	in = new BufferedInputStream(conn.getInputStream());
        	int length = conn.getContentLength();
        	Log.v(TAG, "Url: " + url + " has size " + length);
            final ByteArrayOutputStream dataStream = new ByteArrayOutputStream();
            out = new BufferedOutputStream(dataStream, length != -1 ? length : IO_BUFFER_SIZE);
            copyInputStreamToOutputStream(in, out);
            out.flush();

            data = dataStream.toByteArray();
        } catch (IOException e) {
            Log.e(TAG, "Could not load Bitmap from: " + url);
        } finally {
            closeStream(in);
            closeStream(out);
        }

        return data;
    }
    
    /**
     * Loads a bitmap from the specified url. This can take a while, so it should not
     * be called from the UI thread.
     * 
     * @param url The location of the bitmap asset
     * 
     * @return The bitmap, or null if it could not be loaded
     */
    public static Bitmap loadBitmapFromUrl(String url) {
        byte[] data = loadByteArrayFromUrl(url);
        if (data != null) {
        	return BitmapFactory.decodeByteArray(data, 0, data.length);
        } else {
        	return null;
        }
    }

    /**
     * Closes the specified stream.
     * 
     * @param stream The stream to close.
     */
    public static void closeStream(Closeable stream) {
        if (stream != null) {
            try {
                stream.close();
            } catch (IOException e) {
                android.util.Log.e(TAG, "Could not close stream", e);
            }
        }
    }

    /**
     * Copy the content of the input stream into the output stream, using a
     * temporary byte array buffer whose size is defined by
     * {@link #IO_BUFFER_SIZE}.
     * 
     * @param in The input stream to copy from.
     * @param out The output stream to copy to.
     * @throws IOException If any error occurs during the copy.
     */
    public static void copyInputStreamToOutputStream(InputStream in, OutputStream out) throws IOException {
        byte[] b = new byte[IO_BUFFER_SIZE];
        int read;
        while ((read = in.read(b)) != -1) {
            out.write(b, 0, read);
        }
    }

}