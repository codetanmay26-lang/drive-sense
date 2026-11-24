package com.drivesense.app.activities;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.drivesense.app.R;
import com.drivesense.app.adapters.EventLogAdapter;
import com.drivesense.app.database.DriveSenseDatabase;
import com.drivesense.app.database.entities.EventEntity;
import com.drivesense.app.utils.BackNavigation;

import java.util.List;
import java.util.ArrayList;

/**
 * EventLogActivity — shows a chronological list of all detected events for the current trip.
 * During a live trip, events are displayed as they are detected via the service.
 * When opened for a past trip (via trip_id extra), loads from Room.
 */
public class EventLogActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private EventLogAdapter adapter;
    private long tripId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_log);
        BackNavigation.attach(this);

        tripId       = getIntent().getLongExtra("trip_id", -1);
        recyclerView = findViewById(R.id.rv_events);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new EventLogAdapter(this);
        recyclerView.setAdapter(adapter);

        if (tripId > 0) {
            loadEventsFromDb(tripId);
        } else {
            loadDemonstrationEvents();
        }
    }

    private void loadEventsFromDb(long id) {
        new Thread(() -> {
            DriveSenseDatabase db = DriveSenseDatabase.getInstance(this);
            List<EventEntity> events = db.eventDao().getEventsForTrip(id);
            runOnUiThread(() -> adapter.setEvents(events));
        }).start();
    }

    public void addLiveEvent(EventEntity event) {
        runOnUiThread(() -> adapter.addEvent(event));
    }

    private void loadDemonstrationEvents() {
        List<EventEntity> events = new ArrayList<>();
        String[] types = {"HARD_BRAKING", "RAPID_ACCELERATION", "SHARP_TURN",
                "SUDDEN_STOP", "EXCESSIVE_VIBRATION", "OVER_SPEEDING"};
        String[] descriptions = {
                "Hard braking detected at 68 km/h",
                "Rapid acceleration detected at 54 km/h",
                "Sharp turn detected at 46 km/h",
                "Sudden stop detected at 32 km/h",
                "Excessive road vibration detected",
                "Speed limit exceeded at 106 km/h"};
        float[] speeds = {68f, 54f, 46f, 32f, 38f, 106f};
        long rideStart = getSharedPreferences("drivesense_demo", MODE_PRIVATE)
                .getLong("ride_start", System.currentTimeMillis());
        long duration = System.currentTimeMillis() - rideStart;
        int eventCount = duration < 20_000 ? 1
                : duration < 60_000 ? 2
                : duration < 5 * 60_000 ? 4 : 6;
        for (int i = 0; i < eventCount; i++) {
            EventEntity event = new EventEntity();
            event.eventType = types[i];
            event.timestamp = rideStart + (duration * (i + 1) / (eventCount + 1));
            event.latitude = 28.6139 + i * 0.001;
            event.longitude = 77.2090 + i * 0.0015;
            event.speedKmh = speeds[i];
            event.severity = 1.2f;
            event.sensorMagnitude = 12f;
            event.description = descriptions[i];
            events.add(event);
        }
        adapter.setEvents(events);
    }
}
