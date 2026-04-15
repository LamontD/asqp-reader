package com.lamontd.travel.flight.asqp.service;

import com.lamontd.travel.flight.util.FlightDataIndex;
import com.lamontd.travel.flight.model.ASQPFlightRecord;
import org.jgrapht.GraphPath;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class RouteGraphServiceTest {

    private RouteGraphService graphService;
    private FlightDataIndex index;

    @BeforeEach
    void setUp() {
        // Create sample flight data with a simple network:
        // JFK -> BOS (direct)
        // JFK -> LAX (direct)
        // BOS -> ORD (direct)
        // LAX -> ORD (direct)
        // So JFK -> ORD has two possible paths: JFK->BOS->ORD or JFK->LAX->ORD

        List<ASQPFlightRecord> records = new ArrayList<>();
        LocalDate date = LocalDate.of(2025, 1, 15);
        LocalTime time = LocalTime.of(10, 0);

        // JFK -> BOS (190 miles)
        records.add(ASQPFlightRecord.builder()
                .carrierCode("AA")
                .flightNumber("100")
                .origin("JFK")
                .destination("BOS")
                .departureDate(date)
                .scheduledOagDeparture(time)
                .scheduledCrsDeparture(time)
                .scheduledArrival(time.plusHours(1))
                .scheduledCrsArrival(time.plusHours(1))
                .tailNumber("N12345")
                .build());

        // JFK -> LAX (2475 miles)
        records.add(ASQPFlightRecord.builder()
                .carrierCode("AA")
                .flightNumber("200")
                .origin("JFK")
                .destination("LAX")
                .departureDate(date)
                .scheduledOagDeparture(time)
                .scheduledCrsDeparture(time)
                .scheduledArrival(time.plusHours(5))
                .scheduledCrsArrival(time.plusHours(5))
                .tailNumber("N23456")
                .build());

        // BOS -> ORD (864 miles)
        records.add(ASQPFlightRecord.builder()
                .carrierCode("AA")
                .flightNumber("300")
                .origin("BOS")
                .destination("ORD")
                .departureDate(date)
                .scheduledOagDeparture(time)
                .scheduledCrsDeparture(time)
                .scheduledArrival(time.plusHours(2))
                .scheduledCrsArrival(time.plusHours(2))
                .tailNumber("N34567")
                .build());

        // LAX -> ORD (1745 miles)
        records.add(ASQPFlightRecord.builder()
                .carrierCode("AA")
                .flightNumber("400")
                .origin("LAX")
                .destination("ORD")
                .departureDate(date)
                .scheduledOagDeparture(time)
                .scheduledCrsDeparture(time)
                .scheduledArrival(time.plusHours(3))
                .scheduledCrsArrival(time.plusHours(3))
                .tailNumber("N45678")
                .build());

        index = new FlightDataIndex(records);
        graphService = new RouteGraphService(index);
    }

    @Test
    void testGraphCreation() {
        assertNotNull(graphService);
        assertNotNull(graphService.getGraph());

        // Should have 4 airports as vertices
        assertEquals(4, graphService.getGraph().vertexSet().size());

        // Should have 4 edges (one for each flight route)
        assertEquals(4, graphService.getGraph().edgeSet().size());
    }

    @Test
    void testDirectRoute() {
        // JFK to LAX is a direct route
        GraphPath<String, DefaultWeightedEdge> path = graphService.findShortestPath("JFK", "LAX");

        assertNotNull(path);
        assertEquals(2, path.getVertexList().size()); // JFK, LAX
        assertEquals("JFK", path.getVertexList().get(0));
        assertEquals("LAX", path.getVertexList().get(1));
        assertTrue(path.getWeight() > 2400 && path.getWeight() < 2500); // ~2475 miles
    }

    @Test
    void testMultiHopRoute() {
        // JFK to ORD should find the shortest path
        // Via BOS: 190 + 864 = 1054 miles (shorter)
        // Via LAX: 2475 + 1745 = 4220 miles (longer)

        GraphPath<String, DefaultWeightedEdge> path = graphService.findShortestPath("JFK", "ORD");

        assertNotNull(path);
        assertEquals(3, path.getVertexList().size()); // JFK, BOS, ORD
        assertEquals("JFK", path.getVertexList().get(0));
        assertEquals("BOS", path.getVertexList().get(1));
        assertEquals("ORD", path.getVertexList().get(2));
        assertTrue(path.getWeight() > 1000 && path.getWeight() < 1100); // ~1054 miles
    }

    @Test
    void testReverseRoute() {
        // Graph is undirected, so ORD to JFK should also work
        GraphPath<String, DefaultWeightedEdge> path = graphService.findShortestPath("ORD", "JFK");

        assertNotNull(path);
        // Should go ORD -> BOS -> JFK (reverse of the shortest JFK -> ORD path)
        assertEquals(3, path.getVertexList().size());
        assertEquals("ORD", path.getVertexList().get(0));
        assertEquals("BOS", path.getVertexList().get(1));
        assertEquals("JFK", path.getVertexList().get(2));
    }

    @Test
    void testSameOriginAndDestination() {
        // JGraphT returns a path with just the source vertex for same source/target
        GraphPath<String, DefaultWeightedEdge> path = graphService.findShortestPath("JFK", "JFK");

        assertNotNull(path);
        assertEquals(1, path.getVertexList().size());
        assertEquals(0.0, path.getWeight());
    }

    @Test
    void testReachableAirports() {
        // From JFK, should be able to reach BOS, LAX, and ORD
        Set<String> reachable = graphService.getReachableAirports("JFK");

        assertEquals(3, reachable.size());
        assertTrue(reachable.contains("BOS"));
        assertTrue(reachable.contains("LAX"));
        assertTrue(reachable.contains("ORD"));
    }

    @Test
    void testNetworkStats() {
        RouteGraphService.NetworkStats stats = graphService.getNetworkStats();

        assertNotNull(stats);
        assertEquals(4, stats.airportCount);
        assertEquals(4, stats.routeCount);

        // In an undirected graph, degree counts edges in both directions
        // ORD and LAX each have 2 connections (since edges are bidirectional)
        assertTrue(stats.degreeStats.getMax() >= 2);
        assertTrue(stats.degreeStats.getAverage() > 0);

        // Top hubs (by degree in undirected graph)
        assertFalse(stats.topHubs.isEmpty());
        // ORD has connections to BOS and LAX = degree 2
        // JFK has connections to BOS and LAX = degree 2
        // Either could be first depending on iteration order
        assertTrue(stats.topHubs.get(0).getValue() >= 2);
    }

    @Test
    void testInvalidAirport() {
        GraphPath<String, DefaultWeightedEdge> path = graphService.findShortestPath("JFK", "XXX");
        assertNull(path);

        Set<String> reachable = graphService.getReachableAirports("XXX");
        assertTrue(reachable.isEmpty());
    }
}
