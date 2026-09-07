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

## Iteration 10

Started with an audit rather than new work, because this project's recurring bug is content that is
finished and unreachable, and after nine iterations of adding things it was worth checking I had not
produced more of it.

The audit came back clean. 62 blocks, 29 entities, 25 sounds and 14 config options, and every one of
them is referenced outside its registry; every block is named somewhere under server/gen, so
generation can place it. The four entities absent from server/ are projectiles, spawned by other
entities rather than by generation, which is correct rather than a gap. Recording the clean result
because a negative audit is worth as much as a positive one and is easy to skip.

Then the third drop-crusher telegraph. Thwomp is the clearest case of the rule in the plan: a hazard
that threatens a moment rather than a place is only fair if the moment can be seen coming, and at
this camera distance a block hanging over the lane is easy to miss until it is already falling.
Marked only while raised and idle -- once it commits there is nothing left to warn about, and a
telegraph that kept drawing during the fall would read as part of the hazard rather than as notice
of it.

Section 8 remains complete and the backlog is down to one item that is explicitly the owner's call
(which world water belongs to). Remaining real work lives in plan sections 6 and 7 outside the
section 8 ordering: the rest of the hazards, the traversal set, world map decoration, iris wipe,
title cards.

301 tests, no failures, counted from XML.

## Iteration 11

Climbing poles, from plan section 6.7. The second climbable after the vine, and the reference uses
the two differently: a vine hangs on a wall and is climbed where it is, a pole stands in open space
so crossing to it is half the problem. The spiral stripe is not decoration -- a climbing player is
rendered in the same pose whether moving or not, so the stripe passing the eye is what carries the
motion.

This iteration was mostly me being wrong, four times, and the tests being right each time.

1. Invented my own segment spacing: a four-block gap with a rise in it. The reachability proof
   rejected 188 of 5250 courses. Rebuilt on VINE_WALL's proven skeleton and changed only the thing
   the segment is actually about.
2. Declared width 14 while building 16, so the next segment landed on my exit shelf. A segment that
   lies about its width does not break where the lie is, it breaks wherever the neighbour lands.
3. Forgot to add the pole to the solver's PASSABLE set, so a six-block climbable read as a
   six-block wall. The proof was right given what it had been told.
4. An enemy-density failure I assumed was mine to fix by adding enemies to the new segment. Adding
   them changed nothing, because the failing seed's course does not contain the segment at all --
   adding anything to the catalogue reshuffles the RNG for every seed, and that one landed just
   under the floor. I only found this by stashing my changes and confirming the baseline passed.

The fix for the fourth was not to move the threshold. The roaming pass already says in its own
comment that a single sample lands on a pit often enough to leave stretches empty; it was trying
four times, and tight snow layouts give a slot few valid columns. Six tries is the pass working
harder at the job it already had.

301 tests, no failures, counted from XML.

## Iteration 12

Went looking for title cards (plan 7.5) and stopped short of them on purpose.

The client cannot name the course it is in: CourseState carries the theme but not the course id.
Adding one means extending a hand-written STREAM_CODEC, two parallel lists of fields in matching
order with nothing enforcing that they match. Get it wrong and it does not throw -- the bytes are
still consumed, just read as the wrong fields, so a coin count quietly lands in a lives counter and
surfaces as a gameplay oddity nobody traces to networking.

There was no round-trip test on that codec, and the suite's existing withersPreserveNewFields test
says this record has been extended before and that extending it is where the bugs come from. So this
iteration wrote the missing net rather than the feature: CourseStateCodecTest, with every field set
away from its default, because a codec that reads two fields in the wrong order still round-trips
correctly when both hold the same value.

Then I checked the test actually bites, by swapping two encodes deliberately and confirming it went
red before restoring. A safety net that has never been seen to catch anything is a guess.

Title cards are in the backlog with the reason, and one open question that wants a waking answer:
sync the course id and translate client-side, or sync a Component. That is a UI direction question,
not a plumbing one.

304 tests, no failures, counted from XML.

## Iteration 13

World map decoration, plan 7.3. The map background was one hardcoded grassy gradient for every
world -- the volcano's map and the ice world's map were both green fields. The reference builds each
map from its own world's materials, and the reason is not decorative: the map is the first thing
seen after clearing the previous castle, so it is where the next world introduces itself. A green
field in front of World 6 says the game forgot where it was.

Six ground palettes and six kinds of scenery: dunes, ice floes, volcano cones with a lit crater,
bare trees, stalagmites, weed beds. All drawn from flat rectangles in the screen's existing idiom
rather than as textures -- every shape is two or three fills, and a map that needed an art pipeline
to gain a tree would not have gained one tonight.

The scenery comes from a fixed arithmetic sequence rather than a random source, so the map is
identical every time it is opened. A map whose furniture moves between visits is one nobody can
build a mental image of, and being picturable is the whole argument for a map over a list of
buttons.

Added MapGroundColoursTest, which pins the two claims that can be checked without rendering: the
palettes are genuinely distinct, and each darkens downward so the field reads as ground rather than
ceiling. Nothing here proves it looks good -- rendering is not tested and a screen draws a wrong
colour perfectly happily. That part still needs eyes.

306 tests, no failures, counted from XML.

## Iteration 14

The iris wipe, plan 7.4. The map used to appear all at once, which made returning from a course
feel like being dropped somewhere rather than arriving somewhere. It now opens as a circle growing
from the selected node.

Drawn as horizontal bands, because this screen has rectangle fills and nothing else: for each
scanline the half-width of the circle at that height is solved directly and the two rectangles
either side are filled. That is how an iris was done long before shaders and it is exact rather
than an approximation of a circle.

The radius eases out instead of growing linearly. Constant speed reads as a shutter; easing reads
as an eye opening, which is the whole reference for the effect.

Two things I checked rather than assumed. The effect latches once played, so switching worlds or
resizing the window does not replay it -- an iris that fired on every resize would be a stutter.
And the full radius is now a separate testable function, because there is exactly one way this
fails visibly and permanently: too small a radius finishes the animation, stops the drawing, and
leaves a black wedge in a corner forever. IrisGeometryTest proves every corner is covered from a
focus anywhere on screen, including off it.

Used System.currentTimeMillis() rather than importing Util.getMillis, because the file already
tracks its walk animation that way and one screen with two clocks is a bug waiting for a slow frame.

308 tests, no failures, counted from XML.

## Iteration 15

Progress banners, plan 7.7. The results screen now announces the moment the final world opens.

This mattered more than it looks because of the star-coin gate added earlier tonight. A lock quietly
becoming unlocked is invisible: the player who finally crosses the threshold is looking at the
results screen, not the map, and without saying so the only evidence is a node that stopped being
grey on a screen they may not open for a while. The collectable that gates the world would never get
credited with having done anything.

Needed no networking change, which is why it got done and title cards did not. The client already
holds CourseProgress as an attachment, and "did this clear open it" is derivable by subtracting the
run's own contribution from current progress rather than by remembering the previous state -- so
nothing is synced and nothing is persisted.

The cost of that trick is arithmetic that is easy to get subtly wrong in a way that fires the banner
on every subsequent clear, and a celebration that repeats is worse than none because it teaches the
player to ignore it. So UnlockMomentTest was written before the UI: fires on the coin crossing,
fires on the gating boss clear, stays silent while locked, and specifically does not fire again on
the next course.

Also parenthesised a condition that was relying on && binding tighter than ||. It was correct;
correct and unreadable is a bug waiting for the next person to tidy it.

312 tests, no failures, counted from XML.

## Iteration 16

Buzzsaws, plan 6.6. The third hazard, and the first that threatens a line rather than a point or a
radius: a Thwomp owns the column under it, a firebar owns a disc around it, a saw owns a corridor
the player has to cross. That is why the drawn rail matters more here than anywhere else -- without
it the only way to learn the route is to be standing on it.

It moves on a cosine sweep rather than bouncing between two ends, so it slows at the extremes. That
is what a carriage on a track actually does, and more usefully it gives the player a moment at each
end where the saw is briefly easy to pass.

Renders nothing, following FirebarRenderer: the blade is a ring of sparks the entity emits on the
client. No model to build, and no rotation state that can drift out of step with the position.

This one passed the whole suite first time, which is worth noting against the climb pole two
iterations ago that took four attempts. The difference was not luck -- I kept the floor continuous
so the reachability proof was never load-bearing on a moving hazard it cannot see, and I declared
the width the segment actually builds. Both of those were the exact lessons from that failure.

Added SawPlacementTest for the usual reason: everything about a hazard entity compiles and tests
green whether or not a single course ever contains one. Both bounds again -- it appears, and not in
every course, because a hazard met every level is the floor.

314 tests, no failures, counted from XML.

## Iteration 17

Chain balls, plan 6.6, and the fourth telegraph form. Telegraph.arc has been sitting unused since
the night it was written, which was the right call then and is finally paid off now: a firebar shows
reach as a circle and a saw shows path as a line, but a pendulum needs neither. Its reach is a
circle it only travels part of, so drawing the full disc would claim ground the ball never visits
and drawing the chord would put the warning where the ball is not. Only the arc is true.

Two angle conventions meet in the entity -- the chain measures from straight down, Telegraph.arc
measures anticlockwise from east -- and a quarter turn between them would draw the warning somewhere
the ball never goes. That is strictly worse than drawing nothing: a telegraph the player learns to
trust and which then lies turns a fair hazard into an unfair one. So the maths came out into static
pure functions and ChainBallGeometryTest checks the arc endpoints land exactly on the ball at its
swing extremes, which is the assertion that catches the quarter turn.

The segment places two balls spaced so their arcs do not overlap. Overlapping pendulums open and
close a window on the product of two periods, which nobody can read at a glance -- and readability
is the entire argument for drawing the arc. Two separate problems in a row is harder than one and
still fair; one compound problem is neither.

Floor unbroken again, same as the saw corridor, so the reachability proof is never load-bearing on a
hazard it cannot see. Passed first time, second iteration running.

317 tests, no failures, counted from XML.
