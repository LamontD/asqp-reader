package com.lamontd.asqp.view;

import com.lamontd.asqp.index.FlightDataIndex;
import com.lamontd.asqp.mapper.AirportCodeMapper;
import com.lamontd.asqp.mapper.CancellationCodeMapper;
import com.lamontd.asqp.mapper.CarrierCodeMapper;
import com.lamontd.asqp.model.FlightRecord;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Renders the flight number view screen
 */
public class FlightView implements ViewRenderer {

    /**
     * Helper class to track route information
     */
    private static class RouteInfo {
        final String route;
        final double distance;
        int count;

        RouteInfo(String route, double distance) {
            this.route = route;
            this.distance = distance;
            this.count = 0;
        }
    }

    @Override
    public void render(FlightDataIndex index, Scanner scanner) {
        AirportCodeMapper airportMapper = AirportCodeMapper.getDefault();
        CarrierCodeMapper carrierMapper = CarrierCodeMapper.getDefault();
        CancellationCodeMapper cancellationMapper = CancellationCodeMapper.getDefault();

        System.out.println("\n" + "=".repeat(50));
        System.out.println("FLIGHT VIEW");
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

        // Use indexed lookup - O(1)
        List<FlightRecord> flightRecords = index.getByFlightNumber(carrierCode, flightNumber);

        if (flightRecords.isEmpty()) {
            System.out.println("\nNo flights found for: " + carrierCode + " " + flightNumber);
            return;
        }

        String carrierName = carrierMapper.getCarrierName(carrierCode);
        System.out.println("\n" + "-".repeat(50));
        System.out.println("Flight: " + carrierCode + " " + flightNumber + " (" + carrierName + ")");
        System.out.println("Total Flight Records: " + flightRecords.size());
        System.out.println("-".repeat(50));

        // Group by departure date
        Map<LocalDate, List<FlightRecord>> flightsByDate = flightRecords.stream()
                .collect(Collectors.groupingBy(
                        FlightRecord::getDepartureDate,
                        TreeMap::new,
                        Collectors.toList()
                ));

        // Route Overview - show all unique routes with distances
        System.out.println("\n" + "=".repeat(50));
        System.out.println("ROUTE OVERVIEW");
        System.out.println("=".repeat(50));

        Map<String, RouteInfo> routeInfoMap = new LinkedHashMap<>();
        for (List<FlightRecord> dailyLegs : flightsByDate.values()) {
            // Sort legs by departure time
            List<FlightRecord> sortedLegs = dailyLegs.stream()
                    .sorted(Comparator.comparing(r -> r.getScheduledCrsDeparture() != null ?
                            r.getScheduledCrsDeparture() : LocalTime.MIN))
                    .toList();

            // Build route string
            StringBuilder routeBuilder = new StringBuilder();
            double routeDistance = 0;
            for (int i = 0; i < sortedLegs.size(); i++) {
                FlightRecord leg = sortedLegs.get(i);
                if (i == 0) {
                    routeBuilder.append(leg.getOrigin());
                }
                routeBuilder.append(" -> ").append(leg.getDestination());
                routeDistance += index.getDistance(leg.getOrigin(), leg.getDestination());
            }

            String routeKey = routeBuilder.toString();
            RouteInfo info = routeInfoMap.get(routeKey);
            if (info == null) {
                info = new RouteInfo(routeKey, routeDistance);
                routeInfoMap.put(routeKey, info);
            }
            info.count++;
        }

        System.out.println("\nRoutes flown during this period:");
        for (RouteInfo routeInfo : routeInfoMap.values()) {
            System.out.printf("  %s: %.0f miles (%d time%s)%n",
                    routeInfo.route,
                    routeInfo.distance,
                    routeInfo.count,
                    routeInfo.count == 1 ? "" : "s");
        }

        System.out.println("\n" + "=".repeat(50));
        System.out.println("DAILY FLIGHT REPORT");
        System.out.println("=".repeat(50));

        for (Map.Entry<LocalDate, List<FlightRecord>> entry : flightsByDate.entrySet()) {
            LocalDate date = entry.getKey();
            List<FlightRecord> dailyLegs = entry.getValue();

            // Sort legs by departure time
            dailyLegs.sort(Comparator.comparing(r -> r.getGateDeparture().orElse(LocalTime.MIN)));

            // Check if cancelled
            boolean allCancelled = dailyLegs.stream().allMatch(FlightRecord::isCancelled);
            boolean anyCancelled = dailyLegs.stream().anyMatch(FlightRecord::isCancelled);

            System.out.print("\n  " + date + ":");

            // Build the route chain (show for both operated and cancelled)
            StringBuilder route = new StringBuilder();
            for (int i = 0; i < dailyLegs.size(); i++) {
                FlightRecord leg = dailyLegs.get(i);
                if (i == 0) {
                    route.append(leg.getOrigin());
                }
                route.append(" -> ").append(leg.getDestination());
            }

            if (allCancelled) {
                System.out.println(" CANCELLED");
                System.out.println("    Scheduled Route: " + route);
                if (!dailyLegs.isEmpty()) {
                    FlightRecord first = dailyLegs.get(0);
                    String reason = first.getCancellationCode()
                            .map(cancellationMapper::getFullDescription)
                            .orElse("Unknown reason");
                    System.out.println("    Reason: " + reason);
                }
            } else {
                System.out.println(" OPERATED");
                System.out.println("    Route: " + route);

                if (anyCancelled) {
                    long cancelledLegs = dailyLegs.stream().filter(FlightRecord::isCancelled).count();
                    System.out.println("    Note: " + cancelledLegs + " leg(s) cancelled");
                }
            }

            // Show detailed leg information (for both operated and cancelled)
            System.out.println("    Details:");
            for (int i = 0; i < dailyLegs.size(); i++) {
                FlightRecord leg = dailyLegs.get(i);
                String originCity = airportMapper.getAirportCity(leg.getOrigin());
                String destCity = airportMapper.getAirportCity(leg.getDestination());

                // Show actual times if operated, scheduled if cancelled
                String depTime, arrTime;
                if (leg.isCancelled()) {
                    depTime = leg.getScheduledCrsDeparture() != null ?
                            leg.getScheduledCrsDeparture().toString() : "--:--";
                    arrTime = leg.getScheduledCrsArrival() != null ?
                            leg.getScheduledCrsArrival().toString() : "--:--";
                } else {
                    depTime = leg.getGateDeparture().isPresent() ?
                            leg.getGateDeparture().get().toString() : "--:--";
                    arrTime = leg.getGateArrival().isPresent() ?
                            leg.getGateArrival().get().toString() : "--:--";
                }

                String status = leg.isCancelled() ? " [CANCELLED]" : "";
                String tailInfo = leg.getTailNumber() != null ? " (Tail: " + leg.getTailNumber() + ")" : "";

                System.out.printf("      Leg %d: %s (%s) [%s] -> %s (%s) [%s]%s%s%n",
                        i + 1,
                        leg.getOrigin(),
                        originCity,
                        depTime,
                        leg.getDestination(),
                        destCity,
                        arrTime,
                        tailInfo,
                        status);
            }
        }

        // Summary statistics
        long totalOperated = flightsByDate.values().stream()
                .filter(legs -> legs.stream().anyMatch(l -> !l.isCancelled()))
                .count();
        long totalCancelled = flightsByDate.values().stream()
                .filter(legs -> legs.stream().allMatch(FlightRecord::isCancelled))
                .count();

        // Calculate on-time performance (within 15 minutes of scheduled time)
        List<FlightRecord> operatedFlights = flightsByDate.values().stream()
                .flatMap(List::stream)
                .filter(r -> !r.isCancelled())
                .toList();

        long onTimeDepartures = operatedFlights.stream()
                .filter(r -> {
                    if (r.getScheduledCrsDeparture() == null || r.getGateDeparture().isEmpty()) {
                        return false;
                    }
                    LocalTime scheduled = r.getScheduledCrsDeparture();
                    LocalTime actual = r.getGateDeparture().get();
                    long minutesDiff = java.time.Duration.between(scheduled, actual).toMinutes();
                    return Math.abs(minutesDiff) <= 15;
                })
                .count();

        long onTimeArrivals = operatedFlights.stream()
                .filter(r -> {
                    if (r.getScheduledCrsArrival() == null || r.getGateArrival().isEmpty()) {
                        return false;
                    }
                    LocalTime scheduled = r.getScheduledCrsArrival();
                    LocalTime actual = r.getGateArrival().get();
                    long minutesDiff = java.time.Duration.between(scheduled, actual).toMinutes();
                    return Math.abs(minutesDiff) <= 15;
                })
                .count();

        long flightsWithDepData = operatedFlights.stream()
                .filter(r -> r.getScheduledCrsDeparture() != null && r.getGateDeparture().isPresent())
                .count();

        long flightsWithArrData = operatedFlights.stream()
                .filter(r -> r.getScheduledCrsArrival() != null && r.getGateArrival().isPresent())
                .count();

        System.out.println("\n" + "=".repeat(50));
        System.out.println("PERFORMANCE SUMMARY");
        System.out.println("=".repeat(50));

        System.out.println("\nOperational Stats:");
        System.out.println("  Total Days: " + flightsByDate.size());
        System.out.println("  Days Operated: " + totalOperated);
        System.out.println("  Days Cancelled: " + totalCancelled);
        if (flightsByDate.size() > 0) {
            System.out.printf("  Completion Rate: %.1f%%%n",
                    (totalOperated * 100.0 / flightsByDate.size()));
        }

        // On-time performance (within 15 minutes)
        if (flightsWithDepData > 0 || flightsWithArrData > 0) {
            System.out.println("\nOn-Time Performance (within 15 minutes):");
            if (flightsWithDepData > 0) {
                System.out.printf("  Departures: %d / %d (%.1f%%)%n",
                        onTimeDepartures, flightsWithDepData,
                        (onTimeDepartures * 100.0 / flightsWithDepData));
            }
            if (flightsWithArrData > 0) {
                System.out.printf("  Arrivals: %d / %d (%.1f%%)%n",
                        onTimeArrivals, flightsWithArrData,
                        (onTimeArrivals * 100.0 / flightsWithArrData));
            }
        }
    }
}
