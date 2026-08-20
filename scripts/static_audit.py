#!/usr/bin/env python3
"""Fast repository invariants that do not require Android SDK/Gradle."""

from __future__ import annotations

import re
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
APP = ROOT / "app"
failures: list[str] = []


def fail(message: str) -> None:
    failures.append(message)


def parse_xml(path: Path) -> ET.Element:
    try:
        return ET.parse(path).getroot()
    except Exception as exc:  # noqa: BLE001
        fail(f"XML parse failed: {path.relative_to(ROOT)}: {exc}")
        return ET.Element("invalid")


manifest_path = APP / "src/main/AndroidManifest.xml"
manifest = parse_xml(manifest_path)
manifest_text = manifest_path.read_text(encoding="utf-8")

for permission in (
    "android.permission.INTERNET",
    "android.permission.ACCESS_FINE_LOCATION",
    "android.permission.ACCESS_COARSE_LOCATION",
    "android.permission.READ_CONTACTS",
    "android.permission.RECORD_AUDIO",
    "android.permission.CAMERA",
):
    if permission in manifest_text:
        fail(f"Unexpected runtime/privacy-sensitive permission: {permission}")

if 'android:allowBackup="false"' not in manifest_text:
    fail("Android backup must remain disabled for the v1 local-only policy")

for xml_path in (
    APP / "src/main/res/values/strings.xml",
    APP / "src/main/res/values-ru/strings.xml",
    APP / "src/main/res/values/themes.xml",
    APP / "src/main/res/values-night/themes.xml",
    APP / "src/main/res/drawable/ic_launcher.xml",
):
    parse_xml(xml_path)


def string_keys(path: Path) -> set[str]:
    root = parse_xml(path)
    return {node.attrib["name"] for node in root.findall("string") if "name" in node.attrib}


base_strings = APP / "src/main/res/values/strings.xml"
ru_strings = APP / "src/main/res/values-ru/strings.xml"
base_keys = string_keys(base_strings)
ru_keys = string_keys(ru_strings)
if base_keys != ru_keys:
    missing_ru = sorted(base_keys - ru_keys)
    extra_ru = sorted(ru_keys - base_keys)
    if missing_ru:
        fail(f"Russian resources missing keys: {', '.join(missing_ru)}")
    if extra_ru:
        fail(f"Russian resources have extra keys: {', '.join(extra_ru)}")

framework_money_patterns = re.compile(r"\b(?:Float|Double)\b|\.toFloat\(|\.toDouble\(")
for source_root in (
    APP / "src/main/java/com/worktime/app/domain",
    APP / "src/main/java/com/worktime/app/data",
):
    for kotlin_file in source_root.rglob("*.kt"):
        text = kotlin_file.read_text(encoding="utf-8")
        if framework_money_patterns.search(text):
            fail(f"Binary floating-point token in domain/data: {kotlin_file.relative_to(ROOT)}")

for kotlin_file in (APP / "src/main/java").rglob("*.kt"):
    text = kotlin_file.read_text(encoding="utf-8")
    if "fallbackToDestructiveMigration" in text:
        fail(f"Destructive Room fallback found: {kotlin_file.relative_to(ROOT)}")

build_file = (APP / "build.gradle.kts").read_text(encoding="utf-8")
if 'testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"' not in build_file:
    fail("AndroidJUnitRunner is not configured")
if 'androidx.test:runner:1.7.0' not in build_file:
    fail("Explicit androidx.test:runner dependency is missing")

if failures:
    print("static-audit: FAILED", file=sys.stderr)
    for item in failures:
        print(f" - {item}", file=sys.stderr)
    raise SystemExit(1)

print(
    "static-audit: OK "
    f"({len(base_keys)} localized string keys; XML/privacy/domain invariants passed)"
)
