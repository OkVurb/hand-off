# Work plan, from the reference footage

Built from a sampled walkthrough recording (4h17m at one frame per six seconds, 108 contact
sheets), the owner's screenshots, and the wiki page for that game. Observations are ours, in our own
words, and feed original generated art and original code.

**Method note.** All 108 sheets have now been read. *Sheets show what a frame looked like, not
what the rules are* -- two boss entries here were wrong because a single frame was read as a
pattern, and were only corrected by checking the wiki. Prefer the wiki for mechanics; use the
sheets for how things look. An earlier draft of this plan was written off
five of them and called complete, which was too thin a base for the word. Findings marked
*confirmed* recurred across worlds; those that did not are single sightings, and a few of those are
the most interesting entries here.

**What the full pass changed.** Reading everything did two things sampling had not. It turned five
scattered observations into one rule (§1), and it caught five pieces of *already-shipped* code doing
the wrong thing (§2). The second is the real argument for having read the sheets before writing more
entity and generation code rather than after.

---

## 1. The one rule worth reading first

**Every moving thing renders its own trajectory.**

It showed up five separate ways before it was obvious:

| Form | Where |
|---|---|
| Sweep circle | Firebars rotating on a hub |
| Rail line drawn through the level | Buzzsaws travelling a castle |
| Tether with a visible anchor dot | Enemies hanging from a ceiling |
| Swing arc | Pendulum platforms on cables |
| Wire with anchor dot | Platforms hung from above |

A hazard shows its reach *before* it reaches you; a platform shows its path *before* you commit to
the jump.

**Status: complete.** All five forms are built, and all five are geometry.

| Form | Where | Built as |
|---|---|---|
| Sweep circle | Firebars | The fireballs themselves are the reach |
| Rail line | Buzzsaws | `COURSE_RAIL` laid through the level |
| Tether with an anchor | Chain Chomp | Chain links and a post, both real |
| Swing arc | Chain balls | The chain is real geometry |
| Wire with an anchor | Lifts and crossings | A rail run under a travelling platform; a cable and a bracket above a hanging one |

The last two were the gap. Moving platforms travelled with nothing to show where they went, so a
player on the near bank had to guess how far one would come rather than read it. `MOVING_CROSSING`
now lays a rail run under the sweep and `LIFT_SHAFT` hangs its lift from a visible cable with a
bracket at the top — the bracket being the more important half, since it says how high the thing
goes before the player has waited to find out. `GHOST_CROSSING` deliberately has neither: a ghost
does not run on a track, and the thing carrying it is its own telegraph.

One correction fell out of this. `COURSE_RAIL` is declared `noCollision`, so the world always let
the player walk through it, but `CourseReachability` kept its own list and the rail was not on it —
making the one block the mod draws paths with expensive to place anywhere the player also stands,
which is the exact opposite of what this section needs. Now passable, and tested.

**But the telegraph is always a physical object, never an overlay.** This is the part the first
implementation got wrong. A grinder's route is visible because the *track is really there*; a spiked
ball's arc is visible because the *chain is really there*; a fire bar's reach is the fireballs
themselves; a Thwomp is readable because it is an enormous face on a slab that pauses before it
drops. None of it is drawn on top of the world. The first pass built this rule out of particle
rings and glowing dotted lines, which said the right thing in the wrong language and looked like a
different game entirely. Corrected: physical telegraphs strengthened, drawn ones deleted.

---

## 2. Corrections to code already committed

Bugs in shipped work, not gaps.

**2.1 `backWall()` draws one room over and over.** A single ghost house runs three distinct wall
motifs — tall pointed gothic windows, diamond-check wallpaper, plain wooden boarding. Castles run
arcades, stained glass, brick and grate. One arch motif per course makes every indoor stretch look
like the same room repeated.

**2.2 `backdrop()` always draws something.** The ice caves draw *no background at all* — black with
faint mist — and the translucent cyan ice reads precisely because the void behind it is empty.
Drawing nothing is a third case alongside wall and skyline.

**2.3 `hill()` draws a sine profile for every exterior theme.** Grass is rounded; ice and snow are
angular crystalline cliffs; volcano is steep cones; desert is flat-topped pyramids. Rounded versus
angular is one of the clearest per-world reads in the reference.

**2.4 Aerial perspective desaturates toward grey.** ~~The current implementation cannot express a
warm key over the whole frame.~~ *Fixed.* The haze lived in the far-layer block palette, so it could
only ever reach things that were far away — which is why the entry says "foreground included".
`CourseAtmosphere` sets the fog **colour** per theme instead: hot orange over lava, pale blue over
snow, yellow dust in the desert, green-black in a ghost house, near-nothing underground so the empty
void behind cave terrain stays empty. Fog colour is Minecraft's own name for what colour the air is,
and it is mixed into the whole frame by the renderer without anything being drawn over the world.

Two deliberate limits. It blends 55% toward the key rather than replacing it, because a full
replacement reads as coloured glass and a partial mix reads as light in the air. And it never
touches view distance — that was settled when the underwater fog came out, and for the same reason:
tinting is a look, shortening the view is a handicap, and vanilla ships them in one object.

**2.5 ~~The custom fluids cannot fall.~~ The premise was false, and a gametest says so.** The entry
reasoned from the registration: `levelDecreasePerBlock` is 8, therefore a flowing block has no level
left, therefore nothing falls. But that property governs *horizontal* spread — vanilla handles
downward flow on a separate path — so reading the registration was never enough to know which way it
went.

`lava_fall_test` puts a source in the air over a floor, lets the server tick, and looks. The lava
falls. It has always fallen.

*What that changes:* the backdrop's column of source blocks is not a workaround for a broken fluid.
It stays anyway, and now for a stated reason rather than an assumed one — a poured column is
generator-visible geometry that `CourseReachability` can see, while a flowing fluid exists only at
runtime and the proof would be reasoning about a level that is not the level the player gets. The
right answer, arrived at for the wrong reason, which is worth writing down so nobody "fixes" it back.

*Method note:* this is the third plan entry to be wrong about **how** something is built rather than
about what it does, and the first to be settled by running the game instead of reading the code. The
unit suite cannot answer a physics question; `runGameTestServer` can.

---

## 3. Bosses

`Koopaling.java` was written before this reading and essentially none of its assumptions survived.

**3.1 The final boss is background-scale and attacks through depth.** It fills most of the screen,
stands behind the play lane, and reaches forward into it with fire, punches and grabs while the
player works across small platforms over lava. **The most 2.5D-native idea in the reference, and the
one this project is best placed to take** — the Z axis already exists and the fight is built on it.
~~Every boss in the mod is lane-sized and lane-bound.~~

*Built.* `SuperBowserEntity` stands in the backdrop and now **reaches into the lane** — the wiki's
phase two is dodging claws and fire, and the claw is a real lunge across the depth gap rather than
a hitbox that appears. The wind-up is three times the strike and the boss is visibly leaning out of
the backdrop through all of it, which makes the telegraph the same kind of thing as every other one
in this mod: a real movement of a real object.

Two constraints fell out of the staging. `BackgroundBossGoal` holds depth every tick, so it had to
be taught to let go — `ReachesIn` is one boolean, because the goal needs to know only whether to
keep its hands off, and where the boss is going is the boss's business. And the swipe tracks depth
only, never sideways: a claw that followed the player along the lane would be unavoidable, and the
whole point of a fixed reach is that stepping out of it works.

**3.2 ~~Bosses fly.~~ Wrong — checked against the wiki, not the footage.** In NSMB2 the Koopalings
fight **on the ground**, in a castle room: Roy charges, the walls close in, he stuns himself on
them, then fires magic from his wand. Three hits from jumps or ground pounds. `Koopaling` being
eight ground-walking mobs with `STOMPS_TO_DEFEAT = 3` was already correct, and closer to the
reference than the "fix" I built. A `ClownCarGoal` was written, shipped and reverted on this basis.

*What is actually missing*, now read from a one-frame-per-second pass over the World 1 castle
fight rather than from a six-second sample: the boss walks the floor of an arched hall, **retreats
into its shell and spin-dashes along the ground**, then emerges and walks again. Grey pillars stand
in the arena, which is what the wiki means by hiding behind pillars. Stomping is the answer, three
times.

*And the mod already has most of this.* `KoopaEntity` implements a shell state -- the renderer even
reads `inShell()` for it. A Koopaling spin-dash is that mechanic at boss scale and speed, not a new
one. The staging was never the gap and neither, it turns out, is the hard part of the moveset.

*The self-stun, built.* "He stuns himself on them" was the half of this entry with no implementation
at all, and it is the half that makes the fight a fight: a charge that misses ends in the wall and
leaves the boss helpless for two seconds, which is where the player's hit comes from. Without it the
player waits for a gap in an attack pattern; with it they *make* one by not being where the boss is
going. The shell dash stuns on its second wall too — a thing that cannot see where it is going, that
has already crossed the room once, running into something again — which also stops it rattling
between two walls forever.

**3.3 The clown car is a hazard on the walk-in, not a fight.** ~~Corrected against the wiki~~ —
corrected twice. The first version read a contact sheet as "this is how the Koopalings fight". The
second called it a set-piece boss encounter in World 6-Castle. The wiki says neither: World
6-Castle's boss is Bowser, twice over, and the clown car appears *during* that castle and the Star
castle, where the Koopalings ride it together and **flash its eyes to turn the player to stone,
avoided by passing between the castle's pillars**. It has no health and no defeat. The player gets
past it; they do not beat it.

That is a smaller thing to build than either wrong reading, and a better one: it is the only place
in the game whose verb is *get past* rather than *stomp* or *outrun*, which is exactly what earns
the last castle a shape of its own. *Built:* `ClownCarEntity` over the approach of the last world's
arena, resolving its flash with a single raycast so the rule the player learns is the rule the code
runs, with pillars on the walk-in as the cover.

**3.4 Bosses have phases, and the phase change is visible.** Written from frames as "ordinary,
then skeletal, then giant, with the fire changing colour"; the wiki gives the actual staging for the
final castle and it is simpler. Bowser is fought, dropped into the lava, and then **the Koopalings
use their wands to revive him as an enormous Super Bowser**, who is fought again. Two phases, and
the change is size.

*Built:* `SuperBowserEntity`, a second registration rather than a flag because `EnemyRigProfile`
ties visual scale to the registered hitbox. Hooked on death rather than a health threshold, so it
fires whether he is out-damaged or dropped by the bridge. Same mesh, same sheet, larger rig — the
transformation reads as size because size is the only thing that changed.

*Phase two's shape, built:* the last castle's finish staircase is made of donut blocks instead of
stone, so the climb to the pole falls away under the player while Super Bowser throws fire from the
backdrop. The mod already had the falling block and already had the staircase; the difference
between the last castle and the other four is what the player has to do on it, not how much of it
there is. Only the treads fall — the columns under them stay stone, or one missed step would take
the whole staircase with it.

*The ending, built:* the ledge Super Bowser stands on is ON/OFF blocks switched on, and an ON/OFF
switch sits beside the top tread. Reaching it turns the floor off under him. Two blocks the mod
already had, placed where they mean something, rather than a defeat mechanism written for one
fight — and the switch is only reachable by finishing the climb, so it is the ending rather than a
shortcut past it.

**3.5 The arena is its own room, and it is approached.** *Already true when written:* `BossArena`
builds an approach before the bridge, with a comment saying the castle used to be a corridor and
this is the room it was describing. Fifth entry found to overstate its gap. A long uniform arcade of repeated arches
runs in front of it — rhythmic, empty, visibly not the level you were just in. That corridor is
pacing, and it is most of what makes the arena land.

**3.6 Giant variants of ordinary enemies serve as set-piece threats.** Confirmed against the wiki:
the reference has Big Boos, Mega Cheep-Cheeps, Mega Deep Cheeps, Mega Fuzzies and Mega Piranha
Plants. The mod already shipped one of these without calling it that — `BigCheepEntity` is a Mega
Cheep-Cheep, and it already established the pattern the rest need.

*Built:* `BigBooEntity`, and it is placed rather than merely registered — the ghost-house climb's
last landing carries it, which is what that segment's own comment was already describing ("the
pressure builds with the height") without ever paying for it. Behaviour deliberately unchanged: a
Boo is already answered by looking at it, and a faster big one would be answering a question nobody
asked. What changes is that it fills the corridor.

*Also built:* `MegaPiranhaPlantEntity`, on the tall middle pipe of `piranha_pipes` — the segment
already varied its pipe heights so the row would not read as a fence, so the odd pipe out is where
the threat belongs and the shape of the segment says so before the player is close enough to see
it. Timings untouched: a plant is a metronome the player learns, and changing the beat would make
this a different enemy rather than a bigger one. Its rise had to be overridden, though, or its head
would have stayed inside its own pipe at full extension.

*The Fuzzy now exists* (§6.6's territory, built here): a ball of fuzz that shares the urchin's
brief — nothing you have works on it — and differs in the one way that matters, which is that it
travels. It has eyes for exactly the reason the urchin has none: eyes say the thing is going
somewhere, and the player must read a Fuzzy as an animal that will arrive and an urchin as terrain
in the way. A Mega Fuzzy would now be two lines, and is not built because nothing asks for one. The other three of the reference's four oversized enemies are done — Big Boo,
Mega Piranha Plant, Mega Deep Cheep — alongside the Mega Cheep-Cheep that shipped before the entry
was written.

---

## 4. Structure

**4.1 One course passes through several sub-environments.** *Built.* `CourseComposer` runs a second
`GenContext` over a middle span with its own theme, palette, cast and backdrop — two thirds of
courses descend, and not every one, because a transition that happens every time stops being an
event. The stretch is a cave most of the time and **a flooded descent one time in three**, which is
the other half of the entry's own wording and was the piece missing until now.

**4.2 Themes crossbreed.** Ghost house with ice-block platforms inside. Castle interiors flooded,
god rays raking down between the arches. Volcano levels set against sky. The theme list is not a
partition.

*Begun, by the cheapest route there is:* the sunken sub-environment above means a grass course can
have water in the middle of it. Water stops being a place the player travels to and becomes
something that happened to part of a level they were already in.

This broke an existing test, correctly. `WaterCourseTest` asserted "no other theme floods", which
was true when written and is exactly the partition this entry says does not exist. Narrowed to the
invariant that was really being protected — a dry course may have a wet *stretch*, but must never be
wet end to end, and must never flood its own spawn apron. A stretch is a change of scene; the whole
level underwater is a different level.

**4.3 A lava sea, not lava pits.** *Confirmed across worlds.* A continuous band across the whole
bottom, crossed on narrow bridges — and it emits: fireballs out of its own surface, vertical
geysers, horizontal jets from wall nozzles. Floor, fluid and room all emit.

*The sea is built.* One `lavaSea` pass lays it at a single depth for the whole course, four below
the lowest floor anywhere in it — a per-column depth would follow the terrain up and down, which is
a lava river with hills in it, and a liquid finds one level. It fills only what is still empty and
runs after the routes, so it can never replace deliberate geometry, and being under every walkable
surface keeps it out of the reachability proof entirely. A test asserts continuity rather than
uniformity: set-piece segments build raised lava channels of their own, the reference has those
too, and the finding was always about the bottom of the level rather than about every drop of lava
in it.

*The surface emits too.* Podoboos are now placed by the sea rather than by whichever segment
happened to include one, spaced about a screen apart, and only in columns with six blocks of clear
air above the lava — a fireball under a solid floor rises, hits the underside of the level and
falls back, having cost nothing and taught nothing. *Geysers and wall jets are built too*, as one `LavaJetEntity` with a facing — they are one idea, a
fixed place that is safe most of the time and lethal on a rhythm, and two entities would have meant
two clocks to keep in step. The wind-up is the design: the column grows over a fixed warm-up before
it does damage, and it stays drawn while withdrawing, so "visible" and "lethal" are deliberately
not the same window. The sea alternates them with Podoboos, because a moving threat the player
tracks and a fixed one they time stop being read at all if either is the only thing the lava ever
does. The wall nozzles are placed too, in `NOZZLE_CORRIDOR`: jets in short pillars they
visibly come out of, facing each other from either side at different heights, so the safe moment is
a *place* rather than a pause. Volcano only — a jet of fire out of a wall needs the wall to be part
of a volcano, or it is a flamethrower in a meadow. §4.3 is complete.

**4.4 Water level is a variable.** *Built.* `TideService` moves the surface of a pool up and down
one layer on a four-hundred-tick cycle. A pool at a fixed height is scenery the player swims through
once; a pool that rises is a clock, and everything in the room has to be read against it.

*The rule that makes it safe is the interesting part.* The tide runs at runtime, so
`CourseReachability` — which is run against the generated canvas — cannot see anything it does. The
only safe kind of invisible change is one that cannot break a route, so a layer is **only ever added
on top of water that is already there**. Water is passable and swimmable to the solver, so a column
that gains a layer stays crossable, and a column that loses one returns to exactly the geometry that
was proved. It also means the tide cannot climb out of its pool: the layer above dry land has no
water under it. A gametest asserts that, because it is a statement about blocks in a world and the
unit suite has no world.

*The original wording:* A flooded tower shows a surface line partway up the room that
*moves* during the level. The strongest vindication of having built the fluid as a fluid.

**4.5 Interiors are tinted by world.** *Built.* `Palette.forTheme(theme, world)` cuts a cave from
the rock of the world around it, and the enemy roster now follows the same rule —
`SegmentLibrary.cast(GenContext)` gives a snow cave, a volcano cave and a haunted flooded room their
own casts. Those were the last two places where the shared interiors collapsed six worlds into one.

**4.6 Terrain is masonry of mixed block sizes.** *The single most visible thing in the reference.*
Big slabs, half-slabs and squares in two alternating tones; the eye reads the wall, not the grid.

*Built for the castle block and the sandstone*, which is the wall the player sees most: `coursed_rubble` lays three
courses of unequal height, each split into stones whose widths sum to sixteen — that is what keeps
it seamless, since every row closes exactly at the tile edge while nothing inside repeats at the
same interval twice. Two tones per stone from a position hash, kept close together so the wall
reads as one material rather than as chequerwork. `masonry` stays for the brick and desert-brick blocks, where one module is correct — the difference
between a quarry and a brickworks is exactly whether the pieces come out the same size. Basalt and
deepstone keep their granular treatment: they are rock faces rather than built walls.

**4.7 Pipes are structural, and a colour set of at least five.** *Both halves are built now.*
`PIPE_LATTICE` builds a stretch out of plumbing rather than decorating it with plumbing: verticals
of three heights so the top edge is a skyline rather than a shelf, horizontals joining them, and
platforms in front so it is a place rather than a backdrop. Every pipe in it is scenery and none are
entrances — a wall where three of twelve pipes are doors teaches the player to test all twelve,
which is a chore rather than a puzzle. *The colour set was already built* —
all five exist as `WarpPipeBlock.Colour` with their own textures, and `SegmentLibrary.pipe()` leans
each theme on one of them so a pipe reads as belonging to the world it is in. What remains is the
structural half: whole levels built as pipe lattices rather than pipes as furniture.

**4.8 Platforms come from a parts kit.** *Built, all of it.* Post-and-beam scaffolding with a grate
deck, mushroom capsules on stalks, and — in `CAPSULE_BEAM` — a beam with rounded ends, thin ledges
with a lip to judge a landing against, and an ON/OFF switch on a pole. The pole is the part worth
arguing for: a switch flush in a wall is furniture the player walks past, and one at head height in
open air is visibly what the room is about. The beam's span is the ON/OFF blocks, so throwing the
switch removes the high road and leaves the ledges — both routes exist in the geometry at all times,
which is what keeps the reachability proof honest. *Partly true already:* semisolid platforms, pillars and
trim are registered and placed. What is missing is the specific shapes — capsule beams, mushroom
caps on stalks, thin ledges with inset centres. Post-and-beam scaffolding in open air, thin ledges with
dark inset centres, capsule beams, metal grate panels, pole-mounted switch blocks. Not all cubes,
and not all extruded from terrain.

*Scaffolding and grate panels are built.* `SCAFFOLD_SPAN` stands posts in the back row of the lane,
beams them across the top and decks the span with `COURSE_GRATE` — a raised walkway that is visibly
*built*, where every other elevated surface here is either extruded from terrain or floating with
nothing holding it up. The deck is see-through on purpose and it is the only floor in the mod that
is: a player on it can see the coins and the enemy underneath before deciding whether to drop off
the end, which turns "is there anything down there" from a gamble into a look.

*Mushroom capsules on stalks are built* — `MUSHROOM_STALKS`, and it needed no new block: a pillar
is a stalk and a semisolid platform is a cap. What it adds is a shape the library did not have, a
platform supported by a line rather than by terrain, so the space underneath stays open and getting
on top is a separate decision from the ground below. Semisolid caps specifically: a mushroom you
have to walk around to climb is a wall with a hat.

**4.9 Rope is walkable terrain, and it sags.** Cables strung between anchor posts, hanging in a
catenary and deforming under the player's weight. **The one finding that does not fit a block grid
at all** — soft geometry with its own collision, where the sag is the mechanic. Recorded as
expensive and probably out of scope, deliberately: the cheap version is a flat row of blocks that
looks like a rope and behaves like a floor, which is worse than not having it.

---

## 5. Art direction

**5.1 A block face is drawn by which way it points.** Green grass cap, pale brick body, teal
crystalline fringe below — three faces, three treatments, one block. Cliffs add a striated side face
and a surf band at the waterline; tower ice hangs icicles off the underside. **This is exactly the
up/down/east/west property set `ConnectedBlock` already declares**, currently used only to hide
seams on castle stone. Applying it lands across every theme at once.

**5.2 Sky is per-world data, not one gradient with a brightness knob.** *Built.* Eight skyboxes,
one per theme, each authored rather than tinted — `ThemeAssetTest` asserts every theme has one,
which is how the two missing ones (water and sky) were found. Water is drawn from *inside* the
water, with no horizon at all.

*The original wording:* At least six palettes:
pastel pink-lavender over snow; bright cyan with cumulus over airships; cream and gold over high
desert; saturated sunset orange with magenta bands; toxic green over volcanoes; night with a glowing
moon over teal terrain.

**5.3 A world is one hue family plus one or two rare accents.** *Confirmed hard by the gold
underground*, one olive-and-gold family across walls, ledges, pipes and terrain.

*Done, 2026-09-07.* Reached in three passes, all of them measured off the shipped PNGs rather than
argued from a colour table, because the palette lives in `BlockTextureGen.py` and the sheet is what
the player looks at.

- **Cave structure.** Structural accents and ledges use their world's existing sandstone, ice,
  basalt or ghost-beam material. Previously only floors/fill followed the world while these
  repeated surfaces reverted to shared brick/castle blocks.
- **Terrain.** `COURSE_CASTLE_BLOCK` measures hue 223 and was the floor of both the volcano and the
  ghost house; both now use their own rock and timber. Basalt was also retinted warm, 58,54,62 to
  62,48,46. Water's fill was `COURSE_DEEPSTONE` at hue 232 — cave rock under a grass lid, 176
  degrees across one silhouette — and is now dirt, which is what the entry's own "water reuses the
  land blocks" decision always implied.
- **Background and props.** The indoor back wall is the largest surface in an indoor course and was
  the last thing not following the world: its own comment said "in the room's own colour" while the
  code said castle stone for every theme. Ghost house now walls in its structural timber, the
  volcano in basalt. The other three wall materials were already in family and were left alone.
  The outdoor far layer is **exempt by design** — `distant()` hazes it toward the sky on purpose,
  so its hue is meant to leave the terrain family; that is aerial perspective, not a violation.
- **Pipes.** Reviewed and correct as they stood. Four sit in their world's family (desert 43/48,
  snow 203/215, lava 8/2), water's magenta 319 belongs to its coral accent 336, and the ghost-house
  magenta and grass-world green 113 are this entry's "rare accents" doing their job. Measurements
  recorded in `SegmentLibrary.pipe` so they are not taken again.

Guarded by `HueFamilyTest`, two cases, both verified by deliberate breakage.

*Two defects found while doing this, both recorded in code:* `COURSE_HEDGE_DISTANT_WARM` and
`COURSE_WOOD_DISTANT_WARM` are registered, drawn and unreachable, because the branch selecting them
fires on `LAVA` and `LAVA` draws a back wall rather than a skyline. And `SegmentLibrary.pipe` opened
by reading `Colour.values()` into an unused local under a comment claiming pipe colour came from the
course seed; neither the local nor the seeding ever did anything.

**5.4 Each fluid has its own surface treatment.** *Done for the fluids that exist.* Lava and water
each carry their own animated still and flow art, four distinct sheets. Tar and poison are not
missing *treatment* — they are missing fluids, which is content rather than art direction, and they
belong in §6 if they are ever wanted. Recorded rather than left looking unfinished.

*The original wording:* Four liquids, four different edges: lava has a
bright crust, water a clean ripple, tar hangs in drip lobes, poison grows pink crystalline spikes.
Whatever draws the top of a fluid has to be per-fluid art.

**5.5 Every theme carries one ambient particle.** Embers in castles, snowfall in ice, bubbles
underwater, drifting motes in caves. Static geometry plus one moving particle is most of what makes
a room feel alive. *Done, and the entry was wrong when written:* four themes already had ambience,
so the gap was three, not seven. Corrected by reading the code rather than the footage.

**5.6 Interiors are lit by visible sources.** Sconces, lanterns, glowing crystals set into walls,
stained glass. Flame colour is themed — ghost houses and towers burn green. *Half of this was
already true when written:* `COURSE_LAMP` existed and `CourseDecorator.lit()` places it in the two
dark themes, with a comment explaining that backlighting throws platforms into silhouette. Only the
themed colour was missing, and is now done. Stained glass in castle back walls is now built too: `COURSE_GLASS`, set high in the boss arena's back wall. High because a window at head height reads as a doorway, and a player must never spend a jump finding out it is not one.

**5.7 Background hills carry pattern.** *Built.* `hill()` bands its outline with chevrons —
mirrored about each mound's own centre, so the pattern follows the shape rather than ruling across
the screen behind it — and never on the top row, because a striped skyline edge is a dotted line and
the outline is what the shape is read by. Each mound's bands are phase-offset or a row of them
stripes in unison and the horizon becomes a fence.

`cloudBank()` adds the middle distance at `FAR_Z`, one step in front of the hills and one behind the
props. It is a depth cue rather than weather: without something between them, the backdrop is two
layers at no particular distance. Not in the sky theme, where the ground is already cloud and a bank
of it reads as more floor.

*The band needed its own block*, and that is the interesting part. It was first drawn in the distant
trunk material — already hazed to the right distance, no new asset. Two versions of the test then
passed with the striping switched **off**: the first was counting tree trunks, and the second, which
tried to tell a stripe from a trunk by its neighbours, was defeated by a tree's own crown. A pattern
that shares a material with the things it is drawn among cannot be told from them, by the build or
by the player.

**5.8 Foreground detail sits on the playfield itself.** *Built.* `groundCover` scatters tufts on the
lane floor — green in grass, frosted in snow, dry in the desert — sparsely and only where the floor
is the theme's own surface material, because grass on a girder is exactly the detail that makes a
level look generated. Caves and castles get none on purpose: a stone floor with tufts on it is a
stone floor somebody has neglected.

The blocks are non-solid *and* listed in `CourseReachability`'s passable set, which is the lesson
the rail already taught — a block the world lets you through and the proof does not is unusable in
the one place it is worth having.

*Water still gets none*, and the reason is recorded rather than glossed: `COURSE_CORAL` is a solid
block built for the layer behind the lane, so putting it on the floor walled the corridor and the
proof rejected 108 of 6000 courses. Coral on the playfield needs a non-solid fan of its own.

**5.9 Theme identity can ride on one block's face art.** *Built.* `COURSE_LOG_END` is the grass
world's platform material — cut logs seen end-on, two per tile because a single centred ring tiles
into a grid of bullseyes, mossed across the top. The grass world's raised surfaces used to be cloud,
which is the sky world's material and said nothing about a forest.

*The original wording:* Forest ground is stacked cut logs seen
end-on, with concentric growth rings and moss on top. The cheapest kind of identity there is.

**5.10 Bonus worlds have their own visual language, levels included.** *Built for the bonus room.*
`P_SWITCH_BONUS_ROOM` is now framed in a loud primary border and backed with flat bolted plates in
pastel pink and blue. Every other block in this mod is a material — stone, sand, timber, ice — and
these are deliberately manufactured, because that contrast is how the reference says "you have
stepped outside the game" without a word of text. The grain and mortar every other texture spends
its pixels on is exactly what these must not have; the bolts are the only detail, and they are what
stop a flat plate reading as an untextured error.

*Not built:* the abstract checkered world maps, which belong with §7.3's map work rather than here.

*The original wording:* The playfield is framed by a
chunky primary-coloured border and built from flat untextured plates with bolts, in pastel pink and
blue — nothing is a rock or a brick. Their maps are abstract checkered fields, each world taking its
own checker colour. That framing is what marks a bonus world as outside the game rather than as
another world in it.

---

## 6. Missing content

**6.1 Water — and it is cheaper than it looks.** The underwater levels reuse the *ordinary green
capped terrain block*, not a bespoke tileset. What makes them read as underwater is light shafts,
the fluid, coral props and the cast. The fluid exists; the block exists. This is theme, props and
cast. Multiple palettes observed (green-teal, pale blue-grey), and it combines with castle.

**6.2 Sky.** *Built.* Cloud is walkable terrain and the sky world's whole palette; mushroom capsules
on stalks exist as a segment and as that world's climax tower. `CLOUD_CLIMB` adds the third part —
a stretch whose *subject* is height, stepping upward the whole way with no ground coming back, and
nothing catching a fall. Height is only the subject if losing it costs something.

**6.3 Airships.** *The deck is built*, and it had been built before this session — `AIRSHIP_DECK`
was already there with a planked hull, a rising prow and stern, a rail, a mast and a cannon. What it
lacked was a sky: a hull with nothing under it is a wooden floor with a flag on it, so there is a
cloud sea in the far layer below it now, showing through the gaps at either end.

*Still missing:* the airship **world** — its own palette, skybox and course shape. The deck is the
part the player stands on and was worth having first. *The original wording:* The playfield is a
*vehicle* — a golden ribbed hull with upturned prow and stern
over a cloud sea, under a skull flag. Standard staging for a boss.

**6.4 Water cast.** *Mostly built already:* three fish and a skeletal reskin. The gap was not a
size or a species, it was a **behaviour** — every one of them patrolled, and a threat that never
approaches is one the player can wait out, which in water is fatal to the pacing because swimming
is slow. *Built:* `DeepCheepEntity`, the fish that follows. Same silhouette as the red one on
purpose, with the difference carried by palette and by the one detail the red sheet is emphatic
about not having — a scowl. Also `UrchinEntity`, which is the cast's first thing that cannot be
answered at all: fish are dodged by timing and the pursuing one by moving, and an urchin is
geometry that hurts. It drifts vertically rather than along the corridor, so what it closes is the
gap between floor and ceiling — a different shape of threat from anything else in the water. And
`MegaDeepCheepEntity`, which completes the reference's oversized four. Squid are built too: `BlooperEntity`, which is the water's third
movement idea after patrol and pursuit — it gathers, darts, then sinks helplessly while it
recovers, and that sinking is the beat the player swims through. Written as a two-state clock
rather than pathfinding, because a navigator would close the distance smoothly and produce a squid
with no timing in it. The cast is complete.

**6.5 Enemies reskin per world over identical behaviour.** Skeletal fish, Dry Bones, winged
variants. Cheap cast expansion off entities that already exist.

**6.6 Hazards the mod lacks.** ~~All six.~~ Five were already built or built since: the Thwomp is
the drop-crusher, and firebars, saws, chain balls and volcanic bombs all exist. *The last one, the
Chain Chomp, is now built* — and it is the one worth having for a reason the others do not cover.
Every threat in the mod is either a place (a Thwomp owns a column, a firebar owns a disc) or a
thing that travels. A Chomp is a **radius**: ground that belongs to it, whose edge the player can
see because the chain is drawn, and which is answered by *position* rather than by timing. Its
segment is wider than its reach and the coins are inside it, because a hazard with a free route
past it is a hazard nobody looks at.

**6.7 Traversal the mod lacks.** Climbable vines, barber poles and beanstalks all exist —
`COURSE_VINE`, `COURSE_CLIMB_POLE` and `SECRET_VINE`, which sprouts one. What was actually missing
was not terrain but **verbs**, and the modpack already has them: ParCool ships wall jump, wall
slide, cling, pole climb, zipline, vault and roll. Rebuilding those as blocks would be a worse copy
of a mod the player is already running.

*So the division is:* ParCool owns what the player can do, PlaneShift owns the terrain that asks
for it, and `ParCoolBridge` is the seam. It clears stamina inside courses (Mario has never had a
stamina bar, and a wall kick that fails because a meter emptied is a death the player cannot read)
and stands PlaneShift's own wall jump down when ParCool is present — that one is off by default
anyway, its own comment admitting it reads as a free double jump.

**6.8 Platforms that are enemies.** Ghost-house platforms are carried by Boos. A moving platform
does not have to be a block.

**6.9 Animated background that reaches in.** Volcanoes erupt and drop hazards onto the playfield.
Background and lane interact.

---

## 7. Interface

**7.1 Star coins gate nothing.** The reference gates its final world behind a count.

**7.2 Secret exits and cannons do not know about each other.** A secret exit is precisely what
unlocks a cannon between worlds.

**7.3 World maps are themed terrain.** *Built.* Each world's map draws its own field colour and its
own scenery: dunes, ice floes, volcano cones with a lit crater, bare trees, stalagmites, weed beds,
banked cloud — and, in the grass world, the two the entry names by name, houses and ponds. Those two
are the ones that say somebody lives here; clouds and trees say what every world's scenery says.

Placed from a fixed arithmetic sequence rather than a random source, so the map looks the same every
visit. A map whose furniture moves is one nobody can build a mental image of, and being picturable is
the whole argument for a map over a list of buttons.

*Not built:* the abstract checkered field for bonus worlds (§5.10's other half). There is no bonus
world in the registry to give one to — five named worlds, none of them outside the run — so the
field would be art with nowhere to draw it.

**7.4 Course and map are joined by an iris wipe** at the node just cleared. *Both halves are built
now.* The closing one lives in `CourseResultsScreen` rather than the HUD, which is what the entry
was waiting on — a `Screen` draws over the HUD, so the overlay had to be called by the screen
itself. Run backwards, and eased the other way round so the slow part of the movement stays at the
end: mirroring the curve as well as the direction would put the hesitation at the start of a close.

*The opening half*, paired with the title card: the course opens from a circle. Drawn as scanlines rather than
a mask texture — for each row the circle has a known half-width, so the black outside it is two
fills, needing no texture, shader or blend state, and it cannot be stretched into an oval. Eased
out, because a circle growing at a constant rate covers area at an accelerating one. The closing
half, onto the map node, is not built: the results screen is a `Screen` and the HUD does not draw
under it, so it needs a different home.

**7.5 Levels open on a title card** naming world and level. *Built.* An overlay rather than a
screen -- a screen would pause input and release the mouse, turning a half-second flourish into a
dialog to dismiss. The packet carries the two display strings rather than a course id, which
settles the question the backlog was holding open: the client would otherwise need its own copy of
the world table, and the failure mode of two tables disagreeing is a blank card nobody notices.

**7.6 Levels carry signposts** — arrow boards planted in terrain where a route is ambiguous.
Navigation as set dressing rather than UI. *Built*, in the spawn apron: the one place in a
generated course where guidance is unambiguously wanted, since the player has just arrived and
nothing on screen yet says which way the level runs. Deliberately **not** used at secret entrances
— `signpostSecret` leads the eye there with a rising coin trail instead, and an arrow at a secret
tells the player there is a secret, which is a different and worse job.

**7.7 Progress is announced.** *Half was already built:* the final-world banner draws on the
results screen. The other string, `banner.all_cleared`, had been sitting in the language file with
nothing able to say it. *Now built* — a small `AnnouncementPayload`, separate from the results
packet because that one already fills all eight `StreamCodec.composite` slots and a ninth field
would mean a hand-written codec. Latched on the client and taken once when the results screen is
built, so finishing the game says so on that screen and never again.

**7.8 Clearing a castle plays a scene**, and the credits roll over a *playable* level. *The scene is
built.* The mod had the Toad dialogue without the moment that frames it — he simply started talking
over the results screen. A title card now names the castle and holds for a beat first, which is the
difference between an event and a notification, and the last world's card reads differently from the
other four.

*Not built:* the playable credits. That is a course that runs itself with the roll over it, which
means a camera path, an input lock the player can break out of, and a course built to be watched
rather than played — a set piece, not a screen. Recorded rather than half-done.

**7.9 Shops are architecture, not menus.** *Built.* The Toad house is gold-walled now and its back
wall carries wide arches with the sky behind them. The openings are the entry: a sealed box with
three boxes in it is a menu with a floor, and what makes this read as a building the player has
walked into is being able to see out of it. Wide rather than tall — three across and four high is a
doorway you could walk through, where a one-block slot is a window, and a window says the room is
somewhere you are being kept. The arches sit *between* the boxes, because an opening behind one puts
bright sky behind the thing the player has to read.

*The original wording:* A warm gold room with wide arches and open sky behind the
openings — bright and welcoming, built nothing like the level interiors on either side.

---

## 8. Ordering

**The original ordering is spent.** All eight items on it are built: the background boss (3.1),
per-face block treatment (5.1), the shipped-code corrections (2.1–2.5), trajectory rendering (§1),
world-tinted interiors (4.5), water (6.1), sub-environments (4.1) and the progression links
(7.1, 7.2). Sections **1, 2 and 3 are complete**. What follows replaces it.

### Status by section

| Section | State |
|---|---|
| 1 — the trajectory rule | **Complete.** Five forms, all built as geometry. |
| 2 — corrections | **Complete.** 2.5 turned out to be a false premise; a gametest says so. |
| 3 — bosses | **Complete.** |
| 4 — structure | **Complete.** 4.9 (sagging rope) out of scope by decision, recorded below. |
| 5 — art direction | **Complete**, except the bonus-world *maps*, which are §7.3's work. |
| 6 — missing content | **Complete**, except the airship *world* (palette, skybox, course shape); its deck is built. |
| 7 — interface | **Complete**, except the playable credits roll in 7.8 and the bonus-world map in 7.3 — both recorded with reasons. |

### What to do next, and why in this order

*Done since this list was written: 5.3 (hue families) and 5.7 (hill pattern and cloud bank).*

1. **4.8 — the rest of the parts kit.** Post-and-beam scaffolding, thin ledges with inset centres,
   capsule beams, grate panels. Each is a segment or a block, none of them interact, and they can be
   done one at a time and shipped one at a time.
2. **7.9 — the shop as architecture.** `ToadHouseRoom` exists and is a wooden box; the reference's is
   a warm gold room with wide arches and open sky behind them. One room, hand-built, high payoff for
   how the game reads between courses.
3. **7.4 — the closing half of the iris.** The opening is built. The closing one needs a home,
   because the results screen is a `Screen` and the HUD does not draw under it.
4. **4.4 — water level as a variable.** The most interesting thing left, and the most expensive: it
   needs runtime block editing over a marked band and a story for what the reachability proof is
   told about a floor that moves.
5. **6.3 — airships.** A whole theme: hull geometry, a cloud sea, a skull flag. Large, self-contained,
   and the last big *content* gap.
6. **7.3 — themed world maps** and **7.8 — the castle scene and credits level.** Presentation work
   that only pays once the rest reads well.

### Two things that are recorded rather than built

- **4.9 rope that sags.** Deliberately out of scope: soft geometry with its own collision, where the
  sag is the mechanic. The cheap version — a flat row of blocks that looks like rope and behaves
  like floor — is worse than not having it.
- **Coral on the playfield** (§5.8). `COURSE_CORAL` is solid and built for the layer behind the lane;
  putting it on the floor walled 108 of 6000 courses. Wants a non-solid fan block of its own.

---

## What this plan cannot tell you

None of it has been played. Every item is derived from footage of a different game and from static
analysis of this one. The reference says what a good version of this looks like; it cannot say
whether *our* jump arc makes a given gap fair, whether Thwomps over ice are readable at our camera
distance, or whether the boss fights are fun. That still needs a controller.
