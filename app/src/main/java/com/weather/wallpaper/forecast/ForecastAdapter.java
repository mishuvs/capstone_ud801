package com.weather.wallpaper.forecast;

import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.weather.wallpaper.forecast.data.WeatherContract;
import com.weather.wallpaper.forecast.utilities.SunshineDateUtils;
import com.weather.wallpaper.forecast.utilities.SunshineWeatherUtils;

import java.util.List;

import static com.weather.wallpaper.forecast.MainActivity.SELECTION_FOR_DATE;
import static com.weather.wallpaper.forecast.MainActivity.TODAY_CURSOR_LOADER;

/**
 * Created by Vaibhav on 7/11/2017.
 */

public class ForecastAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>{

    private static final String TAG = "Forecast Adapter";
    private final int VIEW_TYPE_NOW = 0;
    private final int VIEW_TYPE_WEEK = 1;
    private final int VIEW_TYPE_TODAY_HOURS = 2;

    public Cursor mCursor, weekCursor;
    public Context mContext;
    public MainActivity mActivity;
    public RecyclerView mRecyclerView;

    public WeekDaysAdapter weekAdapter;
    public LinearLayoutManager weekLayoutManager;

    public List<Integer> INDEXES;

    final float barHeight;

    ImageView realImage;
    ImageView blurredImage;

    String currentViewMaxTemp,currentViewMinTemp;

    public ForecastAdapter(Context context, Activity activity, RecyclerView recyclerView) {
        mContext = context;
        mActivity = (MainActivity) activity;
        mRecyclerView = recyclerView;
        weekAdapter = new WeekDaysAdapter(mContext);
        weekAdapter.swapCursor(null, this);
        barHeight = R.attr.actionBarSize;
        realImage = mActivity.findViewById(R.id.realImage);
        blurredImage = mActivity.findViewById(R.id.blurredImage);
    }

    @Override
    public int getItemViewType(int position) {
        switch (position){
            case 0: return VIEW_TYPE_NOW;
            case 1: return VIEW_TYPE_WEEK;
            default: return VIEW_TYPE_TODAY_HOURS;
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view;

        switch(viewType){

            case VIEW_TYPE_TODAY_HOURS:
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.hours_item, parent, false);
                return new HoursViewHolder(view);

            case VIEW_TYPE_NOW:
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.forecast_item_now, parent, false);
                return new CurrentViewHolder(view);

            case VIEW_TYPE_WEEK:
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.week_recycler_view, parent, false);
                RecyclerView weekRecyclerView = view.findViewById(R.id.week_recycler_view);
                weekRecyclerView.setAdapter(weekAdapter);
                weekLayoutManager = new LinearLayoutManager(mContext,LinearLayoutManager.HORIZONTAL, false);
                weekRecyclerView.setLayoutManager(weekLayoutManager);
                weekRecyclerView.setHasFixedSize(true);

                //TODO: this probably needs to be changed
                return new ForecastViewHolder(view);
        }
        //TODO:
        return null;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        switch (holder.getItemViewType()){

            case VIEW_TYPE_NOW:
                CurrentViewHolder currentViewHolder = (CurrentViewHolder) holder;

                if( mCursor == null || !mCursor.moveToFirst() ){
                    break;
                }

                mCursor.moveToPosition(position);
                long dateInSeconds;
                int index;

                /* Read DATE */
                index = mCursor.getColumnIndex(MainActivity.MAIN_FORECAST_PROJECTION[1]);
                dateInSeconds = mCursor.getLong(index);
                    /* Get human readable string using our utility method */
                //TODO(5)
//                    String dateString = SunshineDateUtils.newDateMethod(dateInMillis);
                String timeString = SunshineDateUtils.getReadableTime(dateInSeconds);

                currentViewHolder.timeView.setText(timeString);

                /* Read DATE */
                index = mCursor.getColumnIndex(MainActivity.MAIN_FORECAST_PROJECTION[1]);
                dateInSeconds = mCursor.getLong(index);
                    /* Get human readable string using our utility method */
                //TODO(5)
//                    String dateString = SunshineDateUtils.newDateMethod(dateInMillis);
                String dateString = SunshineDateUtils.getFriendlyDateString(mContext, dateInSeconds, true);
                currentViewHolder.dateView.setText(dateString);

                /*Setting resource icon*/
                index = mCursor.getColumnIndex(MainActivity.MAIN_FORECAST_PROJECTION[2]);
                currentViewHolder.iconView.setImageResource(SunshineWeatherUtils.getIconResourceForWeatherCondition(
                        mCursor.getInt(index)
                ));

                /*Set description*/
                index = mCursor.getColumnIndex(MainActivity.MAIN_FORECAST_PROJECTION[3]);
                currentViewHolder.descriptionView.setText(mCursor.getString(index));
                //setting the background image depending on current weather:
//TODO:                SunshineWeatherUtils.setBackgroundImage(mContext,0);

                if( weekCursor == null || !weekCursor.moveToFirst() ){
                    break;
                }

                //TODO: it is setting temperature to be city's current minimum temperature...change this:
                /*setting CURRENT TEMPERATURE*/
                index = mCursor.getColumnIndex(MainActivity.MAIN_FORECAST_PROJECTION[6]);
                String currentTemp = SunshineWeatherUtils.formatTemperature(mContext,mCursor.getDouble(index));
                currentViewHolder.currentTempView.setText(currentTemp);

                //TODO: set correct position:
                weekCursor.moveToPosition(0);

                /*setting MIN TEMPERATURE*/
                index = weekCursor.getColumnIndex(MainActivity.WEEK_FORECAST_PROJECTION[4]);
                if(currentViewMinTemp==null){
                    currentViewMinTemp = SunshineWeatherUtils.formatTemperature(mContext,weekCursor.getDouble(index));
                }
                currentViewHolder.minTempView.setText(currentViewMinTemp);

                /*setting MAX TEMPERATURE*/
                index = weekCursor.getColumnIndex(MainActivity.WEEK_FORECAST_PROJECTION[5]);
                if(currentViewMaxTemp==null){
                    currentViewMaxTemp = SunshineWeatherUtils.formatTemperature(mContext,weekCursor.getDouble(index));
                }
                currentViewHolder.maxTempView.setText(currentViewMaxTemp);

                mActivity.setRecyclerViewPadding();
                break;

            case VIEW_TYPE_TODAY_HOURS:

                HoursViewHolder hoursViewHolder = (HoursViewHolder) holder;

                mCursor.moveToPosition(position - 1);

                /* Read DATE */
                index = mCursor.getColumnIndex(MainActivity.MAIN_FORECAST_PROJECTION[1]);
                dateInSeconds = mCursor.getLong(index);
                    /* Get human readable string using our utility method */
                //TODO(5)
//                    String dateString = SunshineDateUtils.newDateMethod(dateInMillis);
                timeString = SunshineDateUtils.getReadableTime(dateInSeconds);


                hoursViewHolder.timeView.setText(timeString);

                /*Setting resource icon*/
                index = mCursor.getColumnIndex(MainActivity.MAIN_FORECAST_PROJECTION[2]);
                hoursViewHolder.iconView.setImageResource(SunshineWeatherUtils.getIconResourceForWeatherCondition(
                        mCursor.getInt(index)
                ));

                /*Set description*/
                index = mCursor.getColumnIndex(MainActivity.MAIN_FORECAST_PROJECTION[3]);
                hoursViewHolder.descriptionView.setText(mCursor.getString(index));

                /*setting TEMPERATURE*/
                index = mCursor.getColumnIndex(MainActivity.MAIN_FORECAST_PROJECTION[4]);
                String temp = SunshineWeatherUtils.formatTemperature(mContext,mCursor.getDouble(index));
                hoursViewHolder.tempView.setText(temp);
                break;
        }
    }

    @Override
    public int getItemCount() {
        if ( mCursor != null && mCursor.moveToFirst() ) {
            mCursor.moveToFirst();
        /*
        We must add 1 because of the extra weekRecyclerView item that is to be included
         */
            return 1 + mCursor.getCount();
        }
        else if(mCursor==null){
        }
        else if(mCursor.getCount()==0){
        }
        else if (!mCursor.moveToFirst()){

            //toast for user:

            Bundle bundle = new Bundle();

            long nextDateSeconds = SunshineDateUtils.getLocalNextDateSeconds();
            String selection = WeatherContract.WeatherEntry.getSqlSelectForDate(nextDateSeconds);
            bundle.putString(SELECTION_FOR_DATE, selection);
            mActivity.getSupportLoaderManager().restartLoader(TODAY_CURSOR_LOADER,bundle,mActivity);
        }
        return 0;
    }

    public void swapCursor(Cursor newCursor){
        mCursor = newCursor;
        notifyDataSetChanged();
    }

    public void swapWeekCursor(Cursor newCursor){
        weekCursor = newCursor;
        weekAdapter.swapCursor(weekCursor, this);
        notifyDataSetChanged();
        //TODO:
//        setBackgroundImage(newCursor, 0);
    }

//    void setBackgroundImage(Cursor cursor, int position) {
//        if( cursor == null || !cursor.moveToFirst() ){
//            Log.i(TAG,"Either cursor is null or number of rows = 0");
//            return;
//        }
//        cursor.moveToPosition(position);
//        realImage.setBackgroundResource(SunshineWeatherUtils.getBackgroundImageResource(cursor.getInt(
//                2
//        )));
//        createAndSetBlurredImage();
//    }
//
//    public void createAndSetBlurredImage(){
//        BitmapDrawable background = (BitmapDrawable) realImage.getBackground();
//        Bitmap bitmapBlurred = Blur.fastblur(mContext, background.getBitmap(), 12);
//        Drawable drawableBlurred = new BitmapDrawable(mContext.getResources(), bitmapBlurred);
//        blurredImage.setBackground(drawableBlurred);
//    }

    public class ForecastViewHolder extends RecyclerView.ViewHolder
        //TODO: change the name and make this view empty
        implements View.OnClickListener {

        private final TextView descriptionView;
        private final ImageView iconView;

        private final TextView dateView;
        private final TextView maxTempView;
        private final TextView minTempView;

//        TextView emptyView;

        public ForecastViewHolder(View itemView) {
            super(itemView);
            dateView = itemView.findViewById(R.id.date);
            maxTempView = itemView.findViewById(R.id.max);
            minTempView = itemView.findViewById(R.id.min);
            descriptionView = itemView.findViewById(R.id.description);
            iconView = itemView.findViewById(R.id.icon);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            Intent i = new Intent(mContext,DetailActivity.class);

            int adapterPosition = getAdapterPosition();
            mCursor.moveToPosition(adapterPosition);
            long dateInMillis = mCursor.getLong(mCursor.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_DATE));

            Uri uriForDateClicked = WeatherContract.WeatherEntry.buildWeatherUriWithDate(dateInMillis);
            i.setData(uriForDateClicked);

            //for transition:
            Bundle bundle = ActivityOptions.makeSceneTransitionAnimation((MainActivity)mContext).toBundle();
            mContext.startActivity(i, bundle);
        }
    }

    public class HoursViewHolder extends RecyclerView.ViewHolder
        implements View.OnClickListener{

        private final TextView timeView;
        private final TextView tempView;
        private final TextView descriptionView;
        private final ImageView iconView;

        public HoursViewHolder(View itemView) {
            super(itemView);
            timeView = itemView.findViewById(R.id.time);
            descriptionView = itemView.findViewById(R.id.description);
            iconView = itemView.findViewById(R.id.icon);
            tempView = itemView.findViewById(R.id.temp);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            Intent i = new Intent(mContext,DetailActivity.class);

            int adapterPosition = getAdapterPosition();
            //+1 because there is a weekRecyclerView in between
            mCursor.moveToPosition(adapterPosition - 1);
            long dateInMillis = mCursor.getLong(mCursor.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_DATE));
            Uri uriForDateClicked = WeatherContract.WeatherEntry.buildWeatherUriWithDate(dateInMillis);
            i.setData(uriForDateClicked);
            mContext.startActivity(i);
        }
    }

    public class CurrentViewHolder extends RecyclerView.ViewHolder
        implements View.OnClickListener{

        private final TextView descriptionView;
        private final ImageView iconView;

        private final TextView dateView;
        private final TextView timeView;
        final TextView maxTempView;
        final TextView minTempView;
        private final TextView currentTempView;

//        TextView emptyView;

        public CurrentViewHolder(View itemView) {
            super(itemView);
            dateView = itemView.findViewById(R.id.date);
            timeView = itemView.findViewById(R.id.time);
            maxTempView = itemView.findViewById(R.id.maxTemp);
            minTempView = itemView.findViewById(R.id.minTemp);
            currentTempView = itemView.findViewById(R.id.currentTemp);
            descriptionView = itemView.findViewById(R.id.description);
            iconView = itemView.findViewById(R.id.icon);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            Intent i = new Intent(mContext,DetailActivity.class);

            int adapterPosition = getAdapterPosition();
            mCursor.moveToPosition(adapterPosition);
            long dateInMillis = mCursor.getLong(mCursor.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_DATE));

            Uri uriForDateClicked = WeatherContract.WeatherEntry.buildWeatherUriWithDate(dateInMillis);
            i.setData(uriForDateClicked);
            mContext.startActivity(i);
        }
    }
}