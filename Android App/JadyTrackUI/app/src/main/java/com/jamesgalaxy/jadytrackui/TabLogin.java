package com.jamesgalaxy.jadytrackui;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class TabLogin extends Fragment {
    private EditText loginEmail;
    private EditText loginPassword;
    private Button buttonLogin;
    String userEmail, userPassword;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View currentView = inflater.inflate(R.layout.tab_login,container,false);//set the current view to the appropriate xml file

        buttonLogin = (Button) currentView.findViewById(R.id.buttonLogin);
        loginEmail = (EditText)currentView.findViewById( R.id.loginEmail );
        loginPassword = (EditText) currentView.findViewById( R.id.loginPassword );
        buttonLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                userEmail = loginEmail.getText().toString().trim();
                userPassword = loginPassword.getText().toString();

                if (!userEmail.isEmpty() && !userPassword.isEmpty()) {
                    ((MainActivity)getActivity()).signIn(userEmail,userPassword); //Call parent Activity's Method to sign in


                } else {
                    Toast.makeText(getActivity(), "Password or email is empty", Toast.LENGTH_SHORT).show();
                }



            }
        });


        return currentView;
    }
}