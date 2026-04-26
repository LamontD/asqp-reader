package com.lamontd.travel.flight.schedule.index;

import com.lamontd.travel.flight.model.BookableFlight;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ScheduleFlightIndexTest {

    private ScheduleFlightIndex index;

    @BeforeEach
    void setUp() {
        List<BookableFlight> flights = List.of(
                // Jan 10 flights
                createFlight("WN", "100", "LAX", "SFO", LocalDate.of(2025, 1, 10)),
                createFlight("WN", "200", "SFO", "LAX", LocalDate.of(2025, 1, 10)),
                createFlight("AA", "300", "JFK", "LAX", LocalDate.of(2025, 1, 10)),
                createFlight("DL", "500", "ATL", "ORD", LocalDate.of(2025, 1, 10)),
                createFlight("DL", "100", "ORD", "ATL", LocalDate.of(2025, 1, 10)),
                // Jan 20 flights (AA 400 instead of AA 300)
                createFlight("WN", "100", "LAX", "SFO", LocalDate.of(2025, 1, 20)),
                createFlight("WN", "200", "SFO", "LAX", LocalDate.of(2025, 1, 20)),
                createFlight("AA", "400", "LAX", "JFK", LocalDate.of(2025, 1, 20)),
                createFlight("DL", "500", "ATL", "ORD", LocalDate.of(2025, 1, 20)),
                createFlight("DL", "100", "ORD", "ATL", LocalDate.of(2025, 1, 20))
        );
        index = new ScheduleFlightIndex(flights);
    }

    @Test
    void testGetAllFlights() {
        assertEquals(10, index.getAllFlights().size());
    }

    @Test
    void testGetFlightsByCarrier() {
        List<BookableFlight> wnFlights = index.getFlightsByCarrier("WN");
        assertEquals(4, wnFlights.size());  // 2 flights on Jan 10, 2 on Jan 20
        assertTrue(wnFlights.stream().allMatch(f -> f.getCarrierCode().equals("WN")));

        List<BookableFlight> aaFlights = index.getFlightsByCarrier("AA");
        assertEquals(2, aaFlights.size());  // AA 300 on Jan 10, AA 400 on Jan 20

        List<BookableFlight> dlFlights = index.getFlightsByCarrier("DL");
        assertEquals(4, dlFlights.size());  // 2 flights on Jan 10, 2 on Jan 20
    }

    @Test
    void testGetFlightsByCarrierCaseInsensitive() {
        assertEquals(4, index.getFlightsByCarrier("wn").size());  // WN 100 & 200, each on 2 dates
        assertEquals(4, index.getFlightsByCarrier("Wn").size());
    }

    @Test
    void testGetFlightsByOrigin() {
        List<BookableFlight> laxFlights = index.getFlightsByOrigin("LAX");
        assertEquals(3, laxFlights.size());
        assertTrue(laxFlights.stream().allMatch(f -> f.getOriginAirport().equals("LAX")));
    }

    @Test
    void testGetFlightsByDestination() {
        List<BookableFlight> laxFlights = index.getFlightsByDestination("LAX");
        assertEquals(3, laxFlights.size());
        assertTrue(laxFlights.stream().allMatch(f -> f.getDestinationAirport().equals("LAX")));
    }

    @Test
    void testGetFlightsByRoute() {
        List<BookableFlight> laxToSfo = index.getFlightsByRoute("LAX", "SFO");
        assertEquals(2, laxToSfo.size());
        assertEquals("WN", laxToSfo.get(0).getCarrierCode());
        assertEquals("100", laxToSfo.get(0).getFlightNumber());

        List<BookableFlight> sfoToLax = index.getFlightsByRoute("SFO", "LAX");
        assertEquals(2, sfoToLax.size());
        assertEquals("WN", sfoToLax.get(0).getCarrierCode());
        assertEquals("200", sfoToLax.get(0).getFlightNumber());
    }

    @Test
    void testGetFlightsByRouteCaseInsensitive() {
        assertEquals(2, index.getFlightsByRoute("lax", "sfo").size());
        assertEquals(2, index.getFlightsByRoute("LAX", "sfo").size());
    }

    @Test
    void testGetFlightsByFlightNumber() {
        // Flight number 100 exists for both WN and DL
        List<BookableFlight> flights100 = index.getFlightsByFlightNumber("100");
        assertEquals(4, flights100.size());
        assertTrue(flights100.stream().anyMatch(f -> f.getCarrierCode().equals("WN")));
        assertTrue(flights100.stream().anyMatch(f -> f.getCarrierCode().equals("DL")));
    }

    @Test
    void testGetFlightsByCarrierAndFlightNumber() {
        List<BookableFlight> wn100 = index.getFlightsByCarrierAndFlightNumber("WN", "100");
        assertEquals(2, wn100.size());  // On Jan 10 & 20
        assertTrue(wn100.stream().allMatch(f -> f.getCarrierCode().equals("WN") && f.getFlightNumber().equals("100")));

        List<BookableFlight> dl100 = index.getFlightsByCarrierAndFlightNumber("DL", "100");
        assertEquals(2, dl100.size());  // On Jan 10 & 20
        assertTrue(dl100.stream().allMatch(f -> f.getCarrierCode().equals("DL") && f.getFlightNumber().equals("100")));
    }

    @Test
    void testGetFlightsEffectiveOn() {
        // Jan 10 should have 5 flights (AA 300 ends on Jan 15, AA 400 starts on Jan 16)
        List<BookableFlight> jan10 = index.getFlightsOnDate(LocalDate.of(2025, 1, 10));
        assertEquals(5, jan10.size());
        assertTrue(jan10.stream().anyMatch(f -> f.getFlightNumber().equals("300")));
        assertFalse(jan10.stream().anyMatch(f -> f.getFlightNumber().equals("400")));

        // Jan 20 should have 5 flights (AA 400 active, AA 300 ended)
        List<BookableFlight> jan20 = index.getFlightsOnDate(LocalDate.of(2025, 1, 20));
        assertEquals(5, jan20.size());
        assertTrue(jan20.stream().anyMatch(f -> f.getFlightNumber().equals("400")));
        assertFalse(jan20.stream().anyMatch(f -> f.getFlightNumber().equals("300")));
    }

    @Test
    void testGetFlightsByRouteEffectiveOn() {
        // JFK-LAX: AA 300 effective Jan 1-15
        List<BookableFlight> jfkLaxJan10 = index.getFlightsByRouteOnDate("JFK", "LAX", LocalDate.of(2025, 1, 10));
        assertEquals(1, jfkLaxJan10.size());
        assertEquals("300", jfkLaxJan10.get(0).getFlightNumber());

        List<BookableFlight> jfkLaxJan20 = index.getFlightsByRouteOnDate("JFK", "LAX", LocalDate.of(2025, 1, 20));
        assertEquals(0, jfkLaxJan20.size());

        // LAX-JFK: AA 400 effective Jan 16-31
        List<BookableFlight> laxJfkJan10 = index.getFlightsByRouteOnDate("LAX", "JFK", LocalDate.of(2025, 1, 10));
        assertEquals(0, laxJfkJan10.size());

        List<BookableFlight> laxJfkJan20 = index.getFlightsByRouteOnDate("LAX", "JFK", LocalDate.of(2025, 1, 20));
        assertEquals(1, laxJfkJan20.size());
        assertEquals("400", laxJfkJan20.get(0).getFlightNumber());
    }

    @Test
    void testGetAllCarriers() {
        Set<String> carriers = index.getAllCarriers();
        assertEquals(3, carriers.size());
        assertTrue(carriers.containsAll(Set.of("WN", "AA", "DL")));
    }

    @Test
    void testGetAllAirports() {
        Set<String> airports = index.getAllAirports();
        assertEquals(5, airports.size());  // LAX, SFO, JFK, ATL, ORD
        assertTrue(airports.containsAll(Set.of("LAX", "SFO", "JFK", "ATL", "ORD")));
    }

    @Test
    void testGetAllRoutes() {
        Set<String> routes = index.getAllRoutes();
        assertEquals(6, routes.size());
        assertTrue(routes.contains("LAX-SFO"));
        assertTrue(routes.contains("SFO-LAX"));
        assertTrue(routes.contains("JFK-LAX"));
        assertTrue(routes.contains("LAX-JFK"));
        assertTrue(routes.contains("ATL-ORD"));
        assertTrue(routes.contains("ORD-ATL"));
    }

    @Test
    void testGetFlightCount() {
        assertEquals(10, index.getFlightCount());
    }

    @Test
    void testGetStats() {
        ScheduleFlightIndex.IndexStats stats = index.getStats();
        assertEquals(10, stats.totalFlights());
        assertEquals(3, stats.totalCarriers());
        assertEquals(5, stats.totalOriginAirports()); // LAX, SFO, JFK, ATL, ORD
        assertEquals(5, stats.totalDestinationAirports()); // SFO, LAX, LAX, JFK, ORD, ATL
        assertEquals(6, stats.totalRoutes());
    }

    @Test
    void testEmptyIndex() {
        ScheduleFlightIndex emptyIndex = new ScheduleFlightIndex(List.of());
        assertEquals(0, emptyIndex.getFlightCount());
        assertEquals(0, emptyIndex.getAllCarriers().size());
        assertEquals(0, emptyIndex.getAllAirports().size());
        assertEquals(0, emptyIndex.getAllFlights().size());
    }

    @Test
    void testNoMatchingFlights() {
        assertEquals(0, index.getFlightsByCarrier("UA").size());
        assertEquals(0, index.getFlightsByOrigin("DEN").size());
        assertEquals(0, index.getFlightsByRoute("SEA", "PDX").size());
        assertEquals(0, index.getFlightsByFlightNumber("999").size());
    }

    private BookableFlight createFlight(String carrier, String flightNum, String origin, String dest,
                                         LocalDate operatingDate) {
        return BookableFlight.builder()
                .carrierCode(carrier)
                .flightNumber(flightNum)
                .originAirport(origin)
                .destinationAirport(dest)
                .scheduledDepartureTime(LocalTime.of(10, 0))
                .scheduledArrivalTime(LocalTime.of(12, 0))
                .operatingDate(operatingDate)
                .build();
    }
}
