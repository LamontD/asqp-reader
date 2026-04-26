package com.lamontd.travel.flight.schedule.util;

import com.lamontd.travel.flight.mapper.AirportCodeMapper;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Optional;

/**
 * Utility methods for validating user input.
 */
public class InputValidator {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final AirportCodeMapper AIRPORT_MAPPER = AirportCodeMapper.getDefault();

    /**
     * Validates an airport code.
     *
     * @param code Airport code to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValidAirportCode(String code) {
        return code != null && AIRPORT_MAPPER.hasAirport(code);
    }

    /**
     * Validates and displays error message for invalid airport code.
     *
     * @param code Airport code to validate
     * @return true if valid, false if invalid (error message displayed)
     */
    public static boolean validateAirportCode(String code) {
        if (!isValidAirportCode(code)) {
            System.out.println("\n✗ Invalid airport code: " + code);
            System.out.println("Airport not found in database.");
            return false;
        }
        return true;
    }

    /**
     * Parses a travel date from string input.
     *
     * @param dateInput Date string in yyyy-MM-dd format
     * @return Optional containing LocalDate if valid, empty if invalid
     */
    public static Optional<LocalDate> parseTravelDate(String dateInput) {
        try {
            return Optional.of(LocalDate.parse(dateInput, DATE_FORMATTER));
        } catch (DateTimeParseException e) {
            return Optional.empty();
        }
    }

    /**
     * Parses and validates a travel date, displaying error message if invalid.
     *
     * @param dateInput Date string in yyyy-MM-dd format
     * @return Optional containing LocalDate if valid, empty if invalid (error message displayed)
     */
    public static Optional<LocalDate> validateTravelDate(String dateInput) {
        Optional<LocalDate> date = parseTravelDate(dateInput);
        if (date.isEmpty()) {
            System.out.println("\nInvalid date format. Please use yyyy-MM-dd (e.g., 2025-06-10).");
        }
        return date;
    }

    /**
     * Displays a validation error message.
     *
     * @param message Error message to display
     */
    public static void displayValidationError(String message) {
        System.out.println("\n" + message);
    }
}
