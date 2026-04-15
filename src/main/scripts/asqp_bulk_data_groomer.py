#!/usr/bin/env python3
"""
ASQP Data Groomer - Preprocesses BTS ASQP 234 data files.

This script processes Airline Service Quality Performance 234 data from the Bureau
of Transportation Statistics into formats compatible with the Flight Data Analysis
Java application.

Supports two output formats:
  - ASQPFlightRecord: Full ASQP data with 20 fields (default)
  - FlightRecord: Simplified flight record with core fields only

Usage:
    python asqp_bulk_data_groomer.py <input_directory> <output_directory> [options]

Arguments:
    input_directory: Directory containing raw ASQP 234 CSV files (pipe-delimited)
    output_directory: Directory for processed output files (created if needed)

Options:
    --format, -f: Output format (choices: asqp, flightrecord, both; default: asqp)
    --asqp-output-dir: Separate output directory for ASQP format
    --flightrecord-output-dir: Separate output directory for FlightRecord format

Examples:
    # Default: ASQPFlightRecord format
    python asqp_bulk_data_groomer.py ./raw_data ./processed_data

    # FlightRecord format only
    python asqp_bulk_data_groomer.py ./raw_data ./processed_data --format flightrecord

    # Both formats to same directory
    python asqp_bulk_data_groomer.py ./raw_data ./processed_data --format both

    # Both formats to different directories
    python asqp_bulk_data_groomer.py ./raw_data ./output --format both \
        --flightrecord-output-dir ./flightrecord_output

Author: ASQP Reader Project
License: Part of the ASQP data analysis suite
"""

import argparse
import csv
import sys
from typing import List, Dict, Tuple
from pathlib import Path


# File extensions by format
FORMAT_EXTENSIONS = {
    'asqp': '.asqpflightrecord.csv',
    'flightrecord': '.flightrecord.csv'
}


class ASQPFlightRecord:
    """Represents ASQP flight record with all 20 fields."""

    def __init__(self, row: List[str]) -> None:
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
        self.wheels_up = row[23] if len(row) > 23 else ""
        self.wheels_down = row[24] if len(row) > 24 else ""
        self.tail_number = row[25] if len(row) > 25 else ""
        self.cancellation_code = row[29] if len(row) > 29 else ""
        self.carrier_delay = row[30] if len(row) > 30 else ""
        self.weather_delay = row[31] if len(row) > 31 else ""
        self.nas_delay = row[32] if len(row) > 32 else ""
        self.security_delay = row[33] if len(row) > 33 else ""
        self.late_arrival_delay = row[34] if len(row) > 34 else ""

    def to_asqp_dict(self) -> Dict[str, str]:
        """Convert to ASQPFlightRecord format (20 fields)."""
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

    def to_flightrecord_dict(self) -> Dict[str, str]:
        """Convert to FlightRecord format (core fields only)."""
        # Determine status based on cancellation
        status = "CANCELLED" if self.cancellation_code else "ARRIVED"

        return {
            "carrier_code": self.carrier_code,
            "flight_number": self.flight_number,
            "operating_date": self.departure_date,
            "origin_airport": self.origin,
            "destination_airport": self.destination,
            "tail_number": self.tail_number,
            "actual_departure_time": self.gate_departure,
            "actual_arrival_time": self.gate_arrival,
            "wheels_up_time": self.wheels_up,
            "wheels_down_time": self.wheels_down,
            "status": status,
            "cancellation_code": self.cancellation_code
        }


def ingest_csv(filepath: str) -> List[ASQPFlightRecord]:
    """Read CSV file with '|' separator and return list of records."""
    records = []
    file_path = Path(filepath)

    if not file_path.exists():
        raise FileNotFoundError(f"File not found: {filepath}")

    with open(file_path, 'r', encoding='utf-8') as f:
        reader = csv.reader(f, delimiter='|')
        for row in reader:
            records.append(ASQPFlightRecord(row))

    return records


def write_asqp_csv(records: List[ASQPFlightRecord], output_filepath: str) -> None:
    """Write ASQPFlightRecord format CSV (20 fields)."""
    headers = [
        "carrier_code", "flight_number", "origin", "destination", "departure_date",
        "scheduled_oag_departure", "scheduled_crs_departure", "gate_departure",
        "scheduled_arrival", "scheduled_crs_arrival", "gate_arrival",
        "wheels_up", "wheels_down", "tail_number", "cancellation_code",
        "carrier_delay", "weather_delay", "nas_delay", "security_delay", "late_arrival_delay"
    ]

    with open(output_filepath, 'w', encoding='utf-8', newline='') as f:
        writer = csv.writer(f, delimiter='|')
        writer.writerow(headers)

        for record in records:
            data = record.to_asqp_dict()
            writer.writerow([data[h] for h in headers])


def write_flightrecord_csv(records: List[ASQPFlightRecord], output_filepath: str) -> None:
    """Write FlightRecord format CSV (core fields only)."""
    headers = [
        "carrier_code", "flight_number", "operating_date",
        "origin_airport", "destination_airport", "tail_number",
        "actual_departure_time", "actual_arrival_time",
        "wheels_up_time", "wheels_down_time",
        "status", "cancellation_code"
    ]

    with open(output_filepath, 'w', encoding='utf-8', newline='') as f:
        writer = csv.writer(f, delimiter='|')
        writer.writerow(headers)

        for record in records:
            data = record.to_flightrecord_dict()
            writer.writerow([data[h] for h in headers])


def get_output_filename(input_filename: str, format_type: str) -> str:
    """Generate output filename based on format type."""
    base_name = Path(input_filename).stem
    extension = FORMAT_EXTENSIONS[format_type]
    return f"{base_name}{extension}"


def process_file(input_filepath: Path, output_dir: Path, format_type: str) -> Tuple[int, str]:
    """Process a single CSV file and write output in specified format.

    Returns:
        Tuple of (record_count, output_filename)
    """
    records = ingest_csv(str(input_filepath))
    output_filename = get_output_filename(input_filepath.name, format_type)
    output_filepath = output_dir / output_filename

    if format_type == 'asqp':
        write_asqp_csv(records, str(output_filepath))
    else:  # flightrecord
        write_flightrecord_csv(records, str(output_filepath))

    return len(records), output_filename


def main() -> int:
    """Main entry point for the script."""
    parser = argparse.ArgumentParser(
        description='Process ASQP data into various output formats',
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  # Default: ASQPFlightRecord format
  python asqp_bulk_data_groomer.py ./raw_data ./processed_data

  # FlightRecord format only
  python asqp_bulk_data_groomer.py ./raw_data ./processed_data --format flightrecord

  # Both formats to same directory
  python asqp_bulk_data_groomer.py ./raw_data ./processed_data --format both
        """
    )

    parser.add_argument('input_dir',
                       help='Directory containing raw ASQP CSV files')
    parser.add_argument('output_dir',
                       help='Directory for processed output files')

    parser.add_argument('--format', '-f',
                       choices=['asqp', 'flightrecord', 'both'],
                       default='asqp',
                       help='Output format (default: asqp)')

    parser.add_argument('--asqp-output-dir',
                       help='Separate output directory for ASQP format')
    parser.add_argument('--flightrecord-output-dir',
                       help='Separate output directory for FlightRecord format')

    args = parser.parse_args()

    input_dir = Path(args.input_dir)
    output_dir = Path(args.output_dir)

    # Validate input directory
    if not input_dir.exists():
        print(f"Error: Input directory does not exist: {input_dir}", file=sys.stderr)
        return 1

    if not input_dir.is_dir():
        print(f"Error: Input path is not a directory: {input_dir}", file=sys.stderr)
        return 1

    # Determine output directories
    asqp_dir = Path(args.asqp_output_dir) if args.asqp_output_dir else output_dir
    flightrecord_dir = Path(args.flightrecord_output_dir) if args.flightrecord_output_dir else output_dir

    # Create output directories
    try:
        if args.format in ['asqp', 'both']:
            asqp_dir.mkdir(parents=True, exist_ok=True)
        if args.format in ['flightrecord', 'both']:
            flightrecord_dir.mkdir(parents=True, exist_ok=True)
    except Exception as e:
        print(f"Error: Could not create output directory: {e}", file=sys.stderr)
        return 1

    # Find input files
    input_files = [f for f in input_dir.iterdir() if f.is_file()]

    if not input_files:
        print(f"No files found in input directory: {input_dir}")
        return 0

    print(f"Found {len(input_files)} file(s) to process.\n")

    # Process files
    total_records = 0
    successful_files = 0
    failed_files = 0

    for input_file in input_files:
        try:
            print(f"Processing: {input_file.name}")

            # Process ASQP format
            if args.format in ['asqp', 'both']:
                record_count, output_filename = process_file(input_file, asqp_dir, 'asqp')
                print(f"  -> Wrote {record_count} records to: {output_filename}")
                total_records += record_count

            # Process FlightRecord format
            if args.format in ['flightrecord', 'both']:
                record_count, output_filename = process_file(input_file, flightrecord_dir, 'flightrecord')
                print(f"  -> Wrote {record_count} records to: {output_filename}")
                if args.format != 'both':
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
    if args.format in ['asqp', 'both']:
        print(f"ASQP output directory: {asqp_dir.absolute()}")
    if args.format in ['flightrecord', 'both']:
        print(f"FlightRecord output directory: {flightrecord_dir.absolute()}")

    return 0 if failed_files == 0 else 1


if __name__ == "__main__":
    sys.exit(main())
