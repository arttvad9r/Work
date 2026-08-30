#!/usr/bin/env sh
set -eu

ROOT="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
cd "$ROOT"

if [ ! -x ./gradlew ]; then
  echo "Gradle Wrapper is missing or not executable." >&2
  exit 2
fi

./gradlew --no-daemon \
  :app:generateBaselineProfile \
  -Pandroid.testInstrumentationRunnerArguments.androidx.benchmark.enabledRules=BaselineProfile \
  -Pandroid.testoptions.manageddevices.emulator.gpu=swiftshader_indirect \
  --stacktrace

profile="$(find app/src -type f -path '*/generated/baselineProfiles/baseline-prof.txt' -size +0c -print | head -n 1)"
if [ -z "$profile" ]; then
  echo "Baseline Profile generation completed without a non-empty baseline-prof.txt." >&2
  exit 1
fi

echo "Generated profile: $profile"
wc -l "$profile"
