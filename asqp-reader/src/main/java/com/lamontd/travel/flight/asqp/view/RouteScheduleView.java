package com.lamontd.travel.flight.asqp.view;

import com.lamontd.travel.flight.asqp.index.FlightDataIndex;
import com.lamontd.travel.flight.mapper.AirportCodeMapper;
import com.lamontd.travel.flight.mapper.CarrierCodeMapper;
import com.lamontd.travel.flight.model.ScheduledFlight;

import java.time.DayOfWeek;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * View for displaying all scheduled flights between two airports
 */
public class RouteScheduleView {

    public void render(FlightDataIndex index, Scanner scanner) {
        AirportCodeMapper airportMapper = AirportCodeMapper.getDefault();
        CarrierCodeMapper carrierMapper = CarrierCodeMapper.getDefault();

        System.out.println("\n" + "=".repeat(50));
        System.out.println("ROUTE SCHEDULE LOOKUP");
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

        // Get scheduled flights for this route
        List<ScheduledFlight> schedules = index.getScheduledFlightsByRoute(origin, destination);

        if (schedules.isEmpty()) {
            System.out.println("\nNo scheduled flights found from " + origin + " to " + destination);
            System.out.println("\nNote: This data reflects observed operations in the dataset.");
            System.out.println("Some routes may operate but not appear in this time period.");
            return;
        }

        String originCity = airportMapper.getAirportCity(origin);
        String destCity = airportMapper.getAirportCity(destination);
        double distance = index.getRouteDistance(origin, destination);

        System.out.println("\n" + "=".repeat(70));
        System.out.printf("SCHEDULED FLIGHTS: %s (%s) → %s (%s)%n",
            origin, originCity, destination, destCity);
        if (distance > 0) {
            System.out.printf("Distance: %.0f miles%n", distance);
        }
        System.out.println("=".repeat(70));

        System.out.printf("\nFound %d scheduled flight%s on this route:%n%n",
            schedules.size(), schedules.size() == 1 ? "" : "s");

        // Sort by carrier, then by departure time
        List<ScheduledFlight> sortedSchedules = schedules.stream()
                .sorted(Comparator
                    .comparing(ScheduledFlight::getCarrierCode)
                    .thenComparing(s -> s.getScheduledDepartureTime() != null ?
                        s.getScheduledDepartureTime() : java.time.LocalTime.MIN))
                .toList();

        // Display each scheduled flight
        for (int i = 0; i < sortedSchedules.size(); i++) {
            ScheduledFlight schedule = sortedSchedules.get(i);
            displayScheduledFlight(schedule, i + 1, carrierMapper, airportMapper);
        }

        // Summary
        System.out.println("\n" + "=".repeat(70));
        System.out.println("SUMMARY");
        System.out.println("=".repeat(70));

        long totalCarriers = schedules.stream()
                .map(ScheduledFlight::getCarrierCode)
                .distinct()
                .count();

        System.out.printf("Total Carriers: %d%n", totalCarriers);
        System.out.printf("Total Flight Numbers: %d%n", schedules.size());

        // Count daily vs. weekly flights
        long dailyFlights = schedules.stream()
                .filter(s -> s.getDaysOfOperation().isEmpty() ||
                            s.getDaysOfOperation().get().size() == 7)
                .count();

        long weeklyFlights = schedules.size() - dailyFlights;

        if (dailyFlights > 0) {
            System.out.printf("Daily Flights: %d%n", dailyFlights);
        }
        if (weeklyFlights > 0) {
            System.out.printf("Select Days Flights: %d%n", weeklyFlights);
        }

        System.out.println("\n" + "=".repeat(70));
    }

    private void displayScheduledFlight(ScheduledFlight schedule, int number,
                                       CarrierCodeMapper carrierMapper,
                                       AirportCodeMapper airportMapper) {
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

        // Date range
        if (schedule.getEffectiveFrom().isPresent() && schedule.getEffectiveUntil().isPresent()) {
            System.out.printf("   Observed: %s to %s%n",
                schedule.getEffectiveFrom().get(),
                schedule.getEffectiveUntil().get());
        }

        System.out.println();
    }
}
