package com.lamontd.asqp.view;

import com.lamontd.asqp.index.FlightDataIndex;
import com.lamontd.asqp.mapper.AirportCodeMapper;
import com.lamontd.asqp.model.FlightRecord;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Renders the airplane (tail number) view screen
 */
public class AirplaneView implements ViewRenderer {

    @Override
    public void render(FlightDataIndex index, Scanner scanner) {
        AirportCodeMapper airportMapper = AirportCodeMapper.getDefault();

        System.out.println("\n" + "=".repeat(50));
        System.out.println("AIRPLANE VIEW");
        System.out.println("=".repeat(50));

        System.out.print("\nEnter tail number: ");
        String tailNumber = scanner.nextLine().trim().toUpperCase();

        if (tailNumber.isEmpty()) {
            return;
        }

        // Use indexed lookup - O(1) instead of O(n)
        List<FlightRecord> allPlaneFlights = index.getByTailNumber(tailNumber);

        if (allPlaneFlights.isEmpty()) {
            System.out.println("\nNo flights found for tail number: " + tailNumber);
            return;
        }

        // Filter out cancelled and sort
        List<FlightRecord> planeFlights = allPlaneFlights.stream()
                .filter(r -> !r.isCancelled())
                .sorted(Comparator.comparing(FlightRecord::getDepartureDate)
                        .thenComparing(r -> r.getGateDeparture().orElse(LocalTime.MIN)))
                .toList();

        if (planeFlights.isEmpty()) {
            System.out.println("\nNo operated flights found for tail number: " + tailNumber);
            return;
        }

        System.out.println("\n" + "-".repeat(50));
        System.out.println("Tail Number: " + tailNumber);
        System.out.println("Total Operated Flights: " + planeFlights.size());
        System.out.println("-".repeat(50));

        // Group flights by departure date
        Map<LocalDate, List<FlightRecord>> flightsByDate = planeFlights.stream()
                .collect(Collectors.groupingBy(
                        FlightRecord::getDepartureDate,
                        TreeMap::new,
                        Collectors.toList()
                ));

        System.out.println("\nDaily Flight History:");

        for (Map.Entry<LocalDate, List<FlightRecord>> entry : flightsByDate.entrySet()) {
            LocalDate date = entry.getKey();
            List<FlightRecord> dailyFlights = entry.getValue();

            // Sort by departure time
            dailyFlights.sort(Comparator.comparing(r -> r.getGateDeparture().orElse(LocalTime.MIN)));

            // Build the route chain and calculate total distance
            StringBuilder route = new StringBuilder();
            double totalDistance = 0;
            for (int i = 0; i < dailyFlights.size(); i++) {
                FlightRecord flight = dailyFlights.get(i);
                if (i == 0) {
                    route.append(flight.getOrigin());
                }
                route.append(" -> ").append(flight.getDestination());
                totalDistance += index.getDistance(flight.getOrigin(), flight.getDestination());
            }

            System.out.println("\n  " + date + ":");
            System.out.printf("    Route: %s (%.0f miles)%n", route, totalDistance);
            System.out.println("    Legs: " + dailyFlights.size());

            // Show detailed leg information
            for (FlightRecord flight : dailyFlights) {
                String originCity = airportMapper.getAirportCity(flight.getOrigin());
                String destCity = airportMapper.getAirportCity(flight.getDestination());
                String depTime = flight.getGateDeparture().map(LocalTime::toString).orElse("--:--");
                String arrTime = flight.getGateArrival().map(LocalTime::toString).orElse("--:--");

                System.out.printf("      %s%s: %s (%s) [%s] -> %s (%s) [%s]%n",
                        flight.getCarrierCode(),
                        flight.getFlightNumber(),
                        flight.getOrigin(),
                        originCity,
                        depTime,
                        flight.getDestination(),
                        destCity,
                        arrTime);
            }
        }

        // Summary statistics
        System.out.println("\n" + "-".repeat(50));
        System.out.println("Summary:");
        System.out.println("  Total Days Operated: " + flightsByDate.size());
        System.out.println("  Total Flights: " + planeFlights.size());
        System.out.printf("  Average Flights per Day: %.1f%n",
                (double) planeFlights.size() / flightsByDate.size());

        // Most common airports
        Map<String, Long> airportFrequency = planeFlights.stream()
                .flatMap(r -> java.util.stream.Stream.of(r.getOrigin(), r.getDestination()))
                .collect(Collectors.groupingBy(
                        airport -> airport,
                        Collectors.counting()
                ));

        System.out.println("\n  Most Visited Airports:");
        airportFrequency.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
                .forEach(e -> {
                    String city = airportMapper.getAirportCity(e.getKey());
                    System.out.printf("    %s (%s): %d times%n", e.getKey(), city, e.getValue());
                });
    }
}
