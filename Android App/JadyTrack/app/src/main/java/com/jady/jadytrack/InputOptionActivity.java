package com.jady.jadytrack;

import android.content.Intent;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class InputOptionActivity extends AppCompatActivity {

    // id message yang akan dipassing ke activity selanjutnya
    public static final String EXTRA_MESSAGE_NAME = "com.example.yeftaprototypev2.SENDNAME";
    public static final String EXTRA_MESSAGE_UID = "com.example.yeftaprototypev2.SENDUID";

    // scenario message yang akan dipassing ke activity selanjutnya
    public static final String EXTRA_MESSAGE_SCENARIO = "com.example.yeftaprototypev2.SENDSCENARIO";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_input_option);

        Intent intent = getIntent();
        final String scenario = intent.getStringExtra(MainMenu.EXTRA_MESSAGE_SCENARIO);
        final String uid = intent.getStringExtra(MainMenu.EXTRA_MESSAGE_UID);

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
