# Changelog

## Unreleased

### Fixed

- Removed the framework-generated long-press tooltip from the monthly-report handle without changing the report sheet's measured content/anchors.
- Reworked day-editor numeric input around one persistent editable field/input session shared by duration, hourly rate, bonus and penalty. This avoids the client-side IME hide/restart/show cycle observed in physical-device ADB traces when focus moved between separate Compose text fields.
- Numeric field labels now use Material 3's always-minimized attached label position, so labels remain on the outline even when a logical field is empty and inactive.
- Empty-day duration starts blank with a `00:00` hint and leading-zero normalization prevents inputs such as `012` from becoming `01:2`.
- Month navigation updates the requested month immediately without crossfade/old-month flashes.
- Calendar-cell daily income is displayed as a whole rounded number to fit the compact cell.
- Persistence failures are surfaced as overlay Snackbars instead of changing editor/sheet geometry.

### Changed

- Calendar geometry is widened with minimal outer margins and compact inter-cell gaps; date is bold at top-right, duration is larger and centered, and daily income is bottom-left.
- Numeric validation remains outline-only by product decision; obsolete helper-text strings/docs were removed.
- Build verification uses the project's Gradle wrapper rather than a system Gradle.
- Product/UX/QA documentation was aligned with portrait-only behavior and the physical-device validation workflow.
