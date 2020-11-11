package com.jady.jadytrack.activity;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

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

import java.lang.reflect.Field;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;
import java.util.Vector;

public class QuickRouteActivity extends AppCompatActivity
        implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        OnMapReadyCallback,
        GoogleMap.OnMarkerClickListener,
        ResultCallback<Status> {

    private static final String TAG = MainActivity.class.getSimpleName();

    public static final String EXTRA_MESSAGE_ID = "com.jady.jadytrack.SENDID";

    private String targetId = "Test juga";
    private Double longitude;
    private Double latitude;
    private Long time;

    private boolean isGeofenceDrawed = false;
    private boolean isSent = false;

    private Double destLongitude;
    private Double destLatitude;
    private Long destRadius;

    private Double markerLongitude;
    private Double markerLatitude;

    private DatabaseReference databaseReference;
    private DatabaseReference geofenceReference;

    // Google Map
    private GoogleMap map;
    private GoogleApiClient googleApiClient; // Untuk membuat lokasinya center
    private Location lastLocation = new Location(LocationManager.GPS_PROVIDER);
    private Marker locationMarker; // Marker untuk current location
    private Marker destinationMarker; // Marker untuk destination location

    // Array list marker
    private List<Marker> markers = new ArrayList<>();
    private List<Marker> drawer = new ArrayList<>();

    public QuickRouteActivity() {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.quick_route);

        // Load Name and UserID from MainMenu Intent
        Intent intentKu = getIntent();
        /*targetId = intentKu.getStringExtra(AppointmentActivity.EXTRA_MESSAGE_ID);
        String userUID = intentKu.getStringExtra(AppointmentActivity.EXTRA_MESSAGE_UID);*/

        targetId = intentKu.getStringExtra(InputRouteActivity.EXTRA_MESSAGE_ID);
        String userUID = intentKu.getStringExtra(InputRouteActivity.EXTRA_MESSAGE_UID);

        // initialize GoogleMaps
        initGMaps();
        // create GoogleApiClient
        createGoogleApi();

        // Firebase
        DatabaseReference locationReference = FirebaseDatabase.getInstance().getReference("trackingSession/" + targetId);
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
        Button setAppointment = (Button) findViewById(R.id.setgeofence);
        setAppointment.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (destinationMarker != null) {
                    // If a geofence markers minimal 3 exists (minimal bikin triangle):
                    if (markers.size() >= 3) {

                        // Try Catch for Uploading geofence data, destination point, and notification data to Firebase
                        try {

                            geofenceReference = FirebaseDatabase.getInstance().getReference().child("trackingSession/" + targetId);

                            geofenceReference.addValueEventListener(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    if (!dataSnapshot.hasChild("geofence") && !isSent) {
                                        sendtoFirebase();
                                        isSent = true;

                                        Intent intent = new Intent(QuickRouteActivity.this, TrackingActivity.class);
                                        intent.putExtra(EXTRA_MESSAGE_ID, targetId);
                                        startActivity(intent);
                                    } else if (!isSent) {
                                        Alerter.create(QuickRouteActivity.this).setTitle("Unable to set geofence").setText("Unable to set geofence because geofence has been created previously").setBackgroundColorRes(R.color.colorAccent).show();
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {

                                }
                            });


                        } catch (Exception e) {
                            Alerter.create(QuickRouteActivity.this).setTitle("Oops something went wrong!").setText("We failed to set the object on the database").setBackgroundColorRes(R.color.colorAccent).show();

                        }
                    } else {
                        Alerter.create(QuickRouteActivity.this).setTitle("You forgot to draw your geofence!").setText("Please draw at least three points for your geofence").setBackgroundColorRes(R.color.colorAccent).show();

                    }
                } else {
                    Alerter.create(QuickRouteActivity.this).setText("Please set your quickroute").setBackgroundColorRes(R.color.colorAccent).show();
                }

            }
        });


        // ini untuk mengambil history geofence yang pernah dibuat sebelumnya
        DatabaseReference historyReference = FirebaseDatabase.getInstance().getReference().child("users/" + userUID + "/trackingHistory");


        // SPINNER
        // Get reference of SpinnerView from layout/main_activity.xml
        final Spinner spinnerDropDown = (Spinner) findViewById(R.id.spinner);
        final ArrayList<String> historyTime = new ArrayList<String>();
        final ArrayList<String> historyId = new ArrayList<String>();
        final boolean[] isHistoryCollected = {false};
        historyTime.add("TAP TO SELECT QUICK ROUTE");


        historyReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.getValue() != null && !isHistoryCollected[0]) {

                    HashMap<String, Object> data =
                            (HashMap<String, Object>) dataSnapshot.getValue();

                    for (HashMap.Entry<String, Object> entry : data.entrySet()) {

                        // epoch to date
                        String dataTime = entry.getValue().toString();

                        Date date = new Date(Long.parseLong(dataTime));
                        DateFormat format = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
                        format.setTimeZone(TimeZone.getTimeZone("Asia/Jakarta"));
                        String formatted = format.format(date);

                        historyTime.add(formatted);

                        historyId.add(entry.getKey());
                    }
                    isHistoryCollected[0] = true;

                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });


        try {
            Field popup = Spinner.class.getDeclaredField("mPopup");
            popup.setAccessible(true);

            // Get private mPopup member variable and try cast to ListPopupWindow
            android.widget.ListPopupWindow popupWindow = (android.widget.ListPopupWindow) popup.get(spinnerDropDown);

            // Set popupWindow height to 500px
            popupWindow.setHeight(500);
        } catch (NoClassDefFoundError | ClassCastException | NoSuchFieldException | IllegalAccessException e) {
            // silently fail...
        }

        // masukkin array history ke dalam spinner
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.
                R.layout.simple_spinner_dropdown_item, historyTime);

        spinnerDropDown.setAdapter(adapter);

        spinnerDropDown.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int position, long id) {
                // Get select item
                int sid = spinnerDropDown.getSelectedItemPosition();
                if (sid > 0) {
                    // ini digunakan untuk menggambar geofence di mapnya
                    plotGeofence(historyId.get(sid - 1));
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // TODO Auto-generated method stub
            }
        });


    }

    public void sendtoFirebase() {
        //---------Firebase Preparation---------//
        databaseReference = FirebaseDatabase.getInstance().getReference().child("trackingSession");
        //END OF: Firebase Preparation---------//

        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.hasChild(targetId)) {

                    //Add Destination Point into Firebase
                    DestinationPoint currentDestinationPoint = new DestinationPoint(destinationMarker.getPosition().latitude, destinationMarker.getPosition().longitude, GEOFENCE_RADIUS);
                    databaseReference.child(targetId).child("destination").setValue(currentDestinationPoint);

                    //Add Notifications into Firebase
                    Notification currentNotifications = new Notification(false, true, false, false, false); //TODO: changeme
                    databaseReference.child(targetId).child("notifications").setValue(currentNotifications);

                    //Add Geofence Markers into Firebase
                    for (int counterMarkerNumber = 0; counterMarkerNumber < markers.size(); counterMarkerNumber++) {//mulai dari 1 utk skip yang 0
                        GeofencePoint temp = new GeofencePoint(markers.get(counterMarkerNumber).getPosition().latitude, markers.get(counterMarkerNumber).getPosition().longitude);
                        databaseReference.child(targetId).child("geofence").child(Integer.toString(counterMarkerNumber + 1)).setValue(temp);
                    }

                    //Add Geofence Numbers into Firebase
                    databaseReference.child(targetId).child("geofenceNum").setValue(markers.size());

                    Alerter.create(QuickRouteActivity.this).setText("Successfully synchronized Geofence online").setBackgroundColorRes(R.color.colorAccent).show();
                } else {
                    Alerter.create(QuickRouteActivity.this).setTitle("Oh no we failed to find tracking ID").setText("ID doesn't exist").setBackgroundColorRes(R.color.colorAccent).show();

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

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
        map.setOnMarkerClickListener(this);
    }


    // untuk menggambar geofence
    public void plotGeofence(String trackingId) {

        if (isGeofenceDrawed) {
            polygon.remove();
            destinationFence.remove();
            destinationMarker.remove();
            while (markers.size() != 0) {
                markers.get(markers.size() - 1).remove();
                markers.remove(markers.size() - 1);
            }
            isGeofenceDrawed = false;
        }

        //map.clear();
        DatabaseReference plotReference = FirebaseDatabase.getInstance().getReference("trackingSession/" + trackingId);
        plotReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                if (dataSnapshot.hasChild("geofenceNum") && !isGeofenceDrawed) {

                    ArrayList<Object> geofence = (ArrayList<Object>) dataSnapshot.child("geofence").getValue();
                    int geofenceSize = geofence.size() - 1;
                    int geofenceNum = (Integer) Integer.parseInt((String) dataSnapshot.child("geofenceNum").getValue().toString());

                    //Toast.makeText(getApplicationContext(), "geofenceSize "+ geofenceSize + " geofenceNum " + geofenceNum, Toast.LENGTH_SHORT).show();

                    if (geofenceNum == geofenceSize) {
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

                        destinationMarker = map.addMarker(markerOptions);

                        CircleOptions circleOptions = new CircleOptions()
                                .center(destinationMarker.getPosition())
                                .strokeColor(Color.argb(50, 70, 70, 70))
                                .fillColor(Color.argb(100, 150, 150, 150))
                                .radius(destRadius);
                        destinationFence = map.addCircle(circleOptions);


                        // Ini untuk menggambar geofencenya
                        //Toast.makeText(BroadcastActivity.this, "Target (ini kenapa??)"+ geofenceSize, Toast.LENGTH_LONG).show();

                        for (int i = 1; i <= geofenceSize; i++) {

                            HashMap<String, Object> geofenceIndex = (HashMap<String, Object>) dataSnapshot.child("geofence").child(Integer.toString(i)).getValue();

                            //Toast.makeText(TrackingActivity.this, "Target "+geofenceIndex, Toast.LENGTH_LONG).show();

                            for (HashMap.Entry<String, Object> entry : geofenceIndex.entrySet()) {
                                if (entry.getKey().equals("longitude")) {
                                    markerLongitude = (Double) entry.getValue();
                                }
                                if (entry.getKey().equals("latitude")) {
                                    markerLatitude = (Double) entry.getValue();
                                }
                            }

                            Marker marker = map.addMarker(new MarkerOptions().icon(BitmapDescriptorFactory.fromResource(R.drawable.dot15px)).position(new LatLng(markerLatitude, markerLongitude)));
                            markers.add(marker);
                        }

                        drawer = new ArrayList<Marker>(markers);

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

                        isGeofenceDrawed = true;
                    }
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }


    @Override
    public boolean onMarkerClick(Marker marker) {
        Log.d(TAG, "onMarkerClickListener: " + marker.getPosition());
        return false;
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
        String title = latLng.latitude + ", " + latLng.longitude;
        MarkerOptions markerOptions = new MarkerOptions()
                .position(latLng)
                .title(title)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.smile));
        if (map != null) {
            if (locationMarker != null)
                locationMarker.remove();
            locationMarker = map.addMarker(markerOptions);

            float zoom = 17f;
            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, zoom);
            map.animateCamera(cameraUpdate);


        }
    }

    // Attribute untuk membuat geofence bulet
    private static final float GEOFENCE_RADIUS = 20.0f; // in meters

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
    private Circle startingFence;
    private Circle destinationFence;
    private Polygon polygon;
    private Polyline polyline;

    private void drawGeofence() {
        Log.d(TAG, "drawGeofence()");

        // Jumlah marker
        int startingpoint = 0;
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

        } while (p != l);

        drawer = new ArrayList<Marker>(hull);
    }

}
