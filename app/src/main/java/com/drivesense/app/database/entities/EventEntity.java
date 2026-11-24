package com.drivesense.app.database.entities;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Room Entity: stores one detected driving event, associated with a trip.
 */
@Entity(
    tableName = "events",
    foreignKeys = @ForeignKey(
        entity = TripEntity.class,
        parentColumns = "id",
        childColumns = "trip_id",
        onDelete = ForeignKey.CASCADE
    ),
    indices = {@Index("trip_id")}
)
public class EventEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    @ColumnInfo(name = "trip_id")
    public long tripId;

    @ColumnInfo(name = "event_type")
    public String eventType;        // enum name string

    @ColumnInfo(name = "timestamp")
    public long timestamp;

    @ColumnInfo(name = "latitude")
    public double latitude;

    @ColumnInfo(name = "longitude")
    public double longitude;

    @ColumnInfo(name = "speed_kmh")
    public float speedKmh;

    @ColumnInfo(name = "severity")
    public float severity;

    @ColumnInfo(name = "sensor_magnitude")
    public float sensorMagnitude;

    @ColumnInfo(name = "description")
    public String description;
}
