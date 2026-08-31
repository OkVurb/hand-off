# Asset Licenses

## Textures

The production course-block textures, living-entity UV skins and course skybox were generated
specifically for PlaneShift on 2026-08-31. Source sheets and provenance are retained under
`tools/art_sources/`; no commercial-game files, logos or copied character art were supplied as
inputs. The prompts explicitly requested original sky-island designs with no recognizable
franchise likenesses.

- Mew Design produced `mew_block_atlas.png` and `mew_character_atlas.png`.
- OpenAI's built-in image generator produced the course skybox and three 3D turnaround sheets.
- `tools/MewRigSkinImporter.java` converts the character concepts into 64x32 cuboid UV skins.
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
