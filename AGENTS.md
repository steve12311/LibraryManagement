# Repository Guidelines

## Project Structure & Module Organization
This repository is a Maven multi-module backend project (`pom.xml` at root) built around Spring Boot 3 and Java 21.
- `managementsystem-admin`: application entrypoint and REST controllers.
- `managementsystem-framework`: security filters, AOP, AI services (DeepSeek + VectorStore + Tool Calling).
- `managementsystem-system`: domain services, MyBatis mapper XMLs, MapStruct converters, business models.
- `managementsystem-spring-ai-deepseek-patch`: temporary patch for DeepSeek V4 thinking mode tool-calling (upstream Spring AI does not yet support `reasoning_effort` + `reasoning_content`).
- `managementsystem-common`: shared config, constants, utilities, token management, core abstractions (`Result<T>`, `PageResult<T>`, `BaseEntity`).
- `docs/sql`: DDL and migration SQL scripts used by business features.

Code is under `src/main/java`; tests are under `src/test/java` in each module.

Controller sub-packages under `managementsystem-admin`:
- `controller/` — top-level: AuthController, DashboardController, FileController, IndexController
- `controller/lib/` — library domain: Book, Borrow, Category, Publish, Stock
- `controller/sys/` — system admin: User, Role, Menu, Dept

Application entrypoint: `org.dwtech.Application`.

## Build, Test, and Development Commands
Use Maven from repository root:
- `mvn clean verify`: full build + tests across all modules.
- `mvn clean package -DskipTests`: package quickly when tests are not required.
- `mvn -pl managementsystem-admin -am spring-boot:run`: run the backend app locally.
- `mvn -pl managementsystem-system -am test`: run focused tests for the system module.
- `mvn -pl managementsystem-system -am test -Dtest=ClassNameTest`: run a single test.
- `mvn -pl managementsystem-system -am -DskipTests compile`: fast compile check for mapper/service edits.

Requires JDK 21, Maven 3.9+, MySQL 8+, Redis 7+. Integration tests need live MySQL and Redis — configure `managementsystem-admin/src/main/resources/application.yml` and `application-druid.yml`, then initialize schema from `docs/sql/ms.sql`.

## Domain Model Layer
Business logic in `managementsystem-system` follows a clear model sub-package structure:
- `model/entity/` — DB-mapped domain objects (e.g. `BorrowPO`, `StockPO`)
- `model/dto/` — internal transfer objects
- `model/vo/` — view objects sent to frontend (e.g. `BorrowVO`, `StockPageVO`)
- `model/bo/` — business objects (aggregates, computed results)
- `model/query/` — request query/filter parameters (extend `BasePageQuery` for pagination)
- `model/form/` — request body forms (e.g. `BorrowForm`, `StockForm`)

MapStruct converters under `converter/` (e.g. `BookConverter`, `BorrowConverter`) handle entity ↔ DTO ↔ VO mapping.

## Coding Style & Naming Conventions
- Java 21, 4-space indentation, UTF-8.
- Packages use lowercase (`org.dwtech...`); classes use `PascalCase`; methods/fields use `camelCase`.
- All source files include `@author steve12311` and `@since` (first commit date).
- Keep controllers thin; place business logic in service layer; SQL stays in mapper XML under `managementsystem-system/src/main/resources/mapper`.
- Controllers extend `BaseController` for shared utilities.
- Prefer clear Chinese comments for non-obvious logic and flow descriptions.

### Key Conventions
- **Response types**: `Result<T>` (single item) or `PageResult<T>` (paginated list).
- **Permission checks**: `@PreAuthorize("@ss.hasPermi('domain:entity:action')")` for admin endpoints, `@PreAuthorize("isAuthenticated()")` for file/user-owned endpoints.
- **Timestamps**: `LocalDateTime` with MyBatis-Plus `@TableField(fill = FieldFill.INSERT/INSERT_UPDATE)`, auto-filled by `MyMetaObjectHandler`.
- **Audit trail**: entities extend `BaseEntity` for `id`, `createTime`, `updateTime`.
- **Soft delete**: `is_deleted` column (0=normal, 1=deleted), applied manually in mapper XML WHERE clauses.
- **File upload**: SHA-256 content-hash dedup; files accessed via `/api/v1/files/{fileId}` (black-box, never expose real path).

### Key Annotations
- `@RepeatSubmit` — AOP-based duplicate submission prevention via Redisson distributed lock (default 5s window).
- `@OperLog(module, action, bizId)` — AOP audit logging, records operator/user/result to `OperLogPO`.
- `@DataPermission(deptAlias, userAlias)` — row-level data scope filtering, processed by `MyDataPermissionHandler` via MyBatis-Plus interceptor. Supports ALL / DEPT / DEPT_AND_CHILD / SELF levels.
- `@ValidField` — custom field validation marker.

## Testing Guidelines
- Test framework: `spring-boot-starter-test` (JUnit 5 + Mockito stack).
- Test classes should end with `Test` and mirror production package structure.
- Add unit tests for service logic, auth/security behavior, and mapper condition branches.
- Before opening PR: run at least `mvn test` (or module-scoped equivalent) and ensure no failing tests.

## Commit & Pull Request Guidelines
- Follow Conventional Commits style: `feat(scope):`, `fix(scope):`, `refactor(scope):`, `docs(scope):`, `perf(scope):`. Merge commits use `merge:` prefix.
- Keep commits focused by concern (security/perf/business logic separated).
- Create feature/hotfix branches from `main`; merge back after verification.
- PR should include: change summary, affected modules, verification commands/results, and linked issue/task.

## Auth & Security
- Stateless JWT via `OncePerRequestFilter` (`TokenAuthenticationFilter`). Tokens stored in Redis with sliding-window refresh.
- Login flow: Captcha validation (`CaptchaValidationFilter`) → Password authentication → JWT issuance → Refresh token in httpOnly cookie.
- Token invalidation: both access and refresh tokens added to Redis blacklist on logout.
- User-level session invalidation: all tokens issued before invalidation time are rejected (used on password change / role change).
- Permission evaluation: `PermissionService` (bean name `@ss`) loads role permissions from Redis cache, supports wildcard matching.

## AI Search & Vector Store
- DeepSeek API (via Spring AI `spring-ai-starter-model-deepseek` + local patch module) for natural-language → structured query with Tool Calling.
- DeepSeek V4 thinking mode (`reasoning_effort` + `reasoning_content`) supported via `managementsystem-spring-ai-deepseek-patch`.
- Milvus vector store for catalog similarity search via `spring-ai-starter-vector-store-milvus`; Ollama provides embeddings via `spring-ai-starter-model-ollama`.
- Tool Calling tools: `DateTimeTool`, `StockTool`, `VectorTool` (loaded via `ToolsLoader`).
- SSE streaming chat at `GET /chat`.
- Vector store package structure organized into four sub-packages under `framework.ai.vector`:
  - `application/` — orchestration entrypoint for controllers
  - `store/` — Spring AI `VectorStore` adapter
  - `queue/` — Redis Stream message model, publish, and consume (`CatalogVectorSync*`)
  - `rebuild/` — startup + full rebuild runners
- Rate limited via `AIRateLimitInterceptor`.

## Caching & Idempotency
- Spring Cache + Redis caches menu routes, role permissions, and dropdown options.
- Role/permission changes trigger cache eviction and refresh.
- File metadata cached by `fileId` in Redis; short-term null-value caching for non-existent files prevents cache penetration.
- Write endpoints use `@RepeatSubmit` for short-window duplicate prevention.
- **Collaborative filtering**: Item-based CF using cosine similarity on co-borrowing data. Similarity matrix cached in Redis Hash (`cf:sim:{isbn}`), rebuilt nightly at 3 AM. User recommendation list cached 30 min, invalidated on borrow/return. Falls back to default ordering for cold-start / anonymous users.

## Security & Configuration Tips
- Never commit secrets; externalize API keys/passwords via environment variables.
- Validate Redis/MySQL connectivity before local runs.
- For file and auth changes, verify permission checks and cache invalidation paths together.
- Milvus and Ollama are required only when AI vector search features are enabled.
