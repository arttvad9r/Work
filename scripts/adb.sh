#!/usr/bin/env bash
set -euo pipefail

if command -v steam-run >/dev/null 2>&1; then
  exec steam-run adb "$@"
fi

exec adb "$@"
