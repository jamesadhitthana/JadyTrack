package com.jamesgalaxy.jadytrackui;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;

import com.orhanobut.hawk.Hawk;
import com.ramotion.paperonboarding.PaperOnboardingEngine;
import com.ramotion.paperonboarding.PaperOnboardingPage;
import com.ramotion.paperonboarding.listeners.PaperOnboardingOnRightOutListener;

import java.util.ArrayList;

import androidx.appcompat.app.AppCompatActivity;

public class Onboarding extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.onboarding_main_layout);

        //--------------Shared Preference--------------
        //SharedPreference untuk Onboarding & Tutorial Auto Skip kalau udah login//
        Hawk.init(Onboarding.this).build(); //library SharedPreference yang lebih Gampang
        Boolean skipOnboarding = Hawk.get("skipOnboardingIfLoggedIn");
        //END OF: Shared Preference--------------

        if (skipOnboarding == null) {
            //If this value is still empty then revert to launching onboarding
            launchOnboarding();
        } else {//if the value is not empty then
            //If skipOnboarding==true then just launch next intent
            if (skipOnboarding == true) {
                Intent i = new Intent(Onboarding.this, MainActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                // set the new task and clear flags
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); //Clear the previous activities (so that when the user press back, the user will not be brought to the login/register screen)
                startActivity(i);
            }
            //If skipOnboarding==false then launch next intent
            if (skipOnboarding == false) {
                launchOnboarding();
            }
        }


    }//-------

    private void launchOnboarding() {
        PaperOnboardingEngine engine = new PaperOnboardingEngine(findViewById(R.id.onboardingRootView), getDataForOnboarding(), getApplicationContext());

        engine.setOnRightOutListener(new PaperOnboardingOnRightOutListener() {
            @Override
            public void onRightOut() {
                Intent i = new Intent(Onboarding.this, MainActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                // set the new task and clear flags
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); //Clear the previous activities (so that when the user press back, the user will not be brought to the login/register screen)
                startActivity(i);
            }
        });
    }

    // Onboarding Data
    private ArrayList<PaperOnboardingPage> getDataForOnboarding() {
        // Onboarding Contents

        PaperOnboardingPage scr1 = new PaperOnboardingPage(getResources().getString(R.string.onboarding_1_titleText), getResources().getString(R.string.onboarding_1_descriptionText),
                Color.parseColor("#6E8CA0"), R.drawable.onboarding_jadylogo, R.drawable.onboarding_jadylogo);

        PaperOnboardingPage scr2 = new PaperOnboardingPage(getResources().getString(R.string.onboarding_2_titleText), getResources().getString(R.string.onboarding_2_descriptionText),
                Color.parseColor("#D97D54"), R.drawable.onboarding_binoc, R.drawable.onboarding_binoc);

        PaperOnboardingPage scr3 = new PaperOnboardingPage(getResources().getString(R.string.onboarding_3_titleText), getResources().getString(R.string.onboarding_3_descriptionText),
                Color.parseColor("#87BCBF"), R.drawable.onboarding_travel, R.drawable.onboarding_travel);

        PaperOnboardingPage scr4 = new PaperOnboardingPage(getResources().getString(R.string.onboarding_4_titleText), getResources().getString(R.string.onboarding_4_descriptionText),
                Color.parseColor("#B1B0D1"), R.drawable.onboarding_sos, R.drawable.onboarding_sos);

        PaperOnboardingPage scr5 = new PaperOnboardingPage(getResources().getString(R.string.onboarding_5_titleText), getResources().getString(R.string.onboarding_5_descriptionText),
                Color.parseColor("#D5A8A6"), R.drawable.onboarding_check_in, R.drawable.onboarding_check_in);

        PaperOnboardingPage scr6 = new PaperOnboardingPage(getResources().getString(R.string.onboarding_6_titleText), getResources().getString(R.string.onboarding_6_descriptionText),
                Color.parseColor("#B2B694"), R.drawable.onboarding_notification, R.drawable.onboarding_notification);


        ArrayList<PaperOnboardingPage> elements = new ArrayList<>();
        elements.add(scr1);
        elements.add(scr2);
        elements.add(scr3);
        elements.add(scr4);
        elements.add(scr5);
        elements.add(scr6);
        return elements;
    }
}
