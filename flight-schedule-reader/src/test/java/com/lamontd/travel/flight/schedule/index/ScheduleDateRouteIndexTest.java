package com.lamontd.travel.flight.schedule.index;

import com.lamontd.travel.flight.model.BookableFlight;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ScheduleDateRouteIndex adapter
 */
class ScheduleDateRouteIndexTest {

    private ScheduleFlightIndex scheduleIndex;
    private LocalDate june10;
    private LocalDate june11;

    @BeforeEach
    void setUp() {
        june10 = LocalDate.of(2025, 6, 10);
        june11 = LocalDate.of(2025, 6, 11);

        List<BookableFlight> flights = new ArrayList<>();

        // June 10 flights
        flights.add(BookableFlight.builder()
                .carrierCode("AA")
                .flightNumber("100")
                .originAirport("BWI")
                .destinationAirport("ORD")
                .operatingDate(june10)
                .scheduledDepartureTime(LocalTime.of(8, 0))
                .scheduledArrivalTime(LocalTime.of(9, 30))
                .build());

        flights.add(BookableFlight.builder()
                .carrierCode("UA")
                .flightNumber("200")
                .originAirport("ORD")
                .destinationAirport("DAL")
                .operatingDate(june10)
                .scheduledDepartureTime(LocalTime.of(13, 0))
                .scheduledArrivalTime(LocalTime.of(15, 30))
                .build());

        flights.add(BookableFlight.builder()
                .carrierCode("WN")
                .flightNumber("300")
                .originAirport("BWI")
                .destinationAirport("LAX")
                .operatingDate(june10)
                .scheduledDepartureTime(LocalTime.of(10, 0))
                .scheduledArrivalTime(LocalTime.of(13, 0))
                .build());

        // June 11 flights (different routes)
        flights.add(BookableFlight.builder()
                .carrierCode("DL")
                .flightNumber("400")
                .originAirport("ATL")
                .destinationAirport("LAX")
                .operatingDate(june11)
                .scheduledDepartureTime(LocalTime.of(11, 0))
                .scheduledArrivalTime(LocalTime.of(13, 30))
                .build());

        scheduleIndex = new ScheduleFlightIndex(flights);
    }

    @Test
    void testGetOriginAirports() {
        ScheduleDateRouteIndex index = new ScheduleDateRouteIndex(scheduleIndex, june10);

        Set<String> origins = index.getOriginAirports();

        assertEquals(2, origins.size(), "BWI and ORD are origins on June 10");
        assertTrue(origins.contains("BWI"));
        assertTrue(origins.contains("ORD"));
        assertFalse(origins.contains("ATL")); // ATL only on June 11
    }

    @Test
    void testGetDestinationAirports() {
        ScheduleDateRouteIndex index = new ScheduleDateRouteIndex(scheduleIndex, june10);

        Set<String> destinations = index.getDestinationAirports();

        assertEquals(3, destinations.size());
        assertTrue(destinations.contains("ORD"));
        assertTrue(destinations.contains("DAL"));
        assertTrue(destinations.contains("LAX"));
    }

    @Test
    void testGetActualRoutes() {
        ScheduleDateRouteIndex index = new ScheduleDateRouteIndex(scheduleIndex, june10);

        Set<String> routes = index.getActualRoutes();

        assertEquals(3, routes.size());
        assertTrue(routes.contains("BWI-ORD"));
        assertTrue(routes.contains("ORD-DAL"));
        assertTrue(routes.contains("BWI-LAX"));
        assertFalse(routes.contains("ATL-LAX")); // Only on June 11
    }

    @Test
    void testGetActualRoutesForDifferentDate() {
        ScheduleDateRouteIndex index = new ScheduleDateRouteIndex(scheduleIndex, june11);

        Set<String> routes = index.getActualRoutes();

        assertEquals(1, routes.size());
        assertTrue(routes.contains("ATL-LAX"));
        assertFalse(routes.contains("BWI-ORD")); // Only on June 10
    }

    @Test
    void testGetRouteDistance() {
        ScheduleDateRouteIndex index = new ScheduleDateRouteIndex(scheduleIndex, june10);

        // BWI-ORD is a valid route on June 10
        double distance = index.getRouteDistance("BWI", "ORD");

        assertTrue(distance > 0, "Distance should be positive");
        assertTrue(distance > 500 && distance < 800, "BWI-ORD distance should be ~600 miles");
    }

    @Test
    void testGetRouteDistanceForNonExistentRoute() {
        ScheduleDateRouteIndex index = new ScheduleDateRouteIndex(scheduleIndex, june10);

        // ATL-LAX does not operate on June 10 (only June 11)
        double distance = index.getRouteDistance("ATL", "LAX");

        assertEquals(0.0, distance, "Distance for non-existent route should be 0");
    }

    @Test
    void testGetFlightsForRoute() {
        ScheduleDateRouteIndex index = new ScheduleDateRouteIndex(scheduleIndex, june10);

        List<BookableFlight> flights = index.getFlightsForRoute("BWI", "ORD");

        assertEquals(1, flights.size());
        assertEquals("AA", flights.get(0).getCarrierCode());
        assertEquals("100", flights.get(0).getFlightNumber());
    }

    @Test
    void testGetFlightsForRouteOnDifferentDate() {
        ScheduleDateRouteIndex index = new ScheduleDateRouteIndex(scheduleIndex, june11);

        // BWI-ORD doesn't operate on June 11
        List<BookableFlight> flights = index.getFlightsForRoute("BWI", "ORD");

        assertTrue(flights.isEmpty());
    }

    @Test
    void testGetTargetDate() {
        ScheduleDateRouteIndex index = new ScheduleDateRouteIndex(scheduleIndex, june10);

        assertEquals(june10, index.getTargetDate());
    }

    @Test
    void testEmptyDateHasNoRoutes() {
        LocalDate emptyDate = LocalDate.of(2025, 12, 25);
        ScheduleDateRouteIndex index = new ScheduleDateRouteIndex(scheduleIndex, emptyDate);

        assertTrue(index.getOriginAirports().isEmpty());
        assertTrue(index.getDestinationAirports().isEmpty());
        assertTrue(index.getActualRoutes().isEmpty());
    }
}
