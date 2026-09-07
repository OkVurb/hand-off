# Work plan, from the reference footage

Built from a sampled walkthrough recording (4h17m at one frame per six seconds, 108 contact
sheets), the owner's screenshots, and the wiki page for that game. Observations are ours, in our
own words, and feed original generated art and original code.

**Method note.** Roughly a dozen of the 108 sheets read so far, spread to cover every world and
level type; more being worked through. An earlier draft of this plan was written off five sheets and
called complete, which was too thin a base for the word.

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

**A2. A lava sea, not lava pits.** World 6 levels sit above a continuous lava band spanning the
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

**A4. Terrain is built from mixed block sizes.** Ground reads as masonry of varying rectangles, not
a uniform 1×1 grid. `ConnectedBlock` already exists and currently serves only castle stone.

---

## B. Missing content

**B1. Water.** A whole level type with its own palette, cast and movement. `hasUnderwaterStage` has
had zero callers for the life of the codebase; the footage settles that question. The fluid now
exists (`43678fd`), so this is theme + generation + cast, not plumbing.

**B2. Sky.** Cloud platforms, pale palette, height as the subject.

**B3. Water cast.** Fish enemies of at least two sizes. Blocked on B1.

**B5. Giant enemy variants.** A scaled-up version of an ordinary enemy used as a set-piece
threat. The mod has one size per enemy.

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

**C6. The sky world is pastel, not blue.** Pink, lavender and mint rather than a brighter day sky,
with a very soft low-contrast far layer. A sky theme cannot be the grass sky lightened.

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

## E. Ordering

Grouped by what unblocks what, not by size.

1. **A3 — tint interiors by world.** Cheapest real win. One theme becomes six without new art.
2. **C4 — finish aerial perspective** for snow, lava, ghost house. Completes work already started.
3. **C3 — connected terrain.** Machinery exists; this is application.
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
