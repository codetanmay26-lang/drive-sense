package com.drivesense.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.drivesense.app.R;
import com.drivesense.app.adapters.ActiveDriversAdapter;
import com.drivesense.app.models.ActiveDriverModel;

import java.util.ArrayList;
import java.util.List;

/**
 * AdminFleetDashboardActivity — Fleet Manager dashboard showing active drivers & live detailed telemetry.
 */
public class AdminFleetDashboardActivity extends AppCompatActivity {

    private RecyclerView rvActiveDrivers;
    private ActiveDriversAdapter adapter;
    private TextView tvActiveCount, tvFleetAvgScore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_fleet_dashboard);

        rvActiveDrivers = findViewById(R.id.rv_active_drivers);
        tvActiveCount = findViewById(R.id.tv_active_drivers_count);
        tvFleetAvgScore = findViewById(R.id.tv_fleet_avg_score);
        Button btnLogout = findViewById(R.id.btn_logout);

        findViewById(R.id.btn_back).setOnClickListener(v -> navigateToLogin());
        btnLogout.setOnClickListener(v -> navigateToLogin());

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                navigateToLogin();
            }
        });

        rvActiveDrivers.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ActiveDriversAdapter();
        rvActiveDrivers.setAdapter(adapter);

        loadActiveFleetDrivers();
    }

    private void loadActiveFleetDrivers() {
        List<ActiveDriverModel> list = new ArrayList<>();

        // 1. Tanmay (Excellent Driver - Score 94)
        list.add(new ActiveDriverModel(
                "Tanmay", "DL-01-DS-2026", "48 km/h", 48.0f, "60 km/h (City)",
                94, "8.4 km", "16m 20s", "ON TRIP - Safe",
                "Events: 0 Events (Smooth)", "Smooth (85%)", 1L,
                0, 0, 0, 0, 0, 0
        ));

        // 2. Rohit (Good Driver - Score 82)
        list.add(new ActiveDriverModel(
                "Rohit", "HR-26-TR-4512", "58 km/h", 58.0f, "60 km/h (City)",
                82, "12.1 km", "22m 10s", "ON TRIP - Active",
                "Events: 1 Hard Brake, 1 Rapid Accel", "Moderate (65%)", 2L,
                1, 1, 0, 0, 0, 0
        ));

        // 3. Ayan (Average Driver - Score 68)
        list.add(new ActiveDriverModel(
                "Ayan", "MH-02-EX-9988", "76 km/h", 76.0f, "60 km/h (Overspeeding)",
                68, "14.8 km", "25m 05s", "ON TRIP - Warning",
                "Events: 2 Hard Brake, 1 Sharp Turn, 1 Overspeed", "Rough (50%)", 3L,
                2, 0, 1, 0, 0, 1
        ));

        // 4. Vikram (Needs Improvement - Score 58)
        list.add(new ActiveDriverModel(
                "Vikram", "UP-16-VK-8877", "68 km/h", 68.0f, "60 km/h (City)",
                58, "9.2 km", "17m 30s", "ON TRIP - Warning",
                "Events: 1 Hard Brake, 2 Sharp Turn, 2 Vibration", "Rough (60%)", 4L,
                1, 0, 2, 0, 2, 0
        ));

        // 5. Siddharth (Poor / Unsafe Driver - Score 42 - Under 50!)
        list.add(new ActiveDriverModel(
                "Siddharth", "KA-01-SF-3344", "92 km/h", 92.0f, "60 km/h (High Risk Speed)",
                42, "18.5 km", "28m 40s", "ON TRIP - HIGH RISK",
                "Events: 4 Hard Brake, 2 Sudden Stop, 2 Overspeed", "Rough (70%)", 5L,
                4, 0, 0, 2, 0, 2
        ));

        // Compute REAL exact average of all drivers
        int sumScore = 0;
        for (ActiveDriverModel d : list) sumScore += d.safetyScore;
        int avgFleetScore = Math.round((float) sumScore / list.size());

        if (tvFleetAvgScore != null) {
            tvFleetAvgScore.setText(avgFleetScore + " / 100");
        }

        tvActiveCount.setText(list.size() + " Active Drivers Currently Monitoring");
        adapter.setDrivers(list);
    }

    private void navigateToLogin() {
        PreferenceManager.getDefaultSharedPreferences(this).edit()
                .putBoolean("logged_in", false)
                .apply();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
