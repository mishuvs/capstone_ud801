package com.weather.wallpaper.forecast.location;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.android.gms.maps.model.LatLng;
import com.weather.wallpaper.forecast.R;
import com.weather.wallpaper.forecast.SettingsActivity;
import com.weather.wallpaper.forecast.utilities.LocationUtils;

import static com.weather.wallpaper.forecast.SettingsActivity.DIALOG_TAG;
import static com.weather.wallpaper.forecast.SettingsActivity.dialog;

/**
 * Created by Vaibhav on 8/23/2017.
 */

public class LocationPromptDialog extends DialogFragment {

    private SharedPreferences sharedPref;
    private final String THREATEN_EXIT_DIALOG_TAG = "Threaten exit", TAG = LocationPromptDialog.class.getSimpleName();
    private Dialog locationPromptDialog;
    private LocationPromptDialogListener mListener;
    private SettingsActivity settingsActivity;

    public interface LocationPromptDialogListener{
        public void onGpsButtonClick(DialogFragment dialog);
    }

    @Override
    public void onAttach(Context context) {
            super.onAttach(context);
            settingsActivity = (SettingsActivity) context;
            // Verify that the host activity implements the callback interface
            try {
                // Instantiate the NoticeDialogListener so we can send events to the host
                mListener = (LocationPromptDialogListener) context;
            } catch (ClassCastException e) {
                // The activity doesn't implement the interface, throw exception
                throw new ClassCastException(context.toString()+ " must implement NoticeDialogListener");
            }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        settingsActivity = (SettingsActivity) activity;
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            mListener = (LocationPromptDialogListener) activity;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(activity.toString()+ " must implement NoticeDialogListener");
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable final ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.location_prompt_dialog, null, false);

        Window window = getDialog().getWindow();
        if(window!=null) window.requestFeature(Window.FEATURE_NO_TITLE);

        /*
        When the gps button is clicked, SettingsActivity's implemented onGpsButtonClick will be invoked
         */
        view.findViewById(R.id.useGpsDialogButton)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if(mListener!=null) mListener.onGpsButtonClick(LocationPromptDialog.this);
                    }
                });

        /*
        Code for PlaceAutoComplete from Google Places API
         */

        PlaceAutocompleteFragment placeAutocompleteFragment = new PlaceAutocompleteFragment();
        FragmentManager fm = getChildFragmentManager();
        android.app.FragmentTransaction ft = fm.beginTransaction();
        ft.replace(R.id.place_autocomplete_fragment, placeAutocompleteFragment);
        ft.commit();

        placeAutocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {

            /*
            Saving the latitude and longitude from the place selected.
             */
            @Override
            public void onPlaceSelected(Place place) {

                LatLng latLng = place.getLatLng();

                sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
                SharedPreferences.Editor editor = sharedPref.edit();

                editor.putFloat(getString(R.string.latitude_key), (float) latLng.latitude).apply();
                editor.putFloat(getString(R.string.longitude_key), (float) latLng.longitude).apply();
                String placeName = LocationUtils.getPlaceName(getActivity(),latLng.latitude, latLng.longitude);
                if(placeName == null){
                    placeName = (String) place.getName();
                }
                String previousLocation = sharedPref.getString(getString(R.string.pref_location_name_key),"");
                String newLocation = placeName;
                if(!previousLocation.equals(newLocation)){
                    //make the progress bar visible
                    settingsActivity.findViewById(R.id.progress_bar).setVisibility(View.VISIBLE);
                    editor.putString(
                            getString(R.string.pref_location_name_key),
                            placeName
                    ).apply();
                }

                dialog.dismiss();
            }

            @Override
            public void onError(Status status) {
                // TODO: Handle the error.
            }
        });

        return view;
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
        float latitude = sharedPref.getFloat(getString(R.string.latitude_key),0);
        float longitude = sharedPref.getFloat(getString(R.string.longitude_key),0);

        /*
        if cancelled, create new dialog explaining to the user why app can't run
         */
        if(latitude==0 && longitude==0){
            DialogFragment threatenExitDialog = new ThreatenExitDialog();
            threatenExitDialog.setCancelable(false);
            threatenExitDialog.show(getFragmentManager(), THREATEN_EXIT_DIALOG_TAG);
        }
        super.onCancel(dialog);
    }

    public static class ThreatenExitDialog extends DialogFragment {

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.threaten_exit)
                    .setCancelable(false)
                    .setNegativeButton(R.string.threaten_exit_negative_button, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            android.os.Process.killProcess(android.os.Process.myPid());
                            System.exit(1);
                            getActivity().finish();
                        }
                    })
                    .setPositiveButton(R.string.set_location, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            new LocationPromptDialog().show(getFragmentManager(), DIALOG_TAG);
                        }
                    })
                    .setMessage(R.string.threaten_exit_explanation);

            return builder.create();
        }
    }
}
