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
