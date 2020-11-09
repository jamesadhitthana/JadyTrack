package com.jady.jadytrack.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.jady.jadytrack.R;
import com.jady.jadytrack.activity.MainActivity;

public class TabLogin extends Fragment {
    private EditText loginEmail;
    private EditText loginPassword;
    private String userEmail, userPassword;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Set the current view to the appropriate xml file
        View currentView = inflater.inflate(R.layout.tab_login, container, false);

        Button buttonLogin = (Button) currentView.findViewById(R.id.buttonLogin);
        loginEmail = (EditText) currentView.findViewById(R.id.loginEmail);
        loginPassword = (EditText) currentView.findViewById(R.id.loginPassword);
        buttonLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                userEmail = loginEmail.getText().toString().trim();
                userPassword = loginPassword.getText().toString();

                if (!userEmail.isEmpty() && !userPassword.isEmpty()) {
                    // Call parent Activity's Method to sign in
                    ((MainActivity) getActivity()).signIn(userEmail, userPassword);
                } else {
                    Toast.makeText(getActivity(), getResources().getString(R.string.alert_title_password_email_empty), Toast.LENGTH_SHORT).show();
                }
            }
        });

        return currentView;
    }
}