package com.jady.jadytrack.activity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.getkeepsafe.taptargetview.TapTarget;
import com.getkeepsafe.taptargetview.TapTargetSequence;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.jady.jadytrack.R;
import com.kaopiz.kprogresshud.KProgressHUD;
import com.orhanobut.hawk.Hawk;

public class MainMenuActivity extends AppCompatActivity {

    // Message ID
    public static final String EXTRA_MESSAGE_NAME = "com.jady.jadytrack.SENDNAME";
    public static final String EXTRA_MESSAGE_EMAIL = "com.jady.jadytrack.SENDEMAIL";
    public static final String EXTRA_MESSAGE_UID = "com.jady.jadytrack.SENDUID";
    public static final String EXTRA_MESSAGE_SCENARIO = "com.jady.jadytrack.SENDSCENARIO";

    private static final String TAG = "EmailPassword";

    private TextView textViewCurrentUser;
    private TextView textViewUserName;
    private String userName;
    private String userEmail;
    private Button buttonViewer, buttonTarget, buttonAppointment;
    private ImageButton buttonAboutPage;
    private String currentUserUID;
    private KProgressHUD loadingWindow;

    // Firebase authentication
    private FirebaseAuth mAuth;
    FirebaseDatabase databaseKu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home_page);
        textViewCurrentUser = (TextView) findViewById(R.id.textViewCurrentUser);
        textViewUserName = (TextView) findViewById(R.id.textViewUserName);
        ImageButton buttonLogout = (ImageButton) findViewById(R.id.buttonLogout);
        buttonViewer = (Button) findViewById(R.id.buttonViewer);
        buttonAboutPage = (ImageButton) findViewById(R.id.buttonAboutPage);
        buttonTarget = (Button) findViewById(R.id.buttonTarget);
        buttonAppointment = (Button) findViewById(R.id.buttonAppointment);
        // Disable the important buttons by default:
        buttonViewer.setEnabled(false);
        buttonAboutPage.setEnabled(false);
        buttonTarget.setEnabled(false);
        buttonAppointment.setEnabled(false);
        // Set loading window
        loadingWindow = KProgressHUD.create(MainMenuActivity.this)
                .setStyle(KProgressHUD.Style.SPIN_INDETERMINATE)
                .setBackgroundColor(Color.parseColor("#508AF1F7"))
                .setLabel("Please wait")
                .setDetailsLabel("Downloading data")
                .setCancellable(true)
                .setAnimationSpeed(2)
                .setDimAmount(0.5f)
                .show();

        //-----------FIREBASE STUFF HERE-----------//
        //Firebase Realtime Database//
        databaseKu = FirebaseDatabase.getInstance();
        //Firebase auth//
        mAuth = FirebaseAuth.getInstance();
        //-----------------------------------------//

        buttonLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                backDialog(MainMenuActivity.this).show();

            }
        });
        buttonAboutPage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(getApplicationContext(), AboutActivity.class);
                i.putExtra(EXTRA_MESSAGE_NAME, userName);
                i.putExtra(EXTRA_MESSAGE_EMAIL, userEmail);
                startActivity(i);
            }
        });
        buttonViewer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(getApplicationContext(), InputOptionActivity.class);
                i.putExtra(EXTRA_MESSAGE_SCENARIO, "viewer");
                startActivity(i);
            }
        });
        buttonTarget.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(getApplicationContext(), BroadcastActivity.class);
                i.putExtra(EXTRA_MESSAGE_NAME, userName);
                i.putExtra(EXTRA_MESSAGE_UID, currentUserUID);
                startActivity(i);
            }
        });
        buttonAppointment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(getApplicationContext(), InputOptionActivity.class);
                i.putExtra(EXTRA_MESSAGE_NAME, userName);
                i.putExtra(EXTRA_MESSAGE_UID, currentUserUID);
                i.putExtra(EXTRA_MESSAGE_SCENARIO, "appointment");
                startActivity(i);
            }
        });
    }

    // This is an alert dialog that will be shown when the user want to logout
    public AlertDialog.Builder backDialog(Context c) {
        AlertDialog.Builder builder = new AlertDialog.Builder(c);
        builder.setTitle("Logout");
        builder.setMessage("Do you really want to Logout?");
        builder.setCancelable(false);
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                signOut();
            }
        });
        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        return builder;
    }

    @Override
    public void onResume() {
        super.onResume();

        // Enable the tutorial if already loaded
        // ---HAWK set SkipOnboarding to true if the user is logged in---//
        Boolean skipMainMenuTutorial = Hawk.get("skipMainMenuTutorial");
        // END OF: HAWK stuff james
        if (skipMainMenuTutorial == null) {
            launchMainMenuTutorial();
            Boolean skipMainMenuTemp = true;
            Hawk.put("skipMainMenuTutorial", skipMainMenuTemp);
        } else {
            // If skipOnboarding == false then launch tutorial again.
            if (!skipMainMenuTutorial) {
                launchMainMenuTutorial();
                Boolean skipMainMenuTemp = true;
                Hawk.put("skipMainMenuTutorial", skipMainMenuTemp);
            }
        }

    }

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(getApplicationContext(), "Not logged in.", Toast.LENGTH_SHORT).show();
        } else {
            currentUserUID = currentUser.getUid();

            //----CREATE A LISTENER FOR Name of User----
            databaseKu.getReference().child("users").child(currentUser.getUid()).child("name").addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot!= null) {
                        userName = dataSnapshot.getValue().toString();
                        //Toast.makeText(getApplicationContext(), "Welcome :" + userName, Toast.LENGTH_SHORT).show();
                        textViewUserName.setText(userName);
                        //Enable buttons if username is loaded
                        buttonViewer.setEnabled(true);
                        buttonAboutPage.setEnabled(true);
                        buttonTarget.setEnabled(true);
                        buttonAppointment.setEnabled(true);
                        loadingWindow.dismiss();
                        //---------Main Menu stuff to do after loading is complete---------//
                        //---HAWK set SkipOnboarding to true if the user is logged in---//
                        Boolean skipMainMenuTutorial = Hawk.get("skipMainMenuTutorial");
                        //END OF: HAWK stuff james
                        if (skipMainMenuTutorial == null) {
                            launchMainMenuTutorial();
                            Boolean skipMainMenuTemp = true;
                            Hawk.put("skipMainMenuTutorial", skipMainMenuTemp);
                        } else {
                            //If skipOnboarding==false then launch tutorial again.
                            if (!skipMainMenuTutorial) {
                                launchMainMenuTutorial();
                                Boolean skipMainMenuTemp = true;
                                Hawk.put("skipMainMenuTutorial", skipMainMenuTemp);
                            }
                        }

                        //END OF: Main Menu stuff to do after loading is complete---------//

                    }else {
                        //Disable buttons if the username is not loaded and is still null:
                        buttonViewer.setEnabled(false);
                        buttonAboutPage.setEnabled(false);
                        buttonTarget.setEnabled(false);
                        buttonAppointment.setEnabled(false);

                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Log.w(TAG, "Failed getting the user's name", databaseError.toException());
                    //Toast.makeText(getApplicationContext(), "Failed getting the user's name.", Toast.LENGTH_SHORT).show();
                }
            }); //END OF: Listen User's name
            userEmail = currentUser.getEmail();
            //Toast.makeText(getApplicationContext(), "Welcome " + userName + "\nEmail: " + currentUser.getEmail(), Toast.LENGTH_SHORT).show();
        }
        updateUI(currentUser);

    }//end of OnStart

    private void launchMainMenuTutorial() {
        //Tap Target methods (INTRO TUTORIAL PART)//
        new TapTargetSequence(MainMenuActivity.this)
                .targets(
                        TapTarget.forView(findViewById(R.id.buttonAppointment), "Appointment ", "Create and setup the geofence and destination for your target's trip here!").outerCircleColor(R.color.jamesBlue).tintTarget(false),
                        TapTarget.forView(findViewById(R.id.buttonTarget), "Target ", "You can broadcast your location as well as share your tracking session to create appointments!").outerCircleColor(R.color.jamesBlue).tintTarget(false),
                        TapTarget.forView(findViewById(R.id.buttonViewer), "Viewer ", "You can view your target by scanning the QR Code or tracking ID here!").outerCircleColor(R.color.jamesBlue).tintTarget(false),
                        TapTarget.forView(findViewById(R.id.buttonLogout), "Logout ", "You can logout and switch accounts here!").outerCircleColor(R.color.jamesBlue),
                        TapTarget.forView(findViewById(R.id.buttonAboutPage), "Help ", "Your help menu is here! Pressing the help menu will reset the quick tutorial. You can also contact us through the contact form inside").outerCircleColor(R.color.jamesBlue))
                .start();
        //END OF: Tap Target methods (INTRO TUTORIAL PART)//
    }
    public void updateUI(FirebaseUser user) {
        //hideProgressDialog();
        if (user != null) {
            textViewCurrentUser.setText(user.getEmail());
            textViewUserName.setText(userName);
        } else {
            textViewCurrentUser.setText("Not logged in");
            //Change the Activity to Loggin/Register Screen if the user is not logged in to the app.
            Intent i = new Intent(this, MainActivity.class);
            // set the new task and clear flags
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); //Clear the previous activities (so that when the user press back, the user will not be brought to the login/register screen)
            startActivity(i);
        }
    }

    private void signOut() {
        mAuth.signOut();
        updateUI(null);

    }

    @Override
    public void onBackPressed() {
        gobackDialog(MainMenuActivity.this).show();
    }

    public AlertDialog.Builder gobackDialog(Context c) {
        AlertDialog.Builder builder = new AlertDialog.Builder(c);
        builder.setTitle("Quit Application");
        builder.setMessage("Do you really want to quit from the application?");
        builder.setCancelable(false);
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finishAffinity();
            }
        });
        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        return builder;
    }
}
