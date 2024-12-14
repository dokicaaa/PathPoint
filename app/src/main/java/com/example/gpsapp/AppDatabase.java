package com.example.gpsapp;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {Waypoint.class, Path.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {
    public abstract PathDao pathDao();

    //Singleton instance
    private static volatile AppDatabase INSTANCE;

    //getInstance method
    public static AppDatabase getInstance(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "Paths")
                            .allowMainThreadQueries()
                            .fallbackToDestructiveMigration() // Handle migrations as needed
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}