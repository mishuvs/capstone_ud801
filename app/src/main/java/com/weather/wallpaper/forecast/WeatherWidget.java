package com.weather.wallpaper.forecast;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.widget.RemoteViews;

import com.weather.wallpaper.forecast.data.WeatherContract;
import com.weather.wallpaper.forecast.utilities.SunshineDateUtils;
import com.weather.wallpaper.forecast.utilities.SunshineWeatherUtils;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Implementation of App Widget functionality.
 */
public class WeatherWidget extends AppWidgetProvider {
    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId) {
        appWidgetManager.updateAppWidget(appWidgetId, setWidgetData(context));
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
            // Create an Intent to launch ExampleActivity
            Intent intent = new Intent(context, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);

            // Get the layout for the App Widget and attach an on-click listener
            // to the button
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.weather_widget);
            views.setOnClickPendingIntent(R.id.widget, pendingIntent);

            // Tell the AppWidgetManager to perform an update on the current app widget
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }

    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
    }

    private static RemoteViews setWidgetData(Context context){

        String selection = WeatherContract.WeatherEntry.getSqlSelectFromNowToday();
        Uri forecastUri = WeatherContract.WeatherEntry.CONTENT_URI;
        String sortOrder = WeatherContract.WeatherEntry.COLUMN_DATE + " ASC";

        ArrayList<String> projectionList = new ArrayList<>(Arrays.asList(MainActivity.MAIN_FORECAST_PROJECTION));
        projectionList.add(6,WeatherContract.WeatherEntry.COLUMN_CURRENT_TEMP);
        String[] projectionArray = new String[projectionList.size()];
        projectionList.toArray(projectionArray);

        Cursor mCursor = context.getContentResolver().query(
                forecastUri,
                projectionArray,
                selection,
                null,
                sortOrder);

        if(mCursor==null || mCursor.getCount()==0) return null;
        mCursor.moveToFirst();
        // Construct the RemoteViews object
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.weather_widget);
        // Instruct the widget manager to update the widget

        long dateInSeconds;
        int index;

        /* Read TIME */
        index = mCursor.getColumnIndex(MainActivity.MAIN_FORECAST_PROJECTION[1]);
        dateInSeconds = mCursor.getLong(index);
        /* Get human readable string using our utility method */
        String timeString = SunshineDateUtils.getReadableTime(dateInSeconds);
        views.setTextViewText(R.id.time,timeString);

        /* Read DATE */
        String dateString = SunshineDateUtils.getFriendlyDateString(context, dateInSeconds, true);
        views.setTextViewText(R.id.date,dateString);

        /*Setting resource icon*/
        index = mCursor.getColumnIndex(MainActivity.MAIN_FORECAST_PROJECTION[2]);
        views.setImageViewResource(R.id.icon,SunshineWeatherUtils.getIconResourceForWeatherCondition(
                mCursor.getInt(index)
        ));

        /*Set description*/
        index = mCursor.getColumnIndex(MainActivity.MAIN_FORECAST_PROJECTION[3]);
        views.setTextViewText(R.id.description,mCursor.getString(index));
        //setting the background image depending on current weather:

        /*setting CURRENT TEMPERATURE*/
        index = mCursor.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_CURRENT_TEMP);
        String currentTemp = SunshineWeatherUtils.formatTemperature(context,mCursor.getDouble(index));
        views.setTextViewText(R.id.currentTemp,currentTemp);

        mCursor.close();

        //Using week data:
        forecastUri = WeatherContract.WeekDayEntry.CONTENT_URI;
        sortOrder = WeatherContract.WeekDayEntry.COLUMN_DATE + " ASC";
        String weekSelection = WeatherContract.WeekDayEntry.getSqlSelectForTodayOnly();

        mCursor = context.getContentResolver().query(
                forecastUri,
                new String[]{WeatherContract.WeekDayEntry.COLUMN_MIN_TEMP, WeatherContract.WeekDayEntry.COLUMN_MAX_TEMP},
                weekSelection,
                null,
                sortOrder
        );
        if(mCursor==null || mCursor.getCount()==0) {
            return null;
        }
        mCursor.moveToFirst();
        /*Set today's minTemperature and maxTemperature*/

        //MIN TEMP
        index = mCursor.getColumnIndex(WeatherContract.WeekDayEntry.COLUMN_MIN_TEMP);
        String minTemp = SunshineWeatherUtils.formatTemperature(context,mCursor.getDouble(index));
        views.setTextViewText(R.id.minTemp,minTemp);

        //MAX TEMP
        index = mCursor.getColumnIndex(WeatherContract.WeekDayEntry.COLUMN_MAX_TEMP);
        String maxTemp = SunshineWeatherUtils.formatTemperature(context,mCursor.getDouble(index));
        views.setTextViewText(R.id.maxTemp,maxTemp);

        return views;
    }
}

