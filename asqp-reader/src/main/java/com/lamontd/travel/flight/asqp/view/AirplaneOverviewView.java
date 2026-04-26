package com.lamontd.travel.flight.asqp.view;

import com.lamontd.travel.flight.asqp.index.FlightDataIndex;
import com.lamontd.travel.flight.asqp.model.ASQPFlightRecord;
import com.lamontd.travel.flight.mapper.CarrierCodeMapper;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Renders a high-level overview for a specific tail number
 * Shows: tail number, days active, total miles traveled, date range
 */
public class AirplaneOverviewView {

    public void render(FlightDataIndex index, Scanner scanner) {
        System.out.println("\n" + "=".repeat(50));
        System.out.println("AIRPLANE OVERVIEW");
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

        // Filter out cancelled flights and calculate statistics
        List<ASQPFlightRecord> operatedFlights = allPlaneFlights.stream()
                .filter(r -> !r.isCancelled())
                .toList();

        if (operatedFlights.isEmpty()) {
            System.out.println("\nNo operated flights found for tail number: " + tailNumber);
            System.out.println("All " + allPlaneFlights.size() + " flights were cancelled.");
            return;
        }

        // Calculate days active
        TreeSet<LocalDate> activeDays = new TreeSet<>();
        for (ASQPFlightRecord flight : operatedFlights) {
            activeDays.add(flight.getDepartureDate());
        }

        // Calculate total miles traveled
        double totalMiles = 0;
        for (ASQPFlightRecord flight : operatedFlights) {
            totalMiles += index.getRouteDistance(flight.getOrigin(), flight.getDestination());
        }

        // Find date range
        LocalDate firstFlight = activeDays.first();
        LocalDate lastFlight = activeDays.last();

        // Determine carrier(s) for this tail number
        CarrierCodeMapper carrierMapper = CarrierCodeMapper.getDefault();
        Map<String, Long> carrierCounts = operatedFlights.stream()
                .collect(Collectors.groupingBy(
                        ASQPFlightRecord::getCarrierCode,
                        Collectors.counting()
                ));

        // Display overview
        System.out.println("\n" + "-".repeat(50));
        System.out.println("Tail Number: " + tailNumber);
        System.out.println("-".repeat(50));

        // Display carrier information
        if (carrierCounts.size() == 1) {
            // Single carrier - simple display
            String carrierCode = carrierCounts.keySet().iterator().next();
            String carrierName = carrierMapper.getCarrierName(carrierCode);
            System.out.printf("Carrier: %s (%s)%n", carrierCode, carrierName);
        } else if (carrierCounts.size() > 1) {
            // Multi-carrier - show breakdown
            System.out.println("Carriers:");
            carrierCounts.entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                    .forEach(entry -> {
                        String code = entry.getKey();
                        long count = entry.getValue();
                        double percentage = (count * 100.0) / operatedFlights.size();
                        String name = carrierMapper.getCarrierName(code);
                        String label = (percentage > 50) ? "Primary" : "Secondary";
                        System.out.printf("  %s: %s (%s) - %d flight%s (%.1f%%)%n",
                                label, code, name, count, count == 1 ? "" : "s", percentage);
                    });
        }
        System.out.println();

        System.out.printf("\nOperational Summary:%n");
        System.out.printf("  Total Flights: %d%n", operatedFlights.size());
        System.out.printf("  Days Active: %d days%n", activeDays.size());
        System.out.printf("  Total Miles Traveled: %,.0f miles%n", totalMiles);
        System.out.printf("  Average Miles per Day: %,.0f miles%n", totalMiles / activeDays.size());
        System.out.printf("  Average Flights per Day: %.1f flights%n",
                (double) operatedFlights.size() / activeDays.size());

        System.out.printf("\nDate Range:%n");
        System.out.printf("  First Flight: %s%n", firstFlight);
        System.out.printf("  Last Flight: %s%n", lastFlight);

        long daySpan = java.time.temporal.ChronoUnit.DAYS.between(firstFlight, lastFlight) + 1;
        System.out.printf("  Span: %d days%n", daySpan);

        if (activeDays.size() < daySpan) {
            System.out.printf("  Utilization: %.1f%% (%d of %d days)%n",
                    (activeDays.size() * 100.0 / daySpan), activeDays.size(), daySpan);
        } else {
            System.out.printf("  Utilization: 100%% (all days in range)%n");
        }

        // Cancellation info if any
        long cancelledCount = allPlaneFlights.stream()
                .filter(ASQPFlightRecord::isCancelled)
                .count();

        if (cancelledCount > 0) {
            System.out.printf("\nCancelled Flights: %d (%.1f%%)%n",
                    cancelledCount,
                    (cancelledCount * 100.0 / allPlaneFlights.size()));
        }
    }
}
