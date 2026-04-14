package com.lamontd.asqp.view;

import com.lamontd.asqp.mapper.CancellationCodeMapper;
import com.lamontd.asqp.model.FlightRecord;
import com.lamontd.asqp.model.index.FlightDataIndex;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Renders the data overview screen
 */
public class DataOverviewView implements ViewRenderer {

    @Override
    public void render(FlightDataIndex index, Scanner scanner) {
        CancellationCodeMapper cancellationMapper = CancellationCodeMapper.getDefault();

        System.out.println("\n" + "=".repeat(50));
        System.out.println("DATA OVERVIEW");
        System.out.println("=".repeat(50));

        // Use pre-computed statistics from index
        System.out.println("\nTotal Flights: " + index.totalFlights);
        System.out.println("  - Operated Flights: " + index.operatedFlights + " (" +
                String.format("%.1f%%", (index.operatedFlights * 100.0 / index.totalFlights)) + ")");
        System.out.println("  - Cancelled Flights: " + index.cancelledFlights + " (" +
                String.format("%.1f%%", (index.cancelledFlights * 100.0 / index.totalFlights)) + ")");

        // Cancellation breakdown
        if (index.cancelledFlights > 0) {
            Map<String, Long> cancellationReasons = index.allRecords.stream()
                    .filter(FlightRecord::isCancelled)
                    .collect(Collectors.groupingBy(
                            r -> r.getCancellationCode().orElse("Unknown"),
                            Collectors.counting()
                    ));

            System.out.println("\n  Cancellation Breakdown:");
            cancellationReasons.entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                    .forEach(entry -> {
                        String description = cancellationMapper.getFullDescription(entry.getKey());
                        System.out.printf("    %s: %,d (%.1f%%)%n",
                                description,
                                entry.getValue(),
                                (entry.getValue() * 100.0 / index.cancelledFlights));
                    });
        }

        // Enhanced date range information
        if (index.minDate != null && index.maxDate != null) {
            System.out.println("\n" + "=".repeat(50));
            System.out.println("DATE COVERAGE");
            System.out.println("=".repeat(50));

            long daysCovered = java.time.temporal.ChronoUnit.DAYS.between(index.minDate, index.maxDate) + 1;
            long uniqueDays = index.byDate.size();

            System.out.printf("\nDate Range: %s to %s%n", index.minDate, index.maxDate);
            System.out.printf("  Total Span: %d days%n", daysCovered);
            System.out.printf("  Days with Data: %d days%n", uniqueDays);

            if (uniqueDays < daysCovered) {
                System.out.printf("  Coverage: %.1f%% (%d days missing)%n",
                    (uniqueDays * 100.0 / daysCovered),
                    (daysCovered - uniqueDays));
            } else {
                System.out.println("  Coverage: 100% (complete)");
            }

            // Flights per day statistics
            DoubleSummaryStatistics dailyStats = index.byDate.values().stream()
                    .mapToDouble(List::size)
                    .summaryStatistics();

            System.out.printf("\nFlights per Day:%n");
            System.out.printf("  Average: %.1f flights/day%n", dailyStats.getAverage());
            System.out.printf("  Min: %d flights%n", (int)dailyStats.getMin());
            System.out.printf("  Max: %d flights%n", (int)dailyStats.getMax());

            // Find busiest and quietest days
            var sortedByVolume = index.byDate.entrySet().stream()
                    .sorted(Map.Entry.<LocalDate, List<FlightRecord>>comparingByValue(
                            Comparator.comparingInt(List::size)).reversed())
                    .toList();

            if (!sortedByVolume.isEmpty()) {
                var busiestDay = sortedByVolume.get(0);
                var quietestDay = sortedByVolume.get(sortedByVolume.size() - 1);

                System.out.printf("  Busiest Day: %s (%d flights)%n",
                    busiestDay.getKey(), busiestDay.getValue().size());
                System.out.printf("  Quietest Day: %s (%d flights)%n",
                    quietestDay.getKey(), quietestDay.getValue().size());
            }

            // Show day-of-week distribution if data spans multiple weeks
            if (uniqueDays >= 7) {
                Map<java.time.DayOfWeek, Long> dayOfWeekCounts = index.allRecords.stream()
                        .collect(Collectors.groupingBy(
                                r -> r.getDepartureDate().getDayOfWeek(),
                                Collectors.counting()
                        ));

                System.out.printf("\nFlights by Day of Week:%n");
                // Show in order Monday through Sunday
                java.time.DayOfWeek[] orderedDays = {
                    java.time.DayOfWeek.MONDAY,
                    java.time.DayOfWeek.TUESDAY,
                    java.time.DayOfWeek.WEDNESDAY,
                    java.time.DayOfWeek.THURSDAY,
                    java.time.DayOfWeek.FRIDAY,
                    java.time.DayOfWeek.SATURDAY,
                    java.time.DayOfWeek.SUNDAY
                };

                for (java.time.DayOfWeek day : orderedDays) {
                    long count = dayOfWeekCounts.getOrDefault(day, 0L);
                    if (count > 0) {
                        System.out.printf("  %s: %,d flights (%.1f%%)%n",
                            day.toString(),
                            count,
                            (count * 100.0 / index.totalFlights));
                    }
                }
            }
        }

        System.out.println("\n" + "=".repeat(50));
        System.out.println("CARRIER & AIRPORT COVERAGE");
        System.out.println("=".repeat(50));
        System.out.println("\nUnique Carriers: " + index.uniqueCarriers);
        System.out.println("Unique Airports: " + index.uniqueAirports);
        System.out.println("Unique Routes: " + index.routeDistances.size());
    }
}
