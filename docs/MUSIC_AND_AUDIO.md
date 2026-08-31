# Music & Audio

## Important: do not ship copyrighted music

The mod should **never** include Mario music or any other copyrighted tracks. Instead, it defines empty `SoundEvent`s that the player can fill with their own OGG files through a resource pack. You can supply your own fan-made remixes, original tracks, or silence.

## Sound Events

Add under `assets/planeshift/sounds.json`:

- `planeshift:music.course_2_5d` — side-on courses.
- `planeshift:music.course_3d` — free-camera courses.
- `planeshift:music.hub` — world map / hub.
- `planeshift:music.combat` — enemy encounter.
- `planeshift:music.course_complete` — fanfare on flagpole.
- `planeshift:sound.coin` — coin pickup.
- `planeshift:sound.power_up` — star/1-up pickup.
- `planeshift:sound.brick_break` — brick destroy.
- `planeshift:sound.question_bump` — ? block hit.

## Adaptive Music Manager

A client-side `CourseMusicManager` handles the playlist:

- Tracks current `CourseState` and `PlaneMode`.
- Maintains a queue of OGG tracks per mood.
- Randomizes the next track when the current one ends.
- Crossfades between moods (2.5D, 3D, hub, combat).
- Respects the "Music" volume slider.

## Implementation Sketch

```java
@SubscribeEvent
public static void onClientTickPost(ClientTickEvent.Post event) {
    CourseMusicManager.tick(Minecraft.getInstance().player);
}
```

`CourseMusicManager`:

1. Reads `ClientCourseState.local()` for `mode` and `state`.
2. Picks a `SoundEvent` category.
3. If `Minecraft.getInstance().getMusicManager().isPlayingMusic()` and the active track belongs to the wrong category, fade it out.
4. Starts a new `SimpleSoundInstance.forMusic(event)` from the chosen category when the vanilla music system is idle.

## Resource Pack Format

Player drops OGG files into a resource pack:

```
assets/planeshift/sounds/music/course_2_5d_01.ogg
assets/planeshift/sounds/music/course_2_5d_02.ogg
assets/planeshift/sounds/music/hub_01.ogg
```

`sounds.json` example:

```json
{
  "music.course_2_5d": {
    "category": "music",
    "sounds": [
      "planeshift:music/course_2_5d_01",
      "planeshift:music/course_2_5d_02"
    ]
  }
}
```

The manager can use `SimpleSoundInstance.forMusic(...)` and let vanilla randomize within a `SoundEvent`, or pick manually from a list and play each as a separate instance.

## Randomization

- Shuffle a per-category list.
- Pick the next track when the previous finishes (`isActive()` returns false).
- Avoid repeating the same track twice in a row.

## Mario Vibe Without Infringement

Use original chiptune-style tracks, or public-domain/classical pieces, or silence. The mod only provides the plumbing; the audio is the player's choice.
