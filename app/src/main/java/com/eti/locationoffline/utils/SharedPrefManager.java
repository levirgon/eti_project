package com.eti.locationoffline.utils;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by noushad on 3/27/18.
 */

public class SharedPrefManager {

    private static final String KEY_FLIGHT_MODE = "com.noushad.locationoffline.utils.FlightMode";
    private static final String KEY_TELEPORT_LATITUDE = "com.noushad.locationoffline.utils.latitude";
    private static final String KEY_TELEPORT_LONGITUDE = "com.noushad.locationoffline.utils.longitude";

    private static final String FLIGHT_ON = "FLIGHT_MODE_ON";
    private static final String KEY_FIRST_LOAD = "com.noushad.locationoffline.utils.first_time_load";
    private static Context sContext;
    private static SharedPrefManager mInstance;
    private static final String SHARED_PREF_NAME = "com.noushad.locationoffline.utils";


    private SharedPrefManager(Context context) {
        sContext = context;
    }

    public static synchronized SharedPrefManager getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new SharedPrefManager(context);
        }
        return mInstance;
    }

    public void turnOnTeleportMode(String lat, String lng) {
        setTeleportLatitude(lat);
        setTeleportLongitude(lng);
        SharedPreferences sharedPreferences = sContext.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_FLIGHT_MODE, FLIGHT_ON);
        editor.apply();
    }

    public void turnOffTeleportMode() {
        removeLatitude();
        removeLongitude();
        SharedPreferences sharedPreferences = sContext.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(KEY_FLIGHT_MODE);
        editor.apply();
    }

    public boolean isTeleportOn() {
        SharedPreferences sharedPreferences = sContext.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);
        return sharedPreferences.contains(KEY_FLIGHT_MODE);
    }

    private void setTeleportLatitude(String lat) {
        SharedPreferences sharedPreferences = sContext.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_TELEPORT_LATITUDE, lat);
        editor.apply();
    }

    private void setTeleportLongitude(String lng) {
        SharedPreferences sharedPreferences = sContext.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_TELEPORT_LONGITUDE, lng);
        editor.apply();
    }

    private void removeLongitude() {
        SharedPreferences sharedPreferences = sContext.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(KEY_TELEPORT_LONGITUDE);
        editor.apply();
    }

    private void removeLatitude() {
        SharedPreferences sharedPreferences = sContext.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(KEY_TELEPORT_LATITUDE);
        editor.apply();
    }

    public String getTPLatitude() {
        SharedPreferences sharedPreferences = sContext.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);
        return sharedPreferences.getString(KEY_TELEPORT_LATITUDE, null);
    }

    public String getTPLongitude() {
        SharedPreferences sharedPreferences = sContext.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);
        return sharedPreferences.getString(KEY_TELEPORT_LONGITUDE, null);
    }

    public boolean isFirstLoad() {
        SharedPreferences sharedPreferences = sContext.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);
        return !sharedPreferences.contains(KEY_FIRST_LOAD);
    }

    public void clearInstance() {
        if(sContext != null) {
            sContext = null;
        }
    }

}
