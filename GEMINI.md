# Gemini Session Prompt — PlaneShift

Copy-paste the block below into a new Gemini session (Gemini app, Gemini CLI, or Gemini in
Antigravity). The 100-task backlog for Gemini is in `docs/GEMINI_BACKLOG.md`.

---

You are continuing the PlaneShift project. Be ultra-concise. No filler, no pleasantries, no
repeating my prompt back to me.

Repository: https://github.com/OkVurb/hand-off
Local root: C:\Dev\PlaneShift
Branch: main

## What this is

A NeoForge 1.21.11 Minecraft mod (Java 21, NeoForge 21.11.45, MDG 2.0.144). A Mario-inspired
2.5D platformer inside Minecraft: perspective-rail camera, Forms, enemies, generated courses,
shift gates, checkpoints, a course clock and an arcade scoring system.

## Start here, in order

1. `git pull`
2. Read **PROGRESS.md** — where the last session stopped. This is the single source of truth.
3. Read **AGENTS.md** — build invariants and conventions.
4. Read **docs/GEMINI_BACKLOG.md** — your 100 tasks.
5. `.\gradlew build` — must be green; only `this-escape` warnings are expected.

## Hard constraints

1. Client-only code stays under `com.studio.planeshift.client`. Run `.\gradlew checkClientClassLeak`
   after any client change.
2. GameTests register through `Registries.TEST_FUNCTION` via `RegisterEvent`, then wrap in
   `FunctionGameTestInstance` through `RegisterGameTestsEvent`. The `@GameTest` annotation does not
   exist in 1.21.11. Follow `server/test/PlaneShiftGameTests.java` exactly.
3. Commit as `OkVurb <85900298+OkVurb@users.noreply.github.com>`. Put `Generated with Gemini` in
   the commit body. Never use a fabricated AI email.
4. Do not commit `build/`, `.gradle/`, `run/`, `.mcsources/`, generated caches or secrets.
5. Never commit a one-shot mutation script (like the old `update_roles.py`) into the repo root. If
   you write one, run it and delete it. One such script silently broke the mod for a whole session.

## The rule that matters most

**A green build does not mean the mod works.** Datapack JSON that parses fine can still be
rejected by its codec at world load, taking the entire server down. This has already happened
twice: a wrong `dimension_type` schema, and role multipliers pushed outside their declared range.

After touching anything under `src/main/resources/data/`, run:

```
.\gradlew runServer
```

Look for `Done (Xs)! For help, type "help"` and zero `ERROR` lines. It takes about a minute and is
the cheapest real check that exists.

## Build checks you must work within

All are wired into `.\gradlew build` and run in CI on every push.

| Task | Enforces |
|---|---|
| `checkClientClassLeak` | `common`/`server` never reference `net.minecraft.client` |
| `checkNoRawCuboidScan` | Area block searches go through `BlockAreaScan`, not `BlockPos.betweenClosed` |
| `checkSoundAssets` | Every `ModSounds` entry has a real OGG and a subtitle key |
| `checkTextureAssets` | Every registered block/entity/effect/particle has a texture; no two are byte-identical |
| `checkBlockModels` | Model/blockstate JSON parses and resolves; multi-variant blocks must not render identically in every state; items need an `assets/planeshift/items/<id>.json`; `pack.mcmeta` uses `min_format`/`max_format` with a major above 81 |
| `checkDataRanges` | Datapack values stay inside the numeric ranges their codecs declare |

If you genuinely need to change a contract, widen it in the Java codec — `checkDataRanges` reads
the range out of the source, so it follows automatically. Do not disable a check to get green.

## Working rules

- Read `.mcsources/` (decompiled vanilla + NeoForge source) before guessing at a 1.21.11 API. The
  schemas for dimension types, pack metadata and item models all changed recently; guessing costs
  more than looking.
- Verify every new build check by deliberately breaking the thing it guards and confirming it
  fails. Every check in this repo was validated that way.
- Prefer the existing pattern: small focused Gradle verification tasks in the style of
  `checkClientClassLeak`, not broad frameworks.
- Verify which event bus (mod vs game) an event belongs to before wiring it.
- Asset generators live in `tools/` and are not compiled into the mod. `SoundGen.java` needs
  ffmpeg with libvorbis; SFX must be **mono** because Minecraft only positions mono sources.
  Running `TextureGen.java` over the whole texture directory overwrites production art — see
  `ASSET_LICENSES.md`.

## Current state

- All originally-tracked P0/P1/P2 issues are closed.
- Five generated courses exist (grass, desert, snow, lava, underground).
- Course clock, arcade score, stomp combo ladder and auto-scroll are implemented.
- 66 unit tests pass. GameTests exist for `QuestionBlock`, `PSwitchBlock`, `OnOffSwitchBlock`.
- Enemies use a shared procedural rig (`AnimatedCourseEnemyModel`), not bespoke geometry.
- **The mod has never been play-tested end to end.** It loads clean on client and server; nobody
  has actually walked a course.

## How to work

Pick a contiguous chunk of 5–10 tasks from `docs/GEMINI_BACKLOG.md`. Implement them thoroughly.
Run `.\gradlew build`, plus `.\gradlew runServer` if you touched `data/`. Add tests. Update
`PROGRESS.md` and `HANDOFF.md`. Commit and push `main`. Then take the next chunk.

If a task turns out to be already done, say so and move on rather than redoing it. Several tasks
in the previous backlog were already implemented and got reworked wastefully.
