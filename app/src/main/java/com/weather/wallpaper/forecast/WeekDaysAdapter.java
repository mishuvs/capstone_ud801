package com.weather.wallpaper.forecast;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.weather.wallpaper.forecast.data.WeatherContract;
import com.weather.wallpaper.forecast.utilities.ImageUtils;
import com.weather.wallpaper.forecast.utilities.SunshineDateUtils;
import com.weather.wallpaper.forecast.utilities.SunshineWeatherUtils;

import java.util.List;

import static com.weather.wallpaper.forecast.MainActivity.SELECTION_FOR_DATE;
import static com.weather.wallpaper.forecast.MainActivity.TODAY_CURSOR_LOADER;

/**
 * Created by Vaibhav on 8/26/2017.
 */

public class WeekDaysAdapter extends RecyclerView.Adapter<WeekDaysAdapter.WeekDayViewHolder>{

    private static final String TAG = WeekDaysAdapter.class.getName();
    private final int VIEW_TYPE_TODAY = 0;
    private final int VIEW_TYPE_FUTURE_DAY = 1;

    private Cursor mCursor;
    private Context mContext;
    private MainActivity mainActivity;

    private ImageView realImageView, blurredImageView;

    private ForecastAdapter forecastAdapter;

    public List<Integer> INDEXES;

    private int selectedDay=0;

    public WeekDaysAdapter(Context context){
        mContext = context;
        mainActivity = (MainActivity) mContext;
        realImageView = (ImageView) mainActivity.findViewById(R.id.realImage);
        blurredImageView = (ImageView) mainActivity.findViewById(R.id.blurredImage);
    }

    @Override
    public WeekDayViewHolder onCreateViewHolder(ViewGroup parent, int viewType){
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.week_item, parent, false);
        return new WeekDayViewHolder(view);
//            //setting location name inside the first view
//            TextView location_text_view = (TextView) view.findViewById(R.id.location_tv);
//            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(mContext);
//            String locationName = sharedPref.getString(mContext.getString(R.string.pref_location_name_key),"No name");
//            location_text_view.setText(locationName);

    }

    @Override
    public int getItemViewType(int position) {
        if(position==0) return VIEW_TYPE_TODAY;
        else return VIEW_TYPE_FUTURE_DAY;
    }

    @Override
    public void onBindViewHolder(WeekDayViewHolder holder, int position) {
        mCursor.moveToPosition(position);

        int index;
        long dateInMillis;

        /*
        set the following
            1. day
            2. date
            3. icon
            4. max temp
            5. min temp
         */

//        index = mCursor.getColumnIndex(WeatherContract.WeekDayEntry.COLUMN_CURRENT_TEMP);

        //DAY & DATE:
        index = mCursor.getColumnIndex(WeatherContract.WeekDayEntry.COLUMN_DATE);
        /* Read date from the cursor */
        dateInMillis = mCursor.getLong(index);
                    /* Get human readable string using our utility method */
        //TODO(5)
//                    String dateString = SunshineDateUtils.newDateMethod(dateInMillis);

        //TODO: set today's day and date in the minimalist form
        String dateString = SunshineDateUtils.getMinimalDate(dateInMillis*1000);
        holder.dateView.setText(dateString);
        String dayString = SunshineDateUtils.getDayName(mContext, dateInMillis*1000);
        holder.dayView.setText(dayString);

        //ICON
        index = mCursor.getColumnIndex(WeatherContract.WeekDayEntry.COLUMN_WEATHER_ID);
        holder.iconView.setImageResource(SunshineWeatherUtils.getIconResourceForWeatherCondition(
                            mCursor.getInt(index)
                    ));

        //MIN TEMP
        index = mCursor.getColumnIndex(WeatherContract.WeekDayEntry.COLUMN_MIN_TEMP);
        String minTemp = SunshineWeatherUtils.formatTemperature(mContext,mCursor.getDouble(index));
        holder.minTempView.setText(minTemp);

        //MAX TEMP
        index = mCursor.getColumnIndex(WeatherContract.WeekDayEntry.COLUMN_MAX_TEMP);
        String maxTemp = SunshineWeatherUtils.formatTemperature(mContext,mCursor.getDouble(index));
        holder.maxTempView.setText(maxTemp);

        index = mCursor.getColumnIndex(WeatherContract.WeekDayEntry.COLUMN_WEATHER_TYPE);
        String description = mCursor.getString(index);
//        mainActivity.loadWeatherImage(description);

        holder.itemView.setSelected(position==selectedDay);
    }

    @Override
    public int getItemCount() {
        if (null == mCursor) return 0;
        mCursor.moveToFirst();
        return mCursor.getCount();
    }

    public void swapCursor(Cursor newCursor, ForecastAdapter fAdapter){
        mCursor = newCursor;
        forecastAdapter = fAdapter;
        notifyDataSetChanged();
    }

    /*
    WeekDayViewHolder INNER CLASS FOR VIEW HOLDER
     */
    public class WeekDayViewHolder extends RecyclerView.ViewHolder
            implements View.OnClickListener {

        private final TextView dateView;
        private final TextView maxTempView;
        private final TextView minTempView;
        private final TextView dayView;
        private final ImageView iconView;

        public WeekDayViewHolder(View itemView) {
            super(itemView);
            dateView = (TextView) itemView.findViewById(R.id.date);
            maxTempView = (TextView) itemView.findViewById(R.id.max);
            minTempView = (TextView) itemView.findViewById(R.id.min);
            dayView = (TextView) itemView.findViewById(R.id.day);
            iconView = (ImageView) itemView.findViewById(R.id.icon);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            //TODO: find out how to persist recyclerview position for scroll... it gets to the top which is very unpleasant

            int adapterPosition = getAdapterPosition();

            if(adapterPosition>4){
                //TODO: the correct method would be to check for weather availability for that date on Async thread
                Toast.makeText(mContext,mContext.getString(R.string.full_weather_not_available), Toast.LENGTH_SHORT).show();
                return;
            }

            notifyDataSetChanged();
            selectedDay = adapterPosition;
            v.setSelected(true);

            mCursor.moveToPosition(adapterPosition);
            long date = mCursor.getLong(mCursor.getColumnIndex(WeatherContract.WeekDayEntry.COLUMN_DATE));
            String selection = WeatherContract.WeatherEntry.getSqlSelectForDate(date);
            String weatherDescriptionForImage = mCursor.getString(
                    mCursor.getColumnIndex(WeatherContract.WeekDayEntry.COLUMN_WEATHER_TYPE)
            );

            Bundle args = new Bundle();
            args.putString(SELECTION_FOR_DATE, selection);
            mainActivity.getSupportLoaderManager().restartLoader(TODAY_CURSOR_LOADER, args, mainActivity);
            mainActivity.todayAdapter.currentViewMaxTemp = this.maxTempView.getText().toString();
            mainActivity.todayAdapter.currentViewMinTemp = this.minTempView.getText().toString();
            ImageUtils.unsetBlurredImage(blurredImageView);
            ImageUtils.setRealImage(mContext,realImageView,blurredImageView,adapterPosition);
        }

    }

}
