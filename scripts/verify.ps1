$ErrorActionPreference = "Stop"

python scripts/static_audit.py
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

if (-not (Get-Command gradle -ErrorAction SilentlyContinue)) {
    Write-Error "Gradle is not on PATH. Install trusted Gradle 9.5.0 or bootstrap the project wrapper."
    exit 2
}

gradle --no-daemon `
    :app:testDebugUnitTest `
    :app:lintDebug `
    :app:assembleDebug `
    :app:assembleDebugAndroidTest `
    --stacktrace

exit $LASTEXITCODE
