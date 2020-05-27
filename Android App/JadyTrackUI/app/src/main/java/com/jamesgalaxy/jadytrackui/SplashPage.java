package com.jamesgalaxy.jadytrackui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import androidx.appcompat.app.AppCompatActivity;

public class SplashPage extends AppCompatActivity {

    private int splashTime = 1250; //Time to wait for splash screen (1.25 seconds)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash_page);


        /* Handler to start the Menu-Activity and then close this Splash-Screen after some seconds.*/
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                /* Create an Intent that will start the Menu-Activity. */
                Intent i = new Intent(getApplicationContext(), Onboarding.class);
                startActivity(i);
                finish();
            }
        }, splashTime);
    }

}
