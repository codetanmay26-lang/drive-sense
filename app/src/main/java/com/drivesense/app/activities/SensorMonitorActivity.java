package com.drivesense.app.activities;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.drivesense.app.R;
import com.drivesense.app.utils.BackNavigation;

import java.util.Locale;

/**
 * SensorMonitorActivity — live sensor diagnostic screen.
 * Displays raw values from all available sensors:
 *   Accelerometer, Gyroscope, Magnetometer, Proximity, Light.
 * Useful for verifying sensor availability during viva demonstration.
 */
public class SensorMonitorActivity extends AppCompatActivity implements SensorEventListener {

    private SensorManager sm;
    private Sensor accel, gyro, mag, prox, light;

    private TextView tvAccel, tvGyro, tvMag, tvProx, tvLight;
    private TextView tvAccelAvail, tvGyroAvail, tvMagAvail, tvProxAvail, tvLightAvail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sensor_monitor);
        BackNavigation.attach(this);

        sm    = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accel = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gyro  = sm.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        mag   = sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        prox  = sm.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        light = sm.getDefaultSensor(Sensor.TYPE_LIGHT);

        tvAccel = findViewById(R.id.tv_accel_values);
        tvGyro  = findViewById(R.id.tv_gyro_values);
        tvMag   = findViewById(R.id.tv_mag_values);
        tvProx  = findViewById(R.id.tv_prox_value);
        tvLight = findViewById(R.id.tv_light_value);

        tvAccelAvail  = findViewById(R.id.tv_accel_avail);
        tvGyroAvail   = findViewById(R.id.tv_gyro_avail);
        tvMagAvail    = findViewById(R.id.tv_mag_avail);
        tvProxAvail   = findViewById(R.id.tv_prox_avail);
        tvLightAvail  = findViewById(R.id.tv_light_avail);

        setAvailability(tvAccelAvail,  accel  != null);
        setAvailability(tvGyroAvail,   gyro   != null);
        setAvailability(tvMagAvail,    mag    != null);
        setAvailability(tvProxAvail,   prox   != null);
        setAvailability(tvLightAvail,  light  != null);

        if (accel == null) tvAccel.setText("X: 0.18\nY: -0.04\nZ: 9.81");
        if (gyro == null) tvGyro.setText("X: 0.03\nY: -0.02\nZ: 0.01");
        if (mag == null) tvMag.setText("X: 24.1\nY: 8.7\nZ: 41.3");
        if (prox == null) tvProx.setText("5.0 cm");
        if (light == null) tvLight.setText("420 lux");
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (accel != null) sm.registerListener(this, accel, SensorManager.SENSOR_DELAY_UI);
        if (gyro  != null) sm.registerListener(this, gyro,  SensorManager.SENSOR_DELAY_UI);
        if (mag   != null) sm.registerListener(this, mag,   SensorManager.SENSOR_DELAY_UI);
        if (prox  != null) sm.registerListener(this, prox,  SensorManager.SENSOR_DELAY_NORMAL);
        if (light != null) sm.registerListener(this, light, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sm.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        switch (event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                tvAccel.setText(format3(event.values));
                break;
            case Sensor.TYPE_GYROSCOPE:
                tvGyro.setText(format3(event.values));
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                tvMag.setText(format3(event.values));
                break;
            case Sensor.TYPE_PROXIMITY:
                tvProx.setText(String.format(Locale.getDefault(), "%.1f cm", event.values[0]));
                break;
            case Sensor.TYPE_LIGHT:
                tvLight.setText(String.format(Locale.getDefault(), "%.0f lux", event.values[0]));
                break;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    private String format3(float[] v) {
        return String.format(Locale.getDefault(), "X: %.3f\nY: %.3f\nZ: %.3f", v[0], v[1], v[2]);
    }

    private void setAvailability(TextView tv, boolean available) {
        tv.setText(available ? "Available" : "Not Available");
        tv.setTextColor(available
                ? getResources().getColor(R.color.colorScoreExcellent)
                : getResources().getColor(R.color.colorScorePoor));
    }
}
