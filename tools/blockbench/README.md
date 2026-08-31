# Blockbench workflow

Blockbench is the free/open-source authoring tool selected for PlaneShift's cuboid characters and
animation previews. On the current Windows development machine the signed portable 5.1.6 build is
installed at:

`C:\Users\cr0od\Apps\Blockbench\Blockbench.exe`

Use a Modded Entity or Generic Model project with a 64x32 texture and match the UV contract in
`docs/ENEMY_ART_DIRECTION.md`. Keep these clip names when keyframing:

- `idle` — 2 seconds, looping;
- `walk` — 1 second, looping;
- `attack` — 0.55 seconds, hold on last frame only when AI requests it;
- `hurt` — 0.25 seconds;
- `defeat` — 0.7 seconds;
- profile clips: `squash`, `slam`, `fly`, `bite`, and `roar`.

The current implementation reproduces these motions procedurally in Java so the mod has no
animation-library runtime dependency. Store future `.bbmodel` sources in this folder and export
model geometry into `com.studio.planeshift.client.render` only.
