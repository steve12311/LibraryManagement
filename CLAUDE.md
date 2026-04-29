# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run

```bash
# Full build + tests across all modules
mvn clean verify

# Build entire project, skip tests
mvn clean package -DskipTests

# Run the application (includes dep modules via -am)
mvn -pl managementsystem-admin -am spring-boot:run

# Run all tests in system module
mvn -pl managementsystem-system -am test

# Run a single test class
mvn -pl managementsystem-system -am test -Dtest=RoleMenuServiceImplTest

# Fast compile check for mapper/service edits
mvn -pl managementsystem-system -am -DskipTests compile
```

Requires JDK 21, Maven 3.9+, MySQL 8+, Redis 7+. Integration tests need live MySQL and Redis — configure `managementsystem-admin/src/main/resources/application.yml` and `application-druid.yml`, then initialize schema from `docs/sql/ms.sql`.

Application entrypoint: `org.dwtech.Application` (not ManagementsystemApplication).

## Module Architecture

Maven multi-module Spring Boot 3 backend. Request flow (bottom-up dependency):

```
managementsystem-admin       <- HTTP entry: controllers, Application, YAML config
    |
managementsystem-framework   <- Cross-cutting: Security filter chain, AOP (OperLog, RepeatSubmit),
    |                            AI services (ChatGLM, VectorStore, Tool Calling), AuthService
    |
managementsystem-system      <- Business logic: Service/impl, Mapper + XML, MapStruct converters,
    |                            domain model (entity/dto/vo/bo/query/form), scheduled tasks
    |
managementsystem-common      <- Shared foundation: Result<T>, PageResult<T>, BaseEntity,
                                 BaseController, annotations, enums, utils, JWT/Redis token mgmt
```

Controllers are split into sub-packages under `managementsystem-admin`:
- `controller/` — top-level: AuthController, DashboardController, FileController, IndexController
- `controller/lib/` — library domain: Book, Borrow, Category, Publish, Stock
- `controller/sys/` — system admin: User, Role, Menu, Dept

## Key Conventions

- **Response types**: `Result<T>` (single item) or `PageResult<T>` (paginated list) — both in `org.dwtech.common.core.entity`
- **Permission checks**: `@PreAuthorize("@ss.hasPermi('domain:entity:action')")` for admin endpoints, `@PreAuthorize("isAuthenticated()")` for file/user-owned endpoints
- **Timestamps**: `LocalDateTime` with MyBatis-Plus `@TableField(fill = FieldFill.INSERT/INSERT_UPDATE)`, handled by `MyMetaObjectHandler`
- **Audit trail**: extend `BaseEntity` for `id`, `createTime`, `updateTime` (auto-filled via MetaObjectHandler)
- **Soft delete**: `is_deleted` column (0=normal, 1=deleted), applied manually in mapper XML WHERE clauses
- **Standard layering**: `Controller → Service (interface) → ServiceImpl → Mapper + XML → entity`
- **MapStruct converters**: under `managementsystem-system/.../converter/`, e.g. `BookConverter`, `UserConverter` — used for entity↔DTO↔VO mapping
- **Collaborative filtering**: `RecommendationService` provides personalized book recommendations via item-based CF (cosine similarity on co-borrowing data). Similarity matrix stored in Redis Hash (`cf:sim:{isbn}`), rebuilt nightly by `CollaborativeFilteringTask`, user recommendation cache invalidated on borrow/return events
- **Model layer** under `managementsystem-system/.../model/`:
  - `entity/` — DB-mapped domain objects
  - `dto/` — internal transfer objects
  - `vo/` — view objects sent to frontend
  - `bo/` — business objects (aggregates, computed results)
  - `query/` — request query/filter parameters (extend `BasePageQuery` for paginated queries)
  - `form/` — request body forms

## Key Annotations

- `@RepeatSubmit` — AOP-based duplicate submission prevention, applied to write endpoints
- `@OperLog` — AOP audit logging (records operation type, target, result)
- `@DataPermission` — row-level data scope filtering, processed by `MyDataPermissionHandler` in MyBatis-Plus interceptor
- `@ValidField` — custom field validation marker
- Controllers extend `BaseController` for shared utilities (page helper, etc.)

## Auth Flow

Stateless JWT via `OncePerRequestFilter` (`TokenAuthenticationFilter`). Tokens stored in Redis via `TokenManager` / `RedisTokenManager` with sliding-window refresh. Refresh token delivered via httpOnly cookie (`RefreshTokenCookieUtils`). Captcha validated by `CaptchaValidationFilter`. Full security filter chain configured in `SecurityConfig.java`. Permission evaluation delegated to `PermissionService` (`@ss`).

## AI Search & Vector Store

- Calls external DeepSeek API (via Spring AI) for natural-language → structured query conversion
- Vector store backed by Milvus for catalog similarity search
- Ollama provides embedding generation
- Tool Calling: `DateTimeTool`, `StockTool`, `VectorTool` (loaded via `ToolsLoader`)
- SSE streaming chat at `GET /chat`
- Catalog vector sync: async queue (`CatalogVectorSyncPublisher`/`Consumer`) + rebuild runner (`CatalogVectorRebuildRunner`)
- Rate limited via `AIRateLimitInterceptor`

## Commit & PR Style

Conventional Commits: `feat(scope):`, `fix(scope):`, `refactor(scope):`, `docs(scope):`, `perf(scope):`, `merge:` for merge commits. See `git log --oneline` for examples. Branches from `main`, merge back after verification.

## Recommended Skills

- `springboot-patterns` — service/controller/mapper patterns
- `springboot-security` — auth/permission topics
- `systematic-debugging` — before proposing any bug fix
- `staged-rectification-manager` — multi-phase refactor tasks

## Stage Reviews

Ongoing phase work tracked in `docs/p*-*-stage-review-*.md`. Current phase: P4 Governance Hardening (rate limiting, audit logging, access control for AI endpoints).
