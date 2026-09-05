package com.studio.planeshift.common.entity;

import com.studio.planeshift.common.registry.ModSounds;
import java.util.Set;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * A Koopa skeleton that collapses when stomped and puts itself back together.
 *
 * <p>The inverse of the Paratroopa, and that is why it is worth adding next. A Paratroopa
 * <em>loses</em> a state when you hit it and becomes a simpler problem; Dry Bones <em>gains</em>
 * one and becomes a timer. Between them they cover both directions a two-state enemy can go, which
 * is most of the design space a castle level needs.
 *
 * <p>The point of the creature is that stomping is the wrong verb. It works, briefly, and then the
 * problem comes back — so the player has to either use the window or find something that removes
 * it permanently. That is stated in {@link #answers()}: a stomp is not on the list.
 */
public class DryBonesEntity extends CourseEnemyEntity {

    private static final EntityDataAccessor<Integer> COLLAPSE_TICKS =
            SynchedEntityData.defineId(DryBonesEntity.class, EntityDataSerializers.INT);

    /**
     * How long it stays down.
     *
     * <p>Five seconds. Long enough to be a real window — time to cross the gap it was guarding —
     * and short enough that it is a window rather than a kill, which is the whole point.
     */
    public static final int COLLAPSE_DURATION = 100;

    public DryBonesEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 10.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.17D)
                .add(Attributes.ATTACK_DAMAGE, 2.0D);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        // Turns at ledges, like a red Koopa. A skeleton that walks off into a pit undoes the
        // point of an enemy you cannot permanently remove: it removes itself.
        goalSelector.addGoal(1, new LanePatrolGoal(this, 1.0D, true));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(COLLAPSE_TICKS, 0);
    }

    /** Down in a heap, harmless, and about to get back up. */
    public boolean collapsed() {
        return entityData.get(COLLAPSE_TICKS) > 0;
    }

    /** 0 while standing, rising to 1 as it reassembles — the renderer uses this to lift the pile. */
    public float reassembly(float partialTick) {
        int ticks = entityData.get(COLLAPSE_TICKS);
        if (ticks <= 0) {
            return 1.0F;
        }
        float remaining = Math.max(0.0F, ticks - partialTick);
        // Flat on the floor for most of the window, then a quick rise at the end. A linear lift
        // would have it hovering halfway up for two and a half seconds, which reads as broken
        // rather than as reassembling.
        float progress = 1.0F - remaining / COLLAPSE_DURATION;
        return progress < 0.75F ? 0.0F : (progress - 0.75F) / 0.25F;
    }

    @Override
    public Set<DefeatVector> answers() {
        // No stomp. Landing on it works and then stops working, which is the creature's entire
        // reason to exist; listing STOMP here would tell the generator it is a solvable obstacle.
        return java.util.EnumSet.of(DefeatVector.SHELL, DefeatVector.FIRE, DefeatVector.STAR);
    }

    /**
     * A stomp knocks it apart instead of killing it.
     *
     * <p>Same shape as the Paratroopa's wing removal and for the same reason: it is this
     * creature's identity rather than a general rule, so it lives here instead of putting a field
     * on every enemy. Returning false keeps it alive, so the player still gets the bounce.
     */
    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (source.getEntity() instanceof Player && !collapsed()) {
            entityData.set(COLLAPSE_TICKS, COLLAPSE_DURATION);
            setDeltaMovement(0.0D, getDeltaMovement().y, 0.0D);
            level.playSound(null, blockPosition(), ModSounds.BRICK_BREAK.get(),
                    SoundSource.HOSTILE, 0.9F, 1.5F);
            level.sendParticles(ParticleTypes.CRIT, getX(), getY(0.4D), getZ(),
                    12, 0.3D, 0.15D, 0.3D, 0.04D);
            return false;
        }
        // Collapsed, it is a pile of bones and nothing can be done to it either.
        return !collapsed() && super.hurtServer(level, source, amount);
    }

    @Override
    public void playerTouch(Player player) {
        if (collapsed()) {
            // A heap on the floor is scenery. Without this the player takes contact damage from
            // something they just successfully dealt with, which reads as the game cheating.
            return;
        }
        super.playerTouch(player);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            return;
        }
        int ticks = entityData.get(COLLAPSE_TICKS);
        if (ticks > 0) {
            entityData.set(COLLAPSE_TICKS, ticks - 1);
            setDeltaMovement(0.0D, getDeltaMovement().y, 0.0D);
            if (ticks == 1 && level() instanceof ServerLevel server) {
                server.playSound(null, blockPosition(), ModSounds.QUESTION_BUMP.get(),
                        SoundSource.HOSTILE, 0.8F, 0.7F);
                server.sendParticles(ParticleTypes.CRIT, getX(), getY(0.5D), getZ(),
                        8, 0.25D, 0.2D, 0.25D, 0.03D);
            }
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("CollapseTicks", entityData.get(COLLAPSE_TICKS));
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        entityData.set(COLLAPSE_TICKS, input.getIntOr("CollapseTicks", 0));
    }
}
