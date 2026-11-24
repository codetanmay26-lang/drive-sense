package com.drivesense.app.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.drivesense.app.database.entities.TripEntity;

import java.util.List;

/**
 * Data Access Object for trip records.
 */
@Dao
public interface TripDao {

    @Insert
    long insertTrip(TripEntity trip);

    @Query("SELECT * FROM trips ORDER BY start_time DESC")
    List<TripEntity> getAllTrips();

    @Query("SELECT * FROM trips WHERE id = :tripId LIMIT 1")
    TripEntity getTripById(long tripId);

    @Query("SELECT * FROM trips WHERE user_id = :userId ORDER BY start_time DESC")
    List<TripEntity> getTripsByUser(String userId);

    @Query("SELECT COUNT(*) FROM trips")
    int getTripCount();

    @Query("SELECT AVG(final_score) FROM trips WHERE user_id = :userId")
    float getAverageScore(String userId);

    @Query("SELECT * FROM trips ORDER BY start_time DESC LIMIT 1")
    TripEntity getLastTrip();

    @Query("DELETE FROM trips WHERE id = :tripId")
    void deleteTripById(long tripId);

    @Query("DELETE FROM trips")
    void deleteAllTrips();
}
