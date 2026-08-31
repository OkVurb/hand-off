# PlaneShift — Session Progress Log

> **Every session MUST update this file before ending.** This is the single source of truth for "where I stopped" when tokens run out or a session is interrupted. The next session reads this file FIRST, before anything else.

---

## Current State

**Last updated:** 2026-08-31 (Claude session — backlog tasks 9-15)
**Build status:** GREEN — `.\gradlew build` passes with only this-escape warnings
**Server launch:** GREEN — `.\gradlew runServer` loads all data-pack registries cleanly
**Client launch:** GREEN — course 1 generated and rendered with the fixed side camera and skybox
**Open PRs:** None
**Open branches:** None

## What Was Done This Session

### Claude session — Phase 5 batch (63-69, 73, 74)

Hard items first again.

- Task 65 — `DonutBlock`: idle, shaking, then gone, all driven by scheduled ticks so nothing has
  to be scanned per tick. It **restores** after a delay rather than dropping a falling block; a
  player who misses the jump respawns at a checkpoint and would otherwise find the bridge
  permanently destroyed, soft-locking the course.
- Task 69 — `FirebarEntity`: one entity owns the whole rotating bar rather than one entity per
  flame, so a castle room with several bars stays cheap. Contact is tested per segment against a
  line, and it is indestructible on purpose — the player is meant to dodge it, not delete it.
- Task 64 — `AxeBlock` collapses the bridge one tile per scheduled tick, walking back from the
  axe. The scan is bounded and stops at the first non-bridge block, so a collapse cannot escape
  into arbitrary terrain.
- Tasks 73/74 — `SecretVineBlock` grows a climbable `CourseVineBlock` one segment at a time when
  hit from below, leading to a Coin Heaven cloud platform placed directly above it. Growth stops
  at any obstruction instead of carving through it.
- Task 63 — castle finale with a lava pit, bridge and two counter-rotating firebars. Lava theme
  only; a castle in a meadow reads as a mistake.
- Task 66 — a note-block run whose payoff is a ledge only reachable by bouncing off them.
- Tasks 67/68 — moving platforms on both axes over existing planned gaps. Two axes because they
  ask different things: a horizontal track is a timing problem, a vertical one is patience.

Still open in Phase 5: 61 (pipe sub-rooms), 62 (warp transition animation), 70 (underwater
physics), 71/72 (Cheep-Cheep, Blooper), 75 (ghost-house looping maze).

`runServer` reaches `Done (0.418s)` with zero registry errors. 111 textures, all distinct.

### Claude session — Phase 2/3 batch (18, 25, 27-29, 32-34, 42-45)

Hard items first, at the user's request.

- Task 45 — floating score popups. New `ScorePopupPayload` plus a `ScorePopups` HUD renderer;
  a stomp chain now reads as "+100, +200, +400" climbing. **Deliberately HUD-anchored, not
  world-anchored**: 1.21.11's `RenderLevelStageEvent` no longer exposes a camera or partial tick,
  and `Display.TextDisplay`'s setters are private, so world anchoring would mean rebuilding the
  projection by hand for a cosmetic label. Purely presentational — the score itself is synced
  with `CourseState`.
- Task 44 — squish framework. `SQUISH_TICKS` on `CourseEnemyEntity` with a sin-curve dip, read by
  a new `CourseEnemyRenderState`. Width widens as height flattens so volume looks conserved. On
  the base class, so every enemy including future ones squishes for free.
- Tasks 32/33 — Koopa shell. Three states: walking, shell, sliding. A stomp retreats it instead
  of killing it; touching a parked shell kicks it; a sliding shell destroys other enemies and
  ricochets off walls. Stomping a slide stops it, so a lost shell is always recoverable.
- Tasks 42/43 — Piranha Plant emerge cycle, and it will not emerge while a player is on or beside
  the pipe. Without that rule a player who lands on a pipe is bitten with no counterplay.
- Task 25 — the shrink is now animated. `PlayerSizeService` ramps the scale modifier over six
  ticks instead of snapping. Driven server-side because the attribute is synced, so the client
  interpolates for free and collision follows the same curve.
- Task 34 — Buzzy Beetle is fire-immune, including the mod's own fireball and ember bolt, which
  arrive as projectile hits rather than fire damage types.
- Task 18 — fireballs bounce along the ground with decaying arcs, and stop dead on walls so they
  cannot ricochet back at the player.
- Tasks 27/28 — `PowerupDriftService` makes mushrooms slide, turn at walls and fall off ledges.
  Hooked per entity via `EntityTickEvent`, not by sweeping the level, for the same reason
  `checkNoRawCuboidScan` exists.
- Task 29 — Poison Mushroom: drifts and pops like a real one, but costs a pip through
  `DamageService` so the Form buffer and i-frames behave normally.
- Task 7 revisited: bricks break regardless of player state, per the project owner.

Already implemented by earlier sessions, verified not redone: 16, 17, 19, 20, 26, 30, 31, 35, 36,
38, 39, 40, 41. Tasks 21-24 are covered by the texture motif pass.

Unit tests 66 -> 72. `runServer` reaches `Done (0.401s)` with zero registry errors.

### Claude session — backlog tasks 2, 4, 6, 8 and texture motifs

- Task 2 — course crouch: the crouched hitbox collapses to 0.5 blocks so the player can slide
  under one-block gaps. The rule lives in `CourseCrouch` and is applied by both a server and a
  client subscriber, because the client predicts its own movement and a disagreement about height
  makes the player stutter against gaps the server thinks they fit through.
- Task 4 — wall slide and wall jump in `AirMoveService`: a fall against a wall is capped so the
  player visibly clings, and a jump inside a short grace window relaunches them.
- Task 6 — `ModAttributes.BOUNCE_HEIGHT`, a real attribute replacing the hard-coded stomp bounce,
  so a Form, role or effect can tune it without `CourseEnemyEntity` knowing about any of them.
- Task 7 — decided against. Bricks break on any head contact regardless of player state; a block
  that sometimes ignores a correct hit reads as broken rather than as a rule.
- Task 8 — head bumps now rebound off the block instead of stopping dead.
- **Textures.** 72 of 103 were still flat colour with initials. `TextureGen` now draws real
  motifs — coin, ring, spikes, beacon, conveyor, note, button, chest, gate, mushroom, flower,
  star, leaf, egg, aura — plus a bevelled edge, falling back to initials only for unmapped names.
  It also refuses to overwrite any file bigger than a placeholder, so the production art added by
  earlier sessions is now safe from a regeneration run; `ASSET_LICENSES.md` no longer needs to
  warn about it. 24 production textures were left untouched.
- `ModEffects` gave `ice_aura` and `mini_aura` the same colour, so their icons were byte-identical
  once both used the aura motif. `mini_aura` is now distinct. This was a real in-world bug too:
  two different effects tinted the player identically.

### Claude session — backlog tasks 9-15 (course rules, clock and scoring)

- **Fixed a blocker that was already on `main`.** Commit `b3477a3` applied the jump boost twice:
  `RoleService` added a global `+1.5 ADD_MULTIPLIED_BASE` modifier (correct), and a committed
  `update_roles.py` *also* multiplied every role JSON `jump_multiplier` by 1.5. That pushed values
  to 1.5/1.42/1.68, outside the `[0.85, 1.15]` range `PlayerRole` declares, so the datapack failed
  to load and **no world could be created**. The build was green the whole time. Restored the role
  JSONs to their pre-`b3477a3` values, kept the intended global boost, and deleted
  `update_roles.py` so re-running it cannot break the mod again.
- Task 9 — auto-scroll: `PlaneConstrainedInput` drops backward impulse when `CourseState.autoScroll()`.
- Task 10 — course clock: `CourseTimerService` counts `CourseState.timeLeft` down once a second and
  routes expiry through `DamageService.down`, so checkpoint/life/game-over logic stays in one place.
- Task 11 — HUD clock: countdown replaces elapsed time on a timed course, flashing inside the
  100-second warning band.
- Task 12 — score lives in `CourseState` so it survives death, persists and reaches the HUD.
- Task 13 — stomp combo ladder (100/200/400/800/1000/2000/4000/8000, then 1-Ups). The chain only
  deepens while airborne; landing ends it.
- Task 14 — coins and star coins now award score. The 100-coins-to-1-Up rule already existed.
- Task 15 — game over now returns the player to the hub instead of respawning them inside the
  course with a fresh life bar, and clears the clock and auto-scroll rule.
- Added `checkDataRanges`: parses numeric ranges out of the codec source and validates every
  datapack JSON against them, so this class of bug fails the build instead of the server.
- Unit tests 38 -> 66. New cases cover the combo ladder and the clock sentinel/clamping.

### Earlier ChatGPT session

- Added five deterministic generated courses (grass, desert, snow, lava, underground), safely above kill Y, with terrain, gaps, platforms, hazards, rewards, checkpoints, switches, enemies and finish structures.
- Added six registered course terrain blocks and integrated the successful Mew block atlas.
- Added a custom static-feeling pixel-art course skybox and verified it in an integrated world.
- Added an articulated enemy rig, twelve visual profiles, procedural animations, converted entity skins, three detailed turnaround sheets and `docs/ENEMY_ART_DIRECTION.md`.
- Installed and signature-verified portable Blockbench 5.1.6; documented the modelling/animation workflow.
- Changed 2.5D avatar presentation so both body and head face only the last A/D direction; mouse movement no longer turns the visible head toward/away from camera, and actual player yaw remains untouched for gameplay aim.
- Added generated-course planning tests; the suite now has 38 passing cases.
- Implemented and successfully passed GameTests for `QuestionBlock`, `PSwitchBlock`, and `OnOffSwitchBlock` using data-driven registrations and reflection for testing internal block logic without networking issues.

## What Is In Progress

Nothing — all current tasks and prioritized fixes are complete up to the point of manual/creative art steps.

## Handoff

The next agent is Gemini. Its prompt is `GEMINI.md` and its 100-task backlog is
`docs/GEMINI_BACKLOG.md`. Phase A of that backlog is play-testing, which has still never been
done and is worth more than any new feature.

## What To Do Next (priority order)

0. **Play-test the new course rules** — start a course, watch the clock count down and flash under
   100 s, let it hit zero (should cost a life), chain stomps in one jump (100/200/400... then a
   1-Up past the 8th), and lose all lives to confirm the game-over now returns you to the hub.
   Auto-scroll is off for every shipped course; set `"auto_scroll": true` on one to try it.
1. **Live facing check** — launch course 1, hold A/D and move the mouse; body/head must stay left/right while movement and action aim remain stable.
2. **Full play-testing** — walk all five courses and exercise gaps, checkpoint/death, switches, rewards, shop and finish/leave flow.
3. **Bespoke entity models** — use Blockbench plus the three turnaround sheets and `docs/ENEMY_ART_DIRECTION.md`; the current articulated Java rig is shared/procedural, not final geometry.
4. **Art continuation** — per-theme skyboxes and final item/effect textures.

## How To Resume

1. `git pull` to get the latest
2. Read this file (PROGRESS.md) to see where the last session stopped
3. Read CODEX.md for the full project context and rules
4. Pick up from "What To Do Next" above
5. When done, update THIS file with what you did and what's next

## Session History

| Date | Agent | Summary |
|------|-------|---------|
| 2026-08-31 | Claude | Phase 5: donut blocks, firebars, axe bridge collapse, secret vines, coin heaven, note-block run, moving platforms; wrote the Gemini handoff (GEMINI.md + docs/GEMINI_BACKLOG.md) |
| 2026-08-31 | Claude | Phase 2/3: Koopa shell, Piranha cycle, squish framework, score popups, drifting powerups, Poison Mushroom |
| 2026-08-31 | Claude | Backlog tasks 9-15: auto-scroll, course clock, score, stomp combos, game-over exit; fixed a datapack blocker from b3477a3 that stopped any world loading; added checkDataRanges |
| 2026-08-31 | ChatGPT | Added five generated courses, original art/skybox, articulated enemies, Blockbench workflow and travel-locked body/head presentation |
| 2026-08-31 | Devin | Fixed all P0/P1/P2 issues, added 5 build checks, generated assets, pushed to GitHub, created handoff docs |
