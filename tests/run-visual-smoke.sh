#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
if (( $# > 1 )) || [[ "${1:-}" != "" && "${1:-}" != "--previews" ]]; then
  echo "Usage: bash tests/run-visual-smoke.sh [--previews]" >&2
  exit 2
fi
work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT
src=src/main/java/com/harvester/vehicle
java -m jdk.compiler/com.sun.tools.javac.Main --release 21 -d "$work/classes" \
  "$src/VehicleType.java" "$src/VehicleGeometry.java" "$src/VehicleDetailing.java" \
  "$src/VehicleAssembly.java" "$src/VehicleAtlas.java" "$src/VehiclePhysics.java" "$src/PassengerPose.java" \
  "$src/PassengerAnimation.java" "$src/VehiclePresentation.java" \
  tools/src/VehicleAssetGenerator.java tools/src/VehicleVisualSmoke.java
java -Djava.awt.headless=true -cp "$work/classes" VehicleVisualSmoke "$work/resources"
if [[ "${1:-}" == "--previews" ]]; then
  # Generated outputs only; these never enter the production resource source set.
  rm -rf build/preview-assets build/preview-meshes build/previews
  java -Djava.awt.headless=true -cp "$work/classes" VehicleAssetGenerator build/preview-assets build/preview-meshes
  python3 tools/render_vehicle_preview.py
  python3 tools/render_vehicle_preview.py plane --rear
fi
