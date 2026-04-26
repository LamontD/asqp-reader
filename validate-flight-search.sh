#!/bin/bash
# Test Flight Search
echo "=== Test 1: Flight Search - DL 5030 ==="
echo -e "2\nDL\n5030\n5" | java -jar flight-schedule-reader/target/flight-schedule-reader.jar sample-data.scheduledflight.csv 2>/dev/null | grep -A 20 "FLIGHT: DL"
