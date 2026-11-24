package com.drivesense.app.models;

public class ActiveDriverModel {
    public String driverName;
    public String vehicleNo;
    public String currentSpeed;
    public float speedKmh;
    public String speedLimit;
    public int safetyScore;
    public String distanceKm;
    public String duration;
    public String status;
    public String eventsText;
    public String roadCondition;
    public long tripId;

    public int hardBrakingCount;
    public int rapidAccelCount;
    public int sharpTurnCount;
    public int suddenStopCount;
    public int vibrationCount;
    public int overspeedCount;

    public ActiveDriverModel(String driverName, String vehicleNo, String currentSpeed, float speedKmh,
                             String speedLimit, int safetyScore, String distanceKm,
                             String duration, String status, String eventsText,
                             String roadCondition, long tripId,
                             int hardBrakingCount, int rapidAccelCount, int sharpTurnCount,
                             int suddenStopCount, int vibrationCount, int overspeedCount) {
        this.driverName = driverName;
        this.vehicleNo = vehicleNo;
        this.currentSpeed = currentSpeed;
        this.speedKmh = speedKmh;
        this.speedLimit = speedLimit;
        this.safetyScore = safetyScore;
        this.distanceKm = distanceKm;
        this.duration = duration;
        this.status = status;
        this.eventsText = eventsText;
        this.roadCondition = roadCondition;
        this.tripId = tripId;
        this.hardBrakingCount = hardBrakingCount;
        this.rapidAccelCount = rapidAccelCount;
        this.sharpTurnCount = sharpTurnCount;
        this.suddenStopCount = suddenStopCount;
        this.vibrationCount = vibrationCount;
        this.overspeedCount = overspeedCount;
    }
}
