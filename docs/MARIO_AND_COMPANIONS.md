# Mario Inspiration & Companion Mods

This is a wishlist of Mario-style mechanics, content, and companion mods that make PlaneShift feel like a polished 2.5D platformer. Items marked **implemented** are already in the mod; the rest are queued.

## Power-ups / Pickups

- [x] Coin — instant coin pickup, pops from blocks.
- [x] Star Power — 15s invulnerability + speed + jump boost (Mario super star).
- [x] 1-Up — restores pips, grants an extra life.
- [ ] Super Mushroom — grow larger / take an extra hit.
- [ ] Fire Flower — throws fireballs (replace/augment Ember Core).
- [ ] Ice Flower — freeze enemies / create ice platforms.
- [ ] Cape Feather / Propeller — glide or vertical lift.
- [ ] Mini Mushroom — tiny hitbox, floaty jumps, access small pipes.
- [ ] Mega Mushroom — smash through blocks/enemies for a short time.
- [ ] Yoshi Egg — ride a mount, flutter jump, eat enemies.
- [ ] POW Block — screen shake, defeats grounded enemies.
- [ ] Red Coin / Star Coin / Moon — collectibles for secrets.

## Blocks

- [x] Question Block — hit from below to spawn a pickup.
- [x] Brick Block — breakable by hitting from below or while powered up.
- [ ] Note Block / Music Block — bounce higher, plays a note.
- [ ] Spring (already Spring Pad) — launch arc.
- [ ] Donut Block — falls after standing on it briefly.
- [ ] ON/OFF Switch — toggles colored blocks.
- [ ] Snake Block — moving path block.
- [ ] Cloud Platform — temporary platform.
- [ ] Ice Block — slippery surface.
- [ ] Spike Trap / Thwomp — hazards.
- [ ] Moving platforms (lift) on rails.
- [ ] Warp Pipes — enter/exit pipes, optional sub-areas.
- [ ] Flag Pole — course end, slide down for score.
- [ ] Checkpoint Gate (already Checkpoint Beacon).

## Enemies

- [ ] Goomba — walks back and forth, stomp to defeat.
- [ ] Koopa — shell slide after stomp.
- [ ] Piranha Plant — pops out of pipes.
- [ ] Boo — chases when not looked at.
- [ ] Bullet Bill — straight-flying projectile.
- [ ] Cheep Cheep — water / flying sine wave enemy.
- [ ] Hammer Bro — lobs arcing projectiles.
- [ ] Lakitu — throws spinies from above.
- [ ] Thwomp — falls when player is below.
- [ ] Bob-omb — fuse, explosion.
- [ ] Chain Chomp — tethered lunging enemy.
- [ ] Monty Mole — burrows and chases.

## Mechanics

- [x] Coyote time and jump buffering.
- [x] Variable jump height (hold for higher).
- [ ] Triple jump / long jump / backflip.
- [ ] Ground pound (butt stomp).
- [ ] Wall jump and wall slide.
- [ ] Spin jump (break blocks underfoot, bounce off spiky enemies).
- [ ] Crouch-slide / slope slides.
- [ ] Climbable fences / vines.
- [ ] Auto-scroll sections.
- [ ] Time limit per course.
- [ ] Secret exits / multiple paths.
- [ ] Underwater courses with buoyancy.
- [ ] Ghost house / dark courses with limited visibility.

## Music

- Custom `SoundEvent`s for course music, boss music, hub music.
- Client-side `MusicManager` that randomizes tracks based on:
  - current mode (2.5D vs free 3D)
  - course area tag
  - combat / calm state
- Music is supplied by the player through a resource pack or external OGGs; do not ship copyrighted Mario music with the mod.

## Companion Mods (QoL / Performance / Visuals)

- **Sodium** — modern rendering, big FPS boost in courses.
- **Lithium** / **Canary** — server tick optimization.
- **FerriteCore** — memory usage reduction.
- **Starlight** — faster lighting engine.
- **Entity Culling** / **More Culling** — skip off-screen entities.
- **Sound Physics Remastered** — reverb in caves/pipes.
- **AmbientSounds** / **Presence Footsteps** — atmosphere.
- **Dynamic FPS** — lower render rate when unfocused.
- **AppleSkin** — food/hunger display.
- **Xaero's Minimap / JourneyMap** — optional navigation aid.
- **Controlling** — keybind search.
- **Mouse Tweaks** / **Item Scroller** — inventory QoL.
- **JEI** / **EMI** — recipe browser.
- **Customizable Player Models** — optional Mario skins/costumes.
- **BetterF3** — debug overlay tuning.
- **Wavey Capes** / **Capes** — visual flair.
