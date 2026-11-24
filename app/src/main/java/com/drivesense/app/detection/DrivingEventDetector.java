package com.drivesense.app.detection;

import com.drivesense.app.models.DrivingEvent;
import com.drivesense.app.models.DrivingEvent.EventType;

/**
 * DrivingEventDetector — detects driving events from fused sensor data.
 *
 * Implements threshold-based detection for all events defined in the DriveSense synopsis:
 *   1. Hard Braking        (accelerometer Y-axis, negative, + GPS deceleration)
 *   2. Rapid Acceleration  (accelerometer Y-axis, positive)
 *   3. Sharp Turn          (gyroscope Z-axis, fused pitch/yaw)
 *   4. Sudden Stop/Impact  (combined magnitude spikes)
 *   5. Excessive Vibration (accelerometer variance over a sliding window)
 *   6. Over-Speeding       (GPS speed vs threshold)
 *
 * Cooldown periods prevent repeated false detections from sensor noise.
 */
public class DrivingEventDetector {

    // --- THRESHOLDS (can be overridden via Settings) ---
    // Accelerometer in m/s² (after gravity removal)
    private float thresholdHardBraking       = 8.0f;  // m/s²
    private float thresholdRapidAccel        = 7.0f;  // m/s²
    private float thresholdSuddenStop        = 15.0f; // m/s² (very high spike)
    private float thresholdVibration         = 2.5f;  // RMS variance threshold
    // Gyroscope in rad/s
    private float thresholdSharpTurn         = 0.8f;  // rad/s on Z-axis
    // GPS speed in km/h
    private float thresholdOverspeed         = 100f;  // default; set per user setting
    private float overspeedMinorLimit        = 10f;   // km/h above limit = minor
    // Vibration window
    private static final int VIBRATION_WINDOW = 30;   // samples for variance

    // --- COOLDOWN in milliseconds ---
    private static final long COOLDOWN_BRAKING   = 3000;
    private static final long COOLDOWN_ACCEL     = 3000;
    private static final long COOLDOWN_TURN      = 2000;
    private static final long COOLDOWN_STOP      = 5000;
    private static final long COOLDOWN_VIBRATION = 30000; // 30-second period as per synopsis

    // Last detection timestamps
    private long lastBrakingTime    = 0;
    private long lastAccelTime      = 0;
    private long lastTurnTime       = 0;
    private long lastStopTime       = 0;
    private long lastVibrationTime  = 0;

    // Vibration window (circular buffer of acceleration magnitudes)
    private float[] vibrationBuffer = new float[VIBRATION_WINDOW];
    private int vibrationIndex = 0;

    // Callback interface for detected events
    public interface EventCallback {
        void onEventDetected(DrivingEvent event);
    }

    private EventCallback callback;
    private double currentLatitude  = 0.0;
    private double currentLongitude = 0.0;
    private float  currentSpeedKmh  = 0.0f;
    private float  previousSpeedKmh = 0.0f;

    public DrivingEventDetector(EventCallback callback) {
        this.callback = callback;
    }

    /**
     * Update location context (called whenever GPS updates).
     */
    public void updateLocation(double lat, double lon, float speedMs) {
        previousSpeedKmh = currentSpeedKmh;
        currentLatitude  = lat;
        currentLongitude = lon;
        currentSpeedKmh  = speedMs * 3.6f; // convert m/s to km/h

        // Check over-speed (handled separately by TripMonitorService)
    }

    /**
     * Process a new accelerometer reading (gravity-removed linear acceleration).
     * @param linearX  X-axis acceleration (lateral, side-to-side)
     * @param linearY  Y-axis acceleration (longitudinal, forward/backward)
     * @param linearZ  Z-axis acceleration (vertical, up/down)
     */
    public void processAccelerometer(float linearX, float linearY, float linearZ) {
        long now = System.currentTimeMillis();

        // Combined magnitude for sudden stop / impact detection
        float magnitude = (float) Math.sqrt(linearX*linearX + linearY*linearY + linearZ*linearZ);

        // 1. HARD BRAKING — sudden negative longitudinal acceleration
        //    Y-axis: negative = decelerating (phone mounted facing forward)
        if (linearY < -thresholdHardBraking && (now - lastBrakingTime) > COOLDOWN_BRAKING) {
            // Only meaningful if we were moving (> 5 km/h via GPS)
            if (currentSpeedKmh > 5f) {
                float severity = computeAccelSeverity(Math.abs(linearY), thresholdHardBraking, 15f);
                DrivingEvent event = new DrivingEvent(
                        EventType.HARD_BRAKING, now,
                        currentLatitude, currentLongitude,
                        currentSpeedKmh, severity, linearY);
                callback.onEventDetected(event);
                lastBrakingTime = now;
            }
        }

        // 2. RAPID ACCELERATION — sudden positive longitudinal acceleration
        if (linearY > thresholdRapidAccel && (now - lastAccelTime) > COOLDOWN_ACCEL) {
            if (currentSpeedKmh < 150f) { // sanity check
                float severity = computeAccelSeverity(linearY, thresholdRapidAccel, 14f);
                DrivingEvent event = new DrivingEvent(
                        EventType.RAPID_ACCELERATION, now,
                        currentLatitude, currentLongitude,
                        currentSpeedKmh, severity, linearY);
                callback.onEventDetected(event);
                lastAccelTime = now;
            }
        }

        // 3. SUDDEN STOP / IMPACT — very high combined magnitude
        if (magnitude > thresholdSuddenStop && (now - lastStopTime) > COOLDOWN_STOP) {
            // Severity: 1.0 for stop, up to 3.0 for severe impact
            float severity = computeAccelSeverity(magnitude, thresholdSuddenStop, 40f);
            severity = Math.min(severity, 3.0f);
            DrivingEvent event = new DrivingEvent(
                    EventType.SUDDEN_STOP, now,
                    currentLatitude, currentLongitude,
                    currentSpeedKmh, severity, magnitude);
            callback.onEventDetected(event);
            lastStopTime = now;
        }

        // 4. EXCESSIVE VIBRATION — sliding window variance on combined magnitude
        vibrationBuffer[vibrationIndex % VIBRATION_WINDOW] = magnitude;
        vibrationIndex++;
        if (vibrationIndex >= VIBRATION_WINDOW) {
            float variance = computeVariance(vibrationBuffer);
            if (variance > thresholdVibration && (now - lastVibrationTime) > COOLDOWN_VIBRATION) {
                float severity = (variance > thresholdVibration * 2) ? 1.5f : 1.0f;
                DrivingEvent event = new DrivingEvent(
                        EventType.EXCESSIVE_VIBRATION, now,
                        currentLatitude, currentLongitude,
                        currentSpeedKmh, severity, variance);
                callback.onEventDetected(event);
                lastVibrationTime = now;
            }
        }
    }

    /**
     * Process gyroscope reading for sharp turn detection.
     * @param gyroZ  Angular velocity around Z-axis (yaw rate, rad/s)
     * @param fusedPitch  Fused pitch angle (rad)
     */
    public void processGyroscope(float gyroX, float gyroY, float gyroZ, float fusedPitch) {
        long now = System.currentTimeMillis();

        // Sharp turn uses yaw rate (rotation around Z-axis of the phone)
        float absGyroZ = Math.abs(gyroZ);
        if (absGyroZ > thresholdSharpTurn && (now - lastTurnTime) > COOLDOWN_TURN) {
            // Severity: 1.0 (moderate) to 2.5 (aggressive)
            float severity = computeAccelSeverity(absGyroZ, thresholdSharpTurn, 2.5f);
            severity = Math.min(severity, 2.5f);
            DrivingEvent event = new DrivingEvent(
                    EventType.SHARP_TURN, now,
                    currentLatitude, currentLongitude,
                    currentSpeedKmh, severity, gyroZ);
            callback.onEventDetected(event);
            lastTurnTime = now;
        }
    }

    /**
     * Check for over-speeding.
     * @param currentSpeedKmh GPS-derived current speed
     * @param speedLimitKmh   Known speed limit (0 if unknown)
     * @return true if currently over-speeding
     */
    public boolean checkOverSpeeding(float currentSpeedKmh, float speedLimitKmh) {
        if (speedLimitKmh <= 0) return false;
        return currentSpeedKmh > speedLimitKmh;
    }

    /**
     * Compute severity multiplier (linear interpolation between min and max).
     * Result clamped to [1.0, maxSeverity].
     */
    private float computeAccelSeverity(float value, float minThreshold, float maxThreshold) {
        if (value <= minThreshold) return 1.0f;
        float ratio = (value - minThreshold) / (maxThreshold - minThreshold);
        return 1.0f + Math.min(ratio, 1.0f); // 1.0 to 2.0
    }

    /**
     * Compute variance of a float array.
     */
    private float computeVariance(float[] data) {
        float sum = 0;
        for (float v : data) sum += v;
        float mean = sum / data.length;
        float variance = 0;
        for (float v : data) variance += (v - mean) * (v - mean);
        return variance / data.length;
    }

    // --- Road condition classification ---

    /**
     * Classify road condition from vertical (Z-axis) vibration magnitude.
     * @param verticalMagnitude |linearZ| averaged over recent samples
     * @return "Smooth", "Moderate", or "Rough"
     */
    public static String classifyRoadCondition(float verticalMagnitude) {
        if (verticalMagnitude < 1.0f) return "Smooth";
        if (verticalMagnitude < 2.5f) return "Moderate";
        return "Rough";
    }

    // --- Setters for configurable thresholds ---
    public void setThresholdHardBraking(float v)   { thresholdHardBraking = v; }
    public void setThresholdRapidAccel(float v)    { thresholdRapidAccel = v; }
    public void setThresholdSharpTurn(float v)     { thresholdSharpTurn = v; }
    public void setThresholdSuddenStop(float v)    { thresholdSuddenStop = v; }
    public void setThresholdVibration(float v)     { thresholdVibration = v; }
    public void setThresholdOverspeed(float v)     { thresholdOverspeed = v; }

    public void reset() {
        lastBrakingTime = 0; lastAccelTime = 0;
        lastTurnTime = 0; lastStopTime = 0; lastVibrationTime = 0;
        vibrationIndex = 0;
        vibrationBuffer = new float[VIBRATION_WINDOW];
    }
}
