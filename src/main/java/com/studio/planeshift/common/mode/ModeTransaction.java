package com.studio.planeshift.common.mode;

import java.util.Optional;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * A server-authorized perspective shift (Design Bible, "Mode transition transaction").
 *
 * <p>"A shift is a four-step server-authorized transaction lasting roughly 0.6-0.9
 * seconds. It maps momentum, checks clearance, blends presentation, and either commits
 * or rolls back."
 *
 * <p>Lives only on the server, keyed by player. Clients receive the transaction id,
 * target mode, commit tick and corrected transform; presentation blending is theirs,
 * the collision-basis change is not. A course never saves mid-transaction: the owning
 * attachment only ever persists committed states.
 *
 * @param id          server-issued transaction id (monotonic per player)
 * @param from        committed mode the player is leaving
 * @param to          mode the player will be in at commit
 * @param rail        destination rail when {@code to} is {@link PlaneMode#SIDE_ON}
 * @param startPos    validated position at request time (rollback restore point)
 * @param commitPos   cleared destination position applied at the commit tick
 * @param requestTick server tick the transaction was opened
 * @param sourceLevel level the transaction was opened in; aborts if the player leaves it
 * @param commitTick  server tick the collision basis actually changes
 */
public record ModeTransaction(
        long id,
        PlaneMode from,
        PlaneMode to,
        Optional<PlaneRail> rail,
        Vec3 startPos,
        Vec3 commitPos,
        ResourceKey<Level> sourceLevel,
        long requestTick,
        long commitTick
) {
    /** Default presentation blend: 14 ticks = 0.7 s, inside the bible's 0.6-0.9 s window. */
    public static final int DEFAULT_DURATION_TICKS = 14;
    /** Watchdog: a transaction older than this is aborted and rolled back. */
    public static final int TIMEOUT_TICKS = 60;

    public boolean readyToCommit(long gameTime) {
        return gameTime >= commitTick;
    }

    public boolean timedOut(long gameTime) {
        return gameTime - requestTick > TIMEOUT_TICKS;
    }
}
