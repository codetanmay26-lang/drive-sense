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

import java.util.List;

/**
 * MapActivity — displays the trip route on Google Maps with event markers.
 */
public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private long tripId = -1;
    private List<LatLng> routePoints;
    private List<EventEntity> events;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        BackNavigation.attach(this);

        tripId = getIntent().getLongExtra("trip_id", -1);

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

        if (tripId > 0) {
            new Thread(() -> {
                DriveSenseDatabase db = DriveSenseDatabase.getInstance(this);
                TripEntity trip = db.tripDao().getTripById(tripId);
                events = db.eventDao().getEventsForTrip(tripId);
                if (trip != null && trip.routeJson != null) {
                    routePoints = RouteTracker.decodeRouteJson(trip.routeJson);
                }
                runOnUiThread(this::drawRoute);
            }).start();
        }
    }

    private void drawRoute() {
        if (mMap == null) return;

        if (routePoints != null && routePoints.size() > 1) {
            // Draw route polyline in blue
            PolylineOptions polyline = new PolylineOptions()
                    .addAll(routePoints)
                    .color(0xFF1565C0)
                    .width(8f);
            mMap.addPolyline(polyline);

            // Start marker (green)
            mMap.addMarker(new MarkerOptions()
                    .position(routePoints.get(0))
                    .title("Start")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));

            // End marker (red)
            mMap.addMarker(new MarkerOptions()
                    .position(routePoints.get(routePoints.size() - 1))
                    .title("End")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));

            // Fit camera to route
            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            for (LatLng p : routePoints) builder.include(p);
            mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 80));
        }

        // Draw event markers
        if (events != null) {
            for (EventEntity e : events) {
                LatLng pos = new LatLng(e.latitude, e.longitude);
                float hue  = getMarkerHue(e.eventType);
                mMap.addMarker(new MarkerOptions()
                        .position(pos)
                        .title(e.eventType.replace("_", " "))
                        .snippet(e.description)
                        .icon(BitmapDescriptorFactory.defaultMarker(hue)));
            }
        }
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
