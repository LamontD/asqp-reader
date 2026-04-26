package com.lamontd.travel.flight.schedule.service;

import com.lamontd.travel.flight.model.BookableFlight;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ScheduleFlightDataLoaderTest {

    private final ScheduleFlightDataLoader loader = new ScheduleFlightDataLoader();

    @Test
    void testLoadSingleFile(@TempDir Path tempDir) throws IOException {
        Path testFile = createTestFile(tempDir, "test1.csv", 3);

        List<BookableFlight> flights = loader.loadFromFile(testFile);

        assertEquals(3, flights.size());
    }

    @Test
    void testLoadMultipleFiles(@TempDir Path tempDir) throws IOException {
        Path file1 = createTestFile(tempDir, "test1.csv", 2);
        Path file2 = createTestFile(tempDir, "test2.csv", 3);
        Path file3 = createTestFile(tempDir, "test3.csv", 5);

        List<BookableFlight> flights = loader.loadFromFiles(List.of(file1, file2, file3));

        assertEquals(10, flights.size());
    }

    @Test
    void testLoadEmptyFileList() throws IOException {
        List<BookableFlight> flights = loader.loadFromFiles(List.of());

        assertEquals(0, flights.size());
    }

    @Test
    void testLoadNonExistentFile(@TempDir Path tempDir) {
        Path nonExistent = tempDir.resolve("does-not-exist.csv");

        IOException exception = assertThrows(IOException.class, () -> {
            loader.loadFromFile(nonExistent);
        });

        assertTrue(exception.getMessage().contains("File not found"));
    }

    @Test
    void testLoadDirectory(@TempDir Path tempDir) {
        IOException exception = assertThrows(IOException.class, () -> {
            loader.loadFromFile(tempDir);
        });

        assertTrue(exception.getMessage().contains("Not a regular file"));
    }

    @Test
    void testParallelLoadingWithManyFiles(@TempDir Path tempDir) throws IOException {
        // Create 10 files with varying sizes
        List<Path> files = new java.util.ArrayList<>();
        for (int i = 0; i < 10; i++) {
            files.add(createTestFile(tempDir, "test" + i + ".csv", i + 1));
        }

        List<BookableFlight> flights = loader.loadFromFiles(files);

        // Sum: 1+2+3+4+5+6+7+8+9+10 = 55
        assertEquals(55, flights.size());
    }

    @Test
    void testLoadFileWithInvalidRecords(@TempDir Path tempDir) throws IOException {
        Path testFile = tempDir.resolve("mixed.csv");
        String csv = """
                carrier_code|flight_number|origin|destination|departure_date|scheduled_departure|scheduled_arrival
                WN|100|LAX|SFO|20250115|1000|1130
                ||LAX|SFO|20250115|1000|1130
                AA|200|JFK|LAX|20250116|800|1130
                """;
        Files.writeString(testFile, csv);

        List<BookableFlight> flights = loader.loadFromFile(testFile);

        // Should load 2 valid records and skip 1 invalid
        assertEquals(2, flights.size());
    }

    private Path createTestFile(Path dir, String filename, int recordCount) throws IOException {
        Path file = dir.resolve(filename);
        StringBuilder csv = new StringBuilder();
        csv.append("carrier_code|flight_number|origin|destination|departure_date|scheduled_departure|scheduled_arrival\n");

        for (int i = 0; i < recordCount; i++) {
            int day = 15 + i;
            csv.append(String.format("WN|%d|LAX|SFO|202501%02d|1000|1130\n", 100 + i, day));
        }

        Files.writeString(file, csv.toString());
        return file;
    }
}
