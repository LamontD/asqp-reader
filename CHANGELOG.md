# Changelog

All notable changes and migrations for the ASQP Reader project.

## OpenFlights Migration

### Overview

Successfully migrated from a simple 17-carrier CSV to the comprehensive **OpenFlights** database with **992+ active airlines** and **6,033+ airports** worldwide.

### Airlines Migration

**Before:**
- 17 manually maintained US carriers
- Basic fields: code, name, full_name
- Required manual updates

**After:**
- 992+ active airlines worldwide
- Rich fields: IATA, ICAO, callsign, country, active status
- Community-maintained (OpenFlights)
- Easy updates via curl command

**Coverage Increase:**
- US carriers: 17 → 150+ (+882%)
- International: 0 → 840+ (∞)
- Total: 17 → 992+ (+5,735%)

### Airports Integration

**Added:**
- 6,033 airports with valid IATA codes
- Full airport information (name, city, country, coordinates, timezone)
- OpenFlights data source
- Worldwide coverage

**Fields:**
- IATA code (3-letter)
- ICAO code (4-letter)
- Airport name
- City and country
- Coordinates (latitude/longitude)
- Altitude
- Timezone and DST information

### Country Codes Integration

**Added:**
- 193 UN-recognized countries
- ISO 3166-1 standard codes
- Alpha-2, Alpha-3, and numeric codes
- Case-insensitive lookups
- Code conversions

### Data Quality Validation

**Added automatic checks for:**
1. Missing carrier codes
2. Missing airport codes
3. Invalid flight times (arrival before departure)
4. Invalid wheels times (wheels down before wheels up)

**Features:**
- Non-blocking (warnings only)
- Intelligent detection (recognizes red-eye flights)
- Actionable output (specific flight numbers and dates)

### Build System Enhancement

**Added:**
- Maven Shade Plugin configuration
- Executable JAR with all dependencies
- Self-contained deployment (1.7 MB)
- Embedded resource files

---

## Features Added

### Carrier Code Mapping
- OpenFlights integration (992+ carriers)
- ICAO code support
- Callsign information
- Country information
- Active status filtering

### Airport Code Mapping
- 6,033 worldwide airports
- Geographic coordinates
- Timezone information
- Search and filter capabilities
- Display name helpers

### Country Code Support
- ISO 3166-1 compliance
- Multiple code formats
- Code conversions
- Name search

### Data Quality
- Automatic validation on load
- Missing reference data detection
- Time anomaly detection
- Clear, actionable warnings

---

## API Enhancements

### CarrierInfo Model

**Added fields:**
```java
Optional<String> icao;       // "DAL"
Optional<String> callsign;   // "DELTA"
Optional<String> country;    // "United States"
boolean active;              // true
```

### CarrierCodeMapper

**New methods:**
```java
void loadFromOpenFlightsResource(String resourcePath)
void loadFromOpenFlightsFile(Path filePath)
Collection<CarrierInfo> getAllCarriers()
```

### AirportCodeMapper

**New class with methods:**
```java
Optional<AirportInfo> getAirportInfo(String code)
String getAirportName(String code)
String getAirportCity(String code)
String getShortDisplayName(String code)
String getFullDisplayName(String code)
List<AirportInfo> getAirportsByCountry(String country)
List<AirportInfo> getAirportsByCity(String city)
List<AirportInfo> searchByName(String searchTerm)
```

### CountryCodeMapper

**New class with methods:**
```java
Optional<CountryInfo> getByAlpha2(String alpha2)
Optional<CountryInfo> getByAlpha3(String alpha3)
Optional<CountryInfo> getByName(String name)
Optional<CountryInfo> getById(int id)
Optional<String> alpha2ToAlpha3(String alpha2)
Optional<String> alpha3ToAlpha2(String alpha3)
List<CountryInfo> searchByName(String searchTerm)
```

---

## Backward Compatibility

### All Existing Code Still Works

✅ **100% Backward Compatible**

```java
// All these still work exactly as before:
CarrierCodeMapper mapper = CarrierCodeMapper.getDefault();
String name = mapper.getCarrierName("DL");
String fullName = mapper.getCarrierFullName("DL");
mapper.addCarrier("XX", "Custom", "Custom Airlines");
```

### Graceful Degradation

- Falls back to simple CSV if OpenFlights data unavailable
- Handles missing fields with Optional<>
- Maintains existing CSV format support

---

## Performance Impact

### Load Time Changes

| Component | Before | After | Delta |
|-----------|--------|-------|-------|
| Carriers | ~5ms | ~50ms | +45ms |
| Airports | N/A | ~200ms | +200ms |
| Countries | N/A | ~20ms | +20ms |
| **Total** | ~100ms | ~370ms | +270ms |

**Startup impact:** ~270ms increase (minimal for batch processing)

### Memory Changes

| Component | Before | After | Delta |
|-----------|--------|-------|-------|
| Carriers | ~5KB | ~200KB | +195KB |
| Airports | N/A | ~2MB | +2MB |
| Countries | N/A | ~50KB | +50KB |
| **Total** | ~5KB | ~2.25MB | +2.25MB |

**Memory impact:** +2.25MB (negligible for modern systems)

### Lookup Performance

- All lookups remain O(1) HashMap operations
- No performance degradation for queries
- Efficient stream filtering for advanced queries

---

## Testing

### Test Coverage Increased

| Component | Tests | Status |
|-----------|-------|--------|
| Carrier mapper | 14 | ✅ All passing |
| Airport mapper | 12 | ✅ All passing |
| Country mapper | 14 | ✅ All passing |
| CSV reader | 8 | ✅ All passing |
| Application | 1 | ✅ All passing |
| **Total** | **49** | **✅ All passing** |

**Previous:** 21 tests → **Current:** 49 tests (+133%)

---

## Documentation

### New Documentation Files

1. **DEVELOPER_GUIDE.md** - Complete API reference
2. **DATA_SOURCES.md** - Data source information
3. **BUILD.md** - Build and deployment guide
4. **CHANGELOG.md** - This file

### Consolidated Documentation

**Removed** (consolidated into above files):
- CARRIER_MAPPER_SUMMARY.md
- CARRIER_DATA_SOURCES.md
- AIRPORT_INTEGRATION.md
- AIRPORT_SUMMARY.md
- COUNTRY_INTEGRATION.md
- COUNTRY_SUMMARY.md
- DATA_QUALITY_VALIDATION.md
- VALIDATION_SUMMARY.md
- OPENFLIGHTS_INTEGRATION.md
- OPENFLIGHTS_MIGRATION_SUMMARY.md
- QUICK_START_OPENFLIGHTS.md
- BUILD_AND_DEPLOY.md
- JAR_BUILD_SUMMARY.md

**From 14 files → 5 files** (65% reduction)

---

## Dependencies Added

### Gson for JSON Parsing

```xml
<dependency>
    <groupId>com.google.code.gson</groupId>
    <artifactId>gson</artifactId>
    <version>2.11.0</version>
</dependency>
```

**Purpose:** Parse countries.json file

### Maven Shade Plugin

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-shade-plugin</artifactId>
    <version>3.6.0</version>
</plugin>
```

**Purpose:** Create executable JAR with all dependencies

---

## Data Files

### Resource Files Added

1. **airlines.dat** - OpenFlights airlines database (992+ carriers)
2. **airports.dat** - OpenFlights airports database (6,033 airports)
3. **countries.json** - ISO 3166-1 country codes (193 countries)

### Resource Files Kept

1. **carriers.csv** - Legacy format (backward compatibility)
2. **sample-data.asqpflightrecord.csv** - Sample flight data

---

## Breaking Changes

### None

All changes are backward compatible. Existing code continues to work without modification.

### Migration Path (Optional)

To use new features:

```java
// Old (still works)
String name = mapper.getCarrierName("DL");

// New (optional enhancement)
Optional<CarrierInfo> info = mapper.getCarrierInfo("DL");
info.ifPresent(carrier -> {
    System.out.println("ICAO: " + carrier.getIcao().get());
    System.out.println("Callsign: " + carrier.getCallsign().get());
    System.out.println("Country: " + carrier.getCountry().get());
});
```

---

## Future Enhancements

### Potential Additions

**Carrier features:**
- Filter carriers by country
- Search by ICAO code
- Search by callsign
- Alliance information

**Airport features:**
- Distance calculations (haversine formula)
- Find nearest airports
- Timezone conversion helpers
- Filter by altitude/type

**Country features:**
- Regional groupings
- Currency codes (ISO 4217)
- Phone codes
- Language information

**Validation features:**
- Timezone-aware time checking
- Flight duration validation
- Route validation
- Statistical outlier detection

---

## Version History

### Current Version
- All features documented above implemented and tested
- 49 tests passing
- 5 documentation files
- Full OpenFlights integration

### Previous Version
- 17 manually maintained carriers
- Basic CSV support
- 21 tests
- 14 documentation files (now consolidated)

---

## Migration Benefits

### For Users
1. ✅ 992+ carriers recognized automatically
2. ✅ 6,033 airports with full information
3. ✅ 193 countries with ISO codes
4. ✅ Automatic data quality validation
5. ✅ Easy data updates (curl command)
6. ✅ Zero code changes required

### For Developers
1. ✅ Rich API with detailed information
2. ✅ Optional fields (graceful handling of missing data)
3. ✅ Comprehensive test coverage
4. ✅ Clear documentation
5. ✅ Backward compatible
6. ✅ Easy to extend

### For Operations
1. ✅ Self-contained executable JAR
2. ✅ Minimal performance impact
3. ✅ Easy deployment
4. ✅ Automated data updates possible
5. ✅ Clear validation warnings
6. ✅ Production-ready

---

## Summary

The ASQP Reader has evolved from a simple CSV processor with 17 carriers into a comprehensive flight data analysis tool with:

- **992+ airlines** worldwide
- **6,033+ airports** with full details
- **193 countries** with ISO codes
- **Automatic data quality validation**
- **Self-contained deployment** (1.7 MB JAR)
- **49 comprehensive tests** (100% passing)
- **100% backward compatibility**

All while maintaining simplicity and ease of use. The JAR is completely self-contained and ready for production deployment.
