package com.drivesense.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.drivesense.app.R;
import com.drivesense.app.database.entities.TripEntity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TripHistoryAdapter extends RecyclerView.Adapter<TripHistoryAdapter.ViewHolder> {
    public interface OnTripClickListener { void onTripClick(TripEntity trip); }

    private final OnTripClickListener listener;
    private final List<TripEntity> trips = new ArrayList<>();

    public TripHistoryAdapter(OnTripClickListener listener) { this.listener = listener; }

    public void setTrips(List<TripEntity> values) {
        trips.clear();
        if (values != null) trips.addAll(values);
        notifyDataSetChanged();
    }

    @NonNull
    @Override public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_trip, parent, false));
    }

    @Override public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TripEntity trip = trips.get(position);
        holder.score.setText(String.valueOf(trip.finalScore));

        // Colorize score based on value
        int scoreColor;
        if (trip.finalScore >= 90) scoreColor = R.color.colorScoreExcellent;
        else if (trip.finalScore >= 75) scoreColor = R.color.colorScoreGood;
        else if (trip.finalScore >= 60) scoreColor = R.color.colorScoreAverage;
        else scoreColor = R.color.colorScorePoor;
        holder.score.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), scoreColor));

        holder.rating.setText(trip.rating == null ? "RATED" : trip.rating);
        holder.date.setText(new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
                .format(new Date(trip.startTime)));

        holder.distance.setText(String.format(Locale.getDefault(), "%.1f km", trip.distanceKm));

        long secs = trip.durationMs / 1000;
        if (secs >= 3600) {
            holder.duration.setText(String.format(Locale.getDefault(), "%dh %dm", secs/3600, (secs%3600)/60));
        } else {
            holder.duration.setText(String.format(Locale.getDefault(), "%dm %ds", secs/60, secs%60));
        }

        holder.avgSpeed.setText(String.format(Locale.getDefault(), "%.1f km/h", trip.avgSpeedKmh));

        int totalEvents = trip.hardBrakingCount + trip.rapidAccelCount + trip.sharpTurnCount
                + trip.suddenStopCount + trip.vibrationCount + trip.overspeedCount;
        if (totalEvents == 0) {
            holder.events.setText("Smooth Driving (0 Events)");
            holder.events.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.colorScoreExcellent));
        } else {
            holder.events.setText(totalEvents + " Safety Event" + (totalEvents > 1 ? "s" : ""));
            holder.events.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.colorSeverityMedium));
        }

        if (trip.roadSmoothPct >= 60) {
            holder.road.setText("Road: Smooth");
        } else if (trip.roadRoughPct >= 30) {
            holder.road.setText("Road: Rough");
        } else {
            holder.road.setText("Road: Moderate");
        }

        holder.itemView.setOnClickListener(v -> listener.onTripClick(trip));
    }

    @Override public int getItemCount() { return trips.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView score, rating, date, distance, duration, avgSpeed, events, road;
        ViewHolder(@NonNull View itemView) {
            super(itemView);
            score = itemView.findViewById(R.id.item_score);
            rating = itemView.findViewById(R.id.item_rating);
            date = itemView.findViewById(R.id.item_date);
            distance = itemView.findViewById(R.id.item_distance);
            duration = itemView.findViewById(R.id.item_duration);
            avgSpeed = itemView.findViewById(R.id.item_avg_speed);
            events = itemView.findViewById(R.id.item_events_badge);
            road = itemView.findViewById(R.id.item_road_condition);
        }
    }
}
