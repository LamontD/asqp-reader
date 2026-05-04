# City-to-City Travel Planner

## Context

Enhance travel planner to allow users to search by city name instead of airport codes. System finds nearby airports and searches flights across multiple origin/destination combinations.

**Example**: "Columbia, MD" → searches BWI, IAD, DCA automatically

## Requirements (Confirmed)

1. **Search scope**: Top N closest airports that have available flights
2. **Display**: For round-trip, show top 5 outbound + top 5 return separately
3. **Round-trip**: Full city-to-city support
4. **Geography**: US cities only initially
5. **Ranking**: Factor in distance to airport when ranking flights

## Data Sources

- **City data**: [uscities.csv](flight-core/src/main/resources/data/uscities.csv) - 31,258 US cities with lat/lng
- **Airport data**: Existing [airports.dat](flight-core/src/main/resources/data/airports.dat) - 6,033 airports

## Implementation Plan

### Phase 1: Create City and Airport Lookup Services

**1.1 Create USCity Model** - `flight-core/model/USCity.java`
```java
public record USCity(
    String city,
    String stateId,
    String stateName,
    double latitude,
    double longitude,
    int population
) {}
```

**1.2 Create CityMapper** - `flight-core/mapper/CityMapper.java`
- Load uscities.csv into memory
- Index by "City, State" format (e.g., "Columbia, MD")
- Support both state codes and full names
- ~30K cities, small memory footprint

**1.3 Create CityAirportService** - `flight-core/service/CityAirportService.java`
- Find airports near a city
- Calculate distances using existing `DistanceCalculator`
- Return airports sorted by distance
- Configurable radius (default 100 miles)

### Phase 2: Airport Availability Filtering

**2.1 Add method to ScheduleFlightIndex** - `getAirportsWithFlights(LocalDate date)`
- Return Set<String> of airport codes that have flights on given date
- Use existing indices (byOrigin, byDestination)

**2.2 Filter in CityAirportService**
- Only return airports with available flights for the date
- Top N airports (configurable, default 5)

### Phase 3: Update TravelPlannerController

**3.1 Add City Input Mode**
- Prompt: "Search by (Airport/City):"
- If City: collect "City, State" format for origin and destination
- Parse and validate city names

**3.2 Create findFlightsFromCities() method**
```java
private void findFlightsFromCities(
    String originCity, 
    String destCity,
    LocalDate date,
    TimeOfDay timeOfDay,
    boolean isRoundTrip,
    LocalDate returnDate,
    TimeOfDay returnTimeOfDay)
```

**3.3 Ranking Algorithm**
For each flight option, calculate score:
```
score = flightQuality - (airportDistance * distancePenalty)

Where:
- flightQuality: base score (direct=100, 1-stop=80)
- airportDistance: miles from city to origin airport
- distancePenalty: configurable (default 0.5 points per mile)
```

Sort by score descending, return top 5 per leg.

**3.4 Display Format**
```
Origin Airports: BWI (12mi), IAD (28mi), DCA (32mi)
Destination Airports: JFK (0mi), LGA (8mi)

TOP 5 OUTBOUND FLIGHTS (Columbia, MD → New York, NY)
1. AA 100 - BWI→JFK (12mi to airport)
   Direct | Departs: 10:00 | Duration: 1h 15m

TOP 5 RETURN FLIGHTS (New York, NY → Columbia, MD)
1. DL 200 - LGA→DCA (8mi from, 32mi to destination)
   Direct | Departs: 14:00 | Duration: 1h 20m
```

### Phase 4: Testing

**4.1 Unit Tests**
- `CityMapperTest` - city lookup, state parsing
- `CityAirportServiceTest` - distance calculation, filtering
- `DistanceCalculatorTest` - city-to-airport distance

**4.2 Integration Tests**
- `TravelPlannerControllerTest` - city-to-city searches
- Test cases: single airport cities, multi-airport metros, no airports nearby

## Files to Create/Modify

**New Files (flight-core)**:
1. `model/USCity.java` - City data model
2. `mapper/CityMapper.java` - Load/index city data
3. `service/CityAirportService.java` - Find airports near city
4. Tests for above

**Modified Files**:
1. `flight-schedule-reader/.../TravelPlannerController.java` - Add city mode
2. `flight-schedule-reader/.../ScheduleFlightIndex.java` - Add getAirportsWithFlights()
3. Tests for above

## Configuration

Add to application config (or make configurable):
- Max airports per city: 5
- Max search radius: 100 miles
- Distance penalty: 0.5 points per mile
- Minimum airports: 1 (always include closest even if far)

## Verification

```bash
# Build and test
mvn clean install

# Manual test scenarios
1. Columbia, MD → New York, NY (multi-airport both sides)
2. Small city → Small city (1 airport each)
3. City with no nearby airports (error handling)
4. Round-trip city-to-city
```
