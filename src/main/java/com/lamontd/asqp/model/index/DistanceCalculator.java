package com.lamontd.asqp.model.index;

import com.lamontd.asqp.mapper.AirportCodeMapper;
import com.lamontd.asqp.model.AirportInfo;

public class DistanceCalculator {
    private static final double EARTH_RADIUS_MILES = 3958.8;

    private final AirportCodeMapper airportMapper;

    public DistanceCalculator(AirportCodeMapper airportMapper) {
        this.airportMapper = airportMapper;
    }

    /**
     * Calculates distance for a route using Haversine formula
     * @param route Route string in format "ORIGIN-DESTINATION"
     * @return Distance in miles, or 0.0 if airports not found or distance not computable
     */
    public double calculateRouteDistance(String route) {
        String[] parts = route.split("-");
        if (parts.length != 2) {
            return 0.0;
        }

        String origin = parts[0];
        String destination = parts[1];

        var originInfo = airportMapper.getAirportInfo(origin);
        var destInfo = airportMapper.getAirportInfo(destination);

        if (originInfo.isEmpty() || destInfo.isEmpty()) {
            return 0.0;
        }

        var originAirport = originInfo.get();
        var destAirport = destInfo.get();

        if (originAirport.getLatitude().isEmpty() || originAirport.getLongitude().isEmpty() ||
            destAirport.getLatitude().isEmpty() || destAirport.getLongitude().isEmpty()) {
            return 0.0;
        }

        return haversineDistance(
                originAirport.getLatitude().get(),
                originAirport.getLongitude().get(),
                destAirport.getLatitude().get(),
                destAirport.getLongitude().get()
        );
    }

    /**
     * Calculates the great-circle distance between two points using the Haversine formula
     * @param lat1 Latitude of point 1 in degrees
     * @param lon1 Longitude of point 1 in degrees
     * @param lat2 Latitude of point 2 in degrees
     * @param lon2 Longitude of point 2 in degrees
     * @return Distance in miles
     */
    public double haversineDistance(double lat1, double lon1, double lat2, double lon2) {
        // Convert degrees to radians
        double lat1Rad = Math.toRadians(lat1);
        double lon1Rad = Math.toRadians(lon1);
        double lat2Rad = Math.toRadians(lat2);
        double lon2Rad = Math.toRadians(lon2);

        // Haversine formula
        double dLat = lat2Rad - lat1Rad;
        double dLon = lon2Rad - lon1Rad;

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_MILES * c;
    }
}
