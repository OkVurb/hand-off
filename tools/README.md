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
