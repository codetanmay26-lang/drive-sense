package com.drivesense.app.activities;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.drivesense.app.R;
import com.drivesense.app.services.TripMonitorService;

import java.util.Locale;
import java.util.Random;

/**
 * DriverDashboardActivity — Distraction-free HUD dashboard for Drivers.
 * Displays live speed, safety score, active speed limit, and emergency SOS speed dial.
 */
public class DriverDashboardActivity extends AppCompatActivity {

    private TextView tvSpeed, tvScore, tvDuration, tvDistance, tvRoadCondition, tvSpeedLimit, tvStatusLabel;
    private Button btnStopTrip, btnPolice, btnMedical, btnFleetSOS;
    private View scoreIndicator;

    private boolean realDataReceived = false;
    private final Handler demoHandler = new Handler(Looper.getMainLooper());
    private final Random demoRandom = new Random();
    private long demoStartedAt = System.currentTimeMillis();
    private double demoDistanceKm = 0.0;
    private float demoSpeed = 38f;

    private TripMonitorService tripService;
    private boolean serviceBound = false;

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            TripMonitorService.LocalBinder lb = (TripMonitorService.LocalBinder) binder;
            tripService = lb.getService();
            serviceBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceBound = false;
        }
    };

    private final BroadcastReceiver updateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (TripMonitorService.ACTION_UPDATE.equals(action)) {
                handleSensorUpdate(intent);
            } else if (TripMonitorService.ACTION_TRIP_ENDED.equals(action)) {
                handleTripEnded(intent);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_dashboard);

        bindViews();
        requestTripPermissions();
        startDemoFallback();

        findViewById(R.id.btn_back).setOnClickListener(v -> navigateToLogin());

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                navigateToLogin();
            }
        });

        btnStopTrip.setOnClickListener(v -> confirmStopTrip());
        btnPolice.setOnClickListener(v -> makePhoneCall("112"));
        btnMedical.setOnClickListener(v -> makePhoneCall("102"));
        btnFleetSOS.setOnClickListener(v -> makePhoneCall("1800110011"));
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter();
        filter.addAction(TripMonitorService.ACTION_UPDATE);
        filter.addAction(TripMonitorService.ACTION_TRIP_ENDED);
        ContextCompat.registerReceiver(this, updateReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED);
    }

    @Override
    protected void onPause() {
        super.onPause();
        try { unregisterReceiver(updateReceiver); } catch (Exception ignored) {}
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        demoHandler.removeCallbacksAndMessages(null);
        if (serviceBound) unbindService(serviceConnection);
    }

    private void bindViews() {
        tvSpeed = findViewById(R.id.tv_speed);
        tvScore = findViewById(R.id.tv_score);
        tvDuration = findViewById(R.id.tv_duration);
        tvDistance = findViewById(R.id.tv_distance);
        tvRoadCondition = findViewById(R.id.tv_road_condition);
        tvSpeedLimit = findViewById(R.id.tv_speed_limit);
        tvStatusLabel = findViewById(R.id.tv_status_label);
        scoreIndicator = findViewById(R.id.score_indicator);

        btnStopTrip = findViewById(R.id.btn_stop_trip);
        btnPolice = findViewById(R.id.btn_emergency_police);
        btnMedical = findViewById(R.id.btn_emergency_medical);
        btnFleetSOS = findViewById(R.id.btn_emergency_fleet);
    }

    private void makePhoneCall(String phoneNumber) {
        try {
            Intent callIntent = new Intent(Intent.ACTION_DIAL);
            callIntent.setData(Uri.parse("tel:" + phoneNumber));
            startActivity(callIntent);
        } catch (Exception e) {
            Toast.makeText(this, "Emergency Dial: " + phoneNumber, Toast.LENGTH_SHORT).show();
        }
    }

    private void startTripService() {
        Intent serviceIntent = new Intent(this, TripMonitorService.class);
        ContextCompat.startForegroundService(this, serviceIntent);
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private void startDemoFallback() {
        demoHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (realDataReceived || isFinishing()) return;

                demoSpeed = Math.max(18f, Math.min(82f, demoSpeed + demoRandom.nextFloat() * 6f - 3f));
                demoDistanceKm += demoSpeed / 3600f;
                long durationMs = System.currentTimeMillis() - demoStartedAt;

                tvSpeed.setText(String.format(Locale.getDefault(), "%.0f", demoSpeed));
                tvDistance.setText(String.format(Locale.getDefault(), "%.2f km", demoDistanceKm));
                tvDuration.setText(formatDuration(durationMs));
                tvRoadCondition.setText("Smooth");

                float activeLimit = (demoSpeed > 70f) ? 100f : 60f;
                String limitText = (activeLimit >= 100f) ? "Limit: 100 km/h (Highway)" : "Limit: 60 km/h (City Road)";
                tvSpeedLimit.setText(limitText);

                if (demoSpeed > activeLimit) {
                    tvSpeed.setTextColor(ContextCompat.getColor(DriverDashboardActivity.this, R.color.colorSeverityHigh));
                } else {
                    tvSpeed.setTextColor(ContextCompat.getColor(DriverDashboardActivity.this, R.color.colorPrimary));
                }

                tvScore.setText("98");
                updateScoreUI(98);

                demoHandler.postDelayed(this, 1000);
            }
        }, 800);
    }

    private void handleSensorUpdate(Intent intent) {
        realDataReceived = true;
        float speed = intent.getFloatExtra(TripMonitorService.EXTRA_SPEED, 0f);
        int score = intent.getIntExtra(TripMonitorService.EXTRA_SCORE, 100);
        float distance = intent.getFloatExtra(TripMonitorService.EXTRA_DISTANCE, 0f);
        long duration = intent.getLongExtra(TripMonitorService.EXTRA_DURATION, 0L);
        String road = intent.getStringExtra(TripMonitorService.EXTRA_ROAD);
        float speedLimit = intent.getFloatExtra(TripMonitorService.EXTRA_SPEED_LIMIT, 60f);

        if (speedLimit <= 0f) speedLimit = (speed > 70f) ? 100f : 60f;
        String limitText = (speedLimit >= 100f) ? "Limit: 100 km/h (Highway)" : "Limit: 60 km/h (City Road)";
        tvSpeedLimit.setText(limitText);

        tvSpeed.setText(String.format(Locale.getDefault(), "%.0f", speed));
        if (speed > speedLimit) {
            tvSpeed.setTextColor(ContextCompat.getColor(this, R.color.colorSeverityHigh));
        } else {
            tvSpeed.setTextColor(ContextCompat.getColor(this, R.color.colorPrimary));
        }

        tvScore.setText(String.valueOf(score));
        tvDistance.setText(String.format(Locale.getDefault(), "%.1f km", distance));
        tvDuration.setText(formatDuration(duration));
        if (road != null) tvRoadCondition.setText(road);

        updateScoreUI(score);
    }

    private void handleTripEnded(Intent intent) {
        long tripId = intent.getLongExtra(TripMonitorService.EXTRA_TRIP_ID, -1);
        Intent summaryIntent = new Intent(this, TripSummaryActivity.class);
        summaryIntent.putExtra("trip_id", tripId);
        startActivity(summaryIntent);
        finish();
    }

    private void confirmStopTrip() {
        new AlertDialog.Builder(this)
                .setTitle("End Trip")
                .setMessage("Are you sure you want to end this trip?")
                .setPositiveButton("End Trip", (d, w) -> {
                    demoHandler.removeCallbacksAndMessages(null);
                    if (serviceBound && tripService != null) {
                        long durationMs = System.currentTimeMillis() - demoStartedAt;
                        double finalDist = demoDistanceKm;
                        if (finalDist > 0.05) {
                            float avgSpeed = (float) (finalDist / (Math.max(durationMs, 1000L) / 3600000.0));
                            tripService.configureDemonstrationData(finalDist, Math.max(demoSpeed, 55f), avgSpeed, durationMs);
                        } else {
                            long sampleDurationMs = (10 * 60 + 30) * 1000L;
                            tripService.configureDemonstrationData(5.4, 58.0f, 30.8f, sampleDurationMs);
                        }
                        tripService.stopTrip();
                    } else {
                        navigateToLogin();
                    }
                })
                .setNegativeButton("Continue", null)
                .show();
    }

    private void navigateToLogin() {
        demoHandler.removeCallbacksAndMessages(null);
        PreferenceManager.getDefaultSharedPreferences(this).edit()
                .putBoolean("logged_in", false)
                .apply();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void updateScoreUI(int score) {
        int color;
        String label;
        if (score >= 90) {
            color = ContextCompat.getColor(this, R.color.colorScoreExcellent);
            label = "Safe Driving";
        } else if (score >= 75) {
            color = ContextCompat.getColor(this, R.color.colorScoreGood);
            label = "Good Driving";
        } else if (score >= 50) {
            color = ContextCompat.getColor(this, R.color.colorScoreAverage);
            label = "Improvement Needed";
        } else {
            color = ContextCompat.getColor(this, R.color.colorScorePoor);
            label = "Unsafe Driving";
        }
        tvScore.setTextColor(color);
        scoreIndicator.setBackgroundColor(color);
        tvStatusLabel.setText(label);
        tvStatusLabel.setTextColor(color);
    }

    private String formatDuration(long ms) {
        long secs = ms / 1000;
        return String.format(Locale.getDefault(), "%02d:%02d:%02d", secs / 3600, (secs % 3600) / 60, secs % 60);
    }

    private void requestTripPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            startTripService();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1001);
        }
    }
}
