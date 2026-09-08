#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p build/tuning-smoke
javac --release 21 -encoding UTF-8 -d build/tuning-smoke \
  src/main/java/com/prokstudio/militaryvehicles/core/{ControlLatch,VehicleKind,TruckSpec,TruckPhysics,FleetTuning,VehicleOperations,ServiceTargeting}.java \
  src/main/java/com/prokstudio/militaryvehicles/config/FleetConfigFile.java \
  src/test/java/com/prokstudio/militaryvehicles/{FleetTuningCases,TuningSmoke}.java
java -cp build/tuning-smoke com.prokstudio.militaryvehicles.TuningSmoke
