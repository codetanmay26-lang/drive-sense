package com.drivesense.app.database.entities;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Room Entity: stores one completed trip in the local SQLite database.
 */
@Entity(tableName = "trips")
public class TripEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    @ColumnInfo(name = "start_time")
    public long startTime;

    @ColumnInfo(name = "end_time")
    public long endTime;

    @ColumnInfo(name = "duration_ms")
    public long durationMs;

    @ColumnInfo(name = "distance_km")
    public double distanceKm;

    @ColumnInfo(name = "max_speed_kmh")
    public float maxSpeedKmh;

    @ColumnInfo(name = "avg_speed_kmh")
    public float avgSpeedKmh;

    @ColumnInfo(name = "final_score")
    public int finalScore;

    @ColumnInfo(name = "rating")
    public String rating;

    // Event counts
    @ColumnInfo(name = "hard_braking_count")
    public int hardBrakingCount;

    @ColumnInfo(name = "rapid_accel_count")
    public int rapidAccelCount;

    @ColumnInfo(name = "sharp_turn_count")
    public int sharpTurnCount;

    @ColumnInfo(name = "sudden_stop_count")
    public int suddenStopCount;

    @ColumnInfo(name = "vibration_count")
    public int vibrationCount;

    @ColumnInfo(name = "overspeed_count")
    public int overspeedCount;

    // Road condition percentages
    @ColumnInfo(name = "road_smooth_pct")
    public float roadSmoothPct;

    @ColumnInfo(name = "road_moderate_pct")
    public float roadModeratePct;

    @ColumnInfo(name = "road_rough_pct")
    public float roadRoughPct;

    // JSON-encoded route for map replay
    @ColumnInfo(name = "route_json")
    public String routeJson;

    // User-associated (local user ID or username)
    @ColumnInfo(name = "user_id")
    public String userId;
}
