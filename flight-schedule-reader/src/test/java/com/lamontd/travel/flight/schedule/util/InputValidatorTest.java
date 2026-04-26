package com.lamontd.travel.flight.schedule.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class InputValidatorTest {
    private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;

    @BeforeEach
    void setUp() {
        System.setOut(new PrintStream(outputStream));
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
    }

    @Test
    void testIsValidAirportCode_validCode() {
        assertTrue(InputValidator.isValidAirportCode("JFK"));
        assertTrue(InputValidator.isValidAirportCode("LAX"));
        assertTrue(InputValidator.isValidAirportCode("ORD"));
    }

    @Test
    void testIsValidAirportCode_invalidCode() {
        assertFalse(InputValidator.isValidAirportCode("XXX"));
        assertFalse(InputValidator.isValidAirportCode("ZZZ"));
        assertFalse(InputValidator.isValidAirportCode("999"));
    }

    @Test
    void testIsValidAirportCode_nullCode() {
        assertFalse(InputValidator.isValidAirportCode(null));
    }

    @Test
    void testValidateAirportCode_validCode() {
        assertTrue(InputValidator.validateAirportCode("JFK"));
        String output = outputStream.toString();
        assertFalse(output.contains("Invalid airport code"));
    }

    @Test
    void testValidateAirportCode_invalidCode() {
        assertFalse(InputValidator.validateAirportCode("XXX"));
        String output = outputStream.toString();
        assertTrue(output.contains("✗ Invalid airport code: XXX"));
        assertTrue(output.contains("Airport not found in database"));
    }

    @Test
    void testParseTravelDate_validDate() {
        Optional<LocalDate> date = InputValidator.parseTravelDate("2025-06-10");

        assertTrue(date.isPresent());
        assertEquals(LocalDate.of(2025, 6, 10), date.get());
    }

    @Test
    void testParseTravelDate_invalidFormat() {
        Optional<LocalDate> date = InputValidator.parseTravelDate("06/10/2025");

        assertTrue(date.isEmpty());
    }

    @Test
    void testParseTravelDate_invalidDate() {
        Optional<LocalDate> date = InputValidator.parseTravelDate("2025-13-45");

        assertTrue(date.isEmpty());
    }

    @Test
    void testParseTravelDate_emptyString() {
        Optional<LocalDate> date = InputValidator.parseTravelDate("");

        assertTrue(date.isEmpty());
    }

    @Test
    void testValidateTravelDate_validDate() {
        Optional<LocalDate> date = InputValidator.validateTravelDate("2025-12-25");

        assertTrue(date.isPresent());
        assertEquals(LocalDate.of(2025, 12, 25), date.get());
        String output = outputStream.toString();
        assertFalse(output.contains("Invalid date format"));
    }

    @Test
    void testValidateTravelDate_invalidDate() {
        Optional<LocalDate> date = InputValidator.validateTravelDate("2025/06/10");

        assertTrue(date.isEmpty());
        String output = outputStream.toString();
        assertTrue(output.contains("Invalid date format"));
        assertTrue(output.contains("yyyy-MM-dd"));
    }

    @Test
    void testDisplayValidationError() {
        InputValidator.displayValidationError("Test error message");

        String output = outputStream.toString();
        assertTrue(output.contains("Test error message"));
    }

    @Test
    void testParseTravelDate_leapYear() {
        Optional<LocalDate> date = InputValidator.parseTravelDate("2024-02-29");

        assertTrue(date.isPresent());
        assertEquals(LocalDate.of(2024, 2, 29), date.get());
    }

    @Test
    void testParseTravelDate_invalidMonth() {
        Optional<LocalDate> date = InputValidator.parseTravelDate("2025-13-01");

        assertTrue(date.isEmpty()); // Month 13 doesn't exist
    }
}
