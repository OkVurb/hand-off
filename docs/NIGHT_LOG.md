# Night log

Unattended iterations, newest last. Honest about failures; a quiet iteration is a real entry.

## Iteration 1

Plan item 5, per-world interior tinting. UNDERGROUND was one grey cave shared by every world,
which collapsed six distinct interiors into one and made the most common transition in the game --
surface to cave and back -- read as leaving the world rather than going under it. Caves are now cut
from the rock of the world around them: sandstone over sand in the desert, ice in the snow world,
basalt under the volcano.

GenContext carries a worldTheme alongside its theme, defaulting to the theme itself so an untinted
course is exactly the old behaviour. CourseStructureService resolves it through
WorldRegistry.worldForCourse, which already existed -- I wrote a hand-rolled stream version first
and replaced it once I found the real one.

Also fixed in passing: the LAVA palette's hazard was still vanilla Blocks.LAVA despite the custom
fluid existing. A course pit was filling with Minecraft lava, not ours, so none of the fog or tint
work applied to the place it was most meant for.

282 tests, no failures, verified by counting result XML rather than trusting the build line.

## Iteration 2

Finished wiring item 4. MovingPlatformEntity now marks out the two ends of its travel with a
sparse line at its own height. A platform is only a fair jump if the player can see where it goes
while it is still at the near end; without that the choice is between waiting a full cycle to learn
the range and guessing.

Only the endpoints and the run between them are drawn, at the platform's own height, so the line
reads as this platform's path rather than as a floor near it.

Firebar and moving platform are now both telegraphed. Telegraph.arc is still unused -- it is
waiting on the pendulum platforms, which do not exist yet.

282 tests, no failures, counted from XML.

## Iteration 3

Plan item 6, the water theme. Theme, palette, lesson rules, cast slot, a coral block and a reef
set piece. Two courses rethemed to use it (w5_grassland_5, w15_frozen_5) so it is reachable rather
than registered-and-orphaned.

Two tests caught real mistakes, which is the useful part of this iteration. SetPieceCoverageTest
failed first: water had no climax, exactly the "four themes reserve a slot and never fill it" bug
that test was written for. Then CourseGenerationTest rejected the reef arch -- I had built solid
coral columns with the way through above head height, which is how a reef reads once you can swim,
and swimming does not exist. A walking player met a six-block wall. Rebuilt as an overhead arch
with the floor left open: same silhouette, no promise the movement code cannot keep.

Found and recorded, not fixed: CourseStructureService has its own duplicate Palette still built
from vanilla Blocks.DIRT and Blocks.SANDSTONE, missed by the native-block migration.

Water is not finished and BACKLOG says so plainly -- no fish, nothing actually submerged, no
swimming. What exists is a dry course with a marine palette.

291 tests, no failures, counted from XML.

## Iteration 4

Plan item 7, sub-environments. A course now runs surface, drops into an interior for roughly the
middle third, and comes back out before the flag. Four separate worlds in the footage do this,
which is what made it a pattern rather than an observation, and until now a course was one theme
from spawn to flagpole -- so the most common transition in the genre simply did not exist here.

The cave is UNDERGROUND tinted by the world around it, which is exactly what iteration 1 built and
had nothing to call it. The two pieces were designed a day apart and fit without adjustment, which
is the first time that has happened on this project rather than the reverse.

Decoration had to be split into spans. A single pass over the whole course would have painted a
skyline behind the underground stretch, which is the precise bug the split exists to avoid.

Added SubEnvironmentTest rather than trusting the compile. The composer can be told to swap
contexts halfway through and still emit a course identical to the old one, and nothing in the
existing suite would notice, because a course of pure surface segments walks from spawn to flag
perfectly well. The test looks for deepstone -- fill only the underground palette places -- and
asserts both bounds: some courses go under, not all of them do. Both bounds are meaningful; if
either were violated the pair fails.

The reachability proof passed across the environment joins, which was the risk the plan flagged for
this item.

293 tests, no failures, counted from XML.

## Iteration 5

Plan item 8, the last one. Star coins now gate the final world at 60 of the 120 available before
it. They had been tracked, counted and displayed for the life of the codebase while gating nothing,
which is the same shape as every other bug found on this project: finished, correct, unreachable.

The risk in fixing it is the opposite failure -- a requirement set somewhere a player cannot reach
turns a goal into a wall. StarCoinGateTest pins both ends: the gate is real, and there are enough
coins before the final world to open it with margin. It asserts the requirement is under two thirds
of what is available, so it never demands a near-perfect run of everything preceding it.

Found while wiring the message: message.planeshift.cannon_locked was referenced by MapNodeService
and did not exist in en_us.json at all, so a player refused by a cannon saw the raw translation key.
Added, along with a separate message naming the shortfall -- a cannon that refuses without saying
why is indistinguishable from one that is broken, and the coin gate is invisible otherwise.

Section 8 of the work plan is now complete. Remaining work is the backlog: the water cast, actually
submerging water courses, swimming, and the duplicate vanilla-block palette in
CourseStructureService.

296 tests, no failures, counted from XML.

## Iteration 6

Backlog, not the work plan -- section 8 finished last iteration.

Investigated the duplicate palette in CourseStructureService and found something larger than the
backlog note described. The palette was not merely a stale copy: it and the seven methods consuming
it were an entire dead subsystem, an older hand-placement course builder superseded by
CourseComposer plus CourseWriter. Every one of the eight members appeared exactly once in the file,
its own definition, and the class has a single public entry point that reaches none of them.

Removed: buildStartLandmark, buildStaircaseObstacle, buildCastleFinale, buildCoinHeaven,
buildFinish, buildPlatformSet, placeGroundSlice, and the Palette record. 658 lines to 489.

The compiler is the proof here, not my reading -- if anything had referenced them the build would
have failed. It did fail once mid-way, when my brace-matcher skipped a method indented differently
from the rest and left Palette referenced; that is exactly the failure mode this approach is
supposed to surface, and it surfaced.

Also corrected a javadoc in ModBlocks that cited CourseStructureService.buildFinish as the thing
laying flagpole steps. The reasoning in that comment is still right; the function it named was dead.

296 tests, no failures, counted from XML. Test count unchanged, which is what deleting genuinely
unreachable code should look like.

## Iteration 7

Water courses now contain water. The theme had spent an iteration as a dry course with a marine
palette, which is the failure this project keeps producing.

I got one thing wrong in my own backlog and want it recorded. I wrote "no swimming" last iteration
based on how the courses behaved; ModFluids had declared canSwim(true) from the day it was written.
Nothing was missing from movement -- there was simply no water to swim in. The note sent this
iteration looking for a feature that already existed, so the backlog entry is struck through rather
than deleted.

Three test failures shaped the result, all of them correct:
- Flooding the whole course drowned the spawn and filled the finish staircase. The reference agrees
  with the tests: a water level opens on a dry ledge and descends, so the flood is confined between
  the spawn apron and the flagpole run.
- The reachability proof then rejected every water course, and it was right -- it models a walker
  and a walker cannot cross a submerged room. Taught it to swim: water is a third category, both
  passable and supporting, which no block is. Treating water as air would have approved routes
  ending in a drop; treating it as floor would have let the player walk on the surface.

Also set canDrown(false). Every underwater level in the reference lets the player stay down
indefinitely -- the water is a place with different movement, not a timer, and an invisible clock
in a level meant to be explored is a bad surprise.

Note for later: the suite went from 45s to about 2 minutes. Flooded courses make the reachability
search much larger. Not a problem yet, worth watching.

299 tests, no failures, counted from XML.

## Iteration 8

Cheep Cheep. Water courses had been populated by Buzzy Beetles standing in for fish, which was the
least wrong land enemy available and was still a land enemy sitting at the bottom of a flooded room.

Eight places had to agree for one enemy to exist: texture generator, rig profile, entity class,
registration, attributes, renderer, bespoke model, and two cast lists. The compiler found three of
the misses on its own -- a missing import, an unhandled switch case in BespokeEnemyModel -- which is
the argument for those switches being exhaustive.

The behaviour is deliberately dumb: cross the lane, turn at the ends, bob slightly. A Cheep Cheep in
the reference does not hunt, and that is the point of it. The threat is that it is somewhere along
the route you have to swim, so the player's job is timing rather than combat; an enemy that chased
would turn a paced swim into a scramble.

BespokeEnemyModelTest caught the new rig and demanded its part count. That table carries a warning
against editing numbers to make red tests green -- a previous pass halved every entry to accommodate
a broken rewrite and shipped a one-part Thwomp. Seven is the real count here and the entry says why
it is low: a fish is only ever seen side-on, so parts visible only from the front would be geometry
nobody sees.

300 tests, no failures, counted from XML.

## Iteration 9

Big Cheep, closing plan item 6.4. A separate entity type rather than a synced variant, and that
was the codebase's own rule rather than a preference: the Koopaling note says the scale is tied to
the registered hitbox, which is exactly why eight Koopalings share one type -- they are all the
same size. Size is the one thing a variant cannot carry, and size is the entire difference here.

Refactored CheepCheepEntity slightly so the swim speed and range are overridable, rather than
copying the swim logic into a second class. The big one is slower as well as larger, which is what
makes it read as mass: it commits to a direction long before it reaches you, so it can be seen
coming from further away and dodged with more room. A bigger threat that is also a fairer one.

Colour rather than scale carries the size cue in the texture. Scaling the red sheet up would have
produced a big Cheep Cheep, which at this camera distance reads as the same animal standing closer
-- "closer" and "bigger" look identical from a fixed side-on view. Green-grey says different
creature before the silhouette has to.

Both fish share one mesh. The model test wanted a part count for the new rig and got the same seven,
with a note saying why: putting the size difference in the mesh as well as the hitbox would be two
sources for one fact.

301 tests, no failures, counted from XML.
