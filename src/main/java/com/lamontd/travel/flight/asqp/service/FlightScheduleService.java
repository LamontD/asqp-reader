package com.lamontd.travel.flight.asqp.service;

import com.lamontd.travel.flight.util.FlightDataIndex;
import com.lamontd.travel.flight.model.ASQPFlightRecord;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for inferring flight schedules from historical data
 */
public class FlightScheduleService {

    private final FlightDataIndex index;

    public FlightScheduleService(FlightDataIndex index) {
        this.index = index;
    }

    /**
     * Analyzes a flight number to determine its typical schedule
     */
    public FlightScheduleAnalysis analyzeFlightSchedule(String carrierCode, String flightNumber) {
        List<ASQPFlightRecord> records = index.getByFlightNumber(carrierCode, flightNumber);

        if (records.isEmpty()) {
            return null;
        }

        // Group by route (most flights operate on a single route)
        Map<String, List<ASQPFlightRecord>> byRoute = records.stream()
                .collect(Collectors.groupingBy(r -> r.getOrigin() + "-" + r.getDestination()));

        // Analyze the most common route
        Map.Entry<String, List<ASQPFlightRecord>> primaryRoute = byRoute.entrySet().stream()
                .max(Comparator.comparingInt(e -> e.getValue().size()))
                .orElse(null);

        if (primaryRoute == null) {
            return null;
        }

        String route = primaryRoute.getKey();
        String[] routeParts = route.split("-");
        String origin = routeParts[0];
        String destination = routeParts[1];
        List<ASQPFlightRecord> routeRecords = primaryRoute.getValue();

        // Analyze days of operation
        Map<DayOfWeek, Long> dayFrequency = routeRecords.stream()
                .collect(Collectors.groupingBy(
                        r -> r.getDepartureDate().getDayOfWeek(),
                        Collectors.counting()
                ));

        Set<DayOfWeek> operatingDays = dayFrequency.keySet();

        // Find typical scheduled times (using scheduled CRS times, not actuals)
        List<LocalTime> scheduledDepartures = routeRecords.stream()
                .map(ASQPFlightRecord::getScheduledCrsDeparture)
                .filter(Objects::nonNull)
                .sorted()
                .toList();

        List<LocalTime> scheduledArrivals = routeRecords.stream()
                .map(ASQPFlightRecord::getScheduledCrsArrival)
                .filter(Objects::nonNull)
                .sorted()
                .toList();

        LocalTime typicalDeparture = findMostCommonTime(scheduledDepartures);
        LocalTime typicalArrival = findMostCommonTime(scheduledArrivals);

        // Calculate reliability metrics
        long totalOperations = routeRecords.size();
        long cancelled = routeRecords.stream().filter(ASQPFlightRecord::isCancelled).count();
        long operated = totalOperations - cancelled;
        double completionRate = (operated * 100.0) / totalOperations;

        // Calculate on-time performance for operated flights
        List<ASQPFlightRecord> operatedFlights = routeRecords.stream()
                .filter(r -> !r.isCancelled())
                .toList();

        long onTimeCount = operatedFlights.stream()
                .filter(this::isOnTime)
                .count();

        double onTimeRate = operated > 0 ? (onTimeCount * 100.0) / operated : 0.0;

        // Calculate average delay for delayed flights
        List<Integer> delays = operatedFlights.stream()
                .map(this::calculateDepartureDelay)
                .filter(delay -> delay != null && delay > 15)
                .toList();

        Double avgDelay = delays.isEmpty() ? null :
                delays.stream().mapToInt(Integer::intValue).average().orElse(0.0);

        // Check if there are alternate routes
        Map<String, Long> routeFrequencies = records.stream()
                .collect(Collectors.groupingBy(
                        r -> r.getOrigin() + "-" + r.getDestination(),
                        Collectors.counting()
                ));

        return new FlightScheduleAnalysis(
                carrierCode,
                flightNumber,
                origin,
                destination,
                typicalDeparture,
                typicalArrival,
                operatingDays,
                dayFrequency,
                totalOperations,
                operated,
                cancelled,
                completionRate,
                onTimeRate,
                avgDelay,
                routeFrequencies
        );
    }

    /**
     * Finds the most common time (mode) from a list of times
     * Groups times into 15-minute windows to handle minor schedule variations
     */
    private LocalTime findMostCommonTime(List<LocalTime> times) {
        if (times.isEmpty()) {
            return null;
        }

        // Group times into 15-minute windows
        Map<LocalTime, Long> timeFrequency = times.stream()
                .collect(Collectors.groupingBy(
                        time -> roundToNearestQuarterHour(time),
                        Collectors.counting()
                ));

        return timeFrequency.entrySet().stream()
                .max(Comparator.comparingLong(Map.Entry::getValue))
                .map(Map.Entry::getKey)
                .orElse(times.get(0));
    }

    /**
     * Rounds a time to the nearest 15-minute interval
     */
    private LocalTime roundToNearestQuarterHour(LocalTime time) {
        int minutes = time.getMinute();
        int roundedMinutes = ((minutes + 7) / 15) * 15;
        if (roundedMinutes == 60) {
            return time.plusHours(1).withMinute(0).withSecond(0).withNano(0);
        }
        return time.withMinute(roundedMinutes).withSecond(0).withNano(0);
    }

    /**
     * Determines if a flight was on-time (within 15 minutes of schedule)
     */
    private boolean isOnTime(ASQPFlightRecord record) {
        if (record.getScheduledCrsDeparture() == null || record.getGateDeparture().isEmpty()) {
            return false;
        }
        Integer delay = calculateDepartureDelay(record);
        return delay != null && delay <= 15;
    }

    /**
     * Calculates departure delay in minutes
     */
    private Integer calculateDepartureDelay(ASQPFlightRecord record) {
        if (record.getScheduledCrsDeparture() == null || record.getGateDeparture().isEmpty()) {
            return null;
        }
        LocalTime scheduled = record.getScheduledCrsDeparture();
        LocalTime actual = record.getGateDeparture().get();

        long minutesDiff = java.time.Duration.between(scheduled, actual).toMinutes();
        // Handle midnight crossing (negative values indicate early departure or date wrap)
        if (minutesDiff < -12 * 60) {
            minutesDiff += 24 * 60;
        }
        return (int) minutesDiff;
    }

    /**
     * Result of flight schedule analysis
     */
    public static class FlightScheduleAnalysis {
        public final String carrierCode;
        public final String flightNumber;
        public final String origin;
        public final String destination;
        public final LocalTime typicalDeparture;
        public final LocalTime typicalArrival;
        public final Set<DayOfWeek> operatingDays;
        public final Map<DayOfWeek, Long> dayFrequency;
        public final long totalOperations;
        public final long operatedCount;
        public final long cancelledCount;
        public final double completionRate;
        public final double onTimeRate;
        public final Double avgDelay;
        public final Map<String, Long> routeFrequencies;

        public FlightScheduleAnalysis(String carrierCode, String flightNumber,
                                     String origin, String destination,
                                     LocalTime typicalDeparture, LocalTime typicalArrival,
                                     Set<DayOfWeek> operatingDays, Map<DayOfWeek, Long> dayFrequency,
                                     long totalOperations, long operatedCount, long cancelledCount,
                                     double completionRate, double onTimeRate, Double avgDelay,
                                     Map<String, Long> routeFrequencies) {
            this.carrierCode = carrierCode;
            this.flightNumber = flightNumber;
            this.origin = origin;
            this.destination = destination;
            this.typicalDeparture = typicalDeparture;
            this.typicalArrival = typicalArrival;
            this.operatingDays = operatingDays;
            this.dayFrequency = dayFrequency;
            this.totalOperations = totalOperations;
            this.operatedCount = operatedCount;
            this.cancelledCount = cancelledCount;
            this.completionRate = completionRate;
            this.onTimeRate = onTimeRate;
            this.avgDelay = avgDelay;
            this.routeFrequencies = routeFrequencies;
        }
    }
}
