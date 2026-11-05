<<<<<<< HEAD
# roomkh-backend
RoomKH Spring Boot backend API
=======
# roomkh.com — Backend

Spring Boot 3.3 API for the roomkh.com property rental/sale marketplace (Cambodia).
Package-by-feature under `tech.techey.property`, per the technical architecture doc.
This covers **Milestone 1 (Foundation)** and part of **Milestone 2 (Auth)**.

## Running locally

```bash
# from the repo root
docker compose up -d postgres redis        # + minio, mailhog if you want file upload / email later
cd backend
./gradlew bootRun --args='--spring.profiles.active=local'
```

The API listens on `http://localhost:8080`. Swagger UI: `http://localhost:8080/swagger-ui.html`.
OpenAPI spec: `http://localhost:8080/v3/api-docs`.

Flyway runs automatically on boot against `roomkh`/`roomkh`/`roomkh_dev_password` on
**`localhost:55432`** (matches the root `docker-compose.yml`). `spring.jpa.hibernate.ddl-auto=validate`
in every profile — Flyway is the only source of schema truth.

`src/main/resources/application-local.yml` is git-ignored (see root `.gitignore`); a working copy
already exists on disk with dev-only defaults (no real secrets), and
`application-local.yml.example` is committed as the template for a fresh clone.

### Why Postgres is on host port 55432, not 5432

During verification of this milestone a native/Homebrew PostgreSQL already running on the host's
`5432` silently won the port from Docker's forward for `roomkh-postgres` — both ended up
"listening", but macOS routed `localhost:5432` to whichever bound it first, so the app got
`role "roomkh" does not exist` even though the container logs showed it was created correctly.
`5433` was tried as a fix but hit a separate stuck port-binding at the Docker daemon level in this
environment (unrelated to this project — likely leftover VM networking state from other local
projects). Rather than requiring every developer to fight their own machine's port state,
`docker-compose.yml` maps the container's `postgres` service to host port **55432**, and
`application-local*.yml` point at it. If `5432` is free on your machine, feel free to edit the port
mapping and the datasource URL back together.

### Dev accounts

Running with `--spring.profiles.active=local` seeds real accounts on boot (`DevDataSeeder`,
idempotent, `local` profile only — never runs in staging/prod), one per privileged role, all
sharing the same password:

```
admin@roomkh.com  / Admin123!   (ADMIN — admin app login)
owner@roomkh.com  / Admin123!   (OWNER — owner portal login)
```

There's no self-service way to become `ADMIN` or `OWNER` (`AuthService.register` always assigns
`Role.USER`, correctly), so this is the only way to get a real authenticated session for local
development. Log in with `POST /api/v1/auth/login` or through each app's own `/login` page.

## What's implemented

- **Foundation**: project skeleton, Gradle Kotlin DSL + wrapper, Flyway V1–V2, `ApiResponse` /
  `Meta` / `ApiError` envelope, `GlobalExceptionHandler`, stateless JWT `SecurityConfig`, CORS,
  Redis beans, OpenAPI, bilingual (km/en) message source, `BaseEntity` with JPA auditing.
- **Auth**: register, login, refresh (rotating, hashed, family-based reuse detection per
  architecture doc §3.4), logout, OTP send/verify (stub — see below).
- **Users**: `User` entity/repository/service, `GET /api/v1/users/me`.
- **Taxonomy**: `Province`/`District`/`Commune`/`Village`, `PropertyType`, `Amenity` entities +
  repositories, seeded by Flyway (all 25 provinces; Phnom Penh's 14 khans and a representative
  subset of sangkats; 9 property types; 10 amenities). No service/controller yet — exposed in a
  later milestone per the build order.

**Explicitly out of scope this milestone** (empty packages, per the architecture doc §8 build
order — milestones 3–10): `owner`, `listing`, `engagement`, `inquiry`, `analytics`, `billing`,
`moderation`, `admin`, `notification`, and taxonomy business logic/endpoints beyond the seed data.
`common/storage` and `common/util` are also empty — nothing in this milestone needs S3/image
processing or the slug/phone/geo helpers yet.

**OTP is a stub**: `OtpService` generates a code and logs it instead of sending an SMS. Real
SMS/Telegram delivery is out of scope for this milestone; `verifyOtp` always returns `true`. The
DTOs (`OtpRequest`, `SendOtpRequest`) and endpoints (`POST /api/v1/auth/otp/send`,
`POST /api/v1/auth/otp/verify`) are wired up so the shape is right for a follow-up.

## Environment / deviation notes

- **JDK**: the only JDK available in this environment is **25**, not the LTS 21 the architecture
  doc specifies, and there's no way to auto-provision 21 via a Gradle toolchain (no network access
  to a JDK distribution service, no local JDK 21 install). `build.gradle.kts` sets
  `sourceCompatibility`/`targetCompatibility` to `VERSION_21` and `options.release.set(21)`, so the
  code compiles against the **Java 21 language/API level** using the installed JDK 25 — no
  `toolchain {}` block that would try to download a JDK.
- **Lombok**: Spring Boot 3.3.4's managed Lombok version can't run on JDK 25's javac
  (`NoSuchFieldException: com.sun.tools.javac.code.TypeTag :: UNKNOWN`). Pinned explicitly to
  Lombok **1.18.38**, which added JDK 23–25 compatibility. `lombok-mapstruct-binding` is on the
  annotation processor path so MapStruct sees Lombok-generated getters (MapStruct itself isn't
  used yet in this milestone's code, but the dependency is wired per the spec).
- **Gradle wrapper**: no `gradle` binary was preinstalled. A full Gradle 8.10 distribution was
  downloaded to bootstrap `gradle wrapper`, but Gradle 8.10's embedded Kotlin DSL compiler also
  can't parse a JDK 25 version string, so `gradle wrapper` itself failed under JDK 25 for **that**
  version. The wrapper actually shipped is generated with **Gradle 9.4.1** (from a distribution
  already fully cached locally on this machine), which runs cleanly on JDK 25. `./gradlew
  compileJava` and `./gradlew build -x test` both succeed.
- **Build verification**: `./gradlew compileJava` — green. `./gradlew build -x test` — green
  (produces `build/libs/property.jar`). No unit/integration tests were written this milestone
  (none were in scope); `testImplementation`/Testcontainers dependencies are wired for later.
- **Live DB verification**: brought up real `postgres` (PostGIS) + `redis` via the root
  docker-compose and ran the app against them with `--spring.profiles.active=local`. Confirmed:
  - Flyway applies V1 and V2 cleanly; Hibernate's `ddl-auto=validate` passes (entities match the
    migrated schema exactly).
  - `POST /api/v1/auth/register`, `/login`, `GET /api/v1/users/me`, `/logout` all work end-to-end.
  - Refresh rotation works, **and** reuse-of-an-already-rotated-token correctly revokes the whole
    token family (see bug note below).
  - Validation errors, `INVALID_CREDENTIALS`, `CONFLICT` on duplicate email, `401`/`403` JSON
    envelopes (not Spring's default HTML), CORS preflight, and role-gated `/api/v1/admin/**` all
    behave as specified.
  - `/actuator/health` reports `DOWN` unless `mailhog` is also running (its `MailHealthIndicator`
    fails without a reachable SMTP host) — expected, not a bug.
- **Bug found and fixed during live verification**: `AuthService.refresh()` is `@Transactional`.
  On reuse detection it revoked the token family and then threw `ApiException` — a
  `RuntimeException` — which rolled back the *whole* transaction, including the revocation UPDATE,
  so the "burned" family silently came back to life. Fixed by moving the revocation into
  `RefreshTokenFamilyRevoker`, a separate bean with `@Transactional(propagation =
  REQUIRES_NEW)`, so the revocation commits independently before the exception propagates.
  Verified: reusing a rotated token now permanently revokes every token in that family.
- **Bug found and fixed during live verification**: the catch-all `@ExceptionHandler(Exception.class)`
  in `GlobalExceptionHandler` was also catching Spring's `NoResourceFoundException` for genuinely
  unmapped routes, turning what should be a `404` into a `500`. Added explicit handlers for
  `NoResourceFoundException`/`NoHandlerFoundException` (→ `404 NOT_FOUND`) and
  `HttpRequestMethodNotSupportedException` (→ `405`).
>>>>>>> 61c30e3 (add onwer and improve UI desigin)
