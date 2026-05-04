package com.lamontd.travel.flight.mapper;

import com.lamontd.travel.flight.model.USCity;
import com.lamontd.travel.flight.util.PerformanceTimer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

/**
 * Mapper for US city data from SimpleMaps database.
 * Provides city lookup by name and state.
 */
public class CityMapper {
    private static final Logger logger = LoggerFactory.getLogger(CityMapper.class);
    private static final String CITY_DATA_FILE = "/data/uscities.csv";

    private final Map<String, USCity> cityIndex;
    private final Map<String, List<USCity>> stateIndex;
    private static CityMapper defaultInstance;

    private CityMapper(Map<String, USCity> cityIndex, Map<String, List<USCity>> stateIndex) {
        this.cityIndex = cityIndex;
        this.stateIndex = stateIndex;
    }

    /**
     * Returns the default city mapper instance loaded from embedded resource.
     */
    public static synchronized CityMapper getDefault() {
        if (defaultInstance == null) {
            try (var timer = new PerformanceTimer("Load city data")) {
                defaultInstance = loadFromResource(CITY_DATA_FILE);
            }
        }
        return defaultInstance;
    }

    /**
     * Loads city data from a classpath resource.
     */
    public static CityMapper loadFromResource(String resourcePath) {
        try (InputStream is = CityMapper.class.getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new IllegalStateException("City data file not found: " + resourcePath);
            }
            return loadFromStream(is);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load city data from " + resourcePath, e);
        }
    }

    /**
     * Loads city data from an input stream.
     */
    static CityMapper loadFromStream(InputStream inputStream) throws IOException {
        Map<String, USCity> cityIndex = new HashMap<>();
        Map<String, List<USCity>> stateIndex = new HashMap<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String headerLine = reader.readLine(); // Skip header

            String line;
            int count = 0;
            while ((line = reader.readLine()) != null) {
                try {
                    USCity city = parseCityLine(line);

                    // Index by "City, State" format
                    String key = city.city() + ", " + city.stateId();
                    cityIndex.put(key.toUpperCase(), city);

                    // Also index by full state name
                    String keyFull = city.city() + ", " + city.stateName();
                    cityIndex.put(keyFull.toUpperCase(), city);

                    // Index by state
                    stateIndex.computeIfAbsent(city.stateId(), k -> new ArrayList<>()).add(city);

                    count++;
                } catch (Exception e) {
                    logger.warn("Skipping invalid city record: {}", line.substring(0, Math.min(100, line.length())));
                }
            }

            logger.info("Loaded {} US cities", count);
            return new CityMapper(cityIndex, stateIndex);
        }
    }

    /**
     * Parses a CSV line into a USCity object.
     * Expected format: "city","city_ascii","state_id","state_name",...,"lat","lng","population",...
     */
    static USCity parseCityLine(String line) {
        // Simple CSV parsing - handles quoted fields
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (char c : line.toCharArray()) {
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                fields.add(current.toString());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        fields.add(current.toString());

        // Extract fields: city(0), state_id(2), state_name(3), lat(6), lng(7), population(8)
        String city = fields.get(0);
        String stateId = fields.get(2);
        String stateName = fields.get(3);
        double lat = Double.parseDouble(fields.get(6));
        double lng = Double.parseDouble(fields.get(7));
        int population = Integer.parseInt(fields.get(8));

        return new USCity(city, stateId, stateName, lat, lng, population);
    }

    /**
     * Finds a city by name and state.
     * Supports both state codes and full state names.
     * Case-insensitive.
     *
     * @param cityState Format: "City, State" (e.g., "Columbia, MD" or "Columbia, Maryland")
     * @return Optional containing the city if found
     */
    public Optional<USCity> findCity(String cityState) {
        return Optional.ofNullable(cityIndex.get(cityState.toUpperCase()));
    }

    /**
     * Finds all cities in a given state.
     *
     * @param stateId State code (e.g., "MD", "NY")
     * @return List of cities in the state
     */
    public List<USCity> getCitiesInState(String stateId) {
        return stateIndex.getOrDefault(stateId.toUpperCase(), List.of());
    }

    /**
     * Returns total number of cities in the database.
     */
    public int getCityCount() {
        return cityIndex.size() / 2; // Divide by 2 because each city is indexed twice
    }
}
