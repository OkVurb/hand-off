# Gemini session prompt

Copy the block below into Gemini. Everything it needs is in the repo.

---

You are continuing the PlaneShift project. Be ultra-concise. No filler, no pleasantries, no
repeating my prompt back to me.

Repository: https://github.com/OkVurb/hand-off
Local root: C:\Dev\PlaneShift
Branch: main

## What this is

A NeoForge 1.21.11 Minecraft mod (Java 21, NeoForge 21.11.45, MDG 2.0.144). A Mario-style 2.5D
platformer inside Minecraft: perspective-rail camera, Forms, patrolling enemies, procedurally
generated courses, five worlds of ten courses, a world map, scoring and a course clock.

## Start here, in order

1. `git pull`
2. **PROGRESS.md** — where the last session stopped. Single source of truth.
3. **AGENTS.md** — build invariants, conventions, and the mandatory **Token budget** section.
4. **docs/MISSING_MECHANICS.md** — your backlog. 103 items, each tagged [new], [extend] or [fix].
5. **docs/PLAYTEST_INSTANCE.md** — the CurseForge instance and its mod rules. Read before touching
   anything about launching or mod compatibility.
6. `.\gradlew build` — must be green; only `this-escape` warnings are expected.

## Hard constraints

1. Client-only code stays under `com.studio.planeshift.client`. Run `.\gradlew checkClientClassLeak`
   after any client change.
2. GameTests register through `Registries.TEST_FUNCTION` via `RegisterEvent`, then wrap in
   `FunctionGameTestInstance` through `RegisterGameTestsEvent`. The `@GameTest` annotation does not
   exist in 1.21.11. Follow `server/test/PlaneShiftGameTests.java`.
3. Commit as `OkVurb <85900298+OkVurb@users.noreply.github.com>`. Put `Generated with Gemini` in the
   commit body. Never use a fabricated AI email.
4. Do not commit `build/`, `.gradle/`, `run/`, `.mcsources/`, or one-shot mutation scripts. Four
   dead texture scripts were deleted from the repo root for this reason; one of them corrupted the
   datapack for a whole session.
5. The user pays per token. Filter every command's output, batch edits before verifying, and
   escalate `compileJava` → `test` → `build` rather than running a full build after each change.

## The two rules that matter most

**A green build does not mean it works.** Datapack JSON that parses fine can still be rejected by
its codec at world load and take the server down. After touching `src/main/resources/data/`, run
`.\gradlew runServer` and look for `Done (Xs)!` with no ERROR lines.

**Check whether a task is already done before building it.** This is the single largest source of
wasted effort on this project — multiple sessions have rebuilt existing features. The backlog marks
[extend] and [fix] items precisely because real code already sits behind them. `grep` first.

## Build checks

Six, all wired into `build` and CI: `checkClientClassLeak`, `checkNoRawCuboidScan`,
`checkSoundAssets`, `checkTextureAssets`, `checkBlockModels`, `checkDataRanges`. Widen a contract in
the Java codec if you genuinely need to; never disable a check to get green. Verify any new check by
deliberately breaking the thing it guards.

## Current state

Working and verified in-game this session:

- Courses generate from a layout seeded on the **world seed mixed with the course id**, so a new
  save is a new set of courses while a given course still regenerates identically for retries.
- Each world is one biome. The tenth course of each keeps its castle finale via the feature list.
- Ground enemies patrol and turn at walls and ledges. None of them chase — that was removed.
- Head bumps work. Question blocks darken rather than break; roughly one brick in four is a coin
  brick that pays a coin, darkens and stays solid, chosen by position hash so routes are learnable.
- Progression persists: cleared courses, star coin counts, best scores, server-enforced unlocking,
  a world map, a results screen and a game-over screen with retry.
- Movement: 2.5D rail, coyote time, jump buffering, ground pound, crouch, Glider float. Cross-rail
  momentum is folded onto the travel axis so third-party dash mods work. PlaneShift's own wall jump
  is off by default because it read as a free double jump.
- Config covers jump, run, conveyor speed, HUD scale, hurry-up music, boss music and the tester
  menu. Cloth Config gives it an in-game UI.
- Press **F6** or run **`/planeshift test`** for the tester menu: power-ups, enemy spawns, course
  jumps, clock and lives, progress reset.
- 153 unit tests. GameTests exist for three block types only.

Known unfinished, highest value first:

1. **Star coins are never placed by generation**, so the per-course counter can only read zero.
2. **All 37 item textures are generated placeholders** under 400 bytes. Most visible gap in the mod.
3. **Secret vines cannot be climbed**, so the coin heaven above them is unreachable by its route.
4. **Difficulty does not scale across worlds** — world 5 generates with world 1's parameters.
5. The mod has never been played start-to-finish by anyone.

## Working with the installed mods

The playtest instance runs a real 1.21.11 mod set. Several backlog items are already provided by
one of them, and building a second copy is worse than not building it — two systems on one input
reads as a bug in both. `docs/MISSING_MECHANICS.md` has the full table. The short version:

- **Omni** and **Enhanced Movement** already give sprint, ledge grabs and wall jumps. Do not
  reimplement them; PlaneShift yields.
- **Boss Music Mod** owns boss music. PlaneShift's is off by default; leave it off.
- **AttributeFix** is what stops large jump boosts being silently clamped. Do not remove it.
- **SereneSeasons** drifts biome tint over time, which fights the one-biome-per-world rule. Settle
  this before doing anything about per-theme identity.
- **Never install a 1.21.1 mod.** Half a day went into a launch failure caused by them. Five were
  blocking startup outright; FerriteCore and ModernFix crashed during mixin setup; and the rest
  killed the process between mixin config and Minecraft's `main` with no exception, no crash report
  and no log line. Check a jar's declared range rather than its filename:
  `unzip -p <mod>.jar META-INF/neoforge.mods.toml | grep -A3 'modId *= *"minecraft"'`

## How to work

Take a contiguous chunk of 5–10 backlog items. Prefer [fix] items first — they are cheap and the
feature is currently broken rather than absent, which is worse. Implement thoroughly, add tests,
run `.\gradlew build` plus `runServer` if you touched `data/`. Update PROGRESS.md. Commit and push
main. Then take the next chunk.

If an item turns out to be already done, say so in PROGRESS.md and move on rather than redoing it.
