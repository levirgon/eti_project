package com.noushad.locationoffline.utils;

import android.Manifest;
import android.content.Context;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

/**
 * By noushad hasan
 */

public class SmartLocationProvider
        implements NetworkStateReceiver.NetworkStateReceiverListener, LocationListener {

    private static final String TAG = "SmartLocationProvider";
    private static long INTERVAL = 1000;
    private static float DISTANCE = 1;
    private static final long LOCATION_TIMEOUT = 40000;
    private static final long COUNT_INTERVAL = 1000;
    public static final String ONE_TIME = "ONE_TIME_LOCATION";
    public static final String TRACKING = "TRACK LOCATION";
    public static final String FREQUENT = "FREQUENT LOCATION UPDATE";
    public static final String LAZY = "LAZY LOCATION UPDATE";
    public static final String IMMEDIATE = "IMMEDIATE LOCATION UPDATE";
    private LocationManager locationManager;
    private FusedLocationProviderClient mFusedLocationClient;
    private NetworkStateReceiver networkStateReceiver;
    private boolean isGPSTurnedOn;
    private boolean isDemandingNetworkSwitch;
    private boolean isLocationFound;
    private LocationProviderStateListener mListener;
    private String mLocationType;
    private String mIntervalType;
    private Context mContext;


    public SmartLocationProvider(Context context, String locationType, String intervalType) throws Exception {
        mLocationType = locationType;
        mIntervalType = intervalType;
        if (context instanceof LocationProviderStateListener) {

            mListener = (LocationProviderStateListener) context;
            mContext = context.getApplicationContext();

        } else {
            throw new Exception("Must Implement location provider interface");
        }
        if (mIntervalType.equalsIgnoreCase(FREQUENT)) {
            INTERVAL = 1000;
            DISTANCE = 1;
        } else if (mIntervalType.equalsIgnoreCase(LAZY)) {
            INTERVAL = 1000 * 10;
            DISTANCE = 5;
        } else if (mIntervalType.equalsIgnoreCase(IMMEDIATE)) {
            INTERVAL = 0;
            DISTANCE = 0;
        }
    }

    public void startService() {
        Log.e(TAG, "startService: ");
        setupGPSandNetworkListener();
        getLastLocation();
    }

    private void setupGPSandNetworkListener() {


        Log.e(TAG, "setupGPSandNetworkListener: ");
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(mContext);
        locationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {

            return;
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, INTERVAL, DISTANCE, this);

        networkStateReceiver = new NetworkStateReceiver();
        networkStateReceiver.addListener(this);
        mContext.registerReceiver(networkStateReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
    }

    private void getLastLocation() {
        Log.e(TAG, "getLastLocation: ");

        if (ActivityCompat.checkSelfPermission(mContext,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(mContext,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            permissionAbsentEvent();

            return;
        }

        mFusedLocationClient.getLastLocation()
                .addOnSuccessListener(new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        // Got last known location. In some rare situations this can be null.
                        if (location != null) {
                            Log.e(TAG, "onSuccess:Previous Location found");
                            updateLocationEvent(location);
                        }else{
                            Log.e(TAG, "onSuccess: Previous location not found" );
                        }


                    }
                });

        startLocationService();


    }

    private boolean isLocationServiceAvailable() {
        Log.e(TAG, "isLocationServiceAvailable: ");

        if (isGPSTurnedOn)
            return true;
        return false;
    }

    private void startLocationService() {
        Log.e(TAG, "startLocationService: ");

        if (!isLocationServiceAvailable())
            return;

        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            return;
        }

        if (isDemandingNetworkSwitch) {
            switchToNetWorkMode();
        } else {
            switchToGPSMode();
            new CountDownTimer(LOCATION_TIMEOUT, COUNT_INTERVAL) {

                public void onTick(long millisUntilFinished) {

                }

                public void onFinish() {
                    if (!isNetworkAvailable()) {
                        isDemandingNetworkSwitch = true;
                    } else {
                        switchToNetWorkMode();
                    }
                }
            }.start();
        }


    }

    private boolean isNetworkAvailable() {
        Log.e(TAG, "isNetworkAvailable: ");

        ConnectivityManager manager =
                (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo networkInfo = manager != null ? manager.getActiveNetworkInfo() : null;
        boolean isAvailable = false;
        if (networkInfo != null && networkInfo.isConnected()) {
            // Network is present and connected
            isAvailable = true;
        }
        return isAvailable;
    }

    private void switchToGPSMode() {

        Log.e(TAG, "switchToGPSMode: ");
        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                INTERVAL,
                DISTANCE, this);
    }

    private void switchToNetWorkMode() {
        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {

            return;
        }
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
                INTERVAL,
                DISTANCE, this);

        Log.e(TAG, "switchToNetWorkMode: ");
    }

    //=============================================================================================//
    @Override
    public void networkAvailable() {

        if (isDemandingNetworkSwitch && !isLocationFound) {
            startLocationService();
        }
        internetServiceEnabledEvent();
    }

    @Override
    public void networkUnavailable() {

        internetServiceDisabledEvent();

    }

    @Override
    public void onLocationChanged(Location location) {
        updateLocationEvent(location);
        Log.e(TAG, "onLocationChanged: ");

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

        Log.e(TAG, "onStatusChanged: " + status);

    }

    @Override
    public void onProviderEnabled(String provider) {
        Log.e(TAG, "onProviderEnabled: ");
        isGPSTurnedOn = true;
        getLastLocation();
        locationServiceEnabledEvent();
    }

    @Override
    public void onProviderDisabled(String provider) {
        Log.e(TAG, "onProviderDisabled: ");
        isGPSTurnedOn = false;

        locationServiceDisabledEvent();


    }

    //=============================================================================================//
    private void permissionAbsentEvent() {
        mListener.locationPermissionDenied();

    }

    private void locationServiceDisabledEvent() {
        mListener.locationServiceDisabled();
    }

    private void internetServiceDisabledEvent() {
        mListener.internetServiceDisabled();
    }

    private void updateLocationEvent(Location location) {
        isLocationFound = true;
        mListener.locationUpdated(location);

        if (mLocationType.equalsIgnoreCase(ONE_TIME))
            locationManager.removeUpdates(this);
    }

    private void internetServiceEnabledEvent() {
        mListener.internetServiceEnabled();
    }

    private void locationServiceEnabledEvent() {
        mListener.locationServiceEnabled();
    }

    public interface LocationProviderStateListener {

        void locationPermissionDenied();

        void locationServiceDisabled();

        void internetServiceDisabled();

        void locationUpdated(Location location);

        void locationServiceEnabled();

        void internetServiceEnabled();
    }

}
