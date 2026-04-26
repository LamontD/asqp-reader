# Plan: ScheduledFlight CSV Processing Capability

## Context

This plan adds the ability to extract and process ScheduledFlight data from ASQP records. Currently, the system:
1. Reads raw ASQP data and infers schedules at runtime in FlightDataIndex
2. Has a Python grooming script that produces two formats: ASQPFlightRecord and FlightRecord

The goal is to add a third format (ScheduledFlight) that extracts schedule information from ASQP data into a separate CSV format that can be read directly by the Java application. This will enable:
- Standalone schedule data processing (without needing full ASQP records)
- Smaller file sizes for schedule-only queries
- Potential to merge schedule data from multiple sources (ASQP, airline APIs, GDS systems)

## Step 1: Extend Python Grooming Script

### Overview
Add ScheduledFlight as a third output format to `asqp_bulk_data_groomer.py`.

### CSV Format Specification

**File Extension:** `.scheduledflight.csv`

**Fields (pipe-delimited):**
```
carrier_code|flight_number|origin|destination|departure_date|scheduled_departure|scheduled_arrival
```

**Field Mappings from ASQP 234 Data:**
- `carrier_code` → row[0] (carrier code)
- `flight_number` → row[1] (flight number)
- `origin` → row[6] (origin airport)
- `destination` → row[7] (destination airport)
- `departure_date` → row[8] (date in YYYYMMDD format)
- `scheduled_departure` → row[11] (scheduled_crs_departure in HHMM format)
- `scheduled_arrival` → row[14] (scheduled_crs_arrival in HHMM format)

**Note on departure_date:** Including departure_date in the ScheduledFlight CSV allows for:
1. Tracking schedule changes over time (same flight# may have different times on different dates)
2. Seasonal variations
3. One-time schedule adjustments
The Java reader will group by carrier+flight#+origin+dest and infer recurring patterns.

### Implementation Details

**1. Add ScheduledFlight class to Python script:**
```python
def to_scheduledflight_dict(self) -> Dict[str, str]:
    """Convert to ScheduledFlight format (schedule fields only)."""
    return {
        "carrier_code": self.carrier_code,
        "flight_number": self.flight_number,
        "origin": self.origin,
        "destination": self.destination,
        "departure_date": self.departure_date,
        "scheduled_departure": self.scheduled_crs_departure,
        "scheduled_arrival": self.scheduled_crs_arrival
    }
```

**2. Add write function:**
```python
def write_scheduledflight_csv(records: List[ASQPFlightRecord], output_filepath: str) -> None:
    """Write ScheduledFlight format CSV (7 fields)."""
    headers = [
        "carrier_code", "flight_number", "origin", "destination",
        "departure_date", "scheduled_departure", "scheduled_arrival"
    ]
    # ... writer implementation
```

**3. Update FORMAT_EXTENSIONS:**
```python
FORMAT_EXTENSIONS = {
    'asqp': '.asqpflightrecord.csv',
    'flightrecord': '.flightrecord.csv',
    'scheduledflight': '.scheduledflight.csv'
}
```

**4. Update format choices:**
```python
parser.add_argument('--format', '-f',
                   choices=['asqp', 'flightrecord', 'scheduledflight', 'both', 'all'],
                   default='asqp',
                   help='Output format (default: asqp)')
```

**5. Add scheduledflight-output-dir argument:**
```python
parser.add_argument('--scheduledflight-output-dir',
                   help='Separate output directory for ScheduledFlight format')
```

**6. Update main() processing logic:**
- Handle 'scheduledflight' as a format choice
- Support 'all' option to generate all three formats
- Create scheduledflight output directory
- Process files for scheduledflight format

### Files to Modify
- `src/main/scripts/asqp_bulk_data_groomer.py` (all changes in one file)

### Example Usage After Implementation
```bash
# ScheduledFlight format only
python asqp_bulk_data_groomer.py ./raw_data ./output --format scheduledflight

# All three formats
python asqp_bulk_data_groomer.py ./raw_data ./output --format all

# All formats to different directories
python asqp_bulk_data_groomer.py ./raw_data ./output --format all \
    --asqp-output-dir ./asqp \
    --flightrecord-output-dir ./flightrecord \
    --scheduledflight-output-dir ./schedules
```

## Step 2: New Module - flight-schedule-reader

### Architectural Decision

Create a completely separate Maven module `flight-schedule-reader` that mirrors the structure of `asqp-reader` but focuses exclusively on schedule data. This provides:
- **Clean separation of concerns** - schedule vs actual operations
- **Independent evolution** - can modify schedule functionality without affecting ASQP
- **Parallel structure** - similar patterns to asqp-reader for maintainability
- **Flexibility** - can run standalone or integrate with other modules later

### Module Structure

```
flight-schedule-reader/
├── pom.xml
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/lamontd/travel/flight/schedule/
│   │   │       ├── App.java                          # Main entry point
│   │   │       ├── controller/
│   │   │       │   ├── MenuController.java           # Top-level menu (4 options)
│   │   │       │   ├── DataViewSubmenu.java          # Submenu 1: Data views
│   │   │       │   ├── FlightReportSubmenu.java      # Submenu 2: Flight reports
│   │   │       │   ├── RouteAnalysisSubmenu.java     # Submenu 3: Route network
│   │   │       │   └── FlightSchedulingSubmenu.java  # Submenu 4: Travel planning
│   │   │       ├── index/
│   │   │       │   ├── ScheduleIndex.java            # Pre-computed indices
│   │   │       │   └── ScheduleDateRouteIndex.java   # RouteIndex adapter
│   │   │       ├── reader/
│   │   │       │   └── CsvScheduledFlightReader.java # CSV parser
│   │   │       ├── service/
│   │   │       │   └── ScheduleDataLoader.java       # Load multiple files
│   │   │       └── view/
│   │   │           ├── CarrierScheduleView.java      # Carrier view (Data View)
│   │   │           ├── AirportScheduleView.java      # Airport view (Data View)
│   │   │           ├── FlightScheduleLookupView.java # Flight lookup (Flight Report)
│   │   │           ├── RouteScheduleView.java        # Route lookup (Flight Report)
│   │   │           ├── RouteNetworkView.java         # Network analysis (Route Analysis)
│   │   │           ├── TravelPlannerView.java        # Find flights (Scheduling)
│   │   │           └── ConnectionFinderView.java     # Multi-leg trips (Scheduling)
│   │   └── resources/
│   │       ├── data/                                 # Sample schedule CSVs
│   │       └── logback.xml                           # Logging config
│   └── test/
│       └── java/
│           └── com/lamontd/travel/flight/schedule/
│               ├── reader/
│               │   └── CsvScheduledFlightReaderTest.java
│               ├── index/
│               │   └── ScheduleIndexTest.java
│               └── service/
│                   └── ScheduleDataLoaderTest.java
```

### Component Specifications

#### 1. pom.xml Configuration

**Parent:** flight-data-analysis  
**Artifact ID:** flight-schedule-reader  
**Packaging:** jar (executable with Maven Shade Plugin)

**Dependencies:**
- `flight-core` - for ScheduledFlight model, shared utilities, RouteGraphService
- `jgrapht-core` - for RouteGraphService graph operations (inherited via flight-core)
- `slf4j-api` - logging (inherited from parent)
- `logback-classic` - logging implementation
- `junit-jupiter` - testing (test scope)

**Build Configuration:**
- Maven Shade Plugin to create executable JAR
- Main class: `com.lamontd.travel.flight.schedule.App`
- Final JAR name: `flight-schedule-reader.jar`

#### 2. CsvScheduledFlightReader.java

**Location:** `flight-schedule-reader/src/main/java/.../reader/`

**Responsibilities:**
- Parse pipe-delimited ScheduledFlight CSV files
- Handle header row (skip or validate)
- Parse 7 fields per row
- Convert string dates (YYYYMMDD) to LocalDate
- Convert string times (HHMM) to LocalTime
- Create ScheduledFlight objects via builder pattern
- Validation with detailed error messages
- Line-by-line error handling (log and skip bad rows)

**Key Methods:**
```java
public List<ScheduledFlight> read(Path filePath)
public List<ScheduledFlight> read(InputStream inputStream)
private ScheduledFlight parseLine(String line, int lineNumber)
private LocalDate parseDate(String dateStr)
private LocalTime parseTime(String timeStr)
```

**Error Handling:**
- Invalid date/time format: log warning, skip row
- Missing required fields: log error, skip row
- Invalid airport codes: log warning, keep row (validation elsewhere)
- Return all successfully parsed schedules

#### 3. ScheduleDataLoader.java

**Location:** `flight-schedule-reader/src/main/java/.../service/`

**Responsibilities:**
- Load multiple ScheduledFlight CSV files from directory
- Parallel loading using virtual threads (Java 21)
- Aggregate schedules from all files
- Report loading statistics (files, records, errors)
- Performance timing

**Key Methods:**
```java
public List<ScheduledFlight> loadSchedules(Path directory)
public List<ScheduledFlight> loadSchedules(List<Path> files)
public LoadResult loadSchedulesWithStats(Path directory)
```

**Pattern:** Mirror FlightDataLoader from asqp-reader
- Use PerformanceTimer for timing
- Use SLF4J logging for progress
- Virtual threads for parallel file processing
- Collect all results into single list

#### 4. ScheduleIndex.java

**Location:** `flight-schedule-reader/src/main/java/.../index/`

**Responsibilities:**
- Pre-compute indices during construction
- Group and deduplicate schedules
- Provide O(1) lookups for common queries
- Calculate statistics

**Index Maps (Date-Specific Approach):**
```java
// Core indices - optimized for "what flies on this date?" queries
Map<LocalDate, Map<String, List<ScheduledFlight>>> byDateAndRoute;  // date -> "ORD-LAX" -> flights
Map<LocalDate, List<ScheduledFlight>> byDate;                       // all flights on date
Map<String, Map<LocalDate, List<ScheduledFlight>>> byRouteAndDate;  // "ORD-LAX" -> date -> flights

// Supporting indices
Map<String, List<ScheduledFlight>> byCarrier;            // "AA" -> all AA flights
Map<String, List<ScheduledFlight>> byFlightNumber;       // "AA100" -> all dates for AA100
Map<String, List<ScheduledFlight>> byOriginAirport;      // "ORD" -> all departures from ORD
Map<String, List<ScheduledFlight>> byDestinationAirport; // "LAX" -> all arrivals to LAX

// Statistics (cached)
long totalSchedules;
long uniqueRoutes;
long uniqueCarriers;
Set<String> allAirports;
Set<LocalDate> availableDates;  // all dates with schedule data
LocalDate minDate;
LocalDate maxDate;
```

**No Deduplication Needed:**
- Each CSV row represents a scheduled flight on a specific date
- Keep all date-specific records as-is
- No pattern inference or aggregation
- Simple, fast lookups by date

**Key Methods:**
```java
// Date-specific queries (PRIMARY USE CASE)
public List<ScheduledFlight> getFlightsOnDate(LocalDate date, String origin, String dest)
public List<ScheduledFlight> getAllFlightsOnDate(LocalDate date)
public List<ScheduledFlight> getDeparturesOnDate(LocalDate date, String airport)

// Supporting queries
public List<ScheduledFlight> getByRoute(String origin, String dest)  // all dates
public List<ScheduledFlight> getByFlightNumber(String carrier, String flightNum)
public List<ScheduledFlight> getByCarrier(String carrier)
public Set<String> getAllRoutes()
public Set<LocalDate> getAvailableDates()

// RouteIndex adapter for pathfinding
public RouteIndex getRouteIndexForDate(LocalDate date)
```

#### 5a. ScheduleDateRouteIndex (RouteIndex Adapter)

**Location:** `flight-schedule-reader/src/main/java/.../index/`

**Purpose:** Adapter to make date-specific schedule data work with RouteGraphService

**Responsibilities:**
- Implements RouteIndex interface (from flight-core)
- Filters schedules to specific date
- Provides route/airport lists for that date only
- Enables RouteGraphService to build graph from schedule data

**Usage:**
```java
// In ConnectionFinderView:
LocalDate travelDate = LocalDate.of(2025, 6, 10);
RouteIndex dateIndex = scheduleIndex.getRouteIndexForDate(travelDate);
RouteGraphService routeGraph = new RouteGraphService(dateIndex);
List<String> path = routeGraph.findShortestPath("BWI", "DAL");
```

#### 5. App.java - Main Application

**Location:** `flight-schedule-reader/src/main/java/.../App.java`

**Responsibilities:**
- Parse command-line arguments
- Load schedule CSV files
- Build ScheduleIndex
- Launch MenuController
- Handle errors gracefully

**Command-Line Arguments:**
```
Usage: flight-schedule-reader [FILES...]

Arguments:
  FILES...    One or more schedule CSV files or directories

Examples:
  # Single file
  flight-schedule-reader schedules-2025.scheduledflight.csv

  # Multiple files
  flight-schedule-reader jan.csv feb.csv mar.csv

  # Directory (loads all .scheduledflight.csv files)
  flight-schedule-reader ./schedule-data/

  # No arguments: loads from default resource directory
  flight-schedule-reader
```

**Startup Flow:**
1. Parse arguments
2. Determine file paths (args or default resources)
3. Load schedules with ScheduleDataLoader
4. Build ScheduleIndex (with performance timing)
5. Display summary statistics
6. Launch MenuController for interactive queries

#### 6. MenuController.java

**Location:** `flight-schedule-reader/src/main/java/.../controller/`

**Menu Structure (Mirrors ASQP Structure):**
```
FLIGHT SCHEDULE ANALYSIS
========================
1. Data View
2. Flight Report
3. Route Network Analysis
4. Flight Scheduling
5. Exit

Select option (1-5):
```

**Submenu Controllers:**

**1. DataViewSubmenu**
- Carrier Schedule View (all flights for a carrier)
- Airport Schedule View (departures/arrivals at airport)
- Pattern: Mirror ASQP DataViewSubmenu structure

**2. FlightReportSubmenu**
- Flight Schedule Lookup (specific flight number schedule)
- Route Schedule Lookup (flights between two airports)
- Schedule Frequency Analysis (how often flights operate)
- Focus: Schedule information, NOT performance metrics

**3. RouteAnalysisSubmenu**
- Route Network Analysis (leverage RouteGraphService)
- Shortest Path Between Airports
- Hub Airport Analysis
- Pattern: Mirror ASQP RouteAnalysisView functionality

**4. FlightSchedulingSubmenu**
- Travel Planner (PRIMARY: "Find flights from BWI to DAL on June 10")
- Connection Finder (multi-leg journeys)
- Date-based Flight Search
- Future expansion: Multi-city trips, time preferences, etc.

**Pattern:** Identical structure to asqp-reader
- Main MenuController dispatches to submenus
- Each submenu is a separate controller class
- Delegate rendering to view classes
- Scanner for user input

#### 7. View Classes (Organized by Menu)

**Data View Submenu:**

**CarrierScheduleView.java**
- Display all scheduled flights for a carrier
- Group by route (origin → destination)
- Show flight counts per route
- List operating dates
- Pattern: Mirror ASQP CarrierView structure

**AirportScheduleView.java**
- Display departures OR arrivals for an airport
- User selects airport and date (or date range)
- Group by destination/origin
- Sort by departure/arrival time
- Pattern: Mirror ASQP AirportView structure

---

**Flight Report Submenu:**

**FlightScheduleLookupView.java**
- Look up specific flight by carrier + number
- Show all dates it operates
- Show scheduled times for each date
- Display route information

**RouteScheduleView.java**
- Display all flights between two airports
- User inputs origin and destination
- Show all carriers operating the route
- List all available dates
- Sort by carrier and departure time

**ScheduleFrequencyView.java** (NEW)
- Analyze how often a flight operates
- Show weekly patterns
- Identify seasonal variations
- No performance metrics (unlike ASQP)

---

**Route Network Analysis Submenu:**

**RouteNetworkView.java**
- Leverage RouteGraphService for network analysis
- Display hub airports (high connectivity)
- Show route network statistics
- Shortest path finder between any two airports
- Pattern: Mirror ASQP RouteAnalysisView

---

**Flight Scheduling Submenu:**

**TravelPlannerView.java** - PRIMARY USE CASE
- User inputs: origin, destination, travel date
- Shows all direct flights on that date
- Shows connecting flight options (1 connection)
- Sorts by total travel time
- Displays departure/arrival times, carriers
- **This is the core "find a flight" capability**

**ConnectionFinderView.java**
- **Leverages RouteGraphService** for multi-leg journeys
- Build date-specific graph: only flights operating on user's travel date
- Use Dijkstra for shortest path
- Validate connection times between legs
- Show itinerary options with total travel time
- **Implementation:** Uses ScheduleDateRouteIndex (RouteIndex adapter)

**DateRangeSearchView.java** (FUTURE)
- Search for flights across multiple dates
- "Show me flights from BWI to DAL June 10-15"
- Compare options across dates
- Find cheapest day to fly (if price data available)

### Maven Integration

**Update parent pom.xml:**
```xml
<modules>
  <module>flight-core</module>
  <module>asqp-reader</module>
  <module>flight-schedule-reader</module>  <!-- NEW -->
</modules>
```

### Build and Run

**Build:**
```bash
mvn clean install
```

**Run:**
```bash
# From JAR
java -jar flight-schedule-reader/target/flight-schedule-reader.jar [files...]

# From Maven
mvn exec:java -pl flight-schedule-reader -Dexec.args="schedules.csv"
```

### Design Questions (RESOLVED by separate module approach)

**1. Data Source Strategy:** ✓ RESOLVED
- Separate applications: `asqp-reader` (actual operations) vs `flight-schedule-reader` (schedules)
- No data source conflicts - each app has its own purpose
- Can run independently or compare results externally
- Future: could create unified app that loads both, but start separate

**2. Index Strategy:** ✓ RESOLVED
- `ScheduleIndex` in flight-schedule-reader (separate from FlightDataIndex)
- Each module owns its index implementation
- Similar structure but optimized for different queries
- No need for unified interface (separate apps)

**3. Deduplication:** ✓ RESOLVED
- Keep date-specific records in CSV (raw data preservation)
- Deduplicate at load time in ScheduleIndex constructor
- Group by carrier+flight#+route, infer pattern from dates
- Match current FlightDataIndex behavior for consistency

**4. Application Startup:** ✓ RESOLVED
- `asqp-reader` expects .asqpflightrecord.csv files
- `flight-schedule-reader` expects .scheduledflight.csv files
- No auto-detection needed - use appropriate app for data type
- Clean separation, no ambiguity

**5. Schedule Updates:** ✓ RESOLVED
- CSV contains date-specific schedules (departure_date field)
- Infer effectiveFrom/effectiveUntil from min/max dates in data
- Infer daysOfOperation from observed days in date range
- Match current behavior in FlightDataIndex

### Design Questions - FINALIZED

**1. Days of Operation in CSV:** ✓ NOT NEEDED
- Module purpose: Path finding for specific travel dates
- Use case: "What flights from BWI to DAL on 2025-06-10?"
- Query by specific date, not by day-of-week pattern
- CSV contains date-specific records (departure_date field sufficient)

**2. Time Format:** ✓ HHMM
- Confirmed: HHMM format (e.g., "0600" for 6:00 AM)
- Matches ASQP convention

**3. CSV Aggregation:** ✓ One file per month
- Generate one .scheduledflight.csv per input ASQP file
- Maintains monthly granularity

### Primary Use Case: Travel Path Finding

The flight-schedule-reader module focuses on **travel planning queries**, not schedule pattern analysis:

**Example Queries:**
- "What flights are available from BWI to DAL on June 10, 2025?"
- "What's the best route from BWI to SFO with one connection?"
- "Show me all morning flights from ORD to LAX on a specific date"
- "Find the fastest path from BWI to DAL (direct or connecting)"

**Key Capabilities Needed:**
1. **Date-specific lookups** - find flights operating on exact date
2. **Route finding** - identify available paths (direct + multi-leg)
3. **Connection discovery** - find valid connecting flights
4. **Time-based filtering** - morning/afternoon/evening flights
5. **Journey optimization** - shortest time, fewest connections, earliest arrival

## Step 3: Testing Strategy (TO BE DESIGNED)

- Unit tests for CsvScheduledFlightReader
- Unit tests for ScheduleDataLoader
- Integration tests comparing ASQP-inferred vs CSV-loaded schedules
- Performance tests (schedule loading time vs ASQP loading + inference)
- End-to-end test with sample schedule CSV files

## Verification Plan

### Step 1 Verification (Python Script)
1. Run script on sample ASQP data with `--format scheduledflight`
2. Verify output file has correct extension (`.scheduledflight.csv`)
3. Verify headers match specification
4. Verify field count (7 fields per row)
5. Verify data extraction (check carrier, flight#, times match ASQP source)
6. Test `--format all` produces all three output formats
7. Compare record counts across formats (should be identical)

### Step 2 Verification (Java Reader) - TO BE DETAILED
- After discussion and implementation

## Open Questions

1. **Days of Operation:** Should ScheduledFlight CSV include a days_of_operation field (e.g., "1234567" for daily, "17" for Mon/Sun), or infer from dates?

2. **Effective Dates:** Should CSV include effective_from/effective_until, or calculate from min/max dates in the data?

3. **Time Format:** HHMM (0600) vs HH:MM (06:00)? Current ASQP uses HHMM, ScheduledFlight.java expects LocalTime. Reader will need to parse accordingly.

4. **File Naming:** For monthly ASQP files, should schedule files be:
   - One schedule file per ASQP file (e.g., `202501.scheduledflight.csv`)?
   - Aggregated into single schedule file (e.g., `schedules-2025.csv`)?
   - Current plan: mirror ASQP naming (one-to-one mapping)

## Summary

### Step 1: Python Script Enhancement ✓ READY
**Extend asqp_bulk_data_groomer.py to produce ScheduledFlight CSV**
- 7-field format: carrier_code, flight_number, origin, destination, departure_date, scheduled_departure, scheduled_arrival
- HHMM time format
- One CSV file per ASQP input file
- Add 'scheduledflight' and 'all' format options
- ~100-150 lines of code
- Low risk, follows established patterns

### Step 2: New Module - flight-schedule-reader ✓ DESIGNED
**Create standalone Maven module for schedule data processing**

**Key Decisions (Finalized):**
- ✓ Separate module (parallel to asqp-reader)
- ✓ Primary use case: Travel path finding ("BWI to DAL on June 10, 2025")
- ✓ Date-specific indexing (no pattern inference needed)
- ✓ Leverage RouteGraphService for connection finding
- ✓ HHMM time format
- ✓ One CSV per month

**Core Components:**
1. CsvScheduledFlightReader - parse CSV files
2. ScheduleDataLoader - parallel loading with virtual threads
3. ScheduleIndex - date-specific indices for fast lookups
4. ScheduleDateRouteIndex - RouteIndex adapter for pathfinding
5. App - CLI with menu system
6. Views - TravelPlannerView, RouteFinderView, ConnectionFinderView, etc.

**Estimated Scope:**
- ~600-800 lines of production code
- ~300-400 lines of test code
- Reuses flight-core extensively (ScheduledFlight, RouteGraphService, utilities)

### Step 3: Testing ✓ PLANNED
- Unit tests for CsvScheduledFlightReader (CSV parsing, date/time parsing)
- Unit tests for ScheduleDataLoader (parallel loading, error handling)
- Unit tests for ScheduleIndex (date-specific lookups, filtering)
- Integration test for RouteGraphService with schedule data
- End-to-end test: load CSV → query flights on date → find connections

---

## Implementation Order

**Phase 1:** Python Script (Step 1)
1. Add to_scheduledflight_dict() method to ASQPFlightRecord
2. Add write_scheduledflight_csv() function
3. Update FORMAT_EXTENSIONS and argument parser
4. Update main() to handle 'scheduledflight' and 'all' formats
5. Test with sample ASQP data
6. **Estimated time: 1-2 hours**

**Phase 2:** Module Scaffold (Step 2a)
1. Create flight-schedule-reader directory structure
2. Create pom.xml with flight-core dependency
3. Add to parent pom.xml modules list
4. Create package structure (reader, service, index, view, controller)
5. Verify Maven build
6. **Estimated time: 30 minutes**

**Phase 3:** Core Functionality (Step 2b)
1. Implement CsvScheduledFlightReader + tests
2. Implement ScheduleDataLoader + tests
3. Implement ScheduleIndex with date-specific maps + tests
4. Implement ScheduleDateRouteIndex (RouteIndex adapter)
5. **Estimated time: 3-4 hours**

**Phase 4:** Application & Views (Step 2c)
1. Implement App.java with CLI argument handling
2. Implement MenuController (top-level 4-option menu)
3. Implement submenu controllers:
   - DataViewSubmenu (Carrier, Airport views)
   - FlightReportSubmenu (Flight lookup, Route schedule views)
   - RouteAnalysisSubmenu (Network analysis with RouteGraphService)
   - FlightSchedulingSubmenu (Travel planner, Connection finder)
4. Implement view classes (8 views total)
5. Add logback.xml configuration
6. **Estimated time: 4-6 hours**

**Phase 5:** Integration Testing (Step 3)
1. Create sample .scheduledflight.csv files for testing
2. End-to-end test: Python script → Java reader → query flights
3. Performance testing with larger datasets
4. Update CLAUDE.md documentation
5. **Estimated time: 1-2 hours**

**Total Estimated Time: 9-14 hours of development**

---

## Detailed Menu Structure

### Main Menu (MenuController)
```
FLIGHT SCHEDULE ANALYSIS
========================
1. Data View
2. Flight Report  
3. Route Network Analysis
4. Flight Scheduling
5. Exit

Select option (1-5):
```

### Submenu 1: Data View (DataViewSubmenu)
```
DATA VIEW
=========
1. Carrier Schedule View
2. Airport Schedule View
3. Return to Main Menu

Select option (1-3):
```

### Submenu 2: Flight Report (FlightReportSubmenu)
```
FLIGHT REPORT
=============
1. Flight Schedule Lookup (by flight number)
2. Route Schedule Lookup (between two airports)
3. Schedule Frequency Analysis
4. Return to Main Menu

Select option (1-4):
```

### Submenu 3: Route Network Analysis (RouteAnalysisSubmenu)
```
ROUTE NETWORK ANALYSIS
======================
1. Network Overview & Hub Analysis
2. Shortest Path Finder
3. Route Connectivity Map
4. Return to Main Menu

Select option (1-4):
```

### Submenu 4: Flight Scheduling (FlightSchedulingSubmenu)
```
FLIGHT SCHEDULING
=================
1. Travel Planner (Find flights by date)
2. Connection Finder (Multi-leg journeys)
3. Date Range Search (coming soon)
4. Return to Main Menu

Select option (1-4):
```

---

## Success Criteria

**Python Script:**
- ✓ Produces valid .scheduledflight.csv files from ASQP data
- ✓ 7 fields per row, pipe-delimited
- ✓ Record count matches input ASQP file
- ✓ Times in HHMM format, dates in YYYYMMDD format

**Java Module:**
- ✓ Loads schedule CSV files successfully
- ✓ Can query: "Show flights from BWI to DAL on 2025-06-10"
- ✓ Can find connections using RouteGraphService
- ✓ Interactive menu works smoothly
- ✓ All tests pass (95%+ code coverage)
- ✓ Performance: loads 100K schedules in <2 seconds

**Integration:**
- ✓ Python script output can be read by Java module
- ✓ Date-specific queries return correct results
- ✓ Connection finding produces valid multi-leg itineraries
- ✓ Build: `mvn clean install` succeeds for all modules
