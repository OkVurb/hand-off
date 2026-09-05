package com.studio.planeshift.common.entity;

/**
 * The ways an enemy can be defeated.
 *
 * <p>This exists because the roster only ever stated the negative. Six enemies overrode
 * {@code isStompable()} to return {@code false} and nothing anywhere said what <em>did</em> kill
 * them — so "what is the answer to a Spiny?" was a question you could only settle by reading every
 * damage path in the mod and hoping you had found them all. That is exactly backwards for a
 * platformer, where the entire design of an enemy is the answer it demands.
 *
 * <p>Stating it positively also makes a real invariant testable: every enemy that is meant to be
 * <em>fought</em> must answer to at least one vector the player always has. An enemy whose only
 * answer is a power-up they might not be carrying is not an enemy, it is a wall — and the
 * generator will happily place a wall in the middle of a corridor.
 */
public enum DefeatVector {

    /** Landing on it. The genre's default, and the one most enemies must answer to. */
    STOMP,

    /** The spin attack, on the ground or as a safe bounce. Always available. */
    SPIN,

    /** A ground pound, direct or through the shockwave. Always available. */
    GROUND_POUND,

    /** A kicked Koopa shell running through it. */
    SHELL,

    /** Fire — the mod's fireball, or any fire damage type. */
    FIRE,

    /** Ice, which freezes rather than burns. */
    ICE,

    /** Star power: beats everything that can be beaten at all. */
    STAR;

    /**
     * The vectors a player has without holding any power-up.
     *
     * <p>The test that matters is written against this set. A Buzzy Beetle immune to fire is good
     * design; a Buzzy Beetle immune to everything the player is guaranteed to have is a bug that
     * only shows up when someone reaches it without a flower.
     */
    public static final java.util.Set<DefeatVector> ALWAYS_AVAILABLE =
            java.util.EnumSet.of(STOMP, SPIN, GROUND_POUND);
}
