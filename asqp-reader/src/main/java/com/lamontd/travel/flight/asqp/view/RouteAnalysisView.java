package com.lamontd.travel.flight.asqp.view;

import com.lamontd.travel.flight.asqp.index.FlightDataIndex;
import com.lamontd.travel.flight.mapper.AirportCodeMapper;
import com.lamontd.travel.flight.service.RouteGraphService;
import org.jgrapht.GraphPath;
import org.jgrapht.graph.DefaultWeightedEdge;

import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

/**
 * View for route graph analysis and shortest path queries
 */
public class RouteAnalysisView implements ViewRenderer {

    private RouteGraphService graphService;

    @Override
    public void render(FlightDataIndex index, Scanner scanner) {
        System.out.println("\n" + "=".repeat(50));
        System.out.println("ROUTE NETWORK ANALYSIS");
        System.out.println("=".repeat(50));

        // Build graph if not already built
        if (graphService == null) {
            System.out.println("\nBuilding route network graph...");
            long startTime = System.currentTimeMillis();
            graphService = new RouteGraphService(index);
            long buildTime = System.currentTimeMillis() - startTime;
            System.out.printf("Graph built in %d ms%n", buildTime);
        }

        boolean running = true;
        while (running) {
            System.out.println("\n" + "-".repeat(50));
            System.out.println("Route Analysis Options:");
            System.out.println("1. Find Shortest Path");
            System.out.println("2. View Network Statistics");
            System.out.println("3. Find Reachable Airports");
            System.out.println("4. Return to Main Menu");
            System.out.println("-".repeat(50));
            System.out.print("Select option (1-4): ");

            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1":
                    findShortestPath(scanner);
                    break;
                case "2":
                    showNetworkStatistics();
                    break;
                case "3":
                    findReachableAirports(scanner);
                    break;
                case "4":
                    running = false;
                    break;
                default:
                    System.out.println("\nInvalid option. Please select 1-4.");
            }
        }
    }

    private void findShortestPath(Scanner scanner) {
        AirportCodeMapper airportMapper = AirportCodeMapper.getDefault();

        System.out.print("\nEnter origin airport code: ");
        String origin = scanner.nextLine().trim().toUpperCase();

        System.out.print("Enter destination airport code: ");
        String destination = scanner.nextLine().trim().toUpperCase();

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

        System.out.println("\n" + "=".repeat(50));
        System.out.println("SHORTEST PATH FOUND");
        System.out.println("=".repeat(50));

        List<String> vertexList = path.getVertexList();
        double totalDistance = path.getWeight();

        System.out.printf("\nRoute: %s%n", String.join(" → ", vertexList));
        System.out.printf("Total Distance: %.0f miles%n", totalDistance);
        System.out.printf("Number of Segments: %d%n", vertexList.size() - 1);

        // Show detailed segment information
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

        // Compare with direct distance if a direct route exists
        DefaultWeightedEdge directEdge = graphService.getGraph().getEdge(origin, destination);
        if (directEdge != null && vertexList.size() > 2) {
            double directDistance = graphService.getGraph().getEdgeWeight(directEdge);
            System.out.printf("\nDirect route distance: %.0f miles%n", directDistance);
            System.out.printf("Additional distance via connections: %.0f miles (%.1f%% longer)%n",
                    totalDistance - directDistance,
                    ((totalDistance / directDistance) - 1) * 100);
        } else if (vertexList.size() > 2) {
            System.out.println("\nNote: No direct route exists between these airports.");
        }
    }

    private void showNetworkStatistics() {
        AirportCodeMapper airportMapper = AirportCodeMapper.getDefault();
        RouteGraphService.NetworkStats stats = graphService.getNetworkStats();

        System.out.println("\n" + "=".repeat(50));
        System.out.println("NETWORK STATISTICS");
        System.out.println("=".repeat(50));

        System.out.printf("\nNetwork Size:%n");
        System.out.printf("  Airports (nodes): %,d%n", stats.airportCount);
        System.out.printf("  Routes (edges): %,d%n", stats.routeCount);

        System.out.printf("\nConnectivity:%n");
        System.out.printf("  Average connections per airport: %.1f%n", stats.degreeStats.getAverage());
        System.out.printf("  Min connections: %d%n", (int) stats.degreeStats.getMin());
        System.out.printf("  Max connections: %d%n", (int) stats.degreeStats.getMax());

        System.out.println("\nTop 10 Hub Airports (by number of routes):");
        for (int i = 0; i < stats.topHubs.size(); i++) {
            Map.Entry<String, Integer> hub = stats.topHubs.get(i);
            String airport = hub.getKey();
            String city = airportMapper.getAirportCity(airport);
            int connections = hub.getValue();

            System.out.printf("  %2d. %s (%s): %d routes%n",
                    i + 1, airport, city, connections);
        }
    }

    private void findReachableAirports(Scanner scanner) {
        AirportCodeMapper airportMapper = AirportCodeMapper.getDefault();

        System.out.print("\nEnter origin airport code: ");
        String origin = scanner.nextLine().trim().toUpperCase();

        if (origin.isEmpty()) {
            System.out.println("\nOrigin airport is required.");
            return;
        }

        Set<String> reachable = graphService.getReachableAirports(origin);

        if (reachable.isEmpty()) {
            System.out.println("\n✗ No reachable airports found from " + origin);
            return;
        }

        String originCity = airportMapper.getAirportCity(origin);
        System.out.println("\n" + "=".repeat(50));
        System.out.printf("AIRPORTS REACHABLE FROM %s (%s)%n", origin, originCity);
        System.out.println("=".repeat(50));
        System.out.printf("\nTotal: %d airports%n", reachable.size());

        // Group by first letter for easier reading
        Map<Character, List<String>> grouped = reachable.stream()
                .sorted()
                .collect(java.util.stream.Collectors.groupingBy(code -> code.charAt(0)));

        grouped.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    System.out.printf("\n%c: ", entry.getKey());
                    System.out.println(entry.getValue().stream()
                            .map(code -> code + " (" + airportMapper.getAirportCity(code) + ")")
                            .collect(java.util.stream.Collectors.joining(", ")));
                });
    }
}
