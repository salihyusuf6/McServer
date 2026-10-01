#!/usr/bin/env python3
"""Nova yedekleme. Sunucu acikken dunyalari kilitleyip tutarli snapshot alir.

Kullanim:
    python3 tools/backup.py            # snapshot al, son 7 tanesini sakla
    python3 tools/backup.py --keep 14  # son 14 snapshot
    python3 tools/backup.py --list     # mevcut snapshotlar
"""
import argparse
import datetime
import os
import socket
import subprocess
import sys
import tarfile
import time
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DEST = ROOT / 'backups' / 'snapshots'
CONTROL = ROOT / 'run' / 'console.sock'

# Yedege giren yollar (ROOT'a gore). Jar ve log disarida birakilir.
INCLUDE = [
    'skyblockserver/world',
    'skyblockserver/world_nether',
    'skyblockserver/world_the_end',
    'skyblockserver/nova_dungeon',
    'skyblockserver/dungeon',
    'skyblockserver/islands',
    'skyblockserver/SuperiorWorld',
    'skyblockserver/MainLobby',
    'skyblockserver/plugins',
    'skyblockserver/server.properties',
    'skyblockserver/bukkit.yml',
    'skyblockserver/spigot.yml',
    'skyblockserver/config',
    'skyblockserver/ops.json',
    'skyblockserver/usercache.json',
    'a/velocity.toml',
]

EXCLUDE_SUFFIX = ('.jar', '.log', '.log.gz')
EXCLUDE_DIRS = {'logs', 'crash-reports', 'cache', 'libraries', 'versions', 'bundler', 'target'}


def console(command: str) -> bool:
    """Calisan sunucuya konsol komutu gonderir. Sunucu kapaliysa False doner."""
    if not CONTROL.exists():
        return False
    try:
        with socket.socket(socket.AF_UNIX, socket.SOCK_STREAM) as sock:
            sock.settimeout(5)
            sock.connect(str(CONTROL))
            import json
            sock.sendall(json.dumps({'target': 'skyblock', 'command': command}).encode())
            return sock.recv(64) == b'OK'
    except OSError:
        return False


def keep_entry(path: Path) -> bool:
    if path.name.endswith(EXCLUDE_SUFFIX):
        return False
    parts = set(path.parts)
    return not (parts & EXCLUDE_DIRS)


def snapshot(keep: int) -> Path:
    DEST.mkdir(parents=True, exist_ok=True)
    stamp = datetime.datetime.now().strftime('%Y-%m-%d_%H%M%S')
    target = DEST / f'nova-{stamp}.tar.gz'

    running = console('save-off')
    if running:
        console('save-all flush')
        time.sleep(5)          # disk yazimi bitsin
        print('Sunucu acik: dunya yazimi duraklatildi.')
    else:
        print('Sunucu kapali: dogrudan kopyalaniyor.')

    try:
        with tarfile.open(target, 'w:gz') as tar:
            for rel in INCLUDE:
                source = ROOT / rel
                if not source.exists():
                    continue
                if source.is_file():
                    tar.add(source, arcname=rel)
                    continue
                for path in sorted(source.rglob('*')):
                    if path.is_file() and keep_entry(path.relative_to(ROOT)):
                        tar.add(path, arcname=str(path.relative_to(ROOT)))
    finally:
        if running:
            console('save-on')
            print('Dunya yazimi yeniden acildi.')

    size = target.stat().st_size / (1024 * 1024)
    print(f'Yedek: {target}  ({size:.1f} MB)')

    snapshots = sorted(DEST.glob('nova-*.tar.gz'))
    for old in snapshots[:-keep] if keep > 0 else []:
        old.unlink()
        print(f'Eski yedek silindi: {old.name}')
    return target


def main() -> int:
    parser = argparse.ArgumentParser(description='Nova yedekleme')
    parser.add_argument('--keep', type=int, default=7, help='Saklanacak snapshot sayisi (0 = hepsi)')
    parser.add_argument('--list', action='store_true', help='Mevcut snapshotlari listele')
    args = parser.parse_args()

    if args.list:
        if not DEST.exists():
            print('Henuz yedek yok.')
            return 0
        for path in sorted(DEST.glob('nova-*.tar.gz')):
            size = path.stat().st_size / (1024 * 1024)
            print(f'{path.name}  {size:.1f} MB')
        return 0

    snapshot(args.keep)
    return 0


if __name__ == '__main__':
    sys.exit(main())
