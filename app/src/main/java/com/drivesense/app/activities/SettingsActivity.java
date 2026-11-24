package com.drivesense.app.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.drivesense.app.R;
import com.drivesense.app.utils.BackNavigation;

/**
 * SettingsActivity — user-configurable settings.
 *
 * Settings that ACTUALLY affect app behavior (not decorative):
 *  1. Sensor sensitivity   → thresholds in DrivingEventDetector
 *  2. Speed alert threshold → over-speed detection in TripMonitorService
 *  3. Fatigue alert interval → fatigue warning period
 *  4. Auto-start trip (proximity sensor) → triggers trip start when phone in pocket/car mount
 *  5. Auto day/night theme (light sensor) → switches theme based on ambient light
 *  6. Sensor diagnostic tool → opens SensorMonitorActivity
 */
public class SettingsActivity extends AppCompatActivity {

    private SharedPreferences prefs;

    // UI
    private SeekBar sbSensitivity, sbSpeedThreshold, sbFatigueInterval;
    private TextView tvSensitivityValue, tvSpeedValue, tvFatigueValue;
    private Switch swAutoStart, swAutoTheme;
    private Button btnSensorDiag;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        BackNavigation.attach(this);

        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        bindViews();
        loadCurrentValues();
        setupListeners();
    }

    private void bindViews() {
        sbSensitivity    = findViewById(R.id.sb_sensitivity);
        sbSpeedThreshold = findViewById(R.id.sb_speed_threshold);
        sbFatigueInterval = findViewById(R.id.sb_fatigue_interval);
        tvSensitivityValue  = findViewById(R.id.tv_sensitivity_value);
        tvSpeedValue        = findViewById(R.id.tv_speed_value);
        tvFatigueValue      = findViewById(R.id.tv_fatigue_value);
        swAutoStart         = findViewById(R.id.sw_auto_start);
        swAutoTheme         = findViewById(R.id.sw_auto_theme);
        btnSensorDiag       = findViewById(R.id.btn_sensor_diagnostic);
    }

    private void loadCurrentValues() {
        int sensitivity = prefs.getInt("sensor_sensitivity", 50);
        int speed       = prefs.getInt("speed_alert_threshold", 100);
        int fatigue     = prefs.getInt("fatigue_interval_minutes", 30);
        boolean autoStart = prefs.getBoolean("auto_start_proximity", false);
        boolean autoTheme = prefs.getBoolean("auto_theme_light", false);

        sbSensitivity.setProgress(sensitivity);
        sbSpeedThreshold.setProgress(speed);
        sbFatigueInterval.setProgress(fatigue);
        swAutoStart.setChecked(autoStart);
        swAutoTheme.setChecked(autoTheme);

        tvSensitivityValue.setText(sensitivity + "%");
        tvSpeedValue.setText(speed + " km/h");
        tvFatigueValue.setText(fatigue + " min");
    }

    private void setupListeners() {
        sbSensitivity.setOnSeekBarChangeListener(new SimpleSeekBarListener() {
            public void onProgressChanged(SeekBar sb, int progress, boolean user) {
                tvSensitivityValue.setText(progress + "%");
                prefs.edit().putInt("sensor_sensitivity", progress).apply();
            }
        });

        sbSpeedThreshold.setMax(200);
        sbSpeedThreshold.setOnSeekBarChangeListener(new SimpleSeekBarListener() {
            public void onProgressChanged(SeekBar sb, int progress, boolean user) {
                int actual = Math.max(progress, 30); // minimum 30 km/h
                tvSpeedValue.setText(actual + " km/h");
                prefs.edit().putInt("speed_alert_threshold", actual).apply();
            }
        });

        sbFatigueInterval.setMax(120);
        sbFatigueInterval.setOnSeekBarChangeListener(new SimpleSeekBarListener() {
            public void onProgressChanged(SeekBar sb, int progress, boolean user) {
                int actual = Math.max(progress, 10); // minimum 10 min
                tvFatigueValue.setText(actual + " min");
                prefs.edit().putInt("fatigue_interval_minutes", actual).apply();
            }
        });

        swAutoStart.setOnCheckedChangeListener((btn, checked) ->
                prefs.edit().putBoolean("auto_start_proximity", checked).apply());

        swAutoTheme.setOnCheckedChangeListener((btn, checked) ->
                prefs.edit().putBoolean("auto_theme_light", checked).apply());

        btnSensorDiag.setOnClickListener(v ->
                startActivity(new Intent(this, SensorMonitorActivity.class)));
    }

    // Simple anonymous SeekBar listener base
    private abstract static class SimpleSeekBarListener implements SeekBar.OnSeekBarChangeListener {
        public void onStartTrackingTouch(SeekBar sb) {}
        public void onStopTrackingTouch(SeekBar sb)  {}
    }
}
