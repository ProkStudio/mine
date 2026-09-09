#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p build/service-stillness
javac --release 21 -d build/service-stillness src/main/java/com/prokstudio/militaryvehicles/core/VehicleParking.java src/test/java/com/prokstudio/militaryvehicles/{ServiceStillnessCases,ServiceStillnessSmoke}.java
java -cp build/service-stillness com.prokstudio.militaryvehicles.ServiceStillnessSmoke
