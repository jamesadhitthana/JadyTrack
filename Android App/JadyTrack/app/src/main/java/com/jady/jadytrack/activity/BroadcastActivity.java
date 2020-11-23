package com.jady.jadytrack.activity;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.jady.jadytrack.R;
import com.jady.jadytrack.service.ForegroundService;
import com.kaopiz.kprogresshud.KProgressHUD;
import com.tapadoo.alerter.Alerter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.Vector;

public class BroadcastActivity extends FragmentActivity implements OnMapReadyCallback {

    public static final String EXTRA_MESSAGE_ID = "com.jady.jadytrack.SENDID";
    public static final String EXTRA_MESSAGE_SHORT_ID = "com.jady.jadytrack.SENDSHORTID";
    // Google Map
    private GoogleMap mMap;
    private LocationManager locationManager;
    private LocationListener locationListener;

    private LatLng markerPosition = new LatLng(0f, 0f);
    private Marker currentMarker;
    private boolean isFirstMarker = true;
    private boolean isMarkerDrawed = false;

    private Double destLongitude;
    private Double destLatitude;
    private Long destRadius;

    private Double markerLongitude;
    private Double markerLatitude;

    private Marker destinationMarker;
    private Circle destinationFence;

    private List<Marker> markers = new ArrayList<>();
    private List<Marker> drawer = new ArrayList<>();

    private Polygon polygon;

    // Handler
    private boolean isBroadcast = false;
    private boolean internetStatus = true;
    private boolean gpsStatus = true;
    private Long startTime = 0L;

    // Attribute
    private String nama = "Test Aja";
    private String userUID = "Test Juga";
    private boolean isGeofenceDrawed = false;
    public boolean manualCheckIn = false;
    private boolean hasArrived = false;
    private String id;
    private String shortTrackingId; //New shortTrackingId 12Nov2020 James

    // Widget
    private TextView status;
    private Button tombolBroadcast;

    // Data yang akan diberikan ke firebase
    private Map<String, Location> locationHistory = new HashMap<>();
    private Integer numMarker = 0;

    // Polyline
    PolylineOptions options = new PolylineOptions().width(5).color(Color.BLUE).geodesic(true);

    // Firebase
    private DatabaseReference databaseReference;
    private DatabaseReference geofenceReference;
    private DatabaseReference shortTrackingIdDatabaseReference;

    // Process dialog
    private KProgressHUD loadingWindow;

    //loading bar WINDOW
    boolean progressDialogIsShown = false;
    private KProgressHUD loadingWindowEnableBroadcast;
    private KProgressHUD loadingWindowDisableBroadcast;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.broadcast);

        loadingWindow = KProgressHUD.create(BroadcastActivity.this)
                .setStyle(KProgressHUD.Style.SPIN_INDETERMINATE)
                .setBackgroundColor(Color.parseColor("#508AF1F7"))
                .setLabel(getResources().getString(R.string.loading_label_please_wait))
                .setDetailsLabel(getResources().getString(R.string.loading_details_get_location_data))
                .setCancellable(true)
                .setAnimationSpeed(2)
                .setDimAmount(0.5f)
                .show();

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // Untuk menerima nilai dari nama yang diberikan oleh activity sebelumnya
        //@Yefta, I changed the intents below to get it from the AppointmentActivity.java
        Intent intent = getIntent();
        nama = intent.getStringExtra(MainMenuActivity.EXTRA_MESSAGE_NAME);
        //nama = intent.getStringExtra("trackingSession_nama");
        userUID = intent.getStringExtra(MainMenuActivity.EXTRA_MESSAGE_UID);
        //userUID = intent.getStringExtra("trackingSession_userUID");
        //Tambahan James untuk ambil tracking session key dari AppointmentActivity.java
        //trackingSessionKey = intent.getStringExtra("trackingSession_key");

        //*Back Button//
        final ImageButton buttonBack = (ImageButton) findViewById(R.id.buttonBack);
        buttonBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                backDialog(BroadcastActivity.this).show();
            }
        });
        //END OF: Back Button--//
        // Menyiapkan tampilan
        // Widget
        status = (TextView) findViewById(R.id.status);
        tombolBroadcast = (Button) findViewById(R.id.startBroadcast);
        Button tombolGetId = (Button) findViewById(R.id.getId); //Share / Sharing
        status.setText(getResources().getString(R.string.label_broadcast_disabled));

        // Menyiapkan Firebase
        databaseReference = FirebaseDatabase.getInstance().getReference().child("trackingSession");
        shortTrackingIdDatabaseReference = FirebaseDatabase.getInstance().getReference().child("shortTrackingSession");
        //final String id = trackingSessionKey;
        id = databaseReference.push().getKey(); //(James): Yef, I deleted this and changed it to the key sent by the previous intent

        //*Building shortTrackingId
        shortTrackingId = buildShortTrackingId(nama, id);

        geofenceReference = FirebaseDatabase.getInstance().getReference().child("trackingSession/" + id);

        locationManager = (LocationManager) this.getSystemService(LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {

                // mengubah status
                databaseReference.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.child(id).child("notifications").child("statusSOS").getValue() == "true") {
                            databaseReference.child(id).child("notifications").child("statusSOS").setValue(false);
                        }
                        if (dataSnapshot.child(id).child("notifications").child("statusInGeofence").getValue() == "true") {
                            databaseReference.child(id).child("notifications").child("statusInGeofence").setValue(false);
                        }
                        if (dataSnapshot.child(id).child("notifications").child("statusHasArrived").getValue() == "true") {
                            databaseReference.child(id).child("notifications").child("statusHasArrived").setValue(false);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });


                Long endTime = location.getTime();
                long diffTime = endTime - startTime;

                // Mengubah current marker
                markerPosition = new LatLng(location.getLatitude(), location.getLongitude());
                currentMarker.setPosition(markerPosition);
                if (!isMarkerDrawed) {
                    loadingWindow.dismiss();
                    isMarkerDrawed = true;
                }
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(markerPosition, 17.0f));

                // setiap 12 detik (12000) update database
                if (diffTime > 5000 && isBroadcast) {

                    status.setText(getResources().getString(R.string.label_broadcast_enabled));

                    //--stop loading window
                    loadingWindowEnableBroadcast.dismiss();
                    //END OF: stop loading window

                    startTime = endTime;

                    //--UPDATED-- Membuat objek dan mengirim ke firebase
                    //databaseReference.child(id).setValue(target);
                    databaseReference.child(id).child("targetId").setValue(userUID);
                    databaseReference.child(id).child("targetName").setValue(nama);
                    databaseReference.child(id).child("targetLocation").setValue(location);
                    databaseReference.child(id).child("numHistory").setValue(numMarker);
                    databaseReference.child(id).child("locationHistory").setValue(locationHistory);

                    //*--12Nov2020--//Store shortTrackingId to Firebase Database
                    writeShortTrackingIdToDatabase(shortTrackingId);

                    // Current time
                    Calendar calendar = Calendar.getInstance();
                    TimeZone tz = TimeZone.getDefault();
                    calendar.add(Calendar.MILLISECOND, tz.getOffset(calendar.getTimeInMillis()));
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                    java.util.Date currenTimeZone = new java.util.Date((long) 1379487711 * 1000);
                    String date = sdf.format(currenTimeZone);

                    if (isFirstMarker) {
                        // start marker
                        BitmapDescriptor icon = BitmapDescriptorFactory.fromResource(R.drawable.start);
                        mMap.addMarker(new MarkerOptions().position(markerPosition).title("First Marker").icon(icon));
                        isFirstMarker = false;
                    } else {
                        // history marker
                        BitmapDescriptor icon = BitmapDescriptorFactory.fromResource(R.drawable.greendot);
                        mMap.addMarker(new MarkerOptions().position(markerPosition).title(date).icon(icon));
                    }

                    // add polyline
                    options.add(markerPosition);
                    mMap.addPolyline(options);

                    // History marker & jumlah history marker
                    numMarker++;
                    locationHistory.put(numMarker.toString(), location);

                    // handler untuk geofence
                    if (isGeofenceDrawed) {
                        if (!pointInPolygon(currentMarker.getPosition(), polygon)) {
                            databaseReference.child(id).child("notifications").child("statusInGeofence").setValue(false);
                            polygon.setFillColor(Color.RED);
                            Alerter.create(BroadcastActivity.this).setTitle(getResources().getString(R.string.alert_title_crossing_border)).setText(getResources().getString(R.string.alert_msg_crossing_border)).setBackgroundColorRes(R.color.colorAccent).show();
                            notifyAlert(getResources().getString(R.string.notification_target_crossing_border));
                        }
                    }


                } else if (!isBroadcast) {
                    status.setText(getResources().getString(R.string.label_broadcast_disabled));

                    if (progressDialogIsShown) {
                        loadingWindowDisableBroadcast.dismiss();
                        progressDialogIsShown = false;
                    }

                }
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {

            }
        };
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            return;
        } else {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
        }


        // Enable/Disable Broadcast
        tombolBroadcast.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {


                if (isBroadcast) {
                    // Nov 6 2020 UTR09 New Feature to Ask if User is sure about disabling broadcast
                    // Todo: aSK USER
                    askUserDisableBroadcast();

                } else {
                    isBroadcast = true;
                    // ---Start loading window
                    loadingWindowEnableBroadcast = KProgressHUD.create(BroadcastActivity.this)
                            .setStyle(KProgressHUD.Style.SPIN_INDETERMINATE)
                            .setBackgroundColor(Color.parseColor("#508AF1F7"))
                            .setLabel(getResources().getString(R.string.loading_label_please_wait))
                            .setDetailsLabel(getResources().getString(R.string.loading_details_enabling_broadcast))
                            .setCancellable(true)
                            .setAnimationSpeed(2)
                            .setDimAmount(0.5f)
                            .show();
                    // END OF: Start loading window
                    tombolBroadcast.setText(getResources().getString(R.string.button_disable_broadcast));
                }
            }
        });

        // Button to show tracking ID and QR code
        tombolGetId.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                if (isBroadcast) {
                    Intent intent = new Intent(view.getContext(), GenerateIdActivity.class); //Share/Sharing
                    intent.putExtra(EXTRA_MESSAGE_ID, id);
                    intent.putExtra(EXTRA_MESSAGE_SHORT_ID, shortTrackingId);
                    startActivity(intent);
                } else {
                    Alerter.create(BroadcastActivity.this).setTitle(getResources().getString(R.string.alert_title_broadcast_disabled)).setText(getResources().getString(R.string.alert_msg_broadcast_disabled)).setBackgroundColorRes(R.color.colorAccent).show();

                }
            }
        });

        // Set up Buttons
        Button setSOS = (Button) findViewById(R.id.buttonSOS);
        setSOS.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (isBroadcast) {
                    //Check In Alert Dialog
                    AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(BroadcastActivity.this);
                    alertDialogBuilder.setMessage(getResources().getString(R.string.alert_title_enable_sos));
                    alertDialogBuilder.setPositiveButton(getResources().getString(R.string.button_danger),
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface arg0, int arg1) {
                                    //------------------------Emergency Status: ENABLE EMERGENCY----------------------//
                                    try {
                                        databaseReference.child(id).child("notifications").child("statusSOS").setValue(true);
                                        Alerter.create(BroadcastActivity.this).setText(getResources().getString(R.string.alert_title_emergency_status_sent)).setBackgroundColorRes(R.color.colorAccent).show();

                                    } catch (Exception e) {
                                        Alerter.create(BroadcastActivity.this).setTitle(getResources().getString(R.string.alert_title_failed_send_emergency_status)).setText(getResources().getString(R.string.alert_msg_failed_send_emergency_status)).setBackgroundColorRes(R.color.colorAccent).show();

                                    }
                                }
                            });

                    alertDialogBuilder.setNegativeButton(getResources().getString(R.string.button_safe),
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    //------------------------Emergency Status: DISABLE EMERGENCY----------------------//
                                    try {
                                        databaseReference.child(id).child("notifications").child("statusSOS").setValue(false);
                                        Alerter.create(BroadcastActivity.this).setTitle(getResources().getString(R.string.alert_title_select_no)).setText(getResources().getString(R.string.alert_msg_select_no)).setBackgroundColorRes(R.color.colorAccent).show();

                                    } catch (Exception e) {
                                        Alerter.create(BroadcastActivity.this).setTitle(getResources().getString(R.string.alert_title_failed_set_emergency_status)).setText(getResources().getString(R.string.alert_msg_failed_set_emergency_status)).setBackgroundColorRes(R.color.colorAccent).show();
                                    }

                                }
                            });

                    AlertDialog alertDialog = alertDialogBuilder.create();
                    alertDialog.show();
                } else {
                    Alerter.create(BroadcastActivity.this).setTitle(getResources().getString(R.string.alert_title_broadcast_disabled)).setText(getResources().getString(R.string.alert_msg_broadcast_disabled)).setBackgroundColorRes(R.color.colorAccent).show();
                }

            }
        });

        Button setCheckIn = (Button) findViewById(R.id.checkin);
        setCheckIn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (isGeofenceDrawed) {
                    // Check In Alert Dialog
                    AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(BroadcastActivity.this);
                    alertDialogBuilder.setMessage(getResources().getString(R.string.alert_title_check_in_arrived));
                    alertDialogBuilder.setPositiveButton(getResources().getString(R.string.button_check_me_in),
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface arg0, int arg1) {
                                    //------------------------Manual Check in----------------------//
                                    try {
                                        databaseReference.child(id).child("notifications").child("manualCheckIn").setValue(true);
                                        databaseReference.child(id).child("notifications").child("statusHasArrived").setValue(true);
                                        manualCheckIn = true;
                                        hasArrived = true;
                                        notifyInfo(getResources().getString(R.string.notification_target_arrived));
                                        Alerter.create(BroadcastActivity.this).setTitle(getResources().getString(R.string.alert_title_check_in)).setText(getResources().getString(R.string.alert_msg_check_in)).setBackgroundColorRes(R.color.colorAccent).show();

                                    } catch (Exception e) {
                                        Alerter.create(BroadcastActivity.this).setTitle(getResources().getString(R.string.alert_title_failed_check_in)).setText(getResources().getString(R.string.alert_msg_failed_check_in)).setBackgroundColorRes(R.color.colorAccent).show();
                                    }
                                }
                            });

                    alertDialogBuilder.setNegativeButton(getResources().getString(R.string.button_not_there_yet),
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Alerter.create(BroadcastActivity.this).setText(getResources().getString(R.string.alert_title_select_no)).setBackgroundColorRes(R.color.colorAccent).show();
                                }
                            });

                    AlertDialog alertDialog = alertDialogBuilder.create();
                    alertDialog.show();
                    //--
                } else {
                    Alerter.create(BroadcastActivity.this).setText(getResources().getString(R.string.alert_title_dont_have_geofence)).setBackgroundColorRes(R.color.colorAccent).show();

                }
            }
        });


    }


    public void startService() {
        Intent serviceIntent = new Intent(this, ForegroundService.class);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        }
        else{
            startService(serviceIntent);
        }
    }

    public void stopService() {
        Intent serviceIntent = new Intent(this, ForegroundService.class);
        stopService(serviceIntent);
    }


    @Override
    protected void onStart() {
        super.onStart();

        startService();

        if (!isGeofenceDrawed) {
            plotGeofence();
        }
    }

    public void plotGeofence() {

        geofenceReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                if (dataSnapshot.hasChild("geofenceNum") && !isGeofenceDrawed) {
                    ArrayList<Object> geofence = (ArrayList<Object>) dataSnapshot.child("geofence").getValue();
                    int geofenceSize = geofence.size() - 1;
                    int geofenceNum = (Integer) Integer.parseInt((String) dataSnapshot.child("geofenceNum").getValue().toString());

                    if (geofenceNum == geofenceSize) {
                        isGeofenceDrawed = true;

                        //oast.makeText(BroadcastActivity.this, "Target has Geofence", Toast.LENGTH_LONG).show();

                        // Ini untuk menggambar destination
                        HashMap<String, Object> destination =
                                (HashMap<String, Object>) dataSnapshot.child("destination").getValue();

                        for (HashMap.Entry<String, Object> entry : destination.entrySet()) {
                            if (entry.getKey().equals("longitude")) {
                                destLongitude = (Double) entry.getValue();
                            }
                            if (entry.getKey().equals("latitude")) {
                                destLatitude = (Double) entry.getValue();
                            }
                            if (entry.getKey().equals("radius")) {
                                destRadius = (Long) entry.getValue();
                            }
                        }

                        MarkerOptions markerOptions = new MarkerOptions()
                                .position(new LatLng(destLatitude, destLongitude))
                                .icon(BitmapDescriptorFactory.fromResource(R.drawable.finish))
                                .title("Destination")
                                .zIndex(100f);

                        destinationMarker = mMap.addMarker(markerOptions);

                        CircleOptions circleOptions = new CircleOptions()
                                .center(destinationMarker.getPosition())
                                .strokeColor(Color.argb(50, 70, 70, 70))
                                .fillColor(Color.argb(100, 150, 150, 150))
                                .radius(destRadius);
                        destinationFence = mMap.addCircle(circleOptions);


                        // Draw Geofence
                        for (int i = 1; i <= geofenceSize; i++) {

                            HashMap<String, Object> geofenceIndex = (HashMap<String, Object>) dataSnapshot.child("geofence").child(Integer.toString(i)).getValue();

                            for (HashMap.Entry<String, Object> entry : geofenceIndex.entrySet()) {
                                if (entry.getKey().equals("longitude")) {
                                    markerLongitude = (Double) entry.getValue();
                                }
                                if (entry.getKey().equals("latitude")) {
                                    markerLatitude = (Double) entry.getValue();
                                }
                            }

                            Marker marker = mMap.addMarker(new MarkerOptions().icon(BitmapDescriptorFactory.fromResource(R.drawable.dot15px)).position(new LatLng(markerLatitude, markerLongitude)));
                            markers.add(marker);
                        }

                        drawer = new ArrayList<>(markers);

                        convexHull(markers.size());
                        PolygonOptions polygonOptions = new PolygonOptions();
                        polygonOptions.add(drawer.get(0).getPosition());

                        for (int i = 1; i < drawer.size(); i++) {
                            polygonOptions.add(drawer.get(i).getPosition());
                        }

                        polygonOptions.add(drawer.get(0).getPosition());

                        polygonOptions.strokeColor(Color.argb(50, 70, 70, 70));
                        polygonOptions.fillColor(Color.argb(100, 150, 150, 150));


                        polygon = mMap.addPolygon(polygonOptions);
                    }
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    // Prepare the Map
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        // Marker
        BitmapDescriptor icon = BitmapDescriptorFactory.fromResource(R.drawable.smile);
        // Current marker
        MarkerOptions markerOption = new MarkerOptions().position(markerPosition).title("Current Location").icon(icon);
        currentMarker = mMap.addMarker(markerOption);
        currentMarker.setZIndex(1.0f);
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(markerPosition, 17.0f));
    }

    // Set Permission
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        final Handler handler = new Handler();
        Runnable runnable = new Runnable() {

            @Override
            public void run() {
                try {
                    if (isConnected(BroadcastActivity.this)) {
                        if (internetStatus) {
                            internetDialog(BroadcastActivity.this).show();
                            internetStatus = false;
                        }
                    } else {
                        internetStatus = true;
                    }

                    if (isGpsEnabled(BroadcastActivity.this)) {
                        if (gpsStatus && internetStatus) {
                            gpsDialog(BroadcastActivity.this).show();
                            gpsStatus = false;
                        }
                    } else {
                        gpsStatus = true;
                    }


                } catch (Exception e) {
                    // TODO: handle exception
                } finally {
                    // Call the same runnable to call it at regular interval
                    handler.postDelayed(this, 5000);
                }
            }
        };
        handler.postDelayed(runnable, 1000);
    }

    public boolean isConnected(Context context) {

        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netinfo = cm.getActiveNetworkInfo();

        if (netinfo != null && netinfo.isConnectedOrConnecting()) {
            android.net.NetworkInfo wifi = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            android.net.NetworkInfo mobile = cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);

            return (mobile == null || !mobile.isConnectedOrConnecting()) && (wifi == null || !wifi.isConnectedOrConnecting());
        } else
            return true;
    }

    public AlertDialog.Builder internetDialog(Context c) {

        AlertDialog.Builder builder = new AlertDialog.Builder(c);
        builder.setTitle(getResources().getString(R.string.alert_title_no_internet_connection));
        builder.setMessage(getResources().getString(R.string.alert_msg_no_internet_connection));
        builder.setCancelable(false);

        builder.setPositiveButton(getResources().getString(R.string.button_try_again), new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {

                dialog.cancel();
                if (isConnected(BroadcastActivity.this)) {
                    internetDialog(BroadcastActivity.this).show();
                }

            }
        });

        builder.setNegativeButton(getResources().getString(R.string.button_exit), new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {

                finish();
                moveTaskToBack(true);
            }
        });

        return builder;
    }

    public boolean isGpsEnabled(Context context) {
        final LocationManager manager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

        return !manager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    public AlertDialog.Builder gpsDialog(Context c) {

        final AlertDialog.Builder builder = new AlertDialog.Builder(c);
        builder.setTitle(R.string.alert_title_gps_disabled);
        builder.setMessage(R.string.alert_msg_gps_disabled);
        builder.setCancelable(false);

        builder.setPositiveButton(R.string.button_yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
            }
        });
        builder.setNegativeButton(getResources().getString(R.string.button_exit), new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {

                finish();
                moveTaskToBack(true);
            }
        });

        return builder;
    }


    @Override
    public void onBackPressed() {
        backDialog(BroadcastActivity.this).show();
    }

    public AlertDialog.Builder backDialog(Context c) {

        AlertDialog.Builder builder = new AlertDialog.Builder(c);
        builder.setTitle(getResources().getString(R.string.alert_title_quit_broadcasting));
        builder.setMessage(getResources().getString(R.string.alert_msg_quit_broadcasting));
        builder.setCancelable(false);

        builder.setPositiveButton(getResources().getString(R.string.button_yes), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                stopService();
                isBroadcast = false;
                finish();
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

    public double orientation(int p, int q, int r) {
        double val = (markers.get(q).getPosition().latitude - markers.get(p).getPosition().latitude) * (markers.get(r).getPosition().longitude - markers.get(q).getPosition().longitude) -
                (markers.get(q).getPosition().longitude - markers.get(p).getPosition().longitude) * (markers.get(r).getPosition().latitude - markers.get(q).getPosition().latitude);

        if (val == 0) return 0;  // collinear
        return (val > 0) ? 1 : 2; // clock or counterclock wise
    }

    // Prints convex hull of a set of n points.
    public void convexHull(int n) {
        // There must be at least 3 points
        if (n < 3) return;

        // Initialize Result
        Vector<Marker> hull = new Vector<>();

        // Find the leftmost point
        int l = 0;
        for (int i = 1; i < n; i++)
            if (markers.get(i).getPosition().longitude < markers.get(l).getPosition().longitude)
                l = i;

        // Start from leftmost point, keep moving
        // counterclockwise until reach the start point
        // again. This loop runs O(h) times where h is
        // number of points in result or output.
        int p = l, q;
        do {
            // Add current point to result
            hull.add(markers.get(p));

            // Search for a point 'q' such that
            // orientation(p, x, q) is counterclockwise
            // for all points 'x'. The idea is to keep
            // track of last visited most counterclock-
            // wise point in q. If any point 'i' is more
            // counterclock-wise than q, then update q.
            q = (p + 1) % n;

            for (int i = 0; i < n; i++) {
                // If i is more counterclockwise than
                // current q, then update q
                if (orientation(p, i, q)
                        == 2)
                    q = i;
            }

            // Now q is the most counterclockwise with
            // respect to p. Set p as q for next iteration,
            // so that q is added to result 'hull'
            p = q;

        } while (p != l);  // While we don't come to first
        // point

        drawer = new ArrayList<>(hull);
        // Print Result
        /*for (Marker temp : hull)
            System.out.println("(" + temp.x + ", " +
                    temp.y + ")");
        */
    }

    public boolean pointInPolygon(LatLng point, Polygon polygon) {
        // ray casting alogrithm http://rosettacode.org/wiki/Ray-casting_algorithm
        int crossings = 0;
        List<LatLng> path = polygon.getPoints();
        path.remove(path.size() - 1); //remove the last point that is added automatically by getPoints()

        // for each edge
        for (int i = 0; i < path.size(); i++) {
            LatLng a = path.get(i);
            int j = i + 1;
            //to close the last edge, you have to take the first point of your polygon
            if (j >= path.size()) {
                j = 0;
            }
            LatLng b = path.get(j);
            if (rayCrossesSegment(point, a, b)) {
                crossings++;
            }
        }

        // odd number of crossings?
        return (crossings % 2 == 1);
    }

    public boolean rayCrossesSegment(LatLng point, LatLng a, LatLng b) {
        // Ray Casting algorithm checks, for each segment, if the point is 1) to the left of the segment and 2) not above nor below the segment. If these two conditions are met, it returns true
        double px = point.longitude,
                py = point.latitude,
                ax = a.longitude,
                ay = a.latitude,
                bx = b.longitude,
                by = b.latitude;
        if (ay > by) {
            ax = b.longitude;
            ay = b.latitude;
            bx = a.longitude;
            by = a.latitude;
        }
        // alter longitude to cater for 180 degree crossings
        if (px < 0 || ax < 0 || bx < 0) {
            px += 360;
            ax += 360;
            bx += 360;
        }
        // if the point has the same latitude as a or b, increase slightly py
        if (py == ay || py == by) py += 0.00000001;


        // if the point is above, below or to the right of the segment, it returns false
        if ((py > by || py < ay) || (px > Math.max(ax, bx))) {
            return false;
        }
        // if the point is not above, below or to the right and is to the left, return true
        else if (px < Math.min(ax, bx)) {
            return true;
        }
        // if the two above conditions are not met, you have to compare the slope of segment [a,b] (the red one here) and segment [a,p] (the blue one here) to see if your point is to the left of segment [a,b] or not
        else {
            double red = (ax != bx) ? ((by - ay) / (bx - ax)) : Double.POSITIVE_INFINITY;
            double blue = (ax != px) ? ((py - ay) / (px - ax)) : Double.POSITIVE_INFINITY;
            return (blue >= red);
        }

    }

    //    James UTR09
    private void askUserDisableBroadcast() {
        //Check In Alert Dialog
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(BroadcastActivity.this);
        alertDialogBuilder.setMessage(getResources().getString(R.string.alert_title_disable_broadcast_ask));
        alertDialogBuilder.setPositiveButton(getResources().getString(R.string.button_disable_broadcast),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        //------------------------Emergency Status: ENABLE EMERGENCY----------------------//
                        try {
//                            --Execute broadcast disabler--//
                            isBroadcast = false;
                            loadingWindowDisableBroadcast = KProgressHUD.create(BroadcastActivity.this)
                                    .setStyle(KProgressHUD.Style.SPIN_INDETERMINATE)
                                    .setBackgroundColor(Color.parseColor("#508AF1F7"))
                                    .setLabel(getResources().getString(R.string.loading_label_please_wait))
                                    .setDetailsLabel(getResources().getString(R.string.loading_details_disabling_broadcast))
                                    .setCancellable(true)
                                    .setAnimationSpeed(2)
                                    .setDimAmount(0.5f)
                                    .show();
                            progressDialogIsShown = true;
                            //END OF: Start loading window
                            Alerter.create(BroadcastActivity.this).setText(getResources().getString(R.string.alert_title_confirm_disable)).setBackgroundColorRes(R.color.colorAccent).show();
                            tombolBroadcast.setText(getResources().getString(R.string.button_enable_broadcast));
                            //------//
                        } catch (Exception e) {
                            Alerter.create(BroadcastActivity.this).setTitle(getResources().getString(R.string.alert_title_failed_disable_broadcast)).setText(getResources().getString(R.string.alert_msg_failed_disable_broadcast)).setBackgroundColorRes(R.color.colorAccent).show();
                        }
                    }
                });

        alertDialogBuilder.setNegativeButton(getResources().getString(R.string.button_cancel),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //------------------------Emergency Status: DISABLE EMERGENCY----------------------//
//                            Alerter.create(BroadcastActivity.this).setTitle("You selected no!").setText("Emergency status set to false").setBackgroundColorRes(R.color.colorAccent).show();
                    }
                });

        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    // Notification
    private void notifyInfo(String msg) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        String NOTIFICATION_CHANNEL_ID = "info_01";

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
        notificationBuilder.setAutoCancel(true)
                .setDefaults(Notification.DEFAULT_VIBRATE)
                .setWhen(System.currentTimeMillis())
                .setSmallIcon(R.drawable.jadylogo1)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.jadylogo_254))
                .setTicker("Hearty365")
                .setContentTitle("JadyTrack Info")
                .setContentText(msg)
                .setContentInfo("Info")
                .setSound(Uri.parse("android.resource://" + getApplicationContext().getPackageName() + "/" + R.raw.notification_info));

        notificationManager.notify(1, notificationBuilder.build());
    }

    private void notifyAlert(String msg) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        String NOTIFICATION_CHANNEL_ID = "alert_01";

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
        notificationBuilder.setAutoCancel(true)
                .setDefaults(Notification.DEFAULT_ALL)
                .setWhen(System.currentTimeMillis())
                .setSmallIcon(R.drawable.jadylogo1)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.jadylogo_254))
                .setTicker("Hearty365")
                .setContentTitle("JadyTrack Alert")
                .setContentText(msg)
                .setContentInfo("Alert")
                .setSound(Uri.parse("android.resource://" + getApplicationContext().getPackageName() + "/" + R.raw.notification_alert));

        notificationManager.notify(1, notificationBuilder.build());
        //    *--Functions for ShortTrackingId--//
    }

    public String extractAlphanumeric(String inputString) {
        // inputString = "123^&*^&*^asdasdsad";
        return inputString.replaceAll("[^A-Za-z0-9]", "");
    }

    public String buildShortTrackingId(String nama, String fullTrackingId) {
        //* Extract First Name Only
        String[] splitName = nama.split(" ", 2);
        String firstName = splitName[0];
        //* Build shortTrackingId
//        Toast.makeText(getApplicationContext(), "this is the shortTrackingId: " + shortTrackingId, Toast.LENGTH_LONG).show();
        return extractAlphanumeric(firstName) + fullTrackingId.substring(fullTrackingId.length() - 4); //String shortTrackingId
    }

    public void writeShortTrackingIdToDatabase(String shortTrackingId) {
        try {
            shortTrackingIdDatabaseReference.child(shortTrackingId).setValue(id);
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), e.toString(), Toast.LENGTH_LONG).show();
        }
    }

}