package com.jady.jadytrack.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.jady.jadytrack.R;
import com.orhanobut.hawk.Hawk;

import androidx.appcompat.app.AppCompatActivity;

public class AboutActivity extends AppCompatActivity {

    public static final String EXTRA_MESSAGE_NAME = "com.jady.jadytrack.SENDNAME";
    public static final String EXTRA_MESSAGE_EMAIL = "com.jady.jadytrack.SENDEMAIL";

    private String name;
    private String email;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.about_page);

        Intent intent = getIntent();
        name = intent.getStringExtra(MainMenuActivity.EXTRA_MESSAGE_NAME);
        email = intent.getStringExtra(MainMenuActivity.EXTRA_MESSAGE_EMAIL);

        // Reset tutorial in main menu
        Boolean skipMainMenuTutorial = false;
        Hawk.put("skipMainMenuTutorial", skipMainMenuTutorial);

        Button buttonContact = (Button) findViewById(R.id.buttonContact);
        buttonContact.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(getApplicationContext(), ContactUsActivity.class);
                i.putExtra(EXTRA_MESSAGE_NAME, name);
                i.putExtra(EXTRA_MESSAGE_EMAIL, email);
                startActivity(i);
            }
        });

        Button buttonManual = (Button) findViewById(R.id.buttonManual);
        buttonManual.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent browserIntent = new Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://firebasestorage.googleapis.com/v0/b/myjadytrack.appspot.com/o/AndroidUserManual.pdf?alt=media&token=e44a6b4a-d3be-4f0e-bf11-69f6343030b9"));
                startActivity(browserIntent);
            }
        });
    }
}
