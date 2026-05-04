package com.lamontd.travel.flight.schedule.controller;

import com.lamontd.travel.flight.mapper.AirportCodeMapper;
import com.lamontd.travel.flight.mapper.CarrierCodeMapper;
import com.lamontd.travel.flight.mapper.CityMapper;
import com.lamontd.travel.flight.model.BookableFlight;
import com.lamontd.travel.flight.model.USCity;
import com.lamontd.travel.flight.schedule.index.ScheduleDateRouteIndex;
import com.lamontd.travel.flight.schedule.index.ScheduleFlightIndex;
import com.lamontd.travel.flight.schedule.util.FlightTimeUtils;
import com.lamontd.travel.flight.schedule.util.InputValidator;
import com.lamontd.travel.flight.service.CityAirportService;
import com.lamontd.travel.flight.service.RouteGraphService;
import org.jgrapht.GraphPath;
import org.jgrapht.graph.DefaultWeightedEdge;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

/**
 * Enhanced travel planner that finds both direct and connecting flights
 * with time-of-day filtering. Supports both one-way and round-trip searches.
 */
public class TravelPlannerController {
    private static final AirportCodeMapper AIRPORT_MAPPER = AirportCodeMapper.getDefault();
    private static final CarrierCodeMapper CARRIER_MAPPER = CarrierCodeMapper.getDefault();
    private static final CityMapper CITY_MAPPER = CityMapper.getDefault();
    private static final CityAirportService CITY_AIRPORT_SERVICE = new CityAirportService();

    private static final int MIN_LAYOVER_MINUTES = 60;
    private static final int MAX_LAYOVER_MINUTES = 360; // 6 hours
    private static final int WARN_LAYOVER_MINUTES = 90;
    private static final double DISTANCE_PENALTY = 0.5; // Points per mile to airport

    private enum TripType {
        ONE_WAY("One-way"),
        ROUND_TRIP("Round-trip");

        final String label;

        TripType(String label) {
            this.label = label;
        }
    }

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
        System.out.println("Find direct and connecting flights between destinations.");
        System.out.println();

        // Get search mode
        System.out.print("Search by (Airport/City or press Enter for airport): ");
        String searchModeInput = scanner.nextLine().trim();
        boolean isCityMode = searchModeInput.toUpperCase().startsWith("C");

        // Get trip type
        System.out.print("Trip type (One-way/Round-trip or press Enter for one-way): ");
        String tripTypeInput = scanner.nextLine().trim();
        TripType tripType = parseTripType(tripTypeInput);

        if (isCityMode) {
            displayCityMode(index, scanner, tripType);
        } else {
            displayAirportMode(index, scanner, tripType);
        }
    }

    private void displayAirportMode(ScheduleFlightIndex index, Scanner scanner, TripType tripType) {
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

        // Get departure date
        System.out.print("Enter departure date (yyyy-MM-dd): ");
        String dateInput = scanner.nextLine().trim();
        Optional<LocalDate> departureDateOpt = InputValidator.validateTravelDate(dateInput);
        if (departureDateOpt.isEmpty()) {
            return;
        }
        LocalDate departureDate = departureDateOpt.get();

        // Get departure time of day preference
        System.out.print("Preferred departure time (Morning/Afternoon/Evening or press Enter for anytime): ");
        String timeInput = scanner.nextLine().trim();
        TimeOfDay departureTimeOfDay = parseTimeOfDay(timeInput);

        if (tripType == TripType.ONE_WAY) {
            findFlights(index, origin, destination, departureDate, departureTimeOfDay);
        } else {
            // Get return date
            System.out.print("Enter return date (yyyy-MM-dd): ");
            String returnDateInput = scanner.nextLine().trim();
            Optional<LocalDate> returnDateOpt = InputValidator.validateReturnDate(departureDate, returnDateInput);
            if (returnDateOpt.isEmpty()) {
                return;
            }
            LocalDate returnDate = returnDateOpt.get();

            // Get return time of day preference
            System.out.print("Preferred return departure time (Morning/Afternoon/Evening or press Enter for anytime): ");
            String returnTimeInput = scanner.nextLine().trim();
            TimeOfDay returnTimeOfDay = parseTimeOfDay(returnTimeInput);

            findRoundTripFlights(index, origin, destination, departureDate, departureTimeOfDay,
                               returnDate, returnTimeOfDay);
        }
    }

    private void displayCityMode(ScheduleFlightIndex index, Scanner scanner, TripType tripType) {
        // Get origin city
        System.out.print("Enter origin city (format: City, State): ");
        String originCity = scanner.nextLine().trim();
        Optional<USCity> originOpt = CITY_MAPPER.findCity(originCity);
        if (originOpt.isEmpty()) {
            System.out.println("\n✗ City not found: " + originCity);
            System.out.println("Please use format: City, State (e.g., \"Columbia, MD\")");
            return;
        }

        // Get destination city
        System.out.print("Enter destination city (format: City, State): ");
        String destCity = scanner.nextLine().trim();
        Optional<USCity> destOpt = CITY_MAPPER.findCity(destCity);
        if (destOpt.isEmpty()) {
            System.out.println("\n✗ City not found: " + destCity);
            System.out.println("Please use format: City, State (e.g., \"New York, NY\")");
            return;
        }

        USCity origin = originOpt.get();
        USCity destination = destOpt.get();

        // Get departure date
        System.out.print("Enter departure date (yyyy-MM-dd): ");
        String dateInput = scanner.nextLine().trim();
        Optional<LocalDate> departureDateOpt = InputValidator.validateTravelDate(dateInput);
        if (departureDateOpt.isEmpty()) {
            return;
        }
        LocalDate departureDate = departureDateOpt.get();

        // Get departure time preference
        System.out.print("Preferred departure time (Morning/Afternoon/Evening or press Enter for anytime): ");
        String timeInput = scanner.nextLine().trim();
        TimeOfDay departureTimeOfDay = parseTimeOfDay(timeInput);

        if (tripType == TripType.ONE_WAY) {
            findFlightsFromCities(index, origin, destination, departureDate, departureTimeOfDay,
                                 null, null);
        } else {
            // Get return date
            System.out.print("Enter return date (yyyy-MM-dd): ");
            String returnDateInput = scanner.nextLine().trim();
            Optional<LocalDate> returnDateOpt = InputValidator.validateReturnDate(departureDate, returnDateInput);
            if (returnDateOpt.isEmpty()) {
                return;
            }
            LocalDate returnDate = returnDateOpt.get();

            // Get return time preference
            System.out.print("Preferred return departure time (Morning/Afternoon/Evening or press Enter for anytime): ");
            String returnTimeInput = scanner.nextLine().trim();
            TimeOfDay returnTimeOfDay = parseTimeOfDay(returnTimeInput);

            findFlightsFromCities(index, origin, destination, departureDate, departureTimeOfDay,
                                 returnDate, returnTimeOfDay);
        }
    }

    private TripType parseTripType(String input) {
        if (input.isEmpty()) {
            return TripType.ONE_WAY;
        }
        String normalized = input.toUpperCase();
        if (normalized.startsWith("R")) {
            return TripType.ROUND_TRIP;
        }
        return TripType.ONE_WAY;
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

    private void findFlightsFromCities(ScheduleFlightIndex index, USCity originCity, USCity destCity,
                                      LocalDate departureDate, TimeOfDay departureTimeOfDay,
                                      LocalDate returnDate, TimeOfDay returnTimeOfDay) {
        // Find nearby airports
        Set<String> availableOrigins = index.getAirportsWithFlights(departureDate);
        List<CityAirportService.AirportDistance> originAirports =
            CITY_AIRPORT_SERVICE.findNearbyAirports(originCity, availableOrigins);

        Set<String> availableDestinations = index.getAirportsWithFlights(departureDate);
        List<CityAirportService.AirportDistance> destAirports =
            CITY_AIRPORT_SERVICE.findNearbyAirports(destCity, availableDestinations);

        if (originAirports.isEmpty()) {
            System.out.println("\n✗ No airports with flights found near " + originCity.getCityState());
            return;
        }

        if (destAirports.isEmpty()) {
            System.out.println("\n✗ No airports with flights found near " + destCity.getCityState());
            return;
        }

        // Display header
        System.out.println("\n" + "=".repeat(80));
        if (returnDate == null) {
            System.out.printf("CITY-TO-CITY TRAVEL: %s → %s%n",
                originCity.getCityState(), destCity.getCityState());
        } else {
            System.out.printf("ROUND-TRIP CITY-TO-CITY: %s ⇄ %s%n",
                originCity.getCityState(), destCity.getCityState());
        }
        System.out.println("=".repeat(80));

        // Show available airports
        System.out.println("\nOrigin Airports:");
        for (CityAirportService.AirportDistance ad : originAirports) {
            System.out.printf("  %s - %s (%s)%n",
                ad.getAirportCode(), ad.getAirportCity(), ad.getFormattedDistance());
        }

        System.out.println("\nDestination Airports:");
        for (CityAirportService.AirportDistance ad : destAirports) {
            System.out.printf("  %s - %s (%s)%n",
                ad.getAirportCode(), ad.getAirportCity(), ad.getFormattedDistance());
        }
        System.out.println();

        if (returnDate == null) {
            // One-way: find top 5 flights
            findTopCityFlights(index, originAirports, destAirports, departureDate, departureTimeOfDay, 5);
        } else {
            // Round-trip: find top 5 outbound and top 5 return
            findTopCityFlightsRoundTrip(index, originAirports, destAirports,
                departureDate, departureTimeOfDay, returnDate, returnTimeOfDay);
        }
    }

    private void findTopCityFlights(ScheduleFlightIndex index,
                                   List<CityAirportService.AirportDistance> origins,
                                   List<CityAirportService.AirportDistance> destinations,
                                   LocalDate date, TimeOfDay timeOfDay, int limit) {
        List<ScoredFlightOption> scoredOptions = new ArrayList<>();

        // Search all origin/destination combinations
        for (CityAirportService.AirportDistance origin : origins) {
            for (CityAirportService.AirportDistance dest : destinations) {
                List<FlightOption> flightOptions = findFlightOptions(index,
                    origin.getAirportCode(), dest.getAirportCode(), date, timeOfDay);

                for (FlightOption option : flightOptions) {
                    double score = calculateFlightScore(option, origin.distance(), dest.distance());
                    scoredOptions.add(new ScoredFlightOption(option, score,
                        origin.getAirportCode(), origin.distance(),
                        dest.getAirportCode(), dest.distance()));
                }
            }
        }

        // Sort by score (higher is better) and take top N
        scoredOptions.sort(Comparator.comparingDouble(ScoredFlightOption::score).reversed());
        List<ScoredFlightOption> topFlights = scoredOptions.stream().limit(limit).toList();

        if (topFlights.isEmpty()) {
            System.out.println("✗ No flights found for selected criteria.");
            return;
        }

        System.out.println("TOP " + limit + " FLIGHT OPTIONS:");
        System.out.println("=".repeat(80));

        int rank = 1;
        for (ScoredFlightOption scored : topFlights) {
            System.out.printf("%n%d. ", rank++);
            displayScoredOption(scored);
        }

        System.out.println("\n" + "=".repeat(80));
    }

    private void findTopCityFlightsRoundTrip(ScheduleFlightIndex index,
                                            List<CityAirportService.AirportDistance> origins,
                                            List<CityAirportService.AirportDistance> destinations,
                                            LocalDate departureDate, TimeOfDay departureTime,
                                            LocalDate returnDate, TimeOfDay returnTime) {
        // Find top 5 outbound
        System.out.println("OUTBOUND FLIGHTS:");
        System.out.println("-".repeat(80));
        findTopCityFlights(index, origins, destinations, departureDate, departureTime, 5);

        System.out.println("\n\n");

        // Find top 5 return (swap origin/destination)
        System.out.println("RETURN FLIGHTS:");
        System.out.println("-".repeat(80));
        findTopCityFlights(index, destinations, origins, returnDate, returnTime, 5);
    }

    private double calculateFlightScore(FlightOption option, double originDistance, double destDistance) {
        // Base score: direct flights score higher
        double baseScore = option.isDirect() ? 100.0 : 80.0;

        // Apply distance penalty
        double distancePenalty = (originDistance + destDistance) * DISTANCE_PENALTY;

        return baseScore - distancePenalty;
    }

    private void displayScoredOption(ScoredFlightOption scored) {
        FlightOption option = scored.option();
        BookableFlight flight = option.firstFlight;

        if (option.isDirect()) {
            String carrierName = CARRIER_MAPPER.getCarrierName(flight.getCarrierCode());
            String duration = calculateDuration(
                flight.getScheduledDepartureTime(),
                flight.getScheduledArrivalTime(),
                flight.getOriginAirport(),
                flight.getDestinationAirport(),
                flight.getOperatingDate());

            System.out.printf("DIRECT - %s %s (%s) [%s → %s]%n",
                flight.getCarrierCode(), flight.getFlightNumber(), carrierName,
                scored.originAirport(), scored.destAirport());
            System.out.printf("   Distance to origin: %s | Departs: %s | Arrives: %s | Duration: %s%n",
                scored.formatOriginDistance(),
                flight.getScheduledDepartureTime(),
                flight.getScheduledArrivalTime(),
                duration);
        } else {
            BookableFlight leg2 = option.secondFlight;
            String carrier1Name = CARRIER_MAPPER.getCarrierName(flight.getCarrierCode());
            String carrier2Name = CARRIER_MAPPER.getCarrierName(leg2.getCarrierCode());
            String layoverAirport = flight.getDestinationAirport();
            String layoverCity = AIRPORT_MAPPER.getAirportCity(layoverAirport);

            String totalDuration = calculateDuration(
                flight.getScheduledDepartureTime(),
                leg2.getScheduledArrivalTime(),
                flight.getOriginAirport(),
                leg2.getDestinationAirport(),
                flight.getOperatingDate());

            System.out.printf("1 STOP via %s (%s) - Total: %s [%s → %s]%n",
                layoverAirport, layoverCity, totalDuration,
                scored.originAirport(), scored.destAirport());
            System.out.printf("   Distance to origin: %s%n", scored.formatOriginDistance());
            System.out.printf("   Leg 1: %s %s (%s) | %s - %s%n",
                flight.getCarrierCode(), flight.getFlightNumber(), carrier1Name,
                flight.getScheduledDepartureTime(), flight.getScheduledArrivalTime());
            System.out.printf("   Layover: %s", formatLayover(option.layoverMinutes));
            if (option.layoverMinutes < WARN_LAYOVER_MINUTES) {
                System.out.print(" ⚠ TIGHT");
            }
            System.out.println();
            System.out.printf("   Leg 2: %s %s (%s) | %s - %s%n",
                leg2.getCarrierCode(), leg2.getFlightNumber(), carrier2Name,
                leg2.getScheduledDepartureTime(), leg2.getScheduledArrivalTime());
        }
    }

    private record ScoredFlightOption(
        FlightOption option,
        double score,
        String originAirport,
        double originDistance,
        String destAirport,
        double destDistance
    ) {
        String formatOriginDistance() {
            return String.format("%.0fmi", originDistance);
        }

        String formatDestDistance() {
            return String.format("%.0fmi", destDistance);
        }
    }

    private void findRoundTripFlights(ScheduleFlightIndex index, String origin, String destination,
                                     LocalDate departureDate, TimeOfDay departureTimeOfDay,
                                     LocalDate returnDate, TimeOfDay returnTimeOfDay) {

        String originCity = AIRPORT_MAPPER.getAirportCity(origin);
        String destCity = AIRPORT_MAPPER.getAirportCity(destination);

        System.out.println("\n" + "=".repeat(80));
        System.out.printf("ROUND-TRIP OPTIONS: %s (%s) ⇄ %s (%s)%n", origin, originCity, destination, destCity);
        System.out.printf("Outbound: %s (%s) | Return: %s (%s)%n",
                         departureDate, departureTimeOfDay.label,
                         returnDate, returnTimeOfDay.label);
        System.out.println("=".repeat(80));

        // Find outbound flights
        List<FlightOption> outboundOptions = findFlightOptions(
            index, origin, destination, departureDate, departureTimeOfDay);

        // Find return flights (destination → origin)
        List<FlightOption> returnOptions = findFlightOptions(
            index, destination, origin, returnDate, returnTimeOfDay);

        if (outboundOptions.isEmpty()) {
            System.out.println("\n✗ No outbound flights found matching your criteria.");
            System.out.println("\nTry:");
            System.out.println("  • Different departure date");
            System.out.println("  • Different departure time preference");
            return;
        }

        if (returnOptions.isEmpty()) {
            System.out.println("\n✗ No return flights found matching your criteria.");
            System.out.println("\nTry:");
            System.out.println("  • Different return date");
            System.out.println("  • Different return time preference");
            return;
        }

        // Create round-trip combinations
        List<RoundTripOption> roundTripOptions = new ArrayList<>();

        // Take top 10 of each to limit combinations
        List<FlightOption> topOutbound = outboundOptions.stream().limit(10).toList();
        List<FlightOption> topReturn = returnOptions.stream().limit(10).toList();

        for (FlightOption outbound : topOutbound) {
            for (FlightOption returnFlight : topReturn) {
                roundTripOptions.add(new RoundTripOption(outbound, returnFlight));
            }
        }

        // Sort by complexity (prefer direct/direct) then by departure time
        roundTripOptions.sort(Comparator
            .comparing(RoundTripOption::getComplexityScore)
            .thenComparing(RoundTripOption::getOutboundDepartureTime));

        // Limit to top 10 round-trip combinations
        List<RoundTripOption> topRoundTrips = roundTripOptions.stream().limit(10).toList();

        displayRoundTripOptions(topRoundTrips);
    }

    private List<FlightOption> findFlightOptions(ScheduleFlightIndex index, String origin,
                                                 String destination, LocalDate date,
                                                 TimeOfDay timeOfDay) {
        List<FlightOption> options = new ArrayList<>();

        // Find direct flights
        List<BookableFlight> directFlights = index.getFlightsByRouteOnDate(origin, destination, date);
        for (BookableFlight flight : directFlights) {
            if (timeOfDay.matches(flight.getScheduledDepartureTime())) {
                options.add(new FlightOption(flight));
            }
        }

        // Find connecting flights
        List<FlightOption> connectingOptions = findConnectingFlights(
            index, origin, destination, date, timeOfDay);
        options.addAll(connectingOptions);

        // Sort by departure time
        options.sort(Comparator.comparing(FlightOption::getDepartureTime));

        return options;
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
                        leg2.getScheduledDepartureTime(),
                        layover);

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

    private void displayRoundTripOptions(List<RoundTripOption> options) {
        System.out.println("\nTOP ROUND-TRIP OPTIONS:");
        System.out.println("=".repeat(80));

        int rank = 1;
        for (RoundTripOption option : options) {
            System.out.printf("%n%d. ROUND-TRIP OPTION%n", rank++);
            System.out.println();

            // Display outbound
            BookableFlight outboundFirst = option.outbound.firstFlight;
            String originCity = AIRPORT_MAPPER.getAirportCity(outboundFirst.getOriginAirport());
            String destCity = AIRPORT_MAPPER.getAirportCity(outboundFirst.getDestinationAirport());

            System.out.printf("   OUTBOUND: %s - %s (%s) → %s (%s)%n",
                outboundFirst.getOperatingDate(),
                outboundFirst.getOriginAirport(), originCity,
                outboundFirst.getDestinationAirport(), destCity);

            if (option.outbound.isDirect()) {
                displayDirectOptionIndented(option.outbound);
            } else {
                displayConnectionOptionIndented(option.outbound);
            }

            System.out.println();

            // Display return
            BookableFlight returnFirst = option.returnFlight.firstFlight;
            String returnOriginCity = AIRPORT_MAPPER.getAirportCity(returnFirst.getOriginAirport());
            String returnDestCity = AIRPORT_MAPPER.getAirportCity(returnFirst.getDestinationAirport());

            System.out.printf("   RETURN: %s - %s (%s) → %s (%s)%n",
                returnFirst.getOperatingDate(),
                returnFirst.getOriginAirport(), returnOriginCity,
                returnFirst.getDestinationAirport(), returnDestCity);

            if (option.returnFlight.isDirect()) {
                displayDirectOptionIndented(option.returnFlight);
            } else {
                displayConnectionOptionIndented(option.returnFlight);
            }
        }

        System.out.println("\n" + "=".repeat(80));

        // Count combinations by type
        long directDirect = options.stream()
            .filter(o -> o.outbound.isDirect() && o.returnFlight.isDirect()).count();
        long directConnection = options.stream()
            .filter(o -> o.outbound.isDirect() && !o.returnFlight.isDirect()).count();
        long connectionDirect = options.stream()
            .filter(o -> !o.outbound.isDirect() && o.returnFlight.isDirect()).count();
        long connectionConnection = options.stream()
            .filter(o -> !o.outbound.isDirect() && !o.returnFlight.isDirect()).count();

        System.out.printf("Showing %d round-trip option%s:%n",
            options.size(), options.size() == 1 ? "" : "s");
        if (directDirect > 0) {
            System.out.printf("  • %d direct/direct%n", directDirect);
        }
        if (directConnection > 0) {
            System.out.printf("  • %d direct outbound / connecting return%n", directConnection);
        }
        if (connectionDirect > 0) {
            System.out.printf("  • %d connecting outbound / direct return%n", connectionDirect);
        }
        if (connectionConnection > 0) {
            System.out.printf("  • %d connecting/connecting%n", connectionConnection);
        }
    }

    private void displayDirectOptionIndented(FlightOption option) {
        BookableFlight flight = option.firstFlight;
        String carrierName = CARRIER_MAPPER.getCarrierName(flight.getCarrierCode());
        String duration = calculateDuration(
            flight.getScheduledDepartureTime(),
            flight.getScheduledArrivalTime(),
            flight.getOriginAirport(),
            flight.getDestinationAirport(),
            flight.getOperatingDate());

        System.out.printf("   DIRECT - %s %s (%s)%n",
            flight.getCarrierCode(), flight.getFlightNumber(), carrierName);
        System.out.printf("      Departs: %s | Arrives: %s | Duration: %s%n",
            flight.getScheduledDepartureTime(),
            flight.getScheduledArrivalTime(),
            duration);
    }

    private void displayConnectionOptionIndented(FlightOption option) {
        BookableFlight leg1 = option.firstFlight;
        BookableFlight leg2 = option.secondFlight;
        String carrier1Name = CARRIER_MAPPER.getCarrierName(leg1.getCarrierCode());
        String carrier2Name = CARRIER_MAPPER.getCarrierName(leg2.getCarrierCode());
        String layoverAirport = leg1.getDestinationAirport();
        String layoverCity = AIRPORT_MAPPER.getAirportCity(layoverAirport);

        // Calculate total duration from first departure to final arrival
        String totalDuration = calculateDuration(
            leg1.getScheduledDepartureTime(),
            leg2.getScheduledArrivalTime(),
            leg1.getOriginAirport(),
            leg2.getDestinationAirport(),
            leg1.getOperatingDate());

        System.out.printf("   1 STOP via %s (%s) - Total time: %s%n",
            layoverAirport, layoverCity, totalDuration);

        System.out.printf("      Leg 1: %s %s (%s) | %s - %s%n",
            leg1.getCarrierCode(), leg1.getFlightNumber(), carrier1Name,
            leg1.getScheduledDepartureTime(), leg1.getScheduledArrivalTime());

        System.out.printf("      Layover: %s", formatLayover(option.layoverMinutes));
        if (option.layoverMinutes < WARN_LAYOVER_MINUTES) {
            System.out.print(" ⚠ TIGHT");
        }
        System.out.println();

        System.out.printf("      Leg 2: %s %s (%s) | %s - %s%n",
            leg2.getCarrierCode(), leg2.getFlightNumber(), carrier2Name,
            leg2.getScheduledDepartureTime(), leg2.getScheduledArrivalTime());
    }

    private void displayDirectOption(FlightOption option) {
        BookableFlight flight = option.firstFlight;
        String carrierName = CARRIER_MAPPER.getCarrierName(flight.getCarrierCode());
        String duration = calculateDuration(
            flight.getScheduledDepartureTime(),
            flight.getScheduledArrivalTime(),
            flight.getOriginAirport(),
            flight.getDestinationAirport(),
            flight.getOperatingDate());

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

        // Calculate total duration from first departure to final arrival
        String totalDuration = calculateDuration(
            leg1.getScheduledDepartureTime(),
            leg2.getScheduledArrivalTime(),
            leg1.getOriginAirport(),
            leg2.getDestinationAirport(),
            leg1.getOperatingDate());

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

    private long calculateLayoverMinutes(LocalTime arrival, LocalTime departure, String airportCode) {
        double airportTz = AIRPORT_MAPPER.getAirportInfo(airportCode)
            .flatMap(info -> info.getTimezone())
            .orElse(0.0);
        return FlightTimeUtils.calculateLayoverMinutes(arrival, airportTz, departure, airportTz);
    }

    private String calculateDuration(LocalTime departure, LocalTime arrival,
                                     String originCode, String destCode, LocalDate operatingDate) {
        double originTz = AIRPORT_MAPPER.getAirportInfo(originCode)
            .flatMap(info -> info.getTimezone())
            .orElse(0.0);
        double destTz = AIRPORT_MAPPER.getAirportInfo(destCode)
            .flatMap(info -> info.getTimezone())
            .orElse(0.0);

        return FlightTimeUtils.calculateDuration(departure, originTz, arrival, destTz, operatingDate);
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

    /**
     * Represents a round-trip option (outbound + return)
     */
    private static class RoundTripOption {
        final FlightOption outbound;
        final FlightOption returnFlight;

        RoundTripOption(FlightOption outbound, FlightOption returnFlight) {
            this.outbound = outbound;
            this.returnFlight = returnFlight;
        }

        /**
         * Returns complexity score: 0 for direct/direct, 1 for one connection, 2 for two connections
         */
        int getComplexityScore() {
            int score = 0;
            if (!outbound.isDirect()) score++;
            if (!returnFlight.isDirect()) score++;
            return score;
        }

        LocalTime getOutboundDepartureTime() {
            return outbound.getDepartureTime();
        }

        LocalTime getReturnDepartureTime() {
            return returnFlight.getDepartureTime();
        }
    }
}
