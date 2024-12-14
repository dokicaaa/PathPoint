package com.example.gpsapp;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

@Dao
public interface PathDao {
    @Insert
    long insertPath(Path path);

    @Insert
    void insertWaypoint(Waypoint waypoint);

    @Query("SELECT * FROM waypoint_table")
    List<Waypoint> getAllWaypoints();

    @Query("SELECT p.pID, p.createdAt, COUNT(w.wID) as waypointCount FROM path_table p LEFT JOIN waypoint_table w ON p.pID = w.pathId GROUP BY p.pID")
    LiveData<List<PathWithWaypoints>> getAllPathsWithWaypoints();

    @Query("SELECT * FROM waypoint_table WHERE pathId = :pathId")
    LiveData<List<Waypoint>> getWaypointsByPathId(int pathId);

    @Delete
    void deleteWaypoint(Waypoint waypoint);

    @Delete
    void deletePath(Path path);
}