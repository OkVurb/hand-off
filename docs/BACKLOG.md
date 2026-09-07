# PlaneShift backlog

Fifty items, grouped by what kind of work they are. Each says *why* as well as *what*, because the
recurring failure in this codebase is not bad code — it is finished work nobody can reach, and a
task list that only says "add X" produces more of exactly that.

**Ordering note.** Group A blocks on a decision only the project owner can make. Group B needs a
person holding a controller and cannot be done by an agent at all. Everything from C onward is
ordinary work.

---

## A. Decisions needed before the work can start

1. **`WorldDefinition.hasUnderwaterStage` — build it or delete it.** Declares that ~5% of courses
   have an underwater second stage. Zero callers. There is no water handling anywhere in the mod:
   no `isInWater`, no swim logic in the movement services, no water enemies. It is not half-built,
   it is a declared feature with no implementation. Deleting is honest; building is a real feature
   touching `CourseMovementService`, `PlaneConstrainedInput` and `AirMoveService`.
2. **`CameraProfile.lookAhead` — implement or delete.** Authored per profile, documented as
   "horizontal look-ahead toward velocity, in blocks", default 3.0, and read by nothing. The
   config slider that claimed to scale it has already been removed. Implementing means moving the
   camera position, which `ViewportEvent.ComputeCameraAngles` does not expose — needs a different
   hook.
3. **Sapixcraft resource pack — enable or remove.** Installed and deliberately left switched off:
   it restyles every vanilla block, and the course themes were colour-matched against vanilla. A
   taste call, not a technical one.
4. **Boss identity — is every world's boss Bowser?** Five castles currently end with the same
   fight, differing only in furniture. Deciding this shapes tasks 24–27.

## B. Needs a play session (an agent cannot do these)

Nothing below has ever been played. The reachability solver proves courses are traversable and the
tests prove wiring and shape; neither can tell you whether anything is *good*.

5. **Play a boss course end to end.** Does taking the axe read as defeating Bowser, or as a
   disconnected event? Is the walk from the collapsed bridge to the flagpole a victory lap or an
   anticlimax?
6. **`frozen_gauntlet` Thwomp spacing.** Five blocks apart on ice is a guess about slide distance.
   The most likely thing in the game to be unfair.
7. **Toad House box height.** Boxes sit three clear blocks overhead; tests assert the number, not
   that this mod's actual jump arc reaches it.
8. **Koopa shell visuals.** Stomp one: shell sits right, no floating eyes, no missing parts. Then
   kick it. Renderer changes cannot be unit tested.
9. **FancyMenu layout renders.** Does the background appear, and is the vanilla Minecraft logo
   actually hidden? The layout schema was reconstructed from the mod jar, not verified.
10. **LambDynamicLights performance near lava.** Podoboos plus firebars plus a lava pit is the
    worst case; its update rate is the first config to turn down.
11. **Ground pound onto a Toad box and a prize cache.** Newly dispatched. Either it feels obvious
    or it feels like nothing happened.
12. **The flagpole 1-Up.** Run up the finish staircase and jump. The top band is reachable in
    theory; the three-column gap is tuned by eye.
13. **Ghost loop escape.** Walk right until the trigger fires, then confirm the stairs at the
    return end actually let you leave.
14. **Course difficulty curve across a whole world.** Ten courses in sequence is the only way to
    tell whether the lesson/breather pacing works at world scale.

## C. Built but nothing uses it — the recurring pattern

15. **No course uses auto-scroll.** `auto_scroll` is fully implemented — `PlaneConstrainedInput`
    blocks backward travel — and zero of 56 course definitions set it. An entire classic level type
    exists in code and has never been played.
16. **No course has a time limit.** Same shape: `timeLimitTicks`, the hurry-up music trigger and
    the HUD clock all work, and every course is untimed. Consider timed castles first, since a
    boss course with a clock is the traditional use.
17. **Audit `CourseLayout.features`.** `course_1.json` lists `donut_bridge`, `note_block_run`,
    `secret_vine`, `coin_heaven`, `staircase`, `moving_platforms`. Confirm the composer reads that
    list at all — the generated path may ignore it entirely.
18. **Dead code in `CourseStructureService`.** `clearCorridor` and `clearGeneratedEntities` are
    declared and never called; `CourseWriter.write` does its own clearing. Delete or explain.
19. **Advancements: there are none.** No `data/*/advancement` directory exists. A platformer with
    star coins, secret exits and 1-Ups has an obvious achievement surface and uses none of it.
20. **Recipes: there are none.** Probably correct for a course-based mod, but it should be a
    recorded decision rather than an absence.

## D. Enemies

21. **Cheep Cheep.** Blocked on task 1 — pointless without water.
22. **Wiggler.** Walks placidly, turns angry and fast when stomped. A stomp that makes an enemy
    *worse* is a lesson the current roster never teaches.
23. **Chain Chomp.** A hazard anchored to a post, defining an exclusion zone rather than patrolling.
    Nothing in the roster currently owns territory.
24. **Monty Mole.** Emerges from the ground. The only roster gap that would make floors
    untrustworthy.
25. **Lakitu should throw Spinies.** It exists and appears in desert rosters; check whether it
    actually spawns anything or just hovers.
26. **Boo movement.** Confirm Boos advance when the player looks away — that is the entire
    character, and the camera is locked side-on, which complicates "looking away".
27. **Dry Bones reassembly tuning.** `COLLAPSE_DURATION = 100` with non-linear reassembly; never
    watched in play.
28. **Bob-omb chain reactions.** One blast should set off neighbours. `BLAST_RADIUS = 3.5` makes
    this likely already possible — verify, then decide if it is wanted.

## E. Bosses

29. **Boss health feedback.** Bowser has 60 HP and takes 20 per ground pound. Three hits with no
    HUD means the player cannot tell progress from stalemate.
30. **A second boss type.** Depends on task 4. Even one alternative stops the fifth castle being
    the first castle again.
31. **Boss arena intro.** A moment where the camera holds on the boss before the fight starts.
    `ModeTransitionService` already exists to lock control.
32. **Bowser fire patterns.** `BowserGoal` drives him now; check whether the fire attack has any
    pattern or is uniform random.

## F. World generation

33. **An underground-specific set piece.** `CASTLE_BRIDGE` is shared between lava and underground.
    Underground is the only theme without a climax of its own.
34. **Vertical courses.** Every course is a horizontal ribbon. A climb changes what the camera and
    the reachability solver have to do — real work, high payoff.
35. **Bonus rooms.** The fake warp pipes are deliberately non-functional; a few that genuinely lead
    somewhere would make the fakes read as a joke rather than an oversight.
36. **More secret exits.** `KeyholeBlock` is ghost-house only. A secret exit in another theme would
    make the world map branch.
37. **Show the course seed.** Courses are seeded and reproducible; nothing surfaces the seed, so a
    good one cannot be shared or replayed deliberately.
38. **Multi-room courses.** Pipes that move the player between two generated sections.
39. **`CourseRoutes` beyond two decks.** Capped at two per course by design; revisit once vertical
    courses exist.
40. **Weather in courses.** `SereneSeasons` is installed and the firebar/Podoboo light definitions
    already handle rain. Nothing else acknowledges weather.

## G. Systems and UI

41. **Star coin collection UI.** 20 references to `starCoins` in code; check the player can see
    which of a course's three they have.
42. **Speedrun timer.** Cheap given the course clock already exists, and it is the feature this
    genre's players ask for first.
43. **Lives and game over.** `THREE_UP`, `FIVE_UP` and `ONE_UP` all exist. Confirm running out of
    lives does something meaningful.
44. **P-meter visibility.** `PMeter` is a full server-side system with an 8-step ladder. Confirm the
    HUD shows it, or it is invisible mechanics.
45. **Checkpoint mid-course.** One checkpoint per course, sited at a segment boundary. Long courses
    may want two.
46. **Pause menu customisation.** FancyMenu is now configured for the title screen only.

## H. Polish and hygiene

47. **Sound coverage for new blocks.** `ToadBoxBlock` reuses `POWER_UP`; the semisolid platform,
    keyhole and spike block should be checked for having any sound at all.
48. **Colourblind-safe course themes.** Themes are distinguished largely by hue. The lava and grass
    palettes are the likely problem pair.
49. **ParCool ships an unresolved Gradle placeholder** (`${minecraft_version_range}`) in its
    metadata. Harmless today, but it defeats the version-sweep check that caught the crash-causing
    Sound Physics build — worth pinning the version deliberately.
50. **Profile course generation time.** `CourseComposer.compose` runs on the server thread when a
    player loads a course. 480-block courses with the reachability solver are fine in tests; nobody
    has measured the in-game hitch.

---

## How to pick one

Prefer anything in group C. That is where the pattern lives: this project's characteristic bug is
not broken code, it is correct code that no player can reach, and it never fails a build or a test.
Every item there was found by asking "what is registered, and what actually appears in a game?" —
the same question is worth asking again of anything not yet audited.

## Water theme — what is still missing (from plan item 6.1)

The theme exists and two courses use it, but three things are placeholders and one is a decision
only the owner can make.

- ~~**No water cast.**~~ Done. `CheepCheepEntity` swims the lane at a fixed depth and turns at the
  ends. Spiny stays alongside it as the seafloor half of the cast. `BigCheepEntity` adds the large slow
  variant, so plan item 6.4 is closed.
- ~~**Nothing is actually submerged.**~~ Done. Flooded in `CourseComposer` after all geometry is
  placed, so the fluid fills what is left rather than displacing anything.
- ~~**No swimming.**~~ **Wrong when written.** `ModFluids.WATER_TYPE` already declared
  `canSwim(true)`, so the player could always swim in it -- what was missing was any water to swim
  in. Now fixed: water courses are flooded between the spawn apron and the flagpole run, and
  `CourseReachability` understands swimming. Left visible rather than deleted because the mistake
  is instructive: the backlog entry was written from what the courses looked like rather than from
  reading the fluid, and it sent the next iteration hunting for a movement feature that already
  existed.
- **Decision needed: where does water live?** Two grass/snow courses were rethemed
  (`w5_grassland_5`, `w15_frozen_5`) because the reference puts underwater levels *inside* existing
  worlds rather than giving them their own. The alternative is a sixth world, which changes
  progression and is not a call to make unattended.

## Duplicate palette on vanilla blocks

`CourseStructureService` carries its own private `Palette` record, separate from
`GenContext.Palette`, and it is still built from vanilla `Blocks.DIRT`, `Blocks.SANDSTONE` and
`Blocks.ORANGE_TERRACOTTA`. The migration to native course blocks appears to have covered
`GenContext` and missed this one. Worth checking which of the two actually reaches a player before
deleting either.

## Title cards (plan 7.5) — blocked on a sync field

The client cannot name the course it is in. `CourseState` carries the theme but not the course id,
and `CourseHud` already tracks a course-start tick, so the card itself is a small piece of work
sitting behind a networking change.

Adding the id means extending a hand-written `STREAM_CODEC` -- two parallel lists of fields whose
agreement nothing enforces. That is now covered by `CourseStateCodecTest`, so the change is safe to
make; it was not when the iteration started, which is why the test came first and the field did not.

Left for a waking decision because it is a design question as much as a plumbing one: the card
wants a display name, and course display names currently live in the lang file keyed by course id,
which the client would then look up. Syncing the id and translating client-side is the cheap route;
syncing a `Component` is the flexible one. Worth ten seconds of thought from someone who knows which
way the rest of the UI is going.
