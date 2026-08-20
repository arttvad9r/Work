#!/usr/bin/env sh
set -eu

python3 scripts/static_audit.py

if ! command -v gradle >/dev/null 2>&1; then
  echo "Gradle is not on PATH. Install trusted Gradle 9.5.0 or bootstrap the project wrapper." >&2
  exit 2
fi

gradle --no-daemon \
  :app:testDebugUnitTest \
  :app:lintDebug \
  :app:assembleDebug \
  :app:assembleDebugAndroidTest \
  --stacktrace
