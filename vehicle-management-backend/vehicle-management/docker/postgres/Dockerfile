ARG PGVECTOR_IMAGE=pgvector/pgvector:0.8.6-pg17
FROM ${PGVECTOR_IMAGE}

ARG PG_MAJOR=17

RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        postgresql-${PG_MAJOR}-postgis-3 \
        postgresql-${PG_MAJOR}-postgis-3-scripts \
    && rm -rf /var/lib/apt/lists/*

COPY docker/postgres/healthcheck.sh /usr/local/bin/vehicle-postgres-healthcheck
COPY docker/postgres/initdb/001-enable-required-extensions.sql /docker-entrypoint-initdb.d/001-enable-required-extensions.sql

RUN chmod 0555 /usr/local/bin/vehicle-postgres-healthcheck \
    && chmod 0444 /docker-entrypoint-initdb.d/001-enable-required-extensions.sql
