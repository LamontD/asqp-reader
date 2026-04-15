package com.lamontd.travel.flight.asqp.service;

import com.lamontd.travel.flight.util.FlightDataIndex;
import com.lamontd.travel.flight.model.ASQPFlightRecord;
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
}
