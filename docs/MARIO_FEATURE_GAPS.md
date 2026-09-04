# Mario feature gaps

Output of the `mario-gap-medium` agent sweep, 2026-09-03. Five consolidated lenses over the modern
Mario line — the NSMB games, Wonder and Mario Maker, the 3D line, the enemy roster, and game feel —
with every claim fact-checked against this repo by a second agent before it was accepted.

**How to read this.** Each entry was verified absent from the codebase at the time of the sweep and
checked against `docs/MISSING_MECHANICS.md` and `docs/NEXT_TASKS.md`, so nothing here duplicates work
already queued. `value` is the researching agent's own 1-10 score — a starting point for argument,
not a verdict.

**These are specifications, not plans.** The planning stage never ran: all eight planning agents hit
the account session limit. Every sketch below names real classes, but nobody has yet checked those
names against the current code — verify before you build.

```
117 claimed  |  5 already present  |  2 rejected as bad fit  |  110 distinct gaps
```

## enemy

### Enemy defeat-vector contract (weakness matrix)  *(value 10, medium)*

**From:** Modern Mario enemy/boss roster lens — every enemy is defined as much by how it can be killed as by how it moves

Replace the single `isStompable()` boolean with a declarative per-enemy set of accepted defeat vectors: STOMP, SPIN_JUMP, GROUND_POUND, FIREBALL, ICEBALL, SHELL, STAR, THROWN_OBJECT, PIT, CRUSH, NONE. Each vector resolves to one of three outcomes — defeat, state change (flip/stagger/shell/freeze), or reject (bounce the attacker off harmlessly). Terrain-class enemies (Muncher, Amp, Bumper, firebar) declare NONE for everything so nothing can accidentally delete them.

**Why here:** Right now defeat rules are scattered and inconsistent: `CourseEnemyEntity.resolveStomp` hardcodes stomp/spin-jump, `BuzzyBeetleEntity.hurtServer` hand-rolls a fireball rejection by instanceof, `KoopaEntity` fakes shell survival by returning `stompDamage()==0`, and everything else inherits vanilla `Monster` HP so a diamond sword one-shots a Thwomp. Nearly every enemy on this checklist (Ptooie, Chargin' Chuck, Amp, Bumper, Rocky Wrench, Dry Bones, Boom-Boom) is *defined* by an unusual defeat rule, so each one added without this ships another bespoke `hurtServer` override. This is the cheapest thing to do before the twelve enemies below, and the most expensive thing to retrofit after.

**Sketch:** Add `EnemyDefeat` (enum + `EnumSet` field) to `common/entity/`. In `CourseEnemyEntity` add `protected EnumSet<Vector> defeatVectors()` and `protected Outcome onVector(Vector, ServerPlayer)`; route `resolveStomp` and a new `hurtServer` override through it, so a rejected vector plays `ModSounds.DAMAGE` and bounces instead of dealing damage. Migrate `SpinyEntity` (reject STOMP, accept SPIN_JUMP/FIREBALL/SHELL), `BuzzyBeetleEntity` (delete its instanceof block, declare reject FIREBALL), `KoopaEntity` (STOMP -> state change), `BooEntity` (STAR only), `ThwompEntity`/`FirebarEntity` (NONE).

**Touches:** `CourseEnemyEntity.java`, `BuzzyBeetleEntity.java`, `SpinyEntity.java`, `KoopaEntity.java`

**Partly present already.** What is left: What is left: the declarative table itself. Replace `isStompable()` with a per-enemy EnumMap<DefeatVector, Outcome> on CourseEnemyEntity, route resolveStomp (CourseEnemyEntity.java:205) and the two projectiles' onHitEntity through it, and add the vectors that have no call site at all yet (GROUND_POUND, STAR, THROWN_OBJECT, PIT, CRUSH). Do NOT re-derive spin-jump-vs-armour, shell-kills-enemy, or Muncher/Firebar immunity — those exist; fold them into the table rather than rebuilding them.

### Ground pound as an offensive vector (shockwave and flip)  *(value 9, small)*

**From:** Modern Mario roster — ground pound is the standard answer to shelled/armoured enemies and the required finisher on several bosses

Landing a ground pound (a) defeats any enemy the player lands on regardless of armour, (b) emits a one-tile shockwave that flips shelled enemies (Buzzy Beetle, Spike Top, Ant Trooper) onto their backs for a few seconds, leaving them stompable and carryable, and (c) knocks every grounded enemy within ~3 blocks off its feet for a short stagger — the classic "P-switch moment" of clearing a crowd.

**Why here:** The ground pound exists in `AirMoveService` and touches only blocks — it calls `HitFromBelowBlock.impact` on the block below and nothing else. So an armoured enemy has exactly one counter in the whole mod (the spin jump), and the ground pound is a block-opening tool rather than a combat verb. Half the enemies on this checklist assume a flip-then-stomp two-step exists.

**Sketch:** In `AirMoveService`, in the `GROUND_POUNDING.remove(player) != null` branch, add an AABB scan for `CourseEnemyEntity` and dispatch `Vector.GROUND_POUND` through the new defeat contract. Add `flipped` synced ticks to `CourseEnemyEntity` (mirroring the existing `SQUISH_TICKS` pattern) that suppresses `LanePatrolGoal` and forces STOMP acceptance while set.

**Touches:** `AirMoveService.java`, `CourseEnemyEntity.java`, `LanePatrolGoal.java`

### Chargin' Chuck — the enemy that reacts to the player  *(value 8, medium)*

**From:** Chargin' Chuck

A helmeted, shoulder-padded bruiser who idles until the player enters his sight line, then sprints at them along the ground, skidding on a turn and lowering his shoulder. He shrugs off the first two stomps (stagger and get up angrier and faster) and only falls on the third; a fireball does nothing, but a kicked shell or a ground pound ends him at once.

**Why here:** Every ground enemy in the mod patrols and none react — `LanePatrolGoal` is deliberately the only ground behaviour, and the code comments say chasing was removed because it read as "a zombie in a costume". Chuck is the correct way back to reaction: he charges along the 2.5D rail only, telegraphs it, and is beaten by movement rather than by combat. He also gives the mod its first multi-hit non-boss, which is what makes late worlds feel different rather than merely denser.

**Sketch:** `ChargingChuckEntity extends CourseEnemyEntity` with a `ChuckChargeGoal` that triggers on player x-distance along the travel axis (not `getNearestPlayer`, so it stays lane-native), plus a `hitsTaken` counter and the multi-stomp outcome from the defeat contract. Add to `SegmentLibrary.cast()` for GRASS and DESERT at difficulty 3+.

**Touches:** `LanePatrolGoal.java`, `ModEntities.java`, `SegmentLibrary.java`

### Spike / Stone Spike — the enemy that manufactures its own projectile  *(value 8, medium)*

**From:** Spike and Stone Spike

A squat, stationary thrower that pulls a ball from its mouth on a slow telegraphed cycle and rolls it along the ground toward the player. The ball is the actual hazard: it rolls, falls off ledges, and can be jumped over, destroyed by a fireball, or destroyed by a kicked shell. Stone Spike throws a heavier ball that shatters on impact and cannot be destroyed in flight. The thrower itself is stompable but only in the window while it is chewing.

**Why here:** Every ranged threat in the mod fires straight through the air — `HammerProjectile` arcs, `EmberBoltEntity`, `BowserFire` and `IceballProjectile` all fly. There is no ground-travelling hazard except a kicked Koopa shell, so "the floor is dangerous while you stand on it" is an idea the level vocabulary cannot express, and the enemy that adds it also adds a vulnerability *window*, which nothing else has.

**Sketch:** `SpikeEntity extends CourseEnemyEntity` with a chew/throw cycle modelled directly on `PiranhaPlantEntity`'s emerge timer (already unit-tested by `PiranhaPlantCycleTest`) and a `SpikeBallEntity` that rolls on the lane using `CourseEnemyEntity.holdLane()` for free. Register both in `ModEntities` with renderers; place from `SegmentLibrary` on flat REST/ENEMY segments where the roll has room.

**Touches:** `PiranhaPlantEntity.java`, `ModEntities.java`, `SegmentLibrary.java`

### Goomba family — Paragoomba, Goombrat, Galoomba, Mega Goomba  *(value 8, small)*

**From:** Goomba variants

Four cheap variations on the mod's most common enemy. Paragoomba flutters in a low hover and drops micro-Goombas that cling to the player and slow them until shaken off; stomping it removes the wings. Goombrat turns at ledges instead of walking off. Galoomba flips onto its back when stomped rather than dying, and can then be picked up and thrown as a weapon. Mega Goomba is an oversized one that takes two stomps and shrinks to a normal Goomba on the first.

**Why here:** `GoombaEntity` is 36 lines and is the most-placed enemy in the game — it is in the GRASS and DESERT rosters in `SegmentLibrary.cast()`. Everything above is a synced flag on a class that already exists, and `LanePatrolGoal` already takes the `turnsAtLedge` boolean that Goombrat needs (it was added for red Koopas and Goomba does not pass it). This is the highest ratio of expressive range to lines of code available anywhere in the enemy roster. Note that `MISSING_MECHANICS` item 16 covers Paratroopa — a winged *Koopa* — not any of these.

**Sketch:** Add a `Variant` synced enum to `GoombaEntity` mirroring `KoopaEntity`'s `RED` accessor, with save/load in `addAdditionalSaveData`. Goombrat passes `true` to the existing `LanePatrolGoal(this, 1.0D, turnsAtLedge)` ctor. Galoomba's flip and carry reuse the flipped state from the ground-pound item and the throw path queued for Bob-omb. Mega scales via `EnemyRigProfile.scaled()`. `SegmentLibrary.cast()` picks variants by difficulty.

**Touches:** `GoombaEntity.java`, `LanePatrolGoal.java`, `SegmentLibrary.java`

**Partly present already.** What is left: Genuinely new here: Galoomba (flip-on-stomp then carryable/throwable) and the Paragoomba micro-Goomba cling. Goombrat is a one-liner on existing code — do not scope it as a new enemy. Paragoomba's wings and Mega Goomba's two-stomp should be built as the shared mechanic with Paratroopa (task 10) and Rex (item 36) respectively. Note Galoomba needs a carry/throw system that does not exist anywhere in the mod.

### Ice as a state, not damage — frozen enemies as portable blocks  *(value 8, medium)*

**From:** The modern roster's standard secondary answer to enemies that cannot be stomped

An ice projectile encases the enemy it hits in a block of ice rather than damaging it. The frozen enemy becomes a solid, standable, pushable, carryable block: it can be used as a platform to reach a ledge, thrown to defeat other enemies, or left to thaw after a few seconds, releasing the enemy unharmed. Enemies that resist fire (Buzzy Beetle) do not resist ice, and vice versa, so the two flowers stop being reskins.

**Why here:** `IceballProjectile` currently calls `hurtServer(...thrown..., 3.0F)` — it is a fireball with a different texture, so the Ice form has no identity and the mod's answer to every enemy is "deal damage to it". Freezing is what makes Ptooie, Amp-adjacent hazards, Spiny and Chargin' Chuck each solvable in a *second* way, which is the whole point of having a power-up roster. It also creates the mod's first player-authored platform, which is a movement mechanic obtained for free.

**Sketch:** Add a `frozenTicks` synced int to `CourseEnemyEntity` (same shape as the existing `SQUISH_TICKS` accessor) that suppresses goals, sets a solid collision box, and thaws on a timer or on fire contact. `IceballProjectile.onHitEntity` sets it instead of calling `hurtServer`. Carry/throw shares the path queued for Bob-omb in `NEXT_TASKS` item 12. `FireballProjectile` on a frozen target shatters it for an instant defeat.

**Touches:** `IceballProjectile.java`, `CourseEnemyEntity.java`, `FireballProjectile.java`

**Partly present already.** What is left: What is left: (a) drop the damage from IceballProjectile.onHitEntity so ice is purely a state change; (b) an encased-enemy entity that is solid and standable — no precedent, but MovingPlatformEntity.java:87-88 already carries riders on a collidable entity and is the closest model; (c) thaw-on-timer releasing the enemy unharmed; (d) carry and throw, which is the same missing system Galoomba and Bob-omb (doc item 19) need — build it once. The fire-vs-ice resistance split depends on the defeat-vector table.

### Enemy-versus-enemy and enemy-versus-hazard resolution  *(value 8, small)*

**From:** The roster as a system — modern Mario enemies constantly defeat each other, and half the roster's charm is emergent

Enemies interact with the world the way the player does: a Bob-omb blast, a Spike's rolling ball, a thrown frozen enemy, a Chargin' Chuck's sprint and a Chain Chomp's lunge all defeat other enemies; a Bullet Bill flies through and scatters a Goomba line; lava, crushers and Thwomps kill enemies that walk into them; a falling enemy that lands on another one bounces off it. Chained defeats award an escalating score just as a stomp chain does.

**Why here:** `KoopaEntity.tickSlide()` is the *only* place in the mod where one enemy can defeat another, and `MISSING_MECHANICS` item 37 (shell kills other enemies) is already struck through as done — which means the concept is proven and confined to exactly one entity. Generalising it is what turns a room full of hazards into a system the player can exploit, and it is the difference between a roster and a checklist. `CourseScoringService.awardShellKill` already takes a combo count, so the scoring half is built.

**Sketch:** Lift the victim scan out of `KoopaEntity.tickSlide()` into a shared `EnemyImpact.strike(source, box, damage, attributingPlayer, combo)` helper in `common/entity/`, and call it from every new hazard. Add a generic "hostile hazard hurts enemies too" pass so `ThwompEntity`, lava and `FirebarEntity` clear enemies. Route all of it through `CourseScoringService.awardShellKill`, which already exists with the combo parameter.

**Touches:** `KoopaEntity.java`, `CourseScoringService.java`, `ThwompEntity.java`

**Partly present already.** What is left: Remaining: generalise the hazard side (Thwomp, lava, firebar, future crushers hurting CourseEnemyEntity as well as players — ThwompGoal.java:90 is the one-line-per-hazard change), Bullet Bill scattering a line, and enemy-lands-on-enemy bounce. The Bob-omb blast / Spike ball / Chuck sprint / Chain Chomp lunge cases are blocked on enemies that do not exist (Bob-omb is doc item 19, Chain Chomp item 23). The escalating chain score is done — reuse awardShellKill, do not write a second ladder.

### Ptooie and Nipper — Piranha Plants that leave the pipe  *(value 7, medium)*

**From:** Ptooie, Nipper Plant, Stalking Piranha Plant

Three walking-plant variants sharing one body. Ptooie walks slowly while juggling a spiked ball on a jet of air overhead — the ball must be knocked off with a projectile or shell before the plant can be stomped, and the loose ball then rolls as a hazard. Nipper hops in short arcs and cannot be stomped safely mid-hop. Stalking Piranha walks on a stem and lunges when the player comes level with it.

**Why here:** `PiranhaPlantEntity` is a single fixed emerge-from-pipe cycle, so plants are pure terrain timing — the player never interacts with one, only waits. These give the plant family a second axis (it moves, and it has a removable defence), and Ptooie in particular is the mod's first "disarm before you can defeat" enemy, which is the pattern that later makes Ptooie, Amp and Bumper read as different problems rather than three walls.

**Sketch:** Add a `Variant` synced enum to `PiranhaPlantEntity` (same accessor pattern as `KoopaEntity.RED`) selecting FIXED / PTOOIE / NIPPER / STALKING; walking variants get `LanePatrolGoal`, which already handles ledge turning. The Ptooie ball is the same `SpikeBallEntity` as the Spike item above — build them together. `SegmentLibrary` line ~414 already spawns `PIRANHA_PLANT` at pipe mouths; add a ground-placement call for the walkers.

**Touches:** `PiranhaPlantEntity.java`, `LanePatrolGoal.java`, `SegmentLibrary.java`

### Amp — the invincible hazard that patrols a path  *(value 7, small)*

**From:** Amp

A crackling electric sphere that follows a fixed closed path — a circle, a figure-eight, or a straight sweep between two points — at constant speed, damaging on any contact and immune to absolutely everything. Its entire design is that it is unkillable and perfectly predictable, so it converts a corridor into a rhythm puzzle.

**Why here:** The mod's only unkillable hazards are `ThwompEntity` (one axis, triggered) and `FirebarEntity` (rotation pinned to one anchor). There is no free-roaming path hazard, and `MovingPlatformEntity` — which already does a straight axis sweep — proves the movement half is solved. Amps are also the cheapest way to make the UNDERGROUND and LAVA themes read differently from GRASS, which `MISSING_MECHANICS` item 69 flags as an open problem.

**Sketch:** `AmpEntity extends CourseEnemyEntity` (MISC-flavoured: no gravity, no goals) reusing `FirebarEntity`'s parametric-angle tick for circular paths and `MovingPlatformEntity`'s sweep for linear ones. Declares NONE for every defeat vector. Placed by new OVERHEAD/ENEMY-tagged segments in `SegmentLibrary`.

**Touches:** `FirebarEntity.java`, `MovingPlatformEntity.java`, `ModEntities.java`

### Skipsqueak and the synchronised-cadence enemy group  *(value 7, medium)*

**From:** Skipsqueak, including Spiny Skipsqueak

Mice that hop in a strict shared rhythm — every Skipsqueak on screen leaves the ground on the same beat — bouncing between platforms so the player must move on the off-beat. The Spiny variant raises spikes on the up-beat, so it is only stompable at the top of its own arc. The mechanic is not the individual enemy, it is that several of them share one clock.

**Why here:** Nothing in the mod is synchronised. Every cycle — `PiranhaPlantEntity`'s emerge, `ThwompGoal`'s drop, `FirebarEntity`'s rotation, `HammerBroGoal`'s throw — runs off its own independent timer seeded at spawn, so a room with six hazards reads as noise rather than a pattern. A shared course clock is a small piece of infrastructure that would immediately make firebars, Thwomps and note blocks (which the mod already places as a run) feel authored.

**Sketch:** Add a `courseBeat(long gameTime)` helper to `CourseService`/`CourseStateAccess` (course start time modulo a beat length). `SkipsqueakEntity extends CourseEnemyEntity` hops when the beat rolls over; retrofit `ThwompGoal` and `FirebarEntity` to optionally phase-lock to it via an NBT `Beat` offset.

**Touches:** `CourseService.java`, `ThwompGoal.java`, `FirebarEntity.java`

### Bull's-Eye Bill and the Bill family beyond the straight line  *(value 7, trivial)*

**From:** Bull's-Eye Bill, Banzai Bill, Missile Bill

Bullet Bill variants that stop being a fixed obstacle. Bull's-Eye Bill locks onto the player at launch and steers gently toward them for its whole flight, so it must be dodged rather than out-run — and its slow turn rate means it can be led into a wall or another enemy. Missile Bill turns sharply and self-destructs on impact. All remain stompable from above, which is the counter and often the intended platform.

**Why here:** `BulletBillGoal` is 64 lines of straight-line flight, so a Bullet Bill is a moving wall the player walks around. A homing Bill is the same entity with one vector-blend line and it changes the enemy from scenery into a threat that follows the player up onto a platform — which is exactly the pressure an auto-scroll section needs. `MISSING_MECHANICS` item 34 covers Banzai (a bigger Bill) but nothing covers steering.

**Sketch:** Add a synced `Homing` flag to `BulletBillEntity` and, in `BulletBillGoal.tick()`, slerp the velocity toward the vector to the nearest player with a hard cap on turn rate per tick; `holdLane()` already restricts it to the plane, so it is a 2D steer. Missile variant calls the existing explosion path on `horizontalCollision`. Placed by the launcher block queued as item 35.

**Touches:** `BulletBillGoal.java`, `BulletBillEntity.java`

### Boo family — Peepa, Stretch, and a camera-correct watch test  *(value 7, small)*

**From:** Boo variants

Peepas orbit a fixed point in a slow ring and are never vulnerable — they are moving walls, not stalkers. Stretch is a flat Boo that lives *in* a platform, rising through the floor to block the surface and sinking again on a cycle, so the safe standing spot changes over time. Both are immune to everything except a Star. Additionally, the shy-Boo freeze test should key off the direction the player is *travelling and facing along the course rail*, not free-look yaw.

**Why here:** The last part is a live defect, not just a gap: `BooGoal` computes `target.getLookAngle().dot(toBoo)` against the player's camera yaw, but in a fixed side-on 2.5D course the player's yaw is not what the player perceives as "looking at" the Boo — they can be walking right at it while the yaw reads elsewhere, so a Boo freezes and unfreezes for reasons invisible on screen. `MISSING_MECHANICS` item 32 covers Big Boo and Boo circles but not Stretch, Peepa, or this test. Ghost House is one of six themes and currently has two enemies, one of which behaves unpredictably.

**Sketch:** In `BooGoal.tick()`, replace the look-angle dot with the player's travel-axis facing (the same axis `MovementRuleService` folds momentum onto), with the camera yaw as a fallback for 3D courses. Add `PeepaEntity` (orbit tick lifted from `FirebarEntity`) and `StretchEntity` (rise/sink cycle on `PiranhaPlantEntity`'s timer shape, occupying a floor tile).

**Touches:** `BooGoal.java`, `BooEntity.java`, `MovementRuleService.java`

**Partly present already.** What is left: Three separate items, only two worth taking. (1) Fix BooGoal.java:55 to test the sign of the player's travel/facing along PlaneRail's travel axis instead of getLookAngle() — this is a bug fix, small, and should be done regardless. (2) Stretch (in-platform riser) is new and worth building. (3) Peepa is item 32 already on the list — do not open a second entry for it.

### Phanto — a pursuer that exists only while you carry the key  *(value 7, small)*

**From:** Super Mario Maker 2

A floating mask, inert and decorative, that wakes the instant the player picks up a key and pursues them relentlessly through walls until the key is used or dropped. It cannot be defeated. Dropping the key puts it back to sleep on the spot.

**Why here:** It converts a fetch objective into a chase, which is the only reason a key is interesting. The backlog has key-and-keyhole as a ghost-house objective; without a Phanto that objective is 'walk over there and walk back'. It is also cheap here: `BooEntity` and `BooGoal` already implement a phasing pursuer, so the entity is mostly a retarget of existing AI onto a carry-state trigger.

**Sketch:** New entity extending `CourseEnemyEntity` with a goal modelled on `BooGoal` but without the look-away rule — Phanto never stops. Wake condition reads whatever holds the carried-object state (see the carry system entry); until that exists, an inventory check for the key item is enough. Mark it indestructible the way `FirebarEntity` is, deliberately and with the same reasoning. Register in `ModEntities` and add the renderer, or `checkEntityRenderers` fails the build — which is the point of that check.

**Touches:** `BooGoal.java`, `CourseEnemyEntity.java`, `ModEntities.java`

### Thief NPC that steals a Form and must be chased down  *(value 7, medium)*

**From:** New Super Mario Bros. U — the sack-carrying thief you chase through a course to recover what it holds

An enemy that does not attack. It spawns holding a Form, flees along the travel axis at a speed slightly above the player's run tier, vaults small obstacles, and is only catchable where the course geometry corners it — a dead end, a low ceiling that forces it to slow, a P-switch that opens a shortcut. Touching it makes it drop what it carries. If it reaches the end of the course it escapes with the item. A variant spawns already holding a Form the player just lost to damage, giving a lost power-up a second life.

**Why here:** Every enemy in PlaneShift's roster is a threat to be stomped, avoided or shelled; there is no enemy whose whole purpose is to be a moving objective. That is a genuinely different verb, and it is the one that makes a segment about the player's own speed rather than about timing a jump. It also gives the composer's SECRET tag a companion — a chase is a reward route whose gate is skill rather than knowledge — and it directly exercises the P-speed/run tier work that is already on the backlog.

**Sketch:** New ThiefEntity extending CourseEnemyEntity (which already provides the squish framework and the air-drop immunity flag) with a FleeAlongLaneGoal modelled on the existing LanePatrolGoal — same lane clamping, inverted target. Contact handling goes in the same collision path CourseEnemyEntity already uses for stomps, but drops an ItemStack via the existing pickup path rather than calling DamageService. Add a Segment to SegmentLibrary tagged ENEMY plus SECRET whose geometry is a corridor ending in a wall, so the chase has a guaranteed resolution; CourseReachability already proves the corridor is walkable. Register in ModEntities and add the renderer (build.gradle's checkEntityRenderers task will enforce that).

**Touches:** `CourseEnemyEntity.java`, `LanePatrolGoal.java`, `ModEntities.java`, `SegmentLibrary.java`

### Bumper — the hazard that moves the player instead of hurting them  *(value 6, small)*

**From:** Bumper

A round, springy obstacle — static or on a slow orbit — that deals no damage at all but launches the player away hard along the contact normal. Touching one is not a punishment, it is a redirection: bumpers are used to knock a player off a ledge, or as a deliberate route through a gap that cannot be jumped.

**Why here:** Every hostile object in the mod resolves to damage or nothing. There is no non-damaging force object, so course design has no way to say "this pushes you" — and `MISSING_MECHANICS` items 13 (wind zones) and 50 (cannon) are asking for the same missing concept from the block side. A Bumper is the enemy-side version and is the cheapest of the three to build, because the launch maths already exists in `SpringPadBlock`.

**Sketch:** `BumperEntity extends CourseEnemyEntity`, overriding `playerTouch` to apply an impulse instead of `hurtServer` — reuse the vector from `SpringPadBlock` and set `hurtMarked`. Declares NONE for every defeat vector. `holdLane()` keeps it on the rail. Place in SKY/UNDERGROUND segments in `SegmentLibrary`.

**Touches:** `CourseEnemyEntity.java`, `ModEntities.java`, `SegmentLibrary.java`

### Fuzzy — the track-rider that disorients  *(value 6, medium)*

**From:** Fuzzy

A small black fuzzball that clings to a drawn track (a rail line, a vine, a fence) and slides along it forever, forcing the player to share that track and time their crossing. Contact damages, and in the modern games a Fuzzy hit also briefly warps the screen. It cannot be stomped, because the player is usually hanging on the same track.

**Why here:** The mod has no track-riding enemy at all, and it does have the two block types a Fuzzy needs — `CourseVineBlock` (climbable, added with the secret vine) and lattice/fence blocks that already have shaped models. A climbable vine with nothing contesting it is a free ride; a Fuzzy is what makes vertical climbing a decision. It is also the natural inhabitant of the vertical sections `MISSING_MECHANICS` item 61 wants.

**Sketch:** `FuzzyEntity extends CourseEnemyEntity` with no gravity and a goal that scans for adjacent `CourseVineBlock`/lattice and walks the run of them, reversing at either end — the same ledge-detection shape as `LanePatrolGoal` but tested against a block tag instead of solid ground. Screen distortion goes through the existing client HUD payload path, gated on `reducedMotion`.

**Touches:** `LanePatrolGoal.java`, `SecretVineBlock.java`, `ModEntities.java`

### Ninji — the vertical-rhythm hopper  *(value 6, small)*

**From:** Ninji

A small hooded enemy that hops straight up and down in place on a fixed cadence, sometimes drifting slowly toward the player, and hugs walls — climbing a vertical shaft in a steady bounce. Stompable, but only between hops, so it is a metronome the player must read rather than a body to walk over.

**Why here:** Every enemy in the mod occupies the ground plane and moves horizontally; `BooEntity` and `LakituEntity` fly but do not create a *vertical* rhythm. A course wall with Ninjis on it is a timing ladder, and it is the enemy that makes climb-tagged segments (Tag.CLIMB already exists in `Segment.Tag`) more than staircases. Cheap, too — it is a bounce timer and nothing else.

**Sketch:** `NinjiEntity extends CourseEnemyEntity` with a `NinjiHopGoal`: on a fixed period set `setDeltaMovement(0, hopForce, 0)` and optionally nudge along the travel axis toward the nearest player. Wall-hug variant tests `horizontalCollision` against the wall face. Placed by new CLIMB-tagged segments in `SegmentLibrary`.

**Touches:** `ModEntities.java`, `SegmentLibrary.java`, `Segment.java`

### Conkdor — the reach-and-recoil enemy  *(value 6, small)*

**From:** Conkdor

A long-necked bird that shuffles slowly along a fixed line and periodically stabs its head down into the ground with a heavy telegraph, leaving the head planted and vulnerable for about a second. Its body cannot be harmed. The player must stand clear of the strike zone and stomp the buried head during the recovery window.

**Why here:** It is the clearest expression of a mechanic the mod has none of: an enemy whose attack *creates* its own weak point, so aggression is rewarded on the enemy's terms rather than the player's. Every current enemy is either always vulnerable from above or never vulnerable. It also produces a hazard footprint on the ground, which pairs with the theme-hazard gap in `MISSING_MECHANICS` item 69 (Conkdor belongs in DESERT, which currently differs from GRASS only by palette).

**Sketch:** `ConkdorEntity extends CourseEnemyEntity` with a three-phase goal (walk / wind-up / planted) driven exactly like `PiranhaPlantEntity`'s tested emerge cycle; `isStompable()` (or the defeat-vector set) returns true only in the planted phase. Add to `SegmentLibrary.cast()` for DESERT.

**Touches:** `PiranhaPlantEntity.java`, `ModEntities.java`, `SegmentLibrary.java`

### Rocky Wrench — the pop-up thrower with a punish window  *(value 6, medium)*

**From:** Rocky Wrench

A mole that lives in a hatch in the floor: it pops the lid, lobs a wrench in a high arc, and ducks back under. It is invulnerable while submerged and stompable only during the throw, so the hatch is a permanent fixture the player learns to time. Wrenches spin and can be dodged by moving under them.

**Why here:** `MISSING_MECHANICS` item 24 queues Monty Mole (bursts out as the player passes — a one-shot ambush), which is a different enemy: Rocky Wrench is a permanent, repeating, ranged emplacement, and the mod's only ranged ground threat is `HammerBroEntity`, which stands in the open and can be stomped at any time. A thrower that is only vulnerable while attacking is what makes ranged enemies into puzzles instead of walls. Its hatch also gives `SegmentLibrary` a reason to break up a flat floor.

**Sketch:** `RockyWrenchEntity extends CourseEnemyEntity`, cycle again modelled on `PiranhaPlantEntity`; reuse `HammerProjectile` (91 lines, arc solved) as the wrench with a different renderer. A `WrenchHatchBlock` in `ModBlocks` marks the placement so the generator and `CourseReachability` both know the tile is solid floor.

**Touches:** `HammerProjectile.java`, `ModBlocks.java`, `SegmentLibrary.java`

### Ant Trooper and the spike-toggle stomp gate  *(value 6, medium)*

**From:** Ant Trooper and Horned Ant Trooper

Marching ants that walk in single file over any surface — including around a platform's edge onto its underside — so a rotating or floating platform has enemies on all four faces. The Horned variant raises a spike on its back on a periodic cycle, so whether it can be stomped changes second to second and the player must read the animation, not the silhouette.

**Why here:** `MISSING_MECHANICS` item 31 queues Spike Top (a wall-and-ceiling walker), but the two distinct ideas here are not in that item: surface-following *around corners*, and a stomp-safety flag that toggles at runtime. The second one is the more valuable half — the whole current model is that armour is a permanent property declared by `isStompable()`, and a toggling gate turns a stomp into a timing decision using enemies the player already understands.

**Sketch:** A `SurfaceWalkGoal` in `common/entity/` that keeps the entity's feet against the nearest solid face and rotates the gravity axis at a corner (`CourseEnemyEntity.holdLane()` still pins z, so it is a 2D problem). `AntTrooperEntity` adds a synced `Spiked` boolean on a timer that feeds the defeat-vector set, plus a renderer cue so the state is legible.

**Touches:** `CourseEnemyEntity.java`, `LanePatrolGoal.java`, `ModEntities.java`


## level-mechanic

### Wonder Flower — mid-course transformation events  *(value 10, large)*

**From:** Super Mario Bros. Wonder (2023)

A one-per-course pickup that, when touched, does not change the player at all — it changes the level for a bounded stretch. From the flower to a terminating marker the course runs under an alternate rule set: geometry animates, physics change, the enemy cast is replaced, or the camera reframes. Reaching the end marker collects a progression token and reverts everything. Crucially the transformation is authored per-effect, is always survivable, and always ends at a fixed point so the course returns to normal for the flagpole run.

**Why here:** PlaneShift generates every course from a segment grammar, so its main long-term risk is sameness — a player who has seen twenty segments has seen the game. A transformation event is the only mechanic on this list that multiplies the value of every segment already in `SegmentLibrary` rather than adding one more. It is also the single strongest argument for a 2.5D Minecraft platformer existing at all: the block world can visibly rebuild itself mid-run in a way a sprite platformer cannot.

**Sketch:** New `server/WonderEventService` holding, per player, an active effect id and the x-range it covers. New `WonderFlowerBlock` in `common/block` plus a `Tag.WONDER` in `Segment.Tag`; `CourseComposer` places exactly one wonder segment in the 'develop' band and records `canvas.marker("wonder_start"/"wonder_end")`. Effects implement a small `WonderEffect` interface with `begin(ServerLevel, BlockPos region)`, `tick`, `end`, registered in a code-side map exactly as `FormActionKind` gates trusted behaviours. Region rewrites reuse `CourseWriter.write` against a second `CourseCanvas` so the mutation path is the same code that built the course. Sync the active effect id on `CourseState` (new field, same codec pattern as `autoScroll`) so `CourseHud` and `CourseMusicManager` can react.

**Touches:** `Segment.java`, `CourseComposer.java`, `CourseWriter.java`, `CourseState.java`, `ModBlocks.java`

### Per-theme composition rulesets, not just per-theme palettes  *(value 9, large)*

**From:** Mario level grammar — theme as a ruleset (ghost house, castle, athletic)

Each theme constrains what the composer may build, not only what colour it is. A ghost house restricts the catalogue to navigation and secret segments, forbids the plain forward corridor, and requires a non-obvious exit. A castle is a gauntlet: no REST segments after the midpoint and a mandatory set piece. An athletic theme bans ground-level segments outright so the whole course is platform-to-platform. The theme picks the ruleset; the seed varies within it.

**Why here:** CourseTheme has six values and touches exactly three things in generation: the block palette (GenContext.Palette.forTheme), the enemy roster (SegmentLibrary.cast), and one boolean deciding whether CASTLE_BRIDGE may be placed. Every course therefore has the same shape, so a ghost house and a grass field play identically in different colours — which is precisely why the themes read as cosmetic. MISSING_MECHANICS #69 is adjacent but narrower: it asks for themed *hazards*, which would repaint the danger without changing the shape of the level. There is also no athletic/sky theme at all, despite TRAMPOLINE_SKY_LAUNCH, PLATFORM_LADDER, LIFT_SHAFT and MULTI_TIER_CANOPY already existing to build one from.

**Sketch:** Add a ThemeRules record (allowed/forbidden Segment.Tag sets, a required tag, a restAllowedAfterProgress threshold, a mandatory-setpiece flag) with a static forTheme(CourseTheme) table, and consult it in CourseComposer.pick() as a filter *before* the existing weight arithmetic, plus in the breather branch (`lastDifficulty >= 3` forcing SegmentLibrary.BREATHER) so a castle can refuse the breather. Add ATHLETIC to CourseTheme with its palette and cast. This is a change to selection, not to any segment, so no segment needs rewriting — the same pattern NEXT_TASKS Group B uses for the conclude step.

**Touches:** `CourseComposer.java`, `GenContext.java`, `CourseTheme.java`, `SegmentLibrary.java`

### Multiple concurrent routes at different skill levels  *(value 9, large)*

**From:** Mario level grammar — the high road and the low road

Segments expose an optional upper route running above the main line: harder to enter and to hold, denser in coins and star coins, rejoining the main line a few segments later. A confident player rides the roof; a struggling player takes the ground. The same level serves both without a difficulty setting.

**Why here:** CourseComposer is a strictly linear chain — one cursor, one floorY, one segment at a time, appended end to end — so every player walks the identical line and skill expresses itself only as speed. This is the structural reason the difficulty work (MISSING_MECHANICS #68, NEXT_TASKS B9) keeps reducing to 'more pits, more enemies': with one route, the only lever is how punishing that route is. Parallel routes are the lever Mario actually uses, and nothing on either list proposes them (#65 secret exits is a branch in the *world map*, not within a course). The canvas is already 3D-keyed and the reachability solver already flood-fills, so both the placement and the proof are supported.

**Sketch:** Add an optional buildHigh(canvas, x, baseY, ctx) default method to Segment (defaulting to no-op) alongside the existing build, and a highRouteEntry/Exit height on SegmentSpec. In CourseComposer, once per course pick a run of 3-5 consecutive segments in the mid-course band that all support it, draw the high route, and bookend it with an entry (a spring pad or note-block run — SPRING_PAD, PLANESHIFT_NOTE_BLOCK and TRAMPOLINE all exist) and a rejoin drop. Bias placeStarCoins toward the high route: it already ranks by segment difficulty, so add a bonus for high-route columns. CourseReachability then proves both routes clearable in the same pass.

**Touches:** `CourseComposer.java`, `Segment.java`, `SegmentLibrary.java`, `CourseReachability.java`

### Mid-course 2D flatten sections, using the ShiftGateBlock that already exists  *(value 9, small)*

**From:** Super Mario Odyssey (2D pipe sections)

A short authored stretch inside a course where the world snaps to a flat plane and the rules change with it: movement locks to two axes, the camera goes fully side-on, and — this is the part that makes it more than a camera trick — enemies, coins and hazards that were spread across depth collapse onto the plane, so the flat section is a different arrangement of the same objects, not a narrowed version of the 3D one. Exiting restores depth.

**Why here:** PlaneShift built the entire hard half of this and then never used it. `ShiftGateBlock` is a fully implemented, server-validated, transactional perspective gate, and `ModeTransitionService` gives it commit/rollback semantics — and it is placed by exactly nothing. Wiring it into `SegmentLibrary` is a small change that immediately makes the FREE_3D `space_*` courses (currently, per NEXT_TASKS, just wide 2.5D ribbons) into courses with a real structural idea, and gives the 2.5D courses a legitimate reason to briefly open into depth.

**Sketch:** Add `Segment.Tag.PERSPECTIVE` and two `SegmentLibrary` builders — one that opens a 3D pocket inside a 2.5D course, one that flattens a stretch of a 3D course — each placing a facing pair of `ModBlocks.SHIFT_GATE` with the correct `TARGET` value; `ShiftGateBlock.getStateForPlacement` already derives the destination rail from facing, so no course-JSON work is needed. `CourseComposer.pick()` treats the tag as a twist, matching the four-step structure NEXT_TASKS task 6 describes. `CourseReachability` must be taught that a gate pair is a valid traversal edge, or the completability proof will reject the segment.

**Touches:** `ShiftGateBlock.java`, `SegmentLibrary.java`, `CourseComposer.java`, `CourseReachability.java`, `ModeTransitionService.java`

**Partly present already.** What is left: Two things are actually missing. (1) Nothing ever places a gate: the only references to SHIFT_GATE outside the block class are registry and creative-tab entries, so no generated course contains one — this is a `server/gen/CourseComposer`/`SegmentLibrary` change plus probably a new `Segment.Tag`. (2) The object collapse — the part the claim correctly identifies as more than a camera trick. `CourseEnemyEntity` pins each enemy to a depth coordinate on first tick (CourseEnemyEntity.java:50); flattening would need those depth-spread enemies/coins/hazards re-projected onto the plane as a *different arrangement*, and nothing does that today. Also worth checking against docs/NEXT_TASKS.md:52 (task 7, `buildWide` for 3D segments) so the two do not author contradictory depth rules.

### Living terrain Wonder Effect — the course gets up and walks  *(value 8, medium)*

**From:** Super Mario Bros. Wonder (2023)

A transformation in which the static geometry animates: a run of ground blocks detaches into a marching column that carries the player forward, pipes lean and sway, or the whole floor tilts on a slow cycle. The player's inputs are unchanged; what changed is that the platform under them now has intent. It resolves back to static blocks at the end marker.

**Why here:** This is the effect that most needs Minecraft specifically — blocks becoming entities and back is a native capability, and PlaneShift already has an entity that is a solid moving surface. It also answers the flat-corridor criticism in a way no new segment can: the corridor itself becomes the obstacle.

**Sketch:** Reuse `MovingPlatformEntity` as the carrier: on effect begin, replace the marked floor run with a chain of platform entities offset in phase, using `CourseCanvas.movingSurface` records already emitted by `LIFT_SHAFT`/`MOVING_CROSSING` so the reachability model knows about them. On effect end, write the static blocks back via a stored `BlockState` map. `CourseReachability` must treat the wonder region as trivially passable rather than trying to flood-fill an animating volume — gate it on the region markers.

**Touches:** `MovingPlatformEntity.java`, `CourseCanvas.java`, `CourseReachability.java`

### Auto-scroll is a suppressed input, not a moving boundary  *(value 8, medium)*

**From:** Super Mario Maker 2 — forced scroll

A real forced scroll is a boundary that advances at a fixed rate and is lethal or blocking on contact: the player can move freely anywhere in front of it, may fall behind briefly, and is crushed or reset when the boundary catches them. The pressure comes from a visible object closing in, not from the controls being taken away.

**Why here:** This is a materially worse existing form, not a gap. Today `autoScroll` is implemented by dropping backward movement impulse, which reads as broken input rather than as pressure — the player presses left, nothing happens, and there is no on-screen reason. It also cannot create the tension that makes forced-scroll sections work, because there is nothing to lose ground to. Fixing this makes the flag worth setting, which in turn makes the per-segment auto-scroll already on the backlog worth building.

**Sketch:** Track a `scrollX` on `CourseState` advanced by `CourseTimerService` (it already ticks once a second and already routes failure through `DamageService.down`, which is exactly the right owner for 'the wall caught you'). Clamp the camera in `CameraDirector` to the boundary rather than to the player. Replace the impulse drop in `PlaneConstrainedInput` with a soft push when the player is behind the line, so backward input still does something. Render the boundary — a wall of the theme's block, or a particle curtain through `PickupParticles`' emitter path.

**Touches:** `PlaneConstrainedInput.java`, `CourseTimerService.java`, `CameraDirector.java`, `CourseState.java`

**Partly present already.** What is left: Keep the flag and the plumbing; replace the input suppression. Add a boundary entity (a lane-wide plane advancing at a rate carried in CourseState alongside autoScroll) that calls DamageService.down on overlap, and delete the impulse-zeroing branch at PlaneConstrainedInput.java:60 so the player can move freely and fall behind. Pair it with item 64 so a segment can own the boundary rather than the whole course.

### Doors and warp boxes — paired short-range teleports  *(value 8, medium)*

**From:** Super Mario Maker 1/2

A door links to exactly one partner elsewhere in the same course and moves the player between them instantly on interact; a warp box is the one-way, fire-and-forget version that a player enters and cannot return through. Both are course-internal: they restructure a level's topology without leaving it.

**Why here:** PlaneShift has exactly one transport block and it is wrong for this: `WarpPipeBlock.useWithoutItem` hard-codes `CourseService.loadCourse(player, "course_1")`, so right-clicking any pipe in any course dumps the player into course 1. The ghost-house theme exists, `LoopTriggerBlock` exists to build a maze, and a maze with no doors is a corridor. Doors are also the prerequisite for the key/keyhole and boss-door items already on the backlog — those are lock semantics on top of a door that does not exist yet.

**Sketch:** New `CourseDoorBlock` with a block entity holding a partner `BlockPos` in course-local coordinates; `CourseStructureService` resolves local to world at placement, so a regenerated course re-links correctly. Teleport through the same path `CourseCompletionService.beginSlide` uses (`teleportTo` plus `hurtMarked`) and re-pin `PlaneRail` on arrival, or the player lands off-lane. Fix the pipe while you are there: give `WarpPipeBlock` a destination from its block entity rather than a literal course id.

**Touches:** `WarpPipeBlock.java`, `ModBlocks.java`, `CourseStructureService.java`

**Partly present already.** What is left: Left to do: give WarpPipeBlock (and a new door block) a partner reference — a paired BlockPos in the blockstate or a small block entity — instead of the hardcoded course id, plus a one-way flag for the warp-box variant, and a SegmentLibrary segment that emits both halves so CourseReachability can treat the link as an edge in its flood-fill. Without that last part a door-linked course stops being provably walkable.

### Guarantee the first appearance of a mechanic cannot kill  *(value 8, small)*

**From:** Mario level grammar — safe introduction

The introduction of a mechanic is placed where failing it costs nothing: the donut block is over ground, the Thwomp is beside a path rather than over it, the firebar guards a coin rather than the only route. Only the *second* appearance may be lethal.

**Why here:** The composer's teaching rule is about difficulty rating, not about consequence: a segment marks a tag as taught as long as its own difficulty is under 3, and nothing checks whether that segment can kill. SegmentLibrary contains difficulty-2 segments that absolutely can — muncher_pit_hop, p_switch_bonus_room, donut_stagger — so a player's first ever donut block can be over a pit, which is exactly the failure CourseComposer's own javadoc says the rule exists to prevent ('Otherwise the player meets donut blocks for the first time over a lava pit'). The stated rule and the implemented rule are different rules, and the docs record neither.

**Sketch:** Add a boolean `safeIntro` (or a Tag.SAFE marker) to SegmentSpec, set true only on segments whose failure state is a recoverable fall onto ground. In CourseComposer.pick(), when a candidate would be the first use of a tag, require safeIntro; otherwise skip it. The composer already computes exactly this condition — `!taught.contains(tag)` inside the weight loop — so the check has a natural home. Add a JUnit case beside CourseLayoutPlanTest asserting over many seeds that for every tag, the first segment carrying it is safeIntro.

**Touches:** `CourseComposer.java`, `Segment.java`, `SegmentLibrary.java`

**Partly present already.** What is left: What is left is the difference between "gentle" and "cannot kill". The current rule keys on the segment's difficulty number, not on lethality — a difficulty-2 segment may still place a donut block over a pit or a Thwomp over the path, because a segment owns its own layout (each Segment.build writes its geometry directly, e.g. SegmentLibrary.java:654-674). Doing this properly means either a per-mechanic "introduction" segment variant that is guaranteed non-lethal (donut over ground, firebar guarding a coin), or a lethality flag on SegmentSpec that the taught-check consults instead of difficulty >= 3.

### Gold Ring — a pass-through gate that turns the segment's enemies to gold  *(value 8, medium)*

**From:** New Super Mario Bros. 2 — the ring that gilds every enemy on screen into a coin fountain

A ring the player runs through rather than hits. Passing it gilds every enemy currently loaded in the segment for a fixed window: gilded enemies trail a coin behind them as they walk, pay a large coin bonus when defeated, and burst into a spray of coins when the window ends whether they were defeated or not. A gilded Bullet Bill leaves a line of coins along its flight; a gilded Koopa shell kicked through a line of enemies gilds each one it passes, chaining the effect. Nothing about the enemies' behaviour changes — only their payout — so the ring is a pure risk/reward layer over geometry that already exists.

**Why here:** PlaneShift has a coin economy with real teeth (100 coins is a 1-Up, coins feed the Toad shop, coins are 100 points each) and no way to convert skill into a coin burst. The gold ring is the answer, and it costs almost nothing here because the payload is coins and the mod already has a coin item, a coin pickup path, a stomp-chain ladder and a shell that kills other enemies. It is also the cleanest possible reason to run *into* a group of enemies rather than around them, which is the behaviour the current enemy design never rewards.

**Sketch:** A new block implementing entity-collision rather than HitFromBelowBlock (CoinRingBlock is the wrong shape — it is hit from below and drops eight coins once). On collision, use BlockAreaScan's bounded pattern to find CourseEnemyEntity instances in the segment box and set a `gilded` byte on each with an expiry tick. In CourseEnemyEntity.tick, gilded enemies drop a coin every N ticks and on death award a multiplied CourseScoringService.awardCoin. Extend the shell-kill path (KoopaEntity's sliding state already defeats other enemies and calls awardShellKill with a combo step) to propagate the gild. Add a Segment to SegmentLibrary tagged ENEMY that places the ring in front of an ENEMY_LINE.

**Touches:** `CoinRingBlock.java`, `CourseEnemyEntity.java`, `KoopaEntity.java`, `CourseScoringService.java`, `SegmentLibrary.java`

### An open zone with a recurring timed threat  *(value 8, large)*

**From:** Super Mario Odyssey / Bowser's Fury

One continuous, freely-explorable area instead of a corridor: no start-to-flagpole line, several objectives visible from anywhere, checkpoint flags that double as fast-travel points, and collectibles scattered at all heights. Over it runs a clock — every few minutes a world-scale threat arrives, the sky changes, hazard geometry rises out of the ground, a large hostile presence sweeps the zone, and it lasts a fixed window before receding. Objectives remain solvable during the storm, some *only* during it, so the threat is a rotating rule change rather than an interruption.

**Why here:** Everything in PlaneShift is a corridor. `CourseDefinition.length` is bounded 64–2048, `PlaneRail` pins a travel axis, `CourseReachability` proves a linear path, and MISSING_MECHANICS 61 concedes "everything is currently a horizontal corridor." A single open zone would be the mod's one non-linear space and would give the FREE_3D mode — which currently exists but is only used as a wide ribbon — its own reason to exist. The timed threat is what stops an open zone from being formless: it imposes rhythm without imposing a route, which is exactly what a procedurally generated space needs. This is the biggest item here and should be scoped as one hand-authored zone, not a generated one.

**Sketch:** A `CourseDefinition` variant with `startMode = FREE_3D` and no flagpole; suppress the corridor rules by giving `MovementRuleService` and `CourseReachability` an open-zone branch rather than fighting `PlaneRail`. The threat cycle is a server tick service in the `CourseTimerService` mould — that class already counts `CourseState.timeLeft` down once a second and routes expiry through `DamageService.down`, so a phase clock is the same shape with a different consequence. Checkpoint fast-travel extends `CheckpointService`, which already stores respawn points. Objectives are the dense collectible from the Power Moon item above. Sky and music changes go through `CourseThemeService` and `CourseMusicManager`, which already owns the MUSIC channel while in a course.

**Touches:** `CourseDefinition.java`, `CourseTimerService.java`, `CheckpointService.java`, `CourseThemeService.java`, `CourseMusicManager.java`, `CourseReachability.java`

### Snake block — a rideable train that draws its own path  *(value 7, medium)*

**From:** Super Mario Maker 2

A platform of several linked segments that walks a fixed authored route at constant speed, head first, tail following through exactly the same cells. The player rides it; the route can climb, drop, double back and pass through gaps too narrow to stand in, so the ride is a timing and positioning problem rather than a free lift.

**Why here:** Related to but distinct from MISSING_MECHANICS #46 (waypoints on `MovingPlatformEntity`): #46 makes one rigid platform follow a line, whereas the snake's mechanic is the *train* — the surface exists only where the body currently is, so standing still on it is not safe and the player must walk along it as it turns. It is the single best carrier for the vertical sections the generator cannot yet produce, because a snake route can climb without needing a staircase.

**Sketch:** One entity owning the whole body, the way `FirebarEntity` deliberately owns a whole bar rather than one entity per flame — same reasoning, same cost argument. Body cells advance along a path array authored by the segment; collision is the union of the cells. `CourseCanvas.movingSurface` already exists to tell `CourseReachability` a span is traversable; extend it to accept a path so the walkability proof survives. New segment in `SegmentLibrary` tagged `MOVING, CLIMB`.

**Touches:** `FirebarEntity.java`, `CourseCanvas.java`, `SegmentLibrary.java`, `ModEntities.java`

### Twister — a rideable updraft column  *(value 7, small)*

**From:** Super Mario Maker 2

A volume of rising air that lifts anything inside it — the player, enemies, thrown objects — at a fixed rate while allowing full horizontal control and a jump out at any height. Some drift sideways along a track. Leaving the column returns normal gravity immediately, so the mechanic is choosing when to step out.

**Why here:** Distinct from MISSING_MECHANICS #13, which is a horizontal wind push. A vertical lift the player steers is the cheapest way for a horizontal-corridor generator to produce genuine height, and it is the natural partner for the vertical sections the backlog wants but cannot build. It also gives the cloud and sky themes a mechanic instead of a palette.

**Sketch:** A volume rather than a block: a marker-defined AABB registered per course, checked in the same per-entity tick hook `PowerupDriftService` uses (`EntityTickEvent`, explicitly chosen over level sweeps). Apply an upward velocity floor rather than a force so the lift rate is predictable, and leave horizontal input alone — `MovementRuleService` already folds cross-rail momentum onto the travel axis, which is what makes steering inside the column feel right in 2.5D. Emit through `PickupParticles`' emitter so the column is visible, which it must be.

**Touches:** `PowerupDriftService.java`, `MovementRuleService.java`, `SegmentLibrary.java`

### Pipes and blasters with authored contents  *(value 7, medium)*

**From:** Super Mario Maker 1/2

A pipe or launcher configured with what it produces and how often: an enemy on a cycle, a stream of coins, a single power-up on first approach. The source is visible, so the player learns the rhythm and plays around it rather than being ambushed by spawns from nowhere.

**Why here:** MISSING_MECHANICS #40 already notes that `spawnCast` places one enemy per set piece uniformly. The deeper issue is that enemies in PlaneShift have no *source* — they are placed at generation and, once defeated, the space is empty forever. A producing pipe makes a hazard into a persistent obstacle, gives the composer a difficulty lever that is not density, and makes a section that must be crossed rather than cleared. `BULLET_BILL_CANNON` proves the pattern is wanted; nothing generalises it.

**Sketch:** A block entity carrying an `EntityType` id, a period and a cap on live children, ticking on a scheduled tick rather than every tick (the `DonutBlock` and `AxeBlock` precedent — both use scheduled ticks specifically to avoid per-tick scans). Cap live spawns per source or a long course accumulates entities. `SegmentLibrary` gains a producer variant of `PIRANHA_PIPES` and `BULLET_BILL_GAUNTLET`; `CourseReachability` should ignore spawned entities, as it already does for the placed cast.

**Touches:** `BulletBillCannonBlock.java`, `CourseStructureService.java`, `SegmentLibrary.java`

**Partly present already.** What is left: Left: make contents and cadence data rather than constants — a small block entity (or a blockstate variant plus a spawn table id) holding {what, interval, first-approach-only}, so one block covers enemy-on-a-cycle, coin stream and single power-up, and SegmentLibrary picks the payload per segment Tag. Also delete or amend MISSING_MECHANICS.md item 35, which now misdescribes the code.

### ON/OFF as a course-wide metronome tied to the music  *(value 7, small)*

**From:** Super Mario Maker 2

Beyond the player-triggered switch, the ON/OFF state can be driven by a fixed beat that runs for a whole segment or course, flipping on a known interval that the music audibly marks. Blocks appear and vanish, conveyors reverse, and hazards arm and disarm in time. The player's job becomes reading the rhythm rather than hunting for a switch.

**Why here:** `OnOffBlock` and `OnOffSwitchBlock` already exist and work, but the state only ever changes when someone hits the switch, which makes it a door rather than a hazard. A timed toggle turns the same two blocks into rhythm-platforming with no new content. PlaneShift also already has `MusicBlock`, `PlaneshiftNoteBlock` and a `CourseMusicManager` that owns the music channel in-course, so the audio half — the part most games have to build from scratch — is done.

**Sketch:** A per-course toggle phase advanced by `CourseTimerService` (already the once-a-second server tick owner; run it on a shorter fixed interval). Broadcast the phase on `CourseState` so the client can pre-flash blocks a few ticks before a flip — the warning is what makes it fair. `OnOffBlock.setState` is already the single mutation point and takes a boolean, so nothing about the block changes. `CourseMusicManager` accents the beat, matching the existing hurry-up pitch-shift precedent. Add a `RHYTHM` segment to `SegmentLibrary` that only makes sense under the metronome.

**Touches:** `OnOffBlock.java`, `CourseTimerService.java`, `CourseMusicManager.java`, `SegmentLibrary.java`

**Partly present already.** What is left: Left: a course-level driver ticking OnOffBlock.setState on a fixed interval carried in CourseState (or per-segment), plus a tempo/phase value published by CourseMusicManager so the audible beat and the flip are the same clock rather than two that drift. Cache the BlockAreaScan result once per course — running the ±48 box every interval is the failure mode that class already warns about. Then item 57 (conveyors) and armed/disarmed hazards fall out of the same signal.

### Clear pipes — transparent tubes the player travels inside  *(value 7, large)*

**From:** Super Mario 3D World

A pipe you can see through and ride along rather than teleport between. The player enters, control switches to steering along the tube's path at a fixed speed, and the tube is visibly full of content: coins to collect on the way, enemies moving the other way that must be dodged inside the tube, junctions that branch, clear sections where the player is exposed to hazards outside the glass, and transparent stretches that let the player scout what is coming before committing.

**Why here:** The mod's only pipe is a teleporter, and a teleport is invisible — it removes gameplay for its duration. A clear pipe is a whole authored segment with its own tension, and it fits the 2.5D rail perfectly because the tube path *is* a rail. It also solves the pipe sub-room reward path that has been open since Phase 5 (MISSING_MECHANICS 62) more interestingly than a fade-to-black would.

**Sketch:** A `ClearPipeBlock` in `common/block` with a directional connection state, plus a `server/ClearPipeService` that owns a per-player traversal: while inside, suppress normal movement (the same suppression `CourseCompletionService.SLIDING_PLAYERS` uses during the flagpole slide) and advance the player along the segment path. Reuse `PlaneRail`'s axis reasoning for the tube direction. Render as a `noOcclusion()` translucent block — NEXT_TASKS task 33 (review R9) warns explicitly that any non-full-cube model must set `noOcclusion()` in `ModBlocks` or faces behind it get culled. Placement as a new `Segment.Tag.PIPE` builder next to the existing `piranha_pipes` and `illusory_pipe_cache`.

**Touches:** `WarpPipeBlock.java`, `ModBlocks.java`, `CourseCompletionService.java`, `SegmentLibrary.java`, `PlaneRail.java`


## progression

### Comet modifiers — remix a course the player has already cleared  *(value 10, medium)*

**From:** Super Mario Galaxy 1/2 (prankster comets)

After a course is cleared, it can later appear on the map wearing a modifier that changes one rule and nothing else, with its own separate clear record. Speedy: the clock is cut to a fraction and the run must be perfect. Daredevil: one pip, no power-ups, no checkpoints. Cosmic: a ghost racer replays the player's own best run and must be beaten to the flagpole. Purple: every coin in the course is replaced by a countable collectible and all of them must be found. Fast-Foe: every enemy moves at double speed. The modifier is announced before entry and the course geometry is untouched.

**Why here:** This is the highest value-per-line item on the entire list for a mod whose courses are procedurally generated. Fifty courses already exist and `CourseLayoutPlan` seeds each deterministically from world seed plus course id, so a comet turns fifty courses into a hundred and fifty with no new geometry, no new art, and no new segments. Every rule a comet needs is already a field: `CourseDefinition.timeLimitSeconds`, `CourseState.pips`, `CourseState.autoScroll`. It also finally gives `CourseProgress.Record.bestTimeLeft` and `bestScore` something to be *for*, and it directly answers the difficulty-curve gap (MISSING_MECHANICS 68) without regenerating anything.

**Sketch:** A `CourseComet` enum in `common/course` plus an optional field on `CourseProgress.Record` recording which comets have been cleared per course (the record already has a `Codec` and a `StreamCodec`, and adding an optional field is backward compatible). `CourseService.loadCourse` takes the comet and applies it as overrides to the `CourseDefinition` it resolves — clock override into `CourseTimerService`, pip clamp via `CourseStateAccess.update`, checkpoint suppression via `CheckpointService`, enemy speed via an attribute modifier applied in `MobReplacementService` or at spawn in `CourseWriter`. `WorldMapLayout.Node` gains a comet marker so `CourseMapScreen` can draw it, and `CourseSelectPayload` carries the choice. The cosmic ghost needs a recorded position track, so ship it last.

**Touches:** `CourseProgress.java`, `CourseDefinition.java`, `CourseService.java`, `CourseTimerService.java`, `WorldMapLayout.java`, `CourseMapScreen.java`

### Badges — equippable, persistent player modifiers  *(value 9, large)*

**From:** Super Mario Bros. Wonder (2023)

A slot, chosen on the world map before entering a course and independent of the power-up in hand, holding one modifier that changes a global rule for the whole run: how falling works, what a wall does on contact, what happens when the player would land in a pit, how much a coin is worth. Badges are earned or bought, are permanent once owned, and are swapped freely between attempts. They are not consumable and never conflict with a Form — a badge is who you are, a Form is what you picked up.

**Why here:** PlaneShift's Form system is a single active slot plus a reserve, and Forms are lost on damage. That means a player's expressive choice lasts about thirty seconds. A badge slot is the missing persistent axis: it gives a reason to re-enter a cleared course, a sink for a progression currency, and an accessibility dial (a safety badge) that is a player choice rather than a config toggle. It is also the cheapest possible answer to 'the same generated course felt different this time'.

**Sketch:** New `common/badge/BadgeDefinition` as a datapack registry beside `FormDefinition` — same shape: `category`, a code-defined `BadgeEffectKind` enum (mirroring `FormActionKind`'s trusted-behaviour rule), tuning fields. Store `owned: Set<Identifier>` and `equipped: Optional<Identifier>` on `CourseProgress` (it is already the persisted attachment and already has a codec pattern to copy). Read the equipped badge in `MovementRuleService.apply` and `AirMoveService` for movement badges, in `DamageService` for defensive ones, in `CourseScoringService` for economy ones. Equip UI belongs in `CourseMapScreen`, which already draws per-course state. Register the definitions in `ModRegistries` next to the form registry.

**Touches:** `CourseProgress.java`, `FormDefinition.java`, `ModRegistries.java`, `MovementRuleService.java`, `CourseMapScreen.java`

### Super Guide — a demonstration run offered after repeated failure  *(value 9, large)*

**From:** New Super Mario Bros. Wii — the golden block that appears after eight deaths and plays the course for you

The game counts consecutive failures on the current course. After a threshold (eight is the classic number, and it should be configurable), a distinctive golden assist block materialises at the current checkpoint. Activating it plays back a recorded traversal of the remainder of the course: the player becomes a non-interactive observer while a ghost avatar walks, jumps and defeats its way to the flagpole. The run counts as a clear for unlock purposes but is recorded differently — no best score, no best time, and any star coins the guide passes are not credited. The player may take back control at any point, resuming from wherever the guide had reached.

**Why here:** PlaneShift's courses are procedurally generated and proven walkable by CourseReachability's flood-fill, which means the game already knows a solution path exists — it just throws that knowledge away after the assertion. A demonstration is nearly free here in a way it never was for a hand-authored game: the flood fill's predecessor chain is the demo. And because the generator can produce a course nobody has ever played, an escape hatch matters more here than in a game where every jump was human-tested. This is the highest value-to-effort item on this list for exactly that reason.

**Sketch:** Have CourseReachability retain the parent pointers it already builds during the flood fill and expose a solutionPath(from, to) returning the waypoint list; store it on the generated course alongside the layout in CourseWriter/GenContext. Track consecutive failures per player per course in CourseProgress.Record (add a `failures` int to the codec — the codec is already versioned with optionalFieldOf defaults, so old saves load fine) and reset it on clear. DamageService.resolveDown increments it. When it crosses the threshold, CheckpointService places a new GuideBlock (a HitFromBelowBlock like the existing QuestionBlock) at the checkpoint. Activation sets PlayState to a new GUIDE state, spawns a simple path-following display entity, and steps it along the waypoints; ProgressionService.recordClear takes a flag suppressing bestScore/bestTimeLeft.

**Touches:** `CourseReachability.java`, `CourseProgress.java`, `DamageService.java`, `CheckpointService.java`, `QuestionBlock.java`, `PlayState.java`

### Opening badge roster — five modifiers that each change a different rule  *(value 8, medium)*

**From:** Super Mario Bros. Wonder (2023)

Concrete first badges, chosen so no two touch the same system. (1) Canopy Descent: holding jump at apex slows the fall to a constant, steerable rate. (2) Wall Kick: contact with a vertical face refreshes the jump once per face, tracked per-wall so it cannot be spammed on one surface. (3) Tether Line: an aimed grapple that pulls the player to a hooked block and swings; range-limited, one shot in the air. (4) Safety Net: the first pit fall of a run is caught and returns the player to the ledge edge instead of the checkpoint — once, and the HUD shows it is spent. (5) Prospector: every coin pays double but the pip buffer is reduced by one, so the economy badge is a real trade.

**Why here:** A badge framework with one badge is a config option. Five, each landing in a different service, is what proves the abstraction and gives the world map something to unlock. Safety Net in particular gives PlaneShift an accessibility answer that is earned and visible rather than hidden in a TOML.

**Sketch:** Each maps to a `BadgeEffectKind` constant: `SLOW_FALL` read in `AirMoveService` alongside the existing Glider float; `WALL_KICK` implemented with the consecutive-same-face contact tracking that MISSING_MECHANICS #11 already identifies as the correct fix, so the badge and the fix are one job; `TETHER` as a new projectile in `common/entity` reusing `ProjectileTracker`; `SAFETY_NET` as an early return in `DamageService.down` when the cause is the kill-Y check in `CourseService`; `COIN_DOUBLE` in `CourseScoringService` plus a starting-pip override in `CourseState`.

**Touches:** `AirMoveService.java`, `DamageService.java`, `CourseScoringService.java`, `ProjectileTracker.java`

**Partly present already.** What is left: Build the badge slot plus Tether Line, Safety Net and Prospector. Do NOT reimplement Canopy Descent or Wall Kick: wire the badge slot to the existing RoleSignature.FLOAT_GLIDE path and to AirMoveService's wall-jump toggle (config wallJump, currently false), and check ModCompatibility for omni/enhancedmovement before enabling the wall badge at all.

### A spendable cross-course progression currency  *(value 8, small)*

**From:** Super Mario Bros. Wonder (2023) — Wonder Seeds

A single global currency earned in chunks per course — one for the flagpole, one for the transformation event, one per hidden collectible — that accumulates across the whole save and is spent to unlock worlds, badges and shop stock. Unlike coins it never resets, and unlike a per-course counter it is one number the player can watch grow.

**Why here:** PlaneShift's persistence is currently a per-course scorecard: cleared, star coins, best score, best time. Nothing aggregates. That means the world map can only gate on 'cleared the previous course', star coins are a number that does nothing, and the Toad shop spends in-course coins that vanish on death anyway. One global counter fixes all three at once and is the prerequisite for the badge economy.

**Sketch:** Add `seeds` (int) to `CourseProgress` — it is already the durable attachment with a codec and stream codec, and adding a field is the pattern `bestTimeLeft` established. Award in `CourseCompletionService.onComplete` next to `ProgressionService.recordClear`, and on star-coin pickup. Change `ProgressionService`'s unlock predicate from a linear cleared-check to a seed threshold per `WorldDefinition`. Change `ToadShopService.Offer.price` to read the persistent balance rather than `CourseState.coins`, or keep both currencies and make the shop sell consumables for coins and badges for seeds.

**Touches:** `CourseProgress.java`, `CourseCompletionService.java`, `ProgressionService.java`, `ToadShopService.java`

**Partly present already.** What is left: Add a spent/balance field to CourseProgress (a single int alongside records, so totalStarCoins() - spent is the wallet), a debit path in ProgressionService, and make ToadShopService.Offer prices and WorldRegistry unlock rules read that wallet instead of CourseState.coins(). Also credit the flagpole and, if the Wonder Flower lands, its end marker — currently only star coins credit.

### Clear conditions — a per-course objective beyond touching the flag  *(value 8, medium)*

**From:** Super Mario Maker 2

A course may carry a second requirement announced at the start and tracked on the HUD: reach the flag without taking damage, without landing on the ground, having defeated a set number of a named enemy, holding a specific Form, or having collected every coin. Failing the condition does not kill the run — the flag simply does not accept it, and the HUD says why the whole time.

**Why here:** PlaneShift generates courses from a seeded grammar, which means it can produce infinite layouts but has exactly one thing to ask of the player. A clear condition is content-free replay value: the same generated course, played under a different rule, is a different course. It also gives the difficulty curve a lever that is not 'more pits', which is precisely the complaint recorded against the current curve.

**Sketch:** Add an optional `clearCondition` to `CourseDefinition` (it is already a codec'd datapack record) and a matching tracked-progress field on `CourseState` so the HUD gets it for free through the existing sync. A code-defined `ClearConditionKind` enum keeps the trusted-behaviour rule that `FormActionKind` establishes. Evaluate in `CourseCompletionService.onComplete` before `beginSlide` commits; on failure, refuse the slide and play the existing deny sound. Counters increment where the events already fire — `CourseScoringService` for defeats and coins, `DamageService` for the no-hit case.

**Touches:** `CourseCompletionService.java`, `CourseDefinition.java`, `CourseState.java`, `CourseHud.java`

### Star Coins as a spendable currency that gates content  *(value 8, medium)*

**From:** New Super Mario Bros. — star coins buying open the doors, hint movies and bonus worlds

Star coins stop being a score and become a key. Specific things on the world map are locked behind a star-coin price and consume the coins when opened: an alternate route past a hard castle, a bonus course, a permanent Form unlock in the Toad shop, a cosmetic. The price is paid from a running total, and spent coins stay spent — the per-course record still shows which three were found, so nothing is lost from the completion display, but the spendable pool is separate and decreases.

**Why here:** This is the clearest dangling thread in the progression code. CourseProgress tracks three star coins per course, credits them, caps them, sums them across every record, and CourseMapScreen draws the pips — and then nothing in the entire mod reads totalStarCoins() for any purpose. A collectible with no sink is a number, and the mod already went to the trouble of placing them on the hardest segments of every generated course. Giving them a price turns the completionist route into an actual choice about what to buy.

**Sketch:** Add a `starCoinsSpent` int to CourseProgress (the codec uses optionalFieldOf throughout, so existing saves load unchanged) and a spendable() helper returning totalStarCoins() - starCoinsSpent. Add a NodeType.STAR_GATE to WorldMapLayout carrying a price, placed deterministically the same way TOAD_HOUSE and CANNON already are. MapNodeService.enter gains a STAR_GATE branch that validates the price against ProgressionService, deducts, and marks the gate opened in the record map. ToadShopService.Offer already has a price field — add star-coin-priced offers alongside the coin ones. CourseMapScreen draws the price on the node.

**Touches:** `CourseProgress.java`, `WorldMapLayout.java`, `MapNodeService.java`, `ToadShopService.java`, `CourseMapScreen.java`

### Toad Houses that are consumed, and three kinds of them  *(value 8, small)*

**From:** New Super Mario Bros. — red, green and star houses, each single-use

A Toad House is spent when it is used and its node visibly closes. There are three kinds rather than one: an item house that hands over a Form of a tier appropriate to the world; a 1-Up house that plays a small skill or memory game for extra lives rather than handing them over; and a star house that grants a timed invincibility carried into the next course. Which kind sits on a given world's map is deterministic from the world id, so a player can learn the map.

**Why here:** This is a live exploit, not just a missing feature. MapNodeService.visitToadHouse rolls a random gift from an eight-item list and drops it at the player's feet with no record of the visit being written anywhere — the player can re-enter the same node indefinitely and print Fire Flowers, Cat Suits and 3-Ups without limit. The WorldMapLayout.NodeType comment even documents the intended behaviour as 'A free power-up, once per save', so the code contradicts its own specification. Beyond the fix, three house types is what makes a world map feel like it has places in it rather than one repeated vending machine.

**Sketch:** Add a used-nodes set to CourseProgress (a Set<String> of node ids, codec'd as a list with an optionalFieldOf default, bounded the same way MAX_RECORDS bounds the record map). MapNodeService.visitToadHouse checks and writes it, and findNode returns null for a spent id so the packet is rejected server-side rather than filtered client-side. Add HouseKind to the TOAD_HOUSE node, derived from a hash of the world id in WorldMapLayout.forWorld so it stays deterministic. The 1-Up game can reuse ToadShopScreen's existing screen scaffolding. CourseMapScreen greys out spent nodes.

**Touches:** `MapNodeService.java`, `CourseProgress.java`, `WorldMapLayout.java`, `CourseMapScreen.java`, `ToadShopScreen.java`

**Partly present already.** What is left: Left to build: (1) a consumed-nodes set on CourseProgress plus a greyed/closed draw in CourseMapScreen — this is a bug fix the code already claims to have, do it first and cheaply; (2) the 1-Up minigame house and the star house (timed invincibility carried into the next course — note docs/MISSING_MECHANICS.md item 81, power-up carry between courses, is struck through as done, so the carry path exists); (3) deterministic kind-from-world-id, which is a two-line change in WorldMapLayout.forWorld where the node is already built from world.worldId(). The item house itself is done.

### Flagpole height bonus  *(value 7, small)*

**From:** Mario goal — reward for grabbing the pole high

The score awarded at the goal scales with the height at which the player contacts the pole, in bands from the base to the top, with the top band worth a large bonus (and traditionally a 1-Up). The pole stops being a finish line and becomes the last skill test in the course.

**Why here:** FlagPoleBlock is built as a BASE / POLE×6 / TOP stack — the composer explicitly places eight distinct parts — and entityInside ignores which part was touched entirely, handing the same result to a player who limped in on the ground and one who launched off a trampoline into the top. The height information is right there in the block state and is discarded. This is the payoff of the eight blocks the generator already spends, and it is not on any list (MISSING_MECHANICS #96, the slide, is marked done).

**Sketch:** In FlagPoleBlock.entityInside, read state.getValue(PART) and pos.getY() relative to the marker/base height, convert to a band, and pass the band into CourseCompletionService.beginSlide. Score it through CourseScoringService.addScore before finishCourse runs (order matters — finishCourse adds bonuses to the running total and then the state is reset), and send a ScorePopupPayload so the number appears at the pole. Award the 1-Up through the same `s.withLives(s.lives() + 1)` path awardStomp uses.

**Touches:** `FlagPoleBlock.java`, `CourseCompletionService.java`, `CourseScoringService.java`

### A teaching ledger that persists across courses in a world  *(value 7, medium)*

**From:** Mario level grammar — safe introduction, once, then build on it

What the player has been taught is a property of their run, not of one level. Once a mechanic has been safely introduced in world 1-2 it stays taught for the rest of world 1, so later courses can open with it under pressure instead of re-teaching it; a new world introduces its own new mechanics but does not re-explain the old ones.

**Why here:** CourseComposer's `taught` set is a local EnumSet created fresh at the top of compose(), so every one of the ~50 courses believes the player has never seen a donut block, a Thwomp or a firebar. That is why difficulty scaling keeps hitting a ceiling: no matter what a late-world course is allowed to do, it must first spend segments re-introducing every mechanic it wants to use hard. It is also why MISSING_MECHANICS #68 ('World 5 generates with the same parameters as world 1') is only half the story — the parameters are one problem, the amnesia is the other, and it is not recorded anywhere.

**Sketch:** CourseProgress already persists per-player across sessions (currentCourse, bestScore, bestTimeLeft, star coins) and ProgressionService is the accessor. Add a persisted Set<Segment.Tag> taught-per-world to CourseProgress, populate it in CourseService when a course is cleared (from the Composition.segmentIds already returned for exactly this kind of introspection), and pass it into CourseComposer.compose as a seed for the local `taught` set. Keep composition deterministic by making it a parameter rather than a lookup inside the composer, so CourseLayoutPlanTest and the 3,000-seed walkability proof stay reproducible.

**Touches:** `CourseComposer.java`, `CourseProgress.java`, `ProgressionService.java`, `CourseService.java`

### Nabbit-style assist character mode  *(value 7, medium)*

**From:** New Super Mario Bros. U Deluxe — the player character who cannot be hurt by enemies and cannot hold power-ups

A selectable role, not a pickup: the player is permanently immune to enemy contact and projectiles, but cannot hold or pick up any Form (touching one converts it to coins), cannot use a Form action, and still dies to pits and the clock. Star coins and course clears count normally, so a mixed party genuinely progresses together. In co-op the assist character can still be bubbled by falling and can still be grabbed and thrown, so they remain a participant rather than a spectator.

**Why here:** PlaneShift has a role system with datapack-defined roles and a RoleService that applies jump multipliers, so 'a role with a different damage contract' is the natural shape here — it is a role JSON plus a rule, not a new subsystem. It solves the specific problem this mod will hit hardest: a Minecraft player who wants to play with a friend who has never played a platformer. Distinct from the invincibility assist Form above, because it is a permanent, chosen identity with a real cost rather than a temporary handout, and the cost is what keeps it from being the obvious choice for everyone.

**Sketch:** Add a role JSON with a new boolean field on PlayerRole (the codec already declares bounded numeric fields and there is a checkDataRanges build check validating them, so extending it is a known-safe path). RoleService exposes isAssistRole(player). DamageService.interceptDamage returns true early for that role except on pits and kills. FormService.grant returns false for that role and instead calls CourseScoringService.awardCoin a few times. The role picker UI already exists as part of the role system, so the selection point is free.

**Touches:** ``, `RoleService.java`, `DamageService.java`, `FormService.java`

### Roaming enemy encounters on the world map  *(value 7, medium)*

**From:** New Super Mario Bros. — the enemy that wanders the map and blocks your path until you fight it

An enemy token occupies a node on the world map and moves one node along the path each time the player enters and leaves a course. Bumping into it launches a short, fixed, single-room encounter course — an arena with a handful of enemies and a time limit — whose reward is a Form or coins. The token can be dodged if the player takes a branch around it, so it is a tax on the direct route rather than an unavoidable fight. Clearing an encounter removes that token; losing one costs a life and pushes the token onto the node the player was heading for.

**Why here:** PlaneShift's map is already a real place — a serpentine path with links, a token the player walks between nodes, Toad Houses and a cannon — which means it is one node type away from having something happen *between* courses. That is the whole point of a world map as a layer rather than a menu: the space between levels has stakes. The encounter room is also nearly free content, because CourseComposer can already generate a short course and docs/MISSING_MECHANICS.md item 72 notes the mod has nothing short and intense.

**Sketch:** Add NodeType.AMBUSH to WorldMapLayout and a per-world token position stored in CourseProgress alongside the used-nodes set. Advance the token in ProgressionService.leaveCourse. CourseMapScreen already walks links to move the player token in its move() method — reuse the same link-walking to advance the enemy and to detect a collision when the player's target node is occupied. On collision, MapNodeService.enter routes to CourseService.loadCourse with a generated short course id; GenContext already carries difficulty, so pass a single-segment arena spec through CourseComposer.

**Touches:** `WorldMapLayout.java`, `MapNodeService.java`, `ProgressionService.java`, `CourseMapScreen.java`, `CourseComposer.java`

### Coin Rush — a timed multi-course run scored on coins alone  *(value 7, medium)*

**From:** New Super Mario Bros. 2 — the run of consecutive courses where the only score is coins and the clock carries over

A mode entered from the world map rather than a course: three courses back to back with a single clock that does not reset between them, carrying leftover time forward as a bonus. The only score is coins; enemies are worth nothing unless they pay coins. Dying once ends the run and banks nothing. The final total is written to a personal best, and the run's seed is a shareable value so two players can attempt the same three courses.

**Why here:** PlaneShift's generator is its strongest asset and its meta layer does not use it: courses are generated once per id from world seed plus course id and then played linearly. A mode that takes an arbitrary seed and produces three fresh courses turns the generator into replay value instead of a one-time content pipeline, and it does so with no new level content at all. It also gives the coin economy and the existing clock a purpose beyond a per-course pass/fail, and gives CourseProgress's bestScore/bestTimeLeft fields — which currently only appear on the results screen — a leaderboard-shaped home.

**Sketch:** A CoinRushService in server/ holding an ordered list of generated course ids, a carried clock and a coin total. It reuses CourseService.loadCourse for each leg, but suppresses the results screen between legs and passes the remaining timeLeft into the next course's CourseState instead of the CourseDefinition's timeLimitTicks. GenContext already takes a seed, so seeds come straight from user input. Death routes through DamageService.resolveDown with a flag that ends the run rather than decrementing lives. Add an entry point on CourseMapScreen and a personal-best field to CourseProgress.

**Touches:** `CourseService.java`, `CourseTimerService.java`, `GenContext.java`, `CourseProgress.java`, `CourseMapScreen.java`

### Dense small collectibles that gate progress by count, not by clear order  *(value 7, medium)*

**From:** Super Mario Odyssey (Power Moons)

A second collectible tier sitting between the coin and the star coin: dozens per course rather than three, awarded for small discrete acts — clearing a hidden room, defeating a specific enemy, reaching an out-of-the-way ledge, completing a timed challenge, being the first to bump a particular block. World access is bought with a running total rather than earned by clearing the previous course in order, so a player stuck on one course can go find collectibles in earlier ones and still move forward.

**Why here:** Progression is currently strictly linear — five worlds of ten courses each in `WorldRegistry`, unlocked by clearing the previous one, enforced by `ProgressionService`. That means one course the player cannot beat is a wall with no way around it, which for a *procedurally generated* course set is a real risk: a hard seed is not a designed difficulty spike, it is bad luck. A count-based toll makes the generator's variance survivable, and it gives the fifty existing courses a reason to be revisited. Star coins (three per course, already placed on the hardest segments) are too sparse to serve this role.

**Sketch:** Reuse the star-coin plumbing: `CourseProgress.Record` already persists a per-course `starCoins` count with a bounded codec, so add a parallel bounded `moons` count the same way (both codecs are optional-field, so old saves load). Award through `CourseScoringService`, which already has award hooks per pickup type. Change the unlock predicate in `ProgressionService` and mirror it in `WorldRegistry`'s client-visible rule — the class doc is explicit that the rule must be a pure function of `CourseProgress` so the client can grey out honestly while the server refuses the load. `WorldMapLayout` draws the toll on the link into each world.

**Touches:** `CourseProgress.java`, `ProgressionService.java`, `WorldRegistry.java`, `WorldMapLayout.java`, `CourseScoringService.java`

### Goal pole height and the timed-clear bonus  *(value 7, trivial)*

**From:** Super Mario 3D Land / 3D World

The flagpole is scored, not just touched. Grabbing the pole higher pays exponentially more, with the very top paying a life; the height reached is recorded per course so a cleared course still has something left to improve. Separately, the finish is graded on the clock: hitting the pole with the last digit of the timer matching a target, or under a par time, pays a bonus and marks the course record. A pole reached from a nearby platform at full height is a small skill test at the end of every single course.

**Why here:** This exists in a materially worse form right now. `FlagPoleBlock` already has a three-part `BASE`/`POLE`/`TOP` blockstate — the height information is literally in the block being touched — and `CourseCompletionService.beginSlide` throws it away, taking only the position. `CourseScoringService.finishCourse` computes only a pip bonus and a flat per-second time bonus. Reading the part the player actually hit is a handful of lines against a scoring system that already persists `bestScore` and `bestTimeLeft`, and it converts the ending of all fifty courses from a formality into a reason to replay.

**Sketch:** `FlagPoleBlock.entityInside` already receives the `BlockState`; pass `state.getValue(PART)` and the pole's own Y through `CourseCompletionService.beginSlide` into a stored per-player value, then read it in `CourseScoringService.finishCourse` where `healthBonus` and `timeBonus` are already summed. Add a bounded `bestPoleHeight` field to `CourseProgress.Record` next to `bestTimeLeft` — both its `Codec` and `StreamCodec` are composite and extend cleanly. Surface it on `CourseResultsScreen` via `CourseResultsPayload`, which already carries score and time.

**Touches:** `FlagPoleBlock.java`, `CourseCompletionService.java`, `CourseScoringService.java`, `CourseProgress.java`, `CourseResultsPayload.java`

**Partly present already.** What is left: Missing: (1) height sensitivity — `beginSlide(player, polePos)` receives the pole position but never reads which PART was touched or the player's Y, so every grab scores identically and the top pays no life; (2) a per-course best-pole-height field on `CourseProgress.Record` (currently `cleared, starCoins, bestScore, bestTimeLeft`, CourseProgress.java:45) plus its codec and stream codec at :49-64; (3) the graded finish — a par time or last-digit target paying a marked bonus, as opposed to today's flat linear time bonus. (1) is a handful of lines given the PART property already exists.

### Flagpole height payout and the top-of-pole bonus  *(value 6, trivial)*

**From:** New Super Mario Bros. — grabbing the pole high pays more, and the very top pays a life

The height at which the player contacts the flagpole is scored. The pole is banded from bottom to top with escalating point values, the top band pays an extra life, and hitting the top from a wall-jump or a Form launch is worth deliberately setting up. In co-op the highest grab in the party is what counts, so the run's finish is a small competition rather than a formality. A fireworks flourish fires when the finishing coin count ends in a matching digit, giving the coin total a second, non-obvious use.

**Why here:** PlaneShift already built the expensive half of this: FlagPoleBlock calls CourseCompletionService.beginSlide, which teleports the player to the pole and plays the slide, and docs record the slide and fanfare as done. But beginSlide throws the contact height away — it snaps the player's X and Z to the pole and preserves Y only incidentally, and onComplete's scoring reads pips, coins and clock, never height. So the mod has the animation of a skill expression with none of the skill. It is a handful of lines to make the last five seconds of every course worth optimising.

**Sketch:** Pass the contact Y into CourseCompletionService.beginSlide (FlagPoleBlock already has the BlockPos and the player) and store it with the SLIDING_PLAYERS entry, which is already a per-UUID map. In onComplete, convert (contactY - poleBaseY) into a band index and call CourseScoringService.addScore with the band value; the top band additionally increments lives, reusing the 1-Up path already written into onComplete's coin loop. Add the height bonus to CourseResultsPayload so the results screen shows it — the payload already carries finalScore, timeLeft, timeBonus, coins, starCoins and lives, so it is one more int.

**Touches:** `FlagPoleBlock.java`, `CourseCompletionService.java`, `CourseScoringService.java`, `CourseResultsPayload.java`

### A persistent costume wardrobe and a shop that is not consumables-only  *(value 6, medium)*

**From:** Super Mario Odyssey

Two currencies and a wardrobe. Coins buy consumables as they do now; a second, scarcer regional currency found only in a given world buys that world's costumes, which persist across courses and saves. Costumes are mostly cosmetic, but a handful are keys — a specific outfit opens a door or satisfies an NPC that nothing else will, so the wardrobe is a soft collectible checklist with occasional teeth. Purchases are permanent, so money spent is progress banked rather than a consumable burned.

**Why here:** `ToadShopService` has ten offers, every one a consumable, all bought with in-course coins, and all of them evaporate on death or course exit — so there is nothing in the mod a player can *own*. That leaves coins with no long-term meaning, which is why the 100-coins-to-1-Up rule is currently the only sink. A wardrobe gives the run-to-run economy a purpose, gives the world map's Toad Houses a reason to be revisited beyond a one-shot random gift, and is a natural home for the art work already queued (NEXT_TASKS tasks 25–27).

**Sketch:** Persist owned costumes in a new attachment alongside `CourseProgress` via `ModAttachments`, which already registers the progress attachment and gives save/sync for free. Extend `ToadShopService.Offer` from a flat enum into a record carrying price, currency and permanence, and split the purchase switch accordingly; the `PayloadRateLimiter.Action.SHOP_PURCHASE` guard and the `nearToad` proximity check already there stay as-is and remain the security boundary. `ToadShopScreen` and `ToadShopPurchasePayload` gain a tab and a currency field. Costume-as-key checks go in `SecretPassageBlock`/`ShiftGateBlock`-style gate blocks and in `ToadDialogueService`.

**Touches:** `ToadShopService.java`, `ToadShopScreen.java`, `ToadShopPurchasePayload.java`, `ModAttachments.java`, `ToadDialogueService.java`

### A collectible toll that opens map nodes  *(value 6, small)*

**From:** Super Mario Galaxy (Hungry Lumas), Odyssey

An NPC standing on the world map who asks for a quantity of a collectible and, when fed, transforms into a new node — a bonus course, a shortcut link between two branches, a Toad House, a warp to a later world. The toll is visible from the moment the world opens, so the player can see what their collecting is buying and choose to pursue it. Feeding is permanent and per-save.

**Why here:** `WorldMapLayout` is a genuinely nice piece of work — a computed winding path with Toad Houses, a cannon and a castle — and it is entirely static: the same five node types, laid out deterministically, forever. A toll node is the cheapest possible way to make the map change in response to the player, which is what turns a level select into a place. It also closes the loop on the dense-collectible tier: without a sink, more collectibles is just a bigger number.

**Sketch:** Add a `TOLL` constant to `WorldMapLayout.NodeType` (already an enum of five with a `Node` record carrying id, type, x, y, index) and a per-save set of paid tolls in the progression attachment. Handling goes in `MapNodeService.enter`, which already switches on node type and — importantly — already validates the node against the generated map rather than trusting the packet id, with the class doc explaining exactly why ("a Toad House pays out a power-up, so an unvalidated id is an item printer"). The same validation must cover a toll, since it mutates the map. Drawing is a new case in `CourseMapScreen`.

**Touches:** `WorldMapLayout.java`, `MapNodeService.java`, `CourseMapScreen.java`, `MapNodePayload.java`, `CourseProgress.java`


## multiplayer

### Bubble down-state instead of life loss in co-op  *(value 10, large)*

**From:** New Super Mario Bros. Wii / U — the floating bubble a downed player occupies until a teammate pops it

When two or more players are inside the same course and one takes a lethal hit or falls past killY, they do not consume a life and do not return to a checkpoint. They enter a BUBBLED state: a drifting, collision-free, input-limited ghost that floats toward the nearest living party member and can nudge itself horizontally. Any living player who touches the bubble pops it, and the bubbled player re-enters play at that point with one pip and standard invulnerability. If every player in the party is bubbled simultaneously, only then is a shared life consumed and the whole party restarts from the last checkpoint. A player may also bubble voluntarily on a key press, which is the standard way to escape an unwinnable position without spending a life.

**Why here:** PlaneShift already has multiple players in a course and a fully-specified per-player pip/life model, but the failure path is strictly solo: DamageService.resolveDown decrements that one player's lives and teleports them alone to their own checkpoint. In a fixed side-on camera game that means one player's death yanks them out of the shared frame while everyone else keeps playing, which is exactly the situation the bubble was invented to solve. It is also the single cheapest way to make co-op forgiving enough to be worth playing, and PROGRESS.md admits co-op has never been played at all.

**Sketch:** Add BUBBLED to PlayState (common/mode/PlayState.java) next to the existing DOWNED. In DamageService.resolveDown, branch before the life decrement: if CourseCoopService reports another non-bubbled party member in the same course, set BUBBLED, disable collision and damage, and register the player in a new BubbleService tick that eases their position toward the nearest living member (a clamped lerp along the PlaneRail travel axis so the bubble stays on the 2.5D lane). Pop on proximity in ServerEvents' existing per-player tick, restoring PlayState.playingFor(mode) with pips=1 and invulnUntil = now + DamageService.INVULN_TICKS * 2. Sync via the existing CourseState STREAM_CODEC (PlayState is already a synced field, so the HUD and CourseHud need only a new badge). Wire the voluntary bubble to a new KeyMapping in PlaneShiftKeybinds plus a payload alongside the existing swap-reserve payload.

**Touches:** `PlayState.java`, `DamageService.java`, `CourseCoopService.java`, `ServerEvents.java`, `PlaneShiftKeybinds.java`, `CourseHud.java`

### Party course session — one course instance the whole group enters and leaves together  *(value 9, large)*

**From:** New Super Mario Bros. Wii/U — every player enters a course from one map token and the run is a shared object

A course run becomes a party-scoped object rather than a per-player one: a shared life pool, a shared clock, a shared score, a shared checkpoint, and a single entry/exit. When the host selects a node on the world map, every party member in the hub is loaded into the same course dimension instance at the same start position. Course completion, game over, and checkpoint activation resolve once for the party rather than once per player. A player who joins mid-run enters bubbled at the current camera position.

**Why here:** Right now everything about a course is keyed to one ServerPlayer, so two players in the same course are two independent runs that happen to share a dimension. That produces concrete nonsense: two clocks counting down separately, two independent game-overs, two checkpoints, and the existing CourseCoopService.shareLives is an outright multiplier bug — a single 1-Up pickup calls shareLives, which adds that many lives to every other party member on top of the one the picker already got, so lives inflate with party size. NSMB's whole meta layer assumes a party. Without this, none of the other co-op mechanics have anywhere to live.

**Sketch:** Introduce a CourseParty record (leader UUID, member UUIDs, courseId, shared lives, shared score, shared clock, shared checkpoint GlobalPos) owned by a new CourseSessionService and keyed by leader UUID on the server. CourseService.loadCourse gains an overload taking a party and loops the existing single-player path over its members. CourseTimerService ticks the party clock once instead of once per player and pushes the value into every member's CourseState so the existing HUD keeps working unchanged. CourseScoringService's per-UUID CHAINS map stays per-player (chains should be individual) but addScore routes to the party total. CheckpointService.activate writes to the party. Fix shareLives by deleting it and making the 1-Up pickup credit the party pool once.

**Touches:** `CourseService.java`, `CourseCoopService.java`, `CourseTimerService.java`, `CheckpointService.java`, `CourseScoringService.java`, `CourseCompletionService.java`

### Ghost-and-revive co-op with placeable standees  *(value 8, medium)*

**From:** Super Mario Bros. Wonder (2023)

A downed player in co-op does not lose a life immediately — they become a drifting ghost for a window, and any living player touching them restores them at cost of nothing but time. In addition, any player may plant a standee, a personal marker that persists in the course; touching a standee also revives, which lets a strong player leave safety behind at a hard jump for whoever follows. The window expiring costs the life normally.

**Why here:** `CourseCoopService` is thirty lines that share 1-Up pickups within 32 blocks and, per PROGRESS.md, has never been run with two players. Co-op in a platformer is defined by what happens when one player dies; sharing lives is the least interesting version. Revival also solves the real problem in a 2.5D shared-lane game — one player's death currently ends their participation until the whole party resets.

**Sketch:** Intercept in `DamageService.down` before the life decrement: if another `ServerPlayer` is in the same course, set `PlayState` to a new `DOWNED` node on `CourseState` (the state machine already has one) rather than routing to `CheckpointService`. Spectator-ish movement can be a scale/collision change through `PlayerSizeService` rather than a real gamemode swap, so the 2.5D rail still applies. Standee is a block placed from an item, with the owner's UUID in the block entity; `CourseCoopService` grows into the owner of both. This is also the honest way to close NEXT_TASKS item 29 — building the mechanic forces the two-player test.

**Touches:** `CourseCoopService.java`, `DamageService.java`, `PlayState.java`

### Player-to-player grab, carry and throw  *(value 8, medium)*

**From:** New Super Mario Bros. Wii/U — picking up a teammate and hurling them at a ledge (or a pit)

A player standing on the ground behind or beside a teammate can grab them: the carried player is parented above the carrier's head, keeps a reduced set of inputs (they can wriggle free), and the carrier moves at a reduced speed and cannot use their Form action. Releasing drops the carried player; pressing the throw input launches them along the travel axis with a strong arc, which is enough to clear a gap the carried player could not jump. Landing on a teammate's head from above bounces the jumper without damaging the teammate — the same stomp arc as an enemy, without the defeat.

**Why here:** This is the mechanic that makes NSMB co-op memorable, and it is also the accessibility path: a strong player can physically move a weaker one past a section. PlaneShift already has all the physical machinery — a stomp bounce driven by ModAttributes.BOUNCE_HEIGHT, a 2.5D travel axis that makes the throw a one-dimensional problem, and a squish/hitbox framework — but there is no player-to-player interaction of any kind. It also pairs directly with the bubble state: throwing a teammate into a pit is only funny because bubbling makes it recoverable.

**Sketch:** Server-authoritative. Add a GrabService in server/ that on a grab payload validates both players are in the same course and grounded, then calls the carried player's startRiding on the carrier with a custom passenger offset, and flags the carrier in a set so CourseMovementService applies the speed penalty and FormService.useAction refuses. The throw applies a Vec3 impulse folded onto the travel axis by MovementRuleService (which already does exactly this fold for cross-rail momentum), plus hurtMarked. Head-bounce goes in ServerEvents' entity-collide path: if the lower entity is a ServerPlayer in a course and the upper one is falling, apply the existing BOUNCE_HEIGHT impulse and deal no damage. Bind grab/throw to the sprint modifier plus the existing FORM_ACTION key when a teammate is in range, or add a dedicated KeyMapping.

**Touches:** `CourseMovementService.java`, `MovementRuleService.java`, `ServerEvents.java`, `ModAttributes.java`, `PlaneShiftKeybinds.java`


## movement

### Cappy capture — possess an enemy and inherit its moveset  *(value 10, large)*

**From:** Super Mario Odyssey

A thrown hat that lands on a valid enemy transfers player control into that enemy for a bounded duration or until damaged. While captured the player renders as the host, loses their own Form actions, and gains that species' verb set: a captured Bullet Bill flies in a straight line through gaps no jump can cross; a captured Thwomp smashes blocks nothing else breaks; a captured Piranha Plant spits at range; a captured Hammer Bro throws from a perch. Release is voluntary (dismount pops the player upward out of the host) or forced (host takes a hit). The capture is the level key: courses author obstacles solvable only by one species, and the species is standing right next to the obstacle.

**Why here:** PlaneShift already has eleven fully-implemented enemies with distinct AI goals, and every one of them is currently only an obstacle. Capture converts the entire existing enemy roster into a traversal vocabulary at roughly one small class per species, which is the single highest ratio of new gameplay to new content available in this codebase. It also gives the generator a new segment axis ("place enemy X next to obstacle only X can solve") that composes with the existing tag-driven picker rather than replacing it.

**Sketch:** New `common/entity/CapProjectile.java` alongside the existing `BoomerangProjectile` (same registration shape in `ModEntities`). New `server/CaptureService.java` holding a `WeakHashMap<ServerPlayer, CourseEnemyEntity>`; on hit it sets `player.startRiding` semantics via a camera-and-input redirect rather than vanilla riding, since `PlaneConstrainedInput` already owns input on the rail. Add `capturable()` and `captureMoveset()` to `CourseEnemyEntity` so each subclass declares its own verb, defaulting to non-capturable. Route the capture action through a new `FormActionKind.CAP_THROW` so the existing `FormService` charge/cooldown plumbing and `FormActionPayload` handle it with no new networking. Client side: `CourseEnemyRenderer` already has `CourseEnemyRenderState`; add a `captured` flag so the host renders with the player's hat.

**Touches:** `CourseEnemyEntity.java`, `ModEntities.java`, `FormActionKind.java`, `FormService.java`, `PlaneConstrainedInput.java`, `SegmentLibrary.java`

### P-meter as a real gauge: fill, hold, drain, and a HUD readout  *(value 9, medium)*

**From:** Mario game feel — the P-meter and speed tiers

A 0..N meter that fills only while running on the ground at full speed, holds at full for a grace period when the player leaves the ground or briefly stops, and drains (rather than resetting) otherwise. At full it unlocks a distinct speed tier and a longer jump arc, and the meter is drawn on the HUD as segmented chevrons so the player can see how close they are. Losing it should be visible before it is felt.

**Why here:** MISSING_MECHANICS #2 calls P-speed 'the single biggest contributor to feels-like-Mario that is absent' and marks it [new]. A partial version has landed and it has three defects that make it read as a bug: (a) the boost is a compounding per-tick `v.x * 1.05` rather than a raised speed ceiling, so it is frame-rate-of-tick dependent and fights friction instead of setting a target; (b) `SPRINT_TICKS.remove(player)` on any non-sprint tick means the meter zeroes the instant the player jumps or is bumped — you cannot carry P-speed into a jump, which is the entire point of the mechanic; (c) nothing on screen shows it, so the 30-tick threshold is invisible and the one-shot POWER_UP ping at tick 30 is the only cue.

**Sketch:** Replace SPRINT_TICKS in MovementRuleService with a PMeter record (int fill, int holdTicks). Fill on ground+sprint, hold for ~10 ticks off-ground, drain 2/tick otherwise. At full, apply a transient MOVEMENT_SPEED AttributeModifier through the same pattern CourseMovementService uses (a named Identifier modifier, added/removed) instead of multiplying delta, and add a transient JUMP_STRENGTH modifier so the jump arc actually lengthens. Sync the fill to the client on CourseState (it already carries score/coins/pips and is synced) and draw it in CourseHud.renderCluster below the pip row using the existing graphics.fill primitives.

**Touches:** `MovementRuleService.java`, `CourseMovementService.java`, `CourseState.java`, `CourseHud.java`

**Partly present already.** What is left: Left to build: the gauge itself (0..N with fill/hold/drain replacing the boolean at :66-82), the full-tier jump-arc change, and segmented chevrons in CourseHud.renderCluster. Two cautions. (a) The existing `v.x * 1.05D` is compounding per tick with no ceiling — replace it rather than build on it. (b) MISSING_MECHANICS.md's mod table claims Omni already supplies "2 sprint tiers" and says "nothing new"; but PLAYTEST_INSTANCE.md:103-108 says omni and enhanced-movement are a design conflict with the rail and should be disabled, so the overlap argument does not hold — build it, and gate it through ModCompatibility if Omni is present.

### Spin as a universal verb, not just an armored-stomp counter  *(value 9, small)*

**From:** Super Mario Galaxy 1/2

One button that does a context-appropriate thing everywhere. On the ground it is a short 360-degree attack that hits enemies to either side and knocks shells forward. In the air it is a single stall — a brief hover plus a small upward nudge, usable once per airborne stint — which is the forgiveness mechanic that lets Galaxy build platforms at distances a raw jump cannot reach. Against a projectile it deflects. Against a crate or brick it breaks. Against a Launch Star or Pull Star it activates them. The recharge is landing, so it is never spammable.

**Why here:** PlaneShift already has a spin jump, but it is one narrow special case: `CourseEnemyEntity.resolveStomp` checks `AirMoveService.isSpinJumping(player)` only to convert a spike-damage stomp into a safe bounce. Nothing else in the codebase reads it. Promoting it to a universal verb costs almost nothing new — the state map and the edge-triggered input already exist — and it becomes the connective tissue every other Galaxy mechanic on this list hooks into, as well as the counter the generator needs so `SegmentLibrary` can place enemies in pairs flanking the player.

**Sketch:** `AirMoveService.SPIN_JUMPING` already exists and is already exposed via `isSpinJumping`. Split it into `spinCharge` (recharges on `player.onGround()`) and a `spinActive` window of ~8 ticks. Add a `SpinService`, or extend `AirMoveService.tick`, to sweep an AABB for `CourseEnemyEntity` (call the existing `hurtServer` path used by `resolveStomp`), for `BrickBlock`/`RotatingBlock` (reuse `HitFromBelowBlock`'s trigger so a spin and a head bump agree), and for projectiles tracked by `common/entity/ProjectileTracker`. The air stall is a one-tick velocity clamp in the same place `WALL_SLIDE_SPEED` is applied.

**Touches:** `AirMoveService.java`, `CourseEnemyEntity.java`, `HitFromBelowBlock.java`, `ProjectileTracker.java`

**Partly present already.** What is left: Left to build, all on top of the existing SPIN_JUMPING flag: the grounded 360 attack (hit both sides, knock shells forward); the once-per-airborne-stint air stall (brief hover + upward nudge) with landing as the recharge — note `CLAMBER_COOLDOWN` at AirMoveService.java:68-82 is the existing precedent for "one assist per airborne stint" and its comment explains why the recharge rule matters to `CourseReachability`; projectile deflect; crate/brick break (route through `HitFromBelowBlock.impact`). Adding an air stall changes reachable heights, so `server/gen/CourseReachability` must be re-run or courses stop being provably fair.

### A general carry-and-throw system  *(value 8, medium)*

**From:** Super Mario Maker 1/2

One rule that lets the player pick up a designated object, walk with it visibly held, and throw it forward, up, or gently down. The same rule serves every carryable: a parked shell, a trampoline, a live bomb, a key, a POW block. Carrying changes what the player can do — no attack, restricted jump arc — so it is a real trade, and the object stays a live entity the whole time.

**Why here:** This is a missing *system*, not a missing object, and three items already on the backlog quietly assume it exists: #19 says a Bob-omb 'can be picked up and thrown', #43 says a trampoline is 'carryable and placeable', and #55's key is only interesting if it can be carried. Building it once turns three separate features into three small ones. Its absence is also why `KoopaEntity`'s otherwise complete shell has only two verbs — kick and stomp — instead of the four the mechanic is famous for.

**Sketch:** A `Carryable` interface on `CourseEnemyEntity` and on a small carryable-block-entity wrapper; a `carried` reference held server-side per player (a field on a new `CarryService`, not on `CourseState`, since it is an entity reference and must not be persisted). Position the held entity each tick from a `ServerEvents` per-entity hook — the codebase's stated rule is per-entity ticks, never level sweeps. Throw is a new `FormActionKind`-adjacent input; the `SWAP_RESERVE` keybind pattern in `PlaneShiftKeybinds` plus a payload in `ModNetworking` is the existing route for a new verb.

**Touches:** `CourseEnemyEntity.java`, `KoopaEntity.java`, `ServerEvents.java`, `PlaneShiftKeybinds.java`

### Real skid: momentum reversal takes time  *(value 8, medium)*

**From:** Mario game feel — the acceleration/friction/skid model

Turning against your own velocity above a speed threshold should enter a SKID state: horizontal velocity decays toward zero at a skid deceleration rate (much lower than instant), the avatar keeps sliding in the old direction for the duration, and only when velocity crosses zero does the new direction take over. Below the threshold, turning is instant. The skid window is also the input window for a skid-jump (a jump during the skid keeps a fraction of the reversed speed), which is what makes a turnaround feel like a decision rather than a mistake.

**Why here:** MISSING_MECHANICS #3 lists skid as [new], but code has since landed that only fires the *particles and sound* for a turnaround and never touches velocity. The result is worse than nothing: the game plays a skid effect while the player stops dead, so the feedback actively lies about the physics. Anyone reading the backlog will see 'skid' near a smoke puff and assume it is done.

**Sketch:** MovementRuleService.tick() already measures a trustworthy per-tick X delta (LAST_X / SMOOTH_VEL_X) and detects the sign flip. Add a SKID_STATE WeakHashMap<ServerPlayer, Integer>; on detection, latch it for ~8 ticks and each of those ticks write player.setDeltaMovement(prevVx * 0.82, v.y, v.z) with hurtMarked, ignoring the input's requested direction. Gate on player.onGround(). Keep the existing SKID_COOLDOWN for the VFX so the puff still fires once. Expose skidDeceleration in PlaneShiftConfig.SERVER next to courseRunBoost so it can be tuned live during the Group A playtest.

**Touches:** `MovementRuleService.java`, `PlaneShiftConfig.java`

**Partly present already.** What is left: What is left is the physics half. Important: do NOT build it where the detector lives. MovementRuleService's own comment (lines 86-89) says the server's getDeltaMovement() is stale because the client owns player movement — a server-side velocity write each tick would fight the client and read as rubber-banding (the same failure documented in AirMoveService.java:138-148). Implement the SKID state client-side in PlaneMovementAssists (which already writes LocalPlayer velocity legitimately) or in PlaneConstrainedInput, which owns the projected travel direction: on a reversal above threshold, suppress the direction flip and decay travel-axis speed at a skid rate until it crosses zero, then release. Keep the existing MovementRuleService block as the server-side FX trigger. The skid-jump is then a check on jumpPressedEdge while the skid window is open.

### Coyote time and jump buffering in 3D courses  *(value 8, trivial)*

**From:** Mario jump forgiveness — applied to every mode, not one

The forgiveness window (a jump pressed up to 3 ticks after leaving a ledge still fires; a jump pressed up to 3 ticks before landing fires on the landing tick) should be a property of being in a course, not of being on the 2.5D rail.

**Why here:** PlaneMovementAssists.tick() bails out and calls reset() unless state.in2_5D(). The mod ships three 3D courses (space_1..space_3), built by the same composer at a wider ribbon from the same segment library — so they contain the same gaps, sized for the same jump — and in those courses the player has no coyote time and no jump buffering at all. A player moving between a 2.5D and a 3D course experiences the jump silently getting stricter with no explanation. This is a correctness bug hiding as a feature gap, and it is on none of the lists.

**Sketch:** Change the guard in PlaneMovementAssists.tick() from state.in2_5D() to state.inCourse() for the coyote and buffer blocks specifically, keeping the 2.5D-only guard on tickAvatarFacing (which genuinely is a side-scroller presentation rule). The Glider float branch already gates itself on the role signature so it needs no extra guard.

**Touches:** `PlaneMovementAssists.java`

### Cap throw as a standalone verb: hover-throw, cap bounce, and remote activation  *(value 8, medium)*

**From:** Super Mario Odyssey

Independent of capture, the thrown hat is a three-in-one tool. Held at full extension it hovers in place for about half a second, and the player can land on top of it as a one-shot mid-air platform, extending a jump by roughly one block of height and two of distance. It collects coins along its path and returns them. It activates blocks at range — hitting a question block, a P-switch or an ON/OFF switch from across a gap the player cannot cross. And it staggers non-stompable enemies (Spiny, Buzzy Beetle) for a short window without killing them.

**Why here:** The mod has four throwable projectiles (fireball, iceball, hammer, boomerang) and every one is purely offensive. A projectile the player can *stand on* is a traversal primitive, and it is the cheapest way to give the generator gap-and-switch puzzles that read as clever rather than as longer jumps. It also makes the capture throw feel good on its own, so capture does not have to carry the whole mechanic.

**Sketch:** `CapProjectile` extends the pattern in `BoomerangProjectile` (which already implements out-and-return flight with `setNoGravity(true)`). Add a hover phase: at max range, zero velocity for N ticks and expose a solid collision box only on the top face, the same trick `MovingPlatformEntity` already uses to carry riders in its `carryBox` sweep. On block contact, reuse the existing `HitFromBelowBlock.onHitFromBelow` contract so question blocks, bricks and rotating blocks respond identically to a head bump — no per-block special casing. On enemy contact, call a new `stagger()` on `CourseEnemyEntity` next to the existing `startSquish()`.

**Touches:** `BoomerangProjectile.java`, `HitFromBelowBlock.java`, `MovingPlatformEntity.java`, `CourseEnemyEntity.java`

### Ground pound as a puzzle verb rather than a fast fall  *(value 8, small)*

**From:** Super Mario 3D Land / Galaxy / Odyssey

The pound does work on landing: a radial shockwave that stuns or kills enemies within a couple of blocks, driving stake blocks a step further into the ground each time (a three-hit stake becomes a bridge or a switch), triggering pound-only switches that no jump or projectile can reach, cracking floor tiles that then collapse, and a pound-jump — a jump released at the exact frame of impact — that launches higher than a standing jump. The pound also cancels into a dive, chaining into the roll.

**Why here:** The mechanic exists and does nothing. `AirMoveService` implements the full input detection and a `-1.2` descent, and the only consumer of `GROUND_POUNDING` in the entire repo is `AirMoveService` clearing it — no block, no enemy and no scoring path reads it. That is a verb the player learns and then discovers is decorative, which is worse than not having it. Wiring in a shockwave and a pound switch costs very little against systems (`HitFromBelowBlock`, `OnOffSwitchBlock`, `CourseEnemyEntity`) that are all already built.

**Sketch:** In `AirMoveService`, at the point where `GROUND_POUNDING.remove(player) != null` detects the landing (around line 194), sweep an AABB for `CourseEnemyEntity` and call the same `hurtServer` path `resolveStomp` uses so kills feed the existing combo ladder in `CourseScoringService`. Add a `PoundSwitchBlock` and a `StakeBlock` in `common/block` modelled on `OnOffSwitchBlock` and `DonutBlock` respectively — `DonutBlock` already demonstrates the scheduled-tick state machine a multi-hit stake needs, and its comment about restoring rather than dropping (to avoid soft-locking a checkpointed course) applies here too. Respect `reducedMotion` for the screen shake NEXT_TASKS task 23 wants.

**Touches:** `AirMoveService.java`, `CourseEnemyEntity.java`, `OnOffSwitchBlock.java`, `DonutBlock.java`, `CourseScoringService.java`

**Partly present already.** What is left: Left to build: the radial shockwave (no AoE — `CatFormService.java:98-127` already has a radius-damage-on-impact routine to copy), stake blocks driven a step per hit, pound-only switches, cracking floor tiles (adjacent to docs/MISSING_MECHANICS.md #59, breakable floor tiles), and the pound-jump. Two constraints the sketch misses: the pound shares the sneak input with the spin jump and is explicitly excluded from it (AirMoveService.java:184-187 comment), and with the Cat Suit equipped crouch-in-air is the pounce dive instead (CatFormService.java:68-73) — so "pound cancels into a dive" already has an input collision to resolve, not a free slot.

### Apex hang — reduced gravity at the top of a jump  *(value 7, trivial)*

**From:** Mario jump arc — apex float

While |vertical velocity| is under a small threshold near the top of a jump, apply a fraction of normal gravity for a handful of ticks. The player gets a brief hover at the peak, which is where almost all platforming decisions are made: it widens the window to read a landing target, line up a stomp, or steer into a coin arc.

**Why here:** The mod has variable jump height (hold-to-boost), coyote time and jump buffering, but the arc itself is a plain parabola, so the peak is the least controllable moment of the jump rather than the most. This is a two-line change with a disproportionate effect on how the 5-block platforms the generator builds actually feel to land on, and it is on none of the existing lists.

**Sketch:** In AirMoveService.tick(), after the existing hold-jump branch: if (!player.onGround() && Math.abs(measuredRise) < APEX_BAND && apexTicksUsed < APEX_TICKS) add a small positive counter-impulse to y and increment a per-player counter reset on ground contact. Use the same measured-rise-based pattern the file already uses everywhere (never write back a stale getDeltaMovement) and set hurtMarked. Guard it behind PlaneShiftConfig so the Group A playtest can A/B it.

**Touches:** `AirMoveService.java`, `PlaneShiftConfig.java`

### Corner correction on head bumps and ledge edges  *(value 7, small)*

**From:** Mario jump forgiveness — corner correction / ceiling nudge

When a rising player's head clips a block by a small margin at the corner, nudge them horizontally past it and let the jump continue, instead of stopping the rise. The mirror case on the way up a ledge: if the feet clip the lip of a platform by under half a block, lift them onto it. Both are silent forgiveness — the player never sees the correction, they only notice that the game stopped eating jumps that visually cleared.

**Why here:** AirMoveService currently does the opposite of forgiveness on a head bump: it applies HEAD_BUMP_REBOUND = -0.12 and kills the climb, deliberately, so that a hit reads as a collision. That is correct when the player meant to hit the block, and infuriating when they clipped the corner of a brick beside the gap they were aiming at. The 2.5D rail makes it worse — the player cannot sidestep in depth to avoid it, so a corner clip is unavoidable rather than a positioning error.

**Sketch:** In AirMoveService, before dispatching to HitFromBelowBlock: sample the block at the head probe on both sides of the player's x. If exactly one side is solid and the player's horizontal overlap with it is under CORNER_TOLERANCE (~0.3), push the player away from it (setDeltaMovement with an x nudge) and skip the bump/rebound entirely for that tick. Reuse the existing BlockPos.containing(..., getBoundingBox().maxY + HEAD_PROBE, ...) probe so the geometry stays consistent with the bump path.

**Touches:** `AirMoveService.java`

**Partly present already.** What is left: Only the head-corner nudge is left. In the head-bump branch (AirMoveService.java:240-267), before applying HEAD_BUMP_REBOUND, test whether the block the head entered is clipped by less than ~0.3 on the travel axis and the adjacent column is air; if so, nudge the player horizontally past it and preserve the rise instead of rebounding. Keep the rebound for a genuine centre hit — it is what makes a HitFromBelowBlock read as a collision.

### Flying Squirrel wall-cling and mid-air flutter boost  *(value 7, medium)*

**From:** New Super Mario Bros. U — the squirrel suit's sticky wall grab and its single upward flap

Two additions to the acorn glide. First, once per airtime, a jump input during the glide produces a short upward flutter that regains a little height — the difference between a glide that only ever loses altitude and one the player can steer over an obstacle. Second, holding toward a wall while gliding sticks the player flat against it for a limited time, from which they can drop, wall-jump, or slide slowly down. The cling releases automatically after a couple of seconds so it cannot be used to wait out a hazard indefinitely.

**Why here:** The wall cling is the more important half, because it is the one mechanic that would make PlaneShift's wall system honest. The mod's own wall jump is deliberately disabled (wallJump defaults false) because the six-tick grace window fires anywhere in a dense course, and docs/MISSING_MECHANICS.md item 11 asks for real wall-contact tracking before it can return. A cling is exactly that tracking, with a visible state the player can see: it makes wall contact a thing you can observe rather than a hidden timer, which is what would let the wall jump be re-enabled safely rather than being deferred to an external movement mod.

**Sketch:** Add a wall-contact tracker in AirMoveService: count consecutive ticks the player's collision box overlaps a solid face on the travel axis, storing the face. The cling is a velocity zero-out plus a countdown while that count holds and the player's Form is the acorn (checked through FormService's active id). The flutter is a one-shot flag reset on ground contact, consuming a charge from FormSlot. Sync a clinging boolean for the renderer through the existing CourseState path. This tracker is the same data docs item 11 wants, so build it once and let both use it.

**Touches:** `AirMoveService.java`, `FormService.java`, `FormActionKind.java`, `PlaneShiftConfig.java`

**Partly present already.** What is left: Left to build: only the once-per-airtime flutter — a jump input during ACORN_GLIDE that regains a little height and arms a per-airtime flag cleared on landing. The wall cling is not new work; it is generalising CatFormService's cling from a Cat-Suit-only service into something the acorn can also request, which is a small refactor, not a feature. Pitch this as "flutter boost", not "wall-cling and flutter".

### Dive and roll: converting a fall into ground speed  *(value 7, medium)*

**From:** Super Mario Odyssey

Two chained air-to-ground moves. A dive is a forward horizontal lunge from the air that keeps the player's momentum and ends in a belly-slide; releasing into a crouch mid-slide becomes a roll that *accumulates* speed on downhill ground and bleeds it on flat, capping well above run speed. Rolling into a wall cancels; jumping out of a roll launches with the roll's accumulated speed intact. The loop is jump → dive → roll → jump, and a player who executes it cleanly crosses ground far faster than one who runs.

**Why here:** MISSING_MECHANICS 2 asks for a P-speed run meter and 4 for momentum preservation on landing, but both are linear — hold the key longer, go faster. Dive-roll is an *execution* skill layered on top of those, which is what makes an Odyssey course replayable at speed. It also directly serves the `bestTimeLeft` speedrun record that `CourseProgress.Record` already persists and that nothing currently gives a player a reason to chase.

**Sketch:** Extend `server/AirMoveService`, which already owns the airborne state machine with `GROUND_POUNDING`, `SPIN_JUMPING` and `CLAMBER_COOLDOWN` weak maps and already reconstructs trustworthy horizontal velocity from `LAST_XZ` position deltas (its own comment explains why `getDeltaMovement()` is stale). Add `DIVING` and `ROLL_SPEED` maps in the same style. The roll hitbox is the crouch hitbox that `common/course/CourseCrouch` already defines at 0.5 blocks, so no new collision work. Slope acceleration needs MISSING_MECHANICS 9 (slopes) to be interesting but works flat-only as a first pass.

**Touches:** `AirMoveService.java`, `CourseCrouch.java`, `PlaneMovementAssists.java`

**Partly present already.** What is left: What is actually left: (1) a forward horizontal dive distinct from the Cat pounce — but it must not collide with the Cat dive, which already owns crouch-in-air, nor with ParCool/Omni's dodge-roll; (2) the roll itself. The roll's headline behaviour — accumulating speed downhill — has no terrain to run on: docs/MISSING_MECHANICS.md:37 (#9) records that the generator produces only flat surfaces and staircases, no slopes. Ship #9 first or the roll is a flat-ground speed cap with no accumulation curve.

### Stomp bounce modulated by held jump  *(value 6, trivial)*

**From:** Mario stomp — hold A for a higher bounce

A stomp while the jump key is held produces a noticeably higher bounce than a stomp with the key released. That single input turns a stomp chain from a sequence of identical hops into a controllable ladder: the player chooses whether the next bounce reaches the enemy above or drops them back to the row below.

**Why here:** The mod already has the full 100/200/400/.../1-Up ladder and a chain that only deepens while airborne — the reward structure for chaining exists in CourseScoringService — but the physics gives the player no way to *steer* a chain, because every bounce is the same fixed height. The scoring ladder is therefore mostly luck. This is the cheapest possible fix to that and it is not on any list.

**Sketch:** CourseEnemyEntity.bounce() already reads ModAttributes.BOUNCE_HEIGHT rather than a constant, precisely so something can tune it. Multiply the result by a factor when player.getLastClientInput().jump() is true (the same accessor AirMoveService uses for the variable-jump branch). No new attribute or packet needed.

**Touches:** `CourseEnemyEntity.java`, `ModAttributes.java`


## boss

### Boss hit contract: discrete hits, invulnerability window, phase escalation, defeat ritual  *(value 9, medium)*

**From:** Boom-Boom, Pom Pom, the Koopalings and Bowser all share one grammar: a fixed small number of hits, an i-frame flash, a faster phase after each, and a scripted defeat

A shared boss base: N discrete hits (3 is the standard), each hit granting ~40 ticks of invulnerability with a flashing render and a knockback hop; the arena locks (no scroll past the boss) until defeat; each hit advances a phase that changes the attack set rather than only the numbers; defeat is a scripted sequence — stagger, spin-out, flash, then the reward — not an instant despawn.

**Why here:** `BowserEntity` is a 60-HP `Monster` with 4 armour and `NearestAttackableTargetGoal`, so the fight resolves as "hit it with whatever weapon you have until the health bar empties", which is a Minecraft fight, not a Mario one. Every boss on this checklist (Boom-Boom, Pom Pom, seven Koopalings, Bowser Jr — already queued as item 27) needs this scaffolding, and building it once is the difference between four bosses and four copies of Bowser.

**Sketch:** New `common/entity/CourseBossEntity extends CourseEnemyEntity` holding `hitsTaken`, `invulnUntil`, `phase`, and a `ServerBossEvent` (the pattern already exists in the dead `server/entity/BowserEntity.java`, which `NEXT_TASKS` flags as a trap — harvest it, then delete it). Refit `BowserEntity`/`BowserGoal` onto it as the first user. Arena lock reads `CourseService`/`CourseStateAccess`.

**Touches:** `BowserEntity.java`, `BowserGoal.java`, `CourseCompletionService.java`

### A reusable arena mid-boss: locked room, three-hit pattern, escalating phases  *(value 9, large)*

**From:** New Super Mario Bros. — the recurring stomp-three-times tower boss with a boss door in front of it

A boss template rather than a single boss. Entering the arena seals it: the exit closes, the camera stops following the player and frames the room, and the clock pauses. The boss follows a three-phase pattern — a telegraphed charge, a jump-and-shockwave that forces the player off the ground, and a vulnerable recovery window. Three successful stomps defeat it; each hit shortens the recovery window and adds one element to the attack. The reward is a key, and the exit opens onto the flagpole. In co-op, hits are pooled across the party.

**Why here:** PlaneShift has exactly one boss, Bowser, and — this is the important part — nothing in the generator ever spawns it. WorldDefinition.bossCourseId() returns the last course in a world and CourseCompletionService plays a Toad send-off on clearing it, but the course itself is generated by the same composer with the same segments as any other, so 'castle' is currently a label on the map with no fight behind it. Every world in the game ends in an anticlimax. A reusable arena template is worth more than one more unique boss because it makes the ten-world structure deliverable.

**Sketch:** New ArenaFightService in server/ owning the seal (place ShiftGateBlock or a barrier at the arena bounds), the camera override (a third CameraProfile — only two exist), the clock pause (CourseTimerService already gates on a sentinel, so a paused flag is a small addition) and the hit counter. A BossPatternGoal in common/entity modelled on the existing BowserGoal and ThwompGoal, parameterised by phase count so a second boss reuses it. A boss-door block gating on the key. CourseComposer places a boss arena Segment tagged SETPIECE as the finale whenever the course id equals its world's bossCourseId — the composer already has a finale slot and a SETPIECE tag limited to one per course.

**Touches:** `CourseComposer.java`, `SegmentLibrary.java`, `BowserGoal.java`, `CameraProfile.java`, `CourseTimerService.java`, `ShiftGateBlock.java`

### Boom-Boom — the reusable castle mid-boss  *(value 8, medium)*

**From:** Boom-Boom

An armoured Bro who charges the player with arms flailing, is stomped three times, and after each stomp retreats into his shell and spins across the arena as a hazard before popping back out faster. Later encounters add a hop phase (jumps toward the player) or a spin-in-place phase. He is stompable only while walking; the shell spin must be dodged.

**Why here:** Every world needs a boss, but Bowser is the world-10 payoff and `NEXT_TASKS` already wants a cheaper mid-boss (item 27, Bowser Jr). Boom-Boom is cheaper still: he reuses `HammerBroEntity`'s perch/approach logic and `KoopaEntity`'s spin-shell travel code almost wholesale, so he is the boss that can ship the same week as the boss framework and validate it.

**Sketch:** `BoomBoomEntity extends CourseBossEntity` + `BoomBoomGoal` in `common/entity/`, registered in `ModEntities` with a renderer in `ClientModEvents` (the `checkEntityRenderers` Gradle task fails the build otherwise). Shell-spin travel lifts `KoopaEntity.tickSlide()` — including its `horizontalCollision` ricochet. Placed by a new castle segment in `SegmentLibrary` next to the existing `castle` finale.

**Touches:** `BowserEntity.java`, `ModEntities.java`, `SegmentLibrary.java`

### Koopaling wand duel — the seven-boss template  *(value 8, large)*

**From:** The Koopalings

A wand-carrying Koopaling that alternates two phases: firing slow, arcing magic bolts that leave a lingering hazard where they land, and withdrawing into its shell for a fast ricochet dash across the arena. Each of the seven differs by exactly one variable — bolt count, bolt bounce, dash speed, hop height, ground-pound tremor that stuns the player — so one class with a parameter record covers all of them. Stomped three times between phases.

**Why here:** Ten worlds need ten distinct boss fights and the mod has one boss. A parameterised template turns that from ten implementations into one implementation and nine data rows, which is the only version of this that will actually get finished. It also gives `WorldRegistry`'s `bossCourseId` something different to point at per world.

**Sketch:** `KoopalingEntity extends CourseBossEntity` taking a `KoopalingProfile` record (bolt speed, bolts per volley, dash speed, hop force, tremor flag) supplied per world; one renderer with a palette/hat swap, so `checkEntityRenderers` is satisfied by a single entry in `ClientModEvents`. Bolts extend the existing `EmberBoltEntity`; the shell dash reuses `KoopaEntity.tickSlide`. Hooked up in `CourseCompletionService` where `world.bossCourseId()` is already tested.

**Touches:** `EmberBoltEntity.java`, `CourseCompletionService.java`, `ModEntities.java`

### Pom Pom — the shuriken and clone mid-boss  *(value 7, medium)*

**From:** Pom Pom

A Koopa sorceress who throws a boomerang-style shuriken that must be dodged twice (out and back), and who, once damaged, splits into several identical copies of which only one is real — the fakes vanish on contact. She is defeated by stomping the real one three times.

**Why here:** It is the second boss archetype: Boom-Boom tests timing, Pom Pom tests reading. The mod already owns a returning-projectile entity (`BoomerangProjectile`, 141 lines, with the out-and-back arc solved) and a boss-arena need, so the expensive half is done. The clone mechanic also stress-tests the 2.5D lane clamp with several entities on the same rail.

**Sketch:** `PomPomEntity extends CourseBossEntity`; reuse `BoomerangProjectile` with a boss owner. Clones are the same entity type with a `Decoy` synced boolean (same pattern as `KoopaEntity`'s `RED` accessor) that makes `playerTouch` despawn them; `CourseEnemyEntity.holdLane` already keeps them all on the rail.

**Touches:** `BoomerangProjectile.java`, `ModEntities.java`

### A recurring rival boss set with escalating rematches  *(value 7, large)*

**From:** Super Mario Odyssey (Broodals)

A small family of named bosses — four, say — who appear across the game rather than once each. Each has one signature weapon and one weakness, and the player fights them individually in worlds one through four, then all of them together as the finale. Every rematch adds a mechanic to the same fight rather than replacing it: the second time, the arena floor is now breakable; the third, a second boss interferes. The player recognises the fight and is still surprised by it.

**Why here:** The mod has one boss. `ModEntities` registers `BOWSER` and `BOWSER_FIRE` and nothing else bossy, and `WorldRegistry` makes the tenth course of each of the five worlds a castle — meaning either five identical Bowser fights or five castles with no boss at all. A recurring set gives all five castle courses a distinct climax at a fraction of the cost of five unrelated bosses, because the escalation is arena and script rather than new AI. It also gives `CourseComposer`'s missing "conclude" step (NEXT_TASKS task 6) an actual destination.

**Sketch:** Generalise `common/entity/BowserEntity` and `BowserGoal` into a `BossEntity` base with a phase list, then subclass per rival — `BowserGoal` already implements a multi-attack boss goal and `BowserFire` shows the projectile registration pattern. Arena escalation is data: a `bossPhase` field on `CourseDefinition` (a record with a `RecordCodecBuilder` codec, extended the same way its optional fields already are) read by `CourseStructureService.placeTemplate`, which MISSING_MECHANICS 70 notes exists and has no callers — this would be its first. Every new entity needs a `ClientModEvents.registerEntityRenderer` line or `checkEntityRenderers` fails the build. Note `server/entity/BowserEntity.java` is dead duplicate code (flagged in NEXT_TASKS) — delete or avoid it.

**Touches:** `BowserEntity.java`, `BowserGoal.java`, `ModEntities.java`, `CourseStructureService.java`, `CourseDefinition.java`

### Multi-target platform boss — several weak enemies on a rotating rig  *(value 6, medium)*

**From:** New Super Mario Bros. — the four-enemy rotating-platform tower fight

A second boss shape using the same arena template but inverting its structure: instead of one enemy taking three hits, four fragile enemies sit on a slowly rotating rig of platforms over a hazard floor, each lobbing a projectile on its own offset timer. They are defeated in one hit, but reaching each one requires riding the rig, and every defeat tilts the rig's balance so the remaining targets move differently. The last one falls into the hazard and drops the key.

**Why here:** One boss pattern across ten worlds is a repeated cutscene; two that ask opposite questions — precision timing against one target versus route-planning across four — is a structure. This one is also the cheapest possible second boss for this codebase specifically, because it is assembled almost entirely from parts that already work: MovingPlatformEntity does axis sweeps, FirebarEntity already owns a whole rotating assembly as a single entity for cost reasons, and HammerBroEntity already implements a perched enemy that lobs projectiles at a clamped range.

**Sketch:** A RotatingRigEntity modelled directly on FirebarEntity (one entity owning the whole rig, contact tested per segment against a line — the same design note applies) whose segment endpoints are platform anchors rather than flames; passengers ride via standard passenger offsets. The four targets are HammerBroEntity instances with their perch clamp pointed at rig anchors instead of terrain, reusing the perch and range gating already done. Defeat handling and the key drop go through the ArenaFightService from the previous item. Register the rig in ModEntities and add the renderer for the checkEntityRenderers build check.

**Touches:** `FirebarEntity.java`, `MovingPlatformEntity.java`, `HammerBroEntity.java`, `ModEntities.java`


## camera

### Plane-flip Wonder Effect (2.5D ↔ 3D mid-course)  *(value 9, medium)*

**From:** Super Mario Bros. Wonder (2023)

A specific transformation effect: for the duration of the wonder stretch the course leaves the 2.5D rail and opens into depth (or the reverse — a 3D course collapses onto a single lane). The floor the player was running on stays valid; the world simply gains or loses a dimension of movement, and the camera swings to match over a short commit window. The stretch ends at a gate that re-pins the rail.

**Why here:** PlaneShift already owns the hardest part of this — `PlaneMode`, `PlaneRail`, `TransitionSync` and a two-phase committed transition exist and are exercised by `ShiftGateBlock`. Today a mode change is a level-boundary decision (course_1 is 2.5D, space_1 is 3D). Making it a mid-course event turns the mod's title mechanic into a moment-to-moment surprise instead of a menu choice, and it is the wonder effect no other game could have.

**Sketch:** Implement as the first `WonderEffect`: call the same `ModeTransitionService` path `ShiftGateBlock` uses, but scoped — record the pre-effect `PlaneMode`/`PlaneRail` on the effect and restore them at the end marker. `CameraDirector` needs an interpolation case for a transition that begins and ends inside one course rather than at load. Segments in the wonder region should be built with the `buildWide` variant that NEXT_TASKS item 7 introduces, so the depth is actually used.

**Touches:** `ModeTransitionService.java`, `ShiftGateBlock.java`, `CameraDirector.java`

**Partly present already.** What is left: What is actually left is authoring, not mechanics: no SegmentLibrary segment places a shift gate (grep 'shift_gate' hits only ModBlocks.java:111, ModItems.java:149, blockstates/lang and a pickaxe tag — nothing in server/gen or in the 58 course JSONs under data/planeshift/planeshift/course/). Add a shift-gate pair segment (gate in, gate out) to SegmentLibrary with a Tag, and make CourseReachability accept the 3D stretch (NEXT_TASKS.md item 8 already asks for z in the flood-fill).

### Camera lookahead and damped follow (the authored data is dead)  *(value 9, medium)*

**From:** Mario camera — lookahead by facing/velocity

The camera target leads the player along the travel axis by a distance proportional to velocity and capped by the profile's lookAhead, and it chases that target with a damping factor instead of being rigidly pinned. Running right shows more of what is to the right; stopping re-centres smoothly.

**Why here:** CameraProfile is a codec'd datapack record with `lookAhead` (documented '2-4 blocks of horizontal look-ahead based on velocity', range 0-6, default 3) and `damping` (default 0.35), and PlaneShiftConfig exposes a player-facing `lookAheadScale` comfort slider. None of the three is ever read by rendering code — CameraDirector only sets yaw, pitch, roll, distance and FOV. So the camera is rigidly welded to the player, the datapack field is a lie, and the config slider does nothing. In a side-on course the player is permanently looking at as much level behind them as in front, which is the single biggest reason a 2.5D course reads as cramped. This is distinct from NEXT_TASKS E22 (camera zones), which is about authoring *distance and height per segment*.

**Sketch:** In CameraDirector, subscribe to ViewportEvent.ComputeCameraAngles' sibling for camera position (or apply the offset via the setPosition path on the ViewportEvent), compute lead = clamp(velocityAlongRail * k, -lookAhead, +lookAhead) * PlaneShiftConfig.CLIENT.lookAheadScale, and lerp a static smoothedLead toward it by profile.damping() each frame using partialTick. Derive the along-rail axis from state.rail().get() — PlaneRail already exposes travelAxis and sideOnCameraYaw. Snap instantly when reducedMotion is set, matching the existing transition treatment.

**Touches:** `CameraDirector.java`, `CameraProfile.java`, `PlaneShiftConfig.java`

### Vertical camera window that only follows when grounded  *(value 8, medium)*

**From:** Mario camera — vertical ratchet

The camera's vertical target ignores the player inside a dead-band window; it only re-centres vertically when the player is standing on ground (or has been falling long enough to be committed). A jump moves the player within the frame instead of moving the frame, so the horizon stays still and the jump reads as height.

**Why here:** The camera currently inherits vanilla third-person following, so every jump pans the whole world down and back up. In a side-on view where the skybox is the only depth cue, that vertical bob is the most nausea-inducing thing on screen, and it destroys the read on any vertically stacked segment — PLATFORM_LADDER, LIFT_SHAFT, MULTI_TIER_CANOPY, TRAMPOLINE_SKY_LAUNCH all exist in SegmentLibrary and all suffer from it. Not on any list.

**Sketch:** Same insertion point as the lookahead work in CameraDirector. Keep a static targetY; only update it toward the player's Y when player.onGround() (or when |player.getY() - targetY| exceeds a hard window of ~3 blocks, so a genuine fall still follows). Apply the resulting offset to the camera position. Add the window size to CameraProfile as a codec field beside lookAhead so it is authorable per profile, and honour reducedMotion by widening rather than removing it.

**Touches:** `CameraDirector.java`, `CameraProfile.java`

### Party camera bounds with auto-zoom and off-screen bubbling  *(value 8, medium)*

**From:** New Super Mario Bros. Wii/U — one camera owns the whole party; anyone who leaves the frame bubbles back in

The course camera frames the party rather than an individual: its target is the party centroid along the travel axis, and its distance widens as the spread between the leading and trailing player grows, up to a ceiling. A player who is pushed outside the frame — behind the trailing edge, or off the top or bottom — is automatically bubbled and drifts back to the group instead of being dragged along invisibly or falling out of the run. The leading edge of the frame is a soft wall: forward movement stops when the camera cannot widen further, so no single player can drag the course out from under the rest.

**Why here:** PlaneShift's defining choice is a fixed side-on camera, and a fixed camera with more than one player is unsolved in this codebase — each client currently frames itself, so two players in one course are looking at two different windows into the same corridor and can be arbitrarily far apart with no rule about what that means. This is what turns the bubble state from a death mechanic into a pacing mechanic: it is the rule that keeps four players in one shared frame. It also gives the existing auto-scroll flag a co-op meaning it does not currently have.

**Sketch:** Add a partySpread-driven distance term to CameraProfile (it is already a record with distance and height, and only two profiles exist, so the field is cheap). Server-side, a new CourseCameraService computes the party's leading/trailing travel-axis positions once per tick and syncs a small CameraFramePayload (centroid, spread, soft-wall position) alongside the existing CourseState sync. The client camera in client/camera reads that instead of the local player's position when the payload is present. The soft wall reuses the backward-impulse clamp PlaneConstrainedInput already applies for autoScroll, just applied at the leading edge. Off-frame detection calls into the BubbleService from the bubble feature.

**Touches:** `CameraProfile.java`, ``, `PlaneConstrainedInput.java`, `CourseCoopService.java`


## presentation

### Talking flowers — in-course ambient dialogue that teaches the lesson  *(value 9, small)*

**From:** Super Mario Bros. Wonder (2023)

Small fixed props placed along the route that speak one short line as the player passes: a warning before a hazard, a nudge toward a secret, a reaction to a death or a near-miss, an aside with no information at all. They never block, never require input, and are silent on repeat passes within the same run.

**Why here:** This is the highest value-per-line-of-code item here, and it is specifically valuable to a *generated* game. `CourseComposer` already knows which mechanic a segment teaches (`Segment.Tag`) and enforces teach-before-test. A flower placed at the introducing segment can say what the mechanic is, which converts a structural rule the player cannot see into one they can hear. It also gives the generated courses a voice, which is the one thing hand-made levels have that procedural ones usually never get.

**Sketch:** New `TalkingFlowerBlock` with a block entity holding a dialogue key, or cheaper: reuse the marker path — `CourseComposer` calls `canvas.marker("flower:<tag>:<x>")` and `CourseStructureService` places the block. Line selection is a datapack table keyed by `Segment.Tag` plus theme, with several variants per key so a replay differs. Trigger on player proximity in `ServerEvents`' existing per-entity tick hook (not a level sweep — `checkNoRawCuboidScan` exists for a reason), send via a new small payload in `common/network` and render in `CourseHud` where score popups already draw. `ToadDialogueService` is the model to copy; it already owns localised speech for the shopkeeper.

**Touches:** `ToadDialogueService.java`, `CourseComposer.java`, `CourseHud.java`, `ModBlocks.java`

### Power-up transformation staging: pause, flash, and a jingle that ducks the music  *(value 8, medium)*

**From:** Mario presentation — the power-up transformation beat

Taking a power-up briefly halts the player and the level clock, flashes the avatar between its old and new appearance for a beat, plays a short jingle over a ducked level track, and grants a moment of invulnerability so the transformation can never be interrupted by the enemy that was two blocks away. Then control returns.

**Why here:** Getting a power-up is one of about five events the whole game is built to deliver, and here it is a state-record swap plus `SoundEvents.ILLUSIONER_PREPARE_MIRROR` — a borrowed vanilla sound, at a moment where the mod has its own POWER_UP sound registered and unused for this path. There is no pause, no flash, no invulnerability window, and no music treatment. The shrink transition was already given a deliberate six-tick animated ramp in PlayerSizeService (PROGRESS.md task 25) because snapping read badly — the *grow* direction, which is the celebratory one, still snaps.

**Sketch:** In FormService.grant, after the CourseStateAccess.update: set a short invulnerability window through the existing `s.withPips(s.pips(), now + N)` idiom that DamageService uses, latch a transform-ticks value on CourseState so the client can drive the flash, and play ModSounds.POWER_UP instead of the vanilla illusioner sound. Reuse PlayerSizeService's ramp for the size change in both directions. For the duck, add a transform mood or a volume multiplier in CourseMusicManager keyed on the same synced field.

**Touches:** `FormService.java`, `PlayerSizeService.java`, `CourseMusicManager.java`, `CourseState.java`

### Background and foreground depth layers in the canvas  *(value 8, medium)*

**From:** Mario presentation — parallax that reads as depth

The generator writes decorative geometry at depths behind the play lane (distant hills, castle silhouettes, pipe stubs, cloud banks) and occasionally in front of it, themed with the course palette. Because it is real geometry at real z, it parallaxes correctly against the perspective camera for free, and it gives the player's motion a reference frame that a skybox cannot.

**Why here:** MISSING_MECHANICS #91 asks for parallax and scopes it to CourseSkyboxRenderer — but NEXT_TASKS #32 records that the renderer maps one texture onto all six cube faces, so it is not even a cubemap, and a skybox is by definition infinitely distant and therefore *cannot* parallax. Solving depth in the renderer is solving it in the one place where it is impossible. The canvas already keys blocks by (x,y,z) with z packed for a ±128 range, and the camera is explicitly a perspective rail camera rather than orthographic ('to preserve Minecraft rendering compatibility while creating a clean side-on read') — so geometry at z=-8 will parallax against geometry at z=0 with no rendering work at all. The lane is simply never written outside z ∈ [-1, 1].

**Sketch:** Add a decorate pass to CourseComposer, run after populate(), that writes palette blocks at fixed depths (e.g. z=-5 and z=-9) using canvas.fill — a low rolling silhouette whose height is a cheap function of x and the seed, plus occasional theme motifs. Add the depths to GenContext beside LANE_HALF_WIDTH so segments and the decorator agree. CourseWriter flushes whatever the canvas holds, so nothing downstream changes; verify CourseReachability ignores anything outside the lane (it pins laneZ today, which is what makes this safe).

**Touches:** `CourseComposer.java`, `GenContext.java`, `CourseCanvas.java`, `CourseSkyboxRenderer.java`

### Hitstop — a few frozen frames on impact  *(value 7, medium)*

**From:** Mario/2D-platformer juice — hitstop and impact framing

On a stomp kill, a player hit, and a brick break, freeze the participants for 2-4 ticks: the enemy holds its squash pose, the player's bounce is delayed by the same amount, and the sound plays on the first frozen tick. The pause makes an impact land; without it a stomp is a discontinuity in position rather than an event.

**Why here:** The stomp is the mod's core verb and the whole feedback layer around it is simultaneous — sound, particles, squish and bounce all fire in the same tick in resolveStomp, so the impact has no duration. Screen shake is already on the backlog (#93) but hitstop is not, and hitstop is the cheaper and more legible half: it needs no camera work and it respects reducedMotion trivially (skip it).

**Sketch:** Add HITSTOP_TICKS to CourseEnemyEntity's synced data alongside the existing SQUISH_TICKS, decremented in tick(). While non-zero, the entity skips its movement/AI update and CourseEnemyRenderer holds the squash pose. In resolveStomp, defer the bounce(player) call by scheduling it (a small WeakHashMap<ServerPlayer, Integer> pending-bounce in AirMoveService, or a delayed-impulse field ticked in MovementRuleService). Mirror the same freeze on the player side in DamageService.interceptDamage for a pip loss.

**Touches:** `CourseEnemyEntity.java`, `CourseEnemyRenderer.java`, `DamageService.java`

### Player squash-and-stretch, landing dust and heavy-landing feedback  *(value 7, medium)*

**From:** Mario juice — squash and stretch, landing puff

The avatar stretches vertically at jump launch and squashes on landing, with the squash amplitude scaled by fall distance; a dust ring puffs at the feet on any landing above a small threshold, sized by the same number. A heavy landing (a long fall or a ground pound) squashes deeper and holds longer.

**Why here:** Enemies already have a full squish framework — SQUISH_TICKS synced, a sine-curve dip, width widening as height flattens so volume reads as conserved — and the player, who is on screen 100% of the time, has none of it. There is no landing feedback anywhere in the mod: no LivingFallEvent handler exists, and the ground-pound landing only spawns CRIT particles when it happens to break a block underneath. Landings are the most frequent event in a platformer and currently the least acknowledged.

**Sketch:** Mirror the enemy solution rather than inventing a second one: add squash ticks to CourseState (already synced to the client) or a small client-side latch driven from a new LivingFallEvent / onGround-edge subscriber in ServerEvents. Apply the scale in a RenderPlayerEvent.Pre pose push, using the same sine dip and inverse width/height relation as CourseEnemyRenderState. Emit the dust from ServerEvents on the landing tick using ModParticles, matching the CLOUD puff already used for the enemy-defeat smoke.

**Touches:** `ServerEvents.java`, `CourseEnemyRenderState.java`, `ClientEvents.java`, `ModParticles.java`

### Animated score tally on the results screen  *(value 6, small)*

**From:** Mario presentation — the end-of-level count-up

The results panel counts: remaining time ticks down while the score ticks up by the same conversion, each tick playing a short blip at rising pitch, coins and star coins tally after it, and a held input skips to the final total. The count is the reward, not the number.

**Why here:** CourseResultsScreen renders six static rows in render() and is finished the frame it opens. Everything needed for the animation is already there and already correct: CourseScoringService.Results carries timeBonus, healthBonus, enemiesDefeated and runTicks separately (deliberately, so 'the results screen can show the same breakdown'), and CourseResultsPayload ships them to the client. The data was assembled for a tally that was never built. Not on any list — MISSING_MECHANICS #96 covers the flagpole slide, not the screen after it.

**Sketch:** Add a tick counter to CourseResultsScreen (Screen has tick()); hold a per-row 'revealed amount' that lerps toward the payload value over a fixed number of ticks, play a short blip via minecraft.getSoundManager() at a pitch rising with progress, and skip to done on any key press. Keep render() reading the animated values instead of results.* directly. Purely client-side; the payload and the save record are untouched.

**Touches:** `CourseResultsScreen.java`, `CourseResultsPayload.java`


## block

### Semisolid platforms — stand on top, pass through from below  *(value 9, small)*

**From:** Super Mario Maker 1/2

A surface with collision only on its top face, approached from above. Jumping into it from underneath passes straight through; standing on it and holding crouch-plus-jump drops through deliberately. Enemies walk on it normally.

**Why here:** This is the most-used part in the entire Maker palette and PlaneShift does not have it. Every platform in `SegmentLibrary` — `PLATFORM_LADDER`, `STEPPING_STONE_HOPS`, `MULTI_TIER_CANOPY`, `GAP_WITH_PLATFORM` — is a full cube, so a missed jump means a head bump and a dead stop instead of a pass-through and a retry. It is a large part of why layered courses read as cramped, and it is a precondition for the vertical sections the backlog wants: stacked routes are unusable when every floor is also a ceiling.

**Sketch:** A new block class overriding `getCollisionShape` to return a top-face box only when the context's entity is descending and above the block — `CollisionContext.isDescending()` plus a Y check, the vanilla trapdoor/scaffolding pattern. Register in `ModBlocks` with `noOcclusion()` (see NEXT_TASKS item 33 — every shaped block needs it). Drop-through hooks into `CourseCrouch`, which already owns the crouched-hitbox rule on both sides. `CourseReachability`'s flood-fill needs to treat it as one-way so the walkability proof stays honest.

**Touches:** `ModBlocks.java`, `CourseCrouch.java`, `CourseReachability.java`

### POW block — a one-shot course-wide shockwave  *(value 7, small)*

**From:** Super Mario Maker 1/2

Hit it from below, stand on it, or throw it: everything currently standing on a floor within the course shakes loose — grounded enemies flip, loose blocks pop their contents, coins in the air drop. It has a small number of uses and then breaks. Airborne enemies are untouched, so the answer to a POW is to be off the ground.

**Why here:** It is the clearest possible payoff for the carry system, and PlaneShift has no crowd answer at all — every enemy must currently be handled one at a time by stomping. A POW also gives the score chain something to spike on, which the combo ladder already supports (100/200/400… up to 1-Ups) but has no way to actually reach in normal play.

**Sketch:** New block; on trigger, iterate enemies from the course's tracked spawn list rather than a raw cuboid scan (`checkNoRawCuboidScan` is a build check and will fail you otherwise — the course's entities are already enumerable through the generated-entity tag `CourseStructureService` applies). Route each defeat through the same path a stomp uses so `CourseScoringService`'s combo ladder counts it. Pair with the screen shake already queued as NEXT_TASKS item 23, respecting `reducedMotion`.

**Touches:** `ModBlocks.java`, `CourseScoringService.java`, `CourseStructureService.java`


## powerup

### Star Power that actually defeats enemies, with an expiry warning  *(value 9, medium)*

**From:** Mario invincibility — contact kills, escalating chain, ending tell

While invincible, touching an enemy defeats it (advancing the same combo ladder a stomp chain uses) rather than merely failing to hurt the player. The effect flashes the avatar through a colour cycle, and in the last ~2 seconds the flash speeds up and the music pitch rises as an audible countdown so the player knows to stop running into things.

**Why here:** Star Power today grants damage immunity plus vanilla Speed/Jump Boost/Fire Resistance, and nothing else. Running through a Goomba at full speed makes the Goomba survive and the player bounce off it: the fantasy of the item — the one power-up in the genre everyone can describe — does not happen. There is a dedicated MUSIC_STAR_POWER track and a STAR_POWER mob effect with its own texture already shipping, all in service of an effect that does not do its defining thing. Neither the contact kill nor the expiry warning appears in MISSING_MECHANICS or NEXT_TASKS.

**Sketch:** In CourseEnemyEntity.playerTouch, before the side-contact branch, check player.hasEffect(ModEffects.STAR_POWER); if set, call the same kill path resolveStomp uses on a lethal hit (hurtServer with stompDamage, ENEMY_DEFEAT sound, smoke puff) and route the score through CourseScoringService.awardStomp so it shares the ladder. For the warning, read the effect's remaining duration in CourseHud / a client tick and drive a flash; CourseMusicManager already has a pitchedMusic() helper built for the hurry-up, so the countdown pitch is the same call with STAR_POWER as the mood.

**Touches:** `CourseEnemyEntity.java`, `ServerEvents.java`, `CourseMusicManager.java`

**Partly present already.** What is left: Left to build: (1) contact defeat — hook where CourseEnemyEntity already resolves player contact (:160-176, the branch that currently does contact damage) and route a Star kill through CourseScoringService.awardStomp so it shares the existing ladder; (2) the flash — a client render tint keyed off ModEffects.STAR_POWER; (3) the countdown — read the effect's remaining duration and accelerate the flash, and repitch the STAR_POWER music instance, for which CourseMusicManager.pitchedMusic (:148-160) is already the exact tool.

### Drill form — burrow through floors and ceilings  *(value 8, medium)*

**From:** Super Mario Bros. Wonder (2023) — Drill Mushroom

A form that lets the player submerge into any solid surface and travel inside it, emerging on command. Underground the player is immune to everything above; on a ceiling they hang and travel inverted. Depth is one block, so it is a traversal layer rather than free flight, and hazards embedded in the surface still hurt.

**Why here:** It is the one power-up on this list that changes what the level *is* rather than what the player shoots, and it maps unusually well onto a block game — burrowing through terrain is native here and expensive in a sprite platformer. It also gives the generator a legitimate second route through any segment without authoring one, which is exactly what a procedural game needs for secrets. Nothing in the current fourteen-form roster grants traversal through geometry: `CLOUD_STEP` adds a surface, `GALE_DASH` crosses a gap, `ACORN_GLIDE` slows a fall.

**Sketch:** New `FormActionKind.BURROW` plus a datapack form JSON alongside `cloud.json` — the registry, charges, cooldown and reserve rules all come free. Implementation is a movement mode in `MovementRuleService`: suppress collision against course terrain blocks only (never the world border or the kill plane), lock the player to the rail, and force-emerge on entering air or a non-course block. `PlayerSizeService` already ramps a scale modifier over ticks and is the right place for the submerge animation. Reachability must not assume the drill exists — courses stay clearable without it, per the completability rule.

**Touches:** `FormActionKind.java`, `MovementRuleService.java`, `PlayerSizeService.java`, `form`

### Invincibility assist Form granted after repeated failure  *(value 8, small)*

**From:** New Super Mario Bros. 2 / NSMBU — the white assist power-up offered once you have died enough times

A softer sibling of the demonstration run. On the same failure counter, the course start and each checkpoint offer a one-off assist Form: the player is immune to all enemy contact damage and to the course clock, but still dies to pits, cannot collect star coins while it is active, and loses the Form the instant the course is cleared. It is a distinct Form from Star Power — it does not expire on a timer, does not defeat enemies on contact, and does not grant a speed boost, so it reads as a crutch rather than a reward.

**Why here:** It is the low-effort half of the Super Guide, and it lands in a system that already exists: PlaneShift has a full data-driven Form registry with a category enum, a NONE action kind for stat-only Forms, and an accentColor field, so this is a JSON file plus a damage-intercept branch rather than new architecture. It also gives the DOWNED/game-over loop a middle setting between 'try again' and 'be shown the answer', which matters because the game-over path currently dumps the player all the way back to the hub with a fresh life bar.

**Sketch:** Add a form JSON under data/planeshift/planeshift/form/ with category DEFENSE and action NONE, and a matching item in ModItems. In DamageService.interceptDamage, add a branch beside the existing STAR_POWER check that returns true for everything except FELL_OUT_OF_WORLD and GENERIC_KILL — a two-line change to a method that already implements exactly this shape for Star Power. Gate the grant on the CourseProgress failure counter added for the Super Guide, handed out by CheckpointService when a checkpoint activates. Suppress star-coin crediting in CourseScoringService.awardStarCoin while the Form is active.

**Touches:** `DamageService.java`, `ModItems.java`, `CheckpointService.java`, `CourseScoringService.java`, `FormDefinition.java`

### Blue Shell — a wearable shell with a crouch-run slide attack  *(value 8, medium)*

**From:** New Super Mario Bros. DS — the shell you duck into and skid across the ground inside

A Form that puts the player inside a shell. Ducking while running enters a slide: the player becomes a low, fast projectile that defeats enemies on contact, ricochets off walls, and passes under one-block ceilings. While sliding the player cannot jump-cancel immediately — there is a short commitment window, which is what makes it a decision. Ducking while stationary makes the shell a stationary guard that blocks one hit from any direction, including from above, which is the only defence in the roster against a Thwomp or a falling hazard.

**Why here:** The Form roster is heavy on offense-at-range (fire, ice, hammer, boomerang, ember) and traversal (leaf, propeller, acorn, cloud, cat), and thin on anything that changes how the player *moves through terrain*. The shell slide is a movement Form disguised as a weapon. It also has a ready-made home: CourseCrouch already collapses the hitbox to 0.5 blocks specifically so the player can slide under one-block gaps, so the geometry the slide needs is already generated and already proven traversable.

**Sketch:** New FormActionKind.SHELL_SLIDE plus a form JSON and a ModItems entry. The slide is a state in CourseMovementService: entered when CourseCrouch reports crouched and horizontal speed exceeds a threshold, it sets a velocity along the travel axis, suppresses the jump impulse for N ticks, and registers the player as a damage source against CourseEnemyEntity — reusing the same defeat path KoopaEntity's sliding shell already uses against other enemies. Wall ricochet reuses the reflection logic already written for the sliding Koopa shell. The stationary guard reuses FormService.absorbHitWithForm, which already implements 'the Form eats one hit'.

**Touches:** `FormActionKind.java`, `CourseMovementService.java`, `CourseCrouch.java`, `KoopaEntity.java`, `FormService.java`

**Partly present already.** What is left: What is actually left: the crouch-run slide as a moving hurtbox that defeats enemies on contact, wall ricochet along the rail, and the jump-cancel commitment window. The slide is the whole feature; drop the ceiling and guard clauses from the pitch. FormActionKind is a closed enum of trusted behaviours (common/form/FormActionKind.java) so it needs one new constant plus a FormService branch, following the CLAW_SWIPE/TAIL_WHACK pattern.

### Crown Form with a one-shot pit rescue  *(value 8, medium)*

**From:** New Super Mario Bros. U Deluxe — the crown form that floats and saves you from one fall

A Form built around forgiveness rather than power: a hover-float on held jump, a mid-air second jump, and one automatic pit rescue per course life — falling below killY while the Form is active does not down the player; instead they are launched back up to the last solid ground they stood on, the Form is consumed, and the rescue is spent. The Form is not obtainable from question blocks; it is a Toad House gift and a reward for full star-coin completion, so it reads as a chosen assist rather than the default state.

**Why here:** PlaneShift's kill plane is currently absolute: CourseState.killY routes straight into DamageService.down with no counterplay, and it is the failure the generator produces most, because every course is a corridor with pits over a void. A single recoverable fall changes the emotional shape of a course far more than another projectile Form would. It also gives the Toad House and the star-coin total something meaningful to hand out, which they currently lack.

**Sketch:** A new Form JSON with FormActionKind.CLOUD_STEP reused for the float (it already exists and does 'upward hop with slow fall'), plus a rescue flag tracked in FormService's per-player state. Hook the killY check where it already lives — the DamageService.down path handles FELL_OUT_OF_WORLD by calling resolveDown directly — and branch before that: if the rescue is available, teleport to the last-grounded position (CheckpointService already tracks and restores positions, so record a rolling 'last safe block' there) and consume the Form via FormSlot.loseActive. Add the Form to MapNodeService.GIFTS.

**Touches:** `DamageService.java`, `CheckpointService.java`, `FormService.java`, `MapNodeService.java`, `ModItems.java`

### Bubble form — trap an enemy, then stand on it  *(value 7, medium)*

**From:** Super Mario Bros. Wonder (2023) — Bubble Flower

Fires slow floating bubbles that drift forward and upward. A bubble that meets an enemy encloses and defeats it; a bubble that meets nothing persists a few seconds and can be jumped on for a single bounce, then pops. So the same shot is either a weapon or a platform depending on where it is aimed, and a player can build a short staircase out of their own missed shots.

**Why here:** Every offensive Form in the roster is a projectile that deletes a thing — fire, ice, hammer, boomerang, ember bolt all differ only in arc and status. This is the first that answers a traversal problem and a combat problem with one input, which is what makes a power-up memorable rather than a damage type. It is distinct from `CLOUD_STEP`, which places a static platform on command and cannot hit anything.

**Sketch:** New projectile in `common/entity` following `FireballProjectile`'s shape but with near-zero gravity and a lifetime; on entity hit, kill through the same defeat path so `CourseScoringService`'s combo ladder credits it. Bounce is a collision case that reads `ModAttributes.BOUNCE_HEIGHT` — the attribute already exists precisely so a Form can tune the stomp bounce without the enemy knowing about it, and this is the case it was built for. Register in `ModEntities` and add a renderer, or `checkEntityRenderers` fails. Add `FormActionKind.BUBBLE_SHOT` and a form JSON.

**Touches:** `FireballProjectile.java`, `ModAttributes.java`, `FormActionKind.java`, `ModEntities.java`

### Propeller drill-slam and shake-to-hover descent  *(value 7, small)*

**From:** New Super Mario Bros. Wii — the propeller's downward drill and the flutter that turns a fall into a hover

Two additions to the propeller's existing straight-up launch. First, a downward input at the apex converts the spin into a drill: a fast vertical slam that breaks brick blocks from above (which nothing currently can), triggers a wide ground-pound shockwave on landing, and passes through soft ground. Second, holding the jump input during any subsequent descent re-engages the propeller at reduced power for a slow, controllable hover with a hard time budget, so the propeller is a three-part verb — launch, hang, slam — rather than a single button.

**Why here:** PROPELLER_SPIN as it stands is one impulse, functionally a second jump; the propeller's identity in the source material is that up and down are the same power. The drill is the more valuable half here because PlaneShift's block set is entirely bottom-activated — HitFromBelowBlock is an interface, and every question block, brick, coin ring and cache is hit from underneath. A power that reaches blocks from above opens a whole authoring axis for SegmentLibrary that currently does not exist.

**Sketch:** Extend the PROPELLER_SPIN branch in FormService.useAction with a phase field on the Form's charge state (FormSlot already carries charges and cooldownUntil, which is enough to encode phase without a new record). The drill applies a large negative Y velocity and registers the player in AirMoveService's tick as drilling; on landing, call the existing ground-pound shockwave path and break any BrickBlock in a small radius via BlockAreaScan (which already exists and is the sanctioned bounded-scan helper). The hover is a terminal-velocity clamp in AirMoveService alongside the wall-slide clamp already implemented there.

**Touches:** `FormService.java`, `AirMoveService.java`, `BrickBlock.java`, `BlockAreaScan.java`, `HitFromBelowBlock.java`

**Partly present already.** What is left: What is left: (1) the apex-down input that converts the spin into a fast vertical slam — new, and worth it as a propeller verb; (2) the wide AoE shockwave on landing — genuinely new, since the existing pound affects exactly one block, blockPosition().below() (AirMoveService.java:203); (3) the hold-jump hover re-engage at reduced power with a time budget — new. Delete "breaks brick blocks from above (which nothing currently can)" from the pitch; that already works, and the drill's value is the reach and the shockwave, not the brick.

### Boo Mushroom — a phase-through form  *(value 7, medium)*

**From:** Super Mario Galaxy

A form that turns the player translucent and lets them drift slowly upward when the jump key is held, pass through specific block types tagged as permeable (grates, lattices, bars) but not through solid walls, and become invisible to enemies that use sight. Its cost is severe and it is what makes it a Form rather than a cheat: it evaporates instantly in light, so the form only survives in the dark and expires the moment the player leaves the ghost house.

**Why here:** The Haunted Manor world exists (five worlds, world four is `ghost_house`), and MISSING_MECHANICS 69 records the honest state of it: "only LAVA has a distinctive hazard; snow, desert and ghost house differ by palette only." A phase form is the mechanic that makes a ghost house a ghost house — the layout can be built around walls that are only walls for a lit player, which is a real puzzle rather than a palette. `ModBlocks` already has `COURSE_LATTICE` for the permeable set, and `BooEntity` already exists to contrast with.

**Sketch:** A `BooMushroomItem` in `common/item` following the shape of `CloudFlowerItem`, a `boo_aura` `MobEffect` in `ModEffects` (which already registers nine auras), and a Form JSON in `data/planeshift/planeshift/form/`. The phase behaviour is a `server/PhaseFormService` in the style of `CatFormService` — same `WeakHashMap` pattern, same `hasXForm(player)` effect check, same per-tick call from `ServerEvents`. Permeability by block tag so a datapack decides which blocks are ghostable. The light kill-switch reads `level.getBrightness` on the player's tick.

**Touches:** `CloudFlowerItem.java`, `ModEffects.java`, `CatFormService.java`, `FormActionKind.java`, `ModBlocks.java`

### Drop the stocked item into the world  *(value 6, trivial)*

**From:** New Super Mario Bros. Wii/U — pressing the stock slot to release the held item so anyone can grab it

The reserve slot gains a second verb alongside swapping. A separate input drops the reserved Form into the world as a live pickup at the player's feet, where it drifts and can be collected by anyone — including a teammate who has nothing. Dropping is refused while airborne and while a swap lockout is running, so it cannot be used to dodge a hit. In solo play it is still useful: dropping a reserve at a checkpoint stages it for the retry after a death.

**Why here:** FormService already implements the entire reserve rotation — grant while empty, grant while powered, displace an older reserve into coins — and PlaneShiftKeybinds already has a swap key, so the machinery is complete except for the one action that makes the reserve social. It matters most in co-op: the reserve is the only transferable resource in the mod, and without a drop there is no way for a strong player to help a weak one at all. It also cleans up an existing rough edge, where a displaced reserve is silently converted to coins with no chance to hand it over.

**Sketch:** Add a DROP_RESERVE KeyMapping in PlaneShiftKeybinds and a payload beside the existing swap-reserve one (network payloads and PayloadRateLimiter already exist and the limiter has an action enum to extend). Server-side, a dropReserve method in FormService next to swapReserve: validate onGround and no lockout, clear the reserve via FormSlot.withReserve(Optional.empty()), and spawn the matching item. The drifting behaviour is free — PowerupDriftService already makes dropped power-ups slide, turn at walls and fall off ledges via EntityTickEvent, and MapNodeService already establishes that dropping the item rather than inserting it is the correct way to route through the pickup path.

**Touches:** `FormService.java`, `PlaneShiftKeybinds.java`, ``, `PowerupDriftService.java`, `PayloadRateLimiter.java`

### Carried companion creatures with per-type abilities  *(value 6, medium)*

**From:** New Super Mario Bros. U — the baby Yoshis you hold in your arms, each doing one strange thing

Deliberately distinct from a rideable mount: a small creature the player carries in front of them, held in the hands, occupying no inventory slot and lost when the player takes a hit. Three types, each with one verb. A glowing one lights an unlit area in a radius and reveals hidden blocks within it. An inflating one lets the player hold jump to float upward while it puffs up, with a hard time limit. A devouring one eats any enemy or projectile it touches on a short cooldown, growing visibly until it is dropped. Each is found in a specific course rather than dispensed, so encountering one is an event.

**Why here:** A mount is a large piece of work with a hard camera problem in a fixed side-on game — the rider changes the player's height, hitbox and jump arc, and PlaneShift's camera and 2.5D rail assume a single-body player. A carried companion delivers most of the same expressive range with none of that: it is an offset render and one ability, and it composes with every Form rather than replacing one. It is also the natural way to give the ghost-house theme a light source, which the mod currently has no mechanic for at all.

**Sketch:** A CompanionEntity extending the CourseEnemyEntity base (which already provides the squish framework and lane clamping) with a carried mode that parents it to the holder using the same passenger offset approach as the player-carry feature above. Carried state lives in CourseState as an Optional<Identifier> beside the FormSlot, so it syncs and persists through the existing codec with an optionalFieldOf default. The hit-loss rule hooks DamageService.interceptDamage before FormService.absorbHitWithForm, so the companion is lost before the Form is — a clear damage ladder. The glow companion uses the existing HiddenQuestionBlock reveal path; the float reuses AirMoveService's terminal-velocity clamp; the devourer reuses CourseEnemyEntity's defeat path. Place them via a SegmentLibrary segment tagged SECRET.

**Touches:** `CourseEnemyEntity.java`, `CourseState.java`, `DamageService.java`, `AirMoveService.java`, `HiddenQuestionBlock.java`, `SegmentLibrary.java`

### Rock Mushroom — a rolling destruction form  *(value 6, medium)*

**From:** Super Mario Galaxy 2

A form with two states. Standing, the player is normal but heavy. Spinning, the player becomes a boulder that rolls at high speed, cannot stop or turn sharply, smashes through brick and crate blocks and every enemy in its path, ricochets off hard walls, and is steered only by leaning. The player ends the roll deliberately or by hitting something that stops it. It is a form that removes control in exchange for power — the opposite trade from every projectile form in the mod.

**Why here:** All eleven of PlaneShift's action Forms are variations on one verb: press a button, a thing comes out (`EMBER_SHOT`, `FIRE_SHOT`, `ICE_SHOT`, `HAMMER_THROW`, `BOOMERANG_THROW`) or the player moves a little differently (`GALE_DASH`, `PROPELLER_SPIN`, `ACORN_GLIDE`, `CLOUD_STEP`). A form that *changes what the player is* rather than what they emit is a category the roster does not have, and it makes brick walls — which `SegmentLibrary.brick_wall` already places — into a route rather than an obstacle.

**Sketch:** `RockMushroomItem` + `rock_aura` effect + a `ROCK_ROLL` entry in `FormActionKind`, all following the `CatSuitItem`/`CAT_AURA`/`CLAW_SWIPE` precedent exactly. Behaviour in a `server/RockFormService` shaped like `CatFormService`. Block destruction should route through the same `HitFromBelowBlock`/`BrickBlock` break path a head bump uses so a rolled-through brick behaves identically to a bumped one, and enemy contact through `CourseEnemyEntity.hurtServer` so the existing combo ladder in `CourseScoringService` counts the kills. Note `BuzzyBeetleEntity` is deliberately fire-immune; the roll should ignore that, which is what makes it worth carrying.

**Touches:** `CatSuitItem.java`, `CatFormService.java`, `FormActionKind.java`, `BrickBlock.java`, `CourseScoringService.java`

### Spring Mushroom — a form that takes away the jump button  *(value 6, small)*

**From:** Super Mario Galaxy

The player is encased in a coiled spring and can no longer walk-and-jump: they bounce continuously and involuntarily at a small height, and pressing jump at the exact moment of ground contact converts that into a very large bounce. Steering happens mid-air with reduced authority. Every platform becomes a timing problem, and the height it grants is genuinely unreachable any other way, so the generator can hide rewards behind it.

**Why here:** It is the cheapest possible way to add a difficulty lever that is not "a wider gap". NEXT_TASKS task 9 asks specifically for *combination* rather than more-of-the-same as the late-world difficulty lever; a form that changes the player's input contract makes every existing segment in `SegmentLibrary` re-readable without touching a single builder. And unlike most power-ups it is a constraint, so it can be forced on the player by a course rather than chosen, which is a design tool the mod currently has none of.

**Sketch:** `SpringMushroomItem` + `spring_aura` + a per-tick service in the `CatFormService` mould, applying an upward impulse on every ground contact and reading `player.getLastClientInput().jump()` for the charged variant — `AirMoveService` already demonstrates edge-triggered input reading against `getLastClientInput()` and explains why measured position deltas beat `getDeltaMovement()` on the server. The forced variant is a course-level rule applied through `MovementRuleService`, which is the existing home for per-course movement overrides.

**Touches:** `ModItems.java`, `ModEffects.java`, `AirMoveService.java`, `MovementRuleService.java`

### Bee-style flight with a depleting meter and a hard disqualifier  *(value 6, medium)*

**From:** Super Mario Galaxy

Held flight governed by a visible meter: the player rises while the button is held, the meter drains, and it only refills on solid ground. Landing on flowers, honeycombs or other tagged surfaces lets the player stick and cling to vertical faces. The disqualifier is what shapes the levels — touching water instantly ends the form, so a bee course is designed around avoiding one specific substance rather than avoiding falls.

**Why here:** PlaneShift has three flight-adjacent forms — `PROPELLER_SPIN` (one launch), `ACORN_GLIDE` (a forward boost) and `CLOUD_STEP` (a hop with slow fall) — and every one is a discrete impulse with a charge count. None is sustained, metered flight, which is a fundamentally different traversal budget: the player plans a route against a resource instead of counting charges. It also gives the HUD (`client/hud/CourseHud`) something continuous to display, and it is the natural fit for MISSING_MECHANICS 61 (vertical course sections), which currently has no traversal answer.

**Sketch:** Add a `FLIGHT_METER` entry to `FormDefinition` usage via a new `FormActionKind.HOVER_FLIGHT`; the definition record already carries `actionPower`, `maxCharges` and `cooldownTicks`, so the meter can be expressed as charges consumed per tick without a schema change. Drain/refill in a service alongside `LeafFlightService`, which already exists and is the closest neighbour. Meter rendering in `CourseHud`, which already draws pips, coins and the countdown clock. The water disqualifier is a `player.isInWater()` check in the same tick.

**Touches:** `LeafFlightService.java`, `FormDefinition.java`, `FormActionKind.java`, `CourseHud.java`

**Partly present already.** What is left: What is left is the shaping, not the flight: (1) a real drain-while-held meter that refills only on solid ground, plus its HUD readout in `client/hud/CourseHud.java` and a field on `CourseState` so it syncs; (2) cling on *tagged* surfaces (flowers/honeycomb) rather than the Cat's any-wall cling; (3) the water disqualifier. Point (3) is the load-bearing one and it is currently unbuildable as a course design — docs/MISSING_MECHANICS.md:41-43 (#10) records that underwater physics do not exist and #63 that the water theme is blocked on it, so "a course designed around avoiding one substance" has no substance to avoid yet. Sequence after #10, or retarget the disqualifier at lava, which the LAVA theme already has.


## other

### An in-game course editor that writes back to the generator's own format  *(value 8, large)*

**From:** Super Mario Maker 1/2

A build mode inside the course dimension: a palette of the mod's existing blocks and enemies, free placement, an immediate play-test toggle, and a save that produces a course the normal loader can run. The palette is literally the mechanics checklist — anything placeable is a mechanic that exists and is understood.

**Why here:** PlaneShift's generator is good and its parts list is large, but nobody — including the team — can currently hand-author a level to compare against. That is why 'the mod has never been played start to finish' and 'the generator cannot produce X' are both still open: there is no way to build the reference level. An editor is also the honest home for the unused template path, and it turns every future part into content automatically rather than requiring a segment to be coded for it.

**Sketch:** The pieces are further along than they look: `CourseCanvas` is already a pure data course representation with blocks, entity spawns, item drops and markers, and `CourseWriter.write` already turns one into a live world. An editor needs the inverse — read the placed region back into a `CourseCanvas` — plus JSON serialisation of that canvas beside `CourseDefinition`. Play-test is `CourseService.loadCourse` against the in-memory canvas. `CourseStructureService.placeTemplate` exists and nothing calls it (MISSING_MECHANICS #70); an editor's save target is the reason it would finally have a caller. `TesterScreen` and the F6 tester menu are the existing precedent for a developer-facing in-game UI.

**Touches:** `CourseCanvas.java`, `CourseWriter.java`, `CourseStructureService.java`, `TesterScreen.java`


## audio

### Death stinger and music silence during the down sequence  *(value 8, small)*

**From:** Mario audio — the death jingle

On a lethal hit the level music stops dead, a short death stinger plays alone, and the level track only resumes when the player is back in control at the checkpoint. Silence is the point: the absence of the loop is what tells the player the run broke.

**Why here:** The mod has a staged death — DamageService.down sets PlayState.DOWNED, launches the player upward at 0.7, and holds them there before resolveDown, exactly the classic pop-then-fall — and the level music loops cheerfully through the whole thing, because CourseMusicManager picks its mood from in2_5D()/inCourse() and DOWNED is still in a course. The most carefully staged moment in the mod has its audio undermined by the mood table. There is no death sound in ModSounds either; down() reuses DAMAGE at 0.8 volume / 0.9 pitch.

**Sketch:** Add a DOWNED mood to CourseMusicManager.Mood, ranked above COURSE_* and below STAR_POWER, mapped to no looping track. Select it when ClientCourseState.get().state() == PlayState.DOWNED (PlayState is already synced with CourseState). Register MUSIC_DEATH / SFX death stinger in ModSounds and play it as a one-shot from DamageService.down instead of the reused DAMAGE sound.

**Touches:** `CourseMusicManager.java`, `DamageService.java`, `ModSounds.java`

### Hurry-up should retune the running track, not restart it  *(value 7, small)*

**From:** Mario audio — the tempo shift at low time

When the clock crosses the warning threshold, the currently playing loop speeds up in place — same track, same position, higher rate — preceded by a short two-note warning stinger. The player recognises the music they have been hearing put under pressure.

**Why here:** The mod's own comment states this intent exactly: 'A pitch shift rather than a second recording, which is how the original did it: the player recognises the track they have been listening to, sped up.' But the implementation calls play(), which calls stop() and then starts a brand-new SimpleSoundInstance. The track therefore jumps back to bar one at the exact moment the player is most tense — the effect reads as the music glitching, not as urgency, and it undoes the stated design goal. There is also no warning stinger, so the tempo change is the only cue and it arrives simultaneously with the panic.

**Sketch:** CourseMusicManager cannot reposition a SimpleSoundInstance, so either (a) keep the current instance and let a custom SoundInstance subclass expose a mutable pitch that the sound engine reads per-buffer, or (b) accept the restart but mask it: play a short HURRY_WARNING one-shot on the transition tick and fade the old instance out over ~10 ticks before starting the pitched one. (b) is the small change and fixes the perceived glitch. Add HURRY_WARNING to ModSounds beside COURSE_CLEAR / ONE_UP.

**Touches:** `CourseMusicManager.java`, `ModSounds.java`

**Partly present already.** What is left: Two deltas remain, and one of them is not buildable as described. "Same position" is impossible: play() at :126-139 stops the instance and starts a new one because Minecraft's SoundManager cannot seek or repitch a playing SoundInstance — the restart is an engine constraint, not an oversight. What is genuinely left is the two-note stinger (the current warning at CourseTimerService.java:55 is a single pitched blip), and optionally masking the restart by firing the stinger over the seam.

### Course-clear fanfare that owns the music channel  *(value 7, medium)*

**From:** Mario audio — the goal fanfare

Crossing the goal stops the level loop, plays the clear fanfare on the music channel uninterrupted, and only then opens the results screen — the tally animation runs under the tail of the fanfare rather than over the level track.

**Why here:** COURSE_CLEAR is currently played as a positional SoundSource.PLAYERS effect from CourseCompletionService while the 2.5D course loop is still running, and the state flips to HUB in the same method, so CourseMusicManager immediately starts the *hub* track on top of both. Three pieces of audio overlap at the single most rewarding moment in the game. The flagpole slide was specifically implemented as the reward moment (MISSING_MECHANICS #96) and its audio staging was never done.

**Sketch:** Add a CLEAR mood to CourseMusicManager mapped to the fanfare as a non-looping music instance, selected while PlayState.RESULTS is set. In CourseCompletionService.onComplete, keep the state at RESULTS and defer the CourseService.returnToHub / mood flip until the fanfare length has elapsed — the class already has SLIDING_PLAYERS as a per-player staged-sequence map to model this on. Play COURSE_CLEAR through SoundSource.MUSIC so the player's music slider governs it.

**Touches:** `CourseCompletionService.java`, `CourseMusicManager.java`

**Partly present already.** What is left: Left to do: stop the loop (CourseMusicManager.stop() already exists, :162-167, it just needs a trigger), play COURSE_CLEAR as SoundSource.MUSIC, and delay the CourseResultsPayload send by the fanfare length instead of sending it inline at CourseCompletionService.java:85.

### Rising-pitch coin combo  *(value 6, small)*

**From:** Mario audio — the ascending coin/collectible chain

Coins collected in quick succession play at rising pitch, stepping up a scale and resetting after a short gap. The audio itself tells the player they are in a chain, and the reset is the cue that the chain broke.

**Why here:** Every coin in the mod plays at a hard-coded pitch: 1.0 from a coin block, 1.4 from a brick, 1.2 from a coin ring, 1.0 from a floating pickup. Coins are the densest feedback event in a Mario level and here they are a metronome. This matters more than usual for PlaneShift specifically because CoinRingBlock and the coin-brick payout are *designed* as short bursts — the mechanic that makes them feel like a reward is the ascent, and it is missing. Not on any list.

**Sketch:** Add a small per-player coin-chain tracker to CourseScoringService (it already owns CHAINS and CHAIN_TIMEOUT_TICKS for stomps — mirror the shape): step index 0..7, incremented per coin, reset after ~20 ticks without one. Have awardCoin return the step, and have ServerEvents.applyPowerup / CoinBlock / BrickBlock / CoinRingBlock play COIN_PICKUP at 1.0 + step * 0.08 instead of their literals. Cap the pitch so it does not become inaudible.

**Touches:** `CourseScoringService.java`, `ServerEvents.java`, `CoinBlock.java`, `CoinRingBlock.java`, `BrickBlock.java`


## secret

### Secret signposting grammar  *(value 8, medium)*

**From:** Mario level grammar — how a secret announces itself

A secret is never invisible; it is *unexplained*. The generator marks each secret with a legible tell drawn from a small vocabulary — a coin arc leading off the main path and stopping in mid-air, a single out-of-place block in an otherwise regular row, a gap in a ceiling, a dead-end that is obviously too deliberate to be an accident. On discovery, a confirmation beat (a distinct chime and a burst) rewards the reading rather than the luck.

**Why here:** Six SECRET-tagged segments exist (hidden_cache, vine_secret, secret_prize_vault, p_switch_bonus_room, hidden_spring_highway, illusory_pipe_cache) and several are built out of literally invisible affordances — SECRET_PASSAGE fake blocks and a SECRET_VINE that only appears when hit from below. With no tell, these are not secrets, they are content that nobody will ever see: a player has no reason to jump into that specific wall out of the thousands of identical blocks in the course. The mod is currently paying full authoring cost for six segments with a near-zero discovery rate.

**Sketch:** Add a helper to GenContext (e.g. ctx.signpost(canvas, x, y, kind)) that draws the tell, and call it from the six SECRET segments' build methods at the point where they currently place the hidden affordance. Coins are already placeable via canvas.item(ModItems.COIN.get(), ...) so the coin-arc tell needs no new block. For the confirmation beat, SecretPassageBlock and SecretVineBlock are the discovery sites — play a distinct chime there rather than reusing COIN_PICKUP, and send a ScorePopupPayload, which already exists and is already wired.

**Touches:** `SegmentLibrary.java`, `GenContext.java`, `SecretPassageBlock.java`, `SecretVineBlock.java`

**Partly present already.** What is left: What is missing is the grammar, not the secrets. HIDDEN_SPRING_HIGHWAY's hidden question block (:800) is placed with no tell at all, which is exactly the "invisible rather than unexplained" failure. Left to build: a shared tell vocabulary in GenContext (coin arc stopping in mid-air, one out-of-place block in a regular row, a ceiling gap) that every SECRET segment calls instead of hand-placing its own hints, and a uniform confirmation beat on discovery — SecretVineBlock's chime is the template, HiddenQuestionBlock.java:43 currently reuses the ordinary QUESTION_BUMP.

### Self-contained challenge rooms behind a marked door  *(value 7, medium)*

**From:** Super Mario Odyssey (challenge/timer rooms), Super Mario 3D Land (Mystery Boxes)

A door or box in an ordinary course leads to a small sealed room with one rule, a strict timer, and exactly one reward. The room is a single idea executed once — defeat every enemy in twenty seconds, cross a platform sequence with no floor, collect eight coins before they vanish, survive a wave. Failure ejects the player back to the course with no penalty beyond the lost reward, which is what lets the rooms be genuinely hard. Because the room is sealed and short, its rules can be extreme in ways a full course cannot afford.

**Why here:** The generator produces variations on one shape, and its own difficulty lever (per NEXT_TASKS task 9) is pit count and set-piece density. A challenge room is a place to put a *pure* mechanic at full intensity without breaking the main course's difficulty curve, and it is the natural container for the dense collectible tier. It is also the reward destination the pipe sub-room path has been missing since Phase 5 — the sub-room code exists and generation never uses it.

**Sketch:** Build the room as a `Segment` in `server/gen` written into a pocket above or below the corridor, entered by a `WarpPipeBlock` or a new door block; `SegmentLibrary` already has `SECRET`-tagged builders (`hidden_cache`, `secret_prize_vault`, `p_switch_bonus_room`, `illusory_pipe_cache`) that establish the placement pattern. The room's rule and timer belong in a small `ChallengeRoomService` reusing `CourseTimerService`'s countdown and `CourseScoringService`'s award hooks. Ejection reuses `CheckpointService`'s respawn placement. Room templates are the obvious first real caller for `CourseStructureService.placeTemplate`.

**Touches:** `SegmentLibrary.java`, `CourseStructureService.java`, `CourseTimerService.java`, `CheckpointService.java`, `WarpPipeBlock.java`

**Partly present already.** What is left: The genuinely new part is the room's *rules*, not the door: a sealed room with one win condition, a strict independent timer, exactly one reward, and — the important one — failure that ejects the player back to the course with no penalty. Today there is no per-room timer separate from the course clock (`server/CourseTimerService` runs the one course clock off `CourseDefinition.timeLimitSeconds`) and no non-punishing exit path; leaving a course goes through `ProgressionService.leaveCourse`. Build it as an extension of #62 rather than a new subsystem, and reuse the SECRET segment tag so the generator can already place the entrance.


## ui

### A hint economy — buy the location of what you are missing  *(value 6, small)*

**From:** Super Mario Odyssey (Hint Toad / Talkatoo)

An NPC who, for a price in coins, marks the approximate location of one uncollected item in the current course on the HUD or map — and a second NPC who simply names, for free, what is still missing without saying where. The paid hint is a coin sink that scales with frustration; the free naming turns "I don't know what I haven't found" into a checklist, which is a different and cheaper problem to solve.

**Why here:** `CourseProgress` already knows exactly which star coins a player is missing per course, and there is no way for the player to see that in-course — the counter is visible on the map and results screen only, so a player replaying a course to find their third star coin has no information at all. That is the worst kind of hidden collectible: findable only by exhaustive search of a procedurally generated level. A hint system makes hidden collectibles fair specifically *because* the courses are generated. It also gives coins a second sink beyond 1-Ups and reuses the Toad NPC and dialogue service already built.

**Sketch:** `ToadDialogueService` already exists and `ToadEntity` is already placed; add a hint branch to it that reads `ProgressionService.get(player).record(courseId)` for missing star coins and asks `CourseWriter`/the generator's placement record where they were written — this may need the generator to persist star-coin positions per seed, which is cheap given `CourseLayoutPlan` is deterministic from world seed plus course id. Charge through the same `CourseStateAccess.update(s -> s.withCoins(...))` path `ToadShopService.purchase` uses, behind the existing `PayloadRateLimiter`. Display via `CourseHud` or a marker on `CourseMapScreen`.

**Touches:** `ToadDialogueService.java`, `ProgressionService.java`, `CourseWriter.java`, `CourseHud.java`, `ToadShopService.java`


## Rejected as a bad fit

Proposed by the research pass, thrown out by the verifier as fighting the engine, duplicating an
installed mod, or breaking the 2.5D rail. Recorded so nobody re-proposes them.

- **Planetoid gravity wells for the 3D courses** — The mod has no mixins at all — `src/main/resources/META-INF/` contains only `neoforge.mods.toml`, and `build.gradle` has no mixin config. Every movement feature is built from server-side `setDeltaMovement` + `hurtMarked` (e.g. server/AirMoveService.java:189-192) and client camera events (`client/camera/CameraDirector.java:44-73`, which uses `ViewportEvent.ComputeCameraAngles` and explicitly forces `event.setRoll(0.0F)` at :66 and :72). Reorienting "down" toward a point requires rewriting `Entity.move`, collision resolution, step-up, `onGround` and the AABB — all Y-up hardcoded — which is a mixin-scale rewrite of the exact subsystem `CourseReachability` depends on. The nearest thing the docs even contemplate is docs/MISSING_MECHANICS.md:49 (#15, "variable gravity per course" — a scalar, not a direction).
- **Gravity-flip volumes for the 2.5D rail (ceiling walking)** — Same engine wall as the planetoid, for the same reason: no mixins (`src/main/resources/META-INF/` holds only neoforge.mods.toml), and MC resolves ground contact downward only — a player under a ceiling never reports `onGround`, so every system that gates on it breaks at once, including the ground pound (server/AirMoveService.java:187,193), the spin jump (:102,110), the clamber cooldown (:77) and the wall-jump grace. `client/camera/CameraDirector.java:66,72` pins roll to 0 in both the transition and steady-state 2.5D paths, so the flip has nowhere to render either.
