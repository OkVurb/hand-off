# PlaneShift — Continuation Handoff

**Project root:** `C:\Dev\PlaneShift`  
**Date:** 2026-08-31  
**Status:** `BUILD SUCCESSFUL` — all checks pass. Verified at runtime: `runServer` reaches `Done` with zero errors, `runClient` reaches the title screen with zero errors.

This document is a complete handoff for the PlaneShift NeoForge 1.21.11 mod. It contains the current state, what was just changed, how to build it, the full source/resource inventory, and the remaining known issues. Use it to continue work from another session or tool.

---

## 1. Project Overview

PlaneShift is a Mario/PlaneShift-inspired Minecraft mod built with NeoForge 21.11.45, MDG 2.0.144, Java 21. It is structured in the standard `common` / `client` / `server` layers.

- `common` — registries, block/item/entity classes, mode/rail/course state, Form framework, networking payloads, data-pack registries.
- `server` — authoritative services: `ModeTransitionService`, `CheckpointService`, `CourseService`, `FormService`, `DamageService`, etc.
- `client` — keybinds, HUD `CourseHud`, camera `CameraDirector`, input `PlaneConstrainedInput`, music, renderers, screens.

Client code is isolated under `com.studio.planeshift.client`. The `checkClientClassLeak` task enforces that `common` and `server` never load client classes.

---

## 2. Build & Verify

Run from `C:\Dev\PlaneShift`:

- Full build: `.\gradlew build`
- Fast compile: `.\gradlew compileJava`
- Unit tests: `.\gradlew test`
- Client class leak check: `.\gradlew checkClientClassLeak`
- Raw cuboid scan check: `.\gradlew checkNoRawCuboidScan`
- Sound asset check: `.\gradlew checkSoundAssets`
- Texture asset check: `.\gradlew checkTextureAssets`
- Block model check: `.\gradlew checkBlockModels`
- Runtime smoke test: `.gradlew runServer` then `.gradlew runClient` (see section 3.-5)

All `check*` tasks and `test` are wired into `check`, so `.\gradlew build` runs them.

Current results:

```
> Task :compileJava — this-escape warnings only, no errors.
> Task :checkClientClassLeak — PASSED
> Task :checkNoRawCuboidScan — PASSED
> Task :checkSoundAssets — PASSED (22 events, all backed by an OGG)
> Task :checkTextureAssets — PASSED (96 textures, present and distinct)
> Task :checkBlockModels — PASSED
> Task :test — PASSED (31 cases)
> Task :build — BUILD SUCCESSFUL
```

The remaining warnings are `this-escape` in constructors and are considered cosmetic for this vertical slice.

---

## 3. What Was Just Done

### 3.-5 Session 2026-08-31 — runtime verification (runClient / runServer)

Launched the game. The build was green throughout, and the game found four bugs it could not
see. This is the section to read before trusting a green build again.

**World creation crashed (blocker).** `data/planeshift/dimension_type/course.json` used the
pre-1.21.11 schema. `RegistryDataLoader` threw `No key monster_spawn_block_light_limit` /
`No key monster_spawn_light_level`: those live at the *top level* now, not nested under
`monster_settings`. Clicking into a world raised `ReportedException: Registry Loading`. Reading
the `DimensionType` codec showed the schema had also dropped `natural`, `piglin_safe`,
`bed_works`, `has_raids`, `ultrawarm`, `respawn_anchor_works`, `has_respawn_anchor`, `effects`
and `fixed_time` in favour of `attributes`, `skybox`, `cardinal_light` and `timelines`, so the
file carried nine dead keys as well. Rewritten against the codec; `fixed_time: 6000` became
`has_fixed_time: true` plus `timelines: "#minecraft:in_overworld"`.

**All 54 items rendered as missing models.** Since 1.21.4 an item needs
`assets/<ns>/items/<id>.json` naming its model; a file under `models/item/` is not enough.
That directory did not exist, so every item logged "No model loaded for default item model ID"
and rendered as the missing-model block. Generated all 54.

**`pack.mcmeta` was rejected — differently on each side.** The old
`pack_format` + `supported_formats` spelling is refused above format 64. The fix is not obvious:
`PackFormat.validateNewFormat` requires `min_format`'s major to exceed `lastPreMinorVersion`,
which is **64 for CLIENT_RESOURCES but 81 for SERVER_DATA** — and one file is read as both. A
first attempt at `[75, 0]` (the resource format) satisfied the client and then failed the server
with "game versions supporting formats 17 to 81 require a supported_formats field". Now pinned
to `[94, 1]`, matching what NeoForge's own jar declares.

**`moving_platform_spawn_egg` referenced a deleted vanilla model.** It parented to
`minecraft:item/template_spawn_egg`, which no longer exists; the other twelve spawn eggs use
`item/generated` with their own texture. Made it match.

**Verification.** `runServer` reaches `Done (0.540s)! For help, type "help"` with zero ERROR
lines and zero registry errors — that is the path that previously threw. `runClient` reaches the
title screen with zero errors and zero warnings beyond vanilla's own.

`checkBlockModels` was extended to catch two of these classes of bug: an item model with no
`items/` definition, and a `pack.mcmeta` that omits `min_format`/`max_format` or whose
`min_format` major is <= 81. Both verified by reintroducing them.

**What the build still cannot catch:** data-pack *schema* errors like the dimension type. The
JSON parses fine; only the codec rejects it. `.\gradlew runServer` is the cheapest real check —
it loads every data-pack registry headlessly in about a minute. Run it after touching anything
under `src/main/resources/data/`.

### 3.-4 Session 2026-08-31 — remaining P1/P2 sweep

Cleared every outstanding item from section 4 except play-testing.

**GUI overflow (P1).** `ToadShopScreen` started at `height / 4` and stepped 24px per offer, so
ten offers plus the close button needed 270px below the quarter mark. At GUI scale 4 on a 1080p
window there are about 200, and the last offers and the close button fell off the bottom with no
way to reach them. The layout now derives column count, row spacing and button width from the
space actually available, pins the header to the top instead of `height / 4`, and clamps the
close button so it can never leave the window.

`CourseHud` had a fixed 200x76 panel. Two real bugs: the star-coin line sits at `y+64` and ran
past the 76px panel, and the debug overlay drew at a fixed `y=48`, directly on top of the lives,
coins and star-coin readouts. The panel is now sized against `guiWidth`/`guiHeight`, the debug
block starts below it, long form names are trimmed to the panel, and pips stop at the panel edge.

**Head contact (P2).** `BrickBlock`, `QuestionBlock`, `HiddenQuestionBlock` and `PrizeCacheBlock`
tested `player.getY() < pos.getY()` — whether the player was *somewhere* below. Left-click reaches
about 4.5 blocks, so standing well underneath, or off to the side looking at the block edge,
triggered a block that was never touched. `HitFromBelowBlock.isHeadContact` now uses the player
bounding box: the top of it must sit at the block underside, within one block, and overlap the
block column. Sized to cover both callers — `AirMoveService` fires on the block a rising player is
*about* to enter, a left-click fires while standing still, and a crouched head sits lower again.

**PSwitch map leak (P2).** The revert hung off `playerWillDestroy`, which only covers a player
breaking the block by hand. An explosion, piston or `/setblock` left the entry in `CONVERTED` with
no scheduled tick to consume it, so the map leaked and the coins stayed coins. Moved to
`affectNeighborsAfterRemoval`, the hook vanilla buttons and pressure plates use, which runs on
every removal path. It fires only on block-type change (`LevelChunk` gates it on
`!oldState.is(newBlock)`), so pressing and releasing the switch does not trip it.

**Rate limiting (P2).** New `PayloadRateLimiter`. `CourseService.loadCourse` resolves a definition
and teleports across dimensions; `ToadShopService.purchase` moves currency. Neither should be
reachable at packet rate. Cooldowns are counted in server ticks from
`MinecraftServer.getTickCount()` — monotonic, shared across dimensions so the cooldown survives
the teleport `loadCourse` performs, and it stops advancing when the server does. Cleared on logout
via the existing `clearPlayerCaches`.

**State-aware models (P2).** `on_off_block` rendered the same in both states despite having no
collision when off; its off model is now an inset cube so it reads as passable. `question_block`
with `used=true` pointed at the *brick_block* model, making spent blocks look like breakable
bricks. A new `checkBlockModels` task then found three more of the same bug that were not in the
issue list: `checkpoint_beacon` (`lit`), `coin_ring_block` (`used`) and `on_off_switch`
(`powered`) all rendered identically in both states — an activated checkpoint looked exactly like
an inactive one. All fixed with their own models and textures.

`hidden_question_block` needed no change: its model is already `"elements": []`, which is the
correct invisible-until-hit look.

**New build check:** `checkBlockModels` — fails on malformed model/blockstate JSON, a blockstate
naming a missing model, a model naming a missing texture or parent, or a multi-variant blockstate
whose variants all render identically. Rotation is part of the identity, so facing variants that
reuse one model at different angles (`conveyor_belt`, `shift_gate`) are correctly not flagged.
Verified non-vacuous with a typo'd texture path and malformed JSON.

### 3.-3 Session 2026-08-31 — texture assets (P1 + P2, resolved)

**New:** `tools/TextureGen.java`, generating all 89 placeholder textures (was 73).

Two separate problems, both live:

- **Missing files.** All six projectile entities (`boomerang`, `bowser_fire`, `ember_bolt`,
  `fireball`, `hammer`, `iceball`) had no texture, and `textures/mob_effect/` did not exist at
  all — 10 effect icons absent. Both render as the missing-texture checkerboard, which fails
  nothing and is easy to miss.
- **Indistinguishable files.** 24 of the 73 existing placeholders were byte-identical to another.
  `hammer_bro`/`koopa` were the same colour, as were `brick_block`/`secret_passage` — a secret
  passage that looks exactly like a brick is a gameplay bug, not an art gap.

The generator gives each entry a hue spread by the golden angle over a shared index (cannot
collide) plus its initials in a 4×5 pixel font. Initials alone were not enough —
`checkpoint_beacon`, `coin_block` and `conveyor_belt` all reduce to "CB" — so colliding names
advance through progressively more specific strategies until each category is unambiguous.

Sizes match what the game expects: 16×16 blocks/items/particles, 64×32 entity skins, 18×18 mob
effect icons (checked against vanilla in the client resources jar). Effect icons use the colour
the effect declares in `ModEffects`, so the HUD icon matches the aura tint.

**New build check:** `checkTextureAssets` — fails if a block/entity/effect/particle registered in
`Mod*.java` has no texture, if any PNG is empty, or if two PNGs are byte-identical. Items are
deliberately not coverage-checked (`registerCharm` takes a second Form-id argument, so
registration names cannot be extracted unambiguously by regex); the duplicate check still covers
them. Verified non-vacuous: deleting `mob_effect/frozen.png` and duplicating `brick_block` onto
`secret_passage` produced exactly those two failures.

Note for later: `ice_aura` and `mini_aura` share `0x88CCFF` in `ModEffects`, so their icons are
the same colour by design of the registry, not by generator error. The labels distinguish them.

### 3.-2 Session 2026-08-31 — sound assets (P0, resolved)

All 22 registered sound events now ship with real audio. Previously `sounds.json` held empty
arrays and `assets/planeshift/sounds/` did not exist, so every cue was a silent no-op that
failed nothing.

**New:** `tools/SoundGen.java` — a standalone generator (not compiled into the mod; the build
only sources `src/main/java`). It synthesises all 22 sounds from pulse / NES-stepped triangle /
LFSR noise / saw oscillators with envelopes, pitch sweeps and vibrato, plus a small step
sequencer for the four music tracks.

- **Everything is original.** Nothing is sampled from or transcribed from an existing recording
  or composition. The melodies and effect gestures were written for this project; the NES
  four-voice palette is a synthesis technique, not borrowed content. See `ASSET_LICENSES.md`.
- **Reproducible:** re-running the generator reproduces the same audio. `tools/README.md` has
  the regeneration and encoding commands.
- **SFX are mono, music is stereo.** Minecraft only attenuates and positions mono sources, so a
  stereo SFX would play at full volume everywhere in the world. The four music tracks go through
  `SimpleSoundInstance.forMusic`, which is non-positional, so stereo is free there.
- Music entries carry `"stream": true` (they run 23-37 s each).
- 18 SFX + 4 music tracks, ~1.8 MB of OGG Vorbis total, all verified present in the built jar.

Also added `subtitles.planeshift.*` keys in `en_us.json` and wired them into `sounds.json`, so
the cues caption correctly for players using subtitles.

**New build check:** `checkSoundAssets`. Parses the `register(...)` calls out of `ModSounds`
and fails if any registered event is missing from `sounds.json`, has an empty `sounds` array,
names an OGG that does not exist or is zero-length, or declares a subtitle key absent from
`en_us.json`. It also flags `sounds.json` entries that no longer match a registered event.
Wired into `check`. Verified non-vacuous: deleting one OGG, emptying one `sounds` array and
corrupting one subtitle key produced exactly those three failures, and restoring them passed.

Requires `ffmpeg` with `libvorbis` to regenerate (installed via `winget install Gyan.FFmpeg`).

### 3.-1 Session 2026-08-31 — client movement refactor (P1, resolved)

`PlaneConstrainedInput` no longer mutates `LocalPlayer` at all. It is now purely an input
projection, which is what its name always claimed.

**The key change** is in `PlaneConstrainedInput.railProjection(playerYaw, travelYaw)`.
`moveVector` is interpreted *relative to the player's own yaw* — `Entity#getInputVector` rotates
it by `getYRot()` before adding it to velocity — so the old approach of forcing
`setYRot(travelYaw)` and pushing a constant `(0, 1)` was really just a way of making that
rotation the identity. Cancelling the yaw arithmetically instead gives identical world-space
motion with no writes to the player:

```
delta      = playerYaw - travelYaw
moveVector = (sin delta, cos delta)
```

Substituting into the engine's rotation collapses to `(-sin travelYaw, cos travelYaw)` — the
unit vector along the rail, independent of where the player looks.

**Side benefit:** the look direction is now free. `ClientEvents` sends `player.getLookAngle()`
in `FormActionPayload`, so previously you could only ever aim a Form action along the rail.

**New:** `client/input/PlaneMovementAssists.java` holds coyote time, jump buffering and the
Glider float — the parts that genuinely must change velocity. They now run from
`MovementInputUpdateEvent`, which NeoForge fires inside `LocalPlayer#aiStep` right after
`input.tick()` and before the tick's movement is applied. That is the correct point for them,
and it keeps velocity changes out of an `Input` implementation.

- Fixed a real bug moved across in the process: the Glider float read a *stale* velocity that
  the coyote branch may have just replaced, so a coyote jump by a Glider was immediately
  cancelled back to `-0.06` on the same tick. The float now re-reads, and skips entirely on a
  tick where coyote fired.
- Assist state is static now, so `ClientEvents.installInput` calls `PlaneMovementAssists.reset()`
  to stop a half-spent coyote or float budget carrying across a respawn.
- Avatar facing moved to `tickBodyFacing`, called from `ClientTickEvent.Post` (after entities
  tick, so `LivingEntity#tickHeadTurn` cannot drag it back within the same tick). It writes
  `yBodyRot`/`yBodyRotO` — presentation only — not `yRot`.

**New: the project now has unit tests.** `build.gradle` enables MDG's `neoForge.unitTest` with
JUnit 5, and `src/test/java/.../PlaneConstrainedInputTest.java` covers the projection geometry.
The tests re-implement `Entity#getInputVector` exactly as the engine applies it and assert on
the resulting **world-space** direction, so they verify the projection survives the engine's own
rotation rather than just checking the raw numbers. 31 cases, all passing. Verified non-vacuous:
flipping the sign on the sine term failed 12 of them.

Run with `.\gradlew test`; `build` runs them.

### 3.0 Session 2026-08-31 — block scan optimization (P1)

Fixed the "brute-force block scans" P1 issue.

**New:** `src/main/java/com/studio/planeshift/common/block/BlockAreaScan.java`

`BlockAreaScan.findMatching(level, center, radiusXZ, radiusY, predicate)` replaces
`BlockPos.betweenClosed(...)` + a `Level.getBlockState` per position. It walks loaded chunk
sections and uses `LevelChunkSection.maybeHas(Predicate<BlockState>)` to reject a whole
4096-block section from its palette in one call, so the per-block loop only runs in sections
that could actually contain a match. `maybeHas` is conservative — a global-palette section
always answers yes — so it can cost a wasted section scan but never misses a block.

- Chunks are fetched with `level.getChunk(cx, cz, ChunkStatus.FULL, false)`, so unloaded chunks
  are skipped and terrain is never generated to satisfy a search. This replaces the old
  per-position `level.isLoaded(target)` guard.
- Y is clamped to `level.getMinY()` / `level.getMaxY()`.
- Matches are **collected and returned**, not passed to a callback. Both callers mutate the
  blocks they find, and writing into a section while iterating its palette is exactly the
  aliasing this avoids. Returned positions are already immutable.

**Changed:**
- `OnOffSwitchBlock.toggle` — was 49x17x49 = ~40.8k `getBlockState` calls per toggle. The
  predicate now also filters on `OnOffBlock.ON != next`, so sections holding only
  already-correct ON/OFF blocks are rejected too.
- `PSwitchBlock.activate` — was 49x25x49 = ~60k `getBlockState` calls per activation. Now
  collects the brick positions first, then converts them.

Behaviour is unchanged: neither operation creates new blocks of the type being searched for, so
collect-then-write is equivalent to the old interleaved read/write.

**New build check:** `checkNoRawCuboidScan` in `build.gradle`, modelled on `checkClientClassLeak`.
It scans compiled constant pools and fails if any class other than `BlockAreaScan` references both
`net/minecraft/core/BlockPos` and `betweenClosed`/`betweenClosedStream`. Wired into `check`, so
`.\gradlew build` enforces it. Verified non-vacuous: reintroducing a `BlockPos.betweenClosed` loop
in a throwaway class made the task fail with the offending class name, and removing it made it
pass again. Report is written to `build/reports/planeshift/raw-cuboid-scan.txt`.

If a future course feature legitimately needs raw cuboid iteration, add the class to the
`allowedClasses` list in the task rather than deleting the check.

### 3.1 Event-bus split / client registration
- Split `ClientEvents` into game-bus only and `ClientModEvents` for mod-bus registration.
  - `src/main/java/com/studio/planeshift/client/ClientEvents.java`
  - `src/main/java/com/studio/planeshift/client/ClientModEvents.java`
- Removed manual client listener registration from `PlaneShift.java`; `ClientModEvents` is now auto-discovered by `@EventBusSubscriber(modid = ..., value = Dist.CLIENT)`.
- `ServerEvents` made explicit with `@EventBusSubscriber(modid = ...)` and added `PlayerChangedDimensionEvent` cache clearing.

### 3.2 Resource fixes
- `pack.mcmeta` format updated to 71 (Minecraft 1.21.11).
- `dimension_type/course.json` fixed to valid 1.21.11 fields.
- `mineable/pickaxe.json` and new `needs_stone_tool.json` added for tool tags.
- `en_us.json` updated: Toad shop item keys, map/title command keys, moving-platform spawn egg.
- Removed stale `ClientNetworking.java`.

### 3.3 Gameplay & state safety
- `CourseService.returnToHub(ServerPlayer)` now uses `findRespawnPositionAndUseSpawnBlock`.
- `CourseCompletionService.onComplete` and `/planeshift leave` now return to hub and reset pips.
- `CheckpointService.returnToCheckpoint` supports cross-dimension teleport and falls back to hub.
- `ModeTransaction` now stores the source level; `ModeTransitionService` aborts commits/rollbacks when the player changes dimension.
- `ServerEvents` clears player caches on logout, respawn, and dimension change.
- `PlayerSizeService` and `HungerService` only run while `CourseState.inCourse()`.
- `FormService.useAction()` and `grant()` require the player to be in a course.
- `CourseCoopService.shareLives` no longer double-counts the source player.
- `CourseState` clamps coins/star coins/lives to `MAX_VALUE = 1,000,000` and sanitizes loaded mode/rail/state invariants.

### 3.4 Block / entity fixes
- `FireballProjectile` and `IceballProjectile` now `discard()` after hitting an entity.
- `PSwitchBlock` reverts converted brick blocks in `playerWillDestroy` so the `CONVERTED` map does not leak on early removal.
- `DamageService.down()` uses `player.kill(player.level())` instead of `setHealth(0)`.
- `CameraDirector` preserves the previous camera type when forcing third-person.
- `PlaneShiftGui.drawQuestionBlock` null-guards `Minecraft.getInstance()`.
- `PlaneShiftKeybinds.SWAP_RESERVE` moved from `G` to `V` to avoid vanilla/NeoForge conflict.

### 3.5 Commands
- `/planeshift` now requires a player (`CommandSourceStack::isPlayer`).
- `map` and `title` subcommands now send localized success feedback.

### 3.6 Other
- `CourseStructureService` checks the return value of `placeInWorld`.
- `HungerService` caches the reflection `Field` for `exhaustionLevel`.
- `PlaneShiftTitleScreen` and `ToadShopScreen` localized.

---

## 4. Known Remaining Issues

### P0 — Critical
*(None outstanding.)*

### P1 — High
*(None outstanding.)*

**Resolved 2026-08-31:**
- ~~GUI overflow~~ (P1), ~~coarse height checks~~, ~~PSwitch map leak~~, ~~C2S rate limiting~~ and ~~state-aware models~~ (P2) - see section 3.-4.
- ~~Projectile entity textures missing~~ (P1) and ~~mob effect icons missing~~ (P2) - see section 3.-3.
- ~~Sound events are empty~~ (P0) — 22 original OGGs now ship, see §3.-2.
- ~~Client-side player mutation in `PlaneConstrainedInput`~~ — see §3.-1.
- ~~Brute-force block scans in `OnOffSwitchBlock` / `PSwitchBlock`~~ — see §3.0.

### P2 — Medium
*(None outstanding.)*

### Warnings / smaller known issues
- `this-escape` warnings in block constructors. Cosmetic.
- Test coverage is thin: JUnit 5 is wired up (`src/test/java`, 31 cases) but only the 2.5D input
  projection geometry is covered. Everything else is guarded by the `check*` build tasks.
- **Zero GameTests exist** despite `build.gradle` configuring a `gameTestServer` run and setting
  `neoforge.enabledGameTestNamespaces`. 1.21.11 uses the data-driven `test_instance` registry;
  NeoForge bridges to Java through `RegisterGameTestsEvent` on the mod bus. This is the largest
  automated-testing gap.
- **Play-testing has never been done.** The mod launches clean on client and server, but nobody
  has walked the course flow. See §5.
- `src/generated/resources/` is **empty**, yet `build.gradle`'s comment claims datagen output is
  checked in so a clean clone builds without running the game. Either run `runClientData` and
  commit the result, or correct the comment.
- The built jar ships `src/generated/resources/.cache/` because that whole directory is a
  resource root. Harmless, but dead weight in the artifact.
- `ModEffects` gives `ice_aura` and `mini_aura` the same colour (`0x88CCFF`), so their HUD icons
  are identical. Registry-level, not a texture-generator bug.
- All art and audio are generated placeholders. Original and license-clean (see
  `ASSET_LICENSES.md`), replaceable in place at the same paths and sizes.

### Repository
- Remote: `https://github.com/OkVurb/hand-off`, branch `main`.
- CI: `.github/workflows/build.yml` runs `./gradlew build` on push and PR to `main`, so every
  `check*` task and the unit tests run automatically.

---

## 5. How to Continue

1. **Build:** `.\gradlew build` — includes the unit tests and all five `check*` tasks.
2. **After touching `src/main/resources/data/`:** `.\gradlew runServer`. The build cannot catch
   data-pack schema errors (see §3.-5); this loads every registry headlessly in about a minute.
   Look for `Done (Xs)! For help, type "help"` and zero `ERROR` lines.
3. **Run client:** `.\gradlew runClient`.
4. **Regenerate assets** (see `tools/README.md`): `java tools/SoundGen.java <wavDir>` then encode
   to OGG (needs ffmpeg with libvorbis), and
   `java tools/TextureGen.java src/main/resources/assets/planeshift/textures`.

### The play-test that has not been done

Everything below loads without error, but none of it has been played. Launch `runClient`, make a
Creative world, then:

| Step | Command / action | What to watch |
|---|---|---|
| 1 | `/planeshift role planeshift:balanced` | Role accepted |
| 2 | `/planeshift course course_1` | Teleport to `planeshift:course` at 0,64,0 in side-on. This is the path that threw before the §3.-5 dimension fix |
| 3 | — | HUD cluster top-left: pips, timer, lives, coins, star coins all inside the panel |
| 4 | A / D, then turn the mouse while holding D | Rail movement. W/S must do nothing. **Direction must not change as you look around** — that is the §3.-1 refactor |
| 5 | Walk off a ledge, jump ~2 ticks late | Coyote time |
| 6 | `/planeshift role planeshift:float_glide`, hold jump falling | Glider float, ~1.25 s |
| 7 | `/planeshift mode free_3d` / `side_on` | Camera blend, mode badge |
| 8 | `/planeshift checkpoint` | Beacon must visibly light — both states looked identical before §3.-4 |
| 9 | Jump into a `?` block from below | Pickup pops; the used block must look distinct from a brick |
| 10 | Brick + P-switch, then blow one up with TNT | Bricks→coins, revert after 10 s, and the revert must survive an explosion (§3.-4) |
| 11 | ON/OFF switch + ON/OFF block | Off state renders inset and is walk-through |
| 12 | `/planeshift coins 100`, Toad shop **at GUI Scale 4 in a small window** | All ten offers and Close must stay on screen (§3.-4) |
| 13 | Fall below `kill_y` (-50) | Death and checkpoint respawn |
| 14 | `/planeshift map`, `/planeshift leave` | Map screen, return to hub |

Also worth judging by ear and eye: the audio is synthesised chiptune, and the textures are flat
colours with 2-4 letter labels. Both are placeholders — the question is whether cues are audible
and whether blocks are tellable apart, not whether they are pretty.

---

## 6. Full Source & Resource Inventory

The following files make up the `src/main` source tree (generated 2026-08-30). Non-`src` files (Gradle, docs, build artifacts) are not included.

```
C:\Dev\PlaneShift\src\main\java\com\studio\planeshift\client\camera\CameraDirector.java
C:\Dev\PlaneShift\src\main\java\com\studio\planeshift\client\ClientCourseState.java
C:\Dev\PlaneShift\src\main\java\com\studio\planeshift\client\ClientEvents.java
C:\Dev\PlaneShift\src\main\java\com\studio\planeshift\client\ClientModEvents.java
C:\Dev\PlaneShift\src\main\java\com\studio\planeshift\client\gui\PlaneShiftGui.java
C:\Dev\PlaneShift\src\main\java\com\studio\planeshift\client\gui\PlaneShiftTitleScreen.java
C:\Dev\PlaneShift\src\main\java\com\studio\planeshift\client\hud\CourseHud.java
C:\Dev\PlaneShift\src\main\java\com\studio\planeshift\client\input\PlaneConstrainedInput.java
C:\Dev\PlaneShift\src\main\java\com\studio\planeshift\client\music\CourseMusicManager.java
C:\Dev\PlaneShift\src\main\java\com\studio\planeshift\client\PlaneShiftKeybinds.java
C:\Dev\PlaneShift\src\main\java\com\studio\planeshift\client\render\CourseEnemyRenderer.java
C:\Dev\PlaneShift\src\main\java\com\studio\planeshift\client\render\MovingPlatformRenderer.java
C:\Dev\PlaneShift\src\main\java\com\studio\planeshift\client\render\PlaceholderRigModel.java
C:\Dev\PlaneShift\src\main\java\com\studio\planeshift\client\render\ToadRenderer.java
C:\Dev\PlaneShift\src\main\java\com\studio\planeshift\client\screen\CourseMapScreen.java
C:\Dev\PlaneShift\src\main\java\com\studio\planeshift\client\screen\ToadShopScreen.java
C:\Dev\PlaneShift\tools\SoundGen.java                       (asset generator, not compiled into the mod)
C:\Dev\PlaneShift\tools\README.md
C:\Dev\PlaneShift\src\test\java\com\studio\planeshift\client\input\PlaneConstrainedInputTest.java
C:\Dev\PlaneShift\src\main\java\com\studio\planeshift\client\input\PlaneMovementAssists.java
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\sounds\  (22 OGG files, see tools/README.md)
C:\Dev\PlaneShift\src\main\java\com\studio\planeshift\common\block\BlockAreaScan.java
C:\Dev\PlaneShift\src\main\java\com\studio\planeshift\common\block\BrickBlock.java
C:\Dev\PlaneShift\src\main\java\com\studio\planeshift\common\block\CheckpointBeaconBlock.java
C:\Dev\PlaneShift\src\main\java\com\studio\planeshift\common\block\CoinBlock.java
C:\Dev\PlaneShift\src\main\java\com\studio\planeshift\common\block\CoinRingBlock.java
C:\Dev\PlaneShift\src\main\java\com\studio\planeshift\common\block\ConveyorBlock.java
C:\Dev\PlaneShift\src\main\java\com\studio\planeshift\common\block\FlagPoleBlock.java
C:\Dev\PlaneShift\src\main\java\com\studio\planeshift\common\block\HiddenQuestionBlock.java
C:\Dev\PlaneShift\src\main\java\com\studio\planeshift\common\block\HitFromBelowBlock.java
C:\Dev\PlaneShift\src\main\java\com\studio\planeshift\common\block\MusicBlock.java
C:\Dev\PlaneShift\src\main\java\com\studio\planeshift\common\block\OnOffBlock.java
C:\Dev\PlaneShift\src\main\java\com\studio\planeshift\common\block\OnOffSwitchBlock.java
C:\Dev\PlaneShift\src\main\java\com\studio\planeshift\common\block\PlaneshiftNoteBlock.java
C:\Dev\PlaneShift\src\main\java\com\studio\planeshift\common\block\PrizeCacheBlock.java
C:\Dev\PlaneShift\src\main\java\com\studio\planeshift\common\block\PSwitchBlock.java
C:\Dev\PlaneShift\src\main\java\com\studio\planeshift\common\block\QuestionBlock.java
C:\Dev\PlaneShift\src\main\java\com\studio\planeshift\common\block\SecretPassageBlock.java
C:\Dev\PlaneShift\src\main\java\com\studio\planeshift\common\block\ShiftGateBlock.java
C:\Dev\PlaneShift\src\main\java\com\studio\planeshift\common\block\SpikeBlock.java
C:\Dev\PlaneShift\src\main\java\com\studio\planeshift\common\block\SpringPadBlock.java
C:\Dev\PlaneShift\src\main\java\com\studio\planeshift\common\block\WarpPipeBlock.java
C:\Dev\PlaneShift\src\main\java\com\studio\planeshift\common\camera\CameraProfile.java
C:\Dev\PlaneShift\src\main\java\com\studio\planeshift\common\course\CourseDefinition.java
C:\Dev\PlaneShift\src\main\java\com\studio\planeshift\common\course\CourseState.java
C:\Dev\PlaneShift\src\main\java\com\studio\planeshift\common\course\CourseTheme.java
C:\Dev\PlaneShift\src\main\java\com\studio\planeshift\common\entity\BooEntity.java
C:\Dev\PlaneShift\src\main\java\com\studio\planeshift\common\entity\BooGoal.java
C:\Dev\PlaneShift\src\main\java\com\studio\planeshift\common\entity\BoomerangProjectile.java
C:\Dev\PlaneShift\src\main\java\com\studio\planeshift\common\entity\BowserEntity.java
C:\Dev\PlaneShift\src\main\java\com\studio\planeshift\common\entity\BowserFire.java
C:\Dev\PlaneShift\src\main\java\com\studio\planeshift\common\entity\BowserGoal.java
C:\Dev\PlaneShift\src\main\java\com\studio\planeshift\common\entity\BulletBillEntity.java
C:\Dev\PlaneShift\src\main\java\com\studio\planeshift\common\entity\BulletBillGoal.java
C:\Dev\PlaneShift\src\main\java\com\studio\planeshift\common\entity\BuzzyBeetleEntity.java
C:\Dev\PlaneShift\src\main\java\com\studio\planeshift\common\entity\CourseEnemyEntity.java
C:\Dev\PlaneShift\src\main\java\com\studio\planeshift\common\entity\EmberBoltEntity.java
C:\Dev\PlaneShift\src\main\java\com\studio\planeshift\common\entity\FireballProjectile.java
C:\Dev\PlaneShift\src\main\java\com\studio\planeshift\common\entity\GoombaEntity.java
C:\Dev\PlaneShift\src\main\java\com\studio\planeshift\common\entity\HammerBroEntity.java
C:\Dev\PlaneShift\src\main\java\com\studio\planeshift\common\entity\HammerBroGoal.java
C:\Dev\PlaneShift\src\main\java\com\studio\planeshift\common\entity\HammerProjectile.java
C:\Dev\PlaneShift\src\main\java\com\studio\planeshift\common\entity\IceballProjectile.java
C:\Dev\PlaneShift\src\main\java\com\studio\planeshift\common\entity\KoopaEntity.java
C:\Dev\PlaneShift\src\main\java\com\studio\planeshift\common\entity\LakituEntity.java
C:\Dev\PlaneShift\src\main\java\com\studio\planeshift\common\entity\LakituGoal.java
C:\Dev\PlaneShift\src\main\java\com\studio\planeshift\common\entity\LanePatrolGoal.java
C:\Dev\PlaneShift\src\main\java\com\studio\planeshift\common\entity\MovingPlatformEntity.java
C:\Dev\PlaneShift\src\main\java\com\studio\planeshift\common\entity\PiranhaPlantEntity.java
C:\Dev\PlaneShift\src\main\java\com\studio\planeshift\common\entity\ProjectileTracker.java
C:\Dev\PlaneShift\src\main\java\com\studio\planeshift\common\entity\SpinyEntity.java
C:\Dev\PlaneShift\src\main\java\com\studio\planeshift\common\entity\ThwompEntity.java
C:\Dev\PlaneShift\src\main\java\com\studio\planeshift\common\entity\ThwompGoal.java
C:\Dev\PlaneShift\src\main\java\com\studio\planeshift\common\entity\ToadEntity.java
C:\Dev\PlaneShift\src\main\java\com\studio\planeshift\common\form\FormActionKind.java
C:\Dev\PlaneShift\src\main\java\com\studio\planeshift\common\form\FormCategory.java
C:\Dev\PlaneShift\src\main\java\com\studio\planeshift\common\form\FormDefinition.java
C:\Dev\PlaneShift\src\main\java\com\studio\planeshift\common\form\FormSlot.java
C:\Dev\PlaneShift\src\main\java\com\studio\planeshift\common\item\AcornItem.java
C:\Dev\PlaneShift\src\main\java\com\studio\planeshift\common\item\BoomerangItem.java
C:\Dev\PlaneShift\src\main\java\com\studio\planeshift\common\item\CloudFlowerItem.java
C:\Dev\PlaneShift\src\main\java\com\studio\planeshift\common\item\CoinItem.java
C:\Dev\PlaneShift\src\main\java\com\studio\planeshift\common\item\ExtraPipItem.java
C:\Dev\PlaneShift\src\main\java\com\studio\planeshift\common\item\FireFlowerItem.java
C:\Dev\PlaneShift\src\main\java\com\studio\planeshift\common\item\FiveUpItem.java
C:\Dev\PlaneShift\src\main\java\com\studio\planeshift\common\item\FormCharmItem.java
C:\Dev\PlaneShift\src\main\java\com\studio\planeshift\common\item\HammerItem.java
C:\Dev\PlaneShift\src\main\java\com\studio\planeshift\common\item\IceFlowerItem.java
C:\Dev\PlaneShift\src\main\java\com\studio\planeshift\common\item\LeafItem.java
C:\Dev\PlaneShift\src\main\java\com\studio\planeshift\common\item\MegaMushroomItem.java
C:\Dev\PlaneShift\src\main\java\com\studio\planeshift\common\item\MiniMushroomItem.java
C:\Dev\PlaneShift\src\main\java\com\studio\planeshift\common\item\PropellerMushroomItem.java
C:\Dev\PlaneShift\src\main\java\com\studio\planeshift\common\item\StarCoinItem.java
C:\Dev\PlaneShift\src\main\java\com\studio\planeshift\common\item\StarPowerItem.java
C:\Dev\PlaneShift\src\main\java\com\studio\planeshift\common\item\SuperMushroomItem.java
C:\Dev\PlaneShift\src\main\java\com\studio\planeshift\common\item\TanookiSuitItem.java
C:\Dev\PlaneShift\src\main\java\com\studio\planeshift\common\item\ThreeUpItem.java
C:\Dev\PlaneShift\src\main\java\com\studio\planeshift\common\mode\ModeTransaction.java
C:\Dev\PlaneShift\src\main\java\com\studio\planeshift\common\mode\PlaneMode.java
C:\Dev\PlaneShift\src\main\java\com\studio\planeshift\common\mode\PlaneRail.java
C:\Dev\PlaneShift\src\main\java\com\studio\planeshift\common\mode\PlayState.java
C:\Dev\PlaneShift\src\main\java\com\studio\planeshift\common\mode\TransitionSync.java
C:\Dev\PlaneShift\src\main\java\com\studio\planeshift\common\network\CourseSelectPayload.java
C:\Dev\PlaneShift\src\main\java\com\studio\planeshift\common\network\FormActionPayload.java
C:\Dev\PlaneShift\src\main\java\com\studio\planeshift\common\network\ModNetworking.java
C:\Dev\PlaneShift\src\main\java\com\studio\planeshift\common\network\OpenCourseMapPayload.java
C:\Dev\PlaneShift\src\main\java\com\studio\planeshift\common\network\OpenTitleScreenPayload.java
C:\Dev\PlaneShift\src\main\java\com\studio\planeshift\common\network\OpenToadShopPayload.java
C:\Dev\PlaneShift\src\main\java\com\studio\planeshift\common\network\ReserveSwapPayload.java
C:\Dev\PlaneShift\src\main\java\com\studio\planeshift\common\network\ToadShopPurchasePayload.java
C:\Dev\PlaneShift\src\main\java\com\studio\planeshift\common\PlaneShiftConfig.java
C:\Dev\PlaneShift\src\main\java\com\studio\planeshift\common\registry\ModAttachments.java
C:\Dev\PlaneShift\src\main\java\com\studio\planeshift\common\registry\ModBlocks.java
C:\Dev\PlaneShift\src\main\java\com\studio\planeshift\common\registry\ModCreativeTabs.java
C:\Dev\PlaneShift\src\main\java\com\studio\planeshift\common\registry\ModEffects.java
C:\Dev\PlaneShift\src\main\java\com\studio\planeshift\common\registry\ModEntities.java
C:\Dev\PlaneShift\src\main\java\com\studio\planeshift\common\registry\ModItems.java
C:\Dev\PlaneShift\src\main\java\com\studio\planeshift\common\registry\ModParticles.java
C:\Dev\PlaneShift\src\main\java\com\studio\planeshift\common\registry\ModRegistries.java
C:\Dev\PlaneShift\src\main\java\com\studio\planeshift\common\registry\ModSounds.java
C:\Dev\PlaneShift\src\main\java\com\studio\planeshift\common\role\PlayerRole.java
C:\Dev\PlaneShift\src\main\java\com\studio\planeshift\common\role\RoleSignature.java
C:\Dev\PlaneShift\src\main\java\com\studio\planeshift\PlaneShift.java
C:\Dev\PlaneShift\src\main\java\com\studio\planeshift\server\AirMoveService.java
C:\Dev\PlaneShift\src\main\java\com\studio\planeshift\server\CheckpointService.java
C:\Dev\PlaneShift\src\main\java\com\studio\planeshift\server\CourseCompletionService.java
C:\Dev\PlaneShift\src\main\java\com\studio\planeshift\server\CourseCoopService.java
C:\Dev\PlaneShift\src\main\java\com\studio\planeshift\server\CourseScoringService.java
C:\Dev\PlaneShift\src\main\java\com\studio\planeshift\server\CourseService.java
C:\Dev\PlaneShift\src\main\java\com\studio\planeshift\server\CourseStateAccess.java
C:\Dev\PlaneShift\src\main\java\com\studio\planeshift\server\CourseStructureService.java
C:\Dev\PlaneShift\src\main\java\com\studio\planeshift\server\CourseThemeService.java
C:\Dev\PlaneShift\src\main\java\com\studio\planeshift\server\DamageService.java
C:\Dev\PlaneShift\src\main\java\com\studio\planeshift\server\FormService.java
C:\Dev\PlaneShift\src\main\java\com\studio\planeshift\server\HungerService.java
C:\Dev\PlaneShift\src\main\java\com\studio\planeshift\server\LeafFlightService.java
C:\Dev\PlaneShift\src\main\java\com\studio\planeshift\server\MobReplacementService.java
C:\Dev\PlaneShift\src\main\java\com\studio\planeshift\server\ModCompatibility.java
C:\Dev\PlaneShift\src\main\java\com\studio\planeshift\server\ModeTransitionService.java
C:\Dev\PlaneShift\src\main\java\com\studio\planeshift\server\MovementRuleService.java
C:\Dev\PlaneShift\src\main\java\com\studio\planeshift\server\PlaneShiftCommands.java
C:\Dev\PlaneShift\src\main\java\com\studio\planeshift\server\PlayerSizeService.java
C:\Dev\PlaneShift\src\main\java\com\studio\planeshift\server\RoleService.java
C:\Dev\PlaneShift\src\main\java\com\studio\planeshift\server\ServerEvents.java
C:\Dev\PlaneShift\src\main\java\com\studio\planeshift\server\ToadShopService.java
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\blockstates\brick_block.json
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\blockstates\checkpoint_beacon.json
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\blockstates\coin_block.json
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\blockstates\coin_ring_block.json
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\blockstates\conveyor_belt.json
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\blockstates\flag_pole.json
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\blockstates\hidden_question_block.json
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\blockstates\music_block.json
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\blockstates\note_block.json
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\blockstates\on_off_block.json
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\blockstates\on_off_switch.json
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\blockstates\p_switch.json
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\blockstates\prize_cache.json
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\blockstates\question_block.json
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\blockstates\secret_passage.json
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\blockstates\shift_gate.json
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\blockstates\spike_block.json
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\blockstates\spring_pad.json
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\blockstates\warp_pipe.json
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\lang\en_us.json
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\models\block\brick_block.json
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\models\block\checkpoint_beacon.json
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\models\block\coin_block.json
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\models\block\coin_ring_block.json
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\models\block\conveyor_belt.json
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\models\block\flag_pole.json
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\models\block\hidden_question_block.json
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\models\block\music_block.json
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\models\block\note_block.json
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\models\block\on_off_block.json
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\models\block\on_off_switch.json
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\models\block\p_switch.json
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\models\block\prize_cache.json
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\models\block\question_block.json
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\models\block\secret_passage.json
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\models\block\shift_gate.json
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\models\block\spike_block.json
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\models\block\spring_pad.json
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\models\block\warp_pipe.json
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\models\item\acorn.json
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\models\item\barrier_charm.json
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\models\item\boo_spawn_egg.json
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\models\item\boomerang.json
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\models\item\bowser_spawn_egg.json
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\models\item\brick_block.json
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\models\item\bullet_bill_spawn_egg.json
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\models\item\buzzy_beetle_spawn_egg.json
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\models\item\checkpoint_beacon.json
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\models\item\cloud_flower.json
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\models\item\coin.json
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\models\item\coin_block.json
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\models\item\coin_ring_block.json
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\models\item\conveyor_belt.json
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\models\item\ember_charm.json
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\models\item\extra_pip.json
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\models\item\fire_flower.json
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\models\item\five_up.json
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\models\item\flag_pole.json
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\models\item\gale_charm.json
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\models\item\goomba_spawn_egg.json
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\models\item\hammer.json
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\models\item\hammer_bro_spawn_egg.json
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\models\item\hidden_question_block.json
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\models\item\ice_flower.json
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\models\item\koopa_spawn_egg.json
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\models\item\lakitu_spawn_egg.json
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\models\item\leaf.json
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\models\item\magnet_charm.json
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\models\item\mega_mushroom.json
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\models\item\mini_mushroom.json
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\models\item\moving_platform_spawn_egg.json
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\models\item\music_block.json
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\models\item\note_block.json
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\models\item\on_off_block.json
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\models\item\on_off_switch.json
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\models\item\p_switch.json
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\models\item\piranha_plant_spawn_egg.json
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\models\item\prize_cache.json
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\models\item\propeller_mushroom.json
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\models\item\question_block.json
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\models\item\secret_passage.json
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\models\item\shift_gate.json
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\models\item\spike_block.json
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\models\item\spiny_spawn_egg.json
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\models\item\spring_pad.json
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\models\item\star_coin.json
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\models\item\star_power.json
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\models\item\super_mushroom.json
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\models\item\tanooki.json
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\models\item\three_up.json
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\models\item\thwomp_spawn_egg.json
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\models\item\toad_spawn_egg.json
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\models\item\warp_pipe.json
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\particles\coin_sparkle.json
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\particles\hit_burst.json
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\particles\pickup_glow.json
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\particles\respawn_warp.json
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\particles\theme_dust.json
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\sounds.json
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\textures\block\brick_block.png
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\textures\block\checkpoint_beacon.png
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\textures\block\coin_block.png
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\textures\block\coin_ring_block.png
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\textures\block\conveyor_belt.png
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\textures\block\flag_pole.png
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\textures\block\hidden_question_block.png
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\textures\block\music_block.png
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\textures\block\note_block.png
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\textures\block\on_off_block.png
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\textures\block\on_off_switch.png
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\textures\block\p_switch.png
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\textures\block\prize_cache.png
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\textures\block\question_block.png
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\textures\block\secret_passage.png
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\textures\block\shift_gate.png
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\textures\block\spike_block.png
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\textures\block\spring_pad.png
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\textures\block\warp_pipe.png
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\textures\entity\boo.png
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\textures\entity\bowser.png
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\textures\entity\bullet_bill.png
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\textures\entity\buzzy_beetle.png
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\textures\entity\goomba.png
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\textures\entity\hammer_bro.png
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\textures\entity\koopa.png
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\textures\entity\lakitu.png
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\textures\entity\moving_platform.png
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\textures\entity\piranha_plant.png
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\textures\entity\spiny.png
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\textures\entity\thwomp.png
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\textures\entity\toad.png
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\textures\item\acorn.png
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\textures\item\barrier_charm.png
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\textures\item\boo_spawn_egg.png
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\textures\item\boomerang.png
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\textures\item\bowser_spawn_egg.png
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\textures\item\bullet_bill_spawn_egg.png
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\textures\item\buzzy_beetle_spawn_egg.png
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\textures\item\cloud_flower.png
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\textures\item\coin.png
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\textures\item\ember_charm.png
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\textures\item\extra_pip.png
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\textures\item\fire_flower.png
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\textures\item\five_up.png
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\textures\item\gale_charm.png
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\textures\item\goomba_spawn_egg.png
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\textures\item\hammer.png
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\textures\item\hammer_bro_spawn_egg.png
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\textures\item\hidden_question_block.png
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\textures\item\ice_flower.png
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\textures\item\koopa_spawn_egg.png
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\textures\item\lakitu_spawn_egg.png
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\textures\item\leaf.png
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\textures\item\magnet_charm.png
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\textures\item\mega_mushroom.png
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\textures\item\mini_mushroom.png
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\textures\item\moving_platform_spawn_egg.png
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\textures\item\piranha_plant_spawn_egg.png
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\textures\item\propeller_mushroom.png
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\textures\item\spiny_spawn_egg.png
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\textures\item\star_coin.png
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\textures\item\star_power.png
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\textures\item\super_mushroom.png
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\textures\item\tanooki.png
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\textures\item\three_up.png
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\textures\item\thwomp_spawn_egg.png
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\textures\item\toad_spawn_egg.png
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\textures\particle\coin_sparkle.png
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\textures\particle\hit_burst.png
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\textures\particle\pickup_glow.png
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\textures\particle\respawn_warp.png
C:\Dev\PlaneShift\src\main\resources\assets\planeshift\textures\particle\theme_dust.png
C:\Dev\PlaneShift\src\main\resources\data\minecraft\tags\block\mineable\pickaxe.json
C:\Dev\PlaneShift\src\main\resources\data\minecraft\tags\block\needs_stone_tool.json
C:\Dev\PlaneShift\src\main\resources\data\planeshift\dimension\course.json
C:\Dev\PlaneShift\src\main\resources\data\planeshift\dimension_type\course.json
C:\Dev\PlaneShift\src\main\resources\data\planeshift\loot_table\blocks\checkpoint_beacon.json
C:\Dev\PlaneShift\src\main\resources\data\planeshift\loot_table\blocks\coin_block.json
C:\Dev\PlaneShift\src\main\resources\data\planeshift\loot_table\blocks\coin_ring_block.json
C:\Dev\PlaneShift\src\main\resources\data\planeshift\loot_table\blocks\conveyor_belt.json
C:\Dev\PlaneShift\src\main\resources\data\planeshift\loot_table\blocks\flag_pole.json
C:\Dev\PlaneShift\src\main\resources\data\planeshift\loot_table\blocks\hidden_question_block.json
C:\Dev\PlaneShift\src\main\resources\data\planeshift\loot_table\blocks\music_block.json
C:\Dev\PlaneShift\src\main\resources\data\planeshift\loot_table\blocks\note_block.json
C:\Dev\PlaneShift\src\main\resources\data\planeshift\loot_table\blocks\on_off_block.json
C:\Dev\PlaneShift\src\main\resources\data\planeshift\loot_table\blocks\on_off_switch.json
C:\Dev\PlaneShift\src\main\resources\data\planeshift\loot_table\blocks\p_switch.json
C:\Dev\PlaneShift\src\main\resources\data\planeshift\loot_table\blocks\prize_cache.json
C:\Dev\PlaneShift\src\main\resources\data\planeshift\loot_table\blocks\question_block.json
C:\Dev\PlaneShift\src\main\resources\data\planeshift\loot_table\blocks\spike_block.json
C:\Dev\PlaneShift\src\main\resources\data\planeshift\loot_table\blocks\spring_pad.json
C:\Dev\PlaneShift\src\main\resources\data\planeshift\loot_table\blocks\warp_pipe.json
C:\Dev\PlaneShift\src\main\resources\data\planeshift\planeshift\camera_profile\free_standard.json
C:\Dev\PlaneShift\src\main\resources\data\planeshift\planeshift\camera_profile\side_standard.json
C:\Dev\PlaneShift\src\main\resources\data\planeshift\planeshift\course\course_1.json
C:\Dev\PlaneShift\src\main\resources\data\planeshift\planeshift\course\course_2.json
C:\Dev\PlaneShift\src\main\resources\data\planeshift\planeshift\course\course_3.json
C:\Dev\PlaneShift\src\main\resources\data\planeshift\planeshift\form\acorn.json
C:\Dev\PlaneShift\src\main\resources\data\planeshift\planeshift\form\barrier_block.json
C:\Dev\PlaneShift\src\main\resources\data\planeshift\planeshift\form\boomerang.json
C:\Dev\PlaneShift\src\main\resources\data\planeshift\planeshift\form\cloud.json
C:\Dev\PlaneShift\src\main\resources\data\planeshift\planeshift\form\ember_core.json
C:\Dev\PlaneShift\src\main\resources\data\planeshift\planeshift\form\fire_flower.json
C:\Dev\PlaneShift\src\main\resources\data\planeshift\planeshift\form\gale_mantle.json
C:\Dev\PlaneShift\src\main\resources\data\planeshift\planeshift\form\hammer.json
C:\Dev\PlaneShift\src\main\resources\data\planeshift\planeshift\form\ice_flower.json
C:\Dev\PlaneShift\src\main\resources\data\planeshift\planeshift\form\magnet_lantern.json
C:\Dev\PlaneShift\src\main\resources\data\planeshift\planeshift\form\propeller.json
C:\Dev\PlaneShift\src\main\resources\data\planeshift\planeshift\form\tanooki.json
C:\Dev\PlaneShift\src\main\resources\data\planeshift\planeshift\role\balanced.json
C:\Dev\PlaneShift\src\main\resources\data\planeshift\planeshift\role\float_glide.json
C:\Dev\PlaneShift\src\main\resources\data\planeshift\planeshift\role\ground_burst.json
C:\Dev\PlaneShift\src\main\resources\data\planeshift\planeshift\role\sky_arc.json
C:\Dev\PlaneShift\src\main\resources\META-INF\neoforge.mods.toml
C:\Dev\PlaneShift\src\main\resources\pack.mcmeta

```

---

## 7. Environment Notes

- **No Git repository** exists in `C:\Dev\PlaneShift`. Git for Windows is installed at `C:\Program Files\Git\cmd\git.exe`, but the project has not been initialized.
- The MCP server list includes `github-mcp-server`; if configured, the project can be pushed to a GitHub repository.
- The workspace also contains `.mcsources` (MCP decompiled sources), `.gradle`, `build/`, and `run/` directories.

---

## 8. Next Actions (Suggested)

1. ~~Generate the missing sound files~~ — done 2026-08-31, see section 3.-2. Regenerate via `tools/README.md`.
2. ~~Add mob effect textures~~ — done 2026-08-31, see section 3.-3.
3. ~~Improve `PlaneConstrainedInput`~~ — done 2026-08-31, see section 3.-1.
4. ~~Optimize `OnOffSwitchBlock` and `PSwitchBlock` scans~~ — done 2026-08-31, see §3.0.
5. Play-test the course flow: start, checkpoint, respawn, complete, leave, Toad shop.
6. Reuse `BlockAreaScan` if any new course block needs an area search — the `checkNoRawCuboidScan` build check will reject a raw cuboid loop.
7. Replace the greybox textures with real art when ready — drop PNGs into `assets/planeshift/textures/` at the same paths and sizes; `checkTextureAssets` keeps coverage and distinctness honest.
