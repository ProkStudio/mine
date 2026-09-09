#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p build/engine-intent
javac --release 21 -d build/engine-intent src/main/java/com/prokstudio/militaryvehicles/core/ControlLatch.java src/test/java/com/prokstudio/militaryvehicles/{EngineIntentCases,EngineIntentSmoke}.java
java -cp build/engine-intent com.prokstudio.militaryvehicles.EngineIntentSmoke
