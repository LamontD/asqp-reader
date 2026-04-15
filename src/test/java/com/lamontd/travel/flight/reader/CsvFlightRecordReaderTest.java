package com.lamontd.travel.flight.reader;

import com.lamontd.travel.flight.mapper.AirportCodeMapper;
import com.lamontd.travel.flight.model.AirportInfo;
import com.lamontd.travel.flight.model.ASQPFlightRecord;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CsvFlightRecordReaderTest {

    private final CsvFlightRecordReader reader = new CsvFlightRecordReader();

    @Test
    void testReadOperatedFlight() throws IOException {
        String csv = """
                carrier_code|flight_number|origin|destination|departure_date|scheduled_oag_departure|scheduled_crs_departure|gate_departure|scheduled_arrival|scheduled_crs_arrival|gate_arrival|wheels_up|wheels_down|tail_number|cancellation_code|carrier_delay|weather_delay|nas_delay|security_delay|late_arrival_delay
                DL|5030|CVG|LGA|20250127|1700|1700|1658|1859|1859|1902|1716|1853|N917XJ||0|0|0|0|0
                """;

        List<ASQPFlightRecord> records = reader.readFromReader(new StringReader(csv));

        assertEquals(1, records.size());
        ASQPFlightRecord record = records.get(0);

        assertEquals("DL", record.getCarrierCode());
        assertEquals("5030", record.getFlightNumber());
        assertEquals("CVG", record.getOrigin());
        assertEquals("LGA", record.getDestination());
        assertEquals(LocalDate.of(2025, 1, 27), record.getDepartureDate());
        assertEquals(LocalTime.of(17, 0), record.getScheduledOagDeparture());
        assertEquals(LocalTime.of(17, 0), record.getScheduledCrsDeparture());
        assertTrue(record.getGateDeparture().isPresent());
        assertEquals(LocalTime.of(16, 58), record.getGateDeparture().get());
        assertEquals(LocalTime.of(18, 59), record.getScheduledArrival());
        assertEquals(LocalTime.of(18, 59), record.getScheduledCrsArrival());
        assertTrue(record.getGateArrival().isPresent());
        assertEquals(LocalTime.of(19, 2), record.getGateArrival().get());
        assertTrue(record.getWheelsUp().isPresent());
        assertEquals(LocalTime.of(17, 16), record.getWheelsUp().get());
        assertTrue(record.getWheelsDown().isPresent());
        assertEquals(LocalTime.of(18, 53), record.getWheelsDown().get());
        assertEquals("N917XJ", record.getTailNumber());
        assertFalse(record.isCancelled());
    }

    @Test
    void testReadCancelledFlight() throws IOException {
        String csv = """
                carrier_code|flight_number|origin|destination|departure_date|scheduled_oag_departure|scheduled_crs_departure|gate_departure|scheduled_arrival|scheduled_crs_arrival|gate_arrival|wheels_up|wheels_down|tail_number|cancellation_code|carrier_delay|weather_delay|nas_delay|security_delay|late_arrival_delay
                DL|5030|LGA|CVG|20250105|1335|1335|0|1558|1558|0|0|0|N186GJ|B|0|0|0|0|0
                """;

        List<ASQPFlightRecord> records = reader.readFromReader(new StringReader(csv));

        assertEquals(1, records.size());
        ASQPFlightRecord record = records.get(0);

        assertEquals("DL", record.getCarrierCode());
        assertEquals("5030", record.getFlightNumber());
        assertEquals("LGA", record.getOrigin());
        assertEquals("CVG", record.getDestination());
        assertTrue(record.isCancelled());
        assertEquals("B", record.getCancellationCode().get());
        assertFalse(record.getGateDeparture().isPresent());
        assertFalse(record.getGateArrival().isPresent());
        assertFalse(record.getWheelsUp().isPresent());
        assertFalse(record.getWheelsDown().isPresent());
    }

    @Test
    void testReadMultipleRecords() throws IOException {
        String csv = """
                carrier_code|flight_number|origin|destination|departure_date|scheduled_oag_departure|scheduled_crs_departure|gate_departure|scheduled_arrival|scheduled_crs_arrival|gate_arrival|wheels_up|wheels_down|tail_number|cancellation_code|carrier_delay|weather_delay|nas_delay|security_delay|late_arrival_delay
                DL|5030|CVG|LGA|20250127|1700|1700|1658|1859|1859|1902|1716|1853|N917XJ||0|0|0|0|0
                DL|5030|CVG|LGA|20250128|1700|1700|1647|1859|1859|1829|1702|1824|N914XJ||0|0|0|0|0
                DL|5030|LGA|CVG|20250105|1335|1335|0|1558|1558|0|0|0|N186GJ|B|0|0|0|0|0
                """;

        List<ASQPFlightRecord> records = reader.readFromReader(new StringReader(csv));

        assertEquals(3, records.size());
        assertEquals(2, records.stream().filter(r -> !r.isCancelled()).count());
        assertEquals(1, records.stream().filter(ASQPFlightRecord::isCancelled).count());
    }

    @Test
    void testReadSampleDataFile() throws IOException {
        Path samplePath = Paths.get("src/main/resources/data/sample-data.asc.groomed");
        if (!samplePath.toFile().exists()) {
            System.out.println("Sample data file not found, skipping test");
            return;
        }

        List<ASQPFlightRecord> records = reader.readFromFile(samplePath);

        assertTrue(records.size() > 0, "Should have loaded records from sample file");
        long cancelledCount = records.stream().filter(ASQPFlightRecord::isCancelled).count();
        System.out.println("Loaded " + records.size() + " records (" + cancelledCount + " cancelled)");
    }

    @Test
    void testHandleThreeDigitTime() throws IOException {
        String csv = """
                carrier_code|flight_number|origin|destination|departure_date|scheduled_oag_departure|scheduled_crs_departure|gate_departure|scheduled_arrival|scheduled_crs_arrival|gate_arrival|wheels_up|wheels_down|tail_number|cancellation_code|carrier_delay|weather_delay|nas_delay|security_delay|late_arrival_delay
                DL|5033|ILM|ATL|20250106|600|600|554|748|748|747|619|738|N303PQ||0|0|0|0|0
                """;

        List<ASQPFlightRecord> records = reader.readFromReader(new StringReader(csv));

        assertEquals(1, records.size());
        ASQPFlightRecord record = records.get(0);
        assertEquals(LocalTime.of(6, 0), record.getScheduledOagDeparture());
        assertEquals(LocalTime.of(5, 54), record.getGateDeparture().get());
    }

    @Test
    void testAirportCodeValidation() throws IOException {
        String csv = """
                carrier_code|flight_number|origin|destination|departure_date|scheduled_oag_departure|scheduled_crs_departure|gate_departure|scheduled_arrival|scheduled_crs_arrival|gate_arrival|wheels_up|wheels_down|tail_number|cancellation_code|carrier_delay|weather_delay|nas_delay|security_delay|late_arrival_delay
                DL|5030|INVALID|LGA|20250127|1700|1700|1658|1859|1859|1902|1716|1853|N917XJ||0|0|0|0|0
                """;

        List<ASQPFlightRecord> records = reader.readFromReader(new StringReader(csv));
        assertEquals(0, records.size(), "Invalid airport code should be skipped");
    }

    @Test
    void testMissingRequiredField() throws IOException {
        String csv = """
                carrier_code|flight_number|origin|destination|departure_date|scheduled_oag_departure|scheduled_crs_departure|gate_departure|scheduled_arrival|scheduled_crs_arrival|gate_arrival|wheels_up|wheels_down|tail_number|cancellation_code|carrier_delay|weather_delay|nas_delay|security_delay|late_arrival_delay
                ||CVG|LGA|20250127|1700|1700|1658|1859|1859|1902|1716|1853|N917XJ||0|0|0|0|0
                """;

        List<ASQPFlightRecord> records = reader.readFromReader(new StringReader(csv));
        assertEquals(0, records.size(), "Record with missing carrier code should be skipped");
    }

    @Test
    void testInvalidDateFormat() throws IOException {
        String csv = """
                carrier_code|flight_number|origin|destination|departure_date|scheduled_oag_departure|scheduled_crs_departure|gate_departure|scheduled_arrival|scheduled_crs_arrival|gate_arrival|wheels_up|wheels_down|tail_number|cancellation_code|carrier_delay|weather_delay|nas_delay|security_delay|late_arrival_delay
                DL|5030|CVG|LGA|2025-01-27|1700|1700|1658|1859|1859|1902|1716|1853|N917XJ||0|0|0|0|0
                """;

        List<ASQPFlightRecord> records = reader.readFromReader(new StringReader(csv));
        assertEquals(0, records.size(), "Record with invalid date format should be skipped");
    }

    @Test
    void testUtcTimeCalculation() throws IOException {
        // Create a mock airport mapper with known timezone offsets
        AirportCodeMapper airportMapper = new AirportCodeMapper();

        // CVG (Cincinnati) is in EST (UTC-5)
        AirportInfo cvg = AirportInfo.builder()
                .code("CVG")
                .name("Cincinnati/Northern Kentucky International Airport")
                .city("Cincinnati")
                .timezone(-5.0)
                .build();
        airportMapper.addAirport(cvg);

        // LGA (New York) is also in EST (UTC-5)
        AirportInfo lga = AirportInfo.builder()
                .code("LGA")
                .name("LaGuardia Airport")
                .city("New York")
                .timezone(-5.0)
                .build();
        airportMapper.addAirport(lga);

        CsvFlightRecordReader readerWithMapper = new CsvFlightRecordReader(airportMapper);

        String csv = """
                carrier_code|flight_number|origin|destination|departure_date|scheduled_oag_departure|scheduled_crs_departure|gate_departure|scheduled_arrival|scheduled_crs_arrival|gate_arrival|wheels_up|wheels_down|tail_number|cancellation_code|carrier_delay|weather_delay|nas_delay|security_delay|late_arrival_delay
                DL|5030|CVG|LGA|20250127|1700|1700|1658|1859|1859|1902|1716|1853|N917XJ||0|0|0|0|0
                """;

        List<ASQPFlightRecord> records = readerWithMapper.readFromReader(new StringReader(csv));

        assertEquals(1, records.size());
        ASQPFlightRecord record = records.get(0);

        // Verify UTC times are calculated
        assertTrue(record.getUtcGateDeparture().isPresent(), "UTC gate departure should be calculated");
        assertTrue(record.getUtcGateArrival().isPresent(), "UTC gate arrival should be calculated");

        // Gate departure: 16:58 EST (UTC-5) on 2025-01-27 = 21:58 UTC
        LocalDateTime expectedDepartureLocal = LocalDateTime.of(2025, 1, 27, 16, 58);
        Instant expectedDepartureUtc = expectedDepartureLocal.toInstant(ZoneOffset.ofHours(-5));
        assertEquals(expectedDepartureUtc, record.getUtcGateDeparture().get());

        // Gate arrival: 19:02 EST (UTC-5) on 2025-01-27 = 00:02 UTC on 2025-01-28
        LocalDateTime expectedArrivalLocal = LocalDateTime.of(2025, 1, 27, 19, 2);
        Instant expectedArrivalUtc = expectedArrivalLocal.toInstant(ZoneOffset.ofHours(-5));
        assertEquals(expectedArrivalUtc, record.getUtcGateArrival().get());
    }

    @Test
    void testUtcTimeWithMidnightCrossing() throws IOException {
        // Test a flight that crosses midnight in local time
        AirportCodeMapper airportMapper = new AirportCodeMapper();

        AirportInfo cvg = AirportInfo.builder()
                .code("CVG")
                .name("Cincinnati/Northern Kentucky International Airport")
                .city("Cincinnati")
                .timezone(-5.0)
                .build();
        airportMapper.addAirport(cvg);

        AirportInfo lga = AirportInfo.builder()
                .code("LGA")
                .name("LaGuardia Airport")
                .city("New York")
                .timezone(-5.0)
                .build();
        airportMapper.addAirport(lga);

        CsvFlightRecordReader readerWithMapper = new CsvFlightRecordReader(airportMapper);

        String csv = """
                carrier_code|flight_number|origin|destination|departure_date|scheduled_oag_departure|scheduled_crs_departure|gate_departure|scheduled_arrival|scheduled_crs_arrival|gate_arrival|wheels_up|wheels_down|tail_number|cancellation_code|carrier_delay|weather_delay|nas_delay|security_delay|late_arrival_delay
                DL|5035|CVG|LGA|20250111|2300|2300|2333|100|100|131|2351|128|N918XJ||0|0|0|0|0
                """;

        List<ASQPFlightRecord> records = readerWithMapper.readFromReader(new StringReader(csv));

        assertEquals(1, records.size());
        ASQPFlightRecord record = records.get(0);

        assertTrue(record.getUtcGateDeparture().isPresent());
        assertTrue(record.getUtcGateArrival().isPresent());

        // Departure: 23:33 EST on 2025-01-11 = 04:33 UTC on 2025-01-12
        LocalDateTime expectedDepartureLocal = LocalDateTime.of(2025, 1, 11, 23, 33);
        Instant expectedDepartureUtc = expectedDepartureLocal.toInstant(ZoneOffset.ofHours(-5));
        assertEquals(expectedDepartureUtc, record.getUtcGateDeparture().get());

        // Arrival: 01:31 EST on 2025-01-12 (next day) = 06:31 UTC on 2025-01-12
        LocalDateTime expectedArrivalLocal = LocalDateTime.of(2025, 1, 12, 1, 31);
        Instant expectedArrivalUtc = expectedArrivalLocal.toInstant(ZoneOffset.ofHours(-5));
        assertEquals(expectedArrivalUtc, record.getUtcGateArrival().get());
    }

    @Test
    void testUtcTimeNotCalculatedWithoutMapper() throws IOException {
        // When no airport mapper is provided, UTC times should be null
        CsvFlightRecordReader readerNoMapper = new CsvFlightRecordReader();

        String csv = """
                carrier_code|flight_number|origin|destination|departure_date|scheduled_oag_departure|scheduled_crs_departure|gate_departure|scheduled_arrival|scheduled_crs_arrival|gate_arrival|wheels_up|wheels_down|tail_number|cancellation_code|carrier_delay|weather_delay|nas_delay|security_delay|late_arrival_delay
                DL|5030|CVG|LGA|20250127|1700|1700|1658|1859|1859|1902|1716|1853|N917XJ||0|0|0|0|0
                """;

        List<ASQPFlightRecord> records = readerNoMapper.readFromReader(new StringReader(csv));

        assertEquals(1, records.size());
        ASQPFlightRecord record = records.get(0);

        assertFalse(record.getUtcGateDeparture().isPresent(), "UTC times should not be calculated without mapper");
        assertFalse(record.getUtcGateArrival().isPresent(), "UTC times should not be calculated without mapper");
    }

    @Test
    void testUtcWheelsUpDownCalculation() throws IOException {
        // Test that wheels up/down UTC times are calculated with midnight protection
        AirportCodeMapper airportMapper = new AirportCodeMapper();

        AirportInfo cvg = AirportInfo.builder()
                .code("CVG")
                .name("Cincinnati/Northern Kentucky International Airport")
                .city("Cincinnati")
                .timezone(-5.0)
                .build();
        airportMapper.addAirport(cvg);

        AirportInfo lga = AirportInfo.builder()
                .code("LGA")
                .name("LaGuardia Airport")
                .city("New York")
                .timezone(-5.0)
                .build();
        airportMapper.addAirport(lga);

        CsvFlightRecordReader readerWithMapper = new CsvFlightRecordReader(airportMapper);

        String csv = """
                carrier_code|flight_number|origin|destination|departure_date|scheduled_oag_departure|scheduled_crs_departure|gate_departure|scheduled_arrival|scheduled_crs_arrival|gate_arrival|wheels_up|wheels_down|tail_number|cancellation_code|carrier_delay|weather_delay|nas_delay|security_delay|late_arrival_delay
                DL|5030|CVG|LGA|20250127|1700|1700|1658|1859|1859|1902|1716|1853|N917XJ||0|0|0|0|0
                """;

        List<ASQPFlightRecord> records = readerWithMapper.readFromReader(new StringReader(csv));

        assertEquals(1, records.size());
        ASQPFlightRecord record = records.get(0);

        // Verify all UTC times are calculated
        assertTrue(record.getUtcGateDeparture().isPresent(), "UTC gate departure should be calculated");
        assertTrue(record.getUtcGateArrival().isPresent(), "UTC gate arrival should be calculated");
        assertTrue(record.getUtcWheelsUp().isPresent(), "UTC wheels up should be calculated");
        assertTrue(record.getUtcWheelsDown().isPresent(), "UTC wheels down should be calculated");

        // Wheels up: 17:16 EST on 2025-01-27 = 22:16 UTC
        LocalDateTime expectedWheelsUpLocal = LocalDateTime.of(2025, 1, 27, 17, 16);
        Instant expectedWheelsUpUtc = expectedWheelsUpLocal.toInstant(ZoneOffset.ofHours(-5));
        assertEquals(expectedWheelsUpUtc, record.getUtcWheelsUp().get());

        // Wheels down: 18:53 EST on 2025-01-27 = 23:53 UTC
        LocalDateTime expectedWheelsDownLocal = LocalDateTime.of(2025, 1, 27, 18, 53);
        Instant expectedWheelsDownUtc = expectedWheelsDownLocal.toInstant(ZoneOffset.ofHours(-5));
        assertEquals(expectedWheelsDownUtc, record.getUtcWheelsDown().get());

        // Verify ordering: gate departure < wheels up < wheels down < gate arrival
        assertTrue(record.getUtcGateDeparture().get().isBefore(record.getUtcWheelsUp().get()),
                "Gate departure should be before wheels up");
        assertTrue(record.getUtcWheelsUp().get().isBefore(record.getUtcWheelsDown().get()),
                "Wheels up should be before wheels down");
        assertTrue(record.getUtcWheelsDown().get().isBefore(record.getUtcGateArrival().get()),
                "Wheels down should be before gate arrival");
    }

    @Test
    void testUtcWheelsUpDownWithMidnightCrossing() throws IOException {
        // Test wheels up/down with midnight crossing
        AirportCodeMapper airportMapper = new AirportCodeMapper();

        AirportInfo cvg = AirportInfo.builder()
                .code("CVG")
                .name("Cincinnati/Northern Kentucky International Airport")
                .city("Cincinnati")
                .timezone(-5.0)
                .build();
        airportMapper.addAirport(cvg);

        AirportInfo lga = AirportInfo.builder()
                .code("LGA")
                .name("LaGuardia Airport")
                .city("New York")
                .timezone(-5.0)
                .build();
        airportMapper.addAirport(lga);

        CsvFlightRecordReader readerWithMapper = new CsvFlightRecordReader(airportMapper);

        String csv = """
                carrier_code|flight_number|origin|destination|departure_date|scheduled_oag_departure|scheduled_crs_departure|gate_departure|scheduled_arrival|scheduled_crs_arrival|gate_arrival|wheels_up|wheels_down|tail_number|cancellation_code|carrier_delay|weather_delay|nas_delay|security_delay|late_arrival_delay
                DL|5035|CVG|LGA|20250111|2300|2300|2333|100|100|131|2351|128|N918XJ||0|0|0|0|0
                """;

        List<ASQPFlightRecord> records = readerWithMapper.readFromReader(new StringReader(csv));

        assertEquals(1, records.size());
        ASQPFlightRecord record = records.get(0);

        assertTrue(record.getUtcWheelsUp().isPresent());
        assertTrue(record.getUtcWheelsDown().isPresent());

        // Wheels up: 23:51 EST on 2025-01-11 = 04:51 UTC on 2025-01-12
        LocalDateTime expectedWheelsUpLocal = LocalDateTime.of(2025, 1, 11, 23, 51);
        Instant expectedWheelsUpUtc = expectedWheelsUpLocal.toInstant(ZoneOffset.ofHours(-5));
        assertEquals(expectedWheelsUpUtc, record.getUtcWheelsUp().get());

        // Wheels down: 01:28 EST on 2025-01-12 (next day) = 06:28 UTC on 2025-01-12
        LocalDateTime expectedWheelsDownLocal = LocalDateTime.of(2025, 1, 12, 1, 28);
        Instant expectedWheelsDownUtc = expectedWheelsDownLocal.toInstant(ZoneOffset.ofHours(-5));
        assertEquals(expectedWheelsDownUtc, record.getUtcWheelsDown().get());

        // Verify wheels down is after wheels up in UTC
        assertTrue(record.getUtcWheelsDown().get().isAfter(record.getUtcWheelsUp().get()),
                "Wheels down should be after wheels up in UTC");
    }

    @Test
    void testUtcTimeWithCrossTimezoneNoMidnightCrossing() throws IOException {
        // Test a flight that appears to cross midnight in local time but doesn't in UTC
        // LAX (UTC-8) departing 23:00 to JFK (UTC-5) arriving 07:30 (next day local)
        // In local time: 07:30 > 23:00 (no apparent crossing)
        // But this is a red-eye flight crossing midnight
        AirportCodeMapper airportMapper = new AirportCodeMapper();

        AirportInfo lax = AirportInfo.builder()
                .code("LAX")
                .name("Los Angeles International Airport")
                .city("Los Angeles")
                .timezone(-8.0)
                .build();
        airportMapper.addAirport(lax);

        AirportInfo jfk = AirportInfo.builder()
                .code("JFK")
                .name("John F Kennedy International Airport")
                .city("New York")
                .timezone(-5.0)
                .build();
        airportMapper.addAirport(jfk);

        CsvFlightRecordReader readerWithMapper = new CsvFlightRecordReader(airportMapper);

        String csv = """
                carrier_code|flight_number|origin|destination|departure_date|scheduled_oag_departure|scheduled_crs_departure|gate_departure|scheduled_arrival|scheduled_crs_arrival|gate_arrival|wheels_up|wheels_down|tail_number|cancellation_code|carrier_delay|weather_delay|nas_delay|security_delay|late_arrival_delay
                DL|100|LAX|JFK|20250115|2200|2200|2300|630|630|730|2315|715|N123AB||0|0|0|0|0
                """;

        List<ASQPFlightRecord> records = readerWithMapper.readFromReader(new StringReader(csv));

        assertEquals(1, records.size());
        ASQPFlightRecord record = records.get(0);

        assertTrue(record.getUtcGateDeparture().isPresent());
        assertTrue(record.getUtcGateArrival().isPresent());

        // Departure: 23:00 PST on 2025-01-15 = 07:00 UTC on 2025-01-16
        LocalDateTime expectedDepartureLocal = LocalDateTime.of(2025, 1, 15, 23, 0);
        Instant expectedDepartureUtc = expectedDepartureLocal.toInstant(ZoneOffset.ofHours(-8));

        // Arrival: 07:30 EST on 2025-01-15 = 12:30 UTC on 2025-01-15
        // Since this is BEFORE departure in UTC, the system should recognize it crosses midnight
        // and use 2025-01-16 instead: 07:30 EST on 2025-01-16 = 12:30 UTC on 2025-01-16
        LocalDateTime expectedArrivalLocal = LocalDateTime.of(2025, 1, 16, 7, 30);
        Instant expectedArrivalUtc = expectedArrivalLocal.toInstant(ZoneOffset.ofHours(-5));

        assertEquals(expectedDepartureUtc, record.getUtcGateDeparture().get());
        assertEquals(expectedArrivalUtc, record.getUtcGateArrival().get());

        // Verify arrival is AFTER departure in UTC
        assertTrue(record.getUtcGateArrival().get().isAfter(record.getUtcGateDeparture().get()),
                "Arrival should be after departure in UTC");
    }
}
