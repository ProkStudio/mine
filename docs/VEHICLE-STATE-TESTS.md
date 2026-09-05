# VehicleStateTest: unit/integration boundary

## Why the bootstrap was removed

On the reported JDK 21 / Yarn 1.21.11 unit-test classpath, `Bootstrap.initialize()` failed with `IllegalAccessError` inside vanilla registry initialization. This happened before any assertion. A normal Gradle JUnit worker is not the Loom game launcher and does not provide the same transformed Minecraft classes.

`VehicleStateTest` now has no `@BeforeAll`, `Bootstrap`, `SharedConstants`, Minecraft registries, `Items`, `ItemStack`, `NbtComponent`, or other `net.minecraft` imports. It does not catch or suppress bootstrap errors, disable tests, skip assertions, or change JVM access rules.

## What is actually tested

`VehicleStateCodec` is the shared implementation used by production `VehicleState.encode/decode`. It accepts `DynamicOps<T>` and a cargo `Codec<C>`:

- Production: registry-aware NBT operations and **the real `ItemStack.OPTIONAL_CODEC`**.
- JUnit: `JsonOps` and an opaque JSON cargo fixture codec, with a JSON text serialization boundary.

The four existing test method names are retained so `tests/verify_results.py` and `tests/run-windows.ps1` still require all four to execute:

1. Full 54-slot roundtrip, every vehicle field, nested cargo payload, exact saved field names, and reference independence.
2. Empty slots, broken condition, and backward-compatible default fields.
3. Unknown version/type, malformed inventory, and oversized cargo rejection.
4. Stable IDs for all variants, per-variant roundtrip, family coverage and balance invariants.

The persisted version remains **1** and existing NBT field names are unchanged. Production code still encodes full item components using Minecraft's own codec. This is not a migration to JSON saves.

## What is NOT proven by this unit suite

These tests do **not** validate Minecraft's `ItemStack` or `NbtComponent` codecs, registry lookups, entity pickup transactions, or the full in-game NBT/component roundtrip. Those still require a correctly launched Fabric game/integration test or the manual pickup/save/reload checks in `docs/VEHICLE-QA.md`.

Older README/QA references to a JUnit "NBT/component roundtrip" should be read with this narrower boundary: the unit suite now validates the shared transport format and opaque cargo payload, not vanilla registry integration.

## Run and verify

```bash
./gradlew --no-daemon --no-build-cache --rerun-tasks --stacktrace clean build
python3 tests/verify_results.py
```

Windows:

```powershell
.\tests\run-windows.ps1 -GradleHome D:\GradleHome -TempHome D:\GradleTmp
```

Success requires an actual JUnit XML report with all four methods executed and no errors, failures or skips. Committing this change is not proof that those commands passed; review the run for the exact commit. The code change was prepared through GitHub MCP without running a terminal or game client.
