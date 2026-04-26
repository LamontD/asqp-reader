package com.lamontd.travel.flight.schedule.controller;

import com.lamontd.travel.flight.mapper.AirportCodeMapper;
import com.lamontd.travel.flight.mapper.CarrierCodeMapper;
import com.lamontd.travel.flight.model.BookableFlight;
import com.lamontd.travel.flight.schedule.index.ScheduleFlightIndex;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Controller for viewing all flights operated by a carrier.
 */
public class CarrierViewController {
    private static final CarrierCodeMapper CARRIER_MAPPER = CarrierCodeMapper.getDefault();
    private static final AirportCodeMapper AIRPORT_MAPPER = AirportCodeMapper.getDefault();

    public void display(ScheduleFlightIndex index, Scanner scanner) {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("CARRIER VIEW");
        System.out.println("=".repeat(80));

        System.out.print("Enter carrier code (2 letters): ");
        String carrier = scanner.nextLine().trim().toUpperCase();

        List<BookableFlight> flights = index.getFlightsByCarrier(carrier);

        displayResults(carrier, flights);
    }

    private void displayResults(String carrier, List<BookableFlight> flights) {
        String carrierName = CARRIER_MAPPER.getCarrierName(carrier);

        System.out.println("\n" + "=".repeat(80));
        System.out.printf("CARRIER: %s (%s)%n", carrier, carrierName);
        System.out.println("=".repeat(80));

        if (flights.isEmpty()) {
            System.out.println("No flights found for this carrier.");
            return;
        }

        // Calculate date range
        Set<LocalDate> uniqueDates = flights.stream()
                .map(BookableFlight::getOperatingDate)
                .collect(Collectors.toSet());
        int totalDays = uniqueDates.size();

        // Group by route for statistics
        Map<String, List<BookableFlight>> byRoute = flights.stream()
                .collect(Collectors.groupingBy(f ->
                        f.getOriginAirport() + "-" + f.getDestinationAirport()));

        // Calculate statistics
        Set<String> airports = new HashSet<>();
        for (BookableFlight flight : flights) {
            airports.add(flight.getOriginAirport());
            airports.add(flight.getDestinationAirport());
        }

        // Display network statistics
        System.out.println("\nNETWORK STATISTICS:");
        System.out.println("-".repeat(80));
        System.out.printf("  Total Flights: %d%n", flights.size());
        System.out.printf("  Date Range: %s to %s (%d days)%n",
                uniqueDates.stream().min(LocalDate::compareTo).orElse(null),
                uniqueDates.stream().max(LocalDate::compareTo).orElse(null),
                totalDays);
        System.out.printf("  Average Flights per Day: %.1f%n",
                totalDays > 0 ? (double) flights.size() / totalDays : 0.0);
        System.out.printf("  Total Routes: %d%n", byRoute.size());
        System.out.printf("  Airports Served: %d%n", airports.size());

        // Display top 10 routes by flight count
        System.out.println("\nTOP 10 ROUTES (by flight count):");
        System.out.println("-".repeat(80));
        System.out.printf("%-6s %-40s %-40s %-12s%n",
                "Rank", "Origin", "Destination", "Flights");
        System.out.println("-".repeat(80));

        List<Map.Entry<String, List<BookableFlight>>> sortedRoutes = byRoute.entrySet().stream()
                .sorted((e1, e2) -> Integer.compare(e2.getValue().size(), e1.getValue().size()))
                .limit(10)
                .toList();

        int rank = 1;
        for (Map.Entry<String, List<BookableFlight>> entry : sortedRoutes) {
            String[] route = entry.getKey().split("-");
            String origin = route[0];
            String dest = route[1];
            String originCity = AIRPORT_MAPPER.getAirportCity(origin);
            String destCity = AIRPORT_MAPPER.getAirportCity(dest);
            String originFormatted = String.format("%s (%s)", origin, originCity);
            String destFormatted = String.format("%s (%s)", dest, destCity);

            System.out.printf("%-6d %-40s %-40s %-12d%n",
                    rank++, originFormatted, destFormatted, entry.getValue().size());
        }

        // Calculate top 10 airports by average departures per day
        Map<String, List<BookableFlight>> byOrigin = flights.stream()
                .collect(Collectors.groupingBy(BookableFlight::getOriginAirport));

        System.out.println("\nTOP 10 AIRPORTS (by average departures per day):");
        System.out.println("-".repeat(80));
        System.out.printf("%-6s %-8s %-30s %-12s %-12s%n",
                "Rank", "Code", "City", "Total", "Avg/Day");
        System.out.println("-".repeat(80));

        byOrigin.entrySet().stream()
                .sorted((e1, e2) -> {
                    double avg1 = totalDays > 0 ? (double) e1.getValue().size() / totalDays : 0.0;
                    double avg2 = totalDays > 0 ? (double) e2.getValue().size() / totalDays : 0.0;
                    return Double.compare(avg2, avg1);
                })
                .limit(10)
                .forEach(entry -> {
                    String airportCode = entry.getKey();
                    String city = AIRPORT_MAPPER.getAirportCity(airportCode);
                    int airportFlights = entry.getValue().size();
                    double avgPerDay = totalDays > 0 ? (double) airportFlights / totalDays : 0.0;

                    System.out.printf("%-6d %-8s %-30s %-12d %-12.1f%n",
                            byOrigin.keySet().stream()
                                    .sorted((a1, a2) -> {
                                        double avg1 = totalDays > 0 ? (double) byOrigin.get(a1).size() / totalDays : 0.0;
                                        double avg2 = totalDays > 0 ? (double) byOrigin.get(a2).size() / totalDays : 0.0;
                                        return Double.compare(avg2, avg1);
                                    })
                                    .toList()
                                    .indexOf(airportCode) + 1,
                            airportCode, city, airportFlights, avgPerDay);
                });
    }
}
