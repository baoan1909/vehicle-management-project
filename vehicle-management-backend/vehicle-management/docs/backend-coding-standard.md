# Backend Coding Standard

## 1. Purpose

This document defines code-level conventions for the backend implementation of `vehicle-management`.

Use this file for:

- package naming
- class naming
- DTO naming
- MapStruct usage
- entity and repository rules
- PostgreSQL persistence conventions
- testing expectations

For architecture and module boundary decisions, also read:

- `docs/clean-architecture-guide.md`

## 2. General principles

This backend serves a vehicle management platform and must remain readable, testable, scalable, and consistent across modules.

Priorities:

1. Correctness
2. Maintainability
3. Reuse
4. Clear boundaries
5. Performance-aware design

## 2.1. Text validation rule

Validate request/domain text before persistence, not only at the database boundary.

Rules:

- for columns backed by `VARCHAR(n)`, enforce the same maximum length in domain or application validation
- reject ISO control characters in normal business text unless the feature explicitly needs them
- reject raw `<` and `>` in ordinary text fields unless the feature explicitly allows rich content
- code-like fields should prefer uppercase letters, digits, underscore, and hyphen only
- phone-like fields should accept only digits with an optional leading `+`
- identifier-like fields such as citizen/identity card numbers should accept only letters and digits unless the schema defines a different format

Prefer shared reusable validation utilities instead of duplicating regex and length checks across policies.
When a validation rule matches the shared utility behavior, use `shared.utils.TextValidationUtils` instead of handwritten trim, regex, and max-length code in feature policies.

## 3. Module vocabulary must follow the schema

The codebase should use the same business language as `vehicle_management.sql`.

Preferred schema-aligned feature names:

- `iam.role`
- `iam.permission`
- `iam.account`
- `people.userprofile`
- `people.customer`
- `people.employee`
- `people.customervehicle`
- `catalog.vehicletype`
- `catalog.tickettype`
- `catalog.cardtype`
- `catalog.priceplan`
- `catalog.pricerule`
- `accesscontrol.card`
- `accesscontrol.subscription`
- `accesscontrol.lostcardreport`
- `parking.parkingsession`
- `parking.parkingevent`
- `billing.invoice`
- `billing.payment`
- `operations.shift`
- `operations.approvalrequest`
- `hardware.device`
- `notification.notification`
- `audit.auditlog`

Do not create service names based on old MySQL table names or deprecated concepts when the new PostgreSQL schema already defines a clearer name.

## 4. Package conventions

Preferred package structure:

- `entrypoint.controller.<schema>`
- `entrypoint.dto.<schema>.<table>.request`
- `entrypoint.dto.<schema>.<table>.response`
- `application.<schema>.<feature>.port.in`
- `application.<schema>.<feature>.port.out`
- `application.<schema>.<feature>.mapper`
- `application.<schema>.<feature>.usecase`
- `domain.<schema>.<feature>.model`
- `domain.<schema>.<feature>.service`
- `domain.<schema>.<feature>.policy`
- `infrastructure.persistence.database.entity.<schema>`
- `infrastructure.persistence.database.repository.<schema>`
- `infrastructure.persistence.database.specification.<schema>`
- `infrastructure.persistence.adapter.<schema>`
- `infrastructure.security`
- `infrastructure.mapper.<schema>`
- `shared.enumeration.<schema>` for enums that belong to a specific database schema or bounded context

Examples for this project:

- `entrypoint.controller.iam`
- `entrypoint.dto.people.userprofile.request`
- `application.iam.account.port.in`
- `domain.parking.parkingsession.model`
- `infrastructure.persistence.database.entity.accesscontrol`
- `infrastructure.persistence.database.repository.accesscontrol`
- `infrastructure.persistence.database.specification.accesscontrol`
- `infrastructure.persistence.adapter.catalog`

Keep package names schema-first and consistent with the current codebase naming, for example `accesscontrol` as the package form of the `access_control` database schema.

## 4.1. `package-info.java` rule

Use `package-info.java` only for package-level concerns.

Valid use cases:

- documenting the responsibility of a package with package-level JavaDoc
- defining package-level annotations when a framework or project convention needs them
- keeping important package roots visible in the repository when the module skeleton is created before concrete classes exist

Allowed contents:

- package declaration
- package JavaDoc
- package-level annotations

Do not use `package-info.java` for:

- business logic
- helper methods
- constants
- class declarations
- feature behavior of any kind

Recommended usage in this project:

- use it at important architectural roots such as `controller`, `application`, `domain`, `infrastructure`
- optionally use it at schema roots such as `application.iam` or `domain.parking` when package responsibilities need clarification
- do not create it for every leaf package unless it adds real documentation or package-level annotation value

If a package already contains enough concrete classes and its responsibility is obvious, `package-info.java` is optional.

## 5. DTO naming

Use explicit DTO names:

- `CreateXxxRequest`
- `UpdateXxxRequest`
- `XxxFilterRequest`
- `XxxResponse`
- `XxxAdminResponse`
- `XxxUserResponse`

Optional:

- `XxxSummaryResponse`
- `XxxDetailResponse`
- `XxxPageResponse`

Examples aligned with current schema:

- `CreateAccountRequest`
- `UpdateUserProfileRequest`
- `CardFilterRequest`
- `SubscriptionAdminResponse`
- `ParkingSessionDetailResponse`
- `LostCardReportAdminResponse`

Do not use vague names such as `XxxDto` when the actual intent is already known.

Use `XxxFilterRequest` when a `GET` list endpoint has several optional filter
fields and `@ModelAttribute` binding keeps the controller method clearer than
many separate `@RequestParam` arguments.

Request DTOs under `entrypoint.dto.<schema>.<table>.request` should normally be
implemented as Java `record` types. Prefer regular classes only when a concrete
framework-binding or serialization limitation requires mutable DTOs.

## 6. Use case naming

Preferred:

- `XxxPortIn` + `XxxUseCaseImpl`

If separating reads and writes:

- `XxxCommandUseCase`
- `XxxQueryUseCase`

Examples:

- `CreateAccountPortIn`
- `CheckInVehiclePortIn`
- `CloseShiftPortIn`
- `SubscriptionQueryPortIn`

Always define interface first, then implementation.

## 7. Controller rules

Controllers must:

- receive request
- trigger request validation
- call application use case
- return response DTOs
- stay under `entrypoint.controller.<schema>` and use DTOs from `entrypoint.dto.<schema>.<table>.request` and `entrypoint.dto.<schema>.<table>.response`

Controllers must not:

- contain core business logic
- call repositories directly
- expose entities directly
- hardcode persistence or security rules

Controllers should stay thin and predictable.

## 8. MapStruct rules

Always use MapStruct for mapping between layers.

Preferred mapping flow for non-trivial modules:

- Request DTO -> Domain model
- Domain model -> Response DTO
- Domain model <-> Persistence Entity

Create separate mappers when useful:

- `XxxApiMapper`
- `XxxPersistenceMapper`

Place them by responsibility:

- API/request-response mappers under `application.<schema>.<feature>.mapper`
- persistence mappers under `infrastructure.mapper.<schema>`

Use:

- `@Mapper(componentModel = "spring")`
- `uses = {...}`
- `@AfterMapping`
- `@MappingTarget`

Do not map audit fields from request DTOs.

## 9. Manual mapping rule

Manual mapping inside application use cases is allowed only when:

- business validation requires it
- mapping depends on repository or external service
- enrichment is clearer in `@AfterMapping`
- performance reasons are documented

Otherwise, use MapStruct.

## 10. Entity rule

JPA entities belong to persistence concerns.

Do not:

- expose entities directly to controllers
- place API serialization concerns in entities
- put non-trivial business logic inside entities

If audit is needed, use the shared audit abstraction adopted by the project.

Do not duplicate:

- `createdAt`
- `createdBy`
- `updatedAt`
- `updatedBy`

## 11. PostgreSQL persistence rule

The current schema is PostgreSQL-first.

Follow these rules:

- use `UUID` for identifiers
- use `Instant` for timestamp fields
- keep internal date/time data as `Instant`
- reuse `shared.utils.DateTimeUtils` for shared date/time parsing, formatting, and date-range conversion
- when API responses need to display date/time values, map them through `DateTimeUtils`
- for Vietnam date-based filtering and query boundaries, use `DateTimeUtils.startOfDayInVietnamInstant(...)`, `DateTimeUtils.startOfNextDayInVietnamInstant(...)`, and `DateTimeUtils.toVietnamLocalDate(...)`
- model case-insensitive emails consistently with the database `CITEXT` intent
- handle `JSONB`-backed fields with clear typed models or controlled mapping
- respect database defaults such as `gen_random_uuid()` and `now()`

Do not generate new entities with `Long` identifiers when the actual table uses `UUID`.

## 12. Status and enum rule

Application and domain enums must stay aligned with the actual schema constraints.

Place schema-related enums under `shared.enumeration.<schema>` instead of a flat shared enum package so the enum vocabulary stays aligned with the PostgreSQL schema map.

Important current status sets include:

- `people.user_profiles.status`: `ACTIVE`, `INACTIVE`, `SUSPENDED`
- `iam.accounts.status`: `ACTIVE`, `LOCKED`, `DISABLED`, `PENDING`
- `people.customers.status`: `ACTIVE`, `INACTIVE`
- `people.customers.approval_status`: `PENDING`, `APPROVED`, `REJECTED`, `SUSPENDED`
- `people.employees.status`: `ACTIVE`, `INACTIVE`, `SUSPENDED`
- `access_control.cards.status`: `AVAILABLE`, `ASSIGNED`, `IN_USE`, `LOST`, `BLOCKED`, `DAMAGED`, `RETIRED`
- `access_control.subscriptions.status`: `PENDING`, `ACTIVE`, `EXPIRED`, `CANCELLED`, `REJECTED`
- `parking.parking_sessions.status`: `OPEN`, `CLOSED`, `LOST_CARD`, `CANCELLED`
- `billing.invoices.status`: `UNPAID`, `PAID`, `CANCELLED`, `REFUNDED`
- `billing.payments.status`: `PENDING`, `SUCCESS`, `FAILED`, `REFUNDED`

Do not invent new values in code unless the schema is being changed intentionally.

When a module has both operational status and approval status, keep them semantically separate. For `people.customers`, use `status` for active or inactive lifecycle and `approval_status` only for approval workflow.

For `access_control.cards.status`, keep the following transition intent consistent with domain policy:

- `AVAILABLE -> ASSIGNED -> IN_USE` is the normal operational path.
- `ASSIGNED`, `IN_USE`, and `BLOCKED` may return to `AVAILABLE` only through explicit lifecycle operations such as release or unblock.
- `BLOCKED` is temporary and must stay distinct from `LOST`, `DAMAGED`, and `RETIRED`.
- `DAMAGED` describes card condition, while `RETIRED` is the terminal lifecycle decision.
- `delete` in API/use case should normally map to `RETIRED`, not physical delete.
- `IN_USE -> RETIRED` must be rejected directly.

## 13. Security and current user rule

Use the shared security abstraction for current user access.

Rules:

- application use cases should depend on a current-user abstraction, not framework-specific classes
- security context access must stay in infrastructure
- permission evaluation should be centralized
- do not create duplicate security adapters per feature

## 14. File and image rule

If file storage is used:

- keep file handling in infrastructure
- do not access storage providers directly from controllers
- persist file metadata only when required by business flows

This is especially relevant for camera snapshots, parking event images, and supporting files linked to reports or tickets.

## 15. Repository rule

Repositories belong to persistence concerns.

Rules:

- Spring Data repositories stay in infrastructure persistence packages
- JPA entities stay under `infrastructure.persistence.database.entity.<schema>`
- Spring Data repositories stay under `infrastructure.persistence.database.repository.<schema>`
- JPA `Specification` builders stay under `infrastructure.persistence.database.specification.<schema>`
- Do not add placeholder files under `infrastructure.persistence.database.specification.<schema>` just to preserve empty folders. Create files there only when a real specification class is needed.
- persistence adapters stay under `infrastructure.persistence.adapter.<schema>`
- application layer depends on repository ports, not repository implementations
- domain layer must not know JPA repository details
- when API responses depend on freshly generated audit values such as `updatedAt` or `updatedBy`, flush persistence changes before mapping the saved entity back to domain/response

## 16. Response rule

Every API must return proper response DTOs.

Use HTTP status codes intentionally:

- `201 Created` for successful `POST` create operations
- `200 OK` for successful `GET`, `PUT`, `PATCH`, and body-returning soft delete operations
- `204 No Content` only when the API intentionally returns no response body
- `400 Bad Request` for malformed requests and invalid business input that is not a uniqueness/resource conflict
- `401 Unauthorized` for missing or invalid authentication
- `403 Forbidden` for denied permissions
- `404 Not Found` for missing resources
- `409 Conflict` for duplicate unique data or state conflicts with existing resources
- `500 Internal Server Error` for unexpected failures

When needed, define separate response models for:

- Admin: full internal view
- User: restricted business-safe view

Do not expose internal audit fields, security metadata, workflow details, or hidden operational fields to user-facing responses unless explicitly required.

## 17. Testing rule

Always add tests when code behavior changes.

Preferred:

- unit tests for domain rules
- unit tests for use cases with mocked output ports
- persistence integration tests
- controller contract tests when needed
- use JUnit 5 for all test classes
- use `@ExtendWith(MockitoExtension.class)`, `@Mock`, and `@InjectMocks` for tests that mock collaborators such as repositories, ports, adapters, or external services
- keep pure domain rule and utility tests free of mocks when they have no collaborators

For core flows such as parking check-in/check-out, subscription approval, lost card resolution, invoicing, and payment, tests are mandatory.

## 18. Common mistakes to avoid

Avoid:

- fat controllers
- one generic use case class containing many unrelated flows
- direct entity exposure in APIs
- large manual mapping blocks in services
- business rules hidden inside repository queries
- infrastructure concerns leaking into domain
- duplicate permission or status resolution logic
- using old table vocabulary when the new schema already defines the replacement

## 18.1. Reuse-first implementation rule

Before implementing a new function, endpoint, use case, or business flow:

- read the nearest related module and check whether there are existing methods, services, mappers, adapters, validators, helpers, or policies that can be reused
- prefer enhancing an existing implementation when it can support the new requirement safely without breaking completed behavior
- avoid deleting code unless it is truly unnecessary, clearly inefficient, duplicated beyond safe maintenance, or logically incorrect
- create new code only when reuse is not suitable, and keep the new implementation aligned with the current Clean Architecture rules and schema-first package structure

## 19. How to use this file with the architecture guide

When starting a new module:

1. Read `docs/clean-architecture-guide.md` to decide the module shape
2. Return to this file to apply naming, mapping, entity, repository, PostgreSQL, and testing conventions

If there is conflict:

- architecture decisions follow `docs/clean-architecture-guide.md`
- coding conventions follow `docs/backend-coding-standard.md`
