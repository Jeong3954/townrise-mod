#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"
./gradlew --no-daemon clean build
JAR=$(ls -1 build/libs/townrise-*.jar | grep -v 'sources' | head -n1)
SHA=$(sha256sum "$JAR" | awk '{print $1}')
SIZE=$(stat -c '%s' "$JAR")
VERSION=$(awk -F= '/^mod_version=/{print $2}' gradle.properties)
OUT_DIR="$ROOT/updates/files"
mkdir -p "$OUT_DIR"
OUT_NAME="townrise-${VERSION}.jar"
cp "$JAR" "$OUT_DIR/$OUT_NAME"
python3 - "$ROOT/updates/manifest.json" "$OUT_NAME" "$SHA" "$SIZE" "$VERSION" <<'PY'
import json, sys
manifest_path, file_name, sha, size, version = sys.argv[1:]
manifest = json.load(open(manifest_path, encoding='utf-8'))
manifest['version'] = version
manifest['files'] = [{
    'path': 'mods/townrise.jar',
    'name': file_name,
    'sha256': sha,
    'size': int(size),
    'url': f'files/{file_name}'
}]
json.dump(manifest, open(manifest_path, 'w', encoding='utf-8'), ensure_ascii=False, indent=2)
open(manifest_path, 'a', encoding='utf-8').write('\n')
PY
printf 'Published %s\nsha256=%s\nmanifest=%s\n' "$OUT_DIR/$OUT_NAME" "$SHA" "$ROOT/updates/manifest.json"
