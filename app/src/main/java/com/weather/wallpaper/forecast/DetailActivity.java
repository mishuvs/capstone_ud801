package com.weather.wallpaper.forecast;

import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.databinding.DataBindingUtil;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.transition.Slide;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.signature.ObjectKey;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.MobileAds;
import com.weather.wallpaper.forecast.data.WeatherContract;
import com.weather.wallpaper.forecast.databinding.ActivityDetailBinding;
import com.weather.wallpaper.forecast.utilities.GlideApp;
import com.weather.wallpaper.forecast.utilities.SunshineDateUtils;
import com.weather.wallpaper.forecast.utilities.SunshineWeatherUtils;
import com.weather.wallpaper.forecast.utilities.WallpaperUtils;

import java.util.concurrent.TimeUnit;

public class DetailActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    public static final String[] DETAIL_FORECAST_PROJECTION = {
            WeatherContract.WeatherEntry._ID,
            WeatherContract.WeatherEntry.COLUMN_DATE,
            WeatherContract.WeatherEntry.COLUMN_WEATHER_ID,
            WeatherContract.WeatherEntry.COLUMN_WEATHER_TYPE,
            WeatherContract.WeatherEntry.COLUMN_MAX_TEMP,
            WeatherContract.WeatherEntry.COLUMN_MIN_TEMP,
            WeatherContract.WeatherEntry.COLUMN_CURRENT_TEMP,
            WeatherContract.WeatherEntry.COLUMN_PRESSURE,
            WeatherContract.WeatherEntry.COLUMN_HUMIDITY,
            WeatherContract.WeatherEntry.COLUMN_WIND_SPEED,
            WeatherContract.WeatherEntry.COLUMN_WIND_DEGREES,
    };
    private static final String TAG = DetailActivity.class.getName();

    private Uri uri;
    ActivityDetailBinding mBinding;
    private final int LOADER_ID = 0;

    Drawable imageDrawable;

    InterstitialAd mInterstitialAd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_detail);

        Slide slide = new Slide(Gravity.BOTTOM);
        slide.addTarget(R.id.root_layout);
        slide.setInterpolator(
                AnimationUtils.loadInterpolator(this,
                        android.R.interpolator.linear_out_slow_in));
        slide.setDuration(200);
        getWindow().setEnterTransition(slide);

        MobileAds.initialize(this, getString(R.string.detail_admob_banner_id));
        AdRequest adRequest = new AdRequest.Builder().build();
        mBinding.adView.loadAd(adRequest);
        mInterstitialAd = new InterstitialAd(this);
        mInterstitialAd.setAdUnitId(getString(R.string.detail_admob_interstitial_id));
        mInterstitialAd.loadAd(new AdRequest.Builder().build());
        mInterstitialAd.setAdListener(new AdListener() {
            @Override
            public void onAdClosed() {
                // Load the next interstitial.
                mInterstitialAd.loadAd(new AdRequest.Builder().build());
            }

        });

        Toolbar toolbar = findViewById(R.id.tool_bar);
        setSupportActionBar(toolbar);
        if(getSupportActionBar()!=null){
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
            getSupportActionBar().setTitle("More Details");
        }

        Intent i = getIntent();
        uri = i.getData();

        mBinding.actionSetWallpaper.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(imageDrawable!=null){
                    Bitmap wallpaperBitmap = ((BitmapDrawable) imageDrawable).getBitmap();
                    WallpaperUtils.setWallpaper(DetailActivity.this,wallpaperBitmap);
                }
            }
        });

        getLoaderManager().initLoader(LOADER_ID,null,this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(this,uri,null,null,null,null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if(data==null){
            return;
        }
        data.moveToFirst();
        long dateInMillis;

        int index;

        /*DATE: Read date from the cursor */
        index = data.getColumnIndex(DETAIL_FORECAST_PROJECTION[1]);
        dateInMillis = data.getLong(index);
                    /* Get human readable string using our utility method */
        String dateString = SunshineDateUtils.getFriendlyDateString(this, dateInMillis, false);
        mBinding.date.setText(dateString);

        //WEATHER ICON:
        index = data.getColumnIndex(DETAIL_FORECAST_PROJECTION[2]);
        mBinding.icon.setImageResource(SunshineWeatherUtils.getIconResourceForWeatherCondition(
                data.getInt(index)
        ));

        //DESCRIPTION:
        index = data.getColumnIndex(DETAIL_FORECAST_PROJECTION[3]);
        String description = data.getString(index);

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

        //BACKGROUND IMAGE:
        ImageView imageView = findViewById(R.id.image_view);
        mBinding.description.setText(description);
        //also setting image as per the description received:
        String imageUrl = "https://source.unsplash.com/1600x900/?" + SunshineWeatherUtils.getMainDescription(description);
        GlideApp.with(this).load(imageUrl)
                //TODO: put PLACEHOLDER,ERROR,FALLBACK resources
                .override(displayMetrics.widthPixels,displayMetrics.heightPixels)
                .centerCrop()
                .listener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                        mBinding.progressBar.setVisibility(View.GONE);
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                        imageDrawable = resource;
                        mBinding.progressBar.setVisibility(View.GONE);
                        return false;
                    }
                })
                .signature(new ObjectKey(
                        String.valueOf(TimeUnit.MILLISECONDS.toHours(System.currentTimeMillis()))
                ))
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(imageView);

        //MAX TEMP:
        index = data.getColumnIndex(DETAIL_FORECAST_PROJECTION[4]);
        String maxTemp = SunshineWeatherUtils.formatTemperature(this,data.getDouble(index));
        mBinding.maxTemp.setText(maxTemp);

        //MIN TEMP:
        index = data.getColumnIndex(DETAIL_FORECAST_PROJECTION[5]);
        String mintemp = SunshineWeatherUtils.formatTemperature(this,data.getDouble(index));
        mBinding.minTemp.setText(mintemp);

        //PRESSURE:
        index = data.getColumnIndex(DETAIL_FORECAST_PROJECTION[7]);
        String pressure = SunshineWeatherUtils.getFormattedPressure(this,data.getDouble(index));
        mBinding.pressure.setText(pressure);

        //HUMIDITY:
        index = data.getColumnIndex(DETAIL_FORECAST_PROJECTION[8]);
        mBinding.humidity.setText(Integer.toString(data.getInt(index)) + " %");

        //WIND:
        index = data.getColumnIndex(DETAIL_FORECAST_PROJECTION[9]);
        String wind = SunshineWeatherUtils.getFormattedWind(this,
                ((float) data.getDouble(index)),
                ((float) data.getDouble(
                        data.getColumnIndex(DETAIL_FORECAST_PROJECTION[10])
                )));
        mBinding.wind.setText(wind);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        getLoaderManager().restartLoader(LOADER_ID,null,this);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
