# Keycloak Secret Remediation Runbook

This runbook separates secret rotation, source cleanup, and Git history rewrite.
Deleting a value from the current branch does not revoke it and does not remove
it from existing commits, clones, CI artifacts, or backups.

## 1. Containment and rotation

Complete every item before rewriting Git history:

1. Revoke the exposed SMTP app password and create a replacement only after the
   mailbox account has been secured.
2. Regenerate the `vehicle-management-admin-service` client secret in Keycloak,
   update the ignored local environment or deployment secret manager, restart
   the backend, and verify the old secret cannot obtain a token.
3. Create new Keycloak signing and encryption providers, make the new providers
   active, remove the exposed providers, revoke realm sessions, and verify the
   old signing `kid` is absent from JWKS.
4. Reset or disable every user whose credential appeared in a Keycloak user
   export and revoke those users' sessions.
5. Notify collaborators to stop pushing until the history rewrite is complete.

## 2. Current source layout

- `keycloak/bootstrap/realm-template.json` is the only realm bootstrap file
  allowed in version control.
- `keycloak/export/` is ignored and is only for short-lived local diagnostics.
- The bootstrap template contains environment placeholders, no generated
  KeyProvider material, and no real user credentials.
- Real values belong in an ignored `.env` for local development or an external
  secret manager for deployed environments.

## 3. History rewrite

Perform this operation from a clean maintenance clone after the rotation and
team freeze are confirmed. Record the current remote URL because
`git filter-repo` can remove the `origin` remote as a safety measure.

```bash
git filter-repo --force \
  --path vehicle-management-backend/vehicle-management/keycloak/import/vehicle-management-realm.json \
  --path vehicle-management-backend/vehicle-management/keycloak/import/vehicle-management-users-0.json \
  --path vehicle-management-frontend/vehicle_management_react/keycloak/import/vehicle-management-realm.json \
  --path vehicle-management-frontend/vehicle_management_react/keycloak/import/vehicle-management-users-0.json \
  --invert-paths
```

Before running the command, create an untracked `replacements.txt` for any
additional non-export findings reported by Gitleaks, such as obsolete token
examples in documentation or test fixtures. Use `git filter-repo
--replace-text replacements.txt` in the same maintenance pass, then securely
delete that file. Never commit the replacement file because its left-hand side
contains the historical value being removed.

Run Gitleaks across the rewritten history before publishing it:

```bash
gitleaks git --config .gitleaks.toml --redact --verbose .
```

After review and explicit team approval, restore the remote if necessary and
force-push every maintained branch and tag:

```bash
git push --force --all origin
git push --force --tags origin
```

Delete obsolete remote branches separately after their owners confirm they are
no longer needed. All collaborators must discard old clones and clone again.
Review pull-request artifacts, CI artifacts, releases, mirrors, and backups.

## 4. Verification

- The old SMTP credential fails authentication.
- The old admin client secret cannot obtain a client-credentials token.
- The old Keycloak signing `kid` is absent from JWKS and tokens signed by the
  exposed key are rejected.
- A clean Keycloak database imports the sanitized template and generates new
  signing keys.
- The service account can perform only its intended user-management actions.
- Customer and Partner registration can send `VERIFY_EMAIL` using the rotated
  SMTP credential.
- Gitleaks passes for both the working tree and the rewritten Git history.
