# 3D courses, animation and the mods worth leaning on

Research pass, 2026-09-03. Covers three things the project is about to need: how 3D Mario levels
are actually structured, which existing mods do the animation and movement work better than we
would, and how a course can be authored once and read in both 2.5D and 3D.

---

## 1. How Nintendo actually structures a level

The four-step structure is not a metaphor, it is the documented method — it comes from
*kishōtenketsu*, the four-act form Miyamoto used when he drew manga, and Nintendo's designers
describe levels in exactly these terms.

| Step | Japanese | What it does |
|---|---|---|
| Introduction | 起 *ki* | The mechanic appears somewhere completely safe. Flat ground, no hazard, the player is free to poke at it. |
| Development | 承 *shō* | The same mechanic, now doing real work — usually gaining height — but with a safety net still under it. |
| Twist | 転 *ten* | The safety net is removed and a second pressure is added, so the player manages two things at once. |
| Conclusion | 結 *ketsu* | The mechanic pays off, usually leading to the level's reward. |

The worked example everyone cites is *Cakewalk Flip* in Super Mario 3D World: red/blue panels flip
when Mario jumps. First over flat ground. Then as a staircase up a cliff, with ground below to
catch a mistake. Then with skull-bell shockwaves forcing jumps *and* no floor beneath. Then a final
vertical run to the reward.

The important part for us: **a level is a self-contained essay on one idea, taught, complicated and
discarded in about five minutes.** Not a corridor of unrelated obstacles.

`CourseComposer` already implements the first three steps directly — the teaching rule (a mechanic
must appear in a gentle segment before a hard one), the difficulty envelope, and the forced
breather. What it does **not** yet do is the fourth: a course currently has no *conclusion* tied to
its own introduced mechanic. It picks a set piece by theme, not by "what did this level teach?".

**Actionable:** give `CourseComposer` a "lesson" — pick one `Tag` at the start, guarantee an
introduction segment for it early, and prefer segments carrying that tag for the twist and
conclusion. That single change is the biggest remaining gap between our generator and a hand-made
Mario level. It is a change to selection weights, not to any segment.

## 2. 3D course design, and reading a course in both modes

3D Mario levels are still **linear ribbons**, not open worlds — 3D World in particular is a
corridor with width. That is exactly why a 2.5D-first project can extend to 3D without redesigning
anything: our courses are already ribbons, they simply have `LANE_HALF_WIDTH = 1`.

The cheapest honest route to 3D courses:

1. **Widen the lane, keep the spine.** `GenContext.LANE_HALF_WIDTH` is already the single knob.
   A 3D course is the same segment sequence with a half-width of 4–6 and the camera released from
   the rail. Every existing segment keeps working; they just become paths through a wider space.
2. **Segments gain an optional `buildWide` variant.** Where a 2.5D gap is a line, a 3D gap is an
   area — the same idea, expressed across depth as well as length.
3. **`CourseReachability` extends to 3D by adding z to the flood-fill.** The solver already works
   on a canvas that stores z; only `laneZ` is currently pinned. That is a small change and it means
   3D courses get the same walkability proof for free, which is the part nobody else gets right.
4. **Camera is the actual mode switch**, not the level. `PlaneRail` and `CameraProfile` already
   exist; a course would declare `"mode": "free_3d"` and skip the rail constraint.

This is why the 2.5D/3D shift is worth keeping as the mod's identity: a course authored once can be
*read* two ways, and a shift gate mid-course is a genuinely novel mechanic rather than a gimmick.

## 3. Mods worth using rather than rebuilding

Version-checked for **1.21.11 NeoForge** as of September 2026. Do not install anything that is not
confirmed 1.21.11 — see `docs/PLAYTEST_INSTANCE.md` for what happened last time.

### Strongly recommended

| Mod | Version | Why |
|---|---|---|
| **Player Animation Library** | `1.1.6+1.21.11-NeoForge` | The right foundation for the Cat suit. It exists specifically so several mods can animate the player without fighting each other, which is the failure mode if we hand-roll pose overrides. Everything below depends on it. |
| **ParCool!** | `1.21.11-3.4.3.3-NF` | Wall runs, pole climbing, rolls, vaults, with a stamina bar. This is the climbing and wall-run half of the Cat power-up, already built and balanced. Building our own would take a week and be worse. |
| **Not Enough Animations** | 1.21.11 | Fills in third-person animations vanilla only has in first person. Directly improves the third-person view you want, for zero code. |
| **Custom Player Animations** | 1.21.11 | Lets animations be authored and swapped as data rather than compiled in — the practical path to an all-fours cat run without writing a renderer. |

### The honest trade-off

ParCool gives climbing and wall-running immediately, but it is **global** — it applies everywhere,
not only while the Cat suit is active. Two options:

- **Accept it.** Treat parkour as part of how PlaneShift plays, and make the Cat suit about the
  *pounce attack and the dive*, which ParCool does not do.
- **Gate it.** ParCool exposes per-ability config; PlaneShift can toggle abilities on Form change.
  More work, but the power-up then means something.

I would take the first for now and revisit. A power-up whose only job is enabling a thing the
player could already do is a weak power-up, and "the Cat suit is the one that attacks" is a
stronger identity than "the Cat suit is the one that climbs" anyway.

### Also worth a look

- **Better Combat** (if a 1.21.11 build exists) — third-person melee with real swing arcs, which is
  the attack half of your third-person request.
- **Bendy Animation / First-person Model** — makes third person and first person agree, which
  matters once the player has a tail.

## 4. What this means for the Cat power-up

Split it into what mods do well and what only we can do:

| Piece | Who does it |
|---|---|
| All-fours run animation | Player Animation Library + Custom Player Animations |
| Wall climb / wall run | ParCool |
| Pounce attack (dive onto an enemy) | **Us** — it interacts with `CourseEnemyEntity` stomping |
| Claw swipe | **Us** — a Form action, fits the existing `FormActionKind` |
| Dive-bomb from height | **Us** — reuses the ground-pound path in `AirMoveService` |
| Cat suit item, Form, HUD, particles | **Us** — the existing Form pipeline handles it |

The parts that are ours are the parts that touch gameplay systems we own. That is the correct
split, and it is also the fastest one.

---

## Sources

- [The secret to Mario level design — Game Developer](https://www.gamedeveloper.com/design/the-secret-to-i-mario-i-level-design)
- [This is why Mario levels are brilliant — Engadget](https://www.engadget.com/2015-03-17-super-mario-3d-world-design.html)
- [Nintendo's level design secrets in four steps — MCV/DEVELOP](https://mcvuk.com/business-news/publishing/video-nintendos-level-design-secrets-in-four-steps/)
- [Kishōtenketsu & Hakoniwa](https://openedsource.medium.com/kish%C5%8Dtenketsu-hakoniwa-dd5a568da169)
- [Player Animation Library 1.1.6+1.21.11-NeoForge](https://www.curseforge.com/minecraft/mc-mods/player-animation-library/files/7509234)
- [ParCool! 1.21.11-3.4.3.3-NF](https://www.curseforge.com/minecraft/mc-mods/parcool/files/7894158)
- [Not Enough Animations](https://www.curseforge.com/minecraft/mc-mods/not-enough-animations)
- [Custom Player Animations](https://www.curseforge.com/minecraft/mc-mods/cpa)
