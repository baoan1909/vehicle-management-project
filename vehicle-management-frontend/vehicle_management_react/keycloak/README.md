# Keycloak Assets

This folder is owned by the frontend project because it contains user-facing
Keycloak theme and realm import assets.

Contents:

- `themes/`: Keycloak login and email theme resources.
- `import/`: local development realm and seed user import files.

The backend still keeps `docker-compose.keycloak.yml` for local infrastructure,
but that compose file mounts this frontend-owned folder into the Keycloak
container.
