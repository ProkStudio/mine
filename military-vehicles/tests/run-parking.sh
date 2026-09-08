#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p build/parking-smoke
javac --release 21 -d build/parking-smoke src/main/java/com/prokstudio/militaryvehicles/core/VehicleParking.java src/test/java/com/prokstudio/militaryvehicles/{ParkingCases,ParkingSmoke}.java
java -cp build/parking-smoke com.prokstudio.militaryvehicles.ParkingSmoke
