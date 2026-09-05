#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
classes="$(mktemp -d)"
trap 'rm -rf "$classes"' EXIT
java -m jdk.compiler/com.sun.tools.javac.Main --release 21 -d "$classes" \
  src/main/java/com/harvester/entity/HarvesterLogic.java tests/HarvesterLogicTest.java
java -cp "$classes" HarvesterLogicTest
