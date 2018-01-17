package com.weather.wallpaper.forecast.utilities;

import android.app.WallpaperManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.Handler;
import android.support.v7.preference.PreferenceManager;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.target.BaseTarget;
import com.bumptech.glide.request.target.SizeReadyCallback;
import com.bumptech.glide.request.transition.Transition;
import com.weather.wallpaper.forecast.MainActivity;
import com.weather.wallpaper.forecast.R;

import java.io.IOException;

import static com.google.android.gms.internal.zzagz.runOnUiThread;

/**
 * Created by Vaibhav on 10/5/2017.
 */
public class WallpaperUtils {

    private static final String TAG = WallpaperUtils.class.getSimpleName();
    private static final String RANDOM_WALLPAPER_URL = "https://source.unsplash.com/random/";

    public static void setWeatherWallpaper(final Context context, final String weatherDescription){
        final BaseTarget target = new BaseTarget<BitmapDrawable>() {
            @Override
            public void onResourceReady(BitmapDrawable bitmap, Transition<? super BitmapDrawable> transition) {
                setWallpaper(context,bitmap.getBitmap());
            }

            @Override
            public void getSize(SizeReadyCallback cb) {
                cb.onSizeReady(SIZE_ORIGINAL, SIZE_ORIGINAL);
            }

            @Override
            public void removeCallback(SizeReadyCallback cb) {}
        };

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        final int height = sharedPref.getInt(context.getString(R.string.screen_height),1280);
        final int width = sharedPref.getInt(context.getString(R.string.screen_width),720);


        final String url = RANDOM_WALLPAPER_URL + width + "x" + height + "/?" + SunshineWeatherUtils.getMainDescription(weatherDescription);

        Handler mainHandler = new Handler(context.getMainLooper());

        Runnable myRunnable = new Runnable() {
            @Override
            public void run() {
                GlideApp.with(context)
                        .load(url)
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .skipMemoryCache(true)
                        .override(width,height)
                        .centerCrop()
                        .into(target);
            } // This is your code
        };
        mainHandler.post(myRunnable);
    }

    public static void setRandomWallpaper(final Context context){
        final BaseTarget target = new BaseTarget<BitmapDrawable>() {
            @Override
            public void onResourceReady(BitmapDrawable bitmap, Transition<? super BitmapDrawable> transition) {
                setWallpaper(context,bitmap.getBitmap());
            }

            @Override
            public void getSize(SizeReadyCallback cb) {
                cb.onSizeReady(SIZE_ORIGINAL, SIZE_ORIGINAL);
            }

            @Override
            public void removeCallback(SizeReadyCallback cb) {}
        };

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        final int height = sharedPref.getInt(context.getString(R.string.screen_height),1280);
        final int width = sharedPref.getInt(context.getString(R.string.screen_width),720);

        final String url = RANDOM_WALLPAPER_URL + width + "x" + height;

        // Get a handler that can be used to post to the main thread
        Handler mainHandler = new Handler(context.getMainLooper());

        Runnable myRunnable = new Runnable() {
            @Override
            public void run() {
                GlideApp.with(context)
                    .load(url)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)
                    .override(width,height)
                    .centerCrop()
                    .into(target);
            } // This is your code
        };
        mainHandler.post(myRunnable);
    }

    public static void setDisplayedImageAsWallpaper(final Context context, ImageView realImage){
        Drawable realImageDrawable = realImage.getDrawable();
        if(realImageDrawable instanceof TransitionDrawable && context instanceof MainActivity) {
            runOnUiThread(new Runnable() {
                public void run(){
                    Toast.makeText(context.getApplicationContext(),context.getString(R.string.toast_prompt_try_again),Toast.LENGTH_SHORT).show();
                }
            });
            return;
        }
        Bitmap wallpaperBitmap = ((BitmapDrawable)realImageDrawable).getBitmap();
        setWallpaper(context,wallpaperBitmap);
    }

    public static void setWallpaper(final Context context, Bitmap wallpaperBitmap){
        WallpaperManager myWallpaperManager
                = WallpaperManager.getInstance(context);
        try {
            if(wallpaperBitmap!=null){
                myWallpaperManager.setBitmap(wallpaperBitmap);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(context, context.getString(R.string.toast_wallpaper_set_successfully), Toast.LENGTH_SHORT).show();
                    }
                });
            }
            else{
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
