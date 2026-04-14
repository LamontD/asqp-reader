# Data Sources

Reference guide for data sources used in the ASQP Reader.

## OpenFlights Data (Primary Source)

The application uses OpenFlights as the primary data source for airlines and airports.

### Airlines Database

**Status:** ✅ IN USE

- **Coverage:** 992+ active airlines worldwide
- **GitHub:** https://github.com/jpatokal/openflights
- **Direct Data:** https://raw.githubusercontent.com/jpatokal/openflights/master/data/airlines.dat
- **License:** Open Database License (ODbL)

**Format:**
```
id,Name,Alias,IATA,ICAO,Callsign,Country,Active
```

**Example:**
```
2009,"Delta Air Lines",\N,"DL","DAL","DELTA","United States","Y"
24,"American Airlines",\N,"AA","AAL","AMERICAN","United States","Y"
```

**Updating:**
```bash
curl -o src/main/resources/data/airlines.dat \
  https://raw.githubusercontent.com/jpatokal/openflights/master/data/airlines.dat
```

### Airports Database

**Status:** ✅ IN USE

- **Coverage:** 6,033 airports with valid IATA codes (from 7,698 total records)
- **GitHub:** https://github.com/jpatokal/openflights
- **Direct Data:** https://raw.githubusercontent.com/jpatokal/openflights/master/data/airports.dat
- **License:** Open Database License (ODbL)

**Fields:**
- IATA code (3-letter)
- Airport name
- City, Country
- ICAO code (4-letter)
- Latitude, Longitude
- Altitude (feet)
- Timezone, DST rules
- Type

**Updating:**
```bash
curl -o src/main/resources/data/airports.dat \
  https://raw.githubusercontent.com/jpatokal/openflights/master/data/airports.dat
```

### Why OpenFlights?

- ✅ Comprehensive worldwide coverage
- ✅ Community-maintained, regularly updated
- ✅ Free and open (ODbL license)
- ✅ Used by many flight tracking applications
- ✅ Rich data (ICAO codes, coordinates, timezones)
- ✅ Easy to update (single curl command)

---

## Country Codes

### ISO 3166-1 Standard

**Status:** ✅ IN USE

- **Coverage:** 193 UN-recognized countries
- **Standard:** ISO 3166-1 (International Organization for Standardization)
- **Format:** JSON array
- **File:** `src/main/resources/data/countries.json`

**Fields:**
- Numeric country code (e.g., 840 for USA)
- Alpha-2 code (e.g., "US")
- Alpha-3 code (e.g., "USA")
- Country name (e.g., "United States of America")

**Source:**
ISO 3166-1 official country codes

---

## Alternative Carrier Data Sources

### Bureau of Transportation Statistics (BTS)

**Recommended for:** Official US airline data used in ASQP reporting

- **Website:** https://www.transtats.bts.gov
- **Carrier Lookup:** https://www.transtats.bts.gov/Data_Elements.aspx?Data=2
- **Download:** https://www.transtats.bts.gov/DL_SelectFields.aspx?gnoyr_VQ=FGJ&QO_fu146_anzr=b0-gvzr

**Benefits:**
- Official US government source
- Matches ASQP reporting requirements
- Authoritative for US carriers

**Format:**
- Code (2-letter carrier code)
- Description (Full carrier name)

### IATA Airline Codes

**International Air Transport Association**

- **Website:** https://www.iata.org/en/publications/directories/code-search/
- **Coverage:** Official international airline codes
- **Access:** Requires subscription for bulk data (individual lookups free)

**Benefits:**
- Most authoritative for international airlines
- Official IATA designations
- Includes detailed carrier information

---

## Data File Formats

### OpenFlights Airlines (airlines.dat)

CSV format with 8 fields:
```
Airline ID,Name,Alias,IATA,ICAO,Callsign,Country,Active
```

**Loading in code:**
```java
CarrierCodeMapper mapper = CarrierCodeMapper.getDefault();
// Automatically filters to active airlines with valid IATA codes
```

### OpenFlights Airports (airports.dat)

CSV format with 14 fields:
```
Airport ID,Name,City,Country,IATA,ICAO,Latitude,Longitude,Altitude,Timezone,DST,Tz database,Type,Source
```

**Loading in code:**
```java
AirportCodeMapper mapper = AirportCodeMapper.getDefault();
// Filters to airports with valid 3-letter IATA codes
```

### Countries (countries.json)

JSON array format:
```json
[
  {
    "id": 840,
    "alpha2": "us",
    "alpha3": "usa",
    "name": "United States of America"
  }
]
```

**Loading in code:**
```java
CountryCodeMapper mapper = CountryCodeMapper.getDefault();
// Case-insensitive lookups supported
```

### Legacy Carrier CSV (carriers.csv)

Simple CSV format (backward compatibility):
```csv
code,name,full_name
DL,Delta,Delta Air Lines Inc.
```

**Loading in code:**
```java
CarrierCodeMapper mapper = new CarrierCodeMapper();
mapper.loadFromResource("/data/carriers.csv");
```

---

## Updating Reference Data

### Recommended Update Frequency

- **Airlines:** Monthly or quarterly
- **Airports:** Quarterly or when new airports open
- **Countries:** Rarely (only when new countries recognized)

### Update Process

1. **Download latest data:**
   ```bash
   # Airlines
   curl -o src/main/resources/data/airlines.dat \
     https://raw.githubusercontent.com/jpatokal/openflights/master/data/airlines.dat
   
   # Airports
   curl -o src/main/resources/data/airports.dat \
     https://raw.githubusercontent.com/jpatokal/openflights/master/data/airports.dat
   ```

2. **Test:**
   ```bash
   mvn test
   ```

3. **Build:**
   ```bash
   mvn clean package
   ```

4. **Deploy:**
   ```bash
   cp target/asqp-reader.jar /deployment/location/
   ```

### Automated Updates

Example GitHub Action:
```yaml
name: Update Reference Data

on:
  schedule:
    - cron: '0 0 1 * *'  # First of month
  workflow_dispatch:

jobs:
  update:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      
      - name: Download OpenFlights data
        run: |
          curl -o src/main/resources/data/airlines.dat \
            https://raw.githubusercontent.com/jpatokal/openflights/master/data/airlines.dat
          curl -o src/main/resources/data/airports.dat \
            https://raw.githubusercontent.com/jpatokal/openflights/master/data/airports.dat
      
      - name: Test
        run: mvn test
      
      - name: Create Pull Request
        if: success()
        uses: peter-evans/create-pull-request@v5
        with:
          title: 'Update OpenFlights reference data'
          body: 'Automated update of airlines and airports data'
```

---

## Custom Data Sources

### Using Your Own Data

**Option 1: Replace files**
```bash
# Replace with your custom data
cp my-airlines.dat src/main/resources/data/airlines.dat
mvn clean package
```

**Option 2: Load from external file**
```java
CarrierCodeMapper mapper = CarrierCodeMapper.fromFile(
    Paths.get("/path/to/my-carriers.csv")
);
```

**Option 3: Build programmatically**
```java
CarrierCodeMapper mapper = new CarrierCodeMapper();
mapper.addCarrier("DL", "Delta", "Delta Air Lines Inc.");
mapper.addCarrier("AA", "American", "American Airlines Inc.");
```

**Option 4: Mix sources**
```java
CarrierCodeMapper mapper = CarrierCodeMapper.getDefault();
// Add or override specific carriers
mapper.addCarrier("XX", "Custom Airlines", "Custom Airlines Corp");
```

---

## Data Quality

### OpenFlights Coverage

**Airlines:**
- Total entries: 6,000+
- Active airlines: 992+
- Filtered by: Active status + valid IATA codes

**Airports:**
- Total entries: 7,698
- With IATA codes: 6,033
- Coverage: Worldwide, all continents

**Countries:**
- Total: 193
- Coverage: All UN-recognized countries
- Standard: ISO 3166-1

### Validation

After updating data, validate with:
```bash
mvn test -Dtest=CarrierCodeMapperTest,AirportCodeMapperTest,CountryCodeMapperTest
```

---

## License Information

### OpenFlights Data

- **License:** Open Database License (ODbL)
- **Attribution:** OpenFlights project (https://openflights.org)
- **Commercial use:** Allowed with attribution
- **Modifications:** Allowed with share-alike

### ISO 3166-1 Country Codes

- **License:** Public domain (ISO standard codes)
- **Usage:** Free for any purpose
- **Source:** International Organization for Standardization

---

## Resources

- **OpenFlights GitHub:** https://github.com/jpatokal/openflights
- **OpenFlights Website:** https://openflights.org
- **BTS Carrier Data:** https://www.transtats.bts.gov
- **IATA Code Search:** https://www.iata.org/en/publications/directories/code-search/
- **ISO 3166-1:** https://www.iso.org/iso-3166-country-codes.html
