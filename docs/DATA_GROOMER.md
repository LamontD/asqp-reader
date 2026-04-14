# ASQP Data Groomer

Python script for preprocessing ASQP 234 data files from the Bureau of Transportation Statistics.

## Overview

The ASQP Data Groomer processes raw ASQP 234 CSV files, extracting only the fields needed by the ASQP Reader application. This reduces file sizes by **60-70%** and simplifies data handling.

**Source:** [BTS Airline Service Quality Performance 234](https://www.bts.gov/browse-statistical-products-and-data/bts-publications/airline-service-quality-performance-234-time)

## What It Does

- Reads pipe-delimited CSV files from BTS
- Extracts 20 essential fields from 100+ available columns
- Writes groomed files with consistent format
- Processes entire directories in batch

## Usage

```bash
python asqp_bulk_data_groomer.py <input_directory> <output_directory>
```

**Arguments:**
- `input_directory` - Directory containing raw ASQP 234 CSV files
- `output_directory` - Directory for groomed output files (created if needed)

**Output:** Files named `<original-name>.groomed`

## Example

```bash
# Download ASQP data from BTS and place in raw_data/
python asqp_bulk_data_groomer.py ./raw_data ./processed_data

# Output:
# Found 3 file(s) to process.
# 
# Processing: ASQP_2025_01.csv
#   -> Wrote 50000 records to: ASQP_2025_01.csv.groomed
# Processing: ASQP_2025_02.csv
#   -> Wrote 48500 records to: ASQP_2025_02.csv.groomed
# 
# ============================================================
# Processing Complete
# ============================================================
# Successfully processed: 2 file(s)
# Total records processed: 98500
```

## Fields Extracted

### Flight Identification
1. `carrier_code` - 2-letter airline code (e.g., "DL")
2. `flight_number` - Flight number
3. `tail_number` - Aircraft registration

### Route Information
4. `origin` - Origin airport code (3 letters)
5. `destination` - Destination airport code (3 letters)

### Date/Time
6. `departure_date` - Date in YYYYMMDD format
7. `scheduled_oag_departure` - OAG scheduled departure (HHMM)
8. `scheduled_crs_departure` - CRS scheduled departure (HHMM)
9. `gate_departure` - Actual gate departure (HHMM)
10. `scheduled_arrival` - Scheduled arrival (HHMM)
11. `scheduled_crs_arrival` - CRS scheduled arrival (HHMM)
12. `gate_arrival` - Actual gate arrival (HHMM)
13. `wheels_up` - Wheels off time (HHMM)
14. `wheels_down` - Wheels on time (HHMM)

### Operations
15. `cancellation_code` - Cancellation code if applicable

### Delay Information
16. `carrier_delay` - Minutes of carrier delay
17. `weather_delay` - Minutes of weather delay
18. `nas_delay` - Minutes of NAS delay
19. `security_delay` - Minutes of security delay
20. `late_arrival_delay` - Minutes of late arrival delay

## Field Mapping

Original ASQP column positions → Groomed output:

| Field | Original Position | Groomed Position |
|-------|------------------|------------------|
| carrier_code | 1 | 1 |
| flight_number | 2 | 2 |
| origin | 7 | 3 |
| destination | 8 | 4 |
| departure_date | 9 | 5 |
| scheduled_oag_departure | 11 | 6 |
| scheduled_crs_departure | 12 | 7 |
| gate_departure | 13 | 8 |
| scheduled_arrival | 14 | 9 |
| scheduled_crs_arrival | 15 | 10 |
| gate_arrival | 16 | 11 |
| wheels_up | 24 | 12 |
| wheels_down | 25 | 13 |
| tail_number | 26 | 14 |
| cancellation_code | 30 | 15 |
| carrier_delay | 31 | 16 |
| weather_delay | 32 | 17 |
| nas_delay | 33 | 18 |
| security_delay | 34 | 19 |
| late_arrival_delay | 35 | 20 |

## File Size Reduction

**Original ASQP file:** 100+ columns, ~50-100 MB/month
**Groomed file:** 20 columns, ~15-30 MB/month

**Reduction:** 60-70% smaller

## Requirements

- Python 3.6+
- Standard library only (no external dependencies)

## Error Handling

- Creates output directory if it doesn't exist
- Skips malformed files and continues processing
- Reports errors per file
- Returns non-zero exit code if any file fails

## Output Format

Pipe-delimited CSV with header row:

```
carrier_code|flight_number|origin|destination|departure_date|...
DL|5030|CVG|LGA|20250127|1700|1700|1658|1859|1859|1902|1716|1853|N917XJ|
```

Compatible with ASQP Reader Java application.

## Script Location

`src/main/scripts/asqp_bulk_data_groomer.py`

## See Also

- [README.md](../README.md) - ASQP Reader application
- [DEVELOPER_GUIDE.md](DEVELOPER_GUIDE.md) - API reference
- [BTS ASQP Data](https://www.bts.gov/browse-statistical-products-and-data/bts-publications/airline-service-quality-performance-234-time)
