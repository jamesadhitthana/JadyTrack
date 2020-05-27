package com.jamesgalaxy.jadytrackui;

import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Handler;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.fragment.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.maps.CameraUpdate;
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
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

public class TrackingActivity extends AppCompatActivity implements
        OnMapReadyCallback{

    // Google Map
    private GoogleMap mMap;
    private Double longitude;
    private Double latitude;
    private Long time;

    private Double destLongitude;
    private Double destLatitude;
    private Long destRadius;

    private Double markerLongitude;
    private Double markerLatitude;

    private Marker destinationMarker;
    private Circle destinationFence;

    private List<Marker> markers = new ArrayList<Marker>();
    private List<Marker> drawer = new ArrayList<Marker>();

    private Polygon polygon;
    private Polyline polyline;

    int geofenceSize;

    // Marker
    private BitmapDescriptor icon;
    private LatLng markerPosition = new LatLng(0,0);
    private Marker currentMarker;
    private MarkerOptions markerOptions;

    // Attribute
    private String nama;
    private String id;
    private int numMarker = 1; // gambar pointnya akan dimulai dari numMarker terakhir


    // Handler
    private boolean internetStatus = true;
    private Boolean isTargetOnline = true;
    private boolean isFirstMarker = true;
    private boolean isMarkerDrawed = false;
    private boolean isGeofenceDrawed = false;

    private boolean statusHasArrived = false;
    private boolean statusInGeofence = true;
    private boolean statusLinkExpired = false;
    private boolean statusSOS = false;
    private boolean manualCheckIn = false;

    private boolean isArrivedNotified = false;
    private boolean isGeofenceNotified = false;
    private boolean isSOSNotified = false;
    private boolean isOfflineNotified = false;

    // Widget
    private TextView targetStatus;

    // Polyline
    PolylineOptions options = new PolylineOptions().width(5).color(Color.BLUE).geodesic(true);

    // Firebase reference
    private DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tracking);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // Menerima ID
        Intent intent = getIntent();
        id = intent.getStringExtra(InputIdActivity.EXTRA_MESSAGE_ID);

        // Firebase
        databaseReference = FirebaseDatabase.getInstance().getReference("trackingSession/"+id);

        // Mengatur tampilan awal
        targetStatus = (TextView) findViewById(R.id.targetStatus);
        targetStatus.setText("Target is offline");

        // Internet Connection handler
        final Handler handler = new Handler();
        Runnable runnable = new Runnable() {

            @Override
            public void run() {
                try{
                    if (!isConnected(TrackingActivity.this)){
                        if(internetStatus == true){
                            internetDialog(TrackingActivity.this).show();
                            internetStatus = false;
                        }
                    }
                    else {
                        internetStatus = true;
                    }

                    Long tsLong = System.currentTimeMillis()/1000;
                    tsLong = tsLong * 1000;

                    Long diffTime = tsLong - time;
                    // aktif selama 30 detik
                    if(diffTime < 30000){
                        isTargetOnline = true;
                    }
                    else{
                        isTargetOnline = false;
                    }

                    if(isTargetOnline){
                        targetStatus.setText("Target is online");
                        isOfflineNotified = false;
                    }
                    else if (!isTargetOnline && !isOfflineNotified){
                        targetStatus.setText("Target is offline");
                        isOfflineNotified = true;
                        notifyOffline();
                    }

                    //Toast.makeText(TrackingActivity.this, time.toString(), Toast.LENGTH_SHORT).show();

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
    protected void onStart() {
        super.onStart();

        // Menggambar history marker yang tersimpan di firebase
        if(!isMarkerDrawed){
            plotMarkerHistory();
        }
        if(!isGeofenceDrawed){
            plotGeofence();
        }

        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                HashMap<String, Object> location =
                        (HashMap<String, Object>) dataSnapshot.child("targetLocation").getValue();

                for(HashMap.Entry<String, Object> entry: location.entrySet()) {
                    if(entry.getKey().equals("longitude")) {
                        longitude = (Double) entry.getValue();
                    }
                    if(entry.getKey().equals("latitude")){
                        latitude = (Double) entry.getValue();
                    }
                    if(entry.getKey().equals("time")){
                        time = (Long) entry.getValue();
                    }
                }

                //Toast.makeText(TrackingActivity.this, location.toString(), Toast.LENGTH_LONG).show();

                // mengubah lokasi current marker
                markerPosition = new LatLng(latitude, longitude);
                currentMarker.setPosition(markerPosition);
                mMap.animateCamera( CameraUpdateFactory.newLatLngZoom(markerPosition, 17.0f));

                // menambahkan history marker
                icon = BitmapDescriptorFactory.fromResource(R.drawable.greendot);
                markerOptions = new MarkerOptions().position(markerPosition).title("Marker History").icon(icon);
                mMap.addMarker(markerOptions);

                // menambahkan polyline
                options.add(markerPosition);
                mMap.addPolyline(options);


                if(isGeofenceDrawed){
                    // Ini untuk mengambil data notifications dari firebase
                    HashMap<String, Object> notifKu =
                            (HashMap<String, Object>) dataSnapshot.child("notifications").getValue();

                    for(HashMap.Entry<String, Object> entry: notifKu.entrySet()) {
                        if(entry.getKey().equals("manualCheckIn")) {
                            manualCheckIn = (boolean) entry.getValue();
                        }
                        if(entry.getKey().equals("statusHasArrived")){
                            statusHasArrived = (boolean) entry.getValue();
                        }
                        if(entry.getKey().equals("statusInGeofence")){
                            statusInGeofence = (boolean) entry.getValue();
                        }
                        if(entry.getKey().equals("statusLinkExpired")){
                            statusLinkExpired = (boolean) entry.getValue();
                        }
                        if(entry.getKey().equals("statusSOS")){
                            statusSOS = (boolean) entry.getValue();
                        }
                    }


                    // trigger notifikasi

                    if (statusHasArrived == true && isArrivedNotified == false) {//if the user has been notified ONCE, then dont notify again!
                        notifyTargetReachDestination();
                        isArrivedNotified = true;
                    }

                    if (statusInGeofence == false && isGeofenceNotified == false) {//if the user has been notified ONCE, then dont notify again!
                        notifyTargetCrossedGeofence();
                        isGeofenceNotified = true;
                    }

                    if (statusSOS == true && isSOSNotified == false){
                        notifySOS();
                        isSOSNotified = true;
                    }


                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    // untuk menggambar geofence
    public void plotGeofence(){
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                //Toast.makeText(getApplicationContext(), "dataSnapshot "+ dataSnapshot , Toast.LENGTH_SHORT).show();

                if (dataSnapshot.hasChild("geofenceNum") && !isGeofenceDrawed){
                    ArrayList<Object> geofence = (ArrayList<Object>) dataSnapshot.child("geofence").getValue();
                    int geofenceSize = geofence.size()-1;
                    int geofenceNum = (Integer) Integer.parseInt((String) dataSnapshot.child("geofenceNum").getValue().toString());

                    //Toast.makeText(getApplicationContext(), "geofenceSize "+ geofenceSize + " geofenceNum " + geofenceNum, Toast.LENGTH_SHORT).show();

                    if(geofenceNum == geofenceSize) {
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
                        //markers.add(destinationMarker);

                        CircleOptions circleOptions = new CircleOptions()
                                .center(destinationMarker.getPosition())
                                .strokeColor(Color.argb(50, 70, 70, 70))
                                .fillColor(Color.argb(100, 150, 150, 150))
                                .radius(destRadius);
                        destinationFence = mMap.addCircle(circleOptions);


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

                            Marker marker = mMap.addMarker(new MarkerOptions().icon(BitmapDescriptorFactory.fromResource(R.drawable.dot15px)).position(new LatLng(markerLatitude, markerLongitude)));
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


                        polygon = mMap.addPolygon(polygonOptions);
                    }
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    // untuk menggambar marker history pertama kali
    public void plotMarkerHistory (){
        isMarkerDrawed = true;
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                try {
                    // Ambil nilai jumlah marker yang ada pada history
                    Long numHistory = (Long) dataSnapshot.child("numHistory").getValue();

                    for (int i = numMarker; i < numHistory.intValue(); i++) {

                        // Mengambil nilai objek marker pada history ke-i
                        HashMap<String, Object> location =
                                (HashMap<String, Object>) dataSnapshot.child("locationHistory").child(String.valueOf(i)).getValue();
                        double longitude = 0;
                        double latitude = 0;
                        LatLng point = new LatLng(0, 0);
                        ArrayList<LatLng> pointList = new ArrayList<LatLng>();

                        System.out.println(location);

                        for (HashMap.Entry<String, Object> entry : location.entrySet()) {
                            //Toast.makeText(TrackingActivity.this, entry.getKey().toString(), Toast.LENGTH_LONG).show();
                            if (entry.getKey().equals("latitude")) {
                                latitude = (Double) entry.getValue();
                            }
                            if (entry.getKey().equals("longitude")) {
                                longitude = (Double) entry.getValue();
                            }

                            point = new LatLng(latitude, longitude);
                        }
                        if (isFirstMarker) {
                            // start marker
                            BitmapDescriptor icon = BitmapDescriptorFactory.fromResource(R.drawable.start);
                            mMap.addMarker(new MarkerOptions().position(point).title("First Marker").icon(icon));
                            isFirstMarker = false;

                        } else {
                            // history marker
                            BitmapDescriptor icon = BitmapDescriptorFactory.fromResource(R.drawable.greendot);
                            mMap.addMarker(new MarkerOptions().position(point).title("History Marker").icon(icon));
                        }
                        options.add(point);
                        numMarker++;
                    }
                    mMap.addPolyline(options);
                }
                catch (Error e){
                    Toast.makeText(TrackingActivity.this, "No history marker", Toast.LENGTH_LONG).show();
                }

            }
            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        icon = BitmapDescriptorFactory.fromResource(R.drawable.smile);

        markerOptions = new MarkerOptions().position(markerPosition).title("Target is here").icon(icon);
        currentMarker = mMap.addMarker(markerOptions);
        currentMarker.setZIndex(1.0f);
        mMap.animateCamera( CameraUpdateFactory.newLatLngZoom(markerPosition, 17.0f) );
    }


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

    public AlertDialog.Builder internetDialog(Context c) {

        AlertDialog.Builder builder = new AlertDialog.Builder(c);
        builder.setTitle("No Internet Connection");
        builder.setMessage("You need to have Mobile Data or wifi to access this. Press Try Again or Exit");
        builder.setCancelable(false);

        builder.setPositiveButton("Try Again", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {

                dialog.cancel();
                if (!isConnected(TrackingActivity.this))

                    internetDialog(TrackingActivity.this).show();
                else {

                }
            }
        });

        builder.setNegativeButton("Exit", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {

                finish();
            }
        });

        return builder;
    }

    @Override
    public void onBackPressed() {
        backDialog(TrackingActivity.this).show();
    }

    public AlertDialog.Builder backDialog(Context c) {

        AlertDialog.Builder builder = new AlertDialog.Builder(c);
        builder.setTitle("Quit Tracking");
        builder.setMessage("Do you really want to quit tracking?");
        builder.setCancelable(false);

        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent i = new Intent(getApplicationContext(), MainMenu.class);
                startActivity(i);
                finish();
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


    // Notifikasi

    private void notifyOffline(){
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
                .setContentText("Target is offline")
                .setContentInfo("Info");

        notificationManager.notify(/*notifyTargetReachDestination id*/1, notificationBuilder.build());
    }

    public void notifySOS(){
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
                .setContentText("Target is in danger!")
                .setContentInfo("Info");

        notificationManager.notify(/*notifyTargetReachDestination id*/1, notificationBuilder.build());
    }

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

    private void notifyTargetCrossedGeofence() {
        polygon.setFillColor(Color.RED);

        Toast.makeText(getApplicationContext(), "Crossing Border", Toast.LENGTH_SHORT).show();

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

}
