package com.jady.jadytrack.entity;

public class Notifications {
    public Boolean statusHasArrived;
    public Boolean statusInGeofence;
    public Boolean statusLinkExpired;
    public Boolean statusSOS;
    public Boolean manualCheckIn;


    public Notifications(Boolean statusHasArrived, Boolean statusInGeofence, Boolean statusLinkExpired, Boolean statusSOS, Boolean manualCheckIn) {
        this.statusHasArrived = statusHasArrived;
        this.statusInGeofence = statusInGeofence;
        this.statusLinkExpired = statusLinkExpired;
        this.statusSOS = statusSOS;
        this.manualCheckIn = manualCheckIn;
    }
}
