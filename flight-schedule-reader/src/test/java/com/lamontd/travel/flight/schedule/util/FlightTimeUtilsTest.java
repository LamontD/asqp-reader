package com.lamontd.travel.flight.schedule.util;

import org.junit.jupiter.api.Test;

import java.time.Duration;
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
}
