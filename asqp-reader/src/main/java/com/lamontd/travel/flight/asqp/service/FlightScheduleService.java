package com.lamontd.travel.flight.asqp.service;

import com.lamontd.travel.flight.asqp.index.FlightDataIndex;
import com.lamontd.travel.flight.asqp.model.ASQPFlightRecord;
import com.lamontd.travel.flight.model.FlightSegment;

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
     * Analyzes a flight number to determine its typical schedule.
     * Now includes multi-leg segment detection and extraction.
     */
    public FlightScheduleAnalysis analyzeFlightSchedule(String carrierCode, String flightNumber) {
        List<ASQPFlightRecord> records = index.getByFlightNumber(carrierCode, flightNumber);

        if (records.isEmpty()) {
            return null;
        }

        // CRITICAL: Group by DATE first to detect multi-leg operations
        // (multiple legs on same day indicate a through-flight)
        Map<String, List<ASQPFlightRecord>> recordsByDate = records.stream()
                .collect(Collectors.groupingBy(r -> r.getDepartureDate().toString()));

        // Extract all bookable segments from all dates
        Set<FlightSegment> allSegments = new LinkedHashSet<>();
        Set<String> observedRoutePatterns = new HashSet<>();

        for (List<ASQPFlightRecord> dailyRecords : recordsByDate.values()) {
            // Sort by scheduled departure time to get correct leg sequence
            List<ASQPFlightRecord> sortedLegs = dailyRecords.stream()
                    .sorted(Comparator.comparing(r -> r.getScheduledCrsDeparture() != null ?
                            r.getScheduledCrsDeparture() : LocalTime.MIN))
                    .toList();

            // Extract segments for this date
            List<FlightSegment> dailySegments = extractBookableSegments(sortedLegs);
            allSegments.addAll(dailySegments);

            // Build route pattern string (e.g., "ORD-DAL-MCO" for 2-leg flight)
            StringBuilder pattern = new StringBuilder(sortedLegs.get(0).getOrigin());
            for (ASQPFlightRecord leg : sortedLegs) {
                pattern.append("-").append(leg.getDestination());
            }
            observedRoutePatterns.add(pattern.toString());
        }

        // Determine the most common route pattern
        String routePattern = observedRoutePatterns.stream()
                .max(Comparator.comparingInt(String::length))
                .orElse(null);

        // Group by simple route to check for multi-route operation
        Map<String, List<ASQPFlightRecord>> byRoute = records.stream()
                .collect(Collectors.groupingBy(r -> r.getOrigin() + "-" + r.getDestination()));

        boolean isMultiRoute = byRoute.size() > 1;

        // For reporting purposes, use the most common single-leg route
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
                routeFrequencies,
                new ArrayList<>(allSegments),
                isMultiRoute,
                routePattern
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
     * Extracts all bookable segments from a set of legs that operate on the same day.
     * For N legs, this generates:
     * - N direct leg segments (one per leg)
     * - N*(N-1)/2 through connection segments (all combinations of origin to destination)
     *
     * Example: ORD→DAL→MCO (2 legs) produces:
     * - ORD→DAL (direct)
     * - DAL→MCO (direct)
     * - ORD→MCO (through via DAL)
     *
     * @param dailyLegs List of flight legs for the same flight number on the same date,
     *                  must be sorted by departure time
     * @return List of all bookable segments
     */
    private List<FlightSegment> extractBookableSegments(List<ASQPFlightRecord> dailyLegs) {
        if (dailyLegs.isEmpty()) {
            return Collections.emptyList();
        }

        // Single leg - simple case
        if (dailyLegs.size() == 1) {
            ASQPFlightRecord leg = dailyLegs.get(0);
            return Collections.singletonList(
                FlightSegment.builder()
                    .carrierCode(leg.getCarrierCode())
                    .flightNumber(leg.getFlightNumber())
                    .originAirport(leg.getOrigin())
                    .destinationAirport(leg.getDestination())
                    .segmentType(FlightSegment.SegmentType.DIRECT_LEG)
                    .intermediateStops(Collections.emptyList())
                    .legCount(1)
                    .build()
            );
        }

        // Multi-leg: Generate all bookable combinations
        List<FlightSegment> segments = new ArrayList<>();
        String carrierCode = dailyLegs.get(0).getCarrierCode();
        String flightNumber = dailyLegs.get(0).getFlightNumber();

        // Generate all segments: direct legs + through connections
        for (int i = 0; i < dailyLegs.size(); i++) {
            for (int j = i; j < dailyLegs.size(); j++) {
                String origin = dailyLegs.get(i).getOrigin();
                String destination = dailyLegs.get(j).getDestination();

                if (i == j) {
                    // Direct leg (single physical leg)
                    segments.add(FlightSegment.builder()
                        .carrierCode(carrierCode)
                        .flightNumber(flightNumber)
                        .originAirport(origin)
                        .destinationAirport(destination)
                        .segmentType(FlightSegment.SegmentType.DIRECT_LEG)
                        .intermediateStops(Collections.emptyList())
                        .legCount(1)
                        .build());
                } else {
                    // Through connection (multiple legs)
                    List<String> intermediateStops = new ArrayList<>();
                    for (int k = i; k < j; k++) {
                        intermediateStops.add(dailyLegs.get(k).getDestination());
                    }

                    segments.add(FlightSegment.builder()
                        .carrierCode(carrierCode)
                        .flightNumber(flightNumber)
                        .originAirport(origin)
                        .destinationAirport(destination)
                        .segmentType(FlightSegment.SegmentType.THROUGH_CONNECTION)
                        .intermediateStops(intermediateStops)
                        .legCount(j - i + 1)
                        .build());
                }
            }
        }

        return segments;
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

        // New fields for multi-leg support
        public final List<FlightSegment> bookableSegments;
        public final boolean isMultiRoute;
        public final String routePattern;

        public FlightScheduleAnalysis(String carrierCode, String flightNumber,
                                     String origin, String destination,
                                     LocalTime typicalDeparture, LocalTime typicalArrival,
                                     Set<DayOfWeek> operatingDays, Map<DayOfWeek, Long> dayFrequency,
                                     long totalOperations, long operatedCount, long cancelledCount,
                                     double completionRate, double onTimeRate, Double avgDelay,
                                     Map<String, Long> routeFrequencies,
                                     List<FlightSegment> bookableSegments,
                                     boolean isMultiRoute, String routePattern) {
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
            this.bookableSegments = bookableSegments != null ?
                Collections.unmodifiableList(bookableSegments) : Collections.emptyList();
            this.isMultiRoute = isMultiRoute;
            this.routePattern = routePattern;
        }
    }
}
