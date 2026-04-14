# Developer Guide

Complete technical reference for integrating with the ASQP Reader.

## Table of Contents

1. [Carrier Code Mapping](#carrier-code-mapping)
2. [Airport Data](#airport-data)
3. [Country Codes](#country-codes)
4. [Data Quality Validation](#data-quality-validation)

---

## Carrier Code Mapping

### CarrierInfo Model

```java
public class CarrierInfo {
    private String code;            // IATA code (2-letter) - "DL"
    private String name;            // Airline name - "Delta Air Lines"
    private String fullName;        // Same as name (backward compatibility)
    private String icao;            // ICAO code (3-letter) - "DAL"
    private String callsign;        // Callsign - "DELTA"
    private String country;         // Country - "United States"
    private boolean active;         // Active status - true
}
```

### Basic Usage

```java
// Load default mapper (singleton)
CarrierCodeMapper mapper = CarrierCodeMapper.getDefault();

// Get carrier name
String name = mapper.getCarrierName("DL");  // "Delta Air Lines"

// Get full name
String fullName = mapper.getCarrierFullName("DL");  // "Delta Air Lines"

// Check if exists
boolean exists = mapper.hasCarrier("DL");  // true

// Get detailed info
Optional<CarrierInfo> info = mapper.getCarrierInfo("DL");
info.ifPresent(carrier -> {
    System.out.println("Code: " + carrier.getCode());              // DL
    System.out.println("Name: " + carrier.getName());              // Delta Air Lines
    System.out.println("ICAO: " + carrier.getIcao().orElse("N/A")); // DAL
    System.out.println("Callsign: " + carrier.getCallsign().orElse("N/A")); // DELTA
    System.out.println("Country: " + carrier.getCountry().orElse("N/A")); // United States
});
```

### Advanced Usage

```java
// Load from custom file
CarrierCodeMapper mapper = CarrierCodeMapper.fromFile(
    Paths.get("/path/to/carriers.csv")
);

// Build programmatically
CarrierCodeMapper mapper = new CarrierCodeMapper();
mapper.addCarrier("DL", "Delta", "Delta Air Lines Inc.");
mapper.addCarrier("AA", "American", "American Airlines Inc.");

// Get all carriers
Set<String> codes = mapper.getAllCodes();
Collection<CarrierInfo> carriers = mapper.getAllCarriers();

// Query
System.out.println("Total carriers: " + mapper.size());
```

### Integration with Flight Records

```java
CsvFlightRecordReader reader = new CsvFlightRecordReader();
CarrierCodeMapper mapper = CarrierCodeMapper.getDefault();

List<FlightRecord> records = reader.readFromFile(path);

// Generate reports with carrier names
Map<String, Long> stats = records.stream()
    .collect(Collectors.groupingBy(
        FlightRecord::getCarrierCode,
        Collectors.counting()
    ));

stats.forEach((code, count) -> {
    String carrierName = mapper.getCarrierName(code);
    System.out.println(code + " (" + carrierName + "): " + count + " flights");
});
```

---

## Airport Data

### AirportInfo Model

```java
public class AirportInfo {
    private String code;          // IATA code (3-letter) - "ATL"
    private String name;          // Airport name
    private String city;          // City served - "Atlanta"
    private String country;       // Country - "United States"
    private String icao;          // ICAO code (4-letter) - "KATL"
    private Double latitude;      // Decimal degrees
    private Double longitude;     // Decimal degrees
    private Integer altitude;     // Feet above sea level
    private Double timezone;      // Hours offset from UTC
    private String dst;           // DST: E/A/S/O/Z/N/U
    private String tzDatabase;    // Timezone (tz format)
    private String type;          // airport, station, etc.
}
```

### Basic Queries

```java
AirportCodeMapper mapper = AirportCodeMapper.getDefault();

// Get airport info
Optional<AirportInfo> info = mapper.getAirportInfo("ATL");

// Get airport name
String name = mapper.getAirportName("ATL");
// "Hartsfield Jackson Atlanta International Airport"

// Get city
String city = mapper.getAirportCity("ATL");  // "Atlanta"

// Get display names
String shortName = mapper.getShortDisplayName("ATL");
// "Atlanta (ATL)"

String fullName = mapper.getFullDisplayName("ATL");
// "Hartsfield Jackson Atlanta International Airport (Atlanta, United States)"

// Check existence
boolean exists = mapper.hasAirport("ATL");  // true

// Get count
int count = mapper.size();  // 6033
```

### Advanced Queries

```java
// Find airports by country
List<AirportInfo> usAirports = mapper.getAirportsByCountry("United States");
// Returns 500+ US airports

// Find airports by city
List<AirportInfo> nyAirports = mapper.getAirportsByCity("New York");
// Returns [JFK, LGA, ...]

// Search airports by name
List<AirportInfo> results = mapper.searchByName("International");
// Returns all airports with "International" in name or city

// Get all codes
Set<String> codes = mapper.getAllCodes();

// Get all airports
Collection<AirportInfo> all = mapper.getAllAirports();
```

### Enriching Flight Records

```java
CsvFlightRecordReader reader = new CsvFlightRecordReader();
AirportCodeMapper airportMapper = AirportCodeMapper.getDefault();
CarrierCodeMapper carrierMapper = CarrierCodeMapper.getDefault();

List<FlightRecord> records = reader.readFromFile(path);

records.forEach(flight -> {
    String carrier = carrierMapper.getCarrierName(flight.getCarrierCode());
    String origin = airportMapper.getAirportCity(flight.getOrigin());
    String dest = airportMapper.getAirportCity(flight.getDestination());
    
    System.out.printf("%s%s: %s (%s) -> %s (%s)%n",
        flight.getCarrierCode(),
        flight.getFlightNumber(),
        flight.getOrigin(),
        origin,
        flight.getDestination(),
        dest
    );
});
```

---

## Country Codes

### CountryInfo Model

```java
public class CountryInfo {
    private int id;              // Numeric country code
    private String alpha2;       // 2-letter code (ISO 3166-1 alpha-2)
    private String alpha3;       // 3-letter code (ISO 3166-1 alpha-3)
    private String name;         // Country name
}
```

### Basic Queries

```java
CountryCodeMapper mapper = CountryCodeMapper.getDefault();

// Get by alpha-2 code (case-insensitive)
Optional<CountryInfo> us = mapper.getByAlpha2("US");
Optional<CountryInfo> us2 = mapper.getByAlpha2("us");  // Same result

// Get by alpha-3 code (case-insensitive)
Optional<CountryInfo> us3 = mapper.getByAlpha3("USA");

// Get by name (case-insensitive, exact match)
Optional<CountryInfo> us4 = mapper.getByName("United States of America");

// Get by numeric ID
Optional<CountryInfo> us5 = mapper.getById(840);

// Get country name (with fallback)
String name = mapper.getCountryName("US");
// "United States of America"

// Check existence
boolean exists = mapper.hasCountry("US");     // true (checks both alpha-2 and alpha-3)
boolean hasAlpha2 = mapper.hasAlpha2("US");   // true
boolean hasAlpha3 = mapper.hasAlpha3("USA");  // true
```

### Code Conversions

```java
// Convert alpha-2 to alpha-3
Optional<String> alpha3 = mapper.alpha2ToAlpha3("US");
// Returns "USA"

// Convert alpha-3 to alpha-2
Optional<String> alpha2 = mapper.alpha3ToAlpha2("USA");
// Returns "US"

// Chain conversions
String result = mapper.alpha2ToAlpha3("GB")
                     .flatMap(mapper::alpha3ToAlpha2)
                     .orElse("Unknown");
// Returns "GB"
```

### Search

```java
// Search by name (partial match, case-insensitive)
List<CountryInfo> results = mapper.searchByName("United");
// Returns: United States of America, United Kingdom, United Arab Emirates

// Get all countries
Collection<CountryInfo> all = mapper.getAllCountries();

// Get all codes
Set<String> alpha2Codes = mapper.getAllAlpha2Codes();  // [US, CA, GB, ...]
Set<String> alpha3Codes = mapper.getAllAlpha3Codes();  // [USA, CAN, GBR, ...]
```

### Integration with Airport and Carrier Data

```java
AirportCodeMapper airportMapper = AirportCodeMapper.getDefault();
CarrierCodeMapper carrierMapper = CarrierCodeMapper.getDefault();
CountryCodeMapper countryMapper = CountryCodeMapper.getDefault();

// Find ISO code for an airport's country
AirportInfo airport = airportMapper.getAirportInfo("ATL").get();
String countryName = airport.getCountry().get();  // "United States"

CountryInfo country = countryMapper.getByName(countryName).orElse(null);
if (country != null) {
    System.out.println("Airport: " + airport.getCode());
    System.out.println("Country: " + country.getAlpha2Upper());  // "US"
    System.out.println("ISO 3: " + country.getAlpha3Upper());    // "USA"
}
```

---

## Data Quality Validation

The application automatically performs quality checks on loaded data.

### Validation Checks

#### 1. Missing Carrier Codes

Verifies that all carrier codes in flight data exist in the carrier database.

**Example Output:**
```
✓ All carrier codes found in database
```

Or if issues:
```
WARNING: Found 2 unknown carrier code(s):
  - XX (not in carrier database)
  - YY (not in carrier database)
```

#### 2. Missing Airport Codes

Verifies that all airport codes (origin and destination) exist in the airport database.

**Example Output:**
```
✓ All airport codes found in database
```

Or if issues:
```
WARNING: Found 3 unknown airport code(s):
  - ABC (not in airport database)
  - DEF (not in airport database)
  - GHI (not in airport database)
```

#### 3. Invalid Flight Times

Detects flights where gate arrival time is before gate departure time.

**Example Output:**
```
✓ All flight times appear valid
```

Or if issues:
```
WARNING: Found 2 flight(s) with arrival before departure:
  - DL5035 on 2025-01-11: Departed 23:33, Arrived 01:31
  - DL5036 on 2025-01-09: Departed 23:17, Arrived 01:10
  (Note: These may be flights crossing midnight)
```

**Interpretation:**
- **Red-eye flights:** Departing late at night and arriving early next morning (crossing midnight) is normal
- **Data errors:** Daytime flights showing arrival before departure likely indicate data issues

#### 4. Wheels Time Validation

Detects flights where wheels down time is before wheels up time.

### Programmatic Validation

```java
List<FlightRecord> records = reader.readFromFile(path);

// Check for missing carriers
Set<String> unknownCarriers = records.stream()
    .map(FlightRecord::getCarrierCode)
    .filter(code -> !carrierMapper.hasCarrier(code))
    .collect(Collectors.toSet());

if (!unknownCarriers.isEmpty()) {
    System.out.println("Unknown carriers: " + unknownCarriers);
}

// Check for invalid times (excluding likely red-eyes)
List<FlightRecord> suspiciousFlights = records.stream()
    .filter(r -> !r.isCancelled())
    .filter(r -> r.getGateDeparture().isPresent() && 
                 r.getGateArrival().isPresent())
    .filter(r -> {
        LocalTime dep = r.getGateDeparture().get();
        LocalTime arr = r.getGateArrival().get();
        boolean isLateEvening = dep.isAfter(LocalTime.of(20, 0));
        boolean isEarlyMorning = arr.isBefore(LocalTime.of(6, 0));
        boolean crossesMidnight = arr.isBefore(dep);
        
        // Flag if crosses midnight but NOT a red-eye pattern
        return crossesMidnight && !(isLateEvening && isEarlyMorning);
    })
    .toList();
```

### Understanding Time Issues

**Red-eye flights (normal):**
- Departure: 20:00-23:59
- Arrival: 00:00-06:00
- These cross midnight naturally

**Data errors (investigate):**
- Departure: 08:00-19:59
- Arrival before departure in same window
- Likely data quality issues

---

## Performance Notes

### Carrier Mapper
- **Load time:** ~50ms for 992 carriers
- **Memory:** ~200KB
- **Lookup:** O(1) HashMap lookup
- **Singleton:** Use `getDefault()` for shared instance

### Airport Mapper
- **Load time:** ~200ms for 6,033 airports
- **Memory:** ~2MB
- **Lookup:** O(1) HashMap lookup
- **Filtering:** Efficient stream operations

### Country Mapper
- **Load time:** ~20ms for 193 countries
- **Memory:** ~50KB
- **Lookup:** O(1) HashMap lookup
- **Format:** JSON parsed with Gson

### Validation
- **Impact:** ~10-50ms for 500 records
- **Memory:** O(n) where n = number of records
- **Non-blocking:** Processing continues with warnings
