package com.jamesgalaxy.jadytrackui;

import com.google.android.gms.maps.model.Marker;

import java.util.ArrayList;
import java.util.List;

public class GeofencePoint {

    public double latitude;
    public double longitude;

    public GeofencePoint(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }
}
