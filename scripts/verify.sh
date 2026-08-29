#!/usr/bin/env sh
set -eu

python3 scripts/static_audit.py

if [ ! -x ./gradlew ]; then
  echo "Gradle Wrapper is missing or not executable." >&2
  exit 2
fi

./gradlew --no-daemon \
  :app:testDebugUnitTest \
  :app:lintDebug \
  :app:lintRelease \
  :app:assembleDebug \
  :app:assembleDebugAndroidTest \
  :app:bundleRelease \
  --stacktrace
