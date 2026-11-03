package hu.treadmillprogrammer;

import java.util.Locale;

final class SessionStats {
    final long durationMs;
    final double distanceKm, averageSpeedKmh, calories;
    final int maxSpeedHundredths, speedChanges;

    SessionStats(long durationMs, double distanceKm, double averageSpeedKmh, int maxSpeedHundredths, double calories, int speedChanges) {
        this.durationMs = durationMs;
        this.distanceKm = distanceKm;
        this.averageSpeedKmh = averageSpeedKmh;
        this.maxSpeedHundredths = maxSpeedHundredths;
        this.calories = calories;
        this.speedChanges = speedChanges;
    }
    String durationLabel() {
        long seconds = durationMs / 1000;
        return String.format(Locale.ROOT, "%d:%02d:%02d", seconds / 3600, (seconds / 60) % 60, seconds % 60);
    }
    String paceLabel() {
        if (distanceKm <= 0) return "—";
        long secondsPerKm = Math.round(durationMs / 1000.0 / distanceKm);
        return String.format(Locale.ROOT, "%d:%02d perc/km", secondsPerKm / 60, secondsPerKm % 60);
    }
}
