package com.weather.wallpaper.forecast.utilities;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * Created by Vaibhav on 8/26/2017.
 */

public final class LocationUtils {

    public static final String TAG = LocationUtils.class.getSimpleName();

    public static String getPlaceName(Context context, double latitude, double longitude){
        Geocoder geocoder;
        List<Address> addresses = null;
        geocoder = new Geocoder(context, Locale.getDefault());

        //TODO: this method probably requires network request... tackle that
        try {
            addresses = geocoder.getFromLocation(latitude, longitude, 1); // Here 1 represent max location result to returned, by documents it recommended 1 to 5
        } catch (IOException e) {
            //TODO: tackle the exception here
            e.printStackTrace();
        }

        if (addresses == null || addresses.size()==0) {
            return null;
        }

        String city = addresses.get(0).getLocality();
        return city;
    }
}
