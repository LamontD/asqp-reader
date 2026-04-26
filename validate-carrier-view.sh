#!/bin/bash
# Test Carrier View
echo "=== Test 1: Carrier View - DL ==="
echo -e "4\nDL\n5" | java -jar flight-schedule-reader/target/flight-schedule-reader.jar sample-data.scheduledflight.csv 2>/dev/null | grep -A 30 "CARRIER: DL"
