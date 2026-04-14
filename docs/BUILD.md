# Build and Deployment Guide

Complete guide for building and deploying the ASQP Reader.

## Quick Start

```bash
# Build
mvn clean package

# Run
java -jar target/asqp-reader.jar <csv-file>
```

---

## Building

### Full Build

```bash
mvn clean package
```

This will:
1. Clean previous builds
2. Compile the code
3. Run all tests
4. Create an executable JAR with all dependencies

**Build time:** ~10-15 seconds

### Skip Tests (Faster)

```bash
mvn clean package -DskipTests
```

**Build time:** ~5-8 seconds

### Build Output

The build creates:
```
target/
├── asqp-reader.jar                      # ⭐ USE THIS (1.7 MB)
├── asqp-reader-1.0-SNAPSHOT.jar         # Original without dependencies
├── classes/                              # Compiled .class files
├── surefire-reports/                     # Test reports
└── ... other build artifacts
```

**Always use:** `asqp-reader.jar` (the executable with all dependencies)

---

## Running

### Basic Usage

```bash
java -jar target/asqp-reader.jar <csv-file-path>
```

### Examples

**Process a specific file:**
```bash
java -jar target/asqp-reader.jar /path/to/flight-data.csv
```

**Run with sample data (no arguments):**
```bash
java -jar target/asqp-reader.jar
```

**Run from any directory:**
```bash
java -jar /opt/apps/asqp-reader.jar data.csv
```

### Expected Output

```
Loaded 6033 airports from 7698 records
Loaded 193 countries
Loaded 992 carriers, 6033 airports, and 193 countries

Reading flight records from: data.csv
Successfully loaded 500 flight records
Cancelled flights: 22
Operated flights: 478

=== Data Quality Checks ===
✓ All carrier codes found in database
✓ All airport codes found in database
...
```

---

## What's Included in the JAR

### Application Code
- Flight record reader
- Carrier, airport, country mappers
- Data quality validation
- CSV parsing logic

### Dependencies (Bundled)
- Apache Commons CSV 1.12.0
- Apache Commons IO 2.17.0
- Apache Commons Codec 1.17.1
- Google Gson 2.11.0

### Resource Files (Embedded)
- `airlines.dat` - 992 carriers (OpenFlights)
- `airports.dat` - 6,033 airports (OpenFlights)
- `countries.json` - 193 countries (ISO 3166-1)
- `sample-data.asc.groomed` - Sample flight data

**Total Size:** ~1.7 MB (completely self-contained)

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

- **Total tests:** 49
- **Coverage:**
  - Carrier mapper: 14 tests
  - Airport mapper: 12 tests
  - Country mapper: 14 tests
  - CSV reader: 8 tests
  - Application: 1 test

---

## Deployment

### Option 1: Copy JAR Only

The JAR is completely self-contained:

```bash
# Copy to deployment directory
cp target/asqp-reader.jar /opt/asqp-reader/

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
cp target/asqp-reader.jar asqp-reader-dist/
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
curl -o src/main/resources/data/airlines.dat \
  https://raw.githubusercontent.com/jpatokal/openflights/master/data/airlines.dat

curl -o src/main/resources/data/airports.dat \
  https://raw.githubusercontent.com/jpatokal/openflights/master/data/airports.dat

# Test
mvn test

# Rebuild
mvn clean package

# Deploy new JAR
cp target/asqp-reader.jar /deployment/path/
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
        run: mvn clean package
      
      - name: Upload JAR
        uses: actions/upload-artifact@v3
        with:
          name: asqp-reader
          path: target/asqp-reader.jar
```

### Jenkins Pipeline Example

```groovy
pipeline {
    agent any
    stages {
        stage('Build') {
            steps {
                sh 'mvn clean package'
            }
        }
        stage('Test') {
            steps {
                sh 'mvn test'
            }
        }
        stage('Archive') {
            steps {
                archiveArtifacts artifacts: 'target/asqp-reader.jar'
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
mvn clean package
```

Ensure you're running the correct JAR:
```bash
java -jar target/asqp-reader.jar  # Correct (1.7 MB)
# NOT: target/asqp-reader-1.0-SNAPSHOT.jar
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
2. Rebuild with `mvn clean package`
3. Check build completed successfully

### "Resource not found" Errors

If you see errors loading airlines.dat, airports.dat, or countries.json:
1. Verify files exist in `src/main/resources/data/`
2. Rebuild with `mvn clean package` (not just `mvn compile`)
3. Check JAR size is ~1.7 MB (should include resources)

### Build Fails

If Maven build fails:
```bash
# Clear local Maven cache
rm -rf ~/.m2/repository

# Rebuild
mvn clean package
```

---

## Performance

### Build Performance
- Full build with tests: ~10-15 seconds
- Build without tests: ~5-8 seconds
- Clean build: ~10-20 seconds

### Runtime Performance
- JAR size: ~1.7 MB
- Startup time: ~0.5-1.0 seconds
- Memory usage: ~100-200 MB
- Load time: 
  - Airlines: ~50ms (992 carriers)
  - Airports: ~200ms (6,033 airports)
  - Countries: ~20ms (193 countries)

---

## Development vs Production

### Development

During development, use Maven exec:
```bash
mvn exec:java -Dexec.mainClass="com.lamontd.asqp.App" -Dexec.args="data.csv"
```

Or your IDE's run configuration.

### Production

In production, use the JAR:
```bash
java -jar asqp-reader.jar data.csv
```

Benefits:
- Self-contained
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

The project uses `maven-shade-plugin` to create the executable JAR:

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
            <mainClass>com.lamontd.asqp.App</mainClass>
          </transformer>
        </transformers>
        <finalName>asqp-reader</finalName>
      </configuration>
    </execution>
  </executions>
</plugin>
```

### Build Warnings (Normal)

You may see warnings during build - these are normal:
```
[WARNING] commons-codec-1.17.1.jar, commons-csv-1.12.0.jar define 2 overlapping resources:
[WARNING]   - META-INF/LICENSE.txt
[WARNING]   - META-INF/NOTICE.txt
```

Multiple libraries include the same license files. Maven Shade handles this automatically.

---

## Summary

**Build command:**
```bash
mvn clean package
```

**Output:**
```
target/asqp-reader.jar (1.7 MB)
```

**Run:**
```bash
java -jar target/asqp-reader.jar <file.csv>
```

**Deploy:**
Copy the JAR anywhere - it's completely self-contained!
