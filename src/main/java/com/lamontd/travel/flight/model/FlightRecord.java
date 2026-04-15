package com.lamontd.travel.flight.model;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;
import java.util.Optional;

/**
 * Represents an observed flight - what actually happened for a specific flight instance.
 * This is the information you would see on an airport display screen.
 * Think of this as "what did happen" rather than "what should happen".
 */
public class FlightRecord {
    // Flight identification
    private final String carrierCode;
    private final String flightNumber;
    private final LocalDate operatingDate;

    // Route information
    private final String originAirport;
    private final String destinationAirport;

    // Aircraft information
    private final String tailNumber;

    // Actual times (all optional as flight may not have operated)
    private final LocalTime actualDepartureTime;
    private final LocalTime actualArrivalTime;
    private final LocalTime wheelsUpTime;
    private final LocalTime wheelsDownTime;

    // UTC times for cross-timezone analysis
    private final Instant utcDepartureTime;
    private final Instant utcArrivalTime;
    private final Instant utcWheelsUpTime;
    private final Instant utcWheelsDownTime;

    // Status information
    private final FlightStatus status;
    private final String cancellationCode;

    // Delay information
    private final DelayInfo delayInfo;

    private FlightRecord(Builder builder) {
        this.carrierCode = builder.carrierCode;
        this.flightNumber = builder.flightNumber;
        this.operatingDate = builder.operatingDate;
        this.originAirport = builder.originAirport;
        this.destinationAirport = builder.destinationAirport;
        this.tailNumber = builder.tailNumber;
        this.actualDepartureTime = builder.actualDepartureTime;
        this.actualArrivalTime = builder.actualArrivalTime;
        this.wheelsUpTime = builder.wheelsUpTime;
        this.wheelsDownTime = builder.wheelsDownTime;
        this.utcDepartureTime = builder.utcDepartureTime;
        this.utcArrivalTime = builder.utcArrivalTime;
        this.utcWheelsUpTime = builder.utcWheelsUpTime;
        this.utcWheelsDownTime = builder.utcWheelsDownTime;
        this.status = builder.status != null ? builder.status : FlightStatus.SCHEDULED;
        this.cancellationCode = builder.cancellationCode;
        this.delayInfo = builder.delayInfo;
    }

    public String getCarrierCode() {
        return carrierCode;
    }

    public String getFlightNumber() {
        return flightNumber;
    }

    public LocalDate getOperatingDate() {
        return operatingDate;
    }

    public String getOriginAirport() {
        return originAirport;
    }

    public String getDestinationAirport() {
        return destinationAirport;
    }

    public Optional<String> getTailNumber() {
        return Optional.ofNullable(tailNumber);
    }

    public Optional<LocalTime> getActualDepartureTime() {
        return Optional.ofNullable(actualDepartureTime);
    }

    public Optional<LocalTime> getActualArrivalTime() {
        return Optional.ofNullable(actualArrivalTime);
    }

    public Optional<LocalTime> getWheelsUpTime() {
        return Optional.ofNullable(wheelsUpTime);
    }

    public Optional<LocalTime> getWheelsDownTime() {
        return Optional.ofNullable(wheelsDownTime);
    }

    public Optional<Instant> getUtcDepartureTime() {
        return Optional.ofNullable(utcDepartureTime);
    }

    public Optional<Instant> getUtcArrivalTime() {
        return Optional.ofNullable(utcArrivalTime);
    }

    public Optional<Instant> getUtcWheelsUpTime() {
        return Optional.ofNullable(utcWheelsUpTime);
    }

    public Optional<Instant> getUtcWheelsDownTime() {
        return Optional.ofNullable(utcWheelsDownTime);
    }

    public FlightStatus getStatus() {
        return status;
    }

    public Optional<String> getCancellationCode() {
        return Optional.ofNullable(cancellationCode);
    }

    public boolean isCancelled() {
        return status == FlightStatus.CANCELLED;
    }

    public Optional<DelayInfo> getDelayInfo() {
        return Optional.ofNullable(delayInfo);
    }

    /**
     * Returns a unique key for this flight instance (carrier + flight number + date)
     */
    public String getFlightKey() {
        return String.format("%s%s-%s", carrierCode, flightNumber, operatingDate);
    }

    /**
     * Returns the route key (carrier + flight number + origin + destination)
     */
    public String getRouteKey() {
        return String.format("%s%s-%s-%s", carrierCode, flightNumber, originAirport, destinationAirport);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FlightRecord that = (FlightRecord) o;
        return Objects.equals(carrierCode, that.carrierCode) &&
                Objects.equals(flightNumber, that.flightNumber) &&
                Objects.equals(operatingDate, that.operatingDate) &&
                Objects.equals(originAirport, that.originAirport) &&
                Objects.equals(destinationAirport, that.destinationAirport);
    }

    @Override
    public int hashCode() {
        return Objects.hash(carrierCode, flightNumber, operatingDate, originAirport, destinationAirport);
    }

    @Override
    public String toString() {
        return "FlightRecord{" +
                "carrier='" + carrierCode + '\'' +
                ", flightNumber='" + flightNumber + '\'' +
                ", date=" + operatingDate +
                ", route=" + originAirport + "-" + destinationAirport +
                ", status=" + status +
                '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String carrierCode;
        private String flightNumber;
        private LocalDate operatingDate;
        private String originAirport;
        private String destinationAirport;
        private String tailNumber;
        private LocalTime actualDepartureTime;
        private LocalTime actualArrivalTime;
        private LocalTime wheelsUpTime;
        private LocalTime wheelsDownTime;
        private Instant utcDepartureTime;
        private Instant utcArrivalTime;
        private Instant utcWheelsUpTime;
        private Instant utcWheelsDownTime;
        private FlightStatus status;
        private String cancellationCode;
        private DelayInfo delayInfo;

        public Builder carrierCode(String carrierCode) {
            this.carrierCode = carrierCode;
            return this;
        }

        public Builder flightNumber(String flightNumber) {
            this.flightNumber = flightNumber;
            return this;
        }

        public Builder operatingDate(LocalDate operatingDate) {
            this.operatingDate = operatingDate;
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

        public Builder tailNumber(String tailNumber) {
            this.tailNumber = tailNumber;
            return this;
        }

        public Builder actualDepartureTime(LocalTime actualDepartureTime) {
            this.actualDepartureTime = actualDepartureTime;
            return this;
        }

        public Builder actualArrivalTime(LocalTime actualArrivalTime) {
            this.actualArrivalTime = actualArrivalTime;
            return this;
        }

        public Builder wheelsUpTime(LocalTime wheelsUpTime) {
            this.wheelsUpTime = wheelsUpTime;
            return this;
        }

        public Builder wheelsDownTime(LocalTime wheelsDownTime) {
            this.wheelsDownTime = wheelsDownTime;
            return this;
        }

        public Builder utcDepartureTime(Instant utcDepartureTime) {
            this.utcDepartureTime = utcDepartureTime;
            return this;
        }

        public Builder utcArrivalTime(Instant utcArrivalTime) {
            this.utcArrivalTime = utcArrivalTime;
            return this;
        }

        public Builder utcWheelsUpTime(Instant utcWheelsUpTime) {
            this.utcWheelsUpTime = utcWheelsUpTime;
            return this;
        }

        public Builder utcWheelsDownTime(Instant utcWheelsDownTime) {
            this.utcWheelsDownTime = utcWheelsDownTime;
            return this;
        }

        public Builder status(FlightStatus status) {
            this.status = status;
            return this;
        }

        public Builder cancellationCode(String cancellationCode) {
            this.cancellationCode = cancellationCode;
            return this;
        }

        public Builder delayInfo(DelayInfo delayInfo) {
            this.delayInfo = delayInfo;
            return this;
        }

        public FlightRecord build() {
            Objects.requireNonNull(carrierCode, "carrierCode is required");
            Objects.requireNonNull(flightNumber, "flightNumber is required");
            Objects.requireNonNull(operatingDate, "operatingDate is required");
            Objects.requireNonNull(originAirport, "originAirport is required");
            Objects.requireNonNull(destinationAirport, "destinationAirport is required");
            return new FlightRecord(this);
        }
    }

    /**
     * Enum representing the status of a flight
     */
    public enum FlightStatus {
        SCHEDULED,      // Flight is scheduled but not yet departed
        DEPARTED,       // Flight has departed
        IN_FLIGHT,      // Flight is in the air
        LANDED,         // Flight has landed
        ARRIVED,        // Flight has arrived at gate
        CANCELLED,      // Flight was cancelled
        DIVERTED        // Flight was diverted to another airport
    }

    /**
     * Represents delay information for a flight
     */
    public static class DelayInfo {
        private final Integer carrierDelay;
        private final Integer weatherDelay;
        private final Integer nasDelay;
        private final Integer securityDelay;
        private final Integer lateAircraftDelay;

        public DelayInfo(Integer carrierDelay, Integer weatherDelay, Integer nasDelay,
                        Integer securityDelay, Integer lateAircraftDelay) {
            this.carrierDelay = carrierDelay;
            this.weatherDelay = weatherDelay;
            this.nasDelay = nasDelay;
            this.securityDelay = securityDelay;
            this.lateAircraftDelay = lateAircraftDelay;
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

        public Optional<Integer> getLateAircraftDelay() {
            return Optional.ofNullable(lateAircraftDelay);
        }

        public int getTotalDelay() {
            return getCarrierDelay().orElse(0) +
                   getWeatherDelay().orElse(0) +
                   getNasDelay().orElse(0) +
                   getSecurityDelay().orElse(0) +
                   getLateAircraftDelay().orElse(0);
        }

        public boolean hasDelay() {
            return getTotalDelay() > 0;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            DelayInfo delayInfo = (DelayInfo) o;
            return Objects.equals(carrierDelay, delayInfo.carrierDelay) &&
                    Objects.equals(weatherDelay, delayInfo.weatherDelay) &&
                    Objects.equals(nasDelay, delayInfo.nasDelay) &&
                    Objects.equals(securityDelay, delayInfo.securityDelay) &&
                    Objects.equals(lateAircraftDelay, delayInfo.lateAircraftDelay);
        }

        @Override
        public int hashCode() {
            return Objects.hash(carrierDelay, weatherDelay, nasDelay, securityDelay, lateAircraftDelay);
        }

        @Override
        public String toString() {
            return "DelayInfo{total=" + getTotalDelay() + " min}";
        }
    }
}
