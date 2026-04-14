# ASQP Flight Record Reader

A Java application for reading and validating Airline Service Quality Performance (ASQP) flight records from CSV files.

## Features

- **CSV Parsing**: Reads pipe-delimited CSV files with flight data
- **Data Validation**: Validates required fields, airport codes, dates, and times
- **Data Quality Checks**: Automatic validation for missing carriers, missing airports, and invalid flight times
- **Carrier Code Mapping**: Maps 2-letter carrier codes to airline names (992+ airlines)
- **Airport Data Integration**: Maps 3-letter airport codes to detailed airport information (6,033+ airports)
- **Country Code Support**: ISO 3166-1 country codes with alpha-2, alpha-3, and name mapping (193 countries)
- **Cancellation Handling**: Properly handles cancelled flights (marked with cancellation codes)
- **Type Safety**: Uses Java 23 with modern `LocalDate` and `LocalTime` types
- **Error Reporting**: Clear validation error messages with record/field information
- **Comprehensive Testing**: Full unit test coverage with JUnit 5

## Project Structure

```
src/main/java/com/lamontd/asqp/
├── App.java                              # Main application
├── model/
│   ├── FlightRecord.java                 # Flight record model with Builder pattern
│   ├── CarrierInfo.java                  # Carrier information model
│   └── AirportInfo.java                  # Airport information model
├── mapper/
│   ├── CarrierCodeMapper.java            # Maps carrier codes to airline names
│   └── AirportCodeMapper.java            # Maps airport codes to airport details
├── reader/
│   └── CsvFlightRecordReader.java        # CSV parsing and validation
└── validation/
    └── FlightRecordValidationException.java  # Custom exception for validation errors

src/main/resources/data/
├── sample-data.asc.groomed               # Sample ASQP data file
├── airlines.dat                          # OpenFlights airline database (992+ carriers)
├── airports.dat                          # OpenFlights airport database (6,033+ airports)
└── countries.json                        # ISO 3166-1 country codes (193 countries)
```

## Data Format

The CSV file should be pipe-delimited (`|`) with the following fields:

| Field | Type | Description |
|-------|------|-------------|
| carrier_code | String | Airline carrier code (e.g., "DL") |
| flight_number | String | Flight number |
| origin | String | Origin airport code (3 letters) |
| destination | String | Destination airport code (3 letters) |
| departure_date | Date | Departure date (YYYYMMDD format) |
| scheduled_oag_departure | Time | Scheduled OAG departure (HHMM format) |
| scheduled_crs_departure | Time | Scheduled CRS departure (HHMM format) |
| gate_departure | Time | Actual gate departure (HHMM or 0 if cancelled) |
| scheduled_arrival | Time | Scheduled arrival (HHMM format) |
| scheduled_crs_arrival | Time | Scheduled CRS arrival (HHMM format) |
| gate_arrival | Time | Actual gate arrival (HHMM or 0 if cancelled) |
| wheels_up | Time | Wheels up time (HHMM or 0 if cancelled) |
| wheels_down | Time | Wheels down time (HHMM or 0 if cancelled) |
| tail_number | String | Aircraft tail number |
| cancellation_code | String | Cancellation code (e.g., "B") or empty |

### Example Records

**Operated Flight:**
```
DL|5030|CVG|LGA|20250127|1700|1700|1658|1859|1859|1902|1716|1853|N917XJ|
```

**Cancelled Flight:**
```
DL|5030|LGA|CVG|20250105|1335|1335|0|1558|1558|0|0|0|N186GJ|B
```

## Building

### Build Executable JAR

```bash
mvn clean package
```

This creates an executable JAR with all dependencies at `target/asqp-reader.jar` (~1.7 MB).

### Skip Tests (Faster Build)

```bash
mvn clean package -DskipTests
```

See [docs/BUILD.md](docs/BUILD.md) for complete build and deployment instructions.

## Running Tests

```bash
mvn test
```

## Running the JAR

Once built, you can run the application using the JAR file:

```bash
# Process a specific CSV file
java -jar target/asqp-reader.jar /path/to/data.csv

# Run with no arguments to process sample data
java -jar target/asqp-reader.jar
```

The JAR is completely self-contained and includes all dependencies and resource files.

Test results from sample data:
- **Total records**: 500
- **Operated flights**: 478
- **Cancelled flights**: 22
- **Carriers loaded**: 992
- **Airports loaded**: 6,033
- **Countries loaded**: 193

## Usage

### Running with sample data:
```bash
java -jar target/asqp-reader-1.0-SNAPSHOT.jar
```

### Running with custom file:
```bash
java -jar target/asqp-reader-1.0-SNAPSHOT.jar /path/to/your/data.csv
```

### Using as a library:
```java
import com.lamontd.asqp.reader.CsvFlightRecordReader;
import com.lamontd.asqp.mapper.CarrierCodeMapper;
import com.lamontd.asqp.mapper.AirportCodeMapper;
import com.lamontd.asqp.model.FlightRecord;

// Load flight records
CsvFlightRecordReader reader = new CsvFlightRecordReader();
List<FlightRecord> records = reader.readFromFile(Paths.get("data.csv"));

// Load mappers
CarrierCodeMapper carrierMapper = CarrierCodeMapper.getDefault();
AirportCodeMapper airportMapper = AirportCodeMapper.getDefault();

// Process records
for (FlightRecord record : records) {
    String carrierName = carrierMapper.getCarrierName(record.getCarrierCode());
    String originCity = airportMapper.getAirportCity(record.getOrigin());
    String destCity = airportMapper.getAirportCity(record.getDestination());
    
    System.out.println("Flight: " + record.getCarrierCode() + record.getFlightNumber() + 
                       " (" + carrierName + ")");
    System.out.println("Route: " + record.getOrigin() + " (" + originCity + ") -> " + 
                                     record.getDestination() + " (" + destCity + ")");
    System.out.println("Cancelled: " + record.isCancelled());
}
```

## Carrier Code Mapping

The `CarrierCodeMapper` provides translation between 2-letter airline codes (e.g., "DL") and carrier names (e.g., "Delta Air Lines"). The system now uses **OpenFlights** airline database with **992+ active airlines** worldwide.

### OpenFlights Integration

- **Data Source:** OpenFlights community-maintained airline database
- **Coverage:** 992+ active airlines from around the world
- **Rich Data:** IATA/ICAO codes, callsigns, country, active status
- **Auto-updated:** Download latest from [OpenFlights GitHub](https://github.com/jpatokal/openflights)

See [docs/DATA_SOURCES.md](docs/DATA_SOURCES.md) for detailed information.

### Loading Carrier Data

```java
// Option 1: Use default mapper (loads from resources/data/carriers.csv)
CarrierCodeMapper mapper = CarrierCodeMapper.getDefault();

// Option 2: Load from a custom file
CarrierCodeMapper mapper = CarrierCodeMapper.fromFile(Paths.get("my-carriers.csv"));

// Option 3: Load from a resource
CarrierCodeMapper mapper = CarrierCodeMapper.fromResource("/data/custom-carriers.csv");

// Option 4: Build programmatically
CarrierCodeMapper mapper = new CarrierCodeMapper();
mapper.addCarrier("DL", "Delta", "Delta Air Lines Inc.");
mapper.addCarrier("AA", "American", "American Airlines Inc.");
```

### Using the Mapper

```java
// Get carrier name
String name = mapper.getCarrierName("DL");  // "Delta Air Lines"

// Get full carrier name (same as name)
String fullName = mapper.getCarrierFullName("DL");  // "Delta Air Lines"

// Get detailed info with OpenFlights data
Optional<CarrierInfo> info = mapper.getCarrierInfo("DL");
info.ifPresent(carrier -> {
    System.out.println("Code: " + carrier.getCode());        // "DL"
    System.out.println("Name: " + carrier.getName());        // "Delta Air Lines"
    System.out.println("ICAO: " + carrier.getIcao().get());  // "DAL"
    System.out.println("Callsign: " + carrier.getCallsign().get());  // "DELTA"
    System.out.println("Country: " + carrier.getCountry().get());    // "United States"
    System.out.println("Active: " + carrier.isActive());     // true
});

// Check if carrier exists
boolean exists = mapper.hasCarrier("DL");

// Get all carriers
Set<String> codes = mapper.getAllCodes();  // 992+ IATA codes
Collection<CarrierInfo> carriers = mapper.getAllCarriers();
```

### Updating Carrier Data

The default carrier data is now loaded from [src/main/resources/data/airlines.dat](src/main/resources/data/airlines.dat) - the OpenFlights airline database with 992+ active carriers.

**To update to the latest OpenFlights data:**
```bash
curl -o src/main/resources/data/airlines.dat \
  https://raw.githubusercontent.com/jpatokal/openflights/master/data/airlines.dat
```

**Alternative data sources:**
- **OpenFlights** - Current default, 992+ airlines worldwide ⭐ **IN USE**
- **Bureau of Transportation Statistics (BTS)** - Official US carrier data
- **IATA** - International airline codes (requires subscription)

See [docs/DATA_SOURCES.md](docs/DATA_SOURCES.md) for more information.

## Airport Code Mapping

The `AirportCodeMapper` provides detailed information about airports worldwide from the **OpenFlights** database with **6,033+ airports**.

### Using the Airport Mapper

```java
// Load default mapper
AirportCodeMapper mapper = AirportCodeMapper.getDefault();

// Get airport name
String name = mapper.getAirportName("ATL");
// "Hartsfield Jackson Atlanta International Airport"

// Get city
String city = mapper.getAirportCity("ATL");  // "Atlanta"

// Get display names
String shortName = mapper.getShortDisplayName("ATL");  // "Atlanta (ATL)"
String fullName = mapper.getFullDisplayName("ATL");
// "Hartsfield Jackson Atlanta International Airport (Atlanta, United States)"

// Get detailed info
Optional<AirportInfo> info = mapper.getAirportInfo("ATL");
info.ifPresent(airport -> {
    System.out.println("ICAO: " + airport.getIcao().get());         // "KATL"
    System.out.println("Lat: " + airport.getLatitude().get());       // 33.6367
    System.out.println("Lon: " + airport.getLongitude().get());      // -84.428101
    System.out.println("Altitude: " + airport.getAltitude().get());  // 1026 feet
    System.out.println("Timezone: " + airport.getTimezone().get());  // -5.0 (UTC-5)
});

// Find airports
List<AirportInfo> usAirports = mapper.getAirportsByCountry("United States");
List<AirportInfo> nyAirports = mapper.getAirportsByCity("New York");  // [JFK, LGA]
List<AirportInfo> results = mapper.searchByName("International");
```

See [docs/DEVELOPER_GUIDE.md](docs/DEVELOPER_GUIDE.md) for complete documentation.

## Country Code Mapping

The `CountryCodeMapper` provides ISO 3166-1 country code support with **193 countries**.

### Using the Country Mapper

```java
// Load default mapper
CountryCodeMapper mapper = CountryCodeMapper.getDefault();

// Get by alpha-2 code (case-insensitive)
Optional<CountryInfo> us = mapper.getByAlpha2("US");

// Get by alpha-3 code
Optional<CountryInfo> us2 = mapper.getByAlpha3("USA");

// Get by name
Optional<CountryInfo> us3 = mapper.getByName("United States of America");

// Get country name
String name = mapper.getCountryName("US");  // "United States of America"

// Convert between codes
String alpha3 = mapper.alpha2ToAlpha3("US").get();   // "USA"
String alpha2 = mapper.alpha3ToAlpha2("USA").get();  // "US"

// Search by name
List<CountryInfo> results = mapper.searchByName("United");
// Returns: United States, United Kingdom, United Arab Emirates

// Detailed info
CountryInfo country = mapper.getByAlpha2("US").get();
System.out.println("ID: " + country.getId());            // 840
System.out.println("Alpha-2: " + country.getAlpha2Upper());  // "US"
System.out.println("Alpha-3: " + country.getAlpha3Upper());  // "USA"
System.out.println("Name: " + country.getName());            // "United States of America"
```

See [docs/DEVELOPER_GUIDE.md](docs/DEVELOPER_GUIDE.md) for complete documentation.

## Data Quality Checks

The application automatically performs quality checks on loaded data:

### Automated Checks
- **Missing Carriers**: Identifies carrier codes not in the database
- **Missing Airports**: Identifies airport codes not in the database  
- **Invalid Times**: Detects flights with arrival before departure (may indicate red-eye flights crossing midnight)
- **Wheels Time Issues**: Detects inconsistent wheels up/down times

### Example Output
```
=== Data Quality Checks ===
✓ All carrier codes found in database
✓ All airport codes found in database
WARNING: Found 2 flight(s) with arrival before departure:
  - DL5035 on 2025-01-11: Departed 23:33, Arrived 01:31
  - DL5036 on 2025-01-09: Departed 23:17, Arrived 01:10
  (Note: These may be flights crossing midnight)
```

See [docs/DEVELOPER_GUIDE.md](docs/DEVELOPER_GUIDE.md) for complete documentation on validation checks.

## Validation Rules

The reader validates:
- **Required fields**: carrier_code, flight_number, origin, destination, departure_date, scheduled times, tail_number
- **Airport codes**: Must be exactly 3 characters
- **Date format**: YYYYMMDD (e.g., 20250127)
- **Time format**: HHMM or HMM (e.g., 1700 or 600 for 6:00 AM)
- **Cancelled flights**: When cancellation_code is present, actual times (gate_departure, gate_arrival, wheels_up, wheels_down) should be 0 or empty

Invalid records are logged and skipped, allowing the reader to process the rest of the file.

## Dependencies

- **Apache Commons CSV 1.12.0**: CSV parsing
- **Google Gson 2.11.0**: JSON parsing
- **JUnit Jupiter 5.11.4**: Testing framework
- **Java 23**: Modern Java features and APIs

## Data Preprocessing

For processing raw BTS ASQP 234 data files, use the included Python script:

```bash
python src/main/scripts/asqp_bulk_data_groomer.py ./raw_data ./processed_data
```

This reduces file sizes by 60-70% by extracting only the necessary fields. See [docs/DATA_GROOMER.md](docs/DATA_GROOMER.md) for details.

## Documentation

- **[README.md](README.md)** - This file (getting started and overview)
- **[docs/DEVELOPER_GUIDE.md](docs/DEVELOPER_GUIDE.md)** - Complete API reference and integration guide
- **[docs/DATA_SOURCES.md](docs/DATA_SOURCES.md)** - Information about data sources (OpenFlights, BTS, ISO 3166-1)
- **[docs/DATA_GROOMER.md](docs/DATA_GROOMER.md)** - Data preprocessing script documentation
- **[docs/BUILD.md](docs/BUILD.md)** - Building, testing, and deployment instructions
- **[CHANGELOG.md](CHANGELOG.md)** - Version history and migration notes

## License

This project is part of the ASQP data analysis suite.
