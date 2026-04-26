# Plan: Network Analysis Feature for Flight Schedule Reader

## Context

The flight-schedule-reader now has complete travel planning capabilities (direct flights + connections). Next enhancement: **Network Analysis View** to visualize and analyze the route network, identify hub airports, and provide shortest path analysis.

**Key difference from asqp-reader:** Schedule data is date-specific. Network analysis should allow users to select a date to see which routes operate on that specific day.

## Current State

**What exists:**
- ScheduleDateRouteIndex - adapter that creates date-specific RouteIndex
- RouteGraphService - already integrated, provides network stats and pathfinding
- ConnectionFinderController - already builds date-specific graphs (but only internal, not exposed)
- Menu has 7 options (5 & 6 are travel planning)

**What's needed:**
- NetworkAnalysisController - new menu option for network visualization
- Date selection capability for network queries
- Hub airport analysis for specific dates
- Shortest path finder (pure network analysis, not booking-focused like ConnectionFinder)
- Reachable airports analysis

## Design Decisions

### 1. Date-Specific vs All-Dates Network

**Options:**
- A) Always require date selection (consistent with BookableFlight model)
- B) Build network from all available flights (ignores dates)
- C) Let user choose: specific date OR all dates

**Decision: Option C** - User choice provides flexibility:
- **Specific date**: "What's the network on June 10?" (real travel planning)
- **All dates**: "What's the theoretical network?" (route coverage analysis)

### 2. Controller Architecture

**Pattern:** Follow asqp-reader's RouteAnalysisView structure with adaptations

**Menu Structure:**
```
8. Network Analysis ← NEW option
   1. Select Analysis Date (or All Dates)
   2. Network Statistics
   3. Find Shortest Path
   4. Find Reachable Airports
   5. Hub Airport Analysis
   6. Return to Main Menu
```

**Date handling:**
- On first access: prompt for date OR "all dates"
- Allow changing date from submenu
- Cache RouteGraphService per date (rebuild only when date changes)

### 3. Features to Implement

**1. Date Selection**
- Prompt: "Enter date (yyyy-MM-dd) or 'all' for all dates"
- Show which dates have flight data (from ScheduleFlightIndex)
- Cache graph per date to avoid rebuilding

**2. Network Statistics**
- Airport count, route count
- Average connections per airport
- Top 10 hub airports (by connection count)
- **NEW**: Date-specific metrics (e.g., "47 routes operate on 2025-06-10")

**3. Shortest Path Finder**
- Input: origin, destination
- Output: shortest distance path with segment details
- Same as asqp-reader but date-aware

**4. Reachable Airports**
- Input: origin, max layovers
- Output: all reachable airports grouped by layover count
- **NEW**: Show which airports require 0, 1, 2+ layovers on specific date

**5. Hub Airport Analysis**
- Rank airports by connectivity (degree centrality)
- Show which airports are best connection points
- **NEW**: "Best hubs for date X" vs "Best hubs overall"

## Implementation Plan

### Step 1: Create NetworkAnalysisController

**File:** `flight-schedule-reader/src/main/java/com/lamontd/travel/flight/schedule/controller/NetworkAnalysisController.java`

**Structure:**
```java
public class NetworkAnalysisController {
    private RouteGraphService graphService;
    private LocalDate selectedDate; // null = all dates
    private ScheduleFlightIndex scheduleIndex;
    
    public void display(ScheduleFlightIndex index, Scanner scanner) {
        // Submenu loop with options 1-6
    }
    
    private void selectDate(Scanner scanner) {
        // Prompt for date or "all"
        // Show available dates from index
        // Rebuild graph when date changes
    }
    
    private void showNetworkStatistics() {
        // Network size, connectivity, top hubs
    }
    
    private void findShortestPath(Scanner scanner) {
        // Origin/dest input, show path
    }
    
    private void findReachableAirports(Scanner scanner) {
        // Origin + max layovers, show results
    }
    
    private void analyzeHubAirports() {
        // Top 15 hubs with connectivity scores
    }
    
    private void rebuildGraphIfNeeded() {
        // Build graph based on selectedDate
        // Cache to avoid rebuilds
    }
}
```

**Key Implementation Details:**

1. **Date Selection:**
   - Get available dates: `scheduleIndex.getAllFlights().stream().map(f -> f.getOperatingDate()).distinct()`
   - Show date range: "Available dates: 2025-06-10 to 2025-06-15"
   - Accept "all" to build graph from all flights (ignore dates)

2. **Graph Building:**
   - If selectedDate != null: use ScheduleDateRouteIndex
   - If selectedDate == null: create AllDatesRouteIndex adapter (build from all flights)

3. **Display Enhancements:**
   - Show current analysis mode: "Network Analysis (Date: 2025-06-10)" or "Network Analysis (All Dates)"
   - Add date info to all outputs

### Step 2: Create AllDatesRouteIndex (Optional Adapter)

**File:** `flight-schedule-reader/src/main/java/com/lamontd/travel/flight/schedule/index/AllDatesRouteIndex.java`

**Purpose:** RouteIndex adapter that includes ALL flights regardless of date

**Implementation:**
```java
public class AllDatesRouteIndex implements RouteIndex {
    private final ScheduleFlightIndex scheduleIndex;
    
    // Build routes from ALL flights (no date filtering)
    // Used when user selects "all dates" option
}
```

**Alternative:** Reuse ScheduleDateRouteIndex by passing null date? 
**Decision:** Create separate adapter for clarity (null handling is error-prone)

### Step 3: Update MenuController

**File:** `flight-schedule-reader/src/main/java/com/lamontd/travel/flight/schedule/controller/MenuController.java`

**Changes:**
- Add option 8: "Network Analysis"
- Update menu display to show 1-8
- Add case "8" to launch NetworkAnalysisController

**New Menu:**
```
1. Route Search (find flights between airports)
2. Flight Search (search by carrier/flight number)
3. Airport View (view all flights for an airport)
4. Carrier View (view all flights for a carrier)
5. Travel Planner (find direct flights by date)
6. Connection Finder (multi-leg journeys)
7. Network Analysis (route network visualization)  ← NEW
8. Exit
```

### Step 4: Enhance ScheduleFlightIndex (Helper Methods)

**File:** `flight-schedule-reader/src/main/java/com/lamontd/travel/flight/schedule/index/ScheduleFlightIndex.java`

**Add methods:**
```java
/**
 * Returns all unique dates that have flight data
 */
public Set<LocalDate> getAvailableDates() {
    return allFlights.stream()
        .map(BookableFlight::getOperatingDate)
        .collect(Collectors.toSet());
}

/**
 * Returns date range (min to max)
 */
public Optional<LocalDate> getMinDate() { ... }
public Optional<LocalDate> getMaxDate() { ... }
```

## Critical Files

**New Files (2):**
1. `flight-schedule-reader/controller/NetworkAnalysisController.java` (~300 lines)
2. `flight-schedule-reader/index/AllDatesRouteIndex.java` (~80 lines)

**Modified Files (2):**
3. `flight-schedule-reader/controller/MenuController.java` (add option 8)
4. `flight-schedule-reader/index/ScheduleFlightIndex.java` (add date helper methods)

**Tests (optional):**
5. `flight-schedule-reader/test/.../NetworkAnalysisControllerTest.java`
6. `flight-schedule-reader/test/.../AllDatesRouteIndexTest.java`

## Feature Comparison: asqp-reader vs schedule-reader

| Feature | asqp-reader | schedule-reader (NEW) |
|---------|-------------|----------------------|
| Network Stats | All flights | Date-specific OR all dates |
| Shortest Path | Static network | Choose date for analysis |
| Reachable Airports | All routes | Routes operating on date |
| Hub Analysis | Overall connectivity | Date-specific hubs |
| Use Case | Historical analysis | Travel planning + network viz |

## User Experience Flow

**Example 1: Date-Specific Analysis**
```
User: Select option 7 (Network Analysis)
System: Enter date (yyyy-MM-dd) or 'all': 2025-06-10
System: Building route network for 2025-06-10... (3 ms)
System: Network Analysis (Date: 2025-06-10)
        1. Change Analysis Date
        2. Network Statistics
        ...

User: Select 2 (Network Statistics)
System: NETWORK STATISTICS (2025-06-10)
        Airports: 5
        Routes: 7
        Top Hubs: BWI (3 routes), ORD (2 routes)...
```

**Example 2: All-Dates Analysis**
```
User: Select option 7 (Network Analysis)
System: Enter date (yyyy-MM-dd) or 'all': all
System: Building route network from all flights... (8 ms)
System: Network Analysis (All Dates)
        Airports: 6
        Routes: 9
        ...
```

## Success Criteria

✅ User can analyze network for specific date
✅ User can analyze overall network (all dates)
✅ Network statistics show airport/route counts
✅ Hub airport analysis identifies key connection points
✅ Shortest path finder works for date-specific networks
✅ Reachable airports analysis shows layover-based connectivity
✅ Date can be changed without restarting application
✅ Graph caching prevents unnecessary rebuilds

## Testing Strategy

**Manual Testing:**
1. Load test data with multiple dates
2. Test date-specific network (2025-06-10)
3. Test all-dates network
4. Change date and verify graph rebuilds
5. Test shortest path on date-specific network
6. Test reachable airports with different layover limits

**Unit Tests (optional):**
- AllDatesRouteIndex returns correct routes
- Date filtering works correctly
- Available dates query returns expected results

## Estimated Effort

- NetworkAnalysisController: ~3 hours
- AllDatesRouteIndex: ~1 hour
- MenuController update: ~15 minutes
- ScheduleFlightIndex enhancements: ~30 minutes
- Testing: ~1 hour
- **Total: ~5-6 hours**

## Notes

- Reuses 90% of asqp-reader RouteAnalysisView logic
- Main difference: date selection and date-aware graph building
- No changes to RouteGraphService (already perfect)
- Pattern established for future date-based features
