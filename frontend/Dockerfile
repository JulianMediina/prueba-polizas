# syntax=docker/dockerfile:1.7
# Build en la plataforma del builder (el bundle es estático); imagen final sin RUN → multiarquitectura sin emulación.

FROM --platform=$BUILDPLATFORM node:26.12.1-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN --mount=type=cache,target=/root/.npm npm ci --no-audit --no-fund
COPY . .
RUN npx ng build --configuration production

FROM nginxinc/nginx-unprivileged:1.31.7-alpine
COPY nginx/default.conf.template /etc/nginx/templates/default.conf.template
COPY nginx/security-headers.conf /etc/nginx/snippets/security-headers.conf
COPY --from=build /app/dist/frontend/browser /usr/share/nginx/html
ENV BACKEND_HOST=backend \
    ORIGIN_SECRET=""
EXPOSE 8080
HEALTHCHECK --interval=15s --timeout=3s --start-period=10s --retries=5 \
  CMD wget -qO- --header "X-Origin-Verify: ${ORIGIN_SECRET}" http://127.0.0.1:8080/healthz || exit 1
