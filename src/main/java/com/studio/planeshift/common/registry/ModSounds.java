package com.studio.planeshift.common.registry;

import com.studio.planeshift.PlaneShift;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Sound events (Design Bible, "Music and audio").
 *
 * <p>The mod ships empty placeholders. For personal use, drop OGG files into
 * {@code assets/planeshift/sounds/...} and fill the corresponding entries in
 * {@code sounds.json}; for public distribution, replace with original tracks.
 */
public final class ModSounds {

    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(Registries.SOUND_EVENT, PlaneShift.MOD_ID);

    public static final DeferredHolder<SoundEvent, SoundEvent> MUSIC_COURSE_2_5D =
            register("music.course_2_5d");
    public static final DeferredHolder<SoundEvent, SoundEvent> MUSIC_COURSE_3D =
            register("music.course_3d");
    public static final DeferredHolder<SoundEvent, SoundEvent> MUSIC_HUB =
            register("music.hub");
    public static final DeferredHolder<SoundEvent, SoundEvent> MUSIC_COMBAT =
            register("music.combat");

    public static final DeferredHolder<SoundEvent, SoundEvent> COIN_PICKUP =
            register("sound.coin");
    public static final DeferredHolder<SoundEvent, SoundEvent> POWER_UP =
            register("sound.power_up");
    public static final DeferredHolder<SoundEvent, SoundEvent> BRICK_BREAK =
            register("sound.brick_break");
    public static final DeferredHolder<SoundEvent, SoundEvent> QUESTION_BUMP =
            register("sound.question_bump");
    public static final DeferredHolder<SoundEvent, SoundEvent> STOMP =
            register("sound.stomp");
    public static final DeferredHolder<SoundEvent, SoundEvent> ENEMY_DEFEAT =
            register("sound.enemy_defeat");
    public static final DeferredHolder<SoundEvent, SoundEvent> FIREBALL =
            register("sound.fireball");
    public static final DeferredHolder<SoundEvent, SoundEvent> ICESHOT =
            register("sound.iceshot");
    public static final DeferredHolder<SoundEvent, SoundEvent> HAMMER_THROW =
            register("sound.hammer_throw");
    public static final DeferredHolder<SoundEvent, SoundEvent> BOOMERANG_THROW =
            register("sound.boomerang_throw");
    public static final DeferredHolder<SoundEvent, SoundEvent> GAME_OVER =
            register("sound.game_over");
    public static final DeferredHolder<SoundEvent, SoundEvent> COURSE_CLEAR =
            register("sound.course_clear");
    public static final DeferredHolder<SoundEvent, SoundEvent> CHECKPOINT =
            register("sound.checkpoint");
    public static final DeferredHolder<SoundEvent, SoundEvent> DAMAGE =
            register("sound.damage");
    public static final DeferredHolder<SoundEvent, SoundEvent> ONE_UP =
            register("sound.1up");
    public static final DeferredHolder<SoundEvent, SoundEvent> SPRING =
            register("sound.spring");
    public static final DeferredHolder<SoundEvent, SoundEvent> WARP =
            register("sound.warp");
    public static final DeferredHolder<SoundEvent, SoundEvent> BOWSER_ROAR =
            register("sound.bowser_roar");

    private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(PlaneShift.id(name)));
    }

    private ModSounds() {
    }
}
