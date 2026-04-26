package com.lamontd.travel.flight.schedule.index;

import com.lamontd.travel.flight.index.RouteIndex;
import com.lamontd.travel.flight.mapper.AirportCodeMapper;
import com.lamontd.travel.flight.model.BookableFlight;
import com.lamontd.travel.flight.util.DistanceCalculator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Adapter that makes ScheduleFlightIndex compatible with RouteGraphService
 * by implementing the RouteIndex interface for a specific date.
 *
 * This enables date-specific route graph analysis and shortest path queries.
 */
public class ScheduleDateRouteIndex implements RouteIndex {
    private static final Logger logger = LoggerFactory.getLogger(ScheduleDateRouteIndex.class);

    private final ScheduleFlightIndex scheduleIndex;
    private final LocalDate targetDate;
    private final AirportCodeMapper airportMapper;
    private final DistanceCalculator distanceCalculator;

    // Cached data computed at construction
    private final Set<String> originAirports;
    private final Set<String> destinationAirports;
    private final Set<String> actualRoutes;

    /**
     * Creates a date-specific RouteIndex adapter.
     *
     * @param scheduleIndex The schedule index containing all flight data
     * @param targetDate The date to filter flights for
     */
    public ScheduleDateRouteIndex(ScheduleFlightIndex scheduleIndex, LocalDate targetDate) {
        this.scheduleIndex = scheduleIndex;
        this.targetDate = targetDate;
        this.airportMapper = AirportCodeMapper.getDefault();
        this.distanceCalculator = new DistanceCalculator(airportMapper);

        // Pre-compute date-specific data
        List<BookableFlight> dateFlights = scheduleIndex.getFlightsOnDate(targetDate);

        this.originAirports = dateFlights.stream()
                .map(BookableFlight::getOriginAirport)
                .collect(Collectors.toSet());

        this.destinationAirports = dateFlights.stream()
                .map(BookableFlight::getDestinationAirport)
                .collect(Collectors.toSet());

        this.actualRoutes = dateFlights.stream()
                .map(f -> f.getOriginAirport() + "-" + f.getDestinationAirport())
                .collect(Collectors.toSet());

        logger.debug("ScheduleDateRouteIndex created for {}: {} origins, {} destinations, {} routes",
                targetDate, originAirports.size(), destinationAirports.size(), actualRoutes.size());
    }

    @Override
    public Set<String> getOriginAirports() {
        return originAirports;
    }

    @Override
    public Set<String> getDestinationAirports() {
        return destinationAirports;
    }

    @Override
    public Set<String> getActualRoutes() {
        return actualRoutes;
    }

    @Override
    public double getRouteDistance(String origin, String destination) {
        // Verify the route actually operates on this date
        String routeKey = origin + "-" + destination;
        if (!actualRoutes.contains(routeKey)) {
            logger.debug("Route {} not found on date {}", routeKey, targetDate);
            return 0.0;
        }

        // Calculate distance using DistanceCalculator
        try {
            return distanceCalculator.calculateRouteDistance(routeKey);
        } catch (Exception e) {
            logger.warn("Could not calculate distance for route {}: {}", routeKey, e.getMessage());
            return 0.0;
        }
    }

    /**
     * Gets the target date this index was created for.
     */
    public LocalDate getTargetDate() {
        return targetDate;
    }

    /**
     * Gets all flights operating on the target date for a specific route.
     */
    public List<BookableFlight> getFlightsForRoute(String origin, String destination) {
        return scheduleIndex.getFlightsByRouteOnDate(origin, destination, targetDate);
    }
}
