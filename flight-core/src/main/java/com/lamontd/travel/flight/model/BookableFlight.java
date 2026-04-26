package com.lamontd.travel.flight.model;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;

/**
 * Represents a bookable flight instance - a specific flight operating on a specific date.
 * This is used for travel planning and represents "a flight you can book" rather than
 * a recurring schedule pattern.
 *
 * Example: "DL 5030 from LGA to CVG on 2025-01-15 departing at 13:45"
 *
 * This differs from ScheduledFlight which represents recurring patterns with date ranges
 * and days of operation.
 */
public class BookableFlight {
    private final String carrierCode;
    private final String flightNumber;
    private final String originAirport;
    private final String destinationAirport;
    private final LocalDate operatingDate;
    private final LocalTime scheduledDepartureTime;
    private final LocalTime scheduledArrivalTime;

    private BookableFlight(Builder builder) {
        this.carrierCode = builder.carrierCode;
        this.flightNumber = builder.flightNumber;
        this.originAirport = builder.originAirport;
        this.destinationAirport = builder.destinationAirport;
        this.operatingDate = builder.operatingDate;
        this.scheduledDepartureTime = builder.scheduledDepartureTime;
        this.scheduledArrivalTime = builder.scheduledArrivalTime;
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

    public LocalDate getOperatingDate() {
        return operatingDate;
    }

    public LocalTime getScheduledDepartureTime() {
        return scheduledDepartureTime;
    }

    public LocalTime getScheduledArrivalTime() {
        return scheduledArrivalTime;
    }

    /**
     * Returns a unique key for this flight route (carrier + flight number + origin + destination)
     */
    public String getRouteKey() {
        return String.format("%s%s-%s-%s", carrierCode, flightNumber, originAirport, destinationAirport);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BookableFlight that = (BookableFlight) o;
        return Objects.equals(carrierCode, that.carrierCode) &&
                Objects.equals(flightNumber, that.flightNumber) &&
                Objects.equals(originAirport, that.originAirport) &&
                Objects.equals(destinationAirport, that.destinationAirport) &&
                Objects.equals(operatingDate, that.operatingDate) &&
                Objects.equals(scheduledDepartureTime, that.scheduledDepartureTime) &&
                Objects.equals(scheduledArrivalTime, that.scheduledArrivalTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(carrierCode, flightNumber, originAirport, destinationAirport,
                operatingDate, scheduledDepartureTime, scheduledArrivalTime);
    }

    @Override
    public String toString() {
        return "BookableFlight{" +
                "carrier='" + carrierCode + '\'' +
                ", flightNumber='" + flightNumber + '\'' +
                ", route=" + originAirport + "-" + destinationAirport +
                ", date=" + operatingDate +
                ", departure=" + scheduledDepartureTime +
                ", arrival=" + scheduledArrivalTime +
                '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String carrierCode;
        private String flightNumber;
        private String originAirport;
        private String destinationAirport;
        private LocalDate operatingDate;
        private LocalTime scheduledDepartureTime;
        private LocalTime scheduledArrivalTime;

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

        public Builder operatingDate(LocalDate operatingDate) {
            this.operatingDate = operatingDate;
            return this;
        }

        public Builder scheduledDepartureTime(LocalTime scheduledDepartureTime) {
            this.scheduledDepartureTime = scheduledDepartureTime;
            return this;
        }

        public Builder scheduledArrivalTime(LocalTime scheduledArrivalTime) {
            this.scheduledArrivalTime = scheduledArrivalTime;
            return this;
        }

        public BookableFlight build() {
            Objects.requireNonNull(carrierCode, "carrierCode is required");
            Objects.requireNonNull(flightNumber, "flightNumber is required");
            Objects.requireNonNull(originAirport, "originAirport is required");
            Objects.requireNonNull(destinationAirport, "destinationAirport is required");
            Objects.requireNonNull(operatingDate, "operatingDate is required");
            Objects.requireNonNull(scheduledDepartureTime, "scheduledDepartureTime is required");
            Objects.requireNonNull(scheduledArrivalTime, "scheduledArrivalTime is required");
            return new BookableFlight(this);
        }
    }
}
