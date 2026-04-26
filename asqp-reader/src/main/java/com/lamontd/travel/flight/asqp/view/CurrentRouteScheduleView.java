package com.lamontd.travel.flight.asqp.view;

import com.lamontd.travel.flight.asqp.index.FlightDataIndex;
import com.lamontd.travel.flight.asqp.model.ASQPFlightRecord;
import com.lamontd.travel.flight.mapper.AirportCodeMapper;
import com.lamontd.travel.flight.mapper.CarrierCodeMapper;
import com.lamontd.travel.flight.model.ScheduledFlight;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * View for displaying currently active scheduled flights between two airports.
 * Filters out flights that appear to be discontinued based on recent activity.
 */
public class CurrentRouteScheduleView {

    private static final int LOOKBACK_DAYS = 14; // Look back 2 weeks to determine if flight is active

    public void render(FlightDataIndex index, Scanner scanner) {
        AirportCodeMapper airportMapper = AirportCodeMapper.getDefault();
        CarrierCodeMapper carrierMapper = CarrierCodeMapper.getDefault();

        System.out.println("\n" + "=".repeat(50));
        System.out.println("CURRENT ROUTE SCHEDULE LOOKUP");
        System.out.println("=".repeat(50));

        System.out.print("\nEnter origin airport code: ");
        String origin = scanner.nextLine().trim().toUpperCase();

        if (origin.isEmpty()) {
            return;
        }

        System.out.print("Enter destination airport code: ");
        String destination = scanner.nextLine().trim().toUpperCase();

        if (destination.isEmpty()) {
            return;
        }

        // Get all scheduled flights for this route
        List<ScheduledFlight> allSchedules = index.getScheduledFlightsByRoute(origin, destination);

        if (allSchedules.isEmpty()) {
            System.out.println("\nNo scheduled flights found from " + origin + " to " + destination);
            return;
        }

        // Filter to only active flights at end of period
        LocalDate endDate = index.maxDate;
        List<ScheduledFlight> activeSchedules = allSchedules.stream()
                .filter(schedule -> isActiveAtEndOfPeriod(schedule, index, endDate))
                .sorted(Comparator
                    .comparing(ScheduledFlight::getCarrierCode)
                    .thenComparing(s -> s.getScheduledDepartureTime() != null ?
                        s.getScheduledDepartureTime() : java.time.LocalTime.MIN))
                .toList();

        String originCity = airportMapper.getAirportCity(origin);
        String destCity = airportMapper.getAirportCity(destination);
        double distance = index.getRouteDistance(origin, destination);

        System.out.println("\n" + "=".repeat(70));
        System.out.printf("CURRENT SCHEDULE: %s (%s) → %s (%s)%n",
            origin, originCity, destination, destCity);
        if (distance > 0) {
            System.out.printf("Distance: %.0f miles%n", distance);
        }
        System.out.printf("As of: %s (end of dataset period)%n", endDate);
        System.out.println("=".repeat(70));

        if (activeSchedules.isEmpty()) {
            System.out.println("\nNo currently active flights found on this route.");
            System.out.printf("\nNote: %d total flight(s) found, but none operated at least twice%n",
                allSchedules.size());
            System.out.printf("      in their expected schedule during the final %d days of the dataset.%n",
                LOOKBACK_DAYS);
            System.out.println("      These flights may have been discontinued or are seasonal.");
            return;
        }

        System.out.printf("\nFound %d currently active flight%s on this route:%n",
            activeSchedules.size(), activeSchedules.size() == 1 ? "" : "s");

        if (allSchedules.size() > activeSchedules.size()) {
            System.out.printf("(Filtered out %d inactive/discontinued flight%s)%n",
                allSchedules.size() - activeSchedules.size(),
                (allSchedules.size() - activeSchedules.size()) == 1 ? "" : "s");
        }
        System.out.println();

        // Display each active scheduled flight
        for (int i = 0; i < activeSchedules.size(); i++) {
            ScheduledFlight schedule = activeSchedules.get(i);
            displayScheduledFlight(schedule, i + 1, carrierMapper, index, endDate);
        }

        // Summary
        System.out.println("\n" + "=".repeat(70));
        System.out.println("SUMMARY");
        System.out.println("=".repeat(70));

        long totalCarriers = activeSchedules.stream()
                .map(ScheduledFlight::getCarrierCode)
                .distinct()
                .count();

        System.out.printf("Active Carriers: %d%n", totalCarriers);
        System.out.printf("Active Flight Numbers: %d%n", activeSchedules.size());

        // Count daily vs. weekly flights
        long dailyFlights = activeSchedules.stream()
                .filter(s -> s.getDaysOfOperation().isEmpty() ||
                            s.getDaysOfOperation().get().size() == 7)
                .count();

        long weeklyFlights = activeSchedules.size() - dailyFlights;

        if (dailyFlights > 0) {
            System.out.printf("Daily Flights: %d%n", dailyFlights);
        }
        if (weeklyFlights > 0) {
            System.out.printf("Select Days Flights: %d%n", weeklyFlights);
        }

        System.out.printf("\nNote: \"Active\" means the flight operated on at least 2 of its expected%n");
        System.out.printf("      operating days during the final %d days of the dataset.%n", LOOKBACK_DAYS);

        System.out.println("=".repeat(70));
    }

    /**
     * Determines if a scheduled flight is active at the end of the dataset period.
     * A flight is considered active if it operated on at least 2 of its expected
     * operating days during the lookback period.
     */
    private boolean isActiveAtEndOfPeriod(ScheduledFlight schedule, FlightDataIndex index,
                                         LocalDate endDate) {
        // Get all records for this specific flight
        List<ASQPFlightRecord> flightRecords = index.getByFlightNumber(
            schedule.getCarrierCode(),
            schedule.getFlightNumber()
        );

        // Filter to records matching this specific route and within lookback period
        LocalDate startOfLookback = endDate.minusDays(LOOKBACK_DAYS - 1);

        List<ASQPFlightRecord> recentRecords = flightRecords.stream()
                .filter(r -> r.getOrigin().equals(schedule.getOriginAirport()) &&
                           r.getDestination().equals(schedule.getDestinationAirport()))
                .filter(r -> !r.getDepartureDate().isBefore(startOfLookback) &&
                           !r.getDepartureDate().isAfter(endDate))
                .filter(r -> !r.isCancelled()) // Only count operated flights
                .toList();

        // Count how many expected operating days occurred in lookback period
        Set<DayOfWeek> operatingDays = schedule.getDaysOfOperation()
                .orElse(Set.of(DayOfWeek.values())); // If null, assume daily

        long expectedDaysInPeriod = 0;
        LocalDate date = startOfLookback;
        while (!date.isAfter(endDate)) {
            if (operatingDays.contains(date.getDayOfWeek())) {
                expectedDaysInPeriod++;
            }
            date = date.plusDays(1);
        }

        // Count actual operations on expected days
        long actualOperations = recentRecords.size();

        // Flight is active if it operated on at least 2 expected dates
        // AND at least 50% of expected dates (for infrequent flights)
        boolean operatedEnough = actualOperations >= 2;
        boolean meetsMinimumRate = expectedDaysInPeriod < 4 ||
                                  (actualOperations >= Math.max(2, expectedDaysInPeriod / 2));

        return operatedEnough && meetsMinimumRate;
    }

    private void displayScheduledFlight(ScheduledFlight schedule, int number,
                                       CarrierCodeMapper carrierMapper,
                                       FlightDataIndex index,
                                       LocalDate endDate) {
        String carrierName = carrierMapper.getCarrierName(schedule.getCarrierCode());

        System.out.println(number + ". " + schedule.getCarrierCode() + " " +
                          schedule.getFlightNumber() + " - " + carrierName);

        // Departure and arrival times
        if (schedule.getScheduledDepartureTime() != null && schedule.getScheduledArrivalTime() != null) {
            System.out.printf("   Departure: %s   Arrival: %s",
                schedule.getScheduledDepartureTime(),
                schedule.getScheduledArrivalTime());

            // Calculate and display flight duration
            Duration flightTime = Duration.between(
                schedule.getScheduledDepartureTime(),
                schedule.getScheduledArrivalTime()
            );

            // Handle red-eye flights (arrival next day)
            if (flightTime.isNegative()) {
                flightTime = flightTime.plusDays(1);
            }

            long hours = flightTime.toHours();
            long minutes = flightTime.toMinutes() % 60;
            System.out.printf("   Duration: %dh %02dm%n", hours, minutes);
        } else {
            System.out.println("   Schedule times not available");
        }

        // Days of operation
        if (schedule.getDaysOfOperation().isPresent()) {
            Set<DayOfWeek> days = schedule.getDaysOfOperation().get();
            if (!days.isEmpty() && days.size() < 7) {
                String daysStr = days.stream()
                        .sorted()
                        .map(day -> day.toString().substring(0, 3))
                        .collect(Collectors.joining(", "));
                System.out.println("   Operates: " + daysStr);
            } else {
                System.out.println("   Operates: Daily");
            }
        } else {
            System.out.println("   Operates: Daily");
        }

        // Show recent activity
        List<ASQPFlightRecord> recentRecords = index.getByFlightNumber(
            schedule.getCarrierCode(),
            schedule.getFlightNumber()
        ).stream()
                .filter(r -> r.getOrigin().equals(schedule.getOriginAirport()) &&
                           r.getDestination().equals(schedule.getDestinationAirport()))
                .filter(r -> !r.getDepartureDate().isBefore(endDate.minusDays(LOOKBACK_DAYS - 1)))
                .filter(r -> !r.isCancelled())
                .toList();

        System.out.printf("   Recent activity: %d operation%s in last %d days%n",
            recentRecords.size(),
            recentRecords.size() == 1 ? "" : "s",
            LOOKBACK_DAYS);

        System.out.println();
    }
}
