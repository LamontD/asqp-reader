package com.lamontd.travel.flight.asqp.service;

import com.lamontd.travel.flight.asqp.index.FlightDataIndex;
import com.lamontd.travel.flight.asqp.model.ASQPFlightRecord;
import com.lamontd.travel.flight.model.FlightSegment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FlightScheduleServiceTest {

    private FlightScheduleService scheduleService;
    private FlightDataIndex index;

    @BeforeEach
    void setUp() {
        // Create a flight that operates Mon/Wed/Fri at 10:00 JFK->LAX
        List<ASQPFlightRecord> records = new ArrayList<>();

        // Monday flight (operated)
        records.add(createFlight(
                "AA", "100", "JFK", "LAX",
                LocalDate.of(2025, 1, 6), // Monday
                LocalTime.of(10, 0),
                LocalTime.of(13, 30),
                false,
                LocalTime.of(10, 5), // 5 min late
                LocalTime.of(13, 35)
        ));

        // Wednesday flight (operated, on time)
        records.add(createFlight(
                "AA", "100", "JFK", "LAX",
                LocalDate.of(2025, 1, 8), // Wednesday
                LocalTime.of(10, 0),
                LocalTime.of(13, 30),
                false,
                LocalTime.of(9, 58), // 2 min early
                LocalTime.of(13, 25)
        ));

        // Friday flight (cancelled)
        records.add(createFlight(
                "AA", "100", "JFK", "LAX",
                LocalDate.of(2025, 1, 10), // Friday
                LocalTime.of(10, 0),
                LocalTime.of(13, 30),
                true,
                null,
                null
        ));

        // Monday flight (operated, significantly delayed)
        records.add(createFlight(
                "AA", "100", "JFK", "LAX",
                LocalDate.of(2025, 1, 13), // Monday
                LocalTime.of(10, 0),
                LocalTime.of(13, 30),
                false,
                LocalTime.of(10, 45), // 45 min late
                LocalTime.of(14, 15)
        ));

        index = new FlightDataIndex(records);
        scheduleService = new FlightScheduleService(index);
    }

    private ASQPFlightRecord createFlight(String carrier, String flightNum,
                                     String origin, String dest,
                                     LocalDate date,
                                     LocalTime scheduledDep, LocalTime scheduledArr,
                                     boolean cancelled,
                                     LocalTime actualDep, LocalTime actualArr) {
        ASQPFlightRecord.Builder builder = ASQPFlightRecord.builder()
                .carrierCode(carrier)
                .flightNumber(flightNum)
                .origin(origin)
                .destination(dest)
                .departureDate(date)
                .scheduledOagDeparture(scheduledDep)
                .scheduledCrsDeparture(scheduledDep)
                .scheduledArrival(scheduledArr)
                .scheduledCrsArrival(scheduledArr)
                .tailNumber("N12345");

        if (cancelled) {
            builder.cancellationCode("A");
        } else {
            builder.gateDeparture(actualDep);
            builder.gateArrival(actualArr);
        }

        return builder.build();
    }

    @Test
    void testAnalyzeFlightSchedule() {
        FlightScheduleService.FlightScheduleAnalysis analysis =
                scheduleService.analyzeFlightSchedule("AA", "100");

        assertNotNull(analysis);
        assertEquals("AA", analysis.carrierCode);
        assertEquals("100", analysis.flightNumber);
        assertEquals("JFK", analysis.origin);
        assertEquals("LAX", analysis.destination);
    }

    @Test
    void testTypicalTimes() {
        FlightScheduleService.FlightScheduleAnalysis analysis =
                scheduleService.analyzeFlightSchedule("AA", "100");

        assertNotNull(analysis);
        assertNotNull(analysis.typicalDeparture);
        assertNotNull(analysis.typicalArrival);

        // All scheduled for 10:00, so typical should be 10:00 (or within 15min window)
        assertEquals(10, analysis.typicalDeparture.getHour());
        assertEquals(13, analysis.typicalArrival.getHour());
    }

    @Test
    void testOperatingDays() {
        FlightScheduleService.FlightScheduleAnalysis analysis =
                scheduleService.analyzeFlightSchedule("AA", "100");

        assertNotNull(analysis);
        assertTrue(analysis.operatingDays.contains(DayOfWeek.MONDAY));
        assertTrue(analysis.operatingDays.contains(DayOfWeek.WEDNESDAY));
        assertTrue(analysis.operatingDays.contains(DayOfWeek.FRIDAY));

        // Should have 3 operating days
        assertEquals(3, analysis.operatingDays.size());

        // Monday appears twice
        assertEquals(2L, analysis.dayFrequency.get(DayOfWeek.MONDAY));
        // Wednesday and Friday appear once each
        assertEquals(1L, analysis.dayFrequency.get(DayOfWeek.WEDNESDAY));
        assertEquals(1L, analysis.dayFrequency.get(DayOfWeek.FRIDAY));
    }

    @Test
    void testReliabilityMetrics() {
        FlightScheduleService.FlightScheduleAnalysis analysis =
                scheduleService.analyzeFlightSchedule("AA", "100");

        assertNotNull(analysis);
        assertEquals(4, analysis.totalOperations); // 4 total flights
        assertEquals(3, analysis.operatedCount); // 3 operated
        assertEquals(1, analysis.cancelledCount); // 1 cancelled
        assertEquals(75.0, analysis.completionRate, 0.1); // 3/4 = 75%
    }

    @Test
    void testOnTimePerformance() {
        FlightScheduleService.FlightScheduleAnalysis analysis =
                scheduleService.analyzeFlightSchedule("AA", "100");

        assertNotNull(analysis);

        // Out of 3 operated flights:
        // - Flight 1: 5 min late (on time - within 15 min)
        // - Flight 2: 2 min early (on time)
        // - Flight 4: 45 min late (NOT on time)
        // So 2/3 = 66.7% on time

        assertEquals(66.7, analysis.onTimeRate, 1.0);

        // Average delay for delayed flights (only flight 4 counts as delayed)
        assertNotNull(analysis.avgDelay);
        assertEquals(45.0, analysis.avgDelay, 1.0);
    }

    @Test
    void testRouteConsistency() {
        FlightScheduleService.FlightScheduleAnalysis analysis =
                scheduleService.analyzeFlightSchedule("AA", "100");

        assertNotNull(analysis);
        assertEquals(1, analysis.routeFrequencies.size()); // Only one route

        assertTrue(analysis.routeFrequencies.containsKey("JFK-LAX"));
        assertEquals(4L, analysis.routeFrequencies.get("JFK-LAX"));
    }

    @Test
    void testNonExistentFlight() {
        FlightScheduleService.FlightScheduleAnalysis analysis =
                scheduleService.analyzeFlightSchedule("XX", "999");

        assertNull(analysis);
    }

    @Test
    void testMultiLegSegmentExtraction() {
        // Create multi-leg test data for WN 4283
        List<ASQPFlightRecord> multiLegRecords = new ArrayList<>();

        // Day 1: 2-leg flight (ORD -> DAL -> MCO)
        multiLegRecords.add(createFlight("WN", "4283", "ORD", "DAL",
            LocalDate.of(2025, 1, 1), LocalTime.of(7, 30), LocalTime.of(10, 5),
            false, LocalTime.of(7, 23), LocalTime.of(9, 43)));
        multiLegRecords.add(createFlight("WN", "4283", "DAL", "MCO",
            LocalDate.of(2025, 1, 1), LocalTime.of(10, 40), LocalTime.of(14, 10),
            false, LocalTime.of(10, 33), LocalTime.of(13, 54)));

        // Day 2: Single leg (DAL -> MCO only)
        multiLegRecords.add(createFlight("WN", "4283", "DAL", "MCO",
            LocalDate.of(2025, 1, 2), LocalTime.of(11, 5), LocalTime.of(14, 35),
            false, LocalTime.of(11, 8), LocalTime.of(14, 22)));

        FlightDataIndex multiLegIndex = new FlightDataIndex(multiLegRecords);
        FlightScheduleService multiLegService = new FlightScheduleService(multiLegIndex);

        FlightScheduleService.FlightScheduleAnalysis analysis =
            multiLegService.analyzeFlightSchedule("WN", "4283");

        assertNotNull(analysis);
        assertEquals("WN", analysis.carrierCode);
        assertEquals("4283", analysis.flightNumber);

        // Should have extracted segments
        assertNotNull(analysis.bookableSegments);
        assertFalse(analysis.bookableSegments.isEmpty());

        // Should have both direct legs and through connections
        long directLegs = analysis.bookableSegments.stream()
            .filter(FlightSegment::isDirect)
            .count();
        long throughConnections = analysis.bookableSegments.stream()
            .filter(FlightSegment::isThrough)
            .count();

        assertTrue(directLegs > 0, "Should have direct leg segments");
        assertTrue(throughConnections > 0, "Should have through connection segments");

        // Check for specific segments
        boolean hasOrdDal = analysis.bookableSegments.stream()
            .anyMatch(s -> s.getOriginAirport().equals("ORD") &&
                          s.getDestinationAirport().equals("DAL") &&
                          s.isDirect());
        boolean hasDalMco = analysis.bookableSegments.stream()
            .anyMatch(s -> s.getOriginAirport().equals("DAL") &&
                          s.getDestinationAirport().equals("MCO") &&
                          s.isDirect());
        boolean hasOrdMco = analysis.bookableSegments.stream()
            .anyMatch(s -> s.getOriginAirport().equals("ORD") &&
                          s.getDestinationAirport().equals("MCO") &&
                          s.isThrough());

        assertTrue(hasOrdDal, "Should have ORD-DAL direct leg segment");
        assertTrue(hasDalMco, "Should have DAL-MCO direct leg segment");
        assertTrue(hasOrdMco, "Should have ORD-MCO through connection segment");
    }

    @Test
    void testThroughConnectionIntermediateStops() {
        // Create 2-leg test data
        List<ASQPFlightRecord> records = new ArrayList<>();
        records.add(createFlight("WN", "4283", "ORD", "DAL",
            LocalDate.of(2025, 1, 1), LocalTime.of(7, 30), LocalTime.of(10, 5),
            false, LocalTime.of(7, 23), LocalTime.of(9, 43)));
        records.add(createFlight("WN", "4283", "DAL", "MCO",
            LocalDate.of(2025, 1, 1), LocalTime.of(10, 40), LocalTime.of(14, 10),
            false, LocalTime.of(10, 33), LocalTime.of(13, 54)));

        FlightDataIndex testIndex = new FlightDataIndex(records);
        FlightScheduleService testService = new FlightScheduleService(testIndex);

        FlightScheduleService.FlightScheduleAnalysis analysis =
            testService.analyzeFlightSchedule("WN", "4283");

        FlightSegment ordMco = analysis.bookableSegments.stream()
            .filter(s -> s.getOriginAirport().equals("ORD") &&
                        s.getDestinationAirport().equals("MCO") &&
                        s.isThrough())
            .findFirst()
            .orElse(null);

        assertNotNull(ordMco, "Should have ORD-MCO through connection");
        assertEquals(1, ordMco.getIntermediateStops().size(),
            "ORD-MCO should have 1 intermediate stop");
        assertEquals("DAL", ordMco.getIntermediateStops().get(0),
            "Intermediate stop should be DAL");
        assertEquals(2, ordMco.getLegCount(), "ORD-MCO should be 2 legs");
    }

    @Test
    void testThreeLegSegmentExtraction() {
        // Create 3-leg test data: A -> B -> C -> D
        List<ASQPFlightRecord> records = new ArrayList<>();
        records.add(createFlight("WN", "5114", "PHX", "LAS",
            LocalDate.of(2025, 1, 1), LocalTime.of(8, 0), LocalTime.of(9, 0),
            false, LocalTime.of(8, 0), LocalTime.of(9, 0)));
        records.add(createFlight("WN", "5114", "LAS", "SFO",
            LocalDate.of(2025, 1, 1), LocalTime.of(10, 0), LocalTime.of(11, 30),
            false, LocalTime.of(10, 0), LocalTime.of(11, 30)));
        records.add(createFlight("WN", "5114", "SFO", "SEA",
            LocalDate.of(2025, 1, 1), LocalTime.of(13, 0), LocalTime.of(15, 0),
            false, LocalTime.of(13, 0), LocalTime.of(15, 0)));

        FlightDataIndex testIndex = new FlightDataIndex(records);
        FlightScheduleService testService = new FlightScheduleService(testIndex);

        FlightScheduleService.FlightScheduleAnalysis analysis =
            testService.analyzeFlightSchedule("WN", "5114");

        assertNotNull(analysis);

        // 3 legs should produce 6 segments:
        // - 3 direct: PHX-LAS, LAS-SFO, SFO-SEA
        // - 3 through: PHX-SFO (via LAS), PHX-SEA (via LAS,SFO), LAS-SEA (via SFO)
        assertEquals(6, analysis.bookableSegments.size(),
            "3-leg flight should produce 6 segments");

        long directLegs = analysis.bookableSegments.stream()
            .filter(FlightSegment::isDirect).count();
        long throughConnections = analysis.bookableSegments.stream()
            .filter(FlightSegment::isThrough).count();

        assertEquals(3, directLegs, "Should have 3 direct leg segments");
        assertEquals(3, throughConnections, "Should have 3 through connection segments");

        // Check the longest through connection (PHX-SEA via LAS,SFO)
        FlightSegment phxSea = analysis.bookableSegments.stream()
            .filter(s -> s.getOriginAirport().equals("PHX") &&
                        s.getDestinationAirport().equals("SEA") &&
                        s.isThrough())
            .findFirst()
            .orElse(null);

        assertNotNull(phxSea, "Should have PHX-SEA through connection");
        assertEquals(2, phxSea.getIntermediateStops().size(),
            "PHX-SEA should have 2 intermediate stops");
        assertEquals(3, phxSea.getLegCount(), "PHX-SEA should be 3 legs");
    }

    @Test
    void testRoutePattern() {
        // Create 2-leg test data
        List<ASQPFlightRecord> records = new ArrayList<>();
        records.add(createFlight("WN", "4283", "ORD", "DAL",
            LocalDate.of(2025, 1, 1), LocalTime.of(7, 30), LocalTime.of(10, 5),
            false, LocalTime.of(7, 23), LocalTime.of(9, 43)));
        records.add(createFlight("WN", "4283", "DAL", "MCO",
            LocalDate.of(2025, 1, 1), LocalTime.of(10, 40), LocalTime.of(14, 10),
            false, LocalTime.of(10, 33), LocalTime.of(13, 54)));

        FlightDataIndex testIndex = new FlightDataIndex(records);
        FlightScheduleService testService = new FlightScheduleService(testIndex);

        FlightScheduleService.FlightScheduleAnalysis analysis =
            testService.analyzeFlightSchedule("WN", "4283");

        assertNotNull(analysis.routePattern);
        // Route pattern should be ORD-DAL-MCO for the 2-leg operation
        assertTrue(analysis.routePattern.contains("ORD"));
        assertTrue(analysis.routePattern.contains("DAL"));
        assertTrue(analysis.routePattern.contains("MCO"));
    }
}
