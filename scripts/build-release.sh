#!/usr/bin/env bash
set -euo pipefail

if [[ -z "${QUIET_KEYSTORE_PATH:-}" || -z "${QUIET_KEYSTORE_PASSWORD:-}" || -z "${QUIET_KEY_ALIAS:-}" || -z "${QUIET_KEY_PASSWORD:-}" ]]; then
  echo "Set QUIET_KEYSTORE_PATH, QUIET_KEYSTORE_PASSWORD, QUIET_KEY_ALIAS, and QUIET_KEY_PASSWORD."
  exit 1
fi

./gradlew :app:assembleRelease \
  -Pandroid.injected.signing.store.file="$QUIET_KEYSTORE_PATH" \
  -Pandroid.injected.signing.store.password="$QUIET_KEYSTORE_PASSWORD" \
  -Pandroid.injected.signing.key.alias="$QUIET_KEY_ALIAS" \
  -Pandroid.injected.signing.key.password="$QUIET_KEY_PASSWORD"
