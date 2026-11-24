package com.drivesense.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.drivesense.app.R;
import com.drivesense.app.adapters.TripHistoryAdapter;
import com.drivesense.app.database.DriveSenseDatabase;
import com.drivesense.app.database.entities.TripEntity;
import com.drivesense.app.utils.BackNavigation;

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
                            adapter.setTrips(java.util.Collections.emptyList());
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
