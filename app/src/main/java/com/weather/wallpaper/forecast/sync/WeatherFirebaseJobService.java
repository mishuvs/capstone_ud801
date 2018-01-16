package com.weather.wallpaper.forecast.sync;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.support.v7.preference.PreferenceManager;

import com.firebase.jobdispatcher.JobParameters;
import com.firebase.jobdispatcher.JobService;
import com.weather.wallpaper.forecast.R;

/**
 * Created by Vaibhav on 8/15/2017.
 */

public class WeatherFirebaseJobService extends JobService {

    //TODO: is firebase's JobService a background thread?
    AsyncTask<Void,Void,Void> mFetchWeatherTask;

    @Override
    public boolean onStartJob(final JobParameters job) {
        mFetchWeatherTask = new AsyncTask<Void,Void,Void>(){
            @Override
            protected Void doInBackground(Void... params) {
                Context context = getApplicationContext();
                WeatherSyncTask.syncWeather(context);
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
                int count = sharedPreferences.getInt(context.getString(R.string.pref_notification_count),0);
                if(count%2==0){
                    WeatherSyncTask.syncWeekWeather(context);
                }
                WeatherSyncTask.checkIfLocationChanged(context);
                WeatherSyncTask.saveDebugMessage("firebase job service ran",context);
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                jobFinished(job,false);
            }
        }.execute();
        //JobFinished is called to signify that we do not have any more things to do.. and that our work here is done
        return true;
    }

    /**
     *you might want to check what this method does... press Ctrl + U... :)
     */
    @Override
    public boolean onStopJob(JobParameters job) {
        //to clean up any mess that may be caused by the framework cancelling our jobs...
        //we are stopping the background thread that was started in onStartJob
        if(mFetchWeatherTask!=null){
            mFetchWeatherTask.cancel(true);
        }
        return true;
        // ^ yes we'd like to reschedule our job.. because you so rudely interrupted us :(
    }
}
