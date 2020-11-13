package com.jady.jadytrack.activity;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;

import androidx.appcompat.app.AppCompatActivity;

import com.jady.jadytrack.R;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash_page);

        // Prepare the Notification Channels
        createChannels();

        // Handler to start the Menu-Activity and then close this Splash-Screen after some seconds.
        // The time to wait for splash screen is 1.25 seconds.
        int splashTime = 1250;
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                // Create an Intent that will start the Menu-Activity.
                Intent i = new Intent(getApplicationContext(), OnboardingActivity.class);
                startActivity(i);
                finish();
            }
        }, splashTime);
    }

    private void createChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            final String NOTIFICATION_CHANNEL_ID = "info_01";
            final String ALERT_CHANNEL_ID = "alert_01";

            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .build();
            Uri soundUri = Uri.parse("android.resource://" + getApplicationContext().getPackageName() + "/" + R.raw.notification_info);

            NotificationChannel trackingChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID,
                    "Info Notification",
                    NotificationManager.IMPORTANCE_HIGH);
            trackingChannel.setDescription("Channel description");
            trackingChannel.enableLights(true);
            trackingChannel.setLightColor(Color.BLUE);
            trackingChannel.setVibrationPattern(new long[]{100, 200, 300, 400, 500, 400, 300, 200, 400});
            trackingChannel.enableVibration(true);
            trackingChannel.setSound(soundUri, audioAttributes);
            notificationManager.createNotificationChannel(trackingChannel);

            NotificationChannel broadcastingChannel = new NotificationChannel(ALERT_CHANNEL_ID,
                    "Alert Notification",
                    NotificationManager.IMPORTANCE_HIGH);
            broadcastingChannel.setDescription("Channel description");
            broadcastingChannel.enableLights(true);
            broadcastingChannel.setLightColor(Color.RED);
            broadcastingChannel.setVibrationPattern(new long[]{0, 1000, 500, 1000});
            broadcastingChannel.enableVibration(true);
            broadcastingChannel.setSound(soundUri, audioAttributes);
            notificationManager.createNotificationChannel(broadcastingChannel);
        }
    }

}
