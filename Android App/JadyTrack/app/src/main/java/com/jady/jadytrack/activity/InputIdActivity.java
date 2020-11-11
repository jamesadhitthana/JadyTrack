package com.jady.jadytrack.activity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

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
    private String id;
    private String scenario;
    private String uid;

    // Handler
    public boolean internetStatus = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.input_id);

        // To get the messages from the previous Activity
        Intent intent = getIntent();
        scenario = intent.getStringExtra(InputOptionActivity.EXTRA_MESSAGE_SCENARIO);
        uid = intent.getStringExtra(InputOptionActivity.EXTRA_MESSAGE_UID);

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
        id = targetId.getText().toString();

        if (id.isEmpty()) {
            Alerter.create(InputIdActivity.this).setTitle("Tracking ID").setText("Please input the tracking ID").setBackgroundColorRes(R.color.colorAccent).show();
        } else {
            DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference().child("trackingSession");
            rootRef.addListenerForSingleValueEvent(new ValueEventListener() {
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
                        Alerter.create(InputIdActivity.this).setTitle("Failed to find ID").setText("ID does not exist").setBackgroundColorRes(R.color.colorAccent).show();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        }

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
        builder.setTitle("No Internet Connection");
        builder.setMessage("You need to have Mobile Data or wifi to access this. Press Try Again or Exit");
        builder.setCancelable(false);

        builder.setPositiveButton("Try Again", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {

                dialog.cancel();
                if (isConnected(InputIdActivity.this)) {
                    buildDialog(InputIdActivity.this).show();
                }
            }
        });

        builder.setNegativeButton("Exit", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {

                finish();
                moveTaskToBack(true);
            }
        });

        return builder;
    }
}
