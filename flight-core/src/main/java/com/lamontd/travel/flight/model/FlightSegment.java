package com.lamontd.travel.flight.model;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Represents a bookable flight segment - either a direct leg or a through connection.
 * For multi-leg flights, passengers can book any segment of the journey.
 *
 * Example: Flight WN 4283 operates ORD → DAL → MCO
 * This creates 3 bookable segments:
 * - ORD → DAL (direct leg)
 * - DAL → MCO (direct leg)
 * - ORD → MCO (through connection via DAL)
 */
public class FlightSegment {
    private final String carrierCode;
    private final String flightNumber;
    private final String originAirport;
    private final String destinationAirport;
    private final SegmentType segmentType;
    private final List<String> intermediateStops;
    private final int legCount;

    private FlightSegment(Builder builder) {
        this.carrierCode = builder.carrierCode;
        this.flightNumber = builder.flightNumber;
        this.originAirport = builder.originAirport;
        this.destinationAirport = builder.destinationAirport;
        this.segmentType = builder.segmentType;
        this.intermediateStops = builder.intermediateStops != null ?
            Collections.unmodifiableList(builder.intermediateStops) : Collections.emptyList();
        this.legCount = builder.legCount;
    }

    public String getCarrierCode() {
        return carrierCode;
    }

    public String getFlightNumber() {
        return flightNumber;
    }

    public String getOriginAirport() {
        return originAirport;
    }

    public String getDestinationAirport() {
        return destinationAirport;
    }

    public SegmentType getSegmentType() {
        return segmentType;
    }

    public List<String> getIntermediateStops() {
        return intermediateStops;
    }

    public int getLegCount() {
        return legCount;
    }

    /**
     * Returns the segment key (origin-destination pair)
     */
    public String getSegmentKey() {
        return originAirport + "-" + destinationAirport;
    }

    /**
     * Returns the full flight key (carrier + flight number)
     */
    public String getFlightKey() {
        return carrierCode + flightNumber;
    }

    /**
     * Checks if this is a direct flight (single leg, no stops)
     */
    public boolean isDirect() {
        return segmentType == SegmentType.DIRECT_LEG;
    }

    /**
     * Checks if this is a through connection (multiple legs)
     */
    public boolean isThrough() {
        return segmentType == SegmentType.THROUGH_CONNECTION;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FlightSegment that = (FlightSegment) o;
        return legCount == that.legCount &&
                Objects.equals(carrierCode, that.carrierCode) &&
                Objects.equals(flightNumber, that.flightNumber) &&
                Objects.equals(originAirport, that.originAirport) &&
                Objects.equals(destinationAirport, that.destinationAirport) &&
                segmentType == that.segmentType &&
                Objects.equals(intermediateStops, that.intermediateStops);
    }

    @Override
    public int hashCode() {
        return Objects.hash(carrierCode, flightNumber, originAirport, destinationAirport,
                segmentType, intermediateStops, legCount);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("FlightSegment{")
          .append(carrierCode).append(flightNumber)
          .append(": ").append(originAirport).append("→").append(destinationAirport);

        if (!intermediateStops.isEmpty()) {
            sb.append(" via ").append(String.join(",", intermediateStops));
        }

        sb.append(" (").append(segmentType).append(", ").append(legCount).append(" leg");
        if (legCount != 1) {
            sb.append("s");
        }
        sb.append(")}");

        return sb.toString();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String carrierCode;
        private String flightNumber;
        private String originAirport;
        private String destinationAirport;
        private SegmentType segmentType;
        private List<String> intermediateStops;
        private int legCount;

        public Builder carrierCode(String carrierCode) {
            this.carrierCode = carrierCode;
            return this;
        }

        public Builder flightNumber(String flightNumber) {
            this.flightNumber = flightNumber;
            return this;
        }

        public Builder originAirport(String originAirport) {
            this.originAirport = originAirport;
            return this;
        }

        public Builder destinationAirport(String destinationAirport) {
            this.destinationAirport = destinationAirport;
            return this;
        }

        public Builder segmentType(SegmentType segmentType) {
            this.segmentType = segmentType;
            return this;
        }

        public Builder intermediateStops(List<String> intermediateStops) {
            this.intermediateStops = intermediateStops;
            return this;
        }

        public Builder legCount(int legCount) {
            this.legCount = legCount;
            return this;
        }

        public FlightSegment build() {
            Objects.requireNonNull(carrierCode, "carrierCode is required");
            Objects.requireNonNull(flightNumber, "flightNumber is required");
            Objects.requireNonNull(originAirport, "originAirport is required");
            Objects.requireNonNull(destinationAirport, "destinationAirport is required");
            Objects.requireNonNull(segmentType, "segmentType is required");

            if (legCount <= 0) {
                throw new IllegalArgumentException("legCount must be positive");
            }

            if (segmentType == SegmentType.DIRECT_LEG && legCount != 1) {
                throw new IllegalArgumentException("DIRECT_LEG segments must have legCount=1");
            }

            if (segmentType == SegmentType.THROUGH_CONNECTION && legCount < 2) {
                throw new IllegalArgumentException("THROUGH_CONNECTION segments must have legCount>=2");
            }

            return new FlightSegment(this);
        }
    }

    /**
     * Type of flight segment from a passenger booking perspective
     */
    public enum SegmentType {
        /**
         * Single physical leg with no intermediate stops.
         * Example: ORD → DAL (one takeoff, one landing)
         */
        DIRECT_LEG,

        /**
         * Multiple legs on the same flight number, creating a through connection.
         * Passenger stays on the same flight (or connects with same flight number).
         * Example: ORD → MCO via DAL (two legs: ORD→DAL and DAL→MCO)
         */
        THROUGH_CONNECTION
    }
}
