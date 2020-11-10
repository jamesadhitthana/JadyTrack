package com.jady.jadytrack.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.jady.jadytrack.R;
import com.kaopiz.kprogresshud.KProgressHUD;
import com.orhanobut.hawk.Hawk;
import com.tapadoo.alerter.Alerter;

public class WebActivity extends AppCompatActivity {
    private String trackingId;
    //    Following InputOptionActivity.java
    public static final String EXTRA_MESSAGE_ID = "com.jady.jadytrack.SENDID";
    public static final String EXTRA_MESSAGE_UID = "com.jady.jadytrack.SENDUID";

    private Button webAppointmentButton;
    private Button webTrackingButton;
    private TextView labelTrackingIdTitle;
    private TextView labelTrackingId;
    private TextView labelTargetName;
    // Handler
    public boolean internetStatus = true;

    // Firebase authentication
    private FirebaseAuth mAuth;
    private String currentUserUID;
    //Loading Screen
    private KProgressHUD loadingWindow;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web);

//        *Variables//
        labelTrackingIdTitle = (TextView) findViewById(R.id.trackingIdTitleTextView);
        labelTrackingId = (TextView) findViewById(R.id.trackingIdTextView);
        labelTargetName = (TextView) findViewById(R.id.textViewTargetName);
        webAppointmentButton = (Button) findViewById(R.id.webAppointmentButton);
        webTrackingButton = (Button) findViewById(R.id.webTrackingButton);

        //Firebase auth//
        mAuth = FirebaseAuth.getInstance();

        //*--Process Data from Web App Link--//
        Intent intent = getIntent(); //Web app opens an intent of WebActivity
        String action = intent.getAction();
        Uri data = intent.getData();
        String webAppLink = intent.getDataString(); //Raw Link in String
        trackingId = data.getQueryParameter("id");
//        Toast.makeText(getApplicationContext(), "This is the tracking id: " + trackingId, Toast.LENGTH_LONG).show();
        Log.d("james", "This is the data: " + webAppLink);
        Log.d("james", "This is the ID: ");
        //END OF: Process Data from Web App Link//


//        *Update Labels:
        labelTrackingId.setText(trackingId);

//        *Button Functionalites
        webTrackingButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                checkDatabase("viewer", trackingId, currentUserUID);
            }
        });

        //        *Button Functionalites
        webAppointmentButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                checkDatabase("appointment", trackingId, currentUserUID);
            }
        });
        // Internet connection handler
        final Handler handler = new Handler();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    if (isConnected(WebActivity.this)) {
                        if (internetStatus) {
                            buildDialog(WebActivity.this).show();
                            internetStatus = false;
                        }
                    } else {
                        internetStatus = true;
                    }
                } catch (Exception e) {
                    // TODO: handle exception
                } finally {
                    // Also call the same runnable to call it at regular interval
                    handler.postDelayed(this, 1000);
                }
            }
        };
        handler.postDelayed(runnable, 1000);

    }

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            currentUserUID = currentUser.getUid();
            webAppointmentButton.setEnabled(true);
            webTrackingButton.setEnabled(true);
//            Alerter.create(WebActivity.this).setTitle(currentUserUID).setText(currentUserUID).setBackgroundColorRes(R.color.colorAccent).show();
        } else {
//*Notify user is not logged in and so disable the appointment button
            webAppointmentButton.setEnabled(false);
            Alerter.create(WebActivity.this).setTitle(getResources().getString(R.string.layout_web_link_alerter_login_title)).setText(getResources().getString(R.string.layout_web_link_alerter_login)).setBackgroundColorRes(R.color.colorAccent).show();
        }
//        ---Check if Tracking ID Exists
        checkTrackingIdExists(trackingId);
    }//end of OnStart


    //    --Functions--//
    public boolean isConnected(Context context) {

        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netinfo = cm.getActiveNetworkInfo();

        if (netinfo != null && netinfo.isConnectedOrConnecting()) {
            NetworkInfo wifi = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            NetworkInfo mobile = cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);

            return (mobile == null || !mobile.isConnectedOrConnecting()) && (wifi == null || !wifi.isConnectedOrConnecting());
        } else
            return true;
    }

    public AlertDialog.Builder buildDialog(Context c) {

        AlertDialog.Builder builder = new AlertDialog.Builder(c);
        builder.setTitle(getResources().getString(R.string.alert_title_no_internet_connection));
        builder.setMessage(getResources().getString(R.string.alert_msg_no_internet_connection));
        builder.setCancelable(false);

        builder.setPositiveButton(getResources().getString(R.string.button_try_again), new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {

                dialog.cancel();
                if (isConnected(WebActivity.this)) {
                    buildDialog(WebActivity.this).show();
                }
            }
        });

        builder.setNegativeButton(getResources().getString(R.string.button_exit), new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {

                finish();
                moveTaskToBack(true);
            }
        });

        return builder;
    }

    private KProgressHUD buildLoadingWindow() {
        return KProgressHUD.create(WebActivity.this)
                .setStyle(KProgressHUD.Style.SPIN_INDETERMINATE)
                .setBackgroundColor(Color.parseColor("#508AF1F7"))
                .setLabel(getResources().getString(R.string.loading_label_please_wait))
                .setDetailsLabel(getResources().getString(R.string.loading_details_downloading_data))
                .setCancellable(true)
                .setAnimationSpeed(2)
                .setDimAmount(0.5f)
                .show();
    }

    //*Check Database
    private void checkDatabase(final String scenario, final String id, final String uid) {
        // Set loading window
        loadingWindow = buildLoadingWindow();

        //*--Start Tracking--//
        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference().child("trackingSession");
        rootRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.hasChild(id)) {
                    if (scenario.equals("viewer")) {
                        Intent intent = new Intent(WebActivity.this, TrackingActivity.class);
                        intent.putExtra(EXTRA_MESSAGE_ID, id);
                        loadingWindow.dismiss();
                        startActivity(intent);
                    } else if (scenario.equals("appointment")) {

                        Intent intent = new Intent(WebActivity.this, AppointmentActivity.class);
                        intent.putExtra(EXTRA_MESSAGE_ID, id);
                        intent.putExtra(EXTRA_MESSAGE_UID, uid);
                        loadingWindow.dismiss();
                        startActivity(intent);
                    }
                } else {
                    Alerter.create(WebActivity.this).setTitle(getResources().getString(R.string.alert_title_failed_find_id)).setText(getResources().getString(R.string.alert_msg_failed_find_id)).setBackgroundColorRes(R.color.colorAccent).show();
                    loadingWindow.dismiss();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });


    }

    //*checkTrackingIdExists at Start
    private void checkTrackingIdExists(final String id) {
        // Set loading window
        loadingWindow = buildLoadingWindow();

        //*--Start Tracking--//
        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference().child("trackingSession");
        rootRef.addListenerForSingleValueEvent(new ValueEventListener() { //Read Once only
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                loadingWindow.dismiss();
                if (snapshot.hasChild(id)) {
//                    found
                    Alerter.create(WebActivity.this).setTitle(getResources().getString(R.string.layout_web_link_found_tracking_id)).setText(getResources().getString(R.string.layout_web_link_found_tracking_id_desc)).setBackgroundColorRes(R.color.colorAccent).show();

                    labelTargetName.setText((String) snapshot.child(id).child("targetName").getValue());
                } else {
//                    Not found
                    Alerter.create(WebActivity.this).setTitle(getResources().getString(R.string.alert_title_failed_find_id)).setText(getResources().getString(R.string.alert_msg_failed_find_id)).setBackgroundColorRes(R.color.colorAccent).show();
                    labelTargetName.setText(getResources().getString(R.string.layout_web_link_tracking_id_not_found));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });


    }

}