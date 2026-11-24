package com.drivesense.app.scoring;

import com.drivesense.app.models.DrivingEvent;

/**
 * DrivingScoreCalculator — implements the DriveSense weighted penalty scoring algorithm.
 *
 * Formula: Score = max(0, 100 - SUM(Event Penalty × Severity Multiplier))
 *
 * Event Penalty Table (from synopsis):
 *   Hard Braking       : -8 pts  × severity (1.0–2.0)
 *   Sharp Turn         : -6 pts  × severity (1.0–2.5)
 *   Rapid Acceleration : -5 pts  × severity (1.0–2.0)
 *   Sudden Stop/Impact : -10 pts × severity (1.0–3.0)
 *   Excessive Vibration: -3 pts per 30s period × severity (1.0–1.5)
 *   Over-Speeding      : -4 pts per 60s period × severity (1.0–2.0)
 *   Fatigue Warning Ignored: -2 pts × 1.0
 */
public class DrivingScoreCalculator {

    // Base penalties (from synopsis)
    private static final float PENALTY_HARD_BRAKING       = 8f;
    private static final float PENALTY_SHARP_TURN         = 6f;
    private static final float PENALTY_RAPID_ACCEL        = 5f;
    private static final float PENALTY_SUDDEN_STOP        = 10f;
    private static final float PENALTY_VIBRATION          = 3f;   // per 30-second period
    private static final float PENALTY_OVERSPEED          = 4f;   // per 60-second period
    private static final float PENALTY_FATIGUE_IGNORED    = 2f;

    private float totalPenalty = 0f;

    /**
     * Apply a penalty for a detected driving event.
     * @param event The detected driving event with its severity multiplier already set.
     */
    public void applyEventPenalty(DrivingEvent event) {
        float basePenalty = getBasePenalty(event.getType());
        float penalty = basePenalty * event.getSeverity();
        totalPenalty += penalty;
    }

    /**
     * Apply an over-speeding time penalty.
     * Called once per 60 seconds of continuous over-speeding.
     * @param severityMultiplier 1.0 for minor (< 10 km/h over), 2.0 for major (>= 10 km/h over)
     */
    public void applyOverspeedTimePenalty(float severityMultiplier) {
        totalPenalty += PENALTY_OVERSPEED * severityMultiplier;
    }

    /**
     * Apply a vibration time penalty.
     * Called once per 30 seconds of continuous excessive vibration.
     * @param severityMultiplier 1.0 for moderate, 1.5 for severe
     */
    public void applyVibrationTimePenalty(float severityMultiplier) {
        totalPenalty += PENALTY_VIBRATION * severityMultiplier;
    }

    /**
     * Get the current score (clamped to 0–100).
     */
    public int getCurrentScore() {
        int score = (int)(100f - totalPenalty);
        return Math.max(0, Math.min(100, score));
    }

    /**
     * Get the score rating label.
     */
    public static String getRating(int score) {
        if (score >= 90) return "Excellent";
        if (score >= 75) return "Good";
        if (score >= 50) return "Average";
        return "Poor";
    }

    /**
     * Reset for a new trip.
     */
    public void reset() {
        totalPenalty = 0f;
    }

    public float getTotalPenalty() {
        return totalPenalty;
    }

    /**
     * Get base penalty for each event type.
     */
    private float getBasePenalty(DrivingEvent.EventType type) {
        switch (type) {
            case HARD_BRAKING:          return PENALTY_HARD_BRAKING;
            case SHARP_TURN:            return PENALTY_SHARP_TURN;
            case RAPID_ACCELERATION:    return PENALTY_RAPID_ACCEL;
            case SUDDEN_STOP:           return PENALTY_SUDDEN_STOP;
            case EXCESSIVE_VIBRATION:   return PENALTY_VIBRATION;
            case OVER_SPEEDING:         return PENALTY_OVERSPEED;
            case FATIGUE_WARNING_IGNORED: return PENALTY_FATIGUE_IGNORED;
            default:                    return 0f;
        }
    }
}
