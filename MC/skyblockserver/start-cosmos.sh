#!/bin/sh
set -eu
cd "$(CDPATH='' cd -- "$(dirname -- "$0")" && pwd)"
COSMOS_JAVA="${COSMOS_JAVA_HOME:-/opt/homebrew/opt/openjdk@17}/bin/java"
if [ ! -x "$COSMOS_JAVA" ]; then
  echo 'Java 17 bulunamadı. COSMOS_JAVA_HOME değişkenini Java 17 kurulum dizinine ayarla.' >&2
  exit 1
fi
exec "$COSMOS_JAVA" -Xms1G -Xmx3G -jar server.jar --nogui
