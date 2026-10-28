# CLAUDE.md

Guidance for Claude Code in the **CERPS** backend repository. This is a diploma project (EHU 2025–2026) under active review — keep changes focused and minimal.

## Project Overview

CERPS (Currency Exchange Rate Provider System) is the Spring Boot backend for the BudgetControl Android app. Multi-module Maven build:

| Module | Package root | Port | Purpose |
|---|---|---|---|
| `cerps-common` | `com.example.cerps.common` | — | Shared DTOs, constants, validators, base filter/exception classes, logback-common.xml |
| `currency-service` | `com.example.cerpshashkin` | 8080 | Exchange rates: 3 providers (Fixer.io, ExchangeRatesAPI, CurrencyAPI), median aggregation, 8h scheduler, PostgreSQL + Liquibase, admin API, Gemini AI proxy |
| `analytics-service` | `org.example.analyticsservice` | 8082 | Rate trends for the app's RateHistoryScreen; owns its own PostgreSQL (`historical_rates`/`historical_coverage`, separate Liquibase chain) lazily backfilled from Frankfurter; current-rate data still comes from currency-service via REST only |

⚠️ The two services have **different package roots** (`com.example.cerpshashkin` vs `org.example.analyticsservice`). Match the existing root when adding classes.

**Stack:** Java 21 · Spring Boot 3.5.6 (parent POM) · Lombok + MapStruct · PostgreSQL + Liquibase (each service has its own database and changelog chain) · Caffeine · Resilience4j · springdoc · WireMock/H2 for tests · GraalVM native-image profile · Deployed on Railway.

**Build:** `./mvnw clean verify` from repo root (runs checkstyle + PMD at `validate`, tests, JaCoCo gate at `verify`).

## Build environment (WSL)

JAVA_HOME must point to `~/jdks/jdk-21.0.11+10` (Temurin JDK 21 with javac) for Maven builds. The system Java at `/usr/lib/jvm/java-21-openjdk-amd64` is JRE-only and will fail on compilation.

Always run: `JAVA_HOME=~/jdks/jdk-21.0.11+10 ./mvnw ...`

If that directory doesn't exist, download it first:

```bash
mkdir -p ~/jdks && cd ~/jdks && curl -sL -o jdk21.tar.gz \
  "https://api.adoptium.net/v3/binary/latest/21/ga/linux/x64/jdk/hotspot/normal/eclipse" \
  && tar xzf jdk21.tar.gz
```

## Rules — ALWAYS

- **Declare every method parameter `final`.** Checkstyle `FinalParameters` fails the build otherwise. Also enforced: `UnusedImports`, `RedundantImport`, `FinalClass` (suppressed only for `*Application.java`). Checkstyle/PMD run in the `validate` phase with `failsOnError=true` — the build breaks before compilation.
- **Keep JaCoCo line coverage ≥ 70% per module** (`jacoco-check`, BUNDLE/LINE). Every new class needs tests or the module fails `verify`.
- **Use `BigDecimal` for all rate/money math** with `CerpsConstants.CALCULATION_SCALE` (6) and `RoundingMode.HALF_UP`; `DISPLAY_SCALE` (2) only for display values. Never `double` in rate calculations.
- **Put shared constants in `CerpsConstants`** (cerps-common); put per-class log messages in `private static final String` constants at the top of the class (see `FixerioClient`, `ExchangeRateScheduler` for the pattern).
- **Schema changes = new Liquibase file** under `currency-service/src/main/resources/db/changelog/migrations/` (named `vX.Y-description.yaml`/`.xml`), included at the end of `db.changelog-master.yaml`, with a `rollback` block. JPA `ddl-auto` is `none` — Liquibase owns the schema.
- **Register GraalVM reflection hints** in the service's `NativeImageConfig` (a `RuntimeHintsRegistrar`) for every new DTO that crosses an HTTP boundary (request/response bodies, external API responses) and for new JPA projections/converters.
- **Normalize user input in the controller** (`base.trim().toUpperCase()`), not in SpEL: cache annotations must use plain property access only (`key = "#base"`). SpEL method calls like `#currency.toUpperCase()` break GraalVM native builds — this bug shipped once already.
- **Return RFC-7807 `ProblemDetail` for all errors**, with `setTitle(...)`, `setType(URI.create(CerpsConstants.ERROR_URI_PREFIX + "slug"))`, and a `timestamp` property. Extend the per-service `GlobalExceptionHandler`; both extend the shared `BaseGlobalExceptionHandler` (cerps-common), which owns the common handlers (validation, missing param, generic 500) and the `createProblemDetail(...)` helper — add service-specific handlers in the subclass, shared ones in the base.
- **Add a `resilience4j.retry.instances.<name>` entry in `application.yml`** and `@Retry(name = "...")` for every new external HTTP client; build clients via `RestClient.builder()` with the 5s/10s `SimpleClientHttpRequestFactory` and the `correlationIdInterceptor` (see `ExternalClientsConfig`).
- **Prefix admin-operation logs with `AUDIT:`** including `operation=`, `result=`, `timestamp=` fields (see `AdminService`).
- **Add new public endpoints to BOTH** `SecurityConfig.PUBLIC_ENDPOINTS` (security is deny-by-default: `anyRequest().denyAll()`) **and** the rate-limit filter's `ENDPOINT_LIMITS` map. Admin endpoints go under `/api/v1/admin/**` (protected by `ApiKeyAuthFilter`, `X-API-Key` header → `ROLE_ADMIN`).
- **Gate schedulers** with `@ConditionalOnProperty(value = "scheduling.enabled", havingValue = "true")` so tests (which set `scheduling.enabled: false`) don't trigger them.
- **Keep `TrendsService.FRANKFURTER_ONLY_CURRENCIES` (analytics) in sync with currency-service provider coverage.** Currencies served only by the refresh gap-fill (`ExchangeRateService.fillMissingRatesFromFallback`; currently the v2.0 CIS/regional set) get one rate per business day, and the analytics 1D-period guard depends on this list. No endpoint exposes per-currency source metadata yet, so any provider-coverage change must update both services in the same change.

## Rules — NEVER

- **Never edit an existing Liquibase changeset** — checksums are recorded in deployed databases (Railway prod); modifying one breaks startup. Add a new file instead. New inserts into `supported_currencies` must be idempotent (preconditions or `ON CONFLICT DO NOTHING`) — prod may have drifted via `POST /api/v1/admin/currencies`.
- **Never add a bean implementing `ExchangeRateClient` casually.** `ExchangeRateProviderService` injects `List<ExchangeRateClient>` — every implementing `@Component` silently joins the median aggregation for all currencies.
- **Never let Analytics access PostgreSQL or currency-service internals directly.** It talks to currency-service exclusively through `CurrencyServiceClient` (REST). This decoupling was a deliberate refactor — don't regress it.
- **Never log or hardcode secrets.** Provider API keys live AES-256-GCM-encrypted in `api_provider_keys` (managed via admin API); master key/admin key/Gemini key come from env vars (`.env.example` lists them). Gemini prompts live in `application.yml`, not code.
- **Never read `X-Forwarded-For` directly** in filters — extend `AbstractRateLimitFilter` (cerps-common) and let its `extractClientIp` helper handle it. Subclasses define only `resolveLimit` + `resolveBucketKey`; remember the `FilterRegistrationBean(...).setEnabled(false)` bean in `SecurityConfig` so the filter runs only inside the security chain.
- **Never use unbounded caches for unbounded key spaces.** `SimpleCacheManager`/`ConcurrentMapCache` (currency-service) is fine for `currentRates`/`supportedCurrencies` (small key space, scheduled eviction); anything keyed by date or user input needs Caffeine with `maximumSize` (analytics pattern).
- **Never run `git update-index --chmod` / `chmod` on `mvnw`** — the executable bit is intentionally fixed in the git index (Windows checkouts strip it; CI relies on it).
- **Never throw raw exceptions across the trends pipeline for "no data"** — use `InsufficientDataException` (404) / `CurrencyNotSupportedException` (400) / `ExternalServiceException` (503); the handlers map them, and public error messages must not hint at admin endpoints.
- **Never rely on `@Cacheable`/`@CacheEvict` firing on self-invocation** — `ProviderKeyManagementService` evicts via `CacheManager` directly, after transaction commit, for exactly this reason. Follow that pattern when eviction must coordinate with a transaction.

## Code Patterns to Follow

**External provider client** (`client/impl/FixerioClient.java`): `@Component` + `@RequiredArgsConstructor`, injected named `RestClient`, `@Retry(name=...)`, `.onStatus()` throwing `ExternalApiException(operation, provider, detail)`, explicit `validateResponse()` with `Optional.orElseThrow`, conversion delegated to `ExternalApiConverter`. Provider names come from the `ApiProvider` enum (`FIXER("Fixer.io")`).

**Layering** (currency-service): `Controller` → thin facade `Service` (e.g. `CurrencyService`, holds `@Cacheable`) → focused services (`ExchangeRateService`, `RateHistoryService`, `SupportedCurrenciesService`) → `Repository`. Read-only services annotated `@Transactional(readOnly = true)` at class level.

**Repository queries**: derived methods where possible; complex SQL as `static final String` constants + `@Query(nativeQuery = true)` (see `FIND_BEST_RATE_QUERY`). Cross-rate joins go through EUR with the same-hour bucket (`date_trunc('hour', ...)` + functional index `idx_exchange_rates_hour`). ⚠️ Native SQL must also run on H2 in `MODE=PostgreSQL` (the test datasource) — verify before merging.

**Entities**: Lombok `@Data @Builder @NoArgsConstructor @AllArgsConstructor @EqualsAndHashCode(onlyExplicitlyIncluded = true)`; currency columns typed `java.util.Currency` via the shared `CurrencyAttributeConverter`.

**DTOs**: Java records, in `dto/` (service-specific) or `cerps-common/dto/` (crossing service boundaries — e.g. `RateHistoryResponse`, `TrendsResponse`). Validation via shared annotations `@ValidCurrency` / `@ValidPeriod` (period is strictly `1D/7D/30D/90D/180D/1Y`).

**Metrics**: Micrometer counters/gauges named `domain.subject.event` (`currency.provider.failures` tagged by provider, `analytics.trends.success`); registered in `@PostConstruct initMetrics()`.

**Logging config**: each service's `logback-spring.xml` includes `logback-common.xml` (cerps-common) and switches on Spring profile — `dev` = plain text + correlationId, `prod` = JSON `LogstashEncoder` + `service` field. New services replicate this, don't reinvent.

## Testing Requirements

- Tests are split into `src/test/java/.../unit/...` and `.../integration/...` mirrored by feature folder. Naming: `method_ShouldExpectedBehavior_WhenCondition` (e.g. `getProviderName_ShouldReturnCorrectName`).
- AssertJ (`assertThat`) + JUnit 5 + `@ExtendWith(MockitoExtension.class)` for unit tests.
- Integration tests for HTTP clients extend `BaseWireMockTest` (`@SpringBootTest @ActiveProfiles("test")`, static WireMock on a dynamic port, URLs overridden via `@DynamicPropertySource`, JSON fixtures in `src/test/resources/test-data/`). A new provider needs its stub added there.
- Test profile (`application-test.yml`): H2 in PostgreSQL mode, `ddl-auto: create-drop`, **Liquibase disabled**, seed via `test-data.sql`, `scheduling.enabled: false`. New columns/tables must therefore work from JPA annotations alone in tests.
- CI (`.github/workflows/ci.yml`) runs `./mvnw -B -ntp clean verify` on every push/PR to `main` — checkstyle, PMD, all tests, and the 70% JaCoCo gate must pass locally before pushing.

## Deployment Notes

- **Production: Railway**, two independent services:
  - Currency: `https://supportive-vision-production.up.railway.app`
  - Analytics: `https://sparkling-curiosity-production-9ffd.up.railway.app`
- Per-service `Dockerfile`s build from the **repo root context** (`mvn package -pl <service> -am`, copying all three modules); runtime is `eclipse-temurin:21-jre-alpine` as non-root `spring` user with an actuator healthcheck. `Dockerfile.native` variants build GraalVM native images (`mvn -Pnative package`) — native compatibility is why the reflection-hint and no-SpEL rules exist.
- Local: `docker-compose up --build` (Postgres on host port 5434 + both services); env from `.env` (see `.env.example` and README §для запуска). Analytics finds currency-service via `CURRENCY_SERVICE_URL`.
- Provider API keys are loaded at runtime via `POST /api/v1/admin/provider-keys` (X-API-Key auth), not env vars.
- Rates: refreshed every 8h (`scheduling.exchange-rates.rate: 28800000`) + on startup (3 attempts); history retained 395 days (`DatabaseCleanupScheduler`, 03:00 cron); freshness window `exchange-rates.max-age-hours: 24` — must stay > scheduler interval.
- `render.yaml` is an empty placeholder; ignore unless asked to set up Render.

## Scaling & Deployment Topology — single replica per service

Both services are designed and tuned for **one Railway replica each** (`numReplicas: 1`). Several components hold per-instance in-memory state that is correct only with a single instance:

- **Rate limiting** (`AbstractRateLimitFilter`) counts requests in an in-memory `ConcurrentHashMap`. With >1 replica the effective limit multiplies by the replica count and is enforced per-instance, not cluster-wide.
- **Schedulers** — `ExchangeRateScheduler.refreshRates` (8h + startup) and `DatabaseCleanupScheduler` (03:00 cron) — run on **every** instance with no leader election. Two instances refreshing concurrently each use their own `Instant.now()`, so the `(base,target,timestamp)` unique constraint does **not** dedupe them → duplicate rate rows.
- **Per-instance caches** — `CurrencyRateCache`, the `currentRates`/`supportedCurrencies` caches, `TrendsCache`, and the analytics single-flight backfill lock (`HistoricalRatesService.backfillLocks`) — are local to each JVM. Across replicas they are independent (eventually consistent for read-mostly data; the single-flight lock only coalesces within one instance — the DB unique constraint is the cross-instance guard).
- **Hikari pools** are sized for a single replica (currency-service 10, analytics 5). Multiplying replicas multiplies the connection demand against Railway Postgres — re-size if scaling.

**If horizontal scaling is ever required**, this needs: a shared rate-limit store (e.g. Redis / `bucket4j-redis`), scheduler leader election (e.g. ShedLock `@SchedulerLock` on both scheduled methods, or a dedicated one-replica worker), and a review of per-instance cache consistency. None of this is wired today — do not bump `numReplicas` without it. (Railway replica count is a deploy-time setting outside this repo; confirm it is 1 in the service settings.)

## Context Files

- `context-ai/BudgetControl_AI_Context_v13.md` — full project context (Android app + CERPS), Russian. Read before large tasks. Known drift: it claims 18 supported currencies; the Liquibase seed actually has 21.
- `context-ai/CLAUDE.md` — **belongs to the Android repo (BudgetControl)**, not this backend; useful only for understanding the API consumer (e.g. period values, EUR-based conversion in the app).
- `context-ai/MIGRATION_ANALYSIS.md` — Frankfurter integration / historical-rates implementation plan (2026-06).
- `context-ai/roadmap.md` — future plans.
- `context-ai/TECH_DEBT_BACKLOG.md` — deliberately-deferred engineering work (what was investigated and consciously not built, with the reasoning).
- `README.md` — local setup, admin API usage, endpoint list.
