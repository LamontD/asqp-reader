package com.lamontd.travel.flight.schedule.util;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

class FlightTimeUtilsTest {

    @Test
    void testCalculateFlightDuration_sameDayFlight() {
        LocalTime departure = LocalTime.of(10, 30);
        LocalTime arrival = LocalTime.of(13, 45);

        Duration duration = FlightTimeUtils.calculateFlightDuration(departure, arrival);

        assertEquals(3 * 60 + 15, duration.toMinutes());
    }

    @Test
    void testCalculateFlightDuration_redEyeFlight() {
        LocalTime departure = LocalTime.of(23, 30);
        LocalTime arrival = LocalTime.of(2, 15);

        Duration duration = FlightTimeUtils.calculateFlightDuration(departure, arrival);

        assertEquals(2 * 60 + 45, duration.toMinutes());
    }

    @Test
    void testCalculateFlightDuration_midnightCrossing() {
        LocalTime departure = LocalTime.of(22, 0);
        LocalTime arrival = LocalTime.of(1, 30);

        Duration duration = FlightTimeUtils.calculateFlightDuration(departure, arrival);

        assertEquals(3 * 60 + 30, duration.toMinutes());
    }

    @Test
    void testFormatDuration_hoursAndMinutes() {
        Duration duration = Duration.ofHours(2).plusMinutes(30);

        String formatted = FlightTimeUtils.formatDuration(duration);

        assertEquals("2h 30m", formatted);
    }

    @Test
    void testFormatDuration_hoursOnly() {
        Duration duration = Duration.ofHours(3);

        String formatted = FlightTimeUtils.formatDuration(duration);

        assertEquals("3h 00m", formatted);
    }

    @Test
    void testFormatDuration_minutesOnly() {
        Duration duration = Duration.ofMinutes(45);

        String formatted = FlightTimeUtils.formatDuration(duration);

        assertEquals("0h 45m", formatted);
    }

    @Test
    void testCalculateDuration_formattedString() {
        LocalTime departure = LocalTime.of(14, 20);
        LocalTime arrival = LocalTime.of(16, 35);

        String formatted = FlightTimeUtils.calculateDuration(departure, arrival);

        assertEquals("2h 15m", formatted);
    }

    @Test
    void testCalculateLayoverMinutes_validLayover() {
        LocalTime arrival = LocalTime.of(10, 30);
        LocalTime departure = LocalTime.of(12, 15);

        long layover = FlightTimeUtils.calculateLayoverMinutes(arrival, departure);

        assertEquals(105, layover); // 1h 45m = 105 minutes
    }

    @Test
    void testCalculateLayoverMinutes_shortLayover() {
        LocalTime arrival = LocalTime.of(11, 0);
        LocalTime departure = LocalTime.of(12, 0);

        long layover = FlightTimeUtils.calculateLayoverMinutes(arrival, departure);

        assertEquals(60, layover);
    }

    @Test
    void testCalculateLayoverMinutes_overnightReturnsNegative() {
        LocalTime arrival = LocalTime.of(23, 30);
        LocalTime departure = LocalTime.of(6, 0);

        long layover = FlightTimeUtils.calculateLayoverMinutes(arrival, departure);

        assertEquals(-1, layover); // Overnight not supported
    }

    @Test
    void testFormatLayover_hoursAndMinutes() {
        String formatted = FlightTimeUtils.formatLayover(90);

        assertEquals("1h 30m", formatted);
    }

    @Test
    void testFormatLayover_hoursOnly() {
        String formatted = FlightTimeUtils.formatLayover(120);

        assertEquals("2h 00m", formatted);
    }

    @Test
    void testFormatLayover_minutesOnly() {
        String formatted = FlightTimeUtils.formatLayover(45);

        assertEquals("0h 45m", formatted);
    }

    @Test
    void testFormatLayover_longLayover() {
        String formatted = FlightTimeUtils.formatLayover(360);

        assertEquals("6h 00m", formatted);
    }

    // ========== Timezone-Aware Tests ==========

    @Test
    void testCalculateDurationWithTimezones_sameTimezone() {
        // Flight within same timezone should match non-timezone calculation
        LocalTime departure = LocalTime.of(10, 0);
        LocalTime arrival = LocalTime.of(13, 0);
        LocalDate date = LocalDate.of(2025, 6, 10);
        double timezone = -5.0; // EST

        Duration duration = FlightTimeUtils.calculateFlightDuration(
            departure, timezone, arrival, timezone, date);

        assertEquals(3 * 60, duration.toMinutes()); // 3 hours
    }

    @Test
    void testCalculateDurationWithTimezones_eastToWest() {
        // JFK (EST, UTC-5) to LAX (PST, UTC-8)
        // Depart 10:00 EST = 15:00 UTC
        // Arrive 13:00 PST = 21:00 UTC
        // Actual flight time: 6 hours
        LocalTime departure = LocalTime.of(10, 0);
        LocalTime arrival = LocalTime.of(13, 0);
        LocalDate date = LocalDate.of(2025, 6, 10);
        double estTimezone = -5.0;
        double pstTimezone = -8.0;

        Duration duration = FlightTimeUtils.calculateFlightDuration(
            departure, estTimezone, arrival, pstTimezone, date);

        assertEquals(6 * 60, duration.toMinutes()); // 6 hours actual flight time
    }

    @Test
    void testCalculateDurationWithTimezones_westToEast() {
        // LAX (PST, UTC-8) to JFK (EST, UTC-5)
        // Depart 10:00 PST = 18:00 UTC
        // Arrive 18:30 EST = 23:30 UTC
        // Actual flight time: 5.5 hours
        LocalTime departure = LocalTime.of(10, 0);
        LocalTime arrival = LocalTime.of(18, 30);
        LocalDate date = LocalDate.of(2025, 6, 10);
        double pstTimezone = -8.0;
        double estTimezone = -5.0;

        Duration duration = FlightTimeUtils.calculateFlightDuration(
            departure, pstTimezone, arrival, estTimezone, date);

        assertEquals(5 * 60 + 30, duration.toMinutes()); // 5.5 hours
    }

    @Test
    void testCalculateDurationWithTimezones_international() {
        // JFK (EST, UTC-5) to LHR (GMT, UTC+0)
        // Depart 20:00 EST = 01:00 UTC next day
        // Arrive 08:00 GMT = 08:00 UTC next day
        // Actual flight time: 7 hours
        LocalTime departure = LocalTime.of(20, 0);
        LocalTime arrival = LocalTime.of(8, 0);
        LocalDate date = LocalDate.of(2025, 6, 10);
        double estTimezone = -5.0;
        double gmtTimezone = 0.0;

        Duration duration = FlightTimeUtils.calculateFlightDuration(
            departure, estTimezone, arrival, gmtTimezone, date);

        assertEquals(7 * 60, duration.toMinutes()); // 7 hours
    }

    @Test
    void testCalculateDurationWithTimezones_redEye() {
        // Red-eye flight: LAX to NYC
        // Depart 23:00 PST = 07:00 UTC next day
        // Arrive 07:30 EST = 12:30 UTC next day
        // Actual flight time: 5.5 hours
        LocalTime departure = LocalTime.of(23, 0);
        LocalTime arrival = LocalTime.of(7, 30);
        LocalDate date = LocalDate.of(2025, 6, 10);
        double pstTimezone = -8.0;
        double estTimezone = -5.0;

        Duration duration = FlightTimeUtils.calculateFlightDuration(
            departure, pstTimezone, arrival, estTimezone, date);

        assertEquals(5 * 60 + 30, duration.toMinutes()); // 5.5 hours
    }

    @Test
    void testCalculateDurationFormatted_withTimezones() {
        // Test the formatted string version
        LocalTime departure = LocalTime.of(10, 0);
        LocalTime arrival = LocalTime.of(13, 0);
        LocalDate date = LocalDate.of(2025, 6, 10);
        double estTimezone = -5.0;
        double pstTimezone = -8.0;

        String formatted = FlightTimeUtils.calculateDuration(
            departure, estTimezone, arrival, pstTimezone, date);

        assertEquals("6h 00m", formatted);
    }

    @Test
    void testCalculateLayoverWithTimezones_sameAirport() {
        // Layover at same airport - timezone cancels out
        LocalTime arrival = LocalTime.of(10, 30);
        LocalTime departure = LocalTime.of(12, 15);
        double timezone = -6.0; // CST

        long layover = FlightTimeUtils.calculateLayoverMinutes(
            arrival, timezone, departure, timezone);

        assertEquals(105, layover); // 1h 45m
    }
}
