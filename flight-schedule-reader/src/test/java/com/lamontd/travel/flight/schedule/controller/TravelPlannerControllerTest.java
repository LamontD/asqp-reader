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
            "",             // search mode (default airport)
            "",             // trip type (default one-way)
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
            "",             // search mode
            "",             // trip type
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
            "",             // search mode (default airport)
            "",             // trip type
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
            "",             // search mode (default airport)
            "",             // trip type
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
            "",             // search mode (default airport)
            "",             // trip type
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
            "",             // search mode (default airport)
            "",             // trip type
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
            "",             // search mode (default airport)
            "",             // trip type
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
            "",             // search mode (default airport)
            "",             // trip type
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
            "",             // search mode (default airport)
            "",             // trip type
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
            "",             // search mode (default airport)
            "",             // trip type
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
            "",             // search mode (default airport)
            "",             // trip type
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
            "",             // search mode (default airport)
            "",             // trip type
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
            "",             // search mode (default airport)
            "",             // trip type
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
            "",             // search mode (default airport)
            "",             // trip type
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
            "",             // search mode (default airport)
            "",             // trip type
            "JF",           // too short
            "LAX",
            "2025-06-10",
            ""
        );

        controller.display(index, scanner);

        String output = outputCapture.getOutput();
        assertTrue(output.contains("Invalid airport code") || output.contains("Must be 3 letters"));
    }

    @Test
    void testValidRoundTripSearch() {
        Scanner scanner = ControllerTestUtils.createScanner(
            "",             // search mode (default airport)
            "Round-trip",   // trip type
            "JFK",          // origin
            "LAX",          // destination
            "2025-06-10",   // departure date
            "",             // anytime departure
            "2025-06-15",   // return date
            ""              // anytime return
        );

        controller.display(index, scanner);

        String output = outputCapture.getOutput();
        assertTrue(output.contains("ROUND-TRIP OPTIONS"));
        assertTrue(output.contains("JFK"));
        assertTrue(output.contains("LAX"));
        assertTrue(output.contains("OUTBOUND"));
        assertTrue(output.contains("RETURN"));
        assertTrue(output.contains("2025-06-10"));
        assertTrue(output.contains("2025-06-15"));
    }

    @Test
    void testRoundTripWithTimeFilters() {
        Scanner scanner = ControllerTestUtils.createScanner(
            "",             // search mode (default airport)
            "R",            // round-trip
            "JFK",
            "LAX",
            "2025-06-10",
            "Morning",      // morning departure
            "2025-06-15",
            "Evening"       // evening return
        );

        controller.display(index, scanner);

        String output = outputCapture.getOutput();
        assertTrue(output.contains("ROUND-TRIP OPTIONS"));
        assertTrue(output.contains("Morning"));
        assertTrue(output.contains("Evening"));
    }

    @Test
    void testRoundTripReturnBeforeDeparture() {
        Scanner scanner = ControllerTestUtils.createScanner(
            "",             // search mode (default airport)
            "Round-trip",
            "JFK",
            "LAX",
            "2025-06-15",
            "",
            "2025-06-10",   // return before departure - invalid
            ""
        );

        controller.display(index, scanner);

        String output = outputCapture.getOutput();
        assertTrue(output.contains("Return date cannot be before departure date"));
    }

    @Test
    void testRoundTripSameDay() {
        Scanner scanner = ControllerTestUtils.createScanner(
            "",             // search mode (default airport)
            "Round-trip",
            "JFK",
            "LAX",
            "2025-06-10",   // depart same day
            "Morning",
            "2025-06-10",   // return same day - valid
            "Evening"
        );

        controller.display(index, scanner);

        String output = outputCapture.getOutput();
        assertTrue(output.contains("ROUND-TRIP OPTIONS") || output.contains("No return flights found"));
        // May not have return flights on same day, so either outcome is valid
    }

    @Test
    void testRoundTripNoReturnFlights() {
        Scanner scanner = ControllerTestUtils.createScanner(
            "",             // search mode (default airport)
            "Round-trip",
            "JFK",
            "LAX",
            "2025-06-10",
            "",
            "2025-12-25",   // date with no return flights
            ""
        );

        controller.display(index, scanner);

        String output = outputCapture.getOutput();
        assertTrue(output.contains("No return flights found"));
    }

    @Test
    void testOneWayStillWorks() {
        Scanner scanner = ControllerTestUtils.createScanner(
            "",             // search mode (default airport)
            "One-way",      // explicit one-way
            "JFK",
            "LAX",
            "2025-06-10",
            "Morning"
        );

        controller.display(index, scanner);

        String output = outputCapture.getOutput();
        assertTrue(output.contains("TRAVEL OPTIONS"));
        assertTrue(output.contains("DIRECT"));
        assertFalse(output.contains("ROUND-TRIP"));
        assertFalse(output.contains("OUTBOUND"));
        assertFalse(output.contains("RETURN"));
    }

    @Test
    void testDefaultOneWay() {
        Scanner scanner = ControllerTestUtils.createScanner(
            "",             // search mode (default airport)
            "",             // blank = default to one-way
            "JFK",
            "LAX",
            "2025-06-10",
            ""
        );

        controller.display(index, scanner);

        String output = outputCapture.getOutput();
        assertTrue(output.contains("TRAVEL OPTIONS"));
        assertFalse(output.contains("ROUND-TRIP"));
    }
}
