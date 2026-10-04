# Keycloak Assets

This folder is owned by the frontend project because it contains user-facing
Keycloak themes and the sanitized local bootstrap template.

Contents:

- `themes/`: Keycloak login and email theme resources.
- `bootstrap/realm-template.json`: version-controlled realm bootstrap template.
- `export/`: local runtime exports. This directory is ignored and must never be
  committed.

The bootstrap template must contain environment placeholders instead of real
SMTP passwords or confidential client secrets. It must not contain generated
KeyProvider secrets, private keys, or real user credentials. The backend
`docker-compose.keycloak.yml` mounts the template read-only under the filename
expected by Keycloak startup import.

Required local variables are documented in the backend `.env.example`. Keep
their real values only in the ignored `.env` file or an external secret
manager.

Do not commit a raw Keycloak export. If an export is needed for diagnostics,
write it under `keycloak/export/`, sanitize it, and copy only the reviewed
non-secret configuration into the bootstrap template.
