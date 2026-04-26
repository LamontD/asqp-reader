package com.lamontd.travel.flight.schedule.controller;

import com.lamontd.travel.flight.mapper.AirportCodeMapper;
import com.lamontd.travel.flight.schedule.index.AllDatesRouteIndex;
import com.lamontd.travel.flight.schedule.index.ScheduleDateRouteIndex;
import com.lamontd.travel.flight.schedule.index.ScheduleFlightIndex;
import com.lamontd.travel.flight.service.RouteGraphService;
import org.jgrapht.GraphPath;
import org.jgrapht.graph.DefaultWeightedEdge;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Controller for network analysis with date-specific or all-dates graph building.
 */
public class NetworkAnalysisController {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private RouteGraphService graphService;
    private LocalDate selectedDate; // null = all dates
    private final ScheduleFlightIndex scheduleIndex;

    public NetworkAnalysisController(ScheduleFlightIndex scheduleIndex) {
        this.scheduleIndex = scheduleIndex;
    }

    public void display(Scanner scanner) {
        // Prompt for initial date selection
        if (!selectDate(scanner)) {
            return; // User canceled
        }

        boolean running = true;
        while (running) {
            displayMenu();
            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1":
                    if (!selectDate(scanner)) {
                        running = false;
                    }
                    break;
                case "2":
                    showNetworkStatistics();
                    break;
                case "3":
                    findShortestPath(scanner);
                    break;
                case "4":
                    findReachableAirports(scanner);
                    break;
                case "5":
                    running = false;
                    break;
                default:
                    System.out.println("\nInvalid option. Please select 1-5.");
            }
        }
    }

    private void displayMenu() {
        System.out.println("\n" + "=".repeat(80));
        if (selectedDate == null) {
            System.out.println("NETWORK ANALYSIS (All Dates)");
        } else {
            System.out.println("NETWORK ANALYSIS (Date: " + selectedDate + ")");
        }
        System.out.println("=".repeat(80));
        System.out.println("1. Change Analysis Date");
        System.out.println("2. Network Statistics");
        System.out.println("3. Find Shortest Path");
        System.out.println("4. Find Reachable Airports");
        System.out.println("5. Return to Main Menu");
        System.out.println("=".repeat(80));
        System.out.print("Select option (1-5): ");
    }

    private boolean selectDate(Scanner scanner) {
        Set<LocalDate> availableDates = scheduleIndex.getAvailableDates();

        if (availableDates.isEmpty()) {
            System.out.println("\n✗ No flight data available.");
            return false;
        }

        Optional<LocalDate> minDate = scheduleIndex.getMinDate();
        Optional<LocalDate> maxDate = scheduleIndex.getMaxDate();

        System.out.println("\n" + "=".repeat(80));
        System.out.println("SELECT ANALYSIS DATE");
        System.out.println("=".repeat(80));
        if (minDate.isPresent() && maxDate.isPresent()) {
            System.out.printf("Available dates: %s to %s (%d unique dates)%n",
                    minDate.get(), maxDate.get(), availableDates.size());
        }
        System.out.print("Enter date (yyyy-MM-dd) or 'all' for all dates: ");

        String input = scanner.nextLine().trim().toLowerCase();

        if (input.equals("all")) {
            selectedDate = null;
            rebuildGraph();
            return true;
        }

        try {
            LocalDate date = LocalDate.parse(input, DATE_FORMATTER);
            if (!availableDates.contains(date)) {
                System.out.println("\n⚠ No flights found on " + date);
                System.out.print("Continue anyway? (y/n): ");
                String confirm = scanner.nextLine().trim().toLowerCase();
                if (!confirm.equals("y")) {
                    return false;
                }
            }
            selectedDate = date;
            rebuildGraph();
            return true;
        } catch (DateTimeParseException e) {
            System.out.println("\n✗ Invalid date format. Please use yyyy-MM-dd");
            return false;
        }
    }

    private void rebuildGraph() {
        System.out.print("\nBuilding route network");
        if (selectedDate != null) {
            System.out.print(" for " + selectedDate);
        }
        System.out.print("...");

        long startTime = System.currentTimeMillis();

        if (selectedDate == null) {
            graphService = new RouteGraphService(new AllDatesRouteIndex(scheduleIndex));
        } else {
            graphService = new RouteGraphService(new ScheduleDateRouteIndex(scheduleIndex, selectedDate));
        }

        long buildTime = System.currentTimeMillis() - startTime;
        System.out.printf(" done (%d ms)%n", buildTime);
    }

    private void showNetworkStatistics() {
        AirportCodeMapper airportMapper = AirportCodeMapper.getDefault();
        RouteGraphService.NetworkStats stats = graphService.getNetworkStats();

        System.out.println("\n" + "=".repeat(80));
        System.out.println("NETWORK STATISTICS");
        if (selectedDate != null) {
            System.out.println("Date: " + selectedDate);
        } else {
            System.out.println("All Dates Combined");
        }
        System.out.println("=".repeat(80));

        System.out.printf("%nNetwork Size:%n");
        System.out.printf("  Airports (nodes): %,d%n", stats.airportCount);
        System.out.printf("  Routes (edges): %,d%n", stats.routeCount);

        System.out.printf("%nConnectivity:%n");
        System.out.printf("  Average connections per airport: %.1f%n", stats.degreeStats.getAverage());
        System.out.printf("  Min connections: %d%n", (int) stats.degreeStats.getMin());
        System.out.printf("  Max connections: %d%n", (int) stats.degreeStats.getMax());

        System.out.println("\nTop 10 Hub Airports (by number of routes):");
        for (int i = 0; i < Math.min(10, stats.topHubs.size()); i++) {
            var hub = stats.topHubs.get(i);
            String airport = hub.getKey();
            String city = airportMapper.getAirportCity(airport);
            int connections = hub.getValue();

            System.out.printf("  %2d. %s (%s): %d routes%n", i + 1, airport, city, connections);
        }
    }

    private void findShortestPath(Scanner scanner) {
        AirportCodeMapper airportMapper = AirportCodeMapper.getDefault();

        System.out.print("\nEnter origin airport code: ");
        String origin = scanner.nextLine().trim().toUpperCase();
        if (!airportMapper.hasAirport(origin)) {
            System.out.println("\n✗ Invalid airport code: " + origin);
            System.out.println("Airport not found in database.");
            return;
        }

        System.out.print("Enter destination airport code: ");
        String destination = scanner.nextLine().trim().toUpperCase();
        if (!airportMapper.hasAirport(destination)) {
            System.out.println("\n✗ Invalid airport code: " + destination);
            System.out.println("Airport not found in database.");
            return;
        }

        if (origin.isEmpty() || destination.isEmpty()) {
            System.out.println("\nBoth origin and destination are required.");
            return;
        }

        if (origin.equals(destination)) {
            System.out.println("\nOrigin and destination are the same!");
            return;
        }

        GraphPath<String, DefaultWeightedEdge> path = graphService.findShortestPath(origin, destination);

        if (path == null) {
            System.out.println("\n✗ No route found between " + origin + " and " + destination);
            return;
        }

        System.out.println("\n" + "=".repeat(80));
        System.out.println("SHORTEST PATH FOUND");
        System.out.println("=".repeat(80));

        List<String> vertexList = path.getVertexList();
        double totalDistance = path.getWeight();

        System.out.printf("%nRoute: %s%n", String.join(" → ", vertexList));
        System.out.printf("Total Distance: %.0f miles%n", totalDistance);
        System.out.printf("Number of Segments: %d%n", vertexList.size() - 1);

        System.out.println("\nSegment Details:");
        for (int i = 0; i < vertexList.size() - 1; i++) {
            String from = vertexList.get(i);
            String to = vertexList.get(i + 1);
            String fromCity = airportMapper.getAirportCity(from);
            String toCity = airportMapper.getAirportCity(to);

            DefaultWeightedEdge edge = graphService.getGraph().getEdge(from, to);
            double segmentDistance = graphService.getGraph().getEdgeWeight(edge);

            System.out.printf("  %d. %s (%s) → %s (%s): %.0f miles%n",
                    i + 1, from, fromCity, to, toCity, segmentDistance);
        }
    }

    private void findReachableAirports(Scanner scanner) {
        AirportCodeMapper airportMapper = AirportCodeMapper.getDefault();

        System.out.print("\nEnter origin airport code: ");
        String origin = scanner.nextLine().trim().toUpperCase();
        if (!airportMapper.hasAirport(origin)) {
            System.out.println("\n✗ Invalid airport code: " + origin);
            System.out.println("Airport not found in database.");
            return;
        }

        if (origin.isEmpty()) {
            System.out.println("\nOrigin airport is required.");
            return;
        }

        System.out.print("Enter maximum layovers (0=direct, press Enter for unlimited): ");
        String layoverInput = scanner.nextLine().trim();

        if (layoverInput.isEmpty()) {
            Set<String> reachable = graphService.getReachableAirports(origin);
            displayUnlimitedReachable(origin, reachable, airportMapper);
        } else {
            try {
                int maxLayovers = Integer.parseInt(layoverInput);
                if (maxLayovers < 0) {
                    System.out.println("\nLayovers must be 0 or greater.");
                    return;
                }
                Map<String, Integer> reachable = graphService.getReachableAirportsWithLayoverCount(origin, maxLayovers);
                displayLimitedReachable(origin, maxLayovers, reachable, airportMapper);
            } catch (NumberFormatException e) {
                System.out.println("\nInvalid number format.");
            }
        }
    }

    private void displayUnlimitedReachable(String origin, Set<String> reachable, AirportCodeMapper mapper) {
        if (reachable.isEmpty()) {
            System.out.println("\n✗ No reachable airports from " + origin);
            return;
        }

        System.out.println("\n" + "=".repeat(80));
        System.out.printf("AIRPORTS REACHABLE FROM %s (%s)%n", origin, mapper.getAirportCity(origin));
        System.out.println("=".repeat(80));
        System.out.printf("Total: %d airports%n%n", reachable.size());

        reachable.stream().sorted().forEach(code ->
            System.out.printf("  %s (%s)%n", code, mapper.getAirportCity(code))
        );
    }

    private void displayLimitedReachable(String origin, int maxLayovers, Map<String, Integer> reachable, AirportCodeMapper mapper) {
        if (reachable.isEmpty()) {
            System.out.printf("\n✗ No airports reachable from %s within %d layover(s)%n", origin, maxLayovers);
            return;
        }

        System.out.println("\n" + "=".repeat(80));
        System.out.printf("AIRPORTS REACHABLE FROM %s WITH MAX %d LAYOVER%s%n",
                origin, maxLayovers, maxLayovers == 1 ? "" : "S");
        System.out.println("=".repeat(80));

        Map<Integer, List<String>> byLayoverCount = reachable.entrySet().stream()
                .collect(Collectors.groupingBy(Map.Entry::getValue,
                        Collectors.mapping(Map.Entry::getKey, Collectors.toList())));

        for (int layoverCount = 0; layoverCount <= maxLayovers; layoverCount++) {
            List<String> airports = byLayoverCount.get(layoverCount);
            if (airports == null || airports.isEmpty()) continue;

            airports.sort(String::compareTo);
            String label = layoverCount == 0 ? "Direct" : layoverCount + " Layover" + (layoverCount > 1 ? "s" : "");
            System.out.printf("%n%s: %d airport%s%n", label, airports.size(), airports.size() == 1 ? "" : "s");

            for (String airport : airports) {
                System.out.printf("  %s (%s)%n", airport, mapper.getAirportCity(airport));
            }
        }

        System.out.printf("%nTotal: %d airport%s%n", reachable.size(), reachable.size() == 1 ? "" : "s");
    }
}
