package com.lamontd.travel.flight.asqp.view;

import com.lamontd.travel.flight.asqp.index.FlightDataIndex;
import com.lamontd.travel.flight.mapper.AirportCodeMapper;
import com.lamontd.travel.flight.mapper.CarrierCodeMapper;
import com.lamontd.travel.flight.asqp.service.FlightScheduleService;
import com.lamontd.travel.flight.model.FlightSegment;

import java.time.DayOfWeek;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.stream.Collectors;

/**
 * View for displaying inferred flight schedules
 */
public class FlightScheduleView {

    public void render(FlightDataIndex index, Scanner scanner) {
        System.out.println("\n" + "=".repeat(50));
        System.out.println("FLIGHT SCHEDULE ANALYSIS");
        System.out.println("=".repeat(50));

        System.out.print("\nEnter flight number (format: CARRIER FLIGHT#, e.g., WN 5114): ");
        String input = scanner.nextLine().trim();

        if (input.isEmpty()) {
            return;
        }

        // Parse the input
        String[] parts = input.split("\\s+");
        if (parts.length != 2) {
            System.out.println("\nInvalid format. Please use format: CARRIER FLIGHT#");
            return;
        }

        String carrierCode = parts[0].toUpperCase();
        String flightNumber = parts[1];

        FlightScheduleService scheduleService = new FlightScheduleService(index);
        FlightScheduleService.FlightScheduleAnalysis schedule =
            scheduleService.analyzeFlightSchedule(carrierCode, flightNumber);

        if (schedule == null) {
            System.out.println("\nNo schedule data found for: " + carrierCode + " " + flightNumber);
            return;
        }

        displaySchedule(schedule);
    }

    private void displaySchedule(FlightScheduleService.FlightScheduleAnalysis schedule) {
        CarrierCodeMapper carrierMapper = CarrierCodeMapper.getDefault();
        AirportCodeMapper airportMapper = AirportCodeMapper.getDefault();

        String carrierName = carrierMapper.getCarrierName(schedule.carrierCode);
        String originCity = airportMapper.getAirportCity(schedule.origin);
        String destCity = airportMapper.getAirportCity(schedule.destination);

        System.out.println("\n" + "=".repeat(70));
        System.out.printf("FLIGHT SCHEDULE: %s %s (%s)%n",
            schedule.carrierCode, schedule.flightNumber, carrierName);
        System.out.println("=".repeat(70));

        // Route Information
        System.out.println("\n" + "-".repeat(70));
        System.out.println("PRIMARY ROUTE");
        System.out.println("-".repeat(70));
        System.out.printf("%-30s %s (%s)%n", "Origin:", schedule.origin, originCity);
        System.out.printf("%-30s %s (%s)%n", "Destination:", schedule.destination, destCity);

        if (schedule.typicalDeparture != null) {
            System.out.printf("%-30s %s%n", "Typical Departure:", schedule.typicalDeparture);
        }
        if (schedule.typicalArrival != null) {
            System.out.printf("%-30s %s%n", "Typical Arrival:", schedule.typicalArrival);
        }

        if (schedule.typicalDeparture != null && schedule.typicalArrival != null) {
            Duration flightTime = Duration.between(schedule.typicalDeparture, schedule.typicalArrival);
            long hours = flightTime.toHours();
            long minutes = flightTime.toMinutes() % 60;

            // Handle midnight crossing
            if (flightTime.isNegative()) {
                flightTime = flightTime.plusDays(1);
                hours = flightTime.toHours();
                minutes = flightTime.toMinutes() % 60;
            }

            System.out.printf("%-30s %dh %02dm%n", "Typical Flight Time:", hours, minutes);
        }

        // Route Pattern (for multi-leg flights)
        if (schedule.routePattern != null && schedule.routePattern.contains("-")) {
            String[] parts = schedule.routePattern.split("-");
            if (parts.length > 2) {
                System.out.printf("%-30s %s%n", "Multi-Leg Route Pattern:", schedule.routePattern);
            }
        }

        // Bookable Segments
        if (!schedule.bookableSegments.isEmpty() && schedule.bookableSegments.size() > 1) {
            displayBookableSegments(schedule, airportMapper);
        }

        // Days of Operation
        System.out.println("\n" + "-".repeat(70));
        System.out.println("OPERATING SCHEDULE");
        System.out.println("-".repeat(70));

        if (!schedule.operatingDays.isEmpty()) {
            System.out.println("Days of Operation:");
            DayOfWeek[] daysOrder = {
                DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY
            };

            for (DayOfWeek day : daysOrder) {
                if (schedule.operatingDays.contains(day)) {
                    Long count = schedule.dayFrequency.get(day);
                    String indicator = "  ✓ ";
                    System.out.printf("%s%-10s (%d operations)%n", indicator, day, count);
                } else {
                    System.out.printf("    %-10s (no operations)%n", day);
                }
            }
        }

        // Reliability Metrics
        System.out.println("\n" + "-".repeat(70));
        System.out.println("RELIABILITY METRICS");
        System.out.println("-".repeat(70));
        System.out.printf("%-30s %d operations%n", "Total in Dataset:", schedule.totalOperations);
        System.out.printf("%-30s %d (%.1f%%)%n", "Operated:",
            schedule.operatedCount, schedule.completionRate);
        System.out.printf("%-30s %d (%.1f%%)%n", "Cancelled:",
            schedule.cancelledCount, 100 - schedule.completionRate);

        if (schedule.operatedCount > 0) {
            System.out.printf("%-30s %.1f%%", "On-Time Performance:", schedule.onTimeRate);
            System.out.println(" (within 15 min)");

            if (schedule.avgDelay != null) {
                System.out.printf("%-30s %.0f minutes%n", "Average Delay (when late):", schedule.avgDelay);
            }
        }

        // Alternate Routes (if any)
        if (schedule.routeFrequencies.size() > 1) {
            System.out.println("\n" + "-".repeat(70));
            System.out.println("ALTERNATE ROUTES");
            System.out.println("-".repeat(70));
            System.out.println("This flight operates on multiple routes:");

            schedule.routeFrequencies.entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                    .forEach(entry -> {
                        String route = entry.getKey();
                        Long count = entry.getValue();
                        double percentage = (count * 100.0) / schedule.totalOperations;

                        String[] parts = route.split("-");
                        String origin = parts[0];
                        String dest = parts[1];
                        String originName = airportMapper.getAirportCity(origin);
                        String destName = airportMapper.getAirportCity(dest);

                        System.out.printf("  %s (%s) → %s (%s): %d ops (%.1f%%)%n",
                            origin, originName, dest, destName, count, percentage);
                    });
        }

        // Schedule Summary
        System.out.println("\n" + "=".repeat(70));
        System.out.println("SCHEDULE SUMMARY");
        System.out.println("=".repeat(70));

        String daysOfWeek = schedule.operatingDays.stream()
                .sorted()
                .map(day -> day.toString().substring(0, 3))
                .collect(Collectors.joining(", "));

        System.out.printf("%s %s operates %s → %s%n",
            schedule.carrierCode, schedule.flightNumber, schedule.origin, schedule.destination);

        if (schedule.typicalDeparture != null && schedule.typicalArrival != null) {
            System.out.printf("Departing %s, arriving %s%n",
                schedule.typicalDeparture, schedule.typicalArrival);
        }

        System.out.printf("Operating on: %s%n", daysOfWeek);
        System.out.printf("Reliability: %.1f%% completion, %.1f%% on-time%n",
            schedule.completionRate, schedule.onTimeRate);

        System.out.println("=".repeat(70));
    }

    private void displayBookableSegments(FlightScheduleService.FlightScheduleAnalysis schedule,
                                         AirportCodeMapper airportMapper) {
        System.out.println("\n" + "-".repeat(70));
        System.out.println("BOOKABLE SEGMENTS");
        System.out.println("-".repeat(70));

        // Separate direct legs and through connections
        List<FlightSegment> directLegs = schedule.bookableSegments.stream()
                .filter(FlightSegment::isDirect)
                .sorted((s1, s2) -> {
                    // Try to sort by logical order if possible
                    return s1.getOriginAirport().compareTo(s2.getOriginAirport());
                })
                .toList();

        List<FlightSegment> throughConnections = schedule.bookableSegments.stream()
                .filter(FlightSegment::isThrough)
                .sorted((s1, s2) -> {
                    // Sort by leg count (shorter first), then alphabetically
                    int legCompare = Integer.compare(s1.getLegCount(), s2.getLegCount());
                    if (legCompare != 0) return legCompare;
                    return s1.getSegmentKey().compareTo(s2.getSegmentKey());
                })
                .toList();

        System.out.printf("This flight offers %d bookable segment%s:%n%n",
                schedule.bookableSegments.size(),
                schedule.bookableSegments.size() == 1 ? "" : "s");

        // Display direct legs
        if (!directLegs.isEmpty()) {
            System.out.println("Direct Legs:");
            for (int i = 0; i < directLegs.size(); i++) {
                FlightSegment segment = directLegs.get(i);
                String originCity = airportMapper.getAirportCity(segment.getOriginAirport());
                String destCity = airportMapper.getAirportCity(segment.getDestinationAirport());

                double distance = getDistanceIfAvailable(segment, schedule);
                if (distance > 0) {
                    System.out.printf("  %d. %s (%s) → %s (%s): %.0f miles%n",
                            i + 1,
                            segment.getOriginAirport(), originCity,
                            segment.getDestinationAirport(), destCity,
                            distance);
                } else {
                    System.out.printf("  %d. %s (%s) → %s (%s)%n",
                            i + 1,
                            segment.getOriginAirport(), originCity,
                            segment.getDestinationAirport(), destCity);
                }
            }
            System.out.println();
        }

        // Display through connections
        if (!throughConnections.isEmpty()) {
            System.out.println("Through Connections:");
            for (int i = 0; i < throughConnections.size(); i++) {
                FlightSegment segment = throughConnections.get(i);
                String originCity = airportMapper.getAirportCity(segment.getOriginAirport());
                String destCity = airportMapper.getAirportCity(segment.getDestinationAirport());

                String stops = segment.getIntermediateStops().isEmpty() ? "" :
                        " (via " + String.join(", ", segment.getIntermediateStops()) + ")";

                double distance = getDistanceIfAvailable(segment, schedule);
                if (distance > 0) {
                    System.out.printf("  %d. %s (%s) → %s (%s)%s: %.0f miles, %d leg%s%n",
                            i + 1,
                            segment.getOriginAirport(), originCity,
                            segment.getDestinationAirport(), destCity,
                            stops, distance, segment.getLegCount(),
                            segment.getLegCount() == 1 ? "" : "s");
                } else {
                    System.out.printf("  %d. %s (%s) → %s (%s)%s: %d leg%s%n",
                            i + 1,
                            segment.getOriginAirport(), originCity,
                            segment.getDestinationAirport(), destCity,
                            stops, segment.getLegCount(),
                            segment.getLegCount() == 1 ? "" : "s");
                }
            }
        }

        System.out.println("\nNote: All segments share the same flight number " +
                schedule.carrierCode + " " + schedule.flightNumber);
    }

    private double getDistanceIfAvailable(FlightSegment segment,
                                          FlightScheduleService.FlightScheduleAnalysis schedule) {
        // Try to get distance from the analysis route frequencies or calculate it
        // For now, return 0 (distance calculation could be added later if needed)
        return 0.0;
    }
}
