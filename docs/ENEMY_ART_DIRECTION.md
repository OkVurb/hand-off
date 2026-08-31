# PlaneShift enemy art and animation direction

PlaneShift keeps its familiar registry IDs for save/network compatibility, but the production
visuals are an original sky-island cast. The source turnarounds are under
`tools/art_sources/enemy_turnaround_*.png`; the runtime cuboid skins are generated from the Mew
concept atlas by `tools/MewRigSkinImporter.java`.

## Runtime cast

| Registry ID | Production design | 3D silhouette | Animation language |
| --- | --- | --- | --- |
| `goomba` | Amber Clay Sproutling | Pear-shaped clay body, leaf-ear crown, plum boots, no arms | Elastic idle bounce, running boot swing, stomp squash |
| `koopa` | Mossback Clockwork Gecko | Upright jade gecko, angular snout, brass winding backpack, tail | Counter-swing walk, head tracking, broad run stride |
| `thwomp` | Basalt Rune Crusher | Wide rectangular basalt body, floating slab fists, piston feet | Slow hover, fist lift, heavy periodic downward slam |
| `bullet_bill` | Brass-Fin Torpedo Moth | Horizontal charcoal body, copper face plate, four brass wings | Fast wing beat, gentle flight bob, swept-back speed pose |
| `boo` | Moon-Jelly Wisp | Translucent cuboid bell, cyan face, dangling fringe, no legs | Weightless vertical bob, curious turn, startled stretch |
| `lakitu` | Cloud-Manta Rider | Amber aviator seated on a wide pale-cyan manta | Manta fin flap, scarf/arm counter motion, banking turn |
| `hammer_bro` | Crescent Pangolin | Teal scale armor, gold brow guard, crescent throwing tools | Heavy biped walk, wind-up, overhead throw follow-through |
| `spiny` | Pincushion Crab | Low coral shell, six legs, cyan crystal quills | Side-step gait, defensive tuck, quick claw snap |
| `buzzy_beetle` | Navy Burrowing Beetle | Low cobalt armor slabs, six brass feet, cyan lens eyes | Rapid leg cycle, armored charge lean, short recoil |
| `piranha_plant` | Trumpet Vine | Segmented jade stalk, coral bell jaw, leaf arms, rooted base | Swaying idle, recoil anticipation, forward bite |
| `toad` | Lantern-Head Shopkeeper | Amber-and-cyan lantern head, teal coat, brass backpack | Welcoming wave, item presentation, buoyant idle |
| `bowser` | Volcanic Salamander Monarch | Massive basalt salamander, magma belly, stone horns, shell mantle | Grounded four-limb weight, arm spread, roaring ground impact |

## Shared model and UV contract

`AnimatedCourseEnemyModel` has independent head, body, two arms, two legs, and two wings. An
`EnemyRigProfile` toggles and scales those parts to preserve the silhouettes above. All living
course characters use a 64x32 skin:

- head UV origin: `(32, 0)`, 8x8x8;
- torso UV origin: `(0, 0)`, 10x9x6;
- arm UV origin: `(0, 16)`, 3x8x3;
- leg UV origin: `(12, 16)`, 4x6x4;
- wing UV origin: `(28, 16)`, 1x8x6.

The Java rig currently drives walk, look, idle bob, flight flap, sproutling squash, crusher slam,
wisp hover, plant sway, and boss weight shifts. Blockbench is the authoring tool for future
hand-keyed clips; runtime stays dependency-free until a stable NeoForge 1.21.11 animation adapter
is deliberately selected.
