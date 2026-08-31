package com.studio.planeshift.client.music;

import com.studio.planeshift.client.ClientCourseState;
import com.studio.planeshift.common.course.CourseState;
import com.studio.planeshift.common.mode.PlaneMode;
import com.studio.planeshift.common.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;

/**
 * Client-side adaptive music (Design Bible, "Music and audio").
 *
 * <p>Tracks are chosen from the current {@link CourseState}. The four looping OGGs ship
 * with the mod (see {@code tools/README.md}); a resource pack can override any of them.
 */
public final class CourseMusicManager {

    private enum Mood {
        HUB, COURSE_2_5D, COURSE_3D, COMBAT
    }

    private static SoundInstance current = null;
    private static Mood lastMood = null;
    /** Ticks since the last track start; throttles restart attempts to avoid churn. */
    private static int ticksSinceLastStart = 0;

    private CourseMusicManager() {
    }

    /** Call from {@link com.studio.planeshift.client.ClientEvents#onClientTickPost}. */
    public static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.player == null) {
            stop();
            return;
        }

        Mood mood = chooseMood(ClientCourseState.get());
        ticksSinceLastStart++;

        boolean needStart = current == null
                || mood != lastMood
                || !minecraft.getSoundManager().isActive(current) && ticksSinceLastStart > 100;
        if (needStart) {
            play(mood, minecraft);
            lastMood = mood;
        }
    }

    private static Mood chooseMood(CourseState state) {
        // TODO: detect combat from nearby CourseEnemyEntity instances.
        if (state.in2_5D()) {
            return Mood.COURSE_2_5D;
        }
        if (state.mode() == PlaneMode.FREE_3D && state.inCourse()) {
            return Mood.COURSE_3D;
        }
        if (state.inCourse()) {
            return Mood.COURSE_3D;
        }
        return Mood.HUB;
    }

    private static void play(Mood mood, Minecraft minecraft) {
        stop(minecraft);
        ticksSinceLastStart = 0;
        SoundEvent event = switch (mood) {
            case HUB -> ModSounds.MUSIC_HUB.get();
            case COURSE_2_5D -> ModSounds.MUSIC_COURSE_2_5D.get();
            case COURSE_3D -> ModSounds.MUSIC_COURSE_3D.get();
            case COMBAT -> ModSounds.MUSIC_COMBAT.get();
        };
        current = SimpleSoundInstance.forMusic(event);
        minecraft.getSoundManager().play(current);
    }

    public static void stop() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft != null) {
            stop(minecraft);
        }
    }

    private static void stop(Minecraft minecraft) {
        if (current != null) {
            minecraft.getSoundManager().stop(current);
            current = null;
        }
    }
}
