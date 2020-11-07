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

public class TabSignUp extends Fragment {
    private EditText signUpName;
    private EditText signUpEmail;
    private EditText signUpPassword;
    private String userName, userEmail, userPassword;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Set the current view to the appropriate xml file
        View currentView = inflater.inflate(R.layout.tab_sign_up, container, false);
        signUpName = (EditText) currentView.findViewById(R.id.signUpName);
        signUpEmail = (EditText) currentView.findViewById(R.id.signUpEmail);
        signUpPassword = (EditText) currentView.findViewById(R.id.signUpPassword);
        Button buttonSignUp = (Button) currentView.findViewById(R.id.buttonSignUp);

        buttonSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                userName = signUpName.getText().toString().trim();
                userEmail = signUpEmail.getText().toString().trim();
                userPassword = signUpPassword.getText().toString();


                if (!userName.isEmpty() && !userEmail.isEmpty() && !userPassword.isEmpty()) {
                    // Call parent Activity's Method to create account
                    ((MainActivity) getActivity()).createAccount(userName, userEmail, userPassword);
                } else {
                    Toast.makeText(getActivity(), "Make sure all your fields are filled and that your password is at least 6 characters long", Toast.LENGTH_SHORT).show();
                }
            }
        });


        return currentView;
    }
}