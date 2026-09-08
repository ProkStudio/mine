#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p build/core-smoke
javac --release 21 -d build/core-smoke src/main/java/com/prokstudio/militaryvehicles/core/{TruckSpec,ControlLatch,TruckPhysics,TruckGeometry,VehicleSave}.java src/test/java/com/prokstudio/militaryvehicles/CoreSmoke.java
java -cp build/core-smoke com.prokstudio.militaryvehicles.CoreSmoke
