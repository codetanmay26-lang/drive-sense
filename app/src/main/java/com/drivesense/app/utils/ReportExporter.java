package com.drivesense.app.utils;

import com.drivesense.app.database.entities.TripEntity;

import java.util.Locale;

public final class ReportExporter {
    private ReportExporter() {}

    public static String generateTextReport(TripEntity trip) {
        if (trip == null) return "";
        return String.format(Locale.getDefault(),
                "DriveSense Trip Report\n\nScore: %d (%s)\nDistance: %.1f km\n"
                        + "Duration: %s\nAverage speed: %.1f km/h\nMaximum speed: %.1f km/h\n\n"
                        + "Events\nHard braking: %d\nRapid acceleration: %d\nSharp turns: %d\n"
                        + "Sudden stops: %d\nVibration: %d\nOver-speeding: %d\n",
                trip.finalScore, trip.rating == null ? "" : trip.rating, trip.distanceKm,
                formatDuration(trip.durationMs), trip.avgSpeedKmh, trip.maxSpeedKmh,
                trip.hardBrakingCount, trip.rapidAccelCount, trip.sharpTurnCount,
                trip.suddenStopCount, trip.vibrationCount, trip.overspeedCount);
    }

    public static String generateCsvReport(TripEntity trip) {
        if (trip == null) return "";
        return "score,rating,duration_ms,distance_km,avg_speed_kmh,max_speed_kmh,"
                + "hard_braking,rapid_acceleration,sharp_turn,sudden_stop,vibration,overspeed\n"
                + String.format(Locale.US, "%d,%s,%d,%.3f,%.3f,%.3f,%d,%d,%d,%d,%d,%d\n",
                trip.finalScore, trip.rating == null ? "" : trip.rating, trip.durationMs,
                trip.distanceKm, trip.avgSpeedKmh, trip.maxSpeedKmh, trip.hardBrakingCount,
                trip.rapidAccelCount, trip.sharpTurnCount, trip.suddenStopCount,
                trip.vibrationCount, trip.overspeedCount);
    }

    private static String formatDuration(long durationMs) {
        long seconds = Math.max(0, durationMs / 1000);
        return String.format(Locale.getDefault(), "%02d:%02d:%02d",
                seconds / 3600, (seconds % 3600) / 60, seconds % 60);
    }
}
