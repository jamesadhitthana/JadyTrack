package com.jady.jadytrack.activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.jady.jadytrack.ImageResizer;
import com.jady.jadytrack.R;
import com.kaopiz.kprogresshud.KProgressHUD;
import com.tapadoo.alerter.Alerter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class UserAccountManagementActivity extends AppCompatActivity {

    private Boolean profilePhotoCompressionEnabled = true;
    private ImageView profilePic;
    private TextView textViewName;
    private TextView textViewEmail;
    private Uri imageUri;
    //Loading window loader
    private KProgressHUD loadingWindow;
    // Firebase authentication
    private FirebaseAuth mAuth;
    // Firebase storage
    private FirebaseStorage storage;
    private StorageReference storageReference;
    StorageReference profilePhotoRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //UserAccountManagement = UAM
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_account_management);
        //        --
        Intent intent = getIntent();
        String userName = intent.getStringExtra(MainMenuActivity.EXTRA_MESSAGE_NAME);
        final String userEmail = intent.getStringExtra(MainMenuActivity.EXTRA_MESSAGE_EMAIL);
        final String userUID = intent.getStringExtra(MainMenuActivity.EXTRA_MESSAGE_UID);
        // Set loading window
        loadingWindow = KProgressHUD.create(UserAccountManagementActivity.this)
                .setStyle(KProgressHUD.Style.SPIN_INDETERMINATE)
                .setBackgroundColor(Color.parseColor("#508AF1F7"))
                .setLabel(getResources().getString(R.string.loading_label_please_wait))
                .setDetailsLabel(getResources().getString(R.string.loading_details_downloading_data))
                .setCancellable(true)
                .setAnimationSpeed(2)
                .setDimAmount(0.5f)
                .show();


        //Firebase auth//
        mAuth = FirebaseAuth.getInstance();
        //Firebase storage//
        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();
        profilePhotoRef = storageReference.child("public/profilePhotos/" + userUID); //Automatically get the profile photo from the user id
        //--
        profilePic = (ImageView) findViewById(R.id.imageViewProfilePhoto);
        textViewName = (TextView) findViewById(R.id.textViewName);
        textViewEmail = (TextView) findViewById(R.id.textViewEmail);
        final Button buttonChangeProfilePhoto = (Button) findViewById(R.id.buttonChangeProfilePhoto);
        final Button buttonChangePassword = (Button) findViewById(R.id.buttonChangePassword);
        final Button buttonLogout = (Button) findViewById(R.id.buttonLogout);
        //--//
        updateUamUi(userName, userEmail);
        try {
            updateUamProfilePhoto();
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), "SUMTINWONG", Toast.LENGTH_LONG).show();
        }
        //*Back Button//
        final ImageButton buttonBack = (ImageButton) findViewById(R.id.buttonBack);
        buttonBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        //END OF: Back Button--//

        buttonChangeProfilePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                buttonChangeProfilePhoto.setEnabled(false);
                //--
                choosePicture();
            }
        });

        buttonChangePassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                buttonChangePassword.setEnabled(false);
                requestResetPasswordEmail(userEmail);
            }
        });

        buttonLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                buttonLogout.setEnabled(false);
                backDialog(UserAccountManagementActivity.this).show();
                buttonLogout.setEnabled(true);
            }
        });
    }

    private void choosePicture() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, 1);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK && data != null && data.getData() != null) {
            //*Get Image Uri and set the local image to the desired image*//
            imageUri = data.getData();
            profilePic.setImageURI(imageUri);
            //*Upload to Firebase*//
            uploadPicture();
        }
    }

    public Uri getImageUri(Context inContext, Bitmap inImage) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(inContext.getContentResolver(), inImage, "Title", null);
        return Uri.parse(path);
    }


    private void uploadPicture() {
        Uri finalImageUri = imageUri;
        //*---Compress Image [!IMPORTANT!: WHEN THE IMAGE IS COMPRESSED IT LOSES PNG TRANSPARENCY]-------------------//
        if (profilePhotoCompressionEnabled) { //Look at the boolean on global
            Bitmap bitmapToUpload = null;
            int originalBitmapSize = -1;
            int compressedBitmapSize = -1;
            Uri compressedBitmapUri = null;
            try {
                //Prepare Bitmap to upload from the original image's uri
                bitmapToUpload = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);

                //*Check Bitmap Size
                originalBitmapSize = bitmapToUpload.getByteCount();
                Toast.makeText(getApplicationContext(), "Bitmap Aslinya: " + originalBitmapSize, Toast.LENGTH_LONG).show();
                Log.d("james", "Bitmap Asli size: " + originalBitmapSize);

                //Compress the bitmap
                Bitmap bitmapToUploadCompressed = ImageResizer.reduceBitmapSize(bitmapToUpload, 250000);
                compressedBitmapSize = bitmapToUploadCompressed.getByteCount();
                Log.d("james", "Bitmap Compressed size: " + compressedBitmapSize);
                //Get the new uri of the new compressed bitmap
                compressedBitmapUri = getImageUri(UserAccountManagementActivity.this, bitmapToUploadCompressed);
            } catch (IOException e) {
                Toast.makeText(getApplicationContext(), getResources().getString(R.string.layout_uam_image_compress_failure), Toast.LENGTH_LONG).show();
                e.printStackTrace();
            }
            //*--Check if Compression Works & if it works then replace finalImageUri with the uri of the compressed image--//
            if (compressedBitmapUri != null) {
                finalImageUri = compressedBitmapUri;
            }
        }
        //*END OF: COMPRESS IMAGE-------------------------//

        //*---Progress Bar---//
        final ProgressDialog pd = new ProgressDialog(this);
        pd.setTitle(getResources().getString(R.string.layout_uam_image_uploaded_progress_title));
        pd.setCanceledOnTouchOutside(false);
        pd.show();
        //*---Uploader---//
//        final String randomKey = UUID.randomUUID().toString();
        profilePhotoRef.putFile(finalImageUri)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        // Get a URL to the uploaded content
//                        Uri downloadUrl = taskSnapshot.getDownloadUrl();
                        pd.dismiss();//dismiss progress bar
                        Toast.makeText(getApplicationContext(), getResources().getString(R.string.layout_uam_image_uploaded_success), Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        pd.dismiss();//dismiss progress bar
                        Toast.makeText(getApplicationContext(), getResources().getString(R.string.layout_uam_image_uploaded_failure), Toast.LENGTH_LONG).show();
                        // Handle unsuccessful uploads
                        // ...
                    }
                })
                .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onProgress(@NonNull UploadTask.TaskSnapshot snapshot) {
                        double progressPercent = (100.00 * snapshot.getBytesTransferred() / snapshot.getTotalByteCount());
                        pd.setMessage(getResources().getString(R.string.layout_uam_image_uploaded_progress) + (int) progressPercent + "%");
                    }
                });
    }

    private void updateUamUi(String userName, String userEmail) {
        if (userName == null && userEmail == null) {
            textViewName.setText(getResources().getString(R.string.label_title_not_logged_in));
            //Change the Activity to Loggin/Register Screen if the user is not logged in to the app.
            Intent i = new Intent(this, MainActivity.class);
            // set the new task and clear flags
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); //Clear the previous activities (so that when the user press back, the user will not be brought to the login/register screen)
            startActivity(i);
        } else {
            textViewName.setText(userName);
            textViewEmail.setText(userEmail);
        }
    }

    private void updateUamProfilePhoto() {
        profilePhotoRef.getBytes(1024 * 1024)
                .addOnSuccessListener(new OnSuccessListener<byte[]>() {
                    @Override
                    public void onSuccess(byte[] bytes) {
                        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                        profilePic.setImageBitmap(bitmap);
//                        Toast.makeText(getApplicationContext(), getResources().getString(R.string.layout_uam_image_loaded_success), Toast.LENGTH_LONG).show();
                        loadingWindow.dismiss();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d("james", "User does not have a profile photo on the database");
//                        Toast.makeText(getApplicationContext(), getResources().getString(R.string.layout_uam_image_loaded_failure), Toast.LENGTH_LONG).show();
                        loadingWindow.dismiss();
                    }
                });
    }

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

    private void signOut() {
        mAuth.signOut();
        updateUamUi(null, null);
    }

    //    *------RESET PASSWORD-----//
    public void requestResetPasswordEmail(String emailAddress) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
//        String emailAddress = "user@example.com";

        auth.sendPasswordResetEmail(emailAddress)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Log.d("jamesResetPassword", "Email sent.");
                            Alerter.create(UserAccountManagementActivity.this).setTitle(getResources().getString(R.string.layout_uam_password_notif)).setText(getResources().getString(R.string.layout_uam_password_notif_desc)).setBackgroundColorRes(R.color.colorAccent).show();
                        } else {
                            Alerter.create(UserAccountManagementActivity.this).setTitle(getResources().getString(R.string.forget_password_error)).setText(getResources().getString(R.string.forget_password_error_desc)).setBackgroundColorRes(R.color.colorAccent).show();
                            Log.d("jamesResetPassword", "Error gan.");
                        }
                    }
                });
    }
}