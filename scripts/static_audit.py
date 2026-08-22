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
parse_xml(manifest_path)
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

for expected in (
    'android:allowBackup="false"',
    'android:dataExtractionRules="@xml/data_extraction_rules"',
    'android:fullBackupContent="@xml/backup_rules"',
    'android:screenOrientation="portrait"',
    'android:windowSoftInputMode="adjustResize"',
):
    if expected not in manifest_text:
        fail(f"Missing required manifest control: {expected}")

xml_paths = (
    APP / "src/main/res/values/strings.xml",
    APP / "src/main/res/values-ru/strings.xml",
    APP / "src/main/res/values/themes.xml",
    APP / "src/main/res/values-night/themes.xml",
    APP / "src/main/res/drawable/ic_launcher.xml",
    APP / "src/main/res/xml/data_extraction_rules.xml",
    APP / "src/main/res/xml/backup_rules.xml",
)
for xml_path in xml_paths:
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

obsolete_validation_keys = {
    "hours_range_error",
    "minutes_range_error",
    "duration_24h_error",
    "invalid_money_value",
    "money_value_too_large",
    "hourly_rate_required",
}
if obsolete_validation_keys & base_keys:
    fail("Obsolete helper-text validation strings are present; numeric validation is outline-only")

string_ref_pattern = re.compile(r"\bR\.string\.([A-Za-z0-9_]+)")
for kotlin_file in (APP / "src/main/java").rglob("*.kt"):
    referenced_keys = set(string_ref_pattern.findall(kotlin_file.read_text(encoding="utf-8")))
    missing_keys = sorted(referenced_keys - base_keys)
    if missing_keys:
        fail(
            f"Missing string resource(s) referenced by {kotlin_file.relative_to(ROOT)}: "
            + ", ".join(missing_keys)
        )

expected_domains = {
    "root",
    "file",
    "database",
    "sharedpref",
    "external",
    "device_root",
    "device_file",
    "device_database",
    "device_sharedpref",
}
data_rules = parse_xml(APP / "src/main/res/xml/data_extraction_rules.xml")
for section_name in ("cloud-backup", "device-transfer"):
    section = data_rules.find(section_name)
    if section is None:
        fail(f"data extraction rules missing {section_name}")
        continue
    excluded = {node.attrib.get("domain") for node in section.findall("exclude")}
    if excluded != expected_domains:
        fail(f"{section_name} does not exclude every app-data domain")

legacy_rules = parse_xml(APP / "src/main/res/xml/backup_rules.xml")
legacy_excluded = {node.attrib.get("domain") for node in legacy_rules.findall("exclude")}
if legacy_excluded != expected_domains:
    fail("legacy backup rules do not exclude every app-data domain")

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

calendar_screen = (
    APP / "src/main/java/com/worktime/app/ui/calendar/CalendarScreen.kt"
).read_text(encoding="utf-8")
if "sheetDragHandle = null" not in calendar_screen:
    fail("Monthly report must not use Material's tooltip-wrapped sheet drag-handle slot")
if "sheetDragHandle = { PlainDragHandle() }" in calendar_screen:
    fail("Tooltip-prone Material sheet drag-handle slot regressed")
if "Crossfade(" in calendar_screen or "animateColorAsState" in calendar_screen:
    fail("Calendar month navigation must not crossfade/reuse animated day-cell state")
if "formatWholeAmountMicros(totalMicros, locale)" not in calendar_screen:
    fail("Calendar day cells must display whole amounts without fractional digits")
if ".padding(horizontal = 4.dp)" not in calendar_screen:
    fail("Calendar outer horizontal padding must stay compact")

day_editor = (
    APP / "src/main/java/com/worktime/app/ui/dayeditor/DayEditorSheet.kt"
).read_text(encoding="utf-8")
if "withFrameNanos" in day_editor:
    fail("Day editor contains a frame-delayed focus transfer that can restart the IME")
if "rememberTextFieldState" not in day_editor or "InputTransformation.byValue" not in day_editor:
    fail("Day editor must use state-based text input and synchronous input transformations")
if "TextFieldValue" in day_editor or "KeyboardActions" in day_editor:
    fail("Day editor regressed to value-based text input / custom IME action plumbing")
if "onFocusChanged" in day_editor or "state.clearText()" in day_editor:
    fail("Day editor must not mutate text from focus callbacks")
if 'if (workedMinutes == 0) ""' not in day_editor:
    fail("Empty/zero worked duration must start as an empty editor value")
if 'if (micros == 0L) ""' not in day_editor:
    fail("Zero numeric adjustments/defaults must start empty instead of mutating on focus")
if "contentWindowInsets = { WindowInsets(0, 0, 0, 0) }" not in day_editor:
    fail("Day editor sheet must keep geometry independent from transient IME inset changes")

calendar_view_model = (
    APP / "src/main/java/com/worktime/app/ui/calendar/CalendarViewModel.kt"
).read_text(encoding="utf-8")
if "visibleMonth," not in calendar_view_model or "loadedMonth == requestedMonth" not in calendar_view_model:
    fail("Calendar navigation must publish the requested month before Room finishes loading rows")

build_file = (APP / "build.gradle.kts").read_text(encoding="utf-8")
catalog_file = (ROOT / "gradle/libs.versions.toml").read_text(encoding="utf-8")
if 'testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"' not in build_file:
    fail("AndroidJUnitRunner is not configured")
if 'androidx.test:runner' not in build_file and 'androidx.test:runner' not in catalog_file:
    fail("Explicit androidx.test:runner dependency is missing")

if failures:
    print("static-audit: FAILED", file=sys.stderr)
    for item in failures:
        print(f" - {item}", file=sys.stderr)
    raise SystemExit(1)

print(
    "static-audit: OK "
    f"({len(base_keys)} localized string keys; XML/privacy/domain/UI invariants passed)"
)
