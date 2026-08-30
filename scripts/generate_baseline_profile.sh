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

startup_profile="$(find app/src -type f -path '*/generated/baselineProfiles/startup-prof.txt' -size +0c -print | head -n 1)"
if [ -z "$startup_profile" ]; then
  echo "Baseline Profile generation completed without a non-empty startup-prof.txt." >&2
  exit 1
fi

if ! grep -Fq 'Lcom/worktime/app/MainActivity;' "$profile"; then
  echo "Generated Baseline Profile does not contain source-level WorkTime descriptors; capture may be obfuscated." >&2
  exit 1
fi

for runtime_rule in \
  'Lcom/worktime/app/ui/calendar/CalendarPagerState;->navigateNext' \
  'Lcom/worktime/app/ui/calendar/CalendarViewModel;->showMonth'
do
  if ! grep -Fq "$runtime_rule" "$profile"; then
    echo "Generated Baseline Profile is missing calendar runtime rule: $runtime_rule" >&2
    exit 1
  fi
  if grep -Fq "$runtime_rule" "$startup_profile"; then
    echo "Calendar runtime rule leaked into Startup Profile: $runtime_rule" >&2
    exit 1
  fi
done

echo "Generated Baseline Profile: $profile"
wc -l "$profile"
echo "Generated Startup Profile: $startup_profile"
wc -l "$startup_profile"
