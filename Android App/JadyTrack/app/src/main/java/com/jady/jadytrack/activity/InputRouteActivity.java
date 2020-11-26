package com.jady.jadytrack.activity;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.TimeZone;

public class InputRouteActivity extends AppCompatActivity {

    public static final String EXTRA_MESSAGE_ID = "com.jady.jadytrack.SENDID";
    public static final String EXTRA_MESSAGE_UID = "com.jady.jadytrack.SENDUID";

    private String id;
    private String userUID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.input_route);

        Intent intentKu = getIntent();
        id = intentKu.getStringExtra(InputIdActivity.EXTRA_MESSAGE_ID);
        id = intentKu.getStringExtra(ScanQrActivity.EXTRA_MESSAGE_ID);
        userUID = intentKu.getStringExtra(InputIdActivity.EXTRA_MESSAGE_UID);
        userUID = intentKu.getStringExtra(ScanQrActivity.EXTRA_MESSAGE_UID);
        //*Back Button//
        final ImageButton buttonBack = (ImageButton) findViewById(R.id.buttonBack);
        buttonBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        //END OF: Back Button--//

        final Button tombolQuickRoute = (Button) findViewById(R.id.quickRouteButton);
        tombolQuickRoute.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Intent intent = new Intent(InputRouteActivity.this, QuickRouteActivity.class);
                intent.putExtra(EXTRA_MESSAGE_ID, id);
                intent.putExtra(EXTRA_MESSAGE_UID, userUID);
                startActivity(intent);
            }
        });

        Button tombolNormalRoute = (Button) findViewById(R.id.normalRouteButton);
        tombolNormalRoute.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Intent intent = new Intent(InputRouteActivity.this, AppointmentActivity.class);
                intent.putExtra(EXTRA_MESSAGE_ID, id);
                intent.putExtra(EXTRA_MESSAGE_UID, userUID);
                startActivity(intent);
            }
        });

        DatabaseReference historyReference = FirebaseDatabase.getInstance().getReference().child("users/" + userUID + "/trackingHistory");
        final boolean[] isHistoryCollected = {false};

        historyReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.getValue() != null && !isHistoryCollected[0]) {

                    isHistoryCollected[0] = true;

                } else {
                    tombolQuickRoute.setEnabled(false);
                    tombolQuickRoute.getBackground().setColorFilter(Color.GRAY, PorterDuff.Mode.MULTIPLY);
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }

        });
    }
}
