package com.lamontd.travel.flight.schedule.controller;

import com.lamontd.travel.flight.mapper.AirportCodeMapper;
import com.lamontd.travel.flight.model.BookableFlight;
import com.lamontd.travel.flight.schedule.index.ScheduleFlightIndex;
import com.lamontd.travel.flight.schedule.util.InputValidator;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

/**
 * Controller for route search functionality.
 */
public class RouteSearchController {
    private static final AirportCodeMapper AIRPORT_MAPPER = AirportCodeMapper.getDefault();

    public void display(ScheduleFlightIndex index, Scanner scanner) {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("ROUTE SEARCH");
        System.out.println("=".repeat(80));

        System.out.print("Enter origin airport code (3 letters): ");
        String origin = scanner.nextLine().trim().toUpperCase();
        if (!InputValidator.validateAirportCode(origin)) {
            return;
        }

        System.out.print("Enter destination airport code (3 letters): ");
        String destination = scanner.nextLine().trim().toUpperCase();
        if (!InputValidator.validateAirportCode(destination)) {
            return;
        }

        System.out.print("Enter travel date (yyyy-MM-dd) or press Enter for all dates: ");
        String dateInput = scanner.nextLine().trim();

        List<BookableFlight> flights;
        if (dateInput.isEmpty()) {
            flights = index.getFlightsByRoute(origin, destination);
        } else {
            Optional<LocalDate> travelDateOpt = InputValidator.parseTravelDate(dateInput);
            if (travelDateOpt.isPresent()) {
                flights = index.getFlightsByRouteOnDate(origin, destination, travelDateOpt.get());
            } else {
                System.out.println("\nInvalid date format. Showing all flights.");
                flights = index.getFlightsByRoute(origin, destination);
            }
        }

        // Sort flights by date, then departure time
        flights.sort(Comparator
                .comparing(BookableFlight::getOperatingDate)
                .thenComparing(BookableFlight::getScheduledDepartureTime));

        displayResults(origin, destination, flights, dateInput);
    }

    private void displayResults(String origin, String destination, List<BookableFlight> flights, String dateFilter) {
        String originCity = AIRPORT_MAPPER.getAirportCity(origin);
        String destCity = AIRPORT_MAPPER.getAirportCity(destination);

        System.out.println("\n" + "=".repeat(80));
        System.out.printf("ROUTE: %s (%s) → %s (%s)%s%n",
                origin, originCity, destination, destCity,
                dateFilter.isEmpty() ? "" : " (Date: " + dateFilter + ")");
        System.out.println("=".repeat(80));

        if (flights.isEmpty()) {
            System.out.println("No flights found on this route.");
            return;
        }

        System.out.printf("%-8s %-12s %-12s %-12s %-15s%n",
                "Carrier", "Flight", "Departure", "Arrival", "Operating Date");
        System.out.println("-".repeat(65));

        for (BookableFlight flight : flights) {
            System.out.printf("%-8s %-12s %-12s %-12s %-15s%n",
                    flight.getCarrierCode(),
                    flight.getFlightNumber(),
                    flight.getScheduledDepartureTime(),
                    flight.getScheduledArrivalTime(),
                    flight.getOperatingDate().toString());
        }

        System.out.println("\nTotal flights found: " + flights.size());
    }
}
