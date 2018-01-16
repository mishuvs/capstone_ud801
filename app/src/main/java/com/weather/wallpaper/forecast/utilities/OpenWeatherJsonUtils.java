package com.weather.wallpaper.forecast.utilities;

import android.content.ContentValues;
import android.content.Context;

import com.weather.wallpaper.forecast.data.WeatherContract;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.HttpURLConnection;

/**
 * Created by Vaibhav on 7/7/2017.
 */

public final class OpenWeatherJsonUtils {

    /*
    PROBABLY SHOULD USE THE OTHER Own5day3hrJsonUtils.java
     */

    /**
     * This method parses JSON from a web response and returns an array of Strings
     * describing the weather over various days from the forecast.
     * <p/>
     * Later on, we'll be parsing the JSON into structured data within the
     * getFullWeatherDataFromJson function, leveraging the data we have stored in the JSON. For
     * now, we just convert the JSON into human-readable strings.
     *
     * @param forecastJsonStr JSON response from server
     *
     * @return Array of Strings describing weather data
     *
     * @throws JSONException If JSON data cannot be properly parsed
     */
    public static ContentValues[] getSimpleWeatherStringsFromJson(Context context, String forecastJsonStr)
            throws JSONException {

        /* Weather information. Each day's forecast info is an element of the "list" array */
        final String OWM_LIST = "list";

        /* All temperatures are children of the "temp" object */

        /* Max temperature for the day */
        final String OWM_MAX = "max";
        final String OWM_MIN = "min";
        final String OWM_PRESSURE = "pressure";
        final String OWM_HUMIDITY = "humidity";
        final String OWM_WIND = "speed";
        final String OWM_DEGREES = "deg";

        final String OWM_WEATHER = "weather";
        final String OWM_DESCRIPTION = "main";

        final String OWM_MESSAGE_CODE = "cod";
        final String OWM_MAIN = "temp";

        final String OWM_WEATHER_ID = "id";

        /* String array to hold each day's weather String */
        String[] parsedWeatherData = null;

        JSONObject forecastJson = new JSONObject(forecastJsonStr);

        /* Is there an error? */
        if (forecastJson.has(OWM_MESSAGE_CODE)) {
            int errorCode = forecastJson.getInt(OWM_MESSAGE_CODE);

            switch (errorCode) {
                case HttpURLConnection.HTTP_OK:
                    break;
                case HttpURLConnection.HTTP_NOT_FOUND:
                    /* Location invalid */
                    return null;
                default:
                    /* Server probably down */
                    return null;
            }
        }

        JSONArray weatherArray = forecastJson.getJSONArray(OWM_LIST);

        parsedWeatherData = new String[weatherArray.length()];

        long localDate = System.currentTimeMillis();
        long utcDate = SunshineDateUtils.getUTCDateFromLocal(localDate);
        long startDay = SunshineDateUtils.normalizeDate(utcDate);
        ContentValues[] contentValuesArray = new ContentValues[weatherArray.length()];

        long normalizedUtcStartDay = SunshineDateUtils.getNormalizedUtcDateForToday();

        /* These are the values that will be collected */
        String highAndLow;
        long dateTimeMillis;
        double high;
        double low;
        String description;
        int weatherId;
        double pressure;
        int humidity;
        double wind_speed;
        double wind_degrees;

        for (int i = 0; i < weatherArray.length(); i++) {

            /* Get the JSON object representing the day */
            JSONObject dayForecast = weatherArray.getJSONObject(i);
            /*
             * Description is in a child array called "weather", which is 1 element long.
             * That element also contains a weather code.
             */
            JSONObject weatherObject =
                    dayForecast.getJSONArray(OWM_WEATHER).getJSONObject(0);
            weatherId = weatherObject.getInt(OWM_WEATHER_ID);
            description = weatherObject.getString(OWM_DESCRIPTION);

            /*
             * Temperatures are sent by Open Weather Map in a child object called "temp".
             *
             * Editor's Note: Try not to name variables "temp" when working with temperature.
             * It confuses everybody. Temp could easily mean any number of things, including
             * temperature, temporary and is just a bad variable name.
             */
            JSONObject mainObject = dayForecast.getJSONObject(OWM_MAIN);

            dateTimeMillis = normalizedUtcStartDay + SunshineDateUtils.DAY_IN_MILLIS * i;

            pressure = dayForecast.getDouble(OWM_PRESSURE);
            humidity = dayForecast.getInt(OWM_HUMIDITY);
            wind_speed = dayForecast.getDouble(OWM_WIND);
            wind_degrees = dayForecast.getDouble(OWM_DEGREES);

            high = mainObject.getDouble(OWM_MAX);
            low = mainObject.getDouble(OWM_MIN);

            ContentValues values = new ContentValues();
            values.put(WeatherContract.WeatherEntry.COLUMN_WEATHER_ID, weatherId);
            values.put(WeatherContract.WeatherEntry.COLUMN_MAX_TEMP, high);
            values.put(WeatherContract.WeatherEntry.COLUMN_MIN_TEMP, low);
            values.put(WeatherContract.WeatherEntry.COLUMN_DATE, dateTimeMillis);
            values.put(WeatherContract.WeatherEntry.COLUMN_WEATHER_TYPE, description);
            values.put(WeatherContract.WeatherEntry.COLUMN_PRESSURE, pressure);
            values.put(WeatherContract.WeatherEntry.COLUMN_HUMIDITY, humidity);
            values.put(WeatherContract.WeatherEntry.COLUMN_WIND_SPEED, wind_speed);
            values.put(WeatherContract.WeatherEntry.COLUMN_WIND_DEGREES, wind_degrees);

            contentValuesArray[i] = values;
        }

        return contentValuesArray;
    }

    /**
     * Parse the JSON and convert it into ContentValues that can be inserted into our database.
     *
     * @param context         An application context, such as a service or activity context.
     * @param forecastJsonStr The JSON to parse into ContentValues.
     *
     * @return An array of ContentValues parsed from the JSON.
     */
    public static ContentValues[] getFullWeatherDataFromJson(Context context, String forecastJsonStr) {
        /** This will be implemented in a future lesson **/
        return null;
    }

}