#!/bin/bash
# Test script for multi-leg flight segment analysis

echo "=================================================="
echo "Testing Multi-Leg Flight Segment Analysis"
echo "=================================================="
echo ""

JAR_FILE="asqp-reader/target/asqp-reader.jar"
DATA_FILE="$HOME/asqp-data/processed_data/ontime.td.202501.asqpflightrecord.csv"

if [ ! -f "$JAR_FILE" ]; then
    echo "Error: JAR file not found at $JAR_FILE"
    exit 1
fi

if [ ! -f "$DATA_FILE" ]; then
    echo "Error: Data file not found at $DATA_FILE"
    exit 1
fi

echo "Test 1: WN 4283 (2-leg flight: ORD→DAL→MCO)"
echo "Expected: 3 segments (ORD-DAL, DAL-MCO, ORD-MCO via DAL)"
echo ""
echo "8" | java -jar "$JAR_FILE" "$DATA_FILE" 2>&1 | grep -A 100 "Enter flight number" | {
    read line
    echo "WN 4283"
} | java -jar "$JAR_FILE" "$DATA_FILE" 2>&1 | grep -A 50 "BOOKABLE SEGMENTS"

echo ""
echo "=================================================="
echo "Test Complete"
echo "=================================================="
