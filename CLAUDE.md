# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a multi-module Maven Java 23 project for analyzing Airline Service Quality Performance (ASQP) flight records. The project consists of two modules:

- **flight-core**: Reusable library with data models, mappers, and utilities
- **asqp-reader**: Interactive CLI application for flight data analysis

## Build Commands

### Build Entire Project
```bash
# From root directory - builds both modules and runs all 89 tests
mvn clean install

# Skip tests for faster builds
mvn clean install -DskipTests
```

### Build Individual Modules
```bash
# Build flight-core only (must be done before asqp-reader)
cd flight-core && mvn clean install

# Build asqp-reader only (requires flight-core already installed)
cd asqp-reader && mvn clean package
```

### Run Tests
```bash
# Run all tests (88 total: 49 in flight-core, 39 in asqp-reader)
mvn test

# Run tests for specific module
cd flight-core && mvn test
cd asqp-reader && mvn test

# Run single test class
mvn test -Dtest=CarrierCodeMapperTest
mvn test -Dtest=FlightDataIndexTest

# Run single test method
mvn test -Dtest=CarrierCodeMapperTest#testLoadFromResource
```

### Run Application
```bash
# Run with sample data
java -jar asqp-reader/target/asqp-reader.jar

# Run with specific CSV files
java -jar asqp-reader/target/asqp-reader.jar file1.csv file2.csv

# Must build first if JAR doesn't exist
mvn clean install
```

## Architecture Overview

### Module Dependencies
The project uses Maven multi-module architecture:
- **asqp-reader** depends on **flight-core** (declared in asqp-reader/pom.xml)
- **flight-core** must be built and installed to local Maven repo before building asqp-reader
- Both modules share dependency management from parent pom.xml

### Core Data Models (flight-core)

The project uses three distinct model types that serve different purposes:

1. **ASQPFlightRecord** (`asqp-reader/model/ASQPFlightRecord.java`): Raw ASQP data from CSV files with all scheduled and actual times, including both OAG and CRS scheduled times. This is the input format.

2. **FlightRecord** (`flight-core/model/FlightRecord.java`): Represents an *observed* flight instance - what actually happened on a specific date. Contains actual times, status (SCHEDULED/DEPARTED/IN_FLIGHT/LANDED/ARRIVED/CANCELLED), cancellation codes, delay information, and UTC timestamps. Think of this as "what did happen."

3. **ScheduledFlight** (`flight-core/model/ScheduledFlight.java`): Represents a *planned* recurring flight schedule - what should happen. Contains scheduled times, effective date range, and days of operation. Think of this as "what should happen."

The **FlightConverter** class (`asqp-reader/FlightConverter.java`) converts between these representations:
- `toFlightRecord()`: ASQP → FlightRecord (for analyzing actual operations)
- `toScheduledFlight()`: ASQP → ScheduledFlight (for single instance)

### Reference Data Mappers (flight-core)

All mappers follow a common pattern with static factory methods:

- **CarrierCodeMapper**: Maps 2-letter codes (e.g., "DL") to airline info using OpenFlights database (992+ carriers)
  - `CarrierCodeMapper.getDefault()` loads from `resources/data/airlines.dat`
  - Returns `CarrierInfo` with name, ICAO code, callsign, country, active status

- **AirportCodeMapper**: Maps 3-letter codes (e.g., "ATL") to airport details using OpenFlights (6,033+ airports)
  - `AirportCodeMapper.getDefault()` loads from `resources/data/airports.dat`
  - Returns `AirportInfo` with name, city, country, lat/lon, altitude, timezone, ICAO code

- **CancellationCodeMapper**: Maps single-letter cancellation codes (A=Carrier, B=Weather, C=NAS, D=Security)
  - Hardcoded mappings in the class

### Data Flow

1. **CSV Reading** (`asqp-reader/reader/CsvFlightRecordReader.java`):
   - Parses pipe-delimited CSV files
   - Validates required fields and formats
   - Creates ASQPFlightRecord objects
   - Logs validation errors but continues processing valid records

2. **Data Loading** (`asqp-reader/service/FlightDataLoader.java`):
   - Loads multiple CSV files in parallel using virtual threads
   - Validates data quality (missing carriers, airports, invalid times)
   - Returns list of ASQPFlightRecord objects

3. **Indexing** (`asqp-reader/index/FlightDataIndex.java`):
   - Pre-computes indices for efficient queries by carrier, airport, tail number, flight number, route
   - Caches airport and carrier information
   - Groups records for fast lookups

4. **Route Graph** (`flight-core/service/RouteGraphService.java`):
   - Builds JGraphT weighted graph from RouteIndex
   - Nodes = airports, Edges = routes, Weights = great circle distances
   - Uses Dijkstra's algorithm for shortest path queries
   - **NOTE**: Recently moved from asqp-reader to flight-core (see CHANGELOG.md)

5. **Views** (`asqp-reader/view/*`):
   - MVC-style separation: Controller → View → User
   - Each view queries the index and formats output
   - MenuController orchestrates the interactive menu system

### Distance Calculations

The **DistanceCalculator** (`flight-core/util/DistanceCalculator.java`) uses the Haversine formula to calculate great circle distances between airports. It requires AirportCodeMapper to get coordinates and returns distances in miles.

## CSV Data Format

The application expects pipe-delimited CSV files with these fields:
- `carrier_code|flight_number|origin|destination|departure_date|scheduled_oag_departure|scheduled_crs_departure|gate_departure|scheduled_arrival|scheduled_crs_arrival|gate_arrival|wheels_up|wheels_down|tail_number|cancellation_code`

**Key format requirements:**
- Dates: YYYYMMDD format (e.g., 20250127)
- Times: HHMM or HMM format (e.g., 1700 or 600)
- Cancelled flights: actual times (gate_departure, gate_arrival, wheels_up, wheels_down) should be 0 or empty
- Airport codes: Exactly 3 characters

## Data Preprocessing

For raw BTS ASQP data (234 columns), use the Python grooming script to extract only necessary fields:
```bash
python src/main/scripts/asqp_bulk_data_groomer.py ./raw_data ./processed_data
```
This reduces file sizes by 60-70%. See docs/DATA_GROOMER.md for details.

## Testing Practices

- Use JUnit 5 (Jupiter) for all tests
- Test files mirror the structure: `src/test/java/com/lamontd/travel/flight/...`
- Mappers use resource files: `src/test/resources/data/` for test data
- Use `@BeforeAll` to load shared resources like mappers
- Test both happy paths and error conditions
- Validate edge cases like cancelled flights, missing data, red-eye flights

## Code Conventions

- **Java 23**: Use modern Java features (records, sealed classes, pattern matching when appropriate)
- **Immutability**: All models use Builder pattern and are immutable
- **Optional**: Use `Optional<T>` for nullable return values
- **Time handling**: 
  - Use `LocalDate` for dates
  - Use `LocalTime` for local times
  - Use `Instant` for UTC timestamps
- **Validation**: Validate in constructors/builders with `Objects.requireNonNull()`
- **Resource loading**: Use try-with-resources for file/stream handling
- **Logging**: Use SLF4J for all logging (no System.out/err except in views for user interaction)

## Logging and Performance Instrumentation

### Logging Framework

This project uses **SLF4J** with **Logback** for structured logging.

**Guidelines:**
- Use SLF4J for all logging (no System.out/err except in views for user interaction)
- Log performance metrics for startup operations
- Use appropriate log levels: ERROR (failures), WARN (recoverable issues), INFO (key events), DEBUG (detailed flow)

### Logger Declaration

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

private static final Logger logger = LoggerFactory.getLogger(YourClass.class);
```

### Performance Timing

Use `PerformanceTimer` for critical operations:

```java
try (var timer = new PerformanceTimer("Operation description")) {
    // your operation
} // Automatically logs duration on close
```

**Use for:**
- File loading (FlightDataLoader)
- Index building (FlightDataIndex)
- Mapper initialization (CarrierCodeMapper, etc.)
- Route graph construction (RouteGraphService)

### Log Levels

- **ERROR**: Operation failures that prevent functionality
- **WARN**: Recoverable issues (missing reference data, skipped records)
- **INFO**: Key events (files loaded, indices built, performance metrics)
- **DEBUG**: Detailed flow, individual index sizes

### Configuration

Logging configured in `asqp-reader/src/main/resources/logback.xml`:
- Console output: Brief format for user feedback
- File output: `flight-analysis-performance.log` with detailed timestamps
- Performance metrics always logged to file for analysis

## Common Development Scenarios

### Adding a new mapper
1. Create model class in `flight-core/src/main/java/com/lamontd/travel/flight/model/`
2. Create mapper in `flight-core/src/main/java/com/lamontd/travel/flight/mapper/`
3. Add static `getDefault()` factory method that loads from resources
4. Place data file in `flight-core/src/main/resources/data/`
5. Write tests in `flight-core/src/test/java/.../mapper/`
6. Rebuild flight-core: `cd flight-core && mvn clean install`

### Adding a new view to asqp-reader
1. Create view class in `asqp-reader/src/main/java/.../view/`
2. Implement query logic using FlightDataIndex
3. Format output using ViewUtils or ViewRenderer
4. Add menu option in MenuController
5. Test with sample data: `mvn clean package && java -jar asqp-reader/target/asqp-reader.jar`

### Modifying FlightRecord or ScheduledFlight models
1. Update model in `flight-core/src/main/java/.../model/`
2. Update FlightConverter in `asqp-reader/src/main/java/.../FlightConverter.java`
3. Rebuild both modules: `mvn clean install` from root
4. Run all tests to ensure conversions work correctly

### Working with route graphs
1. RouteGraphService is in `flight-core/src/main/java/.../service/` (moved from asqp-reader)
2. Requires RouteIndex as input (which contains airport-to-airport distances)
3. Uses JGraphT SimpleWeightedGraph with DefaultWeightedEdge
4. Graph is built once on construction, then queried via Dijkstra
5. Example usage in `asqp-reader/src/main/java/.../view/RouteAnalysisView.java`

## Important Notes

- The project uses Java 23 features, so ensure JDK 23 is installed
- Always rebuild flight-core before asqp-reader if core models change
- The asqp-reader JAR is a shaded/uber JAR containing all dependencies (Maven Shade Plugin)
- Reference data files (airlines.dat, airports.dat, countries.json) are embedded in JARs
- Red-eye flights (arrival before departure) are normal and should not be flagged as errors
- Validation exceptions are logged but don't stop CSV processing
