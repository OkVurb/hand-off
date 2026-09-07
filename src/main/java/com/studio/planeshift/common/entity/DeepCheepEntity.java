package com.studio.planeshift.common.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * The fish that follows you.
 *
 * <p>Its red sibling patrols a fixed stretch and is dangerous by being in the way; this one turns
 * and comes after the player. That is the entire difference, and it is the reason the water cast
 * needs two fish rather than one paint job: a level made only of patrolling fish is a timing
 * puzzle, and a level with one of these in it is a chase.
 *
 * <p>Slower than the player swims. Deliberately -- it is meant to make the player keep moving
 * rather than to catch them, and a pursuer that is faster than you turns "keep swimming" into
 * "you are already dead". It also gives up outside {@link #NOTICE}, so a room does not fill up
 * with fish converging from three screens away.
 */
public class DeepCheepEntity extends CheepCheepEntity {

    /** How far it will notice a player from. */
    private static final double NOTICE = 12.0D;

    /** Chase speed, in blocks per tick. Under a swimming player's, on purpose. */
    private static final double PURSUE_SPEED = 0.048D;

    public DeepCheepEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return CheepCheepEntity.createAttributes()
                .add(Attributes.MAX_HEALTH, 8.0D)
                .add(Attributes.ATTACK_DAMAGE, 3.0D);
    }

    @Override
    public void tick() {
        Player target = level().isClientSide() || !isInWater()
                ? null : level().getNearestPlayer(this, NOTICE);
        if (target == null) {
            // Nobody in range: patrol exactly like the red one, so an empty room reads the same.
            super.tick();
            return;
        }
        // Deliberately not calling super: the patrol would fight the pursuit for the same
        // velocity every tick, and the visible result of two systems writing one field is a fish
        // that stutters. LivingEntity's tick still runs, so everything else about it is unchanged.
        super.baseTick();
        Vec3 toward = target.position().subtract(position()).normalize().scale(PURSUE_SPEED);
        setDeltaMovement(toward);
        setYRot((float) (Math.atan2(toward.z, toward.x) * (180.0D / Math.PI)) - 90.0F);
        yBodyRot = getYRot();
        move(net.minecraft.world.entity.MoverType.SELF, getDeltaMovement());
    }
}
