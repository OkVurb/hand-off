# PlaneShift — Gemini Backlog (100 tasks)

Work through this in **contiguous chunks of 5–10**. Read `PROGRESS.md` first — it says where the
last session stopped. Read `GEMINI.md` for the rules and build invariants.

**Before starting any task, check whether it is already done.** The previous backlog wasted
significant effort re-implementing things earlier sessions had already built. If a task turns out
to be complete, say so in `PROGRESS.md` and move on.

**After each chunk:** `.\gradlew build`, plus `.\gradlew runServer` if you touched
`src/main/resources/data/`. Update `PROGRESS.md` and `HANDOFF.md`. Commit and push `main`.

---

## Phase A — Verification and play-testing (1–10)

The mod has **never been played end to end**. This phase is worth more than any new feature.

1. Play course 1 start to finish. Record every problem in `PROGRESS.md` before fixing anything.
2. Play courses 2–5. Note which themes have unfair gaps, unreachable platforms or dead ends.
3. Verify the 2.5D rail: hold A/D while moving the mouse. Direction must not change with facing.
4. Verify coyote time, jump buffering and the Glider float feel right; tune the constants in
   `PlaneMovementAssists` if not.
5. Verify the course clock: countdown, the flash under 100 s, and death at zero.
6. Verify the stomp combo ladder: chain enemies in one jump and confirm 100/200/400 popups, then
   a 1-Up past the eighth.
7. Verify game over returns to the hub rather than respawning inside the course.
8. Verify the Toad shop at GUI Scale 4 in a small window — all ten offers and Close must be
   reachable.
9. Verify every new Phase 5 object in world: donut bridge, firebar, axe collapse, secret vine,
   coin heaven, note-block ledge, both moving platforms.
10. Write up a "known feel problems" section in `HANDOFF.md` from everything above.

## Phase B — Blockbench models and animation (11–25)

Portable Blockbench 5.1.6 is at `C:\Users\cr0od\Apps\Blockbench\Blockbench.exe`. Read
`tools/blockbench/README.md` and `docs/ENEMY_ART_DIRECTION.md`. Turnaround sheets are in
`tools/art_sources/`. Keep the current procedural Java rig working as a fallback until each
replacement is verified in game.

11. Goomba model and walk clip.
12. Koopa Troopa model, plus a distinct **shell** model — the shell is a separate gameplay state
    and currently renders identically to the walker.
13. Koopa shell sliding animation.
14. Buzzy Beetle model.
15. Spiny model, with spikes readable in silhouette from the side camera.
16. Lakitu model and its cloud.
17. Boo model, including the covering-face pose for when the player looks at it.
18. Thwomp model, with a distinct wind-up pose before it drops.
19. Hammer Bro model and throw animation.
20. Piranha Plant model that reads correctly at partial extension (see `extensionFor`).
21. Toad NPC model for the shop.
22. Flagpole model with a flag that slides down.
23. Firebar visual: currently pure flame particles with an empty renderer. Decide whether a model
    improves it or whether particles are the right call, and write down why.
24. Donut Block "shaking" model that visibly differs from idle.
25. Player model swap while inside a course dimension.

## Phase C — Remaining level generation (26–38)

26. Warp Pipe sub-rooms: entering a pipe moves the player to an offset area and back.
27. Warp Pipe transition animation (the pipe entry/exit currently teleports instantly).
28. Underwater course theme with water physics overrides.
29. Cheep-Cheep entity: jumps in arcs from below.
30. Blooper entity: jerky diagonal bursts underwater.
31. Ghost-house looping maze: a corridor that returns the player to its own start until they find
    the correct exit.
32. Generate a proper castle interior for the lava theme rather than only the bridge arena.
33. Add a second, harder donut-block section gated behind the checkpoint.
34. Vertical climbing section using note blocks and moving platforms together.
35. Conveyor-belt gauntlet using the existing `ConveyorBlock`.
36. Make `CourseLayoutPlan` data-driven from the course JSON instead of hard-coded per theme.
37. Add a course-length parameter that actually changes the number of set pieces, not just terrain.
38. Ensure every generated course is completable without taking damage. Prove it, do not assume it.

## Phase D — Boss fight and finale (39–50)

39. Bowser entity logic: fire breath, jumps, hammer throws, phases.
40. Bowser Blockbench model and animations.
41. Bowser arena generation tied to the axe bridge already built.
42. Bowser death animation: falling into the lava when the bridge drops.
43. Boss health bar HUD.
44. Boss music that starts on arena entry and stops on defeat.
45. "Thank you" Toad dialogue sequence after a castle clear.
46. Course-complete results screen with score breakdown.
47. World map screen for course selection (`docs/WORLD_MAP_DESIGN.md` has the design).
48. Save data tracking which courses have been beaten.
49. Star Coin collection tracking, per course, persisted.
50. Unlock gating: later courses require earlier ones cleared.

## Phase E — Audio and particles (51–62)

All 22 sounds already exist as generated OGGs (`tools/SoundGen.java`). This phase is about
*using* them well and adding what is missing.

51. Audit which registered sounds are never actually played, and either wire them up or remove
    them.
52. "Hurry up" music variant when the clock drops below 100 s — `CourseState.timeCritical()`
    already reports this.
53. Star Power invincibility music override.
54. Distinct per-theme music instead of one course track for all five.
55. Coin particle that flies up and fades when a coin block is hit.
56. Brick debris particles on break.
57. Smoke puff when an enemy is defeated.
58. Underwater ripple effect for the water theme.
59. Ambient ash and spark particles for the lava theme.
60. Footstep and landing sounds tuned per course block type.
61. Sound for the donut block shaking, distinct from the bump it currently reuses.
62. Mix pass: several cues are currently reused across unrelated events. Give each event its own.

## Phase F — UI, UX and accessibility (63–74)

63. Move the score popups from HUD-anchored to world-anchored, if the 1.21.11 render API allows
    it cleanly. Read the note in `ScorePopups` first — this was deliberately deferred.
64. HUD layout pass at GUI scales 1 through 4 and at ultrawide aspect ratios.
65. Controller/gamepad support for the 2.5D lane.
66. Rebindable keys for Form action and reserve swap (currently R and V).
67. Colour-blind-safe palette check across every HUD element and block state.
68. Subtitles for every sound (the keys exist; verify they are accurate and complete).
69. A reduced-motion option that damps the camera shake and transitions.
70. In-game tutorial or first-course signposting for the shift-gate mechanic.
71. Death and game-over screens with a retry option.
72. Pause menu that does not unload the course.
73. Course timer and score visible on the results screen, not just in chat.
74. Localisation pass: extract any remaining hard-coded English strings into `en_us.json`.

## Phase G — Config, performance and multiplayer (75–87)

75. NeoForge config for jump multipliers, gravity, course clock length and HUD scale.
76. Server config for whether courses are shared or per-player.
77. Profile a full course with the debug HUD; find the most expensive tick.
78. Audit every service for per-tick allocation and remove it from hot paths.
79. Verify `BlockAreaScan` is still used for every area search added since it was written.
80. Two-player co-op through one course: verify `CourseCoopService` actually works.
81. Decide and document what happens when co-op players are in different course dimensions.
82. Handle a player disconnecting mid-course and rejoining cleanly.
83. Verify the dedicated server never loads a client class (`checkClientClassLeak` covers this;
    confirm it still passes with all new code).
84. Chunk-loading strategy for long courses, so the far end is not generated until approached.
85. Entity cleanup: verify `GENERATED_TAG` entities are removed on course reload, including the
    new firebars and platforms.
86. Memory audit over repeated course loads; look for leaked static state.
87. Verify the mod loads alongside Sodium, Lithium and Iris (`docs/THEME_FRIENDLY_MODS.md`).

## Phase H — Testing, CI and release (88–100)

88. GameTests for the Koopa shell state machine: stomp, kick, slide, stop.
89. GameTests for the donut block phase cycle.
90. GameTests for the axe bridge collapse sequence.
91. GameTests for the secret vine growth and its obstruction stop.
92. GameTests for the course clock expiring and costing a life.
93. Unit tests for `CourseLayoutPlan` gap placement across all five themes.
94. A build check that every registered entity type has a renderer — a missing one crashes the
    client on sight, and nothing currently catches it.
95. A build check that every block with a `BooleanProperty`/`IntegerProperty` has blockstate
    entries for every combination.
96. Extend CI to run `runGameTestServer` as well as `build`.
97. Extend CI to run `runServer` headlessly, since datapack schema errors are invisible to
    `build`.
98. Fix the remaining `this-escape` warnings in block and entity constructors.
99. Remove `src/generated/resources/.cache/` from the built jar, and either commit real datagen
    output or correct the comment in `build.gradle` that claims it is checked in.
100. Tag a release, write `CHANGELOG.md` from the git history, and build the distributable jar.

---

## Things that are already done — do not redo

- All original P0/P1/P2 issues from `HANDOFF.md` section 4.
- 2.5D input projection, coyote time, jump buffering, Glider float, wall jump, ground pound,
  crouch hitbox.
- Course clock, arcade score, stomp combo ladder, auto-scroll flag, game-over exit to hub.
- Koopa shell, Piranha Plant cycle, Buzzy Beetle fire immunity, Spiny anti-stomp, enemy squish
  framework, score popups.
- Donut blocks, firebars, axe bridge collapse, secret vines, coin heaven, note-block run, moving
  platforms on both axes, castle finale.
- Mushroom drift, Poison Mushroom, fireball ground bounce, animated shrink.
- Five generated courses, all 22 sounds, 111 placeholder textures, six build checks, 72 unit
  tests, GameTests for three block types.
