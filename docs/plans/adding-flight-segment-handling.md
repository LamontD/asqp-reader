---
name: Multi-Leg Flight Segment Analysis
description: Enhance FlightScheduleService to expose all bookable flight segments from multi-leg operations
type: feature
---

## Context

When analyzing ASQP flight data, some flights operate with multiple legs using the same flight number. For example, WN 4283 operates ORD → DAL → MCO (2 physical legs). From a passenger/booking perspective, this represents three distinct bookable flights:

1. **ORD to DAL** (first leg only)
2. **DAL to MCO** (second leg only)  
3. **ORD to MCO** (through connection with stop in DAL)

Currently, FlightScheduleService groups flights by route (origin-destination pairs) and only analyzes the "primary route" (most frequent). This approach misses the multi-leg structure and doesn't expose all bookable segments.

**User Requirements:**
- Enhance FlightScheduleService to detect and analyze all bookable segments
- Distinguish between direct legs and through connections
- For N-leg flights, expose all possible origin-destination combinations (not just individual legs)

## Test Data Available

**Sample Data (in repo):**

1. **sample-data.asqpflightrecord.csv**: ~500 Delta flights, all single-leg point-to-point operations. Regression testing to ensure single-leg flights show exactly 1 segment.

2. **multileg-examples.asqpflightrecord.csv**: Multi-leg test cases (UTF-16 encoded):
   - **WN 4283**: 2-leg operation on specific dates
     - 2025-01-01: ORD→DAL + DAL→MCO (2 legs, same day, same tail N480WN)
     - 2025-01-06: ORD→DAL + DAL→MCO (2 legs, same day, same tail N7727A)
     - Other dates: DAL→MCO only (single-leg)
     - **Expected on 2-leg days**: 3 segments (ORD→DAL direct, DAL→MCO direct, ORD→MCO through)
     - **Expected on 1-leg days**: 1 segment (DAL→MCO direct)
   
   - **WN 3310**: Operates on multiple different routes (not sequential legs)
     - ONT→HOU (most frequent route - ~25+ days)
     - PIT→MCO (alternate route - 2 days)
     - PHX→BNA (alternate route - 1 day)
     - **Expected**: System should detect multiple routes and either:
       1. Present separate schedule analysis for each route, OR
       2. Prompt user to select which route to analyze
     - **Note**: This tests multi-route detection (same flight number, different city pairs), not multi-leg sequential operations
   
   - **WN 5224**: Single-leg flights (OAK→LGB)
     - Control case for single-leg operations

**User's Full Dataset (runtime):**
Contains additional multi-leg examples including WN 5114 (reported as 3-leg operation).

## Proposed Solution

### 1. Create FlightSegment Model (flight-core)

Add a new `FlightSegment` model to represent a bookable flight segment:

**Location:** `flight-core/src/main/java/com/lamontd/travel/flight/model/FlightSegment.java`

```java
public class FlightSegment {
    private final String carrierCode;
    private final String flightNumber;
    private final String originAirport;
    private final String destinationAirport;
    private final SegmentType segmentType;
    private final List<String> intermediateStops; // Empty for direct legs
    private final int legCount;
    
    public enum SegmentType {
        DIRECT_LEG,      // Single physical leg (ORD-DAL)
        THROUGH_CONNECTION // Multiple legs, same flight number (ORD-MCO via DAL)
    }
}
```

### 2. Enhance FlightScheduleService (asqp-reader)

Modify `FlightScheduleService` to detect multi-leg operations and extract all bookable segments:

**Location:** `asqp-reader/src/main/java/com/lamontd/travel/flight/asqp/service/FlightScheduleService.java`

#### Key Changes:

**a) Add new method for route-specific analysis:**
```java
public FlightScheduleAnalysis analyzeFlightSchedule(String carrierCode, String flightNumber, 
                                                     String origin, String destination)
```
This allows analyzing a specific route when a flight operates on multiple routes.

**b) Add multi-route detection logic:**
- Group records by origin-destination pairs
- If multiple distinct routes exist, set `isMultiRoute = true` in result
- Include route frequency map for user selection

**c) Add multi-leg detection logic:**
- Group records by date to find daily flight operations
- For each date, sort legs by departure time to build the route chain
- Identify if a flight number operates multiple legs on the same day

**d) Add segment extraction method:**
```java
private List<FlightSegment> extractBookableSegments(List<ASQPFlightRecord> dailyLegs)
```
- Sort legs by scheduled departure time
- Build route chain (A→B→C)
- Generate all bookable combinations:
  - Direct legs: each individual leg
  - Through connections: all origin-to-destination pairs spanning multiple legs

**e) Enhance FlightScheduleAnalysis:**
- Add field: `List<FlightSegment> bookableSegments`
- Add field: `Map<String, FlightScheduleAnalysis> segmentAnalyses` 
  - Key: "ORIGIN-DEST", Value: Schedule analysis for that segment
- Modify analysis to compute stats per segment, not just primary route
- Add field: `boolean isMultiRoute` - indicates if this flight operates on different routes
- Add field: `String routePattern` - e.g., "ORD-DAL-MCO" for multi-leg, or "ONT-HOU" for single route

**f) Update analyzeFlightSchedule() flow:**

**Current behavior:** Returns analysis for the "primary route" (most frequent origin-destination pair)

**New behavior:** 

1. **Multi-route detection:** Group records by origin-destination to find all unique routes
2. **Decision point:**
   - If flight operates on multiple completely different routes (e.g., WN 3310: ONT→HOU vs PIT→MCO), return a result indicating "multiple schedules" and prompt user to specify which route
   - If flight operates on a single route pattern (could be multi-leg or single-leg), proceed with segment analysis

3. **For single route pattern:**
   - Group records by date
   - For each date, extract bookable segments from the legs
   - Generate all segments (direct legs + through connections)
   - Group segments across dates by origin-destination
   - Compute schedule analysis for each unique segment
   - Return enhanced analysis with all segments

**Alternative approach for multi-route flights:**
Instead of returning early, could return a `List<FlightScheduleAnalysis>` where each entry represents one route pattern. For WN 3310:
- Analysis 1: WN 3310 ONT→HOU (schedule, days, performance)
- Analysis 2: WN 3310 PIT→MCO (schedule, days, performance)
- Analysis 3: WN 3310 PHX→BNA (schedule, days, performance)

### 3. Update FlightView and FlightScheduleView (asqp-reader)

**FlightView** already handles multi-route display correctly in the Route Overview section (lines 83-124). This doesn't need changes for multi-route flights.

**FlightScheduleView** needs enhancement to handle both multi-route and multi-leg scenarios:

**Location:** `asqp-reader/src/main/java/com/lamontd/travel/flight/asqp/view/FlightScheduleView.java`

**Changes needed:**

1. **Multi-route detection:** After calling `analyzeFlightSchedule()`, check if flight operates on multiple distinct routes
2. **If multi-route flight (like WN 3310):**
   - Display message: "This flight operates on multiple routes. Please specify which route to analyze:"
   - List all unique routes with frequency:
     ```
     1. ONT → HOU (25 flights)
     2. PIT → MCO (2 flights)
     3. PHX → BNA (1 flight)
     ```
   - Prompt user to select a route
   - Call `analyzeFlightSchedule()` again with route filter, OR enhance service to return separate analyses per route

3. **If single route pattern (may be multi-leg):**
   - Display existing schedule analysis
   - Add new **"Bookable Segments"** section that shows:
     - All direct legs with their schedules
     - All through connections with intermediate stops noted
     - Performance stats per segment (completion rate, on-time %)

Example output:
```
BOOKABLE SEGMENTS
===================================
This flight offers 3 bookable segments:

Direct Legs:
  1. ORD → DAL: 733 miles
     Schedule: Departs 08:00, Arrives 10:30
     Operating Days: Daily
     Performance: 95% completion, 87% on-time

  2. DAL → MCO: 1,083 miles  
     Schedule: Departs 11:15, Arrives 14:45
     Operating Days: Daily
     Performance: 96% completion, 89% on-time

Through Connections:
  3. ORD → MCO (via DAL): 1,816 miles
     Legs: ORD → DAL → MCO
     Schedule: Departs ORD 08:00, Arrives MCO 14:45
     Operating Days: Daily
     Performance: 94% completion, 82% on-time
```

### 4. Implementation Phases

**Phase 1: Core functionality (minimum viable)**
- Create FlightSegment model
- Add segment extraction to FlightScheduleService (on-demand computation)
- Update FlightScheduleView to display segments
- Handle WN 4283 correctly (2-leg multi-leg)
- Handle WN 3310 correctly (multi-route)

**Phase 2: Performance optimization (if needed)**
- Add caching to FlightScheduleService
- Measure query performance with large Southwest dataset
- If too slow: Pre-compute segments in FlightDataIndex

**Phase 3: Edge case handling**
- Tail number validation for true through-flights
- Partial cancellation handling
- Hub detection
- Circular route detection

### 5. Critical Files

**flight-core:**
- `flight-core/src/main/java/com/lamontd/travel/flight/model/FlightSegment.java` (NEW)
- `flight-core/src/main/java/com/lamontd/travel/flight/index/RouteIndex.java` (NO CHANGE - just reference)

**asqp-reader:**
- `asqp-reader/src/main/java/com/lamontd/travel/flight/asqp/service/FlightScheduleService.java` (MODIFY)
  - Line 25: analyzeFlightSchedule() - Add multi-route and multi-leg detection
  - Line 194: FlightScheduleAnalysis class - Add bookableSegments fields
  - NEW: analyzeFlightSchedule(carrier, flight, origin, dest) overload
  - NEW: extractBookableSegments(dailyLegs) private method
- `asqp-reader/src/main/java/com/lamontd/travel/flight/asqp/view/FlightScheduleView.java` (MODIFY)
  - Add multi-route detection and user prompt
  - Add "Bookable Segments" display section
- `asqp-reader/src/main/java/com/lamontd/travel/flight/asqp/index/FlightDataIndex.java` (OPTIONAL - Phase 2)
  - Add segment pre-computation if performance requires

**Tests:**
- `flight-core/src/test/java/com/lamontd/travel/flight/model/FlightSegmentTest.java` (NEW)
- `asqp-reader/src/test/java/com/lamontd/travel/flight/asqp/service/FlightScheduleServiceTest.java` (NEW or ENHANCE if exists)
  - Test multi-leg segment extraction
  - Test multi-route detection
  - Test edge cases (partial cancellations, tail swaps)

### 5. Implementation Details

#### Segment Extraction Algorithm

For a daily flight operation with N legs sorted by time: [Leg1, Leg2, ..., LegN]:

1. **Direct legs:** Generate N segments (one per leg)
   - Segment(Leg[i].origin → Leg[i].destination, DIRECT_LEG)

2. **Through connections:** Generate all subsequences of length > 1
   - For i from 0 to N-1:
     - For j from i+1 to N:
       - Create segment from Leg[i].origin to Leg[j].destination
       - Intermediate stops: all destinations between i and j-1
       - Type: THROUGH_CONNECTION

Example: ORD→DAL→MCO (2 legs)
- Direct: ORD→DAL, DAL→MCO
- Through: ORD→MCO (via DAL)
- Total: 3 segments

Example: A→B→C→D (3 legs)
- Direct: A→B, B→C, C→D  
- Through: A→C (via B), A→D (via B,C), B→D (via C)
- Total: 6 segments

#### Performance Considerations

- Multi-leg detection happens once per flight number query (not pre-indexed)
- Segment extraction is O(N²) per date where N = legs per day (typically 1-3)
- Memory impact: Minimal, segments computed on-demand
- For WN 4283 with ~30 days of data and 2 legs/day: ~90 segment instances analyzed

### 6. Performance Considerations - ACTUAL DATA ANALYSIS

**Real-world dataset: January 2025 ASQP data**
- Total records: **599,030 flights**
- Southwest records: **105,307 flights** (17.6% of total)

**Multi-leg analysis:**
- **Total multi-leg flight-dates: 84,119** (14% of all flights!)
- Southwest multi-leg flight-dates: **10,910**

**Breakdown by carrier:**
- American (AA): 32,000 multi-leg flight-dates
- Delta (DL): 29,631 multi-leg flight-dates  
- **Southwest (WN): 10,910 multi-leg flight-dates**
- United (UA): 8,591 multi-leg flight-dates
- Alaska (AS): 2,968 multi-leg flight-dates

**Leg count distribution (WN only):**
- 1 leg: 80,770 flight-dates (86%)
- **2 legs: 8,810 flight-dates** (9.4%)
- **3 legs: 1,606 flight-dates** (1.7%)
- 4 legs: 385 flight-dates (0.4%)
- 5 legs: 95 flight-dates (0.1%)
- 6 legs: 14 flight-dates (<0.1%)

**Segment explosion calculation:**
- 2-leg flights: 8,810 × 3 segments = **26,430 segments**
- 3-leg flights: 1,606 × 6 segments = **9,636 segments**
- 4-leg flights: 385 × 10 segments = **3,850 segments**
- 5-leg flights: 95 × 15 segments = **1,425 segments**
- 6-leg flights: 14 × 21 segments = **294 segments**
- **Total WN segments from multi-leg: ~41,635**
- **Total WN segments with single-leg: ~122,000**

**Performance implications:**

**On-demand computation (original plan):**
- First query for typical 2-leg flight: ~1-5ms per flight number
- BUT: User browsing multiple flights = repeated computations
- Cache helps but only for repeated queries of same flight
- Memory: Minimal (only cached results)

**Pre-computed index (recommended now):**
- Index build time estimate: +2-5 seconds for full month
- Memory cost: ~122,000 FlightSegment objects × 200 bytes = **~24MB**
- Query time: O(1) lookup, <1ms
- **STRONGLY RECOMMENDED** given the scale

**Updated recommendation:**
Given that **14% of all flights are multi-leg**, and segment extraction is O(N²) per flight-date:
- **Phase 1**: Implement with on-demand computation for MVP
- **Phase 2 (CRITICAL)**: Pre-compute segments during FlightDataIndex construction
  - Add to index build process (one-time cost at startup)
  - Store in `Map<String, List<FlightSegment>> segmentsByFlightNumber`
  - 24MB memory cost is acceptable for 600k records
  - Enables instant queries for any flight

### 7. Testing Strategy

**Unit tests for FlightScheduleService:**

Create test with mock ASQPFlightRecord data:

1. **Single-leg flight** (regression test)
   - Mock WN 1234: ATL→DFW daily for a week
   - Expected: 1 segment (ATL→DFW, Direct leg)
   
2. **Two-leg flight** 
   - Mock WN 4283: ORD→DAL→MCO daily for a week
   - Expected: 3 segments
     - ORD→DAL (Direct leg)
     - DAL→MCO (Direct leg)  
     - ORD→MCO (Through connection)
   
3. **Three-leg flight**
   - Mock WN 5114: A→B→C→D daily for a week
   - Expected: 6 segments (3 direct + 3 through)
   
4. **Mixed operations** 
   - Mock WN 9999: Some days 1 leg (A→B), some days 2 legs (A→B→C)
   - Should handle both patterns and report appropriately
   
5. **Cancelled multi-leg flight**
   - Mock 2-leg flight where all legs cancelled
   - Segments should still be identified but marked as cancelled
   
6. **Performance stats per segment**
   - Verify completion rate, on-time %, delays computed correctly for each segment

**Integration tests with real data:**

User has identified these multi-leg flights in the dataset:
- **WN 4283**: 2-leg flight (ORD→DAL→MCO) - Should produce 3 segments
- **WN 5114**: 3-leg flight - Should produce 6 segments
- **WN 3310**: 3-leg flight - Should produce 6 segments

Test procedure:
1. Load actual data file with these flights
2. Query each flight via FlightView and FlightScheduleView
3. Verify correct segment count and types
4. Verify schedule analysis matches expected values per segment

### 8. Verification

After implementation:

1. **Build and test:**
   ```bash
   mvn clean install
   ```

2. **Run application with test data:**
   ```bash
   # Test with multi-leg examples
   java -jar asqp-reader/target/asqp-reader.jar asqp-reader/src/main/resources/data/multileg-examples.asqpflightrecord.csv
   ```

3. **Test WN 4283 (2-leg operation on specific dates):**
   - Select "Flight View" from menu
   - Enter "WN 4283"
   - Verify Route Overview shows:
     - Route 1: ORD → DAL → MCO (appears on dates with both legs)
     - Route 2: DAL → MCO (appears on dates with single leg)
   - Verify "Bookable Segments" section shows:
     - For 2-leg dates (2025-01-01, 2025-01-06):
       * ORD → DAL (Direct leg)
       * DAL → MCO (Direct leg)
       * ORD → MCO via DAL (Through connection)
     - For single-leg dates:
       * DAL → MCO (Direct leg) only
   - Verify performance stats computed per segment

4. **Test WN 3310 (multiple routes, not multi-leg):**
   - Enter "WN 3310" in FlightScheduleView
   - **Expected behavior:**
     - System detects 3 distinct routes
     - Either displays all 3 schedules separately, OR prompts user to select route
     - Route 1 (ONT→HOU): Shows schedule with ~25 operations, 1 segment (Direct leg)
     - Route 2 (PIT→MCO): Shows schedule with 2 operations, 1 segment (Direct leg)
     - Route 3 (PHX→BNA): Shows schedule with 1 operation, 1 segment (Direct leg)
   - This validates the system correctly handles flights that operate on completely different routes on different days

5. **Test WN 5224 (single-leg regression):**
   - Enter "WN 5224" 
   - Verify shows 1 segment: OAK → LGB (Direct leg)
   - Verify existing single-leg functionality unchanged

6. **Test with full dataset (if available):**
   - Run with user's full ASQP data
   - Test WN 5114 (reported as 3-leg operation)
   - Verify 6 segments displayed correctly

7. **Edge cases in multileg-examples.csv:**
   - WN 4283 on 2025-01-09, 2025-01-10: DAL→MCO cancelled (cancellation_code=B)
   - Verify cancelled legs are still identified in segments but marked appropriately

## Edge Cases to Handle

1. **Variable leg counts per day** (already present in WN 4283)
   - Some days: 2 legs (ORD→DAL→MCO)
   - Other days: 1 leg (DAL→MCO)
   - Solution: Extract segments per date, then aggregate

2. **Partial cancellations**
   - Example: Leg 1 operates, Leg 2 cancelled
   - Should still show segments but mark as "partially operated"
   - Affects performance stats (completion rate)

3. **Tail number changes mid-route**
   - Equipment swap between legs
   - How to detect: Different tail numbers for legs on same date/flight
   - May indicate NOT a true through-flight (separate operations)
   - Decision: Only treat as multi-leg if tail number matches OR is a known equipment swap pattern

4. **Very long chains (5+ legs)**
   - Performance impact: 5 legs = 15 segments (5 direct + 10 through)
   - Mitigation: Could limit through-segment depth (e.g., max 3 legs in a through connection)

5. **Circular routes**
   - Example: A→B→C→A (returns to origin)
   - Should detect and mark specially
   - All segments still valid but A→A would be unusual

6. **Same airport appears twice (non-circular)**
   - Example: A→B→A→C (return to A then continue)
   - Rare but possible with crew/equipment positioning
   - Need to track leg sequence, not just airport list

7. **Date boundary crossings**
   - Late-night departure, next-day arrival
   - Multi-leg flight departing at 23:00 with last arrival at 02:00
   - Group by departure date of first leg

8. **Hub-and-spoke detection**
   - Flight touches hub airport (e.g., WN via DAL)
   - Distinguish from point-to-point multi-leg
   - Could add flag: `isHubConnection`

## Performance Optimization

**Problem:** If many Southwest flights have multiple legs, computing segments on-demand for every query is expensive.

**Current approach:**
- Segment extraction happens in `FlightScheduleService.analyzeFlightSchedule()`
- O(N²) per date where N = legs per day
- Computed fresh for each query

**Proposed optimization: Pre-compute during index building**

Add to FlightDataIndex:

```java
// New index structures
private final Map<String, List<FlightSegment>> segmentsByFlightNumber;
private final Map<String, List<FlightSegment>> segmentsByRoute;
```

**Index build process:**
1. During `FlightDataIndex` construction, group all records by flight number + date
2. For each flight-date combination with multiple legs:
   - Extract all bookable segments
   - Cache in `segmentsByFlightNumber` map
3. Single-leg flights: Create 1 segment automatically

**Benefits:**
- Segments computed once during startup (included in index build time)
- Queries become O(1) lookups
- Memory cost: ~3x records for 2-leg flights (3 segments per 2 legs)

**Trade-offs:**
- Increased memory usage (estimate: +50% for Southwest-heavy datasets)
- Longer index build time (acceptable if it's a one-time cost)
- Stale data if records change (not an issue for historical analysis)

**Alternative: Lazy computation with caching**
- Compute segments on first query for a flight number
- Cache result in FlightScheduleService
- Best of both worlds: No memory cost for unqueried flights, but fast repeat queries

**Recommendation:** 
- Start with **lazy computation + caching** (simpler, lower risk)
- If performance testing shows issues, migrate to pre-computed index

## Architecture Notes

- `FlightSegment` goes in **flight-core** because it's a domain model reusable across modules
- Segment extraction logic: Initially in **FlightScheduleService** (service layer)
- If pre-computation adopted: Move to **FlightDataIndex** (index layer)
- FlightView consumes the enhanced analysis - presentation layer shows the segments to users
- No changes needed to FlightConverter - works with raw records
- RouteGraphService operates on physical routes, not affected by segment bookability
