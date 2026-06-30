#!/bin/sh
set -e

# Render sets PORT; Spring Boot reads SERVER_PORT
if [ -n "$PORT" ]; then
  export SERVER_PORT="$PORT"
fi

# Convert Render/Heroku-style postgres:// URLs to JDBC + separate credentials
if printf '%s' "$DATABASE_URL" | grep -qE '^postgres(ql)?://'; then
  DB_URL_NO_SCHEME=$(printf '%s' "$DATABASE_URL" | sed -E 's/^postgres(ql)?:\/\///')
  export DATABASE_USERNAME=$(printf '%s' "$DB_URL_NO_SCHEME" | sed -E 's/^([^:@/]+)(:([^@/]*))?@.*/\1/')
  export DATABASE_PASSWORD=$(printf '%s' "$DB_URL_NO_SCHEME" | sed -E 's/^([^:@/]+)(:([^@/]*))?@.*/\3/')
  DB_HOST_PORT_DB=$(printf '%s' "$DB_URL_NO_SCHEME" | sed -E 's/^[^@]+@//')
  export DATABASE_URL="jdbc:postgresql://${DB_HOST_PORT_DB}"
fi

mkdir -p "${FILE_UPLOAD_BASE:-/app/uploads}/products"

exec java -jar app.jar
