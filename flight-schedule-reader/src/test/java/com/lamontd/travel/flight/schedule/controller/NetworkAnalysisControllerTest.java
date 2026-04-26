package com.lamontd.travel.flight.schedule.controller;

import com.lamontd.travel.flight.schedule.index.ScheduleFlightIndex;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;

class NetworkAnalysisControllerTest {
    private NetworkAnalysisController controller;
    private ScheduleFlightIndex index;
    private ControllerTestUtils.OutputCapture outputCapture;

    @BeforeEach
    void setUp() {
        index = ControllerTestUtils.createNetworkTestIndex();
        controller = new NetworkAnalysisController(index);
        outputCapture = new ControllerTestUtils.OutputCapture();
        outputCapture.start();
    }

    @AfterEach
    void tearDown() {
        outputCapture.stop();
    }

    @Test
    void testDateSelection_validDate() {
        Scanner scanner = ControllerTestUtils.createScanner(
            "2025-06-10",   // valid date
            "5"             // exit
        );

        controller.display(scanner);

        String output = outputCapture.getOutput();
        assertTrue(output.contains("NETWORK ANALYSIS"));
        assertTrue(output.contains("2025-06-10"));
    }

    @Test
    void testDateSelection_allDates() {
        Scanner scanner = ControllerTestUtils.createScanner(
            "all",          // all dates option
            "5"             // exit
        );

        controller.display(scanner);

        String output = outputCapture.getOutput();
        assertTrue(output.contains("All Dates"));
    }

    @Test
    void testDateSelection_invalidFormat() {
        Scanner scanner = ControllerTestUtils.createScanner(
            "06/10/2025",   // wrong format
            "5"             // exit after error
        );

        controller.display(scanner);

        String output = outputCapture.getOutput();
        assertTrue(output.contains("Invalid date format"));
    }

    @Test
    void testNetworkStatistics() {
        Scanner scanner = ControllerTestUtils.createScanner(
            "2025-06-10",
            "2",            // network statistics
            "5"             // exit
        );

        controller.display(scanner);

        String output = outputCapture.getOutput();
        assertTrue(output.contains("NETWORK STATISTICS"));
        assertTrue(output.contains("Airports"));
        assertTrue(output.contains("Routes"));
        assertTrue(output.contains("connections per airport"));
    }

    @Test
    void testTopHubsDisplayed() {
        Scanner scanner = ControllerTestUtils.createScanner(
            "2025-06-10",
            "2",            // network statistics
            "5"
        );

        controller.display(scanner);

        String output = outputCapture.getOutput();
        assertTrue(output.contains("Top") && output.contains("Hub"));
        assertTrue(output.contains("ORD") || output.contains("LAX")); // Should show hubs
    }

    @Test
    void testShortestPath_validRoute() {
        Scanner scanner = ControllerTestUtils.createScanner(
            "2025-06-10",
            "3",            // shortest path
            "JFK",          // origin
            "SFO",          // destination
            "5"             // exit
        );

        controller.display(scanner);

        String output = outputCapture.getOutput();
        assertTrue(output.contains("SHORTEST PATH"));
        assertTrue(output.contains("JFK"));
        assertTrue(output.contains("SFO"));
        assertTrue(output.contains("miles"));
    }

    @Test
    void testShortestPath_invalidOrigin() {
        Scanner scanner = ControllerTestUtils.createScanner(
            "2025-06-10",
            "3",
            "XXX",          // invalid origin
            "LAX",
            "5"
        );

        controller.display(scanner);

        String output = outputCapture.getOutput();
        assertTrue(output.contains("Invalid airport code: XXX"));
    }

    @Test
    void testShortestPath_invalidDestination() {
        Scanner scanner = ControllerTestUtils.createScanner(
            "2025-06-10",
            "3",
            "JFK",
            "ZZZ",          // invalid destination
            "5"
        );

        controller.display(scanner);

        String output = outputCapture.getOutput();
        assertTrue(output.contains("Invalid airport code: ZZZ"));
    }

    @Test
    void testShortestPath_sameAirport() {
        Scanner scanner = ControllerTestUtils.createScanner(
            "2025-06-10",
            "3",
            "JFK",
            "JFK",          // same as origin
            "5"
        );

        controller.display(scanner);

        String output = outputCapture.getOutput();
        assertTrue(output.contains("Origin and destination are the same"));
    }

    @Test
    void testShortestPath_noRouteExists() {
        Scanner scanner = ControllerTestUtils.createScanner(
            "2025-06-10",
            "3",
            "JFK",
            "ATL",          // Not in our test network
            "5"
        );

        controller.display(scanner);

        String output = outputCapture.getOutput();
        assertTrue(output.contains("No route found"));
    }

    @Test
    void testShortestPath_segmentDetails() {
        Scanner scanner = ControllerTestUtils.createScanner(
            "2025-06-10",
            "3",
            "JFK",
            "LAX",
            "5"
        );

        controller.display(scanner);

        String output = outputCapture.getOutput();
        assertTrue(output.contains("Segment"));
        assertTrue(output.contains("miles"));
        assertTrue(output.contains("Total Distance"));
    }

    @Test
    void testReachableAirports_unlimited() {
        Scanner scanner = ControllerTestUtils.createScanner(
            "2025-06-10",
            "4",            // reachable airports
            "JFK",          // origin
            "",             // unlimited layovers
            "5"
        );

        controller.display(scanner);

        String output = outputCapture.getOutput();
        assertTrue(output.contains("AIRPORTS REACHABLE"));
        assertTrue(output.contains("JFK"));
        assertTrue(output.contains("Total"));
    }

    @Test
    void testReachableAirports_withMaxLayovers() {
        Scanner scanner = ControllerTestUtils.createScanner(
            "2025-06-10",
            "4",
            "JFK",
            "1",            // max 1 layover
            "5"
        );

        controller.display(scanner);

        String output = outputCapture.getOutput();
        assertTrue(output.contains("MAX 1 LAYOVER"));
        assertTrue(output.contains("Direct") || output.contains("1 Layover"));
    }

    @Test
    void testReachableAirports_directOnly() {
        Scanner scanner = ControllerTestUtils.createScanner(
            "2025-06-10",
            "4",
            "JFK",
            "0",            // direct only
            "5"
        );

        controller.display(scanner);

        String output = outputCapture.getOutput();
        assertTrue(output.contains("Direct"));
    }

    @Test
    void testReachableAirports_invalidOrigin() {
        Scanner scanner = ControllerTestUtils.createScanner(
            "2025-06-10",
            "4",
            "XXX",          // invalid
            "",
            "5"
        );

        controller.display(scanner);

        String output = outputCapture.getOutput();
        assertTrue(output.contains("Invalid airport code"));
    }

    @Test
    void testReachableAirports_invalidLayoverInput() {
        Scanner scanner = ControllerTestUtils.createScanner(
            "2025-06-10",
            "4",
            "JFK",
            "abc",          // invalid number
            "5"
        );

        controller.display(scanner);

        String output = outputCapture.getOutput();
        assertTrue(output.contains("Invalid number format"));
    }

    @Test
    void testReachableAirports_negativeLayovers() {
        Scanner scanner = ControllerTestUtils.createScanner(
            "2025-06-10",
            "4",
            "JFK",
            "-1",           // negative
            "5"
        );

        controller.display(scanner);

        String output = outputCapture.getOutput();
        assertTrue(output.contains("Layovers must be 0 or greater"));
    }

    @Test
    void testChangeDateOption() {
        Scanner scanner = ControllerTestUtils.createScanner(
            "2025-06-10",   // initial date
            "1",            // change date
            "2025-06-11",   // new date
            "5"             // exit
        );

        controller.display(scanner);

        String output = outputCapture.getOutput();
        assertTrue(output.contains("2025-06-10"));
        assertTrue(output.contains("2025-06-11") || output.contains("Change"));
    }

    @Test
    void testMenuNavigation() {
        Scanner scanner = ControllerTestUtils.createScanner(
            "2025-06-10",
            "2",            // statistics
            "5"             // exit
        );

        controller.display(scanner);

        String output = outputCapture.getOutput();
        assertTrue(output.contains("Select option"));
    }

    @Test
    void testInvalidMenuOption() {
        Scanner scanner = ControllerTestUtils.createScanner(
            "2025-06-10",
            "9",            // invalid option
            "5"             // exit
        );

        controller.display(scanner);

        String output = outputCapture.getOutput();
        assertTrue(output.contains("Invalid option"));
    }

    @Test
    void testGraphBuiltForDate() {
        Scanner scanner = ControllerTestUtils.createScanner(
            "2025-06-10",
            "2",            // trigger graph build
            "5"
        );

        controller.display(scanner);

        String output = outputCapture.getOutput();
        assertTrue(output.contains("Building route network") || output.contains("NETWORK"));
    }
}
