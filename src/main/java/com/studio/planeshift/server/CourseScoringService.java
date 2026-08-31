package com.studio.planeshift.server;

import com.studio.planeshift.common.course.CourseState;
import com.studio.planeshift.common.network.ScorePopupPayload;
import com.studio.planeshift.common.registry.ModSounds;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Course scoring: the running score, the stomp combo ladder, and end-of-course results.
 *
 * <p>Score lives in {@link CourseState} rather than here so it survives a death, persists with
 * the player and reaches the client HUD. This class owns only the transient bookkeeping that
 * must not persist: when the current combo started and how deep it is.
 */
public final class CourseScoringService {

    /** Points per coin, matching the arcade convention the rest of the scoring follows. */
    public static final int COIN_SCORE = 100;
    /** Points per star coin. */
    public static final int STAR_COIN_SCORE = 1000;
    /** Points per surviving pip at the finish. */
    public static final int PIP_BONUS = 500;
    /** Points per remaining second on the clock at the finish. */
    public static final int TIME_BONUS_PER_SECOND = 50;

    /**
     * The classic stomp ladder. Each consecutive enemy defeated without touching the ground is
     * worth the next rung; past the end of the ladder the chain awards a 1-Up instead of points.
     */
    private static final int[] STOMP_LADDER = {100, 200, 400, 800, 1000, 2000, 4000, 8000};

    /** A chain only survives this long without another stomp, so it cannot span a whole course. */
    private static final long CHAIN_TIMEOUT_TICKS = 60L;

    private static final Map<UUID, Long> COURSE_START = new HashMap<>();
    private static final Map<UUID, Chain> CHAINS = new HashMap<>();

    /** Live combo bookkeeping. Deliberately not persisted. */
    private static final class Chain {
        int depth;
        long lastStompTick;
    }

    private CourseScoringService() {
    }

    public static void startCourse(ServerPlayer player) {
        COURSE_START.put(player.getUUID(), player.level().getGameTime());
        CHAINS.remove(player.getUUID());
        // A fresh run starts from zero; score is not carried between courses.
        CourseStateAccess.update(player, s -> s.withScore(0));
    }

    /** Adds points, clamped by {@link CourseState}. Returns the new total. */
    public static int addScore(ServerPlayer player, int amount) {
        if (amount == 0) {
            return CourseStateAccess.get(player).score();
        }
        return CourseStateAccess.update(player, s -> s.withScore(s.score() + amount)).score();
    }

    /** Awards coin points. The 1-Up at 100 coins is handled by the pickup itself. */
    public static void awardCoin(ServerPlayer player) {
        addScore(player, COIN_SCORE);
    }

    public static void awardStarCoin(ServerPlayer player) {
        addScore(player, STAR_COIN_SCORE);
    }

    /**
     * Scores a defeated enemy and advances the airborne combo.
     *
     * <p>The chain deepens only while the player stays off the ground, which is what makes a
     * bounce-chain a deliberate act rather than an accident of walking through a crowd. Landing
     * ends it; so does {@link #CHAIN_TIMEOUT_TICKS} without another defeat, so a chain cannot be
     * parked mid-air and resumed much later.
     *
     * @return the points awarded, or 0 when the chain paid out a 1-Up instead
     */
    public static int awardStomp(ServerPlayer player) {
        long now = player.level().getGameTime();
        Chain chain = CHAINS.computeIfAbsent(player.getUUID(), id -> new Chain());

        if (now - chain.lastStompTick > CHAIN_TIMEOUT_TICKS) {
            chain.depth = 0;
        }
        chain.lastStompTick = now;

        int points = stompValue(chain.depth);
        chain.depth++;

        // Presentational only: the popup tells the player which rung just paid. A dropped packet
        // costs nothing, since the score itself rides along with the synced CourseState.
        sendPopup(player, points == ONE_UP_INSTEAD ? 0 : points);

        if (points == ONE_UP_INSTEAD) {
            // Past the top of the ladder every further link is an extra life, as in the original.
            CourseStateAccess.update(player, s -> s.withLives(s.lives() + 1));
            player.level().playSound(null, player.blockPosition(), ModSounds.ONE_UP.get(),
                    SoundSource.PLAYERS, 0.9F, 1.0F);
            return 0;
        }

        addScore(player, points);
        return points;
    }

    /**
     * Points for the {@code depth}-th consecutive defeat in one airborne chain, or
     * {@link #ONE_UP_INSTEAD} once the chain has climbed past the top of the ladder.
     *
     * <p>Package-private and pure so the ladder can be verified without a world or a player.
     */
    static int stompValue(int depth) {
        if (depth < 0) {
            throw new IllegalArgumentException("negative chain depth: " + depth);
        }
        return depth >= STOMP_LADDER.length ? ONE_UP_INSTEAD : STOMP_LADDER[depth];
    }

    /** Sentinel returned by {@link #stompValue} when the chain earns a life rather than points. */
    static final int ONE_UP_INSTEAD = -1;

    /** The ladder length, for tests and tuning. */
    static int ladderLength() {
        return STOMP_LADDER.length;
    }

    /** Tells the client to float a number above the action. */
    private static void sendPopup(ServerPlayer player, int amount) {
        PacketDistributor.sendToPlayer(player,
                new ScorePopupPayload(player.getX(), player.getY() + 1.6D, player.getZ(), amount));
    }

    /**
     * Ends any open combo. Called when the player lands, so the ladder only rewards a genuinely
     * airborne chain.
     */
    public static void endChain(ServerPlayer player) {
        Chain chain = CHAINS.get(player.getUUID());
        if (chain != null && chain.depth > 0) {
            chain.depth = 0;
        }
    }

    /** Current combo depth, for HUD or debug. Zero when no chain is running. */
    public static int chainDepth(ServerPlayer player) {
        Chain chain = CHAINS.get(player.getUUID());
        return chain == null ? 0 : chain.depth;
    }

    public static void finishCourse(ServerPlayer player) {
        Long start = COURSE_START.remove(player.getUUID());
        CHAINS.remove(player.getUUID());
        long ticks = start != null ? player.level().getGameTime() - start : 0L;
        CourseState state = CourseStateAccess.get(player);

        // Coins and stomps have already been scored as they happened; the finish adds the
        // survival and speed bonuses on top of that running total.
        int bonus = state.pips() * PIP_BONUS;
        if (state.timed()) {
            bonus += (state.timeLeft() / 20) * TIME_BONUS_PER_SECOND;
        }
        int finalScore = addScore(player, bonus);

        player.sendSystemMessage(Component.translatable("chat.planeshift.course_complete",
                state.coins(), state.starCoins(), formatTime(ticks), finalScore));
    }

    public static String formatTime(long ticks) {
        int totalSeconds = (int) (ticks / 20L);
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        int centi = (int) ((ticks % 20L) * 5L);
        return String.format("%d:%02d.%02d", minutes, seconds, centi);
    }

    public static void clear(UUID playerId) {
        COURSE_START.remove(playerId);
        CHAINS.remove(playerId);
    }
}
