package com.jady.jadytrack.activity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.jady.jadytrack.R;
import com.tapadoo.alerter.Alerter;

public class InputIdActivity extends AppCompatActivity {

    public static final String EXTRA_MESSAGE_ID = "com.jady.jadytrack.SENDID";
    public static final String EXTRA_MESSAGE_UID = "com.jady.jadytrack.SENDUID";

    // Attribute
    private String receivedTrackingId;
    private String id;
    private String scenario;
    private String uid;

    // Handler
    public boolean internetStatus = true;

    //Firebase database activity
    private DatabaseReference shortTrackingIdDatabaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.input_id);

        //Database Activity
        shortTrackingIdDatabaseReference = FirebaseDatabase.getInstance().getReference().child("shortTrackingSession");

        // To get the messages from the previous Activity
        Intent intent = getIntent();
        scenario = intent.getStringExtra(InputOptionActivity.EXTRA_MESSAGE_SCENARIO);
        uid = intent.getStringExtra(InputOptionActivity.EXTRA_MESSAGE_UID);

        //*Back Button//
        final ImageButton buttonBack = (ImageButton) findViewById(R.id.buttonBack);
        buttonBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        //END OF: Back Button--//

        // Button to begin tracking
        Button trackingButton = (Button) findViewById(R.id.trackButton);
        trackingButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                checkDatabase();
            }
        });

        // Internet connection handler
        final Handler handler = new Handler();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    if (isConnected(InputIdActivity.this)) {
                        if (internetStatus) {
                            buildDialog(InputIdActivity.this).show();
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


    private void checkDatabase() {

        EditText targetId = (EditText) findViewById(R.id.inputId);
        receivedTrackingId = targetId.getText().toString();

        if (receivedTrackingId.isEmpty()) {
            Alerter.create(InputIdActivity.this).setTitle(getResources().getString(R.string.alert_title_input_tracking_id)).setText(getResources().getString(R.string.alert_msg_input_tracking_id)).setBackgroundColorRes(R.color.colorAccent).show();
        } else {

            //*--Check and Convert shortTrackingID to fullTrackingID--*//
            shortTrackingIdDatabaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    //* If shortTrackingId exists, then get the full tracking Id
                    if (snapshot.hasChild(receivedTrackingId)) {
                        //*Replace global trackingId with fullTrackingId from database
                        id = snapshot.child(receivedTrackingId).getValue().toString();
//                        //*Update Labels:
//                        labelShortTrackingIdTextView.setText("(" + receivedTrackingId + ")");
//                        labelTrackingId.setText(id);
                        Toast.makeText(getApplicationContext(), getResources().getString(R.string.layout_short_tracking_id_updated_notif) + id, Toast.LENGTH_LONG).show();
                        Log.d("james", "Short tracking ID found! Setting id as the new FullTrackingId");
                    } else {
                        //*Replace global id with the initial receivedTrackingId
                        id = receivedTrackingId;
                        Log.d("james", "Short tracking ID not found! Setting id as receivedTrackingId");
                    }

                    //* Check if the fullTrackingId exists
                    checkTrackingIdExists(id);
                    //*---//
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
//                found[0] = false;
                    Log.d("james", "Canceled checking shortTrackingId");
                }
            });


        }

    }

    public void checkTrackingIdExists(final String id) {
        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference().child("trackingSession");
        rootRef.addListenerForSingleValueEvent(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.hasChild(id)) {
                            if (scenario.equals("viewer")) {
                                Intent intent = new Intent(InputIdActivity.this, TrackingActivity.class);
                                intent.putExtra(EXTRA_MESSAGE_ID, id);
                                startActivity(intent);
                            } else if (scenario.equals("appointment")) {

                                //Intent intent = new Intent(InputIdActivity.this, AppointmentActivity.class);
                                Intent intent = new Intent(InputIdActivity.this, InputRouteActivity.class);
                                intent.putExtra(EXTRA_MESSAGE_ID, id);
                                intent.putExtra(EXTRA_MESSAGE_UID, uid);
                                startActivity(intent);
                            }

                        } else {
                            Alerter.create(InputIdActivity.this).setTitle(getResources().getString(R.string.alert_title_failed_find_id)).setText(getResources().getString(R.string.alert_msg_failed_find_id)).setBackgroundColorRes(R.color.colorAccent).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
    }

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
                if (isConnected(InputIdActivity.this)) {
                    buildDialog(InputIdActivity.this).show();
                }
            }
        });

        builder.setNegativeButton(getResources().getString(R.string.button_exit), new DialogInterface.OnClickListener() {
//        builder.setNegativeButton("Exit", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {

                finish();
                moveTaskToBack(true);
            }
        });

        return builder;
    }
}
