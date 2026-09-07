# Mods worth having in `run/mods/`

Researched September 2026 against NeoForge 1.21.11, which is the constraint that rules most things
out — a lot of well-known mods simply have no build for it.

**Installed and verified** (7 September 2026). All four load together with PlaneShift on NeoForge
1.21.11, client reaches the render thread, zero asset warnings.

Originally written as a shortlist before install; kept in that form because the reasoning for each
choice is the useful part. `docs/DEPENDENCY_POLICY.md` says third-party jars go into run profiles by
hand, and downloading executables is not something to do on a guess. This is a shortlist to pick
from.

## Worth it for developing this mod

**Jade** — shows what block or entity you are looking at, live. Directly useful here: this mod
generates courses procedurally from a large block set, and "which of my 38 blocks is this" is a
question I currently answer by reading generator source. Actively maintained, 21.1.3 for NeoForge
1.21.11 published March 2026.

**Sodium** — the performance mod, and the only one of its kind still shipping for this version.
0.8.11 for NeoForge 1.21.11, May 2026. Relevant because courses are long and this mod fills them
with entities; if generation ever tanks the frame rate, it matters whether that is our fault or
vanilla's, and Sodium is the control.

**JEI** — an item and recipe browser. The reason it earns a slot here rather than in a play profile
is that this mod registers 38 blocks and 39 items, and JEI is the fastest way to see all of them at
once. A block that exists but is unobtainable, or an item with no display name, shows up instantly
in a list and not at all in a course.

**FerriteCore** — memory reduction. Modest on its own, and it matters because developing this mod
means launching the client repeatedly; anything that makes a two-minute boot cheaper pays for itself
across a session.

## Ruled out

**Embeddium** — the usual NeoForge alternative to Sodium. Last NeoForge build is 1.0.15+mc1.21.1
from January 2025, with nothing since; the 1.21.4 build is beta only. There is no 1.21.11 release.
Sodium now covers NeoForge anyway, which is why Embeddium stopped being necessary.

**ModernFix** — the usual startup-time mod, and it would have been the obvious fourth pick. No
NeoForge 1.21.11 build exists.

**WTHIT** — the other block-inspection mod. Fabric builds are current; no NeoForge 1.21.11 version
turned up. Jade covers the same ground here.

## One thing found while installing

The machine already had Sodium, in a CurseForge backup profile -- but built for **1.21.1**, not
1.21.11. Close enough to look right in a filename and completely wrong for this game. Worth
remembering before reusing a jar from another profile.

## Caution worth stating

These are compatibility test targets, not dependencies, and the policy is right to keep them that
way. Sodium in particular replaces large parts of the renderer, so a visual bug seen with it
installed has to be reproduced without it before it means anything about our code — the mod already
has a `CourseSkyboxRenderer` and custom fluid rendering, both of which are exactly the sort of thing
a rewritten renderer interacts with.

## The playtest pack, and what fights PlaneShift

Read out of `PlaneShift Playtest/mods` (52 jars) rather than guessed at. Only the ones that touch
something this mod also touches are listed; the rest are performance or convenience and are fine.

**ParCool** — *integrated, see `ParCoolBridge`.* Ships wall jump, wall slide, cling, pole climb,
zipline, vault, roll, and more. This is most of plan 6.7 already built and better than the mod would
have built it. PlaneShift clears its stamina inside courses and stands its own wall jump down when
ParCool is present.

**enhanced-movement** — **turn Double Jump off.** It also adds a dash, a ledge grab and
Sandevistan-style afterimages, all configurable in its own screen. The dash is harmless and arguably
fits; the afterimages are not this game's look but hurt nothing. The double jump does real damage:
every gap in a generated course is sized against a proven jump arc, and `CourseReachability` proves
completability against that arc. A double jump does not make a course unbeatable — it makes every
gap free and every secret trivially reachable, which is the same as deleting the level design and
leaving the scenery. Its own comment problem, in another mod.

**SereneSeasons** — harmless here, and this was checked rather than assumed. It works by tinting
biome foliage, and PlaneShift's terrain is custom blocks whose models carry no `tintindex` at all
(zero across every block model in the mod). It cannot reach them.

**better-clouds** — probably harmless, unverified. PlaneShift installs a `CustomSkyboxRenderer` for
courses, so inside a course the sky is this mod's texture and there is nothing for a cloud renderer
to draw on. Outside a course they should not meet. Worth a look in game if the hub sky ever looks
wrong.

**Boss Music Mod** — not usable, and not needed. It is a datapack keyed to the ender dragon and the
wither by name, with no hook for other bosses. PlaneShift has its own boss track and its own
proximity trigger; the fix there was in this mod, not in that one.
