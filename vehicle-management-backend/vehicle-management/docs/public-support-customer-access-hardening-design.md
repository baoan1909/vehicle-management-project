# Public support and customer access hardening design

## Current architecture and root causes

- `CurrentAccountAccess` is the authoritative request snapshot for account/role state,
  assigned and lifecycle-filtered permissions, scopes, and `accessRevision`.
- Support ownership is enforced by `SupportTicketAccessGuard`; chat requires active membership.
- Support previously reused the approved-customer resolver and a wide profile projection, so
  `PENDING`/`REJECTED` customers could not use their explicitly assigned support permissions.
- Redis already has one shared connection and namespace. Guest assistant throttling was the
  remaining process-local counter.
- AI knowledge scope is server-resolved and tools are re-authorized when they execute.

## Design

1. `CustomerIdentityResolver` exposes an explicit support resolver and retains the approved
   resolver for business/private workflows. Both derive the account from authoritative security
   state and use one narrow customer-id query.
2. `PermissionAccessPrerequisitePolicy` contains an explicit pre-approval support allowlist and
   explicit approved-customer prerequisites. Permission suffixes and role codes never grant access.
3. Guest limits use a Redis Lua fixed-window operation (`INCR`, first-write `PEXPIRE`, `PTTL`).
   Keys use the existing environment/service namespace, a versioned bucket, and a hashed identity.
   Redis failure is `503`; local-memory fallback is available only through an explicit non-production
   switch.
4. Cloudflare Turnstile implements `PublicChallengeVerificationPort`. Successful verification
   issues a short-lived opaque proof; Redis stores only its hash and atomically consumes it once.
   The proof is bound to purpose, hashed session, and hashed IP. Intake and assistant purposes differ.
5. Approval/customer-state invalidations publish after commit. The frontend treats realtime only as
   a refresh signal and keeps REST `current-access`, revision ordering, dedupe, and single-flight
   refresh authoritative.

## Error and migration contract

- `401`: invalid authentication; `403`: permission/lifecycle/ownership; `404`: enumeration-safe
  resource/capability miss; `422`: authenticated account has no support customer identity;
  `429`: distributed limit with `Retry-After`; `503`: Redis/CAPTCHA/provider unavailable.
- No PostgreSQL migration is required. The narrow lookup uses the existing unique account/profile
  and customer/profile indexes. Redis is non-authoritative and every new key has a TTL.
- Rollback restores the previous application artifact. Redis keys expire without a data migration;
  PostgreSQL ownership data and response contracts remain unchanged.

## Verification plan

- Unit and integration coverage for lifecycle policy, identity, ownership, knowledge/tool scope,
  Redis multi-instance concurrency/TTL/opaque keys, CAPTCHA validation/replay, and failure semantics.
- Query-count tests for current access and support requests.
- Approval/revoke after-commit invalidation and access revision coverage.
- Frontend static, TypeScript, production build, auth lifecycle E2E, and guest support/CAPTCHA E2E.
- Full Maven regression, static-analysis profile, and fresh PostgreSQL/Flyway migration verification.
