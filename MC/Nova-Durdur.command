#!/bin/sh
set -eu
cd "$(dirname "$0")"
python3 tools/serverctl.py stop
