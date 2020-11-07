package com.jady.jadytrack;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.zxing.Result;
import com.kaopiz.kprogresshud.KProgressHUD;
import com.tapadoo.alerter.Alerter;

import me.dm7.barcodescanner.zxing.ZXingScannerView;

public class ScanQrActivity extends AppCompatActivity implements ZXingScannerView.ResultHandler {

    // Handler
    public boolean internetStatus = true;
    private int sessionHandler = 0; // untuk menjaga, ketika scan qr, tidak kedouble scannya

    // Attribute
    private String id;
    private String nama;
    private String scenario;
    private String uid;
    // id message yang akan dipassing ke activity selanjutnya
    public static final String EXTRA_MESSAGE_ID = "com.example.yeftaprototypev2.SENDID";
    public static final String EXTRA_MESSAGE_NAME = "com.example.yeftaprototypev2.SENDNAME";
    public static final String EXTRA_MESSAGE_UID = "com.example.yeftaprototypev2.SENDUID";

    // view untuk qr scanner
    private ZXingScannerView mScannerView;

   private KProgressHUD loadingWindow;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_qr);

        // melihat skenarionya
        Intent intent = getIntent();
        scenario = intent.getStringExtra(InputOptionActivity.EXTRA_MESSAGE_SCENARIO);
        uid = intent.getStringExtra(InputOptionActivity.EXTRA_MESSAGE_UID);

        // meminta permission untuk mengakses kamera
        ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.CAMERA}, 1400); //getCameraInstance();

        // untuk menampilkan scanner view
        mScannerView = new ZXingScannerView(this);
        setContentView(mScannerView);

        // handler jika koneksi internet tidak ditemukan
        final Handler handler = new Handler();
        Runnable runnable = new Runnable() {

            @Override
            public void run() {
                try{
                    if (!isConnected(ScanQrActivity.this)){
                        if(internetStatus == true){
                            buildDialog(ScanQrActivity.this).show();
                            internetStatus = false;
                        }
                    }
                    else {
                        internetStatus = true;
                    }
                }
                catch (Exception e) {
                    // TODO: handle exception
                }
                finally{
                    //also call the same runnable to call it at regular interval
                    handler.postDelayed(this, 1000);
                }
            }
        };
        handler.postDelayed(runnable, 1000);
    }


    @Override
    protected void onResume() {
        super.onResume();

        mScannerView.setResultHandler(this); // Register ourselves as a handler for scan results.
        mScannerView.startCamera();          // Start camera on resume
    }

    // kamera akan dihentikan ketika beralih ke activity lain
    @Override
    public void onPause() {
        super.onPause();
        mScannerView.stopCamera();
        sessionHandler = 0;
    }

    // hal yang dilakukan ketika qr code terdeteksi
    @Override
    public void handleResult(Result rawResult) {
        // dalam satu porses hanya boleh ada 1 sesi untuk mendeteksi qr code
        if(sessionHandler == 0){
            //Set loading window
            loadingWindow = KProgressHUD.create(ScanQrActivity.this)
                    .setStyle(KProgressHUD.Style.SPIN_INDETERMINATE)
                    .setBackgroundColor(Color.parseColor("#508AF1F7"))
                    .setLabel("Please wait")
                    .setDetailsLabel("Finding tracking id")
                    .setCancellable(true)
                    .setAnimationSpeed(2)
                    .setDimAmount(0.5f)
                    .show();


            checkDatabase(rawResult.getText());
            sessionHandler = 1;
        }

        mScannerView.resumeCameraPreview(this);
    }

    // untuk menyocokkan id yang dimasukkan dengan id yang ada di database
    private void checkDatabase(String idcode){
        id = idcode;
        id = id.replaceAll("\\.", "");
        id = id.replaceAll("\\#", "");
        id = id.replaceAll("\\$", "");
        id = id.replaceAll("\\[", "");
        id = id.replaceAll("\\]", "");

        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference().child("trackingSession");
        rootRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.hasChild(id)) {
                    if(scenario.equals("viewer")){
                        loadingWindow.dismiss();//dismiss window
                        Intent intent = new Intent(ScanQrActivity.this, TrackingActivity.class);
                        intent.putExtra(EXTRA_MESSAGE_ID, id);
                        intent.putExtra(EXTRA_MESSAGE_NAME, nama);
                        startActivity(intent);
                    }
                    else if(scenario.equals("appointment")){
                        // cek apakah sudah pernah buat geofence
                        loadingWindow.dismiss();//dismiss window
                        Intent intent = new Intent(ScanQrActivity.this, AppointmentActivity.class);
                        intent.putExtra(EXTRA_MESSAGE_ID, id);
                        intent.putExtra(EXTRA_MESSAGE_NAME, nama);
                        intent.putExtra(EXTRA_MESSAGE_UID, uid);
                        startActivity(intent);
                    }
                }
                else{
                    Alerter.create(ScanQrActivity.this).setTitle("Oh no!").setText("ID does not exist").setBackgroundColorRes(R.color.colorAccent).show();

                    loadingWindow.dismiss();
                    sessionHandler = 0;
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    // untuk mengecek apakah ada koneksi internet
    public boolean isConnected(Context context) {

        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netinfo = cm.getActiveNetworkInfo();

        if (netinfo != null && netinfo.isConnectedOrConnecting()) {
            NetworkInfo wifi = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            NetworkInfo mobile = cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);

            if((mobile != null && mobile.isConnectedOrConnecting()) || (wifi != null && wifi.isConnectedOrConnecting())) return true;
            else return false;
        } else
            return false;
    }
    public AlertDialog.Builder buildDialog(Context c) {

        AlertDialog.Builder builder = new AlertDialog.Builder(c);
        builder.setTitle("No Internet Connection");
        builder.setMessage("You need to have Mobile Data or wifi to access this. Press Try Again or Exit");
        builder.setCancelable(false);

        builder.setPositiveButton("Try Again", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {

                dialog.cancel();
                if (!isConnected(ScanQrActivity.this))

                    buildDialog(ScanQrActivity.this).show();
                else {

                }
            }
        });

        builder.setNegativeButton("Exit", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {

                finish();
                moveTaskToBack(true);
            }
        });

        return builder;
    }
}
