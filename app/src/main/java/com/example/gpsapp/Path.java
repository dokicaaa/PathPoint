package com.example.gpsapp;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "path_table")
public class Path {
    @PrimaryKey(autoGenerate = true)
    public int pID;
    public long createdAt; // Use System.currentTimeMillis() to set this value

    public Path() {
        this.createdAt = System.currentTimeMillis();
    }
}
