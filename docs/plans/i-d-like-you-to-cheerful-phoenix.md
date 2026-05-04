# Round-Trip Flight Support for Travel Planner

## Context

The current travel planner ([TravelPlannerController.java](flight-schedule-reader/src/main/java/com/lamontd/travel/flight/schedule/controller/TravelPlannerController.java)) only supports one-way flight searches. Users can search for flights from origin to destination on a specific date with time-of-day filtering (Morning/Afternoon/Evening/Anytime), and the system finds both direct flights and single-connection flights using the RouteGraphService.

This enhancement adds round-trip support, allowing users to:
- Specify a return date
- Specify return time preferences independently
- View paired round-trip combinations (outbound + return as complete packages)
- Get both direct and connecting flight options for both legs

## User Requirements (Confirmed)

1. **Display format**: Show paired round-trip combinations (outbound + return as one option)
2. **Return timing**: Independent time preference (user selects Morning/Afternoon/Evening/Anytime for return)
3. **Connections**: Support both direct and connecting flights for both outbound and return legs

## Implementation Plan

### 1. Add Trip Type Selection to User Flow

**File**: [TravelPlannerController.java](flight-schedule-reader/src/main/java/com/lamontd/travel/flight/schedule/controller/TravelPlannerController.java)

**Changes to `display()` method** (lines 51-100):
- After displaying the header, prompt user: "Trip type (One-way/Round-trip or press Enter for one-way):"
- Parse input to determine trip type (enum: ONE_WAY, ROUND_TRIP)
- If one-way: use existing flow (no changes to current behavior)
- If round-trip: collect additional inputs:
  - Return date (yyyy-MM-dd format, must be >= travel date)
  - Return time preference (Morning/Afternoon/Evening/Anytime)
- Branch to `findRoundTripFlights()` or keep existing `findFlights()` call

### 2. Create Round-Trip Flight Search Logic

**File**: [TravelPlannerController.java](flight-schedule-reader/src/main/java/com/lamontd/travel/flight/schedule/controller/TravelPlannerController.java)

**New method**: `findRoundTripFlights()`
- Find outbound flights using existing logic (direct + connecting)
- Find return flights (destination → origin) on return date with return time preference
- Create `RoundTripOption` objects pairing each outbound with each return
- Limit combinations intelligently:
  - Take top 10 outbound options (sorted by departure time)
  - Take top 10 return options (sorted by departure time)
  - Create all combinations (up to 100 possible)
  - Sort by total travel time and price proxy (prefer direct over connecting)
  - Show top 10 round-trip combinations

**Reuse existing methods**:
- `findConnectingFlights()` for both outbound and return legs
- TimeOfDay enum for filtering both legs
- Existing validation and route graph logic

### 3. Create RoundTripOption Data Structure

**File**: [TravelPlannerController.java](flight-schedule-reader/src/main/java/com/lamontd/travel/flight/schedule/controller/TravelPlannerController.java)

**New inner class**: `RoundTripOption`
```java
private static class RoundTripOption {
    final FlightOption outbound;
    final FlightOption return;
    
    // Methods:
    // - getTotalTravelTime() - sum of both legs
    // - getComplexityScore() - 0 for direct/direct, 1 for one connection, 2 for two connections
    // - getOutboundDepartureTime()
    // - getReturnDepartureTime()
}
```

This reuses the existing `FlightOption` class (lines 291-317) which already handles direct vs connecting flights.

### 4. Display Round-Trip Results

**File**: [TravelPlannerController.java](flight-schedule-reader/src/main/java/com/lamontd/travel/flight/schedule/controller/TravelPlannerController.java)

**New method**: `displayRoundTripOptions()`
- Format: Show paired options with clear visual separation
- For each round-trip option (up to 10):
  ```
  1. ROUND-TRIP OPTION - Total Time: 6h 30m
     
     OUTBOUND: [DATE] - JFK (New York) → LAX (Los Angeles)
     DIRECT - AA 100 (American Airlines)
        Departs: 09:00 | Arrives: 12:30 | Duration: 6h 30m
     
     RETURN: [DATE] - LAX (Los Angeles) → JFK (New York)
     1 STOP via ORD (Chicago) - Total time: 7h 15m
        Leg 1: UA 456 (United Airlines) | 14:00 - 20:05
        Layover: 1h 45m
        Leg 2: UA 789 (United Airlines) | 21:50 - 00:30
  ```
- Reuse existing display methods:
  - `displayDirectOption()` (lines 233-245)
  - `displayConnectionOption()` (lines 247-274)
- Add summary footer showing breakdown (e.g., "3 direct/direct, 5 direct/connection, 2 connection/connection")

### 5. Input Validation

**File**: [InputValidator.java](flight-schedule-reader/src/main/java/com/lamontd/travel/flight/schedule/util/InputValidator.java)

**Reuse existing**: `validateTravelDate()` for return date validation
**Add new method**: `validateReturnDate(LocalDate departureDate, String returnDateInput)`
- Parse return date using existing logic
- Validate return date >= departure date
- Return Optional<LocalDate>

### 6. Update Tests

**File**: [TravelPlannerControllerTest.java](flight-schedule-reader/src/test/java/com/lamontd/travel/flight/schedule/controller/TravelPlannerControllerTest.java)

**Add new test cases**:
- `testValidRoundTripSearch()` - basic round-trip with direct flights
- `testRoundTripWithConnections()` - round-trip with connecting flights
- `testRoundTripReturnBeforeDeparture()` - invalid return date
- `testRoundTripSameDay()` - valid same-day return
- `testRoundTripMorningOutboundEveningReturn()` - time filtering on both legs
- `testRoundTripNoReturnFlights()` - handle case where return flights don't exist
- `testOneWayStillWorks()` - ensure existing one-way flow unaffected

**Test data setup**:
- Reuse existing `ControllerTestUtils.createTestIndex()` which already has JFK-LAX flights
- May need to add return direction flights (LAX-JFK) if not present

## Critical Files to Modify

1. **TravelPlannerController.java** (main changes)
   - Add trip type enum and selection logic
   - Add `findRoundTripFlights()` method
   - Add `RoundTripOption` inner class
   - Add `displayRoundTripOptions()` method
   - Modify `display()` to branch based on trip type

2. **InputValidator.java** (validation)
   - Add `validateReturnDate()` method

3. **TravelPlannerControllerTest.java** (tests)
   - Add 7 new test cases for round-trip scenarios

## Verification Plan

### Manual Testing
1. Build the project: `mvn clean install`
2. Run the flight-schedule-reader application
3. Select option 5 (Travel Planner)
4. Test scenarios:
   - **One-way search**: Ensure existing behavior unchanged (JFK → LAX, 2025-06-10, Morning)
   - **Round-trip same day**: JFK → LAX, depart 2025-06-10 morning, return 2025-06-10 evening
   - **Round-trip multi-day**: JFK → LAX, depart 2025-06-10, return 2025-06-15
   - **Invalid return date**: Verify error when return date < departure date
   - **No return flights**: Test route with no return options available

### Automated Testing
```bash
# Run all travel planner tests
mvn test -Dtest=TravelPlannerControllerTest

# Verify all 89 existing tests still pass
mvn test
```

### Expected Output Review
- Round-trip options should be clearly formatted and easy to read
- Both direct and connecting flights should appear for both legs
- Time preferences should filter correctly for both outbound and return
- Summary should accurately count option types
- No more than 10 round-trip combinations displayed
