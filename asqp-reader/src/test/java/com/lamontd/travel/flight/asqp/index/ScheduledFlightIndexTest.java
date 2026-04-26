package com.lamontd.travel.flight.asqp.index;

import com.lamontd.travel.flight.asqp.model.ASQPFlightRecord;
import com.lamontd.travel.flight.model.ScheduledFlight;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the scheduled flight index functionality in FlightDataIndex
 */
class ScheduledFlightIndexTest {

    @Test
    void testScheduledFlightsIndexBuilding() {
        // Create sample records for WN 3310 ONT->HOU
        List<ASQPFlightRecord> records = List.of(
            createRecord("WN", "3310", "ONT", "HOU", LocalDate.of(2025, 1, 18), LocalTime.of(7, 55), LocalTime.of(12, 50)),
            createRecord("WN", "3310", "ONT", "HOU", LocalDate.of(2025, 1, 19), LocalTime.of(11, 55), LocalTime.of(16, 50)),
            createRecord("WN", "3310", "ONT", "HOU", LocalDate.of(2025, 1, 27), LocalTime.of(11, 55), LocalTime.of(16, 50))
        );

        FlightDataIndex index = new FlightDataIndex(records);

        // Verify index was built
        assertNotNull(index.scheduledFlightsByRoute);
        assertFalse(index.scheduledFlightsByRoute.isEmpty());
    }

    @Test
    void testGetScheduledFlightsByRoute() {
        List<ASQPFlightRecord> records = List.of(
            createRecord("WN", "3310", "ONT", "HOU", LocalDate.of(2025, 1, 18), LocalTime.of(7, 55), LocalTime.of(12, 50)),
            createRecord("WN", "3310", "ONT", "HOU", LocalDate.of(2025, 1, 19), LocalTime.of(11, 55), LocalTime.of(16, 50)),
            createRecord("WN", "4283", "DAL", "MCO", LocalDate.of(2025, 1, 19), LocalTime.of(11, 5), LocalTime.of(14, 35))
        );

        FlightDataIndex index = new FlightDataIndex(records);

        // Get schedules for ONT->HOU route
        List<ScheduledFlight> ontToHou = index.getScheduledFlightsByRoute("ONT", "HOU");
        assertEquals(1, ontToHou.size());

        ScheduledFlight schedule = ontToHou.get(0);
        assertEquals("WN", schedule.getCarrierCode());
        assertEquals("3310", schedule.getFlightNumber());
        assertEquals("ONT", schedule.getOriginAirport());
        assertEquals("HOU", schedule.getDestinationAirport());

        // Get schedules for DAL->MCO route
        List<ScheduledFlight> dalToMco = index.getScheduledFlightsByRoute("DAL", "MCO");
        assertEquals(1, dalToMco.size());
        assertEquals("4283", dalToMco.get(0).getFlightNumber());

        // Non-existent route
        List<ScheduledFlight> noRoute = index.getScheduledFlightsByRoute("XXX", "YYY");
        assertTrue(noRoute.isEmpty());
    }

    @Test
    void testScheduledFlightInferredFromMultipleRecords() {
        // Multiple records with slightly different times - should infer most common
        List<ASQPFlightRecord> records = List.of(
            createRecord("WN", "3310", "ONT", "HOU", LocalDate.of(2025, 1, 18), LocalTime.of(11, 55), LocalTime.of(16, 50)),
            createRecord("WN", "3310", "ONT", "HOU", LocalDate.of(2025, 1, 19), LocalTime.of(11, 55), LocalTime.of(16, 50)),
            createRecord("WN", "3310", "ONT", "HOU", LocalDate.of(2025, 1, 27), LocalTime.of(11, 55), LocalTime.of(16, 50)),
            createRecord("WN", "3310", "ONT", "HOU", LocalDate.of(2025, 1, 28), LocalTime.of(7, 55), LocalTime.of(12, 50)) // Different time
        );

        FlightDataIndex index = new FlightDataIndex(records);
        List<ScheduledFlight> schedules = index.getScheduledFlightsByRoute("ONT", "HOU");

        assertEquals(1, schedules.size());
        ScheduledFlight schedule = schedules.get(0);

        // Should pick the most common time (11:55)
        assertEquals(LocalTime.of(11, 55), schedule.getScheduledDepartureTime());
        assertEquals(LocalTime.of(16, 50), schedule.getScheduledArrivalTime());

        // Date range
        assertEquals(LocalDate.of(2025, 1, 18), schedule.getEffectiveFrom().get());
        assertEquals(LocalDate.of(2025, 1, 28), schedule.getEffectiveUntil().get());
    }

    @Test
    void testDaysOfOperationInferred() {
        // Records on specific days of week
        List<ASQPFlightRecord> records = List.of(
            createRecord("WN", "100", "LAX", "SFO", LocalDate.of(2025, 1, 6), LocalTime.of(10, 0), LocalTime.of(11, 30)),  // Monday
            createRecord("WN", "100", "LAX", "SFO", LocalDate.of(2025, 1, 8), LocalTime.of(10, 0), LocalTime.of(11, 30)),  // Wednesday
            createRecord("WN", "100", "LAX", "SFO", LocalDate.of(2025, 1, 10), LocalTime.of(10, 0), LocalTime.of(11, 30))  // Friday
        );

        FlightDataIndex index = new FlightDataIndex(records);
        List<ScheduledFlight> schedules = index.getScheduledFlightsByRoute("LAX", "SFO");

        assertEquals(1, schedules.size());
        ScheduledFlight schedule = schedules.get(0);

        // Should have 3 days of operation (Mon, Wed, Fri)
        assertTrue(schedule.getDaysOfOperation().isPresent());
        Set<DayOfWeek> days = schedule.getDaysOfOperation().get();
        assertEquals(3, days.size());
        assertTrue(days.contains(DayOfWeek.MONDAY));
        assertTrue(days.contains(DayOfWeek.WEDNESDAY));
        assertTrue(days.contains(DayOfWeek.FRIDAY));
    }

    @Test
    void testMultipleFlightsOnSameRoute() {
        // Two different carriers on the same route
        List<ASQPFlightRecord> records = List.of(
            createRecord("AA", "100", "JFK", "LAX", LocalDate.of(2025, 1, 15), LocalTime.of(8, 0), LocalTime.of(11, 30)),
            createRecord("DL", "200", "JFK", "LAX", LocalDate.of(2025, 1, 15), LocalTime.of(10, 0), LocalTime.of(13, 30)),
            createRecord("UA", "300", "JFK", "LAX", LocalDate.of(2025, 1, 15), LocalTime.of(14, 0), LocalTime.of(17, 30))
        );

        FlightDataIndex index = new FlightDataIndex(records);
        List<ScheduledFlight> schedules = index.getScheduledFlightsByRoute("JFK", "LAX");

        // Should have 3 different scheduled flights
        assertEquals(3, schedules.size());

        // Verify each carrier has their schedule
        assertTrue(schedules.stream().anyMatch(s -> s.getCarrierCode().equals("AA") && s.getFlightNumber().equals("100")));
        assertTrue(schedules.stream().anyMatch(s -> s.getCarrierCode().equals("DL") && s.getFlightNumber().equals("200")));
        assertTrue(schedules.stream().anyMatch(s -> s.getCarrierCode().equals("UA") && s.getFlightNumber().equals("300")));
    }

    @Test
    void testSameFlightNumberDifferentRoutes() {
        // WN 3310 operates multiple routes (multi-leg scenario)
        List<ASQPFlightRecord> records = List.of(
            createRecord("WN", "3310", "ONT", "HOU", LocalDate.of(2025, 1, 18), LocalTime.of(11, 55), LocalTime.of(16, 50)),
            createRecord("WN", "3310", "PHX", "BNA", LocalDate.of(2025, 1, 3), LocalTime.of(17, 55), LocalTime.of(22, 5))
        );

        FlightDataIndex index = new FlightDataIndex(records);

        // Should have separate schedules for each route
        List<ScheduledFlight> ontToHou = index.getScheduledFlightsByRoute("ONT", "HOU");
        List<ScheduledFlight> phxToBna = index.getScheduledFlightsByRoute("PHX", "BNA");

        assertEquals(1, ontToHou.size());
        assertEquals(1, phxToBna.size());

        // Both should be WN 3310 but different routes
        assertEquals("3310", ontToHou.get(0).getFlightNumber());
        assertEquals("3310", phxToBna.get(0).getFlightNumber());
        assertNotEquals(ontToHou.get(0).getOriginAirport(), phxToBna.get(0).getOriginAirport());
    }

    private ASQPFlightRecord createRecord(String carrier, String flightNum, String origin, String dest,
                                         LocalDate date, LocalTime depTime, LocalTime arrTime) {
        return ASQPFlightRecord.builder()
                .carrierCode(carrier)
                .flightNumber(flightNum)
                .origin(origin)
                .destination(dest)
                .departureDate(date)
                .scheduledCrsDeparture(depTime)
                .scheduledCrsArrival(arrTime)
                .scheduledOagDeparture(depTime)
                .scheduledArrival(arrTime)
                .gateDeparture(depTime.plusMinutes(5))
                .gateArrival(arrTime.minusMinutes(5))
                .tailNumber("N12345")
                .build();
    }
}
