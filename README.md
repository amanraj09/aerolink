# Aerolink

Aerolink is a high-performance Spring Boot application acting as an API wrapper for aviation-related data, specifically integrating with [AviationWeather.gov](https://aviationweather.gov/). It provides a robust, resilient, and monitored interface for retrieving airport details.

## 🚀 Key Features

- **ICAO Airport Data**: Retrieve detailed airport information using 4-letter ICAO codes.
- **Smart Rate Limiting**: Implements request-based rate limiting using **Bucket4j** to ensure compliance with upstream provider limits.
- **Resilience & Fault Tolerance**: Uses **Resilience4j** for Circuit Breaker and Retry mechanisms, shielding the application from upstream instability.
- **Advanced Monitoring**: Full integration with **Spring Boot Actuator** and **Prometheus** for real-time observability.
- **Validation**: Strict request validation for ICAO codes and batch request limits (max 15 per call).

## 🛠 Tech Stack

- **Framework**: Spring Boot 3.5.0
- **Java Version**: 21
- **Resilience**: Resilience4j 2.2.0
- **Rate Limiting**: Bucket4j 8.10.1
- **Metrics**: Micrometer + Prometheus
- **Testing**: JUnit 5 + WireMock (for API integration testing)
- **Utilities**: Lombok, Apache HttpClient5

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

### Health & Monitoring

- **Health Status**: `http://localhost:8081/status`
- **Metrics (Prometheus)**: `http://localhost:8081/prometheus`
- **App Info**: `http://localhost:8081/info`

## ⚙️ Configuration

Key settings can be adjusted in `src/main/resources/application.properties`:

| Property | Description | Default |
|----------|-------------|---------|
| `aviation.provider.api.base-url` | Upstream API URL | `https://aviationweather.gov/api/data` |
| `aviation.provider.api.request-limit-per-minute` | Rate limit window | `60` |
| `aviation.provider.api.connect-timeout` | HTTP connect timeout | `5s` |
| `aviation.provider.api.read-timeout` | HTTP read timeout | `10s` |

