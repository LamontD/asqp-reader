#!/bin/bash

echo "Testing Flight Schedule Reader Menu Options"
echo "==========================================="
echo ""

JAR="flight-schedule-reader/target/flight-schedule-reader.jar"
DATA="sample-data.scheduledflight.csv"

# Test 1: Route Search
echo "Test 1: Route Search (CVG to LGA)"
echo -e "1\nCVG\nLGA\n2025-01-27\nn\n5" | java -jar $JAR $DATA 2>&1 | grep -A 20 "Route Search"

echo ""
echo "Test 2: Flight Search"
echo -e "2\nDL\n5030\n5" | java -jar $JAR $DATA 2>&1 | grep -A 10 "Flight Search"

echo ""
echo "Test 3: Airport View"
echo -e "3\nCVG\n5" | java -jar $JAR $DATA 2>&1 | grep -A 10 "Airport View"

echo ""
echo "Test 4: Carrier View"
echo -e "4\nDL\n5" | java -jar $JAR $DATA 2>&1 | grep -A 10 "Carrier View"

echo ""
echo "All tests completed!"
