# PlaneShift — Agent Notes

## Communication style (MANDATORY — applies to every session, every model)
Act as an ultra-concise assistant. Provide only direct, high-value answers with no filler, pleasantries, or repeating of my prompt back to me. Keep responses short and punchy. If you need more context to complete the task, ask a single clarifying question rather than guessing. This saves token usage and credits.

## Token budget (MANDATORY — applies to every session, every model)
The user pays per token, and every tool result is re-sent with the whole conversation on each
request. **What costs money is output coming back into context, not work happening on disk.** Act
accordingly:

- **Filter every command's output.** Never let a raw `gradlew` run dump into context. Use
  `... 2>&1 | Select-String -Pattern "error:|FAILED|BUILD"` (PowerShell) or `| grep -E`. A full
  unfiltered build log is thousands of tokens for one line of information.
- **Batch edits, then verify once.** A verify run after every single edit is the biggest avoidable
  cost in a session. Make a coherent group of related changes, then check.
- **Escalate verification, do not start at the top.** `compileJava` for a syntax check
  (seconds, tiny output) → `test` for logic → full `build` only before committing. `runServer`
  only after touching `src/main/resources/data/`, where it is genuinely the only thing that
  catches codec errors.
- **Read line ranges, not whole files.** `CourseStructureService` and `ServerEvents` are 500+
  lines each; `sed -n '120,180p'` after a `grep -n` costs a fraction of a full read. Never re-read
  a file you already have in context, and never re-read one you just edited to confirm the edit —
  the edit tools error if they fail.
- **Search, do not enumerate.** `grep -rn` for a symbol beats reading candidate files. Never run a
  filesystem-wide `find /` — vanilla and NeoForge sources are already cached in `.mcsources`.
- **Do not leave background processes running.** If a command is backgrounded, either read its
  result or stop it. An abandoned process is waste even when it is not costing tokens.
- **Check whether a task is already done before implementing it.** Sessions on this project have
  repeatedly rebuilt things that already existed — the single largest source of wasted spend here.
  `grep` for the feature first and say so in `PROGRESS.md` if it turns out to be complete.
- **Do not add unrequested work.** No extra docs, changelogs, formatting passes or coverage the
  task did not ask for.

## Working alongside another agent (MANDATORY when one is running)

More than one agent can be working on this repo at the same time. When that happens:

- **`docs/AGENT_CHANNEL.md` is the channel.** Read it before starting a chunk; post your claim
  before you start, not after. It names who owns which area and which working directory.
- **Do not share a working directory.** A claims file cannot stop two processes writing the same
  file — that is a race, not a coordination problem. The second agent takes a git worktree
  (`git worktree add ../PlaneShift-<agent> -b <agent>/work`) and merges into `main`. Whoever holds
  `main` should never have to stop for a merge.
- **Never use blind find-and-replace on a file another agent may be editing.** An anchor that
  silently misses leaves a half-applied edit, and a half-applied edit to a record signature or a
  method signature breaks the other agent's build in a file they believe they own. Assert that
  every replacement matched, or read-verify after writing.
- **Claim by area, not by file.** Work always spreads to neighbouring classes.

## Handoff sync (MANDATORY — do this before you finish, every session)

Four agents work on this repo — Claude, ChatGPT/Codex, Gemini and whatever runs from
`NEXT_SESSION_PROMPT.md`. They cannot see each other's sessions. The only thing that stops them
duplicating work or acting on a stale picture is the files below, and those files rot the moment
one session updates its own and leaves the rest describing a mod that no longer exists.

**Before your final commit of a session, update all of these:**

| File | What it must say |
|---|---|
| `PROGRESS.md` | What you did, what is next, a session-history row. **The single source of truth for state.** |
| `docs/MISSING_MECHANICS.md` | Tick off what you finished; add what you discovered is missing. **The single source of truth for the backlog.** |
| `CLAUDE.md`, `CODEX.md`, `GEMINI.md`, `NEXT_SESSION_PROMPT.md` | Only if a *rule* changed. |
| `HANDOFF.md` | Only if the open-issues list changed. |

**Do not restate current state inside the per-tool prompt files.** They point at `PROGRESS.md` and
`docs/MISSING_MECHANICS.md` on purpose. Four copies of "here is what works today" is four things to
forget to update, and the one that gets forgotten is the one the next agent reads. Describing state
in exactly one place is what makes this maintainable rather than a discipline problem.

If you changed something another agent is mid-way through — a shared file, a renamed class, a
disabled mod — say so explicitly in `PROGRESS.md` under "What To Do Next". Assume they will not
read your diff.

## Continuation prompt
When continuing this project in a fresh session, read **PROGRESS.md** first (where the last session stopped), then read the prompt file for your tool:
- **Claude** (Claude.ai, Claude Code, Claude in Antigravity): `CLAUDE.md`
- **ChatGPT/Codex**: `CODEX.md`
- **Gemini** (Gemini app, Gemini CLI, Gemini in Antigravity): `GEMINI.md`, backlog in `docs/GEMINI_BACKLOG.md`
- **Google Antigravity** (Gemini, Claude, GPT-OSS): `AGENTS.md` (this file) — Antigravity auto-reads it
- **Any other agent**: `NEXT_SESSION_PROMPT.md`

## Project
- Minecraft mod built from the NeoForge 1.21.11 MDK (NeoForge 21.11.45, MDG 2.0.144, Java 21).
- Root: `C:\Dev\PlaneShift`.
- GitHub repo: `https://github.com/OkVurb/hand-off`
- MCP decompiled sources are cached in `.mcsources` for quick lookup of vanilla/NeoForge APIs. This directory is generated by the build and is listed in `.gitignore`.

## Build & Verify
- Full build: `.\gradlew build` from `C:\Dev\PlaneShift`.
- Fast compile check: `.\gradlew compileJava`.
- Unit tests: `.\gradlew test` (JUnit 5 via MDG `neoForge.unitTest`, sources in `src/test/java`).
- Client class leak check: `.\gradlew checkClientClassLeak`.
- Raw cuboid scan check: `.\gradlew checkNoRawCuboidScan`.
- Sound asset check: `.\gradlew checkSoundAssets`.
- Data range check: `.\gradlew checkDataRanges`.
- Texture asset check: `.\gradlew checkTextureAssets`.
- Block model check: `.\gradlew checkBlockModels`.
- The build includes `checkClientClassLeak`; client code must stay in `com.studio.planeshift.client`.
- The build includes `checkNoRawCuboidScan`; area searches over blocks must go through
  `com.studio.planeshift.common.block.BlockAreaScan`, not `BlockPos.betweenClosed`. The helper
  skips unloaded chunks and rejects whole chunk sections from the palette instead of doing a
  `Level.getBlockState` lookup per position. If a class legitimately needs raw cuboid iteration,
  add it to `allowedClasses` in the task rather than removing the check.
- The build includes `checkSoundAssets`; every event registered in `ModSounds` must have a
  non-empty `sounds.json` entry, a real OGG under `assets/planeshift/sounds/`, and a subtitle
  key present in `en_us.json`. The OGGs are generated by `tools/SoundGen.java` — see
  `tools/README.md` to regenerate and `ASSET_LICENSES.md` for provenance. SFX must be **mono**
  (Minecraft only positions mono sources); the four music tracks are stereo.
- The build includes `checkDataRanges`; a datapack JSON value outside the numeric range its codec
  declares parses fine but kills the server at world load. The ranges are parsed out of the codec
  source, so widening a contract in Java widens the check automatically.
- The build includes `checkBlockModels`; model and blockstate JSON must parse, every model and
  texture reference must resolve, and a block with multiple variants must not render identically
  in all of them (rotation counts as a difference, so facing variants are fine).
- Blocks hit from below use `HitFromBelowBlock.isHeadContact`, not a bare `player.getY()` compare.
- C2S payload handlers that do real work go through `PayloadRateLimiter`.
- Build checks cannot catch data-pack **schema** errors (the JSON parses; only the codec rejects
  it). After touching `src/main/resources/data/`, run `.\gradlew runServer` — it loads every
  data-pack registry headlessly and is the cheapest real check. A wrong `dimension_type` schema
  crashed world creation while the build stayed green.
- Items need `assets/planeshift/items/<id>.json` as well as `models/item/<id>.json` (1.21.4+),
  or they render as the missing model. `checkBlockModels` enforces this.
- `pack.mcmeta` must use `min_format`/`max_format` with a major above 81. The file is read as
  both a resource pack and a data pack, and the legacy-format cutoff differs (64 vs 81), so the
  data cutoff binds. Pinned to `[94, 1]` to match NeoForge.
- Client movement: `PlaneConstrainedInput` writes only `moveVector`/`keyPresses` and must not
  touch the player entity. Velocity assists belong in `PlaneMovementAssists`, which runs from
  `MovementInputUpdateEvent` (game bus, client only).
- Run client: `.\gradlew runClient`.
- Run server: `.\gradlew runServer`.
- CI: `.github/workflows/build.yml` runs `./gradlew build` on push/PR to main, so every `check*` task and the unit tests run automatically.
- Runtime smoke test: `.gradlew runServer` (headless, loads all registries) and `.gradlew runClient`.

## Generated Artifacts & Jar Locations
After the first build, the following jars and caches are available:

- NeoForge merged / sources / coremod jars: `C:\Dev\PlaneShift\build\moddev\artifacts\`
  - `neoforge-21.11.45.jar`
  - `neoforge-21.11.45-merged.jar`
  - `neoforge-21.11.45-sources.jar`
  - `neoforge-21.11.45-client-extra-aka-minecraft-resources.jar`
- Built mod jar: `C:\Dev\PlaneShift\build\libs\planeshift-0.1.0.jar`
- Decompiled source cache: `C:\Dev\PlaneShift\.mcsources\` (regenerated, not committed)
- Gradle dependency cache: `C:\Users\<user>\.gradle\caches\`
- Test client/server directory: `C:\Dev\PlaneShift\run\`

## Key Architecture
- Common layer: `common/` — registries, mode/rail/course state, Form framework, block/entity/item definitions.
- Server layer: `server/` — `ModeTransitionService`, `CheckpointService`, `FormService`, `CourseStateAccess`.
- Client layer: `client/` — `ClientEvents`, `PlaneShiftKeybinds`, `CourseHud`, `CameraDirector`, `PlaneConstrainedInput`, renderers.
- Networking: `common/network/` — `FormActionPayload`, `ReserveSwapPayload`, etc.

## Data Pack Registries
Loaded from `data/planeshift/planeshift/<registry>/`:
- `form/` — `FormDefinition` records.
- `role/` — `PlayerRole` records.
- `camera_profile/` — `CameraProfile` records.

## Resources
- Language: `assets/planeshift/lang/en_us.json`.
- Block/item model and blockstate JSONs are in `assets/planeshift/models/` and `assets/planeshift/blockstates/`.
- Placeholder PNG textures are generated by `tools/TextureGen.java` into `assets/planeshift/textures/`; real art can replace them in place. `checkTextureAssets` enforces that every registered block/entity/effect/particle has one and that no two are identical.
- Sound events in `assets/planeshift/sounds.json` are all backed by original OGGs in `assets/planeshift/sounds/`, generated by `tools/SoundGen.java`.
- Mob effect icons live in `assets/planeshift/textures/mob_effect/` (18x18, one per `ModEffects` entry).

## Handoff Files
- `README.md` — quick project overview and build instructions.
- `HANDOFF.md` — full continuation handoff with current state, recent changes, known issues, and file inventory.
- `PROGRESS.md` — **READ THIS FIRST** every session. Where the last session stopped. Update before ending.
- `CLAUDE.md` — copy-paste prompt for Claude sessions (Claude.ai, Claude Code, Claude in Antigravity).
- `CODEX.md` — copy-paste prompt for ChatGPT/Codex sessions.
- `GEMINI.md` — copy-paste prompt for Gemini sessions.
- `docs/GEMINI_BACKLOG.md` — the 100-task backlog Gemini works through.
- `NEXT_SESSION_PROMPT.md` — generic copy-paste prompt for any agent.
- `MCP_TOOLS.md` — auto-generated registry of all available MCP tools.

## Notes
- The `plumberplatformer` project at `C:\Users\cr0od\OneDrive\Documents\Minecraft Mod` is a separate mod and should not be mixed with PlaneShift.
