package com.weather.wallpaper.forecast;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.databinding.DataBindingUtil;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.MobileAds;
import com.weather.wallpaper.forecast.data.WeatherContract;
import com.weather.wallpaper.forecast.databinding.ActivityMainBinding;
import com.weather.wallpaper.forecast.sync.WeatherSyncUtils;
import com.weather.wallpaper.forecast.utilities.ImageUtils;
import com.weather.wallpaper.forecast.utilities.LocationUtils;
import com.weather.wallpaper.forecast.utilities.NetworkUtils;
import com.weather.wallpaper.forecast.utilities.SunshineDateUtils;
import com.weather.wallpaper.forecast.utilities.WallpaperUtils;

import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements
        SharedPreferences.OnSharedPreferenceChangeListener,
        LoaderManager.LoaderCallbacks<Cursor> {

    /*
    DATABASE related constants
     */
    static final String SELECTION_FOR_DATE = "selection statement for desired date";
    static final int TODAY_CURSOR_LOADER = 0;
    private static final int WEEK_LOADER_ID = 1;
    public static final String[] WEEK_FORECAST_PROJECTION = {
            WeatherContract.WeekDayEntry._ID,
            WeatherContract.WeekDayEntry.COLUMN_DATE,
            WeatherContract.WeekDayEntry.COLUMN_WEATHER_ID,
            WeatherContract.WeekDayEntry.COLUMN_WEATHER_TYPE,
            WeatherContract.WeekDayEntry.COLUMN_MIN_TEMP,
            WeatherContract.WeekDayEntry.COLUMN_MAX_TEMP,
    };
    public static final String[] MAIN_FORECAST_PROJECTION = {
            WeatherContract.WeatherEntry._ID,
            WeatherContract.WeatherEntry.COLUMN_DATE,
            WeatherContract.WeatherEntry.COLUMN_WEATHER_ID,
            WeatherContract.WeatherEntry.COLUMN_WEATHER_TYPE,
            WeatherContract.WeatherEntry.COLUMN_MIN_TEMP,
            WeatherContract.WeatherEntry.COLUMN_MAX_TEMP,
            WeatherContract.WeatherEntry.COLUMN_CURRENT_TEMP
    };
    private static final String TAG = MainActivity.class.getName();
    static final String BUY_APP_DIALOG_TAG = "buy";

    /*
    RECYCLER VIEW Variables
     */
    RecyclerView todayRecyclerView;
    ForecastAdapter todayAdapter;
    public LinearLayoutManager todayLayoutManager;
    int mPosition = RecyclerView.NO_POSITION;

    /*
    DISPLAY related variables
     */
    private int screenHeight,padding;
    private int screenWidth;
    private int firstViewHeight;
    private ImageView blurredImage;
    public ActivityMainBinding mainBinding;

    private SharedPreferences sharedPref;
    private boolean isSelectionStatementPresent;

    private Toolbar toolbar;
    private ActionBarDrawerToggle mDrawerToggle;

    private float latitude,longitude;

    private boolean triedForNextDate=false;

    int offset=0, extent, range;
    float factor, percentage, blurPercentage;

    private static InterstitialAd mInterstitialAd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mainBinding = DataBindingUtil.setContentView(this, R.layout.activity_main);

        //displaying add
        MobileAds.initialize(this, getString(R.string.main_admob_banner_id));
        AdRequest adRequest = new AdRequest.Builder().build();
        mainBinding.adView.loadAd(adRequest);
        mInterstitialAd = new InterstitialAd(this);
        mInterstitialAd.setAdUnitId(getString(R.string.main_admob_interstitial_id));
        mInterstitialAd.loadAd(new AdRequest.Builder().build());
        mInterstitialAd.setAdListener(new AdListener() {
            @Override
            public void onAdClosed() {
                // Load the next interstitial.
                mInterstitialAd.loadAd(new AdRequest.Builder().build());
            }

        });

        NetworkUtils.updateAndroidSecurityProvider(this);

        if(initialize()) return;

        Intent i = getIntent();
        if(i!=null){
            String splash = i.getStringExtra("Activity");
            if(splash!=null && splash.equals("Splash")){
                // This solution will leak memory!  Don't use!!!
                FragmentManager fragmentManager = getSupportFragmentManager();
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                final SplashFragment fragment = new SplashFragment();
                fragmentTransaction.add(R.id.splash_fragment_container, fragment);
                fragmentTransaction.commit();
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        getSupportFragmentManager().beginTransaction().setCustomAnimations(0,R.anim.fade_out).remove(fragment).commit();
                    }
                }, 1000);
            };
        }

        /*
        initializing data binding object
         */

        //finding the views
        todayRecyclerView = mainBinding.todayRecyclerView;
        todayRecyclerView.setItemViewCacheSize(0);
        todayAdapter = new ForecastAdapter(this, MainActivity.this, todayRecyclerView);
        todayRecyclerView.setAdapter(todayAdapter);
        todayLayoutManager = new LinearLayoutManager(this,LinearLayoutManager.VERTICAL,false);
        todayRecyclerView.setLayoutManager(todayLayoutManager);

        /*TODO: find out why using this
         * Use this setting to improve performance if you know that changes in content do not
         * change the child layout size in the RecyclerView
         */
//        todayRecyclerView.setHasFixedSize(false);

        setDisplayMetrics();

        configureToolbar();
        configureDrawer();
        configureSwipeToRefresh();

//        final View view = findViewById(R.id.color_shade);
        blurredImage = findViewById(R.id.blurredImage);
        //setting recyclerview onScrollListener:
        todayRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                View v = recyclerView.getChildAt(0);
                if(v!=null) offset = -(v.getTop() - padding);
                extent = recyclerView.computeVerticalScrollExtent();
                range = 4*extent;

                //setting factor manually:
                factor = 0.5f;

                percentage = (float) (offset / (float)(padding));
                if(percentage>1) percentage=1;
                blurPercentage = (float) (offset / (float)(firstViewHeight));
                if(blurPercentage>1) blurPercentage=1;


                blurredImage.setAlpha(blurPercentage);
//                recyclerView.getBackground().setAlpha((int) (percentage*255*factor));
//                view.getBackground().setAlpha((int) (percentage*255*factor));
                toolbar.getBackground().setAlpha((int) (percentage*255*factor));
                //toolbar alpha setting will come here
            }
        });

        //registering OnSharedPreferenceChangeListener
        sharedPref.registerOnSharedPreferenceChangeListener(this);

        //TODO: first check here if there is network connection
        //TODO: display error message if no network connectivity
        //TODO: display loading indicator if network connection available
        getSupportLoaderManager().initLoader(TODAY_CURSOR_LOADER, null, this).forceLoad();
        getSupportLoaderManager().initLoader(WEEK_LOADER_ID, null, this).forceLoad();
    }

    /**
     * @return true if intent for Settings activated else false
     */
    private boolean initialize() {
        /*
        Checking if we have location co-ordinates
         */
        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        latitude = sharedPref.getFloat(getString(R.string.latitude_key),0);
        longitude = sharedPref.getFloat(getString(R.string.longitude_key),0);
        if(latitude==0 && longitude==0){
            /*
            Sending user to SettingsActivity where they will be prompted by a dialog
             */
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            finish();
            return true;
        }


        if(!sharedPref.getBoolean(getString(R.string.is_initialized_key),false)){
            WeatherSyncUtils.initialize(this);
            mainBinding.progressBar.setVisibility(View.VISIBLE);
        }
        return false;
    }

    private void checkConnectivityForError(Cursor data) {
        //TODO: display loading indicator if network connection available

        if(!NetworkUtils.checkNetworkConnection(this)) {
            mainBinding.swipeRefresh.setVisibility(View.GONE);
            mainBinding.refreshWhenErrorButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    initialize();
                }
            });
            return;
        }
        else if(data==null || data.getCount()==0){
            return;
        }
        else mainBinding.swipeRefresh.setVisibility(View.VISIBLE);
    }

    private void configureSwipeToRefresh() {
        final SwipeRefreshLayout mySwipeRefresh = findViewById(R.id.swipe_refresh);
        mySwipeRefresh.setOnRefreshListener(
                new SwipeRefreshLayout.OnRefreshListener() {
                    @Override
                    public void onRefresh() {
                        long sinceLastSync = SunshineDateUtils.timeSinceLastUpdated(MainActivity.this);
                        if(sinceLastSync <= 1800){
                            Toast.makeText(MainActivity.this,"Weather Data Up To Date ",Toast.LENGTH_SHORT).show();
                            mySwipeRefresh.setRefreshing(false);
                            return;
                        }
                        WeatherSyncUtils.startImmediateSync(MainActivity.this);
                        mySwipeRefresh.setRefreshing(false);
                    }
                }
        );
    }

    private void configureDrawer() {
        DrawerLayout mDrawerLayout = findViewById(R.id.drawer_layout);
        final RelativeLayout mainContent = findViewById(R.id.main_content);
        final RelativeLayout mDrawerList = findViewById(R.id.left_drawer);
        mDrawerToggle = new ActionBarDrawerToggle(
                this,
                mDrawerLayout,
                toolbar,
                R.string.open_drawer,
                R.string.closeDrawer
        ){
            @Override
            public void onDrawerSlide(View drawerView, float slideOffset)
            {
                //TODO: this probably works only on honeycomb or later... make sure to update when you lower minSdk
                //https://stackoverflow.com/questions/20057084/how-to-move-main-content-with-drawer-layout-left-side
                super.onDrawerSlide(drawerView, slideOffset);
                float moveFactor = (mDrawerList.getWidth() * slideOffset);

                mainContent.setTranslationX(moveFactor);
            }


        };
        mDrawerLayout.addDrawerListener(mDrawerToggle);

        //SHARE ACTION:
        mainBinding.actionShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.app_share_message));
                sendIntent.setType("text/plain");
                startActivity(Intent.createChooser(sendIntent, "Share"));
            }
        });

        //SUGGEST ACTION:
        mainBinding.actionSuggest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_SENDTO);
                String email = getResources().getString(R.string.app_email_id);
                intent.setData(Uri.parse("mailto:" + email)); // only email apps should handle this
                intent.putExtra(Intent.EXTRA_SUBJECT, "Feedback");
                intent.putExtra(Intent.EXTRA_TEXT, "Your feedback");
                if (intent.resolveActivity(getPackageManager()) != null) {
                    startActivity(intent);
                }
            }
        });

        //SET IMAGE AS WALLPAPER:
        mainBinding.actionSetWallpaper.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                WallpaperUtils.setDisplayedImageAsWallpaper(MainActivity.this,mainBinding.realImage);
            }
        });

        //HARSH WEATHER ALERTS ENABLED?
        mainBinding.alertEnabled.setChecked(sharedPref.getBoolean(getString(R.string.pref_are_alerts_enabled_key),true));
        mainBinding.alertEnabled.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if(isChecked){
                    //if notifications aren't enabled... you can't turn harsh weather alerts on
                    if(!sharedPref.getBoolean(getString(R.string.pref_notifications_enabled_key),true)) {
                        mainBinding.alertEnabled.setChecked(false);
                        Toast.makeText(MainActivity.this,"turn on notifications first",Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
                sharedPref.edit().putBoolean(getString(R.string.pref_are_alerts_enabled_key),isChecked).apply();
            }
        });

        //LIVE WALLPAPER ENABLED?
        mainBinding.wallpaperEnabled.setChecked(sharedPref.getBoolean(getString(R.string.pref_live_wallpaper_enabled_key),true));
        mainBinding.wallpaperEnabled.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                sharedPref.edit().putBoolean(getString(R.string.pref_live_wallpaper_enabled_key),isChecked).apply();
            }
        });

        //ABOUT:
        mainBinding.actionAbout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent toAbout = new Intent(MainActivity.this,About.class);
                startActivity(toAbout);
            }
        });

        mainBinding.actionChangeLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(MainActivity.this, SettingsActivity.class);
                i.putExtra(SettingsActivity.LOCATION_CHANGE_INTENT,true);
                startActivity(i);
            }
        });

        mainBinding.actionBuyApp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showBuyDialog(MainActivity.this);
            }
        });
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mDrawerToggle.syncState();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //return true for ActionBarToggle to handle the touch event
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void configureToolbar() {
        /*
        setting up toolbar
        //TODO: find out why action bar doesn't work if kept just at the start of onCreate()
         */
        toolbar = findViewById(R.id.tool_bar);
        toolbar.setBackgroundColor(getResources().getColor(R.color.toolbar));
        toolbar.getBackground().setAlpha((int) (percentage*255*factor));
        setSupportActionBar(toolbar);

        TextView toolbarLocation = findViewById(R.id.toolbarLocation);
        String location = sharedPref.getString(getString(R.string.pref_location_name_key), null);
        if(location==null){
            location = LocationUtils.getPlaceName(this,latitude,longitude);
        }
        if(location==null){
            toolbarLocation.setText("No Name... Check connection");
        } else toolbarLocation.setText(location);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        getSupportActionBar().setHomeButtonEnabled(true);
        //TODO: apparently you can't remove title from toolbar using xml
    }

    public void tryForNextDate() {
        long halfDay = TimeUnit.DAYS.toSeconds(1) / 2;
        long nextDateInSeconds = (System.currentTimeMillis() / 1000) + halfDay;
        String selection = WeatherContract.WeatherEntry.getSqlSelectForDate(nextDateInSeconds);

        Bundle args = new Bundle();
        args.putString(SELECTION_FOR_DATE, selection);

        LoaderManager mLoaderManager = getSupportLoaderManager();
        Loader<Cursor> loader = mLoaderManager.getLoader(TODAY_CURSOR_LOADER);
        mLoaderManager.restartLoader(TODAY_CURSOR_LOADER, args, this).forceLoad();
        triedForNextDate=true;
    }

    public void setDisplayMetrics(){
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        screenHeight = displayMetrics.heightPixels;
        screenWidth = displayMetrics.widthPixels;
        sharedPref.edit().putInt(getString(R.string.screen_height),screenHeight).apply();
        sharedPref.edit().putInt(getString(R.string.screen_width),screenWidth).apply();
    }

    public void setRecyclerViewPadding(){

        if(sharedPref.getInt(getString(R.string.current_weather_view_height),0)==0){

            final ViewTreeObserver vto = todayRecyclerView.getViewTreeObserver();
            vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {

                @Override
                public void onGlobalLayout() {
                    View view = todayRecyclerView.getChildAt(0);

                    ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) view.getLayoutParams();

                    firstViewHeight = view.getMeasuredHeight() + lp.bottomMargin;

                    vto.removeOnGlobalLayoutListener(this);
                }
            });

            sharedPref.edit().putInt(getString(R.string.current_weather_view_height),firstViewHeight).apply();
        }

        firstViewHeight = sharedPref.getInt(getString(R.string.current_weather_view_height),0);

        padding =  screenHeight - firstViewHeight - (int) getResources().getDimension(R.dimen.banner_height) - toolbar.getHeight();

        todayRecyclerView.setPadding(0,padding,0,(int) getResources().getDimension(R.dimen.banner_height));
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {

        mainBinding.progressBar.setVisibility(View.VISIBLE);

        switch (id){
            case TODAY_CURSOR_LOADER:
                Uri forecastUri = WeatherContract.WeatherEntry.CONTENT_URI;

                /* Sort order: Ascending by date */
                String sortOrder = WeatherContract.WeatherEntry.COLUMN_DATE + " ASC";

                /*
                 * A SELECTION in SQL declares which rows you'd like to return. In our case, we
                 * want all weather data from today onwards that is stored in our weather table.
                 * We created a handy method to do that in our WeatherEntry class.
                 */

                String selection;
                if(args==null){
                    isSelectionStatementPresent=false;
                    /*
                    TODO: also check if there is any weather info present for today
                    TODO: this scenario arises for night time when all the new data is available only for the next day
                     */
                    selection = WeatherContract.WeatherEntry.getSqlSelectFromNowToday();
                }
                else{
                    isSelectionStatementPresent=true;
                    selection = args.getString(SELECTION_FOR_DATE,"");
                }

                //TODO: change the null,null,null to something useful
                return new CursorLoader(this,
                        forecastUri,
                        MAIN_FORECAST_PROJECTION,
                        selection,
                        null,
                        null);

            case WEEK_LOADER_ID:
                forecastUri = WeatherContract.WeekDayEntry.CONTENT_URI;
                sortOrder = WeatherContract.WeekDayEntry.COLUMN_DATE + " ASC";
                String weekSelection = WeatherContract.WeekDayEntry.getSqlSelectForTodayOnwards();

                return new CursorLoader(this,
                        forecastUri,
                        WEEK_FORECAST_PROJECTION,
                        weekSelection,
                        null,
                        sortOrder);

            default:
                throw new RuntimeException("Loader Not Implemented: " + id);
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {

        switch (loader.getId()){
            case TODAY_CURSOR_LOADER:
                /*
                if the cursor returned is null, then this means that
                1 > either I need to refresh,
                2 > OR THAT THERE IS NO DATA AVAILABLE FOR TODAY, EVEN FOR THE API
                The first use case is already taken care of.... so I need to take care about only the seconds use case
                 */
                if(data==null || data.getCount()==0){
                    //reload cursor for next date
                    checkConnectivityForError(data);
                    if(!triedForNextDate){
                        tryForNextDate();
                    }
                }
                else{
                    mainBinding.swipeRefresh.setVisibility(View.VISIBLE);
                    mainBinding.emptyView.setVisibility(View.GONE);
                }
                configureToolbar();
                todayAdapter.swapCursor(data);
                break;

            case WEEK_LOADER_ID:
                if(data==null){
                    break;
                }
                todayAdapter.swapWeekCursor(data);
                ImageUtils.setRealImage(
                            this,
                            (ImageView) findViewById(R.id.realImage),
                            (ImageView) findViewById(R.id.blurredImage),
                            0
                    );
                break;
        }

        //changing the cursor
        //TODO: what's this??
        if (mPosition == RecyclerView.NO_POSITION) mPosition = 0;
        //TODO(2): temporarily commenting out below line
        if(!isSelectionStatementPresent){
            //boolean is false means that first time loading in this session
            todayRecyclerView.smoothScrollToPosition(0);
        }
        else {
            //TODO: I need to make the scrollBar to SWIFTLY jump to the top when a new day is clicked
        }

//        todayRecyclerView.smoothScrollToPosition(mPosition);

        mainBinding.progressBar.setVisibility(View.INVISIBLE);

    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        //reset recyclerview adapter here

        switch (loader.getId()){
            case TODAY_CURSOR_LOADER:
                todayAdapter.swapCursor(null);
                break;

            case WEEK_LOADER_ID:
                todayAdapter.swapWeekCursor(null);
                break;
        }
        //TODO: Find out why there's no need to restart the loader?.... probably because you already called notifyDataSetChanged()
        //TODO: What does notifyDataSetChanged() does?
        // reset the loader
    }

    //TODO: Remove the onDestroy override because we removed the SharedPreferences implementation
    @Override
    protected void onDestroy() {
        super.onDestroy();
        sharedPref.unregisterOnSharedPreferenceChangeListener(this);
    }

    //TODO: Find out why we need to remove the OnSharedPreferenceChangedListener implementation
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(getString(R.string.pref_location_name_key))) {
            WeatherSyncUtils.startImmediateSync(this);
        }
        getSupportLoaderManager().restartLoader(TODAY_CURSOR_LOADER,null,this);
        getSupportLoaderManager().restartLoader(WEEK_LOADER_ID,null,this);
    }

    public void openSettings(View view) {
        Intent toSettings = new Intent(this,SettingsActivity.class);
        startActivity(toSettings);
    }

    public static class BuyAppDialog extends DialogFragment {

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.buy_app_prompt)
                    .setCancelable(false)
                    .setNegativeButton(R.string.buy_app_no, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dismiss();
                            if(mInterstitialAd.isLoaded()){
                                mInterstitialAd.show();
                            }
                            else{
                                mInterstitialAd.setAdListener(new AdListener(){
                                    public void onAdLoaded(){
                                        mInterstitialAd.show();
                                    }
                                });
                            }
                        }
                    })
                    .setPositiveButton(R.string.buy_app_yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            buyApp(getActivity());
                        }
                    })
                    .setMessage(R.string.buy_app_message);

            return builder.create();
        }

        private static void buyApp(Context context){
            Toast.makeText(context, "Redirect user to paid app", Toast.LENGTH_SHORT).show();
        }
    }

    private void showBuyDialog(Context context){
        DialogFragment buyAppDialog = new BuyAppDialog();
        buyAppDialog.setCancelable(false);
        buyAppDialog.show(getFragmentManager(), BUY_APP_DIALOG_TAG);
    }

}