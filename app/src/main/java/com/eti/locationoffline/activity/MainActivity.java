package com.eti.locationoffline.activity;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.CardView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.eti.locationoffline.broadcastreciever.NetworkStateReceiver;
import com.eti.locationoffline.utils.SharedPrefManager;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.android.gms.maps.model.LatLng;
import com.eti.locationoffline.R;

public class MainActivity extends BaseActivity implements View.OnClickListener,
        LocationListener, NetworkStateReceiver.NetworkStateReceiverListener {


    private static final int PLACE_PICKER_REQUEST = 101;
    private static final int ACCESS_LOCATION_REQUEST = 111;
    LocationManager locationManager;
    Context mContext;
    private double mLatitude;
    private double mLongitude;
    public boolean isFirstLoad = true;
    public boolean isGPSTurnedOn = false;
    private boolean isLocationStillUnknown = true;
    private NetworkStateReceiver networkStateReceiver;
    private boolean isAlertShowing = false;


    //views

    private TextView mTitleText;
    private TextView mSubTitleText;
    private ImageView mStatusImage;
    private NestedScrollView mButtonsContainer;
    private CardView shareLocButton;
    private CardView teleportButton;
    private CardView findFrndButton;
    private CardView currentLocationButton;
    private AlertDialog mGpsDialog;
    private ProgressDialog mLocationProgressDialog;

    private static final String TAG = "MainActivity";


    //System Method
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mContext = this;
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        initializeViews();


        if (SharedPrefManager.getInstance(this).isFirstLoad()) {
            setLoadingState();
            startLocationProvider();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (SharedPrefManager.getInstance(this).isTeleportOn()) {
            toggleButtonState(true);
            setEnabledState();
        }
    }

    private void startLocationProvider() {


        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION
                    , Manifest.permission.ACCESS_COARSE_LOCATION}, ACCESS_LOCATION_REQUEST);

            return;
        }

        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, INTERVAL, DISTANCE, this);
        networkStateReceiver = new NetworkStateReceiver();
        networkStateReceiver.addListener(this);
        registerReceiver(networkStateReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));


    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case ACCESS_LOCATION_REQUEST: {

                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(MainActivity.this, "Permission granted", Toast.LENGTH_SHORT).show();
                    startLocationProvider();

                } else {
                    Toast.makeText(MainActivity.this, "Permission denied to get your location", Toast.LENGTH_SHORT).show();
                }
                return;
            }


        }
    }

    @Override
    public void onClick(View v) {

        switch (v.getId()) {

            case R.id.check_current_location:
                if (!SharedPrefManager.getInstance(mContext).isTeleportOn()) {
                    if (isFirstLoad) {
                        showSnack(v, "Your Location Is Still Unknown");
                    } else {
                        showPoint(String.valueOf(mLatitude), String.valueOf(mLongitude), "You Are Currently Here!");
                    }
                } else {
                    showPoint(SharedPrefManager.getInstance(mContext).getTPLatitude()
                            , SharedPrefManager.getInstance(mContext).getTPLongitude(), "You Are Currently Here!");
                }
                break;

            case R.id.loc_share_button:
                if (!SharedPrefManager.getInstance(mContext).isTeleportOn()) {
                    shareMyLocation(" ", String.valueOf(mLatitude), String.valueOf(mLongitude));
                } else {

                    if (isFirstLoad) {
                        showSnack(v, "Your Location Is Still Unknown");
                    } else {

                        String lat = SharedPrefManager.getInstance(mContext).getTPLatitude();
                        String lng = SharedPrefManager.getInstance(mContext).getTPLongitude();
                        shareMyLocation(" ", lat, lng);
                    }
                }
                break;
            case R.id.find_place_button:
                showInputLocationDialog();
                break;
            case R.id.teleport_button:
                if (SharedPrefManager.getInstance(mContext).isTeleportOn()) {
                    showTeleportOffDialog();
                } else {
                    showPlacePicker();
                }

                break;

        }

    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void showTeleportOffDialog() {
        AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.setTitle("Teleport Mode");
        alertDialog.setMessage("You Are Already On Teleport Mode, What Do You Want To Do? ");
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "TURN OFF",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        SharedPrefManager.getInstance(mContext).turnOffTeleportMode();
                        toggleButtonState(false);
                        dialog.dismiss();
                    }
                });
        alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "TELEPORT AGAIN", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                showPlacePicker();
                dialog.dismiss();
            }
        });
        alertDialog.show();

    }

    private void showPlacePicker() {
        PlacePicker.IntentBuilder builder = new PlacePicker.IntentBuilder();
        try {
            startActivityForResult(builder.build(this), PLACE_PICKER_REQUEST);
        } catch (GooglePlayServicesRepairableException e) {
            Toast.makeText(mContext, e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
        } catch (GooglePlayServicesNotAvailableException e) {
            Toast.makeText(mContext, e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PLACE_PICKER_REQUEST) {
            if (resultCode == RESULT_OK) {
                Place place = PlacePicker.getPlace(mContext, data);
                LatLng coordinates = place.getLatLng();
                String lat = String.valueOf(coordinates.latitude);
                String lng = String.valueOf(coordinates.longitude);
                showTeleportConfirmDialog(lat, lng);
            }
        }
    }

    private void showTeleportConfirmDialog(final String lat, final String lng) {
        AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.setTitle("Teleport Mode");
        alertDialog.setCancelable(false);
        alertDialog.setMessage("This action will change your current location to the location you have selected," +
                " All your future actions will be taken based on this location until you turn it off ");
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        turnOnTeleportMode(lat, lng);
                        toggleButtonState(true);
                        dialog.dismiss();
                    }
                });
        alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "CANCEL", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        alertDialog.show();

    }

    private void toggleButtonState(boolean isTeleportOn) {
        LinearLayout teleportIndicator = findViewById(R.id.teleport_indicator_main);
        TextView teleportText = findViewById(R.id.textViewTele);
        TextView teleportTextSub = findViewById(R.id.textViewTeleSub);
        if (isTeleportOn) {
            teleportIndicator.setVisibility(View.VISIBLE);
            teleportButton.setBackground(getResources().getDrawable(R.drawable.violet_gradient));
            teleportButton.requestFocus();
            teleportText.setText("TURN OFF");
            teleportText.setTextColor(getResources().getColor(R.color.colorGrey));
            teleportTextSub.setText("Your Are On A Different Location");
            teleportTextSub.setTextColor(getResources().getColor(R.color.colorGrey));

        } else {
            teleportIndicator.setVisibility(View.INVISIBLE);
            teleportButton.setBackground(getResources().getDrawable(R.drawable.white_round_bg));
            teleportText.setText("TELEPORT");
            teleportText.setTextColor(getResources().getColor(R.color.colorText));
            teleportTextSub.setText("Be Anywhere Anytime!!");
            teleportTextSub.setTextColor(getResources().getColor(R.color.colorText));
        }


    }

    //initiate
    private void initializeViews() {

        buildGpsDialog();
        buildProgressDialog();
        mButtonsContainer = findViewById(R.id.button_container);
        mTitleText = findViewById(R.id.status_text_title);
        mSubTitleText = findViewById(R.id.status_sub);
        mStatusImage = findViewById(R.id.status_image);

        shareLocButton = findViewById(R.id.loc_share_button);
        shareLocButton.setOnClickListener(this);
        teleportButton = findViewById(R.id.teleport_button);
        teleportButton.setOnClickListener(this);
        findFrndButton = findViewById(R.id.find_place_button);
        findFrndButton.setOnClickListener(this);
        currentLocationButton = findViewById(R.id.check_current_location);
        currentLocationButton.setOnClickListener(this);

        String indicator = getIntent().getStringExtra("indicator");


    }

    private void buildProgressDialog() {
        mLocationProgressDialog = new ProgressDialog(this);
        mLocationProgressDialog.setMessage("Getting your location");
        mLocationProgressDialog.setCancelable(false);
    }

    private void buildGpsDialog() {
        mGpsDialog = new AlertDialog.Builder(this).create();
        mGpsDialog.setTitle("Enable Location");
        mGpsDialog.setMessage("Your locations setting is not enabled. Please enabled it in settings menu.");
        mGpsDialog.setButton(DialogInterface.BUTTON_POSITIVE, "SETTING", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(intent);
            }
        });
        mGpsDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "DISMISS", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });
        mGpsDialog.setCancelable(false);
    }


    private void foundLocation(double latitude, double longitude) {
        mLatitude = latitude;
        mLongitude = longitude;
        setEnabledState();

        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            return;
        }


    }

    //view state
    private void setLoadingState() {

        if (mGpsDialog != null) {
            mGpsDialog.dismiss();
            isAlertShowing = false;
        }

        if (isFirstLoad) {
            isLocationStillUnknown = true;
            mTitleText.setText("We will find you..");
            mSubTitleText.setText("Almost there... ");
            mLocationProgressDialog.show();

            greyText(mTitleText);
            greyText(mSubTitleText);


        }

    }

    private void setEnabledState() {
        isLocationStillUnknown = false;
        mButtonsContainer.setVisibility(View.VISIBLE);
        mLocationProgressDialog.dismiss();
        if (isFirstLoad) {
            animateButtons();
            isFirstLoad = false;

        }

        mStatusImage.setImageResource(R.drawable.street_map);
        mTitleText.setText("There You Are!!");
        mSubTitleText.setText("You are ready to explore the world offline");
        Log.e(TAG, "setEnabledState: ");

        colorizeText(mTitleText);
        colorizeText(mSubTitleText);
    }

    private void setDisabledState() {

        mLocationProgressDialog.hide();
        mStatusImage.setImageResource(R.drawable.street_map_grey);
        mTitleText.setText("Location service is turned Off");
        mSubTitleText.setText("The app cannot work without location access");

        Log.e(TAG, "setDisabledState: ");

        greyText(mTitleText);
        greyText(mSubTitleText);


    }

    private void colorizeText(TextView view) {
        view.setTextColor(getResources().getColor(R.color.colorAccent));
    }

    private void greyText(TextView view) {
        view.setTextColor(getResources().getColor(R.color.colorGreyDark));
        switch (view.getId()) {
            case R.id.status_sub:
                view.setTextColor(getResources().getColor(R.color.colorRed));
        }
    }

    //Animations
    private void animateButtons() {

        animateFromRight(shareLocButton);
        animateFromRight(teleportButton);

        animateFromLeft(currentLocationButton);
        animateFromLeft(findFrndButton);


    }

    //Action
    private void showInputLocationDialog() {

        final AlertDialog.Builder builder;
        builder = new AlertDialog.Builder(mContext);
        final AlertDialog dialog = builder.create();
        dialog.setTitle("Input Your Friends Location ");
        View viewInflated = LayoutInflater.from(mContext).inflate(R.layout.coordinate_input, null, false);
        final EditText latInput = viewInflated.findViewById(R.id.latitude_input);
        final EditText lngInput = viewInflated.findViewById(R.id.longitude_input);

        dialog.setButton(DialogInterface.BUTTON_POSITIVE, "FIND", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String lat = latInput.getText().toString();
                String lng = lngInput.getText().toString();
                if (lat.trim().isEmpty() || lng.trim().isEmpty()) {
                    Toast.makeText(MainActivity.this, "No Fields Can Be  Empty", Toast.LENGTH_SHORT).show();
                } else {
                    startNavigation(lat, lng, "I Am Here !!");
                    dialog.dismiss();
                }

            }
        });

        dialog.setButton(DialogInterface.BUTTON_NEGATIVE, "CANCEL", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        dialog.setView(viewInflated);
        dialog.show();

    }


    @Override
    public void onLocationChanged(Location location) {
        mLocationProgressDialog.dismiss();
        foundLocation(location.getLatitude(), location.getLongitude());
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {
        mGpsDialog.dismiss();
        mLocationProgressDialog.show();
        setLoadingState();
    }

    @Override
    public void onProviderDisabled(String s) {
        mLocationProgressDialog.dismiss();
        mGpsDialog.show();
        setDisabledState();
    }

    @Override
    public void networkAvailable() {

    }

    @Override
    public void networkUnavailable() {

    }
}
