# Next Claude / ChatGPT / Codex Session Prompt — PlaneShift

Copy and paste the block below into a fresh session, or let the agent read `PROGRESS.md`,
`HANDOFF.md`, and `CODEX.md` in the repository root.

---

You are continuing the PlaneShift project.

Repository: https://github.com/OkVurb/hand-off
Local root: C:\Dev\PlaneShift

PlaneShift is a NeoForge 1.21.11 Minecraft mod using Java 21, NeoForge 21.11.45 and MDG
2.0.144. Client-only code must remain under `com.studio.planeshift.client`; run
`checkClientClassLeak` after client changes.

Start by pulling `main`, then read `PROGRESS.md` and HANDOFF.md section 3.-6. Run
`.\gradlew build`; only existing this-escape warnings are expected. After changing anything in
`src/main/resources/data/`, also run `.\gradlew runServer` because JSON codec/schema errors are
not caught by the ordinary build.

Current state:

- Five deterministic generated courses now exist: grass, desert, snow, lava and underground.
  They have safe start/finish zones above kill Y, jump gaps, platforms, rewards, switches,
  checkpoints, hazards, enemies, shopkeepers and finish structures.
- Course 1 has been loaded in `runClient`. The fixed side camera, generated terrain and custom
  static-feeling pixel skybox rendered correctly. `runServer` reached `Done` cleanly.
- Six real course terrain blocks use the successful Mew atlas. Entity skins use the original Mew
  character sheet. Source/provenance is in `tools/art_sources/` and `ASSET_LICENSES.md`.
- Enemies use `AnimatedCourseEnemyModel` plus `EnemyRigProfile`, with articulated body parts and
  procedural walk/idle/wing/special animations. This is a working shared rig, not final bespoke
  geometry.
- Three detailed original enemy turnaround sheets and exact visual/animation requirements are in
  `tools/art_sources/enemy_turnaround_01.png` through `_03.png` and
  `docs/ENEMY_ART_DIRECTION.md`.
- Portable Blockbench 5.1.6 is installed at
  `C:\Users\cr0od\Apps\Blockbench\Blockbench.exe`. Read `tools/blockbench/README.md` for clip
  names and workflow. Do not bypass Mew credit limits or create throwaway accounts.
- In 2.5D, `PlaneConstrainedInput` changes only `Input`/`moveVector`. The post-tick presentation
  pass locks both visible body and head to the last attempted A/D travel direction, so mouse
  movement cannot turn the head toward/away from camera. It deliberately does not alter `yRot`,
  preserving movement projection and gameplay action aim.
- The full unit suite has 38 passing cases: input projection plus generated-course safety/gaps.

Priority work:

1. Live-check the newest facing change: in course 1 hold A/D while moving the mouse. The body and
   head must face only left/right; movement and action aim must remain stable.
2. Play all five courses end-to-end and fix practical layout/gameplay problems. Test coyote time,
   glider, switches, checkpoint/death at kill Y=40, rewards, shop, flag finish and leave flow.
3. Create bespoke Blockbench models and keyframed clips for each enemy from the turnaround sheets.
   Preserve the original sky-island designs; do not copy protected franchise characters. Keep
   the current Java rig as a fallback until replacements work in-game.
4. Add GameTests for QuestionBlock, PSwitchBlock and OnOffSwitch. Existing `build.gradle` already
   configures `gameTestServer` but no GameTests exist.
5. Continue final art: per-theme skyboxes and remaining item/effect textures. The current skybox
   is intentionally shared by all courses.

Rules:

- Do not commit `build/`, `.gradle/`, `run/`, `.mcsources/`, generated caches or secrets.
- Prefer existing project patterns and inspect `.mcsources/` before guessing 1.21.11 APIs.
- Verify event bus ownership before changing event wiring.
- Run `.\gradlew build`, update `HANDOFF.md` and `PROGRESS.md`, commit, and push `main` when done.

---
