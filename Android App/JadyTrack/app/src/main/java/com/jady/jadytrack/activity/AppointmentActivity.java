package com.jady.jadytrack.activity;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationListener;
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
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.jady.jadytrack.R;
import com.jady.jadytrack.entity.DestinationPoint;
import com.jady.jadytrack.entity.GeofencePoint;
import com.jady.jadytrack.entity.Notification;
import com.jady.jadytrack.service.GeofenceTrasitionService;
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

    public static final String EXTRA_MESSAGE_ID = "com.jady.jadytrack.SENDID";
    public static final String EXTRA_MESSAGE_UID = "com.jady.jadytrack.SENDUID";
    private static final String NOTIFICATION_MSG = "com.jady.notifiction";

    private String userUID;
    private Double longitude;
    private Double latitude;
    private Long time;

    private DatabaseReference databaseReference;
    private DatabaseReference geofenceReference;
    private String id;
    // Google Map
    private GoogleMap map;
    private GoogleApiClient googleApiClient; // Untuk membuat lokasinya center
    private Location lastLocation = new Location(LocationManager.GPS_PROVIDER);
    private Marker locationMarker; // Marker untuk current location
    private Marker destinationMarker; // Marker untuk destination location
    private boolean isSent = false;

    // Jumlah marker
    private int startingpoint = 0;

    // Array list marker
    private List<Marker> markers = new ArrayList<>();
    private List<Marker> drawer = new ArrayList<>();

    // Create a Intent send by the notifyTargetReachDestination
    public static Intent makeNotificationIntent(Context context, String msg) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra(NOTIFICATION_MSG, msg);
        return intent;
    }

    public boolean undo = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.appointment);

        // Load Name and UserID from MainMenu Intent passing
        Intent intentKu = getIntent();
        id = intentKu.getStringExtra(InputIdActivity.EXTRA_MESSAGE_ID);
        id = intentKu.getStringExtra(ScanQrActivity.EXTRA_MESSAGE_ID);
        userUID = intentKu.getStringExtra(InputIdActivity.EXTRA_MESSAGE_UID);
        userUID = intentKu.getStringExtra(ScanQrActivity.EXTRA_MESSAGE_UID);

        // Initialize GoogleMaps
        initGMaps();
        // Create GoogleApiClient
        createGoogleApi();

        // Firebase
        DatabaseReference locationReference = FirebaseDatabase.getInstance().getReference("trackingSession/" + id);
        locationReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                HashMap<String, Object> location =
                        (HashMap<String, Object>) dataSnapshot.child("targetLocation").getValue();

                for (HashMap.Entry<String, Object> entry : location.entrySet()) {
                    if (entry.getKey().equals("longitude")) {
                        longitude = (Double) entry.getValue();
                        lastLocation.setLongitude(longitude);
                    }
                    if (entry.getKey().equals("latitude")) {
                        latitude = (Double) entry.getValue();
                        lastLocation.setLatitude(latitude);
                    }
                    if (entry.getKey().equals("time")) {
                        time = (Long) entry.getValue();
                    }
                }
                getLastKnownLocation();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        // Widget
        Button setStartingPoint = (Button) findViewById(R.id.undo);
        setStartingPoint.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (startingpoint == 1) {//if there is a destination point then
                    destinationMarker.remove();
                    destinationFence.remove();
                    startingpoint = 0;
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

        // Set Geofence button will create the trackingSession ID and contents (@Yefta, maybe you should put your stuff here)
        Button setAppointment = (Button) findViewById(R.id.setgeofence);
        setAppointment.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {//Set Appointment Button
                // If a destination marker exists:
                if (destinationMarker != null) {
                    // If a geofence markers minimal 3 exists (minimal bikin triangle):
                    if (markers.size() >= 3) {

                        // Try Catch for Uploading geofence data, destination point, and notification data to Firebase
                        try {

                            geofenceReference = FirebaseDatabase.getInstance().getReference().child("trackingSession/" + id);

                            geofenceReference.addValueEventListener(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    if (!dataSnapshot.hasChild("geofence") && !isSent) {
                                        isSent = true;
                                        sendtoFirebase();

                                        Intent intent = new Intent(AppointmentActivity.this, TrackingActivity.class);
                                        intent.putExtra(EXTRA_MESSAGE_ID, id);
                                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                        // Set the new task and clear flags
                                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                        startActivity(intent);
                                    } else if (!isSent) {
                                        Alerter.create(AppointmentActivity.this).setTitle(getResources().getString(R.string.alert_title_geofence)).setText(getResources().getString(R.string.alert_msg_geofence)).setBackgroundColorRes(R.color.colorAccent).show();
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {

                                }
                            });


                        } catch (Exception e) {
                            Alerter.create(AppointmentActivity.this).setTitle(getResources().getString(R.string.alert_title_failed_set_object)).setText(getResources().getString(R.string.alert_msg_failed_set_object)).setBackgroundColorRes(R.color.colorAccent).show();
                        }
                    } else {
                        Alerter.create(AppointmentActivity.this).setTitle(getResources().getString(R.string.alert_title_forgot_draw)).setText(getResources().getString(R.string.alert_msg_forgot_draw)).setBackgroundColorRes(R.color.colorAccent).show();
                    }
                } else {
                    Alerter.create(AppointmentActivity.this).setTitle(getResources().getString(R.string.alert_title_forgot_destination)).setText(getResources().getString(R.string.alert_msg_forgot_destination)).setBackgroundColorRes(R.color.colorAccent).show();
                }

            }
        });


        // To choose Quick Route
        Button quickroute = (Button) findViewById(R.id.quickroute);
        quickroute.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent(AppointmentActivity.this, QuickRouteActivity.class);
                intent.putExtra(EXTRA_MESSAGE_ID, id);
                intent.putExtra(EXTRA_MESSAGE_UID, userUID);
                startActivity(intent);
            }
        });

    }

    public void sendtoFirebase() {
        //---------Firebase Preparation---------//
        databaseReference = FirebaseDatabase.getInstance().getReference().child("trackingSession");
        DatabaseReference historyReference = FirebaseDatabase.getInstance().getReference().child("users/" + userUID);
        //END OF: Firebase Preparation---------//

        Date date = new Date();
        long time = date.getTime();

        historyReference.child("trackingHistory").child(id).setValue(time);

        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.hasChild(id)) {

                    // Add Destination Point into Firebase
                    DestinationPoint currentDestinationPoint = new DestinationPoint(destinationMarker.getPosition().latitude, destinationMarker.getPosition().longitude, GEOFENCE_RADIUS);
                    databaseReference.child(id).child("destination").setValue(currentDestinationPoint);

                    // Add Notifications into Firebase
                    Notification currentNotifications = new Notification(false, true, false, false, false); //TODO: changeme
                    databaseReference.child(id).child("notifications").setValue(currentNotifications);

                    // Add Geofence Markers into Firebase
                    for (int counterMarkerNumber = 0; counterMarkerNumber < markers.size(); counterMarkerNumber++) {//mulai dari 1 utk skip yang 0
                        GeofencePoint temp = new GeofencePoint(markers.get(counterMarkerNumber).getPosition().latitude, markers.get(counterMarkerNumber).getPosition().longitude);
                        databaseReference.child(id).child("geofence").child(Integer.toString(counterMarkerNumber + 1)).setValue(temp);
                    }

                    // Add Geofence Numbers into Firebase
                    databaseReference.child(id).child("geofenceNum").setValue(markers.size());

                    Alerter.create(AppointmentActivity.this).setText(getResources().getString(R.string.alert_title_synchronize_geofence)).setBackgroundColorRes(R.color.colorAccent).show();

                } else {
                    Alerter.create(AppointmentActivity.this).setTitle(getResources().getString(R.string.alert_title_failed_find_id)).setText(getResources().getString(R.string.alert_msg_failed_find_id)).setBackgroundColorRes(R.color.colorAccent).show();

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });


    }

    public void undoPolygon() {
        polygon.remove();
        markers.get(markers.size() - 1).remove();
        markers.remove(markers.size() - 1);
        // Delete coordinate data in database
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


    // --- For all permission --- //
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
        if (requestCode == REQ_PERMISSION) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted
                getLastKnownLocation();

            } else {
                // Permission denied
                permissionsDenied();
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
        // Fragment peta
        MapFragment mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map);
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
        markers.add(marker);
        startingpoint += 1;
    }

    @Override
    public void onMapClick(LatLng latLng) {
        Log.d(TAG, "onMapClick(" + latLng + ")");
        if (startingpoint == 0) {
            markerForDestination(latLng);
            Geofence geofence = createGeofence(destinationMarker.getPosition());
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
            if (lastLocation != null) {
                Log.i(TAG, "LasKnown location. " +
                        "Long: " + lastLocation.getLongitude() +
                        " | Lat: " + lastLocation.getLatitude());

                markerLocation(new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude()));

            } else {
                Log.w(TAG, "No location retrieved yet");
            }
        } else askPermission();
    }

    private void markerLocation(LatLng latLng) {
        Log.i(TAG, "markerLocation(" + latLng + ")");
        MarkerOptions markerOptions = new MarkerOptions()
                .position(latLng)
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
        // Define marker options
        MarkerOptions markerOptions = new MarkerOptions()
                .position(latLng)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.finish));

        destinationMarker = map.addMarker(markerOptions);
    }


    // Attribute untuk membuat geofence bulet
    private static final long GEO_DURATION = 60 * 60 * 1000;
    private static final String GEOFENCE_REQ_ID = "My Geofence";
    private static final float GEOFENCE_RADIUS = 20.0f; // in meters

    // Create a Geofence
    private Geofence createGeofence(LatLng latLng) {
        Log.d(TAG, "createGeofence");
        return new Geofence.Builder()
                .setRequestId(GEOFENCE_REQ_ID)
                .setCircularRegion(latLng.latitude, latLng.longitude, AppointmentActivity.GEOFENCE_RADIUS)
                .setExpirationDuration(GEO_DURATION)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER
                        | Geofence.GEOFENCE_TRANSITION_EXIT)
                .build();
    }

    // Create a Geofence Request
    // Give notification when the user exits the Geofence
    private GeofencingRequest createGeofenceRequest(Geofence geofence) {
        Log.d(TAG, "createGeofenceRequest");
        return new GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofence(geofence)
                .build();
    }


    private PendingIntent geoFencePendingIntent;

    private PendingIntent createGeofencePendingIntent() {
        Log.d(TAG, "createGeofencePendingIntent");
        if (geoFencePendingIntent != null)
            return geoFencePendingIntent;

        Intent intent = new Intent(this, GeofenceTrasitionService.class);
        int GEOFENCE_REQ_CODE = 0;
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
    }

    // Draw Geofence circle on GoogleMap
    private Circle destinationFence;
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

            if (undo) {
                drawer = new ArrayList<>(markers);
                undo = false;
            }

            convexHull(markers.size());

            PolygonOptions polygonOptions = new PolygonOptions();
            polygonOptions.add(drawer.get(0).getPosition());

            for (int i = 1; i < drawer.size(); i++) {
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

        if (val == 0) return 0;  // Collinear
        return (val > 0) ? 1 : 2; // Clock or counterclock wise
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

        drawer = new ArrayList<>(hull);
    }


    @Override
    public void onLocationChanged(Location location) {

    }
}
