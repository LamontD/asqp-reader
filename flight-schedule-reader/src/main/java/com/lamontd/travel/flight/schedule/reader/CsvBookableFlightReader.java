package com.lamontd.travel.flight.schedule.reader;

import com.lamontd.travel.flight.model.BookableFlight;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads BookableFlight records from pipe-delimited CSV files.
 * Expected format: carrier_code|flight_number|origin|destination|departure_date|scheduled_departure|scheduled_arrival
 */
public class CsvBookableFlightReader {
    private static final Logger logger = LoggerFactory.getLogger(CsvBookableFlightReader.class);
    private static final String DELIMITER = "\\|";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    /**
     * Reads scheduled flight records from a file path.
     *
     * @param filePath path to CSV file
     * @return list of BookableFlight objects
     * @throws IOException if file cannot be read
     */
    public List<BookableFlight> readFromFile(Path filePath) throws IOException {
        logger.info("Reading scheduled flights from file: {}", filePath);

        try (BufferedReader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {
            return readFromReader(reader, filePath.getFileName().toString());
        }
    }

    /**
     * Reads scheduled flight records from an input stream (e.g., from resources).
     *
     * @param inputStream input stream containing CSV data
     * @param sourceName name of source for logging
     * @return list of BookableFlight objects
     * @throws IOException if stream cannot be read
     */
    public List<BookableFlight> readFromStream(InputStream inputStream, String sourceName) throws IOException {
        logger.info("Reading scheduled flights from stream: {}", sourceName);

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            return readFromReader(reader, sourceName);
        }
    }

    private List<BookableFlight> readFromReader(BufferedReader reader, String sourceName) throws IOException {
        List<BookableFlight> flights = new ArrayList<>();
        String line;
        int lineNumber = 0;
        int validRecords = 0;
        int skippedRecords = 0;

        while ((line = reader.readLine()) != null) {
            lineNumber++;

            // Skip header line
            if (lineNumber == 1 && line.startsWith("carrier_code")) {
                logger.debug("Skipping header line");
                continue;
            }

            // Skip empty lines
            if (line.trim().isEmpty()) {
                continue;
            }

            try {
                BookableFlight flight = parseLine(line, lineNumber);
                flights.add(flight);
                validRecords++;
            } catch (CsvParseException e) {
                logger.warn("Skipping invalid record at line {}: {}", lineNumber, e.getMessage());
                skippedRecords++;
            }
        }

        logger.info("Loaded {} scheduled flights from {} ({} skipped)",
                    validRecords, sourceName, skippedRecords);

        return flights;
    }

    private BookableFlight parseLine(String line, int lineNumber) throws CsvParseException {
        String[] fields = line.split(DELIMITER, -1);

        if (fields.length != 7) {
            throw new CsvParseException("Expected 7 fields, found " + fields.length);
        }

        try {
            String carrierCode = validateRequired(fields[0], "carrier_code");
            String flightNumber = validateRequired(fields[1], "flight_number");
            String origin = validateAirportCode(fields[2], "origin");
            String destination = validateAirportCode(fields[3], "destination");
            LocalDate departureDate = parseDate(fields[4], "departure_date");
            LocalTime scheduledDeparture = parseTime(fields[5], "scheduled_departure");
            LocalTime scheduledArrival = parseTime(fields[6], "scheduled_arrival");

            return BookableFlight.builder()
                    .carrierCode(carrierCode)
                    .flightNumber(flightNumber)
                    .originAirport(origin)
                    .destinationAirport(destination)
                    .operatingDate(departureDate)
                    .scheduledDepartureTime(scheduledDeparture)
                    .scheduledArrivalTime(scheduledArrival)
                    .build();

        } catch (IllegalArgumentException e) {
            throw new CsvParseException("Validation error: " + e.getMessage());
        }
    }

    private String validateRequired(String value, String fieldName) throws CsvParseException {
        if (value == null || value.trim().isEmpty()) {
            throw new CsvParseException(fieldName + " is required");
        }
        return value.trim();
    }

    private String validateAirportCode(String value, String fieldName) throws CsvParseException {
        String code = validateRequired(value, fieldName);
        if (code.length() != 3) {
            throw new CsvParseException(fieldName + " must be exactly 3 characters, got: " + code);
        }
        return code.toUpperCase();
    }

    private LocalDate parseDate(String value, String fieldName) throws CsvParseException {
        validateRequired(value, fieldName);
        try {
            return LocalDate.parse(value.trim(), DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new CsvParseException(fieldName + " must be in YYYYMMDD format, got: " + value);
        }
    }

    private LocalTime parseTime(String value, String fieldName) throws CsvParseException {
        validateRequired(value, fieldName);
        String timeStr = value.trim();

        try {
            // Handle HHMM or HMM format (e.g., "0600" or "600" both valid)
            int timeInt = Integer.parseInt(timeStr);
            int hours = timeInt / 100;
            int minutes = timeInt % 100;

            if (hours < 0 || hours > 23) {
                throw new CsvParseException(fieldName + " hours must be 0-23, got: " + hours);
            }
            if (minutes < 0 || minutes > 59) {
                throw new CsvParseException(fieldName + " minutes must be 0-59, got: " + minutes);
            }

            return LocalTime.of(hours, minutes);
        } catch (NumberFormatException e) {
            throw new CsvParseException(fieldName + " must be numeric HHMM format, got: " + value);
        }
    }

    /**
     * Exception thrown when CSV parsing fails.
     */
    public static class CsvParseException extends Exception {
        public CsvParseException(String message) {
            super(message);
        }
    }
}
