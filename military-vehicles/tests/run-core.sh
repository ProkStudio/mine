#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p build/core-smoke
javac --release 21 -d build/core-smoke src/main/java/com/prokstudio/militaryvehicles/core/{TruckSpec,VehicleKind,ControlLatch,TruckPhysics,TruckGeometry,VehicleGeometry,ExpansionGeometry,TrackDrive,FleetTuning,VehicleOperations,VehicleSave}.java src/test/java/com/prokstudio/militaryvehicles/CoreSmoke.java
java -cp build/core-smoke com.prokstudio.militaryvehicles.CoreSmoke
javac --release 21 -cp build/core-smoke -d build/core-smoke src/main/java/com/prokstudio/militaryvehicles/core/{EngineFeedback,TruckFeedback}.java src/test/java/com/prokstudio/militaryvehicles/FeedbackSmoke.java
java -cp build/core-smoke com.prokstudio.militaryvehicles.FeedbackSmoke
javac --release 21 -cp build/core-smoke -d build/core-smoke src/test/java/com/prokstudio/militaryvehicles/FleetSmoke.java
java -cp build/core-smoke com.prokstudio.militaryvehicles.FleetSmoke
javac --release 21 -cp build/core-smoke -d build/core-smoke src/test/java/com/prokstudio/militaryvehicles/{OperationsCases,OperationsSmoke}.java
java -cp build/core-smoke com.prokstudio.militaryvehicles.OperationsSmoke
bash tests/run-tuning.sh
bash tests/run-parking.sh
bash tests/run-controls.sh
bash tests/run-service.sh
bash tests/run-models.sh
