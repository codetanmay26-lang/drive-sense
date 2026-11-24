package com.drivesense.app.models;

import java.util.List;

/**
 * Holds all data for an active or completed trip.
 * This is the in-memory representation; persisted via TripEntity + EventEntity in Room.
 */
public class Trip {
    private long startTime;
    private long endTime;
    private double distanceKm;
    private float maxSpeedKmh;
    private float avgSpeedKmh;
    private int finalScore;
    private String rating;
    private List<DrivingEvent> events;
    private String routeJson;   // JSON-encoded list of LatLng for route replay

    // Road condition percentages
    private float roadSmoothPercent;
    private float roadModeratePercent;
    private float roadRoughPercent;

    public Trip() {
        this.startTime = System.currentTimeMillis();
        this.distanceKm = 0;
        this.maxSpeedKmh = 0;
        this.finalScore = 100;
    }

    public long getDurationMs() {
        long end = (endTime > 0) ? endTime : System.currentTimeMillis();
        return end - startTime;
    }

    public String getFormattedDuration() {
        long ms = getDurationMs();
        long totalSeconds = ms / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    // Getters and setters
    public long getStartTime() { return startTime; }
    public void setStartTime(long startTime) { this.startTime = startTime; }

    public long getEndTime() { return endTime; }
    public void setEndTime(long endTime) { this.endTime = endTime; }

    public double getDistanceKm() { return distanceKm; }
    public void setDistanceKm(double distanceKm) { this.distanceKm = distanceKm; }

    public float getMaxSpeedKmh() { return maxSpeedKmh; }
    public void setMaxSpeedKmh(float maxSpeedKmh) { this.maxSpeedKmh = maxSpeedKmh; }

    public float getAvgSpeedKmh() { return avgSpeedKmh; }
    public void setAvgSpeedKmh(float avgSpeedKmh) { this.avgSpeedKmh = avgSpeedKmh; }

    public int getFinalScore() { return finalScore; }
    public void setFinalScore(int finalScore) { this.finalScore = finalScore; }

    public String getRating() { return rating; }
    public void setRating(String rating) { this.rating = rating; }

    public List<DrivingEvent> getEvents() { return events; }
    public void setEvents(List<DrivingEvent> events) { this.events = events; }

    public String getRouteJson() { return routeJson; }
    public void setRouteJson(String routeJson) { this.routeJson = routeJson; }

    public float getRoadSmoothPercent() { return roadSmoothPercent; }
    public void setRoadSmoothPercent(float roadSmoothPercent) { this.roadSmoothPercent = roadSmoothPercent; }

    public float getRoadModeratePercent() { return roadModeratePercent; }
    public void setRoadModeratePercent(float roadModeratePercent) { this.roadModeratePercent = roadModeratePercent; }

    public float getRoadRoughPercent() { return roadRoughPercent; }
    public void setRoadRoughPercent(float roadRoughPercent) { this.roadRoughPercent = roadRoughPercent; }
}
