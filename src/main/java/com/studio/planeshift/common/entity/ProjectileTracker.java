package com.studio.planeshift.common.entity;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.world.entity.Entity;

/**
 * Tracks how many live projectiles each owner has of each projectile class.
 *
 * <p>This replaces the per-action 128³ AABB scan in {@link com.studio.planeshift.server.FormService}
 * with an O(1) counter lookup.
 */
public final class ProjectileTracker {

    private record Key(UUID owner, Class<? extends Entity> type) {
    }

    private static final Map<UUID, Key> BY_PROJECTILE = new HashMap<>();
    private static final Map<UUID, Object2IntMap<Class<? extends Entity>>> COUNT_BY_OWNER = new HashMap<>();

    private ProjectileTracker() {
    }

    /** Register a newly spawned projectile. Safe to call when the owner is offline/unloaded; the shot is ignored. */
    public static void add(Entity projectile, UUID owner, Class<? extends Entity> type) {
        if (owner == null) {
            return;
        }
        synchronized (BY_PROJECTILE) {
            BY_PROJECTILE.put(projectile.getUUID(), new Key(owner, type));
            COUNT_BY_OWNER
                    .computeIfAbsent(owner, k -> new Object2IntOpenHashMap<>())
                    .merge(type, 1, Integer::sum);
        }
    }

    /** Unregister a projectile when it is removed from the world. */
    public static void remove(Entity projectile) {
        synchronized (BY_PROJECTILE) {
            Key key = BY_PROJECTILE.remove(projectile.getUUID());
            if (key == null) {
                return;
            }
            Object2IntMap<Class<? extends Entity>> counts = COUNT_BY_OWNER.get(key.owner);
            if (counts == null) {
                return;
            }
            int value = counts.getInt(key.type) - 1;
            if (value <= 0) {
                counts.removeInt(key.type);
            } else {
                counts.put(key.type, value);
            }
            if (counts.isEmpty()) {
                COUNT_BY_OWNER.remove(key.owner);
            }
        }
    }

    /** Number of live projectiles of the given class owned by the player. */
    public static int count(UUID owner, Class<? extends Entity> type) {
        synchronized (BY_PROJECTILE) {
            Object2IntMap<Class<? extends Entity>> counts = COUNT_BY_OWNER.get(owner);
            return counts == null ? 0 : counts.getOrDefault(type, 0);
        }
    }
}
