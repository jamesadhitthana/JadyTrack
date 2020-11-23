package com.jady.jadytrack.activity;

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
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.jady.jadytrack.fragment.PagerAdapter;
import com.jady.jadytrack.R;
import com.jady.jadytrack.entity.User;
import com.orhanobut.hawk.Hawk;
import com.tapadoo.alerter.Alerter;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "EmailPassword";

    private TextView title1;
    private TextView title2;
    private TextView description1;
    private TextView description2;
    private TextView garisTemporary;
    private FirebaseDatabase databaseKu;
    private DatabaseReference databaseUser;

    // Firebase authentication
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.register_account);

        title1 = findViewById(R.id.title1);
        title2 = findViewById(R.id.title2);
        description1 = findViewById(R.id.description1);
        description2 = findViewById(R.id.description2);
        garisTemporary = findViewById(R.id.garisTemporary);

        //---SETUP TABS---//
        TabLayout tabLayout = findViewById(R.id.tab_layout);
        tabLayout.addTab(tabLayout.newTab().setText("Sign Up"));
        tabLayout.addTab(tabLayout.newTab().setText("Log In"));
        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);

        final ViewPager viewPager = findViewById(R.id.pager);
        final PagerAdapter adapter = new PagerAdapter
                (getSupportFragmentManager(), tabLayout.getTabCount());
        viewPager.setAdapter(adapter);
        viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                viewPager.setCurrentItem(tab.getPosition());
                if (tab.getPosition() == 0) {
                    title1.setText(getResources().getString(R.string.label_title_create_account_1));
                    title2.setText(getResources().getString(R.string.label_title_create_account_2));
                    description1.setText(getResources().getString(R.string.label_desc_create_account_1));
                    description2.setText(getResources().getString(R.string.label_desc_create_account_2));
                } else {
                    title1.setText(getResources().getString(R.string.label_title_login_1));
                    title2.setText(getResources().getString(R.string.label_title_login_2));
                    description1.setText(getResources().getString(R.string.label_desc_login_1));
                    description2.setText(getResources().getString(R.string.label_desc_login_2));
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });
        //END OF: SETUP TABS----//


        // -----------FIREBASE STUFF HERE-----------//
        // Firebase Realtime Database
        databaseKu = FirebaseDatabase.getInstance();
        // Firebase auth
        mAuth = FirebaseAuth.getInstance();
        // -----------------------------------------//

    }

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            Alerter.create(MainActivity.this).setTitle(getResources().getString(R.string.alert_title_not_logged_in)).setText(getResources().getString(R.string.alert_msg_not_logged_in)).setBackgroundColorRes(R.color.colorAccent).show();
        } else {
            Log.d(TAG, "signIn:" + currentUser.getEmail());
        }

        updateUI(currentUser);

    }

    public void signIn(String email, String password) {
        Log.d(TAG, "signIn:" + email);
        Alerter.create(MainActivity.this).setText(getResources().getString(R.string.alert_title_logging_in)).setBackgroundColorRes(R.color.colorAccent).show();

        // [START sign_in_with_email]
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "signInWithEmail:success");
                            Toast.makeText(getApplicationContext(), getResources().getString(R.string.alert_title_logged_in_as) + " " + mAuth.getCurrentUser().getEmail(),
                                    Toast.LENGTH_SHORT).show(); //diemin aja
                            FirebaseUser user = mAuth.getCurrentUser();
                            updateUI(user);

                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "signInWithEmail:failure", task.getException());
                            Alerter.create(MainActivity.this).setTitle(getResources().getString(R.string.alert_title_failed_login)).setText(task.getException().getMessage()).setBackgroundColorRes(R.color.colorAccent).show();
                            updateUI(null);
                        }

                        // [START_EXCLUDE]
                        if (!task.isSuccessful()) {
                            Alerter.create(MainActivity.this).setTitle(getResources().getString(R.string.alert_title_failed_authentication)).setText(task.getException().getMessage()).setBackgroundColorRes(R.color.colorAccent).show();
                        }
                        // [END_EXCLUDE]
                    }
                });
        // [END sign_in_with_email]
    }

    public void createAccount(final String name, final String email, final String password) {
        Log.d(TAG, "createAccount:" + email);
        Alerter.create(MainActivity.this).setTitle(getResources().getString(R.string.alert_title_creating_account)).setText(getResources().getString(R.string.alert_msg_creating_account)).setBackgroundColorRes(R.color.colorAccent).show();

        // [START create_user_with_email]
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "createUserWithEmail:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            // Auto flag Login After SignUp
                            updateUI(user);

//                            Write User data to DB
                            databaseUser = databaseKu.getReference("users");
                            User registeredUser = new User(name, email);//,password); //Nov 10 2020 (Password is now encrypted) using SCRYPT hashing
                            databaseUser.child(user.getUid()).setValue(registeredUser);

//                            Update ENCRYPTED User Display Name in Firebase Auth //TODO: This
                            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                    .setDisplayName(name) //user displayName
//                                    .setPhotoUri(Uri.parse("https://example.com/jane-q-user/profile.jpg")) //profile picture
                                    .build();

                            user.updateProfile(profileUpdates)
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if (task.isSuccessful()) {
                                                Log.d(TAG, "User profile updated.");
                                            }
                                        }
                                    });


                        } else {
                            // If register fails, display a message to the user.
                            Log.w(TAG, "createUserWithEmail:failure", task.getException());
                            Alerter.create(MainActivity.this).setTitle(getResources().getString(R.string.alert_title_failed_register)).setText(task.getException().getMessage()).setBackgroundColorRes(R.color.colorAccent).show();
                            updateUI(null);
                        }

                    }
                });
        // [END create_user_with_email]
    }

    public void updateUI(FirebaseUser user) {
        if (user != null) {
            garisTemporary.setText(user.getEmail());

            //---------HAWK set SkipOnboarding to true if the user is logged in---------//
            Hawk.put("skipOnboardingIfLoggedIn", true);
            // END OF: HAWK stuff james

            // Change the Activity to Main Menu if user is logged in
            Intent i = new Intent(getApplicationContext(), MainMenuActivity.class);
            // set the new task and clear flags
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); //Clear the previous activities (so that when the user press back, the user will not be brought to the login/register screen)
            startActivity(i);
        } else {
            //---------HAWK set SkipOnboarding to FALSE if the user is NOT logged in---------//
            Hawk.put("skipOnboardingIfLoggedIn", false);
            //---------HAWK set Home Tutorial to FALSE if the user is NOT logged in (MainActivity)---------//
            Hawk.put("skipMainMenuTutorial", false);
        }
    }

    public void startForgotPasswordIntent() {
        Intent i = new Intent(getApplicationContext(), ForgotPasswordActivity.class);
        startActivity(i);
    }
}
