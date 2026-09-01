package com.studio.planeshift.common.entity;

import com.studio.planeshift.common.registry.ModSounds;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Koopa Troopa — stompable walker that retreats into its shell instead of dying.
 *
 * <p>Three states, which is the whole point of the enemy:
 * <ul>
 *   <li><b>Walking</b> — an ordinary lane patroller.</li>
 *   <li><b>Shell</b> — stomped once. Stationary, harmless to touch, and the player can kick it.</li>
 *   <li><b>Sliding</b> — kicked. Travels fast in a straight line and destroys other enemies it
 *       hits, which is what makes a shell a weapon rather than debris.</li>
 * </ul>
 *
 * <p>A sliding shell that is stomped again stops rather than reversing, so the player can always
 * regain control of one they have lost track of.
 */
public class KoopaEntity extends CourseEnemyEntity {

    private static final EntityDataAccessor<Boolean> IN_SHELL =
            SynchedEntityData.defineId(KoopaEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> SLIDING =
            SynchedEntityData.defineId(KoopaEntity.class, EntityDataSerializers.BOOLEAN);

    /** Horizontal speed of a kicked shell. Fast enough to outrun the player. */
    private static final double SHELL_SPEED = 0.62D;
    /** How close the player must be for a kick to register. */
    private static final double KICK_REACH = 1.4D;
    /** Damage a sliding shell deals to whatever it runs into. */
    private static final float SHELL_DAMAGE = 20.0F;
    /** Ticks a freshly-created shell ignores kicks, so the stomp that made it cannot kick it. */
    private static final int KICK_GRACE_TICKS = 10;

    private int shellSince = -1;

    public KoopaEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 8.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.18D)
                .add(Attributes.ATTACK_DAMAGE, 2.0D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(IN_SHELL, false);
        builder.define(SLIDING, false);
    }

    public boolean inShell() {
        return entityData.get(IN_SHELL);
    }

    public boolean sliding() {
        return entityData.get(SLIDING);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0D, false));
        goalSelector.addGoal(2, new LanePatrolGoal(this, 1.0D));
        targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    /**
     * A shell survives being stomped: the first stomp retreats it, and later stomps stop a slide
     * rather than killing it. Only a fireball or a lava pit finishes a Koopa off.
     */
    @Override
    protected float stompDamage() {
        return 0.0F;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            return;
        }
        if (!inShell()) {
            return;
        }
        if (shellSince >= 0) {
            shellSince++;
        }
        if (sliding()) {
            tickSlide();
        } else {
            // A parked shell does not drift; it waits to be kicked.
            setDeltaMovement(0.0D, getDeltaMovement().y, 0.0D);
        }
    }

    private void tickSlide() {
        Vec3 velocity = getDeltaMovement();
        if (horizontalCollision) {
            // Bounce off walls so a shell ricochets down a corridor instead of stalling.
            setDeltaMovement(-velocity.x, velocity.y, -velocity.z);
            setYRot(getYRot() + 180.0F);
        }

        // Anything the shell runs through is destroyed. This is the reward for kicking it.
        for (CourseEnemyEntity victim : level().getEntitiesOfClass(CourseEnemyEntity.class,
                getBoundingBox().inflate(0.2D), e -> e != this && e.isAlive())) {
            victim.hurtServer((ServerLevel) level(), damageSources().mobAttack(this), SHELL_DAMAGE);
            level().playSound(null, blockPosition(), ModSounds.ENEMY_DEFEAT.get(),
                    SoundSource.HOSTILE, 0.9F, 1.1F);
        }
    }

    /**
     * Player contact. Overridden because a Koopa's response depends on its state rather than only
     * on the contact normal that {@link CourseEnemyEntity} checks.
     */
    @Override
    public void playerTouch(Player player) {
        if (level().isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        if (!inShell()) {
            super.playerTouch(player);
            // A survivable stomp retreats it rather than killing it.
            if (isAlive() && wasStompedThisTick(serverPlayer)) {
                enterShell();
            }
            return;
        }
        if (sliding()) {
            // Running into a moving shell hurts; stomping one stops it.
            if (serverPlayer.getBoundingBox().minY >= getBoundingBox().maxY - 0.25D) {
                stopSlide();
            } else {
                serverPlayer.hurtServer((ServerLevel) level(), damageSources().mobAttack(this), 2.0F);
            }
            return;
        }
        kick(serverPlayer);
    }

    /** True when the player is above us and descending, i.e. this touch was a stomp. */
    private boolean wasStompedThisTick(ServerPlayer player) {
        return player.getBoundingBox().minY >= getBoundingBox().maxY - 0.35D;
    }

    private void enterShell() {
        entityData.set(IN_SHELL, true);
        entityData.set(SLIDING, false);
        shellSince = 0;
        setDeltaMovement(0.0D, getDeltaMovement().y, 0.0D);
        goalSelector.removeAllGoals(g -> true);
        targetSelector.removeAllGoals(g -> true);
        level().playSound(null, blockPosition(), ModSounds.STOMP.get(), SoundSource.HOSTILE, 1.0F, 0.8F);
    }

    /** Launches the shell away from the player who touched it. */
    private void kick(ServerPlayer player) {
        if (shellSince >= 0 && shellSince < KICK_GRACE_TICKS) {
            return;
        }
        if (player.distanceToSqr(this) > KICK_REACH * KICK_REACH) {
            return;
        }
        Vec3 away = position().subtract(player.position());
        // Flatten and normalise: a shell travels along the ground, never up at the player.
        Vec3 direction = new Vec3(away.x, 0.0D, away.z);
        if (direction.lengthSqr() < 1.0E-4) {
            direction = player.getLookAngle().multiply(1.0D, 0.0D, 1.0D);
        }
        direction = direction.normalize().scale(SHELL_SPEED);

        entityData.set(SLIDING, true);
        setDeltaMovement(direction.x, 0.0D, direction.z);
        hurtMarked = true;
        level().playSound(null, blockPosition(), ModSounds.STOMP.get(), SoundSource.HOSTILE, 1.0F, 1.6F);
    }

    private void stopSlide() {
        entityData.set(SLIDING, false);
        setDeltaMovement(0.0D, getDeltaMovement().y, 0.0D);
        shellSince = 0;
        level().playSound(null, blockPosition(), ModSounds.STOMP.get(), SoundSource.HOSTILE, 0.9F, 1.0F);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("InShell", inShell());
        output.putBoolean("Sliding", sliding());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        boolean shell = input.getBooleanOr("InShell", false);
        entityData.set(IN_SHELL, shell);
        entityData.set(SLIDING, input.getBooleanOr("Sliding", false));
        if (shell) {
            shellSince = KICK_GRACE_TICKS;
            goalSelector.removeAllGoals(g -> true);
            targetSelector.removeAllGoals(g -> true);
        }
    }

    /** Exposed so the shell can be told apart from a walker for rendering and shell collisions. */
    public AABB shellBox() {
        return getBoundingBox();
    }
}

