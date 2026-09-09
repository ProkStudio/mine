#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p build/fleet-models
javac --release 21 -d build/fleet-models \
  src/main/java/com/prokstudio/militaryvehicles/core/TruckGeometry.java \
  src/main/java/com/prokstudio/militaryvehicles/core/VehicleKind.java \
  src/main/java/com/prokstudio/militaryvehicles/core/ExpansionGeometry.java \
  src/test/java/com/prokstudio/militaryvehicles/FleetModelCases.java \
  src/test/java/com/prokstudio/militaryvehicles/FleetModelSmoke.java \
  src/test/java/com/prokstudio/militaryvehicles/ServiceModelCases.java \
  src/test/java/com/prokstudio/militaryvehicles/ServiceModelSmoke.java
java -cp build/fleet-models com.prokstudio.militaryvehicles.FleetModelSmoke
java -cp build/fleet-models com.prokstudio.militaryvehicles.ServiceModelSmoke
