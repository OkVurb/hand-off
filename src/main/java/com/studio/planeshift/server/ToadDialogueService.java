package com.studio.planeshift.server;

import com.studio.planeshift.common.registry.ModSounds;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;

/**
 * Toad's thank-you speech after a castle clear.
 *
 * <p>Lines arrive one at a time on a timer rather than all at once. A wall of four messages in a
 * single tick is a log entry; the same four spaced out is someone talking to you, and the pause
 * between them is what makes it read as a scene rather than a notification.
 *
 * <p>The queue is per player and weakly keyed, so a player who disconnects mid-speech does not
 * keep an entry alive, and a player who starts another course simply has the remainder cleared.
 */
public final class ToadDialogueService {

    /** Ticks between lines. Long enough to read one, short enough not to feel stalled. */
    private static final int LINE_INTERVAL_TICKS = 45;
    /** Ticks before the first line, so the fanfare has a moment on its own. */
    private static final int OPENING_DELAY_TICKS = 30;

    private static final List<String> CASTLE_LINES = List.of(
            "message.planeshift.toad.thanks_1",
            "message.planeshift.toad.thanks_2",
            "message.planeshift.toad.thanks_3");

    private static final List<String> FINAL_CASTLE_LINES = List.of(
            "message.planeshift.toad.final_1",
            "message.planeshift.toad.final_2",
            "message.planeshift.toad.final_3",
            "message.planeshift.toad.final_4");

    private static final Map<ServerPlayer, Speech> ACTIVE = new WeakHashMap<>();

    private static final class Speech {
        final Deque<String> remaining;
        int ticksUntilNext;

        Speech(List<String> lines) {
            this.remaining = new ArrayDeque<>(lines);
            this.ticksUntilNext = OPENING_DELAY_TICKS;
        }
    }

    private ToadDialogueService() {
    }

    /**
     * Starts the speech after a castle clear.
     *
     * @param finalCastle whether this was the last world's boss, which gets the longer send-off
     */
    public static void begin(ServerPlayer player, boolean finalCastle) {
        ACTIVE.put(player, new Speech(finalCastle ? FINAL_CASTLE_LINES : CASTLE_LINES));
        player.level().playSound(null, player.blockPosition(), ModSounds.TOAD_FANFARE.get(),
                SoundSource.PLAYERS, 0.9F, 1.0F);
    }

    /** Called once per player tick from {@code ServerEvents}. Cheap when nothing is speaking. */
    public static void tick(ServerPlayer player) {
        Speech speech = ACTIVE.get(player);
        if (speech == null) {
            return;
        }
        if (--speech.ticksUntilNext > 0) {
            return;
        }

        String key = speech.remaining.poll();
        if (key == null) {
            ACTIVE.remove(player);
            return;
        }
        player.sendSystemMessage(Component.translatable(key));
        speech.ticksUntilNext = LINE_INTERVAL_TICKS;
    }

    /** Cuts the speech short — a player who has moved on should not be talked at. */
    public static void clear(ServerPlayer player) {
        ACTIVE.remove(player);
    }
}
