ARG POSTGIS_IMAGE=postgis/postgis:17-3.7@sha256:1f0d64ef12bfb344d3569ab6419aca183258b146951daec83f0585d01e6c9cc9
FROM ${POSTGIS_IMAGE}

ARG PG_MAJOR=17
ARG PGVECTOR_COMMIT=8ee86c96f0fd72390f890aa8a336fda6d3ab4c6c

# Compile pgvector against the exact PostgreSQL server shipped by the PostGIS
# base image. Installing a PostGIS binary package on top of a separately built
# PostgreSQL image can break whenever a PostgreSQL minor update changes the
# extension ABI.
ADD https://github.com/pgvector/pgvector.git#${PGVECTOR_COMMIT} /tmp/pgvector

RUN apt-get update \
    && apt-mark hold locales \
    && apt-get install -y --no-install-recommends \
        build-essential \
        postgresql-server-dev-${PG_MAJOR} \
    && cd /tmp/pgvector \
    && make clean \
    && make OPTFLAGS="" \
    && make install \
    && mkdir -p /usr/share/doc/pgvector \
    && cp LICENSE README.md /usr/share/doc/pgvector \
    && rm -rf /tmp/pgvector \
    && apt-get remove -y build-essential postgresql-server-dev-${PG_MAJOR} \
    && apt-get autoremove -y \
    && apt-mark unhold locales \
    && rm -rf /var/lib/apt/lists/*

COPY docker/postgres/healthcheck.sh /usr/local/bin/vehicle-postgres-healthcheck
COPY docker/postgres/initdb/001-enable-required-extensions.sql /docker-entrypoint-initdb.d/001-enable-required-extensions.sql

RUN if [ ! -e /usr/share/zoneinfo/Asia/Saigon ]; then \
        ln -s Ho_Chi_Minh /usr/share/zoneinfo/Asia/Saigon; \
    fi \
    && sed -i 's/\r$//' /usr/local/bin/vehicle-postgres-healthcheck \
    && chmod 0555 /usr/local/bin/vehicle-postgres-healthcheck \
    && chmod 0444 /docker-entrypoint-initdb.d/001-enable-required-extensions.sql

# The official entrypoint still initializes PostgreSQL as this account; running
# the final container as postgres prevents the database server from using root.
USER postgres
