package com.lamontd.travel.flight.asqp.index;

import com.lamontd.travel.flight.index.RouteIndex;
import com.lamontd.travel.flight.mapper.AirportCodeMapper;
import com.lamontd.travel.flight.mapper.CarrierCodeMapper;
import com.lamontd.travel.flight.util.DistanceCalculator;
import com.lamontd.travel.flight.util.PerformanceTimer;
import com.lamontd.travel.flight.asqp.model.ASQPFlightRecord;
import com.lamontd.travel.flight.asqp.FlightConverter;
import com.lamontd.travel.flight.model.ScheduledFlight;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Pre-computed indices for efficient data access
 */
public class FlightDataIndex implements RouteIndex {
    private static final Logger logger = LoggerFactory.getLogger(FlightDataIndex.class);
    public final List<ASQPFlightRecord> allRecords;
    public final AirportCodeMapper airportMapper;

    // Indexed by various keys for O(1) or O(log n) lookups
    public final Map<String, List<ASQPFlightRecord>> byCarrier;
    public final Map<String, List<ASQPFlightRecord>> byOriginAirport;
    public final Map<String, List<ASQPFlightRecord>> byDestinationAirport;
    public final Map<String, List<ASQPFlightRecord>> byTailNumber;
    public final Map<String, List<ASQPFlightRecord>> byFlightNumber;
    public final Map<LocalDate, List<ASQPFlightRecord>> byDate;

    // Pre-computed route distances (origin-destination -> distance in miles)
    public final Map<String, Double> routeDistances;

    // Pre-computed scheduled flights by route (origin-destination -> list of schedules)
    public final Map<String, List<ScheduledFlight>> scheduledFlightsByRoute;

    // Cached statistics (computed once)
    public final long totalFlights;
    public final long operatedFlights;
    public final long cancelledFlights;
    public final Map<String, Long> carrierCounts;
    public final LocalDate minDate;
    public final LocalDate maxDate;
    public final long uniqueCarriers;
    public final long uniqueAirports;

    private final DistanceCalculator distanceCalculator;

    public FlightDataIndex(List<ASQPFlightRecord> records) {
        this.allRecords = records;
        this.totalFlights = records.size();
        this.airportMapper = AirportCodeMapper.getDefault();
        this.distanceCalculator = new DistanceCalculator(airportMapper);

        // Show mapper info
        CarrierCodeMapper carrierMapper = CarrierCodeMapper.getDefault();
        logger.info("Reference data loaded: {} carriers, {} airports",
                carrierMapper.size(), airportMapper.size());

        try (var timer = new PerformanceTimer("Build flight data indices")) {

        // Build all indices in a single pass where possible
        this.byCarrier = records.stream()
                .collect(Collectors.groupingBy(ASQPFlightRecord::getCarrierCode));

        this.byOriginAirport = records.stream()
                .collect(Collectors.groupingBy(ASQPFlightRecord::getOrigin));

        this.byDestinationAirport = records.stream()
                .collect(Collectors.groupingBy(ASQPFlightRecord::getDestination));

        this.byTailNumber = records.stream()
                .filter(r -> r.getTailNumber() != null && !r.getTailNumber().isEmpty())
                .collect(Collectors.groupingBy(ASQPFlightRecord::getTailNumber));

        this.byFlightNumber = records.stream()
                .collect(Collectors.groupingBy(
                        r -> r.getCarrierCode() + r.getFlightNumber()
                ));

        this.byDate = records.stream()
                .collect(Collectors.groupingBy(ASQPFlightRecord::getDepartureDate));

        // Compute statistics once
        this.operatedFlights = records.stream()
                .filter(r -> !r.isCancelled())
                .count();
        this.cancelledFlights = totalFlights - operatedFlights;

        this.carrierCounts = byCarrier.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> (long) e.getValue().size()
                ));

        this.minDate = records.stream()
                .map(ASQPFlightRecord::getDepartureDate)
                .min(LocalDate::compareTo)
                .orElse(null);

        this.maxDate = records.stream()
                .map(ASQPFlightRecord::getDepartureDate)
                .max(LocalDate::compareTo)
                .orElse(null);

        this.uniqueCarriers = byCarrier.size();

        Set<String> airports = new HashSet<>();
        airports.addAll(byOriginAirport.keySet());
        airports.addAll(byDestinationAirport.keySet());
        this.uniqueAirports = airports.size();

            // Pre-compute distances for all unique routes
            logger.debug("Computing route distances...");
            Set<String> uniqueRoutes = records.stream()
                    .map(r -> r.getOrigin() + "-" + r.getDestination())
                    .collect(Collectors.toSet());

            this.routeDistances = uniqueRoutes.stream()
                    .collect(Collectors.toMap(
                            route -> route,
                            distanceCalculator::calculateRouteDistance
                    ));

            // Build scheduled flights index
            logger.debug("Building scheduled flights index...");
            this.scheduledFlightsByRoute = buildScheduledFlightsIndex(records);
        }

        logger.info("Indices: {} carriers, {} airports, {} tail numbers, {} flight numbers, {} dates, {} routes, {} scheduled flights",
                byCarrier.size(), this.uniqueAirports, byTailNumber.size(),
                byFlightNumber.size(), byDate.size(), routeDistances.size(),
                scheduledFlightsByRoute.values().stream().mapToInt(List::size).sum());
    }

    public List<ASQPFlightRecord> getByCarrier(String carrierCode) {
        return byCarrier.getOrDefault(carrierCode, Collections.emptyList());
    }

    public List<ASQPFlightRecord> getByOriginAirport(String airportCode) {
        return byOriginAirport.getOrDefault(airportCode, Collections.emptyList());
    }

    public List<ASQPFlightRecord> getByTailNumber(String tailNumber) {
        return byTailNumber.getOrDefault(tailNumber.toUpperCase(), Collections.emptyList());
    }

    public List<ASQPFlightRecord> getByFlightNumber(String carrierCode, String flightNumber) {
        return byFlightNumber.getOrDefault(carrierCode + flightNumber, Collections.emptyList());
    }

    /**
     * Gets all scheduled flights for a specific route
     * @param origin Origin airport code
     * @param destination Destination airport code
     * @return List of scheduled flights operating this route
     */
    public List<ScheduledFlight> getScheduledFlightsByRoute(String origin, String destination) {
        String routeKey = origin + "-" + destination;
        return scheduledFlightsByRoute.getOrDefault(routeKey, Collections.emptyList());
    }

    /**
     * Builds the scheduled flights index by grouping flight records and inferring schedules
     */
    private Map<String, List<ScheduledFlight>> buildScheduledFlightsIndex(List<ASQPFlightRecord> records) {
        // Group by carrier + flight# + origin + destination (unique scheduled flight key)
        Map<String, List<ASQPFlightRecord>> groupedBySchedule = records.stream()
                .collect(Collectors.groupingBy(r ->
                    String.format("%s|%s|%s|%s",
                        r.getCarrierCode(),
                        r.getFlightNumber(),
                        r.getOrigin(),
                        r.getDestination()
                    )));

        // Convert each group to a ScheduledFlight
        List<ScheduledFlight> allSchedules = groupedBySchedule.values().stream()
                .map(this::inferScheduledFlight)
                .filter(Objects::nonNull)
                .toList();

        // Index by route (origin-destination)
        return allSchedules.stream()
                .collect(Collectors.groupingBy(
                    s -> s.getOriginAirport() + "-" + s.getDestinationAirport()
                ));
    }

    /**
     * Infers a ScheduledFlight from a list of observed flight records
     */
    private ScheduledFlight inferScheduledFlight(List<ASQPFlightRecord> records) {
        if (records.isEmpty()) {
            return null;
        }

        ASQPFlightRecord template = records.get(0);

        // Determine date range
        LocalDate minDate = records.stream()
                .map(ASQPFlightRecord::getDepartureDate)
                .min(LocalDate::compareTo)
                .orElse(null);

        LocalDate maxDate = records.stream()
                .map(ASQPFlightRecord::getDepartureDate)
                .max(LocalDate::compareTo)
                .orElse(null);

        // Determine days of operation
        Set<DayOfWeek> daysOfOperation = records.stream()
                .map(r -> r.getDepartureDate().getDayOfWeek())
                .collect(Collectors.toSet());

        // Use most common scheduled time (mode) - prefer CRS times
        var departureTime = records.stream()
                .map(ASQPFlightRecord::getScheduledCrsDeparture)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(t -> t, Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(template.getScheduledCrsDeparture());

        var arrivalTime = records.stream()
                .map(ASQPFlightRecord::getScheduledCrsArrival)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(t -> t, Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(template.getScheduledCrsArrival());

        return ScheduledFlight.builder()
                .carrierCode(template.getCarrierCode())
                .flightNumber(template.getFlightNumber())
                .originAirport(template.getOrigin())
                .destinationAirport(template.getDestination())
                .scheduledDepartureTime(departureTime)
                .scheduledArrivalTime(arrivalTime)
                .effectiveFrom(minDate)
                .effectiveUntil(maxDate)
                .daysOfOperation(daysOfOperation.size() == 7 ? null : daysOfOperation)
                .build();
    }

    // RouteIndex interface implementation

    @Override
    public Set<String> getOriginAirports() {
        return byOriginAirport.keySet();
    }

    @Override
    public Set<String> getDestinationAirports() {
        return byDestinationAirport.keySet();
    }

    @Override
    public Set<String> getActualRoutes() {
        return routeDistances.keySet();
    }

    @Override
    public double getRouteDistance(String origin, String destination) {
        String routeKey = origin + "-" + destination;
        return routeDistances.getOrDefault(routeKey,
                distanceCalculator.calculateRouteDistance(routeKey));
    }
}
