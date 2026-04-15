package com.lamontd.travel.flight.reader;

import com.lamontd.travel.flight.mapper.AirportCodeMapper;
import com.lamontd.travel.flight.model.AirportInfo;
import com.lamontd.travel.flight.model.ASQPFlightRecord;
import com.lamontd.travel.flight.validation.FlightRecordValidationException;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

public class CsvFlightRecordReader {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HHmm");

    private static final String[] HEADERS = {
            "carrier_code", "flight_number", "origin", "destination", "departure_date",
            "scheduled_oag_departure", "scheduled_crs_departure", "gate_departure",
            "scheduled_arrival", "scheduled_crs_arrival", "gate_arrival",
            "wheels_up", "wheels_down", "tail_number", "cancellation_code",
            "carrier_delay", "weather_delay", "nas_delay", "security_delay", "late_arrival_delay"
    };

    private final AirportCodeMapper airportMapper;

    public CsvFlightRecordReader() {
        this(null);
    }

    public CsvFlightRecordReader(AirportCodeMapper airportMapper) {
        this.airportMapper = airportMapper;
    }

    public List<ASQPFlightRecord> readFromFile(Path filePath) throws IOException {
        try (Reader reader = Files.newBufferedReader(filePath)) {
            return readFromReader(reader);
        }
    }

    public List<ASQPFlightRecord> readFromReader(Reader reader) throws IOException {
        List<ASQPFlightRecord> records = new ArrayList<>();
        CSVFormat csvFormat = CSVFormat.DEFAULT
                .builder()
                .setDelimiter('|')
                .setHeader(HEADERS)
                .setSkipHeaderRecord(true)
                .setTrim(true)
                .build();

        try (CSVParser parser = new CSVParser(reader, csvFormat)) {
            int recordNumber = 1;
            for (CSVRecord csvRecord : parser) {
                try {
                    ASQPFlightRecord flightRecord = parseRecord(csvRecord, recordNumber);
                    records.add(flightRecord);
                } catch (FlightRecordValidationException e) {
                    System.err.println("Skipping invalid record: " + e.getMessage());
                }
                recordNumber++;
            }
        }
        return records;
    }

    private ASQPFlightRecord parseRecord(CSVRecord csvRecord, int recordNumber) throws FlightRecordValidationException {
        ASQPFlightRecord.Builder builder = ASQPFlightRecord.builder();

        builder.carrierCode(validateRequiredString(csvRecord, "carrier_code", recordNumber));
        builder.flightNumber(validateRequiredString(csvRecord, "flight_number", recordNumber));

        String origin = validateAirportCode(csvRecord, "origin", recordNumber);
        String destination = validateAirportCode(csvRecord, "destination", recordNumber);
        LocalDate departureDate = parseDate(csvRecord, "departure_date", recordNumber);

        builder.origin(origin);
        builder.destination(destination);
        builder.departureDate(departureDate);
        builder.scheduledOagDeparture(parseRequiredTime(csvRecord, "scheduled_oag_departure", recordNumber));
        builder.scheduledCrsDeparture(parseRequiredTime(csvRecord, "scheduled_crs_departure", recordNumber));
        builder.scheduledArrival(parseRequiredTime(csvRecord, "scheduled_arrival", recordNumber));
        builder.scheduledCrsArrival(parseRequiredTime(csvRecord, "scheduled_crs_arrival", recordNumber));

        String cancellationCode = csvRecord.get("cancellation_code");
        boolean isCancelled = cancellationCode != null && !cancellationCode.trim().isEmpty();

        LocalTime gateDeparture = null;
        LocalTime gateArrival = null;
        LocalTime wheelsUp = null;
        LocalTime wheelsDown = null;

        if (isCancelled) {
            builder.cancellationCode(cancellationCode.trim());
            builder.gateDeparture(null);
            builder.gateArrival(null);
            builder.wheelsUp(null);
            builder.wheelsDown(null);
            // Tail number is optional for cancelled flights
            String tailNumber = csvRecord.get("tail_number");
            if (tailNumber != null && !tailNumber.trim().isEmpty()) {
                builder.tailNumber(tailNumber.trim());
            }
        } else {
            gateDeparture = parseOptionalTime(csvRecord, "gate_departure");
            gateArrival = parseOptionalTime(csvRecord, "gate_arrival");
            wheelsUp = parseOptionalTime(csvRecord, "wheels_up");
            wheelsDown = parseOptionalTime(csvRecord, "wheels_down");
            builder.gateDeparture(gateDeparture);
            builder.gateArrival(gateArrival);
            builder.wheelsUp(wheelsUp);
            builder.wheelsDown(wheelsDown);
            // Tail number is required for operated flights
            builder.tailNumber(validateRequiredString(csvRecord, "tail_number", recordNumber));
        }

        // Calculate UTC times if airport mapper is available
        if (airportMapper != null) {
            Instant utcGateDeparture = null;
            Instant utcGateArrival = null;
            Instant utcWheelsUpTime = null;
            Instant utcWheelsDownTime = null;

            // Calculate gate departure UTC time
            if (gateDeparture != null) {
                utcGateDeparture = calculateUtcTime(origin, departureDate, gateDeparture);
                builder.utcGateDeparture(utcGateDeparture);
            }

            // Calculate gate arrival UTC time (with midnight crossing check)
            if (gateArrival != null && utcGateDeparture != null) {
                // First try calculating arrival with same date
                utcGateArrival = calculateUtcTime(destination, departureDate, gateArrival);

                // If UTC arrival is before UTC departure, the flight crossed midnight
                // Recalculate arrival with next day
                if (utcGateArrival != null && utcGateArrival.isBefore(utcGateDeparture)) {
                    utcGateArrival = calculateUtcTime(destination, departureDate.plusDays(1), gateArrival);
                }

                builder.utcGateArrival(utcGateArrival);
            } else if (gateArrival != null) {
                // No gate departure to compare against, use same date
                builder.utcGateArrival(calculateUtcTime(destination, departureDate, gateArrival));
            }

            // Calculate wheels up UTC time (with midnight crossing check)
            if (wheelsUp != null && utcGateDeparture != null) {
                // First try calculating wheels up with same date
                utcWheelsUpTime = calculateUtcTime(origin, departureDate, wheelsUp);

                // If UTC wheels up is before UTC gate departure, the flight crossed midnight
                // Recalculate wheels up with next day
                if (utcWheelsUpTime != null && utcWheelsUpTime.isBefore(utcGateDeparture)) {
                    utcWheelsUpTime = calculateUtcTime(origin, departureDate.plusDays(1), wheelsUp);
                }

                builder.utcWheelsUp(utcWheelsUpTime);
            } else if (wheelsUp != null) {
                // No gate departure to compare against, use same date
                builder.utcWheelsUp(calculateUtcTime(origin, departureDate, wheelsUp));
            }

            // Calculate wheels down UTC time (with midnight crossing check)
            if (wheelsDown != null && utcWheelsUpTime != null) {
                // First try calculating wheels down with same date
                utcWheelsDownTime = calculateUtcTime(destination, departureDate, wheelsDown);

                // If UTC wheels down is before UTC wheels up, the flight crossed midnight
                // Recalculate wheels down with next day
                if (utcWheelsDownTime != null && utcWheelsDownTime.isBefore(utcWheelsUpTime)) {
                    utcWheelsDownTime = calculateUtcTime(destination, departureDate.plusDays(1), wheelsDown);
                }

                builder.utcWheelsDown(utcWheelsDownTime);
            } else if (wheelsDown != null && utcGateDeparture != null) {
                // No wheels up but have gate departure, compare against that
                utcWheelsDownTime = calculateUtcTime(destination, departureDate, wheelsDown);

                if (utcWheelsDownTime != null && utcWheelsDownTime.isBefore(utcGateDeparture)) {
                    utcWheelsDownTime = calculateUtcTime(destination, departureDate.plusDays(1), wheelsDown);
                }

                builder.utcWheelsDown(utcWheelsDownTime);
            } else if (wheelsDown != null) {
                // No reference time to compare against, use same date
                builder.utcWheelsDown(calculateUtcTime(destination, departureDate, wheelsDown));
            }
        }

        // Parse delay fields (optional)
        builder.carrierDelay(parseOptionalInteger(csvRecord, "carrier_delay"));
        builder.weatherDelay(parseOptionalInteger(csvRecord, "weather_delay"));
        builder.nasDelay(parseOptionalInteger(csvRecord, "nas_delay"));
        builder.securityDelay(parseOptionalInteger(csvRecord, "security_delay"));
        builder.lateArrivalDelay(parseOptionalInteger(csvRecord, "late_arrival_delay"));

        return builder.build();
    }

    private String validateRequiredString(CSVRecord record, String fieldName, int recordNumber)
            throws FlightRecordValidationException {
        String value = record.get(fieldName);
        if (value == null || value.trim().isEmpty()) {
            throw new FlightRecordValidationException("Required field is missing or empty", recordNumber, fieldName);
        }
        return value.trim();
    }

    private String validateAirportCode(CSVRecord record, String fieldName, int recordNumber)
            throws FlightRecordValidationException {
        String code = validateRequiredString(record, fieldName, recordNumber);
        if (code.length() != 3) {
            throw new FlightRecordValidationException(
                    "Airport code must be exactly 3 characters, found: " + code,
                    recordNumber,
                    fieldName);
        }
        return code.toUpperCase();
    }

    private LocalDate parseDate(CSVRecord record, String fieldName, int recordNumber)
            throws FlightRecordValidationException {
        String dateStr = validateRequiredString(record, fieldName, recordNumber);
        try {
            return LocalDate.parse(dateStr, DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new FlightRecordValidationException(
                    "Invalid date format. Expected YYYYMMDD, found: " + dateStr,
                    recordNumber,
                    fieldName,
                    e);
        }
    }

    private LocalTime parseRequiredTime(CSVRecord record, String fieldName, int recordNumber)
            throws FlightRecordValidationException {
        String timeStr = validateRequiredString(record, fieldName, recordNumber);
        return parseTime(timeStr, fieldName, recordNumber);
    }

    private LocalTime parseOptionalTime(CSVRecord record, String fieldName) {
        String timeStr = record.get(fieldName);
        if (timeStr == null || timeStr.trim().isEmpty() || "0".equals(timeStr.trim())) {
            return null;
        }
        try {
            return parseTime(timeStr.trim(), fieldName, -1);
        } catch (FlightRecordValidationException e) {
            return null;
        }
    }

    private LocalTime parseTime(String timeStr, String fieldName, int recordNumber)
            throws FlightRecordValidationException {
        try {
            // Pad with leading zeros to ensure 4-digit format (HHMM)
            while (timeStr.length() < 4) {
                timeStr = "0" + timeStr;
            }
            return LocalTime.parse(timeStr, TIME_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new FlightRecordValidationException(
                    "Invalid time format. Expected HHMM, found: " + timeStr,
                    recordNumber,
                    fieldName,
                    e);
        }
    }

    private Integer parseOptionalInteger(CSVRecord record, String fieldName) {
        String value = record.get(fieldName);
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            int intValue = Integer.parseInt(value.trim());
            // Return null for 0 or negative values (treat as no delay)
            return intValue > 0 ? intValue : null;
        } catch (NumberFormatException e) {
            // Invalid integer, return null
            return null;
        }
    }

    /**
     * Converts a local time at an airport to UTC using the airport's timezone offset.
     *
     * @param airportCode The IATA airport code
     * @param date The local date
     * @param localTime The local time at the airport
     * @return The UTC instant, or null if timezone information is not available
     */
    private Instant calculateUtcTime(String airportCode, LocalDate date, LocalTime localTime) {
        return airportMapper.getAirportInfo(airportCode)
                .flatMap(AirportInfo::getTimezone)
                .map(timezoneOffset -> {
                    // Create LocalDateTime from date and time
                    LocalDateTime localDateTime = LocalDateTime.of(date, localTime);
                    // Convert to UTC by subtracting the timezone offset
                    // OpenFlights timezone is hours offset from UTC (e.g., -5.0 for EST)
                    int offsetHours = (int) Math.floor(timezoneOffset);
                    int offsetMinutes = (int) ((timezoneOffset - offsetHours) * 60);
                    ZoneOffset zoneOffset = ZoneOffset.ofHoursMinutes(offsetHours, offsetMinutes);
                    return localDateTime.toInstant(zoneOffset);
                })
                .orElse(null);
    }
}
