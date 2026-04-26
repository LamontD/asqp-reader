#!/bin/bash
# Test Airport View
echo "=== Test 1: Airport View - CVG ==="
echo -e "3\nCVG\n5" | java -jar flight-schedule-reader/target/flight-schedule-reader.jar sample-data.scheduledflight.csv 2>/dev/null | grep -A 15 "AIRPORT: CVG"

echo ""
echo "=== Test 2: Airport View - LGA ==="
echo -e "3\nLGA\n5" | java -jar flight-schedule-reader/target/flight-schedule-reader.jar sample-data.scheduledflight.csv 2>/dev/null | grep -A 15 "AIRPORT: LGA"
