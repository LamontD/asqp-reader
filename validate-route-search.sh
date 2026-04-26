#!/bin/bash
# Test 1: Route Search with date
echo "=== Test 1: Route Search CVG to LGA on 2025-01-27 ==="
echo -e "1\nCVG\nLGA\n2025-01-27\nn\n5" | java -jar flight-schedule-reader/target/flight-schedule-reader.jar sample-data.scheduledflight.csv 2>/dev/null | grep -A 10 "ROUTE: CVG"

echo ""
echo "=== Test 2: Route Search LGA to CVG on 2025-01-02 ==="
echo -e "1\nLGA\nCVG\n2025-01-02\nn\n5" | java -jar flight-schedule-reader/target/flight-schedule-reader.jar sample-data.scheduledflight.csv 2>/dev/null | grep -A 10 "ROUTE: LGA"

echo ""
echo "=== Test 3: Route Search all dates (CVG to LGA) ==="
echo -e "1\nCVG\nLGA\n\nn\n5" | java -jar flight-schedule-reader/target/flight-schedule-reader.jar sample-data.scheduledflight.csv 2>/dev/null | grep -A 15 "Total flights found"
