#!/bin/sh
set -e

# Sync with Transifex only when explicitly enabled and credentials are available.
if { [ "${TRANSIFEX_PUSH}" = "true" ] || [ "${TRANSIFEX_PULL}" = "true" ]; } \
  && { [ -n "${TRANSIFEX_TOKEN}" ] || [ -f "${HOME}/.transifexrc" ]; }; then
  /transifex/sync_transifex.sh \
    --resource openlmis-fulfillment \
    --pattern 'src/main/resources/messages_<lang>.properties' \
    --source-file src/main/resources/messages_en.properties
fi

# Run the build with the project wrapper version.
./gradlew --no-daemon clean build integrationTest
