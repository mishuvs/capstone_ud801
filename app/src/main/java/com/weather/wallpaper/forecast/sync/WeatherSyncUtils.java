package com.weather.wallpaper.forecast.sync;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.support.v7.preference.PreferenceManager;
import android.view.View;

import com.firebase.jobdispatcher.Constraint;
import com.firebase.jobdispatcher.FirebaseJobDispatcher;
import com.firebase.jobdispatcher.GooglePlayDriver;
import com.firebase.jobdispatcher.Job;
import com.firebase.jobdispatcher.Lifetime;
import com.firebase.jobdispatcher.RetryStrategy;
import com.firebase.jobdispatcher.Trigger;
import com.weather.wallpaper.forecast.MainActivity;
import com.weather.wallpaper.forecast.R;
import com.weather.wallpaper.forecast.data.WeatherContract;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

/**
 * Created by Vaibhav on 8/13/2017.
 */

public class WeatherSyncUtils {

    public static boolean sInitialized = false;

    public final static int UPDATE_FREQUENCY_HOURS = 2;
    //TODO: change this to correct one... only for debugging now
    private final static int UPDATE_FREQUENCY_SECONDS = (int) TimeUnit.HOURS.toSeconds(UPDATE_FREQUENCY_HOURS);
    private final static int UPDATE_MAX_LIMIT_SECONDS = UPDATE_FREQUENCY_SECONDS / 3;

    public final static String WEATHER_SYNC_TAG = "WEATHER SYNC JOB";

    public static void initialize(final Context context){
        //Don't do anything if sInitialized is already true
        sInitialized = true;
        //this method is called to schedule the job
        //but it schedules only for only the next time...
        //so this time we need to use startImmediateSync() method

        buildAndDispatchJob(context);

        //We need to check if the content provider is empty
        // in case the app was newly installed, or the data wiped
        //Doing this in a background thread:....

        InitializeAsyncTask task = new InitializeAsyncTask(context);
        task.execute();

        setSyncAlarm(context);

    }

    private static void setSyncAlarm(Context context) {
        //set recurring alarm, since we can't rely on job scheduler
        AlarmManager alarmMgr = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, AlarmReceiver.class);
        PendingIntent alarmIntent = PendingIntent.getBroadcast(context, 0, intent, 0);

        Calendar calendar = Calendar.getInstance();

        alarmMgr.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(),
                AlarmManager.INTERVAL_HALF_DAY, alarmIntent);
    }

    static class InitializeAsyncTask extends AsyncTask<Void,Void,Void>{

        Context context;

        InitializeAsyncTask(Context mContext) {
            context = mContext;
        }

        @Override
        protected Void doInBackground(Void... params) {
            //we need to query only the ids to find out if it's empty
            String[] projectionColumns = {WeatherContract.WeatherEntry._ID};
            //checking for the present day...
            String selectionStatement = WeatherContract.WeatherEntry
                    .getSqlSelectForTodayOnwards();
            Cursor cursor = context.getContentResolver().query(
                    WeatherContract.WeatherEntry.CONTENT_URI,
                    projectionColumns,
                    selectionStatement,
                    null,
                    null);

            if(cursor==null || cursor.getCount()==0) {
                startImmediateSync(context);
                //Checking the cursor values again:
                cursor = context.getContentResolver().query(WeatherContract.WeatherEntry.CONTENT_URI, projectionColumns, selectionStatement, null, null);
                if (cursor != null && cursor.getCount() > 0) {
                    cursor.close();
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            MainActivity mActivity = (MainActivity) context;
            mActivity.mainBinding.progressBar.setVisibility(View.INVISIBLE);
            PreferenceManager.getDefaultSharedPreferences(context)
                    .edit().putBoolean(context.getString(R.string.is_initialized_key), true).apply();
        }
    }

    static void buildAndDispatchJob(Context context){

        FirebaseJobDispatcher dispatcher = new FirebaseJobDispatcher(new GooglePlayDriver(context));

        Job syncJob = dispatcher.newJobBuilder()
                // the JobService that will be called
                .setService(WeatherFirebaseJobService.class)
                // uniquely identifies the job
                .setTag(WEATHER_SYNC_TAG)
                // one-off job
                .setRecurring(true)
                // run forever
                .setLifetime(Lifetime.FOREVER)
                .setTrigger(Trigger.executionWindow(
                        UPDATE_FREQUENCY_SECONDS,
                        UPDATE_FREQUENCY_SECONDS + UPDATE_MAX_LIMIT_SECONDS))
                // don't overwrite an existing job with the same tag
                .setReplaceCurrent(false)
                // constraints that need to be satisfied for the job to run
                .setConstraints(Constraint.ON_ANY_NETWORK)
                .setRetryStrategy(RetryStrategy.DEFAULT_LINEAR)
                .build();

        dispatcher.mustSchedule(syncJob);
        WeatherSyncTask.saveDebugMessage("Job Service Dispatched",context);
    }

    synchronized public static void startImmediateSync(Context context){
        Intent serviceIntent = new Intent(context, WeatherSyncIntentService.class);
        context.startService(serviceIntent);
    }
}
