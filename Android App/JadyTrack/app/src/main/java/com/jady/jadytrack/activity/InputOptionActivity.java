package com.jady.jadytrack.activity;

import android.content.Intent;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.jady.jadytrack.R;

public class InputOptionActivity extends AppCompatActivity {

    public static final String EXTRA_MESSAGE_NAME = "com.jady.jadytrack.SENDNAME";
    public static final String EXTRA_MESSAGE_UID = "com.jady.jadytrack.SENDUID";
    public static final String EXTRA_MESSAGE_SCENARIO = "com.jady.jadytrack.SENDSCENARIO";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.input_option);

        Intent intent = getIntent();
        final String scenario = intent.getStringExtra(MainMenuActivity.EXTRA_MESSAGE_SCENARIO);
        final String uid = intent.getStringExtra(MainMenuActivity.EXTRA_MESSAGE_UID);

        Button tombolScan = (Button) findViewById(R.id.scanqrButton);
        tombolScan.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Intent intent = new Intent(view.getContext(), ScanQrActivity.class);
                intent.putExtra(EXTRA_MESSAGE_NAME, "test");
                intent.putExtra(EXTRA_MESSAGE_SCENARIO, scenario);
                intent.putExtra(EXTRA_MESSAGE_UID, uid);
                startActivity(intent);
            }
        });
        Button tombolViewer = (Button) findViewById(R.id.inputidButton);
        tombolViewer.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Intent intent = new Intent(view.getContext(), InputIdActivity.class);
                intent.putExtra(EXTRA_MESSAGE_NAME, "test");
                intent.putExtra(EXTRA_MESSAGE_SCENARIO, scenario);
                intent.putExtra(EXTRA_MESSAGE_UID, uid);
                startActivity(intent);
            }
        });
    }
}
