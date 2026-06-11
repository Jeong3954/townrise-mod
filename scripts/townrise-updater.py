#!/usr/bin/env python3
"""Tiny TownRise dev updater prototype.

Usage on a client PC later:
  python townrise-updater.py <manifest-url-or-file> <minecraft-instance-dir>

For now this proves the update model: compare sha256, download/copy changed jar,
and place it at mods/townrise.jar without manual jar replacement.
"""
from __future__ import annotations

import hashlib
import json
import shutil
import sys
import urllib.parse
import urllib.request
from pathlib import Path


def read_bytes(uri: str) -> bytes:
    parsed = urllib.parse.urlparse(uri)
    if parsed.scheme in ('http', 'https'):
        return urllib.request.urlopen(uri, timeout=30).read()
    return Path(uri).read_bytes()


def resolve_url(base: str, url: str) -> str:
    if urllib.parse.urlparse(url).scheme:
        return url
    parsed = urllib.parse.urlparse(base)
    if parsed.scheme in ('http', 'https'):
        return urllib.parse.urljoin(base, url)
    return str((Path(base).parent / url).resolve())


def sha256(path: Path) -> str | None:
    if not path.exists():
        return None
    h = hashlib.sha256()
    with path.open('rb') as f:
        for chunk in iter(lambda: f.read(1024 * 1024), b''):
            h.update(chunk)
    return h.hexdigest()


def main() -> int:
    if len(sys.argv) != 3:
        print('usage: townrise-updater.py <manifest-url-or-file> <minecraft-instance-dir>', file=sys.stderr)
        return 2
    manifest_uri = sys.argv[1]
    instance_dir = Path(sys.argv[2]).resolve()
    manifest = json.loads(read_bytes(manifest_uri).decode('utf-8'))
    for entry in manifest.get('files', []):
        target = instance_dir / entry['path']
        if sha256(target) == entry['sha256']:
            print(f'up-to-date {target}')
            continue
        target.parent.mkdir(parents=True, exist_ok=True)
        data = read_bytes(resolve_url(manifest_uri, entry['url']))
        actual = hashlib.sha256(data).hexdigest()
        if actual != entry['sha256']:
            raise RuntimeError(f'sha256 mismatch for {entry["url"]}: {actual} != {entry["sha256"]}')
        tmp = target.with_suffix(target.suffix + '.tmp')
        tmp.write_bytes(data)
        shutil.move(str(tmp), str(target))
        print(f'updated {target}')
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
