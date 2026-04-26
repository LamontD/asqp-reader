package com.lamontd.travel.flight.schedule.controller;

import com.lamontd.travel.flight.mapper.AirportCodeMapper;
import com.lamontd.travel.flight.mapper.CarrierCodeMapper;
import com.lamontd.travel.flight.model.BookableFlight;
import com.lamontd.travel.flight.schedule.index.ScheduleFlightIndex;
import com.lamontd.travel.flight.schedule.util.InputValidator;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Controller for viewing all flights at an airport (arrivals and departures).
 */
public class AirportViewController {
    private static final AirportCodeMapper AIRPORT_MAPPER = AirportCodeMapper.getDefault();
    private static final CarrierCodeMapper CARRIER_MAPPER = CarrierCodeMapper.getDefault();

    public void display(ScheduleFlightIndex index, Scanner scanner) {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("AIRPORT VIEW");
        System.out.println("=".repeat(80));

        System.out.print("Enter airport code (3 letters): ");
        String airport = scanner.nextLine().trim().toUpperCase();
        if (!InputValidator.validateAirportCode(airport)) {
            return;
        }

        List<BookableFlight> departures = index.getFlightsByOrigin(airport);
        List<BookableFlight> arrivals = index.getFlightsByDestination(airport);

        displayResults(airport, departures, arrivals);
    }

    private void displayResults(String airport, List<BookableFlight> departures, List<BookableFlight> arrivals) {
        String city = AIRPORT_MAPPER.getAirportCity(airport);

        System.out.println("\n" + "=".repeat(80));
        System.out.printf("AIRPORT SUMMARY: %s (%s)%n", airport, city);
        System.out.println("=".repeat(80));

        if (departures.isEmpty()) {
            System.out.println("\nNo departure data available for this airport.");
            return;
        }

        // Calculate total flights over the period
        int totalFlights = departures.size();

        // Get date range
        Set<LocalDate> uniqueDates = departures.stream()
                .map(BookableFlight::getOperatingDate)
                .collect(Collectors.toSet());
        int totalDays = uniqueDates.size();

        // Display overall statistics
        System.out.println("\nOVERALL STATISTICS:");
        System.out.println("-".repeat(80));
        System.out.printf("  Total Departures: %d%n", totalFlights);
        System.out.printf("  Date Range: %s to %s (%d days)%n",
                uniqueDates.stream().min(LocalDate::compareTo).orElse(null),
                uniqueDates.stream().max(LocalDate::compareTo).orElse(null),
                totalDays);
        System.out.printf("  Overall Average: %.1f flights per day%n",
                totalDays > 0 ? (double) totalFlights / totalDays : 0.0);

        // Calculate average flights per day per carrier
        Map<String, List<BookableFlight>> byCarrier = departures.stream()
                .collect(Collectors.groupingBy(BookableFlight::getCarrierCode));

        System.out.println("\nAVERAGE FLIGHTS PER DAY BY CARRIER:");
        System.out.println("-".repeat(80));
        System.out.printf("%-8s %-30s %-12s %-12s%n",
                "Code", "Carrier Name", "Total", "Avg/Day");
        System.out.println("-".repeat(80));

        byCarrier.entrySet().stream()
                .sorted((e1, e2) -> Integer.compare(e2.getValue().size(), e1.getValue().size()))
                .forEach(entry -> {
                    String carrierCode = entry.getKey();
                    String carrierName = CARRIER_MAPPER.getCarrierName(carrierCode);
                    int carrierFlights = entry.getValue().size();
                    double avgPerDay = totalDays > 0 ? (double) carrierFlights / totalDays : 0.0;

                    System.out.printf("%-8s %-30s %-12d %-12.1f%n",
                            carrierCode, carrierName, carrierFlights, avgPerDay);
                });

        // Calculate top 10 destinations
        Map<String, Long> destinationCounts = departures.stream()
                .collect(Collectors.groupingBy(
                        BookableFlight::getDestinationAirport,
                        Collectors.counting()));

        System.out.println("\nTOP 10 DESTINATIONS:");
        System.out.println("-".repeat(80));
        System.out.printf("%-6s %-8s %-30s %-12s%n",
                "Rank", "Code", "City", "Flights");
        System.out.println("-".repeat(80));

        int[] rank = {1};
        destinationCounts.entrySet().stream()
                .sorted((e1, e2) -> Long.compare(e2.getValue(), e1.getValue()))
                .limit(10)
                .forEach(entry -> {
                    String destCode = entry.getKey();
                    String destCity = AIRPORT_MAPPER.getAirportCity(destCode);
                    long flightCount = entry.getValue();

                    System.out.printf("%-6d %-8s %-30s %-12d%n",
                            rank[0]++, destCode, destCity, flightCount);
                });
    }
}
