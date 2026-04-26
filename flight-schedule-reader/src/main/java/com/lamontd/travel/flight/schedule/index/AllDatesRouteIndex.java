package com.lamontd.travel.flight.schedule.index;

import com.lamontd.travel.flight.index.RouteIndex;
import com.lamontd.travel.flight.mapper.AirportCodeMapper;
import com.lamontd.travel.flight.model.BookableFlight;
import com.lamontd.travel.flight.util.DistanceCalculator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * RouteIndex adapter that includes ALL flights regardless of operating date.
 * Used for "all dates" network analysis to show theoretical route coverage.
 *
 * Contrast with ScheduleDateRouteIndex which filters to a specific date.
 */
public class AllDatesRouteIndex implements RouteIndex {
    private static final Logger logger = LoggerFactory.getLogger(AllDatesRouteIndex.class);

    private final AirportCodeMapper airportMapper;
    private final DistanceCalculator distanceCalculator;

    // Cached data computed at construction
    private final Set<String> originAirports;
    private final Set<String> destinationAirports;
    private final Set<String> actualRoutes;

    /**
     * Creates a RouteIndex adapter that includes all flights across all dates.
     *
     * @param scheduleIndex The schedule index containing all flight data
     */
    public AllDatesRouteIndex(ScheduleFlightIndex scheduleIndex) {
        this.airportMapper = AirportCodeMapper.getDefault();
        this.distanceCalculator = new DistanceCalculator(airportMapper);

        // Pre-compute data from ALL flights (no date filtering)
        this.originAirports = scheduleIndex.getAllFlights().stream()
                .map(BookableFlight::getOriginAirport)
                .collect(Collectors.toSet());

        this.destinationAirports = scheduleIndex.getAllFlights().stream()
                .map(BookableFlight::getDestinationAirport)
                .collect(Collectors.toSet());

        this.actualRoutes = scheduleIndex.getAllFlights().stream()
                .map(f -> f.getOriginAirport() + "-" + f.getDestinationAirport())
                .collect(Collectors.toSet());

        logger.debug("AllDatesRouteIndex created: {} origins, {} destinations, {} routes",
                originAirports.size(), destinationAirports.size(), actualRoutes.size());
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
        // Verify the route exists in the data
        String routeKey = origin + "-" + destination;
        if (!actualRoutes.contains(routeKey)) {
            logger.debug("Route {} not found in schedule data", routeKey);
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
}
