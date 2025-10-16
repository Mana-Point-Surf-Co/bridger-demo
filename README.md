# Bridger Demo - GeoJSON to KML Converter

A Spring Boot application that converts GeoJSON data to KML format asynchronously with real-time WebSocket notifications.

## Quick Start (TL;DR)

```bash
# Clone the repo
git clone https://github.com/Mana-Point-Surf-Co/bridger-demo.git
cd bridger-demo

# Run with Docker
docker-compose up --build

# In another terminal, run tests
cd scripts
npm install
node test.js

# Or Import the postman-collection.json at the root of the project.
# And in post create a WebSocket connection to ws://localhost:8080/ws?userId=user123
# Or use the api-tests.http file in IntelliJ

```

That's it! The API will be running at `http://localhost:8080` with an in-memory H2 database.

---

## Features

- 🗺️ Convert GeoJSON to KML format
- 🔄 Asynchronous job processing with queue
- 📡 Real-time WebSocket notifications for job status updates
- 📊 Job management API (create, read, update, delete)
- 🎯 Filter jobs by status with pagination
- 💾 H2 in-memory database (fast and simple)
- 🐳 Dockerized for easy deployment
- 🧪 Comprehensive test suite

## Tech Stack

- **Backend**: Kotlin + Spring Boot 3.4.10
- **Database**: H2 in-memory database
- **ORM**: Exposed with Kotlin DateTime support
- **Async Processing**: Kotlin Coroutines
- **Real-time**: WebSockets
- **Build Tool**: Gradle
- **Container**: Docker

## Prerequisites

### Option 1: Docker (Recommended)
- Docker
- Docker Compose
- Node.js (for running test scripts)

### Option 2: Local Development
- Java 21 or higher
- Node.js (for running test scripts)

## Getting Started

### Option 1: Using Docker (Recommended) 🐳

This is the easiest way to get started. Docker will build and run everything.


#### 1. Start the Application with Docker Compose

```bash
docker-compose up --build
```

This will:
- Build the Spring Boot application
- Start the application on `http://localhost:8080`
- Use H2 in-memory database (no external DB needed)

#### 2. Stop the Application

```bash
docker-compose down
```

#### 3. View Logs

```bash
# View logs
docker-compose logs -f app
```

### Option 2: Local Development (Without Docker)

#### 1. Build the Project

```bash
./gradlew build
```

#### 2. Run the Application

```bash
./gradlew bootRun
```

The application will start on `http://localhost:8080` with an in-memory H2 database.

## API Endpoints

### Job Management

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/job/convert` | Create a new GeoJSON to KML conversion job |
| GET | `/api/job/{id}` | Get job status by ID |
| GET | `/api/job` | Get all jobs with optional filtering and pagination |
| GET | `/api/job/{id}/files` | Get both GeoJSON and KML files for a job |
| GET | `/api/job/{id}/kml` | Download KML file directly |
| DELETE | `/api/job/{id}` | Delete a job and its associated data |

### WebSocket

- **URL**: `ws://localhost:8080/ws?userId={userId}`
- **Purpose**: Receive real-time job status updates

## Job Statuses

- `PENDING` - Job is queued and waiting to be processed
- `PROCESSING` - Job is currently being processed
- `DONE` - Job completed successfully
- `FAILED` - Job failed during processing

## Running the Test Suite

The project includes a comprehensive test script that exercises all API endpoints and WebSocket functionality.

### Setup Test Environment

1. Navigate to the scripts directory:
```bash
cd scripts
```

2. Initialize npm (if not already done):
```bash
npm init -y
```

3. Install dependencies:
```bash
npm install ws axios
```

### Run the Tests

**If using Docker:**

1. Make sure the Docker container is running:
```bash
docker-compose up
```

2. In a new terminal, run the test script:
```bash
cd scripts
npm install ws axios
node test.js
```

**If running locally:**

1. Make sure the application is running:
```bash
./gradlew bootRun
```

2. In a new terminal, run the test script:
```bash
cd scripts
npm install ws axios
node test.js
```

### What the Test Suite Does

The comprehensive test suite (`scripts/test.js`) performs the following operations:

1. **WebSocket Connection**: Connects to WebSocket with `userId=user123`
2. **Create Jobs**: Creates 3 conversion jobs using Hawaii GeoJSON data
3. **Wait for Processing**: Allows time for jobs to be processed
4. **Get Job Status**: Retrieves status for the first created job
5. **Get All Jobs**: Fetches all jobs in the system
6. **Filter by Status**: Tests filtering by each status (PENDING, PROCESSING, DONE, FAILED)
7. **Get Job Files**: Retrieves both GeoJSON and KML for a completed job
8. **Download KML**: Downloads raw KML file with proper content-type
9. **Delete Job**: Deletes the first created job
10. **Verify Deletion**: Confirms the job was deleted (expects 404)
11. **WebSocket Summary**: Displays all WebSocket messages received during the test

### Test Output

The test script provides detailed console output including:
- ✅ Success indicators for each step
- 📊 Formatted JSON responses
- 📩 Real-time WebSocket messages
- 📈 Summary statistics
- ❌ Error messages if something fails

## Example API Usage

### Create a Conversion Job

```bash
curl -X POST http://localhost:8080/api/job/convert \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user123",
    "geo": {
      "type": "FeatureCollection",
      "features": [
        {
          "type": "Feature",
          "geometry": {
            "type": "Point",
            "coordinates": [-122.4194, 37.7749]
          },
          "properties": {
            "name": "San Francisco"
          }
        }
      ]
    }
  }'
```

Response (202 ACCEPTED):
```json
{
  "jobId": "550e8400-e29b-41d4-a716-446655440000",
  "geoRecordId": "660e8400-e29b-41d4-a716-446655440001",
  "status": "PENDING"
}
```

### Get Job Status

```bash
curl http://localhost:8080/api/job/550e8400-e29b-41d4-a716-446655440000
```

### Get All Jobs with Filtering

```bash
# Get all jobs
curl http://localhost:8080/api/job

# Filter by status
curl http://localhost:8080/api/job?status=DONE

# With pagination
curl "http://localhost:8080/api/job?status=PENDING&page=0&pageSize=10"
```

### Download KML

```bash
curl http://localhost:8080/api/job/550e8400-e29b-41d4-a716-446655440000/kml \
  -o output.kml
```

### WebSocket Connection (JavaScript)

```javascript
const ws = new WebSocket('ws://localhost:8080/ws?userId=user123');

ws.on('message', (data) => {
  const message = JSON.parse(data);
  console.log('Job Status Update:', message);
  // Example: { type: "JOB_STATUS", jobId: "...", status: "DONE", ... }
});
```

## Database Schema

### Jobs Table
- `id` (UUID, Primary Key)
- `user_id` (String)
- `status` (String: PENDING, PROCESSING, DONE, FAILED)
- `attempts` (Integer)
- `last_error` (String, nullable)
- `created_at` (Timestamp with timezone)
- `updated_at` (Timestamp with timezone)

### GeoJsons Table
- `id` (UUID, Primary Key)
- `job_id` (UUID, Foreign Key → jobs.id, CASCADE delete)
- `user_id` (String)
- `geojson_data` (Text)
- `kml` (Text, nullable)
- `created_at` (Timestamp with timezone)
- `updated_at` (Timestamp with timezone)

## Docker

The application is fully containerized for easy deployment.

### Docker Commands

```bash
# Start the application
docker-compose up --build

# Start in background
docker-compose up -d

# Stop the application
docker-compose down

# View logs
docker-compose logs -f app

# Restart the application
docker-compose restart app
```

### Configuration

The Docker setup uses:
- **Port**: 8080
- **Database**: H2 in-memory (resets on restart)
- **JVM Options**: -Xmx512m -Xms256m

Note: Since H2 is in-memory, all data is lost when the container stops. This is perfect for development and testing.

## Development

### Running Locally

```bash
# Start the application
./gradlew bootRun

# Build a JAR
./gradlew bootJar

# The JAR will be in build/libs/
```

### Database

The application uses H2 in-memory database with Exposed ORM. Tables are created automatically on startup using `SchemaUtils.createMissingTablesAndColumns()`.

## Testing with HTTP Files

For IntelliJ IDEA users, an HTTP request collection is available:

```
api-tests.http
```

Open this file in IntelliJ and click the play button next to each request to execute it.

## Project Structure

```
src/main/kotlin/com/bridger/job/
├── config/          # Configuration classes
├── controller/      # REST controllers
├── db/              # Database table definitions
├── repository/      # Data access layer
├── service/         # Business logic and job queue
└── websocket/       # WebSocket handlers

src/main/resources/
└── hawaii.json      # Sample GeoJSON data
```

## Error Handling

The API returns appropriate HTTP status codes:
- `200 OK` - Successful GET requests
- `202 ACCEPTED` - Job created successfully
- `400 BAD REQUEST` - Invalid request (e.g., invalid status filter)
- `404 NOT FOUND` - Resource not found
- `500 INTERNAL SERVER ERROR` - Server error

Error responses include a message:
```json
{
  "error": "Job ID: xxx not found"
}
```
