package com.drivesense.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.drivesense.app.R;
import com.drivesense.app.database.DriveSenseDatabase;
import com.drivesense.app.database.entities.TripEntity;
import com.drivesense.app.utils.ReportExporter;
import com.drivesense.app.utils.BackNavigation;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * TripDetailActivity — detailed view of a single past trip.
 * Shows full summary and links to route map.
 */
public class TripDetailActivity extends AppCompatActivity {

    private long tripId;
    private TripEntity tripEntity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trip_detail);
        BackNavigation.attach(this);

        tripId = getIntent().getLongExtra("trip_id", -1);
        loadAndDisplay();
    }

    private void loadAndDisplay() {
        new Thread(() -> {
            if (tripId < 0) return;
            DriveSenseDatabase db = DriveSenseDatabase.getInstance(this);
            tripEntity = db.tripDao().getTripById(tripId);
            runOnUiThread(() -> {
                if (tripEntity == null) return;

                TextView tvScore    = findViewById(R.id.tv_score);
                TextView tvRating   = findViewById(R.id.tv_rating);
                TextView tvDate     = findViewById(R.id.tv_date);
                TextView tvDuration = findViewById(R.id.tv_duration);
                TextView tvDistance = findViewById(R.id.tv_distance);
                TextView tvAvgSpeed = findViewById(R.id.tv_avg_speed);
                TextView tvMaxSpeed = findViewById(R.id.tv_max_speed);
                TextView tvEvents   = findViewById(R.id.tv_events);
                Button btnMap       = findViewById(R.id.btn_view_map);
                Button btnShare     = findViewById(R.id.btn_share);

                tvScore.setText(String.valueOf(tripEntity.finalScore));
                tvRating.setText(tripEntity.rating);

                SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());
                tvDate.setText(sdf.format(new Date(tripEntity.startTime)));

                long secs = tripEntity.durationMs / 1000;
                tvDuration.setText(String.format(Locale.getDefault(), "%02d:%02d:%02d",
                        secs/3600, (secs%3600)/60, secs%60));
                tvDistance.setText(String.format(Locale.getDefault(), "%.1f km", tripEntity.distanceKm));
                tvAvgSpeed.setText(String.format(Locale.getDefault(), "%.1f km/h", tripEntity.avgSpeedKmh));
                tvMaxSpeed.setText(String.format(Locale.getDefault(), "%.1f km/h", tripEntity.maxSpeedKmh));

                String events = "Hard Braking: " + tripEntity.hardBrakingCount
                        + "\nRapid Acceleration: " + tripEntity.rapidAccelCount
                        + "\nSharp Turns: " + tripEntity.sharpTurnCount
                        + "\nSudden Stops: " + tripEntity.suddenStopCount
                        + "\nVibration: " + tripEntity.vibrationCount
                        + "\nOver-Speed: " + tripEntity.overspeedCount;
                tvEvents.setText(events);

                btnMap.setOnClickListener(v -> {
                    Intent i = new Intent(this, MapActivity.class);
                    i.putExtra("trip_id", tripId);
                    startActivity(i);
                });
                btnShare.setOnClickListener(v -> {
                    String report = ReportExporter.generateTextReport(tripEntity);
                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
                    shareIntent.setType("text/plain");
                    shareIntent.putExtra(Intent.EXTRA_SUBJECT, "DriveSense Trip Report");
                    shareIntent.putExtra(Intent.EXTRA_TEXT, report);
                    startActivity(Intent.createChooser(shareIntent, getString(R.string.share_report)));
                });
            });
        }).start();
    }
}
