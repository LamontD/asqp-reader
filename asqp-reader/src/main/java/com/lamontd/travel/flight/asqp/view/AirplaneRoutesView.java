package com.lamontd.travel.flight.asqp.view;

import com.lamontd.travel.flight.asqp.index.FlightDataIndex;
import com.lamontd.travel.flight.asqp.model.ASQPFlightRecord;
import com.lamontd.travel.flight.mapper.CarrierCodeMapper;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Renders daily route patterns for a tail number without detailed leg information
 * Shows only: date, route chain, and total distance
 */
public class AirplaneRoutesView {

    public void render(FlightDataIndex index, Scanner scanner) {
        System.out.println("\n" + "=".repeat(50));
        System.out.println("AIRPLANE ROUTES");
        System.out.println("=".repeat(50));

        System.out.print("\nEnter tail number: ");
        String tailNumber = scanner.nextLine().trim().toUpperCase();

        if (tailNumber.isEmpty()) {
            return;
        }

        // Use indexed lookup
        List<ASQPFlightRecord> allPlaneFlights = index.getByTailNumber(tailNumber);

        if (allPlaneFlights.isEmpty()) {
            System.out.println("\nNo flights found for tail number: " + tailNumber);
            return;
        }

        // Filter out cancelled and sort
        List<ASQPFlightRecord> planeFlights = allPlaneFlights.stream()
                .filter(r -> !r.isCancelled())
                .sorted(Comparator.comparing(ASQPFlightRecord::getDepartureDate)
                        .thenComparing(r -> r.getGateDeparture().orElse(LocalTime.MIN)))
                .toList();

        if (planeFlights.isEmpty()) {
            System.out.println("\nNo operated flights found for tail number: " + tailNumber);
            return;
        }

        // Determine carrier(s) for this tail number
        CarrierCodeMapper carrierMapper = CarrierCodeMapper.getDefault();
        Map<String, Long> carrierCounts = planeFlights.stream()
                .collect(Collectors.groupingBy(
                        ASQPFlightRecord::getCarrierCode,
                        Collectors.counting()
                ));

        System.out.println("\n" + "-".repeat(50));
        System.out.println("Tail Number: " + tailNumber);

        // Display carrier information
        if (carrierCounts.size() == 1) {
            String carrierCode = carrierCounts.keySet().iterator().next();
            String carrierName = carrierMapper.getCarrierName(carrierCode);
            System.out.printf("Carrier: %s (%s)%n", carrierCode, carrierName);
        } else if (carrierCounts.size() > 1) {
            System.out.print("Carriers: ");
            String carrierList = carrierCounts.entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                    .map(entry -> {
                        String code = entry.getKey();
                        String name = carrierMapper.getCarrierName(code);
                        return code + " (" + name + ")";
                    })
                    .collect(Collectors.joining(", "));
            System.out.println(carrierList);
        }

        System.out.printf("Total Active Days: %d%n",
                planeFlights.stream()
                        .map(ASQPFlightRecord::getDepartureDate)
                        .distinct()
                        .count());
        System.out.println("-".repeat(50));

        // Group flights by departure date
        Map<LocalDate, List<ASQPFlightRecord>> flightsByDate = planeFlights.stream()
                .collect(Collectors.groupingBy(
                        ASQPFlightRecord::getDepartureDate,
                        TreeMap::new,
                        Collectors.toList()
                ));

        System.out.println("\nDaily Route History:");

        for (Map.Entry<LocalDate, List<ASQPFlightRecord>> entry : flightsByDate.entrySet()) {
            LocalDate date = entry.getKey();
            List<ASQPFlightRecord> dailyFlights = entry.getValue();

            // Sort by departure time
            dailyFlights.sort(Comparator.comparing(r -> r.getGateDeparture().orElse(LocalTime.MIN)));

            // Build the route chain
            StringBuilder route = new StringBuilder();
            double totalDistance = 0;
            for (int i = 0; i < dailyFlights.size(); i++) {
                ASQPFlightRecord flight = dailyFlights.get(i);
                if (i == 0) {
                    route.append(flight.getOrigin());
                }
                route.append(" -> ").append(flight.getDestination());
                totalDistance += index.getRouteDistance(flight.getOrigin(), flight.getDestination());
            }

            System.out.printf("  %s: %s (%,.0f miles)%n", date, route, totalDistance);
        }
    }
}
