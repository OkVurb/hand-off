package com.studio.planeshift.common.entity;

import com.studio.planeshift.common.registry.ModSounds;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * A Koopa with wings, which stops being a Paratroopa the moment you land on it.
 *
 * <p>Extends {@link KoopaEntity} rather than duplicating it, because that is the whole design: a
 * Paratroopa <em>is</em> a Koopa with one extra state in front of it. Everything downstream — the
 * shell, the kick, the slide, the combo scoring — is inherited untouched, so the two-hit sequence
 * the player learns is "remove the wings, then you are back to a problem you already know how to
 * solve". Written as a separate entity with its own copy of the shell logic, the two would drift.
 *
 * <p>The reason this is the cheapest enemy in the roster to add, and the reason it is worth adding
 * first: it doubles the useful depth of an encounter without introducing a single new verb.
 */
public class ParatroopaEntity extends KoopaEntity {

    private static final EntityDataAccessor<Boolean> WINGED =
            SynchedEntityData.defineId(ParatroopaEntity.class, EntityDataSerializers.BOOLEAN);

    /** Upward impulse of a hop. Enough to clear a block, not enough to leave the lane's headroom. */
    private static final double HOP_STRENGTH = 0.44D;

    /** Ticks between hops. Slow enough that the arc is readable and can be timed under. */
    private static final int HOP_INTERVAL = 32;

    private int hopTimer;

    public ParatroopaEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 8.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.19D)
                .add(Attributes.ATTACK_DAMAGE, 2.0D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(WINGED, true);
    }

    public boolean winged() {
        return entityData.get(WINGED);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide() || !winged() || !isAlive()) {
            return;
        }
        // Hopping rather than flying. A Paratroopa that hovers is a different enemy entirely: the
        // hop is what makes it a rhythm problem the player can walk underneath.
        if (onGround() && ++hopTimer >= HOP_INTERVAL) {
            hopTimer = 0;
            setDeltaMovement(getDeltaMovement().x, HOP_STRENGTH, getDeltaMovement().z);
            hurtMarked = true;
        }
    }

    /**
     * The first hit takes the wings instead of the Paratroopa.
     *
     * <p>Absorbed here rather than through a hook on {@code CourseEnemyEntity} because it is not a
     * general rule — it is this creature's entire identity, and every other enemy would have to
     * carry a field for it. Returning false leaves the entity alive, so the stomp path bounces the
     * player and plays its ordinary sound, which is exactly the feedback the moment wants.
     */
    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (winged() && source.getEntity() instanceof Player) {
            entityData.set(WINGED, false);
            hopTimer = 0;
            level.playSound(null, blockPosition(), ModSounds.STOMP.get(),
                    SoundSource.HOSTILE, 1.0F, 1.4F);
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.CLOUD,
                    getX(), getY(0.8D), getZ(), 8, 0.25D, 0.1D, 0.25D, 0.02D);
            startSquish();
            return false;
        }
        return super.hurtServer(level, source, amount);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("Winged", winged());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        entityData.set(WINGED, input.getBooleanOr("Winged", true));
    }
}
