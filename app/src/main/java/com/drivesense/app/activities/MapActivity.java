package com.drivesense.app.activities;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.drivesense.app.R;
import com.drivesense.app.database.DriveSenseDatabase;
import com.drivesense.app.database.entities.EventEntity;
import com.drivesense.app.database.entities.TripEntity;
import com.drivesense.app.utils.RouteTracker;
import com.drivesense.app.utils.BackNavigation;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * MapActivity — displays the trip route on Google Maps with distinct Delhi-NCR corridors for each driver.
 */
public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private long tripId = -1;
    private List<LatLng> routePoints;
    private List<EventEntity> events;
    private TripEntity currentTrip;

    private TextView tvScore, tvDistance, tvDuration, tvEndLocation;
    private String activeStartName = "Trip Start";
    private String activeEndName = "Trip Destination";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        BackNavigation.attach(this);

        tripId = getIntent().getLongExtra("trip_id", -1);

        tvScore = findViewById(R.id.tv_map_score);
        tvDistance = findViewById(R.id.tv_map_distance);
        tvDuration = findViewById(R.id.tv_map_duration);
        tvEndLocation = findViewById(R.id.tv_map_end_location);

        SupportMapFragment mapFragment = (SupportMapFragment)
                getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(false);

        new Thread(() -> {
            DriveSenseDatabase db = DriveSenseDatabase.getInstance(this);
            if (tripId > 0) {
                currentTrip = db.tripDao().getTripById(tripId);
                events = db.eventDao().getEventsForTrip(tripId);
            }

            if (currentTrip == null) {
                List<TripEntity> trips = db.tripDao().getAllTrips();
                if (trips != null && !trips.isEmpty()) {
                    currentTrip = trips.get(0);
                    if (events == null) events = db.eventDao().getEventsForTrip(currentTrip.id);
                }
            }

            if (currentTrip != null && currentTrip.routeJson != null) {
                routePoints = RouteTracker.decodeRouteJson(currentTrip.routeJson);
            }

            String driverName = getIntent().getStringExtra("driver_name");
            if (driverName == null && currentTrip != null) {
                driverName = "Driver";
            } else if (driverName == null) {
                driverName = "Tanmay";
            }

            double distKm = 8.4;
            if (getIntent().hasExtra("driver_distance")) {
                String distStr = getIntent().getStringExtra("driver_distance");
                if (distStr != null) {
                    try { distKm = Double.parseDouble(distStr.replaceAll("[^0-9.]", "")); } catch (Exception ignored) {}
                }
            } else if (currentTrip != null && currentTrip.distanceKm > 0) {
                distKm = currentTrip.distanceKm;
            }

            // Always generate distinct Delhi-NCR route for active driver
            routePoints = getDelhiNcrRouteForDriver(driverName, distKm);

            runOnUiThread(this::drawRoute);
        }).start();
    }

    private void drawRoute() {
        if (mMap == null) return;

        if (getIntent().hasExtra("driver_name")) {
            String driverName = getIntent().getStringExtra("driver_name");
            int score = getIntent().getIntExtra("driver_score", 92);
            String dist = getIntent().getStringExtra("driver_distance");
            String dur = getIntent().getStringExtra("driver_duration");

            TextView tvTitle = findViewById(R.id.tv_map_title);
            if (tvTitle != null) tvTitle.setText("Live Route: " + driverName);
            if (tvScore != null) tvScore.setText("Score: " + score);
            if (dist != null && tvDistance != null) tvDistance.setText("Distance: " + dist);
            if (dur != null && tvDuration != null) tvDuration.setText("Duration: " + dur);
        } else if (currentTrip != null) {
            tvScore.setText("Score: " + currentTrip.finalScore);
            tvDistance.setText(String.format(Locale.getDefault(), "Distance: %.1f km", currentTrip.distanceKm));

            long secs = currentTrip.durationMs / 1000;
            if (secs >= 3600) {
                tvDuration.setText(String.format(Locale.getDefault(), "Duration: %dh %dm", secs / 3600, (secs % 3600) / 60));
            } else {
                tvDuration.setText(String.format(Locale.getDefault(), "Duration: %dm %ds", secs / 60, secs % 60));
            }
        }

        if (routePoints != null && routePoints.size() > 1) {
            mMap.clear();

            LatLng startPos = routePoints.get(0);
            LatLng endPos = routePoints.get(routePoints.size() - 1);

            tvEndLocation.setText("📍 Destination: " + activeEndName);

            // Polyline route in bold primary blue
            PolylineOptions polyline = new PolylineOptions()
                    .addAll(routePoints)
                    .color(0xFF1565C0)
                    .width(12f);
            mMap.addPolyline(polyline);

            // Green Start Marker
            mMap.addMarker(new MarkerOptions()
                    .position(startPos)
                    .title("Start: " + activeStartName)
                    .snippet("Trip Origin")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));

            // Red End Trip Marker
            mMap.addMarker(new MarkerOptions()
                    .position(endPos)
                    .title("End: " + activeEndName)
                    .snippet("Trip Destination")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));

            // Immediate camera move
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(endPos, 13f));

            // Safe camera zoom to fit full route bounds after map layout finishes
            mMap.setOnMapLoadedCallback(() -> {
                try {
                    LatLngBounds.Builder builder = new LatLngBounds.Builder();
                    for (LatLng p : routePoints) builder.include(p);
                    mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 100));
                } catch (Exception ignored) {
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(endPos, 13f));
                }
            });
        }

        // Draw event markers
        if (events != null) {
            for (EventEntity e : events) {
                LatLng pos = new LatLng(e.latitude, e.longitude);
                float hue = getMarkerHue(e.eventType);
                mMap.addMarker(new MarkerOptions()
                        .position(pos)
                        .title(e.eventType == null ? "Event" : e.eventType.replace("_", " "))
                        .snippet(e.description)
                        .icon(BitmapDescriptorFactory.defaultMarker(hue)));
            }
        }
    }

    private List<LatLng> getDelhiNcrRouteForDriver(String driverName, double distKm) {
        double startLat, startLon, endLat, endLon;
        String startName, endName;

        if ("Rohit".equalsIgnoreCase(driverName)) {
            // Cyber City to Sector 56 Gurgaon (12.1 km)
            startLat = 28.4950; startLon = 77.0890;
            endLat = 28.4230; endLon = 77.1050;
            startName = "DLF Cyber City, Gurgaon";
            endName = "Sector 56, Gurgaon";
        } else if ("Ayan".equalsIgnoreCase(driverName)) {
            // Noida Sector 18 to Sector 62 IT Hub (14.8 km)
            startLat = 28.5708; startLon = 77.3261;
            endLat = 28.6280; endLon = 77.3680;
            startName = "Noida Sector 18";
            endName = "Noida Sector 62 IT Hub";
        } else if ("Vikram".equalsIgnoreCase(driverName)) {
            // Indirapuram Ghaziabad to Anand Vihar ISBT (9.2 km)
            startLat = 28.6410; startLon = 77.3710;
            endLat = 28.6500; endLon = 77.3150;
            startName = "Indirapuram, Ghaziabad";
            endName = "Anand Vihar ISBT, Delhi";
        } else if ("Siddharth".equalsIgnoreCase(driverName)) {
            // Dwarka Expressway to IGI Airport T3 (18.5 km)
            startLat = 28.5520; startLon = 77.0580;
            endLat = 28.5562; endLon = 77.1000;
            startName = "Dwarka Sector 21";
            endName = "Terminal 3, IGI Airport";
        } else {
            // Tanmay: Connaught Place to Saket / Qutub Minar (8.4 km)
            startLat = 28.6315; startLon = 77.2167;
            endLat = 28.5244; endLon = 77.1855;
            startName = "Connaught Place, New Delhi";
            endName = "Saket / Qutub Minar, Delhi";
        }

        activeStartName = startName;
        activeEndName = endName;

        List<LatLng> points = new ArrayList<>();
        int steps = 28;
        for (int i = 0; i <= steps; i++) {
            double frac = (double) i / steps;
            double curveOffset = Math.sin(frac * Math.PI) * 0.005;
            double lat = startLat + (frac * (endLat - startLat)) + curveOffset;
            double lon = startLon + (frac * (endLon - startLon)) - (curveOffset * 0.3);
            points.add(new LatLng(lat, lon));
        }
        return points;
    }

    private float getMarkerHue(String eventType) {
        if (eventType == null) return BitmapDescriptorFactory.HUE_ORANGE;
        switch (eventType) {
            case "HARD_BRAKING":        return BitmapDescriptorFactory.HUE_RED;
            case "RAPID_ACCELERATION":  return BitmapDescriptorFactory.HUE_YELLOW;
            case "SHARP_TURN":          return BitmapDescriptorFactory.HUE_BLUE;
            case "SUDDEN_STOP":         return BitmapDescriptorFactory.HUE_ROSE;
            case "EXCESSIVE_VIBRATION": return BitmapDescriptorFactory.HUE_ORANGE;
            case "OVER_SPEEDING":       return BitmapDescriptorFactory.HUE_VIOLET;
            default:                    return BitmapDescriptorFactory.HUE_CYAN;
        }
    }
}
