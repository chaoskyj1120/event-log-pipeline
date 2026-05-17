# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Development Commands

On Windows use `.\gradlew`; on Unix/Git Bash use `./gradlew`.

```bash
# Build
.\gradlew build

# Run application
.\gradlew bootRun

# Run all tests
.\gradlew test

# Run a single test class
.\gradlew test --tests "com.example.eventlogpipeline.EventLogPipelineApplicationTests"

# Build without tests
.\gradlew build -x test

# Clean
.\gradlew clean
```

## Project Architecture

**Stack:** Spring Boot 4.0.6, Java 17, Gradle, PostgreSQL, Spring Data JPA, Lombok

**Current state:** Early-stage pipeline. Only the `EventLog` entity is implemented. The `controller/` and `service/` packages exist but are empty.

### EventLog Entity

`src/main/java/com/example/eventlogpipeline/entity/EventLog.java`

Core domain model for user action events. Key design decisions:

- **UUID primary key** auto-generated in the constructor if not supplied.
- **Protected no-arg constructor** (`@NoArgsConstructor(access = AccessLevel.PROTECTED)`) — JPA requires it but application code must use the `@Builder`.
- **`eventType` stored as VARCHAR(50)** — free-form string (login, page_view, purchase, etc.). The in-code comment recommends using an `EventType` enum at the application layer, but no such enum exists yet.
- **`deviceType` mapped to a `DeviceType` enum** via `@Enumerated(EnumType.STRING)` — the enum does not exist yet.
- **`eventTime` auto-set** by Hibernate `@CreationTimestamp` on insert; not settable via Builder.
- **Five indexes** on `event_logs`: user_id, event_type, event_time, product_id, and composite (user_id, event_time).
- **Lazy-loaded FK to `User`** (required) and **`Product`** (optional) — neither entity exists yet.

### Database

PostgreSQL expected at `localhost:5432`, database `event_log_pipeline`, user `event_log_pipeline_user`, password `000000` (see `src/main/resources/application.yml`).

### Missing pieces before the app can run

1. `User` entity referenced by `EventLog`
2. `Product` entity referenced by `EventLog`
3. `DeviceType` enum referenced by `EventLog`
4. Repositories (`JpaRepository` subinterfaces)
5. Service layer
6. REST controllers (uses `spring-boot-starter-webmvc`, not the common `spring-boot-starter-web`)
