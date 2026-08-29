#!/usr/bin/env sh
set -eu

ROOT="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
cd "$ROOT"

: "${RELEASE_STORE_FILE:?Set RELEASE_STORE_FILE to the upload keystore path}"
: "${RELEASE_STORE_PASSWORD:?Set RELEASE_STORE_PASSWORD}"
: "${RELEASE_KEY_ALIAS:?Set RELEASE_KEY_ALIAS}"
: "${RELEASE_KEY_PASSWORD:?Set RELEASE_KEY_PASSWORD}"

if [ ! -f "$RELEASE_STORE_FILE" ]; then
  echo "Release keystore not found: $RELEASE_STORE_FILE" >&2
  exit 2
fi

for tool in git python3 jarsigner keytool; do
  if ! command -v "$tool" >/dev/null 2>&1; then
    echo "Required tool is missing: $tool" >&2
    exit 2
  fi
done

if [ ! -x ./gradlew ]; then
  echo "Gradle Wrapper is missing or not executable." >&2
  exit 2
fi

if [ -n "$(git status --porcelain --untracked-files=normal)" ]; then
  echo "Refusing to build a release candidate from a dirty working tree." >&2
  exit 2
fi

python3 scripts/static_audit.py

./gradlew --no-daemon \
  :app:lintRelease \
  :app:bundleRelease \
  --stacktrace

AAB="app/build/outputs/bundle/release/app-release.aab"
MAPPING="app/build/outputs/mapping/release/mapping.txt"
METADATA_DIR="app/build/outputs/release-candidate"
METADATA="$METADATA_DIR/metadata.txt"

if [ ! -s "$AAB" ]; then
  echo "Signed release AAB was not produced: $AAB" >&2
  exit 1
fi
if [ ! -s "$MAPPING" ]; then
  echo "R8 mapping was not produced: $MAPPING" >&2
  exit 1
fi

verification="$(LC_ALL=C jarsigner -verify "$AAB" 2>&1)"
if ! printf '%s\n' "$verification" | grep -Fq "jar verified."; then
  printf '%s\n' "$verification" >&2
  echo "Release AAB signature verification failed." >&2
  exit 1
fi

if command -v sha256sum >/dev/null 2>&1; then
  aab_sha256="$(sha256sum "$AAB" | awk '{print $1}')"
elif command -v shasum >/dev/null 2>&1; then
  aab_sha256="$(shasum -a 256 "$AAB" | awk '{print $1}')"
else
  echo "Neither sha256sum nor shasum is available." >&2
  exit 2
fi

signer_sha256="$(
  LC_ALL=C keytool -printcert -jarfile "$AAB" 2>/dev/null \
    | awk -F'SHA256: ' '/SHA256:/{print $2; exit}'
)"
if [ -z "$signer_sha256" ]; then
  echo "Could not read the signer SHA-256 fingerprint from the release AAB." >&2
  exit 1
fi

commit="$(git rev-parse HEAD)"
version_code="$(sed -n 's/.*versionCode = \([0-9][0-9]*\).*/\1/p' app/build.gradle.kts | head -n 1)"
version_name="$(sed -n 's/.*versionName = "\([^"]*\)".*/\1/p' app/build.gradle.kts | head -n 1)"

mkdir -p "$METADATA_DIR"
cat > "$METADATA" <<EOF
commit=$commit
versionCode=$version_code
versionName=$version_name
aab=$AAB
aabSha256=$aab_sha256
signerSha256=$signer_sha256
mapping=$MAPPING
EOF

printf 'Release candidate built and verified.\n'
printf 'Commit: %s\n' "$commit"
printf 'Version: %s (%s)\n' "$version_name" "$version_code"
printf 'AAB: %s\n' "$AAB"
printf 'AAB SHA-256: %s\n' "$aab_sha256"
printf 'Signer SHA-256: %s\n' "$signer_sha256"
printf 'R8 mapping: %s\n' "$MAPPING"
printf 'Metadata: %s\n' "$METADATA"
