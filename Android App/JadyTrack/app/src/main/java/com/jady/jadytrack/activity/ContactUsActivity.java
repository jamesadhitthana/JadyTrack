package com.jady.jadytrack.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.jady.jadytrack.R;
import com.jady.jadytrack.entity.ContactMessage;
import com.tapadoo.alerter.Alerter;

import static com.jady.jadytrack.activity.AboutActivity.EXTRA_MESSAGE_EMAIL;
import static com.jady.jadytrack.activity.AboutActivity.EXTRA_MESSAGE_NAME;

public class ContactUsActivity extends AppCompatActivity {

    private String name;
    private String email;
    private EditText message;
    private String nameContact;
    private String emailContact;
    private String messageContact;
    private FirebaseDatabase databaseKu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.contact_us_page);

        Intent intent = getIntent();
        name = intent.getStringExtra(EXTRA_MESSAGE_NAME);
        email = intent.getStringExtra(EXTRA_MESSAGE_EMAIL);
        message = (EditText) findViewById(R.id.message);
        Button buttonSendMessage = (Button) findViewById(R.id.buttonSendMessage);
        // -----------FIREBASE DATABASE-----------
        // Firebase Realtime Database
        databaseKu = FirebaseDatabase.getInstance();
        // -----------------------------------------


        buttonSendMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Get field data and cleaning it
                nameContact = name.trim();
                emailContact = email.trim();
                messageContact = message.getText().toString().trim();

                if (!nameContact.isEmpty() && isValidEmail(emailContact) && !messageContact.isEmpty()) {
                    // Send message functionality
                    sendContactUsMessage();
                } else {
                    Alerter.create(ContactUsActivity.this).setTitle(getResources().getString(R.string.alert_title_failed_contact_us)).setText(getResources().getString(R.string.alert_msg_failed_contact_us)).setBackgroundColorRes(R.color.colorAccent).show();

                }
            }
        });

    }

    public static boolean isValidEmail(CharSequence target) {
        return (!TextUtils.isEmpty(target) && Patterns.EMAIL_ADDRESS.matcher(target).matches());
    }

    public void sendContactUsMessage() {
        DatabaseReference databaseUser = databaseKu.getReference("contactUs");
        String key = databaseUser.push().getKey();
        ContactMessage msg = new ContactMessage(nameContact, emailContact, messageContact);
        databaseUser.child(key).setValue(msg)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Toast.makeText(getApplicationContext(), getResources().getString(R.string.alert_title_contact_us), Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Alerter.create(ContactUsActivity.this).setTitle(getResources().getString(R.string.alert_title_failed_send_message)).setText(getResources().getString(R.string.alert_msg_failed_send_message)).setBackgroundColorRes(R.color.colorAccent).show();
                    }
                });

    }
}






















