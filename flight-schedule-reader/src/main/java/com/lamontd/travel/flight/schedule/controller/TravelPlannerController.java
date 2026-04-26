package com.lamontd.travel.flight.schedule.controller;

import com.lamontd.travel.flight.mapper.AirportCodeMapper;
import com.lamontd.travel.flight.mapper.CarrierCodeMapper;
import com.lamontd.travel.flight.model.BookableFlight;
import com.lamontd.travel.flight.schedule.index.ScheduleDateRouteIndex;
import com.lamontd.travel.flight.schedule.index.ScheduleFlightIndex;
import com.lamontd.travel.flight.schedule.util.FlightTimeUtils;
import com.lamontd.travel.flight.schedule.util.InputValidator;
import com.lamontd.travel.flight.service.RouteGraphService;
import org.jgrapht.GraphPath;
import org.jgrapht.graph.DefaultWeightedEdge;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

/**
 * Enhanced travel planner that finds both direct and connecting flights
 * with time-of-day filtering.
 */
public class TravelPlannerController {
    private static final AirportCodeMapper AIRPORT_MAPPER = AirportCodeMapper.getDefault();
    private static final CarrierCodeMapper CARRIER_MAPPER = CarrierCodeMapper.getDefault();

    private static final int MIN_LAYOVER_MINUTES = 60;
    private static final int MAX_LAYOVER_MINUTES = 360; // 6 hours
    private static final int WARN_LAYOVER_MINUTES = 90;

    private enum TimeOfDay {
        MORNING("Morning", LocalTime.of(6, 0), LocalTime.of(11, 59)),
        AFTERNOON("Afternoon", LocalTime.of(12, 0), LocalTime.of(17, 59)),
        EVENING("Evening", LocalTime.of(18, 0), LocalTime.of(23, 59)),
        ANYTIME("Anytime", LocalTime.MIN, LocalTime.MAX);

        final String label;
        final LocalTime start;
        final LocalTime end;

        TimeOfDay(String label, LocalTime start, LocalTime end) {
            this.label = label;
            this.start = start;
            this.end = end;
        }

        boolean matches(LocalTime time) {
            return !time.isBefore(start) && !time.isAfter(end);
        }
    }

    public void display(ScheduleFlightIndex index, Scanner scanner) {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("TRAVEL PLANNER");
        System.out.println("=".repeat(80));
        System.out.println("Find direct and connecting flights between airports.");
        System.out.println();

        // Get origin
        System.out.print("Enter origin airport code (3 letters): ");
        String origin = scanner.nextLine().trim().toUpperCase();
        if (origin.length() != 3) {
            System.out.println("\nInvalid airport code. Must be 3 letters.");
            return;
        }
        if (!InputValidator.validateAirportCode(origin)) {
            return;
        }

        // Get destination
        System.out.print("Enter destination airport code (3 letters): ");
        String destination = scanner.nextLine().trim().toUpperCase();
        if (destination.length() != 3) {
            System.out.println("\nInvalid airport code. Must be 3 letters.");
            return;
        }
        if (!InputValidator.validateAirportCode(destination)) {
            return;
        }

        if (origin.equals(destination)) {
            System.out.println("\nOrigin and destination cannot be the same.");
            return;
        }

        // Get travel date
        System.out.print("Enter travel date (yyyy-MM-dd): ");
        String dateInput = scanner.nextLine().trim();
        Optional<LocalDate> travelDateOpt = InputValidator.validateTravelDate(dateInput);
        if (travelDateOpt.isEmpty()) {
            return;
        }
        LocalDate travelDate = travelDateOpt.get();

        // Get time of day preference
        System.out.print("Preferred departure time (Morning/Afternoon/Evening or press Enter for anytime): ");
        String timeInput = scanner.nextLine().trim();
        TimeOfDay timeOfDay = parseTimeOfDay(timeInput);

        findFlights(index, origin, destination, travelDate, timeOfDay);
    }

    private TimeOfDay parseTimeOfDay(String input) {
        if (input.isEmpty()) {
            return TimeOfDay.ANYTIME;
        }
        String normalized = input.toUpperCase();
        if (normalized.startsWith("M")) {
            return TimeOfDay.MORNING;
        } else if (normalized.startsWith("A")) {
            return TimeOfDay.AFTERNOON;
        } else if (normalized.startsWith("E")) {
            return TimeOfDay.EVENING;
        }
        return TimeOfDay.ANYTIME;
    }

    private void findFlights(ScheduleFlightIndex index, String origin, String destination,
                            LocalDate travelDate, TimeOfDay timeOfDay) {

        String originCity = AIRPORT_MAPPER.getAirportCity(origin);
        String destCity = AIRPORT_MAPPER.getAirportCity(destination);

        System.out.println("\n" + "=".repeat(80));
        System.out.printf("TRAVEL OPTIONS: %s (%s) → %s (%s)%n", origin, originCity, destination, destCity);
        System.out.printf("Date: %s | Time Preference: %s%n", travelDate, timeOfDay.label);
        System.out.println("=".repeat(80));

        // Find direct flights
        List<BookableFlight> directFlights = index.getFlightsByRouteOnDate(origin, destination, travelDate);
        List<FlightOption> options = new ArrayList<>();

        // Add direct flight options
        for (BookableFlight flight : directFlights) {
            if (timeOfDay.matches(flight.getScheduledDepartureTime())) {
                options.add(new FlightOption(flight));
            }
        }

        // If we don't have 10 options yet, find connecting flights
        if (options.size() < 10) {
            List<FlightOption> connectingOptions = findConnectingFlights(
                index, origin, destination, travelDate, timeOfDay);
            options.addAll(connectingOptions);
        }

        // Sort by departure time and limit to top 10
        options.sort(Comparator.comparing(FlightOption::getDepartureTime));
        List<FlightOption> topOptions = options.stream().limit(10).toList();

        if (topOptions.isEmpty()) {
            System.out.println("\n✗ No flights found matching your criteria.");
            System.out.println("\nTry:");
            System.out.println("  • Different travel date");
            System.out.println("  • Different time preference");
            return;
        }

        displayFlightOptions(topOptions);
    }

    private List<FlightOption> findConnectingFlights(ScheduleFlightIndex index, String origin,
                                                     String destination, LocalDate travelDate,
                                                     TimeOfDay timeOfDay) {
        List<FlightOption> connectingOptions = new ArrayList<>();

        // Build route graph for this date
        ScheduleDateRouteIndex dateIndex = new ScheduleDateRouteIndex(index, travelDate);

        if (dateIndex.getActualRoutes().isEmpty()) {
            return connectingOptions;
        }

        RouteGraphService graphService = new RouteGraphService(dateIndex);
        GraphPath<String, DefaultWeightedEdge> path = graphService.findShortestPath(origin, destination);

        if (path == null || path.getVertexList().size() <= 2) {
            return connectingOptions; // No indirect route or only direct
        }

        // Find all possible connections along the path
        List<String> airports = path.getVertexList();

        // For now, only handle single-connection flights (2 segments)
        if (airports.size() == 3) {
            String layover = airports.get(1);
            List<BookableFlight> firstLeg = dateIndex.getFlightsForRoute(origin, layover);
            List<BookableFlight> secondLeg = dateIndex.getFlightsForRoute(layover, destination);

            for (BookableFlight leg1 : firstLeg) {
                if (!timeOfDay.matches(leg1.getScheduledDepartureTime())) {
                    continue;
                }

                for (BookableFlight leg2 : secondLeg) {
                    long layoverMinutes = calculateLayoverMinutes(
                        leg1.getScheduledArrivalTime(),
                        leg2.getScheduledDepartureTime());

                    if (layoverMinutes >= MIN_LAYOVER_MINUTES &&
                        layoverMinutes <= MAX_LAYOVER_MINUTES) {
                        connectingOptions.add(new FlightOption(leg1, leg2, layoverMinutes));
                    }
                }
            }
        }

        return connectingOptions;
    }

    private void displayFlightOptions(List<FlightOption> options) {
        System.out.println("\nTOP FLIGHT OPTIONS:");
        System.out.println("=".repeat(80));

        int rank = 1;
        for (FlightOption option : options) {
            System.out.printf("%n%d. ", rank++);

            if (option.isDirect()) {
                displayDirectOption(option);
            } else {
                displayConnectionOption(option);
            }
        }

        System.out.println("\n" + "=".repeat(80));
        System.out.printf("Showing %d option%s (%d direct, %d with connections)%n",
            options.size(),
            options.size() == 1 ? "" : "s",
            options.stream().filter(FlightOption::isDirect).count(),
            options.stream().filter(o -> !o.isDirect()).count());
    }

    private void displayDirectOption(FlightOption option) {
        BookableFlight flight = option.firstFlight;
        String carrierName = CARRIER_MAPPER.getCarrierName(flight.getCarrierCode());
        String duration = calculateDuration(flight.getScheduledDepartureTime(),
                                           flight.getScheduledArrivalTime());

        System.out.printf("DIRECT - %s %s (%s)%n",
            flight.getCarrierCode(), flight.getFlightNumber(), carrierName);
        System.out.printf("   Departs: %s | Arrives: %s | Duration: %s%n",
            flight.getScheduledDepartureTime(),
            flight.getScheduledArrivalTime(),
            duration);
    }

    private void displayConnectionOption(FlightOption option) {
        BookableFlight leg1 = option.firstFlight;
        BookableFlight leg2 = option.secondFlight;
        String carrier1Name = CARRIER_MAPPER.getCarrierName(leg1.getCarrierCode());
        String carrier2Name = CARRIER_MAPPER.getCarrierName(leg2.getCarrierCode());
        String layoverAirport = leg1.getDestinationAirport();
        String layoverCity = AIRPORT_MAPPER.getAirportCity(layoverAirport);

        String totalDuration = calculateDuration(leg1.getScheduledDepartureTime(),
                                                leg2.getScheduledArrivalTime());

        System.out.printf("1 STOP via %s (%s) - Total time: %s%n",
            layoverAirport, layoverCity, totalDuration);

        System.out.printf("   Leg 1: %s %s (%s) | %s - %s%n",
            leg1.getCarrierCode(), leg1.getFlightNumber(), carrier1Name,
            leg1.getScheduledDepartureTime(), leg1.getScheduledArrivalTime());

        System.out.printf("   Layover: %s", formatLayover(option.layoverMinutes));
        if (option.layoverMinutes < WARN_LAYOVER_MINUTES) {
            System.out.print(" ⚠ TIGHT");
        }
        System.out.println();

        System.out.printf("   Leg 2: %s %s (%s) | %s - %s%n",
            leg2.getCarrierCode(), leg2.getFlightNumber(), carrier2Name,
            leg2.getScheduledDepartureTime(), leg2.getScheduledArrivalTime());
    }

    private long calculateLayoverMinutes(LocalTime arrival, LocalTime departure) {
        return FlightTimeUtils.calculateLayoverMinutes(arrival, departure);
    }

    private String calculateDuration(LocalTime departure, LocalTime arrival) {
        return FlightTimeUtils.calculateDuration(departure, arrival);
    }

    private String formatLayover(long minutes) {
        return FlightTimeUtils.formatLayover(minutes);
    }

    /**
     * Represents a flight option (direct or with connections)
     */
    private static class FlightOption {
        final BookableFlight firstFlight;
        final BookableFlight secondFlight; // null for direct flights
        final long layoverMinutes;

        // Direct flight constructor
        FlightOption(BookableFlight flight) {
            this.firstFlight = flight;
            this.secondFlight = null;
            this.layoverMinutes = 0;
        }

        // Connecting flight constructor
        FlightOption(BookableFlight firstFlight, BookableFlight secondFlight, long layoverMinutes) {
            this.firstFlight = firstFlight;
            this.secondFlight = secondFlight;
            this.layoverMinutes = layoverMinutes;
        }

        boolean isDirect() {
            return secondFlight == null;
        }

        LocalTime getDepartureTime() {
            return firstFlight.getScheduledDepartureTime();
        }
    }
}
