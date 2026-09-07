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

**2.4 Aerial perspective desaturates toward grey.** The volcano tints the *whole scene* warm orange,
foreground included — the air itself is hot. The current implementation cannot express a warm key
over the whole frame.

**2.5 The custom fluids cannot fall.** `ModFluids` sets `levelDecreasePerBlock` to 8 so pools stay
where the generator puts them, and lava visibly pours down cliff faces into the sea below. A column
of source blocks looks right and costs nothing, but it is a workaround for a property chosen for
other reasons, not the fluid behaving like a fluid.

---

## 3. Bosses

`Koopaling.java` was written before this reading and essentially none of its assumptions survived.

**3.1 The final boss is background-scale and attacks through depth.** It fills most of the screen,
stands behind the play lane, and reaches forward into it with fire, punches and grabs while the
player works across small platforms over lava. **The most 2.5D-native idea in the reference, and the
one this project is best placed to take** — the Z axis already exists and the fight is built on it.
Every boss in the mod is lane-sized and lane-bound.

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

*Not built:* Mega Deep Cheep and Mega Fuzzy. The Fuzzy does not exist in the mod at any size, so it
belongs to §6 rather than here.

---

## 4. Structure

**4.1 One course passes through several sub-environments.** *Confirmed four times across different
worlds.* Outdoor to cave to outdoor; or dry ledge, then a descent into water. `CourseComposer`
builds one theme per course. The largest structural gap, and it changes what a theme is: a course
wants an *ordered sequence* of environments.

**4.2 Themes crossbreed.** Ghost house with ice-block platforms inside. Castle interiors flooded,
god rays raking down between the arches. Volcano levels set against sky. The theme list is not a
partition.

**4.3 A lava sea, not lava pits.** *Confirmed across worlds.* A continuous band across the whole
bottom with a bright crust line, crossed on narrow bridges — and it emits: fireballs out of its own
surface, vertical geysers, horizontal jets from wall nozzles. Floor, fluid and room all emit.

**4.4 Water level is a variable.** A flooded tower shows a surface line partway up the room that
*moves* during the level. The strongest vindication of having built the fluid as a fluid.

**4.5 Interiors are tinted by world.** *At least five distinct tints* — green towers, ochre desert
caves, blue-green flooded towers, brown-grey volcano, purple ghost house. `UNDERGROUND` is one grey
theme collapsing all of them.

**4.6 Terrain is masonry of mixed block sizes.** *The single most visible thing in the reference.*
Big slabs, half-slabs and squares in two alternating tones; the eye reads the wall, not the grid.

**4.7 Pipes are structural, and a colour set of at least five.** Whole levels are built as pipe
lattices. Green, yellow, blue, red and magenta read as different objects. They also spawn enemies.

**4.8 Platforms come from a parts kit.** *Partly true already:* semisolid platforms, pillars and
trim are registered and placed. What is missing is the specific shapes — capsule beams, mushroom
caps on stalks, thin ledges with inset centres. Post-and-beam scaffolding in open air, thin ledges with
dark inset centres, capsule beams, mushroom capsules on stalks, metal grate panels, pole-mounted
switch blocks. Not all cubes, and not all extruded from terrain.

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

**5.2 Sky is per-world data, not one gradient with a brightness knob.** At least six palettes:
pastel pink-lavender over snow; bright cyan with cumulus over airships; cream and gold over high
desert; saturated sunset orange with magenta bands; toxic green over volcanoes; night with a glowing
moon over teal terrain.

**5.3 A world is one hue family plus one or two rare accents.** *Confirmed hard by the gold
underground*, one olive-and-gold family across walls, ledges, pipes and terrain.

**5.4 Each fluid has its own surface treatment.** Four liquids, four different edges: lava has a
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
themed colour was missing, and is now done. Stained glass in castle back walls remains open.

**5.7 Background hills carry pattern.** Rounded mounds with chevron and zigzag striping, not flat
silhouettes. A cloud bank often sits between terrain and far hills.

**5.8 Foreground detail sits on the playfield itself.** Flowers, tufts, fences and coral on the
walkable surface, not only behind it. Every decorator prop is currently placed behind the lane.

**5.9 Theme identity can ride on one block's face art.** Forest ground is stacked cut logs seen
end-on, with concentric growth rings and moss on top. The cheapest kind of identity there is.

**5.10 Bonus worlds have their own visual language, levels included.** The playfield is framed by a
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

**6.2 Sky.** Cloud as solid walkable terrain, mushroom capsules on stalks, height as the subject.

**6.3 Airships.** The playfield is a *vehicle* — a golden ribbed hull with upturned prow and stern
over a cloud sea, under a skull flag. Standard staging for a boss.

**6.4 Water cast.** *Mostly built already:* three fish and a skeletal reskin. The gap was not a
size or a species, it was a **behaviour** — every one of them patrolled, and a threat that never
approaches is one the player can wait out, which in water is fatal to the pacing because swimming
is slow. *Built:* `DeepCheepEntity`, the fish that follows. Same silhouette as the red one on
purpose, with the difference carried by palette and by the one detail the red sheet is emphatic
about not having — a scowl. Urchins and squid remain.

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

**7.3 World maps are themed terrain.** Built from each world's own materials with dense scenery —
trees, houses, ponds, volcano cones, ice floes — around the node graph. Ours draws nodes and paths
on a flat field.

**7.4 Course and map are joined by an iris wipe** at the node just cleared.

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

**7.8 Clearing a castle plays a scene**, and the credits roll over a *playable* level.

**7.9 Shops are architecture, not menus.** A warm gold room with wide arches and open sky behind the
openings — bright and welcoming, built nothing like the level interiors on either side.

---

## 8. Ordering

Grouped by what unblocks what, and by cost against payoff.

1. **3.1 — the boss in the background plane.** No new terrain, no new theme, no new art pipeline.
   Move the boss off the lane and give it reach. The one thing this project has that a 2D reference
   does not.
2. **5.1 — per-face block treatment.** The machinery exists and is unused. One change, every theme.
3. **2.1 to 2.5 — fix the shipped code** while the findings are fresh.
4. **1 — trajectory rendering.** One shared concern; do it before adding the hazards in 6.6, not
   after.
5. **4.5 — tint interiors by world.** One theme becomes six without new art.
6. **6.1 — water.** Much cheaper than previously scoped.
7. **4.1 — sub-environments within a course.** The big structural one. Needs composer work and a
   reachability re-proof across environment joins.
8. **7.1 and 7.2 — progression links.** Small, self-contained, high payoff for how the game reads.

---

## What this plan cannot tell you

None of it has been played. Every item is derived from footage of a different game and from static
analysis of this one. The reference says what a good version of this looks like; it cannot say
whether *our* jump arc makes a given gap fair, whether Thwomps over ice are readable at our camera
distance, or whether the boss fights are fun. That still needs a controller.
