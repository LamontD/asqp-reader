package com.lamontd.travel.flight.schedule.index;

import com.lamontd.travel.flight.model.BookableFlight;
import com.lamontd.travel.flight.util.PerformanceTimer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * In-memory index for efficient querying of scheduled flight data.
 * Pre-computes indices by carrier, route, airport, and flight number.
 */
public class ScheduleFlightIndex {
    private static final Logger logger = LoggerFactory.getLogger(ScheduleFlightIndex.class);

    private final List<BookableFlight> allFlights;
    private final Map<String, List<BookableFlight>> byCarrier;
    private final Map<String, List<BookableFlight>> byOrigin;
    private final Map<String, List<BookableFlight>> byDestination;
    private final Map<String, List<BookableFlight>> byRoute;
    private final Map<String, List<BookableFlight>> byFlightNumber;
    private final Map<String, List<BookableFlight>> byCarrierAndFlightNumber;

    public ScheduleFlightIndex(List<BookableFlight> flights) {
        try (var timer = new PerformanceTimer("Building schedule flight index")) {
            this.allFlights = List.copyOf(flights);

            this.byCarrier = buildCarrierIndex(flights);
            this.byOrigin = buildOriginIndex(flights);
            this.byDestination = buildDestinationIndex(flights);
            this.byRoute = buildRouteIndex(flights);
            this.byFlightNumber = buildFlightNumberIndex(flights);
            this.byCarrierAndFlightNumber = buildCarrierFlightNumberIndex(flights);

            logger.info("Index contains {} scheduled flights", allFlights.size());
            logger.debug("  {} carriers, {} origins, {} destinations, {} routes",
                    byCarrier.size(), byOrigin.size(), byDestination.size(), byRoute.size());
        }
    }

    private Map<String, List<BookableFlight>> buildCarrierIndex(List<BookableFlight> flights) {
        return flights.stream()
                .collect(Collectors.groupingBy(
                        BookableFlight::getCarrierCode,
                        Collectors.toList()));
    }

    private Map<String, List<BookableFlight>> buildOriginIndex(List<BookableFlight> flights) {
        return flights.stream()
                .collect(Collectors.groupingBy(
                        BookableFlight::getOriginAirport,
                        Collectors.toList()));
    }

    private Map<String, List<BookableFlight>> buildDestinationIndex(List<BookableFlight> flights) {
        return flights.stream()
                .collect(Collectors.groupingBy(
                        BookableFlight::getDestinationAirport,
                        Collectors.toList()));
    }

    private Map<String, List<BookableFlight>> buildRouteIndex(List<BookableFlight> flights) {
        return flights.stream()
                .collect(Collectors.groupingBy(
                        f -> f.getOriginAirport() + "-" + f.getDestinationAirport(),
                        Collectors.toList()));
    }

    private Map<String, List<BookableFlight>> buildFlightNumberIndex(List<BookableFlight> flights) {
        return flights.stream()
                .collect(Collectors.groupingBy(
                        BookableFlight::getFlightNumber,
                        Collectors.toList()));
    }

    private Map<String, List<BookableFlight>> buildCarrierFlightNumberIndex(List<BookableFlight> flights) {
        return flights.stream()
                .collect(Collectors.groupingBy(
                        f -> f.getCarrierCode() + f.getFlightNumber(),
                        Collectors.toList()));
    }

    /**
     * Returns all scheduled flights in the index.
     */
    public List<BookableFlight> getAllFlights() {
        return allFlights;
    }

    /**
     * Returns all flights for a specific carrier.
     */
    public List<BookableFlight> getFlightsByCarrier(String carrierCode) {
        return byCarrier.getOrDefault(carrierCode.toUpperCase(), List.of());
    }

    /**
     * Returns all flights departing from a specific airport.
     */
    public List<BookableFlight> getFlightsByOrigin(String airportCode) {
        return byOrigin.getOrDefault(airportCode.toUpperCase(), List.of());
    }

    /**
     * Returns all flights arriving at a specific airport.
     */
    public List<BookableFlight> getFlightsByDestination(String airportCode) {
        return byDestination.getOrDefault(airportCode.toUpperCase(), List.of());
    }

    /**
     * Returns all flights on a specific route.
     */
    public List<BookableFlight> getFlightsByRoute(String origin, String destination) {
        String routeKey = origin.toUpperCase() + "-" + destination.toUpperCase();
        return byRoute.getOrDefault(routeKey, List.of());
    }

    /**
     * Returns all flights with a specific flight number (across all carriers).
     */
    public List<BookableFlight> getFlightsByFlightNumber(String flightNumber) {
        return byFlightNumber.getOrDefault(flightNumber, List.of());
    }

    /**
     * Returns flights for a specific carrier and flight number combination.
     */
    public List<BookableFlight> getFlightsByCarrierAndFlightNumber(String carrierCode, String flightNumber) {
        String key = carrierCode.toUpperCase() + flightNumber;
        return byCarrierAndFlightNumber.getOrDefault(key, List.of());
    }

    /**
     * Returns flights operating on a specific date.
     */
    public List<BookableFlight> getFlightsOnDate(LocalDate date) {
        return allFlights.stream()
                .filter(f -> f.getOperatingDate().equals(date))
                .collect(Collectors.toList());
    }

    /**
     * Returns flights on a route operating on a specific date.
     */
    public List<BookableFlight> getFlightsByRouteOnDate(String origin, String destination, LocalDate date) {
        return getFlightsByRoute(origin, destination).stream()
                .filter(f -> f.getOperatingDate().equals(date))
                .collect(Collectors.toList());
    }

    /**
     * Returns all unique carrier codes in the index.
     */
    public Set<String> getAllCarriers() {
        return byCarrier.keySet();
    }

    /**
     * Returns all unique airport codes (origins and destinations).
     */
    public Set<String> getAllAirports() {
        Set<String> airports = new HashSet<>(byOrigin.keySet());
        airports.addAll(byDestination.keySet());
        return airports;
    }

    /**
     * Returns all unique routes (origin-destination pairs).
     */
    public Set<String> getAllRoutes() {
        return byRoute.keySet();
    }

    /**
     * Returns total count of scheduled flights.
     */
    public int getFlightCount() {
        return allFlights.size();
    }

    /**
     * Returns statistics about the index.
     */
    public IndexStats getStats() {
        return new IndexStats(
                allFlights.size(),
                byCarrier.size(),
                byOrigin.size(),
                byDestination.size(),
                byRoute.size()
        );
    }

    /**
     * Returns set of airport codes that have flights on a given date.
     * Includes both origin and destination airports.
     *
     * @param date Date to check for flight availability
     * @return Set of airport codes with flights on that date
     */
    public Set<String> getAirportsWithFlights(LocalDate date) {
        Set<String> airports = new HashSet<>();

        for (BookableFlight flight : allFlights) {
            if (flight.getOperatingDate().equals(date)) {
                airports.add(flight.getOriginAirport());
                airports.add(flight.getDestinationAirport());
            }
        }

        return airports;
    }

    /**
     * Returns all unique dates that have flight data.
     */
    public Set<LocalDate> getAvailableDates() {
        return allFlights.stream()
                .map(BookableFlight::getOperatingDate)
                .collect(Collectors.toSet());
    }

    /**
     * Returns the earliest date with flight data.
     */
    public Optional<LocalDate> getMinDate() {
        return allFlights.stream()
                .map(BookableFlight::getOperatingDate)
                .min(LocalDate::compareTo);
    }

    /**
     * Returns the latest date with flight data.
     */
    public Optional<LocalDate> getMaxDate() {
        return allFlights.stream()
                .map(BookableFlight::getOperatingDate)
                .max(LocalDate::compareTo);
    }

    /**
     * Container for index statistics.
     */
    public record IndexStats(
            int totalFlights,
            int totalCarriers,
            int totalOriginAirports,
            int totalDestinationAirports,
            int totalRoutes
    ) {}
}
