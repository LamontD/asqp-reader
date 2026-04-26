package com.lamontd.travel.flight.schedule.controller;

import com.lamontd.travel.flight.schedule.index.ScheduleFlightIndex;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;

class TravelPlannerControllerTest {
    private TravelPlannerController controller;
    private ScheduleFlightIndex index;
    private ControllerTestUtils.OutputCapture outputCapture;

    @BeforeEach
    void setUp() {
        controller = new TravelPlannerController();
        index = ControllerTestUtils.createTestIndex();
        outputCapture = new ControllerTestUtils.OutputCapture();
        outputCapture.start();
    }

    @AfterEach
    void tearDown() {
        outputCapture.stop();
    }

    @Test
    void testValidDirectFlightSearch() {
        Scanner scanner = ControllerTestUtils.createScanner(
            "JFK",          // origin
            "LAX",          // destination
            "2025-06-10",   // date
            ""              // anytime
        );

        controller.display(index, scanner);

        String output = outputCapture.getOutput();
        assertTrue(output.contains("TRAVEL PLANNER"));
        assertTrue(output.contains("JFK"));
        assertTrue(output.contains("LAX"));
        assertTrue(output.contains("2025-06-10"));
        assertTrue(output.contains("DIRECT"));
        assertTrue(output.contains("AA 100"));
    }

    @Test
    void testInvalidOriginAirport() {
        Scanner scanner = ControllerTestUtils.createScanner(
            "XXX",          // invalid origin
            "LAX",
            "2025-06-10",
            ""
        );

        controller.display(index, scanner);

        String output = outputCapture.getOutput();
        assertTrue(output.contains("Invalid airport code: XXX"));
        assertTrue(output.contains("Airport not found in database"));
    }

    @Test
    void testInvalidDestinationAirport() {
        Scanner scanner = ControllerTestUtils.createScanner(
            "JFK",
            "ZZZ",          // invalid destination
            "2025-06-10",
            ""
        );

        controller.display(index, scanner);

        String output = outputCapture.getOutput();
        assertTrue(output.contains("Invalid airport code: ZZZ"));
    }

    @Test
    void testInvalidDate() {
        Scanner scanner = ControllerTestUtils.createScanner(
            "JFK",
            "LAX",
            "06/10/2025",   // wrong format
            ""
        );

        controller.display(index, scanner);

        String output = outputCapture.getOutput();
        assertTrue(output.contains("Invalid date format"));
        assertTrue(output.contains("yyyy-MM-dd"));
    }

    @Test
    void testSameOriginAndDestination() {
        Scanner scanner = ControllerTestUtils.createScanner(
            "JFK",
            "JFK",          // same as origin
            "2025-06-10",
            ""
        );

        controller.display(index, scanner);

        String output = outputCapture.getOutput();
        assertTrue(output.contains("Origin and destination cannot be the same"));
    }

    @Test
    void testMorningTimeFilter() {
        Scanner scanner = ControllerTestUtils.createScanner(
            "JFK",
            "LAX",
            "2025-06-10",
            "Morning"       // should only show 9:00 AM flight
        );

        controller.display(index, scanner);

        String output = outputCapture.getOutput();
        assertTrue(output.contains("Time Preference: Morning"));
        assertTrue(output.contains("09:00"));  // AA 100 departs at 9 AM
        assertFalse(output.contains("20:00")); // DL 500 at 8 PM should not show
    }

    @Test
    void testAfternoonTimeFilter() {
        Scanner scanner = ControllerTestUtils.createScanner(
            "JFK",
            "LAX",
            "2025-06-10",
            "Afternoon"
        );

        controller.display(index, scanner);

        String output = outputCapture.getOutput();
        assertTrue(output.contains("Time Preference: Afternoon"));
        assertTrue(output.contains("14:30"));  // AA 200 departs at 2:30 PM
    }

    @Test
    void testEveningTimeFilter() {
        Scanner scanner = ControllerTestUtils.createScanner(
            "JFK",
            "LAX",
            "2025-06-10",
            "Evening"
        );

        controller.display(index, scanner);

        String output = outputCapture.getOutput();
        assertTrue(output.contains("Time Preference: Evening"));
        assertTrue(output.contains("20:00"));  // DL 500 departs at 8 PM
    }

    @Test
    void testAnytimeDefaultFilter() {
        Scanner scanner = ControllerTestUtils.createScanner(
            "JFK",
            "LAX",
            "2025-06-10",
            ""              // blank = anytime
        );

        controller.display(index, scanner);

        String output = outputCapture.getOutput();
        assertTrue(output.contains("Time Preference: Anytime"));
        // Should show all flights
        assertTrue(output.contains("AA 100"));
        assertTrue(output.contains("AA 200"));
        assertTrue(output.contains("DL 500"));
    }

    @Test
    void testNoFlightsFound() {
        Scanner scanner = ControllerTestUtils.createScanner(
            "JFK",
            "LAX",
            "2025-12-25",   // date with no flights
            ""
        );

        controller.display(index, scanner);

        String output = outputCapture.getOutput();
        assertTrue(output.contains("No flights found"));
    }

    @Test
    void testConnectionsIncluded() {
        Scanner scanner = ControllerTestUtils.createScanner(
            "JFK",
            "LAX",
            "2025-06-10",
            ""
        );

        controller.display(index, scanner);

        String output = outputCapture.getOutput();
        // Should show both direct and connecting flights
        assertTrue(output.contains("DIRECT") || output.contains("1 STOP"));
    }

    @Test
    void testTop10Limiting() {
        Scanner scanner = ControllerTestUtils.createScanner(
            "JFK",
            "LAX",
            "2025-06-10",
            ""
        );

        controller.display(index, scanner);

        String output = outputCapture.getOutput();
        // Output should mention how many options are shown
        assertTrue(output.contains("option"));
    }

    @Test
    void testCarrierNamesDisplayed() {
        Scanner scanner = ControllerTestUtils.createScanner(
            "JFK",
            "LAX",
            "2025-06-10",
            ""
        );

        controller.display(index, scanner);

        String output = outputCapture.getOutput();
        // Should show carrier names, not just codes
        assertTrue(output.contains("American Airlines") || output.contains("Delta Air Lines"));
    }

    @Test
    void testCityNamesDisplayed() {
        Scanner scanner = ControllerTestUtils.createScanner(
            "JFK",
            "LAX",
            "2025-06-10",
            ""
        );

        controller.display(index, scanner);

        String output = outputCapture.getOutput();
        assertTrue(output.contains("New York") || output.contains("Los Angeles"));
    }

    @Test
    void testAirportCodeLengthValidation() {
        Scanner scanner = ControllerTestUtils.createScanner(
            "JF",           // too short
            "LAX",
            "2025-06-10",
            ""
        );

        controller.display(index, scanner);

        String output = outputCapture.getOutput();
        assertTrue(output.contains("Invalid airport code") || output.contains("Must be 3 letters"));
    }
}
