package com.drivesense.app.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.drivesense.app.database.dao.EventDao;
import com.drivesense.app.database.dao.TripDao;
import com.drivesense.app.database.entities.EventEntity;
import com.drivesense.app.database.entities.TripEntity;

/**
 * DriveSense local Room database.
 * Stores trip records and driving events persistently across app restarts.
 */
@Database(
    entities = {TripEntity.class, EventEntity.class},
    version = 1,
    exportSchema = false
)
public abstract class DriveSenseDatabase extends RoomDatabase {

    private static DriveSenseDatabase instance;
    private static final String DB_NAME = "drivesense_db";

    public abstract TripDao tripDao();
    public abstract EventDao eventDao();

    /**
     * Singleton pattern — only one database instance per application.
     */
    public static synchronized DriveSenseDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(
                    context.getApplicationContext(),
                    DriveSenseDatabase.class,
                    DB_NAME
            )
            .fallbackToDestructiveMigration()
            .build();
        }
        return instance;
    }
}
