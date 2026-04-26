# Plan: Add Summary Views and Date Range Filtering to Airport and Carrier Views

## Context

Currently, AirportViewController and CarrierViewController immediately dump all flight data when a code is entered. This can be overwhelming with large datasets. The user wants:

1. **Airport View Enhancement**: Show summary statistics first, then offer sub-menu for date-range filtered queries grouped by destination
2. **Carrier View Enhancement**: Show summary statistics first, then offer sub-menu for date-range filtered queries grouped by route

## Current Implementation

**AirportViewController:**
- Prompts for airport code
- Displays ALL departures and ALL arrivals
- No filtering, no grouping, no sub-menu

**CarrierViewController:**
- Prompts for carrier code
- Displays ALL flights grouped by route
- Shows all dates for every route
- No sub-menu

**ScheduleFlightIndex Methods Available:**
- `getFlightsByOrigin(airport)` - all departures
- `getFlightsByDestination(airport)` - all arrivals
- `getFlightsByCarrier(carrier)` - all carrier flights
- `getFlightsOnDate(date)` - filter by exact date
- `getFlightsByRouteOnDate(origin, dest, date)` - route on specific date

## Proposed Changes

### 1. AirportViewController Enhancement

**New Flow:**
```
Enter airport code: CVG

AIRPORT SUMMARY: CVG
===========================================
Total Departures: 245
Total Arrivals: 238
Destinations Served: 18
Origins Served: 16
Date Range in Data: 2025-01-01 to 2025-01-31

AIRPORT OPTIONS:
1. View departures by date range
2. View arrivals by date range
3. Return to main menu
```

**Sub-option 1 (Departures by Date Range):**
```
Enter start date (yyyy-MM-dd): 2025-01-10
Enter end date (yyyy-MM-dd): 2025-01-15

DEPARTURES FROM CVG (2025-01-10 to 2025-01-15)
================================================
Destination  Flights  Carriers
-----------  -------  ---------
LGA          12       DL, AA
JFK          8        DL
ATL          24       DL
...
```

**Sub-option 2 (Arrivals):** Same format but for arrivals grouped by origin

**Implementation:**
- Add `displaySummary()` method - calculates stats from full dataset
- Add `displaySubMenu()` method - shows 3 options
- Add `displayDeparturesByDateRange()` - prompts for dates, filters, groups by destination
- Add `displayArrivalsByDateRange()` - prompts for dates, filters, groups by origin
- Add helper `parseDate()` method for date input validation

### 2. CarrierViewController Enhancement

**New Flow:**
```
Enter carrier code: DL

CARRIER SUMMARY: DL
===========================================
Total Flights: 1,234
Total Routes: 42
Date Range in Data: 2025-01-01 to 2025-01-31
Average Flights per Day: 39.8

CARRIER OPTIONS:
1. View flights by date range (grouped by route)
2. Return to main menu
```

**Sub-option 1:**
```
Enter start date (yyyy-MM-dd): 2025-01-10
Enter end date (yyyy-MM-dd): 2025-01-15

DL FLIGHTS (2025-01-10 to 2025-01-15)
================================================
Route        Flights  Unique Flight Numbers
-----------  -------  --------------------
CVG-LGA      12       5030, 5032
ATL-ORD      24       2100, 2102, 2104
...

Total Routes in Range: 38
Total Flights in Range: 235
```

**Implementation:**
- Add `displaySummary()` method - calculates carrier-wide stats
- Add `displaySubMenu()` method - shows 2 options
- Add `displayFlightsByDateRange()` - prompts for dates, filters, groups by route
- Add helper `parseDate()` method

### 3. New Index Methods (ScheduleFlightIndex)

Add filtering methods:

```java
// Filter by date range
public List<BookableFlight> getFlightsByOriginInDateRange(
    String airport, LocalDate startDate, LocalDate endDate)

public List<BookableFlight> getFlightsByDestinationInDateRange(
    String airport, LocalDate startDate, LocalDate endDate)

public List<BookableFlight> getFlightsByCarrierInDateRange(
    String carrier, LocalDate startDate, LocalDate endDate)
```

These filter existing indices by date range using `operatingDate`.

## Files to Modify

1. **ScheduleFlightIndex.java** - Add 3 date-range filter methods
2. **AirportViewController.java** - Refactor to summary + sub-menu pattern
3. **CarrierViewController.java** - Refactor to summary + sub-menu pattern

## Implementation Details

### Date Input Pattern
```java
private LocalDate parseDate(Scanner scanner, String prompt) {
    System.out.print(prompt);
    String input = scanner.nextLine().trim();
    try {
        return LocalDate.parse(input); // yyyy-MM-dd format
    } catch (DateTimeParseException e) {
        System.out.println("Invalid date format. Use yyyy-MM-dd");
        return null;
    }
}
```

### Summary Statistics Pattern
```java
// Calculate date range
LocalDate minDate = flights.stream()
    .map(BookableFlight::getOperatingDate)
    .min(LocalDate::compareTo)
    .orElse(null);
    
LocalDate maxDate = flights.stream()
    .map(BookableFlight::getOperatingDate)
    .max(LocalDate::compareTo)
    .orElse(null);

// Calculate unique destinations
Set<String> destinations = flights.stream()
    .map(BookableFlight::getDestinationAirport)
    .collect(Collectors.toSet());
```

### Grouping Pattern for Display
```java
Map<String, Long> flightsByDestination = filteredFlights.stream()
    .collect(Collectors.groupingBy(
        BookableFlight::getDestinationAirport,
        Collectors.counting()
    ));

// Display grouped results
for (Map.Entry<String, Long> entry : flightsByDestination.entrySet()) {
    System.out.printf("%-12s %7d%n", entry.getKey(), entry.getValue());
}
```

## Verification Plan

### Test AirportViewController
```bash
# Build
mvn clean package -DskipTests

# Run and test
java -jar flight-schedule-reader/target/flight-schedule-reader.jar sample-data.bookableflight.csv

# Select option 3 (Airport View)
# Enter: CVG
# Verify summary displays
# Select option 1 (departures by date)
# Enter dates: 2025-01-10 to 2025-01-15
# Verify grouped output by destination
```

### Test CarrierViewController
```bash
# Select option 4 (Carrier View)
# Enter: DL
# Verify summary displays with avg flights/day
# Select option 1 (flights by date range)
# Enter dates: 2025-01-10 to 2025-01-15
# Verify grouped output by route
```

## Success Criteria

- ✓ Airport View shows summary before detailed data
- ✓ Airport View has sub-menu with date range options
- ✓ Date range filtering works correctly
- ✓ Departures grouped by destination with counts
- ✓ Arrivals grouped by origin with counts
- ✓ Carrier View shows summary with statistics
- ✓ Carrier View has sub-menu for date range queries
- ✓ Carrier flights grouped by route with counts
- ✓ Invalid dates handled gracefully
- ✓ User can return to main menu from sub-menus
