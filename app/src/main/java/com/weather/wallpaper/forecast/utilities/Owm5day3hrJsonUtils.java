package com.weather.wallpaper.forecast.utilities;

import android.content.ContentValues;
import android.content.Context;

import com.weather.wallpaper.forecast.data.WeatherContract;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.HttpURLConnection;

/**
 * Created by Vaibhav on 8/26/2017.
 */

public class Owm5day3hrJsonUtils {

    public static final String TAG = Owm5day3hrJsonUtils.class.getSimpleName();

    /* Location information */
    private static final String OWM_CITY = "city";
    private static final String OWM_COORD = "coord";

    /* Location coordinate */
    private static final String OWM_LATITUDE = "lat";
    private static final String OWM_LONGITUDE = "lon";

    /* Weather information. Each day's forecast info is an element of the "list" array */
    private static final String OWM_LIST = "list";

    private static final String OWM_DATE = "dt";

    private static final String OWM_PRESSURE = "pressure";
    private static final String OWM_HUMIDITY = "humidity";
    private static final String OWM_WINDSPEED = "speed";
    private static final String OWM_WIND_DIRECTION = "deg";

    /* All temperatures are children of the "temp" object */
    private static final String OWM_WEEK_TEMPERATURE = "temp";

    /* Max temperature for the day */
    private static final String OWM_MAX = "temp_max";
    private static final String OWM_MIN = "temp_min";
    private static final String OWM_CURRENT_TEMP = "temp";

    private static final String OWM_WEATHER = "weather";
    private static final String OWM_WEATHER_ID = "id";
    private static final String OWM_WEATHER_DESCRIPTION = "description";

    private static final String OWM_MESSAGE_CODE = "cod";
    private static final String OWM_MAIN = "main";
    private static final String OWM_WIND_OBJECT = "wind";

    /*
    Constants for Week table:
     */
    private static final String OWM_WEEK_MAX = "max";
    private static final String OWM_WEEK_MIN = "min";


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
    public static ContentValues[] getWeatherContentValuesFromJson(Context context, String forecastJsonStr)
            throws JSONException {

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

        JSONArray jsonWeatherArray = forecastJson.getJSONArray(OWM_LIST);

        //TODO: check the below line... saving latlng again??
//        JSONObject cityJson = forecastJson.getJSONObject(OWM_CITY);
//
//        JSONObject cityCoord = cityJson.getJSONObject(OWM_COORD);
//        double cityLatitude = cityCoord.getDouble(OWM_LATITUDE);
//        double cityLongitude = cityCoord.getDouble(OWM_LONGITUDE);
//        WeatherPreferences.setLocationDetails(context, cityLatitude, cityLongitude);

        ContentValues[] weatherContentValues = new ContentValues[jsonWeatherArray.length()];

        /*
         * OWM returns daily forecasts based upon the local time of the city that is being asked
         * for, which means that we need to know the GMT offset to translate this data properly.
         * Since this data is also sent in-order and the first day is always the current day, we're
         * going to take advantage of that to get a nice normalized UTC date for all of our weather.
         */
//        long now = System.currentTimeMillis();
//        long normalizedUtcStartDay = SunshineDateUtils.normalizeDate(now);

        long normalizedUtcStartDay = SunshineDateUtils.getNormalizedUtcDateForToday();
        long localDateTimeSeconds, dateTimeSeconds;

        for (int i = 0; i < jsonWeatherArray.length(); i++) {

            double pressure;
            int humidity;
            double windSpeed;
            double windDirection;

            double high,low,currentTemp;

            int weatherId;
            String weatherDescription;

            /* Get the JSON object representing the day */
            JSONObject dayForecast = jsonWeatherArray.getJSONObject(i);

            /*
             * We ignore all the datetime values embedded in the JSON and assume that
             * the values are returned in-order by day (which is not guaranteed to be correct).
             */
            //TODO(1): dateTimeSeconds = normalizedUtcStartDay + SunshineDateUtils.DAY_IN_MILLIS * i;
            dateTimeSeconds = dayForecast.getLong(OWM_DATE);
            localDateTimeSeconds = SunshineDateUtils.getLocalDateFromUTC(dateTimeSeconds);

            JSONObject dayForecastMain = dayForecast.getJSONObject(OWM_MAIN);

            pressure = dayForecastMain.getDouble(OWM_PRESSURE);
            humidity = dayForecastMain.getInt(OWM_HUMIDITY);
            high = dayForecastMain.getDouble(OWM_MAX);
            low = dayForecastMain.getDouble(OWM_MIN);
            currentTemp = dayForecastMain.getDouble(OWM_CURRENT_TEMP);

            JSONObject dayForecastWind = dayForecast.getJSONObject(OWM_WIND_OBJECT);
            windSpeed = dayForecastWind.getDouble(OWM_WINDSPEED);
            windDirection = dayForecastWind.getDouble(OWM_WIND_DIRECTION);


            /*
             * Description is in a child array called "weather", which is 1 element long.
             * That element also contains a weather code.
             */
            JSONObject weatherObject =
                    dayForecast.getJSONArray(OWM_WEATHER).getJSONObject(0);

            weatherId = weatherObject.getInt(OWM_WEATHER_ID);

            weatherDescription = weatherObject.getString(OWM_WEATHER_DESCRIPTION);

            /*
             * Temperatures are sent by Open Weather Map in a child object called "temp".
             *
             * Editor's Note: Try not to name variables "temp" when working with temperature.
             * It confuses everybody. Temp could easily mean any number of things, including
             * temperature, temporary variable, temporary folder, temporary employee, or many
             * others, and is just a bad variable name.
             */
//            JSONObject temperatureObject = dayForecast.getJSONObject(OWM_TEMPERATURE);
//            high = temperatureObject.getDouble(OWM_MAX);
//            low = temperatureObject.getDouble(OWM_MIN);

            ContentValues weatherValues = new ContentValues();
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_DATE, localDateTimeSeconds);
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_HUMIDITY, humidity);
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_PRESSURE, pressure);
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_WIND_SPEED, windSpeed);
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_WIND_DEGREES, windDirection);
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_MAX_TEMP, high);
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_MIN_TEMP, low);
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_CURRENT_TEMP, currentTemp);
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_WEATHER_ID, weatherId);
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_WEATHER_TYPE, weatherDescription);

            weatherContentValues[i] = weatherValues;
        }

        return weatherContentValues;
    }

    public static ContentValues[] getWeekDaysContentValues(Context context, String forecastJsonStr) throws JSONException{
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

        JSONArray jsonWeatherArray = forecastJson.getJSONArray(OWM_LIST);

        //TODO: you can actually use the below values instead of the geocoder
//        JSONObject cityJson = forecastJson.getJSONObject(OWM_CITY);
//
//        JSONObject cityCoord = cityJson.getJSONObject(OWM_COORD);
//        double cityLatitude = cityCoord.getDouble(OWM_LATITUDE);
//        double cityLongitude = cityCoord.getDouble(OWM_LONGITUDE);

        //TODO: check the below line... saving latlng again??
//        WeatherPreferences.setLocationDetails(context, cityLatitude, cityLongitude);

        ContentValues[] weatherContentValues = new ContentValues[jsonWeatherArray.length()];

        /*
         * OWM returns daily forecasts based upon the local time of the city that is being asked
         * for, which means that we need to know the GMT offset to translate this data properly.
         * Since this data is also sent in-order and the first day is always the current day, we're
         * going to take advantage of that to get a nice normalized UTC date for all of our weather.
         */
//        long now = System.currentTimeMillis();
//        long normalizedUtcStartDay = SunshineDateUtils.normalizeDate(now);

//        long normalizedUtcStartDay = SunshineDateUtils.getNormalizedUtcDateForToday();

        for (int i = 0; i < jsonWeatherArray.length(); i++) {

            long dateTimeSeconds, localDateTimeSeconds;
//            double pressure;
//            int humidity;
//            double windSpeed;
//            double windDirection;

            double high;
            double low;

            int weatherId;
            String weatherDescription;

            /* Get the JSON object representing the day */
            JSONObject dayForecast = jsonWeatherArray.getJSONObject(i);

            /*
             * We ignore all the datetime values embedded in the JSON and assume that
             * the values are returned in-order by day (which is not guaranteed to be correct).
             */
            //TODO(1): localDateTimeSeconds = normalizedUtcStartDay + SunshineDateUtils.DAY_IN_MILLIS * i;
            dateTimeSeconds = dayForecast.getLong(OWM_DATE);
            localDateTimeSeconds = SunshineDateUtils.getLocalDateFromUTC(dateTimeSeconds);


            JSONObject dayForecastTemp = dayForecast.getJSONObject(OWM_WEEK_TEMPERATURE);

//            pressure = dayForecastMain.getDouble(OWM_PRESSURE);
//            humidity = dayForecastMain.getInt(OWM_HUMIDITY);
            high = dayForecastTemp.getDouble(OWM_WEEK_MAX);
            low = dayForecastTemp.getDouble(OWM_WEEK_MIN);
//            JSONObject dayForecastWind = dayForecast.getJSONObject(OWM_WIND_OBJECT);
//            windSpeed = dayForecastWind.getDouble(OWM_WINDSPEED);
//            windDirection = dayForecastWind.getDouble(OWM_WIND_DIRECTION);
            /*
             * Description is in a child array called "weather", which is 1 element long.
             * That element also contains a weather code.
             */
            JSONObject weatherObject = dayForecast.getJSONArray(OWM_WEATHER).getJSONObject(0);

            weatherId = weatherObject.getInt(OWM_WEATHER_ID);

            weatherDescription = SunshineWeatherUtils.getMainDescription(
                    weatherObject.getString(OWM_WEATHER_DESCRIPTION)
            );

            /*
             * Temperatures are sent by Open Weather Map in a child object called "temp".
             *
             * Editor's Note: Try not to name variables "temp" when working with temperature.
             * It confuses everybody. Temp could easily mean any number of things, including
             * temperature, temporary variable, temporary folder, temporary employee, or many
             * others, and is just a bad variable name.
             */
//            JSONObject temperatureObject = dayForecast.getJSONObject(OWM_TEMPERATURE);
//            high = temperatureObject.getDouble(OWM_MAX);
//            low = temperatureObject.getDouble(OWM_MIN);

            ContentValues weatherValues = new ContentValues();
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_DATE, localDateTimeSeconds);
//            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_HUMIDITY, humidity);
//            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_PRESSURE, pressure);
//            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_WIND_SPEED, windSpeed);
//            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_WIND_DEGREES, windDirection);
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_MAX_TEMP, high);
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_MIN_TEMP, low);
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_WEATHER_ID, weatherId);
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_WEATHER_TYPE, weatherDescription);

            weatherContentValues[i] = weatherValues;
            //TODO: Complete this
        }

        return weatherContentValues;
    }
}
