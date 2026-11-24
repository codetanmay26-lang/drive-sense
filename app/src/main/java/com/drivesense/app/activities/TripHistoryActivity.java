package com.drivesense.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.drivesense.app.R;
import com.drivesense.app.adapters.TripHistoryAdapter;
import com.drivesense.app.database.DriveSenseDatabase;
import com.drivesense.app.database.entities.TripEntity;
import com.drivesense.app.utils.BackNavigation;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collections;
import java.util.List;

/**
 * TripHistoryActivity — shows a list of all past trips from local Room database.
 */
public class TripHistoryActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TextView tvEmpty;
    private TripHistoryAdapter adapter;
    private Button btnClearHistory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trip_history);
        BackNavigation.attach(this);

        recyclerView = findViewById(R.id.rv_trips);
        tvEmpty      = findViewById(R.id.tv_empty);
        btnClearHistory = findViewById(R.id.btn_clear_history);
        btnClearHistory.setOnClickListener(v -> confirmClearHistory());

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TripHistoryAdapter(trip -> {
            Intent i = new Intent(this, TripDetailActivity.class);
            i.putExtra("trip_id", trip.id);
            startActivity(i);
        });
        recyclerView.setAdapter(adapter);

        loadTrips();
    }

    private void loadTrips() {
        new Thread(() -> {
            DriveSenseDatabase db = DriveSenseDatabase.getInstance(this);
            ensureSampleTripsExist(db);
            List<TripEntity> trips = db.tripDao().getAllTrips();
            runOnUiThread(() -> {
                if (trips.isEmpty()) {
                    tvEmpty.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                } else {
                    tvEmpty.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                    adapter.setTrips(trips);
                }
            });
        }).start();
    }

    private void ensureSampleTripsExist(DriveSenseDatabase db) {
        List<TripEntity> existing = db.tripDao().getAllTrips();
        if (existing != null && !existing.isEmpty()) return;

        long now = System.currentTimeMillis();

        // Trip 1 (7.4 km, Score 94 - Ending at Current City / Delhi Location)
        TripEntity t1 = new TripEntity();
        t1.startTime = now - (2 * 3600 * 1000L);
        t1.endTime = now - (2 * 3600 * 1000L) + (18 * 60 * 1000L);
        t1.durationMs = 18 * 60 * 1000L;
        t1.distanceKm = 7.4;
        t1.avgSpeedKmh = 42.5f;
        t1.maxSpeedKmh = 68.0f;
        t1.finalScore = 94;
        t1.rating = "EXCELLENT";
        t1.hardBrakingCount = 1;
        t1.rapidAccelCount = 0;
        t1.sharpTurnCount = 0;
        t1.suddenStopCount = 0;
        t1.vibrationCount = 0;
        t1.overspeedCount = 0;
        t1.roadSmoothPct = 78f;
        t1.roadModeratePct = 18f;
        t1.roadRoughPct = 4f;
        t1.routeJson = generateSampleRouteJson(7.4, 28.6139, 77.2090);
        t1.userId = "default";
        db.tripDao().insertTrip(t1);

        // Trip 2 (9.2 km, Score 88)
        TripEntity t2 = new TripEntity();
        t2.startTime = now - (26 * 3600 * 1000L);
        t2.endTime = now - (26 * 3600 * 1000L) + (24 * 60 * 1000L);
        t2.durationMs = 24 * 60 * 1000L;
        t2.distanceKm = 9.2;
        t2.avgSpeedKmh = 38.0f;
        t2.maxSpeedKmh = 72.5f;
        t2.finalScore = 88;
        t2.rating = "GOOD";
        t2.hardBrakingCount = 2;
        t2.rapidAccelCount = 1;
        t2.sharpTurnCount = 1;
        t2.suddenStopCount = 0;
        t2.vibrationCount = 1;
        t2.overspeedCount = 0;
        t2.roadSmoothPct = 60f;
        t2.roadModeratePct = 30f;
        t2.roadRoughPct = 10f;
        t2.routeJson = generateSampleRouteJson(9.2, 28.6180, 77.2150);
        t2.userId = "default";
        db.tripDao().insertTrip(t2);

        // Trip 3 (5.8 km, Score 96)
        TripEntity t3 = new TripEntity();
        t3.startTime = now - (50 * 3600 * 1000L);
        t3.endTime = now - (50 * 3600 * 1000L) + (14 * 60 * 1000L);
        t3.durationMs = 14 * 60 * 1000L;
        t3.distanceKm = 5.8;
        t3.avgSpeedKmh = 36.2f;
        t3.maxSpeedKmh = 58.0f;
        t3.finalScore = 96;
        t3.rating = "EXCELLENT";
        t3.hardBrakingCount = 0;
        t3.rapidAccelCount = 0;
        t3.sharpTurnCount = 0;
        t3.suddenStopCount = 0;
        t3.vibrationCount = 0;
        t3.overspeedCount = 0;
        t3.roadSmoothPct = 85f;
        t3.roadModeratePct = 12f;
        t3.roadRoughPct = 3f;
        t3.routeJson = generateSampleRouteJson(5.8, 28.6220, 77.2000);
        t3.userId = "default";
        db.tripDao().insertTrip(t3);
    }

    public static String generateSampleRouteJson(double targetDistanceKm, double endLat, double endLon) {
        JSONArray arr = new JSONArray();
        int steps = 20;
        double totalLatDelta = (targetDistanceKm / 111.0) * 0.75;
        double totalLonDelta = (targetDistanceKm / 98.0) * 0.65;
        double startLat = endLat - totalLatDelta;
        double startLon = endLon - totalLonDelta;

        try {
            for (int i = 0; i <= steps; i++) {
                double frac = (double) i / steps;
                double curveOffset = Math.sin(frac * Math.PI) * 0.0035;
                double lat = startLat + (frac * totalLatDelta) + curveOffset;
                double lon = startLon + (frac * totalLonDelta) - (curveOffset * 0.4);

                JSONObject point = new JSONObject();
                point.put("lat", lat);
                point.put("lon", lon);
                arr.put(point);
            }
        } catch (JSONException ignored) {}
        return arr.toString();
    }

    private void confirmClearHistory() {
        new AlertDialog.Builder(this)
                .setTitle("Clear Trip History")
                .setMessage("Delete all saved trips and driving events?")
                .setPositiveButton("Clear", (dialog, which) -> {
                    new Thread(() -> {
                        DriveSenseDatabase db = DriveSenseDatabase.getInstance(this);
                        db.eventDao().deleteAllEvents();
                        db.tripDao().deleteAllTrips();
                        runOnUiThread(() -> {
                            adapter.setTrips(Collections.emptyList());
                            tvEmpty.setVisibility(View.VISIBLE);
                            recyclerView.setVisibility(View.GONE);
                            Toast.makeText(this, "Trip history cleared", Toast.LENGTH_SHORT).show();
                        });
                    }).start();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
