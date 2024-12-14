package com.example.gpsapp;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "waypoint_table")

public class Waypoint {
    @PrimaryKey(autoGenerate = true)
    public int wID;

    public double latitude;
    public double longitude;
    public int pathId; // This links a waypoint to a path

    public Waypoint(double latitude, double longitude, int pathId) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.pathId = pathId;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public int getPathId() {
        return pathId;
    }

    public void setPathId(int pathId) {
        this.pathId = pathId;
    }
}