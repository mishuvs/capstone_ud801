package com.weather.wallpaper.forecast.sync;

import android.app.IntentService;
import android.content.Intent;
import android.support.annotation.Nullable;

/**
 * Created by Vaibhav on 8/13/2017.
 */

public class WeatherSyncIntentService extends IntentService {

    public static final String TAG = WeatherSyncIntentService.class.getName();
    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     */
    public WeatherSyncIntentService() {
        super("WeatherSyncIntentService");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        WeatherSyncTask.syncWeather(this);
        WeatherSyncTask.syncWeekWeather(this);
        WeatherSyncTask.checkIfLocationChanged(this);
        WeatherSyncTask.saveDebugMessage("intent service ran",this);
    }
}
