package com.weather.wallpaper.forecast.data;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * Created by Vaibhav on 7/20/2017.
 */

public class WeatherProvider extends ContentProvider {

    private static final int CODE_WEATHER_WITH_DATE = 100;
    private static final int CODE_WEATHER_LIST = 101;
    //TODO: ADD NEW CODES
    private static final int CODE_WEEK_WITH_DATE = 800;
    private static final int CODE_WEEK_LIST = 801;
    private static final String TAG = WeatherProvider.class.getName();

    SQLiteOpenHelper mDbHelper;
    UriMatcher sUriMatcher = buildUriMatcher();

    public UriMatcher buildUriMatcher(){
        UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        matcher.addURI(WeatherContract.CONTENT_AUTHORITY,WeatherContract.WeatherEntry.PATH,CODE_WEATHER_LIST);
        matcher.addURI(WeatherContract.CONTENT_AUTHORITY,WeatherContract.WeatherEntry.PATH + "/#",CODE_WEATHER_WITH_DATE);
        //TODO:
        matcher.addURI(WeatherContract.CONTENT_AUTHORITY, WeatherContract.WeekDayEntry.PATH, CODE_WEEK_LIST);
        matcher.addURI(WeatherContract.CONTENT_AUTHORITY,WeatherContract.WeekDayEntry.PATH + "/#",CODE_WEEK_LIST);
        return matcher;
    }

    @Override
    public boolean onCreate() {
        mDbHelper = new WeatherDbHelper(getContext());
        return true;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        SQLiteDatabase db = mDbHelper.getReadableDatabase();

        Cursor cursor;

        switch (sUriMatcher.match(uri)){
            case CODE_WEATHER_WITH_DATE:

                String normalizedDateUtcString = uri.getLastPathSegment();

                selectionArgs = new String[]{normalizedDateUtcString};

                cursor = db.query(WeatherContract.WeatherEntry.TABLE_NAME,
                        projection,
                        WeatherContract.WeatherEntry.COLUMN_DATE + " = ? ",
                        selectionArgs,
                        null,
                        null,
                        null);
                break;

            case CODE_WEATHER_LIST:
                cursor = db.query(WeatherContract.WeatherEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder);
                break;

            case CODE_WEEK_WITH_DATE:

                normalizedDateUtcString = uri.getLastPathSegment();

                selectionArgs = new String[]{normalizedDateUtcString};

                cursor = db.query(WeatherContract.WeekDayEntry.TABLE_NAME,
                        projection,
                        WeatherContract.WeekDayEntry.COLUMN_DATE + " = ? ",
                        selectionArgs,
                        null,
                        null,
                        null);
                break;

            case CODE_WEEK_LIST:
                cursor = db.query(WeatherContract.WeekDayEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder);
                break;

            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }

        cursor.setNotificationUri(getContext().getContentResolver(),uri);

        return cursor;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        throw new RuntimeException("We are not implementing getType in Sunshine.");
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        throw new RuntimeException(
                "We are not implementing insert in Sunshine. Use bulkInsert instead");
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        int numberOfItems;
        switch (sUriMatcher.match(uri)){
            case CODE_WEATHER_LIST:
                numberOfItems = db.delete(WeatherContract.WeatherEntry.TABLE_NAME,
                        selection,
                        selectionArgs);
                break;

            case CODE_WEEK_LIST:
                numberOfItems = db.delete(WeatherContract.WeekDayEntry.TABLE_NAME,
                        selection,
                        selectionArgs);
                break;
            //We don't need to delete single items therefore no delete option for that
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }

        if(numberOfItems>0){
            getContext().getContentResolver().notifyChange(uri,null);
        }
        return numberOfItems;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection, @Nullable String[] selectionArgs) {

        //TODO: SHOULD I IMPLEMENT UPDATE? SUNSHINE DIDN'T!!

        throw new RuntimeException(
                "We are not implementing insert in Sunshine. Use bulkInsert instead");
    }

    @Override
    public int bulkInsert(@NonNull Uri uri, @NonNull ContentValues[] values) {
        final SQLiteDatabase db = mDbHelper.getWritableDatabase();
        switch (sUriMatcher.match(uri)){
            case CODE_WEATHER_LIST:

                db.beginTransaction();
                int rowsInserted = 0;
                long _id, weatherDate;
                try{
                    for (ContentValues value : values) {
                        weatherDate = value.getAsLong(WeatherContract.WeatherEntry.COLUMN_DATE);
//TODO(3): tempp
//                          if(!SunshineDateUtils.isDateNormalized(weatherDate)){
//                            throw new IllegalArgumentException("Date must be normalized to insert");
//                        }

                        _id = db.insert(WeatherContract.WeatherEntry.TABLE_NAME,
                                null,
                                value);

                        if(_id != -1){
                            rowsInserted++;
                        }
                    }
                    db.setTransactionSuccessful();
                }finally {
                    db.endTransaction();
                }

                if (rowsInserted > 0) {
                    getContext().getContentResolver().notifyChange(uri, null);
                }

                return rowsInserted;

            case CODE_WEEK_LIST:
                db.beginTransaction();
                rowsInserted = 0;
                try{
                    for (ContentValues value : values) {
                        weatherDate = value.getAsLong(WeatherContract.WeekDayEntry.COLUMN_DATE);
//TODO(3): tempp
//                          if(!SunshineDateUtils.isDateNormalized(weatherDate)){
//                            throw new IllegalArgumentException("Date must be normalized to insert");
//                        }

                        _id = db.insert(WeatherContract.WeekDayEntry.TABLE_NAME,
                                null,
                                value);

                        if(_id != -1){
                            rowsInserted++;
                        }
                    }
                    db.setTransactionSuccessful();
                }finally {
                    db.endTransaction();
                }

                if (rowsInserted > 0) {
                    getContext().getContentResolver().notifyChange(uri, null);
                }

                return rowsInserted;

            default:
                return super.bulkInsert(uri,values);
        }
    }
}
