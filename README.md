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

## 🏛️ Architecture Overview

### Layered Design

The application follows a clean layered architecture with strict separation of concerns:

```
 HTTP Request
      │
      ▼
┌─────────────────────┐
│   AeroLinkController │  ← Input validation, HTTP mapping
└─────────┬───────────┘
          │
          ▼
┌─────────────────────┐
│   AeroLinkService    │  ← Business logic, ICAO normalisation
└─────────┬───────────┘
          │
          ▼
┌──────────────────────────┐
│  AviationDataProvider    │  ← Provider interface (Strategy pattern)
│  (AviationWeatherClient) │  ← Rate limit → Circuit Breaker → Retry → HTTP
└──────────────────────────┘
          │
          ▼
   AviationWeather.gov API
```

### Design Patterns

| Pattern | Where | Purpose |
|---------|-------|---------|
| **Plugin** | `AviationDataProvider` interface + `AviationWeatherClient` | Decouples service from any specific upstream vendor — the active provider is resolved once at startup from config (`aerolink.provider`); adding a new provider requires only a new implementation and a config change, no business logic changes |
| **Registry** | `AviationDataProviderRegistry` | Holds all registered `AviationDataProvider` implementations keyed by provider name; acts as a lookup table for config-driven provider resolution at startup |
| **AOP / Decorator** | `@TrackMetrics` + `MetricTrackingAspect` | Cross-cutting metrics collection applied declaratively — no metrics code in business logic |
| **Retry** | `RetryTemplate` in `AviationWeatherClient` | Automatically re-attempts failed upstream calls with exponential backoff — only for transient failures (502, 503, network errors) where retrying is meaningful |
| **Circuit Breaker** | `CircuitBreaker` wrapping upstream calls | Tracks upstream failure rate over a sliding window; short-circuits all calls when the threshold is breached, giving the upstream time to recover and preventing thread exhaustion |

---

### Resiliency & Fault Tolerance

Every upstream call passes through three layers of protection in order: `Rate Limiter → Circuit Breaker → Retry`

- **Rate Limiter** — 60 req/min token bucket; rejected requests get an immediate `AERO-104` response with a `Retry-After` header, no threads held
- **Circuit Breaker** — opens after 20% failure rate over a 10-call window; recovers via half-open probe (3 calls) after 10s
- **Retry** — up to 3 attempts with exponential backoff (200ms, 400ms, 800ms); only for transient errors (502, 503, network failures) — deterministic failures like 500 are never retried

---

### Observability

- **Prometheus Metrics** — custom business metrics eagerly pre-registered at startup; circuit breaker state, HTTP client and server metrics all exported out of the box
- **Custom Business Metrics** — lookup counts, latency, error rates, and rate limit hits tracked per provider and outcome (`success` / `empty` / `error`)
- **Health & Info** — `/status` exposes application liveness; `/info` surfaces dependency versions and build metadata via Spring Boot Actuator

---

### Error Handling

- **Typed error codes** — all failures map to a named `ErrorCode` (`AERO-1xx` for client errors, `AERO-2xx` for upstream errors) carried by a single `AeroLinkException`
- **Centralised response shaping** — `GlobalExceptionHandler` translates every error code to the correct HTTP status and a consistent `{ errorCode, message }` body; no error logic scattered across layers
- **Exception hierarchy drives resilience decisions** — `UpstreamTransientServerException` (502/503) extends `UpstreamServerException` (500), letting the circuit breaker and retry policy each act independently on the same exception without coupling

---

### Architecture Decisions & Assumptions

- **Provider abstraction** — `AviationDataProvider` is an interface even though only one implementation exists today. The active provider is resolved at startup from `aerolink.provider` config via `AviationDataProviderRegistry`. Adding a second provider (e.g. FlightAware) requires only a new `@Component` implementing the interface and a config change — no business logic changes.

- **Separate management port (8081)** — Prometheus, health, and info endpoints are internal operational tooling. Binding them to a dedicated port allows them to be firewalled at the infrastructure level without any application-level authentication logic.

- **Resilience at the client layer, not the service layer** — Retry and Circuit Breaker live inside `AviationWeatherClient`, not `AeroLinkService`. Resilience is a transport concern — it deals with HTTP status codes, network failures, and upstream behaviour that the service layer has no business knowing about. Placing it at the client boundary keeps business logic clean and makes resilience behaviour easy to reason about and test in isolation.

- **Mapping at the client boundary** — `AviationWeatherClient` maps the raw upstream response to provider-agnostic domain models (`AirportDetail`, `RunwayDetail`, etc.) before returning. The service layer never sees the upstream schema. If the upstream API changes, only the client mapping needs updating.

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

#### Key Metrics

| Prometheus Metric | Type | Description |
|-------------------|------|-------------|
| `aerolink_airport_lookups_total` | Counter | Lookup requests by `outcome` (`success` / `empty` / `error`) |
| `aerolink_airport_lookup_duration_seconds` | Timer | End-to-end lookup latency by `outcome` |
| `aerolink_errors_total` | Counter | Application errors by `error_code` and `error_type` |
| `aerolink_upstream_rate_limit_hits_total` | Counter | Requests blocked by the upstream API rate limit |
| `resilience4j_circuitbreaker_state` | Gauge | Circuit breaker state — `0` closed, `1` open, `2` half-open |
| `resilience4j_circuitbreaker_failure_rate` | Gauge | Failure rate (%) within the sliding window |
| `http_server_requests_seconds` | Timer | Inbound HTTP request count and latency by endpoint and status |

> [!IMPORTANT]
> **Why are `/status`, `/info`, and `/prometheus` on port 8081 instead of 8080?**
> These endpoints must never be reachable by end users or the public internet. Exposing Prometheus metrics leaks internal implementation details (method names, dependency versions, error rates), while health and info endpoints can reveal infrastructure topology. Running them on a separate port lets us block port 8081 entirely at the network/infrastructure level (firewall, security group, ingress rule).

---

## 🤖 AI Assistance

Parts of this project were developed with the help of **Claude (Anthropic)** as an AI pair programmer. AI assistance was used for:

- **Unit test generation** — test cases for the client, service, and controller layers including edge cases and error scenarios
- **Integration test generation** — WireMock-based circuit breaker integration tests covering 4xx, 5xx, and open circuit scenarios
- **Configuration boilerplate** — setting up `RestClient`, `RetryTemplate`, `CircuitBreaker`, and Bucket4j with connection pooling and metrics binding
- **README documentation** — architecture overview, design decisions, and API documentation

All AI-generated code was reviewed, validated, and intentionally integrated. Core application logic, resilience design decisions, and architecture were human-driven.
