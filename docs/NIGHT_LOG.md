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

## Iteration 18

Volcanic bombs, plan 6.9 -- the one case in the whole reference where the background is not just
scenery. Volcanoes erupt behind the playfield and drop debris into it.

That created a problem none of the other hazards have. A firebar, saw and chain ball are visible for
their entire cycle, so drawing their path is a courtesy; a rock arriving from off-screen is
invisible until it is already falling, and drawing its path does not help because the path starts
where the player cannot see. Left alone it would have been the only genuinely unfair hazard in the
game.

So the telegraph moved to the other end: the landing spot is marked on the ground before the rock is
released. That is a fifth form of the same rule -- reach as a circle, path as a line, swing as an
arc, and here the destination, because the destination is the only part of this hazard the player
can act on. Marked for the whole warning window rather than flashed once, since a single flash is
missed by a player looking elsewhere.

Found a real bug while gating it to the volcano. suitsTheme was only consulted for set pieces, so my
theme gate would have done nothing and rocks would have fallen in grass levels -- and it would have
compiled, tested and shipped perfectly happily. Ordinary segments now go through the same filter.
That was latent rather than mine: it was harmless only for as long as set pieces were the only
theme-specific segments, which stopped being true tonight.

319 tests, no failures, counted from XML.

## Iteration 19

Bone Cheep, plan 6.5 -- the same fish repainted as a skeleton. Behaviour inherited untouched,
because a reskin that also changed how the thing moved would not be a reskin. It is a little faster,
which is the one deliberate difference: an enemy the player already knows how to read should ask
slightly more of them the second time, or the repaint is only a repaint.

Two things here were compromises rather than the right answer, and both are in BACKLOG rather than
buried.

CourseEnemyRenderer already picks a texture from a synced variant, but only for Koopalings, through
a koopalingVariant field on the render state. Generalising that to any enemy is clearly correct and
touches render code the suite cannot check, so it was not something to do unattended at seven in the
morning. Registering a third fish type costs one entry and is provably safe. That is fine at three
types and wrong at ten, which is exactly why it is written down.

And the reskin swims beside the living fish rather than replacing it in the darker worlds, which is
what the reference does. SegmentLibrary.cast() is keyed on theme and cannot see which world a course
belongs to -- GenContext has carried worldTheme since iteration 1, cast() is simply never given it.
Small change, but it alters what appears in every course in the game, so it wants a waking eye
rather than mine.

Needed no rig profile: the bone fish reuses CHEEP_CHEEP, so the model test's part-count table is
untouched. Reusing the rig is the whole point of a reskin.

319 tests, no failures, counted from XML.

## Iteration 20

Ghost platforms, plan 6.8 -- and the finding turned inside out when I checked it against the code.

The note from the sheets was that ghost-house platforms are carried by Boos, so a moving platform
does not have to be a block. MovingPlatformEntity already extends Mob. Structurally the mod has had
platforms-as-creatures since before tonight; what was missing was that none of them looked like one.
The gap was presentation, not architecture, and the plan entry overstated it.

So this changes presentation and nothing else. Movement is inherited untouched, which is the safety
property rather than laziness: CourseCanvas.movingSurface declares the band a platform sweeps so the
proof knows a pit is crossable, and a subclass that moved differently would make every one of those
declarations false. The segment is MOVING_CROSSING copied down to its declared band, with only the
entity swapped -- the existing proof then applies word for word.

The renderer is reused outright. registerEntityRenderer's wildcard accepts a parent-typed renderer
for a subclass type, which I checked by compiling rather than assuming, and the ghost is particles
the entity emits rather than anything the renderer draws.

Ghost house only. A Boo holding up a platform in a grass level is a floating slab with an
unexplained effect underneath it, and the fiction is the entire justification for the reskin.

319 tests, no failures, counted from XML.

## Iteration 21

Audited the remaining plan entries against the code before building anything, because 6.8 last
iteration described a gap that was not there. That was worth doing: 5.5 was wrong too.

The plan said every theme should carry an ambient particle and implied none did. Four already did --
embers in lava, snowfall in snow, dust in desert, souls in ghost houses. The real gap was three
themes: grass, underground, and water. Water is the one that matters, because I added that theme
tonight and it fell straight off the end of an else-if chain, leaving a submerged course completely
still while every other theme drifts. Nothing failed. It just looked dead.

Fixed all three, with the rarity graded rather than uniform: bubbles underwater are constant,
because that is half of what says "under water" before anything moves; cave motes are rare, because
a cave should feel still; grass pollen is rarest of all, because a grass level is the baseline the
other themes are read against and anything constant there raises the floor for everything.

Added ThemeAmbienceTest, which reads the source and checks each theme is named in the ambience pass.
That is a blunt instrument and the right one: the alternative is a headless client ticking to
observe particles, which is a lot of machinery to answer "did anyone remember the new theme". It is
scoped to the pass itself so an unrelated mention elsewhere cannot satisfy it.

Corrected the plan entry too, rather than quietly ticking it off. The plan was written from footage;
some of its entries describe gaps the code does not have.

320 tests, no failures, counted from XML.

## Iteration 22

Second audit pass over plan sections 4 and 5. Two more entries overstated their gaps, making four
found in three iterations.

5.6 said interiors want visible light sources. COURSE_LAMP already existed and CourseDecorator.lit()
already places it in both dark themes -- with a comment explaining that backlighting throws
platforms into silhouette, which is a better argument than the one in my plan entry. The only part
genuinely missing was the second sentence: flame colour is themed. Done now, and the reasoning is
not decorative -- a warm lamp says somebody lives here, which is exactly what a ghost house is not,
and in a dark room a cold light also separates scenery from the fire hazards, which the player has
to tell apart at a glance.

4.8 said platforms come from a parts kit as though none existed. Semisolid platforms, pillars and
trim are registered and placed. The real gap is narrower: the specific shapes from the sheets --
capsule beams, mushroom caps on stalks, thin ledges with inset centres.

Corrected both entries in the plan rather than working from them. The pattern is now clear enough to
name: the plan was written by reading footage and asking "does the mod do this", and I answered that
question from memory of the code rather than from the code. Four entries have been wrong in the same
direction -- the mod having more than I credited it with -- which is the pleasant direction to be
wrong in and still wrong.

Genuine remaining gap confirmed by this pass: pipes are one colour. The sheets show at least five
reading as different objects.

320 tests, no failures, counted from XML.

## Iteration 23

Pipe colours, plan 4.7 -- the one gap last iteration's audit confirmed was real rather than assumed.
Green, yellow, blue, red, magenta, as a blockstate property rather than five registered blocks.

That choice is the codebase's own rule rather than my preference. Size is tied to the registered
hitbox and needs its own type, which is why BigCheep is a separate entity; anything purely visual
does not, which is why this is a property. A pipe is a pipe whatever colour it is.

The side texture predates BlockTextureGen and is not generated, so redrawing it would have meant
inventing a second pipe style and hoping it matched. Recoloured the existing art instead: each
pixel's brightness is mapped onto a ramp built from the new hue, which keeps the rim, the highlight
arc and the dark mouth exactly as they are and changes only the colour. Green is untouched, so
nothing that already looked right moved.

Colour is assigned per theme rather than per placement. Deterministic, so a course looks the same
every time it generates, and stable within a course, so two pipes in one level match. The reference
uses colour to tell destinations apart, and that only works if the colour means something --
recolouring every pipe independently would make it mean nothing.

PipeColourTest checks the blockstate and art against the enum rather than a hardcoded list. The
failure mode here is quiet: a property value with no variant renders as the missing-model
placeholder and the build says nothing, so it is found by walking into a pipe that looks like a bug
report. Adding a sixth colour and forgetting its art now fails here instead.

323 tests, no failures, counted from XML.

## Iteration 24

Audited plan sections 3 and 7. One more overstated entry, one genuine conflict, one real gap.

3.5 said the boss arena should be approached rather than entered. BossArena already builds an
approach, and its comment makes the point better than the plan did -- the castle used to be a
corridor, and this is the room it was describing. Fifth entry corrected.

7.2 is not an oversight but a disagreement. The plan says a secret exit should unlock a cannon;
MapNodeService.fireCannon deliberately refuses, arguing that a shortcut which also grants access is
a cheat code that would let a player reach the last world without clearing a castle. Both positions
are coherent and mutually exclusive, and the current rule does leave the cannon nearly pointless --
it opens only when the next world is already reachable on foot. Left for a ruling rather than
implemented, because overriding a documented decision with nobody awake to disagree is not my call.

Signposts are the real gap, and placing them turned out to be the interesting part. The composer
already argues, correctly, that an arrow near a secret is worse than nothing: it announces the
secret instead of letting the player find one, which is why it leads with coins instead. The
reference uses signs for ambiguous *open* routes, not hidden ones -- so they went on the two
segments with genuinely two ways up, the climbing pole and the vine wall, and nowhere near a secret.

323 tests, no failures, counted from XML.

## Iteration 25 — corrected to the genre

The owner looked at the night's work and said, in effect, make it feel like Mario. They were right,
and the biggest offender was the rule I had been most pleased with.

"Every moving hazard renders its own trajectory" is a correct observation. My implementation of it
was not: sweep circles in smoke, glowing dotted rails, a ground marker under falling rock. That is
a modern indie-game idiom -- diegetic UI painted over the world -- and this genre never does it.
The genre telegraphs constantly and always with objects that are actually there. A grinder runs on
visible track. A spiked ball hangs from a real chain. A fire bar's warning is the fireballs. A
Thwomp is readable because it is a huge face that pauses before dropping.

So the drawn telegraphs are gone and Telegraph.java is deleted. What replaces them is physical: the
saw corridor now lays a line of grinder track blocks the saw runs along, which carries the same
information as a thing the player can see for the same reason they can see a wall. The chain ball
keeps its chain, which was always the right answer and was doing the job before I added an arc on
top of it. The firebar, the moving platform, the Thwomp and the fire rock simply lost their
overlays; each was already readable without one.

Also renamed the new cast into the vocabulary the mod already speaks -- it has had Goombas, Koopas,
Thwomps and Boos since long before tonight, and then I added a "Saw" and a "Chain Ball". They are
now Grinder, Spiked Ball, Fish Bone, Big Cheep Cheep, Fire Rock and Boo Platform.

Worth recording plainly: this is the second time tonight the plan was right about *what* the
reference does and wrong about *how*. The lesson is the same one as the sheets -- describing an
observation is not the same as knowing how it is built.

323 tests, no failures, counted from XML.

## Iteration 26

Second half of the genre correction, and it found the same mistake made a second way.

Three hazards built last night -- grinder, spiked ball, fire rock -- had no models at all. Each was
an invisible entity with a renderer that drew nothing, represented by a cloud of particles. I had
copied that from FirebarRenderer, where it is exactly right, because a fire bar's flames genuinely
are the thing. It is wrong everywhere else: these are solid objects in this genre, big and readable,
and a scatter of sparks where one should be reads as an effect rather than a hazard.

BespokeProjectileRenderer already draws baked geometry for plain non-mob entities. It was here the
whole time and I did not look. Three profiles, three meshes, three 64x64 sheets in the existing
layout, and the three empty renderer classes are deleted.

The meshes are deliberately coarse. A spinning disc, a swinging ball and a falling rock are all seen
in motion at distance, and detail that cannot resolve while the thing moves is texture memory spent
on a smear. Silhouette and value contrast are what read -- hence pale spikes on a dark ball, because
a uniformly dark ball swinging through a dark castle is a shape nobody can track.

Wrote the rule into AUTONOMOUS_BRIEF.md rather than only into this log, so it survives a context
reset: telegraphs are physical, hazards are solid, names come from the vocabulary already here.

326 tests, no failures, counted from XML.

## Iteration 27

Swept the rest of last night's work for anything else built in the wrong idiom. Two found, both the
same instinct: reasoning from Minecraft defaults rather than from the game being imitated.

Underwater fog was set to 24 blocks and the comment called it generous. That was still vanilla
thinking -- pushing a Minecraft default outward instead of asking what the reference does, which is
nothing at all. Its underwater levels are not murky: the water is a colour over a fully visible
screen, and every platform and coin is legible the moment it appears. Fog is how a first-person
game says "submerged"; a side-on platformer says it with the tint and then gets out of the way.
Pushed past any distance the camera can see, so the value is really "none".

Grass pollen and cave motes, both added yesterday on the reasoning that every theme deserves some
air. Checked the reference: its grass levels and caves are visually clean. Snow has snowfall,
castles have embers, ghost houses have wisps, water has bubbles -- every one of those is the weather
of that place. Pollen drifting through a meadow is a Minecraft habit.

Removed both, and listed them in the test as deliberate absences rather than deleting the coverage.
An absence that is written down is a decision; an absence that is not is a bug waiting to be
helpfully fixed by whoever notices the omission next.

326 tests, no failures, counted from XML.

## Iteration 28

Ran the client, fixed what it found, and made the finding permanent.

The remaining warning after the first pass was the Koopaling spawn egg, and my fix for it had gone
to the wrong place: I wrote models/item/koopaling_spawn_egg.json, but 1.21.11 reads item definitions
from assets/planeshift/items/. Every other spawn egg in the mod already used that directory. I
guessed the format instead of looking at the fourteen working examples sitting next to it.

The client now loads with zero asset warnings, verified by launching it rather than inferring it.

AssetWiringTest is the standing guard: every registered block has a blockstate, every registered
item has a definition in the directory the loader actually reads, no model points at a texture that
does not exist, every renderer names a texture that is on disk, every entity has a renderer. Five
checks, two seconds, and they fail in CI where nobody is watching a client boot.

The brief now requires running the game, not just the tests, with the reason spelled out --
runGameTestServer is headless and takes two minutes. Worth noting for whoever reads this next: the
headless server does not load client models, so a clean run there says nothing about assets. The
client is the oracle for that; the server is the oracle for world behaviour. I confused the two
briefly this iteration and nearly reported a false all-clear.

331 tests, no failures, counted from XML.

## Iteration 29

Plan item 6.2, the sky theme, following the water theme's path exactly -- enum, palette, lesson
rules, cast, set piece, and made reachable.

Made reachable is the important half. The "Sky Kingdom" world existed and was themed GRASS, which
the sheets had flagged as a mismatch: its map is clouds and its levels are about height, and it was
generating meadows. Its ten courses are rethemed and the world registry now agrees with its own
name.

The cast is the point of the theme rather than decoration. Paratroopa, Lakitu and Bullet Bill all
ignore the floor, and an enemy that ignores the floor is only interesting where the floor is the
scarce thing -- which is to say all three were being wasted everywhere else in the game.

CLOUD_SPIRE is the climax: five mushroom caps rising, gaps tightening as it climbs, each on a short
stalk so it reads as growing rather than floating. Tightening matters -- a climb with even spacing
is a staircase turned sideways, and a climax should put its hardest jump where a miss costs most.

Two tests written yesterday caught the new theme immediately, which is the first time this session
that guards I wrote earlier have paid off on work I did later. MapGroundColoursTest failed because
SKY reused another world's map palette; ThemeAmbienceTest failed because it had no ambience. The
second is a decision rather than a gap: the reference's cloud worlds are visually clean, same as its
grass and its caves, so SKY joins the deliberately-still list with the reasoning attached.

340 tests, no failures, counted from XML.

## Iteration 30

Two plan items and a ruling.

AIRSHIP_DECK is plan 6.3: the playfield as a vehicle rather than terrain. Everywhere else in this
mod the floor is ground; an airship is an object with a silhouette, hull and prow and stern, with
open sky under both ends so the player can see where it stops. Two courses of planking because a
one-block deck reads as a plank bridge, ends raised because the curve is what says boat -- and the
raised ends also stop a player walking off a deck they are seeing side-on for the first time.

Then the cannon. This was in BACKLOG as a genuine disagreement rather than an oversight: the plan
said a secret exit should unlock a cannon, and MapNodeService deliberately refused, arguing a
shortcut that also grants access is a cheat code. The owner ruled -- make it like Mario -- and Mario
is unambiguous here. Finding a secret exit is the achievement; the cannon is what it buys.

Implemented rather than bolted on. CourseProgress.Record gains a secretExit flag, defaulting false
so every existing save loads unchanged and simply has none recorded, which is exactly true of them.
The keyhole records it before beginning the slide, since the slide ends the course. Both withers
preserve it, because clearing a course must never revoke a secret exit already found there.

Kept the star-coin gate on the final world. A cannon is a shortcut through the ordinary sequence,
not a way around the one requirement the whole run is built on -- and the old code's instinct was
right about that much even where it was wrong about the rest.

Removed my own `if (false)` while doing it. It compiled and it would have shipped.

340 tests, no failures, counted from XML.

## Iteration 31

Installed the dev mods and made the tower bosses fly.

Four mods, all verified loading together with PlaneShift: Sodium 0.8.14, Jade 21.1.7, JEI 27.37.0,
FerriteCore 8.2.0. Client reaches the render thread with zero asset warnings. The only errors in the
log are log-file locks from the previous client still being open, which is not a failure.

Worth recording: the machine already had Sodium, in a CurseForge backup -- built for 1.21.1, not
1.21.11. Close enough to look right in a filename and completely wrong for this game. ModernFix
would have been the obvious fourth pick and has no 1.21.11 build, so FerriteCore took the slot.

Then plan item 3.2, which is the largest remaining boss gap. Koopalings were eight ground-walking
mobs; the reference fights them from a hovering clown car, and that is the shape of the fight rather
than decoration. A boss on the player's own floor turns the encounter into a shoving match along a
line whose answer is to walk forward. In the air it owns a space the player cannot reach, and the
fight becomes about the moments it comes down.

The dive is the mechanic. It hovers above jump height, tracks the player more slowly than the player
runs, and drops on a fixed interval along a sine arch -- slowest at the bottom, which is exactly
where the player has to meet it. Being stompable down there is the point rather than an oversight:
three stomps defeat a Koopaling, and the dive is the game handing over those three chances on a
rhythm that can be learned. A boss that dived only when it chose to would be a wall with a health
bar.

340 tests, no failures, counted from XML.

## Iteration 32

The owner asked whether the Koopalings really fight as a group in a shared vehicle, and whether I
had read the wiki. I had not -- I read it off contact sheets and treated one frame as a rule. Two
plan entries were wrong and one shipped feature was a regression.

Corrected, reverted, and then the tooling gap behind it fixed.

The sheets already cover 100% of the video: 15427 seconds at one frame per six seconds is 2571
frames across 108 sheets, and the arithmetic leaves no gap. Nothing was missed in the sense of
sections. What was missed is resolution -- one frame in 180 -- which is fine for finding where
something happens and useless for reading how it works. A forty-second boss fight is four frames at
that rate: enough to see a boss exists, not enough to see what it does.

So VideoFrames gained a `dense` mode: sheets over an arbitrary slice at one or two frames a second.
Ran it over the World 1 castle at 1fps and the fight is legible for the first time. The boss walks
the floor of an arched hall, retreats into its shell and spin-dashes along the ground, then emerges
and walks again. Grey pillars stand in the arena, which is what the wiki means about hiding behind
them. Three stomps.

The useful part: KoopaEntity already implements a shell state, and the renderer already reads
inShell() for it. A Koopaling spin-dash is that mechanic at boss scale, not a new one. So the plan
entry now says what to build and what to build it from, instead of describing a vehicle that does
not exist in this game.

Finer sampling would not have prevented the second error -- no number of frames tells you a rule --
but it would have made the first one obvious immediately.

340 tests, no failures, counted from XML.

## Koopaling shell dash (plan 3.2)
Wired the shell dash. Checked the wiki first this time rather than building from frames:
it is shared by every Koopaling and fires after *every hit*, so it is a reaction, not a
timed attack. `hurtServer` sets `dashTicks`, the dash owns `tick()` while it runs, and it
bounces off arena walls. Direction is away from the attacker so it never reads as a lunge
at a player still airborne from a stomp. `spinning()` from `ShellSpinner` drives the shell
skin. 340 tests, 0 failures.

## Boss arena pillars (plan 3.2)
Four castle-stone columns in the back row of the bridge lane. Back row, not the
centre: a pillar in a three-wide lane is a wall, and the gaps are what makes cover
cover. Spaced four apart -- wider than a jump -- so moving between them costs
something. They are also the shelter the 3.3 stone attack will need. 340 tests, 0 failures.

## The clown car (plan 3.3)
Read the wiki before building, and it contradicted my own plan entry twice over.
World 6-Castle's boss is Bowser, not the Koopalings, and the clown car is a hazard
*during* the castle: it flashes its eyes to turn the player to stone, and you evade
by passing between the pillars. No health, no defeat. So this is a set-piece with a
verb -- get past -- that nothing else in the mod has.

Built: STONE effect (FROZEN's mechanism taken to -1.0, so it is a state not a
penalty), ClownCarEntity hovering over the approach of the last world's arena,
flash resolved with one raycast so the rule the player learns is the rule that
runs, eyes lit during the wind-up, approach pillars as the cover, baked mesh and
texture. 341 tests, 0 failures. Not yet seen in-game.

## Super Bowser (plan 3.4)
The wiki again disagreed with the plan: the final castle's phase change is not
skeletal-then-giant with recoloured fire, it is Bowser knocked into the lava and
revived enormous by the Koopalings' wands. Built that. A second entity type rather
than a flag, because rig scale is tied to the registered hitbox and a bigger Bowser
has to be registered bigger. Hooked on death, not on a health threshold, so the
bridge drop cannot skip it. Only the last world's arena sets the flag.
342 tests, 0 failures. Phase two's climb is not built yet and is recorded as such.

## The first Bowser was killing himself (found while building phase two)
Chasing the reference's phase-two climb, I checked where BackgroundBossGoal
actually puts a boss: five blocks behind the play plane. The arena lays floor
across the three-block lane and nothing else. So Bowser eased out past the back
wall, stood over nothing, fell, and BowserEntity.tick killed him for leaving the
world -- in every castle in the game. Nothing failed, because an arena with no
boss left in it is still an arena.

The wiki says the same thing the fix does: the last castle is fought twice, and
it is the *second* Bowser who is enormous. The first is a corridor fight, which is
what the arena builds and what the axe is for. So the backdrop staging moved to
SuperBowserEntity, where it is both correct and survivable, and the last castle
gained the ledge and the wall set back to hold him.

Verified rather than assumed: with the ledge disabled the new test fails, with it
on it passes. 344 tests, 0 failures. (Cost me the edit once -- I ran git checkout
on an uncommitted file to undo the experiment and threw away the fix with it.)

## Phase two is a climb (plan 3.4)
Built out of blocks that already existed. The last castle's finish staircase is
donut blocks rather than stone, so the way up to the pole falls away under the
player while Super Bowser throws fire from the backdrop. Treads only -- the columns
stay stone, because a fully-donut staircase would vanish under one missed step and
leave the player waiting for respawn timers.

Not built: the switch at the top that drops the floor under him. Recorded in the
plan rather than glossed. 345 tests, 0 failures.

## The ending (plan 3.4, finished)
The ledge Super Bowser stands on is ON/OFF blocks switched on; an ON/OFF switch
sits beside the top tread of the climb. Reaching it turns the floor off under him.
Two blocks that already existed, placed where they mean something.

Nearly shipped broken: I put the switch nine blocks above the ledge, and
OnOffSwitchBlock reaches eight. It would have been hit, made its noise, and done
nothing -- the same shape of bug as Bowser standing on air, found the same way, by
checking the number instead of assuming it. RANGE_Y is public now, because a
number placement depends on and cannot read is a number that gets guessed.

The test reads both heights back out of the canvas rather than restating them as
literals; the first version I wrote compared two constants and would have passed
whatever the arena did. Verified by moving the switch back to y=9 and watching it
fail. 346 tests, 0 failures.

## Big Boo (plan 3.6)
Checked the wiki first: Big Boo, Mega Cheep-Cheep, Mega Deep Cheep, Mega Fuzzy,
Mega Piranha Plant. The mod had already shipped a Mega Cheep-Cheep without calling
it one, and with it the pattern -- separate registration, same mesh, larger rig,
because scale is tied to the hitbox.

Built Big Boo the same way, and placed it in the same commit rather than
registering it and moving on: the ghost-house climb's last landing carries it. That
segment's comment already said the pressure builds with the height; now the climb
arrives somewhere. Behaviour unchanged on purpose. 347 tests, 0 failures.

## Mega Piranha Plant (plan 3.6)
Same pattern, third time. Placed on the tall middle pipe of piranha_pipes, which
the segment already made taller than its neighbours so the row would not read as a
fence -- so the odd pipe out is where the threat goes.

One thing that was not just scale: the emerge height had to become overridable.
At the inherited 1.2 blocks the big plant's head would still be inside its own pipe
at full extension, and the rise-and-fall the player times their run against would
have happened out of sight. A hitbox and a mesh scale together; a hand-written
animation distance does not. 348 tests, 0 failures.

## Title cards and the finish banner (7.5, 7.7)
Title card: an overlay, not a Screen -- a screen would pause input and release the
mouse, turning a half-second flourish into a dialog. The packet carries the two
display strings rather than a course id, which settles the backlog question: the
client would otherwise need its own copy of the world table.

The all-cleared banner string had been in the language file the whole time with
nothing able to say it. Wired via a separate small payload, because the results
packet already fills all eight composite slots. Two traps hit and fixed on the way:
"everything is cleared" is true forever after, so the check needed the before-state
too; and taking the latched banner inside render() clears it sixty times a second
and shows the news for one frame. Both are covered by tests. 350 tests, 0 failures.

## Chain Chomp (6.6, last one)
Checked the rest of 6.6 first and five of the six were already there -- the Thwomp
is the drop-crusher the entry describes. The Chomp was the real gap, and it is the
only threat in the mod answered by position rather than timing: it owns a radius,
the edge does not move, and the chain is drawn so the player can see where it is.

Tether is clamped rather than repelled, so the reach measured by eye is the reach
you get. No goals at all -- a navigator that did not know about the chain would drag
it off its post. Placed in its own segment, wider than the reach, with the coins
inside it. 351 tests, 0 failures.

## Signposts (7.6)
An arrow board in the spawn apron of every generated course. Scenery, not UI: no
collision, no text, no state. Placed at the start rather than at secrets -- the
composer already leads the eye to a secret with a rising coin trail and has a
comment explaining why an arrow there would be worse, so this only fills the gap
that comment does not cover. 351 tests, 0 failures.

## One signpost, not two
I built a signpost block without checking, and COURSE_SIGNPOST already existed --
with a texture, and already placed at the foot of the climbing pole, under a comment
making the same argument about secrets I put in my own commit message. Exactly the
failure this project keeps producing, this time by me and in the other direction:
not unreachable work, but a second copy of reachable work.

Kept the newer one on the owner's call. It is also the better block: a real post and
board rather than a painted cube. The two segment placements were repointed to it and
the old block, its texture entry, the now-dead signboard() generator helper and its
three asset files are gone. 351 tests, 0 failures.

## ParCool instead of building the verbs myself
The playtest instance has ParCool, and it ships exactly what plan 6.7 was asking
for: wall jump, wall slide, cling, pole climb, zipline, vault, roll. Most of 6.7
was never terrain, it was verbs, and the terrain half (vines, barber poles,
beanstalks) already existed.

Built ParCoolBridge. It clears stamina inside courses via parcool:inexhaustible --
Mario has never had a stamina bar -- and stands PlaneShift's own wall jump down when
ParCool is loaded. That one is off by default and its own comment says why: in a
course packed with blocks it fired on almost any airborne moment and read as a free
double jump. ParCool's asks the player to actually be against a wall.

Looked up by registry id rather than compiled against ParCool, so nothing has to
ship somebody else's jar in this repo and the mod runs identically without it.

Three failed attempts at a wall-kick chimney, recorded in BACKLOG: full-lane walls
rejected 189 of 6000 courses, raised walls 201, a centre column 189 again. The
constant count was the clue -- CourseReachability searches the x/y plane at z=0, so
any block in the lane centre is a wall to it whatever is beside it. A chimney needs
to be an alcove off the route, which is a segment shape the library does not have.
Dropped rather than shipped half-working. 351 tests, 0 failures.

## The mod had boss music and eight bosses it never played it for
CourseMusicManager decided "boss nearby" by asking for BowserEntity, written back
when he was the only boss. Eight Koopalings were added later and nobody revisited
the question, so the tower fights ran on ordinary course music. One predicate.
SuperBowser needs no mention -- it extends BowserEntity, so the revived fight keeps
the track the first one had, which is right: it is the same fight continuing.

Also read the playtest pack properly and wrote up what actually collides with this
mod. The one that matters is enhanced-movement's double jump: it does not make a
course unbeatable, it makes every gap free and every secret trivial, which is the
same as deleting the level design and keeping the scenery. SereneSeasons turned out
to be harmless and that was checked rather than assumed -- it tints biome foliage,
and no block model in this mod carries a tintindex at all. 351 tests, 0 failures.

## Two missing skyboxes and a fish that follows
CourseSkyboxRenderer builds its path from the theme name, so water and sky courses
had been asking for files nobody drew. Eight themes, six pictures, no error --
a missing texture is a purple chequerboard, not a crash. Drawn both, and a test now
reads the files for every theme.

Water's cast was three patrol patterns and a Spiny. Swimming is slow, so a threat
that never approaches is one you wait out. DeepCheepEntity follows instead --
slower than the player swims, on purpose: it exists to keep them moving, not to
catch them. Same mesh and silhouette as the red one; the difference is the palette
and a scowl, which the red sheet's own comment says it deliberately does not have.
353 tests, 0 failures.

## Urchin, and the last oversized fish
The water cast could be answered entirely by timing and movement. UrchinEntity is
the first thing in it that cannot be answered at all -- it is terrain that moves,
and the only play is not being there. Vertical drift rather than a sideways patrol,
so what it closes is the floor-to-ceiling gap rather than a stretch of corridor.

No face, and that is the design: eyes invite the player to look for the front, and
this has no front. It is the only rig in the model table without a face plate.

MegaDeepCheep is the fourth run through the oversized pattern and took two lines,
which is the evidence the "size lives in the registered hitbox" rule is paying for
itself. 355 tests, 0 failures.

## Iris wipe and castle windows
The iris opens onto a course, paired with the title card. Scanlines rather than a
mask texture: each row of a circle has a known half-width, so the black outside it
is two fills -- no texture, no shader, and it cannot end up an oval. Eased out,
because a circle growing at a constant rate covers area at an accelerating one, and
the radius clears the corners rather than the edges or the last thing you see is
four black triangles. The closing half is not built and the plan says why.

Castle windows finish the visible-light-sources entry. Set high in the back wall:
a window at head height reads as a doorway, and the player must never spend a jump
finding out it is not one. 355 tests, 0 failures.

## Blooper and Fuzzy
Blooper is the water's third movement idea after patrol and pursuit: gather, dart,
then sink helplessly while recovering. The sinking is the encounter -- the player is
not dodging the squid, they are waiting for the beat where it cannot steer. A
two-state clock rather than pathfinding, because a navigator would smooth exactly
the thing worth having.

Fuzzy shares the urchin's brief and differs in travelling. It has eyes and the
urchin deliberately does not: eyes say the thing is going somewhere. The two are the
same size and nearly the same colour, so at a glance the difference is that one is
regular and one is not -- regular reads as mineral, irregular as alive. Put in the
cave roster, which was four things you could stomp, shell or wait out. 357 tests, 0 failures.

## Para-Goomba
The cheapest kind of content: the Goomba's walk with a hop on a clock, and the
Paratroopa's wings lifted unchanged onto its rig. One stomp takes the wings and
what lands is an ordinary Goomba, which is the reference's own rule and the reason
this is worth building rather than being a Goomba that moves oddly -- the player
learns that wings are a layer, not a creature. Replaced rather than mutated on
death, because the wings live in the registered type. 358 tests, 0 failures.

## Casts that know their world
Closed a backlog item the water roster's own comment had been describing: cast()
was keyed on theme, so a cave in the snow world and a cave in the volcano drew the
same four enemies. Palette.forTheme already took the world for exactly this reason
-- the rock a cave is cut through is the rock the world is made of -- and the cast
is the other half of that sentence. Snow caves, volcano caves and haunted flooded
rooms now have their own rosters; everything else falls through to the theme.
358 tests, 0 failures.

## Mushroom stalks
The cheapest thing on the parts-kit list: no new block, because a pillar is a stalk
and a semisolid platform is a cap. It adds a shape the library did not have -- a
platform held up by a line rather than extruded from terrain, so the space under it
stays open. Caps are semisolid so they can be jumped up through; a mushroom you have
to walk around to climb is a wall with a hat. 358 tests, 0 failures.

## Castle walls stop being a grid
The plan calls mixed-size masonry the single most visible thing in the reference,
and the castle block was one brick module in even courses. coursed_rubble lays three
courses of unequal height, each split into stones whose widths sum to sixteen -- the
sums are what make it tile, since every row closes at the block edge while nothing
inside repeats at the same interval twice.

Two tones per stone, hashed from position and kept close together: one flat colour
makes the sizes invisible at 16px, and tones far apart make chequerwork rather than
stone. masonry stays for the brick block, where a single module is the right answer.
Sixteen connected variants regenerated. 358 tests, 0 failures.

## A lava sea
Volcano courses had lava under each gap and nowhere else. One pass now lays it at
a single depth for the whole course, four below the lowest floor: a per-column depth
follows the terrain and reads as a lava river with hills in it. Fills only what is
still empty and runs after the routes, so it cannot overwrite deliberate geometry,
and sitting under every walkable surface keeps it clear of the reachability proof.

The test I wrote first asserted all lava sits at one height, and it failed --
correctly. Set-piece segments build raised lava channels, and so does the reference.
The finding was never "one lava height", it was "the bottom of the level is one
surface", so the test now asserts continuity of the deepest lava instead. Kept the
weaker claim rather than the tidier one. 360 tests, 0 failures.

## The sea emits
Podoboos now belong to the lava sea rather than to whichever segment happened to
include one, so a volcano course is played above a surface that does something
instead of one that just sits there. About one per screen -- punctuation, not a wall
of fire -- and only where six blocks of clear air mean the player can actually see
it happen. A fireball under a solid floor rises, hits the underside of the level and
falls back, having cost nothing and taught nothing. 361 tests, 0 failures.

## Lava jets
One entity with a facing rather than a geyser class and a wall-jet class: they are
one idea -- a fixed place that is safe most of the time and lethal on a rhythm --
and two would have meant two clocks to keep in step.

The wind-up is the whole design. The column grows over a fixed warm-up before it
can hurt anything, and it stays drawn while withdrawing, so visible and lethal are
deliberately different windows. Without that it is a trap, and a hazard the player
cannot read is a death they cannot learn from.

The sea alternates jets with Podoboos: a moving threat the player tracks and a
fixed one they time. Either alone is a rhythm you stop reading by the second world.
The sideways orientation is built and nothing places one yet -- recorded, not
glossed. 362 tests, 0 failures.

## Wall nozzles, placed the same hour they were noticed
The sideways jet orientation was built and nothing generated one -- this project's
signature failure, so I placed it immediately rather than logging it. NOZZLE_CORRIDOR
puts jets in short pillars they visibly come out of: a column of fire starting in
mid-air is an effect, one coming out of a hole in a wall is plumbing, and players
read plumbing as something that will do it again. Facing each other at different
heights, so the safe moment is a place rather than a pause.

The test was checked against a build with the segment unregistered and it failed
there, so it is testing generation rather than the class existing. 363 tests, 0 failures.

## Section 1 finished
Five telegraph forms, five built as geometry. The gap was platforms: they moved
with nothing showing where they went, so the player had to guess how far one would
come. MOVING_CROSSING lays a rail run under the sweep; LIFT_SHAFT hangs its lift
from a cable with a bracket at the top, and the bracket is the more important half
-- it says how high the thing goes before you have waited to find out. GHOST_CROSSING
gets neither on purpose: a ghost does not run on a track.

Fell out of it: COURSE_RAIL is noCollision but was not in the reachability proof's
passable set, so the one block the mod draws paths with read as a wall to the
solver. Fixed and tested.

Two tests written, both checked against sabotaged builds. The first version of the
platform test searched a wide height band and passed with the horizontal track
removed -- it was finding the lift's cable, a different telegraph on a different
platform. True and meaningless. Narrowed to the block directly underneath, where it
fails properly. 366 tests, 0 failures.

## Section 2, all but one
2.1, 2.2 and 2.3 were already built -- three wall motifs per course, caves that
draw no backdrop at all, and per-theme hill profiles. Checked rather than assumed.

2.4 was real and now done. The haze lived in the far-layer block palette, so it
could only reach things that were far away, which is exactly what "foreground
included" was pointing at. CourseAtmosphere sets fog colour per theme instead --
Minecraft's own name for what colour the air is, mixed into the whole frame with
nothing drawn over the world. Blends 55% rather than replacing, because a full
replacement is coloured glass. Never touches view distance: that was settled when
the underwater fog came out.

The test checks the themes are distinguishable from each other, not merely that a
tint exists -- one tint everywhere is the same grey in a warmer hue.

2.5 is the only entry left in the section: the fluids still cannot fall, and the
lavafall is a column of source blocks. 368 tests, 0 failures.

## 2.5 was wrong, and the game said so
The entry reasoned from the registration: levelDecreasePerBlock is 8, so a flowing
block has no level left, so nothing falls. That property governs horizontal spread;
vanilla handles falling on a separate path. Reading the code was never going to
settle it.

Wrote lava_fall_test -- source in the air over a floor, tick, look. The lava falls.
It always has. All 10 gametests pass.

The backdrop's column of source blocks stays, now for a stated reason instead of an
assumed one: a poured column is generator-visible geometry the reachability proof
can see, while a flowing fluid exists only at runtime and the proof would be
reasoning about a level the player never gets. Right answer, wrong reason, worth
writing down so nobody fixes it back.

Third plan entry wrong about how something is built rather than what it does, and
the first settled by running the game. Section 2 is complete. 368 unit tests, 10
gametests, 0 failures.

## Section 3 finished: the two moves that were missing
3.2's "he stuns himself on them" had no implementation, and it is the half that
makes the fight a fight. A charge that misses now ends in the wall and leaves the
boss helpless for two seconds. Without it the player waits for a gap in a pattern;
with it they make one by not being where the boss is going. The shell dash stuns on
its second wall too, which also stops it rattling between two walls forever.

3.1's "reaches forward into the lane" is built as a real lunge across the depth
gap rather than a hitbox that appears. Wind-up is three times the strike and the
boss leans visibly out of the backdrop through all of it. BackgroundBossGoal holds
depth every tick, so it had to be taught to let go -- ReachesIn is one boolean,
because the goal only needs to know whether to keep its hands off. The swipe tracks
depth and never sideways: a claw that followed you along the lane would be
unavoidable, and a fixed reach only works if stepping out of it does.

Tested as timings rather than behaviour -- neither move runs without a level, and
the numbers are the fight. 371 unit tests, 10 gametests, 0 failures.

## Courses can descend into water now (4.1, 4.2)
The sub-environment machinery already existed and was only ever a cave. The entry's
own wording is "outdoor to cave to outdoor, or dry ledge then a descent into water",
so the interior stretch is now flooded one time in three. That is also the cheapest
crossbreed in 4.2: water stops being a place you travel to and becomes something
that happened to part of a level you were already in.

Two things it broke, both usefully. WaterCourseTest said "no other theme floods" --
true when written, and exactly the partition 4.2 says does not exist. Narrowed to
what was really being protected: a dry course may have a wet stretch but must never
be wet end to end or flood its spawn apron.

And LavaSeaTest failed because one extra RNG draw shifted every seed, dropping sea
emitters from 6 of 8 courses to 4. That was not churn -- it exposed that emitter
placement used a fixed stride plus a visibility test, which silently becomes "often
nothing at all" since most of a volcano is solid floor. The pass now walks forward to
the next visible column instead of skipping the slot. Fixed the placement rather than
the threshold. 372 tests, 0 failures.

## Things growing on the floor (5.8), and sandstone (4.6)
Every prop in the decorator sat behind the lane, which is why the walkable surface
read as a shelf the level is displayed on. Tufts now stand on the floor the player
runs along: sparse, and only where the floor is the theme's own surface, because
grass on a girder is the detail that makes a level look generated.

Non-solid and in the reachability proof's passable set -- the rail taught that
lesson already. Coral was going to cover water and it walled 108 of 6000 courses,
because COURSE_CORAL is solid and built for the layer behind the lane. Dropped water
and wrote down why rather than forcing it.

Sandstone joins the castle block on mixed-size masonry. Brick and desert brick keep
the even module: a quarry and a brickworks differ in exactly whether the pieces come
out the same size. 373 tests, 0 failures.
