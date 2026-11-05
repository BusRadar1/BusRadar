# BusRadar
BusRadar Gdańsk: punktualność komunikacji miejskiej w czasie rzeczywistym na otwartych danych ZTM w Gdańsku


# How to start the environment

From the main folder of the app do:
```sh
cd infra
cp .env.example .env
docker compose --profile apps up -d --build
```
( Make sure that before starting you have .env file copied in the infra folder )

Open http://localhost:8080 to view the map.

Without the `apps` profile, only PostgreSQL, Redis, Kafka and Keycloak start:

```sh
docker compose up -d
```

Check container status:

```sh
docker compose --profile apps ps
```

Check all backend services through nginx:
( Use `curl.exe` in PowerShell )

```sh
curl http://localhost:8080/api/ingest/actuator/health
curl http://localhost:8080/api/tracking/actuator/health
curl http://localhost:8080/api/stats/actuator/health
curl http://localhost:8080/api/trips/actuator/health
```

Each endpoint should return `"status":"UP"`.


Make sure that in `infra/postgres/init/01-create-databases.sh` 
uses LF convention instead of CRLF.