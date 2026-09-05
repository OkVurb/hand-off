package com.studio.planeshift.server;

/**
 * The P-meter: fill while running, hold briefly when you stop, drain, and a top state.
 *
 * <p>What was here before was a tick counter. Sprint for thirty ticks and a boost switched on;
 * stop sprinting for one tick and it switched off. That is not a meter — it is a boolean with a
 * delay, and it gave the player nothing to read and nothing to protect. The whole point of the
 * gauge in the games it comes from is the <em>hold</em>: momentum survives a jump, a turn, a
 * moment in the air, so the skill is in stringing movement together rather than in holding one key.
 *
 * <p>Pure arithmetic with no Minecraft types, for the same reason {@code GroundPoundResolver} is.
 * Fill and drain rates are exactly the sort of thing that is wrong by a factor of two for weeks
 * without anyone being able to point at it, and a unit test can say precisely how long the meter
 * takes to fill.
 */
public final class PMeter {

    /**
     * How many display steps the meter has.
     *
     * <p>Eight, not a float. The client only ever draws it as a row of segments, and quantising
     * server-side means the sync packet is sent when a <em>segment</em> changes rather than on
     * every tick of a continuous value — roughly one packet per twelve ticks of running instead of
     * twenty per second.
     */
    public static final int STEPS = 8;

    /** Ticks of running to go from empty to full. Just over a second and a half. */
    public static final int FILL_TICKS = 34;

    /** Ticks the meter holds its value after the player stops driving it. */
    public static final int HOLD_TICKS = 12;

    /**
     * Ticks to fall from full to empty once the hold expires.
     *
     * <p>Equal to {@link #FILL_TICKS}, i.e. the meter drains at exactly the rate it fills. This
     * started out as an independent, shorter number with a comment claiming the drain was "slower
     * than the fill" — which was wrong twice over. A smaller value makes the drain *faster*, and
     * the implementation could not honour it anyway: draining {@code FILL_TICKS / DRAIN_TICKS} per
     * tick is integer division, so 34/26 was 1 and the meter took 34 ticks to empty no matter what
     * this said. A unit test caught it immediately.
     *
     * <p>An asymmetric drain needs a sub-tick accumulator, which is real state to carry and sync
     * for a cosmetic gauge. It is not needed: the forgiveness in this mechanic is {@link
     * #HOLD_TICKS}, which protects the whole meter through a jump or a turn. The drain is what
     * happens after you have genuinely stopped, and there is no reason for that to be gentle.
     */
    public static final int DRAIN_TICKS = FILL_TICKS;

    private PMeter() {
    }

    /**
     * One tick of the meter.
     *
     * @param charge  current charge, 0..FILL_TICKS
     * @param hold    hold ticks remaining
     * @param driving whether the player is doing something that fills the meter this tick
     * @return the packed next state; read it with {@link #charge} and {@link #hold}
     */
    public static long advance(int charge, int hold, boolean driving) {
        if (driving) {
            return pack(Math.min(FILL_TICKS, charge + 1), HOLD_TICKS);
        }
        if (hold > 0) {
            // Holding: the value is protected, so a jump or a turn does not cost the run.
            return pack(charge, hold - 1);
        }
        // Draining, one charge per tick, so full-to-empty is exactly DRAIN_TICKS.
        return pack(Math.max(0, charge - 1), 0);
    }

    /** Charge component of a packed state. */
    public static int charge(long packed) {
        return (int) (packed >> 32);
    }

    /** Hold component of a packed state. */
    public static int hold(long packed) {
        return (int) (packed & 0xFFFFFFFFL);
    }

    private static long pack(int charge, int hold) {
        return ((long) charge << 32) | (hold & 0xFFFFFFFFL);
    }

    /** The charge as a 0..STEPS display value — what the client is told and what it draws. */
    public static int step(int charge) {
        if (charge <= 0) {
            return 0;
        }
        return Math.min(STEPS, (charge * STEPS + FILL_TICKS - 1) / FILL_TICKS);
    }

    /** Whether the meter is topped out, which is what actually grants the speed bonus. */
    public static boolean atFull(int charge) {
        return charge >= FILL_TICKS;
    }
}
