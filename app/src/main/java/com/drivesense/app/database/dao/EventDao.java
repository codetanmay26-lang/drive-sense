package com.drivesense.app.database.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.drivesense.app.database.entities.EventEntity;

import java.util.List;

/**
 * Data Access Object for driving events.
 */
@Dao
public interface EventDao {

    @Insert
    long insertEvent(EventEntity event);

    @Query("SELECT * FROM events WHERE trip_id = :tripId ORDER BY timestamp ASC")
    List<EventEntity> getEventsForTrip(long tripId);

    @Query("SELECT COUNT(*) FROM events WHERE trip_id = :tripId AND event_type = :type")
    int countEventsOfType(long tripId, String type);

    @Query("DELETE FROM events WHERE trip_id = :tripId")
    void deleteEventsForTrip(long tripId);

    @Query("DELETE FROM events")
    void deleteAllEvents();
}
