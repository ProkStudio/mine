# Custom vehicle art and refueling

## Refueling

1. Place the vehicle in the Overworld. A fresh vehicle has an empty tank.
2. Open the creative tab **Harvester — техника и заправка** and take an S/M/L fuel can, or use `/give @s harvester:fuel_can_large`.
3. Hold the can and right-click the placed vehicle **without Shift**.
4. Watch the fuel value in the actionbar. Partial refills keep unused fuel in the can. Shift + right-click picks up the vehicle instead of refueling it.

Vehicle items, can tooltips, right-clicking a can in the air and placing an empty vehicle now explain this interaction. Coal is not fuel in the transport pack.

## Original geometry and assets

`VehicleGeometry.java` defines the mod's own meshes: chassis, cabins, window frames, mirrors, seats, wheels with sliced round profiles and hubs, tracked undercarriages, cutter/reel, hulls, wings, engine housings and rotors. Each variant has a generated handheld mesh and a 2D orthographic icon from that same geometry.

The entity renderer now submits custom `ModelPart` meshes with **harvester-owned texture PNGs**. It no longer submits vanilla block models. No vanilla item sprite or block texture is used by the active vehicle/service-item resource graph. Vanilla's `minecraft:item/generated` parent is only the standard 2D item rendering mechanism, not borrowed artwork.

PNG textures and JSON models are reproducibly generated during Gradle `processResources`, using Java 21 / Java2D without new libraries or network image downloads:

- Source: `tools/src/VehicleAssetGenerator.java` and `VehicleGeometry.java`.
- Generated resources: `build/generated/vehicle-assets/assets/harvester/`.
- Packaged resources: `build/resources/main/assets/harvester/` and the final jar.
- Each registered item has its own `items/<id>.json` definition, including all can sizes.
- Old family-level item model IDs are kept as aliases for existing stacks.
- The old source item/model trees are replaced in processed resources, so stale vanilla-sprite definitions cannot override the generated assets in the jar.

A build check validates registered IDs, JSON model references, parent cycles and every referenced PNG; it rejects borrowed textures. Duplicate generated paths fail immediately. This checks packaging, not visual quality.

## Updating and diagnosis

Use a **new complete jar from a successful build**, not copied source JSON files alone. Keep only one Harvester jar in the mods folder. Test first with resource packs disabled; resource packs can override any mod's assets. The new generator has not been executed in the MCP-only session, nor has the client renderer been visually inspected. Build success and absence of pink cubes are not claimed until checked in a running client.

If missing models remain after updating, record the exact item ID, whether the issue is in GUI/hand/world, and the corresponding model/texture error from `latest.log`. Do not destroy a stateful vehicle item to diagnose it; compare it with a fresh `/give` item and retain the original cargo.

Existing README graphics paragraphs describing temporary vanilla icons are superseded by this document. The engine sound remains the previously documented vanilla sound-event placeholder; this change replaces graphics, not audio.
