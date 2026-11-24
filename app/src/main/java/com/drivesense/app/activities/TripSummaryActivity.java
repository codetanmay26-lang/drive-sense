package com.drivesense.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.drivesense.app.R;
import com.drivesense.app.database.DriveSenseDatabase;
import com.drivesense.app.database.entities.TripEntity;
import com.drivesense.app.scoring.DrivingScoreCalculator;
import com.drivesense.app.utils.ReportExporter;
import com.drivesense.app.utils.BackNavigation;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * TripSummaryActivity — displayed after a trip ends.
 * Shows final score, event breakdown, and trip statistics.
 */
public class TripSummaryActivity extends AppCompatActivity {

    private TextView tvFinalScore, tvRating, tvDuration, tvDistance;
    private TextView tvAvgSpeed, tvMaxSpeed, tvHardBraking, tvRapidAccel;
    private TextView tvSharpTurn, tvSuddenStop, tvVibration, tvOverspeed;
    private TextView tvRoadCondition, tvTripDate;
    private Button btnViewMap, btnExport, btnHome;
    private View scoreBar;

    private long tripId = -1;
    private TripEntity tripEntity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trip_summary);
        BackNavigation.attach(this);

        tripId = getIntent().getLongExtra("trip_id", -1);
        bindViews();
        loadTripData();
    }

    private void bindViews() {
        tvFinalScore    = findViewById(R.id.tv_final_score);
        tvRating        = findViewById(R.id.tv_rating);
        tvDuration      = findViewById(R.id.tv_duration);
        tvDistance      = findViewById(R.id.tv_distance);
        tvAvgSpeed      = findViewById(R.id.tv_avg_speed);
        tvMaxSpeed      = findViewById(R.id.tv_max_speed);
        tvHardBraking   = findViewById(R.id.tv_hard_braking_count);
        tvRapidAccel    = findViewById(R.id.tv_rapid_accel_count);
        tvSharpTurn     = findViewById(R.id.tv_sharp_turn_count);
        tvSuddenStop    = findViewById(R.id.tv_sudden_stop_count);
        tvVibration     = findViewById(R.id.tv_vibration_count);
        tvOverspeed     = findViewById(R.id.tv_overspeed_count);
        tvRoadCondition = findViewById(R.id.tv_road_condition);
        tvTripDate      = findViewById(R.id.tv_trip_date);
        scoreBar        = findViewById(R.id.score_bar);
        btnViewMap      = findViewById(R.id.btn_view_map);
        btnExport       = findViewById(R.id.btn_export);
        btnHome         = findViewById(R.id.btn_home);

        btnViewMap.setOnClickListener(v -> viewMap());
        btnExport.setOnClickListener(v -> exportReport());
        btnHome.setOnClickListener(v -> {
            startActivity(new Intent(this, HomeActivity.class)
                    .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
            finish();
        });
    }

    private void loadTripData() {
        if (tripId < 0) {
            Toast.makeText(this, "Trip data not found.", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            DriveSenseDatabase db = DriveSenseDatabase.getInstance(this);
            tripEntity = db.tripDao().getTripById(tripId);
            runOnUiThread(() -> {
                if (tripEntity == null) return;
                populateUI(tripEntity);
            });
        }).start();
    }

    private void populateUI(TripEntity t) {
        tvFinalScore.setText(String.valueOf(t.finalScore));
        tvRating.setText(t.rating);
        setScoreColor(t.finalScore);

        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());
        tvTripDate.setText(sdf.format(new Date(t.startTime)));

        long secs = t.durationMs / 1000;
        tvDuration.setText(String.format(Locale.getDefault(), "%02d:%02d:%02d",
                secs/3600, (secs%3600)/60, secs%60));
        tvDistance.setText(String.format(Locale.getDefault(), "%.1f km", t.distanceKm));
        tvAvgSpeed.setText(String.format(Locale.getDefault(), "%.1f km/h", t.avgSpeedKmh));
        tvMaxSpeed.setText(String.format(Locale.getDefault(), "%.1f km/h", t.maxSpeedKmh));

        tvHardBraking.setText(String.valueOf(t.hardBrakingCount));
        tvRapidAccel.setText(String.valueOf(t.rapidAccelCount));
        tvSharpTurn.setText(String.valueOf(t.sharpTurnCount));
        tvSuddenStop.setText(String.valueOf(t.suddenStopCount));
        tvVibration.setText(String.valueOf(t.vibrationCount));
        tvOverspeed.setText(String.valueOf(t.overspeedCount));

        String road = "Smooth: " + (int)t.roadSmoothPct + "% | "
                + "Moderate: " + (int)t.roadModeratePct + "% | "
                + "Rough: " + (int)t.roadRoughPct + "%";
        tvRoadCondition.setText(road);
    }

    private void setScoreColor(int score) {
        int color;
        if (score >= 90)      color = ContextCompat.getColor(this, R.color.colorScoreExcellent);
        else if (score >= 75) color = ContextCompat.getColor(this, R.color.colorScoreGood);
        else if (score >= 50) color = ContextCompat.getColor(this, R.color.colorScoreAverage);
        else                  color = ContextCompat.getColor(this, R.color.colorScorePoor);
        tvFinalScore.setTextColor(color);
        tvRating.setTextColor(color);
        scoreBar.setBackgroundColor(color);
    }

    private void viewMap() {
        Intent i = new Intent(this, MapActivity.class);
        i.putExtra("trip_id", tripId);
        startActivity(i);
    }

    private void exportReport() {
        if (tripEntity != null) {
            String report = ReportExporter.generateTextReport(tripEntity);
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "DriveSense Trip Report");
            shareIntent.putExtra(Intent.EXTRA_TEXT, report);
            startActivity(Intent.createChooser(shareIntent, getString(R.string.share_report)));
        }
    }
}
