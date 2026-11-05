# Build the Angular application.
FROM node:24-alpine AS build

WORKDIR /app

# Install dependencies from the lockfile.
# Docker can reuse this layer until dependency files change.
COPY package.json package-lock.json ./
RUN npm ci

COPY . .
RUN npm run build -- --configuration production

# Serve the compiled frontend using nginx.
FROM nginx:stable-alpine AS runtime

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist/busradar/browser /usr/share/nginx/html

EXPOSE 80