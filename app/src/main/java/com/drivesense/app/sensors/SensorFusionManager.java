package com.drivesense.app.sensors;

/**
 * SensorFusionManager — complementary filter combining accelerometer + gyroscope + magnetometer.
 *
 * Reused and improved from the original SensorFusionActivity.java / MapsActivity.java.
 * The complementary filter coefficient (0.98) blends gyroscope integration (fast, drifts)
 * with accelerometer+magnetometer orientation (slow, stable).
 *
 * Result: pitch, roll, and yaw (azimuth) angles in radians.
 *
 * Usage: feed onAccelerometerChanged, onGyroscopeChanged, onMagnetometerChanged from
 * SensorEventListener, then read fusedPitch/fusedRoll/fusedYaw.
 */
public class SensorFusionManager {

    // Complementary filter coefficient: 0.98 favors gyroscope for short-term,
    // 0.02 from acc+mag keeps long-term stability.
    private static final float FILTER_COEFFICIENT = 0.98f;
    private static final float ONE_MINUS_COEFF = 1.0f - FILTER_COEFFICIENT;
    private static final float NS2S = 1.0f / 1_000_000_000.0f; // nanoseconds to seconds

    // Sensor data
    private float[] accel = new float[3];
    private float[] magnet = new float[3];
    private float[] gyro = new float[3];

    // Orientation from accelerometer + magnetometer
    private float[] accMagOrientation = new float[3];
    // Orientation integrated from gyroscope
    private float[] gyroOrientation = new float[3];
    // Rotation matrix from gyroscope
    private float[] gyroMatrix = new float[9];
    // Final fused orientation [azimuth, pitch, roll]
    private float[] fusedOrientation = new float[3];
    // Rotation matrix from acc+mag
    private float[] rotationMatrix = new float[9];

    private float lastGyroTimestamp = 0;
    private boolean gyroInitialized = false;
    private boolean hasAccMag = false;

    // Output values in radians
    public float fusedPitch = 0f;
    public float fusedRoll  = 0f;
    public float fusedYaw   = 0f;

    public SensorFusionManager() {
        // Identity matrix for gyroscope rotation matrix
        gyroMatrix[0] = 1f; gyroMatrix[4] = 1f; gyroMatrix[8] = 1f;
        gyroOrientation[0] = 0f;
        gyroOrientation[1] = 0f;
        gyroOrientation[2] = 0f;
    }

    public void onAccelerometerChanged(float[] values) {
        accel[0] = values[0];
        accel[1] = values[1];
        accel[2] = values[2];
        updateAccMagOrientation();
    }

    public void onMagnetometerChanged(float[] values) {
        magnet[0] = values[0];
        magnet[1] = values[1];
        magnet[2] = values[2];
        updateAccMagOrientation();
    }

    public void onGyroscopeChanged(float[] values, long timestamp) {
        gyro[0] = values[0];
        gyro[1] = values[1];
        gyro[2] = values[2];

        if (!gyroInitialized) {
            if (hasAccMag) {
                // Initialize gyro matrix from acc+mag orientation
                gyroMatrix = getRotationMatrixFromOrientation(accMagOrientation);
                System.arraycopy(accMagOrientation, 0, gyroOrientation, 0, 3);
                gyroInitialized = true;
            }
            lastGyroTimestamp = timestamp;
            return;
        }

        // Integrate angular velocity to get rotation delta
        float dt = (timestamp - lastGyroTimestamp) * NS2S;
        lastGyroTimestamp = timestamp;

        if (dt <= 0 || dt > 1.0f) return; // skip bad timestamps

        float[] deltaMatrix = getGyroscopeDeltaRotationMatrix(gyro, dt);
        gyroMatrix = matrixMultiplication(gyroMatrix, deltaMatrix);

        // Extract orientation from gyro matrix
        android.hardware.SensorManager.getOrientation(gyroMatrix, gyroOrientation);

        // Apply complementary filter to fuse with acc+mag
        applyComplementaryFilter();
    }

    /**
     * Update acc+mag orientation using Android's built-in method.
     */
    private void updateAccMagOrientation() {
        float[] R = new float[9];
        float[] I = new float[9];
        boolean success = android.hardware.SensorManager.getRotationMatrix(R, I, accel, magnet);
        if (success) {
            android.hardware.SensorManager.getOrientation(R, accMagOrientation);
            rotationMatrix = R;
            hasAccMag = true;
        }
    }

    /**
     * Complementary filter: blend gyro orientation (fast) with acc+mag (stable).
     * Handles angle wrapping around ±π.
     */
    private void applyComplementaryFilter() {
        for (int i = 0; i < 3; i++) {
            float gyroVal = gyroOrientation[i];
            float accVal  = accMagOrientation[i];

            // Handle wraparound: if the difference > π, adjust
            if (gyroVal < -0.5f * Math.PI && accVal > 0.0f) {
                fusedOrientation[i] = (float)(FILTER_COEFFICIENT * (gyroVal + 2.0 * Math.PI) + ONE_MINUS_COEFF * accVal);
                if (fusedOrientation[i] > Math.PI) fusedOrientation[i] -= 2.0 * Math.PI;
            } else if (accVal < -0.5f * Math.PI && gyroVal > 0.0f) {
                fusedOrientation[i] = (float)(FILTER_COEFFICIENT * gyroVal + ONE_MINUS_COEFF * (accVal + 2.0 * Math.PI));
                if (fusedOrientation[i] > Math.PI) fusedOrientation[i] -= 2.0 * Math.PI;
            } else {
                fusedOrientation[i] = FILTER_COEFFICIENT * gyroVal + ONE_MINUS_COEFF * accVal;
            }
        }

        // Update gyro matrix with fused result to prevent drift accumulation
        gyroMatrix = getRotationMatrixFromOrientation(fusedOrientation);
        System.arraycopy(fusedOrientation, 0, gyroOrientation, 0, 3);

        // azimuth=0, pitch=1, roll=2
        fusedYaw   = fusedOrientation[0];
        fusedPitch = fusedOrientation[1];
        fusedRoll  = fusedOrientation[2];
    }

    /**
     * Compute rotation matrix for a small gyroscope rotation step (Rodrigues' rotation).
     * @param omega Angular velocity vector [rad/s]
     * @param dt    Time step [s]
     */
    private float[] getGyroscopeDeltaRotationMatrix(float[] omega, float dt) {
        float[] deltaR = new float[9];
        // Magnitude of angular velocity
        float norm = (float) Math.sqrt(omega[0]*omega[0] + omega[1]*omega[1] + omega[2]*omega[2]);
        if (norm < 1e-9f) {
            // No significant rotation: identity matrix
            deltaR[0] = 1f; deltaR[4] = 1f; deltaR[8] = 1f;
            return deltaR;
        }
        float angle = norm * dt;
        float sinA = (float) Math.sin(angle);
        float cosA = (float) Math.cos(angle);
        float nx = omega[0] / norm;
        float ny = omega[1] / norm;
        float nz = omega[2] / norm;
        // Rodrigues rotation matrix
        deltaR[0] = cosA + nx*nx*(1-cosA);
        deltaR[1] = nx*ny*(1-cosA) - nz*sinA;
        deltaR[2] = nx*nz*(1-cosA) + ny*sinA;
        deltaR[3] = ny*nx*(1-cosA) + nz*sinA;
        deltaR[4] = cosA + ny*ny*(1-cosA);
        deltaR[5] = ny*nz*(1-cosA) - nx*sinA;
        deltaR[6] = nz*nx*(1-cosA) - ny*sinA;
        deltaR[7] = nz*ny*(1-cosA) + nx*sinA;
        deltaR[8] = cosA + nz*nz*(1-cosA);
        return deltaR;
    }

    /**
     * Build rotation matrix from an orientation vector [azimuth, pitch, roll].
     * (Reused from original SensorFusionActivity.)
     */
    private float[] getRotationMatrixFromOrientation(float[] o) {
        float[] xM = new float[9];
        float[] yM = new float[9];
        float[] zM = new float[9];
        float sinX = (float) Math.sin(o[1]);
        float cosX = (float) Math.cos(o[1]);
        float sinY = (float) Math.sin(o[2]);
        float cosY = (float) Math.cos(o[2]);
        float sinZ = (float) Math.sin(o[0]);
        float cosZ = (float) Math.cos(o[0]);
        // X (pitch)
        xM[0]=1; xM[4]=cosX; xM[5]=sinX; xM[7]=-sinX; xM[8]=cosX;
        // Y (roll)
        yM[0]=cosY; yM[2]=sinY; yM[4]=1; yM[6]=-sinY; yM[8]=cosY;
        // Z (azimuth)
        zM[0]=cosZ; zM[1]=sinZ; zM[3]=-sinZ; zM[4]=cosZ; zM[8]=1;
        float[] result = matrixMultiplication(xM, yM);
        return matrixMultiplication(zM, result);
    }

    /**
     * 3x3 matrix multiplication: A * B, stored row-major.
     */
    private float[] matrixMultiplication(float[] A, float[] B) {
        float[] result = new float[9];
        result[0] = A[0]*B[0] + A[1]*B[3] + A[2]*B[6];
        result[1] = A[0]*B[1] + A[1]*B[4] + A[2]*B[7];
        result[2] = A[0]*B[2] + A[1]*B[5] + A[2]*B[8];
        result[3] = A[3]*B[0] + A[4]*B[3] + A[5]*B[6];
        result[4] = A[3]*B[1] + A[4]*B[4] + A[5]*B[7];
        result[5] = A[3]*B[2] + A[4]*B[5] + A[5]*B[8];
        result[6] = A[6]*B[0] + A[7]*B[3] + A[8]*B[6];
        result[7] = A[6]*B[1] + A[7]*B[4] + A[8]*B[7];
        result[8] = A[6]*B[2] + A[7]*B[5] + A[8]*B[8];
        return result;
    }

    public void reset() {
        gyroInitialized = false;
        hasAccMag = false;
        lastGyroTimestamp = 0;
        fusedPitch = 0f; fusedRoll = 0f; fusedYaw = 0f;
        gyroMatrix = new float[]{1,0,0, 0,1,0, 0,0,1};
        gyroOrientation = new float[3];
        accMagOrientation = new float[3];
    }
}
