package com.lamontd.travel.flight.asqp;

import com.lamontd.travel.flight.asqp.controller.MenuController;
import com.lamontd.travel.flight.util.FlightDataIndex;
import com.lamontd.travel.flight.model.ASQPFlightRecord;
import com.lamontd.travel.flight.asqp.service.FlightDataLoader;
import com.lamontd.travel.flight.asqp.view.DataOverviewView;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Scanner;

public class App {

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Usage: java -jar asqp-reader.jar <file1> [file2] [file3] ...");
            System.out.println("  Load one or more flight data files");
            System.out.println("\nExample with sample data:");
            processSampleData();
            return;
        }

        // Load all files (in parallel if multiple files)
        FlightDataLoader loader = new FlightDataLoader();
        List<ASQPFlightRecord> allRecords = loader.loadFiles(args);

        if (allRecords.isEmpty()) {
            System.err.println("No records loaded. Exiting.");
            return;
        }

        // Build index ONCE for all records
        FlightDataIndex index = new FlightDataIndex(allRecords);

        // Run interactive menu
        MenuController controller = new MenuController(index);
        controller.run();
    }

    private static void processSampleData() {
        Path samplePath = Paths.get("src/main/resources/data/sample-data.asc.groomed");
        FlightDataLoader loader = new FlightDataLoader();
        List<ASQPFlightRecord> records = loader.loadFiles(new String[]{samplePath.toString()});

        if (!records.isEmpty()) {
            FlightDataIndex index = new FlightDataIndex(records);
            new DataOverviewView().render(index, new Scanner(System.in));
        }
    }
}
