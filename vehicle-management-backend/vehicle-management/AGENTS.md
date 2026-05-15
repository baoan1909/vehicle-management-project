# AGENTS.md

## Purpose

This repository follows Clean Architecture with DDD-style naming, but it must stay aligned with the actual PostgreSQL schema in `vehicle_management.sql`.

Before generating or modifying code, always read:

- `docs/backend-coding-standard.md`
- `docs/clean-architecture-guide.md`
- `docs/package-structure.md` when the task changes package layout, module scaffolding, or folder structure

Use them with this priority:

1. `docs/clean-architecture-guide.md` for architecture, module boundaries, and layer responsibilities
2. `docs/backend-coding-standard.md` for coding conventions, naming, mapping, persistence rules, and testing
3. `docs/package-structure.md` for package tree and schema-first folder layout

## Current schema map

The database is already organized by business schemas. Generated code must respect this structure and vocabulary:

- `iam`: roles, permissions, accounts, refresh tokens, login attempts, account status history
- `people`: user profiles, customers, employees, customer vehicles
- `catalog`: vehicle types, ticket types, card types, price plans, price rules, holiday calendar
- `access_control`: cards, subscriptions, lost card reports
- `parking`: parking lots, zones, spaces, lanes, parking sessions, parking events
- `billing`: invoices, payments
- `operations`: shifts, shift assignments, approval requests, support tickets
- `hardware`: devices
- `notification`: notifications
- `audit`: audit logs

Do not invent new core modules or persistence concepts when the current schema already defines them.

## Core rules

- Always prefer clean, maintainable, production-ready code.
- Avoid duplicate code. Reuse existing abstractions before creating new classes.
- Always check whether an existing mapper, adapter, validator, helper, policy, or shared component already exists.
- Keep controllers thin.
- Put business logic in application and domain layers, not controllers.
- Prefer cohesive use cases over giant application classes.
- Always use `Instant` for timestamp fields in backend code unless a very specific reason is documented.
- Keep internal timestamp data as `Instant`.
- Reuse `shared.utils.DateTimeUtils` for shared date/time parsing, formatting, and day-range calculations.
- API-visible date/time values must go through `DateTimeUtils` before being returned for display.
- For Vietnam day-based queries, use `DateTimeUtils.startOfDayInVietnamInstant(...)`, `DateTimeUtils.startOfNextDayInVietnamInstant(...)`, and `DateTimeUtils.toVietnamLocalDate(...)`.
- For PostgreSQL identifiers, prefer `UUID`.
- If an entity mirrors auditable tables with `created_at`, `created_by`, `updated_at`, `updated_by`, it should follow the shared audit abstraction used by the project.
- Current user access must respect the existing security abstraction.

## Mandatory architecture rules

- Follow Clean Architecture strictly: controller -> application -> domain -> infrastructure.
- Do not mix layers or put business logic in controllers.
- Controller layer uses Request DTO and Response DTO only.
- Application layer uses interface-first use cases, then implementations.
- Domain layer contains business rules and domain models when the use case is not trivial.
- Infrastructure layer contains persistence adapters, security adapters, and external integrations.
- Do not expose JPA entities directly from controllers.
- Do not inject repositories directly into controllers.

## Entrypoint package structure

- Keep REST controllers under `entrypoint.controller.<schema>`.
- Keep API DTOs under `entrypoint.dto.<schema>.<table>.request` and `entrypoint.dto.<schema>.<table>.response`.
- Do not place DTO packages under `entrypoint.controller`.
- Keep package naming aligned with existing code conventions such as `accesscontrol` for the `access_control` schema.

## Schema-aligned design rules

- `iam.accounts` is the authentication account. Do not merge its responsibility with `people.user_profiles`.
- `people.user_profiles` is the shared personal profile for admin, employee, and customer-related people data.
- `people.customers` and `people.employees` are role-specific business records built on top of `people.user_profiles`.
- Authorization is currently role-based through `iam.roles`, `iam.permissions`, and `iam.role_permissions`.
- Do not introduce `account_permissions` or other new authorization tables unless the task explicitly requires a schema change.
- `access_control.subscriptions` is the registered ticket and subscription table. Do not create a parallel monthly-ticket concept.
- `parking.parking_sessions` and `parking.parking_events` are core operational aggregates and must be treated as non-trivial business flows.
- `billing.invoices` and `billing.payments` are separate concerns. Do not collapse payment data into session or subscription tables at application level.
- `operations.approval_requests` and `audit.audit_logs` are generic cross-module tables. Reuse them instead of inventing per-feature approval or audit tables when the existing model is sufficient.

## Naming rules

- `CreateXxxRequest` for create input
- `UpdateXxxRequest` for update input
- `XxxResponse` for general output
- `XxxAdminResponse` for admin-facing output
- `XxxUserResponse` for user-facing output
- `XxxController` for REST controllers
- `XxxUseCase` for interfaces
- `XxxUseCaseImpl` for implementations
- `XxxMapper` for MapStruct mappers
- `XxxPersistenceAdapter` for persistence adapters
- `XxxSecurityAdapter` for security adapters

## Mapping rules

- Always use MapStruct for mapping between layers.
- Preferred flow for non-trivial modules:
  - Request DTO -> Domain
  - Domain -> Response DTO
  - Domain <-> Entity via persistence mapper
- Do not write large manual mapping blocks in application use cases.
- Manual mapping is allowed only when:
  - validation or business rule requires it
  - mapping depends on repository or external service
  - mapping is done in `@AfterMapping`

## Reuse rules

- Do not create one security adapter per feature if the logic is identical.
- Shared concerns such as current user access, audit handling, paging response, time handling, file handling, and exception translation must be reused from common components.
- If a new class only proxies an existing method with identical logic, prefer a shared adapter instead of a feature-specific copy.
- Before creating a new class, check if a mapper, adapter, helper, validator, or policy already exists.
- Before implementing any new feature or flow, read the nearest related code and check whether existing methods, services, mappers, adapters, validators, helpers, or policies can be reused.
- Prefer extending or generalizing an existing implementation when it can support the new requirement without breaking completed behavior or violating current architecture rules.
- Avoid deleting code by default. Only remove code when it is truly unnecessary, clearly inefficient, duplicated beyond safe maintenance, or logically incorrect.
- If no reusable implementation is suitable, create a new one only after confirming reuse is not reasonable, and keep the new code aligned with the current Clean Architecture boundaries.

## PostgreSQL rules

- Prefer `UUID` for identifiers in domain entities and persistence entities that reflect the current schema.
- Keep UUID strategy consistent within the same bounded context.
- Respect PostgreSQL-specific types already modeled in the schema, such as `TIMESTAMPTZ`, `CITEXT`, and `JSONB`.

## Status and enum rules

- Align code-level enums with the actual database check constraints.
- Do not invent status values that do not exist in the schema unless the task explicitly includes a schema change.
- When a table already models status separately, keep that boundary. For example:
  - `people.user_profiles.status`
  - `iam.accounts.status`
  - `access_control.cards.status`
  - `access_control.subscriptions.status`
  - `parking.parking_sessions.status`

## Response design rule

- Every API must return proper Response DTOs.
- Always design separate response models for:
  - Admin: full data, includes audit fields if needed
  - User: only necessary fields, hide internal or audit data

Do not expose internal fields such as `createdAt`, `createdBy`, `updatedAt`, `updatedBy`, security metadata, or internal workflow status to user-facing responses unless explicitly required.

## Before generating code

Before adding new code:

1. Read `docs/backend-coding-standard.md`
2. Read `docs/clean-architecture-guide.md`
3. Read the nearest relevant section of `vehicle_management.sql`
4. Inspect the nearest relevant package and reuse its style
5. Check whether a shared abstraction already exists
6. Preserve consistency with the current module unless a stronger project rule overrides it

## For review tasks

When reviewing code, check:

- clean architecture boundaries
- duplicate code
- MapStruct usage
- naming consistency
- audit consistency
- time handling with `Instant`
- UUID and PostgreSQL type consistency
- status and enum consistency with schema
- correct separation between admin responses and user responses

## Testing rule

- Always add tests when code behavior changes.
- Prefer unit tests for domain and application logic.
- Add integration tests when persistence or infrastructure behavior changes.
- Use JUnit 5 for all test classes.
- For tests with mocked collaborators such as use cases, adapters, or services that depend on ports or repositories, use `@ExtendWith(MockitoExtension.class)` together with `@Mock` and `@InjectMocks`.
- For pure domain policy, utility, or value-object tests with no collaborators, instantiate the subject directly and do not add mocks only for style consistency.
