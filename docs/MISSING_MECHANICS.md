# PlaneShift — 100 missing mechanics

Written 2026-09-01, after a play-test pass. This is what the mod does **not** have yet, grouped by
area and ordered roughly by value within each group. Every entry names the class or file it would
touch where one already exists.

**Read `PROGRESS.md` and `AGENTS.md` first.** In particular, check whether an item is already done
before building it — several entries here are "partially there", and the single largest source of
wasted effort on this project has been rebuilding things that already existed.

Conventions used below:
- **[new]** — nothing exists yet
- **[extend]** — a class exists and needs behaviour added
- **[fix]** — exists but is wrong or incomplete

---

## Movement and physics (1–15)

The 2.5D rail, coyote time, jump buffering, ground pound, crouch and the Glider float already
exist in `PlaneMovementAssists` and `AirMoveService`. These do not.

1. **Slippery ice physics** for the SNOW theme. [new] Every biome currently has identical friction,
   so snow courses play exactly like grass ones. Needs a per-theme friction multiplier applied in
   `MovementRuleService`.
2. **Running meter / P-speed.** [new] Hold run for ~2 s to reach a faster tier that enables longer
   jumps. The single biggest contributor to "feels like Mario" that is absent.
3. **Skid turnaround.** [new] Reversing at speed should slide and show a skid, not stop dead.
4. **Momentum preservation on landing.** [fix] Horizontal speed is currently unaffected by landing;
   a well-timed landing should keep speed and a bad one should shed it.
5. **Spin jump.** [new] A second jump variant that bounces safely off spiked enemies — the intended
   counter to Spiny, which currently has no counter at all beyond avoidance.
6. **Long jump** from a crouch-run. [new]
7. **Triple jump** on three consecutive well-timed landings. [new]
8. **Ledge grab and pull-up.** [new] Would remove a lot of the "just missed it" frustration on the
   5-block platforms the generator builds.
9. **Slope blocks and slope sliding.** [new] The generator only produces flat surfaces and
   staircases; real slopes change how a course reads.
10. **Underwater physics** — buoyancy, slower fall, swim stroke on jump. [new] Blocks the whole
    water theme (see 63).
11. **Wall jump re-tune.** [fix] Exists in `AirMoveService` but is off by default because the
    six-tick grace window fires almost anywhere in a dense course and reads as a double jump. It
    needs real wall-contact tracking (consecutive ticks against the same face) before being
    re-enabled.
12. **Quicksand** for the DESERT theme. [new] Sinking unless the player keeps jumping.
13. **Wind zones** that push horizontally. [new]
~~14. **Conveyor influence~~ while airborne.** [fix] `ConveyorBlock.stepOn` only fires on contact, so
    jumping straight up over a belt loses all belt momentum instantly.
15. **Variable gravity per course.** [new] A "low gravity" sky course is one JSON field away given
    the config work already done in `PlaneShiftConfig`.

## Enemies (16–40)

Existing: Goomba, Koopa (with shell states), Spiny, Buzzy Beetle, Boo, Thwomp, Hammer Bro, Lakitu,
Bullet Bill, Piranha Plant, Bowser. All ground walkers now patrol rather than chase.

16. **Koopa Paratroopa.** [new] A winged Koopa that hops; stomping it removes the wings and leaves
    an ordinary Koopa. High value — it reuses `KoopaEntity` almost entirely.
~~17. **Red vs green Koopa.** [extend] Green walks off ledges, red turns at them. One boolean on
    `LanePatrolGoal`, and it doubles the enemy's expressive range.~~ Done: `LanePatrolGoal` takes
    `turnsAtLedge`; `KoopaEntity` has a synced `Red` tag (default `true`, use `Red:0b` NBT for green).
18. **Dry Bones.** [new] Collapses when stomped and reassembles after a few seconds.
19. **Bob-omb.** [new] Walks, then flashes and explodes; can be picked up and thrown.
20. **Cheep-Cheep.** [new] Jumps in arcs out of water or lava.
21. **Blooper.** [new] Jerky diagonal pursuit underwater.
22. **Podoboo / lava bubble.** [new] Leaps from a lava pit on a fixed cycle. The LAVA theme has
    lava pits already and nothing that uses them.
23. **Chain Chomp.** [new] Lunges to the end of a fixed chain; the chain length is the mechanic.
24. **Monty Mole.** [new] Bursts out of the ground as the player passes.
25. **Wiggler.** [new] Walks calmly; stomping enrages it and speeds it up rather than killing it.
26. **Magikoopa.** [new] Teleports and turns blocks into enemies.
27. **Bowser Jr.** [new] A mid-boss, cheaper to fight than Bowser.
28. **Fire Bro / Boomerang Bro / Sledge Bro.** [extend] Variants of `HammerBroEntity` differing in
    projectile — most of the work is already done by the perch and range logic added today.
29. **Fire-spitting Piranha Plant.** [extend] `PiranhaPlantEntity` has the emerge cycle; add a
    projectile at full extension.
30. **Muncher** — an indestructible plant that is purely terrain. [new]
31. **Spike Top** — a Buzzy Beetle that walks on walls and ceilings. [new]
32. **Big Boo** and **Boo circles** (a ring that rotates). [extend] `BooEntity` exists.
33. **Thwimp** — a small hopping Thwomp. [extend]
34. **Banzai Bill** — an oversized Bullet Bill. [extend] `BulletBillEntity` exists.
35. **Bullet Bill launcher block.** [new] Currently Bullet Bills have no source; a cannon block
    would make them part of the level rather than spawned from nowhere.
36. **Rex** — takes two stomps, flattening on the first. [new]
37. **Enemy shell interactions.** [extend] A kicked Koopa shell should defeat other enemies it
    passes through and build a combo. `KoopaEntity` has the shell; it does not hit other enemies.
38. **Enemies knocked off by a shell falling into pits.** [extend]
39. **Stomp chains across different enemy types.** [fix] Verify `CourseScoringService` counts a
    mixed chain; it currently counts defeats, which may not match a shell-kill chain.
40. **Enemy spawn density curve.** [fix] `spawnCast` places one enemy per set piece uniformly;
    later worlds should be denser and mix types.

## Blocks and interactive objects (41–60)

Existing: question, brick (now with coin bricks), hidden question, coin, coin ring, P-switch,
ON/OFF switch and block, note block, music block, donut, conveyor, spring pad, warp pipe,
checkpoint beacon, flagpole, axe, secret vine, spike, prize cache, loop trigger, castle/cloud/
grass/sand/snow/magma terrain blocks.

41. **Multi-coin brick.** [extend] A brick that pays several coins over a short window before
    turning spent. `BrickBlock` now has the SPENT state to build on.
42. **Rotating block.** [new] Spins when hit and is passable while spinning.
43. **Trampoline** distinct from the spring pad — carryable and placeable. [new]
44. **Climbable vines.** [fix] `SecretVineBlock` grows a vine but the player cannot climb it, which
    makes the coin heaven above it unreachable by its intended route.
45. **Ice block** that melts to water under fire. [new]
46. **Moving platform on a rail path** with authored waypoints. [extend] `MovingPlatformEntity`
    only does a straight axis sweep.
47. **See-saw platform** that tilts under the player's weight. [new]
48. **Falling platform** that drops after a delay. [extend] Donut behaviour generalised.
49. **Rope / pulley lift** — two platforms on a shared rope. [new]
50. **Cannon** that fires the player. [new]
51. **Skewer / spike pillar** that slams horizontally. [new]
52. **Crusher** — a vertical press with a timing window. [new]
53. **Water current blocks.** [new]
54. **One-way gate.** [new]
55. **Key and keyhole.** [new] Would give ghost houses an actual objective.
56. **Boss door** that requires the world's key. [new]
57. **Directional conveyor switch** wired to the existing ON/OFF system. [extend]
58. **Coin ring completion bonus.** [fix] `CoinRingBlock` exists; passing through all rings in a
    course should pay a bonus, and currently does not.
59. **Breakable floor tiles** that crumble after being stood on. [new]
60. **Pipe variants** — horizontal, short, and decorative-only. [extend] `WarpPipeBlock` is one
    vertical form.

## Level generation (61–72)

`CourseLayoutPlan` is now data-driven and seeded from the world seed plus course id, so every save
generates a different set. These are the shapes it still cannot produce.

61. **Vertical course sections.** [new] Everything is currently a horizontal corridor.
62. **Underground bonus rooms** reached by pipes. [extend] `buildWarpPipe` places pipes; the
    sub-room path exists but is not wired into generation as a reward.
63. **Water course theme.** [new] Needs 10 first.
64. **Per-segment auto-scroll.** [extend] Auto-scroll is a whole-course flag; classic design turns
    it on for one section.
65. **Secret exits** — a second flagpole that unlocks an alternate world path. [new]
66. **Warp zones** that skip worlds. [new]
67. **Star coin placement rules.** [fix] `CourseProgress` tracks three star coins per course, but
    generation never places them — the counter can only ever read zero.
68. **Difficulty curve across worlds.** [fix] World 5 generates with the same parameters as world 1.
    The seed varies the layout but not the challenge.
69. **Themed hazards per biome.** [fix] Only LAVA has a distinctive hazard; snow, desert and ghost
    house differ by palette only.
70. **Set-piece templates** — hand-authored NBT chunks dropped into generated courses. The template
    path exists in `CourseStructureService.placeTemplate` and nothing uses it.
71. **Guaranteed completability proof.** [new] Backlog item 38, never done: assert every generated
    course is clearable, ideally as a unit test over many seeds now that seeding exists.
~~72. **Course length~~ variety.** [fix] Almost every course JSON is 144–224; nothing short and intense.

## Power-ups and Forms (73–82)

Existing: Super, Mega, Mini, Fire, Ice, Leaf, Propeller, Cloud, Tanooki, Hammer, Boomerang, Acorn,
Star, Poison, 1-Up/3-Up/5-Up, Extra Pip.

~~73. **Reserve item~~ box.** [fix] `FormSlot` has an active and a reserve and a swap key, but nothing
    ever fills the reserve automatically the way picking up a power-up while already powered should.
74. **Cape feather** — glide plus dive-and-climb. [new]
75. **Yoshi.** [new] Large, but the highest-impact single addition on this list.
76. **Double cherry** — clones the player. [new]
77. **Super bell** — wall climbing. [new]
78. **Frog / Penguin suit** for water courses. [new]
79. **Gold flower** — turns blocks to coins. [new]
80. **P-balloon.** [new]
81. **Power-up carry between courses.** [fix] Forms reset on course load; classic behaviour keeps
    them.
82. **Form-specific interactions with blocks.** [new] Fire melting ice, Mega smashing anything,
    Mini fitting through one-block gaps — Mini and Mega exist but change nothing about what the
    player can traverse.

## Course rules, scoring and meta (83–90)

83. **100 coins = 1-Up.** [fix] Mentioned in a comment in `ServerEvents` as handled "by the pickup
    itself" — verify it actually fires, because nothing obviously implements it.
84. **Speedrun timer and best-time display.** [extend] `CourseProgress` stores `bestTimeLeft`;
    nothing shows it outside the results screen.
85. **Continue system after game over.** [extend] Game over now returns to the map with a retry;
    classic design offers a limited number of continues.
86. **World map branching paths.** [fix] `WorldRegistry` is a linear list of ten per world.
87. **Toad houses** on the map for power-ups. [new]
88. **Achievements / challenge medals.** [new]
89. **Per-course leaderboards.** [new]
90. **Course ratings and sharing.** [new] Depends on 91.

## Presentation (91–96)

91. **Parallax backgrounds per theme.** [fix] `CourseSkyboxRenderer` draws one shared skybox; the
    per-theme skyboxes have been on the list since the first session.
92. **Camera zones** — authored camera distance and height per course segment. [extend]
    `CameraProfile` exists as data and only two profiles are defined.
93. **Screen shake** on heavy landings and Thwomp impacts, respecting `reducedMotion`. [new]
~~94. **Death animation~~.** [fix] Death is instant; the classic pop-up-then-fall is most of its charm.
95. **Course intro card** — "World 1-1" before play. [new]
~~96. **Flagpole slide~~ and fanfare sequence.** [fix] `FlagPoleBlock` completes the course instantly;
    the slide down the pole is the reward moment.

## Technical and multiplayer (97–100)

97. **Co-op verification.** [fix] `CourseCoopService` exists and has never been tested with two
    players. Backlog item 80.
98. **Mid-course disconnect and rejoin.** [fix] `CourseProgress.currentCourse` now survives a
    relog, so the pieces exist; the rejoin path is not implemented.
99. **Item textures.** [fix] All 37 item textures are still generated placeholders under 400 bytes.
    This is the most visible unfinished thing in the mod.
100. **GameTests for the new systems** — coin bricks, air-drop immunity, the perch clamp, layout
     seeding. Air-drop, coin bricks and Hammer Bro perch are now covered (`testAirDrop`,
     `testCoinBrick`, `testHammerBroPerch`); layout seeding is covered by JUnit in
     `CourseLayoutPlanTest`.

---

## Working with the mods already in the instance

The playtest instance ships a working 1.21.11 mod set (see `docs/PLAYTEST_INSTANCE.md`). Several
items above are already provided by one of them, and building a second copy would be worse than
useless — two systems fighting over the same input or channel reads as a bug in both. Check this
table before starting anything in the movement, audio or particle groups.

### Already provided — integrate, do not rebuild

| Installed mod | Covers | What PlaneShift must do |
|---|---|---|
| **Omni** (movement overhaul) | 2 sprint tiers, 5 spin/dodge, 8 ledge grab, 11 wall jump | Nothing new. `MovementRuleService` now folds cross-rail momentum onto the travel axis, so its dashes and vaults already work in 2.5D. Keep PlaneShift's own wall jump off (`wallJump=false`) so only one system owns the mechanic. |
| **Enhanced Movement** | 8 ledge grab / vault, climbing | Same. Verify a vault onto a 5-block platform does not bypass an intended route — if it trivialises courses, that is a level-design response, not a code one. |
| **Boss Music Mod** | 44 boss music | PlaneShift's boss theme is already off by default (`bossMusic=false`). Leave it off. |
| **Music Notification / Melody / Pause Music On Pause** | audio plumbing | `CourseMusicManager` owns the MUSIC channel while in a course. If tracks double up, PlaneShift yields — it is the newer system. |
| **Particle Effects / Smart Particles** | 93-adjacent visual polish | Complements `PickupParticles`; both can run. Smart Particles culls, which helps the lava theme's ambient emitters. |
| **Cloth Config + Configured** | config UI | `PlaneShiftConfig` gets an in-game settings screen for free. Worth verifying every value added today (jump, run, conveyor, HUD scale, wall jump, boss music, tester menu) renders sensibly there. |
| **AttributeFix** | attribute ceilings | Relevant to item 15 and to `courseJumpBoost`: it raises vanilla's attribute caps, so large boosts stop being silently clamped. Do not remove it while tuning movement. |
| **Entity Texture Features** | 99 textures | Can retexture entities from a resource pack without touching code — a cheap path to better enemy art. Note **Entity Model Features is disabled**: it NPEs on the first-person hand render. |
| **Better Advancements** | 88 achievements | Build achievements as vanilla advancements and this displays them properly; no custom UI needed. |
| **Crash Assistant / CrashGuard** | resilience | Keep installed while play-testing. CrashGuard can swallow a render crash that would otherwise be a useful signal, so disable it when specifically hunting one. |

### Known conflicts to design around

- **SereneSeasons vs one-biome-per-world.** Seasons change biome colour and behaviour over time,
  which fights the guarantee that each world reads as a single consistent biome. Either exclude the
  `planeshift:course` dimension from season effects in the SereneSeasons config, or accept that
  course tint drifts across a long session. This is worth settling before doing item 69.
- **Omni / Enhanced Movement vs authored gaps.** Both can cross gaps the generator sized for a
  plain jump. Item 71 (completability proof) should be checked with them *disabled*, so the
  baseline course is provably fair on its own.
- **Any 1.21.1 mod.** Not a design conflict, a hard one. Five were blocking launch outright and
  two more (FerriteCore, ModernFix) crashed during mixin setup. All are disabled; do not re-enable
  without a 1.21.11 build.

### Integration work worth doing

101. **Detect the movement mods explicitly.** `ModCompatibility` already exists and checks for
     conflicting mob mods. Add `omni` and `enhancedmovement` to it, and use that to skip
     PlaneShift's own overlapping assists rather than relying on a config default nobody changes.
102. **Season suppression in the course dimension**, per the conflict above.
103. **Verify the Cloth Config screen** renders every PlaneShift value with a sensible range.

## Notes for whoever picks this up

- **Do not trust filenames for mod versions.** See `docs/PLAYTEST_INSTANCE.md`; half a day went
  into a launch failure caused by 1.21.1 mods in a 1.21.11 instance.
- **A green build does not mean it works.** Run `.\gradlew runServer` after touching
  `src/main/resources/data/`, and actually launch the game after touching rendering.
- **Check before building.** Items marked [fix] and [extend] have real code behind them already.

## Completed this session

- **Entity-renderer build check** � every ModEntities entry now has a checkEntityRenderers Gradle check that fails the build if a registered entity lacks a client renderer. Done in uild.gradle and verified by deliberate failure/repair.
