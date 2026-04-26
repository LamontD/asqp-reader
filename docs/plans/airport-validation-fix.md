# Plan: Airport Code Validation Fix

## Context

**Problem:** When users enter non-existent airport codes (e.g., "MD"), the application treats them as valid and displays them without error. This leads to confusing empty results instead of clear error messages.

**User Experience Issue:**
```
Enter origin airport code: MD
Enter destination airport code: DAL
ROUTE: MD (MD) → DAL (Dallas)
No flights found on this route.
```

**Expected Behavior:**
```
Enter origin airport code: MD
✗ Invalid airport code: MD
Airport not found in database
```

## Root Cause

`AirportCodeMapper.getAirportCity(code)` returns the code itself if not found (graceful fallback):
```java
public String getAirportCity(String code) {
    return getAirportInfo(code)
            .map(AirportInfo::getCity)
            .orElse(code);  // Returns code if not found!
}
```

Controllers don't validate before proceeding with queries.

## Affected Controllers

Found **6 controllers** with airport code input (9 total input points):

1. **RouteSearchController** - 2 airports (origin, destination)
2. **DirectFlightFinderController** - 2 airports (origin, destination)  
3. **ConnectionFinderController** - 2 airports (origin, destination)
4. **AirportViewController** - 1 airport
5. **NetworkAnalysisController** - 3 input points:
   - Shortest path: 2 airports (origin, destination)
   - Reachable airports: 1 airport (origin)

**Note:** CarrierViewController and FlightSearchController use carrier codes, not airport codes.

## Solution Design

### Option 1: Validation Helper Method (Recommended)

Create a reusable validation method in a utility class:

**Pros:**
- DRY principle - single validation logic
- Consistent error messages
- Easy to maintain

**Cons:**
- Need to create utility class or add to existing

### Option 2: Inline Validation in Each Controller

Add validation directly in each controller:

**Pros:**
- No new dependencies
- Simple to implement

**Cons:**
- Code duplication (9 places)
- Inconsistent error messages
- Hard to maintain

**Decision:** Option 1 - Create validation utility

## Implementation Plan

### Step 1: Create Validation Helper

**New Method in AirportCodeMapper:**

```java
/**
 * Validates that an airport code exists in the database.
 * 
 * @param code Airport code to validate
 * @return true if airport exists, false otherwise
 */
public boolean isValidAirportCode(String code) {
    return airportMap.containsKey(code);
}
```

**Why add to AirportCodeMapper?**
- Already manages airport data
- Natural location for validation
- Controllers already use this class

### Step 2: Update RouteSearchController

**Current Code (lines 24-29):**
```java
System.out.print("Enter origin airport code (3 letters): ");
String origin = scanner.nextLine().trim().toUpperCase();

System.out.print("Enter destination airport code (3 letters): ");
String destination = scanner.nextLine().trim().toUpperCase();
```

**New Code:**
```java
System.out.print("Enter origin airport code (3 letters): ");
String origin = scanner.nextLine().trim().toUpperCase();
if (!AIRPORT_MAPPER.isValidAirportCode(origin)) {
    System.out.println("\n✗ Invalid airport code: " + origin);
    System.out.println("Airport not found in database.");
    return;
}

System.out.print("Enter destination airport code (3 letters): ");
String destination = scanner.nextLine().trim().toUpperCase();
if (!AIRPORT_MAPPER.isValidAirportCode(destination)) {
    System.out.println("\n✗ Invalid airport code: " + destination);
    System.out.println("Airport not found in database.");
    return;
}
```

### Step 3: Update DirectFlightFinderController

Similar pattern at lines 30-48:
- Validate origin (after line 33)
- Validate destination (after line 43)

### Step 4: Update ConnectionFinderController

Similar pattern at lines 34-52:
- Validate origin (after line 38)
- Validate destination (after line 47)

### Step 5: Update AirportViewController

At line 22, validate airport code:
```java
String airport = scanner.nextLine().trim().toUpperCase();
if (!AIRPORT_MAPPER.isValidAirportCode(airport)) {
    System.out.println("\n✗ Invalid airport code: " + airport);
    System.out.println("Airport not found in database.");
    return;
}
```

### Step 6: Update NetworkAnalysisController

**Shortest Path (line 185-194):**
- Validate origin
- Validate destination

**Reachable Airports (line 237-241):**
- Validate origin

## Files to Modify

1. **AirportCodeMapper.java** - Add `isValidAirportCode()` method
2. **RouteSearchController.java** - Validate 2 airports
3. **DirectFlightFinderController.java** - Validate 2 airports
4. **ConnectionFinderController.java** - Validate 2 airports
5. **AirportViewController.java** - Validate 1 airport
6. **NetworkAnalysisController.java** - Validate 3 input points (origin in 2 places, destination in 1)

**Total:** 6 files, 9 validation points

## Error Message Format

**Consistent Format:**
```
✗ Invalid airport code: XYZ
Airport not found in database.
```

**Why this format:**
- ✗ symbol clearly indicates error
- States the invalid code for clarity
- Explains what went wrong

## Edge Cases

1. **Empty input**: Already handled by existing length checks
2. **Lowercase input**: Already normalized with `.toUpperCase()`
3. **Extra whitespace**: Already handled with `.trim()`
4. **Valid but no flights**: Different message ("No flights found") - correct behavior

## Testing Strategy

### Manual Testing

**Test 1: Invalid Origin in Route Search**
```
Input: Origin = "MD", Destination = "DAL"
Expected: Error message, return to menu
```

**Test 2: Valid Origin, Invalid Destination**
```
Input: Origin = "BWI", Destination = "XYZ"
Expected: Error message, return to menu
```

**Test 3: Both Valid but No Flights**
```
Input: Origin = "JFK", Destination = "SFO"  
Expected: "No flights found" (not an error)
```

**Test 4: Valid Airports with Flights**
```
Input: Origin = "BWI", Destination = "DAL"
Expected: Flight results displayed
```

### Testing Each Controller

Test validation in:
- Route Search (option 1)
- Flight Search (option 2) - N/A, uses carrier codes
- Airport View (option 3)
- Carrier View (option 4) - N/A, uses carrier codes
- Travel Planner (option 5)
- Connection Finder (option 6)
- Network Analysis (option 7) - test both shortest path and reachable airports

## Risk Assessment

**Low Risk:**
- Simple validation check
- Early return prevents bad data from propagating
- No data modification
- No breaking changes to existing valid inputs

**Potential Issues:**
- None identified

## Success Criteria

✅ Invalid airport codes rejected with clear error message
✅ Valid airport codes continue to work normally
✅ All 9 input points validated
✅ Consistent error messages across controllers
✅ All existing tests still pass
✅ Manual testing confirms validation works

## Verification

```bash
# Build and test
mvn clean test

# Run application
java -jar flight-schedule-reader/target/flight-schedule-reader.jar test-travel-planning.bookableflight.csv

# Test each validation point:
# 1. Route Search: Enter "MD" as origin → expect error
# 2. Route Search: Enter "BWI" origin, "XYZ" destination → expect error  
# 3. Travel Planner: Enter "MD" → expect error
# 4. Airport View: Enter "MD" → expect error
# 5. Connection Finder: Enter "MD" → expect error
# 6. Network Analysis → Shortest Path: Enter "MD" → expect error
# 7. Network Analysis → Reachable Airports: Enter "MD" → expect error
```

## Estimated Effort

- AirportCodeMapper method: 5 minutes
- RouteSearchController: 5 minutes
- DirectFlightFinderController: 5 minutes
- ConnectionFinderController: 5 minutes
- AirportViewController: 3 minutes
- NetworkAnalysisController: 8 minutes (3 validation points)
- Testing: 15 minutes
- **Total: ~45 minutes**
