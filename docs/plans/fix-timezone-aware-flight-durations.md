# Fix Timezone-Aware Flight Duration Calculations

## Context

The travel planner currently calculates flight durations and layover times using `LocalTime` without considering timezone differences between airports. This causes incorrect duration displays.

**Example Problem:**
- Flight: JFK (EST, UTC-5) → LAX (PST, UTC-8)  
- Departure: 10:00 AM EST (15:00 UTC)
- Arrival: 1:00 PM PST (21:00 UTC)
- **Current Calculation**: 3 hours (1:00 PM - 10:00 AM local times)
- **Actual Duration**: 6 hours (21:00 UTC - 15:00 UTC)

The `AirportInfo` model already contains timezone offset data (`timezone` field stores hours offset from UTC), so we can use this to fix the calculations.

## Affected Areas

1. **FlightTimeUtils** - Duration and layover calculations
2. **TravelPlannerController** - Uses FlightTimeUtils for display
3. Both direct flights and connecting flights are affected
4. Round-trip calculations use the same utilities

## Implementation Plan

### 1. Enhance FlightTimeUtils with Timezone-Aware Methods

**File**: [FlightTimeUtils.java](flight-schedule-reader/src/main/java/com/lamontd/travel/flight/schedule/util/FlightTimeUtils.java)

**Add new overloaded methods** that accept airport timezone information:

```java
/**
 * Calculates flight duration considering timezone differences.
 * 
 * @param departure Local departure time
 * @param departureTimezoneOffset Origin airport timezone offset from UTC (hours)
 * @param arrival Local arrival time  
 * @param arrivalTimezoneOffset Destination airport timezone offset from UTC (hours)
 * @param operatingDate Date of flight operation
 * @return Duration object representing actual flight time
 */
public static Duration calculateFlightDuration(
    LocalTime departure, 
    double departureTimezoneOffset,
    LocalTime arrival,
    double arrivalTimezoneOffset,
    LocalDate operatingDate)
```

**Algorithm:**
1. Convert departure local time to UTC: `departureUTC = departure + departureTimezoneOffset`
2. Convert arrival local time to UTC: `arrivalUTC = arrival + arrivalTimezoneOffset`
3. Handle day boundaries (if arrivalUTC < departureUTC, arrival is next day)
4. Calculate duration: `Duration.between(departureUTC, arrivalUTC)`

**Also add:**
```java
public static long calculateLayoverMinutes(
    LocalTime arrivalTime,
    double arrivalTimezoneOffset,
    LocalTime departureTime,
    double departureTimezoneOffset)
```

For layovers at the same airport (same timezone), this simplifies to the existing logic, but it's important for connecting flights through different timezones.

### 2. Update TravelPlannerController to Use Timezone-Aware Calculations

**File**: [TravelPlannerController.java](flight-schedule-reader/src/main/java/com/lamontd/travel/flight/schedule/controller/TravelPlannerController.java)

**Changes needed:**

1. **Add AirportCodeMapper instance** (already exists as `AIRPORT_MAPPER`)

2. **Update `calculateDuration()` method** (line 527-529):
```java
private String calculateDuration(LocalTime departure, LocalTime arrival, 
                                 String originCode, String destCode, LocalDate date) {
    double originTz = AIRPORT_MAPPER.getAirportInfo(originCode)
        .flatMap(AirportInfo::getTimezone).orElse(0.0);
    double destTz = AIRPORT_MAPPER.getAirportInfo(destCode)
        .flatMap(AirportInfo::getTimezone).orElse(0.0);
    
    return FlightTimeUtils.calculateDuration(departure, originTz, arrival, destTz, date);
}
```

3. **Update `calculateLayoverMinutes()` method** (line 523-525):
```java
private long calculateLayoverMinutes(LocalTime arrival, LocalTime departure, String airportCode) {
    double airportTz = AIRPORT_MAPPER.getAirportInfo(airportCode)
        .flatMap(AirportInfo::getTimezone).orElse(0.0);
    
    return FlightTimeUtils.calculateLayoverMinutes(arrival, airportTz, departure, airportTz);
}
```

4. **Update all call sites**:
   - Line 440: `displayDirectOption()` - pass origin, destination, operating date
   - Line 459: `displayConnectionOption()` - pass airport codes for both legs
   - Line 483: `displayDirectOptionIndented()` - pass airport codes
   - Line 502: `displayConnectionOptionIndented()` - pass airport codes
   - Line 350 (approx): `findConnectingFlights()` - update layover calculation

### 3. Handle Edge Cases

**Missing Timezone Data:**
- If airport timezone is unavailable, fall back to 0.0 (UTC)
- Log a warning when timezone data is missing

**International Date Line:**
- The UTC conversion handles this naturally
- Flights crossing the date line will have correct durations

**Daylight Saving Time:**
- The `timezone` field in AirportInfo is a static offset
- For true DST handling, would need to use `tzDatabase` field with `ZoneId`
- **For this fix**: Use static offset (good enough for most cases)
- **Future enhancement**: Could use `tzDatabase` for DST-aware calculations

### 4. Update Tests

**File**: [FlightTimeUtilsTest.java](flight-schedule-reader/src/test/java/com/lamontd/travel/flight/schedule/util/FlightTimeUtilsTest.java)

**Add new test cases:**
- `testCalculateDurationWithTimezones()` - Same timezone (should match old behavior)
- `testCalculateDurationEastToWest()` - JFK (-5) to LAX (-8), expect 3-hour time shift
- `testCalculateDurationWestToEast()` - LAX (-8) to JFK (-5), expect 3-hour time shift
- `testCalculateDurationInternational()` - JFK (-5) to LHR (0), expect 5-hour shift
- `testCalculateDurationRedEyeWithTimezones()` - Overnight flight with timezone crossing
- `testCalculateLayoverWithSameTimezone()` - Standard layover case
- `testCalculateLayoverDifferentTimezone()` - Edge case (shouldn't happen but test it)

**File**: [TravelPlannerControllerTest.java](flight-schedule-reader/src/test/java/com/lamontd/travel/flight/schedule/controller/TravelPlannerControllerTest.java)

**Existing tests should still pass** because test data uses same/nearby timezones
- May need to update assertions if test data crosses significant timezone boundaries

### 5. Backwards Compatibility

**Keep existing methods** in FlightTimeUtils:
- `calculateFlightDuration(LocalTime, LocalTime)` - for code that doesn't have timezone info
- `calculateDuration(LocalTime, LocalTime)` - convenience method
- `calculateLayoverMinutes(LocalTime, LocalTime)` - for same-timezone layovers

This ensures other parts of the codebase (asqp-reader module) continue to work.

## Files to Modify

1. **FlightTimeUtils.java** - Add timezone-aware calculation methods
2. **TravelPlannerController.java** - Use timezone-aware methods with airport lookups
3. **FlightTimeUtilsTest.java** - Add timezone-aware test cases
4. **TravelPlannerControllerTest.java** - Verify existing tests still pass

## Verification Plan

### Unit Tests
```bash
# Run FlightTimeUtils tests
mvn test -Dtest=FlightTimeUtilsTest

# Run TravelPlannerController tests
mvn test -Dtest=TravelPlannerControllerTest

# Run all tests
mvn test
```

### Manual Testing

Test with flights crossing major timezone boundaries:

**Test Case 1: East to West (JFK → LAX)**
- Expected: ~6 hour flight duration despite 3-hour local time difference
- Verify: Duration shows "6h 00m" or similar

**Test Case 2: West to East (LAX → JFK)**  
- Expected: ~5 hour flight duration with 8-hour local time difference
- Verify: Duration shows "5h 00m" or similar

**Test Case 3: Connecting Flight with Layover**
- JFK → ORD (same timezone, EST → CST = 1 hour)
- Layover in ORD
- ORD → LAX (CST → PST = 2 hours)
- Verify: Both leg durations and layover time are correct

**Test Case 4: Round-Trip**
- Verify outbound and return durations are both correct with timezone handling

### Expected Output Changes

Before fix:
```
DIRECT - AA 100 (American Airlines)
   Departs: 10:00 | Arrives: 13:00 | Duration: 3h 00m  ❌ WRONG
```

After fix:
```
DIRECT - AA 100 (American Airlines)
   Departs: 10:00 | Arrives: 13:00 | Duration: 6h 00m  ✅ CORRECT
```

## Implementation Notes

- Use `Optional.orElse(0.0)` for missing timezone data (default to UTC)
- The operating date is needed for day boundary calculations
- Consider logging warnings when timezone data is missing
- Document the DST limitation in JavaDoc comments
