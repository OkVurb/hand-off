package com.studio.planeshift.common.entity;

/**
 * The silhouettes the shared articulated enemy rig can build, and the readability scale each one
 * is drawn at.
 *
 * <p>Lives in {@code common} rather than in the render package, even though it is a description of
 * geometry, because <strong>the scale it carries has to reach the server.</strong> The renderer
 * applies {@link #visualScale()} to the pose stack so a small enemy is not two featureless pixels
 * from the side camera's usual 20-30 block framing. That is a good reason to enlarge the art — but
 * the previous version enlarged <em>only</em> the art, with a comment noting that "the
 * authoritative hitbox remains unchanged", and that is precisely the problem.
 *
 * <p>A player aims at what they can see. A Koopa drawn 25% larger than its hitbox has a visible
 * quarter-block shell that nothing can hit: stomps land on the sprite and pass through, which
 * reads as the game ignoring an input rather than as a miss. In a genre where the entire
 * interaction vocabulary is "land on top of that thing", a hitbox that disagrees with the drawing
 * is the single most damaging bug available.
 *
 * <p>So the scale is declared once, here, and both sides read it: {@code CourseEnemyRenderer}
 * scales the pose by it, and {@code ModEntities} multiplies the model's true size by it when
 * registering the entity. The numbers in {@code ModEntities} are therefore the size the artwork is
 * actually built at, and what gets registered is what appears on screen.
 */
public enum EnemyRigProfile {
    GOOMBA(1.35F),
    KOOPA(1.25F),
    PARATROOPA(1.25F),
    DRY_BONES(1.20F),
    PODOBOO(1.15F),
    BOB_OMB(1.25F),
    THWOMP(1.05F),
    BULLET_BILL(1.30F),
    BOO(1.25F),
    /** The Chomp. Large on purpose: its whole threat is that it is bigger than the gap. */
    CHAIN_CHOMP(1.55F),
    /**
     * The big ghost. Same mesh, larger registration, for the reason {@link #BIG_CHEEP} is: size
     * lives in the hitbox, so it cannot live in a variant flag.
     */
    BIG_BOO(2.6F),
    LAKITU(1.10F),
    HAMMER_BRO(1.15F),
    SPINY(1.25F),
    BUZZY_BEETLE(1.25F),
    PIRANHA_PLANT(1.05F),
    /** The big plant. Same mesh at a larger registration, as with {@link #BIG_BOO}. */
    MEGA_PIRANHA_PLANT(2.2F),
    /** Read side-on and never head-on, so it carries a little more visual scale than it needs. */
    CHEEP_CHEEP(1.20F),
    /** The large fish. Its size comes from the registered hitbox; this only matches the art. */
    BIG_CHEEP(1.90F),
    /** The pursuing fish. Same body as the red one; the difference is behaviour and palette. */
    DEEP_CHEEP(1.20F),
    /** The spine ball. Read as a silhouette of points, so it carries a little extra scale. */
    URCHIN(1.30F),
    /** The big pursuing fish, for the same reason {@link #BIG_CHEEP} is its own entry. */
    MEGA_DEEP_CHEEP(1.90F),
    TOAD(1.0F),
    /**
     * All eight tower bosses share this one. See {@link Koopaling}: the scale here is tied to the
     * registered hitbox, so per-sibling sizes would mean per-sibling registrations and eight
     * chances to let the art and the hitbox disagree.
     */
    KOOPALING(1.15F),
    BOWSER(1.0F),
    /**
     * Bowser after the Koopalings revive him. The one profile whose whole job is being bigger.
     *
     * <p>A separate entry rather than a multiplier applied at render time, for the reason this
     * enum exists: the number here is also the number the hitbox is registered with, and the two
     * drifting apart is what makes a stomp land on the art and pass through the mob.
     */
    SUPER_BOWSER(2.4F);

    private final float visualScale;

    EnemyRigProfile(float visualScale) {
        this.visualScale = visualScale;
    }

    /** How much larger than its built size this silhouette is drawn, for legibility at range. */
    public float visualScale() {
        return visualScale;
    }

    /** A built dimension scaled to what the player actually sees — use for {@code sized(...)}. */
    public float scaled(float built) {
        return built * visualScale;
    }
}
