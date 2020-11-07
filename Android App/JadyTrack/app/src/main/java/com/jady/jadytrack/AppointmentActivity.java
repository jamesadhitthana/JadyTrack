package com.jady.jadytrack;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.database.ValueEventListener;
import com.tapadoo.alerter.Alerter;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

public class AppointmentActivity extends AppCompatActivity
        implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener,
        OnMapReadyCallback,
        GoogleMap.OnMapClickListener,
        ResultCallback<Status> {

    private static final String TAG = MainActivity.class.getSimpleName();

    // Attribut
    private String nama = "Test Aja";
    private String userUID = "Test Juga";
    public static final String EXTRA_MESSAGE_ID = "com.example.yeftaprototypev2.SENDID";
    public static final String EXTRA_MESSAGE_UID = "com.example.yeftaprototypev2.SENDUID";
    private Double longitude;
    private Double latitude;
    private Long time;


    private boolean geofenceActivated = false; //set geofence not activated by default
    private DatabaseReference databaseReference;
    private DatabaseReference locationReference;
    private DatabaseReference geofenceReference;
    private DatabaseReference historyReference;
    private String id;
    // Google Map
    private GoogleMap map;
    private GoogleApiClient googleApiClient; // Untuk membuat lokasinya center
    private Location lastLocation = new Location(LocationManager.GPS_PROVIDER);
    private MapFragment mapFragment; // Fragment peta
    private Marker locationMarker; // Marker untuk current location
    private Marker destinationMarker; // Marker untuk destination location
    private boolean isCameraUpdate = false;
    private boolean isSent = false;

    // Jumlah marker
    private int startingpoint = 0;

    // Widget
    private Button setStartingPoint;
    private Button setAppointment;
    private Button quickroute;
    private Button setCheckIn;
    private Button setSOS; //set SOS button
    private Button goToTarget; //temporary variable nunggu yefta gabungin -james

    // Array list marker
    private List<Marker> markers = new ArrayList<Marker>();
    private List<Marker> drawer = new ArrayList<Marker>();


    // Passing message ke kelas notif
    private static final String NOTIFICATION_MSG = "NOTIFICATION MSG";

    // Create a Intent send by the notifyTargetReachDestination
    public static Intent makeNotificationIntent(Context context, String msg) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra(NOTIFICATION_MSG, msg);
        return intent;
    }

    public boolean undo = false;
    public boolean manualCheckIn = false; // ini buat check in secara manual dan mengubah nilai hasArrived jadi true

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_appointment);
        //Load Name and UserID from MainMenu Intent passing
        Intent intentKu = getIntent();
        id = intentKu.getStringExtra(InputIdActivity.EXTRA_MESSAGE_ID);
        id = intentKu.getStringExtra(ScanQrActivity.EXTRA_MESSAGE_ID);
        userUID = intentKu.getStringExtra(InputIdActivity.EXTRA_MESSAGE_UID);
        userUID = intentKu.getStringExtra(ScanQrActivity.EXTRA_MESSAGE_UID);


        // initialize GoogleMaps
        initGMaps();
        // create GoogleApiClient
        createGoogleApi();

        // Firebase
        locationReference = FirebaseDatabase.getInstance().getReference("trackingSession/"+id);
        locationReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                HashMap<String, Object> location =
                        (HashMap<String, Object>) dataSnapshot.child("targetLocation").getValue();

                for(HashMap.Entry<String, Object> entry: location.entrySet()) {
                    if(entry.getKey().equals("longitude")) {
                        longitude = (Double) entry.getValue();
                        lastLocation.setLongitude(longitude);
                    }
                    if(entry.getKey().equals("latitude")){
                        latitude = (Double) entry.getValue();
                        lastLocation.setLatitude(latitude);
                    }
                    if(entry.getKey().equals("time")){
                        time = (Long) entry.getValue();
                    }
                }
                getLastKnownLocation();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        setStartingPoint = (Button) findViewById(R.id.undo);
        setStartingPoint.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (startingpoint == 1) {//if there is a destination point then
                    destinationMarker.remove();
                    destinationFence.remove();
                    startingpoint = 0;
                    isArrivedHasNotifiedUser = false;
                }
                if (startingpoint == 2) {
                    markers.get(markers.size() - 1).remove();
                    markers.remove(markers.size() - 1);
                    startingpoint = 1;
                }
                if (startingpoint == 3) {
                    polyline.remove();
                    markers.get(markers.size() - 1).remove();
                    markers.remove(markers.size() - 1);
                    startingpoint = 2;
                }
                if (startingpoint == 4) {
                    undoPolygon();
                }
                if (startingpoint > 4) {
                    undoPolygon();
                    drawGeofence();
                }
            }
        });

        //Set Geofence button will create the trackingSession ID and contents (@Yefta, maybe you should put your stuff here)
        setAppointment = (Button) findViewById(R.id.setgeofence);
        setAppointment.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {//Set Appointment Button
                //If a destination marker exists:
                if (destinationMarker != null) {
                    //If a geofence markers minimal 3 exists (minimal bikin triangle):
                    if (markers.size() >= 3) {

                        //Try Catch for Uploading geofence data, destination point, and notification data to Firebase
                        try {

                            geofenceReference = FirebaseDatabase.getInstance().getReference().child("trackingSession/"+id);

                            geofenceReference.addValueEventListener(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot dataSnapshot) {
                                    if(!dataSnapshot.hasChild("geofence") && !isSent){
                                        isSent = true;
                                        sendtoFirebase();

                                        Intent intent = new Intent(AppointmentActivity.this, TrackingActivity.class);
                                        intent.putExtra(EXTRA_MESSAGE_ID, id);
                                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                        // set the new task and clear flags
                                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                        startActivity(intent);
                                    }
                                    else if(!isSent){
                                        Alerter.create(AppointmentActivity.this).setTitle("Unable to set geofence").setText("Unable to set geofence because geofence has been created previously").setBackgroundColorRes(R.color.colorAccent).show();
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {

                                }
                            });


                        } catch (Exception e) {
                            Alerter.create(AppointmentActivity.this).setTitle("Oops something went wrong!").setText("We failed to set the object on the database").setBackgroundColorRes(R.color.colorAccent).show();
                        }
                    } else {
                        Alerter.create(AppointmentActivity.this).setTitle("You forgot to draw your geofence!").setText("Please draw at least three points for your geofence").setBackgroundColorRes(R.color.colorAccent).show();
                    }
                } else {
                    Alerter.create(AppointmentActivity.this).setTitle("You forgot to set your destination!").setText("Please set your destination by tapping on the desired destination").setBackgroundColorRes(R.color.colorAccent).show();
                }

            }
        });


        // Untuk memilih quickroute
        quickroute = (Button) findViewById(R.id.quickroute);
        quickroute.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {//Set Appointment Button
                Intent intent = new Intent(AppointmentActivity.this, QuickrouteActivity.class);
                intent.putExtra(EXTRA_MESSAGE_ID, id);
                intent.putExtra(EXTRA_MESSAGE_UID, userUID);
                startActivity(intent);
            }
        });


        // check in dan sos bukan fungsi dalam activity Appointment, jadi ini akan dipindahkan ke broadcast activity
        /*

        // Set up Buttons

        goToTarget = (Button) findViewById(R.id.buttonGoToTarget);//Broadcast button
        goToTarget.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (geofenceActivated) {
                    //TODO: @Yefta tolong gabungin activity BroadcastActivity ke-dalam activity AppointmentActivity (activity ini) -james
                    //TODO: @Yefta bikin supaya hal hal plotting, gambar polyline coordinate, dan tombol "Enable Broadcast" dan tombol "Get Tracking ID" bisa dipakai di activity ini
                    //gw send data nya sebagai intent ke dalam BroadcastActivity
                    Intent intentBroadcast = new Intent(getApplicationContext(), BroadcastActivity.class);
                    intentBroadcast.putExtra("trackingSession_key", id);//passing the key to the activity
                    intentBroadcast.putExtra("trackingSession_nama", nama);//passing the key to the activity
                    intentBroadcast.putExtra("trackingSession_userUID", userUID);//passing the key to the activity
                    startActivity(intentBroadcast);
                    Toast.makeText(getApplicationContext(), "Nanti activity \"BroadcastActivity\" bakalan hilang dan bakalan langsung digabung ke activity AppointmentActivity, (termasuk tombolnya)", Toast.LENGTH_LONG);
                    // Menyimpan data geofence ke firebase
                    //Geofence fence = new Geofence();
                    //databaseReference.child(id).setValue(fence);
                    // Membuka intent baru untuk memilih orang
                } else {
                    Toast.makeText(getApplicationContext(), "Please set the Geofence first.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        setCheckIn = (Button) findViewById(R.id.checkin);
        setCheckIn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (geofenceActivated) {
                    //Check In Alert Dialog
                    AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(AppointmentActivity.this);
                    alertDialogBuilder.setMessage("Are you sure you want to check in with your peers? You won't be able to cancel your decision and JadyTrack will automatically assume that you have arrived.");
                    alertDialogBuilder.setPositiveButton("Yes, Check me in!",
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface arg0, int arg1) {
                                    //------------------------Manual Check in----------------------//
                                    try {
                                        databaseReference.child(id).child("notifications").child("manualCheckIn").setValue(true);
                                        databaseReference.child(id).child("notifications").child("statusHasArrived").setValue(true);
                                        manualCheckIn = true;
                                        hasArrived = true;
                                        notifyTargetReachDestination();
                                        Toast.makeText(getApplicationContext(), "You have checked in manually!", Toast.LENGTH_SHORT).show();
                                    } catch (Exception e) {
                                        Toast.makeText(getApplicationContext(), "Failed to check in, please check your internet connection.", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });

                    alertDialogBuilder.setNegativeButton("No I'm not there yet!",
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Toast.makeText(AppointmentActivity.this, "You selected no.", Toast.LENGTH_SHORT).show();
                                }
                            });

                    AlertDialog alertDialog = alertDialogBuilder.create();
                    alertDialog.show();
                    //--
                } else {
                    Toast.makeText(getApplicationContext(), "Please set the Geofence first.", Toast.LENGTH_SHORT).show();
                }
            }
        });
        setSOS = (Button) findViewById(R.id.buttonSOS);
        setSOS.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (geofenceActivated) {
                    //Check In Alert Dialog
                    AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(AppointmentActivity.this);
                    alertDialogBuilder.setMessage("Are you sure you want to enable SOS (Emergency) mode? If you are connected to the internet, your peers will get notified of an emergency immediately.");
                    alertDialogBuilder.setPositiveButton("Yes, I'm in danger!",
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface arg0, int arg1) {
                                    //------------------------Emergency Status: ENABLE EMERGENCY----------------------//
                                    try {
                                        databaseReference.child(id).child("notifications").child("statusSOS").setValue(true);
                                        Toast.makeText(getApplicationContext(), "Emergency status sent!", Toast.LENGTH_SHORT).show();
                                    } catch (Exception e) {
                                        Toast.makeText(getApplicationContext(), "Failed to send emergency status, please check your internet connection.", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });

                    alertDialogBuilder.setNegativeButton("No I'm safe!",
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    //------------------------Emergency Status: DISABLE EMERGENCY----------------------//
                                    try {
                                        databaseReference.child(id).child("notifications").child("statusSOS").setValue(false);
                                        Toast.makeText(AppointmentActivity.this, "You selected no. \nEmergency status set to false.", Toast.LENGTH_SHORT).show();
                                    } catch (Exception e) {
                                        Toast.makeText(getApplicationContext(), "Failed to set Emergency status to false, if you did not previously enabled your emergency status then you are fine. \nIf you have, then please check your internet connection and try again.", Toast.LENGTH_LONG).show();
                                    }

                                }
                            });

                    AlertDialog alertDialog = alertDialogBuilder.create();
                    alertDialog.show();
                    //--
                } else {
                    Toast.makeText(getApplicationContext(), "Please set the Geofence first.", Toast.LENGTH_SHORT).show();
                }
            }
        });
        */



    }

    public void sendtoFirebase(){
        //---------Firebase Preparation---------//
        databaseReference = FirebaseDatabase.getInstance().getReference().child("trackingSession");
        historyReference = FirebaseDatabase.getInstance().getReference().child("users/"+userUID);
        //END OF: Firebase Preparation---------//

        Date date= new Date();
        long time = date.getTime();

        historyReference.child("trackingHistory").child(id).setValue(time);


        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.hasChild(id)) {
                        //Toast.makeText(getApplicationContext(), "[APPOINTMENTACTIVITY] Nama: " + nama + "\n[APPOINTMENTACTIVITY] User ID: " + userUID + "\n[APPOINTMENTACTIVITY] Key ID: " + id, Toast.LENGTH_LONG).show();

                        //Add Destination Point into Firebase
                        DestinationPoint currentDestinationPoint = new DestinationPoint(destinationMarker.getPosition().latitude, destinationMarker.getPosition().longitude, GEOFENCE_RADIUS);
                        databaseReference.child(id).child("destination").setValue(currentDestinationPoint);

                        //Add Notifications into Firebase
                        Notifications currentNotifications = new Notifications(false, true, false, false, false); //TODO: changeme
                        databaseReference.child(id).child("notifications").setValue(currentNotifications);

                        //Add Geofence Markers into Firebase
                        for (int counterMarkerNumber = 0; counterMarkerNumber < markers.size(); counterMarkerNumber++) {//mulai dari 1 utk skip yang 0
                            GeofencePoint temp = new GeofencePoint(markers.get(counterMarkerNumber).getPosition().latitude, markers.get(counterMarkerNumber).getPosition().longitude);
                            databaseReference.child(id).child("geofence").child(Integer.toString(counterMarkerNumber + 1)).setValue(temp);
                        }

                        //Add Geofence Numbers into Firebase
                        databaseReference.child(id).child("geofenceNum").setValue(markers.size());

                        geofenceActivated = true;//activate geofence
                        Alerter.create(AppointmentActivity.this).setText("Successfully synchronized Geofence online").setBackgroundColorRes(R.color.colorAccent).show();



                }
                else{
                    Alerter.create(AppointmentActivity.this).setTitle("Oh no we failed to find tracking ID").setText("ID doesn't exist").setBackgroundColorRes(R.color.colorAccent).show();

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });


    }


    /*public void showInputDialog(){
        // mempersiapkan input dialog di dalam setAppointment
        AlertDialog.Builder inputDialog = new AlertDialog.Builder(this);
        inputDialog.setTitle("Input Your Target Tracking Session(ID)");

        // Set up the input
        final EditText input = new EditText(this);
        // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        inputDialog.setView(input);

        // Set up the buttons
        inputDialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                id = input.getText().toString();
                sendtoFirebase();
            }
        });
        inputDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        inputDialog.show();
    }
*/

    public void undoPolygon() {
        polygon.remove();
        markers.get(markers.size() - 1).remove();
        markers.remove(markers.size() - 1);
        //koordinat yang ada di firebase juga diapus
        startingpoint -= 1;
        undo = true;
    }

    // Create GoogleApiClient instance
    private void createGoogleApi() {
        Log.d(TAG, "createGoogleApi()");
        if (googleApiClient == null) {
            googleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Call GoogleApiClient connection when starting the Activity
        googleApiClient.connect();

    }

    @Override
    protected void onStop() {
        super.onStop();

        // Disconnect GoogleApiClient when stopping Activity
        googleApiClient.disconnect();
    }


    // --- Untuk permission semua --- //
    private final int REQ_PERMISSION = 999;

    // Check for permission to access Location
    private boolean checkPermission() {
        Log.d(TAG, "checkPermission()");
        // Ask for permission if it wasn't granted yet
        return (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED);
    }

    // Asks for permission
    private void askPermission() {
        Log.d(TAG, "askPermission()");
        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                REQ_PERMISSION
        );
    }

    // Verify user's response of the permission requested
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.d(TAG, "onRequestPermissionsResult()");
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQ_PERMISSION: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission granted
                    getLastKnownLocation();

                } else {
                    // Permission denied
                    permissionsDenied();
                }
                break;
            }
        }
    }

    // App cannot work without the permissions
    private void permissionsDenied() {
        Log.w(TAG, "permissionsDenied()");
        // TODO close app and warn user
    }

    // Initialize GoogleMaps
    private void initGMaps() {
        mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    // Callback called when Map is ready
    @Override
    public void onMapReady(GoogleMap googleMap) {
        Log.d(TAG, "onMapReady()");
        map = googleMap;
        map.setOnMapClickListener(this);
    }

    public void createPolygon(LatLng latLng) {
        Marker marker = map.addMarker(new MarkerOptions().icon(BitmapDescriptorFactory.fromResource(R.drawable.dot15px)).position(latLng)); //...
        //cobabaru: MarkerOptions markerOptions = new MarkerOptions().icon(BitmapDescriptorFactory.fromResource(R.drawable.icon_location));
        //Buat masukin marker ke firebasenya, marker.getPosition().latitude buat latitude, marker.getPosition().longitude buat longitude
        markers.add(marker);
        startingpoint += 1;
    }

    @Override
    public void onMapClick(LatLng latLng) {
        Log.d(TAG, "onMapClick(" + latLng + ")");
        if (startingpoint == 0) {
            markerForDestination(latLng);
            Geofence geofence = createGeofence(destinationMarker.getPosition(), GEOFENCE_RADIUS);
            GeofencingRequest geofenceRequest = createGeofenceRequest(geofence);
            addGeofence(geofenceRequest);

            drawGeofence();

            startingpoint = 1;
            System.out.println("Stage 1 " + markers.size());
        } else if (startingpoint == 1) {
            createPolygon(latLng);
        } else if (startingpoint == 2) {
            createPolygon(latLng);
            drawGeofence();
        } else if (startingpoint > 2) {
            createPolygon(latLng);
            drawGeofence();
        }

    }


    // Untuk lokasi update
    private LocationRequest locationRequest;
    // Defined in mili seconds.
    // This number in extremely low, and should be used only for debug
    private final int UPDATE_INTERVAL = 1000;
    private final int FASTEST_INTERVAL = 900;

    // Start location Updates
    private void startLocationUpdates() {
        Log.i(TAG, "startLocationUpdates()");
        locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(UPDATE_INTERVAL)
                .setFastestInterval(FASTEST_INTERVAL);

        //if (checkPermission())
           // LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);
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

    private boolean isCrossed = false; //ini boolean kalo ngelewatin geofence
    private boolean isCrossedHasNotifiedUser = false;
    private boolean targetCrossingBorderHasNotifiedUser = false;
    private boolean targetInsideBorderHasNotifiedUser = false;
    private boolean hasArrived = false;  //yang ini boolean kalo udah nyampe otoomatis
    private boolean isArrivedHasNotifiedUser = false;

    public void notifyTargetReachDestination() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        String NOTIFICATION_CHANNEL_ID = "my_channel_id_01";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, "My Notifications", NotificationManager.IMPORTANCE_HIGH);

            // Configure the notifyTargetReachDestination channel.
            notificationChannel.setDescription("Channel description");
            notificationChannel.enableLights(true);
            notificationChannel.setLightColor(Color.RED);
            notificationChannel.setVibrationPattern(new long[]{0, 1000, 500, 1000});
            notificationChannel.enableVibration(true);
            notificationManager.createNotificationChannel(notificationChannel);
        }


        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);

        notificationBuilder.setAutoCancel(true)
                .setDefaults(Notification.DEFAULT_ALL)
                .setWhen(System.currentTimeMillis())
                .setSmallIcon(R.drawable.ic_action_location)
                .setTicker("Hearty365")
                //     .setPriority(Notification.PRIORITY_MAX)
                .setContentTitle("Geofence")
                .setContentText("Target has reached destination!")
                .setContentInfo("Info");

        notificationManager.notify(/*notifyTargetReachDestination id*/1, notificationBuilder.build());
    }

    // gak pakai ini lagi
    /*@Override
    public void onLocationChanged(Location location) {
        Log.d(TAG, "onLocationChanged [" + location + "]");
        lastLocation = location;
        writeActualLocation(location);

        if (startingpoint > 0) {
            float[] distance = new float[2];
            Location.distanceBetween(locationMarker.getPosition().latitude, locationMarker.getPosition().longitude, destinationFence.getCenter().latitude, destinationFence.getCenter().longitude, distance);

            if (distance[0] > destinationFence.getRadius()) {
                hasArrived = false;
            } else if (distance[0] < destinationFence.getRadius()) { //If user has entered the Final destination geofence.
                //------------------------Has Arrived in Finish Line----------------------//
                hasArrived = true; //Ini isArrivednya
                if (geofenceActivated) {
                    databaseReference.child(id).child("notifications").child("statusHasArrived").setValue(true);
                }
                if (isArrivedHasNotifiedUser == false) {//if the user has been notified ONCE, then dont notify again!
                    Toast.makeText(getApplicationContext(), "You have arrived at your destination.", Toast.LENGTH_SHORT).show();
                    notifyTargetReachDestination();
                }
                isArrivedHasNotifiedUser = true;

                //END OF: Has Arrived in Finish Line----------------------//
            }
        }

        if (startingpoint > 3) {
            if (!pointInPolygon(locationMarker.getPosition(), polygon)) {
                //------------------------Crossing Border----------------------//
                isCrossed = true;
                if (geofenceActivated) {
                    databaseReference.child(id).child("notifications").child("statusInGeofence").setValue(false);
                }
                targetInsideBorderHasNotifiedUser = false;//if target crosses border, then set the inside border notifyTargetReachDestination feature back on.
                if (targetCrossingBorderHasNotifiedUser == false) {//if the user has been notified ONCE, then dont notify again!
                    notifyTargetCrossedGeofence();
                }
                targetCrossingBorderHasNotifiedUser = true;
                //END OF: Crossing Border----------------------//
            }

            if (pointInPolygon(locationMarker.getPosition(), polygon)) {
                //------------------------Inside Border----------------------//
                isCrossed = false;
                if (geofenceActivated) {
                    databaseReference.child(id).child("notifications").child("statusInGeofence").setValue(true);
                }
                targetCrossingBorderHasNotifiedUser = false;//if target inside border, then set the crossing notifyTargetReachDestination feature back on.
                if (targetInsideBorderHasNotifiedUser == false) {//if the user has been notified ONCE, then dont notify again!
                    notifyTargetInsideGeofence();
                }
                targetInsideBorderHasNotifiedUser = true;
                //END OF: Inside Border----------------------//
            }
        }
    }*/

    private void notifyTargetInsideGeofence() {
        polygon.setFillColor(Color.argb(100, 150, 150, 150));
        Alerter.create(AppointmentActivity.this).setText("You are inside the geofence!").setBackgroundColorRes(R.color.colorAccent).show();

    }

    private void notifyTargetCrossedGeofence() {
        polygon.setFillColor(Color.RED);

        Alerter.create(AppointmentActivity.this).setText("Crossing the border!").setBackgroundColorRes(R.color.colorAccent).show();

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        String NOTIFICATION_CHANNEL_ID = "my_channel_id_01";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, "My Notifications", NotificationManager.IMPORTANCE_HIGH);

            // Configure the notifyTargetReachDestination channel.
            notificationChannel.setDescription("Channel description");
            notificationChannel.enableLights(true);
            notificationChannel.setLightColor(Color.RED);
            notificationChannel.setVibrationPattern(new long[]{0, 1000, 500, 1000});
            notificationChannel.enableVibration(true);
            notificationManager.createNotificationChannel(notificationChannel);
        }


        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);

        notificationBuilder.setAutoCancel(true)
                .setDefaults(Notification.DEFAULT_ALL)
                .setWhen(System.currentTimeMillis())
                .setSmallIcon(R.drawable.ic_action_location)
                .setTicker("Hearty365")
                //     .setPriority(Notification.PRIORITY_MAX)
                .setContentTitle("Geofence")
                .setContentText("Crossing the border")
                .setContentInfo("Info");

        notificationManager.notify(/*notifyTargetReachDestination id*/1, notificationBuilder.build());
    }

    // GoogleApiClient.ConnectionCallbacks connected
    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.i(TAG, "onConnected()");
        getLastKnownLocation();
    }

    // GoogleApiClient.ConnectionCallbacks suspended
    @Override
    public void onConnectionSuspended(int i) {
        Log.w(TAG, "onConnectionSuspended()");
    }

    // GoogleApiClient.OnConnectionFailedListener fail
    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.w(TAG, "onConnectionFailed()");
    }

    // Get last known location
    private void getLastKnownLocation() {
        Log.d(TAG, "getLastKnownLocation()");
        if (checkPermission()) {
            //lastLocation = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
            if (lastLocation != null) {
                Log.i(TAG, "LasKnown location. " +
                        "Long: " + lastLocation.getLongitude() +
                        " | Lat: " + lastLocation.getLatitude());

                markerLocation(new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude()));

                //startLocationUpdates();
            } else {
                Log.w(TAG, "No location retrieved yet");
                //startLocationUpdates();
            }
        } else askPermission();
    }

    private void markerLocation(LatLng latLng) {
        Log.i(TAG, "markerLocation(" + latLng + ")");
        //String title = latLng.latitude + ", " + latLng.longitude;
        MarkerOptions markerOptions = new MarkerOptions()
                .position(latLng)
                //.title(title)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.smile));
        if (map != null) {
            if (locationMarker != null)
                locationMarker.remove();
            locationMarker = map.addMarker(markerOptions);

            float zoom = 17f;
            if (latLng.latitude != 0 && latLng.longitude != 0) {
                CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, zoom);
                map.animateCamera(cameraUpdate);
            }

        }
    }

    private void markerForDestination(LatLng latLng) {
        String title = latLng.latitude + ", " + latLng.longitude;
        // Define marker options
        MarkerOptions markerOptions = new MarkerOptions()
                .position(latLng)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.finish));
                //.title(title);

        destinationMarker = map.addMarker(markerOptions);
    }


    // Attribute untuk membuat geofence bulet
    private static final long GEO_DURATION = 60 * 60 * 1000;
    private static final String GEOFENCE_REQ_ID = "My Geofence";
    private static final float GEOFENCE_RADIUS = 20.0f; // in meters

    // Create a Geofence
    private Geofence createGeofence(LatLng latLng, float radius) {
        Log.d(TAG, "createGeofence");
        return new Geofence.Builder()
                .setRequestId(GEOFENCE_REQ_ID)
                .setCircularRegion(latLng.latitude, latLng.longitude, radius)
                .setExpirationDuration(GEO_DURATION)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER
                        | Geofence.GEOFENCE_TRANSITION_EXIT)
                .build();
    }

    // Create a Geofence Request
    // kalau misalkan keluar, notifikasi dimunculkan
    private GeofencingRequest createGeofenceRequest(Geofence geofence) {
        Log.d(TAG, "createGeofenceRequest");
        return new GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofence(geofence)
                .build();
    }


    private PendingIntent geoFencePendingIntent;
    private final int GEOFENCE_REQ_CODE = 0;

    private PendingIntent createGeofencePendingIntent() {
        Log.d(TAG, "createGeofencePendingIntent");
        if (geoFencePendingIntent != null)
            return geoFencePendingIntent;

        Intent intent = new Intent(this, GeofenceTrasitionService.class);
        return PendingIntent.getService(
                this, GEOFENCE_REQ_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    // Add the created GeofenceRequest to the device's monitoring list
    private void addGeofence(GeofencingRequest request) {
        Log.d(TAG, "addGeofence");
        if (checkPermission())
            LocationServices.GeofencingApi.addGeofences(
                    googleApiClient,
                    request,
                    createGeofencePendingIntent()
            ).setResultCallback(this);
    }


    @Override
    public void onResult(@NonNull Status status) {
        Log.i(TAG, "onResult: " + status);
        if (status.isSuccess()) {
            //saveGeofence();
            //drawGeofence();
        } else {
            // inform about fail
        }
    }

    // Draw Geofence circle on GoogleMap
    private Circle startingFence;
    private Circle destinationFence;
    private Circle geoFence;
    private Polygon polygon;
    private Polyline polyline;

    private void drawGeofence() {
        Log.d(TAG, "drawGeofence()");

        if (startingpoint == 0) {
            if (destinationFence != null)
                destinationFence.remove();

            CircleOptions circleOptions = new CircleOptions()
                    .center(destinationMarker.getPosition())
                    .strokeColor(Color.argb(50, 70, 70, 70))
                    .fillColor(Color.argb(100, 150, 150, 150))
                    //.radius( GEOFENCE_RADIUS );
                    .radius(GEOFENCE_RADIUS);
            destinationFence = map.addCircle(circleOptions);
        }

        if (startingpoint == 3) {

            if (polyline != null) {
                polyline.remove();
            }

            polyline = map.addPolyline(
                    new PolylineOptions()
                            .add(markers.get(0).getPosition())
                            .add(markers.get(1).getPosition())
                            .width(10f)
                            .color(Color.argb(50, 70, 70, 70))
            );
        }

        if (startingpoint > 3) {

            polyline.remove();

            if (polygon != null) {
                polygon.remove();
            }

            if (undo == true) {
                drawer = new ArrayList<Marker>(markers);
                undo = false;
            }

            convexHull(markers.size());

            PolygonOptions polygonOptions = new PolygonOptions();
            polygonOptions.add(drawer.get(0).getPosition());

            for (int i = 1; i < drawer.size(); i++) {
                //polygonOptions.add(new LatLng(array[i].a, array[i].b));
                polygonOptions.add(drawer.get(i).getPosition());
            }

            polygonOptions.add(drawer.get(0).getPosition());

            polygonOptions.strokeColor(Color.argb(50, 70, 70, 70));
            polygonOptions.fillColor(Color.argb(100, 150, 150, 150));


            polygon = map.addPolygon(polygonOptions);

        }

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

        drawer = new ArrayList<Marker>(hull);
        // Print Result
        /*for (Marker temp : hull)
            System.out.println("(" + temp.x + ", " +
                    temp.y + ")");
        */
    }


    @Override
    public void onLocationChanged(Location location) {

    }
}
