package com.weather.wallpaper.forecast.sync;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.appwidget.AppWidgetManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import com.weather.wallpaper.forecast.MainActivity;
import com.weather.wallpaper.forecast.R;
import com.weather.wallpaper.forecast.WeatherWidget;
import com.weather.wallpaper.forecast.data.WeatherContract;
import com.weather.wallpaper.forecast.utilities.ImageUtils;
import com.weather.wallpaper.forecast.utilities.NetworkUtils;
import com.weather.wallpaper.forecast.utilities.Owm5day3hrJsonUtils;
import com.weather.wallpaper.forecast.utilities.SunshineWeatherUtils;
import com.weather.wallpaper.forecast.utilities.UnsplashImagesJsonUtils;
import com.weather.wallpaper.forecast.utilities.WallpaperUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Calendar;

import static com.google.android.gms.internal.zzagz.runOnUiThread;

/**
 * Created by Vaibhav on 8/13/2017.
 */
class WeatherSyncTask {

    private static final int PRECIPITATE_ALERT = 101;
    private static final int CHANGE_LOCATION = 202;
    private static final int WEATHER_UPDATED = 303;
    private static final int HARSH_WEATHER_SOON  = 404;
    private static final int WEATHER_TODAY = 505;
    private static final String TAG = WeatherSyncTask.class.getName();
    private static final int NOTIFICATION_ID_DEFAULT = 999;
    private static SharedPreferences sharedPref;

    synchronized static void syncWeather(Context context){

        try {
            ContentValues[] values = null;
            String response;

            sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
            float latitude = sharedPref.getFloat(context.getString(R.string.latitude_key), 0);
            float longitude = sharedPref.getFloat(context.getString(R.string.longitude_key), 0);

            response = NetworkUtils.getResponseFromHttpUrl(NetworkUtils.buildUrl(latitude, longitude));

            try {
                values = Owm5day3hrJsonUtils.getWeatherContentValuesFromJson(context, response);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            if(values != null){
                //Deleting old weather data
                ContentResolver resolver = context.getContentResolver();
                resolver.delete(WeatherContract.WeatherEntry.CONTENT_URI, null, null);
                //Inserting newly fetched data
                int count = resolver.bulkInsert(WeatherContract.WeatherEntry.CONTENT_URI,values);
                if(count>0){
                    checkWeather(context,HARSH_WEATHER_SOON);//check for harsh weather every time it is updated
                    sendNotification(context,WEATHER_UPDATED,"Weather data Updated!", null,0);
                    updateWidget(context);
                    sharedPref.edit().putLong(
                            context.getString(R.string.time_since_last_update),
                            System.currentTimeMillis()
                    ).apply();
                }
                //TODO: probably should notifyDataSetChanged() here...?
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        if(ifHourBetween(6,10)){
            checkWeather(context,WEATHER_TODAY);//check for harsh weather in the morning for whole day
        }

    }

    static ContentValues[] syncWeekWeather(Context context){
        ContentValues[] values = null;

        try {
            String response;

            sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
            float latitude = sharedPref.getFloat(context.getString(R.string.latitude_key), 0);
            float longitude = sharedPref.getFloat(context.getString(R.string.longitude_key), 0);

            response = NetworkUtils.getResponseFromHttpUrl(NetworkUtils.weekDayWeatherBuildUrl(latitude, longitude));

            try {
                //TODO: this line was changed (using own5..)
                //values = OpenWeatherJsonUtils.getSimpleWeatherStringsFromJson(context,response);
                values = Owm5day3hrJsonUtils.getWeekDaysContentValues(context, response);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            if(values != null){
                //Deleting old weather data
                ContentResolver resolver = context.getContentResolver();
                resolver.delete(WeatherContract.WeekDayEntry.CONTENT_URI, null, null);
                //Inserting newly fetched data
                int count = resolver.bulkInsert(WeatherContract.WeekDayEntry.CONTENT_URI,values);
//                if(count>0){
//                    sendNotification(context,WEATHER_UPDATED,"Week weather data updated");
//                }
                //TODO: probably should notify data set changed? if bulkinsert return value not 0?
            }

            if(values!=null){
                String unsplashHtmlResponse;
                String description;
                JSONArray imageUrlString;
                JSONObject urlJson = new JSONObject();

                for(int i=0;i<=5;i++){
                    description = values[i].getAsString(WeatherContract.WeekDayEntry.COLUMN_WEATHER_TYPE);
                    unsplashHtmlResponse = NetworkUtils.getResponseFromHttpUrl(
                            NetworkUtils.buildUrl(description)
                    );
                    try{
                        imageUrlString = UnsplashImagesJsonUtils.getUnsplashImageUrlString(context,unsplashHtmlResponse);
                        urlJson.put(Integer.toString(i),imageUrlString);
                        ImageUtils.saveUrlJson(context,urlJson,"imagesSet");
                    }
                    catch (JSONException e){
                        e.printStackTrace();
                    }
                    //wait before sending next query... so that responses don't get mixed up
                    Handler handler = new Handler(Looper.getMainLooper());
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {}
                    }, 1000);
                    //continue loop
                }
            }

            /*
            Check here from the values ... if it's gonna rain today
             */
        } catch (IOException e) {
            e.printStackTrace();
        }

        if(values!=null){
            return values;
        }
        return null;
    }

    static void checkIfLocationChanged(Context context){
        boolean isUserInsideLocation = sharedPref.getBoolean(context.getString(R.string.pref_is_user_inside_set_location_key), true);
        int notificationCount = sharedPref.getInt(context.getString(R.string.pref_notification_count), 0);

        if(!isUserInsideLocation){
            int timesUserFoundOut = sharedPref.getInt(context.getString(R.string.pref_times_user_found_out_key),0);
            timesUserFoundOut++;
            if(timesUserFoundOut > 10) {
                //TODO: add extra argument so that the method can know what type of notification to send
                sendNotification(context, CHANGE_LOCATION, "",null,0);
            }
            sharedPref.edit().putInt(context.getString(R.string.pref_times_user_found_out_key),timesUserFoundOut).apply();
        }

        sharedPref.edit().putInt(context.getString(R.string.pref_notification_count),notificationCount).apply();
    }

    private static void checkWeather(Context context, int checkWeatherIdType){

        String selection = WeatherContract.WeatherEntry.getSqlSelectFromNowToday();
        Uri forecastUri = WeatherContract.WeatherEntry.CONTENT_URI;
        String sortOrder = WeatherContract.WeatherEntry.COLUMN_DATE + " ASC";
        Cursor mCursor = context.getContentResolver().query(
                forecastUri,
                new String[]{
                        WeatherContract.WeatherEntry.COLUMN_WEATHER_ID,
                        WeatherContract.WeatherEntry.COLUMN_WEATHER_TYPE,
                        WeatherContract.WeatherEntry.COLUMN_CURRENT_TEMP
                },
                selection,
                null,
                sortOrder);

        if(mCursor==null) return;

        int weatherId;
        boolean isWeatherHarsh = false;

        //check weather on every sync OR check weather in morning:
        switch (checkWeatherIdType){
            case HARSH_WEATHER_SOON:
                //every time the service runs, it will check if the first weather entry contains harsh weather
                //if yes... it will send a notification to the user that "harsh weather expected soon"

                if(!mCursor.moveToFirst()){
                    return;
                }
                weatherId = mCursor.getInt(mCursor.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_WEATHER_ID));
                String weatherDescription = mCursor.getString(mCursor.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_WEATHER_TYPE));
                setWallpaper(context,weatherDescription);
                isWeatherHarsh = SunshineWeatherUtils.isWeatherHarsh(weatherId);
                if(isWeatherHarsh){
                    //TODO: change notification count
                    double currentTemp = mCursor.getDouble(mCursor.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_CURRENT_TEMP));
                    int iconId = SunshineWeatherUtils.getIconResourceForWeatherCondition(weatherId);
                    String title = "Weather Alert! " + "(" +SunshineWeatherUtils.formatTemperature(context,currentTemp) + ")";
                    sendNotification(context,HARSH_WEATHER_SOON,weatherDescription,title,iconId);
                }
                break;

            case WEATHER_TODAY:
                //will run only in the morning... i.e. once in a day

                while(!isWeatherHarsh && mCursor.moveToNext()){
                    weatherId = mCursor.getInt(mCursor.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_WEATHER_ID));
                    isWeatherHarsh = SunshineWeatherUtils.isWeatherHarsh(weatherId);
                }
                String todayWeatherDescription = context.getString(R.string.pleasant_weather_message);
                if(isWeatherHarsh){
                    //TODO: change notification count
                    todayWeatherDescription = mCursor.getString(mCursor.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_WEATHER_TYPE));
                }
                sendNotification(context,WEATHER_TODAY,todayWeatherDescription,null,0);
                break;
        }
        mCursor.close();

    }

    private static void sendNotification(Context context, int notificationType, String weatherDescriptionMessage, String title, int notificationIconIdType){

        int notificationID = NOTIFICATION_ID_DEFAULT;

        //if notifications not enabled return and log
        if(!sharedPref.getBoolean(context.getString(R.string.pref_notifications_enabled_key),true)){
            return;
        }
        if(!ifHourBetween(6,23)){//it's not day yet!
            return;
        }

        String message = "";
        if(notificationIconIdType==0){
            notificationIconIdType = R.drawable.app_icon_1;
        }

        switch(notificationType){

            //TODO: change the messages for better english

            case CHANGE_LOCATION:
                title = context.getString(R.string.change_location_notification_title);
                message = context.getString(R.string.change_location_notification_message);
                notificationID = CHANGE_LOCATION;
                break;

            case WEATHER_UPDATED:
                title = context.getString(R.string.app_name);
                message = weatherDescriptionMessage;
                notificationID = WEATHER_UPDATED;
                break;

            case HARSH_WEATHER_SOON:
                message = weatherDescriptionMessage + " ... any time soon";
                notificationID = HARSH_WEATHER_SOON;
                break;

            case WEATHER_TODAY:
                title = context.getString(R.string.notification_weather_today);
                message = weatherDescriptionMessage + " expected today!";
                notificationID = WEATHER_TODAY;
                break;

            default:
                //TODO: you can throw an exception here
                break;

        }

        Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        //TODO: make switch cases to customize notification as per situation
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                .setContentTitle(title)
                .setContentText(message)
                .setSmallIcon(notificationIconIdType)
                .setSound(alarmSound)
                .setAutoCancel(true);
        //TODO: remove hard coded string
        Intent intent = new Intent(context, MainActivity.class);
        TaskStackBuilder taskStackBuilder = TaskStackBuilder.create(context)
                .addNextIntentWithParentStack(intent);
        PendingIntent pendingIntent = taskStackBuilder.getPendingIntent(
                0,
                PendingIntent.FLAG_UPDATE_CURRENT
        );
        builder.setContentIntent(pendingIntent);
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(notificationID,builder.build());
        //TODO: set unique notification number to 0; using random number so that multiple notifications can be created (for debugging only)

        saveDebugMessage(message,context);
    }

    public static void saveDebugMessage(String message, Context context) {
        Calendar rightNow = Calendar.getInstance();
        int hour = rightNow.get(Calendar.HOUR_OF_DAY);
        int minutes = rightNow.get(Calendar.MINUTE);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

        String notificationDebugString = sharedPreferences.getString(context.getString(R.string.notification_debug_string),"");
        notificationDebugString = notificationDebugString +
                " " + hour + ":" + minutes + "\t" + message + "\n";
        sharedPreferences.edit().putString(context.getString(R.string.notification_debug_string), notificationDebugString).apply();
    }

    private static boolean ifHourBetween(int startHour, int endHour) {
        Calendar rightNow = Calendar.getInstance();
        int hour = rightNow.get(Calendar.HOUR_OF_DAY);
        return hour >= startHour && hour <= endHour;
    }

    private static void setWallpaper(final Context context, String weatherDescription){
        boolean liveWallpaperEnabled = sharedPref.getBoolean(context.getString(R.string.pref_live_wallpaper_enabled_key),false);
        boolean randomWallpaperEnabled = sharedPref.getBoolean(context.getString(R.string.pref_random_wallpapers_enabled),false);

        if (!liveWallpaperEnabled){
            runOnUiThread(new Runnable() {
                public void run()
                {
                    Toast.makeText(context.getApplicationContext(),context.getString(R.string.toast_buy_on_wallpaper_update),Toast.LENGTH_SHORT).show();
                }
            });
            return;
        }
        if(randomWallpaperEnabled){
            WallpaperUtils.setRandomWallpaper(context);
        }
        else WallpaperUtils.setWeatherWallpaper(context,weatherDescription);
        saveDebugMessage("Wallpapaer updated",context);
    }

    private static void updateWidget(Context context) {
        Intent intent = new Intent(context,WeatherWidget.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
// Use an array and EXTRA_APPWIDGET_IDS instead of AppWidgetManager.EXTRA_APPWIDGET_ID,
// since it seems the onUpdate() is only fired on that:
        context.sendBroadcast(intent);
    }
}
