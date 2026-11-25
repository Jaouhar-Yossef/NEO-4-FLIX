# Build stage: pinned Node 24 (Angular 22 supports ^24.15.0).
FROM node:24.22.3-alpine3.24 AS build
WORKDIR /app
# Lockfile first: the npm ci layer is reused until dependencies change.
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npx ng build

# Serve stage: only the built files and the nginx config, no Node.
FROM nginx:1.30.7-alpine3.24
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist/neo4flix/browser /usr/share/nginx/html
EXPOSE 80
