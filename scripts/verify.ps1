$ErrorActionPreference = "Stop"

python scripts/static_audit.py
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

$gradleWrapper = Join-Path $PSScriptRoot "..\gradlew.bat"
if (-not (Test-Path $gradleWrapper)) {
    Write-Error "Gradle Wrapper not found: $gradleWrapper"
    exit 2
}

& $gradleWrapper --no-daemon `
    :app:testDebugUnitTest `
    :app:lintDebug `
    :app:assembleDebug `
    :app:assembleDebugAndroidTest `
    --stacktrace

exit $LASTEXITCODE
