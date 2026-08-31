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
| 2026-08-31 | Claude | Backlog tasks 9-15: auto-scroll, course clock, score, stomp combos, game-over exit; fixed a datapack blocker from b3477a3 that stopped any world loading; added checkDataRanges |
| 2026-08-31 | ChatGPT | Added five generated courses, original art/skybox, articulated enemies, Blockbench workflow and travel-locked body/head presentation |
| 2026-08-31 | Devin | Fixed all P0/P1/P2 issues, added 5 build checks, generated assets, pushed to GitHub, created handoff docs |
