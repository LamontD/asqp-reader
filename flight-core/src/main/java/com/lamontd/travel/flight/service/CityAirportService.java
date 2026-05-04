package com.lamontd.travel.flight.service;

import com.lamontd.travel.flight.mapper.AirportCodeMapper;
import com.lamontd.travel.flight.model.AirportInfo;
import com.lamontd.travel.flight.model.USCity;
import com.lamontd.travel.flight.util.DistanceCalculator;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for finding airports near US cities.
 * Uses distance calculations to rank airports by proximity.
 */
public class CityAirportService {
    private final AirportCodeMapper airportMapper;
    private final int maxAirports;
    private final double maxRadiusMiles;

    /**
     * Creates a service with default settings.
     * Default: top 5 airports within 100 miles
     */
    public CityAirportService() {
        this(AirportCodeMapper.getDefault(), 5, 100.0);
    }

    /**
     * Creates a service with custom settings.
     *
     * @param airportMapper Airport data source
     * @param maxAirports Maximum number of airports to return
     * @param maxRadiusMiles Maximum distance in miles to search
     */
    public CityAirportService(AirportCodeMapper airportMapper, int maxAirports, double maxRadiusMiles) {
        this.airportMapper = airportMapper;
        this.maxAirports = maxAirports;
        this.maxRadiusMiles = maxRadiusMiles;
    }

    /**
     * Finds airports near a city, sorted by distance.
     *
     * @param city The city to search near
     * @return List of airports with distances, sorted closest first
     */
    public List<AirportDistance> findNearbyAirports(USCity city) {
        return findNearbyAirports(city, null);
    }

    /**
     * Finds airports near a city that have available flights, sorted by distance.
     *
     * @param city The city to search near
     * @param availableAirports Set of airport codes with available flights (null = all airports)
     * @return List of airports with distances, sorted closest first
     */
    public List<AirportDistance> findNearbyAirports(USCity city, Set<String> availableAirports) {
        List<AirportDistance> nearby = new ArrayList<>();

        // Calculate distance to all airports
        for (AirportInfo airport : airportMapper.getAllAirports()) {
            String airportCode = airport.getCode();

            // Skip if airport doesn't have coordinates
            if (airport.getLatitude().isEmpty() || airport.getLongitude().isEmpty()) continue;

            // Skip if not in available airports list (if provided)
            if (availableAirports != null && !availableAirports.contains(airportCode)) continue;

            // Calculate distance
            double distance = DistanceCalculator.calculateDistance(
                city.latitude(), city.longitude(),
                airport.getLatitude().get(), airport.getLongitude().get()
            );

            // Skip if beyond max radius
            if (distance > maxRadiusMiles) continue;

            nearby.add(new AirportDistance(airport, distance));
        }

        // Sort by distance (closest first)
        nearby.sort(Comparator.comparingDouble(AirportDistance::distance));

        // Return top N airports (but always at least 1 if any exist)
        if (nearby.isEmpty()) {
            return nearby;
        }

        int limit = Math.min(maxAirports, nearby.size());
        // Always include closest airport even if beyond max radius
        if (limit == 0 && !nearby.isEmpty()) {
            limit = 1;
        }

        return nearby.subList(0, limit);
    }

    /**
     * Represents an airport and its distance from a reference point.
     */
    public record AirportDistance(
        AirportInfo airport,
        double distance
    ) {
        public String getAirportCode() {
            return airport.getCode();
        }

        public String getAirportName() {
            return airport.getName();
        }

        public String getAirportCity() {
            return airport.getCity();
        }

        /**
         * Returns formatted distance string (e.g., "12mi", "45mi")
         */
        public String getFormattedDistance() {
            return String.format("%.0fmi", distance);
        }
    }
}
