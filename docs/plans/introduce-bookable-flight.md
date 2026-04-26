# Plan: Introduce BookableFlight Model for Date-Specific Flight Instances

## Context

**Problem Identified:** The flight-schedule-reader module incorrectly uses `ScheduledFlight` to represent specific bookable flight instances on particular dates. This causes incorrect behavior where a date-specific route search returns ALL instances of a flight instead of just the flight operating on the requested date.

**Root Cause:** The CSV format includes `departure_date` (e.g., 20250115) representing a specific instance, but `ScheduledFlight` is designed for recurring patterns with `effectiveFrom`/`effectiveUntil`/`daysOfOperation` fields. The reader currently maps `departure_date` → `effectiveFrom`, creating a single-day "pattern" for each instance.

**Example of Current Bug:**
- User queries: "CVG to LGA on 2025-01-15"
- CSV contains 5 different date instances of DL 5030 (lines 2-6 in sample-data.scheduledflight.csv)
- Current behavior: Returns all 5 instances
- Expected: Return only the flight on 2025-01-15

**Two Distinct Concepts:**
1. **ScheduledFlight** (asqp-reader): Recurring pattern - "DL 5030 operates Mon-Fri from LGA at 13:45, effective Jan-Mar 2025"
2. **BookableFlight** (flight-schedule-reader): Specific instance - "DL 5030 on 2025-01-15 from LGA at 13:45"

## Solution: Introduce BookableFlight Model

Create a new model in `flight-core` specifically for date-specific bookable instances.

---

## BookableFlight Model Structure

**Location:** `flight-core/src/main/java/com/lamontd/travel/flight/model/BookableFlight.java`

### Fields

```java
public class BookableFlight {
    // Flight identification
    private final String carrierCode;        // e.g., "DL"
    private final String flightNumber;       // e.g., "5030"
    
    // Route
    private final String originAirport;      // e.g., "LGA"
    private final String destinationAirport; // e.g., "CVG"
    
    // Date-specific scheduling (THE KEY DIFFERENCE)
    private final LocalDate operatingDate;   // e.g., 2025-01-15
    private final LocalTime scheduledDepartureTime; // e.g., 13:45
    private final LocalTime scheduledArrivalTime;   // e.g., 16:05
}
```

### CSV to BookableFlight Mapping

**CSV Format:**
```
carrier_code|flight_number|origin|destination|departure_date|scheduled_departure|scheduled_arrival
DL|5030|LGA|CVG|20250115|1345|1605
```

**Field Mapping:**
- `carrier_code` → `carrierCode` ("DL")
- `flight_number` → `flightNumber` ("5030")
- `origin` → `originAirport` ("LGA")
- `destination` → `destinationAirport` ("CVG")
- `departure_date` → `operatingDate` (LocalDate: 2025-01-15) **← KEY CHANGE**
- `scheduled_departure` → `scheduledDepartureTime` (LocalTime: 13:45)
- `scheduled_arrival` → `scheduledArrivalTime` (LocalTime: 16:05)

### Comparison with Other Models

| Field | FlightRecord | ScheduledFlight | BookableFlight |
|-------|-------------|-----------------|----------------|
| **Purpose** | Actual operations | Recurring pattern | Specific bookable instance |
| **Date Field** | `operatingDate` (what happened) | `effectiveFrom`/`effectiveUntil` (date range) | `operatingDate` (when scheduled) |
| **Time Fields** | `actualDepartureTime` (actual) | `scheduledDepartureTime` (pattern) | `scheduledDepartureTime` (scheduled) |
| **Day Pattern** | None | `daysOfOperation` (Set<DayOfWeek>) | None |
| **Status** | `FlightStatus` enum | None | None |
| **Tail Number** | Yes | No | No |

---

## Files Requiring Changes

### 1. CREATE: BookableFlight.java
**Path:** `flight-core/src/main/java/com/lamontd/travel/flight/model/BookableFlight.java`

**Structure:**
- 7 private final fields (as shown above)
- Builder pattern (consistent with FlightRecord and ScheduledFlight)
- Getters for all fields
- `getRouteKey()` method: returns `"carrierCode-flightNumber-origin-destination"`
- `equals()` and `hashCode()` including `operatingDate`
- `toString()` for debugging

### 2. RENAME: CsvScheduledFlightReader → CsvBookableFlightReader
**Path:** `flight-schedule-reader/src/main/java/com/lamontd/travel/flight/schedule/reader/CsvBookableFlightReader.java`

**Changes:**
- Rename class
- Change return type: `List<ScheduledFlight>` → `List<BookableFlight>`
- Line 119: Change mapping from `.effectiveFrom(departureDate)` to `.operatingDate(departureDate)`
- Update builder to use `BookableFlight.builder()`

### 3. UPDATE: ScheduleFlightIndex
**Path:** `flight-schedule-reader/src/main/java/com/lamontd/travel/flight/schedule/index/ScheduleFlightIndex.java`

**Changes:**
- Replace all `ScheduledFlight` with `BookableFlight`
- Update `getFlightsByRouteEffectiveOn(origin, dest, date)`:
  - Current: filters using `.isEffectiveOn(date)` (checks date range + day of week)
  - New: filters using `.getOperatingDate().equals(date)` (exact date match)
- Remove method: `getFlightsEffectiveOn(date)` - no longer meaningful without date ranges

### 4. UPDATE: ScheduleFlightDataLoader
**Path:** `flight-schedule-reader/src/main/java/com/lamontd/travel/flight/schedule/service/ScheduleFlightDataLoader.java`

**Changes:**
- Replace `ScheduledFlight` with `BookableFlight`
- Update reader instantiation: `new CsvBookableFlightReader()`

### 5. UPDATE: All View Classes
**Paths:**
- `flight-schedule-reader/src/main/java/com/lamontd/travel/flight/schedule/controller/RouteSearchController.java`
- `flight-schedule-reader/src/main/java/com/lamontd/travel/flight/schedule/controller/FlightSearchController.java`
- `flight-schedule-reader/src/main/java/com/lamontd/travel/flight/schedule/controller/AirportViewController.java`
- `flight-schedule-reader/src/main/java/com/lamontd/travel/flight/schedule/controller/CarrierViewController.java`

**Changes:**
- Replace `ScheduledFlight` with `BookableFlight`
- Update field access:
  - `.getEffectiveFrom()` → `.getOperatingDate()`
  - Remove calls to `.isEffectiveOn()` - no longer available

### 6. UPDATE: All Test Classes
**Paths:**
- `flight-schedule-reader/src/test/java/com/lamontd/travel/flight/schedule/reader/CsvScheduledFlightReaderTest.java`
- `flight-schedule-reader/src/test/java/com/lamontd/travel/flight/schedule/index/ScheduleFlightIndexTest.java`
- `flight-schedule-reader/src/test/java/com/lamontd/travel/flight/schedule/service/ScheduleFlightDataLoaderTest.java`

**Changes:**
- Rename test file: `CsvScheduledFlightReaderTest` → `CsvBookableFlightReaderTest`
- Replace `ScheduledFlight` with `BookableFlight`
- Update test assertions to use `.getOperatingDate()` instead of `.getEffectiveFrom()`

---

## Data Mapping Verification

### Current CSV Structure (from sample-data.scheduledflight.csv)
```
DL|5030|LGA|CVG|20250115|1345|1605  ← Each row is a SPECIFIC INSTANCE on a SPECIFIC DATE
```

### Current Mapping (INCORRECT)
```java
ScheduledFlight.builder()
    .carrierCode("DL")
    .flightNumber("5030")
    .originAirport("LGA")
    .destinationAirport("CVG")
    .effectiveFrom(LocalDate.of(2025, 1, 15))  // ← Treats as start of date range
    .scheduledDepartureTime(LocalTime.of(13, 45))
    .scheduledArrivalTime(LocalTime.of(16, 5))
    .build();
```

### New Mapping (CORRECT)
```java
BookableFlight.builder()
    .carrierCode("DL")
    .flightNumber("5030")
    .originAirport("LGA")
    .destinationAirport("CVG")
    .operatingDate(LocalDate.of(2025, 1, 15))  // ← Specific operating date
    .scheduledDepartureTime(LocalTime.of(13, 45))
    .scheduledArrivalTime(LocalTime.of(16, 5))
    .build();
```

---

## Preservation of ScheduledFlight in asqp-reader

**No changes required to asqp-reader.** The `ScheduledFlight` model remains in flight-core for its original purpose:

### Current asqp-reader Usage (FlightConverter.java, lines 22-33)
```java
public static ScheduledFlight toScheduledFlight(ASQPFlightRecord asqpRecord) {
    return ScheduledFlight.builder()
        .carrierCode(asqpRecord.getCarrierCode())
        .flightNumber(asqpRecord.getFlightNumber())
        .originAirport(asqpRecord.getOrigin())
        .destinationAirport(asqpRecord.getDestination())
        .scheduledDepartureTime(asqpRecord.getScheduledCrsDeparture())
        .scheduledArrivalTime(asqpRecord.getScheduledCrsArrival())
        .effectiveFrom(asqpRecord.getDepartureDate())     // ← Sets range
        .effectiveUntil(asqpRecord.getDepartureDate())    // ← Single day range
        .build();
}
```

This usage is **intentional** - it creates single-day schedule patterns from observed ASQP data. Later, `FlightScheduleService` aggregates these into recurring patterns by analyzing multiple dates.

---

## Implementation Order

1. **Create BookableFlight model** (flight-core)
2. **Update tests first** (TDD approach) - rename and update test classes
3. **Rename and update CsvBookableFlightReader**
4. **Update ScheduleFlightIndex**
5. **Update ScheduleFlightDataLoader**
6. **Update all view controllers**
7. **Run full test suite** (`mvn test`)
8. **Manual validation** - test route search with specific dates

---

## Verification Plan

### Unit Tests
```bash
cd flight-core && mvn test -Dtest=BookableFlightTest
cd flight-schedule-reader && mvn test
```

### Integration Test
```bash
# Build
mvn clean install

# Test route search with specific date
echo -e "1\nLGA\nCVG\n2025-01-15\nn\n5" | \
  java -jar flight-schedule-reader/target/flight-schedule-reader.jar \
  sample-data.scheduledflight.csv
```

**Expected Output:**
```
ROUTE: LGA → CVG (Date: 2025-01-15)
================================================================================
Carrier  Flight       Departure    Arrival      Operating Date
--------------------------------------------------------------------------------
DL       5030         13:45        16:05        2025-01-15

Total flights found: 1
```

**Current Buggy Output:**
```
Total flights found: 5  ← WRONG - returns all instances across dates
```

---

### 7. UPDATE: Python Grooming Script
**Path:** `src/main/scripts/asqp_bulk_data_groomer.py`

**Changes:**
- Update docstring: "ScheduledFlight: Schedule data with 7 fields for travel planning" → "BookableFlight: Bookable flight instances with 7 fields for travel planning"
- Update format choices: `'scheduledflight'` → `'bookableflight'` in argument parser
- Update FORMAT_EXTENSIONS: `'scheduledflight': '.scheduledflight.csv'` → `'bookableflight': '.bookableflight.csv'`
- Update all function names: `to_scheduledflight_dict()` → `to_bookableflight_dict()`, `write_scheduledflight_csv()` → `write_bookableflight_csv()`
- Update help text and example commands to use "bookableflight" terminology
- Update main() processing logic to handle 'bookableflight' format option

### 8. RENAME: Sample Data Files
**Files to rename:**
- `sample-data.scheduledflight.csv` → `sample-data.bookableflight.csv` (root directory)
- `flight-schedule-reader/src/main/resources/data/sample-data.scheduledflight.csv` → `sample-data.bookableflight.csv`

**Update references in:**
- `flight-schedule-reader/src/main/java/com/lamontd/travel/flight/schedule/App.java` - update default resource path and file pattern matching
- `flight-schedule-reader/src/test/java/com/lamontd/travel/flight/schedule/reader/CsvScheduledFlightReaderTest.java` - update test file references
- Shell scripts: `validate-*.sh` and `test-menu-options.sh` - update to use `sample-data.bookableflight.csv`
- Any documentation that references `.scheduledflight.csv` extension

---

## Summary

**What Changes:**
- New `BookableFlight` model for date-specific instances
- flight-schedule-reader refactored to use `BookableFlight`
- Query behavior fixed: date-specific searches return exact matches
- Python script fully refactored: `scheduledflight` → `bookableflight` (format option, file extension, function names)
- All data files renamed to `.bookableflight.csv` extension
- CSV reader class renamed: `CsvScheduledFlightReader` → `CsvBookableFlightReader`

**What Stays the Same:**
- `ScheduledFlight` remains unchanged in flight-core
- asqp-reader continues using `ScheduledFlight` for pattern analysis
- CSV format unchanged (same 7 fields, same data)
- Only naming changes to properly reflect the bookable flight concept
