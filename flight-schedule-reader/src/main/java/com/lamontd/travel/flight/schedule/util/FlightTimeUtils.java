package com.lamontd.travel.flight.schedule.util;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;

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

    /**
     * Calculates flight duration considering timezone differences between airports.
     * This provides accurate flight duration by converting local times to UTC before calculation.
     *
     * Note: Uses static timezone offsets. Does not account for daylight saving time changes.
     * For DST-aware calculations, consider using ZoneId with the airport's tzDatabase field.
     *
     * @param departure Local departure time at origin
     * @param departureTimezoneOffset Origin airport timezone offset from UTC in hours (e.g., -5.0 for EST)
     * @param arrival Local arrival time at destination
     * @param arrivalTimezoneOffset Destination airport timezone offset from UTC in hours (e.g., -8.0 for PST)
     * @param operatingDate Date of flight operation
     * @return Duration object representing actual flight time
     */
    public static Duration calculateFlightDuration(LocalTime departure, double departureTimezoneOffset,
                                                   LocalTime arrival, double arrivalTimezoneOffset,
                                                   LocalDate operatingDate) {
        // Convert timezone offsets to ZoneOffset (hours to seconds)
        ZoneOffset departureOffset = ZoneOffset.ofTotalSeconds((int) (departureTimezoneOffset * 3600));
        ZoneOffset arrivalOffset = ZoneOffset.ofTotalSeconds((int) (arrivalTimezoneOffset * 3600));

        // Create LocalDateTime for departure
        LocalDateTime departureDateTime = LocalDateTime.of(operatingDate, departure);

        // Assume arrival is same day initially
        LocalDateTime arrivalDateTime = LocalDateTime.of(operatingDate, arrival);

        // Convert to UTC instants
        var departureInstant = departureDateTime.toInstant(departureOffset);
        var arrivalInstant = arrivalDateTime.toInstant(arrivalOffset);

        // If arrival is before departure in UTC, it must be next day
        if (arrivalInstant.isBefore(departureInstant)) {
            arrivalDateTime = arrivalDateTime.plusDays(1);
            arrivalInstant = arrivalDateTime.toInstant(arrivalOffset);
        }

        return Duration.between(departureInstant, arrivalInstant);
    }

    /**
     * Calculates flight duration with timezone awareness and returns formatted string.
     *
     * @param departure Local departure time at origin
     * @param departureTimezoneOffset Origin airport timezone offset from UTC in hours
     * @param arrival Local arrival time at destination
     * @param arrivalTimezoneOffset Destination airport timezone offset from UTC in hours
     * @param operatingDate Date of flight operation
     * @return Formatted duration string (e.g., "6h 15m")
     */
    public static String calculateDuration(LocalTime departure, double departureTimezoneOffset,
                                          LocalTime arrival, double arrivalTimezoneOffset,
                                          LocalDate operatingDate) {
        return formatDuration(calculateFlightDuration(departure, departureTimezoneOffset,
                                                     arrival, arrivalTimezoneOffset, operatingDate));
    }

    /**
     * Calculates layover time between two flights considering timezone.
     * For layovers at the same airport, timezone should be the same for both times.
     *
     * @param arrivalTime Arrival time of first flight
     * @param arrivalTimezoneOffset Timezone offset at layover airport
     * @param departureTime Departure time of second flight
     * @param departureTimezoneOffset Timezone offset at layover airport (should match arrival)
     * @return Layover time in minutes, or -1 if overnight
     */
    public static long calculateLayoverMinutes(LocalTime arrivalTime, double arrivalTimezoneOffset,
                                               LocalTime departureTime, double departureTimezoneOffset) {
        // For same airport, timezones should be equal, so this simplifies to time difference
        if (departureTime.isBefore(arrivalTime)) {
            return -1; // Overnight layover - not supported
        }
        return Duration.between(arrivalTime, departureTime).toMinutes();
    }
}
