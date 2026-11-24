package com.drivesense.app.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.drivesense.app.R;
import com.drivesense.app.database.DriveSenseDatabase;
import com.drivesense.app.database.entities.TripEntity;
import com.drivesense.app.database.entities.EventEntity;
import com.drivesense.app.utils.BackNavigation;
import com.drivesense.app.scoring.DrivingScoreCalculator;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * HomeActivity — application home screen.
 * Shows last trip score summary and navigation buttons.
 */
public class HomeActivity extends AppCompatActivity {

    private TextView tvLastScore, tvLastDate, tvLastDistance, tvUsername;
    private Button btnStartTrip, btnTripHistory, btnSettings, btnLogout;
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
        cardLastTrip   = findViewById(R.id.card_last_trip);
        btnStartTrip   = findViewById(R.id.btn_start_trip);
        btnTripHistory = findViewById(R.id.btn_trip_history);
        btnSettings    = findViewById(R.id.btn_settings);
        btnLogout = findViewById(R.id.btn_logout);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String username = prefs.getString("username", "Driver");
        tvUsername.setText("Welcome, " + username);

        btnStartTrip.setOnClickListener(v ->
                startActivity(new Intent(this, TripDashboardActivity.class)));
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
        loadLastTrip();
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

    private TripEntity createDemoTrip(DriveSenseDatabase db) {
        TripEntity trip = new TripEntity();
        trip.startTime = System.currentTimeMillis() - 36 * 60 * 1000L;
        trip.endTime = System.currentTimeMillis();
        trip.durationMs = 36 * 60 * 1000L;
        trip.distanceKm = 18.7;
        trip.maxSpeedKmh = 86f;
        trip.avgSpeedKmh = 31.2f;
        trip.finalScore = 88;
        trip.rating = "Good";
        trip.hardBrakingCount = 1;
        trip.rapidAccelCount = 2;
        trip.sharpTurnCount = 1;
        trip.suddenStopCount = 0;
        trip.vibrationCount = 3;
        trip.overspeedCount = 1;
        trip.roadSmoothPct = 68f;
        trip.roadModeratePct = 24f;
        trip.roadRoughPct = 8f;
        trip.routeJson = "[{\"lat\":28.6139,\"lon\":77.2090},{\"lat\":28.6152,\"lon\":77.2105}"
                + ",{\"lat\":28.6170,\"lon\":77.2120},{\"lat\":28.6191,\"lon\":77.2142}"
                + ",{\"lat\":28.6210,\"lon\":77.2160},{\"lat\":28.6235,\"lon\":77.2181}"
                + ",{\"lat\":28.6260,\"lon\":77.2200},{\"lat\":28.6285,\"lon\":77.2222}]";
        trip.userId = "local_tanmay";
        trip.id = db.tripDao().insertTrip(trip);
        EventEntity hardBraking = new EventEntity();
        hardBraking.tripId = trip.id;
        hardBraking.eventType = "HARD_BRAKING";
        hardBraking.timestamp = trip.startTime + 7 * 60 * 1000L;
        hardBraking.latitude = 28.6139;
        hardBraking.longitude = 77.2090;
        hardBraking.speedKmh = 68f;
        hardBraking.severity = 1.4f;
        hardBraking.sensorMagnitude = 14f;
        hardBraking.description = "Hard braking detected";
        db.eventDao().insertEvent(hardBraking);
        return trip;
    }
}
