package com.lamontd.travel.flight.schedule;

import com.lamontd.travel.flight.model.BookableFlight;
import com.lamontd.travel.flight.schedule.controller.MenuController;
import com.lamontd.travel.flight.schedule.index.ScheduleFlightIndex;
import com.lamontd.travel.flight.schedule.service.ScheduleFlightDataLoader;
import com.lamontd.travel.flight.util.PerformanceTimer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Main application entry point for Flight Schedule Reader.
 * Interactive travel planning application using scheduled flight data.
 */
public class App {
    private static final Logger logger = LoggerFactory.getLogger(App.class);
    private static final String DEFAULT_RESOURCE = "/data/sample-data.bookableflight.csv";

    public static void main(String[] args) {
        displayBanner();

        try (var timer = new PerformanceTimer("Application startup")) {
            // Load data
            ScheduleFlightIndex index = loadData(args);

            // Display summary
            displayDataSummary(index);

            // Start interactive menu
            runInteractiveMenu(index);

        } catch (Exception e) {
            logger.error("Application error: {}", e.getMessage(), e);
            System.err.println("\nError: " + e.getMessage());
            System.exit(1);
        }
    }

    private static ScheduleFlightIndex loadData(String[] args) throws IOException {
        ScheduleFlightDataLoader loader = new ScheduleFlightDataLoader();
        List<BookableFlight> flights;

        if (args.length == 0) {
            // Load from embedded resource
            logger.info("No files specified, loading sample data from resources");
            flights = loadFromResource(DEFAULT_RESOURCE);
        } else {
            // Load from command-line files
            List<Path> filePaths = new ArrayList<>();
            for (String arg : args) {
                filePaths.add(Path.of(arg));
            }
            flights = loader.loadFromFiles(filePaths);
        }

        return new ScheduleFlightIndex(flights);
    }

    private static List<BookableFlight> loadFromResource(String resourcePath) throws IOException {
        try (InputStream is = App.class.getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new IOException("Resource not found: " + resourcePath);
            }

            com.lamontd.travel.flight.schedule.reader.CsvBookableFlightReader reader =
                new com.lamontd.travel.flight.schedule.reader.CsvBookableFlightReader();
            return reader.readFromStream(is, resourcePath);
        }
    }

    private static void displayBanner() {
        System.out.println("╔" + "═".repeat(78) + "╗");
        System.out.println("║" + " ".repeat(20) + "FLIGHT SCHEDULE READER" + " ".repeat(36) + "║");
        System.out.println("║" + " ".repeat(16) + "Travel Planning & Schedule Analysis" + " ".repeat(27) + "║");
        System.out.println("╚" + "═".repeat(78) + "╝");
        System.out.println();
    }

    private static void displayDataSummary(ScheduleFlightIndex index) {
        ScheduleFlightIndex.IndexStats stats = index.getStats();

        System.out.println("Data Summary:");
        System.out.println("  Scheduled Flights: " + stats.totalFlights());
        System.out.println("  Carriers: " + stats.totalCarriers());
        System.out.println("  Airports: " + index.getAllAirports().size());
        System.out.println("  Routes: " + stats.totalRoutes());
        System.out.println();
    }

    private static void runInteractiveMenu(ScheduleFlightIndex index) {
        try (Scanner scanner = new Scanner(System.in)) {
            MenuController menuController = new MenuController();
            menuController.display(index, scanner);
        }

        System.out.println("\nThank you for using Flight Schedule Reader!");
    }
}
