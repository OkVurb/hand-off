package com.studio.planeshift.common.entity;

/**
 * The eight mid-world bosses, as data.
 *
 * <p>Every castle in the game currently ends with the same fight. These are the smaller bosses that
 * belong halfway through a world rather than at the end of it — the tower to the castle's keep —
 * and the point of having eight is that a player should be able to tell which one they are fighting
 * from across the room, before it has done anything.
 *
 * <h2>One entity type, eight variants</h2>
 *
 * <p>Deliberately not eight entity types. {@link EnemyRigProfile} carries a scale that both the
 * renderer and {@code ModEntities} read, and its javadoc is emphatic about why: art drawn larger
 * than its hitbox produces stomps that land on the sprite and pass through, which reads as the game
 * ignoring an input. Eight registrations would mean eight chances to get that wrong.
 *
 * <p>So all eight share one body, one rig and one hitbox, and differ in palette and behaviour. The
 * honest cost: two of them are canonically off-model. One of these characters is the big heavy of
 * the group and another is the runt, and expressing that through scale is exactly the thing that
 * breaks the hitbox invariant. It is expressed through the fight instead — the heavy shakes the
 * floor, the small one never touches it — which is the half a player actually reads.
 *
 * <h2>Colour is the identifier, and it does not live here</h2>
 *
 * <p>At this camera distance a boss is a silhouette and two colours, so the palette matters as much
 * as the behaviour — but it is owned by {@code tools/EnemyTextureGen.py} and reaches the game as a
 * PNG. This enum deliberately does not carry a second copy of it.
 *
 * <p>That is not tidiness. Two rival copies of one quantity is exactly how {@code CameraProfile}'s
 * damping and the {@code cameraSmoothing} config option both ended up dead: neither was applied,
 * so neither could contradict the other, and the disagreement only surfaced when someone tried to
 * use one. {@code KoopalingPaletteTest} measures the shipped sheets instead, because the sheet is
 * what the player actually looks at.
 */
public enum Koopaling {

    /** The first tower. Plain straight shot, so the pattern is learned before it is decorated. */
    LARRY("larry", Attack.STRAIGHT_SHOT),

    /** The heavy. Slams the floor; the shot is secondary to being unable to stand still. */
    MORTON("morton", Attack.GROUND_SLAM),

    /** Rings that come back. The only attack in the set that punishes standing in a safe spot. */
    WENDY("wendy", Attack.RICOCHET_RING),

    /** Erratic pacing rather than a new projectile: the same shot, never twice at one rhythm. */
    IGGY("iggy", Attack.ERRATIC_SHOT),

    /** Comes down from above instead of across. The one fight where the ceiling matters. */
    ROY("roy", Attack.CEILING_DROP),

    /** Never on the floor. Bounces, so the timing window moves rather than the aim. */
    LEMMY("lemmy", Attack.BALL_BOUNCE),

    /** The eldest, and the only one that cannot be chain-stomped: it leaves the ground to recover. */
    LUDWIG("ludwig", Attack.SPREAD_SHOT),

    /** Not a caster. Charges, so the answer is to move rather than to time a jump. */
    BOWSER_JR("bowser_jr", Attack.CHARGE);

    /**
     * What the boss does between stomps.
     *
     * <p>Each is a different <em>question</em> rather than a different projectile. A set of eight
     * bosses that all fire something slightly different is one boss fought eight times.
     */
    public enum Attack {
        /** Fires along the lane at a steady interval. */
        STRAIGHT_SHOT,
        /** Shakes the floor, staggering a grounded player, then fires. */
        GROUND_SLAM,
        /** Fires a ring that reverses at the arena wall and comes back through the middle. */
        RICOCHET_RING,
        /** Fires on an irregular clock, so the gap cannot be counted. */
        ERRATIC_SHOT,
        /** Climbs out of reach and drops onto the player's column. */
        CEILING_DROP,
        /** Rides a ball and never lands, so the head moves vertically as well as across. */
        BALL_BOUNCE,
        /** Fires several at once, and hangs in the air between hits to refuse a chain stomp. */
        SPREAD_SHOT,
        /** Runs at the player instead of casting. */
        CHARGE
    }

    /** How many stomps a mid-boss takes. Three is the genre's answer and it is the right one. */
    public static final int STOMPS_TO_DEFEAT = 3;

    private final String id;
    private final Attack attack;

    Koopaling(String id, Attack attack) {
        this.id = id;
        this.attack = attack;
    }

    /** Registry-safe name, used for the texture path and the translation key. */
    public String id() {
        return id;
    }

    public Attack attack() {
        return attack;
    }

    /** Lookup by ordinal, clamped, for the synced entity data. */
    public static Koopaling byOrdinal(int ordinal) {
        Koopaling[] all = values();
        // Clamped rather than thrown: this is fed by a network field, and a boss that renders as
        // the wrong sibling is a far better failure than a client-side crash mid-fight.
        return all[Math.floorMod(ordinal, all.length)];
    }

    /**
     * Which one guards a given world's tower.
     *
     * <p>Wraps, so adding a sixth world gets a boss rather than an empty tower — the same reason
     * {@code BossArena}'s escalation table is clamped instead of indexed directly.
     */
    public static Koopaling forWorld(int worldIndex) {
        return byOrdinal(Math.max(0, worldIndex));
    }
}
