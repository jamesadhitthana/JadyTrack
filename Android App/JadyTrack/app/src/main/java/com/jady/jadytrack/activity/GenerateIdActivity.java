package com.jady.jadytrack.activity;

import android.content.Intent;
import android.graphics.Bitmap;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.jady.jadytrack.R;

import net.glxn.qrgen.android.QRCode;

public class GenerateIdActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.generate_id);

        Intent intent = getIntent();
        final String id = intent.getStringExtra(BroadcastActivity.EXTRA_MESSAGE_ID);

        // The TextView to show your Text
        TextView showText = (TextView) findViewById(R.id.trackingidView);
        showText.setText(id);
        showText.setTextIsSelectable(true);

        Bitmap myBitmap= QRCode.from(id).bitmap();
        ImageView myImage = (ImageView) findViewById(R.id.qrView);
        myImage.setImageBitmap(myBitmap);

        // Button to share ID
        Button shareButton = (Button) findViewById(R.id.shareButton);
        shareButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //Share button functionality
                Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND); //Intent to share
                sharingIntent.setType("text/plain");//Set type of shared data to text
                //shared string
                String trackingSessionID = id;

                sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "JadyTrack Tracking Session ID");
                sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, trackingSessionID);


                startActivity(Intent.createChooser(sharingIntent, "Share Tracking Session ID via"));
            }

        });
    }
}
