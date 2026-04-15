package com.lamontd.travel.flight.model;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Represents a scheduled flight - the planned/recurring schedule for a flight route.
 * This captures the "what should happen" rather than "what did happen".
 * Multiple instances may need to be combined to represent a complete recurring schedule.
 */
public class ScheduledFlight {
    private final String carrierCode;
    private final String flightNumber;
    private final String originAirport;
    private final String destinationAirport;
    private final LocalTime scheduledDepartureTime;
    private final LocalTime scheduledArrivalTime;

    // Optional fields for recurring schedules
    private final LocalDate effectiveFrom;      // When this schedule starts
    private final LocalDate effectiveUntil;     // When this schedule ends (nullable for ongoing)
    private final Set<DayOfWeek> daysOfOperation; // Which days this schedule operates (nullable for all days)

    private ScheduledFlight(Builder builder) {
        this.carrierCode = builder.carrierCode;
        this.flightNumber = builder.flightNumber;
        this.originAirport = builder.originAirport;
        this.destinationAirport = builder.destinationAirport;
        this.scheduledDepartureTime = builder.scheduledDepartureTime;
        this.scheduledArrivalTime = builder.scheduledArrivalTime;
        this.effectiveFrom = builder.effectiveFrom;
        this.effectiveUntil = builder.effectiveUntil;
        this.daysOfOperation = builder.daysOfOperation;
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

    public LocalTime getScheduledDepartureTime() {
        return scheduledDepartureTime;
    }

    public LocalTime getScheduledArrivalTime() {
        return scheduledArrivalTime;
    }

    public Optional<LocalDate> getEffectiveFrom() {
        return Optional.ofNullable(effectiveFrom);
    }

    public Optional<LocalDate> getEffectiveUntil() {
        return Optional.ofNullable(effectiveUntil);
    }

    public Optional<Set<DayOfWeek>> getDaysOfOperation() {
        return Optional.ofNullable(daysOfOperation);
    }

    /**
     * Checks if this schedule is effective on the given date
     */
    public boolean isEffectiveOn(LocalDate date) {
        // Check date range
        if (effectiveFrom != null && date.isBefore(effectiveFrom)) {
            return false;
        }
        if (effectiveUntil != null && date.isAfter(effectiveUntil)) {
            return false;
        }

        // Check day of week
        if (daysOfOperation != null && !daysOfOperation.isEmpty()) {
            return daysOfOperation.contains(date.getDayOfWeek());
        }

        return true;
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
        ScheduledFlight that = (ScheduledFlight) o;
        return Objects.equals(carrierCode, that.carrierCode) &&
                Objects.equals(flightNumber, that.flightNumber) &&
                Objects.equals(originAirport, that.originAirport) &&
                Objects.equals(destinationAirport, that.destinationAirport) &&
                Objects.equals(scheduledDepartureTime, that.scheduledDepartureTime) &&
                Objects.equals(scheduledArrivalTime, that.scheduledArrivalTime) &&
                Objects.equals(effectiveFrom, that.effectiveFrom) &&
                Objects.equals(effectiveUntil, that.effectiveUntil) &&
                Objects.equals(daysOfOperation, that.daysOfOperation);
    }

    @Override
    public int hashCode() {
        return Objects.hash(carrierCode, flightNumber, originAirport, destinationAirport,
                scheduledDepartureTime, scheduledArrivalTime, effectiveFrom, effectiveUntil, daysOfOperation);
    }

    @Override
    public String toString() {
        return "ScheduledFlight{" +
                "carrier='" + carrierCode + '\'' +
                ", flightNumber='" + flightNumber + '\'' +
                ", route=" + originAirport + "-" + destinationAirport +
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
        private LocalTime scheduledDepartureTime;
        private LocalTime scheduledArrivalTime;
        private LocalDate effectiveFrom;
        private LocalDate effectiveUntil;
        private Set<DayOfWeek> daysOfOperation;

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

        public Builder scheduledDepartureTime(LocalTime scheduledDepartureTime) {
            this.scheduledDepartureTime = scheduledDepartureTime;
            return this;
        }

        public Builder scheduledArrivalTime(LocalTime scheduledArrivalTime) {
            this.scheduledArrivalTime = scheduledArrivalTime;
            return this;
        }

        public Builder effectiveFrom(LocalDate effectiveFrom) {
            this.effectiveFrom = effectiveFrom;
            return this;
        }

        public Builder effectiveUntil(LocalDate effectiveUntil) {
            this.effectiveUntil = effectiveUntil;
            return this;
        }

        public Builder daysOfOperation(Set<DayOfWeek> daysOfOperation) {
            this.daysOfOperation = daysOfOperation;
            return this;
        }

        public ScheduledFlight build() {
            Objects.requireNonNull(carrierCode, "carrierCode is required");
            Objects.requireNonNull(flightNumber, "flightNumber is required");
            Objects.requireNonNull(originAirport, "originAirport is required");
            Objects.requireNonNull(destinationAirport, "destinationAirport is required");
            Objects.requireNonNull(scheduledDepartureTime, "scheduledDepartureTime is required");
            Objects.requireNonNull(scheduledArrivalTime, "scheduledArrivalTime is required");
            return new ScheduledFlight(this);
        }
    }
}
