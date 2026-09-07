# Reference notes

Design notes taken from three sources: screenshots the project owner supplied, a sampled
walkthrough recording (4h17m, sampled at one frame per six seconds — see `tools/VideoFrames.py`),
and the Super Mario Wiki page for the game the footage is from.

These are observations and conclusions in our own words, written to inform original generated art
and original code. Nothing here is copied from the source material, and nothing generated from
these notes should be either — every texture and model in this mod is drawn from primitives by a
generator, and that stays true.

---

## What the reference validates

Worth recording, because it is easy to assume everything differs and then rebuild things that were
already right.

| Structure | Reference | PlaneShift |
|---|---|---|
| Levels per world | 10 | 10 |
| Star coins per level | 3 | 3 |
| Star coins per world | ~30 | 30 |
| Bosses per world | 2 — one tower, one castle | 2 — `KoopalingTower`, `BossArena` |
| Tower position | Midway through the world | Middle course, computed |
| Toad houses | 3–5 per world | Present |

The two-boss shape was arrived at independently and matches. The tower-then-castle escalation is
the reference's structure as well as ours.

---

## Art direction

### 1. Aerial perspective is the depth cue — done

The far background layer is visibly **paler and flatter** than the mid layer, and the playfield is
the most saturated thing on screen. The eye reads distance from that far more than from position
or size.

The mod placed props at depth 2 or 3 and drew the identical block at either, so depth existed in
the geometry and not in the image. Fixed in `distant()` / `COURSE_*_FAR`. Two limits remain: the
haze tint is one neutral blue rather than each theme's own sky, and snow plus the two lit themes
still draw the same block at both depths.

### 2. A world is one hue family plus one or two accents

The desert set is almost entirely warm ochres — ground, dunes, distant structures and even the
cave interiors sit in one hue family. The strong contrasts are deliberate and rare: a green pipe,
the blue sky. This is what makes a world read as a *place* rather than as a set of blocks that
happen to be nearby.

**Action:** audit each theme's palette for hue-family discipline. Suspect several currently mix
hues freely.

### 3. Caves are tinted by their world, not universally grey

Desert cave sections are dark **ochre**; other worlds' interiors carry their own world's hue. The
mod has a single `UNDERGROUND` theme used everywhere, which flattens six distinct interiors into
one grey one.

**Action:** either tint `UNDERGROUND` by the world it sits in, or fold "interior" into each theme
as a variant rather than a separate theme.

### 4. Three background layers, not two

Consistently: far silhouettes, mid-ground shapes, near props — at three distinct depths and three
distinct saturations. `CourseDecorator` has `NEAR_Z = 2` and `FAR_Z = 3`, one usable band.

### 5. Terrain has a capped top edge

Ground reads as a body with a distinct surface band on top, and vertical faces get an edge
treatment where the material ends. This is what `ConnectedBlock` was built for; it currently only
serves castle stone.

---

## Content gaps

### Themes the mod has no equivalent for

| Reference world | Character | PlaneShift |
|---|---|---|
| Archipelago / islands | Water, beaches, forest | **missing** |
| Sky | Cloud platforms, pale palette, height | **missing** |

The mod's six themes are grass, desert, snow, lava, underground and ghost house. The reference's
six main worlds are grass, desert, archipelago, snow, sky and lava/haunted — so underground and
ghost house are level *types within* worlds there, and water and sky are whole worlds we lack.

**Underwater is the bigger of the two**, and it settles a standing question:
`WorldDefinition.hasUnderwaterStage` has existed as dead code with zero callers for the life of the
project, and `docs/BACKLOG.md` item 1 lists "build it or delete it" as a decision needing the owner.
The footage answers it — water levels are a substantial, recurring level type with their own
palette, their own enemies and their own movement. It is a real feature that was never built, not
speculative scope.

### Enemies with no equivalent

- **Cheep Cheep** and larger fish variants — the entire water cast. Blocked on the water decision.
- **Sledge Bro** — the mod has Hammer, Fire and Boomerang Bros but not the heavy one.
- **Gold enemy variants** — a coin-yielding version of a normal enemy, appearing across all worlds.

### Progression links that exist separately but are not connected

The mod has secret exits (`KeyholeBlock`) and it has map cannons (`MapNodeService`). In the
reference these are one mechanism: a secret exit is what *unlocks* a cannon, and a cannon is what
skips you between worlds. Ours are two features that do not know about each other.

The reference also gates its final world behind a star-coin count. The mod tracks star coins and
gates nothing on them.

---

## Method note

Frame-by-frame is not achievable and would not help: at 60 fps consecutive frames are near
identical, so the information density is very low. 925,645 frames exist in the source; the useful
unit is a *sheet* of frames spaced far enough apart to show change. Coarse pass to find what
matters, dense pass on the sections that do — `tools/VideoFrames.py` has both.
