# PlaneShift — Agent Notes

## Project
- Minecraft mod built from the NeoForge 1.21.11 MDK (NeoForge 21.11.45, MDG 2.0.144, Java 21).
- Root: `C:\Dev\PlaneShift`.
- MCP decompiled sources are cached in `.mcsources` for quick lookup of vanilla/NeoForge APIs.

## Build & Verify
- Full build: ` .\gradlew build` from `C:\Dev\PlaneShift`.
- Fast compile check: ` .\gradlew compileJava`.
- The build includes `checkClientClassLeak`; client code must stay in `com.studio.planeshift.client`.
- No tests or CI workflow exist yet.

## Key Architecture
- Common layer: `common/` — registries, mode/rail/course state, Form framework, block/entity/item definitions.
- Server layer: `server/` — `ModeTransitionService`, `CheckpointService`, `FormService`, `CourseStateAccess`.
- Client layer: `client/` — `ClientEvents`, `PlaneShiftKeybinds`, `CourseHud`, `CameraDirector`, `PlaneConstrainedInput`, renderers.
- Networking: `common/network/` — `FormActionPayload`, `ReserveSwapPayload`, etc.

## Data Pack Registries
Loaded from `data/planeshift/planeshift/<registry>/`:
- `form/` — `FormDefinition` records.
- `role/` — `PlayerRole` records.
- `camera_profile/` — `CameraProfile` records.

## Resources
- Language: `assets/planeshift/lang/en_us.json`.
- Block/item model and blockstate JSONs are in `assets/planeshift/models/` and `assets/planeshift/blockstates/`.
- Placeholder PNG textures are intentionally not generated; real textures belong in `assets/planeshift/textures/`.

## Notes
- Git for Windows is not installed in this environment, so commits cannot be made from here.
- The `plumberplatformer` project at `C:\Users\cr0od\OneDrive\Documents\Minecraft Mod` is a separate mod and should not be mixed with PlaneShift.
