# Work plan, from the reference footage

Built from a sampled walkthrough recording (4h17m at one frame per six seconds, 108 contact
sheets), the owner's screenshots, and the wiki page for that game. Observations are ours, in our
own words, and feed original generated art and original code.

**Method note.** Twenty-two of the 108 sheets read so far, spread to cover every world and level
type; more being worked through. An earlier draft of this plan was written off five sheets and
called complete, which was too thin a base for the word. Items below are marked *confirmed* where a
later sheet independently repeated an earlier read.

---

## Already done from this reference

| Finding | Commit |
|---|---|
| Far layer is paler and flatter — aerial perspective | `338a9dd` |
| Themes were built from vanilla blocks | `cad390c` |
| Interiors get a wall, exteriors get silhouettes; third depth band | `6dd16bc` |
| Lava composition from a generated pixel tile | `e5f1ce1` |

---

## A. Structural — how a level is put together

**A1. One level passes through several sub-environments.** World 2-1 runs outdoor desert, then a
dark interior section, then back outdoors to the flagpole. `CourseComposer` builds a single theme
for a whole course. This is the largest structural gap found and it changes what a "theme" is: a
course wants an *ordered sequence* of environments, not one.

**A2. A lava sea, not lava pits.** *Confirmed on a second world.* Castle levels run a continuous
lava band across the entire bottom of the screen with a bright crust line, crossed on narrow
bridges, and it **throws fireballs upward out of its own surface**. The hazard is not only the
floor, it is an emitter. World 6 levels sit above a continuous lava band spanning the
whole level with a bright crust line along its top edge. The mod places lava in discrete pits. A
floor of hazard under the entire course is a different tension and a different failure state.

**A3. Interiors are tinted by their world.** Desert caves are dark ochre; volcano interiors are
brown-grey. `UNDERGROUND` is one grey theme used everywhere, collapsing six distinct interiors into
one.

**A5. Silhouette shape is per theme, and this contradicts what was built.** Grass uses rounded
hills; ice and snow use angular crystalline cliff shapes; volcano uses steep cones. `CourseDecorator
.hill()` currently draws a sine profile for every exterior theme, which is right for grass and wrong
for the other two. The rounded-vs-angular distinction is one of the clearest per-world reads there
is.

**A6. Snow sits on top of blocks as a separate cap.** Ice levels show a white cap band on the upper
face of otherwise blue-grey blocks, independent of the block itself. This is a per-block top
treatment, not a different block.

**A7. Pipes are structural.** A whole level is built as a lattice of pipes forming the walkable
geometry. The mod's pipes are decorative or fake by design decision.

**A8. Blocks are a body plus a contrasting cap row.** Green tower platforms carry an orange top
band; rock ledges carry a grass-and-tuft top band; ice carries a snow band. A6 recorded this as a
snow behaviour, but it is general: the top face of a platform is drawn differently from its body in
every theme. Cliff terrain goes further and gives the *side* a third treatment again: green top,
horizontally striated pale rock face, white surf band where it meets the sea. So a block face is
drawn by which way it points — top, side, buried — not by which block it is.
This is `ConnectedBlock` work — the cap is exactly the up-neighbour case the property
set already models, and it would land across every theme at once.

**A9. Every theme carries an ambient particle.** Castles drift embers, snow levels drift flakes,
underwater drifts bubbles. Static geometry plus one moving particle is most of what makes these
rooms feel alive; the mod's rooms are entirely still.

**A10. A course passes through a cave and back out.** *Confirmed again.* The mountain level drops
into an unlit cave mid-course and returns to the sky before the flagpole. This is A1 seen a second
time in a different world, which promotes it from an observation to a pattern.

**A11. Water level is a variable, not scenery.** A flooded tower has a visible surface line
partway up the room, dry stone above and submerged rock below, and the line *moves* during the
level. This is the strongest argument yet for having built the fluid as a fluid: a hazard block
cannot do it.

**A12. There is a post-and-beam construction kit.** Spotted vertical posts carrying horizontal
beams, assembled as scaffolding in open air rather than stacked up from the ground. Platforms come
from a parts kit, not only extruded from terrain.

**A13. An interior uses several wall motifs, not one.** A single ghost house runs tall pointed
gothic windows, a diamond-check wallpaper, and plain wooden boarding in different rooms.
`backWall()` draws one arch motif for the whole course, so every indoor stretch looks like the same
room repeated.

**A14. Platforms tilt.** Several are drawn at an angle and pivot under the player. Everything the
mod places is grid-aligned and level.

**A15. Airship levels: the playfield is a vehicle.** Golden ribbed hulls with upturned prow and
stern, floating over a cloud sea, with a skull flag flying. The ground is a shaped object with its
own silhouette rather than terrain, and it is the classic staging for a boss. Nothing in the mod is
shaped like this.

**A16. There is a pre-boss corridor.** A long uniform arcade of repeated arches and pillars leads
into the boss room — rhythmic, empty, and visibly not the level you were just in. It is
pacing, and it is what makes the arena land.

**A4. Terrain is built from mixed block sizes.** Ground reads as masonry of varying rectangles, not
a uniform 1×1 grid. `ConnectedBlock` already exists and currently serves only castle stone.

---

## B. Missing content

**B1. Water.** A whole level type with its own palette, cast and movement. `hasUnderwaterStage` has
had zero callers for the life of the codebase; the footage settles that question. The fluid now
exists (`43678fd`), so this is theme + generation + cast, not plumbing. What the footage adds:
underwater has **light shafts** raking down from the surface, coral and weed on the floor, drifting
bubbles, and -- where a level is half-submerged -- a **visible surface line** with open air above
it. Swimming is a movement mode, not a slower walk.

**B2. Sky.** Cloud platforms, pale palette, height as the subject.

**B3. Water cast.** Fish enemies of at least two sizes. Blocked on B1.

**B5. Giant enemy variants.** A scaled-up version of an ordinary enemy used as a set-piece
threat. The mod has one size per enemy.

**B6. Bosses fight in their own arena.** The Koopaling room is a separate space with its own
wall treatment, its own platform set and a lit window as its back marker -- entered by door, not
walked into. Ours fight wherever the course happens to end.

**B7. Bosses fly.** The Koopaling rides a hovering vehicle and attacks from above, dropping hazards.
`Koopaling` is written entirely as a ground-walking entity with eight ground attacks.

**B8. Moving platforms rotate.** Platforms are mounted on spinning arms around a hub, not only
sliding along a line.

**B9. Vertical climbables.** Vines and stalks that are climbed rather than jumped.

**B10. Enemies emerge from pipes.** A pipe is a spawner as well as a passage.

**B11. Enemies have per-world reskins.** The underwater fish appears as a skeleton variant in the
flooded tower; ground enemies appear winged in sky levels. Same behaviour, different world,
different sprite — cheap cast expansion off entities that already exist.

**B12. Some platforms are enemies.** Ghost-house platforms are carried by Boos: the thing you
stand on is a mob, and it behaves like one. A moving platform does not have to be a block.

**B13. Swingable ropes.** A hanging rope the player grabs and swings on, drawn as an arc.
Traversal that is neither walking nor jumping.

**B14. Drop-crushers.** Heavy stone faces that hang above the lane and slam down when passed under.
The mod has static spikes, which threaten a place; these threaten a moment.

**B4. Animated background elements.** Volcanoes erupt; background is not static. All mod scenery is
static blocks.

---

## C. Art direction

**C1. A world is one hue family plus one or two rare accents.** The desert set is almost entirely
warm ochres — ground, dunes, distant structures, cave interiors — with a green pipe and blue sky as
deliberate exceptions. Audit each theme for hue discipline.

**C2. Remaining skyboxes.** Five themes still carry the old generated art. Blocked on the ChatGPT
composer refusing long prompts reliably; procedural fallback is viable since skies are mostly
gradient.

**C3. Connected textures beyond castle stone.** Terrain, brick and ice all want edge treatment.
The machinery is built and proven.

**C5. Foreground detail sits on the playfield itself.** Flowers and tufts are scattered on the
walkable surface, not only behind it. Every decorator prop is placed behind the lane.

**C6. Sky levels come in more than one palette, and none of them is the grass sky lightened.**
Recorded first as "the sky world is pastel" off a snow-sky level — pink, lavender and mint
over a heavily blurred far layer. The airship world is the other case: bright cyan with large white
cumulus and a very pale low-contrast horizon. Both are strongly aerial; neither is the day sky with
the brightness pushed up.

**C7. Pipes are a colour set.** Green, yellow, blue and red pipes appear in one level and read as
different objects. Ours are green.

**C8. A cloud bank sits between terrain and far hills.** On mountain levels the peaks are rooted in
a white haze band rather than meeting the ground plane. Cheap, and it is most of why the far layer
reads as far.

**C9. Bonus worlds have their own visual language, levels included.** Not only the checkered map
of D4: the playfield itself is framed by a chunky primary-coloured border, and the blocks are flat
untextured plates with bolts, in pastel pink, blue and yellow. Nothing is a rock or a brick. That
framing is what makes a bonus world read as outside the game rather than as another world in it.

**C10. Interiors are lit by embedded sources.** Small glowing crystals and lamps set into the wall,
so the light in a dark room visibly comes from somewhere. Ours are uniformly dim.

**C11. The ghost theme has an exterior, and it is purple twilight.** The course leaves the house
and finishes outdoors under a violet sky with bare dead trees as silhouettes and pale ground. Ours
treats ghost house as indoor-only, so this palette does not exist anywhere in the mod.

**C12. The volcano look is columns behind, arches in front.** Background is a wall of vertical
basalt columns with lava seams glowing between them; the playfield is grey stone arch viaducts
crossing above the lava. Two very specific layers, and neither is what the lava theme draws now.

**C13. Flame colour is themed.** Ghost-house sconces burn green, not orange.

**C4. Snow and the two lit themes still draw the same prop at both depths.** They use different
decorator builders and never got the aerial-perspective treatment.

---

## D. Interface

**D1. Star coins gate nothing.** The reference gates its final world behind a star-coin count. The
mod tracks them and gates nothing.

**D2. Secret exits and cannons do not know about each other.** Both exist here as separate
features; in the reference a secret exit is precisely what unlocks a cannon between worlds.

**D3. World-map decoration.** Map screens carry scenery — trees, buildings, terrain — around the
node graph. Ours draws nodes and paths on a flat field.

---

**D4. World maps are themed terrain, and special worlds break the pattern deliberately.** Each
world's map is built from that world's own materials with scenery around the node graph; the bonus
worlds use an abstract checkered field instead, which is what marks them as outside the sequence.

**D6. Level and map are joined by an iris wipe.** A circular iris closes on the course and opens
on the map at the node you just cleared, which is what ties the two screens together as one place.
Ours cuts.

**D7. Clearing a castle plays a scene.** A lit room, the rescued character, an ending beat before
the map returns. The castle is the only course that resolves anything.

**D5. A level is introduced by a title card.** A plain black screen naming the world and level,
between the map and the course. Ours cuts straight in.

## E. Ordering

Grouped by what unblocks what, not by size.

1. **A3 — tint interiors by world.** Cheapest real win. One theme becomes six without new art.
2. **C4 — finish aerial perspective** for snow, lava, ghost house. Completes work already started.
3. **A8/C3 — connected terrain and cap rows.** Machinery exists; this is application, and the
   cap row is the single change that touches every theme at once.
4. **A1 — sub-environments within a course.** The big structural one. Needs composer work and a
   reachability re-proof across environment joins.
5. **B1/B2 — water and sky themes.** Largest content addition; water is now unblocked.
6. **A2 — lava sea** as a course variant.
7. **D1/D2 — progression links.** Small, self-contained, high payoff for how the game reads.

---

## What this plan cannot tell you

None of it has been played. Every item is derived from footage of a different game and from static
analysis of this one. The reference says what a good version of this looks like; it cannot say
whether *our* jump arc makes a given gap fair, whether Thwomps over ice are readable at our camera
distance, or whether the boss fights are fun. That still needs a controller.
