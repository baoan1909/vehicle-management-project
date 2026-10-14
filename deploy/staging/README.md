# Staging Docker Compose

This directory deploys prebuilt images only. The staging server must never build
source code; CI publishes an immutable image tag and deployment selects that tag.

## First-time setup

1. Install Docker Engine and Docker Compose plugin on the staging host.
2. Copy `.env.example` to `.env`.
3. Replace every `replace_with_...` value in `.env` using the staging secret
   manager or the host's protected environment file.
4. Authenticate Docker to the private registry if the images are private.
5. Start the selected immutable release:

   ```bash
   docker compose --env-file .env pull
   docker compose --env-file .env up -d --remove-orphans
   ```

6. Verify the backend health endpoint from the host:

   ```bash
   curl --fail http://127.0.0.1:8080/vehicle-management/actuator/health
   ```

## Update and rollback

To update, change only `BACKEND_IMAGE` and `FRONTEND_IMAGE` to the new immutable
SHA tags, then run the two Docker Compose commands above. To roll back, restore
the prior known-good SHA tags and run the same commands. Do not rebuild old source
on the staging host.

The compose file intentionally does not create a database, Keycloak, Redis,
RabbitMQ, MinIO, or OCR service. Those dependencies must be provisioned and
secured separately for staging; this prevents accidental replacement of
stateful data during application deployment.
