package com.jady.jadytrack.activity;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.zxing.Result;
import com.jady.jadytrack.R;
import com.kaopiz.kprogresshud.KProgressHUD;
import com.tapadoo.alerter.Alerter;

import me.dm7.barcodescanner.zxing.ZXingScannerView;

public class ScanQrActivity extends AppCompatActivity implements ZXingScannerView.ResultHandler {

    public static final String EXTRA_MESSAGE_ID = "com.jady.jadytrack.SENDID";
    public static final String EXTRA_MESSAGE_UID = "com.jady.jadytrack.SENDUID";

    // Handler
    public boolean internetStatus = true;
    private int sessionHandler = 0; // Prevent QR scans repeatedly

    // Attribute
    private String id;
    private String scenario;
    private String uid;

    // View for QR Scanner
    private ZXingScannerView mScannerView;
    private KProgressHUD loadingWindow;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.scan_qr);

        Intent intent = getIntent();
        scenario = intent.getStringExtra(InputOptionActivity.EXTRA_MESSAGE_SCENARIO);
        uid = intent.getStringExtra(InputOptionActivity.EXTRA_MESSAGE_UID);

        // Request Camera permission
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 1400);

        // Show the Scanner view
        mScannerView = new ZXingScannerView(this);
        setContentView(mScannerView);

        // Internet connection handler
        final Handler handler = new Handler();
        Runnable runnable = new Runnable() {

            @Override
            public void run() {
                try {
                    if (isConnected(ScanQrActivity.this)) {
                        if (internetStatus) {
                            buildDialog(ScanQrActivity.this).show();
                            internetStatus = false;
                        }
                    } else {
                        internetStatus = true;
                    }
                } catch (Exception e) {
                    // TODO: handle exception
                } finally {
                    //also call the same runnable to call it at regular interval
                    handler.postDelayed(this, 1000);
                }
            }
        };
        handler.postDelayed(runnable, 1000);
    }


    @Override
    protected void onResume() {
        super.onResume();

        mScannerView.setResultHandler(this); // Register ourselves as a handler for scan results.
        mScannerView.startCamera();          // Start camera on resume
    }

    // Stop the Camera when move to another Activity
    @Override
    public void onPause() {
        super.onPause();
        mScannerView.stopCamera();
        sessionHandler = 0;
    }

    // After QR code detected
    @Override
    public void handleResult(Result rawResult) {
        // In one process only have one session to scan the QR Code
        if (sessionHandler == 0) {
            // Set loading window
            loadingWindow = KProgressHUD.create(ScanQrActivity.this)
                    .setStyle(KProgressHUD.Style.SPIN_INDETERMINATE)
                    .setBackgroundColor(Color.parseColor("#508AF1F7"))
                    .setLabel(getResources().getString(R.string.loading_label_please_wait))
                    .setDetailsLabel(getResources().getString(R.string.loading_details_finding_id))
                    .setCancellable(true)
                    .setAnimationSpeed(2)
                    .setDimAmount(0.5f)
                    .show();

            checkDatabase(rawResult.getText());
            sessionHandler = 1;
        }

        mScannerView.resumeCameraPreview(this);
    }

    // Search the entered ID to the database
    private void checkDatabase(String idcode) {

        id = idcode;
        id = id.replaceAll("\\.", "");
        id = id.replaceAll("#", "");
        id = id.replaceAll("\\$", "");
        id = id.replaceAll("\\[", "");
        id = id.replaceAll("]", "");

        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference().child("trackingSession");
        rootRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.hasChild(id)) {
                    if (scenario.equals("viewer")) {
                        loadingWindow.dismiss();//dismiss window
                        Intent intent = new Intent(ScanQrActivity.this, TrackingActivity.class);
                        intent.putExtra(EXTRA_MESSAGE_ID, id);
                        startActivity(intent);
                    } else if (scenario.equals("appointment")) {
                        // cek apakah sudah pernah buat geofence
                        loadingWindow.dismiss();//dismiss window
                        //Intent intent = new Intent(ScanQrActivity.this, AppointmentActivity.class);
                        Intent intent = new Intent(ScanQrActivity.this, InputRouteActivity.class);
                        intent.putExtra(EXTRA_MESSAGE_ID, id);
                        intent.putExtra(EXTRA_MESSAGE_UID, uid);
                        startActivity(intent);
                    }
                } else {
                    Alerter.create(ScanQrActivity.this).setTitle(getResources().getString(R.string.alert_title_failed_find_id)).setText(getResources().getString(R.string.alert_msg_failed_find_id)).setBackgroundColorRes(R.color.colorAccent).show();

                    loadingWindow.dismiss();
                    sessionHandler = 0;
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    // Check internet connection
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
                if (isConnected(ScanQrActivity.this))

                    buildDialog(ScanQrActivity.this).show();
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
}
