#!/bin/sh

# Check if MB_DB_PASS is unset or empty
if [ -z "${MB_DB_PASS}" ]; then
    echo 'You must set MB_DB_PASS to the password'
    exit 1
fi

docker run --rm -p 3000:3000 \
  -e "MB_DB_TYPE=postgres" \
  -e "MB_DB_DBNAME=metabaseappdb" \
  -e "MB_DB_PORT=5433" \
  -e "MB_DB_USER=evan" \
  -e "MB_DB_PASS=$MB_DB_PASS" \
  -e "MB_DB_HOST=host.docker.internal" \
  --name metabase metabase/metabase:latest