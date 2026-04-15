package com.lamontd.travel.flight.asqp.view;

import com.lamontd.travel.flight.util.FlightDataIndex;
import com.lamontd.travel.flight.mapper.AirportCodeMapper;
import com.lamontd.travel.flight.mapper.CarrierCodeMapper;
import com.lamontd.travel.flight.model.ASQPFlightRecord;

import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.stream.Collectors;

/**
 * Renders the carrier view screen
 */
public class CarrierView implements ViewRenderer {

    @Override
    public void render(FlightDataIndex index, Scanner scanner) {
        CarrierCodeMapper carrierMapper = CarrierCodeMapper.getDefault();
        AirportCodeMapper airportMapper = AirportCodeMapper.getDefault();

        System.out.println("\n" + "=".repeat(50));
        System.out.println("CARRIER VIEW");
        System.out.println("=".repeat(50));

        // Use pre-computed carrier counts from index
        System.out.println("\nFlights by Carrier:");
        index.carrierCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .forEach(entry -> {
                    String carrierName = carrierMapper.getCarrierName(entry.getKey());
                    System.out.println("  " + entry.getKey() + " (" + carrierName + "): " +
                            entry.getValue() + " flights");
                });

        System.out.print("\nEnter carrier code (or press Enter to cancel): ");
        String carrierCode = scanner.nextLine().trim().toUpperCase();

        if (carrierCode.isEmpty()) {
            return;
        }

        // Use indexed lookup - O(1) instead of O(n)
        List<ASQPFlightRecord> carrierFlights = index.getByCarrier(carrierCode);

        if (carrierFlights.isEmpty()) {
            System.out.println("\nNo flights found for carrier code: " + carrierCode);
            return;
        }

        String carrierName = carrierMapper.getCarrierName(carrierCode);
        System.out.println("\n" + "-".repeat(50));
        System.out.println("Carrier: " + carrierCode + " (" + carrierName + ")");
        System.out.println("Total Flights: " + carrierFlights.size());
        System.out.println("-".repeat(50));

        // Group by origin airport
        Map<String, Long> airportCounts = carrierFlights.stream()
                .collect(Collectors.groupingBy(
                        ASQPFlightRecord::getOrigin,
                        Collectors.counting()
                ));

        System.out.println("\nFlights by Origin Airport (largest to smallest):");
        airportCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .forEach(entry -> {
                    String airport = entry.getKey();
                    String city = airportMapper.getAirportCity(airport);
                    long count = entry.getValue();
                    String bar = ViewUtils.createBar(count, 40,
                            airportCounts.values().stream().mapToLong(Long::longValue).max().orElse(1));
                    System.out.printf("  %s (%s): %3d flights %s%n",
                            airport, city, count, bar);
                });
    }
}
