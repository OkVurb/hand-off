# PlaneShift — handoff prompt

Paste the block below into a fresh session. It is written to be self-contained: everything it
asks you to read is in this repository.

---

You are continuing **PlaneShift**, a NeoForge 1.21.11 Minecraft mod that turns Minecraft into a
side-on 2.5D platformer in the style of *New Super Mario Bros. 2*.

- Repository: `https://github.com/OkVurb/hand-off`
- Local root: `C:\Dev\PlaneShift`
- NeoForge 21.11.45, MDG 2.0.144, Java 21, Gradle 8.14.3
- Playtest instance: `C:\Users\cr0od\curseforge\minecraft\Instances\PlaneShift Playtest` (52 mods)

## Read these first, in this order

1. **`docs/WORK_PLAN.md`** — the spine of the project. Derived from a full frame-by-frame pass over
   four hours of reference footage plus the wiki. Eight sections; §8 is the ordering and tells you
   what to do next and why. Sections 1–3 are complete; 4–7 are partly done and each entry says which.
2. **`docs/NIGHT_LOG.md`** — an honest per-change log, failures included. Read the last ~15 entries.
   It is the fastest way to learn how this codebase wants to be worked on.
3. **`docs/AUTONOMOUS_BRIEF.md`** — the standing rules for unattended work.
4. **`docs/BACKLOG.md`** — things deliberately deferred, with reasons.
5. **`docs/DEV_MODS.md`** — the playtest pack and what in it conflicts with this mod.
6. `AGENTS.md` and `CLAUDE.md` for repo conventions.

## How to work here

**Build and test.**
```
./gradlew test --rerun-tasks
```
Then count results, because an up-to-date build prints BUILD SUCCESSFUL having run nothing:
```
grep -ho 'tests="[0-9]*" skipped="[0-9]*" failures="[0-9]*" errors="[0-9]*"' \
  build/test-results/test/*.xml | awk -F'"' '{t+=$2;f+=$6;e+=$8} END{print t,f,e}'
```
Baseline as of this handoff: **373 unit tests, 0 failures; 10 gametests, 0 failures.**

`./gradlew runGameTestServer` is the world-behaviour oracle — it answers physics questions the unit
suite cannot, and it does **not** load client models, so it proves nothing about assets.
`./gradlew runClient` is the asset oracle.

**The rules that actually matter.**

- **A test that cannot fail is worse than no test.** Before trusting a new test, break the thing it
  covers and watch it fail. This has caught three worthless tests here: one compared two literals,
  one searched too wide a band and passed with the feature removed, one asserted a constant.
- **Check whether it already exists before you build it.** This project's signature bug is finished,
  correct, tested work that no player can reach — and its mirror, a second copy of something already
  built. A duplicate signpost block shipped that way. `grep` the registries first.
- **Every telegraph is a physical object, never an overlay.** A saw's route is visible because the
  rail is really there. Particle rings and drawn lines were built once and deleted.
- **`CourseReachability` is the proof that courses are completable.** It searches the x/y plane at
  z=0, so *any* block in the lane centre is a wall to it regardless of collision. A block that is
  `noCollision` in the world must also be in its `PASSABLE` set, or it becomes unusable in the one
  place it is worth having. Never move a threshold to make the proof pass — fix the placement.
- **Register, place and test in the same change.** A registered entity nothing spawns is the bug
  above wearing a different hat.
- **Size lives in the registered hitbox.** `EnemyRigProfile`'s scale is tied to it, so a bigger
  variant needs its own entity type, not a synced flag.
- Commit early and often, one idea per commit, with the reasoning in the message. Append an honest
  entry to `docs/NIGHT_LOG.md` — including what failed.

**Scope rules:** stay inside the repository, spend no money, publish nothing, rewrite no history.
When blocked, record it in `docs/BACKLOG.md` and move to the next item.

## Where things stand

Sections 1, 2 and 3 of the work plan are complete. The most recent work: lava seas that emit,
geysers and wall nozzles, mixed-size masonry, a sunken sub-environment, ground cover on the
playfield, boss self-stuns and a background boss that reaches into the lane.

`ParCoolBridge` integrates the playtest pack's ParCool: it clears stamina inside courses and stands
PlaneShift's own wall jump down when ParCool is present. It is looked up by registry id rather than
compiled against, so the mod runs identically without it.

## What to do next

Follow §8 of `docs/WORK_PLAN.md`. In short: **5.3 hue families**, then **5.7 hill patterning**, then
**4.8 the rest of the parts kit**, then **7.9 the shop as architecture**. Each entry in the plan says
what is built, what is missing, and why the missing part matters.

## One thing nobody has done

**None of this has been played.** Every item is derived from footage of another game and from static
analysis of this one. The reference cannot say whether *our* jump arc makes a given gap fair or
whether the boss fights are fun. If you can run the client and play a course, that is worth more than
the next three features.
