package com.lamontd.travel.flight.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FlightRecordDelayTest {

    @Test
    void testDelayFields() {
        ASQPFlightRecord record = ASQPFlightRecord.builder()
                .carrierCode("DL")
                .flightNumber("100")
                .carrierDelay(15)
                .weatherDelay(30)
                .nasDelay(10)
                .securityDelay(5)
                .lateArrivalDelay(20)
                .build();

        assertTrue(record.getCarrierDelay().isPresent());
        assertEquals(15, record.getCarrierDelay().get());

        assertTrue(record.getWeatherDelay().isPresent());
        assertEquals(30, record.getWeatherDelay().get());

        assertTrue(record.getNasDelay().isPresent());
        assertEquals(10, record.getNasDelay().get());

        assertTrue(record.getSecurityDelay().isPresent());
        assertEquals(5, record.getSecurityDelay().get());

        assertTrue(record.getLateArrivalDelay().isPresent());
        assertEquals(20, record.getLateArrivalDelay().get());
    }

    @Test
    void testTotalDelay() {
        ASQPFlightRecord record = ASQPFlightRecord.builder()
                .carrierCode("DL")
                .flightNumber("100")
                .carrierDelay(15)
                .weatherDelay(30)
                .nasDelay(10)
                .securityDelay(5)
                .lateArrivalDelay(20)
                .build();

        assertEquals(80, record.getTotalDelay());
        assertTrue(record.hasDelay());
    }

    @Test
    void testNoDelay() {
        ASQPFlightRecord record = ASQPFlightRecord.builder()
                .carrierCode("DL")
                .flightNumber("100")
                .build();

        assertFalse(record.getCarrierDelay().isPresent());
        assertFalse(record.getWeatherDelay().isPresent());
        assertFalse(record.getNasDelay().isPresent());
        assertFalse(record.getSecurityDelay().isPresent());
        assertFalse(record.getLateArrivalDelay().isPresent());

        assertEquals(0, record.getTotalDelay());
        assertFalse(record.hasDelay());
    }

    @Test
    void testPartialDelay() {
        ASQPFlightRecord record = ASQPFlightRecord.builder()
                .carrierCode("DL")
                .flightNumber("100")
                .weatherDelay(45)
                .lateArrivalDelay(15)
                .build();

        assertFalse(record.getCarrierDelay().isPresent());
        assertTrue(record.getWeatherDelay().isPresent());
        assertEquals(45, record.getWeatherDelay().get());
        assertFalse(record.getNasDelay().isPresent());
        assertFalse(record.getSecurityDelay().isPresent());
        assertTrue(record.getLateArrivalDelay().isPresent());
        assertEquals(15, record.getLateArrivalDelay().get());

        assertEquals(60, record.getTotalDelay());
        assertTrue(record.hasDelay());
    }
}
