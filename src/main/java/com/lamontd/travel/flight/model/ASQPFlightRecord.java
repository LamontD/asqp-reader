package com.lamontd.travel.flight.model;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;
import java.util.Optional;

public class ASQPFlightRecord {
    private final String carrierCode;
    private final String flightNumber;
    private final String origin;
    private final String destination;
    private final LocalDate departureDate;
    private final LocalTime scheduledOagDeparture;
    private final LocalTime scheduledCrsDeparture;
    private final LocalTime gateDeparture;
    private final LocalTime scheduledArrival;
    private final LocalTime scheduledCrsArrival;
    private final LocalTime gateArrival;
    private final LocalTime wheelsUp;
    private final LocalTime wheelsDown;
    private final String tailNumber;
    private final String cancellationCode;
    private final Instant utcGateDeparture;
    private final Instant utcGateArrival;
    private final Instant utcWheelsUp;
    private final Instant utcWheelsDown;
    private final Integer carrierDelay;
    private final Integer weatherDelay;
    private final Integer nasDelay;
    private final Integer securityDelay;
    private final Integer lateArrivalDelay;

    private ASQPFlightRecord(Builder builder) {
        this.carrierCode = builder.carrierCode;
        this.flightNumber = builder.flightNumber;
        this.origin = builder.origin;
        this.destination = builder.destination;
        this.departureDate = builder.departureDate;
        this.scheduledOagDeparture = builder.scheduledOagDeparture;
        this.scheduledCrsDeparture = builder.scheduledCrsDeparture;
        this.gateDeparture = builder.gateDeparture;
        this.scheduledArrival = builder.scheduledArrival;
        this.scheduledCrsArrival = builder.scheduledCrsArrival;
        this.gateArrival = builder.gateArrival;
        this.wheelsUp = builder.wheelsUp;
        this.wheelsDown = builder.wheelsDown;
        this.tailNumber = builder.tailNumber;
        this.cancellationCode = builder.cancellationCode;
        this.utcGateDeparture = builder.utcGateDeparture;
        this.utcGateArrival = builder.utcGateArrival;
        this.utcWheelsUp = builder.utcWheelsUp;
        this.utcWheelsDown = builder.utcWheelsDown;
        this.carrierDelay = builder.carrierDelay;
        this.weatherDelay = builder.weatherDelay;
        this.nasDelay = builder.nasDelay;
        this.securityDelay = builder.securityDelay;
        this.lateArrivalDelay = builder.lateArrivalDelay;
    }

    public String getCarrierCode() {
        return carrierCode;
    }

    public String getFlightNumber() {
        return flightNumber;
    }

    public String getOrigin() {
        return origin;
    }

    public String getDestination() {
        return destination;
    }

    public LocalDate getDepartureDate() {
        return departureDate;
    }

    public LocalTime getScheduledOagDeparture() {
        return scheduledOagDeparture;
    }

    public LocalTime getScheduledCrsDeparture() {
        return scheduledCrsDeparture;
    }

    public Optional<LocalTime> getGateDeparture() {
        return Optional.ofNullable(gateDeparture);
    }

    public LocalTime getScheduledArrival() {
        return scheduledArrival;
    }

    public LocalTime getScheduledCrsArrival() {
        return scheduledCrsArrival;
    }

    public Optional<LocalTime> getGateArrival() {
        return Optional.ofNullable(gateArrival);
    }

    public Optional<LocalTime> getWheelsUp() {
        return Optional.ofNullable(wheelsUp);
    }

    public Optional<LocalTime> getWheelsDown() {
        return Optional.ofNullable(wheelsDown);
    }

    public String getTailNumber() {
        return tailNumber;
    }

    public Optional<String> getCancellationCode() {
        return Optional.ofNullable(cancellationCode);
    }

    public boolean isCancelled() {
        return cancellationCode != null && !cancellationCode.trim().isEmpty();
    }

    public Optional<Instant> getUtcGateDeparture() {
        return Optional.ofNullable(utcGateDeparture);
    }

    public Optional<Instant> getUtcGateArrival() {
        return Optional.ofNullable(utcGateArrival);
    }

    public Optional<Instant> getUtcWheelsUp() {
        return Optional.ofNullable(utcWheelsUp);
    }

    public Optional<Instant> getUtcWheelsDown() {
        return Optional.ofNullable(utcWheelsDown);
    }

    public Optional<Integer> getCarrierDelay() {
        return Optional.ofNullable(carrierDelay);
    }

    public Optional<Integer> getWeatherDelay() {
        return Optional.ofNullable(weatherDelay);
    }

    public Optional<Integer> getNasDelay() {
        return Optional.ofNullable(nasDelay);
    }

    public Optional<Integer> getSecurityDelay() {
        return Optional.ofNullable(securityDelay);
    }

    public Optional<Integer> getLateArrivalDelay() {
        return Optional.ofNullable(lateArrivalDelay);
    }

    /**
     * Gets the total delay in minutes across all delay categories
     */
    public int getTotalDelay() {
        return getCarrierDelay().orElse(0) +
               getWeatherDelay().orElse(0) +
               getNasDelay().orElse(0) +
               getSecurityDelay().orElse(0) +
               getLateArrivalDelay().orElse(0);
    }

    /**
     * Checks if this flight has any delay
     */
    public boolean hasDelay() {
        return getTotalDelay() > 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ASQPFlightRecord that = (ASQPFlightRecord) o;
        return Objects.equals(carrierCode, that.carrierCode) &&
                Objects.equals(flightNumber, that.flightNumber) &&
                Objects.equals(origin, that.origin) &&
                Objects.equals(destination, that.destination) &&
                Objects.equals(departureDate, that.departureDate) &&
                Objects.equals(scheduledOagDeparture, that.scheduledOagDeparture) &&
                Objects.equals(scheduledCrsDeparture, that.scheduledCrsDeparture) &&
                Objects.equals(gateDeparture, that.gateDeparture) &&
                Objects.equals(scheduledArrival, that.scheduledArrival) &&
                Objects.equals(scheduledCrsArrival, that.scheduledCrsArrival) &&
                Objects.equals(gateArrival, that.gateArrival) &&
                Objects.equals(wheelsUp, that.wheelsUp) &&
                Objects.equals(wheelsDown, that.wheelsDown) &&
                Objects.equals(tailNumber, that.tailNumber) &&
                Objects.equals(cancellationCode, that.cancellationCode) &&
                Objects.equals(utcGateDeparture, that.utcGateDeparture) &&
                Objects.equals(utcGateArrival, that.utcGateArrival) &&
                Objects.equals(utcWheelsUp, that.utcWheelsUp) &&
                Objects.equals(utcWheelsDown, that.utcWheelsDown) &&
                Objects.equals(carrierDelay, that.carrierDelay) &&
                Objects.equals(weatherDelay, that.weatherDelay) &&
                Objects.equals(nasDelay, that.nasDelay) &&
                Objects.equals(securityDelay, that.securityDelay) &&
                Objects.equals(lateArrivalDelay, that.lateArrivalDelay);
    }

    @Override
    public int hashCode() {
        return Objects.hash(carrierCode, flightNumber, origin, destination, departureDate,
                scheduledOagDeparture, scheduledCrsDeparture, gateDeparture,
                scheduledArrival, scheduledCrsArrival, gateArrival,
                wheelsUp, wheelsDown, tailNumber, cancellationCode,
                utcGateDeparture, utcGateArrival, utcWheelsUp, utcWheelsDown,
                carrierDelay, weatherDelay, nasDelay, securityDelay, lateArrivalDelay);
    }

    @Override
    public String toString() {
        return "ASQPFlightRecord{" +
                "carrierCode='" + carrierCode + '\'' +
                ", flightNumber='" + flightNumber + '\'' +
                ", origin='" + origin + '\'' +
                ", destination='" + destination + '\'' +
                ", departureDate=" + departureDate +
                ", cancelled=" + isCancelled() +
                '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String carrierCode;
        private String flightNumber;
        private String origin;
        private String destination;
        private LocalDate departureDate;
        private LocalTime scheduledOagDeparture;
        private LocalTime scheduledCrsDeparture;
        private LocalTime gateDeparture;
        private LocalTime scheduledArrival;
        private LocalTime scheduledCrsArrival;
        private LocalTime gateArrival;
        private LocalTime wheelsUp;
        private LocalTime wheelsDown;
        private String tailNumber;
        private String cancellationCode;
        private Instant utcGateDeparture;
        private Instant utcGateArrival;
        private Instant utcWheelsUp;
        private Instant utcWheelsDown;
        private Integer carrierDelay;
        private Integer weatherDelay;
        private Integer nasDelay;
        private Integer securityDelay;
        private Integer lateArrivalDelay;

        public Builder carrierCode(String carrierCode) {
            this.carrierCode = carrierCode;
            return this;
        }

        public Builder flightNumber(String flightNumber) {
            this.flightNumber = flightNumber;
            return this;
        }

        public Builder origin(String origin) {
            this.origin = origin;
            return this;
        }

        public Builder destination(String destination) {
            this.destination = destination;
            return this;
        }

        public Builder departureDate(LocalDate departureDate) {
            this.departureDate = departureDate;
            return this;
        }

        public Builder scheduledOagDeparture(LocalTime scheduledOagDeparture) {
            this.scheduledOagDeparture = scheduledOagDeparture;
            return this;
        }

        public Builder scheduledCrsDeparture(LocalTime scheduledCrsDeparture) {
            this.scheduledCrsDeparture = scheduledCrsDeparture;
            return this;
        }

        public Builder gateDeparture(LocalTime gateDeparture) {
            this.gateDeparture = gateDeparture;
            return this;
        }

        public Builder scheduledArrival(LocalTime scheduledArrival) {
            this.scheduledArrival = scheduledArrival;
            return this;
        }

        public Builder scheduledCrsArrival(LocalTime scheduledCrsArrival) {
            this.scheduledCrsArrival = scheduledCrsArrival;
            return this;
        }

        public Builder gateArrival(LocalTime gateArrival) {
            this.gateArrival = gateArrival;
            return this;
        }

        public Builder wheelsUp(LocalTime wheelsUp) {
            this.wheelsUp = wheelsUp;
            return this;
        }

        public Builder wheelsDown(LocalTime wheelsDown) {
            this.wheelsDown = wheelsDown;
            return this;
        }

        public Builder tailNumber(String tailNumber) {
            this.tailNumber = tailNumber;
            return this;
        }

        public Builder cancellationCode(String cancellationCode) {
            this.cancellationCode = cancellationCode;
            return this;
        }

        public Builder utcGateDeparture(Instant utcGateDeparture) {
            this.utcGateDeparture = utcGateDeparture;
            return this;
        }

        public Builder utcGateArrival(Instant utcGateArrival) {
            this.utcGateArrival = utcGateArrival;
            return this;
        }

        public Builder utcWheelsUp(Instant utcWheelsUp) {
            this.utcWheelsUp = utcWheelsUp;
            return this;
        }

        public Builder utcWheelsDown(Instant utcWheelsDown) {
            this.utcWheelsDown = utcWheelsDown;
            return this;
        }

        public Builder carrierDelay(Integer carrierDelay) {
            this.carrierDelay = carrierDelay;
            return this;
        }

        public Builder weatherDelay(Integer weatherDelay) {
            this.weatherDelay = weatherDelay;
            return this;
        }

        public Builder nasDelay(Integer nasDelay) {
            this.nasDelay = nasDelay;
            return this;
        }

        public Builder securityDelay(Integer securityDelay) {
            this.securityDelay = securityDelay;
            return this;
        }

        public Builder lateArrivalDelay(Integer lateArrivalDelay) {
            this.lateArrivalDelay = lateArrivalDelay;
            return this;
        }

        public ASQPFlightRecord build() {
            return new ASQPFlightRecord(this);
        }
    }
}
