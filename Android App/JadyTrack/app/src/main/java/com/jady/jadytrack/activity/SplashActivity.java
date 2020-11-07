package com.jady.jadytrack.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import androidx.appcompat.app.AppCompatActivity;

import com.jady.jadytrack.R;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash_page);

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

}
