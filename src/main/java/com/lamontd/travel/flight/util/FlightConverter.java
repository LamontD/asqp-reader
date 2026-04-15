package com.lamontd.travel.flight.util;

import com.lamontd.travel.flight.model.ASQPFlightRecord;
import com.lamontd.travel.flight.model.FlightRecord;
import com.lamontd.travel.flight.model.ScheduledFlight;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Utility class for converting between different flight model representations
 */
public class FlightConverter {

    /**
     * Converts an ASQP flight record to a ScheduledFlight
     * Uses the scheduled times from the ASQP record
     */
    public static ScheduledFlight toScheduledFlight(ASQPFlightRecord asqpRecord) {
        return ScheduledFlight.builder()
                .carrierCode(asqpRecord.getCarrierCode())
                .flightNumber(asqpRecord.getFlightNumber())
                .originAirport(asqpRecord.getOrigin())
                .destinationAirport(asqpRecord.getDestination())
                .scheduledDepartureTime(asqpRecord.getScheduledCrsDeparture())
                .scheduledArrivalTime(asqpRecord.getScheduledCrsArrival())
                .effectiveFrom(asqpRecord.getDepartureDate())
                .effectiveUntil(asqpRecord.getDepartureDate())
                .build();
    }

    /**
     * Converts an ASQP flight record to an observed FlightRecord
     * Uses the actual times and status from the ASQP record
     */
    public static FlightRecord toFlightRecord(ASQPFlightRecord asqpRecord) {
        FlightRecord.Builder builder = FlightRecord.builder()
                .carrierCode(asqpRecord.getCarrierCode())
                .flightNumber(asqpRecord.getFlightNumber())
                .operatingDate(asqpRecord.getDepartureDate())
                .originAirport(asqpRecord.getOrigin())
                .destinationAirport(asqpRecord.getDestination());

        // Add tail number if present
        if (asqpRecord.getTailNumber() != null && !asqpRecord.getTailNumber().trim().isEmpty()) {
            builder.tailNumber(asqpRecord.getTailNumber());
        }

        // Determine status
        FlightRecord.FlightStatus status;
        if (asqpRecord.isCancelled()) {
            status = FlightRecord.FlightStatus.CANCELLED;
            asqpRecord.getCancellationCode().ifPresent(builder::cancellationCode);
        } else if (asqpRecord.getGateArrival().isPresent()) {
            status = FlightRecord.FlightStatus.ARRIVED;
        } else if (asqpRecord.getWheelsDown().isPresent()) {
            status = FlightRecord.FlightStatus.LANDED;
        } else if (asqpRecord.getWheelsUp().isPresent()) {
            status = FlightRecord.FlightStatus.IN_FLIGHT;
        } else if (asqpRecord.getGateDeparture().isPresent()) {
            status = FlightRecord.FlightStatus.DEPARTED;
        } else {
            status = FlightRecord.FlightStatus.SCHEDULED;
        }
        builder.status(status);

        // Add actual times
        asqpRecord.getGateDeparture().ifPresent(builder::actualDepartureTime);
        asqpRecord.getGateArrival().ifPresent(builder::actualArrivalTime);
        asqpRecord.getWheelsUp().ifPresent(builder::wheelsUpTime);
        asqpRecord.getWheelsDown().ifPresent(builder::wheelsDownTime);

        // Add UTC times
        asqpRecord.getUtcGateDeparture().ifPresent(builder::utcDepartureTime);
        asqpRecord.getUtcGateArrival().ifPresent(builder::utcArrivalTime);
        asqpRecord.getUtcWheelsUp().ifPresent(builder::utcWheelsUpTime);
        asqpRecord.getUtcWheelsDown().ifPresent(builder::utcWheelsDownTime);

        // Add delay information
        if (asqpRecord.hasDelay()) {
            FlightRecord.DelayInfo delayInfo = new FlightRecord.DelayInfo(
                    asqpRecord.getCarrierDelay().orElse(null),
                    asqpRecord.getWeatherDelay().orElse(null),
                    asqpRecord.getNasDelay().orElse(null),
                    asqpRecord.getSecurityDelay().orElse(null),
                    asqpRecord.getLateArrivalDelay().orElse(null)
            );
            builder.delayInfo(delayInfo);
        }

        return builder.build();
    }

    /**
     * Builds a recurring ScheduledFlight from multiple ASQP records of the same flight
     * Analyzes the records to determine days of operation and date range
     */
    public static ScheduledFlight buildRecurringSchedule(List<ASQPFlightRecord> asqpRecords) {
        if (asqpRecords.isEmpty()) {
            throw new IllegalArgumentException("Cannot build schedule from empty list");
        }

        // Group by carrier+flight+route to ensure they're all the same flight
        Map<String, List<ASQPFlightRecord>> grouped = asqpRecords.stream()
                .collect(Collectors.groupingBy(r ->
                    String.format("%s%s-%s-%s",
                        r.getCarrierCode(),
                        r.getFlightNumber(),
                        r.getOrigin(),
                        r.getDestination())));

        if (grouped.size() > 1) {
            throw new IllegalArgumentException("Cannot build schedule from records of different flights");
        }

        // Use the first record as template
        ASQPFlightRecord template = asqpRecords.get(0);

        // Determine date range
        var minDate = asqpRecords.stream()
                .map(ASQPFlightRecord::getDepartureDate)
                .min(java.time.LocalDate::compareTo)
                .orElseThrow();

        var maxDate = asqpRecords.stream()
                .map(ASQPFlightRecord::getDepartureDate)
                .max(java.time.LocalDate::compareTo)
                .orElseThrow();

        // Determine days of operation
        Set<DayOfWeek> daysOfOperation = asqpRecords.stream()
                .map(r -> r.getDepartureDate().getDayOfWeek())
                .collect(Collectors.toSet());

        // Use the most common scheduled time (mode)
        var departureTime = asqpRecords.stream()
                .collect(Collectors.groupingBy(ASQPFlightRecord::getScheduledCrsDeparture, Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(template.getScheduledCrsDeparture());

        var arrivalTime = asqpRecords.stream()
                .collect(Collectors.groupingBy(ASQPFlightRecord::getScheduledCrsArrival, Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(template.getScheduledCrsArrival());

        return ScheduledFlight.builder()
                .carrierCode(template.getCarrierCode())
                .flightNumber(template.getFlightNumber())
                .originAirport(template.getOrigin())
                .destinationAirport(template.getDestination())
                .scheduledDepartureTime(departureTime)
                .scheduledArrivalTime(arrivalTime)
                .effectiveFrom(minDate)
                .effectiveUntil(maxDate)
                .daysOfOperation(daysOfOperation.size() == 7 ? null : daysOfOperation)
                .build();
    }

    /**
     * Converts a list of ASQP records to FlightRecords
     */
    public static List<FlightRecord> toFlightRecords(List<ASQPFlightRecord> asqpRecords) {
        return asqpRecords.stream()
                .map(FlightConverter::toFlightRecord)
                .collect(Collectors.toList());
    }

    /**
     * Converts a list of ASQP records to ScheduledFlights (one per record)
     */
    public static List<ScheduledFlight> toScheduledFlights(List<ASQPFlightRecord> asqpRecords) {
        return asqpRecords.stream()
                .map(FlightConverter::toScheduledFlight)
                .collect(Collectors.toList());
    }
}
