package com.weather.wallpaper.forecast.location;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.annotation.Nullable;
import android.support.v7.preference.PreferenceManager;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;
import com.weather.wallpaper.forecast.R;

/**
 * Created by Vaibhav on 8/25/2017.
 */

public class GeoFencingTransitionsIntentService extends IntentService {

    public GeoFencingTransitionsIntentService() {
        super("Geofening Intent Service");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent){
        GeofencingEvent event = GeofencingEvent.fromIntent(intent);

        //checking if event is null
        if(event==null){
            return;
        }

        if(event.hasError()){

            String errorMessage = GeofenceErrorMessages.getErrorString(this,
                    event.getErrorCode());

            //event is null
            return;
        }

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);

        switch (event.getGeofenceTransition()){

            /*
            If user enters location, setting the keys to true and 0
             */
            case Geofence.GEOFENCE_TRANSITION_ENTER:
                sharedPref.edit().putBoolean(
                        getString(R.string.pref_is_user_inside_set_location_key), true
                ).apply();
                sharedPref.edit().putInt(
                        getString(R.string.pref_times_user_found_out_key), 0
                ).apply();
                break;

            /*
            setting the key to false
            and it's job of the sync task to increment value of the other key
             */
            case Geofence.GEOFENCE_TRANSITION_EXIT:
                sharedPref.edit().putBoolean(
                        getString(R.string.pref_is_user_inside_set_location_key), true
                ).apply();
                break;

            default:
                break;
        }

        //TODO: you can also log down the triggeringGeofences for helpful logging
    }



}
