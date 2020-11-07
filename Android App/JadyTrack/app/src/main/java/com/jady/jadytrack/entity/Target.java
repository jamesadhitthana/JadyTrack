package com.jady.jadytrack.entity;

import android.location.Location;

import java.util.HashMap;
import java.util.Map;

public class Target {
    private String targetId;
    private String targetName;
    private Location targetLocation;
    private int numHistory;
    private Map<String, Location> targetLocationHistory = new HashMap<>();

    public Target(String targetId, String targetName, Location targetLocation, int numHistory, Map<String, Location> targetLocationHistory) {
        this.targetId = targetId;
        this.targetName = targetName;
        this.targetLocation = targetLocation;
        this.numHistory = numHistory;
        this.targetLocationHistory = targetLocationHistory;
    }

    public String getTargetId() {
        return targetId;
    }

    public String getTargetName() {
        return targetName;
    }

    public Location getTargetLocation() {
        return targetLocation;
    }

    public int getNumHistory(){return numHistory;}

    public Map<String, Location> getLocationHistory(){return targetLocationHistory;}
}
