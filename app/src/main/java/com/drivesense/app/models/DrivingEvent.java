package com.drivesense.app.models;

/**
 * Represents a detected driving event during a trip.
 * Each event has a type, severity, timestamp, GPS coordinates, and sensor readings.
 */
public class DrivingEvent {

    public enum EventType {
        HARD_BRAKING,
        RAPID_ACCELERATION,
        SHARP_TURN,
        SUDDEN_STOP,
        EXCESSIVE_VIBRATION,
        OVER_SPEEDING,
        FATIGUE_WARNING_IGNORED
    }

    private EventType type;
    private long timestamp;         // System.currentTimeMillis()
    private double latitude;
    private double longitude;
    private float speed;            // km/h at time of event
    private float severity;         // Severity multiplier (1.0 = mild, up to 3.0 = severe)
    private float sensorMagnitude;  // Raw sensor value that triggered detection
    private String description;

    public DrivingEvent() {}

    public DrivingEvent(EventType type, long timestamp, double latitude, double longitude,
                        float speed, float severity, float sensorMagnitude) {
        this.type = type;
        this.timestamp = timestamp;
        this.latitude = latitude;
        this.longitude = longitude;
        this.speed = speed;
        this.severity = severity;
        this.sensorMagnitude = sensorMagnitude;
        this.description = buildDescription();
    }

    private String buildDescription() {
        switch (type) {
            case HARD_BRAKING:
                return "Hard braking detected at " + String.format("%.1f", speed) + " km/h";
            case RAPID_ACCELERATION:
                return "Rapid acceleration detected at " + String.format("%.1f", speed) + " km/h";
            case SHARP_TURN:
                return "Sharp turn detected at " + String.format("%.1f", speed) + " km/h";
            case SUDDEN_STOP:
                return "Sudden stop or impact detected";
            case EXCESSIVE_VIBRATION:
                return "Excessive road vibration detected";
            case OVER_SPEEDING:
                return "Speed limit exceeded at " + String.format("%.1f", speed) + " km/h";
            case FATIGUE_WARNING_IGNORED:
                return "Fatigue warning dismissed";
            default:
                return "Driving event detected";
        }
    }

    // Getters and setters
    public EventType getType() { return type; }
    public void setType(EventType type) { this.type = type; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public float getSpeed() { return speed; }
    public void setSpeed(float speed) { this.speed = speed; }

    public float getSeverity() { return severity; }
    public void setSeverity(float severity) { this.severity = severity; }

    public float getSensorMagnitude() { return sensorMagnitude; }
    public void setSensorMagnitude(float sensorMagnitude) { this.sensorMagnitude = sensorMagnitude; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
