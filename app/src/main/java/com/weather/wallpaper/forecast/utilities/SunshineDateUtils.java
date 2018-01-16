package com.weather.wallpaper.forecast.utilities;

import android.content.Context;
import android.preference.PreferenceManager;
import android.text.format.DateUtils;

import com.weather.wallpaper.forecast.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * Created by Vaibhav on 7/7/2017.
 */

public final class SunshineDateUtils {

    public static final long SECOND_IN_MILLIS = 1000;
    public static final long MINUTE_IN_MILLIS = SECOND_IN_MILLIS * 60;
    public static final long HOUR_IN_MILLIS = MINUTE_IN_MILLIS * 60;
    public static final long DAY_IN_MILLIS = HOUR_IN_MILLIS * 24;
    private static final String TAG = SunshineDateUtils.class.getSimpleName();

    /**
     * This method returns the number of days since the epoch (January 01, 1970, 12:00 Midnight UTC)
     * in UTC time from the current date.
     *
     * @param dateInMillis A date in milliseconds.
     *
     * @return The number of days in UTC time from the beginning of time in input date's timezone.
     */
    public static long getDayNumber(long dateInMillis) {
        return dateInMillis / DAY_IN_MILLIS;
    }

    /**
     * To make it easy to query for the exact date, we normalize all dates that go into
     * the database to the start of the day in UTC time.
     *
     * @param date The UTC date to normalize
     *
     * @return The UTC date at 12 midnight
     */
    public static long normalizeDate(long date) {
        // Normalize the start date to the beginning of the (UTC) day in local time
        long retValNew = date / DAY_IN_MILLIS * DAY_IN_MILLIS;
        return retValNew;
    }

    /**
     * In order to ensure consistent inserts into WeatherProvider, we check that dates have been
     * normalized before they are inserted. If they are not normalized, we don't want to accept
     * them, and leave it up to the caller to throw an IllegalArgumentException.
     *
     * @param millisSinceEpoch Milliseconds since January 1, 1970 at midnight
     *
     * @return true if the date represents the beginning of a day in Unix time, false otherwise
     */
    public static boolean isDateNormalized(long millisSinceEpoch) {
        boolean isDateNormalized = false;
        if (millisSinceEpoch % DAY_IN_MILLIS == 0) {
            isDateNormalized = true;
        }

        return isDateNormalized;
    }

    /**
     * Since all dates from the database are in UTC, we must convert the given date
     * (in UTC timezone) to the date in the local timezone. Ths function performs that conversion
     * using the TimeZone offset.
     *
     * @param utcDate The UTC datetime to convert to a local datetime, in milliseconds.
     * @return The local date (the UTC datetime - the TimeZone offset) in milliseconds.
     */
    public static long getLocalDateFromUTC(long utcDate) {
        long utcDateInMillis = correctDateForMillis(utcDate);
        TimeZone tz = TimeZone.getDefault();
        long gmtOffset = tz.getOffset(utcDateInMillis);
        long localDateInMillis = utcDateInMillis + gmtOffset;

        //returning local date in seconds
        return localDateInMillis/1000;
    }

    /**
     * checks if the long date input is in seconds, if not then returns in seconds
     * @param utcDate
     * @return
     */
    private static long correctDateForMillis(long date) {
        long constant = 1000000000000L;
        if(date/constant == 0){
            return date*1000;
        }
        else return date;
    }

    /**
     * Since all dates from the database are in UTC, we must convert the local date to the date in
     * UTC time. This function performs that conversion using the TimeZone offset.
     *
     * @param localDate The local datetime to convert to a UTC datetime, in milliseconds.
     * @return The UTC date (the local datetime + the TimeZone offset) in milliseconds.
     */
    //TODO: CAN BE DELETED this method is used only by OpenWeatherJsonUtils, while I am using Owm5...
    public static long getUTCDateFromLocal(long localDate) {
        localDate = correctDateForMillis(localDate);
        TimeZone tz = TimeZone.getDefault();
        long gmtOffset = tz.getOffset(localDate);
        return localDate - gmtOffset;
    }

    /**
     * Helper method to convert the database representation of the date into something to display
     * to users.  As classy and polished a user experience as "20140102" is, we can do better.
     * <p/>
     * The day string for forecast uses the following logic:
     * For today: "Today, June 8"
     * For tomorrow:  "Tomorrow"
     * For the next 5 days: "Wednesday" (just the day name)
     * For all days after that: "Mon, Jun 8" (Mon, 8 Jun in UK, for mishu)
     *
     * @param context      Context to use for resource localization
     * @param localDateInSeconds The date in milliseconds (UTC)
     * @param showFullDate Used to show a fuller-version of the date, which always contains either
     *                     the day of the week, today, or tomorrow, in addition to the date.
     *
     * @return A user-friendly representation of the date such as "Today, June 8", "Tomorrow",
     * or "Friday"
     */
    public static String getFriendlyDateString(Context context, long localDateInSeconds, boolean showFullDate) {

        //TODO:
        long localDateInMillis = correctDateForMillis(localDateInSeconds);
        long dayNumber = getDayNumber(localDateInMillis);

        TimeZone userTimeZone = TimeZone.getDefault();
        long gmtOffset = userTimeZone.getOffset(System.currentTimeMillis());
        long currentLocalDateInMillis = System.currentTimeMillis() + gmtOffset;
        long currentDayNumber = getDayNumber(currentLocalDateInMillis);

        //TODO: I think it should be dayNumber == currentDayNumber || !showFullDate
        if (dayNumber == currentDayNumber || showFullDate) {
            /*
             * If the date we're building the String for is today's date, the format
             * is "Today, June 24"
             */
            String dayName = getDayName(context, localDateInMillis);
            String readableDate = getReadableDateString(context, localDateInMillis);
            if (dayNumber - currentDayNumber < 2) {
                /*
                 * Since there is no localized format that returns "Today" or "Tomorrow" in the API
                 * levels we have to support, we take the name of the day (from SimpleDateFormat)
                 * and use it to replace the date from DateUtils. This isn't guaranteed to work,
                 * but our testing so far has been conclusively positive.
                 *
                 * For information on a simpler API to use (on API > 18), please check out the
                 * documentation on DateFormat#getBestDateTimePattern(Locale, String)
                 * https://developer.android.com/reference/android/text/format/DateFormat.html#getBestDateTimePattern
                 */
                String localizedDayName = new SimpleDateFormat("EEEE").format(localDateInMillis);
                return readableDate.replace(localizedDayName, dayName);
            } else {
                return readableDate;
            }
        } else if (dayNumber < currentDayNumber + 7) {
            /* If the input date is less than a week in the future, just return the day name. */
            return getDayName(context, localDateInMillis);
        } else {
            int flags = DateUtils.FORMAT_SHOW_DATE
                    | DateUtils.FORMAT_NO_YEAR
                    | DateUtils.FORMAT_ABBREV_ALL
                    | DateUtils.FORMAT_SHOW_WEEKDAY;

            return DateUtils.formatDateTime(context, localDateInMillis, flags);
        }
    }

    /**
     * Returns a date string in the format specified, which shows a date, without a year,
     * abbreviated, showing the full weekday.
     *
     * @param context      Used by DateUtils to formate the date in the current locale
     * @param timeInMillis Time in milliseconds since the epoch (local time)
     *
     * @return The formatted date string
     */
    private static String getReadableDateString(Context context, long timeInMillis) {
        int flags = DateUtils.FORMAT_SHOW_DATE
                | DateUtils.FORMAT_NO_YEAR
                | DateUtils.FORMAT_SHOW_WEEKDAY;

        return DateUtils.formatDateTime(context, timeInMillis, flags);
    }

    /**
     * Given a day, returns just the name to use for that day.
     *   E.g "today", "tomorrow", "Wednesday".
     *
     * @param context      Context to use for resource localization
     * @param dateInMillis The date in milliseconds (local time)
     *
     * @return the string day of the week
     */
    public static String getDayName(Context context, long dateInMillis) {
        /*
         * If the date is today, return the localized version of "Today" instead of the actual
         * day name.
         */
        long dayNumber = getDayNumber(dateInMillis);

        long gmtOffset = TimeZone.getDefault().getOffset(System.currentTimeMillis());
        long currentDayNumber = getDayNumber(System.currentTimeMillis() + gmtOffset);

        if (dayNumber == currentDayNumber) {
            return context.getString(R.string.today);
        } else if (dayNumber == currentDayNumber + 1) {
            return context.getString(R.string.tomorrow);
        } else {
            /*
             * Otherwise, if the day is not today, the format is just the day of the week
             * (e.g "Wednesday")
             */
            SimpleDateFormat dayFormat = new SimpleDateFormat("EEE");
            return dayFormat.format(dateInMillis);
        }
    }

    public static long getNormalizedUtcDateForToday() {

        /*
         * This number represents the number of milliseconds that have elapsed since January
         * 1st, 1970 at midnight in the GMT time zone.
         */
        long utcNowMillis = System.currentTimeMillis();

        /*
         * This TimeZone represents the device's current time zone. It provides us with a means
         * of acquiring the offset for local time from a UTC time stamp.
         */
        TimeZone currentTimeZone = TimeZone.getDefault();

        /*
         * The getOffset method returns the number of milliseconds to add to UTC time to get the
         * elapsed time since the epoch for our current time zone. We pass the current UTC time
         * into this method so it can determine changes to account for daylight savings time.
         */
        long gmtOffsetMillis = currentTimeZone.getOffset(utcNowMillis);

        /*
         * UTC time is measured in milliseconds from January 1, 1970 at midnight from the GMT
         * time zone. Depending on your time zone, the time since January 1, 1970 at midnight (GMT)
         * will be greater or smaller. This variable represents the number of milliseconds since
         * January 1, 1970 (GMT) time.
         */
        long timeSinceEpochLocalTimeMillis = utcNowMillis + gmtOffsetMillis;

        /* This method simply converts milliseconds to days, disregarding any fractional days */
        long daysSinceEpochLocal = TimeUnit.MILLISECONDS.toDays(timeSinceEpochLocalTimeMillis);

        /*
         * Finally, we convert back to milliseconds. This time stamp represents today's date at
         * midnight in GMT time. We will need to account for local time zone offsets when
         * extracting this information from the database.
         */
        long normalizedUtcMidnightMillis = TimeUnit.DAYS.toMillis(daysSinceEpochLocal);

        return normalizedUtcMidnightMillis;
    }

    /**
     * converts epoch time to minimal date format for week RecyclerView
     * @param epochTime expected in milliseconds.
     * @return
     */
    public static String getMinimalDate(long epochTime){
        Date UTCDate = new Date(epochTime);
        Date localDate; // How to get this?

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd/MM" );
        simpleDateFormat.setTimeZone(TimeZone.getDefault());


        return simpleDateFormat.format(UTCDate);
    }

    public static String getReadableTime(long longDate){
        long longDateInMillis = correctDateForMillis(longDate);
        Date date = new Date(longDateInMillis);

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("hh:mm a" );

        //TODO: Why is there a need to add the time zone GMT, after all, isn't it the default one equal to UTC?
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));

        return simpleDateFormat.format(date);
    }

    public static long getLocalNextDateSeconds() {
        long currentLocalDateSeconds = getCurrentLocalDateSeconds();
        long oneDayInSeconds = TimeUnit.DAYS.toSeconds(1);
        return currentLocalDateSeconds + oneDayInSeconds;
    }

    public static long getCurrentLocalDateSeconds() {
        long currentDateMillis = System.currentTimeMillis();
        long gmtOffsetMillis = TimeZone.getDefault().getOffset(System.currentTimeMillis());
        long currentLocalDateMillis = currentDateMillis + gmtOffsetMillis;
        return currentLocalDateMillis/1000;
    }

    public static long timeSinceLastUpdated(Context context){
        long lastUpdateTime = PreferenceManager.getDefaultSharedPreferences(context).getLong(context.getString(R.string.time_since_last_update),System.currentTimeMillis());
        return TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()-lastUpdateTime);
    }
}
