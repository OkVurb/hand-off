package com.studio.planeshift.common.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

/**
 * A Goomba that hops.
 *
 * <p>The plan's cheapest entry: reskin and rebehave what already exists rather than invent. This is
 * the Goomba's walk with a hop on a clock, which changes the question from "when do I jump on it"
 * to "when is it low enough to jump on", and that is a different question in a corridor with a
 * ceiling.
 *
 * <p>One stomp takes the wings, exactly as the reference does it, and what lands is an ordinary
 * Goomba. That is the whole reason this is worth building rather than being a Goomba that moves
 * differently: the player learns that the wings are a layer rather than a creature, which is the
 * lesson the Paratroopa teaches too and the one that makes a winged anything readable on sight.
 */
public class ParaGoombaEntity extends GoombaEntity {

    /** Ticks between hops. */
    private static final int HOP_INTERVAL = 34;

    /** Upward impulse. Enough to clear a block, not enough to reach a ceiling. */
    private static final double HOP_STRENGTH = 0.42D;

    private int hopCooldown;

    public ParaGoombaEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return GoombaEntity.createAttributes();
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide() || !isAlive()) {
            return;
        }
        if (--hopCooldown > 0 || !onGround()) {
            return;
        }
        hopCooldown = HOP_INTERVAL;
        setDeltaMovement(getDeltaMovement().x, HOP_STRENGTH, 0.0D);
        hurtMarked = true;
    }

    /**
     * The first stomp takes the wings, not the creature.
     *
     * <p>Replaced rather than mutated because the wings live in the registered entity type, the
     * same rule that gives every oversized variant its own registration: what changes here is the
     * rig, and a rig cannot be changed on a spawned entity.
     */
    @Override
    public void die(net.minecraft.world.damagesource.DamageSource cause) {
        super.die(cause);
        if (level() instanceof net.minecraft.server.level.ServerLevel server) {
            GoombaEntity grounded = com.studio.planeshift.common.registry.ModEntities.GOOMBA.get()
                    .create(server, net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);
            if (grounded != null) {
                grounded.snapTo(getX(), getY(), getZ(), getYRot(), 0.0F);
                server.addFreshEntity(grounded);
            }
        }
    }
}
