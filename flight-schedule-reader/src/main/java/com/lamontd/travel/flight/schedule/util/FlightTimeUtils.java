package com.lamontd.travel.flight.schedule.util;

import java.time.Duration;
import java.time.LocalTime;

/**
 * Utility methods for flight time calculations and formatting.
 */
public class FlightTimeUtils {

    /**
     * Calculates flight duration from departure and arrival times.
     * Handles red-eye flights (arrival next day).
     *
     * @param departure Scheduled departure time
     * @param arrival Scheduled arrival time
     * @return Duration object representing flight time
     */
    public static Duration calculateFlightDuration(LocalTime departure, LocalTime arrival) {
        if (arrival.isBefore(departure)) {
            // Red-eye flight - arrival is next day
            return Duration.between(departure, arrival).plusHours(24);
        } else {
            return Duration.between(departure, arrival);
        }
    }

    /**
     * Formats a duration in hours and minutes (e.g., "2h 30m").
     *
     * @param duration Duration to format
     * @return Formatted string
     */
    public static String formatDuration(Duration duration) {
        long hours = duration.toHours();
        long minutes = duration.toMinutes() % 60;
        return String.format("%dh %02dm", hours, minutes);
    }

    /**
     * Calculates flight duration and returns formatted string.
     *
     * @param departure Scheduled departure time
     * @param arrival Scheduled arrival time
     * @return Formatted duration string (e.g., "2h 30m")
     */
    public static String calculateDuration(LocalTime departure, LocalTime arrival) {
        return formatDuration(calculateFlightDuration(departure, arrival));
    }

    /**
     * Calculates layover time between two flights in minutes.
     * Returns -1 for overnight layovers (not currently supported).
     *
     * @param arrivalTime Arrival time of first flight
     * @param departureTime Departure time of second flight
     * @return Layover time in minutes, or -1 if overnight
     */
    public static long calculateLayoverMinutes(LocalTime arrivalTime, LocalTime departureTime) {
        if (departureTime.isBefore(arrivalTime)) {
            return -1; // Overnight layover - not supported
        }
        return Duration.between(arrivalTime, departureTime).toMinutes();
    }

    /**
     * Formats layover time in hours and minutes (e.g., "1h 30m").
     *
     * @param minutes Layover time in minutes
     * @return Formatted string
     */
    public static String formatLayover(long minutes) {
        long hours = minutes / 60;
        long mins = minutes % 60;
        return String.format("%dh %02dm", hours, mins);
    }
}
