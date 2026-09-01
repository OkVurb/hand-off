# tools

Build-time asset generators. Nothing here is compiled into the mod — `build.gradle` only
sources `src/main/java`, so these are standalone programs run by hand when an asset needs
regenerating.

## SoundGen.java

Generates all 22 PlaneShift sound events as original chiptune-style waveforms.

Every waveform is synthesised from the oscillators in the file (pulse, NES-stepped triangle,
LFSR noise, saw) and every melody was written for this project. Nothing is sampled from, or
transcribed from, an existing recording. The four-voice NES palette is a synthesis technique,
not borrowed content.

### Regenerating

Requires a JDK 21 (already needed to build the mod) and `ffmpeg` with `libvorbis`.

```bash
java tools/SoundGen.java /tmp/psgen
```

Then encode each WAV to OGG Vorbis into the assets tree:

```bash
for f in /tmp/psgen/*.wav; do b=$(basename "$f" .wav); case "$b" in music_*) q=4;; *) q=3;; esac; ffmpeg -loglevel error -y -i "$f" -c:a libvorbis -q:a $q "src/main/resources/assets/planeshift/sounds/$b.ogg"; done
```

Verify with:

```bash
./gradlew checkSoundAssets
```

### Channel layout matters

SFX are rendered **mono**. Minecraft only attenuates and positions mono sources — a stereo
OGG plays at full volume everywhere in the world, which is why every positional cue here is
single-channel.

The four music tracks are **stereo**. They play through `SimpleSoundInstance.forMusic`, which
is non-positional, so the extra channel is free.

### Adding a sound

1. Add a `register("sound.thing")` line to `ModSounds`.
2. Add a `Buf thing()` builder to `SoundGen` and a `write(...)` call in `main`.
3. Add the `sounds.json` entry plus its `subtitles.planeshift.thing` key in `en_us.json`.
4. Regenerate, encode, and run `./gradlew checkSoundAssets` — it fails on any event that has
   no playable file behind it, and on any subtitle key missing from the language file.

## TextureGen.java

Generates all 89 placeholder (greybox) textures.

```bash
java tools/TextureGen.java src/main/resources/assets/planeshift/textures
./gradlew checkTextureAssets
```

Each texture is a flat fill with a darker border and the entry's initials in a 4x5 pixel font.
Two properties matter and are enforced:

- **Distinct colours.** Hues are spread by the golden angle across a shared index, so no two
  entries can land on the same colour. The previous hand-made set had 24 of 73 files
  byte-identical, including `hammer_bro`/`koopa` and `brick_block`/`secret_passage`.
- **Distinct labels.** Plain initials collide (`checkpoint_beacon`, `coin_block` and
  `conveyor_belt` all give "CB"), so colliding names are advanced through more specific
  strategies until the category is unambiguous.

Sizes are what the game expects: 16x16 blocks/items/particles, 64x32 entity skins, 18x18 mob
effect icons. Mob effect icons take the colour the effect declares in `ModEffects` so the HUD
icon matches the aura tint.

These are placeholders, not art — replace them freely.

## MewRigSkinImporter.java

Converts the retained Mew 4x4 character concept atlas into the 64x32 UV layout used by
`AnimatedCourseEnemyModel`:

```bash
java tools/MewRigSkinImporter.java \
  tools/art_sources/mew_character_atlas.png \
  src/main/resources/assets/planeshift/textures/entity
./gradlew checkTextureAssets
```

It crops each original subject, derives its palette, paints every cuboid face, and places the
concept on the front faces with nearest-neighbour sampling. See `docs/ENEMY_ART_DIRECTION.md` for
the exact model and animation contract.

## EnemyTextureGen.java

Builds the production 128x128 skins for all eleven hostile enemy meshes from the retained,
original material atlas:

```powershell
java tools/EnemyTextureGen.java `
  tools/art_sources/enemy_material_atlas.png `
  src/main/resources/assets/planeshift/textures/entity
```

The source atlas supplies material language only. The Java generator enforces exact UV regions,
nearest-neighbour pixel density, gameplay-safe brightness, and lateral eyes/mouths for the fixed
side camera. This avoids the common failure where an attractive AI texture has no usable model UV
layout or only looks correct when the mob faces the camera.

## CoursePropTextureImporter.java

Crops the retained transparent 4x4 prop atlas into the remaining visible course-block sprites:

```powershell
java tools/CoursePropTextureImporter.java `
  tools/art_sources/course_prop_atlas.png `
  src/main/resources/assets/planeshift/textures/block
```

The result replaces the old coloured squares/initials for axes, beacons, rings, vines, switches,
spikes and the loop trigger. `TextureGen` explicitly protects these production files; transparent
sprites may compress below its historical 400-byte placeholder threshold.
