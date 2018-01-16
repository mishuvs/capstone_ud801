package com.weather.wallpaper.forecast.utilities;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.util.Log;

import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.security.ProviderInstaller;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Scanner;

/**
 * Created by Vaibhav on 7/7/2017.
 */

public final class NetworkUtils {

    private static final String TAG = NetworkUtils.class.getSimpleName();

    //TODO: remove this
    //private static final String DYNAMIC_WEATHER_URL = "https://andfun-weather.udacity.com/weather";

    private static final String STATIC_WEATHER_URL =
            "http://api.openweathermap.org/data/2.5/forecast?";
            //TODO(4): "http://api.openweathermap.org/data/2.5/forecast/daily?";

    private static final String DAILY_WEATHER_URL =
            "http://api.openweathermap.org/data/2.5/forecast/daily?";

    private static final String FORECAST_BASE_URL = STATIC_WEATHER_URL;
    //http://api.openweathermap.org/data/2.5/forecast?lat=22&lon=75&appid=dfa8912f1fc286b806601445556f2ba6

    //https://samples.openweathermap.org/data/2.5/forecast/daily?lat=35&lon=139&cnt=10&appid=dfa8912f1fc286b806601445556f2ba6
    private static final String API_KEY = "dfa8912f1fc286b806601445556f2ba6";
    private static String UNSPLASH_APPLICATION_ID = "848e63368c665e3c1074d50dafe14a87e28101a6d829aa769a180a22f3e6d002";

    /*
     * NOTE: These values only effect responses from OpenWeatherMap, NOT from the fake weather
     * server. They are simply here to allow us to teach you how to build a URL if you were to use
     * a real API.If you want to connect your app to OpenWeatherMap's API, feel free to! However,
     * we are not going to show you how to do so in this course.
     */

    /* The format we want our API to return */
    private static final String format = "json";
    /* The units we want our API to return */
    private static final String units = "metric";
    /* The number of days we want our API to return */
    private static final int numDays = 14;

    final static String QUERY_PARAM = "q";
    final static String LAT_PARAM = "lat";
    final static String LON_PARAM = "lon";
    final static String FORMAT_PARAM = "mode";
    final static String UNITS_PARAM = "units";
    final static String NO_OF_DAYS_PARAM = "cnt";
    final static String API_KEY_PARAM = "appid";
    private static final String UNSPLASH_SOURCE_BASE_URL = "https://api.unsplash.com/photos/random" ;
    private static final String UNSPLASH_QUERY_PARAM = "query";
    private static final String UNSPLASH_ID_PARAM = "client_id";
    private static final String UNSPLASH_WIDTH_PARAM = "w";
    private static final String UNSPLASH_HEIGHT_PARAM = "w";
    private static final String UNSPLASH_ORIENTATION_PARAM = "orientation";

    static String htmlResponse;

    /**
     * Builds the URL used to talk to the weather server using a location. This location is based
     * on the query capabilities of the weather provider that we are using.
     *
     * @return The URL to use to query the weather server.
     */
    public static URL buildUrl(double latitude, double longitude) {
        Uri builtUri = Uri.parse(FORECAST_BASE_URL);
        Uri finalUri = builtUri.buildUpon()
                .appendQueryParameter(LAT_PARAM, String.valueOf(latitude))
                .appendQueryParameter(LON_PARAM, String.valueOf(longitude))
                .appendQueryParameter(API_KEY_PARAM, API_KEY)
                .build();

        Log.i(TAG,"This is the built uri: " + finalUri.toString());

        try {
            return new URL(finalUri.toString());
        } catch (MalformedURLException e) {
            Log.i(TAG,"error buildUrl");
            e.printStackTrace();
            return null;
        }
    }

    public static URL weekDayWeatherBuildUrl(double latitude, double longitude){
        Uri builtUri = Uri.parse(DAILY_WEATHER_URL);
        Uri finalUri = builtUri.buildUpon()
                .appendQueryParameter(LAT_PARAM, String.valueOf(latitude))
                .appendQueryParameter(LON_PARAM, String.valueOf(longitude))
                .appendQueryParameter(NO_OF_DAYS_PARAM, Integer.toString(numDays))
                .appendQueryParameter(API_KEY_PARAM, API_KEY)
                .build();

        Log.i(TAG,"This is the built uri: " + finalUri.toString());

        try {
            return new URL(finalUri.toString());
        } catch (MalformedURLException e) {
            Log.i(TAG,"error weekDayWeatherBuild");
            e.printStackTrace();
            return null;
        }
    }

    public static URL buildUrl(String description){

        //TODO: Temporarily making url a global variable, convert it later on
        int random = (int) ((Math.random())*10);
        Uri finalUri,baseUri;
        //with portrait parameter
        if(random>6){
            baseUri = Uri.parse(UNSPLASH_SOURCE_BASE_URL);
            finalUri = baseUri.buildUpon()
                    .appendQueryParameter(UNSPLASH_QUERY_PARAM,description)
                    .appendQueryParameter(UNSPLASH_ID_PARAM, UNSPLASH_APPLICATION_ID)
                    .appendQueryParameter(UNSPLASH_ORIENTATION_PARAM,"portrait")
                    .build();
        }
        //without portrait parameter:
        else{
            baseUri = Uri.parse(UNSPLASH_SOURCE_BASE_URL);
            finalUri = baseUri.buildUpon()
                    .appendQueryParameter(UNSPLASH_QUERY_PARAM,description)
                    .appendQueryParameter(UNSPLASH_ID_PARAM, UNSPLASH_APPLICATION_ID)
                    .build();
        }

        Log.i(TAG,"random number: " + random + " unsplash Uri: " + finalUri);

        try {
            return new URL(finalUri.toString());
        } catch (MalformedURLException e) {
            e.printStackTrace();
            Log.e(TAG, "url formed is null");
            return null;
        }
    }

    /**
     * This method returns the entire result from the HTTP response.
     *
     * @param url The URL to fetch the HTTP response from.
     * @return The contents of the HTTP response.
     * @throws IOException Related to network and stream reading
     */
    public static String getResponseFromHttpUrl(URL url) throws IOException {
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        try {
            InputStream in = urlConnection.getInputStream();

            Scanner scanner = new Scanner(in);
            scanner.useDelimiter("\\A");

            boolean hasInput = scanner.hasNext();
            if (hasInput) {
                return scanner.next();
            } else {
                return null;
            }
        } finally {
            urlConnection.disconnect();
        }
    }

    public static boolean checkNetworkConnection(Context context){
        ConnectivityManager cm =
                (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();
    }

    public static void updateAndroidSecurityProvider(Activity callingActivity) {
        try {
            ProviderInstaller.installIfNeeded(callingActivity.getApplicationContext());
        } catch (GooglePlayServicesRepairableException e) {
            // Thrown when Google Play Services is not installed, up-to-date, or enabled
            // Show dialog to allow users to install, update, or otherwise enable Google Play services.
            GooglePlayServicesUtil.getErrorDialog(e.getConnectionStatusCode(), callingActivity, 0);
        } catch (GooglePlayServicesNotAvailableException e) {
        }
    }
}