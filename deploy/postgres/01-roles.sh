#!/bin/sh
# Se ejecuta una sola vez, al inicializar el volumen de datos.
# POSTGRES_USER es el dueño del esquema (Flyway ejecuta el DDL con él).
# DB_APP_USER es el usuario de la aplicación: solo DML, sin permisos para alterar el esquema.
set -eu

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" \
  -v owner="$POSTGRES_USER" -v db="$POSTGRES_DB" \
  -v app_user="$DB_APP_USER" -v app_pass="$DB_APP_PASSWORD" <<'EOSQL'
CREATE ROLE :"app_user" LOGIN PASSWORD :'app_pass';
REVOKE ALL ON DATABASE :"db" FROM PUBLIC;
GRANT CONNECT ON DATABASE :"db" TO :"app_user";
GRANT USAGE ON SCHEMA public TO :"app_user";
ALTER DEFAULT PRIVILEGES FOR ROLE :"owner" IN SCHEMA public
  GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO :"app_user";
ALTER DEFAULT PRIVILEGES FOR ROLE :"owner" IN SCHEMA public
  GRANT USAGE, SELECT ON SEQUENCES TO :"app_user";
EOSQL
