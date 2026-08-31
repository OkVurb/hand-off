# PlaneShift — Continuation Handoff

**Project root:** `C:\Dev\PlaneShift`  
**Date:** 2026-08-30  
**Status:** `BUILD SUCCESSFUL` — `compileJava`, `checkClientClassLeak`, and `build` all pass.

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
- Client class leak check: `.\gradlew checkClientClassLeak`

Current results:

```
> Task :compileJava — 9 this-escape warnings only, no errors.
> Task :checkClientClassLeak — PASSED
> Task :build — BUILD SUCCESSFUL
```

The remaining warnings are `this-escape` in constructors and are considered cosmetic for this vertical slice.

---

## 3. What Was Just Done

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
- **Sound events are empty.** `assets/planeshift/sounds.json` lists sounds with empty arrays and there are no `.ogg` files in `assets/planeshift/sounds/`. All audio is currently silent.

### P1 — High
- **GUI overflow on small screens.** `ToadShopScreen` uses a fixed layout that can fall off the bottom. `CourseHud` uses a fixed panel and pips may overflow.
- **Client-side player mutation.** `PlaneConstrainedInput` directly mutates `LocalPlayer` motion/rotation, which will be overwritten by the server and may rubber-band.
- **Brute-force block scans.** `OnOffSwitchBlock` and `PSwitchBlock` scan large `BlockPos` ranges every activation. This can stall the server tick in large courses.
- **Projectile-only entity textures missing.** Some projectiles have no dedicated textures.

### P2 — Medium
- **Mob effect icons missing.** `assets/planeshift/textures/mob_effect/*.png` are absent for the mod effects.
- **State-aware models missing.** `on_off_block.json` uses the same model for `on=true` and `on=false`. `hidden_question_block.json` has no empty/hidden model distinction.
- **Height checks are coarse.** `HiddenQuestionBlock`, `QuestionBlock`, `PrizeCacheBlock`, `BrickBlock` check the whole player Y instead of bounding-box/head contact.
- **PSwitch CONVERTED map still leaks on non-player removal** (explosions/pistons/SETBLOCK). `playerWillDestroy` only covers player breaks.
- **C2S payload handlers have no per-player rate-limit.** `CourseService.loadCourse` and `ToadShopService.purchase` can still be spammed.

### Warnings
- `this-escape` warnings in constructors.
- No tests exist.
- Git is not initialized in this workspace.

---

## 5. How to Continue

1. **Build:** `.\gradlew build`
2. **Run client:** `.\gradlew runClient`
3. **Run server:** `.\gradlew runServer`
4. **Add missing assets:**
   - Create `.ogg` sound files under `assets/planeshift/sounds/` and update `sounds.json`.
   - Add missing mob-effect PNGs under `assets/planeshift/textures/mob_effect/`.
   - Add state-aware block models for `on_off_block` and `hidden_question_block`.
5. **Test gameplay:** load a course, use shift gates, die/fall, complete course, leave, buy from Toad.

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

1. Generate or source the missing sound files and update `sounds.json`.
2. Add mob effect textures in `assets/planeshift/textures/mob_effect/`.
3. Improve `PlaneConstrainedInput` so it only modifies `Input`/`moveVector` and does not set `LocalPlayer` motion directly.
4. Optimize `OnOffSwitchBlock` and `PSwitchBlock` scans with chunk or tag-based lookups.
5. Play-test the course flow: start, checkpoint, respawn, complete, leave, Toad shop.
