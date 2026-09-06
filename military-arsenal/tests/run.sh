#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")/.."
mkdir -p build/standalone
find src/main/java/dev/mine/arsenal/core -name '*.java' > build/standalone/sources.txt
printf '%s\n' tools/ArsenalAssets.java tools/ArsenalAudio.java tests/ArsenalCoreChecks.java >> build/standalone/sources.txt
if command -v javac >/dev/null 2>&1; then
    javac -encoding UTF-8 --release 21 -d build/standalone @build/standalone/sources.txt
else
    java com.sun.tools.javac.Main -encoding UTF-8 --release 21 -d build/standalone @build/standalone/sources.txt
fi
java -cp build/standalone ArsenalCoreChecks
java -Djava.awt.headless=true -cp build/standalone ArsenalAssets build/standalone/resources
python3 tests/validate_assets.py build/standalone/resources
