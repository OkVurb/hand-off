# Art source provenance

These are source/reference sheets, not files loaded directly by Minecraft.

- `mew_block_atlas.png` — generated in Mew Design on 2026-08-31 from the requested original
  4x4 platformer block-atlas prompt. Cells are imported into 16x16 runtime textures with nearest
  neighbour scaling.
- `mew_character_atlas.png` — generated in Mew Design on 2026-08-31 after Mew rejected the first
  cast prompt as too close to recognizable franchise archetypes. The accepted prompt explicitly
  invented a sky-island cast with unique species, silhouettes, clothing and props.
- `enemy_turnaround_01.png` — generated with the built-in image generator using the Mew character
  atlas as a style reference. It defines front/right/back/action views for the sproutling, gecko,
  rune crusher and torpedo moth.
- `enemy_turnaround_02.png` — production views for the moon-jelly wisp, manta rider, crescent
  pangolin and pincushion crab.
- `enemy_turnaround_03.png` — production views for the burrowing beetle, trumpet vine,
  lantern-headed shopkeeper and volcanic salamander monarch.
- `enemy_material_atlas.png` — generated with OpenAI's built-in image generator on 2026-09-01
  as twelve original pixel-material swatches matching the turnaround cast. It is sampled by
  `tools/EnemyTextureGen.java`; the generator then pixelates, colour-grades and places those
  materials into deterministic 128x128 cuboid UV sheets. It contains no copied game art.
- `enemy_material_atlas.prompt.txt` — the exact built-in image-generator prompt used for that
  atlas, retained so the source can be audited or intentionally regenerated.
- `course_prop_atlas.png` — generated with the built-in image generator on 2026-09-01 as an
  original transparent 4x4 atlas for the remaining visible greybox course props. Imported by
  `tools/CoursePropTextureImporter.java`; no commercial-game art was used.
- `course_prop_atlas.prompt.txt` — exact generation prompt for the transparent prop atlas.

The course panorama was generated with the built-in image generator from the Mew block atlas's
pixel treatment and is shipped as `assets/planeshift/textures/environment/course_skybox.png`.

All prompts required original designs, no logos, no copied characters and no recognizable
franchise likenesses.
