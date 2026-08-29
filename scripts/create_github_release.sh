#!/usr/bin/env sh
set -eu

ROOT="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
cd "$ROOT"

for tool in git gh; do
  if ! command -v "$tool" >/dev/null 2>&1; then
    echo "Required tool is missing: $tool" >&2
    exit 2
  fi
done

if [ -n "$(git status --porcelain --untracked-files=normal)" ]; then
  echo "Refusing to create a release from a dirty working tree." >&2
  exit 2
fi

normalize_sha256() {
  printf '%s' "$1" \
    | tr -d '[:space:]:' \
    | tr '[:upper:]' '[:lower:]'
}

version_code="$(sed -n 's/.*versionCode = \([0-9][0-9]*\).*/\1/p' app/build.gradle.kts | head -n 1)"
version_name="$(sed -n 's/.*versionName = "\([^"]*\)".*/\1/p' app/build.gradle.kts | head -n 1)"
commit="$(git rev-parse HEAD)"
tag="v$version_name"
out="app/build/outputs/release-candidate"
apk="$out/WorkTime-$version_name.apk"
checksums="$out/SHA256SUMS.txt"
metadata="$out/metadata.txt"
mapping="$out/WorkTime-$version_name-mapping.txt"
signer_fingerprint_file="release/production-signing-cert-sha256.txt"

if [ -z "$version_code" ] || [ -z "$version_name" ]; then
  echo "Could not read versionCode/versionName from app/build.gradle.kts." >&2
  exit 1
fi

for file in "$apk" "$checksums" "$metadata" "$mapping" "$signer_fingerprint_file"; do
  if [ ! -s "$file" ]; then
    echo "Required release input is missing: $file" >&2
    exit 1
  fi
done

if ! grep -Fqx "commit=$commit" "$metadata"; then
  echo "Release metadata does not match current commit $commit." >&2
  exit 1
fi
if ! grep -Fqx "versionCode=$version_code" "$metadata"; then
  echo "Release metadata does not match versionCode $version_code." >&2
  exit 1
fi
if ! grep -Fqx "versionName=$version_name" "$metadata"; then
  echo "Release metadata does not match versionName $version_name." >&2
  exit 1
fi

expected_signer_sha256="$(normalize_sha256 "$(cat "$signer_fingerprint_file")")"
metadata_signer_sha256="$(
  sed -n 's/^signerSha256=//p' "$metadata" \
    | head -n 1
)"
metadata_signer_sha256="$(normalize_sha256 "$metadata_signer_sha256")"
if [ "$metadata_signer_sha256" != "$expected_signer_sha256" ]; then
  echo "Release candidate signer does not match the pinned production certificate." >&2
  echo "Expected SHA-256: $expected_signer_sha256" >&2
  echo "Candidate SHA-256: $metadata_signer_sha256" >&2
  exit 1
fi

if command -v sha256sum >/dev/null 2>&1; then
  (cd "$out" && sha256sum -c SHA256SUMS.txt)
elif command -v shasum >/dev/null 2>&1; then
  expected="$(awk '{print $1; exit}' "$checksums")"
  actual="$(shasum -a 256 "$apk" | awk '{print $1}')"
  if [ "$expected" != "$actual" ]; then
    echo "Release APK SHA-256 does not match SHA256SUMS.txt." >&2
    exit 1
  fi
else
  echo "Neither sha256sum nor shasum is available." >&2
  exit 2
fi

git fetch --quiet origin main
if ! git merge-base --is-ancestor "$commit" origin/main; then
  echo "Current release commit $commit is not contained in origin/main." >&2
  exit 1
fi

if ! git rev-parse -q --verify "refs/tags/$tag" >/dev/null; then
  echo "Local release tag $tag does not exist." >&2
  echo "Create and push the tag after the candidate commit has passed CI." >&2
  exit 1
fi

if [ "$(git rev-list -n 1 "$tag")" != "$commit" ]; then
  echo "Local tag $tag does not point to current commit $commit." >&2
  exit 1
fi

remote_commit="$(
  git ls-remote --tags origin "refs/tags/$tag^{}" \
    | awk 'NR == 1 {print $1}'
)"
if [ -z "$remote_commit" ]; then
  remote_commit="$(
    git ls-remote --tags origin "refs/tags/$tag" \
      | awk 'NR == 1 {print $1}'
  )"
fi
if [ -z "$remote_commit" ]; then
  echo "Tag $tag has not been pushed to origin." >&2
  exit 1
fi
if [ "$remote_commit" != "$commit" ]; then
  echo "Remote tag $tag resolves to $remote_commit instead of release commit $commit." >&2
  exit 1
fi

if gh release view "$tag" >/dev/null 2>&1; then
  echo "GitHub Release for $tag already exists; refusing to replace it." >&2
  exit 1
fi

gh release create "$tag" \
  "$apk" \
  "$checksums" \
  "$metadata" \
  "$mapping" \
  --verify-tag \
  --draft \
  --generate-notes \
  --title "WorkTime $version_name"

printf 'Draft GitHub Release created for %s.\n' "$tag"
printf 'Publish it only after testing the exact APK downloaded from the draft release.\n'
