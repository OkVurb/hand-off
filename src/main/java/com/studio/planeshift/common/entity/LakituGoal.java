package com.studio.planeshift.common.entity;

import com.studio.planeshift.common.registry.ModEntities;
import java.util.EnumSet;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Lakitu behavior: hover over the nearest player and periodically drop Spiny eggs.
 */
public class LakituGoal extends Goal {

    private static final double DETECT_RANGE = 32.0D;
    private static final double HOVER_HEIGHT = 5.0D;
    private static final double HOVER_SPEED = 0.12D;
    private static final int SPAWN_COOLDOWN = 100;
    private static final int SPAWN_RANGE_SQ = 16;

    private final LakituEntity lakitu;
    private int spawnTimer = SPAWN_COOLDOWN;

    public LakituGoal(LakituEntity lakitu) {
        this.lakitu = lakitu;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return !lakitu.isDeadOrDying();
    }

    @Override
    public boolean canContinueToUse() {
        return !lakitu.isDeadOrDying();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (lakitu.level().isClientSide()) {
            return;
        }

        Player target = lakitu.level().getNearestPlayer(lakitu, DETECT_RANGE);
        if (target == null || !target.isAlive()) {
            lakitu.setDeltaMovement(Vec3.ZERO);
            return;
        }

        Vec3 hoverPos = target.position().add(0.0D, HOVER_HEIGHT, 0.0D);
        Vec3 diff = hoverPos.subtract(lakitu.position());
        double distSqr = diff.lengthSqr();

        if (distSqr > 0.5D) {
            Vec3 move = diff.normalize().scale(Math.min(HOVER_SPEED, Math.sqrt(distSqr) * 0.05D));
            lakitu.setDeltaMovement(move);
        } else {
            lakitu.setDeltaMovement(Vec3.ZERO);
        }
        lakitu.getLookControl().setLookAt(target, 30.0F, 30.0F);
        lakitu.hurtMarked = true;

        spawnTimer--;
        if (spawnTimer <= 0 && distSqr < SPAWN_RANGE_SQ) {
            spawnTimer = SPAWN_COOLDOWN;
            dropSpiny();
        }
    }

    private void dropSpiny() {
        SpinyEntity spiny = new SpinyEntity(ModEntities.SPINY.get(), lakitu.level());
        spiny.setPos(lakitu.getX(), lakitu.getY() - 1.0D, lakitu.getZ());
        spiny.setDeltaMovement(new Vec3(0.0D, -0.3D, 0.0D));
        // Harmless until it lands. A Spiny dropped straight onto the player would otherwise deal
        // contact damage on the way down, which is a hit with no counterplay.
        spiny.markAirDropped();
        lakitu.level().addFreshEntity(spiny);
    }
}
