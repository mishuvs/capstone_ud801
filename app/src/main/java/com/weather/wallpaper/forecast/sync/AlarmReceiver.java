package com.weather.wallpaper.forecast.sync;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created by Vaibhav on 12/23/2017.
 */

public class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        WeatherSyncUtils.startImmediateSync(context);
    }
}
