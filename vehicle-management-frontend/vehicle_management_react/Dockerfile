FROM node:22-alpine AS build

WORKDIR /app

ARG VITE_APP_NAME=CoParking
ARG VITE_API_BASE_URL=/vehicle-management/api
ARG VITE_ASSET_BASE_URL=/assets
ARG VITE_KEYCLOAK_LOGIN_URL=

ENV VITE_APP_NAME=$VITE_APP_NAME \
    VITE_API_BASE_URL=$VITE_API_BASE_URL \
    VITE_ASSET_BASE_URL=$VITE_ASSET_BASE_URL \
    VITE_KEYCLOAK_LOGIN_URL=$VITE_KEYCLOAK_LOGIN_URL

COPY package*.json ./
RUN npm ci

COPY . .
RUN npm run build

# Nginx 1.29 on Alpine 3.23 includes the patched OS package line reported by Trivy.
FROM nginx:1.29.8-alpine3.23

# Refresh packages, move the pid outside /var/run, and give the Nginx account
# ownership of the locations it must write to at runtime.
RUN apk upgrade --no-cache \
    && sed -i 's#/var/run/nginx.pid#/tmp/nginx.pid#' /etc/nginx/nginx.conf \
    && mkdir -p /var/cache/nginx /var/log/nginx \
    && chown -R nginx:nginx /var/cache/nginx /var/log/nginx /usr/share/nginx/html

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --chown=nginx:nginx --from=build /app/dist /usr/share/nginx/html

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=15s --retries=3 \
    CMD wget -q -O /dev/null http://localhost:8080/healthz || exit 1

USER nginx

CMD ["nginx", "-g", "daemon off;"]
