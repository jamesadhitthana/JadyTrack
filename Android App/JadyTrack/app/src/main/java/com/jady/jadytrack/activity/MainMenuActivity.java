package com.jady.jadytrack.activity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.getkeepsafe.taptargetview.TapTarget;
import com.getkeepsafe.taptargetview.TapTargetSequence;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.jady.jadytrack.R;
import com.jady.jadytrack.UserAccountManagementActivity;
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

    //17 nov 2020
    private ImageView imageViewUserAccountManagement;

    // Firebase authentication
    private FirebaseAuth mAuth;
    FirebaseDatabase databaseKu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home_page);
        textViewCurrentUser = findViewById(R.id.textViewCurrentUser);
        textViewUserName = findViewById(R.id.textViewUserName);
        ImageButton buttonLogout = findViewById(R.id.buttonLogout);
        buttonViewer = findViewById(R.id.buttonViewer);
        buttonAboutPage = findViewById(R.id.buttonAboutPage);
        buttonTarget = findViewById(R.id.buttonTarget);
        buttonAppointment = findViewById(R.id.buttonAppointment);
        imageViewUserAccountManagement = findViewById(R.id.imageView2);
        // Disable the important buttons by default:
        buttonViewer.setEnabled(false);
        buttonAboutPage.setEnabled(false);
        buttonTarget.setEnabled(false);
        buttonAppointment.setEnabled(false);
        // Set loading window
        loadingWindow = KProgressHUD.create(MainMenuActivity.this)
                .setStyle(KProgressHUD.Style.SPIN_INDETERMINATE)
                .setBackgroundColor(Color.parseColor("#508AF1F7"))
                .setLabel(getResources().getString(R.string.loading_label_please_wait))
                .setDetailsLabel(getResources().getString(R.string.loading_details_downloading_data))
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
        imageViewUserAccountManagement.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(getApplicationContext(), UserAccountManagementActivity.class);
                i.putExtra(EXTRA_MESSAGE_NAME, userName);
                i.putExtra(EXTRA_MESSAGE_UID, currentUserUID);
                i.putExtra(EXTRA_MESSAGE_EMAIL, userEmail);
                startActivity(i);
            }
        });
    }

    // This is an alert dialog that will be shown when the user want to logout
    public AlertDialog.Builder backDialog(Context c) {
        AlertDialog.Builder builder = new AlertDialog.Builder(c);
        builder.setTitle(getResources().getString(R.string.alert_title_logout));
        builder.setMessage(getResources().getString(R.string.alert_msg_logout));
        builder.setCancelable(false);
        builder.setPositiveButton(getResources().getString(R.string.button_yes), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                signOut();
            }
        });
        builder.setNegativeButton(getResources().getString(R.string.button_no), new DialogInterface.OnClickListener() {

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

        if (currentUser != null) {
            currentUserUID = currentUser.getUid();

            //----CREATE A LISTENER FOR Name of User----
            databaseKu.getReference().child("users").child(currentUser.getUid()).child("name").addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot != null) {
                        userName = dataSnapshot.getValue().toString();
                        textViewUserName.setText(userName);
                        //Enable buttons if username is loaded
                        buttonViewer.setEnabled(true);
                        buttonAboutPage.setEnabled(true);
                        buttonTarget.setEnabled(true);
                        buttonAppointment.setEnabled(true);
                        loadProfilePhoto(); //Load the profile image
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

                    } else {
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
                }
            }); //END OF: Listen User's name
            userEmail = currentUser.getEmail();
        }
        updateUI(currentUser);

    }//end of OnStart

    private void launchMainMenuTutorial() {
        //Tap Target methods (INTRO TUTORIAL PART)//
        new TapTargetSequence(MainMenuActivity.this)
                .targets(
                        TapTarget.forView(findViewById(R.id.buttonAppointment), getResources().getString(R.string.tutorial_title_appointment), getResources().getString(R.string.tutorial_desc_appointment)).outerCircleColor(R.color.jamesBlue).tintTarget(false),
                        TapTarget.forView(findViewById(R.id.buttonTarget), getResources().getString(R.string.tutorial_title_target), getResources().getString(R.string.tutorial_desc_target)).outerCircleColor(R.color.jamesBlue).tintTarget(false),
                        TapTarget.forView(findViewById(R.id.buttonViewer), getResources().getString(R.string.tutorial_title_viewer), getResources().getString(R.string.tutorial_desc_viewer)).outerCircleColor(R.color.jamesBlue).tintTarget(false),
                        TapTarget.forView(findViewById(R.id.buttonLogout), getResources().getString(R.string.tutorial_title_logout), getResources().getString(R.string.tutorial_desc_logout)).outerCircleColor(R.color.jamesBlue),
                        TapTarget.forView(findViewById(R.id.buttonAboutPage), getResources().getString(R.string.tutorial_title_help), getResources().getString(R.string.tutorial_desc_help)).outerCircleColor(R.color.jamesBlue))
                .start();
        //END OF: Tap Target methods (INTRO TUTORIAL PART)//
    }

    public void updateUI(FirebaseUser user) {
        //hideProgressDialog();
        if (user != null) {
            textViewCurrentUser.setText(user.getEmail());
            textViewUserName.setText(userName);
        } else {
            textViewCurrentUser.setText(getResources().getString(R.string.label_title_not_logged_in));
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
        builder.setTitle(getResources().getString(R.string.alert_title_quit_application));
        builder.setMessage(getResources().getString(R.string.alert_msg_quit_application));
        builder.setCancelable(false);
        builder.setPositiveButton(getResources().getString(R.string.button_yes), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finishAffinity();
            }
        });
        builder.setNegativeButton(getResources().getString(R.string.button_no), new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        return builder;
    }

    //--Profile Photo--//
    private void loadProfilePhoto() {

        //Firebase storage//
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageReference = storage.getReference();
        StorageReference profilePhotoRef = storageReference.child("public/profilePhotos/" + currentUserUID); //Automatically get the profile photo from the user id

        profilePhotoRef.getBytes(1024 * 1024)
                .addOnSuccessListener(new OnSuccessListener<byte[]>() {
                    @Override
                    public void onSuccess(byte[] bytes) {
                        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                        imageViewUserAccountManagement.setImageBitmap(bitmap);
//                        Toast.makeText(getApplicationContext(), getResources().getString(R.string.layout_uam_image_loaded_success), Toast.LENGTH_LONG).show();
                        loadingWindow.dismiss();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(getApplicationContext(), getResources().getString(R.string.layout_uam_image_loaded_failure), Toast.LENGTH_LONG).show();
                        loadingWindow.dismiss();
                    }
                });
    }

}
