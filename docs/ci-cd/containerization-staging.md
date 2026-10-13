# Containerization and staging deployment

## What was prepared

- Backend and frontend use multi-stage Docker builds.
- The backend image runs as a non-root user and checks
  `/vehicle-management/actuator/health`.
- The frontend image serves static assets with Nginx, exposes `/healthz`, and
  proxies the relative API path to the Docker Compose backend service.
- `deploy/staging/docker-compose.yml` accepts only prebuilt image references;
  it does not use a `build:` section.
- `deploy/staging/.env.example` documents all staging variables without real
  values. The real `.env` remains ignored by Git.

## Local verification

From the repository root:

```powershell
docker build -t coparking-backend:local .\vehicle-management-backend\vehicle-management
docker build -t coparking-frontend:local .\vehicle-management-frontend\vehicle_management_react
docker image inspect coparking-backend:local
docker image inspect coparking-frontend:local
```

Validate the staging compose syntax without creating a real environment file:

```powershell
$env:STAGING_ENV_FILE = ".env.example"
docker compose -f deploy/staging/docker-compose.yml --env-file deploy/staging/.env.example config
```

The backend needs its external dependencies (PostgreSQL, Keycloak and configured
services) before its application health endpoint becomes healthy. The frontend
health endpoint can be checked after it starts:

```powershell
docker run --rm -p 8088:80 coparking-frontend:local
curl http://localhost:8088/healthz
```

## Concepts to present

### Docker image

A Docker image is an immutable package containing the runtime, application and
its required files. CI tags each image with the commit SHA, so a deployed version
can be identified and deployed again exactly.

### Docker Compose

Docker Compose describes how multiple containers run together. Here it connects
the frontend and backend on an internal network, provides restart behavior and
uses health checks before the frontend starts.

### Why secrets stay outside Git

Git history is durable and shared. A password, token or private key committed
once can remain recoverable even after it is deleted. Staging secrets therefore
belong in GitHub Environment Secrets and in the protected `.env` file or secret
manager on the staging host. Only variable names and safe templates are versioned.

## Current infrastructure limitation

The repository does not define a staging server, registry access, domain name or
secret manager. Therefore these files are deploy-ready templates, not evidence of
a completed staging deployment. The later CD workflow will use the same immutable
image tags once the required GitHub Environment secrets are configured.
