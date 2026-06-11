#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"
./gradlew --no-daemon clean build
JAR=$(ls -1 build/libs/townrise-*.jar | grep -v 'sources' | head -n1)
SHA=$(sha256sum "$JAR" | awk '{print $1}')
SIZE=$(stat -c '%s' "$JAR")
VERSION=$(awk -F= '/^mod_version=/{print $2}' gradle.properties)
TAG="v${VERSION}"
OUT_DIR="$ROOT/updates/files"
mkdir -p "$OUT_DIR"
OUT_NAME="townrise-${VERSION}.jar"
cp "$JAR" "$OUT_DIR/$OUT_NAME"
python3 - "$ROOT/updates/manifest.json" "$OUT_NAME" "$SHA" "$SIZE" "$VERSION" "$TAG" <<'PY'
import json, sys
manifest_path, file_name, sha, size, version, tag = sys.argv[1:]
try:
    manifest = json.load(open(manifest_path, encoding='utf-8'))
except FileNotFoundError:
    manifest = {
        'schemaVersion': 1,
        'modpack': 'townrise-dev',
        'minecraft': '1.21.1',
        'loader': {'type': 'neoforge', 'version': '21.1.233'},
    }
manifest['version'] = version
manifest['files'] = [{
    'path': 'mods/townrise.jar',
    'name': file_name,
    'sha256': sha,
    'size': int(size),
    # Stable JVM option should point at updates/manifest.json on main.
    # The manifest itself points at the versioned GitHub Release asset.
    'url': f'https://github.com/Jeong3954/townrise-mod/releases/download/{tag}/{file_name}'
}]
json.dump(manifest, open(manifest_path, 'w', encoding='utf-8'), ensure_ascii=False, indent=2)
open(manifest_path, 'a', encoding='utf-8').write('\n')
PY
printf 'Published %s\nsha256=%s\nmanifest=%s\n' "$OUT_DIR/$OUT_NAME" "$SHA" "$ROOT/updates/manifest.json"
