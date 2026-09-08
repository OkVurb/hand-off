package com.studio.planeshift.server;

import com.studio.planeshift.common.course.CourseState;
import com.studio.planeshift.common.course.WorldDefinition;
import com.studio.planeshift.common.network.LetterboxPayload;
import com.studio.planeshift.common.network.TitleCardPayload;
import com.studio.planeshift.common.registry.ModParticles;
import com.studio.planeshift.common.registry.ModSounds;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Scripted sequences: a list of beats on a clock, run against one player.
 *
 * <h2>Why a service and not four calls in a row</h2>
 *
 * <p>The castle clear used to fire everything at once — the card, the fanfare and Toad all in the
 * same tick — and that is not a scene, it is a notification with three parts. A scene is beats
 * separated by silence, and the silence is doing most of the work: the pause after the boss falls
 * is what makes the card land, and the pause after the card is what makes Toad's first line read as
 * somebody speaking rather than as a fourth line of UI.
 *
 * <p>So this is a timeline. Beats carry a tick offset and a lambda, the service ticks them off, and
 * writing a new scene is writing a list rather than threading delays through the code that
 * triggered it.
 *
 * <h2>This is the one thing allowed to take the controls</h2>
 *
 * <p>The title card, the iris and the credits all deliberately leave the player playing — that is
 * argued for at each of them. A cutscene is the exception, and it has to be visibly one or the
 * player cannot tell a flourish from a scene: the letterbox goes down, and horizontal input is
 * held for exactly as long as the bars are. Jumping is left alone, because taking a player's jump
 * mid-air drops them, and a scene that kills you is worse than no scene.
 */
public final class CutsceneService {

    /** One moment in a scene: how long after it starts, and what happens. */
    private record Beat(int atTick, Consumer<ServerPlayer> action) {
    }

    /** A scene in progress for one player. */
    private static final class Playing {
        private final List<Beat> beats;
        private int tick;
        private int next;

        private Playing(List<Beat> beats) {
            this.beats = beats;
        }
    }

    private static final Map<UUID, Playing> RUNNING = new HashMap<>();

    private CutsceneService() {
    }

    /** Whether this player is mid-scene, so movement and other services can stand back. */
    public static boolean playing(ServerPlayer player) {
        return RUNNING.containsKey(player.getUUID());
    }

    /**
     * The castle clear.
     *
     * <p>Beats rather than a blur: the room goes quiet, the boss goes down, a moment passes, the
     * card names what happened, another moment passes, and then somebody thanks you. Six seconds
     * end to end, which is long enough to be an event and short enough that a player clearing their
     * fifth castle is not waiting through it.
     */
    public static void castleCleared(ServerPlayer player, WorldDefinition world, boolean lastWorld) {
        List<Beat> beats = new ArrayList<>();

        // Bars down first. Everything after this is inside the scene.
        beats.add(new Beat(0, p -> letterbox(p, 130)));
        // The room reacting: dust off the ceiling where the castle is coming apart.
        beats.add(new Beat(6, p -> {
            if (p.level() instanceof ServerLevel level) {
                level.sendParticles(ModParticles.PICKUP_GLOW.get(),
                        p.getX(), p.getY() + 3.0D, p.getZ(), 24, 2.5D, 1.0D, 0.4D, 0.02D);
            }
            p.level().playSound(null, p.blockPosition(), ModSounds.BRICK_BREAK.get(),
                    SoundSource.BLOCKS, 1.0F, 0.6F);
        }));
        // A beat of nothing. This is the one that makes the card land, and it is the reason the
        // whole thing is a timeline instead of four statements.
        beats.add(new Beat(40, p -> p.level().playSound(null, p.blockPosition(),
                ModSounds.COURSE_CLEAR.get(), SoundSource.PLAYERS, 1.0F, 1.0F)));
        beats.add(new Beat(58, p -> PacketDistributor.sendToPlayer(p,
                new TitleCardPayload(world.displayName(),
                        lastWorld ? "The castle falls" : "Castle cleared"))));
        // Toad speaks only once the card has had the screen to itself.
        beats.add(new Beat(104, p -> ToadDialogueService.begin(p, lastWorld)));
        // Bars up on their own clock, slightly before the last beat, so the scene hands the game
        // back while Toad is still talking rather than ending on an empty screen.
        beats.add(new Beat(126, p -> letterbox(p, 0)));

        RUNNING.put(player.getUUID(), new Playing(beats));
    }

    /** Advances every running scene. Called once per player tick. */
    public static void tick(ServerPlayer player, CourseState state) {
        Playing playing = RUNNING.get(player.getUUID());
        if (playing == null) {
            return;
        }
        // Held still, not frozen: vertical movement is left alone so a scene that starts while the
        // player is falling does not leave them hanging in the air.
        player.setDeltaMovement(0.0D, player.getDeltaMovement().y, 0.0D);
        player.hurtMarked = true;

        while (playing.next < playing.beats.size()
                && playing.beats.get(playing.next).atTick() <= playing.tick) {
            playing.beats.get(playing.next).action().accept(player);
            playing.next++;
        }
        playing.tick++;

        if (playing.next >= playing.beats.size()) {
            RUNNING.remove(player.getUUID());
        }
    }

    /** Drops a player's scene, for a disconnect or a course they walked out of. */
    public static void clear(ServerPlayer player) {
        if (RUNNING.remove(player.getUUID()) != null) {
            letterbox(player, 0);
        }
    }

    private static void letterbox(ServerPlayer player, int ticks) {
        PacketDistributor.sendToPlayer(player, new LetterboxPayload(ticks));
    }
}
