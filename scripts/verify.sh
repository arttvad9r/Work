#!/usr/bin/env sh
set -eu

python3 scripts/static_audit.py

GRADLEW="${GRADLEW:-./gradlew}"
if [ ! -x "$GRADLEW" ]; then
  echo "Gradle Wrapper not found or not executable: $GRADLEW" >&2
  exit 2
fi

"$GRADLEW" --no-daemon \
  :app:testDebugUnitTest \
  :app:lintDebug \
  :app:assembleDebug \
  :app:assembleDebugAndroidTest \
  --stacktrace
