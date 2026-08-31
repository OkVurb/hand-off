package com.studio.planeshift.server;

import com.studio.planeshift.common.item.MegaMushroomItem;
import com.studio.planeshift.common.item.MiniMushroomItem;
import com.studio.planeshift.common.item.PoisonMushroomItem;
import com.studio.planeshift.common.item.PropellerMushroomItem;
import com.studio.planeshift.common.item.SuperMushroomItem;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.Vec3;

/**
 * Makes mushroom-style power-ups walk instead of sitting where they popped.
 *
 * <p>In the source material a mushroom is a small chase: it slides along the surface it landed on,
 * turns around at walls, and drops off ledges. That turns collecting one into a decision about
 * whether to follow it, which a stationary pickup never asks.
 *
 * <p>Only mushrooms drift. Flowers, stars and coins stay put deliberately — a Fire Flower that
 * ran away would be frustrating rather than playful, and the original treats them the same way.
 */
public final class PowerupDriftService {

    /** Horizontal speed while drifting. Slow enough to catch, fast enough to matter. */
    private static final double DRIFT_SPEED = 0.11D;
    /** Below this the item is treated as stationary and given a fresh push. */
    private static final double STALL_THRESHOLD = 0.01D;

    private PowerupDriftService() {
    }

    /** Whether this item is one that should wander. */
    public static boolean drifts(Item item) {
        return item instanceof SuperMushroomItem
                || item instanceof MegaMushroomItem
                || item instanceof MiniMushroomItem
                || item instanceof PropellerMushroomItem
                // Drifts too: a trap that sits still while real mushrooms walk is no trap at all.
                || item instanceof PoisonMushroomItem;
    }

    /**
     * Advances one item's drift. Called for every item entity in a course level each tick.
     *
     * <p>Gravity is left to vanilla, so an item that runs out of floor simply falls, which is the
     * "drops off ledges" half of the behaviour without any edge detection.
     */
    public static void tick(ItemEntity entity) {
        if (entity.level().isClientSide() || !entity.isAlive()) {
            return;
        }
        if (!drifts(entity.getItem().getItem())) {
            return;
        }

        Vec3 velocity = entity.getDeltaMovement();

        if (entity.horizontalCollision) {
            // Turn around at a wall rather than grinding against it.
            entity.setDeltaMovement(-velocity.x, velocity.y, -velocity.z);
            return;
        }

        double horizontal = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
        if (horizontal < STALL_THRESHOLD) {
            // Freshly popped, or stopped by friction: push it off along the course lane.
            double heading = entity.getRandom().nextBoolean() ? DRIFT_SPEED : -DRIFT_SPEED;
            entity.setDeltaMovement(heading, velocity.y, 0.0D);
            return;
        }

        // Hold a steady speed: vanilla item friction would otherwise stall it within a second.
        double scale = DRIFT_SPEED / horizontal;
        entity.setDeltaMovement(velocity.x * scale, velocity.y, velocity.z * scale);
    }

}
