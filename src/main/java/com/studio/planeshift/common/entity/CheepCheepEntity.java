package com.studio.planeshift.common.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Cheep Cheep: the water cast, and the first enemy here that does not walk.
 *
 * <p>Water courses shipped with Buzzy Beetles standing in for fish, which was the least wrong land
 * enemy available -- armoured and slow is at least the right silhouette for something drifting --
 * and was still a land enemy at the bottom of a flooded room.
 *
 * <p>The behaviour is deliberately simple: cross the lane at a steady depth and turn around at the
 * ends. A Cheep Cheep in the reference does not hunt, and that is the point of it. The threat is
 * that it is somewhere along the route you have to swim, so the player's job is timing rather than
 * combat, and an enemy that chased would turn a paced swim into a scramble.
 *
 * <p>It is dangerous out of water in the sense that it stops working, not in the sense that it
 * dies: courses place it in the flooded middle, and a fish left on dry land simply sinks and sits
 * there. That is worth knowing rather than guarding against -- generation controls where these go,
 * and a guard would be code defending against a case that generation does not produce.
 */
public class CheepCheepEntity extends CourseEnemyEntity {

    /**
     * Horizontal drift per tick.
     *
     * <p>Slower than a walking enemy on purpose. Water already slows the player, so a fish moving
     * at land speed would be unavoidable rather than merely in the way, and being in the way is
     * the whole of this enemy's job.
     */
    private static final double SWIM_SPEED = 0.055D;

    /** How far it wanders from where it was placed before turning back, in blocks. */
    private static final double RANGE = 6.0D;

    /** Overridable so a larger sibling can be slower without duplicating the swim logic. */
    protected double swimSpeed() {
        return SWIM_SPEED;
    }

    /** Overridable for the same reason: a bigger fish patrols a longer stretch. */
    protected double swimRange() {
        return RANGE;
    }

    /** Gentle vertical wander, so it does not read as a sprite sliding along an invisible rail. */
    private static final double BOB_AMPLITUDE = 0.02D;
    private static final double BOB_PERIOD = 55.0D;

    private double originX = Double.NaN;
    private double direction = 1.0D;
    private int tickOffset;

    public CheepCheepEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.tickOffset = this.random.nextInt(200);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 6.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.10D)
                .add(Attributes.ATTACK_DAMAGE, 2.0D);
    }

    /**
     * Swims rather than falls.
     *
     * <p>Gravity is cancelled while submerged rather than disabled outright, so a fish that ends
     * up out of water still behaves like an object. Turning gravity off entirely would leave one
     * hanging in mid-air if generation ever placed it above the surface, which reads as a bug
     * even though nothing would ever collide with it.
     */
    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            return;
        }
        if (!isInWater()) {
            return;
        }

        if (Double.isNaN(originX)) {
            originX = getX();
        }
        if (Math.abs(getX() - originX) > swimRange()) {
            direction = getX() > originX ? -1.0D : 1.0D;
        }

        double bob = Math.sin((tickCount + tickOffset) / BOB_PERIOD) * BOB_AMPLITUDE;
        setDeltaMovement(new Vec3(swimSpeed() * direction, bob, 0.0D));

        // Face the way it is going. A fish swimming backwards is the sort of thing nobody can name
        // but everybody notices.
        setYRot(direction > 0 ? -90.0F : 90.0F);
        yBodyRot = getYRot();
        yHeadRot = getYRot();
    }

    /** Stomping a fish underwater is not a thing the player can reliably do, so it is not asked. */
    @Override
    public boolean canBeStaggered() {
        return false;
    }
}
