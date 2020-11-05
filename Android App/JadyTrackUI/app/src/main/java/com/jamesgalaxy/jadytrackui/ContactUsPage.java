package com.jamesgalaxy.jadytrackui;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.tapadoo.alerter.Alerter;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import static com.jamesgalaxy.jadytrackui.AboutPage.EXTRA_MESSAGE_EMAIL;
import static com.jamesgalaxy.jadytrackui.AboutPage.EXTRA_MESSAGE_NAME;

public class ContactUsPage extends AppCompatActivity {
    private TextView titleContactUs;
    private TextView jadyEmail;
    private TabLayout tabLayout;
    private String name;
    private String email;
    private EditText message;
    private Button buttonSendMessage;
    String databaseParentPath;
    private String nameContact;
    private String emailContact;
    private String messageContact;
    FirebaseDatabase databaseKu;
    DatabaseReference databaseUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.contact_us_page);
        titleContactUs = (TextView) findViewById(R.id.titleContactUs);
        jadyEmail = (TextView) findViewById(R.id.jadyEmail);
        tabLayout = (TabLayout) findViewById(R.id.tab_layout);

        Intent intent = getIntent();
        name = intent.getStringExtra(EXTRA_MESSAGE_NAME);
        email = intent.getStringExtra(EXTRA_MESSAGE_EMAIL);
//        name = (EditText) findViewById(R.id.name);
//        email = (EditText) findViewById(R.id.email);
        message = (EditText) findViewById(R.id.message);
        buttonSendMessage = (Button) findViewById(R.id.buttonSendMessage);
        //-----------FIREBASE DATABASE-----------//
        //Firebase Realtime Database//
        databaseKu = FirebaseDatabase.getInstance();
        //-----------------------------------------//


        buttonSendMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Get field data and cleaning it
                nameContact = name.toString().trim();
                emailContact = email.toString().trim();
                messageContact = message.getText().toString().trim();

                if (!nameContact.isEmpty() && isValidEmail(emailContact) && !messageContact.isEmpty()) {
                    //send message functionality
                    sendContactUsMessage();
                } else {
                    Alerter.create(ContactUsPage.this).setTitle("Oh no!").setText("Make sure all your fields are filled properly").setBackgroundColorRes(R.color.colorAccent).show();

                }
            }
        });

    }

    public static boolean isValidEmail(CharSequence target) {
        return (!TextUtils.isEmpty(target) && Patterns.EMAIL_ADDRESS.matcher(target).matches());
    }

    public void sendContactUsMessage() {
        databaseUser = databaseKu.getReference("contactUs");
        String key = databaseUser.push().getKey();
        ContactMessage msg = new ContactMessage(nameContact, emailContact, messageContact);
        databaseUser.child(key).setValue(msg)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Toast.makeText(getApplicationContext(), "Your message was successfully sent. \nWe will respond to your message as soon as we can!", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Alerter.create(ContactUsPage.this).setTitle("Failed to send message!").setText("Please check your internet connection").setBackgroundColorRes(R.color.colorAccent).show();
                    }
                });

    }
}






















