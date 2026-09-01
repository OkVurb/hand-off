# Asset Licenses

## Textures

The production course-block textures, living-entity UV skins and course skybox were generated
specifically for PlaneShift on 2026-08-31. Source sheets and provenance are retained under
`tools/art_sources/`; no commercial-game files, logos or copied character art were supplied as
inputs. The prompts explicitly requested original sky-island designs with no recognizable
franchise likenesses.

- Mew Design produced `mew_block_atlas.png` and `mew_character_atlas.png`.
- OpenAI's built-in image generator produced the course skybox and three 3D turnaround sheets.
- OpenAI's built-in image generator produced `enemy_material_atlas.png`, an original 4x3 set of
  material swatches with no characters, logos or commercial-game imagery.
- OpenAI's built-in image generator produced `course_prop_atlas.png`, an original transparent
  4x4 prop sheet used for axes, checkpoints, coin rings, vines, switches, spikes and loop trigger.
- OpenAI's built-in image generator produced `projectile_material_atlas.png`, an original 3x2
  material sheet containing no characters, objects, logos or commercial-game imagery.
- `tools/MewRigSkinImporter.java` converts the character concepts into 64x32 cuboid UV skins.
- `tools/EnemyTextureGen.java` converts the material atlas into the final 128x128 UV sheets used
  by the eleven bespoke enemy meshes. It owns exact UV placement, side-camera faces and value
  grading; generated imagery is never trusted to supply UV coordinates directly.
- `tools/CoursePropTextureImporter.java` crops and scales the prop atlas into exact 16x16 runtime
  textures while preserving alpha for cutout block models.
- `tools/ProjectileTextureGen.java` converts its material atlas into exact 64x64 UV sheets for six
  independently baked voxel projectile props. `tools/ConveyorTextureGen.java` deterministically
  draws the conveyor surface, rollers and particle texture without external source art.
- `tools/TextureGen.java` owns the remaining temporary greybox textures. It refuses to overwrite
  any file larger than a placeholder, so running it over the complete texture directory is safe:
  production art is skipped and reported.

## Sounds

All OGG files in `assets/planeshift/sounds` are original works created for this project by
`tools/SoundGen.java`, which synthesises them from first principles — pulse, triangle, LFSR
noise and saw oscillators driven by envelopes and note tables written in that file.

- Nothing is sampled from, or transcribed from, an existing recording or composition.
- The melodies, fanfares and effect gestures are original to PlaneShift.
- The four-voice NES-style palette (two pulses, triangle bass, noise percussion) is a synthesis
  technique, not borrowed content.

They are reproducible: run `tools/SoundGen.java` and re-encode as described in `tools/README.md`,
and you get the same audio back. That generator is the authoritative source for these assets.

The mod deliberately does **not** ship, reference, or depend on audio from any commercial game.
If you replace these with other audio, you are responsible for the rights to what you add.
