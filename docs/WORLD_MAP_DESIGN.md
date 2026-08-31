# World Map & Course Dimensions

Goal: replace the open-world with a course-select map. Each course is a self-contained level; completing it returns the player to a top-down map where they pick the next level.

## Core Concepts

- **Hub world** — a small overworld-like dimension used only for the map.
- **Course dimension** — one or more dedicated dimensions (or regions in one dimension) that hold the authored levels.
- **Course node** — a data-driven definition (JSON) pointing at a course structure/region.
- **Map screen** — a top-down view of the world map. The player icon walks between nodes.
- **Progression** — unlocking nodes by completing prerequisites; stored per player.

## Proposed Architecture

### 1. Dimensions

Use a single `course` dimension with a flat/void generator. Each course is a `StructureTemplate` placed at a reserved region:

- course_1 at (0, 0, 0)
- course_2 at (256, 0, 0)
- course_3 at (512, 0, 0)

Reserved spacing: 256 blocks apart (one region) to avoid crosstalk and keep loading simple.

`data/planeshift/dimension/course.json` uses a custom chunk generator or `minecraft:flat` with a floor layer. `data/planeshift/dimension_type/course.json` locks time, disables weather, and sets the ceiling to void.

### 2. Course Generation

Two paths:

- **Structure templates**: pre-build a course in creative mode, save with a Structure Block, and export to `data/planeshift/structures/course_<name>.nbt`. The server loads and places it when the player enters.
- **Procedural chunk generator**: data-driven `CourseChunkGenerator` that builds a course from a `CourseDefinition` JSON (blocks, rails, enemy spawn groups). Heavier but gives unlimited courses.

For the vertical slice, structure templates are the better bet: artists can build, and code only needs to load and place.

### 3. Course Completion

- Add a `FlagPoleBlock` at the end of each course.
- Touching the flagpole sends `CourseCompletePayload` to the server.
- Server records completion, grants rewards, and teleports the player back to the hub.
- Client opens `CourseMapScreen`.

### 4. Map Screen

A client `Screen` (not a world) with:

- Parchment/paper background texture.
- Nodes laid out as a path.
- A player icon that can be moved with A/D or arrow keys.
- Pressing jump/enter selects the highlighted course and sends `CourseSelectPayload`.
- Server loads the course region and teleports the player.

Alternatively, render the hub world with an orthographic top-down `CameraProfile` (pitch ~90, distance 20) and let the player physically walk on a flat map. The screen approach is simpler and more Mario-like.

### 5. Hub / Map Dimension

If using the physical hub approach:

- New dimension `hub` with a flat map surface.
- Player has a top-down `CameraProfile`.
- Nodes are blocks/entities on the ground; walking onto one prompts to enter.
- `PlaneConstrainedInput` extended to support top-down movement (W/A/S/D as X/Z with no camera roll).

### 6. Progression

`CourseProgress` attachment stores:

- unlocked nodes
- completed nodes
- best times / glint counts
- current node

`CourseDefinition` codec fields:

- `id`
- `display_name`
- `unlock_requirements`
- `structure` (NBT path) or `generator`
- `camera_profile`
- `role_recommended`
- `rewards`

## Suggested Implementation Order

1. `data/planeshift/dimension/course.json` and `dimension_type/course.json`.
2. `FlagPoleBlock` + `CourseCompletePayload`.
3. `CourseMapScreen` skeleton.
4. `/course load <id>` command for testing.
5. `CourseProgress` attachment.
6. `CourseCompletionService` to handle rewards, unlocks, and teleportation.
7. Structure-based course loader.
8. Hub dimension or map screen as final polish.
