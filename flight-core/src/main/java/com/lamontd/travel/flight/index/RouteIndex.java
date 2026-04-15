package com.lamontd.travel.flight.index;

import java.util.Set;

/**
 * Minimal index interface for route network analysis.
 * Provides airport lists and route distance lookups needed for graph operations.
 */
public interface RouteIndex {
    /**
     * @return All airports that serve as origins in the dataset
     */
    Set<String> getOriginAirports();

    /**
     * @return All airports that serve as destinations in the dataset
     */
    Set<String> getDestinationAirports();

    /**
     * Calculate or retrieve the distance between two airports in miles.
     * @param origin Origin airport code
     * @param destination Destination airport code
     * @return Distance in miles, or 0.0 if airports not found or distance not computable
     */
    double getRouteDistance(String origin, String destination);
}
