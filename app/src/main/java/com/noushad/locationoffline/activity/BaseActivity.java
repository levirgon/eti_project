package com.noushad.locationoffline.activity;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.Toast;

import com.noushad.locationoffline.R;
import com.noushad.locationoffline.utils.SharedPrefManager;

public class BaseActivity extends AppCompatActivity {

    public final long INTERVAL = 1000;
    public final float DISTANCE = 1;
    private BaseActivity mContext;
    ProgressDialog mProgressDialog;
    private static final String TAG = "BaseActivity";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        mProgressDialog = new ProgressDialog(this);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
        mContext = this;

    }

    public void navigate(String lat, String lng, String placeName) {
        if (!SharedPrefManager.getInstance(mContext).isTeleportOn()) {
            Uri gmmIntentUri = Uri.parse("google.navigation:q=" + lat + "," + lng);
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
            mapIntent.setPackage("com.google.android.apps.maps");
            startActivity(mapIntent);
        } else {
            String tpLatitude = SharedPrefManager.getInstance(mContext).getTPLatitude();
            String tpLongitude = SharedPrefManager.getInstance(mContext).getTPLongitude();
            Uri gmmIntentUri = Uri.parse("http://maps.google.com/maps?" +
                    "saddr=" + tpLatitude + "," + tpLongitude + "&daddr=" + lat + "," + lng);
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
            mapIntent.setPackage("com.google.android.apps.maps");

            startActivity(mapIntent);

        }
    }

    public void showPoint(String lat, String lng, String placeName) {
        Intent intent = new Intent(Intent.ACTION_VIEW,
                Uri.parse("geo:<" + 0 + ">,<" + 0 + ">?q=<" + lat + ">,<" + lng + ">(" + placeName + ")"));
        startActivity(intent);
    }



    public void shareMyLocation(final String msg, final String latitude, final String longitude) {

        //Message Dialog


        final AlertDialog.Builder builder;
        builder = new AlertDialog.Builder(mContext);
        final AlertDialog dialog = builder.create();
        dialog.setTitle("Enter A Message About It");
        View viewInflated = LayoutInflater.from(mContext).inflate(R.layout.message_input_layout, null, false);

        final EditText msgInsert = viewInflated.findViewById(R.id.message_input);
        TextInputLayout inputLayout = viewInflated.findViewById(R.id.message_input_layout);
        inputLayout.setHint("Your Message ");
        dialog.setButton(DialogInterface.BUTTON_POSITIVE, "SEND", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                String userMessage = msgInsert.getText().toString();
                String text =
                        userMessage + '\n' +
                                msg + '\n'
                                + "Insert These Coordinates To Your " + '\n'
                                + " | Find Place |" + " option " + '\n'
                                + "Latitude : " + latitude + '\n'
                                + "Longitude : " + longitude;

                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_TEXT, text);
                sendIntent.setType("text/plain");
                mContext.startActivity(sendIntent);
            }
        });

        dialog.setView(viewInflated);
        dialog.show();

    }

    public void startNavigation(final String lat, final String lng, final String msg) {

        if (isNetworkAvailable()) {
            navigate(lat, lng, msg);
        } else {
            showPoint(lat, lng, msg);
        }

    }

    public boolean isValidLatLng(double lat, double lng) {
        if (lat < -90 || lat > 90) {
            return false;
        } else if (lng < -180 || lng > 180) {
            return false;
        }
        return true;
    }




    public boolean isNetworkAvailable() {
        ConnectivityManager manager =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = manager.getActiveNetworkInfo();
        boolean isAvailable = false;
        if (networkInfo != null && networkInfo.isConnected()) {
            // Network is present and connected
            isAvailable = true;
        }
        return isAvailable;
    }

    public void showSnack(View parent, String msg) {

        Snackbar snackbar;
        snackbar = Snackbar.make(parent, msg, Snackbar.LENGTH_LONG);
        View snackBarView = snackbar.getView();
        snackBarView.setBackgroundColor(getResources().getColor(R.color.colorAccent));
        snackbar.setActionTextColor(getResources().getColor(R.color.colorGrey));
        snackbar.show();

    }


    public void animateFromRight(View view) {
        view.startAnimation(AnimationUtils.loadAnimation(
                this, R.anim.slide_from_right
        ));
    }

    public void animateFromLeft(View view) {
        view.startAnimation(AnimationUtils.loadAnimation(
                this, R.anim.slide_from_left
        ));
    }

    public void turnOnTeleportMode(String lat, String lng) {

        SharedPrefManager.getInstance(mContext).turnOnTeleportMode(lat, lng);
        Toast.makeText(this, "You Are Now On TeleportMode, Explore!!", Toast.LENGTH_LONG).show();


    }

}
