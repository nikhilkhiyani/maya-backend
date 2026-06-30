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

UPLOAD_BASE="${FILE_UPLOAD_BASE:-/app/uploads}"
mkdir -p "${UPLOAD_BASE}/products" "${UPLOAD_BASE}/categories"

# Seed bundled uploads onto persistent disk when empty (Render disk mount)
if [ -d /app/bundled-uploads ] && [ -z "$(ls -A "${UPLOAD_BASE}/products" 2>/dev/null)" ]; then
  echo "Seeding uploads from bundled image to ${UPLOAD_BASE}"
  cp -R /app/bundled-uploads/. "${UPLOAD_BASE}/"
fi

exec java -jar app.jar
