package com.lamontd.travel.flight.service;

import com.lamontd.travel.flight.index.RouteIndex;
import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for building and analyzing route graphs using JGraphT
 */
public class RouteGraphService {

    private final Graph<String, DefaultWeightedEdge> routeGraph;
    private final RouteIndex index;
    private final DijkstraShortestPath<String, DefaultWeightedEdge> dijkstra;

    /**
     * Builds a weighted graph from the route index
     * Nodes = airports, Edges = routes, Weights = great circle distances
     */
    public RouteGraphService(RouteIndex index) {
        this.index = index;
        this.routeGraph = new SimpleWeightedGraph<>(DefaultWeightedEdge.class);
        buildGraph();
        this.dijkstra = new DijkstraShortestPath<>(routeGraph);
    }

    /**
     * Builds the route graph from route index
     */
    private void buildGraph() {
        // Get all unique airports from the index
        Set<String> airports = new HashSet<>();
        airports.addAll(index.getOriginAirports());
        airports.addAll(index.getDestinationAirports());

        // Add all airports as vertices
        airports.forEach(routeGraph::addVertex);

        // Add all routes as weighted edges
        for (String origin : index.getOriginAirports()) {
            for (String dest : index.getDestinationAirports()) {
                if (!origin.equals(dest)) {
                    double distance = index.getRouteDistance(origin, dest);
                    if (distance > 0 && routeGraph.containsVertex(origin) && routeGraph.containsVertex(dest)) {
                        DefaultWeightedEdge edge = routeGraph.addEdge(origin, dest);
                        if (edge != null) {
                            routeGraph.setEdgeWeight(edge, distance);
                        }
                    }
                }
            }
        }
    }

    /**
     * Finds the shortest path between two airports
     * @return GraphPath with route and total distance, or null if no path exists
     */
    public GraphPath<String, DefaultWeightedEdge> findShortestPath(String origin, String destination) {
        if (!routeGraph.containsVertex(origin) || !routeGraph.containsVertex(destination)) {
            return null;
        }
        return dijkstra.getPath(origin, destination);
    }

    /**
     * Finds all reachable airports from a given origin
     */
    public Set<String> getReachableAirports(String origin) {
        if (!routeGraph.containsVertex(origin)) {
            return Collections.emptySet();
        }

        Set<String> reachable = new HashSet<>();
        for (String destination : routeGraph.vertexSet()) {
            if (!origin.equals(destination) && dijkstra.getPath(origin, destination) != null) {
                reachable.add(destination);
            }
        }
        return reachable;
    }

    /**
     * Gets statistics about the route network
     */
    public NetworkStats getNetworkStats() {
        int vertices = routeGraph.vertexSet().size();
        int edges = routeGraph.edgeSet().size();

        // Calculate degree statistics (number of connections per airport)
        DoubleSummaryStatistics degreeStats = routeGraph.vertexSet().stream()
                .mapToDouble(routeGraph::degreeOf)
                .summaryStatistics();

        // Find most connected airports (hubs)
        List<Map.Entry<String, Integer>> hubs = routeGraph.vertexSet().stream()
                .collect(Collectors.toMap(
                        airport -> airport,
                        routeGraph::degreeOf
                ))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(10)
                .collect(Collectors.toList());

        return new NetworkStats(vertices, edges, degreeStats, hubs);
    }

    public Graph<String, DefaultWeightedEdge> getGraph() {
        return routeGraph;
    }

    /**
     * Network statistics data class
     */
    public static class NetworkStats {
        public final int airportCount;
        public final int routeCount;
        public final DoubleSummaryStatistics degreeStats;
        public final List<Map.Entry<String, Integer>> topHubs;

        public NetworkStats(int airportCount, int routeCount,
                          DoubleSummaryStatistics degreeStats,
                          List<Map.Entry<String, Integer>> topHubs) {
            this.airportCount = airportCount;
            this.routeCount = routeCount;
            this.degreeStats = degreeStats;
            this.topHubs = topHubs;
        }
    }
}
