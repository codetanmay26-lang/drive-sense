package com.drivesense.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.drivesense.app.R;
import com.drivesense.app.database.entities.EventEntity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class EventLogAdapter extends RecyclerView.Adapter<EventLogAdapter.ViewHolder> {
    private final android.content.Context context;
    private final List<EventEntity> events = new ArrayList<>();

    public EventLogAdapter(android.content.Context context) { this.context = context; }

    public void setEvents(List<EventEntity> values) {
        events.clear();
        if (values != null) events.addAll(values);
        notifyDataSetChanged();
    }

    public void addEvent(EventEntity event) {
        if (event == null) return;
        events.add(event);
        notifyItemInserted(events.size() - 1);
    }

    @NonNull
    @Override public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_event, parent, false));
    }

    @Override public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        EventEntity event = events.get(position);
        holder.type.setText(event.eventType == null ? "Driving event"
                : event.eventType.replace("_", " "));
        holder.description.setText(event.description == null ? "" : event.description);
        holder.time.setText(new SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                .format(new Date(event.timestamp)));
        holder.speed.setText(String.format(Locale.getDefault(), "%.1f km/h", event.speedKmh));
    }

    @Override public int getItemCount() { return events.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView type, description, time, speed;
        ViewHolder(@NonNull View itemView) {
            super(itemView);
            type = itemView.findViewById(R.id.item_event_type);
            description = itemView.findViewById(R.id.item_event_description);
            time = itemView.findViewById(R.id.item_event_time);
            speed = itemView.findViewById(R.id.item_event_speed);
        }
    }
}
