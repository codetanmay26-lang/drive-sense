package com.drivesense.app.activities;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.app.ActivityCompat;

import com.drivesense.app.R;
import com.drivesense.app.services.TripMonitorService;
import com.drivesense.app.utils.BackNavigation;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Random;

/**
 * TripDashboardActivity — real-time driving monitoring screen.
 *
 * Shows live:
 *  - GPS speed
 *  - Driving score (color-coded)
 *  - Trip duration and distance
 *  - Road condition indicator
 *  - Accelerometer waveform (X, Y, Z axes) via MPAndroidChart
 *  - Gyroscope rotation graph
 *  - Event counter badges
 *  - Stop Trip button
 */
public class TripDashboardActivity extends AppCompatActivity {

    private static final int MAX_GRAPH_ENTRIES = 100; // keep last 100 samples visible
    private static final int TRIP_PERMISSION_REQUEST = 1001;

    // UI
    private TextView tvSpeed, tvScore, tvDuration, tvDistance, tvRoadCondition;
    private TextView tvSpeedLimit, tvHardBraking, tvRapidAccel, tvSharpTurn;
    private TextView tvSuddenStop, tvVibration, tvOverspeed, tvStatusLabel;
    private Button btnStopTrip, btnEventLog;
    private LineChart accelChart, gyroChart;
    private View scoreIndicator;

    // Graph data
    private LineDataSet accXSet, accYSet, accZSet;
    private LineDataSet gyrXSet, gyrYSet, gyrZSet;
    private LineData accelData, gyroData;
    private float graphX = 0;
    private boolean realDataReceived = false;
    private final Handler demoHandler = new Handler(Looper.getMainLooper());
    private final Random demoRandom = new Random();
    private long demoStartedAt = System.currentTimeMillis();
    private double demoDistanceKm = 0.0;
    private float demoSpeed = 38f;

    // Event counters
    private int cntBraking = 0, cntAccel = 0, cntTurn = 0;
    private int cntStop = 0, cntVib = 0, cntSpeed = 0;

    // Service
    private TripMonitorService tripService;
    private boolean serviceBound = false;

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            TripMonitorService.LocalBinder lb = (TripMonitorService.LocalBinder) binder;
            tripService  = lb.getService();
            serviceBound = true;
        }
        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceBound = false;
        }
    };

    // Broadcast receiver for live sensor data
    private final BroadcastReceiver updateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (TripMonitorService.ACTION_UPDATE.equals(action)) {
                handleSensorUpdate(intent);
            } else if (TripMonitorService.ACTION_EVENT_DETECTED.equals(action)) {
                handleEventDetected(intent);
            } else if (TripMonitorService.ACTION_TRIP_ENDED.equals(action)) {
                handleTripEnded(intent);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trip_dashboard);
        BackNavigation.attach(this);
        bindViews();
        setupCharts();
        loadDriverAdminData();
        getSharedPreferences("drivesense_demo", MODE_PRIVATE).edit()
                .putLong("ride_start", System.currentTimeMillis())
                .apply();
        requestTripPermissions();
        startDemoFallback();

        btnStopTrip.setOnClickListener(v -> confirmStopTrip());
        btnEventLog.setOnClickListener(v ->
                startActivity(new Intent(this, EventLogActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter();
        filter.addAction(TripMonitorService.ACTION_UPDATE);
        filter.addAction(TripMonitorService.ACTION_EVENT_DETECTED);
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
        tvSpeed         = findViewById(R.id.tv_speed);
        tvScore         = findViewById(R.id.tv_score);
        tvDuration      = findViewById(R.id.tv_duration);
        tvDistance      = findViewById(R.id.tv_distance);
        tvRoadCondition = findViewById(R.id.tv_road_condition);
        tvSpeedLimit    = findViewById(R.id.tv_speed_limit);
        tvHardBraking   = findViewById(R.id.tv_hard_braking);
        tvRapidAccel    = findViewById(R.id.tv_rapid_accel);
        tvSharpTurn     = findViewById(R.id.tv_sharp_turn);
        tvSuddenStop    = findViewById(R.id.tv_sudden_stop);
        tvVibration     = findViewById(R.id.tv_vibration);
        tvOverspeed     = findViewById(R.id.tv_overspeed);
        tvStatusLabel   = findViewById(R.id.tv_status_label);
        scoreIndicator  = findViewById(R.id.score_indicator);
        btnStopTrip     = findViewById(R.id.btn_stop_trip);
        btnEventLog     = findViewById(R.id.btn_event_log);
        accelChart      = findViewById(R.id.chart_accelerometer);
        gyroChart       = findViewById(R.id.chart_gyroscope);
    }

    private void setupCharts() {
        // Accelerometer chart
        accXSet = createDataSet("X", ContextCompat.getColor(this, R.color.colorGraphX));
        accYSet = createDataSet("Y", ContextCompat.getColor(this, R.color.colorGraphY));
        accZSet = createDataSet("Z", ContextCompat.getColor(this, R.color.colorGraphZ));
        accelData = new LineData(accXSet, accYSet, accZSet);
        styleChart(accelChart, accelData, "Accelerometer (m/s\u00B2)");

        // Gyroscope chart
        gyrXSet = createDataSet("X", ContextCompat.getColor(this, R.color.colorGraphX));
        gyrYSet = createDataSet("Y", ContextCompat.getColor(this, R.color.colorGraphY));
        gyrZSet = createDataSet("Z", ContextCompat.getColor(this, R.color.colorGraphZ));
        gyroData = new LineData(gyrXSet, gyrYSet, gyrZSet);
        styleChart(gyroChart, gyroData, "Gyroscope (rad/s)");
    }

    private LineDataSet createDataSet(String label, int color) {
        LineDataSet ds = new LineDataSet(null, label);
        ds.setColor(color);
        ds.setLineWidth(1.5f);
        ds.setDrawCircles(false);
        ds.setDrawValues(false);
        ds.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        return ds;
    }

    private void styleChart(LineChart chart, LineData data, String desc) {
        chart.setData(data);
        chart.setMaxVisibleValueCount(MAX_GRAPH_ENTRIES);
        chart.setBackgroundColor(Color.WHITE);
        chart.setDrawGridBackground(false);
        chart.setDrawBorders(false);
        chart.setTouchEnabled(false);
        chart.setDragEnabled(false);
        chart.setScaleEnabled(false);

        Description d = new Description();
        d.setText(desc);
        d.setTextSize(10f);
        chart.setDescription(d);

        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setDrawLabels(false);

        chart.getAxisRight().setEnabled(false);
        chart.getAxisLeft().setDrawGridLines(true);
        chart.getAxisLeft().setGridColor(Color.parseColor("#EEEEEE"));

        Legend legend = chart.getLegend();
        legend.setForm(Legend.LegendForm.LINE);

        chart.invalidate();
    }

    private void startTripService() {
        Intent serviceIntent = new Intent(this, TripMonitorService.class);
        ContextCompat.startForegroundService(this, serviceIntent);
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private void loadDriverAdminData() {
        if (getIntent().hasExtra("driver_name")) {
            String driverName = getIntent().getStringExtra("driver_name");
            String vehicleNo = getIntent().getStringExtra("vehicle_no");
            int driverScore = getIntent().getIntExtra("driver_score", 92);
            String speedStr = getIntent().getStringExtra("driver_speed");
            String limitStr = getIntent().getStringExtra("driver_speed_limit");
            String distStr = getIntent().getStringExtra("driver_distance");
            String durStr = getIntent().getStringExtra("driver_duration");
            int hardBrake = getIntent().getIntExtra("hard_braking_count", 0);
            int rapidAccel = getIntent().getIntExtra("rapid_accel_count", 0);
            int sharpTurn = getIntent().getIntExtra("sharp_turn_count", 0);
            int suddenStop = getIntent().getIntExtra("sudden_stop_count", 0);
            int vibration = getIntent().getIntExtra("vibration_count", 0);
            int overspeed = getIntent().getIntExtra("overspeed_count", 0);
            String roadStr = getIntent().getStringExtra("road_condition");

            TextView tvHeaderTitle = findViewById(R.id.tv_dashboard_title);
            if (tvHeaderTitle != null) {
                tvHeaderTitle.setText("Driver: " + driverName + (vehicleNo != null ? " (" + vehicleNo + ")" : ""));
            }

            if (speedStr != null && tvSpeed != null) tvSpeed.setText(speedStr.replaceAll("[^0-9]", ""));
            if (tvScore != null) {
                tvScore.setText(String.valueOf(driverScore));
                updateScoreUI(driverScore);
            }
            if (limitStr != null && tvSpeedLimit != null) tvSpeedLimit.setText(limitStr);
            if (distStr != null && tvDistance != null) tvDistance.setText(distStr);
            if (durStr != null && tvDuration != null) tvDuration.setText(durStr);
            if (roadStr != null && tvRoadCondition != null) tvRoadCondition.setText("Road: " + roadStr);

            cntBraking = hardBrake;
            cntAccel = rapidAccel;
            cntTurn = sharpTurn;
            cntStop = suddenStop;
            cntVib = vibration;
            cntSpeed = overspeed;

            if (tvHardBraking != null) tvHardBraking.setText(String.valueOf(hardBrake));
            if (tvRapidAccel != null) tvRapidAccel.setText(String.valueOf(rapidAccel));
            if (tvSharpTurn != null) tvSharpTurn.setText(String.valueOf(sharpTurn));
            if (tvSuddenStop != null) tvSuddenStop.setText(String.valueOf(suddenStop));
            if (tvVibration != null) tvVibration.setText(String.valueOf(vibration));
            if (tvOverspeed != null) tvOverspeed.setText(String.valueOf(overspeed));
        }
    }

    private void startDemoFallback() {
        demoHandler.postDelayed(new Runnable() {
            @Override public void run() {
                if (realDataReceived || isFinishing()) return;

                if (!getIntent().hasExtra("driver_name")) {
                    demoSpeed = Math.max(18f, Math.min(82f,
                            demoSpeed + demoRandom.nextFloat() * 8f - 4f));
                    demoDistanceKm += demoSpeed / 3600f;
                    long durationMs = System.currentTimeMillis() - demoStartedAt;

                    tvSpeed.setText(String.format(Locale.getDefault(), "%.0f", demoSpeed));
                    tvDistance.setText(String.format(Locale.getDefault(), "%.2f km", demoDistanceKm));
                    tvDuration.setText(formatDuration(durationMs));
                    tvRoadCondition.setText("Road: Smooth");

                    float activeLimit = (demoSpeed > 70f) ? 100f : 60f;
                    String limitText = (activeLimit >= 100f) ? "100 km/h (Highway)" : "60 km/h (City)";
                    if (tvSpeedLimit != null) tvSpeedLimit.setText(limitText);

                    if (demoSpeed > activeLimit) {
                        tvSpeed.setTextColor(ContextCompat.getColor(TripDashboardActivity.this, R.color.colorSeverityHigh));
                    } else {
                        tvSpeed.setTextColor(ContextCompat.getColor(TripDashboardActivity.this, R.color.colorPrimary));
                    }

                    tvStatusLabel.setText("Monitoring active");
                    tvStatusLabel.setTextColor(ContextCompat.getColor(TripDashboardActivity.this,
                            R.color.colorPrimary));
                    tvScore.setText("92");
                    updateScoreUI(92);
                }

                float accX = (float) (demoRandom.nextGaussian() * 0.45f);
                float accY = (float) (demoRandom.nextGaussian() * 0.35f);
                float accZ = 9.8f + (float) (demoRandom.nextGaussian() * 0.20f);
                float gyrX = (float) (demoRandom.nextGaussian() * 0.18f);
                float gyrY = (float) (demoRandom.nextGaussian() * 0.16f);
                float gyrZ = (float) (demoRandom.nextGaussian() * 0.12f);
                addGraphEntry(accelChart, accelData, accXSet, accYSet, accZSet,
                        accX, accY, accZ);
                addGraphEntry(gyroChart, gyroData, gyrXSet, gyrYSet, gyrZSet,
                        gyrX, gyrY, gyrZ);
                graphX++;

                demoHandler.postDelayed(this, 1000);
            }
        }, 800);
    }

    private void requestTripPermissions() {
        ArrayList<String> permissions = new ArrayList<>();
        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS);
        }

        ArrayList<String> missing = new ArrayList<>();
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                missing.add(permission);
            }
        }

        if (missing.isEmpty()) {
            startTripService();
        } else {
            ActivityCompat.requestPermissions(this,
                    missing.toArray(new String[0]), TRIP_PERMISSION_REQUEST);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != TRIP_PERMISSION_REQUEST) return;

        boolean locationGranted = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        if (locationGranted) {
            startTripService();
        } else {
            new AlertDialog.Builder(this)
                    .setTitle("Location permission required")
                    .setMessage("Grant location permission to start trip monitoring.")
                    .setPositiveButton("Try again", (dialog, which) -> requestTripPermissions())
                    .setNegativeButton("Cancel", (dialog, which) -> finish())
                    .show();
        }
    }

    private void handleSensorUpdate(Intent intent) {
        realDataReceived = true;
        float speed    = intent.getFloatExtra(TripMonitorService.EXTRA_SPEED, 0f);
        int   score    = intent.getIntExtra(TripMonitorService.EXTRA_SCORE, 100);
        float distance = intent.getFloatExtra(TripMonitorService.EXTRA_DISTANCE, 0f);
        long  duration = intent.getLongExtra(TripMonitorService.EXTRA_DURATION, 0L);
        String road    = intent.getStringExtra(TripMonitorService.EXTRA_ROAD);
        float speedLimit = intent.getFloatExtra(TripMonitorService.EXTRA_SPEED_LIMIT, 60f);

        if (speedLimit <= 0f) speedLimit = (speed > 70f) ? 100f : 60f;
        String limitText = (speedLimit >= 100f) ? "100 km/h (Highway)" : "60 km/h (City)";
        if (tvSpeedLimit != null) tvSpeedLimit.setText(limitText);

        tvSpeed.setText(String.format(Locale.getDefault(), "%.0f", speed));
        if (speed > speedLimit) {
            tvSpeed.setTextColor(ContextCompat.getColor(this, R.color.colorSeverityHigh));
        } else {
            tvSpeed.setTextColor(ContextCompat.getColor(this, R.color.colorPrimary));
        }

        tvScore.setText(String.valueOf(score));
        tvDistance.setText(String.format(Locale.getDefault(), "%.1f km", distance));
        tvDuration.setText(formatDuration(duration));
        if (road != null) tvRoadCondition.setText("Road: " + road);

        updateScoreUI(score);

        // Update graphs
        float accX = intent.getFloatExtra("acc_x", 0f);
        float accY = intent.getFloatExtra("acc_y", 0f);
        float accZ = intent.getFloatExtra("acc_z", 0f);
        float gyrX = intent.getFloatExtra("gyro_x", 0f);
        float gyrY = intent.getFloatExtra("gyro_y", 0f);
        float gyrZ = intent.getFloatExtra("gyro_z", 0f);

        addGraphEntry(accelChart, accelData, accXSet, accYSet, accZSet, accX, accY, accZ);
        addGraphEntry(gyroChart,  gyroData,  gyrXSet, gyrYSet, gyrZSet, gyrX, gyrY, gyrZ);
        graphX++;
    }

    private void addGraphEntry(LineChart chart, LineData data,
                               LineDataSet ds1, LineDataSet ds2, LineDataSet ds3,
                               float v1, float v2, float v3) {
        data.addEntry(new Entry(graphX, v1), 0);
        data.addEntry(new Entry(graphX, v2), 1);
        data.addEntry(new Entry(graphX, v3), 2);
        data.notifyDataChanged();
        chart.notifyDataSetChanged();
        // Keep only last MAX_GRAPH_ENTRIES
        if (ds1.getEntryCount() > MAX_GRAPH_ENTRIES) {
            ds1.removeFirst(); ds2.removeFirst(); ds3.removeFirst();
        }
        chart.moveViewToX(data.getEntryCount());
        chart.invalidate();
    }

    private void handleEventDetected(Intent intent) {
        String type = intent.getStringExtra(TripMonitorService.EXTRA_EVENT_TYPE);
        if (type == null) return;
        switch (type) {
            case "HARD_BRAKING":        cntBraking++; tvHardBraking.setText(String.valueOf(cntBraking)); break;
            case "RAPID_ACCELERATION":  cntAccel++;   tvRapidAccel.setText(String.valueOf(cntAccel));    break;
            case "SHARP_TURN":          cntTurn++;    tvSharpTurn.setText(String.valueOf(cntTurn));      break;
            case "SUDDEN_STOP":         cntStop++;    tvSuddenStop.setText(String.valueOf(cntStop));     break;
            case "EXCESSIVE_VIBRATION": cntVib++;     tvVibration.setText(String.valueOf(cntVib));       break;
            case "OVER_SPEEDING":       cntSpeed++;   tvOverspeed.setText(String.valueOf(cntSpeed));     break;
        }
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
                            // Live trip tracking: use exact live distance & duration
                            float avgSpeed = (float) (finalDist / (Math.max(durationMs, 1000L) / 3600000.0));
                            tripService.configureDemonstrationData(
                                    finalDist, Math.max(demoSpeed, 55f),
                                    avgSpeed, durationMs);
                        } else {
                            // Full 5.4 km sample trip with matching 10 min 30 sec duration
                            long sampleDurationMs = (10 * 60 + 30) * 1000L; // 10m 30s
                            tripService.configureDemonstrationData(
                                    5.4, 58.0f,
                                    30.8f, sampleDurationMs);
                        }
                        tripService.stopTrip();
                    }
                })
                .setNegativeButton("Continue", null)
                .show();
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
        return String.format(Locale.getDefault(), "%02d:%02d:%02d",
                secs/3600, (secs%3600)/60, secs%60);
    }
}
