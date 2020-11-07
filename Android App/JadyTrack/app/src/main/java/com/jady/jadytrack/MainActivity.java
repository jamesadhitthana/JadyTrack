package com.jady.jadytrack;

import android.content.Intent;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.google.android.material.tabs.TabLayout;
import androidx.viewpager.widget.ViewPager;

import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.orhanobut.hawk.Hawk;
import com.tapadoo.alerter.Alerter;

public class MainActivity extends AppCompatActivity {
    private TextView title1;
    private TextView title2;
    private TextView description1;
    private TextView description2;
    private TextView garisTemporary;
    String databaseParentPath;
    FirebaseDatabase databaseKu;
    DatabaseReference databaseUser;
    private static final String TAG = "EmailPassword";
    //---Firebase Stuff:-----------------------//
    //Firebase authentication
    private FirebaseAuth mAuth;

    //END OF: Firebase Stuff ---//


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.register_account);

        title1 = (TextView)findViewById( R.id.title1 );
        title2 = (TextView)findViewById( R.id.title2 );
        description1 = (TextView)findViewById( R.id.description1 );
        description2 = (TextView)findViewById( R.id.description2 );
        garisTemporary = (TextView)findViewById( R.id.garisTemporary );

    //---SETUP TABS---//
        TabLayout tabLayout = (TabLayout) findViewById(R.id.tab_layout);
        tabLayout.addTab(tabLayout.newTab().setText("Sign Up"));
        tabLayout.addTab(tabLayout.newTab().setText("Log In"));
        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);

        final ViewPager viewPager = (ViewPager) findViewById(R.id.pager);
        final PagerAdapter adapter = new PagerAdapter
                (getSupportFragmentManager(), tabLayout.getTabCount());
        viewPager.setAdapter(adapter);
        viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
        tabLayout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                viewPager.setCurrentItem(tab.getPosition());
                if(tab.getPosition()==0) {
                    title1.setText("Create a");
                    title2.setText("New Account");
                    description1.setText("For the best experience");
                    description2.setText("with JadyTrack");
                }else {
                    title1.setText("Login to");
                    title2.setText("Your Account");
                    description1.setText("Your beloved one is important");
                    description2.setText("so is your account.");
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            //empty -james
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            //empty -james
            }
        });
    //END OF: SETUP TABS----//


        //-----------FIREBASE STUFF HERE-----------//
        //Firebase Realtime Database//
        databaseParentPath = "androidJames";
        databaseKu = FirebaseDatabase.getInstance();
        //Firebase auth//
        mAuth = FirebaseAuth.getInstance();
        //-----------------------------------------//

    }//END OF onCreate





    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            Alerter.create(MainActivity.this).setTitle("Login to Access JadyTrack").setText("You are currently not logged in").setBackgroundColorRes(R.color.colorAccent).show();
        } else {
            //Toast.makeText(getApplicationContext(), "Welcome " + currentUser.getEmail(), Toast.LENGTH_SHORT).show();
            Log.d(TAG, "signIn:" + currentUser.getEmail());
        }

        updateUI(currentUser);

    }//end of OnStart


    public void signIn(String email, String password) {
        Log.d(TAG, "signIn:" + email);
        Alerter.create(MainActivity.this).setText("Logging in...").setBackgroundColorRes(R.color.colorAccent).show();

        //showProgressDialog();

        // [START sign_in_with_email]
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "signInWithEmail:success");
                            Toast.makeText(getApplicationContext(), "Logged in as " + mAuth.getCurrentUser().getEmail(),
                                    Toast.LENGTH_SHORT).show(); //diemin aja
                            FirebaseUser user = mAuth.getCurrentUser();
                            updateUI(user);

                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "signInWithEmail:failure", task.getException());
                            Alerter.create(MainActivity.this).setTitle("Login Failed").setText(task.getException().getMessage()).setBackgroundColorRes(R.color.colorAccent).show();
                            updateUI(null);
                        }

                        // [START_EXCLUDE]
                        if (!task.isSuccessful()) {
                            Alerter.create(MainActivity.this).setTitle("Authentication Failed").setText(task.getException().getMessage()).setBackgroundColorRes(R.color.colorAccent).show();
                        }

                        // [END_EXCLUDE]
                    }
                });
        // [END sign_in_with_email]
    }

    public void createAccount(final String name, final String email, final String password) {
        Log.d(TAG, "createAccount:" + email);
        Alerter.create(MainActivity.this).setTitle("Creating Account").setText("Setting up your account...").setBackgroundColorRes(R.color.colorAccent).show();

        // [START create_user_with_email]
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "createUserWithEmail:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            updateUI(user);//Auto flag Login After SignUp

                            databaseUser = databaseKu.getReference("users");
                            User registeredUser = new User(name,email,password);
                            databaseUser.child(user.getUid()).setValue(registeredUser);

                        } else {
                            // If register fails, display a message to the user.
                            Log.w(TAG, "createUserWithEmail:failure", task.getException());
                            Alerter.create(MainActivity.this).setTitle("Failed to Register").setText(task.getException().getMessage()).setBackgroundColorRes(R.color.colorAccent).show();
                            updateUI(null);
                        }

                    }
                });
        // [END create_user_with_email]
    }

    public void updateUI(FirebaseUser user) {
        //hideProgressDialog();
        if (user != null) {
            garisTemporary.setText("Current user: " + user.getEmail());

            //---------HAWK set SkipOnboarding to true if the user is logged in---------//
            Boolean skipOnboardingIfLoggedIn = true;
            Hawk.put("skipOnboardingIfLoggedIn", skipOnboardingIfLoggedIn);
            //END OF: HAWK stuff james

            //Change the Activity to Main Menu if user is logged in
                Intent i = new Intent(getApplicationContext(), MainMenu.class);
                // set the new task and clear flags
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); //Clear the previous activities (so that when the user press back, the user will not be brought to the login/register screen)
                startActivity(i);
        } else {
            //---------HAWK set SkipOnboarding to FALSE if the user is NOT logged in---------//
            Boolean skipOnboardingIfLoggedIn = false;
            Hawk.put("skipOnboardingIfLoggedIn", skipOnboardingIfLoggedIn);
            //---------HAWK set Home Tutorial to FALSE if the user is NOT logged in (MainActivity)---------//
            Boolean skipMainMenuTutorial = false;
            Hawk.put("skipMainMenuTutorial", skipMainMenuTutorial);
        }
    }
}
