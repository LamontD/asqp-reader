package com.lamontd.travel.flight.model;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class FlightSegmentTest {

    @Test
    void testDirectLegCreation() {
        FlightSegment segment = FlightSegment.builder()
                .carrierCode("WN")
                .flightNumber("4283")
                .originAirport("ORD")
                .destinationAirport("DAL")
                .segmentType(FlightSegment.SegmentType.DIRECT_LEG)
                .intermediateStops(Collections.emptyList())
                .legCount(1)
                .build();

        assertEquals("WN", segment.getCarrierCode());
        assertEquals("4283", segment.getFlightNumber());
        assertEquals("ORD", segment.getOriginAirport());
        assertEquals("DAL", segment.getDestinationAirport());
        assertEquals(FlightSegment.SegmentType.DIRECT_LEG, segment.getSegmentType());
        assertTrue(segment.getIntermediateStops().isEmpty());
        assertEquals(1, segment.getLegCount());
        assertTrue(segment.isDirect());
        assertFalse(segment.isThrough());
    }

    @Test
    void testThroughConnectionCreation() {
        FlightSegment segment = FlightSegment.builder()
                .carrierCode("WN")
                .flightNumber("4283")
                .originAirport("ORD")
                .destinationAirport("MCO")
                .segmentType(FlightSegment.SegmentType.THROUGH_CONNECTION)
                .intermediateStops(Collections.singletonList("DAL"))
                .legCount(2)
                .build();

        assertEquals("WN", segment.getCarrierCode());
        assertEquals("4283", segment.getFlightNumber());
        assertEquals("ORD", segment.getOriginAirport());
        assertEquals("MCO", segment.getDestinationAirport());
        assertEquals(FlightSegment.SegmentType.THROUGH_CONNECTION, segment.getSegmentType());
        assertEquals(1, segment.getIntermediateStops().size());
        assertEquals("DAL", segment.getIntermediateStops().get(0));
        assertEquals(2, segment.getLegCount());
        assertFalse(segment.isDirect());
        assertTrue(segment.isThrough());
    }

    @Test
    void testThreeLegThroughConnection() {
        FlightSegment segment = FlightSegment.builder()
                .carrierCode("WN")
                .flightNumber("5114")
                .originAirport("A")
                .destinationAirport("D")
                .segmentType(FlightSegment.SegmentType.THROUGH_CONNECTION)
                .intermediateStops(Arrays.asList("B", "C"))
                .legCount(3)
                .build();

        assertEquals(2, segment.getIntermediateStops().size());
        assertEquals("B", segment.getIntermediateStops().get(0));
        assertEquals("C", segment.getIntermediateStops().get(1));
        assertEquals(3, segment.getLegCount());
    }

    @Test
    void testSegmentKey() {
        FlightSegment segment = FlightSegment.builder()
                .carrierCode("WN")
                .flightNumber("4283")
                .originAirport("ORD")
                .destinationAirport("DAL")
                .segmentType(FlightSegment.SegmentType.DIRECT_LEG)
                .legCount(1)
                .build();

        assertEquals("ORD-DAL", segment.getSegmentKey());
    }

    @Test
    void testFlightKey() {
        FlightSegment segment = FlightSegment.builder()
                .carrierCode("WN")
                .flightNumber("4283")
                .originAirport("ORD")
                .destinationAirport("DAL")
                .segmentType(FlightSegment.SegmentType.DIRECT_LEG)
                .legCount(1)
                .build();

        assertEquals("WN4283", segment.getFlightKey());
    }

    @Test
    void testIntermediateStopsImmutable() {
        FlightSegment segment = FlightSegment.builder()
                .carrierCode("WN")
                .flightNumber("4283")
                .originAirport("ORD")
                .destinationAirport("MCO")
                .segmentType(FlightSegment.SegmentType.THROUGH_CONNECTION)
                .intermediateStops(Arrays.asList("DAL"))
                .legCount(2)
                .build();

        assertThrows(UnsupportedOperationException.class, () -> {
            segment.getIntermediateStops().add("DEN");
        });
    }

    @Test
    void testRequiredFields() {
        assertThrows(NullPointerException.class, () -> {
            FlightSegment.builder()
                    .flightNumber("4283")
                    .originAirport("ORD")
                    .destinationAirport("DAL")
                    .segmentType(FlightSegment.SegmentType.DIRECT_LEG)
                    .legCount(1)
                    .build();
        });

        assertThrows(NullPointerException.class, () -> {
            FlightSegment.builder()
                    .carrierCode("WN")
                    .originAirport("ORD")
                    .destinationAirport("DAL")
                    .segmentType(FlightSegment.SegmentType.DIRECT_LEG)
                    .legCount(1)
                    .build();
        });

        assertThrows(NullPointerException.class, () -> {
            FlightSegment.builder()
                    .carrierCode("WN")
                    .flightNumber("4283")
                    .destinationAirport("DAL")
                    .segmentType(FlightSegment.SegmentType.DIRECT_LEG)
                    .legCount(1)
                    .build();
        });
    }

    @Test
    void testInvalidLegCount() {
        assertThrows(IllegalArgumentException.class, () -> {
            FlightSegment.builder()
                    .carrierCode("WN")
                    .flightNumber("4283")
                    .originAirport("ORD")
                    .destinationAirport("DAL")
                    .segmentType(FlightSegment.SegmentType.DIRECT_LEG)
                    .legCount(0)
                    .build();
        });

        assertThrows(IllegalArgumentException.class, () -> {
            FlightSegment.builder()
                    .carrierCode("WN")
                    .flightNumber("4283")
                    .originAirport("ORD")
                    .destinationAirport("DAL")
                    .segmentType(FlightSegment.SegmentType.DIRECT_LEG)
                    .legCount(-1)
                    .build();
        });
    }

    @Test
    void testDirectLegMustHaveOneLeg() {
        assertThrows(IllegalArgumentException.class, () -> {
            FlightSegment.builder()
                    .carrierCode("WN")
                    .flightNumber("4283")
                    .originAirport("ORD")
                    .destinationAirport("DAL")
                    .segmentType(FlightSegment.SegmentType.DIRECT_LEG)
                    .legCount(2)
                    .build();
        });
    }

    @Test
    void testThroughConnectionMustHaveMultipleLegs() {
        assertThrows(IllegalArgumentException.class, () -> {
            FlightSegment.builder()
                    .carrierCode("WN")
                    .flightNumber("4283")
                    .originAirport("ORD")
                    .destinationAirport("MCO")
                    .segmentType(FlightSegment.SegmentType.THROUGH_CONNECTION)
                    .intermediateStops(Collections.singletonList("DAL"))
                    .legCount(1)
                    .build();
        });
    }

    @Test
    void testEquality() {
        FlightSegment segment1 = FlightSegment.builder()
                .carrierCode("WN")
                .flightNumber("4283")
                .originAirport("ORD")
                .destinationAirport("DAL")
                .segmentType(FlightSegment.SegmentType.DIRECT_LEG)
                .legCount(1)
                .build();

        FlightSegment segment2 = FlightSegment.builder()
                .carrierCode("WN")
                .flightNumber("4283")
                .originAirport("ORD")
                .destinationAirport("DAL")
                .segmentType(FlightSegment.SegmentType.DIRECT_LEG)
                .legCount(1)
                .build();

        assertEquals(segment1, segment2);
        assertEquals(segment1.hashCode(), segment2.hashCode());
    }

    @Test
    void testToString() {
        FlightSegment directLeg = FlightSegment.builder()
                .carrierCode("WN")
                .flightNumber("4283")
                .originAirport("ORD")
                .destinationAirport("DAL")
                .segmentType(FlightSegment.SegmentType.DIRECT_LEG)
                .legCount(1)
                .build();

        String str = directLeg.toString();
        assertTrue(str.contains("WN4283"));
        assertTrue(str.contains("ORD→DAL"));
        assertTrue(str.contains("DIRECT_LEG"));
        assertTrue(str.contains("1 leg"));

        FlightSegment throughConnection = FlightSegment.builder()
                .carrierCode("WN")
                .flightNumber("4283")
                .originAirport("ORD")
                .destinationAirport("MCO")
                .segmentType(FlightSegment.SegmentType.THROUGH_CONNECTION)
                .intermediateStops(Collections.singletonList("DAL"))
                .legCount(2)
                .build();

        String throughStr = throughConnection.toString();
        assertTrue(throughStr.contains("WN4283"));
        assertTrue(throughStr.contains("ORD→MCO"));
        assertTrue(throughStr.contains("via DAL"));
        assertTrue(throughStr.contains("THROUGH_CONNECTION"));
        assertTrue(throughStr.contains("2 legs"));
    }
}
