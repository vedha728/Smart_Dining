package com.example.smartdining;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.BitmapShader;
import android.graphics.Shader;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.ImageView;
import android.graphics.Color;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Loads remote food images without blocking the UI thread. */
public final class FoodImageLoader {
    private static final String TAG = "FoodImageLoader";
    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(4);
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private FoodImageLoader() { }

    public static void load(String imageUrl, ImageView target, boolean circular) {
        final String requestedUrl = imageUrl == null ? "" : imageUrl.trim();
        target.setImageDrawable(placeholder(circular));
        target.setTag(requestedUrl);
        if (requestedUrl.isEmpty()) return;

        EXECUTOR.execute(() -> {
            Bitmap bitmap = download(requestedUrl);
            if (bitmap == null) {
                Log.w(TAG, "Image could not be loaded: " + requestedUrl);
                return; // Keep the placeholder instead of crashing the screen.
            }
            final Bitmap displayBitmap = circular ? circle(bitmap) : bitmap;
            MAIN.post(() -> {
                Object currentTag = target.getTag();
                if (currentTag instanceof String && requestedUrl.equals(currentTag)) {
                    target.setImageBitmap(displayBitmap);
                }
            });
        });
    }

    private static Bitmap download(String imageUrl) {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(imageUrl);
            // Handle redirects explicitly; some image hosts redirect to a CDN.
            for (int redirect = 0; redirect < 6; redirect++) {
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(12000);
                connection.setReadTimeout(15000);
                connection.setInstanceFollowRedirects(false);
                connection.setUseCaches(true);
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36");
                connection.setRequestProperty("Accept", "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8");
                connection.setRequestProperty("Accept-Encoding", "identity");
                int status = connection.getResponseCode();
                if (status == HttpURLConnection.HTTP_MOVED_PERM || status == HttpURLConnection.HTTP_MOVED_TEMP
                        || status == HttpURLConnection.HTTP_SEE_OTHER || status == 307 || status == 308) {
                    String location = connection.getHeaderField("Location");
                    connection.disconnect(); connection = null;
                    if (location == null || location.trim().isEmpty()) return null;
                    url = new URL(url, location);
                    continue;
                }
                if (status < 200 || status >= 300) {
                    Log.w(TAG, "Image server returned HTTP " + status + " for " + url);
                    return null;
                }
                try (InputStream input = connection.getInputStream(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                    byte[] buffer = new byte[8192]; int read;
                    while ((read = input.read(buffer)) != -1) output.write(buffer, 0, read);
                    byte[] bytes = output.toByteArray();
                    Bitmap decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                    if (decoded == null) Log.w(TAG, "Response was not a supported bitmap image: " + url + ", content-type=" + connection.getContentType());
                    return decoded;
                }
            }
            Log.w(TAG, "Too many image redirects: " + imageUrl);
        } catch (Exception e) {
            Log.w(TAG, "Image request failed for " + imageUrl, e);
        } finally {
            if (connection != null) connection.disconnect();
        }
        return null;
    }

    private static Bitmap circle(Bitmap source) {
        int size = Math.min(source.getWidth(), source.getHeight());
        Bitmap output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        BitmapShader shader = new BitmapShader(source, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
        float scale = Math.max((float) size / source.getWidth(), (float) size / source.getHeight());
        android.graphics.Matrix matrix = new android.graphics.Matrix();
        matrix.setScale(scale, scale);
        matrix.postTranslate((size - source.getWidth() * scale) / 2f, (size - source.getHeight() * scale) / 2f);
        shader.setLocalMatrix(matrix);
        paint.setShader(shader);
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint);
        return output;
    }

    private static android.graphics.drawable.Drawable placeholder(boolean circular) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(Color.rgb(229, 236, 243));
        drawable.setShape(circular ? GradientDrawable.OVAL : GradientDrawable.RECTANGLE);
        return drawable;
    }
}
