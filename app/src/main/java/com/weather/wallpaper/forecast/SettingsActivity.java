package com.weather.wallpaper.forecast;

import android.Manifest;
import android.app.Activity;
import android.app.DialogFragment;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.CheckBoxPreference;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.preference.PreferenceScreen;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.weather.wallpaper.forecast.location.GeoFencingTransitionsIntentService;
import com.weather.wallpaper.forecast.location.GeofenceErrorMessages;
import com.weather.wallpaper.forecast.location.LocationPromptDialog;
import com.weather.wallpaper.forecast.utilities.LocationUtils;

public class SettingsActivity extends AppCompatActivity implements
        LocationPromptDialog.LocationPromptDialogListener, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener, ResultCallback<Status> {

    private static final int MY_PERMISSIONS_FINE_LOCATION = 0;
    private static final float GEOFENCE_RADIUS_IN_METERS = 500;
    private static final String TAG = SettingsActivity.class.getName();
    static final String LOCATION_CHANGE_INTENT = "change location";
    SharedPreferences sharedPref;
    public static final String DIALOG_TAG = "Location Dialog";
    private static boolean GPS_ALLOWED = false;

    private String USER_LOCATION_REQUEST_ID = "1919283";

    private GoogleApiClient mGoogleApiClient;
    private Location mLastLocation;

    public static LocationPromptDialog dialog;

    private static ProgressBar progressBar;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        Toolbar toolbar = findViewById(R.id.tool_bar);
        toolbar.setBackgroundColor(getResources().getColor(R.color.toolbar));
        setSupportActionBar(toolbar);
        if(getSupportActionBar()!=null){
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
            getSupportActionBar().setTitle("Settings");
        }

        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        float latitude = sharedPref.getFloat(getString(R.string.latitude_key), 0);
        float longitude = sharedPref.getFloat(getString(R.string.longitude_key), 0);

        Intent i = getIntent();
        Boolean change_location = i.getBooleanExtra(LOCATION_CHANGE_INTENT,false);

        if (latitude == 0 && longitude == 0 || change_location) {
            /*
            Creating dialog because no lat-lng available
             */
            createLocationPromptDialog();
        }
    }

    public void createLocationPromptDialog(){
        dialog = new LocationPromptDialog();
        dialog.show(getFragmentManager(), DIALOG_TAG);
    }

    @Override
    public void onGpsButtonClick(DialogFragment dialog) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionToUseGPS();
            return;
        }
        useGps();
        //TODO: Do some change in the UI to let the user know that location is set successfully
    }

    public void useGps(){

        dialog.dismiss();

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addOnConnectionFailedListener(this)
                .addConnectionCallbacks(this)
                .addApi(LocationServices.API)
                .build();

        mGoogleApiClient.connect();
    }

    public void requestPermissionToUseGPS() {

        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    MY_PERMISSIONS_FINE_LOCATION);

        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.

                    //dismissing the location prompt dialog
                    useGps();

                } else {
                    //TODO: do something about it
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
            }
        }

    }

    public Geofence getGeofence(double latitude, double longitude, float radius) {
        return new Geofence.Builder()
                //setting the request id... which is a string
                .setRequestId(USER_LOCATION_REQUEST_ID)

                //set the circular area
                .setCircularRegion(latitude, longitude, radius)

                .setExpirationDuration(Geofence.NEVER_EXPIRE)

                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT)

                .build();
    }


    @Override
    public void onConnected(@Nullable Bundle bundle) {

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionToUseGPS();
            return;
        }
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

        //checking if the mLastLocation is recent enough or if the location is null
        //if this is the case, we must request location updates
        if(mLastLocation == null || mLastLocation.getTime() < System.currentTimeMillis() - 2 * 60 * 1000) {
            LocationServices.FusedLocationApi.requestLocationUpdates(
                    mGoogleApiClient,
                    LocationRequest.create(),
                    this
            );
        }

        //Once we have the location, we need to add it to the sharedPreferences
        /*
        ADDING LOCATION TO SHARED PREFERENCES
         */
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPref.edit().putFloat(getString(R.string.latitude_key),(float) mLastLocation.getLatitude()).apply();
        sharedPref.edit().putFloat(getString(R.string.longitude_key),(float) mLastLocation.getLongitude()).apply();

        String previousLocation = sharedPref.getString(getString(R.string.pref_location_name_key),"");
        String newLocation = LocationUtils.getPlaceName(this, mLastLocation.getLatitude(), mLastLocation.getLongitude());
        if(!previousLocation.equals(newLocation)){
            //make the progress bar visible
            progressBar = findViewById(R.id.progress_bar);
            progressBar.setVisibility(View.VISIBLE);
        }
        sharedPref.edit().putString(
                getString(R.string.pref_location_name_key),
                newLocation
        ).apply();

        LocationServices.GeofencingApi.addGeofences(
                mGoogleApiClient,
                getGeofencingRequest(),
                getGeofencedPendingIntent()
        );

//        Intent i = new Intent(this,MainActivity.class);
//        startActivity(i);
    }

    @Override
    public void onLocationChanged(Location location) {
        mLastLocation = location;

        //now that we have the location, we should stop receiving updates
        LocationServices.FusedLocationApi.removeLocationUpdates(
                mGoogleApiClient,
                this
        );
    }

    public GeofencingRequest getGeofencingRequest(){
        return new GeofencingRequest.Builder()
                .addGeofence(
                        getGeofence(
                                mLastLocation.getLatitude(),
                                mLastLocation.getLongitude(),
                                GEOFENCE_RADIUS_IN_METERS
                        )
                )
                //not setting initial trigger as we don't need to prompt user for location update when he is already there
                .build();
    }

    public PendingIntent getGeofencedPendingIntent(){
        Intent i = new Intent(this,GeoFencingTransitionsIntentService.class);
        return PendingIntent.getService(this,0,i,PendingIntent.FLAG_UPDATE_CURRENT);
    }

    @Override
    public void onConnectionSuspended(int i) {
        //retry connecting
        mGoogleApiClient.connect();
        progressBar.setVisibility(View.INVISIBLE);

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        //TODO: handle error IMPORTANT
        progressBar.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onResult(@NonNull Status status) {
        if (status.isSuccess()) {
            Toast.makeText(
                    this,
                    "Geofences Added",
                    Toast.LENGTH_SHORT
            ).show();
        } else {
            // Get the status code for the error and log it using a user-friendly message.
            String errorMessage = GeofenceErrorMessages.getErrorString(this,
                    status.getStatusCode());
        }
    }

    /**
     * SETTINGS FRAGMENT inner class
     */
    public static class SettingsFragment extends PreferenceFragmentCompat implements
            SharedPreferences.OnSharedPreferenceChangeListener {

        private void setPreferenceSummary(Preference preference, Object value) {
            String stringValue = value.toString();

            if (preference instanceof ListPreference) {
                // For list preferences, look up the correct display value in
                // the preference's 'entries' list (since they have separate labels/values).
                ListPreference listPreference = (ListPreference) preference;
                int prefIndex = listPreference.findIndexOfValue(stringValue);
                if (prefIndex >= 0) {
                    preference.setSummary(listPreference.getEntries()[prefIndex]);
                }
            }
            else {
                // For other preferences, set the summary to the value's simple string representation.
                preference.setSummary(stringValue);
            }
        }

        @Override
        public void onCreatePreferences(Bundle bundle, String s) {
            // Add 'general' preferences, defined in the XML file
            addPreferencesFromResource(R.xml.pref_general);

            /*
            for LOCATION
             */
            Preference locationButton = findPreference(getString(R.string.pref_location_name_key));
            locationButton.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    ((SettingsActivity) getActivity()).createLocationPromptDialog();
                    return true;
                }
            });

            findPreference(getString(R.string.item_rate_settings_activity)).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Toast.makeText(getActivity(), "Instead of toast, a link to paid app will open", Toast.LENGTH_SHORT).show();
                    return true;
                }
            });

            findPreference(getString(R.string.item_share_settings_activity)).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Intent sendIntent = new Intent();
                    sendIntent.setAction(Intent.ACTION_SEND);
                    sendIntent.putExtra(
                            Intent.EXTRA_TEXT,
                            getResources().getString(R.string.app_share_message)
                    );
                    sendIntent.setType("text/plain");
                    startActivity(Intent.createChooser(sendIntent, "Share"));
                    return true;
                }
            });

            findPreference(getString(R.string.item_suggest_settings_activity)).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Intent intent = new Intent(Intent.ACTION_SENDTO);
                    String email = getResources().getString(R.string.app_email_id);
                    intent.setData(Uri.parse("mailto:" + email)); // only email apps should handle this
                    intent.putExtra(Intent.EXTRA_SUBJECT, "Feedback");
                    intent.putExtra(Intent.EXTRA_TEXT, "Your feedback");
                    if (intent.resolveActivity(getContext().getPackageManager()) != null) {
                        startActivity(intent);
                    }
                    return true;
                }
            });

            //TODO: probably no need to set summary since mine is only simple preference and checkbox preference
            SharedPreferences sharedPreferences = getPreferenceScreen().getSharedPreferences();
            PreferenceScreen prefScreen = getPreferenceScreen();
            int count = prefScreen.getPreferenceCount();
            for (int i = 0; i < count; i++) {
                Preference p = prefScreen.getPreference(i);
                if (!(p instanceof CheckBoxPreference)) {
                    String value = sharedPreferences.getString(p.getKey(), "");
                    setPreferenceSummary(p, value);
                }
            }
        }

        @Override
        public void onStop() {
            super.onStop();
            // unregister the preference change listener
            getPreferenceScreen().getSharedPreferences()
                    .unregisterOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onStart() {
            super.onStart();
            // register the preference change listener
            getPreferenceScreen().getSharedPreferences()
                    .registerOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            //TODO: Implement this
            Activity activity = getActivity();

            if (key.equals(getString(R.string.pref_location_name_key))) {
                // we've changed the location
                // Wipe out any potential PlacePicker latlng values so that we can use this text entry.
                //WeatherPreferences.resetLocationCoordinates(activity);
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {

                        if(progressBar!=null) progressBar.setVisibility(View.INVISIBLE);
                        Intent i = new Intent(getContext(),MainActivity.class);
                        startActivity(i);
                        ((SettingsActivity)getContext()).finish();
                    }
                }, 1500);

            }
//             else if (key.equals(getString(R.string.pref_units_key))) {
//                // units have changed. update lists of weather entries accordingly
//                activity.getContentResolver().notifyChange(WeatherContract.WeatherEntry.CONTENT_URI, null);
//            }
            Preference preference = findPreference(key);
            if (null != preference) {
                if (!(preference instanceof CheckBoxPreference)) {
                    setPreferenceSummary(preference, sharedPreferences.getString(key, ""));
                }
            }
        }
    }

    //TODO: send user to main screen when location is set
}