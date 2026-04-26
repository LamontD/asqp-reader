package com.lamontd.travel.flight.schedule.controller;

import com.lamontd.travel.flight.model.BookableFlight;
import com.lamontd.travel.flight.schedule.index.ScheduleFlightIndex;

import java.util.Comparator;
import java.util.List;
import java.util.Scanner;

/**
 * Controller for flight search by carrier and flight number.
 */
public class FlightSearchController {

    public void display(ScheduleFlightIndex index, Scanner scanner) {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("FLIGHT SEARCH");
        System.out.println("=".repeat(80));

        System.out.print("Enter carrier code (2 letters): ");
        String carrier = scanner.nextLine().trim().toUpperCase();

        System.out.print("Enter flight number: ");
        String flightNumber = scanner.nextLine().trim();

        List<BookableFlight> flights = index.getFlightsByCarrierAndFlightNumber(carrier, flightNumber);

        // Sort flights by date, then departure time
        flights.sort(Comparator
                .comparing(BookableFlight::getOperatingDate)
                .thenComparing(BookableFlight::getScheduledDepartureTime));

        displayResults(carrier, flightNumber, flights);
    }

    private void displayResults(String carrier, String flightNumber, List<BookableFlight> flights) {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("FLIGHT: " + carrier + " " + flightNumber);
        System.out.println("=".repeat(80));

        if (flights.isEmpty()) {
            System.out.println("Flight not found.");
            return;
        }

        System.out.printf("%-8s %-8s %-12s %-12s %-15s%n",
                "Origin", "Dest", "Departure", "Arrival", "Operating Date");
        System.out.println("-".repeat(65));

        for (BookableFlight flight : flights) {
            System.out.printf("%-8s %-8s %-12s %-12s %-15s%n",
                    flight.getOriginAirport(),
                    flight.getDestinationAirport(),
                    flight.getScheduledDepartureTime(),
                    flight.getScheduledArrivalTime(),
                    flight.getOperatingDate().toString());
        }

        System.out.println("\nTotal schedules found: " + flights.size());
    }
}
