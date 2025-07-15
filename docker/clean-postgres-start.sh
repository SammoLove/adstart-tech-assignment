#!/bin/bash

set -e

echo "Stopping and removing containers..."
docker compose down -v --remove-orphans

echo "Cleaning up the volume with data (if it exists)"
volume_name=$(docker volume ls --format '{{.Name}}' | grep domain-watchdog_pgdata || true)
if [ ! -z "$volume_name" ]; then
  docker volume rm "$volume_name"
  echo "Volume $volume_name has been removed."
else
  echo "Volume pgdata not found — moving on."
fi

echo "Starting containers again..."
docker compose up -d

echo "Checking Postgres status..."
until docker exec domain-watchdog-postgres pg_isready -U "${DB_USERNAME}" -d "${DB_NAME}" > /dev/null 2>&1; do
  echo "Waiting for database to become ready..."
  sleep 2
done

echo "Postgres is ready!"