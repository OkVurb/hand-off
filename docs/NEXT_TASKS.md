# Next tasks

A ready-to-take list for whichever agent picks this up. Written 2026-09-03, after a long session
covering course generation, the world map, mod integration and art.

**Before you start:** read `PROGRESS.md` (state), `docs/AGENT_CHANNEL.md` (who else is working),
`docs/CODE_REVIEWS.md` (mistakes already made — R6 in particular), and `AGENTS.md` (rules, token
budget, handoff duties). Post what you are taking in the channel **before** you start.

Tasks are grouped so two agents can work at once without colliding. **Take a whole group.** Groups
were chosen so that the files inside one barely overlap the files in another.

---

## Group A — Play-testing and feel *(no coding required to start)*

Nobody has played this end to end. It is still the highest-value work available and it needs a
person more than a compiler.

1. Play courses 1-1 through 1-10 start to finish. Write every problem into `PROGRESS.md` **before**
   fixing anything — the list is worth more than any individual fix.
2. Tune `courseJumpBoost` and `courseRunBoost` live in `run/config/planeshift-server.toml`. Current
   values (1.3 / 0.9) were derived from platform heights, not from playing.
3. Check the reachability model matches reality. `CourseReachability.MAX_RISE` is 4 against a real
   ~6; if courses feel cramped or trivially easy, that gap is the dial.
4. Verify the 2.5D rail with ParCool installed: wall runs, cat leaps and vaults should work along
   the lane and never push you off it.
5. Confirm charge jump and hide-in-block are refused **inside** 2.5D courses and allowed in the hub
   and 3D courses. That is `ParCoolCompat`, and it is entirely reflective, so it fails silently by
   design — check the log line at startup to confirm it hooked.

## Group B — The four-step level structure *(one file, high value)*

`CourseComposer` implements three of Nintendo's four steps: introduce, develop, rest. It does not
implement **conclude**.

6. Give each course a "lesson": pick one `Segment.Tag` at composition start, guarantee an
   introduction segment for it early, then prefer that tag for the twist and the finale. This is a
   change to selection weights in `pick()`, not to any segment, and it is the single biggest
   remaining gap between our generator and a hand-made Mario level. See `docs/THREE_D_AND_ANIMATION.md`.
7. Add a `buildWide` variant to segments that would benefit, so a 3D course uses its depth rather
   than being a wide 2.5D course. Start with `SINGLE_GAP`, `ENEMY_LINE` and `PLATFORM_LADDER`.
8. Extend `CourseReachability` to 3D by including z in the flood-fill. The canvas already stores z;
   only `laneZ` is pinned. 3D courses then get the same walkability proof for free.
9. Difficulty currently scales pit count and set-piece density. Add *combination* — two mechanics
   overlapping in one segment — as the late-world difficulty lever instead of more of the same.

## Group C — Enemies *(self-contained, no overlap with B)*

`SegmentLibrary` places these; the entities live in `common/entity`.

10. **Koopa Paratroopa** — a winged Koopa that hops; stomping removes the wings and leaves an
    ordinary Koopa. Reuses `KoopaEntity` almost entirely, so it is the cheapest new enemy by far.
11. **Dry Bones** — collapses when stomped, reassembles after a few seconds.
12. **Bob-omb** — walks, flashes, explodes; can be picked up and thrown.
13. **Podoboo** — leaps from a lava pit on a fixed cycle. The LAVA theme has pits already and
    nothing that uses them.
14. **Shell interactions** — a kicked Koopa shell should defeat other enemies it passes through and
    build a combo. `KoopaEntity` has the shell; it does not hit anything but the player.
15. **Fire Bro / Boomerang Bro** — variants of `HammerBroEntity` differing only in projectile. The
    perch clamp and range gating are already done.

## Group D — Blocks and objects

16. **Multi-coin brick** — pays several coins over a short window before turning spent.
    `BrickBlock.SPENT` already exists to build on.
17. **Rotating block** — spins when hit, passable while spinning.
18. **Key and keyhole** — would give ghost houses an actual objective.
19. **Cannon block** that fires the player, and a **Bullet Bill launcher**. Bullet Bills currently
    have no source in the world.
20. **Crusher** and **skewer** — timing hazards, which the block set has none of.
21. **Ice physics** for the SNOW theme: a per-theme friction multiplier in `MovementRuleService`.
    Every biome currently plays identically, which is most of why themes feel cosmetic.

## Group E — Presentation

22. **Camera zones** — authored camera distance and height per course segment. `CameraProfile`
    exists as data and only two profiles are defined.
23. **Screen shake** on heavy landings and Thwomp impacts, respecting `reducedMotion`.
24. **Course intro card** — "World 1-1" before play starts.
25. **Enemy art pass.** `BespokeEnemyModel` covers the hostile cast; the Toad shopkeeper and the
    projectiles could use a close-range silhouette review.
26. **Item models.** Blocks now have shaped models (`tools/BlockModelGen.py`); the *items* are
    still flat 16×16 sprites. The power-ups in particular would carry a lot from a small amount
    of depth. Same generator shape as the block one.
27. **The remaining `cube_all` blocks.** `course_lamp`, `shift_gate`, `secret_passage`,
    `prize_cache` and the theme blocks are still painted cubes. For the theme blocks that is
    correct — they are walls. For the other four it is not; they are objects.
28. **Animate the rotating block.** `RotatingBlock` swaps to a smeared texture while spinning.
    A real spin would be a model with a `y` rotation driven per-frame, which needs a block entity
    renderer.

## Group F — Systems and correctness

29. **Co-op.** `CourseCoopService` exists and has never been tested with two players. Either verify
    it or document precisely how it fails.
30. **Mid-course disconnect and rejoin.** `CourseProgress.currentCourse` survives a relog, so the
    pieces exist; the rejoin path does not.
31. **A GameTest per subsystem.** See review R6: a bug that made the mod unplayable passed 182 unit
    tests because ServiceLoader behaves differently under FML's classloader. Anything touching
    reflection, services, resources or the module system needs at least one test that runs in game.
32. **Fix the skybox renderer properly.** `CourseSkyboxRenderer` maps the whole texture onto all six
    cube faces, so it is not a cubemap at all. Real per-face UVs would let the skyboxes have a
    proper sky above and ground below instead of the same image six times.
33. **Shaped models are only half the job.** Every block whose model is not a full cube also needs
    `noOcclusion()` in `ModBlocks`, or the renderer culls the faces of whatever is behind it and
    a hole appears. Seven blocks were missing it; if you add a shaped model, check this. See R9.

---

## Things that are done — do not redo

- Course generation is a segment grammar (`server/gen`), seeded from world seed + course id, with
  every generated course **proven walkable** by `CourseReachability`. 3,000 courses verified.
- Star coins are placed (three per course, on the hardest segments).
- The world map is a real map: winding path, Toad Houses, cannon, castle, star-coin pips.
- 3D courses exist (`space_1`–`space_3`) and are the same generator at a wider ribbon.
- Cat Suit: claw swipe, pounce dive, wall cling. Climbing and all-fours animation are ParCool and
  Player Animation Library's job on purpose — do not reimplement them.
- Item textures, coin block, cloud block, moving platform and all six skyboxes are regenerated.
- Block textures are regenerated from `tools/BlockTextureGen.py` — 44 files, drawn from shape
  functions, no placeholders left.
- Nineteen blocks have real shaped models from `tools/BlockModelGen.py`, matched to their
  `VoxelShape`s: trampoline, spring pad, muncher, blaster, checkpoint beacon, coin ring, axe,
  warp pipe, pillar, lattice, banner, both switches, spikes and the flag pole pennant.
- Mod configs are set: ParCool per-course via `ParCoolCompat`, Enhanced Movement dash off,
  Particle Effects borrowed by meaning, Serene Seasons on a second dimension.
- Head bumps, coin bricks, air-drop immunity, Hammer Bro perch, patrolling ground enemies.

## Known open problems

- The mod has **never been played start to finish.** Group A.
- `server/entity/BowserEntity.java` is dead code duplicating `common/entity/BowserEntity`. Left in
  place deliberately rather than deleted unasked, but it is a trap.
- Six skyboxes are now 43 KB total, but the renderer limitation in task 30 remains.
- `CourseLayoutPlan` still exists alongside `server/gen` and is only used for the seed and
  difficulty helpers. Worth collapsing into the new package once nothing else reads it.
