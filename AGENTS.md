# AGENTS.md

## Build & run

```bash
mvn clean verify          # full build (compile + test all modules)
mvn clean install -DskipTests   # quick build, skip tests
mvn dependency:tree       # inspect dependency graph
```

- Java 21, Spring Boot 3.5.15, multi-module Maven (`pom` packaging at root).
- Root POM defines shared deps (Lombok, uuid-creator, JUnit Jupiter, spring-boot-starter-test). Module POMs add layer-specific deps.
- No entry point yet (`@SpringBootApplication` missing). No `application.yml` exists. No Flyway migrations exist. No test files exist.
- Kotlin 2.3.10 is configured but no `.kt` files exist.

## Module boundaries (strict downward: api → service → repo)

| Module | Layer | Internal dep | Key external deps |
|--------|-------|-------------|-------------------|
| `inkpulse-repo` | Data | none | JPA, PostgreSQL, Flyway |
| `inkpulse-service` | Business | `inkpulse-repo` | Redis, PipelinR, Jackson JSR310 |
| `inkpulse-api` | Presentation | `inkpulse-service` | Spring Web, PipelinR |

- **Never reverse dependencies.** API imports from service, service imports from repo. Never the other way.
- Never import web/HTTP classes (`HttpServletRequest`, `ResponseEntity`, etc.) in service or repo modules.

## CQRS via PipelinR

- `cqrs.Command<R>` — for writes. Implement `CommandHandler<C, R>`.
- `cqrs.Query<R>` — for reads. Implement `QueryHandler<Q, R>`.
- Pipeline bean is defined in `inkpulse-api/config/PipelinRConfig.java`. Handlers are auto-discovered by Spring.
- One handler per command/query.

## Controller conventions

- All controller responses MUST be wrapped in `dtos.ResultRes<T>` using static factories: `successResult(data)`, `errorResult(...)`.
- Handlers return raw domain/DTO objects — never `ResultRes`. Controllers do the wrapping.
- Use constructor injection with `@RequiredArgsConstructor` (Lombok) — never `@Autowired` on fields.
- `@Transactional` goes on **handlers**, never on controllers.
- `@Transactional(readOnly = true)` on all query handlers.

## Entity conventions (inkpulse-repo)

- Entities extend `BaseAuditableEntity<java.util.UUID>` (UUID v7 PK via `@GeneratedUuidV7`, plus `createdAt`, `updatedAt`, soft-delete `deleted`).
- `@SQLRestriction("is_deleted = false")` on entities for automatic soft-delete filtering.
- Table names: `snake_case`, plural (`users`, `user_roles`).
- Flyway migrations in `inkpulse-repo/src/main/resources/db/migration/`, named `V{version}__{snake_case_description}.sql`. Every schema change goes through Flyway — no manual DDL.
- **Lưu ý quan trọng về ID tự sinh (JPA/Hibernate)**: Tuyệt đối KHÔNG gán ID thủ công (bằng cách dùng `.setId()` hoặc đặt ID trong builder/constructor) trước khi gọi `.save()` cho một thực thể mới tạo.
  - *Lý do*: Vì thực thể kế thừa `BaseAuditableEntity` có trường `@Id` tự sinh và kiểm soát khóa lạc quan `@Version`. Khi gán ID khác null khi tạo mới, Spring Data JPA sẽ hiểu đây là thực thể cũ cần cập nhật và gọi `merge` thay vì `persist`, gây ra lỗi `ObjectOptimisticLockingFailureException` do Hibernate cố cập nhật dòng chưa tồn tại trong database.
  - *Cách xử lý đúng*: Hãy để Hibernate tự sinh ID. Nếu cần dùng ID trước (như tạo đường dẫn upload file MinIO chứa ID sách), hãy gọi `repository.save(book)` trước để lấy ID đã sinh (`book.getId()`), sau đó mới gán đường dẫn file và `save` lần thứ 2 để cập nhật.

## Cache system (inkpulse-service)

Two layers:
- **`ICacheService`** — low-level Redis ops (get/set/remove, locks, pub/sub). Use for raw Redis access.
- **`SectionCacheService`** — annotation-driven domain cache. Cacheable DTOs must implement `Cacheable` and carry `@CacheSection("section-name")`. The section name must match a YML key under `cache.sections`.
- Key construction: `CacheKeyHelper` builds `"prefix:identifier"` keys.
- `JsonHelper.serializeSafe()` / `deserializeSafe()` for cache serialization (non-throwing).

## Naming conventions

| Item | Pattern | Example |
|------|---------|---------|
| Command | `{Verb}{Entity}Command` | `CreateUserCommand` |
| Query | `Get{Entity}By{Field}Query` | `GetUserByIdQuery` |
| Handler | `{CommandName}Handler` | `CreateUserHandler` |
| Controller | `{Entity}Controller` | `UserController` |
| Repository | `{Entity}Repository` | `UserRepository` |
| DTO | `{Entity}Response` / `{Entity}Request` | `UserResponse` |
| Package (features) | `features.{domain}/` | `features.user/` |

## Other rules

- `@Slf4j` for logging; parameterized (`log.info("User {}", id)`) — never string concatenation or `System.out.println`.
- No sensitive data in logs (passwords, tokens, PII).
- No linter/formatter configured yet — follow existing file style when editing.

## Feature Implementation Design Patterns

When building new features, always prioritize using these design patterns to keep the code clean, modular, and maintainable:
1. **CQRS Pattern (via PipelinR):** Separate write actions (`Command`) from read actions (`Query`).
2. **Strategy Pattern:** Use to decouple algorithms or business rules into interchangeable strategies.
3. **EligibilityPipeline Pattern (pipeline package):** Use `EligibilityPipeline`, `EligibilityContext`, and `IEligibilityRule` to perform structured step-by-step checks/rules/validations on domain entities before executing actions.


## OpenCode skills reference

26 skills live in `.opencode/skills/`. Key ones to invoke when working on relevant features:
- `inkpulse-project-conventions` — architecture, packages, naming (read first for any new file)
- `inkpulse-cqrs-pattern` — full CQRS handler implementation guide
- `inkpulse-cache-system` — cache DTOs, YML config, Redis patterns
- `flyway-migration-author` — migration naming, idempotent SQL
- `repository-entity-scaffold` — JPA entity + Spring Data repo generation
- `rest-controller-scaffold` — REST controller generation
- `global-exception-handler` — `@RestControllerAdvice` + `ResultRes` error handling
