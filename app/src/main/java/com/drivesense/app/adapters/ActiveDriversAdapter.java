package com.drivesense.app.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.drivesense.app.R;
import com.drivesense.app.activities.MapActivity;
import com.drivesense.app.activities.TripDashboardActivity;
import com.drivesense.app.models.ActiveDriverModel;

import java.util.ArrayList;
import java.util.List;

public class ActiveDriversAdapter extends RecyclerView.Adapter<ActiveDriversAdapter.ViewHolder> {

    private final List<ActiveDriverModel> drivers = new ArrayList<>();

    public void setDrivers(List<ActiveDriverModel> list) {
        drivers.clear();
        if (list != null) drivers.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_active_driver, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ActiveDriverModel driver = drivers.get(position);
        Context ctx = holder.itemView.getContext();

        holder.initial.setText(driver.driverName.substring(0, 1).toUpperCase());
        holder.name.setText(driver.driverName);
        holder.vehicle.setText("Vehicle: " + driver.vehicleNo);
        holder.status.setText(driver.status);

        if (driver.status.contains("RISK") || driver.status.contains("POOR")) {
            holder.status.setTextColor(ContextCompat.getColor(ctx, R.color.colorSeverityHigh));
        } else if (driver.status.contains("Warning")) {
            holder.status.setTextColor(ContextCompat.getColor(ctx, R.color.colorSeverityMedium));
        } else {
            holder.status.setTextColor(ContextCompat.getColor(ctx, R.color.colorScoreExcellent));
        }

        holder.speed.setText(driver.currentSpeed);
        holder.speedLimit.setText("Limit: " + driver.speedLimit);
        holder.score.setText(driver.safetyScore + " / 100");

        int scoreColor;
        if (driver.safetyScore >= 90) {
            scoreColor = R.color.colorScoreExcellent;
        } else if (driver.safetyScore >= 75) {
            scoreColor = R.color.colorScoreGood;
        } else if (driver.safetyScore >= 50) {
            scoreColor = R.color.colorScoreAverage;
        } else {
            scoreColor = R.color.colorScorePoor;
        }
        holder.score.setTextColor(ContextCompat.getColor(ctx, scoreColor));

        holder.distance.setText(driver.distanceKm + "  |  " + driver.duration);
        holder.events.setText(driver.eventsText);
        if (driver.safetyScore < 50) {
            holder.events.setTextColor(ContextCompat.getColor(ctx, R.color.colorSeverityHigh));
        } else {
            holder.events.setTextColor(ContextCompat.getColor(ctx, R.color.colorSeverityMedium));
        }

        holder.road.setText("Road: " + driver.roadCondition);

        holder.btnTelemetry.setOnClickListener(v -> {
            Intent intent = new Intent(ctx, TripDashboardActivity.class);
            intent.putExtra("driver_name", driver.driverName);
            intent.putExtra("vehicle_no", driver.vehicleNo);
            intent.putExtra("driver_score", driver.safetyScore);
            intent.putExtra("driver_speed", driver.currentSpeed);
            intent.putExtra("driver_speed_limit", driver.speedLimit);
            intent.putExtra("driver_distance", driver.distanceKm);
            intent.putExtra("driver_duration", driver.duration);
            intent.putExtra("driver_status", driver.status);
            intent.putExtra("hard_braking_count", driver.hardBrakingCount);
            intent.putExtra("rapid_accel_count", driver.rapidAccelCount);
            intent.putExtra("sharp_turn_count", driver.sharpTurnCount);
            intent.putExtra("sudden_stop_count", driver.suddenStopCount);
            intent.putExtra("vibration_count", driver.vibrationCount);
            intent.putExtra("overspeed_count", driver.overspeedCount);
            intent.putExtra("road_condition", driver.roadCondition);
            intent.putExtra("trip_id", driver.tripId);
            ctx.startActivity(intent);
        });

        holder.btnMap.setOnClickListener(v -> {
            Intent intent = new Intent(ctx, MapActivity.class);
            intent.putExtra("driver_name", driver.driverName);
            intent.putExtra("driver_score", driver.safetyScore);
            intent.putExtra("driver_distance", driver.distanceKm);
            intent.putExtra("driver_duration", driver.duration);
            intent.putExtra("trip_id", driver.tripId);
            ctx.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return drivers.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView initial, name, vehicle, status, speed, speedLimit, score, distance, events, road;
        final Button btnTelemetry, btnMap;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            initial = itemView.findViewById(R.id.tv_driver_initial);
            name = itemView.findViewById(R.id.tv_driver_name);
            vehicle = itemView.findViewById(R.id.tv_vehicle_no);
            status = itemView.findViewById(R.id.tv_driver_status);
            speed = itemView.findViewById(R.id.tv_driver_speed);
            speedLimit = itemView.findViewById(R.id.tv_driver_speed_limit);
            score = itemView.findViewById(R.id.tv_driver_score);
            distance = itemView.findViewById(R.id.tv_driver_distance);
            events = itemView.findViewById(R.id.tv_driver_events);
            road = itemView.findViewById(R.id.tv_driver_road);

            btnTelemetry = itemView.findViewById(R.id.btn_view_telemetry);
            btnMap = itemView.findViewById(R.id.btn_view_driver_map);
        }
    }
}
