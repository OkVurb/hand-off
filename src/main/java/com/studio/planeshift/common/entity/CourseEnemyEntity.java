package com.studio.planeshift.common.entity;

import com.studio.planeshift.common.registry.ModParticles;
import com.studio.planeshift.server.CourseScoringService;
import com.studio.planeshift.common.registry.ModSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * Base class for PlaneShift enemies (Design Bible, "Enemy AI framework" and
 * "Combat, stomp, damage, and recovery").
 *
 * <p>Stomp contract: "Server verifies downward relative velocity, contact normal, target
 * tag, and per-target cooldown. Valid stomp defeats or staggers, then applies a
 * predictable bounce. Armored/spiked enemies use distinct silhouettes. Side contact
 * causes damage unless a Form or role action explicitly grants offense."
 */
public abstract class CourseEnemyEntity extends Monster {

    /** Minimum downward relative velocity for a stomp to count. */
    private static final double STOMP_MIN_FALL_SPEED = 0.08D;
    /** Predictable bounce applied to the stomper. */
    private static final double STOMP_BOUNCE = 0.55D;
    /** Per-target cooldown so one landing cannot double-hit. */
    private static final int STOMP_COOLDOWN_TICKS = 10;

    private long lastStompGameTime = -STOMP_COOLDOWN_TICKS;

    protected CourseEnemyEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    /** Whether landing on this enemy defeats it. Armored silhouettes return false. */
    public boolean isStompable() {
        return true;
    }

    /** Damage dealt to the enemy by a valid stomp. */
    protected float stompDamage() {
        return 10.0F;
    }

    @Override
    public void playerTouch(Player player) {
        if (level().isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        if (isStompContact(serverPlayer)) {
            resolveStomp(serverPlayer);
        } else {
            // Side contact: ordinary touch damage; the pip damage model intercepts it.
            serverPlayer.hurtServer((ServerLevel) level(), damageSources().mobAttack(this), 2.0F);
        }
    }

    private boolean isStompContact(ServerPlayer player) {
        // Contact normal check: the player's feet must be in the top band of our box.
        boolean fromAbove = player.getBoundingBox().minY >= getBoundingBox().maxY - 0.25D;
        // Relative velocity check: falling onto us, not brushing past.
        boolean falling = player.getDeltaMovement().y - getDeltaMovement().y < -STOMP_MIN_FALL_SPEED;
        return fromAbove && falling;
    }

    private void resolveStomp(ServerPlayer player) {
        long now = level().getGameTime();
        if (now - lastStompGameTime < STOMP_COOLDOWN_TICKS) {
            return;
        }
        lastStompGameTime = now;

        if (isStompable()) {
            hurtServer((ServerLevel) level(), damageSources().playerAttack(player), stompDamage());
            bounce(player);
            if (!isAlive()) {
                // Only a defeat advances the combo ladder; a stagger is not worth a rung.
                CourseScoringService.awardStomp(player);
                level().playSound(null, blockPosition(), ModSounds.ENEMY_DEFEAT.get(), SoundSource.HOSTILE, 1.0F, 1.0F);
                spawnHitParticles(10);
            } else {
                level().playSound(null, blockPosition(), ModSounds.STOMP.get(), SoundSource.HOSTILE, 1.0F, 1.2F);
                spawnHitParticles(4);
            }
        } else {
            // Armored top: the silhouette warned them.
            player.hurtServer((ServerLevel) level(), damageSources().mobAttack(this), 2.0F);
            bounce(player);
            level().playSound(null, blockPosition(), ModSounds.DAMAGE.get(), SoundSource.HOSTILE, 0.8F, 0.9F);
        }
    }

    private void spawnHitParticles(int count) {
        if (level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            for (int i = 0; i < count; i++) {
                double ox = random.nextGaussian() * 0.15D;
                double oy = random.nextGaussian() * 0.15D;
                double oz = random.nextGaussian() * 0.15D;
                serverLevel.sendParticles(ModParticles.HIT_BURST.get(),
                        getX(), getY(0.5D), getZ(), 1, ox, oy, oz, 0.05D);
            }
        }
    }

    private static void bounce(ServerPlayer player) {
        Vec3 velocity = player.getDeltaMovement();
        player.setDeltaMovement(velocity.x, STOMP_BOUNCE, velocity.z);
        player.hurtMarked = true;
        player.resetFallDistance();
    }
}
