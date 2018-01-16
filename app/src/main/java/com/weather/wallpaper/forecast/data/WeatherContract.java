package com.weather.wallpaper.forecast.data;

import android.net.Uri;
import android.provider.BaseColumns;

import com.weather.wallpaper.forecast.utilities.SunshineDateUtils;

import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * Created by Vaibhav on 7/20/2017.
 */

public class WeatherContract {

    private static final String TAG = WeatherContract.class.getSimpleName();

    public static final String CONTENT_AUTHORITY = "com.weather.wallpaper.forecast";
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    public static class WeatherEntry implements BaseColumns {

        public static final String PATH = "weather";
        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon()
                .appendPath(PATH)
                .build();

        public static final String TABLE_NAME = "weather";
        public static final String COLUMN_DATE = "date";
        public static final String COLUMN_WEATHER_TYPE = "type";
        public static final String COLUMN_CURRENT_TEMP = "temp";
        public static final String COLUMN_PRESSURE = "pressure";
        public static final String COLUMN_HUMIDITY = "humidity";
        public static final String COLUMN_WIND_SPEED = "wind";
        public static final String COLUMN_WIND_DEGREES = "degrees";
        public static final String COLUMN_MAX_TEMP = "max_temp";
        public static final String COLUMN_MIN_TEMP = "min_temp";

        /* Weather ID as returned by API, used to identify the icon to be used */
        public static final String COLUMN_WEATHER_ID = "weather_id";

        public static final String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME + " (" +
                _ID + " INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                COLUMN_DATE + " INTEGER NOT NULL, " +
                COLUMN_WEATHER_ID + " INTEGER NOT NULL, " +
                COLUMN_WEATHER_TYPE + " TEXT NOT NULL, " +
                COLUMN_MAX_TEMP + " REAL NOT NULL, " +
                COLUMN_MIN_TEMP + " REAL NOT NULL, " +
                COLUMN_CURRENT_TEMP + " REAL NOT NULL, " +
                COLUMN_PRESSURE + " REAL NOT NULL, " +
                COLUMN_HUMIDITY + " INTEGER NOT NULL, " +
                COLUMN_WIND_SPEED + " REAL NOT NULL, " +
                COLUMN_WIND_DEGREES + " REAL NOT NULL, " +

                /*
                 * To ensure this table can only contain one weather entry per date, we declare
                 * the date column to be unique. We also specify "ON CONFLICT REPLACE". This tells
                 * SQLite that if we have a weather entry for a certain date and we attempt to
                 * insert another weather entry with that date, we replace the old weather entry.
                 */
                " UNIQUE (" + COLUMN_DATE + ") ON CONFLICT REPLACE);";

        /**
         * Builds a URI that adds the weather date to the end of the forecast content URI path.
         * This is used to query details about a single weather entry by date. This is what we
         * use for the detail view query. We assume a normalized date is passed to this method.
         *
         * @param date Normalized date in milliseconds
         * @return Uri to query details about a single weather entry
         */
        public static Uri buildWeatherUriWithDate(long date) {
            return CONTENT_URI.buildUpon()
                    .appendPath(Long.toString(date))
                    .build();
        }

        /**
         * Returns just the selection part of the weather query from a normalized today value.
         * This is used to get a weather forecast from today's date. To make this easy to use
         * in compound selection, we embed today's date as an argument in the query.
         *
         * @return The selection part of the weather query for today onwards
         */
        public static String getSqlSelectForTodayOnwards() {
            long normalizedUtcNow = SunshineDateUtils.normalizeDate(System.currentTimeMillis());
            return WeatherContract.WeatherEntry.COLUMN_DATE + " >= " + normalizedUtcNow;
        }

        public static String getSqlSelectForOnlyToday(){
            long oneDayInSeconds = TimeUnit.DAYS.toSeconds(1);
            long currentTimeInSeconds = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis());
            long currentLocalTimeInSeconds = SunshineDateUtils.getLocalDateFromUTC(currentTimeInSeconds);
            String selectionStatement = WeatherContract.WeatherEntry.COLUMN_DATE + "/" +oneDayInSeconds + " = " + currentLocalTimeInSeconds + "/" + oneDayInSeconds;

            return selectionStatement;
        }

        public static String getSqlSelectForDate(long dateInSeconds){
            long oneDayInSeconds = TimeUnit.DAYS.toSeconds(1);
            return WeatherContract.WeatherEntry.COLUMN_DATE + "/" +oneDayInSeconds + " = " + dateInSeconds + "/" + oneDayInSeconds;
        }

        public static String getSqlSelectFromNowToday(){
            long dateInSeconds = SunshineDateUtils.getCurrentLocalDateSeconds();
            return WeatherEntry.COLUMN_DATE + " >= " + dateInSeconds + " AND " + getSqlSelectForOnlyToday();
        }

    }

    public static class WeekDayEntry implements BaseColumns {
        public static final String PATH = "week";
        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon()
                .appendPath(PATH)
                .build();

        public static final String TABLE_NAME = "week";
        public static final String COLUMN_DATE = "date";
        public static final String COLUMN_WEATHER_TYPE = "type";
        public static final String COLUMN_CURRENT_TEMP = "currentTemp";
        public static final String COLUMN_MAX_TEMP = "max_temp";
        public static final String COLUMN_MIN_TEMP = "min_temp";

        /* Weather ID as returned by API, used to identify the icon to be used */
        public static final String COLUMN_WEATHER_ID = "weather_id";

        public static final String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME + " (" +
                _ID + " INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                COLUMN_DATE + " INTEGER NOT NULL, " +
                COLUMN_WEATHER_ID + " INTEGER NOT NULL, " +
                COLUMN_WEATHER_TYPE + " TEXT NOT NULL, " +
                COLUMN_MAX_TEMP + " REAL NOT NULL, " +
                COLUMN_MIN_TEMP + " REAL NOT NULL, " +
                /*
                 * To ensure this table can only contain one weather entry per date, we declare
                 * the date column to be unique. We also specify "ON CONFLICT REPLACE". This tells
                 * SQLite that if we have a weather entry for a certain date and we attempt to
                 * insert another weather entry with that date, we replace the old weather entry.
                 */
                " UNIQUE (" + COLUMN_DATE + ") ON CONFLICT REPLACE);";


        public static String getSqlSelectForTodayOnwards() {
            long currentTime = (System.currentTimeMillis() + TimeZone.getDefault().getOffset(System.currentTimeMillis())) / 1000;
            long oneDayInSeconds = TimeUnit.DAYS.toSeconds(1);
            return WeatherContract.WeekDayEntry.COLUMN_DATE + "/" +oneDayInSeconds + " >= " + currentTime + "/" + oneDayInSeconds;
        }

        public static String getSqlSelectForTodayOnly() {
            long currentTime = (System.currentTimeMillis() + TimeZone.getDefault().getOffset(System.currentTimeMillis())) / 1000;
            long oneDayInSeconds = TimeUnit.DAYS.toSeconds(1);
            return WeatherContract.WeekDayEntry.COLUMN_DATE + "/" +oneDayInSeconds + " = " + currentTime + "/" + oneDayInSeconds;
        }
    }
}
