package com.lamontd.travel.flight.model;

/**
 * Represents a US city with geographic and demographic information.
 * Data sourced from SimpleMaps US Cities database.
 */
public record USCity(
    String city,
    String stateId,
    String stateName,
    double latitude,
    double longitude,
    int population
) {
    /**
     * Returns formatted city, state identifier (e.g., "Columbia, MD")
     */
    public String getCityState() {
        return city + ", " + stateId;
    }

    /**
     * Returns formatted city, full state name (e.g., "Columbia, Maryland")
     */
    public String getCityStateFull() {
        return city + ", " + stateName;
    }
}
