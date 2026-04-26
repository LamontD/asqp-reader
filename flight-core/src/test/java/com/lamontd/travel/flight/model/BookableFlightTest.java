package com.lamontd.travel.flight.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

class BookableFlightTest {

    @Test
    void testBuilder() {
        BookableFlight flight = BookableFlight.builder()
                .carrierCode("DL")
                .flightNumber("5030")
                .originAirport("LGA")
                .destinationAirport("CVG")
                .operatingDate(LocalDate.of(2025, 1, 15))
                .scheduledDepartureTime(LocalTime.of(13, 45))
                .scheduledArrivalTime(LocalTime.of(16, 5))
                .build();

        assertEquals("DL", flight.getCarrierCode());
        assertEquals("5030", flight.getFlightNumber());
        assertEquals("LGA", flight.getOriginAirport());
        assertEquals("CVG", flight.getDestinationAirport());
        assertEquals(LocalDate.of(2025, 1, 15), flight.getOperatingDate());
        assertEquals(LocalTime.of(13, 45), flight.getScheduledDepartureTime());
        assertEquals(LocalTime.of(16, 5), flight.getScheduledArrivalTime());
    }

    @Test
    void testRouteKey() {
        BookableFlight flight = BookableFlight.builder()
                .carrierCode("AA")
                .flightNumber("100")
                .originAirport("ORD")
                .destinationAirport("DFW")
                .operatingDate(LocalDate.of(2025, 6, 1))
                .scheduledDepartureTime(LocalTime.of(10, 0))
                .scheduledArrivalTime(LocalTime.of(12, 30))
                .build();

        assertEquals("AA100-ORD-DFW", flight.getRouteKey());
    }

    @Test
    void testEqualsAndHashCode() {
        BookableFlight flight1 = BookableFlight.builder()
                .carrierCode("DL")
                .flightNumber("5030")
                .originAirport("LGA")
                .destinationAirport("CVG")
                .operatingDate(LocalDate.of(2025, 1, 15))
                .scheduledDepartureTime(LocalTime.of(13, 45))
                .scheduledArrivalTime(LocalTime.of(16, 5))
                .build();

        BookableFlight flight2 = BookableFlight.builder()
                .carrierCode("DL")
                .flightNumber("5030")
                .originAirport("LGA")
                .destinationAirport("CVG")
                .operatingDate(LocalDate.of(2025, 1, 15))
                .scheduledDepartureTime(LocalTime.of(13, 45))
                .scheduledArrivalTime(LocalTime.of(16, 5))
                .build();

        assertEquals(flight1, flight2);
        assertEquals(flight1.hashCode(), flight2.hashCode());
    }

    @Test
    void testNotEquals_DifferentDate() {
        BookableFlight flight1 = BookableFlight.builder()
                .carrierCode("DL")
                .flightNumber("5030")
                .originAirport("LGA")
                .destinationAirport("CVG")
                .operatingDate(LocalDate.of(2025, 1, 15))
                .scheduledDepartureTime(LocalTime.of(13, 45))
                .scheduledArrivalTime(LocalTime.of(16, 5))
                .build();

        BookableFlight flight2 = BookableFlight.builder()
                .carrierCode("DL")
                .flightNumber("5030")
                .originAirport("LGA")
                .destinationAirport("CVG")
                .operatingDate(LocalDate.of(2025, 1, 16)) // Different date
                .scheduledDepartureTime(LocalTime.of(13, 45))
                .scheduledArrivalTime(LocalTime.of(16, 5))
                .build();

        assertNotEquals(flight1, flight2);
    }

    @Test
    void testBuilderRequiredFields() {
        assertThrows(NullPointerException.class, () ->
                BookableFlight.builder()
                        // Missing carrierCode
                        .flightNumber("5030")
                        .originAirport("LGA")
                        .destinationAirport("CVG")
                        .operatingDate(LocalDate.of(2025, 1, 15))
                        .scheduledDepartureTime(LocalTime.of(13, 45))
                        .scheduledArrivalTime(LocalTime.of(16, 5))
                        .build()
        );

        assertThrows(NullPointerException.class, () ->
                BookableFlight.builder()
                        .carrierCode("DL")
                        // Missing flightNumber
                        .originAirport("LGA")
                        .destinationAirport("CVG")
                        .operatingDate(LocalDate.of(2025, 1, 15))
                        .scheduledDepartureTime(LocalTime.of(13, 45))
                        .scheduledArrivalTime(LocalTime.of(16, 5))
                        .build()
        );

        assertThrows(NullPointerException.class, () ->
                BookableFlight.builder()
                        .carrierCode("DL")
                        .flightNumber("5030")
                        .originAirport("LGA")
                        .destinationAirport("CVG")
                        // Missing operatingDate
                        .scheduledDepartureTime(LocalTime.of(13, 45))
                        .scheduledArrivalTime(LocalTime.of(16, 5))
                        .build()
        );
    }

    @Test
    void testToString() {
        BookableFlight flight = BookableFlight.builder()
                .carrierCode("DL")
                .flightNumber("5030")
                .originAirport("LGA")
                .destinationAirport("CVG")
                .operatingDate(LocalDate.of(2025, 1, 15))
                .scheduledDepartureTime(LocalTime.of(13, 45))
                .scheduledArrivalTime(LocalTime.of(16, 5))
                .build();

        String toString = flight.toString();
        assertTrue(toString.contains("DL"));
        assertTrue(toString.contains("5030"));
        assertTrue(toString.contains("LGA-CVG"));
        assertTrue(toString.contains("2025-01-15"));
    }
}
