package com.lamontd.travel.flight.schedule.reader;

import com.lamontd.travel.flight.model.BookableFlight;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CsvBookableFlightReaderTest {

    private final CsvBookableFlightReader reader = new CsvBookableFlightReader();

    @Test
    void testReadValidRecord() throws IOException {
        String csv = """
                carrier_code|flight_number|origin|destination|departure_date|scheduled_departure|scheduled_arrival
                WN|100|LAX|SFO|20250115|1000|1130
                """;

        List<BookableFlight> flights = readFromString(csv);

        assertEquals(1, flights.size());
        BookableFlight flight = flights.get(0);
        assertEquals("WN", flight.getCarrierCode());
        assertEquals("100", flight.getFlightNumber());
        assertEquals("LAX", flight.getOriginAirport());
        assertEquals("SFO", flight.getDestinationAirport());
        assertEquals(LocalDate.of(2025, 1, 15), flight.getOperatingDate());
        assertEquals(LocalTime.of(10, 0), flight.getScheduledDepartureTime());
        assertEquals(LocalTime.of(11, 30), flight.getScheduledArrivalTime());
    }

    @Test
    void testReadMultipleRecords() throws IOException {
        String csv = """
                carrier_code|flight_number|origin|destination|departure_date|scheduled_departure|scheduled_arrival
                WN|100|LAX|SFO|20250115|1000|1130
                AA|200|JFK|LAX|20250116|800|1130
                DL|300|ATL|ORD|20250117|1400|1600
                """;

        List<BookableFlight> flights = readFromString(csv);

        assertEquals(3, flights.size());
        assertEquals("WN", flights.get(0).getCarrierCode());
        assertEquals("AA", flights.get(1).getCarrierCode());
        assertEquals("DL", flights.get(2).getCarrierCode());
    }

    @Test
    void testReadWithoutHeader() throws IOException {
        String csv = """
                WN|100|LAX|SFO|20250115|1000|1130
                AA|200|JFK|LAX|20250116|800|1130
                """;

        List<BookableFlight> flights = readFromString(csv);

        assertEquals(2, flights.size());
    }

    @Test
    void testReadWithEmptyLines() throws IOException {
        String csv = """
                carrier_code|flight_number|origin|destination|departure_date|scheduled_departure|scheduled_arrival
                WN|100|LAX|SFO|20250115|1000|1130

                AA|200|JFK|LAX|20250116|800|1130

                """;

        List<BookableFlight> flights = readFromString(csv);

        assertEquals(2, flights.size());
    }

    @Test
    void testTimeWithLeadingZero() throws IOException {
        String csv = """
                carrier_code|flight_number|origin|destination|departure_date|scheduled_departure|scheduled_arrival
                WN|100|LAX|SFO|20250115|600|730
                """;

        List<BookableFlight> flights = readFromString(csv);

        assertEquals(1, flights.size());
        assertEquals(LocalTime.of(6, 0), flights.get(0).getScheduledDepartureTime());
        assertEquals(LocalTime.of(7, 30), flights.get(0).getScheduledArrivalTime());
    }

    @Test
    void testRedEyeFlight() throws IOException {
        String csv = """
                carrier_code|flight_number|origin|destination|departure_date|scheduled_departure|scheduled_arrival
                AA|100|LAX|JFK|20250115|2300|730
                """;

        List<BookableFlight> flights = readFromString(csv);

        assertEquals(1, flights.size());
        BookableFlight flight = flights.get(0);
        assertEquals(LocalTime.of(23, 0), flight.getScheduledDepartureTime());
        assertEquals(LocalTime.of(7, 30), flight.getScheduledArrivalTime());
    }

    @Test
    void testLowercaseAirportCodes() throws IOException {
        String csv = """
                carrier_code|flight_number|origin|destination|departure_date|scheduled_departure|scheduled_arrival
                WN|100|lax|sfo|20250115|1000|1130
                """;

        List<BookableFlight> flights = readFromString(csv);

        assertEquals(1, flights.size());
        assertEquals("LAX", flights.get(0).getOriginAirport());
        assertEquals("SFO", flights.get(0).getDestinationAirport());
    }

    @Test
    void testSkipInvalidRecordContinueProcessing() throws IOException {
        String csv = """
                carrier_code|flight_number|origin|destination|departure_date|scheduled_departure|scheduled_arrival
                WN|100|LAX|SFO|20250115|1000|1130
                ||LAX|SFO|20250115|1000|1130
                AA|200|JFK|LAX|20250116|800|1130
                """;

        List<BookableFlight> flights = readFromString(csv);

        assertEquals(2, flights.size());
        assertEquals("WN", flights.get(0).getCarrierCode());
        assertEquals("AA", flights.get(1).getCarrierCode());
    }

    @Test
    void testInvalidFieldCount() throws IOException {
        String csv = """
                carrier_code|flight_number|origin|destination|departure_date|scheduled_departure|scheduled_arrival
                WN|100|LAX|SFO|20250115|1000
                """;

        List<BookableFlight> flights = readFromString(csv);

        assertEquals(0, flights.size());
    }

    @Test
    void testMissingCarrierCode() throws IOException {
        String csv = """
                carrier_code|flight_number|origin|destination|departure_date|scheduled_departure|scheduled_arrival
                |100|LAX|SFO|20250115|1000|1130
                """;

        List<BookableFlight> flights = readFromString(csv);

        assertEquals(0, flights.size());
    }

    @Test
    void testInvalidAirportCode() throws IOException {
        String csv = """
                carrier_code|flight_number|origin|destination|departure_date|scheduled_departure|scheduled_arrival
                WN|100|LAXX|SFO|20250115|1000|1130
                """;

        List<BookableFlight> flights = readFromString(csv);

        assertEquals(0, flights.size());
    }

    @Test
    void testInvalidDate() throws IOException {
        String csv = """
                carrier_code|flight_number|origin|destination|departure_date|scheduled_departure|scheduled_arrival
                WN|100|LAX|SFO|2025-01-15|1000|1130
                """;

        List<BookableFlight> flights = readFromString(csv);

        assertEquals(0, flights.size());
    }

    @Test
    void testInvalidTime() throws IOException {
        String csv = """
                carrier_code|flight_number|origin|destination|departure_date|scheduled_departure|scheduled_arrival
                WN|100|LAX|SFO|20250115|2500|1130
                """;

        List<BookableFlight> flights = readFromString(csv);

        assertEquals(0, flights.size());
    }

    @Test
    void testReadFromFile(@TempDir Path tempDir) throws IOException {
        Path testFile = tempDir.resolve("test-flights.csv");
        String csv = """
                carrier_code|flight_number|origin|destination|departure_date|scheduled_departure|scheduled_arrival
                WN|100|LAX|SFO|20250115|1000|1130
                AA|200|JFK|LAX|20250116|800|1130
                """;
        Files.writeString(testFile, csv);

        List<BookableFlight> flights = reader.readFromFile(testFile);

        assertEquals(2, flights.size());
    }

    @Test
    void testReadSampleDataFromResources() throws IOException {
        try (InputStream is = getClass().getResourceAsStream("/data/sample-data.bookableflight.csv")) {
            assertNotNull(is, "Sample data file should exist in resources");

            List<BookableFlight> flights = reader.readFromStream(is, "sample-data.bookableflight.csv");

            assertTrue(flights.size() > 0, "Should load at least one flight from sample data");

            // Verify first flight has valid structure
            BookableFlight first = flights.get(0);
            assertNotNull(first.getCarrierCode());
            assertNotNull(first.getFlightNumber());
            assertNotNull(first.getOriginAirport());
            assertNotNull(first.getDestinationAirport());
            assertNotNull(first.getOperatingDate());
            assertNotNull(first.getScheduledDepartureTime());
            assertNotNull(first.getScheduledArrivalTime());
        }
    }

    private List<BookableFlight> readFromString(String csv) throws IOException {
        InputStream is = new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));
        return reader.readFromStream(is, "test-data");
    }
}
