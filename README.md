# Aerolink

Aerolink is a Spring Boot application acting as an API wrapper for aviation-related data, specifically integrating with [AviationWeather.gov](https://aviationweather.gov/). It provides a robust, resilient, and monitored interface for retrieving airport details.

## 🚀 Key Features

- **ICAO Airport Data**: Retrieve detailed airport information using 4-letter ICAO codes.
- **Smart Rate Limiting**: Implements request-based rate limiting using **Bucket4j** to ensure compliance with upstream provider limits.
- **Resilience & Fault Tolerance**: Uses **Resilience4j** for Circuit Breaker and Retry mechanisms, shielding the application from upstream instability.
- **Monitoring**: Full integration with **Spring Boot Actuator** and **Prometheus** for real-time observability.
- **Validation**: Strict request validation for ICAO codes and batch request limits (max 15 per call).

## 🛠 Tech Stack

- **Framework**: Spring Boot 3.5.0
- **Java Version**: 21
- **Resilience**: Resilience4j 2.2.0
- **Rate Limiting**: Bucket4j 8.10.1
- **Metrics**: Micrometer + Prometheus
- **Testing**: JUnit 5 + WireMock (for API integration testing)
- **Utilities**: Lombok, Apache HttpClient5

---

## ⚙️ Setup & Run

### Prerequisites

| Tool | Version |
|------|---------|
| Java | 21+ |
| Maven | 3.9+ |

Verify your setup:
```bash
java -version
mvn -version
```

### Clone & Build

```bash
git clone https://github.com/your-org/aerolink.git
cd aerolink
mvn clean install -DskipTests
```

> `-DskipTests` skips running tests and builds the application JAR directly.

### Run Tests

```bash
# Build and run all tests (unit + integration)
mvn clean install
```

### Run

**Option 1 — Maven plugin:**
```bash
mvn spring-boot:run
```

**Option 2 — JAR directly:**
```bash
java -jar target/aerolink-1.0.0-SNAPSHOT.jar
```

The API starts on port **8080** and the management/metrics server on port **8081**.

### Configuration

All configuration lives in `src/main/resources/application.properties`. Key properties you may want to override:

| Property | Default | Description |
|----------|---------|-------------|
| `aviation.provider.api.base-url` | `https://aviationweather.gov/api/data` | Upstream API base URL |
| `aviation.provider.api.connect-timeout` | `5s` | Connection timeout |
| `aviation.provider.api.read-timeout` | `10s` | Read timeout |
| `aviation.provider.api.max-total-connections` | `50` | HTTP connection pool size |
| `aviation.provider.api.max-connections-per-route` | `20` | Max connections per route |
| `aviation.provider.api.request-limit-per-minute` | `60` | Rate limit (requests/min) |

Override at runtime with system properties:
```bash
java -jar target/aerolink-1.0.0-SNAPSHOT.jar \
  --aviation.provider.api.request-limit-per-minute=120 \
  --aviation.provider.api.read-timeout=15s
```

---

## 📖 API Documentation

### Get Airport Details

Retrieves details for a batch of airports specified by their ICAO identifiers.

- **URL**: `/api/v1/airport`
- **Method**: `GET`
- **Parameters**:
    - `icaoCodes`: A comma-separated list of 4-letter ICAO codes (e.g., `KLAX,KSFO,KJFK`).
    - *Limit*: Maximum 15 codes per request.
- **Success Response**: `200 OK` with a JSON array of `AirportDetail` objects.

Example:
```bash
curl "http://localhost:8080/api/v1/airport?icaoCodes=KLAX,KSFO"
```

### Health & Info

- **Health Status**: `http://localhost:8081/status`
- **App Info**: `http://localhost:8081/info`

### Monitoring

- **Metrics (Prometheus)**: `http://localhost:8081/prometheus`

> [!IMPORTANT]
> **Why are `/status`, `/info`, and `/prometheus` on port 8081 instead of 8080?**
> These endpoints must never be reachable by end users or the public internet. Exposing Prometheus metrics leaks internal implementation details (method names, dependency versions, error rates), while health and info endpoints can reveal infrastructure topology. Running them on a separate port lets us block port 8081 entirely at the network/infrastructure level (firewall, security group, ingress rule).
