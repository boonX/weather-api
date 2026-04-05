# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Build
./mvnw clean install

# Run (requires PostgreSQL, Redis, and env vars set)
./mvnw spring-boot:run

# Run all tests
./mvnw test

# Run a single test class
./mvnw test -Dtest=WeatherControllerTest

# Run a specific nested test class
./mvnw test -Dtest=WeatherControllerTest#GetCurrentWeather

# Start local dependencies (PostgreSQL + Redis)
docker compose up -d
```

## Environment Variables

| Variable | Purpose |
|---|---|
| `VISUAL_CROSSING_API_KEY` | External weather API key |
| `JWT_SECRET_KEY` | 256-bit base64 HS256 signing key |

For local development, activate the `local` profile (`SPRING_PROFILES_ACTIVE=local`) which uses dummy values and a higher rate limit.

## Architecture

Spring Boot 4.0 REST API with JWT authentication, Redis caching, and PostgreSQL persistence.

**Package layout** (`io.github.boonx.weather_api`):

- `controller/` — REST endpoints; extracts user identity from the JWT `SecurityContext`
- `service/` — business logic (`WeatherService`, `AuthService`, `JwtService`)
- `client/` — `VisualCrossingWeatherApiClient`, caches responses in Redis with a 60-minute TTL
- `repository/` — Spring Data JPA interfaces for `User`, `Location`, `Subscription`
- `dto/` — request/response objects decoupling the API from JPA entities
- `config/` — cross-cutting concerns: `SecurityConfig` (stateless JWT), `RateLimitInterceptor` (Bucket4j, applied to `/api/weather/**`), `RedisConfig`, `GlobalExceptionHandler`
- `exception/` — `HttpStatusException` carries an HTTP status and is translated to `ProblemDetail` by the global handler

**Request flow:**
`JwtAuthenticationFilter` → `RateLimitInterceptor` → Controller → Service → Repository / `VisualCrossingWeatherApiClient`

## Testing

Tests run against H2 (in-memory) with Redis autoconfiguration disabled — no external services needed.

- `WeatherControllerTest` is the main integration test suite. It uses `MockMvc`, `@Sql` to seed a test user, and a hardcoded long-lived JWT signed with the test secret key.
- Tests are organised with JUnit 5 `@Nested` classes, one per endpoint group.
- Assert side effects via `subscriptionRepository.count()` directly, not just HTTP status codes.

## API Endpoints

| Method | Path | Auth |
|---|---|---|
| POST | `/api/user/register` | public |
| POST | `/api/user/login` | public |
| GET | `/api/weather/{location}/current` | public |
| POST | `/api/weather/{location}/subscribe` | Bearer JWT |
| DELETE | `/api/weather/{location}/subscribe` | Bearer JWT |
| GET | `/api/weather/locations/me` | Bearer JWT |
| GET | `/api/weather/locations/current` | Bearer JWT |
