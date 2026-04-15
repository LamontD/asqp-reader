# Build and Deployment Guide

Complete guide for building and deploying the Flight Data Analysis multi-module project.

## Quick Start

```bash
# Build all modules
mvn clean install

# Run the application
java -jar asqp-reader/target/asqp-reader.jar <csv-file>
```

---

## Multi-Module Build

This project uses a Maven multi-module structure with two modules:
- `flight-core`: Reusable library with core models and utilities
- `asqp-reader`: Interactive CLI application (depends on flight-core)

### Full Build (All Modules)

```bash
mvn clean install
```

This will:
1. Build and install `flight-core` to local Maven repository
2. Build `asqp-reader` (depends on installed `flight-core`)
3. Run all 89 tests (50 in flight-core, 39 in asqp-reader)
4. Create executable JAR with all dependencies

**Build time:** ~10-15 seconds

### Skip Tests (Faster)

```bash
mvn clean install -DskipTests
```

**Build time:** ~5-8 seconds

### Build Individual Modules

```bash
# Build flight-core only
cd flight-core
mvn clean install

# Build asqp-reader only (requires flight-core installed first)
cd asqp-reader
mvn clean package
```

### Build Output

After a full build from the root:

```
flight-core/target/
├── flight-core-1.0-SNAPSHOT.jar          # Library JAR
├── classes/                               # Compiled .class files
├── test-classes/                          # Compiled test files
└── surefire-reports/                      # Test reports (50 tests)

asqp-reader/target/
├── asqp-reader.jar                        # ⭐ USE THIS (~2 MB, executable with all dependencies)
├── asqp-reader-1.0-SNAPSHOT.jar           # Original without dependencies
├── classes/                               # Compiled .class files
├── test-classes/                          # Compiled test files
└── surefire-reports/                      # Test reports (39 tests)
```

**Always use:** `asqp-reader.jar` (the executable with all dependencies)

---

## Running

### Basic Usage

```bash
java -jar asqp-reader/target/asqp-reader.jar <csv-file-path> [csv-file-path2] [...]
```

### Examples

**Process one or more specific files:**
```bash
java -jar asqp-reader/target/asqp-reader.jar /path/to/flight-data.csv
java -jar asqp-reader/target/asqp-reader.jar file1.csv file2.csv file3.csv
```

**Run with sample data (no arguments):**
```bash
java -jar asqp-reader/target/asqp-reader.jar
```

**Run from any directory:**
```bash
java -jar /opt/apps/asqp-reader.jar data.csv
```

### Expected Output

```
Loading file...
Loaded 6033 airports from 7698 records
  ✓ Loaded 500 records from file1.csv
  ✓ Loaded 300 records from file2.csv

✓ Successfully loaded 800 total records from 2 file(s) in 245 ms
  Operated: 756 (94.5%), Cancelled: 44 (5.5%)
Loaded 193 countries

Reference data: 992 carriers, 6033 airports, 193 countries
Building data indices for efficient querying...
Computing route distances...
Indices built in 18 ms
Indexed: 2 carriers, 24 airports, 156 tail numbers, 32 flight numbers, 62 dates, 48 routes

==================================================
ASQP Flight Data Analysis Menu
==================================================
1. Data Overview
2. Carrier View
3. Airport View
4. Airplane View
5. Flight View
6. Filter by Date Range
7. Route Network Analysis (Shortest Path)
8. Flight Schedule Analysis
9. Exit
==================================================
Select an option (1-9):
```

---

## What's Included in the JAR

### Application Code
- **flight-core library**: Core models, mappers, readers, utilities
- **asqp-reader application**: Interactive CLI, views, controllers, services
- Data quality validation
- Route network analysis
- Flight schedule analysis

### Dependencies (Bundled)
- Apache Commons CSV 1.12.0
- Apache Commons IO 2.17.0
- Apache Commons Codec 1.17.1
- Google Gson 2.11.0
- JGraphT Core 1.5.2
- JHeaps 0.14
- Apfloat 1.10.1

### Resource Files (Embedded)
- `airlines.dat` - 992 carriers (OpenFlights)
- `airports.dat` - 6,033 airports (OpenFlights)
- `countries.json` - 193 countries (ISO 3166-1)
- `sample-data.asqpflightrecord.csv` - Sample flight data (500 records)

**Total Size:** ~2 MB (completely self-contained)

---

## Testing

### Run All Tests

```bash
mvn test
```

### Run Specific Test

```bash
mvn test -Dtest=CarrierCodeMapperTest
mvn test -Dtest=AirportCodeMapperTest
mvn test -Dtest=CountryCodeMapperTest
```

### Test Coverage

- **Total tests:** 89 (50 in flight-core, 39 in asqp-reader)
- **flight-core module:**
  - Carrier mapper: 14 tests
  - Airport mapper: 12 tests
  - Country mapper: 14 tests
  - Cancellation mapper: 10 tests
- **asqp-reader module:**
  - CSV reader: 14 tests
  - Flight delays: 4 tests
  - Distance calculations: 5 tests
  - Route graph service: 8 tests
  - Flight schedule service: 7 tests
  - Application: 1 test

---

## Deployment

### Option 1: Copy JAR Only

The JAR is completely self-contained:

```bash
# Copy to deployment directory
cp asqp-reader/target/asqp-reader.jar /opt/asqp-reader/

# Run from deployment directory
cd /opt/asqp-reader
java -jar asqp-reader.jar flight-data.csv
```

### Option 2: Create Wrapper Script

**Linux/Mac (asqp-reader.sh):**
```bash
#!/bin/bash
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
java -jar "$DIR/asqp-reader.jar" "$@"
```

Make executable:
```bash
chmod +x asqp-reader.sh
./asqp-reader.sh data.csv
```

**Windows (asqp-reader.bat):**
```batch
@echo off
java -jar "%~dp0asqp-reader.jar" %*
```

Usage:
```cmd
asqp-reader.bat data.csv
```

### Option 3: Distribution Package

```bash
mkdir asqp-reader-dist
cp asqp-reader/target/asqp-reader.jar asqp-reader-dist/
cp README.md asqp-reader-dist/
cp -r docs asqp-reader-dist/
zip -r asqp-reader-dist.zip asqp-reader-dist/
```

---

## Requirements

### Build Requirements
- Java 23 JDK
- Maven 3.6+
- Internet connection (first build only, for downloading dependencies)

### Runtime Requirements
- Java 23 JRE or JDK
- No other dependencies needed (JAR is self-contained)

### Check Java Version

```bash
java -version
```

Should show: `java version "23"` or higher

### If Java 23 Not Available

Modify `pom.xml` to target an earlier version:

```xml
<properties>
  <maven.compiler.target>17</maven.compiler.target>
  <maven.compiler.source>17</maven.compiler.source>
</properties>
```

Then rebuild:
```bash
mvn clean package
```

---

## Updating Reference Data

### Update Airlines/Airports

```bash
# Download latest OpenFlights data
curl -o flight-core/src/main/resources/data/airlines.dat \
  https://raw.githubusercontent.com/jpatokal/openflights/master/data/airlines.dat

curl -o flight-core/src/main/resources/data/airports.dat \
  https://raw.githubusercontent.com/jpatokal/openflights/master/data/airports.dat

# Test
mvn test

# Rebuild
mvn clean install

# Deploy new JAR
cp asqp-reader/target/asqp-reader.jar /deployment/path/
```

---

## Continuous Integration

### GitHub Actions Example

```yaml
name: Build and Test

on: [push, pull_request]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      
      - name: Set up JDK 23
        uses: actions/setup-java@v3
        with:
          java-version: '23'
          distribution: 'temurin'
      
      - name: Build with Maven
        run: mvn clean install
      
      - name: Upload JAR
        uses: actions/upload-artifact@v3
        with:
          name: asqp-reader
          path: asqp-reader/target/asqp-reader.jar
```

### Jenkins Pipeline Example

```groovy
pipeline {
    agent any
    stages {
        stage('Build') {
            steps {
                sh 'mvn clean install'
            }
        }
        stage('Test') {
            steps {
                sh 'mvn test'
            }
        }
        stage('Archive') {
            steps {
                archiveArtifacts artifacts: 'asqp-reader/target/asqp-reader.jar'
            }
        }
    }
}
```

---

## Scheduled Batch Jobs

### Linux Cron

```bash
# Run daily at 2 AM
0 2 * * * /usr/bin/java -jar /opt/asqp/asqp-reader.jar /data/daily/flights-$(date +\%Y\%m\%d).csv >> /var/log/asqp-reader.log 2>&1
```

### Windows Task Scheduler

Create a batch file:
```batch
@echo off
java -jar C:\Apps\asqp-reader.jar C:\Data\flights-%date:~-4,4%%date:~-10,2%%date:~-7,2%.csv >> C:\Logs\asqp-reader.log 2>&1
```

Schedule via Task Scheduler GUI or PowerShell:
```powershell
$action = New-ScheduledTaskAction -Execute "java" -Argument "-jar C:\Apps\asqp-reader.jar C:\Data\flights.csv"
$trigger = New-ScheduledTaskTrigger -Daily -At 2am
Register-ScheduledTask -Action $action -Trigger $trigger -TaskName "ASQP Reader" -Description "Daily flight data processing"
```

---

## Troubleshooting

### "No main manifest attribute"

The JAR wasn't built correctly. Rebuild:
```bash
mvn clean install
```

Ensure you're running the correct JAR:
```bash
java -jar asqp-reader/target/asqp-reader.jar  # Correct (~2 MB)
# NOT: asqp-reader/target/asqp-reader-1.0-SNAPSHOT.jar
```

### "UnsupportedClassVersionError"

Your Java version is too old. The JAR requires Java 23+.

Check version:
```bash
java -version
```

Install Java 23 from:
- https://adoptium.net/ (Temurin)
- https://www.oracle.com/java/technologies/downloads/

### "ClassNotFoundException"

This shouldn't happen with the shaded JAR. If it does:
1. Verify you're using `asqp-reader.jar` (not the SNAPSHOT version)
2. Rebuild with `mvn clean install`
3. Check build completed successfully for both modules

### "Resource not found" Errors

If you see errors loading airlines.dat, airports.dat, or countries.json:
1. Verify files exist in `flight-core/src/main/resources/data/`
2. Rebuild with `mvn clean install` (not just `mvn compile`)
3. Check JAR size is ~2 MB (should include resources)

### Build Fails

If Maven build fails:
```bash
# Clear local Maven cache
rm -rf ~/.m2/repository/com/lamontd

# Rebuild
mvn clean install
```

### Module Dependency Issues

If asqp-reader can't find flight-core:
```bash
# Install flight-core to local Maven repository first
cd flight-core
mvn clean install

# Then build asqp-reader
cd ../asqp-reader
mvn clean package
```

---

## Performance

### Build Performance
- Full build with tests: ~10-15 seconds
- Build without tests: ~5-8 seconds
- Clean build: ~10-20 seconds

### Runtime Performance
- JAR size: ~2 MB
- Startup time: ~0.5-1.0 seconds
- Memory usage: ~100-250 MB
- Load time: 
  - Airlines: ~50ms (992 carriers)
  - Airports: ~200ms (6,033 airports)
  - Countries: ~20ms (193 countries)
- Index build time: ~15-25ms (for 500 records)
- Route distance calculations: ~5-10ms (for 24 unique routes)

---

## Development vs Production

### Development

During development, use Maven exec:
```bash
cd asqp-reader
mvn exec:java -Dexec.mainClass="com.lamontd.travel.flight.asqp.App" -Dexec.args="data.csv"
```

Or your IDE's run configuration.

### Production

In production, use the JAR:
```bash
java -jar asqp-reader.jar data.csv
```

Benefits:
- Self-contained (includes flight-core library)
- No Maven needed
- Consistent environment
- Easy deployment

---

## Version Management

### Update Version

Edit `pom.xml`:
```xml
<version>1.1-SNAPSHOT</version>
```

Rebuild:
```bash
mvn clean package
```

The JAR will be named: `target/asqp-reader.jar` (name stays consistent due to `<finalName>`)

### Release Version

```bash
# Update version in pom.xml
<version>1.0</version>

# Build release
mvn clean package

# Tag in git
git tag -a v1.0 -m "Release version 1.0"
git push origin v1.0
```

---

## Maven Configuration

### Shade Plugin

The asqp-reader module uses `maven-shade-plugin` to create the executable JAR:

```xml
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-shade-plugin</artifactId>
  <version>3.6.0</version>
  <executions>
    <execution>
      <phase>package</phase>
      <goals>
        <goal>shade</goal>
      </goals>
      <configuration>
        <transformers>
          <transformer implementation="...ManifestResourceTransformer">
            <mainClass>com.lamontd.travel.flight.asqp.App</mainClass>
          </transformer>
        </transformers>
        <finalName>asqp-reader</finalName>
      </configuration>
    </execution>
  </executions>
</plugin>
```

This bundles flight-core and all dependencies into a single executable JAR.

### Build Warnings (Normal)

You may see warnings during build - these are normal:
```
[WARNING] commons-codec-1.17.1.jar, commons-csv-1.12.0.jar define 2 overlapping resources:
[WARNING]   - META-INF/LICENSE.txt
[WARNING]   - META-INF/NOTICE.txt
[WARNING] Discovered module-info.class. Shading will break its strong encapsulation.
```

Multiple libraries include the same license files and module-info classes. Maven Shade handles this automatically by merging or selecting one version.

---

## Summary

**Build command:**
```bash
mvn clean install
```

**Output:**
```
flight-core/target/flight-core-1.0-SNAPSHOT.jar (library)
asqp-reader/target/asqp-reader.jar (~2 MB, executable)
```

**Run:**
```bash
java -jar asqp-reader/target/asqp-reader.jar <file.csv>
```

**Deploy:**
Copy the asqp-reader.jar anywhere - it's completely self-contained!
