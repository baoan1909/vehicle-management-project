#!/bin/sh
set -eu

pg_isready --username "${POSTGRES_USER}" --dbname "${POSTGRES_DB}" >/dev/null

extension_count="$(
    psql \
        --username "${POSTGRES_USER}" \
        --dbname "${POSTGRES_DB}" \
        --tuples-only \
        --no-align \
        --command "SELECT count(*) FROM pg_extension WHERE extname IN ('vector', 'postgis');"
)"

[ "${extension_count}" = "2" ]
