package com.lamontd.travel.flight.asqp.service;

import com.lamontd.travel.flight.mapper.AirportCodeMapper;
import com.lamontd.travel.flight.model.ASQPFlightRecord;
import com.lamontd.travel.flight.reader.CsvFlightRecordReader;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Service for loading flight data from files
 */
public class FlightDataLoader {

    /**
     * Loads flight records from multiple files in parallel
     * @param filePaths Array of file paths to load
     * @return Combined list of all flight records
     */
    public List<ASQPFlightRecord> loadFiles(String[] filePaths) {
        if (filePaths.length == 1) {
            System.out.println("Loading file...");
        } else {
            System.out.printf("Loading %d files in parallel...%n", filePaths.length);
        }
        long startTime = System.currentTimeMillis();

        // Use parallel streams for multiple files, sequential for single file
        List<ASQPFlightRecord> allRecords = (filePaths.length > 1
                ? Arrays.stream(filePaths).parallel()
                : Arrays.stream(filePaths))
                .map(this::processFile)
                .filter(Objects::nonNull)
                .flatMap(List::stream)
                .collect(Collectors.toList());

        long loadTime = System.currentTimeMillis() - startTime;

        // Show summary
        if (allRecords.isEmpty()) {
            System.err.println("\n✗ No valid records found in any file.");
        } else {
            System.out.printf("\n✓ Successfully loaded %,d total records from %d file(s) in %,d ms%n",
                allRecords.size(), filePaths.length, loadTime);

            if (filePaths.length > 1) {
                System.out.printf("  Average: %,d records per file%n",
                    (allRecords.size() / filePaths.length));
            }

            // Show cancelled vs operated summary
            long cancelled = allRecords.stream().filter(ASQPFlightRecord::isCancelled).count();
            long operated = allRecords.size() - cancelled;
            System.out.printf("  Operated: %,d (%.1f%%), Cancelled: %,d (%.1f%%)%n",
                operated, (operated * 100.0 / allRecords.size()),
                cancelled, (cancelled * 100.0 / allRecords.size()));
        }

        return allRecords;
    }

    /**
     * Processes a single flight data file
     * @param filePath Path to the file to process
     * @return List of flight records, or null if error occurred
     */
    private List<ASQPFlightRecord> processFile(String filePath) {
        AirportCodeMapper airportMapper = AirportCodeMapper.getDefault();
        CsvFlightRecordReader reader = new CsvFlightRecordReader(airportMapper);
        Path path = Paths.get(filePath);

        try {
            List<ASQPFlightRecord> records = reader.readFromFile(path);

            if (records.isEmpty()) {
                System.err.println("  Warning: No records found in " + filePath);
            } else {
                System.out.printf("  ✓ Loaded %,d records from %s%n", records.size(),
                    path.getFileName());
            }

            return records;

        } catch (IOException e) {
            System.err.println("  ✗ Error reading " + filePath + ": " + e.getMessage());
            return null;
        } catch (Exception e) {
            System.err.println("  ✗ Unexpected error processing " + filePath + ": " + e.getMessage());
            return null;
        }
    }
}
