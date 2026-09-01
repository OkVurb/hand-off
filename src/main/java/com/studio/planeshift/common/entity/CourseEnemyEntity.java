package com.studio.planeshift.common.entity;

import com.studio.planeshift.common.registry.ModAttributes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
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

    /**
     * Ticks of squish left, synced so the renderer can flatten the model.
     *
     * <p>Lives on the base entity rather than in each subclass so every enemy squishes the same
     * way for free, and so the renderer needs to know about exactly one field instead of a
     * per-enemy animation hook.
     */
    private static final EntityDataAccessor<Integer> SQUISH_TICKS =
            SynchedEntityData.defineId(CourseEnemyEntity.class, EntityDataSerializers.INT);

    /** How long the squish lasts. Short: it is a punctuation mark, not an animation to watch. */
    public static final int SQUISH_DURATION = 8;
    /** How flat the model gets at peak squish, as a fraction of normal height. */
    public static final float SQUISH_MIN_SCALE = 0.25F;

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(SQUISH_TICKS, 0);
    }

    /** Starts the squish. Safe to call repeatedly; a fresh hit restarts it. */
    public void startSquish() {
        entityData.set(SQUISH_TICKS, SQUISH_DURATION);
    }

    /**
     * Vertical scale to render at: 1 when unsquished, dipping toward {@link #SQUISH_MIN_SCALE}
     * and springing back.
     *
     * <p>Width is widened by the inverse so the enemy appears to conserve volume, which is what
     * makes a squash read as a squash rather than as the model simply shrinking.
     */
    public float squishScaleY(float partialTick) {
        int ticks = entityData.get(SQUISH_TICKS);
        if (ticks <= 0) {
            return 1.0F;
        }
        float remaining = Math.max(0.0F, ticks - partialTick);
        // Progress runs 0 -> 1 across the squish; sin gives a fast dip and an ease back out.
        float progress = 1.0F - (remaining / SQUISH_DURATION);
        float dip = (float) Math.sin(progress * Math.PI);
        return 1.0F - (1.0F - SQUISH_MIN_SCALE) * dip;
    }

    /** Companion to {@link #squishScaleY}: widen as the model flattens. */
    public float squishScaleXZ(float partialTick) {
        float y = squishScaleY(partialTick);
        return y >= 1.0F ? 1.0F : (float) (1.0 / Math.sqrt(y));
    }

    public boolean squishing() {
        return entityData.get(SQUISH_TICKS) > 0;
    }

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

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide()) {
                        int ticks = entityData.get(SQUISH_TICKS);
            if (ticks > 0) {
                entityData.set(SQUISH_TICKS, ticks - 1);
            }
            
            if (getTarget() instanceof ServerPlayer player) {
                com.studio.planeshift.common.course.CourseState state = com.studio.planeshift.server.CourseStateAccess.get(player);
                if (state.inCourse() && state.rail().isPresent()) {
                    var rail = state.rail().get();
                    double depthCoord = rail.planeCoord();
                    
                    if (rail.travelAxis() == net.minecraft.core.Direction.Axis.X) {
                        if (Math.abs(getZ() - depthCoord) > 0.05) {
                            setPos(getX(), getY(), depthCoord);
                            setDeltaMovement(getDeltaMovement().x, getDeltaMovement().y, 0);
                        }
                    } else if (rail.travelAxis() == net.minecraft.core.Direction.Axis.Z) {
                        if (Math.abs(getX() - depthCoord) > 0.05) {
                            setPos(depthCoord, getY(), getZ());
                            setDeltaMovement(0, getDeltaMovement().y, getDeltaMovement().z);
                        }
                    }
                }
            }
        }
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
            startSquish();
            if (!isAlive()) {
                // Only a defeat advances the combo ladder; a stagger is not worth a rung.
                CourseScoringService.awardStomp(player);
                level().playSound(null, blockPosition(), ModSounds.ENEMY_DEFEAT.get(), SoundSource.HOSTILE, 1.0F, 1.0F);
                spawnHitParticles(10);
                // Task 57: Smoke puff on enemy defeat
                if (level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.CLOUD,
                            getX(), getY(0.5D), getZ(), 12, 0.3D, 0.2D, 0.3D, 0.02D);
                }
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

    /**
     * Applies the stomp bounce, read from {@link ModAttributes#BOUNCE_HEIGHT} so a Form, role or
     * effect can tune it without this class knowing about any of them. Falls back to the original
     * constant if the attribute is somehow absent.
     */
    private static void bounce(ServerPlayer player) {
        Vec3 velocity = player.getDeltaMovement();
        var attribute = player.getAttribute(ModAttributes.BOUNCE_HEIGHT);
        double height = attribute != null ? attribute.getValue() : STOMP_BOUNCE;
        player.setDeltaMovement(velocity.x, height, velocity.z);
        player.hurtMarked = true;
        player.resetFallDistance();
    }
}

