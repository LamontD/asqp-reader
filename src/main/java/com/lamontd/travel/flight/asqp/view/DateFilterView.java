package com.lamontd.travel.flight.asqp.view;

import com.lamontd.travel.flight.util.FlightDataIndex;
import com.lamontd.travel.flight.model.ASQPFlightRecord;

import java.time.LocalDate;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

/**
 * Renders the date filter screen and handles date filtering
 */
public class DateFilterView {

    /**
     * Result of date filtering operation
     */
    public static class DateFilterResult {
        public final FlightDataIndex index;
        public final LocalDate startDate;
        public final LocalDate endDate;

        public DateFilterResult(FlightDataIndex index, LocalDate startDate, LocalDate endDate) {
            this.index = index;
            this.startDate = startDate;
            this.endDate = endDate;
        }
    }

    public DateFilterResult render(FlightDataIndex originalIndex,
                                   LocalDate currentStartDate,
                                   LocalDate currentEndDate,
                                   Scanner scanner) {
        System.out.println("\n" + "=".repeat(50));
        System.out.println("DATE RANGE FILTER");
        System.out.println("=".repeat(50));

        System.out.printf("\nAvailable date range: %s to %s%n",
            originalIndex.minDate, originalIndex.maxDate);

        if (currentStartDate != null || currentEndDate != null) {
            System.out.print("Current filter: ");
            if (currentStartDate != null && currentEndDate != null) {
                System.out.printf("%s to %s%n", currentStartDate, currentEndDate);
            } else if (currentStartDate != null) {
                System.out.printf("From %s onwards%n", currentStartDate);
            } else {
                System.out.printf("Up to %s%n", currentEndDate);
            }
        } else {
            System.out.println("Current filter: None (showing all data)");
        }

        System.out.println("\nOptions:");
        System.out.println("1. Set date range filter");
        System.out.println("2. Clear filter (show all data)");
        System.out.println("3. Cancel (return to menu)");
        System.out.print("\nSelect option (1-3): ");

        String choice = scanner.nextLine().trim();

        switch (choice) {
            case "1":
                // Set new filter
                System.out.println("\nEnter date range (leave blank to skip):");
                System.out.print("Start date (YYYY-MM-DD): ");
                String startInput = scanner.nextLine().trim();

                LocalDate parsedStartDate = null;
                if (!startInput.isEmpty()) {
                    try {
                        parsedStartDate = LocalDate.parse(startInput);
                        if (parsedStartDate.isBefore(originalIndex.minDate)) {
                            System.out.printf("Warning: Start date before available data. Using %s%n",
                                originalIndex.minDate);
                            parsedStartDate = originalIndex.minDate;
                        }
                    } catch (Exception e) {
                        System.out.println("Invalid date format. Skipping start date.");
                    }
                }

                System.out.print("End date (YYYY-MM-DD): ");
                String endInput = scanner.nextLine().trim();

                LocalDate parsedEndDate = null;
                if (!endInput.isEmpty()) {
                    try {
                        parsedEndDate = LocalDate.parse(endInput);
                        if (parsedEndDate.isAfter(originalIndex.maxDate)) {
                            System.out.printf("Warning: End date after available data. Using %s%n",
                                originalIndex.maxDate);
                            parsedEndDate = originalIndex.maxDate;
                        }
                    } catch (Exception e) {
                        System.out.println("Invalid date format. Skipping end date.");
                    }
                }

                // Validate date range
                if (parsedStartDate != null && parsedEndDate != null && parsedStartDate.isAfter(parsedEndDate)) {
                    System.out.println("\nError: Start date must be before or equal to end date.");
                    return null;
                }

                if (parsedStartDate == null && parsedEndDate == null) {
                    System.out.println("\nNo dates entered. Filter not changed.");
                    return null;
                }

                // Make final for lambda
                final LocalDate finalStartDate = parsedStartDate;
                final LocalDate finalEndDate = parsedEndDate;

                // Filter the data
                List<ASQPFlightRecord> filteredRecords = originalIndex.allRecords.stream()
                        .filter(r -> {
                            LocalDate flightDate = r.getDepartureDate();
                            boolean afterStart = finalStartDate == null || !flightDate.isBefore(finalStartDate);
                            boolean beforeEnd = finalEndDate == null || !flightDate.isAfter(finalEndDate);
                            return afterStart && beforeEnd;
                        })
                        .collect(Collectors.toList());

                if (filteredRecords.isEmpty()) {
                    System.out.println("\nNo flights found in the specified date range.");
                    return null;
                }

                System.out.printf("\n✓ Filter applied: %,d flights match the date range%n",
                    filteredRecords.size());

                // Build new index with filtered data
                FlightDataIndex filteredIndex = new FlightDataIndex(filteredRecords);
                return new DateFilterResult(filteredIndex, finalStartDate, finalEndDate);

            case "2":
                // Clear filter
                if (currentStartDate == null && currentEndDate == null) {
                    System.out.println("\nNo active filter to clear.");
                    return null;
                }
                System.out.printf("\n✓ Filter cleared. Showing all %,d flights%n",
                    originalIndex.totalFlights);
                return new DateFilterResult(originalIndex, null, null);

            case "3":
                // Cancel
                return null;

            default:
                System.out.println("\nInvalid option.");
                return null;
        }
    }
}
