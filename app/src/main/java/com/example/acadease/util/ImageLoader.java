package com.example.acadease.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageView;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ImageLoader {
    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(2);
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    public static void load(ImageView target, String url, int placeholderResId) {
        if (url == null || url.trim().isEmpty()) {
            if (placeholderResId != 0) target.setImageResource(placeholderResId);
            return;
        }
        if (placeholderResId != 0) {
            target.setImageResource(placeholderResId);
        }
        EXECUTOR.submit(() -> {
            Bitmap bmp = null;
            HttpURLConnection connection = null;
            try {
                URL u = new URL(url);
                connection = (HttpURLConnection) u.openConnection();
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                connection.connect();
                try (InputStream is = connection.getInputStream()) {
                    bmp = BitmapFactory.decodeStream(is);
                }
            } catch (Exception ignored) {
            } finally {
                if (connection != null) connection.disconnect();
            }
            final Bitmap result = bmp;
            MAIN.post(() -> {
                if (result != null) target.setImageBitmap(result);
            });
        });
    }
}
