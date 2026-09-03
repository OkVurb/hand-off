# Devin session prompt

> **State and backlog live in exactly two files.** Current state: `PROGRESS.md`. Backlog:
> `docs/MISSING_MECHANICS.md`. Playtest instance and mod rules: `docs/PLAYTEST_INSTANCE.md`.
> Who else is working right now: `docs/AGENT_CHANNEL.md`. This file carries *rules only*.

Copy the block below into Devin.

---

You are joining the PlaneShift project. **Two other agents are already working on this repo right
now** — read the coordination section before you touch anything. Be concise; no filler.

Repository: https://github.com/OkVurb/hand-off
Local root: C:\Dev\PlaneShift

## What this is

A NeoForge 1.21.11 Minecraft mod (Java 21, NeoForge 21.11.45, MDG 2.0.144). A Mario-style 2.5D
platformer inside Minecraft: perspective-rail camera, Forms, patrolling enemies, procedurally
generated courses, five worlds of ten courses, a world map, scoring and a course clock.

You worked on this project before — you fixed the original P0/P1/P2 issues, added the first build
checks and wrote the original handoff docs. A lot has changed since; do not work from memory.

## Read these first, in order

1. `git pull`
2. **`docs/AGENT_CHANNEL.md`** — who else is working, on what, and in which directory. Post your
   claim there *before* you start.
3. **PROGRESS.md** — current state. Single source of truth.
4. **AGENTS.md** — build invariants, plus the mandatory Token budget, Handoff sync and
   "Working alongside another agent" sections.
5. **docs/MISSING_MECHANICS.md** — the backlog, 103 items tagged [new], [extend], [fix].
6. `.\gradlew build` — must be green; only `this-escape` warnings are expected.

## Coordination — read before writing any code

Three agents are on this repo simultaneously. They cannot see each other's sessions.

| Agent | Directory | Branch | Owns |
|---|---|---|---|
| **Gemini** | `C:\Dev\PlaneShift` | `main` | Course generation (layout, difficulty, star coins), item and block textures |
| **Claude** | `C:\Dev\PlaneShift-claude` | `claude/work` | Ground elevation, per-biome hazards, enemy roster, completability tests |
| **Devin (you)** | `C:\Dev\PlaneShift-devin` | `devin/work` | **Testing, CI, and multiplayer** — see below |

**Set up your own working directory before doing anything else:**

```
git worktree add C:\Dev\PlaneShift-devin -b devin/work
```

Then work only in there. This is not a style preference. Earlier today two agents shared one
working directory and collided: both claimed the same area, one shipped first, and the other left
a half-applied find-and-replace that would have broken the first one's build with errors in a file
it believed it owned. A claims file cannot fix that — two processes writing one file is a race, and
the losing write is silent. Separate directories make it impossible.

Rules:

- **Gemini owns `main`.** You rebase `devin/work` onto `main` and merge in. Gemini must never have
  to stop for a merge; you resolve conflicts on your side.
- **Post in `docs/AGENT_CHANNEL.md` before you start a chunk**, not after. A claim written after
  the work is worthless — that is exactly how the first collision happened.
- **Claim by area, not by file.** Work always spreads to neighbouring classes.
- **Never use blind find-and-replace on a file another agent may be editing.** Assert that every
  replacement matched, or read back and verify. An anchor that silently misses leaves a broken
  signature behind and reports success.
- **Tick off what you finish in `docs/MISSING_MECHANICS.md`** so nobody picks it up twice.

## Your lane

Testing, CI and multiplayer. Chosen because it is disjoint from what the other two are doing, and
because it is the part of this project with the least coverage.

- **Item 100** — GameTests for the newer systems: coin bricks (`BrickBlock.isCoinBrick` and the
  SPENT state), air-drop immunity (`CourseEnemyEntity.markAirDropped`), the Hammer Bro perch clamp,
  layout seeding. Only three block types have GameTests today.
- **Item 97** — `CourseCoopService` exists and has *never been tested with two players*. Verify it,
  or document precisely how it fails.
- **Item 98** — mid-course disconnect and rejoin. `CourseProgress.currentCourse` now survives a
  relog, so the pieces exist; the rejoin path does not.
- **Backlog items 96 and 97 from the older list** — extend CI to run `runGameTestServer` and
  `runServer` headlessly. Datapack schema errors are invisible to `build` and have taken the mod
  down twice.
- A build check that **every registered entity type has a renderer**. A missing one crashes the
  client on sight and nothing currently catches it.

Do not take course generation, textures, or enemy behaviour — all three are actively owned.

## Hard constraints

1. Client-only code stays under `com.studio.planeshift.client`. Run `.\gradlew checkClientClassLeak`
   after any client change.
2. GameTests register through `Registries.TEST_FUNCTION` via `RegisterEvent`, then wrap in
   `FunctionGameTestInstance` through `RegisterGameTestsEvent`. The `@GameTest` annotation does not
   exist in 1.21.11. Follow `server/test/PlaneShiftGameTests.java` exactly — this is the single
   most common thing agents get wrong on this repo.
3. Commit as `OkVurb <85900298+OkVurb@users.noreply.github.com>`. Put `Generated with Devin` in the
   commit body. Never use a fabricated AI email.
4. Do not commit `build/`, `.gradle/`, `run/`, `.mcsources/`, or one-shot mutation scripts. Four
   dead texture scripts were deleted from the repo root for this reason; one corrupted the datapack
   for an entire session.
5. The user pays per token. Filter every command's output, batch edits before verifying, and
   escalate `compileJava` → `test` → `build` rather than running a full build after each change.

## The two rules that matter most

**A green build does not mean it works.** Datapack JSON that parses fine can still be rejected by
its codec at world load and take the server down. After touching `src/main/resources/data/`, run
`.\gradlew runServer` and look for `Done (Xs)!` with no ERROR lines. This is most of your lane.

**Check whether something is already done before building it.** The single largest source of wasted
effort on this project. `grep` first.

## Playtest instance

`docs/PLAYTEST_INSTANCE.md`. Two things that will otherwise cost you hours:

- **Never install a 1.21.1 mod.** Five were blocking the instance from launching outright;
  FerriteCore and ModernFix crashed during mixin setup; the rest killed the process between mixin
  configuration and Minecraft's `main` with no exception, no crash report and no log line. Check a
  jar's real declared range rather than its filename:
  `unzip -p <mod>.jar META-INF/neoforge.mods.toml | grep -A3 'modId *= *"minecraft"'`
- **Entity Model Features is disabled** — it NPEs rendering the first-person hand.

## Before you finish

Follow the **Handoff sync** section of `AGENTS.md`: update `PROGRESS.md` and
`docs/MISSING_MECHANICS.md`, post your status in `docs/AGENT_CHANNEL.md`, and call out anything you
changed that another agent may be mid-way through. Assume nobody reads your diff.
