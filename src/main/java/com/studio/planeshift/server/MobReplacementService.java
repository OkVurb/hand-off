package com.studio.planeshift.server;

import com.studio.planeshift.PlaneShift;
import com.studio.planeshift.common.course.CourseState;
import com.studio.planeshift.common.registry.ModEntities;
import java.util.List;
import java.util.Optional;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

/**
 * Replaces non-Mario mobs with Mario counterparts inside PlaneShift course dimensions.
 */
public final class MobReplacementService {

    private static final List<EntityType<? extends Mob>> COURSE_ENEMIES = List.of(
            ModEntities.GOOMBA.get(),
            ModEntities.KOOPA.get(),
            ModEntities.SPINY.get(),
            ModEntities.BUZZY_BEETLE.get(),
            ModEntities.GOOMBA.get()
    );

    private MobReplacementService() {
    }

    public static boolean disableMobReplacement() {
        return ModCompatibility.disableMobReplacement();
    }

    public static boolean shouldReplace(ServerPlayer player) {
        if (ModCompatibility.disableMobReplacement()) {
            return false;
        }
        CourseState state = CourseStateAccess.get(player);
        return state.inCourse();
    }

    public static Optional<Mob> replace(Entity entity) {
        if (!(entity instanceof Mob mob) || entity.level().isClientSide()) {
            return Optional.empty();
        }

        EntityType<?> type = entity.getType();
        EntityType<? extends Mob> replacement = switch (type) {
            case EntityType<?> t when t == EntityType.ZOMBIE || t == EntityType.ZOMBIFIED_PIGLIN
                    || t == EntityType.DROWNED || t == EntityType.HUSK -> ModEntities.GOOMBA.get();
            case EntityType<?> t when t == EntityType.SKELETON || t == EntityType.STRAY -> ModEntities.KOOPA.get();
            case EntityType<?> t when t == EntityType.CREEPER -> random(COURSE_ENEMIES);
            case EntityType<?> t when t == EntityType.SPIDER || t == EntityType.CAVE_SPIDER -> ModEntities.SPINY.get();
            case EntityType<?> t when t == EntityType.PILLAGER -> ModEntities.HAMMER_BRO.get();
            case EntityType<?> t when t == EntityType.VINDICATOR -> ModEntities.THWOMP.get();
            case EntityType<?> t when t == EntityType.WITCH -> ModEntities.LAKITU.get();
            case EntityType<?> t when t == EntityType.GHAST -> ModEntities.BOO.get();
            case EntityType<?> t when t == EntityType.MAGMA_CUBE || t == EntityType.SLIME -> ModEntities.BUZZY_BEETLE.get();
            default -> null;
        };

        if (replacement == null) {
            return Optional.empty();
        }

        Mob mario = replacement.create(entity.level(), EntitySpawnReason.CONVERSION);
        if (mario == null) {
            return Optional.empty();
        }
        mario.copyPosition(entity);
        mario.setYRot(entity.getYRot());
        mario.setXRot(entity.getXRot());
        if (entity instanceof LivingEntity living) {
            mario.setHealth(living.getHealth());
        }
        entity.discard();
        entity.level().addFreshEntity(mario);
        return Optional.of(mario);
    }

    private static EntityType<? extends Mob> random(List<EntityType<? extends Mob>> list) {
        return list.get(java.util.concurrent.ThreadLocalRandom.current().nextInt(list.size()));
    }
}
