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
    THWOMP(1.05F),
    BULLET_BILL(1.30F),
    BOO(1.25F),
    LAKITU(1.10F),
    HAMMER_BRO(1.15F),
    SPINY(1.25F),
    BUZZY_BEETLE(1.25F),
    PIRANHA_PLANT(1.05F),
    TOAD(1.0F),
    BOWSER(1.0F);

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
