package com.lamontd.travel.flight.schedule.controller;

import com.lamontd.travel.flight.model.BookableFlight;
import com.lamontd.travel.flight.schedule.index.ScheduleFlightIndex;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Test utilities for controller integration tests.
 */
public class ControllerTestUtils {

    /**
     * Creates a Scanner with predefined input lines.
     *
     * @param inputs Array of input strings to simulate user input
     * @return Scanner configured with the inputs
     */
    public static Scanner createScanner(String... inputs) {
        String inputString = String.join("\n", inputs) + "\n";
        InputStream inputStream = new ByteArrayInputStream(inputString.getBytes());
        return new Scanner(inputStream);
    }

    /**
     * Captures System.out output during test execution.
     */
    public static class OutputCapture {
        private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        private final PrintStream originalOut;

        public OutputCapture() {
            this.originalOut = System.out;
        }

        public void start() {
            System.setOut(new PrintStream(outputStream));
        }

        public void stop() {
            System.setOut(originalOut);
        }

        public String getOutput() {
            return outputStream.toString();
        }

        public void reset() {
            outputStream.reset();
        }
    }

    /**
     * Builder for creating test BookableFlight instances.
     */
    public static class FlightBuilder {
        private String carrierCode = "AA";
        private String flightNumber = "100";
        private String origin = "JFK";
        private String destination = "LAX";
        private LocalDate operatingDate = LocalDate.of(2025, 6, 10);
        private LocalTime scheduledDeparture = LocalTime.of(9, 0);
        private LocalTime scheduledArrival = LocalTime.of(12, 0);

        public FlightBuilder carrierCode(String carrierCode) {
            this.carrierCode = carrierCode;
            return this;
        }

        public FlightBuilder flightNumber(String flightNumber) {
            this.flightNumber = flightNumber;
            return this;
        }

        public FlightBuilder origin(String origin) {
            this.origin = origin;
            return this;
        }

        public FlightBuilder destination(String destination) {
            this.destination = destination;
            return this;
        }

        public FlightBuilder operatingDate(LocalDate date) {
            this.operatingDate = date;
            return this;
        }

        public FlightBuilder scheduledDeparture(LocalTime time) {
            this.scheduledDeparture = time;
            return this;
        }

        public FlightBuilder scheduledArrival(LocalTime time) {
            this.scheduledArrival = time;
            return this;
        }

        public BookableFlight build() {
            return BookableFlight.builder()
                    .carrierCode(carrierCode)
                    .flightNumber(flightNumber)
                    .originAirport(origin)
                    .destinationAirport(destination)
                    .operatingDate(operatingDate)
                    .scheduledDepartureTime(scheduledDeparture)
                    .scheduledArrivalTime(scheduledArrival)
                    .build();
        }
    }

    /**
     * Creates a test index with sample flight data.
     */
    public static ScheduleFlightIndex createTestIndex() {
        List<BookableFlight> flights = new ArrayList<>();

        // Add some test flights for JFK-LAX route
        flights.add(new FlightBuilder()
                .carrierCode("AA")
                .flightNumber("100")
                .origin("JFK")
                .destination("LAX")
                .operatingDate(LocalDate.of(2025, 6, 10))
                .scheduledDeparture(LocalTime.of(9, 0))
                .scheduledArrival(LocalTime.of(12, 0))
                .build());

        flights.add(new FlightBuilder()
                .carrierCode("AA")
                .flightNumber("200")
                .origin("JFK")
                .destination("LAX")
                .operatingDate(LocalDate.of(2025, 6, 10))
                .scheduledDeparture(LocalTime.of(14, 30))
                .scheduledArrival(LocalTime.of(17, 30))
                .build());

        flights.add(new FlightBuilder()
                .carrierCode("DL")
                .flightNumber("500")
                .origin("JFK")
                .destination("LAX")
                .operatingDate(LocalDate.of(2025, 6, 10))
                .scheduledDeparture(LocalTime.of(20, 0))
                .scheduledArrival(LocalTime.of(23, 0))
                .build());

        // Add connecting flight options (JFK-ORD-LAX)
        flights.add(new FlightBuilder()
                .carrierCode("UA")
                .flightNumber("300")
                .origin("JFK")
                .destination("ORD")
                .operatingDate(LocalDate.of(2025, 6, 10))
                .scheduledDeparture(LocalTime.of(8, 0))
                .scheduledArrival(LocalTime.of(10, 0))
                .build());

        flights.add(new FlightBuilder()
                .carrierCode("UA")
                .flightNumber("400")
                .origin("ORD")
                .destination("LAX")
                .operatingDate(LocalDate.of(2025, 6, 10))
                .scheduledDeparture(LocalTime.of(12, 0))
                .scheduledArrival(LocalTime.of(14, 0))
                .build());

        // Add return flights (LAX-JFK) for round-trip testing
        flights.add(new FlightBuilder()
                .carrierCode("AA")
                .flightNumber("101")
                .origin("LAX")
                .destination("JFK")
                .operatingDate(LocalDate.of(2025, 6, 10))
                .scheduledDeparture(LocalTime.of(10, 0))
                .scheduledArrival(LocalTime.of(18, 0))
                .build());

        flights.add(new FlightBuilder()
                .carrierCode("AA")
                .flightNumber("201")
                .origin("LAX")
                .destination("JFK")
                .operatingDate(LocalDate.of(2025, 6, 15))
                .scheduledDeparture(LocalTime.of(14, 0))
                .scheduledArrival(LocalTime.of(22, 0))
                .build());

        flights.add(new FlightBuilder()
                .carrierCode("DL")
                .flightNumber("501")
                .origin("LAX")
                .destination("JFK")
                .operatingDate(LocalDate.of(2025, 6, 15))
                .scheduledDeparture(LocalTime.of(19, 0))
                .scheduledArrival(LocalTime.of(3, 0))
                .build());

        // Add return connecting flights (LAX-ORD-JFK) for round-trip testing
        flights.add(new FlightBuilder()
                .carrierCode("UA")
                .flightNumber("401")
                .origin("LAX")
                .destination("ORD")
                .operatingDate(LocalDate.of(2025, 6, 15))
                .scheduledDeparture(LocalTime.of(9, 0))
                .scheduledArrival(LocalTime.of(15, 0))
                .build());

        flights.add(new FlightBuilder()
                .carrierCode("UA")
                .flightNumber("301")
                .origin("ORD")
                .destination("JFK")
                .operatingDate(LocalDate.of(2025, 6, 15))
                .scheduledDeparture(LocalTime.of(17, 0))
                .scheduledArrival(LocalTime.of(20, 0))
                .build());

        return new ScheduleFlightIndex(flights);
    }

    /**
     * Creates a test index with network connections for graph testing.
     */
    public static ScheduleFlightIndex createNetworkTestIndex() {
        List<BookableFlight> flights = new ArrayList<>();
        LocalDate testDate = LocalDate.of(2025, 6, 10);

        // Create a small network: JFK <-> ORD <-> LAX <-> SFO
        String[][] routes = {
            {"JFK", "ORD"}, {"ORD", "JFK"},
            {"ORD", "LAX"}, {"LAX", "ORD"},
            {"LAX", "SFO"}, {"SFO", "LAX"},
            {"JFK", "LAX"}  // Direct route
        };

        int flightNum = 100;
        for (String[] route : routes) {
            flights.add(new FlightBuilder()
                    .carrierCode("AA")
                    .flightNumber(String.valueOf(flightNum++))
                    .origin(route[0])
                    .destination(route[1])
                    .operatingDate(testDate)
                    .scheduledDeparture(LocalTime.of(9, 0))
                    .scheduledArrival(LocalTime.of(11, 0))
                    .build());
        }

        return new ScheduleFlightIndex(flights);
    }
}
