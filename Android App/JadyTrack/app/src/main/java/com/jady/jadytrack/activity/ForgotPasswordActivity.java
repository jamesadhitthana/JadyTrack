package com.jady.jadytrack.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.jady.jadytrack.R;
import com.tapadoo.alerter.Alerter;

public class ForgotPasswordActivity extends AppCompatActivity {
    private EditText loginEmail;
    private Button buttonLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);
//        ----//

        loginEmail = (EditText) findViewById(R.id.loginEmail);
        buttonLogin = (Button) findViewById(R.id.buttonLogin);

        //*Back Button//
        final ImageButton buttonBack = (ImageButton) findViewById(R.id.buttonBack);
        buttonBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        //END OF: Back Button--//

        //--//
        buttonLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loginEmail.setEnabled(false);
                buttonLogin.setEnabled(false);
                requestResetPasswordEmail(loginEmail.getText().toString());
            }
        });

    }

    public void requestResetPasswordEmail(String emailAddress) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
//        String emailAddress = "user@example.com";

        auth.sendPasswordResetEmail(emailAddress)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Log.d("jamesResetPassword", "Email sent.");
                            Alerter.create(ForgotPasswordActivity.this).setTitle(getResources().getString(R.string.forget_password_notif)).setText(getResources().getString(R.string.forget_password_notif_desc)).setBackgroundColorRes(R.color.colorAccent).show();
                        } else {
                            loginEmail.setEnabled(true);
                            buttonLogin.setEnabled(true);
                            Alerter.create(ForgotPasswordActivity.this).setTitle(getResources().getString(R.string.forget_password_error)).setText(getResources().getString(R.string.forget_password_error_desc)).setBackgroundColorRes(R.color.colorAccent).show();
                            Log.d("jamesResetPassword", "Error gan.");
                        }
                    }
                });
    }
}