#!/usr/bin/env python3
"""
ASQP Data Groomer - Preprocesses BTS ASQP 234 data files.

This script processes Airline Service Quality Performance 234 data from the Bureau
of Transportation Statistics (https://www.bts.gov/browse-statistical-products-and-data/
bts-publications/airline-service-quality-performance-234-time).

It extracts 20 essential fields from the original 100+ column format, reducing file
sizes by 60-70% and creating a simplified format for the ASQP Reader Java application.

Usage:
    python asqp_bulk_data_groomer.py <input_directory> <output_directory>

Arguments:
    input_directory: Directory containing raw ASQP 234 CSV files (pipe-delimited)
    output_directory: Directory for groomed output files (created if needed)

Output:
    Files named <original-filename>.groomed with 20 columns:
    - Flight identification (carrier, flight number, tail number)
    - Route information (origin, destination)
    - Date/time information (departure date, scheduled/actual times)
    - Operations (cancellation codes)
    - Delay information (carrier, weather, NAS, security, late arrival)

Example:
    $ python asqp_bulk_data_groomer.py ./raw_data ./processed_data

    Found 2 file(s) to process.

    Processing: ASQP_2025_01.csv
      -> Wrote 50000 records to: ASQP_2025_01.csv.groomed
    Processing: ASQP_2025_02.csv
      -> Wrote 48500 records to: ASQP_2025_02.csv.groomed

    ============================================================
    Processing Complete
    ============================================================
    Successfully processed: 2 file(s)
    Total records processed: 98500

Author: ASQP Reader Project
License: Part of the ASQP data analysis suite
"""

import csv
import sys
from typing import List, Dict
from pathlib import Path


class FlightRecord:
    """Represents a single flight record from ASQP data.

    Extracts 20 essential fields from the original ASQP 234 format which contains 100+ columns.

    Field Mapping (Original Position -> Field Name):
        1  -> carrier_code          (2-letter airline code, e.g., "DL")
        2  -> flight_number          (Flight number)
        7  -> origin                 (3-letter origin airport code)
        8  -> destination            (3-letter destination airport code)
        9  -> departure_date         (YYYYMMDD format)
        11 -> scheduled_oag_departure (HHMM format)
        12 -> scheduled_crs_departure (HHMM format)
        13 -> gate_departure         (HHMM format, actual)
        14 -> scheduled_arrival      (HHMM format)
        15 -> scheduled_crs_arrival  (HHMM format)
        16 -> gate_arrival           (HHMM format, actual)
        24 -> wheels_up              (HHMM format)
        25 -> wheels_down            (HHMM format)
        26 -> tail_number            (Aircraft registration)
        30 -> cancellation_code      (Cancellation reason code)
        31 -> carrier_delay          (Minutes)
        32 -> weather_delay          (Minutes)
        33 -> nas_delay              (Minutes, National Airspace System)
        34 -> security_delay         (Minutes)
        35 -> late_arrival_delay     (Minutes)

    Attributes:
        All 20 fields listed above as instance variables.
    """

    def __init__(self, row: List[str]) -> None:
        """Initialize flight record from CSV row.

        Args:
            row: List of strings representing a CSV row from raw ASQP data.
                 Expected to have 100+ columns in BTS ASQP 234 format.
        """
        # Map columns to fields (0-based indexing for Python lists)
        self.carrier_code = row[0] if len(row) > 0 else ""
        self.flight_number = row[1] if len(row) > 1 else ""
        self.origin = row[6] if len(row) > 6 else ""
        self.destination = row[7] if len(row) > 7 else ""
        self.departure_date = row[8] if len(row) > 8 else ""
        self.scheduled_oag_departure = row[10] if len(row) > 10 else ""
        self.scheduled_crs_departure = row[11] if len(row) > 11 else ""
        self.gate_departure = row[12] if len(row) > 12 else ""
        self.scheduled_arrival = row[13] if len(row) > 13 else ""
        self.scheduled_crs_arrival = row[14] if len(row) > 14 else ""
        self.gate_arrival = row[15] if len(row) > 15 else ""
        self.wheels_up = row[23] if len(row) > 22 else ""
        self.wheels_down = row[24] if len(row) > 23 else ""
        self.tail_number = row[25] if len(row) > 24 else ""
        self.cancellation_code = row[29] if len(row) > 30 else ""
        self.carrier_delay = row[30] if len(row) > 31 else ""
        self.weather_delay = row[31] if len(row) > 32 else ""
        self.nas_delay = row[32] if len(row) > 33 else ""
        self.security_delay = row[33] if len(row) > 34 else ""
        self.late_arrival_delay = row[34] if len(row) > 35 else ""
    
    def to_dict(self) -> Dict[str, str]:
        """Convert record to dictionary.
        
        Returns:
            Dictionary mapping field names to values
        """
        return {
            "carrier_code": self.carrier_code,
            "flight_number": self.flight_number,
            "origin": self.origin,
            "destination": self.destination,
            "departure_date": self.departure_date,
            "scheduled_oag_departure": self.scheduled_oag_departure,
            "scheduled_crs_departure": self.scheduled_crs_departure,
            "gate_departure": self.gate_departure,
            "scheduled_arrival": self.scheduled_arrival,
            "scheduled_crs_arrival": self.scheduled_crs_arrival,
            "gate_arrival": self.gate_arrival,
            "wheels_up": self.wheels_up,
            "wheels_down": self.wheels_down,
            "tail_number": self.tail_number,
            "cancellation_code": self.cancellation_code,
            "carrier_delay": self.carrier_delay,
            "weather_delay": self.weather_delay,
            "nas_delay": self.nas_delay,
            "security_delay": self.security_delay,
            "late_arrival_delay": self.late_arrival_delay
        }


def ingest_csv(filepath: str) -> List[FlightRecord]:
    """Read CSV file with '|' separator and return list of flight records.
    
    Args:
        filepath: Path to the CSV file
        
    Returns:
        List of FlightRecord objects
        
    Raises:
        FileNotFoundError: If the file doesn't exist
        Exception: For other file reading errors
    """
    records = []
    file_path = Path(filepath)
    
    if not file_path.exists():
        raise FileNotFoundError(f"File not found: {filepath}")
    
    with open(file_path, 'r', encoding='utf-8') as f:
        # Use csv.reader with custom delimiter '|'
        reader = csv.reader(f, delimiter='|')
            
        for row in reader:
            records.append(FlightRecord(row))
    
    return records


def write_groomed_csv(records: List[FlightRecord], output_filepath: str) -> None:
    """Write groomed records to CSV file with '|' separator.

    Creates a simplified CSV file with 20 columns and a header row.
    Compatible with the ASQP Reader Java application.

    Output format:
        - Delimiter: pipe character ('|')
        - Header: Yes (field names)
        - Columns: 20 (reduced from 100+)
        - Size reduction: 60-70% smaller than original

    Args:
        records: List of FlightRecord objects to write
        output_filepath: Path to the output CSV file (will be created/overwritten)
    """
    # Define output columns (20 fields in groomed format)
    # Order matches the expected format for ASQP Reader Java application
    headers = [
        "carrier_code", "flight_number", "origin", "destination", "departure_date",
        "scheduled_oag_departure", "scheduled_crs_departure", "gate_departure",
        "scheduled_arrival", "scheduled_crs_arrival", "gate_arrival",
        "wheels_up", "wheels_down", "tail_number", "cancellation_code",
        "carrier_delay", "weather_delay", "nas_delay", "security_delay", "late_arrival_delay"
    ]

    with open(output_filepath, 'w', encoding='utf-8', newline='') as f:
        writer = csv.writer(f, delimiter='|')
        
        # Write header row
        writer.writerow(headers)
        
        # Write data rows
        for record in records:
            data = record.to_dict()
            writer.writerow([data[h] for h in headers])


def process_file(input_filepath: Path, output_filepath: Path) -> int:
    """Process a single CSV file and write groomed output.
    
    Args:
        input_filepath: Path to input CSV file
        output_filepath: Path to output groomed CSV file
        
    Returns:
        Number of records processed
        
    Raises:
        Exception: For file processing errors
    """
    records = ingest_csv(str(input_filepath))
    write_groomed_csv(records, str(output_filepath))
    return len(records)


def print_table(records: List[FlightRecord], max_rows: int = 20) -> None:
    """Print records in a formatted table.
    
    Args:
        records: List of FlightRecord objects
        max_rows: Maximum number of rows to display
    """
    if not records:
        print("No records to display.")
        return
    
    # Get first N records
    display_records = records[:max_rows]
    
        # Define column headers
    headers = [
        "carrier_code", "flight_number", "origin", "destination", "departure_date", 
        "scheduled_oag_departure", "scheduled_crs_departure", "gate_departure", "scheduled_arrival", "scheduled_crs_arrival",
        "gate_arrival", "wheels_up", "wheels_down", "tail_number", "cancellation_code",
        "carrier_delay", "weather_delay", "nas_delay", "security_delay", "late_arrival_delay"
    ]
    
    # Calculate column widths
    col_widths = {h: len(h) for h in headers}
    for record in display_records:
        data = record.to_dict()
        for header in headers:
            col_widths[header] = max(col_widths[header], len(str(data[header])))
    
    # Print header
    header_row = " | ".join(h.ljust(col_widths[h]) for h in headers)
    print("\n" + header_row)
    print("-" * len(header_row))
    
    # Print data rows
    for record in display_records:
        data = record.to_dict()
        row = " | ".join(str(data[h]).ljust(col_widths[h]) for h in headers)
        print(row)
    
    # Print summary
    print(f"\nDisplaying {len(display_records)} of {len(records)} total records.")


def main() -> int:
    """Main entry point for the script.
    
    Returns:
        Exit code (0 for success, 1 for error)
    """
    if len(sys.argv) < 3:
        print("Usage: python asqp_groomer.py <input_directory> <output_directory>")
        print("\nExample: python asqp_groomer.py ./input_data ./output_data")
        return 1
    
    input_dir = Path(sys.argv[1])
    output_dir = Path(sys.argv[2])
    
    # Validate input directory
    if not input_dir.exists():
        print(f"Error: Input directory does not exist: {input_dir}", file=sys.stderr)
        return 1
    
    if not input_dir.is_dir():
        print(f"Error: Input path is not a directory: {input_dir}", file=sys.stderr)
        return 1
    
    # Create output directory if it doesn't exist
    try:
        output_dir.mkdir(parents=True, exist_ok=True)
    except Exception as e:
        print(f"Error: Could not create output directory: {e}", file=sys.stderr)
        return 1
    
    # Find all files in input directory (non-recursive)
    input_files = [f for f in input_dir.iterdir() if f.is_file()]
    
    if not input_files:
        print(f"No files found in input directory: {input_dir}")
        return 0
    
    print(f"Found {len(input_files)} file(s) to process.\n")
    
    # Process each file
    total_records = 0
    successful_files = 0
    failed_files = 0
    
    for input_file in input_files:
        try:
            output_filename = input_file.name + ".groomed"
            output_filepath = output_dir / output_filename
            
            print(f"Processing: {input_file.name}")
            record_count = process_file(input_file, output_filepath)
            print(f"  -> Wrote {record_count} records to: {output_filename}")
            
            total_records += record_count
            successful_files += 1
            
        except Exception as e:
            print(f"  -> Error processing {input_file.name}: {e}", file=sys.stderr)
            failed_files += 1
    
    # Print summary
    print(f"\n{'='*60}")
    print(f"Processing Complete")
    print(f"{'='*60}")
    print(f"Successfully processed: {successful_files} file(s)")
    print(f"Failed: {failed_files} file(s)")
    print(f"Total records processed: {total_records}")
    print(f"Output directory: {output_dir.absolute()}")
    
    return 0 if failed_files == 0 else 1


if __name__ == "__main__":
    sys.exit(main())


