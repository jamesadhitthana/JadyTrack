package com.jady.jadytrack.entity;

public class Notification {

    public Boolean statusHasArrived;
    public Boolean statusInGeofence;
    public Boolean statusLinkExpired;
    public Boolean statusSOS;
    public Boolean manualCheckIn;

    public Notification(Boolean statusHasArrived, Boolean statusInGeofence, Boolean statusLinkExpired, Boolean statusSOS, Boolean manualCheckIn) {
        this.statusHasArrived = statusHasArrived;
        this.statusInGeofence = statusInGeofence;
        this.statusLinkExpired = statusLinkExpired;
        this.statusSOS = statusSOS;
        this.manualCheckIn = manualCheckIn;
    }
}
