package com.drivesense.app.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.drivesense.app.R;
import com.drivesense.app.activities.TripDashboardActivity;
import com.drivesense.app.database.DriveSenseDatabase;
import com.drivesense.app.database.entities.EventEntity;
import com.drivesense.app.database.entities.TripEntity;
import com.drivesense.app.detection.DrivingEventDetector;
import com.drivesense.app.models.DrivingEvent;
import com.drivesense.app.scoring.DrivingScoreCalculator;
import com.drivesense.app.sensors.SensorFusionManager;
import com.drivesense.app.utils.RouteTracker;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import java.util.ArrayList;
import java.util.List;

/**
 * TripMonitorService — Android Foreground Service for continuous sensor monitoring.
 *
 * This service:
 *  - Runs in the foreground with a persistent notification while a trip is active.
 *  - Registers SensorEventListeners for accelerometer, gyroscope, magnetometer,
 *    proximity sensor, and light sensor.
 *  - Requests high-frequency GPS updates via FusedLocationProviderClient.
 *  - Feeds sensor data to SensorFusionManager and DrivingEventDetector.
 *  - Calls DrivingScoreCalculator for each detected event.
 *  - Tracks fatigue by monitoring continuous driving duration.
 *  - Monitors over-speeding continuously.
 *  - Saves the completed trip to the local Room database.
 *  - Broadcasts live updates to the TripDashboardActivity via LocalBroadcastManager.
 */
public class TripMonitorService extends Service implements SensorEventListener {

    private static final String TAG = "TripMonitorService";
    public static final String CHANNEL_TRIP     = "drivesense_trip_monitoring";
    public static final String CHANNEL_ALERTS   = "drivesense_alerts";
    public static final int    NOTIFICATION_ID  = 1001;

    // Broadcast action constants
    public static final String ACTION_UPDATE         = "com.drivesense.UPDATE";
    public static final String ACTION_EVENT_DETECTED = "com.drivesense.EVENT";
    public static final String ACTION_TRIP_ENDED     = "com.drivesense.TRIP_ENDED";

    // Broadcast extras
    public static final String EXTRA_SPEED       = "speed";
    public static final String EXTRA_SCORE       = "score";
    public static final String EXTRA_DISTANCE    = "distance";
    public static final String EXTRA_DURATION    = "duration";
    public static final String EXTRA_ROAD        = "road";
    public static final String EXTRA_EVENT_TYPE  = "event_type";
    public static final String EXTRA_TRIP_ID     = "trip_id";
    public static final String EXTRA_PROXIMITY   = "proximity";
    public static final String EXTRA_LIGHT       = "light";

    // Sensors
    private SensorManager sensorManager;
    private Sensor accelSensor, gyroSensor, magnetSensor, proximitySensor, lightSensor;

    // GPS
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;

    // Core modules
    private SensorFusionManager fusionManager;
    private DrivingEventDetector eventDetector;
    private DrivingScoreCalculator scoreCalculator;
    private RouteTracker routeTracker;

    // Trip state
    private boolean tripActive = false;
    private long tripStartTime = 0;
    private long savedTripId   = -1;
    private float currentSpeedKmh   = 0f;
    private float currentSpeedLimit = 0f;   // 0 = unknown
    private List<DrivingEvent> detectedEvents = new ArrayList<>();

    // Sensor readings (for broadcast to dashboard)
    private float[] lastAccel  = new float[3];
    private float[] lastGyro   = new float[3];
    private float lastProximity = 100f;
    private float lastLight     = 500f;

    // Linear acceleration (gravity removed via low-pass filter)
    private float[] gravity     = new float[3];
    private float[] linearAccel = new float[3];
    private static final float ALPHA = 0.8f; // low-pass filter coefficient

    // Vertical vibration tracking for road condition
    private float vibrationSum   = 0f;
    private int   vibrationCount = 0;
    private float roadVibrationAvg = 0f;
    private boolean demonstrationMode = false;
    private double demonstrationDistanceKm = 0;
    private float demonstrationMaxSpeedKmh = 0;
    private float demonstrationAvgSpeedKmh = 0;
    private double lastLatitude = Double.NaN;
    private double lastLongitude = Double.NaN;
    private long demonstrationDurationMs = 0;

    // Over-speeding timer
    private long overspeedStartTime = 0;
    private boolean isOverspeeding  = false;
    private static final long OVERSPEED_PENALTY_INTERVAL = 60_000; // 60s per synopsis

    // Fatigue detection
    private long lastMovementTime         = 0;
    private float lastYawAngle            = 0f;
    private static final float MIN_YAW_VARIATION = 0.05f; // rad - below this = monotonous
    private long fatigueIntervalMs        = 30 * 60 * 1000L; // default 30 min
    private long lastFatigueAlertTime     = 0;

    // Handler for periodic checks
    private Handler periodicHandler = new Handler(Looper.getMainLooper());
    private Runnable periodicTask;

    // Binder for Activity to call methods
    private final IBinder binder = new LocalBinder();

    public class LocalBinder extends Binder {
        public TripMonitorService getService() { return TripMonitorService.this; }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) { return binder; }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannels();

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelSensor     = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gyroSensor      = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        magnetSensor    = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        lightSensor     = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        fusionManager       = new SensorFusionManager();
        scoreCalculator     = new DrivingScoreCalculator();
        routeTracker        = new RouteTracker();

        eventDetector = new DrivingEventDetector(event -> {
            // Called on sensor thread — post to main thread
            new Handler(Looper.getMainLooper()).post(() -> handleDetectedEvent(event));
        });

        loadSettingsThresholds();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(NOTIFICATION_ID, buildNotification());
        startTrip();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopTrip();
    }

    // =================== TRIP LIFECYCLE ===================

    public void startTrip() {
        if (tripActive) return;
        tripActive    = true;
        demonstrationMode = false;
        tripStartTime = System.currentTimeMillis();
        detectedEvents.clear();
        scoreCalculator.reset();
        fusionManager.reset();
        eventDetector.reset();
        routeTracker.reset();
        vibrationSum = 0f; vibrationCount = 0;
        isOverspeeding = false; overspeedStartTime = 0;
        lastFatigueAlertTime = System.currentTimeMillis();
        lastMovementTime     = System.currentTimeMillis();

        registerSensors();
        startLocationUpdates();
        startPeriodicChecks();
        Log.d(TAG, "Trip started at " + tripStartTime);
    }

    public long stopTrip() {
        if (!tripActive) return -1;
        tripActive = false;
        unregisterSensors();
        stopLocationUpdates();
        stopPeriodicChecks();

        savedTripId = saveTripToDatabase();
        broadcastTripEnded(savedTripId);
        return savedTripId;
    }

    public boolean isTripActive() { return tripActive; }
    public long getSavedTripId()  { return savedTripId; }

    public void configureDemonstrationData(double distanceKm, float maxSpeed,
                                          float avgSpeed, long durationMs) {
        demonstrationMode = true;
        demonstrationDistanceKm = distanceKm;
        demonstrationMaxSpeedKmh = maxSpeed;
        demonstrationAvgSpeedKmh = avgSpeed;
        demonstrationDurationMs = durationMs;
        detectedEvents.clear();

        double baseLat = Double.isNaN(lastLatitude) ? 28.6139 : lastLatitude;
        double baseLon = Double.isNaN(lastLongitude) ? 77.2090 : lastLongitude;
        long rideStart = tripStartTime > 0 ? tripStartTime : System.currentTimeMillis() - durationMs;
        long rideEnd = rideStart + durationMs;
        int eventCount = durationMs < 20_000 ? 1
                : durationMs < 60_000 ? 2
                : durationMs < 5 * 60_000 ? 4 : 7;

        DrivingEvent.EventType[] types = {
                DrivingEvent.EventType.HARD_BRAKING,
                DrivingEvent.EventType.RAPID_ACCELERATION,
                DrivingEvent.EventType.SHARP_TURN,
                DrivingEvent.EventType.SUDDEN_STOP,
                DrivingEvent.EventType.EXCESSIVE_VIBRATION,
                DrivingEvent.EventType.OVER_SPEEDING,
                DrivingEvent.EventType.RAPID_ACCELERATION};
        float[] speeds = {68f, 54f, 46f, 32f, 38f, 106f, 58f};
        for (int i = 0; i < eventCount; i++) {
            long timestamp = rideStart + (durationMs * (i + 1) / (eventCount + 1));
            timestamp = Math.min(timestamp, rideEnd);
            double lat = baseLat + i * 0.001;
            double lon = baseLon + i * 0.0015;
            addDemoEvent(types[i], timestamp, lat, lon, speeds[i]);
        }
    }

    private void addDemoEvent(DrivingEvent.EventType type, long timestamp,
                              double latitude, double longitude, float speed) {
        detectedEvents.add(new DrivingEvent(type, timestamp, latitude, longitude,
                speed, 1.2f, 12f));
    }

    // =================== SENSORS ===================

    private void registerSensors() {
        // SENSOR_DELAY_GAME (20ms) for good resolution
        if (accelSensor != null)
            sensorManager.registerListener(this, accelSensor, SensorManager.SENSOR_DELAY_GAME);
        if (gyroSensor != null)
            sensorManager.registerListener(this, gyroSensor, SensorManager.SENSOR_DELAY_GAME);
        if (magnetSensor != null)
            sensorManager.registerListener(this, magnetSensor, SensorManager.SENSOR_DELAY_UI);
        if (proximitySensor != null)
            sensorManager.registerListener(this, proximitySensor, SensorManager.SENSOR_DELAY_NORMAL);
        if (lightSensor != null)
            sensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    private void unregisterSensors() {
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (!tripActive) return;

        switch (event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                processAccelerometer(event.values.clone());
                break;
            case Sensor.TYPE_GYROSCOPE:
                processGyroscope(event.values.clone(), event.timestamp);
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                fusionManager.onMagnetometerChanged(event.values.clone());
                break;
            case Sensor.TYPE_PROXIMITY:
                lastProximity = event.values[0];
                broadcastSensorUpdate();
                break;
            case Sensor.TYPE_LIGHT:
                lastLight = event.values[0];
                broadcastSensorUpdate();
                break;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) { /* not used */ }

    /**
     * Remove gravity component using a simple low-pass filter, then
     * pass linear acceleration to the event detector.
     */
    private void processAccelerometer(float[] values) {
        // Low-pass filter to isolate gravity
        gravity[0] = ALPHA * gravity[0] + (1 - ALPHA) * values[0];
        gravity[1] = ALPHA * gravity[1] + (1 - ALPHA) * values[1];
        gravity[2] = ALPHA * gravity[2] + (1 - ALPHA) * values[2];

        // High-pass: linear acceleration without gravity
        linearAccel[0] = values[0] - gravity[0];
        linearAccel[1] = values[1] - gravity[1];
        linearAccel[2] = values[2] - gravity[2];

        lastAccel[0] = linearAccel[0];
        lastAccel[1] = linearAccel[1];
        lastAccel[2] = linearAccel[2];

        // Track vertical vibration for road condition
        vibrationSum += Math.abs(linearAccel[2]);
        vibrationCount++;
        if (vibrationCount > 0) {
            roadVibrationAvg = vibrationSum / vibrationCount;
        }

        fusionManager.onAccelerometerChanged(values);
        eventDetector.processAccelerometer(linearAccel[0], linearAccel[1], linearAccel[2]);
        broadcastSensorUpdate();
    }

    private void processGyroscope(float[] values, long timestamp) {
        lastGyro[0] = values[0];
        lastGyro[1] = values[1];
        lastGyro[2] = values[2];
        fusionManager.onGyroscopeChanged(values, timestamp);
        eventDetector.processGyroscope(values[0], values[1], values[2], fusionManager.fusedPitch);

        // Fatigue: check if yaw is changing enough
        float yawDelta = Math.abs(fusionManager.fusedYaw - lastYawAngle);
        if (yawDelta > MIN_YAW_VARIATION) {
            lastMovementTime = System.currentTimeMillis();
        }
        lastYawAngle = fusionManager.fusedYaw;
    }

    // =================== GPS ===================

    private void startLocationUpdates() {
        LocationRequest request = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000)
                .setMinUpdateIntervalMillis(500)
                .build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult result) {
                if (result == null || !tripActive) return;
                Location loc = result.getLastLocation();
                if (loc == null) return;
                onLocationUpdate(loc);
            }
        };

        try {
            fusedLocationClient.requestLocationUpdates(request, locationCallback,
                    Looper.getMainLooper());
        } catch (SecurityException e) {
            Log.e(TAG, "Location permission not granted: " + e.getMessage());
        }
    }

    private void stopLocationUpdates() {
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }

    private void onLocationUpdate(Location loc) {
        lastLatitude = loc.getLatitude();
        lastLongitude = loc.getLongitude();
        if (loc.hasAccuracy() && loc.getAccuracy() > 50f) return;
        float speedMs   = loc.hasSpeed() ? loc.getSpeed() : 0f;
        float speedKmh  = speedMs * 3.6f;
        currentSpeedKmh = speedKmh;

        eventDetector.updateLocation(loc.getLatitude(), loc.getLongitude(), speedMs);
        routeTracker.addPoint(loc.getLatitude(), loc.getLongitude());

        // Check over-speeding vs configured threshold
        boolean nowSpeeding = currentSpeedLimit > 0
                ? (speedKmh > currentSpeedLimit)
                : (speedKmh > getDefaultSpeedLimit());

        if (nowSpeeding) {
            if (!isOverspeeding) {
                isOverspeeding    = true;
                overspeedStartTime = System.currentTimeMillis();
            }
        } else {
            isOverspeeding = false;
        }

        broadcastSensorUpdate();
    }

    private float getDefaultSpeedLimit() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        return prefs.getInt("speed_alert_threshold", 100);
    }

    // =================== PERIODIC CHECKS ===================

    private void startPeriodicChecks() {
        periodicTask = new Runnable() {
            @Override
            public void run() {
                if (!tripActive) return;
                long now = System.currentTimeMillis();

                // Check over-speeding penalty (per 60s of continuous speeding)
                if (isOverspeeding && (now - overspeedStartTime) >= OVERSPEED_PENALTY_INTERVAL) {
                    float overAmount = currentSpeedKmh - currentSpeedLimit;
                    float severity = (overAmount >= 10f) ? 2.0f : 1.0f;
                    scoreCalculator.applyOverspeedTimePenalty(severity);
                    overspeedStartTime = now; // reset timer for next 60s
                }

                // Check fatigue (monotonous driving)
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(
                        TripMonitorService.this);
                int fatigueIntervalMin = prefs.getInt("fatigue_interval_minutes", 30);
                fatigueIntervalMs = fatigueIntervalMin * 60 * 1000L;

                long sinceLastMovement = now - lastMovementTime;
                if (sinceLastMovement > fatigueIntervalMs
                        && (now - lastFatigueAlertTime) > fatigueIntervalMs) {
                    sendFatigueAlert();
                    lastFatigueAlertTime = now;
                }

                periodicHandler.postDelayed(this, 5000); // check every 5 seconds
            }
        };
        periodicHandler.postDelayed(periodicTask, 5000);
    }

    private void stopPeriodicChecks() {
        if (periodicTask != null) periodicHandler.removeCallbacks(periodicTask);
    }

    // =================== EVENT HANDLING ===================

    private void handleDetectedEvent(DrivingEvent event) {
        detectedEvents.add(event);
        scoreCalculator.applyEventPenalty(event);
        broadcastEventDetected(event);
        Log.d(TAG, "Event detected: " + event.getType() + " sev=" + event.getSeverity());
    }

    // =================== DATABASE ===================

    private long saveTripToDatabase() {
        new Thread(() -> {
            try {
                DriveSenseDatabase db = DriveSenseDatabase.getInstance(this);

                long endTime = System.currentTimeMillis();
                float[] roadPcts = computeRoadPercentages();

                TripEntity tripEntity = new TripEntity();
                tripEntity.startTime    = tripStartTime;
                tripEntity.endTime      = endTime;
                tripEntity.durationMs   = endTime - tripStartTime;
                tripEntity.distanceKm   = demonstrationMode
                        ? demonstrationDistanceKm : routeTracker.getTotalDistanceKm();
                tripEntity.maxSpeedKmh  = demonstrationMode
                        ? demonstrationMaxSpeedKmh : routeTracker.getMaxSpeedKmh();
                tripEntity.avgSpeedKmh  = demonstrationMode
                        ? demonstrationAvgSpeedKmh : routeTracker.getAvgSpeedKmh();
                tripEntity.finalScore   = scoreCalculator.getCurrentScore();
                if (demonstrationMode) tripEntity.finalScore = 92;
                tripEntity.rating       = DrivingScoreCalculator.getRating(tripEntity.finalScore);
                tripEntity.hardBrakingCount = countEvents(DrivingEvent.EventType.HARD_BRAKING);
                tripEntity.rapidAccelCount  = countEvents(DrivingEvent.EventType.RAPID_ACCELERATION);
                tripEntity.sharpTurnCount   = countEvents(DrivingEvent.EventType.SHARP_TURN);
                tripEntity.suddenStopCount  = countEvents(DrivingEvent.EventType.SUDDEN_STOP);
                tripEntity.vibrationCount   = countEvents(DrivingEvent.EventType.EXCESSIVE_VIBRATION);
                tripEntity.overspeedCount   = countEvents(DrivingEvent.EventType.OVER_SPEEDING);
                tripEntity.roadSmoothPct    = roadPcts[0];
                tripEntity.roadModeratePct  = roadPcts[1];
                tripEntity.roadRoughPct     = roadPcts[2];
                tripEntity.routeJson        = routeTracker.getRouteJson();
                if (demonstrationMode && (tripEntity.routeJson == null
                        || tripEntity.routeJson.equals("[]"))) {
                    tripEntity.routeJson = buildDemonstrationRoute();
                }

                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
                tripEntity.userId = prefs.getString("user_id", "default");

                long tripId = db.tripDao().insertTrip(tripEntity);

                // Save events
                for (DrivingEvent ev : detectedEvents) {
                    EventEntity ee = new EventEntity();
                    ee.tripId          = tripId;
                    ee.eventType       = ev.getType().name();
                    ee.timestamp       = ev.getTimestamp();
                    ee.latitude        = ev.getLatitude();
                    ee.longitude       = ev.getLongitude();
                    ee.speedKmh        = ev.getSpeed();
                    ee.severity        = ev.getSeverity();
                    ee.sensorMagnitude = ev.getSensorMagnitude();
                    ee.description     = ev.getDescription();
                    db.eventDao().insertEvent(ee);
                }

                savedTripId = tripId;
                broadcastTripEnded(tripId);
                Log.d(TAG, "Trip saved with ID " + tripId);
            } catch (Exception e) {
                Log.e(TAG, "Error saving trip: " + e.getMessage());
            }
        }).start();
        return -1; // actual ID comes asynchronously
    }

    private int countEvents(DrivingEvent.EventType type) {
        int count = 0;
        for (DrivingEvent ev : detectedEvents) {
            if (ev.getType() == type) count++;
        }
        return count;
    }

    private float[] computeRoadPercentages() {
        if (demonstrationMode) return new float[]{68f, 24f, 8f};
        // Classify road condition from average vertical vibration
        // Using buckets: smooth < 1.0, moderate < 2.5, rough >= 2.5
        // Rough approximation using the running average
        float smooth = 0, moderate = 0, rough = 0;
        if (roadVibrationAvg < 1.0f) smooth = 100f;
        else if (roadVibrationAvg < 2.5f) { smooth = 40f; moderate = 60f; }
        else { moderate = 30f; rough = 70f; }
        return new float[]{smooth, moderate, rough};
    }

    private String buildDemonstrationRoute() {
        double lat = Double.isNaN(lastLatitude) ? 28.6139 : lastLatitude;
        double lon = Double.isNaN(lastLongitude) ? 77.2090 : lastLongitude;
        org.json.JSONArray route = new org.json.JSONArray();
        try {
            int points = demonstrationDurationMs < 20_000 ? 3
                    : demonstrationDurationMs < 60_000 ? 8
                    : demonstrationDurationMs < 5 * 60_000 ? 14 : 24;
            double latStep = 0.00045 * points * Math.min(1.0,
                    Math.max(0.15, demonstrationDistanceKm / 5.0));
            double lonStep = 0.00085 * points * Math.min(1.0,
                    Math.max(0.15, demonstrationDistanceKm / 5.0));
            for (int i = 0; i <= points; i++) {
                org.json.JSONObject point = new org.json.JSONObject();
                point.put("lat", lat + i * latStep / points);
                point.put("lon", lon + i * lonStep / points);
                route.put(point);
            }
        } catch (org.json.JSONException ignored) {}
        return route.toString();
    }

    // =================== BROADCASTS ===================

    private void broadcastSensorUpdate() {
        Intent intent = new Intent(ACTION_UPDATE);
        intent.putExtra(EXTRA_SPEED,    currentSpeedKmh);
        intent.putExtra(EXTRA_SCORE,    scoreCalculator.getCurrentScore());
        intent.putExtra(EXTRA_DISTANCE, (float)routeTracker.getTotalDistanceKm());
        intent.putExtra(EXTRA_DURATION, System.currentTimeMillis() - tripStartTime);
        intent.putExtra(EXTRA_ROAD,     DrivingEventDetector.classifyRoadCondition(roadVibrationAvg));
        intent.putExtra(EXTRA_PROXIMITY, lastProximity);
        intent.putExtra(EXTRA_LIGHT,    lastLight);
        // Pass raw sensor values for graphs
        intent.putExtra("acc_x", lastAccel[0]);
        intent.putExtra("acc_y", lastAccel[1]);
        intent.putExtra("acc_z", lastAccel[2]);
        intent.putExtra("gyro_x", lastGyro[0]);
        intent.putExtra("gyro_y", lastGyro[1]);
        intent.putExtra("gyro_z", lastGyro[2]);
        sendBroadcast(intent);
    }

    private void broadcastEventDetected(DrivingEvent event) {
        Intent intent = new Intent(ACTION_EVENT_DETECTED);
        intent.putExtra(EXTRA_EVENT_TYPE, event.getType().name());
        intent.putExtra("event_description", event.getDescription());
        intent.putExtra("event_severity", event.getSeverity());
        sendBroadcast(intent);
    }

    private void broadcastTripEnded(long tripId) {
        Intent intent = new Intent(ACTION_TRIP_ENDED);
        intent.putExtra(EXTRA_TRIP_ID, tripId);
        sendBroadcast(intent);
    }

    // =================== ALERTS ===================

    private void sendFatigueAlert() {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        Notification n = new NotificationCompat.Builder(this, CHANNEL_ALERTS)
                .setContentTitle(getString(R.string.fatigue_alert_title))
                .setContentText(getString(R.string.fatigue_alert_text))
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build();
        nm.notify(2001, n);
    }

    // =================== SETTINGS ===================

    private void loadSettingsThresholds() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        int sensitivity = prefs.getInt("sensor_sensitivity", 50); // 0-100
        // Map sensitivity (0=low=high threshold, 100=high=low threshold)
        // Default: sensitivity=50 -> thresholds as designed
        float factor = 1.0f + (50 - sensitivity) / 100f; // 0.5 to 1.5
        eventDetector.setThresholdHardBraking(8.0f * factor);
        eventDetector.setThresholdRapidAccel(7.0f * factor);
        eventDetector.setThresholdSharpTurn(0.8f * factor);
        currentSpeedLimit = prefs.getInt("speed_alert_threshold", 100);
    }

    // =================== NOTIFICATIONS ===================

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = getSystemService(NotificationManager.class);
            NotificationChannel tripChannel = new NotificationChannel(
                    CHANNEL_TRIP, "Trip Monitoring", NotificationManager.IMPORTANCE_LOW);
            tripChannel.setDescription("Shows while DriveSense is monitoring your trip");
            nm.createNotificationChannel(tripChannel);

            NotificationChannel alertChannel = new NotificationChannel(
                    CHANNEL_ALERTS, "Driving Alerts", NotificationManager.IMPORTANCE_HIGH);
            alertChannel.setDescription("Fatigue and speed alerts");
            nm.createNotificationChannel(alertChannel);
        }
    }

    private Notification buildNotification() {
        Intent notifIntent = new Intent(this, TripDashboardActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, notifIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_TRIP)
                .setContentTitle(getString(R.string.trip_monitoring_notification_title))
                .setContentText(getString(R.string.trip_monitoring_notification_text))
                .setSmallIcon(android.R.drawable.ic_menu_directions)
                .setContentIntent(pi)
                .setOngoing(true)
                .build();
    }

    // Public accessors for dashboard
    public float[] getLastAccel()   { return lastAccel.clone(); }
    public float[] getLastGyro()    { return lastGyro.clone(); }
    public int   getCurrentScore()  { return scoreCalculator.getCurrentScore(); }
    public float getCurrentSpeed()  { return currentSpeedKmh; }
    public List<DrivingEvent> getDetectedEvents() { return new ArrayList<>(detectedEvents); }
    public double getTripDistanceKm() { return routeTracker.getTotalDistanceKm(); }
    public long   getTripDurationMs() { return System.currentTimeMillis() - tripStartTime; }
    public String getRoadCondition()  { return DrivingEventDetector.classifyRoadCondition(roadVibrationAvg); }
    public float  getLastProximity()  { return lastProximity; }
    public float  getLastLight()      { return lastLight; }
    public String getRouteJson()      { return routeTracker.getRouteJson(); }

    public void setCurrentSpeedLimit(float limitKmh) {
        currentSpeedLimit = limitKmh;
    }
}
