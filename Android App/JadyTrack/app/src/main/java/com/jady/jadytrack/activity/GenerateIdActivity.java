package com.jady.jadytrack.activity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.jady.jadytrack.R;

import net.glxn.qrgen.android.QRCode;

public class GenerateIdActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.generate_id);

        Intent intent = getIntent();
        final String id = intent.getStringExtra(BroadcastActivity.EXTRA_MESSAGE_ID);
        final String shortTrackingId = intent.getStringExtra(BroadcastActivity.EXTRA_MESSAGE_SHORT_ID);

        //Show the shortTrackingId
        TextView showShortTrackingId = (TextView) findViewById(R.id.shortTrackingidView);
        showShortTrackingId.setText(shortTrackingId);
        showShortTrackingId.setTextIsSelectable(true);

        // Show the tracking ID
        TextView showText = (TextView) findViewById(R.id.trackingidView);
        showText.setText(id);
        showText.setTextIsSelectable(true);

        // Show the QR code
        Bitmap myBitmap = QRCode.from(id).bitmap();
        ImageView myImage = (ImageView) findViewById(R.id.qrView);
        myImage.setImageBitmap(myBitmap);

        //*Back Button//
        final ImageButton buttonBack = (ImageButton) findViewById(R.id.buttonBack);
        buttonBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        //END OF: Back Button--//

        // Button to share the tracking ID
        Button shareButton = (Button) findViewById(R.id.shareButton);
        shareButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
                // Set type to text
                sharingIntent.setType("text/plain");

                sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "JadyTrack Tracking Session ID");

                // Using short tracking id
                sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, shortTrackingId); //!DEPRECEATED
                //!DEPRECEATED: using full tracking id
                //sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, id); //!DEPRECEATED

                startActivity(Intent.createChooser(sharingIntent, "Share Tracking Session ID via"));
            }

        });

//        Button to share link
        Button shareLinkButton = (Button) findViewById(R.id.shareLinkButton);
        shareLinkButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                //*Using short tracking id
                String trackingLink = buildTrackingLink(shortTrackingId); //!DEPRECEATED

                //!DEPRECEATED: using full tracking id
                //String trackingLink = buildTrackingLink(id); //!DEPRECEATED

//                Toast.makeText(getApplicationContext(), "Hasil link nya: " + trackingLink, Toast.LENGTH_SHORT).show();
//                Sharing Intent
                Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
                // Set type to text
                sharingIntent.setType("text/plain");

                sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "JadyTrack Tracking Link");
                sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, trackingLink);

                startActivity(Intent.createChooser(sharingIntent, "Share JadyTrack Tracking Link via"));

            }
        });
    }

    private String buildTrackingLink(String trackingID) {
        String template = "https://myjadytrack.web.app/app/index.html?id="; //-L_tTv8ulabhy1vPOdo0
        return template += trackingID;

    }
}
