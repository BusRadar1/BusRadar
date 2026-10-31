#!/usr/bin/env bash
# Error protection
set -Eeuo pipefail

# Confirmation on the existence of identifiers in the .env
: "${POSTGRES_USER:?POSTGRES_USER must be set}"
: "${KEYCLOAK_DB_NAME:?KEYCLOAK_DB_NAME must be set}"
: "${KEYCLOAK_DB_USER:?KEYCLOAK_DB_USER must be set}"
: "${KEYCLOAK_DB_PASSWORD:?KEYCLOAK_DB_PASSWORD must be set}"
: "${BUSRADAR_DB_NAME:?BUSRADAR_DB_NAME must be set}"
: "${BUSRADAR_DB_USER:?BUSRADAR_DB_USER must be set}"
: "${BUSRADAR_DB_PASSWORD:?BUSRADAR_DB_PASSWORD must be set}"

# Using psql format() and \gexec so passwords are SQL-literal-escaped and
# identifiers are safely quoted.
psql --username "$POSTGRES_USER" --dbname postgres \
  --set=keycloak_user="$KEYCLOAK_DB_USER" \
  --set=keycloak_password="$KEYCLOAK_DB_PASSWORD" \
  --set=keycloak_db="$KEYCLOAK_DB_NAME" \
  --set=busradar_user="$BUSRADAR_DB_USER" \
  --set=busradar_password="$BUSRADAR_DB_PASSWORD" \
  --set=busradar_db="$BUSRADAR_DB_NAME" <<'SQL'
SELECT format('CREATE ROLE %I LOGIN PASSWORD %L', :'keycloak_user', :'keycloak_password') \gexec
SELECT format('CREATE DATABASE %I OWNER %I', :'keycloak_db', :'keycloak_user') \gexec

SELECT format('CREATE ROLE %I LOGIN PASSWORD %L', :'busradar_user', :'busradar_password') \gexec
SELECT format('CREATE DATABASE %I OWNER %I', :'busradar_db', :'busradar_user') \gexec
SQL

psql --username "$BUSRADAR_DB_USER" --dbname "$BUSRADAR_DB_NAME" <<'SQL'
CREATE SCHEMA IF NOT EXISTS stats AUTHORIZATION CURRENT_USER;
CREATE SCHEMA IF NOT EXISTS trips AUTHORIZATION CURRENT_USER;
SQL
