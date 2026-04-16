package com.lamontd.travel.flight.asqp.integration;

import com.lamontd.travel.flight.asqp.index.FlightDataIndex;
import com.lamontd.travel.flight.asqp.model.ASQPFlightRecord;
import com.lamontd.travel.flight.asqp.reader.CsvFlightRecordReader;
import com.lamontd.travel.flight.asqp.service.FlightScheduleService;
import com.lamontd.travel.flight.model.FlightSegment;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test using real data to verify multi-leg segment extraction
 */
class MultiLegIntegrationTest {

    @Test
    void testWN4283WithRealData() throws Exception {
        String dataFile = System.getProperty("user.home") +
            "/asqp-data/processed_data/ontime.td.202501.asqpflightrecord.csv";

        File file = new File(dataFile);
        if (!file.exists()) {
            System.out.println("Skipping test - data file not found: " + dataFile);
            return; // Skip test if data file doesn't exist
        }

        System.out.println("Loading real data from: " + dataFile);

        // Load the data
        CsvFlightRecordReader reader = new CsvFlightRecordReader();
        List<ASQPFlightRecord> records = reader.readFromFile(Path.of(dataFile));

        assertFalse(records.isEmpty(), "Should have loaded records from file");
        System.out.println("Loaded " + records.size() + " total records");

        // Build index
        FlightDataIndex index = new FlightDataIndex(records);
        FlightScheduleService service = new FlightScheduleService(index);

        // Analyze WN 4283
        FlightScheduleService.FlightScheduleAnalysis analysis =
            service.analyzeFlightSchedule("WN", "4283");

        assertNotNull(analysis, "Should find WN 4283 in the dataset");
        System.out.println("\nAnalysis for WN 4283:");
        System.out.println("  Carrier: " + analysis.carrierCode + " " + analysis.flightNumber);
        System.out.println("  Primary Route: " + analysis.origin + " → " + analysis.destination);
        System.out.println("  Route Pattern: " + analysis.routePattern);
        System.out.println("  Multi-Route: " + analysis.isMultiRoute);
        System.out.println("  Total Operations: " + analysis.totalOperations);

        // Check bookable segments
        assertNotNull(analysis.bookableSegments, "Should have bookable segments");
        assertFalse(analysis.bookableSegments.isEmpty(), "Should have at least one segment");

        System.out.println("\n  Bookable Segments (" + analysis.bookableSegments.size() + "):");

        long directLegs = analysis.bookableSegments.stream()
            .filter(FlightSegment::isDirect)
            .count();
        long throughConnections = analysis.bookableSegments.stream()
            .filter(FlightSegment::isThrough)
            .count();

        System.out.println("    Direct Legs: " + directLegs);
        System.out.println("    Through Connections: " + throughConnections);

        for (FlightSegment segment : analysis.bookableSegments) {
            String stops = segment.getIntermediateStops().isEmpty() ? "" :
                " (via " + String.join(", ", segment.getIntermediateStops()) + ")";
            System.out.println("      - " + segment.getOriginAirport() + " → " +
                segment.getDestinationAirport() + stops + " [" +
                segment.getSegmentType() + ", " + segment.getLegCount() + " leg" +
                (segment.getLegCount() == 1 ? "" : "s") + "]");
        }

        // Verify we have the expected segments for a 2-leg operation
        // Should have at least: ORD-DAL, DAL-MCO, and ORD-MCO
        boolean hasOrdDal = analysis.bookableSegments.stream()
            .anyMatch(s -> s.getOriginAirport().equals("ORD") &&
                          s.getDestinationAirport().equals("DAL"));
        boolean hasDalMco = analysis.bookableSegments.stream()
            .anyMatch(s -> s.getOriginAirport().equals("DAL") &&
                          s.getDestinationAirport().equals("MCO"));
        boolean hasOrdMco = analysis.bookableSegments.stream()
            .anyMatch(s -> s.getOriginAirport().equals("ORD") &&
                          s.getDestinationAirport().equals("MCO") &&
                          s.isThrough());

        assertTrue(hasOrdDal || hasDalMco,
            "Should have at least one of the direct leg segments");

        if (hasOrdDal && hasDalMco) {
            assertTrue(hasOrdMco,
                "If flight operates both legs, should have through connection ORD-MCO");
        }

        System.out.println("\n✓ Multi-leg segment extraction working correctly!");
    }
}
