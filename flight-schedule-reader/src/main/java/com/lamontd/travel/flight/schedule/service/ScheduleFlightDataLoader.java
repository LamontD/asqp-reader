package com.lamontd.travel.flight.schedule.service;

import com.lamontd.travel.flight.model.BookableFlight;
import com.lamontd.travel.flight.schedule.reader.CsvBookableFlightReader;
import com.lamontd.travel.flight.util.PerformanceTimer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Loads scheduled flight data from CSV files using virtual threads for parallel processing.
 */
public class ScheduleFlightDataLoader {
    private static final Logger logger = LoggerFactory.getLogger(ScheduleFlightDataLoader.class);
    private final CsvBookableFlightReader reader;

    public ScheduleFlightDataLoader() {
        this.reader = new CsvBookableFlightReader();
    }

    /**
     * Loads scheduled flights from multiple CSV files in parallel.
     *
     * @param filePaths paths to CSV files
     * @return list of all loaded scheduled flights
     * @throws IOException if any file cannot be read
     */
    public List<BookableFlight> loadFromFiles(List<Path> filePaths) throws IOException {
        if (filePaths.isEmpty()) {
            logger.warn("No files provided to load");
            return List.of();
        }

        logger.info("Loading scheduled flights from {} file(s)", filePaths.size());

        try (var timer = new PerformanceTimer("Total data loading")) {
            // Validate all files exist
            for (Path path : filePaths) {
                if (!Files.exists(path)) {
                    throw new IOException("File not found: " + path);
                }
                if (!Files.isRegularFile(path)) {
                    throw new IOException("Not a regular file: " + path);
                }
            }

            ConcurrentLinkedQueue<BookableFlight> allFlights = new ConcurrentLinkedQueue<>();
            ConcurrentLinkedQueue<Exception> errors = new ConcurrentLinkedQueue<>();

            // Use virtual threads for parallel file loading
            List<Thread> threads = new ArrayList<>();
            for (Path filePath : filePaths) {
                Thread thread = Thread.ofVirtual().start(() -> {
                    try {
                        List<BookableFlight> flights = reader.readFromFile(filePath);
                        allFlights.addAll(flights);
                    } catch (Exception e) {
                        logger.error("Error loading file {}: {}", filePath, e.getMessage());
                        errors.add(e);
                    }
                });
                threads.add(thread);
            }

            // Wait for all threads to complete
            for (Thread thread : threads) {
                try {
                    thread.join();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Loading interrupted", e);
                }
            }

            // Check for errors
            if (!errors.isEmpty()) {
                Exception firstError = errors.peek();
                throw new IOException("Failed to load some files: " + firstError.getMessage(), firstError);
            }

            List<BookableFlight> result = new ArrayList<>(allFlights);
            logger.info("Loaded {} total scheduled flights from {} file(s)", result.size(), filePaths.size());

            return result;
        }
    }

    /**
     * Loads scheduled flights from a single CSV file.
     *
     * @param filePath path to CSV file
     * @return list of loaded scheduled flights
     * @throws IOException if file cannot be read
     */
    public List<BookableFlight> loadFromFile(Path filePath) throws IOException {
        return loadFromFiles(List.of(filePath));
    }
}
