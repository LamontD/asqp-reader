package com.lamontd.travel.flight.util;

import com.lamontd.travel.flight.mapper.AirportCodeMapper;
import com.lamontd.travel.flight.mapper.CarrierCodeMapper;
import com.lamontd.travel.flight.mapper.CountryCodeMapper;
import com.lamontd.travel.flight.model.ASQPFlightRecord;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Pre-computed indices for efficient data access
 */
public class FlightDataIndex {
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
        CountryCodeMapper countryMapper = CountryCodeMapper.getDefault();
        System.out.printf("\nReference data: %d carriers, %d airports, %d countries%n",
            carrierMapper.size(), airportMapper.size(), countryMapper.size());

        System.out.println("Building data indices for efficient querying...");
        long startTime = System.currentTimeMillis();

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
        System.out.println("Computing route distances...");
        Set<String> uniqueRoutes = records.stream()
                .map(r -> r.getOrigin() + "-" + r.getDestination())
                .collect(Collectors.toSet());

        this.routeDistances = uniqueRoutes.stream()
                .collect(Collectors.toMap(
                        route -> route,
                        distanceCalculator::calculateRouteDistance
                ));

        long endTime = System.currentTimeMillis();
        System.out.printf("Indices built in %d ms%n", (endTime - startTime));
        System.out.printf("Indexed: %d carriers, %d airports, %d tail numbers, %d flight numbers, %d dates, %d routes%n",
                byCarrier.size(), airports.size(), byTailNumber.size(), byFlightNumber.size(), byDate.size(), routeDistances.size());
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
     * Gets the distance in miles between two airports
     * @param origin Origin airport code
     * @param destination Destination airport code
     * @return Distance in miles, or 0.0 if airports not found or distance not computable
     */
    public double getDistance(String origin, String destination) {
        String routeKey = origin + "-" + destination;
        return routeDistances.getOrDefault(routeKey,
                distanceCalculator.calculateRouteDistance(routeKey)); // fallback calculation
    }
}
