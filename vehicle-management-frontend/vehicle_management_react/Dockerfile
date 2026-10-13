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

FROM nginx:1.27-alpine

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=5s --start-period=15s --retries=3 \
    CMD wget -q -O /dev/null http://localhost/healthz || exit 1

CMD ["nginx", "-g", "daemon off;"]
