package com.lamontd.asqp;

import com.lamontd.asqp.mapper.AirportCodeMapper;
import com.lamontd.asqp.model.index.DistanceCalculator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for distance calculation using the Haversine formula.
 * Known distances from public sources used for verification.
 */
class DistanceCalculationTest {

    private DistanceCalculator calculator;

    @BeforeEach
    void setUp() {
        AirportCodeMapper mapper = AirportCodeMapper.getDefault();
        calculator = new DistanceCalculator(mapper);
    }

    @Test
    void testHaversineFormula_NewYorkToLosAngeles() {
        // JFK to LAX: approximately 2,475 miles
        double lat1 = 40.6413; // JFK
        double lon1 = -73.7781;
        double lat2 = 33.9416; // LAX
        double lon2 = -118.4085;

        double distance = calculator.haversineDistance(lat1, lon1, lat2, lon2);

        // Allow 1% margin of error
        assertEquals(2475.0, distance, 25.0);
    }

    @Test
    void testHaversineFormula_ShortDistance() {
        // JFK to LGA (both in NYC): approximately 10.6 miles
        double lat1 = 40.6413; // JFK
        double lon1 = -73.7781;
        double lat2 = 40.7769; // LGA
        double lon2 = -73.8740;

        double distance = calculator.haversineDistance(lat1, lon1, lat2, lon2);

        // Allow 1 mile margin for short distances
        assertEquals(10.6, distance, 1.0);
    }

    @Test
    void testHaversineFormula_AtlantaToChicago() {
        // ATL to ORD: approximately 606 miles
        double lat1 = 33.6407; // ATL
        double lon1 = -84.4277;
        double lat2 = 41.9742; // ORD
        double lon2 = -87.9073;

        double distance = calculator.haversineDistance(lat1, lon1, lat2, lon2);

        // Allow 10 mile margin
        assertEquals(606.0, distance, 10.0);
    }

    @Test
    void testHaversineFormula_SameLocation() {
        // Same airport should be 0 miles
        double lat = 33.6407;
        double lon = -84.4277;

        double distance = calculator.haversineDistance(lat, lon, lat, lon);

        assertEquals(0.0, distance, 0.1);
    }

    @Test
    void testHaversineFormula_CrossingPrimeMeridian() {
        // LHR (London) to JFK (New York): approximately 3,459 miles
        double lat1 = 51.4700; // LHR
        double lon1 = -0.4543;
        double lat2 = 40.6413; // JFK
        double lon2 = -73.7781;

        double distance = calculator.haversineDistance(lat1, lon1, lat2, lon2);

        // Allow 50 mile margin for long distances
        assertEquals(3459.0, distance, 50.0);
    }
}
