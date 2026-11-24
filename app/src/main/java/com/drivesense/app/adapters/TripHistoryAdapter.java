package com.drivesense.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
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
        holder.rating.setText(trip.rating == null ? "" : trip.rating);
        holder.date.setText(new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
                .format(new Date(trip.startTime)));
        holder.summary.setText(String.format(Locale.getDefault(), "%.1f km  |  %.1f km/h",
                trip.distanceKm, trip.avgSpeedKmh));
        holder.itemView.setOnClickListener(v -> listener.onTripClick(trip));
    }

    @Override public int getItemCount() { return trips.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView score, rating, date, summary;
        ViewHolder(@NonNull View itemView) {
            super(itemView);
            score = itemView.findViewById(R.id.item_score);
            rating = itemView.findViewById(R.id.item_rating);
            date = itemView.findViewById(R.id.item_date);
            summary = itemView.findViewById(R.id.item_summary);
        }
    }
}
