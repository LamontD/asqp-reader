package com.lamontd.asqp.view;

import com.lamontd.asqp.index.FlightDataIndex;
import com.lamontd.asqp.mapper.AirportCodeMapper;
import com.lamontd.asqp.mapper.CancellationCodeMapper;
import com.lamontd.asqp.model.FlightRecord;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Renders the airport view screen
 */
public class AirportView implements ViewRenderer {

    @Override
    public void render(FlightDataIndex index, Scanner scanner) {
        AirportCodeMapper airportMapper = AirportCodeMapper.getDefault();
        CancellationCodeMapper cancellationMapper = CancellationCodeMapper.getDefault();

        System.out.println("\n" + "=".repeat(50));
        System.out.println("AIRPORT VIEW");
        System.out.println("=".repeat(50));

        System.out.print("\nEnter airport code: ");
        String airportCode = scanner.nextLine().trim().toUpperCase();

        if (airportCode.isEmpty()) {
            return;
        }

        // Use indexed lookup - O(1) instead of O(n)
        List<FlightRecord> airportFlights = index.getByOriginAirport(airportCode);

        if (airportFlights.isEmpty()) {
            System.out.println("\nNo flights found departing from airport: " + airportCode);
            return;
        }

        String city = airportMapper.getAirportCity(airportCode);
        System.out.println("\n" + "-".repeat(50));
        System.out.println("Airport: " + airportCode + " (" + city + ")");
        System.out.println("Total Departing Flights: " + airportFlights.size());
        System.out.println("-".repeat(50));

        // Group by date and cancellation status
        Map<LocalDate, Long> operatedByDate = airportFlights.stream()
                .filter(r -> !r.isCancelled())
                .collect(Collectors.groupingBy(
                        FlightRecord::getDepartureDate,
                        Collectors.counting()
                ));

        Map<LocalDate, Long> cancelledByDate = airportFlights.stream()
                .filter(FlightRecord::isCancelled)
                .collect(Collectors.groupingBy(
                        FlightRecord::getDepartureDate,
                        Collectors.counting()
                ));

        // Get all dates
        Set<LocalDate> allDates = new TreeSet<>();
        allDates.addAll(operatedByDate.keySet());
        allDates.addAll(cancelledByDate.keySet());

        long maxCount = Math.max(
                operatedByDate.values().stream().mapToLong(Long::longValue).max().orElse(1),
                cancelledByDate.values().stream().mapToLong(Long::longValue).max().orElse(1)
        );

        System.out.println("\nDaily Flight Histogram:");
        for (LocalDate date : allDates) {
            long operated = operatedByDate.getOrDefault(date, 0L);
            long cancelled = cancelledByDate.getOrDefault(date, 0L);

            System.out.println("\n  " + date + ":");
            String operatedBar = ViewUtils.createBar(operated, 40, maxCount);
            String cancelledBar = ViewUtils.createBar(cancelled, 40, maxCount);

            System.out.printf("    Operated:  %3d %s%n", operated, operatedBar);
            System.out.printf("    Cancelled: %3d %s%n", cancelled, cancelledBar);
        }

        // Summary statistics
        long totalOperated = operatedByDate.values().stream().mapToLong(Long::longValue).sum();
        long totalCancelled = cancelledByDate.values().stream().mapToLong(Long::longValue).sum();

        System.out.println("\n  Summary:");
        System.out.println("    Total Operated: " + totalOperated);
        System.out.println("    Total Cancelled: " + totalCancelled);
        System.out.printf("    Cancellation Rate: %.1f%%%n",
                (totalCancelled * 100.0 / (totalOperated + totalCancelled)));

        // Cancellation breakdown by reason
        if (totalCancelled > 0) {
            Map<String, Long> cancellationReasons = airportFlights.stream()
                    .filter(FlightRecord::isCancelled)
                    .collect(Collectors.groupingBy(
                            r -> r.getCancellationCode().orElse("Unknown"),
                            Collectors.counting()
                    ));

            System.out.println("\n  Cancellation Reasons:");
            cancellationReasons.entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                    .forEach(entry -> {
                        String description = cancellationMapper.getFullDescription(entry.getKey());
                        System.out.printf("    %s: %d (%.1f%%)%n",
                                description,
                                entry.getValue(),
                                (entry.getValue() * 100.0 / totalCancelled));
                    });
        }
    }
}
