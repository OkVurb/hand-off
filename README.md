# PlaneShift

A NeoForge 1.21.11 mod that turns selected areas of a Minecraft world into tight, side-on 2.5D courses with perspective-shifting gates, Forms, and a readable enemy roster.

## Build

```bash
./gradlew build
```

On Windows:

```powershell
.\gradlew build
```

The build includes `checkClientClassLeak`, so all client classes must live under `com.studio.planeshift.client`.

## Controls

- **W / A / S / D** — move (2.5D maps A/D to forward/back along the rail while keeping screen-relative left/right readable).
- **Space** — jump (hold for Glider float).
- **F** — Use active Form.
- **G** — Swap active/reserve Form.

## Features

- 2.5D perspective-rail camera and 3D orbit camera, blended during transitions.
- Coyote time, jump buffering, and variable jump height.
- Four Forms: Ember Core, Gale Mantle, Barrier Block, Magnet Lantern.
- Four enemy archetypes: Pebblekin, Springmite, Bastion Beetle, plus the Ember Bolt projectile.
- Course blocks: Shift Gate, Checkpoint Beacon, Spring Pad, Prize Cache.
- Data-driven Forms, Roles, and Camera Profiles.

## Architecture

- `common/` — registries, state model, course logic, Form framework, block/entity/item definitions.
- `server/` — `ModeTransitionService`, `CheckpointService`, `FormService`, `CourseStateAccess`.
- `client/` — `ClientEvents`, `PlaneShiftKeybinds`, `CourseHud`, `CameraDirector`, `PlaneConstrainedInput`, renderers.
- `data/planeshift/planeshift/` — datapack registries for `form`, `role`, and `camera_profile`.

## Status

Vertical slice / foundation gate. Placeholder textures and block models are in place for greybox readability.
