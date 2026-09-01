package com.studio.planeshift.client.music;

import com.studio.planeshift.client.ClientCourseState;
import com.studio.planeshift.common.PlaneShiftConfig;
import com.studio.planeshift.common.course.CourseState;
import com.studio.planeshift.common.entity.BowserEntity;
import com.studio.planeshift.common.registry.ModEffects;
import com.studio.planeshift.common.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;

/**
 * Client-side adaptive music (Design Bible, "Music and audio").
 *
 * <p>Tracks are chosen from the current {@link CourseState}. The OGGs ship with the mod (see
 * {@code tools/README.md}); a resource pack can override any of them.
 *
 * <p>Moods are ordered by urgency rather than by where the player is standing. Star Power
 * outranks everything, including the boss: while it is running, nothing else about the player's
 * situation matters as much as the fact that it is about to run out.
 */
public final class CourseMusicManager {

    private enum Mood {
        HUB, COURSE_2_5D, COURSE_3D, COMBAT, BOSS, STAR_POWER
    }

    /**
     * How much faster the course track plays once the clock is critical.
     *
     * <p>A pitch shift rather than a second recording, which is how the original did it: the
     * player recognises the track they have been listening to, sped up, and understands
     * immediately that it is the same music under pressure rather than a new area.
     */
    private static final float HURRY_PITCH = 1.28F;

    /** How close Bowser has to be for the arena music to take over. */
    private static final double BOSS_RANGE = 42.0D;
    /** Bowser is only searched for this often; a per-tick entity sweep is not worth it. */
    private static final int BOSS_SCAN_INTERVAL = 20;

    private static SoundInstance current = null;
    private static Mood lastMood = null;
    private static boolean lastHurry = false;
    /** Ticks since the last track start; throttles restart attempts to avoid churn. */
    private static int ticksSinceLastStart = 0;
    private static int ticksSinceBossScan = BOSS_SCAN_INTERVAL;
    private static boolean bossNearby = false;

    private CourseMusicManager() {
    }

    /** Call from {@link com.studio.planeshift.client.ClientEvents#onClientTickPost}. */
    public static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.player == null) {
            stop();
            reset();
            return;
        }

        CourseState state = ClientCourseState.get();
        Mood mood = chooseMood(minecraft, state);
        // Only the ordinary course tracks speed up. Speeding up the boss or Star Power loops
        // would double up two different "hurry" signals and read as a glitch.
        boolean hurry = state.timeCritical()
                && PlaneShiftConfig.CLIENT.hurryUpMusic.get()
                && (mood == Mood.COURSE_2_5D || mood == Mood.COURSE_3D || mood == Mood.COMBAT);
        ticksSinceLastStart++;

        boolean needStart = current == null
                || mood != lastMood
                || hurry != lastHurry
                || !minecraft.getSoundManager().isActive(current) && ticksSinceLastStart > 100;
        if (needStart) {
            play(mood, hurry, minecraft);
            lastMood = mood;
            lastHurry = hurry;
        }
    }

    private static Mood chooseMood(Minecraft minecraft, CourseState state) {
        if (minecraft.player.hasEffect(ModEffects.STAR_POWER)) {
            return Mood.STAR_POWER;
        }
        if (PlaneShiftConfig.CLIENT.bossMusic.get() && state.inCourse() && bossNearby(minecraft)) {
            return Mood.BOSS;
        }
        if (state.in2_5D()) {
            return Mood.COURSE_2_5D;
        }
        if (state.inCourse()) {
            return Mood.COURSE_3D;
        }
        return Mood.HUB;
    }

    /**
     * Whether Bowser is close enough to own the soundtrack.
     *
     * <p>Cached and rescanned once a second. An entity sweep every frame to decide which music to
     * play would be one of the more expensive things the client does, for information that cannot
     * meaningfully change in fewer than twenty ticks.
     */
    private static boolean bossNearby(Minecraft minecraft) {
        if (++ticksSinceBossScan < BOSS_SCAN_INTERVAL) {
            return bossNearby;
        }
        ticksSinceBossScan = 0;
        if (minecraft.level == null) {
            bossNearby = false;
            return false;
        }
        AABB range = minecraft.player.getBoundingBox().inflate(BOSS_RANGE);
        bossNearby = !minecraft.level
                .getEntities((Entity) null, range, e -> e instanceof BowserEntity && e.isAlive())
                .isEmpty();
        return bossNearby;
    }

    private static void play(Mood mood, boolean hurry, Minecraft minecraft) {
        stop(minecraft);
        ticksSinceLastStart = 0;
        SoundEvent event = switch (mood) {
            case HUB -> ModSounds.MUSIC_HUB.get();
            case COURSE_2_5D -> ModSounds.MUSIC_COURSE_2_5D.get();
            case COURSE_3D -> ModSounds.MUSIC_COURSE_3D.get();
            case COMBAT -> ModSounds.MUSIC_COMBAT.get();
            case BOSS -> ModSounds.MUSIC_BOSS.get();
            case STAR_POWER -> ModSounds.MUSIC_STAR_POWER.get();
        };
        current = hurry ? pitchedMusic(event, HURRY_PITCH) : SimpleSoundInstance.forMusic(event);
        minecraft.getSoundManager().play(current);
    }

    /**
     * A non-positional looping music instance at a chosen pitch.
     *
     * <p>{@link SimpleSoundInstance#forMusic} hard-codes pitch 1, so the hurry-up variant has to
     * build the instance itself. The arguments match {@code forMusic} exactly except for pitch:
     * music source, no attenuation, non-looping, positioned relative to the listener.
     */
    private static SoundInstance pitchedMusic(SoundEvent event, float pitch) {
        return new SimpleSoundInstance(
                event.location(),
                SoundSource.MUSIC,
                1.0F,
                pitch,
                SoundInstance.createUnseededRandom(),
                false,
                0,
                SoundInstance.Attenuation.NONE,
                0.0D, 0.0D, 0.0D,
                true);
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

    /** Forgets the cached mood so the next tick starts a track rather than assuming one is playing. */
    private static void reset() {
        lastMood = null;
        lastHurry = false;
        bossNearby = false;
        ticksSinceBossScan = BOSS_SCAN_INTERVAL;
    }
}
