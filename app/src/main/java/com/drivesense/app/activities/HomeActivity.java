package com.drivesense.app.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.drivesense.app.R;
import com.drivesense.app.database.DriveSenseDatabase;
import com.drivesense.app.database.entities.TripEntity;
import com.drivesense.app.utils.BackNavigation;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * HomeActivity — application home screen with Role-based Access Control (Driver vs Admin Mode).
 */
public class HomeActivity extends AppCompatActivity {

    public static final String ROLE_DRIVER = "DRIVER";
    public static final String ROLE_ADMIN = "ADMIN";

    private TextView tvLastScore, tvLastDate, tvLastDistance, tvUsername, tvUserRole;
    private Button btnStartTrip, btnTripHistory, btnSettings, btnLogout, btnSwitchRole;
    private CardView cardLastTrip;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        BackNavigation.attach(this);

        tvLastScore    = findViewById(R.id.tv_last_score);
        tvLastDate     = findViewById(R.id.tv_last_date);
        tvLastDistance = findViewById(R.id.tv_last_distance);
        tvUsername     = findViewById(R.id.tv_username);
        tvUserRole     = findViewById(R.id.tv_user_role);
        cardLastTrip   = findViewById(R.id.card_last_trip);
        btnStartTrip   = findViewById(R.id.btn_start_trip);
        btnTripHistory = findViewById(R.id.btn_trip_history);
        btnSettings    = findViewById(R.id.btn_settings);
        btnLogout      = findViewById(R.id.btn_logout);
        btnSwitchRole  = findViewById(R.id.btn_switch_role);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String username = prefs.getString("username", "Tanmay");
        tvUsername.setText("Welcome, " + username);

        updateRoleUI();

        btnSwitchRole.setOnClickListener(v -> toggleUserRole());
        btnStartTrip.setOnClickListener(v -> startTripForActiveRole());
        btnTripHistory.setOnClickListener(v ->
                startActivity(new Intent(this, TripHistoryActivity.class)));
        btnSettings.setOnClickListener(v ->
                startActivity(new Intent(this, SettingsActivity.class)));
        btnLogout.setOnClickListener(v -> logout());

        loadLastTrip();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateRoleUI();
        loadLastTrip();
    }

    private void updateRoleUI() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String currentRole = prefs.getString("user_role", ROLE_DRIVER);

        if (ROLE_ADMIN.equalsIgnoreCase(currentRole)) {
            tvUserRole.setText("Portal Mode: FLEET ADMIN (Full Graphs & Telemetry)");
            tvUserRole.setTextColor(getResources().getColor(R.color.colorAccent));
        } else {
            tvUserRole.setText("Portal Mode: DRIVER (HUD Speed, Score & SOS)");
            tvUserRole.setTextColor(getResources().getColor(R.color.colorScoreExcellent));
        }
    }

    private void toggleUserRole() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String currentRole = prefs.getString("user_role", ROLE_DRIVER);
        String newRole = ROLE_DRIVER.equalsIgnoreCase(currentRole) ? ROLE_ADMIN : ROLE_DRIVER;

        prefs.edit().putString("user_role", newRole).apply();
        updateRoleUI();

        String roleName = ROLE_ADMIN.equalsIgnoreCase(newRole) ? "Fleet Admin / Manager" : "Driver Mode";
        Toast.makeText(this, "Switched to " + roleName, Toast.LENGTH_SHORT).show();
    }

    private void startTripForActiveRole() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String currentRole = prefs.getString("user_role", ROLE_DRIVER);

        if (ROLE_ADMIN.equalsIgnoreCase(currentRole)) {
            startActivity(new Intent(this, TripDashboardActivity.class));
        } else {
            startActivity(new Intent(this, DriverDashboardActivity.class));
        }
    }

    private void loadLastTrip() {
        new Thread(() -> {
            DriveSenseDatabase db = DriveSenseDatabase.getInstance(this);
            TripEntity last = db.tripDao().getLastTrip();
            TripEntity finalLast = last;
            runOnUiThread(() -> {
                if (finalLast != null) {
                    cardLastTrip.setVisibility(View.VISIBLE);
                    tvLastScore.setText(String.valueOf(finalLast.finalScore));
                    setScoreColor(finalLast.finalScore);
                    SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());
                    tvLastDate.setText(sdf.format(new Date(finalLast.startTime)));
                    tvLastDistance.setText(String.format(Locale.getDefault(), "%.1f km", finalLast.distanceKm));
                } else {
                    cardLastTrip.setVisibility(View.GONE);
                }
            });
        }).start();
    }

    private void setScoreColor(int score) {
        int color;
        if (score >= 90)      color = getResources().getColor(R.color.colorScoreExcellent);
        else if (score >= 75) color = getResources().getColor(R.color.colorScoreGood);
        else if (score >= 50) color = getResources().getColor(R.color.colorScoreAverage);
        else                  color = getResources().getColor(R.color.colorScorePoor);
        tvLastScore.setTextColor(color);
    }

    private void logout() {
        PreferenceManager.getDefaultSharedPreferences(this).edit()
                .putBoolean("logged_in", false)
                .apply();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
