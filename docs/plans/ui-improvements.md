# Plan: UI Improvements for Flight Schedule Reader

## Context

User feedback identified several UI improvements to make flight information more readable and useful:

1. **Route Search**: Results are currently unsorted, making it hard to find flights chronologically
2. **Travel Planner (Direct Flight Finder)**: Airport codes displayed without city names
3. **Flight Search**: Results are unsorted, making it hard to track when flights operate
4. **Airport View**: Airport code displayed without city name
5. **Carrier View**: Missing carrier name and statistics (airports served, routes, top routes)

Most changes are simple display improvements. Carrier View requires enhanced analytics.

## Current Issues

### Issue 1: Route Search - No Sorting

**File:** [RouteSearchController.java](../flight-schedule-reader/src/main/java/com/lamontd/travel/flight/schedule/controller/RouteSearchController.java)

**Current behavior:**
- Line 34: `flights = index.getFlightsByRoute(origin, destination);`
- Line 37: `flights = index.getFlightsByRouteOnDate(origin, destination, travelDate);`
- Results displayed in arbitrary order (index insertion order)

**Problem:** When showing flights across multiple dates, or even on a single date, users can't easily see chronological progression.

### Issue 2: Travel Planner - No City Names

**File:** [DirectFlightFinderController.java](../flight-schedule-reader/src/main/java/com/lamontd/travel/flight/schedule/controller/DirectFlightFinderController.java)

**Current behavior:**
- Line 72: `System.out.println("DIRECT FLIGHTS: " + origin + " → " + destination + " (" + travelDate + ")");`
- Shows only airport codes: "DIRECT FLIGHTS: BWI → DAL (2025-06-10)"

**Problem:** Users may not know what "BWI" and "DAL" represent without looking them up.

**Desired:** "DIRECT FLIGHTS: BWI (Baltimore) → DAL (Dallas) (2025-06-10)"

### Issue 3: Flight Search - No Sorting

**File:** [FlightSearchController.java](../flight-schedule-reader/src/main/java/com/lamontd/travel/flight/schedule/controller/FlightSearchController.java)

**Current behavior:**
- Line 25: `flights = index.getFlightsByCarrierAndFlightNumber(carrier, flightNumber);`
- Results displayed in arbitrary order

**Problem:** When a flight operates on multiple dates, users can't easily see the schedule progression.

### Issue 4: Airport View - No City Name

**File:** [AirportViewController.java](../flight-schedule-reader/src/main/java/com/lamontd/travel/flight/schedule/controller/AirportViewController.java)

**Current behavior:**
- Line 30: `System.out.println("AIRPORT: " + airport);`
- Shows only airport code: "AIRPORT: BWI"

**Problem:** Users may not recognize what airport "BWI" represents.

**Desired:** "AIRPORT: BWI (Baltimore)"

### Issue 5: Carrier View - Limited Information

**File:** [CarrierViewController.java](../flight-schedule-reader/src/main/java/com/lamontd/travel/flight/schedule/controller/CarrierViewController.java)

**Current behavior:**
- Line 31: `System.out.println("CARRIER: " + carrier);`
- Shows only carrier code: "CARRIER: AA"
- Lines 59-60: Shows total flights and total routes at bottom
- No statistics about airport coverage or route frequency

**Problems:**
1. Users don't see carrier name (e.g., "American Airlines")
2. No visibility into network size (how many airports served)
3. No insight into which routes are most frequent
4. Statistics buried at bottom after long flight list

**Desired Output:**
```
CARRIER: AA (American Airlines)
================================================================================

NETWORK STATISTICS:
  Total Flights: 150
  Total Routes: 25
  Airports Served: 18
  
TOP 10 ROUTES (by flight count):
  1. ORD-DFW: 12 flights
  2. DFW-LAX: 10 flights
  3. JFK-LAX: 8 flights
  ...

FLIGHT DETAILS:
[detailed flight listing follows]
```

## Solution Design

### Fix 1: Sort Route Search Results

**Sorting order:** Primary by operating date, secondary by departure time

**Implementation:**
```java
// After line 43 in RouteSearchController
flights.sort(Comparator
    .comparing(BookableFlight::getOperatingDate)
    .thenComparing(BookableFlight::getScheduledDepartureTime));
```

**Why this order:**
- Date first: Shows flights chronologically across days
- Time second: Within a day, shows flights in departure order
- Matches natural user expectation for "when can I fly"

### Fix 2: Add City Names to Travel Planner

**Existing utility:** AirportCodeMapper.getAirportCity(String code) exists and is already used in NetworkAnalysisController

**Implementation:**
```java
// Add at top of DirectFlightFinderController
private static final AirportCodeMapper airportMapper = AirportCodeMapper.getDefault();

// In displayResults() method, replace line 72:
String originCity = airportMapper.getAirportCity(origin);
String destCity = airportMapper.getAirportCity(destination);
System.out.printf("DIRECT FLIGHTS: %s (%s) → %s (%s) (%s)%n", 
    origin, originCity, destination, destCity, travelDate);
```

**Pattern:** Matches NetworkAnalysisController which already does this (lines 153, 154, 209, 230, etc.)

### Fix 3: Sort Flight Search Results

**Sorting order:** Primary by operating date, secondary by departure time

**Implementation:**
```java
// After line 25 in FlightSearchController
flights.sort(Comparator
    .comparing(BookableFlight::getOperatingDate)
    .thenComparing(BookableFlight::getScheduledDepartureTime));
```

**Why:** Same rationale as Fix 1 - chronological order is most intuitive

### Fix 4: Add City Name to Airport View

**Existing utility:** AirportCodeMapper.getAirportCity(String code)

**Implementation:**
```java
// Add at top of AirportViewController
private static final AirportCodeMapper airportMapper = AirportCodeMapper.getDefault();

// In displayResults() method, replace line 30:
String city = airportMapper.getAirportCity(airport);
System.out.printf("AIRPORT: %s (%s)%n", airport, city);
```

**Pattern:** Consistent with DirectFlightFinderController city name display

### Fix 5: Enhance Carrier View with Name and Statistics

**Existing utilities:**
- CarrierCodeMapper.getCarrierName(String code) - returns carrier name
- Existing grouping logic (line 40-42) can be extended for statistics

**New Display Structure:**
1. Header with carrier code and name
2. Network statistics section
3. Top 10 routes by flight count
4. Detailed flight listing (existing)

**Implementation:**
```java
// Add at top of CarrierViewController
private static final CarrierCodeMapper carrierMapper = CarrierCodeMapper.getDefault();

// Modify displayResults() to:
// 1. Get carrier name
// 2. Calculate statistics (airports, routes, top routes)
// 3. Display statistics BEFORE flight details
// 4. Display detailed flights (existing logic)
```

**Statistics Calculation:**
- **Airports Served**: Collect unique origins + destinations from flights
- **Total Routes**: byRoute.size() (already calculated)
- **Top 10 Routes**: Sort byRoute entries by flight count, take top 10

**Pattern:** Similar to NetworkAnalysisController statistics display

## Implementation Details

### Files to Modify

1. **RouteSearchController.java** - Add sorting (1 line)
2. **DirectFlightFinderController.java** - Add city names (3 lines)
3. **FlightSearchController.java** - Add sorting (1 line)
4. **AirportViewController.java** - Add city name (3 lines)
5. **CarrierViewController.java** - Add name, statistics, top routes (~40 lines)

### Dependencies

- `Comparator` - Already imported in DirectFlightFinderController
- `AirportCodeMapper` - Already available from flight-core
- `CarrierCodeMapper` - Already available from flight-core
- `getAirportCity()` - Already used in NetworkAnalysisController
- `getCarrierName()` - Available in CarrierCodeMapper

No new dependencies needed.

### Code Changes

**Change 1: RouteSearchController.java**
```java
// After line 43 (after getting flights list)
flights.sort(Comparator
    .comparing(BookableFlight::getOperatingDate)
    .thenComparing(BookableFlight::getScheduledDepartureTime));
```

**Change 2: DirectFlightFinderController.java**
```java
// Add import at top (line 4)
import com.lamontd.travel.flight.mapper.AirportCodeMapper;

// Add static field after DATE_FORMATTER (line 21)
private static final AirportCodeMapper AIRPORT_MAPPER = AirportCodeMapper.getDefault();

// Modify displayResults() method (line 70-73)
private void displayResults(String origin, String destination, LocalDate travelDate, List<BookableFlight> flights) {
    String originCity = AIRPORT_MAPPER.getAirportCity(origin);
    String destCity = AIRPORT_MAPPER.getAirportCity(destination);
    
    System.out.println("\n" + "=".repeat(80));
    System.out.printf("DIRECT FLIGHTS: %s (%s) → %s (%s) (%s)%n", 
        origin, originCity, destination, destCity, travelDate);
    System.out.println("=".repeat(80));
```

**Change 3: FlightSearchController.java**
```java
// Add import at top
import java.util.Comparator;

// After line 25 (after getting flights list)
flights.sort(Comparator
    .comparing(BookableFlight::getOperatingDate)
    .thenComparing(BookableFlight::getScheduledDepartureTime));
```

**Change 4: AirportViewController.java**
```java
// Add import at top (line 4)
import com.lamontd.travel.flight.mapper.AirportCodeMapper;

// Add static field after class declaration (line 13)
private static final AirportCodeMapper AIRPORT_MAPPER = AirportCodeMapper.getDefault();

// Modify displayResults() method (line 28-31)
private void displayResults(String airport, List<BookableFlight> departures, List<BookableFlight> arrivals) {
    String city = AIRPORT_MAPPER.getAirportCity(airport);
    
    System.out.println("\n" + "=".repeat(80));
    System.out.printf("AIRPORT: %s (%s)%n", airport, city);
    System.out.println("=".repeat(80));
```

**Change 5: CarrierViewController.java** (Major Enhancement)
```java
// Add imports at top
import com.lamontd.travel.flight.mapper.CarrierCodeMapper;
import java.util.*;
import java.util.stream.Collectors;

// Add static field after class declaration (line 14)
private static final CarrierCodeMapper CARRIER_MAPPER = CarrierCodeMapper.getDefault();

// Completely rewrite displayResults() method:
private void displayResults(String carrier, List<BookableFlight> flights) {
    String carrierName = CARRIER_MAPPER.getCarrierName(carrier);
    
    System.out.println("\n" + "=".repeat(80));
    System.out.printf("CARRIER: %s (%s)%n", carrier, carrierName);
    System.out.println("=".repeat(80));

    if (flights.isEmpty()) {
        System.out.println("No flights found for this carrier.");
        return;
    }

    // Group by route for statistics
    Map<String, List<BookableFlight>> byRoute = flights.stream()
            .collect(Collectors.groupingBy(f ->
                    f.getOriginAirport() + "-" + f.getDestinationAirport()));

    // Calculate statistics
    Set<String> airports = new HashSet<>();
    for (BookableFlight flight : flights) {
        airports.add(flight.getOriginAirport());
        airports.add(flight.getDestinationAirport());
    }

    // Display network statistics
    System.out.println("\nNETWORK STATISTICS:");
    System.out.println("-".repeat(80));
    System.out.printf("  Total Flights: %d%n", flights.size());
    System.out.printf("  Total Routes: %d%n", byRoute.size());
    System.out.printf("  Airports Served: %d%n", airports.size());

    // Display top 10 routes by flight count
    System.out.println("\nTOP 10 ROUTES (by flight count):");
    System.out.println("-".repeat(80));
    byRoute.entrySet().stream()
            .sorted((e1, e2) -> Integer.compare(e2.getValue().size(), e1.getValue().size()))
            .limit(10)
            .forEach(entry -> {
                int rank = byRoute.entrySet().stream()
                        .filter(e -> e.getValue().size() > entry.getValue().size())
                        .count() + 1;
                System.out.printf("  %2d. %s: %d flight%s%n", 
                        (int)rank, entry.getKey(), entry.getValue().size(),
                        entry.getValue().size() == 1 ? "" : "s");
            });

    // Display detailed flight listing (existing logic, slightly modified)
    System.out.println("\nFLIGHT DETAILS:");
    System.out.println("=".repeat(80));
    System.out.printf("%-12s %-20s %-12s %-12s %-15s%n",
            "Flight", "Route", "Departure", "Arrival", "Operating Date");
    System.out.println("-".repeat(80));

    for (String route : byRoute.keySet().stream().sorted().toList()) {
        for (BookableFlight flight : byRoute.get(route)) {
            System.out.printf("%-12s %-20s %-12s %-12s %-15s%n",
                    flight.getFlightNumber(),
                    route,
                    flight.getScheduledDepartureTime(),
                    flight.getScheduledArrivalTime(),
                    flight.getOperatingDate().toString());
        }
    }
}
```

## Testing Strategy

### Manual Testing

**Test 1: Route Search Sorting**
```
Use: test-travel-planning.bookableflight.csv
Query: Route BWI → DAL (no date filter)
Expected: Flights shown in date order, then time order
Verify: AA 100 (0800) before WN 700 (0900) on same date
```

**Test 2: City Names in Travel Planner**
```
Use: test-travel-planning.bookableflight.csv
Query: BWI → DAL on 2025-06-10
Expected: Header shows "BWI (Baltimore) → DAL (Dallas)"
```

**Test 3: Flight Search Sorting**
```
Use: test-travel-planning.bookableflight.csv
Query: AA 101 (appears on multiple dates in test data)
Expected: Results sorted by date, then departure time
```

**Test 4: City Name in Airport View**
```
Use: test-travel-planning.bookableflight.csv
Query: Airport BWI
Expected: Header shows "AIRPORT: BWI (Baltimore)"
```

**Test 5: Enhanced Carrier View**
```
Use: test-travel-planning.bookableflight.csv
Query: Carrier AA
Expected: 
  - Header: "CARRIER: AA (American Airlines)"
  - Network Statistics section with flights, routes, airports
  - Top 10 Routes section sorted by flight count
  - Detailed flight listing below statistics
```

### Edge Cases

- **Unknown city**: AirportCodeMapper returns airport code if city unknown (graceful fallback)
- **Empty results**: Sorting empty list is safe (no-op)
- **Same date/time**: Sort is stable, maintains relative order

## Risk Assessment

**Low Risk Changes:**
- Sorting is non-destructive (doesn't modify data)
- City name display uses existing, tested utility
- No database or file I/O changes
- No API contract changes

**Potential Issues:**
- None identified

## Success Criteria

✅ Route Search results appear in chronological order (date, then time)
✅ Travel Planner shows city names alongside airport codes in header
✅ Flight Search results appear in chronological order
✅ Airport View shows city name alongside airport code in header
✅ Carrier View shows carrier name and comprehensive network statistics
✅ Carrier View displays top 10 routes by flight frequency
✅ All existing tests still pass
✅ No performance degradation (sorting and statistics are O(n log n), negligible for typical result sets)

## Verification

```bash
# Build and test
mvn clean test

# Run application
java -jar flight-schedule-reader/target/flight-schedule-reader.jar test-travel-planning.bookableflight.csv

# Test each feature:
# 1. Route Search: BWI → DAL (no date) - verify sorting
# 2. Travel Planner: BWI → DAL on 2025-06-10 - verify city names
# 3. Flight Search: AA 101 - verify sorting
# 4. Airport View: BWI - verify city name
# 5. Carrier View: AA - verify name, statistics, top routes
```

## Estimated Effort

- RouteSearchController: 2 minutes (simple sorting)
- DirectFlightFinderController: 5 minutes (add city names)
- FlightSearchController: 2 minutes (simple sorting)
- AirportViewController: 3 minutes (add city name)
- CarrierViewController: 15 minutes (statistics calculation, top routes, reformatting)
- Testing: 15 minutes (5 features to verify)
- **Total: ~40 minutes**
