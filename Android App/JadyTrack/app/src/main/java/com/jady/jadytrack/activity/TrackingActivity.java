package com.jady.jadytrack.activity;

import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;

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
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.jady.jadytrack.R;
import com.kaopiz.kprogresshud.KProgressHUD;
import com.tapadoo.alerter.Alerter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

public class TrackingActivity extends AppCompatActivity implements
        OnMapReadyCallback {

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

    private List<Marker> markers = new ArrayList<>();
    private List<Marker> drawer = new ArrayList<>();

    private Polygon polygon;

    // Marker
    private Bitmap targetProfilePhoto; //james 18 nov 2020
    private BitmapDescriptor icon;
    private LatLng markerPosition = new LatLng(0, 0);
    private Marker currentMarker;
    private MarkerOptions markerOptions;

    private int numMarker = 1; // gambar pointnya akan dimulai dari numMarker terakhir

    // Handler
    private boolean internetStatus = true;
    private boolean gpsStatus = true;
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

    // Process dialog
    private KProgressHUD loadingWindow;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tracking);

        // Set loading window
        loadingWindow = KProgressHUD.create(TrackingActivity.this)
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

        // Menerima ID
        Intent intent = getIntent();
        // Attribute
        String id = intent.getStringExtra(InputIdActivity.EXTRA_MESSAGE_ID);

        // Firebase
        databaseReference = FirebaseDatabase.getInstance().getReference("trackingSession/" + id);

        // Mengatur tampilan awal
        targetStatus = (TextView) findViewById(R.id.targetStatus);
        targetStatus.setText(getResources().getString(R.string.label_title_target_offline));

        // Internet Connection handler
        final Handler handler = new Handler();
        Runnable runnable = new Runnable() {

            @Override
            public void run() {
                try {
                    if (isConnected(TrackingActivity.this)) {
                        if (internetStatus) {
                            internetDialog(TrackingActivity.this).show();
                            internetStatus = false;
                        }
                    } else {
                        internetStatus = true;
                    }

                    if (isGpsEnabled(TrackingActivity.this)) {
                        if (gpsStatus && internetStatus) {
                            gpsDialog(TrackingActivity.this).show();
                            gpsStatus = false;
                        }
                    } else {
                        gpsStatus = true;
                    }

                    long tsLong = System.currentTimeMillis() / 1000;
                    tsLong = tsLong * 1000;

                    long diffTime = tsLong - time;
                    // aktif selama 30 detik
                    isTargetOnline = diffTime < 20000;

                    if (isTargetOnline) {
                        targetStatus.setText(getResources().getString(R.string.label_title_target_online));
                        isOfflineNotified = false;
                    } else if (!isTargetOnline && !isOfflineNotified) {
                        targetStatus.setText(getResources().getString(R.string.label_title_target_offline));
                        isOfflineNotified = true;
                        notifyInfo(getResources().getString(R.string.notification_target_offline));
                    }

                } catch (Exception e) {
                    // TODO: handle exception
                } finally {
                    // Also call the same runnable to call it at regular interval
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
        if (!isMarkerDrawed) {
            plotMarkerHistory();
            loadingWindow.dismiss();
        }
        if (!isGeofenceDrawed) {
            plotGeofence();
        }

        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                HashMap<String, Object> location =
                        (HashMap<String, Object>) dataSnapshot.child("targetLocation").getValue();

                for (HashMap.Entry<String, Object> entry : location.entrySet()) {
                    if (entry.getKey().equals("longitude")) {
                        longitude = (Double) entry.getValue();
                    }
                    if (entry.getKey().equals("latitude")) {
                        latitude = (Double) entry.getValue();
                    }
                    if (entry.getKey().equals("time")) {
                        time = (Long) entry.getValue();
                    }
                }

                // mengubah lokasi current marker
                markerPosition = new LatLng(latitude, longitude);
                currentMarker.setPosition(markerPosition);
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(markerPosition, 17.0f));

                // menambahkan history marker
                icon = BitmapDescriptorFactory.fromResource(R.drawable.greendot);
                markerOptions = new MarkerOptions().position(markerPosition).title(getResources().getString(R.string.marker_history)).icon(icon);
                mMap.addMarker(markerOptions);

                // menambahkan polyline
                options.add(markerPosition);
                mMap.addPolyline(options);

                if (isGeofenceDrawed) {
                    // Ini untuk mengambil data notifications dari firebase
                    HashMap<String, Object> notifKu =
                            (HashMap<String, Object>) dataSnapshot.child("notifications").getValue();

                    for (HashMap.Entry<String, Object> entry : notifKu.entrySet()) {
                        if (entry.getKey().equals("manualCheckIn")) {
                            manualCheckIn = (boolean) entry.getValue();
                        }
                        if (entry.getKey().equals("statusHasArrived")) {
                            statusHasArrived = (boolean) entry.getValue();
                        }
                        if (entry.getKey().equals("statusInGeofence")) {
                            statusInGeofence = (boolean) entry.getValue();
                        }
                        if (entry.getKey().equals("statusLinkExpired")) {
                            statusLinkExpired = (boolean) entry.getValue();
                        }
                        if (entry.getKey().equals("statusSOS")) {
                            statusSOS = (boolean) entry.getValue();
                        }
                    }


                    // Notification trigger
                    // If the user has been notified ONCE, then dont notify again!
                    if (statusHasArrived && !isArrivedNotified) {
                        notifyInfo(getResources().getString(R.string.notification_target_arrived));
                        isArrivedNotified = true;
                    }

                    // If the user has been notified ONCE, then dont notify again!
                    if (!statusInGeofence && !isGeofenceNotified) {
                        polygon.setFillColor(Color.RED);
                        Alerter.create(TrackingActivity.this).setTitle(getResources().getString(R.string.alert_title_crossed_geofence)).setText(getResources().getString(R.string.alert_msg_crossed_geofence)).setBackgroundColorRes(R.color.colorAccent).show();

                        notifyAlert(getResources().getString(R.string.notification_target_crossing_border));
                        isGeofenceNotified = true;
                    }

                    if (statusSOS && !isSOSNotified) {
                        notifyAlert(getResources().getString(R.string.notification_target_in_danger));
                        isSOSNotified = true;
                    }


                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    // untuk menggambar geofence
    public void plotGeofence() {
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                if (dataSnapshot.hasChild("geofenceNum") && !isGeofenceDrawed) {
                    ArrayList<Object> geofence = (ArrayList<Object>) dataSnapshot.child("geofence").getValue();
                    int geofenceSize = geofence.size() - 1;
                    int geofenceNum = (Integer) Integer.parseInt((String) dataSnapshot.child("geofenceNum").getValue().toString());

                    if (geofenceNum == geofenceSize) {
                        isGeofenceDrawed = true;

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
                                .title(getResources().getString(R.string.marker_destination))
                                .zIndex(100f);

                        destinationMarker = mMap.addMarker(markerOptions);

                        CircleOptions circleOptions = new CircleOptions()
                                .center(destinationMarker.getPosition())
                                .strokeColor(Color.argb(50, 70, 70, 70))
                                .fillColor(Color.argb(100, 150, 150, 150))
                                .radius(destRadius);
                        destinationFence = mMap.addCircle(circleOptions);


                        // Draw the Geofence
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

    // untuk menggambar marker history pertama kali
    public void plotMarkerHistory() {
        isMarkerDrawed = true;
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

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

                        System.out.println(location);

                        for (HashMap.Entry<String, Object> entry : location.entrySet()) {
                            if (entry.getKey().equals("latitude")) {
                                latitude = (Double) entry.getValue();
                            }
                            if (entry.getKey().equals("longitude")) {
                                longitude = (Double) entry.getValue();
                            }

                            point = new LatLng(latitude, longitude);
                        }
                        if (isFirstMarker) {
                            // Start marker
                            BitmapDescriptor icon = BitmapDescriptorFactory.fromResource(R.drawable.start);
                            mMap.addMarker(new MarkerOptions().position(point).title(getResources().getString(R.string.marker_starting_point)).icon(icon));
                            isFirstMarker = false;

                        } else {
                            // History marker
                            BitmapDescriptor icon = BitmapDescriptorFactory.fromResource(R.drawable.greendot);
                            mMap.addMarker(new MarkerOptions().position(point).title(getResources().getString(R.string.marker_history)).icon(icon));
                        }
                        options.add(point);
                        numMarker++;
                    }
                    mMap.addPolyline(options);


                    //*--James 18Nov2020--//
                    loadTargetPhoto(dataSnapshot.child("targetId").getValue().toString());


                } catch (Error e) {
                    Alerter.create(TrackingActivity.this).setTitle(getResources().getString(R.string.alert_title_history_marker)).setText(getResources().getString(R.string.alert_msg_history_marker)).setBackgroundColorRes(R.color.colorAccent).show();

                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        try {
            icon = BitmapDescriptorFactory.fromBitmap(targetProfilePhoto);
            markerOptions = new MarkerOptions().position(markerPosition).title(getResources().getString(R.string.marker_current_location)).icon(icon);
            currentMarker = mMap.addMarker(markerOptions);
        } catch (Exception e) {
            icon = BitmapDescriptorFactory.fromResource(R.drawable.smile);
            markerOptions = new MarkerOptions().position(markerPosition).title(getResources().getString(R.string.marker_current_location)).icon(icon);
            currentMarker = mMap.addMarker(markerOptions);
            Toast.makeText(getApplicationContext(), "Failed to load profile picture", Toast.LENGTH_SHORT).show();
        }
        currentMarker.setZIndex(1.0f);
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(markerPosition, 17.0f));
    }


    public boolean isConnected(Context context) {

        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netinfo = cm.getActiveNetworkInfo();

        if (netinfo != null && netinfo.isConnectedOrConnecting()) {
            NetworkInfo wifi = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            NetworkInfo mobile = cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);

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
                if (isConnected(TrackingActivity.this)) {
                    internetDialog(TrackingActivity.this).show();
                }
            }
        });

        builder.setNegativeButton(getResources().getString(R.string.button_exit), new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish();
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
        backDialog(TrackingActivity.this).show();
    }

    public AlertDialog.Builder backDialog(Context c) {

        AlertDialog.Builder builder = new AlertDialog.Builder(c);
        builder.setTitle(getResources().getString(R.string.alert_title_quit_tracking));
        builder.setMessage(getResources().getString(R.string.alert_msg_quit_tracking));
        builder.setCancelable(false);

        builder.setPositiveButton(getResources().getString(R.string.button_yes), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent i = new Intent(getApplicationContext(), MainMenuActivity.class);
                startActivity(i);
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
    }

    //--Profile Photo--//
    private void loadTargetPhoto(String targetUID) {

        //Firebase storage//
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageReference = storage.getReference();
        StorageReference profilePhotoRef = storageReference.child("public/profilePhotos/" + targetUID); //Automatically get the profile photo from the user id

        profilePhotoRef.getBytes(1024 * 1024)
                .addOnSuccessListener(new OnSuccessListener<byte[]>() {
                    @Override
                    public void onSuccess(byte[] bytes) {
//                        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                        targetProfilePhoto = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
//                        imageViewUserAccountManagement.setImageBitmap(bitmap);
                        Toast.makeText(getApplicationContext(), getResources().getString(R.string.layout_uam_image_loaded_success), Toast.LENGTH_LONG).show();
//                        loadingWindow.dismiss();

                        try {
                            icon = BitmapDescriptorFactory.fromBitmap(targetProfilePhoto);
                            markerOptions = new MarkerOptions().position(markerPosition).title(getResources().getString(R.string.marker_current_location)).icon(icon);
                            currentMarker = mMap.addMarker(markerOptions);
                        } catch (Exception e) {
                            Toast.makeText(getApplicationContext(), ("Failed to change currentMarker using loadTargetPhoto"), Toast.LENGTH_LONG).show();
//
                        }


                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(getApplicationContext(), getResources().getString(R.string.layout_uam_image_loaded_failure), Toast.LENGTH_LONG).show();
//                        loadingWindow.dismiss();
                    }
                });
    }
}