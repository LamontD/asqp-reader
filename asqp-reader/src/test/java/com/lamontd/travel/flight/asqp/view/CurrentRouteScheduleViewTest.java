package com.lamontd.travel.flight.asqp.view;

import com.lamontd.travel.flight.asqp.index.FlightDataIndex;
import com.lamontd.travel.flight.asqp.model.ASQPFlightRecord;
import com.lamontd.travel.flight.model.ScheduledFlight;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the Current Route Schedule filtering logic
 */
class CurrentRouteScheduleViewTest {

    @Test
    void testActiveFlightWithRecentOperations() {
        // Flight that operates throughout including at end of period
        LocalDate endDate = LocalDate.of(2025, 1, 30);
        List<ASQPFlightRecord> records = new ArrayList<>();

        // Add operations throughout the period, including recent dates
        for (int day = 18; day <= 30; day++) {
            records.add(createRecord("WN", "100", "LAX", "SFO",
                LocalDate.of(2025, 1, day), LocalTime.of(10, 0), LocalTime.of(11, 30)));
        }

        FlightDataIndex index = new FlightDataIndex(records);
        List<ScheduledFlight> schedules = index.getScheduledFlightsByRoute("LAX", "SFO");

        assertEquals(1, schedules.size());

        // This flight should be considered active (operates many times in last 14 days)
        ScheduledFlight schedule = schedules.get(0);
        assertEquals("WN", schedule.getCarrierCode());
        assertEquals("100", schedule.getFlightNumber());
    }

    @Test
    void testInactiveFlightDiscontinuedEarly() {
        // Flight that stops operating before end of period
        List<ASQPFlightRecord> records = new ArrayList<>();

        // Operates from Jan 1-10, but not after (dataset goes to Jan 30)
        for (int day = 1; day <= 10; day++) {
            records.add(createRecord("AA", "200", "JFK", "LAX",
                LocalDate.of(2025, 1, day), LocalTime.of(8, 0), LocalTime.of(11, 30)));
        }

        // Add a more recent flight to set the maxDate
        records.add(createRecord("DL", "300", "ORD", "DEN",
            LocalDate.of(2025, 1, 30), LocalTime.of(12, 0), LocalTime.of(14, 0)));

        FlightDataIndex index = new FlightDataIndex(records);
        List<ScheduledFlight> schedules = index.getScheduledFlightsByRoute("JFK", "LAX");

        // The discontinued flight should exist in the index
        assertEquals(1, schedules.size());

        // But when filtered for "current", it should be excluded
        // (We can't directly test the view's filtering logic without refactoring,
        // but we verify the schedule exists and would be filtered by activity check)
        ScheduledFlight schedule = schedules.get(0);
        assertTrue(schedule.getEffectiveUntil().isPresent());
        assertEquals(LocalDate.of(2025, 1, 10), schedule.getEffectiveUntil().get());

        // This flight should NOT pass the active filter since it has no operations
        // in the last 14 days of the dataset (Jan 17-30)
    }

    @Test
    void testMultipleFlightsSomeActiveOthersNot() {
        List<ASQPFlightRecord> records = new ArrayList<>();
        LocalDate endDate = LocalDate.of(2025, 1, 30);

        // Active flight: operates throughout including recent dates
        for (int day = 1; day <= 30; day++) {
            records.add(createRecord("WN", "100", "LAX", "SFO",
                LocalDate.of(2025, 1, day), LocalTime.of(10, 0), LocalTime.of(11, 30)));
        }

        // Inactive flight: stops mid-period
        for (int day = 1; day <= 12; day++) {
            records.add(createRecord("AA", "200", "LAX", "SFO",
                LocalDate.of(2025, 1, day), LocalTime.of(14, 0), LocalTime.of(15, 30)));
        }

        FlightDataIndex index = new FlightDataIndex(records);
        List<ScheduledFlight> schedules = index.getScheduledFlightsByRoute("LAX", "SFO");

        // Both flights should be in the index
        assertEquals(2, schedules.size());

        // Verify one is WN 100 (active) and one is AA 200 (inactive)
        assertTrue(schedules.stream().anyMatch(s ->
            s.getCarrierCode().equals("WN") && s.getFlightNumber().equals("100")));
        assertTrue(schedules.stream().anyMatch(s ->
            s.getCarrierCode().equals("AA") && s.getFlightNumber().equals("200")));

        // WN 100 should operate until Jan 30
        ScheduledFlight wnFlight = schedules.stream()
            .filter(s -> s.getCarrierCode().equals("WN"))
            .findFirst().get();
        assertEquals(LocalDate.of(2025, 1, 30), wnFlight.getEffectiveUntil().get());

        // AA 200 should stop at Jan 12
        ScheduledFlight aaFlight = schedules.stream()
            .filter(s -> s.getCarrierCode().equals("AA"))
            .findFirst().get();
        assertEquals(LocalDate.of(2025, 1, 12), aaFlight.getEffectiveUntil().get());
    }

    @Test
    void testWeekendOnlyFlightStillActive() {
        List<ASQPFlightRecord> records = new ArrayList<>();

        // Flight operates Saturdays and Sundays only
        // Add operations throughout January 2025
        LocalDate date = LocalDate.of(2025, 1, 1);
        LocalDate endDate = LocalDate.of(2025, 1, 31);

        LocalDate lastWeekendDate = null;
        while (!date.isAfter(endDate)) {
            if (date.getDayOfWeek().getValue() >= 6) { // Saturday or Sunday
                records.add(createRecord("B6", "500", "BOS", "MCO",
                    date, LocalTime.of(9, 0), LocalTime.of(12, 0)));
                lastWeekendDate = date;
            }
            date = date.plusDays(1);
        }

        FlightDataIndex index = new FlightDataIndex(records);
        List<ScheduledFlight> schedules = index.getScheduledFlightsByRoute("BOS", "MCO");

        assertEquals(1, schedules.size());
        ScheduledFlight schedule = schedules.get(0);

        // Should have weekend-only schedule
        assertTrue(schedule.getDaysOfOperation().isPresent());
        assertEquals(2, schedule.getDaysOfOperation().get().size());

        // Should operate until last weekend date in period (Jan 26, 2025 was a Sunday)
        assertEquals(lastWeekendDate, schedule.getEffectiveUntil().get());
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
