package com.studio.planeshift.common.entity;

import com.studio.planeshift.common.block.HitFromBelowBlock;
import com.studio.planeshift.common.registry.ModSounds;
import com.studio.planeshift.server.CourseScoringService;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
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
public class KoopaEntity extends CourseEnemyEntity implements ShellSpinner {

    private static final EntityDataAccessor<Boolean> IN_SHELL =
            SynchedEntityData.defineId(KoopaEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> SLIDING =
            SynchedEntityData.defineId(KoopaEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> RED =
            SynchedEntityData.defineId(KoopaEntity.class, EntityDataSerializers.BOOLEAN);

    /** Horizontal speed of a kicked shell. Fast enough to outrun the player. */
    private static final double SHELL_SPEED = 0.62D;
    /** How close the player must be for a kick to register. */
    private static final double KICK_REACH = 1.4D;
    /** Damage a sliding shell deals to whatever it runs into. */
    private static final float SHELL_DAMAGE = 20.0F;
    /**
     * How long a parked shell stays a shell.
     *
     * <p>Twelve seconds. Long enough to be a resource the player can walk back to and use, short
     * enough that a room cannot be cleared by stomping everything once and then strolling through
     * it. The reference's is shorter, but its rooms are smaller: at this camera distance a shell
     * that popped in five seconds would be back before the player had finished dealing with what
     * else is on screen.
     */
    private static final int RECOVERY_TICKS = 240;

    /** How long the shell rocks before it stands up. The only warning the player gets. */
    private static final int WOBBLE_TICKS = 40;

    /** Ticks a freshly-created shell ignores kicks, so the stomp that made it cannot kick it. */
    private static final int KICK_GRACE_TICKS = 10;

    private int shellSince = -1;
    private UUID kickerUuid;
    private int shellCombo;

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
        builder.define(RED, true);
    }

    public boolean isRed() {
        return entityData.get(RED);
    }

    public void setRed(boolean red) {
        entityData.set(RED, red);
        updatePatrolGoal();
    }

    private void updatePatrolGoal() {
        goalSelector.removeAllGoals(g -> g instanceof LanePatrolGoal);
        if (!inShell()) {
            goalSelector.addGoal(1, new LanePatrolGoal(this, 1.0D, isRed()));
        }
    }

    /** {@inheritDoc} For a Koopa the shell shows whenever it is in one, sliding or parked. */
    @Override
    public boolean spinning() {
        return inShell();
    }

    public boolean inShell() {
        return entityData.get(IN_SHELL);
    }

    public boolean sliding() {
        return entityData.get(SLIDING);
    }

    @Override
    protected void registerGoals() {
        // Walks its lane like the rest of the ground cast; the shell states are what make a
        // Koopa a Koopa, not pursuit.
        goalSelector.addGoal(0, new FloatGoal(this));
        updatePatrolGoal();
    }

    /**
     * A shell survives being stomped: the first stomp retreats it, and later stomps stop a slide
     * rather than killing it. Only a fireball or a lava pit finishes a Koopa off.
     */
    @Override
    protected float stompDamage() {
        return 0.0F;
    }

    /**
     * A shell cannot be hurt.
     *
     * <p>The reference is unambiguous: a Koopa in its shell is invulnerable, and what removes it is
     * falling out of the level. That is not a detail — it is what makes the shell a *thing* rather
     * than an enemy in a weaker state. A shell you could destroy with a fireball would be a Koopa
     * that takes two hits, and the whole point of the object is that it survives to be used.
     *
     * <p>The out-of-world case is let through, or a kicked shell that ran off the edge of a course
     * would live forever at the bottom of the world, ticking.
     */
    @Override
    public boolean hurtServer(net.minecraft.server.level.ServerLevel level,
                              net.minecraft.world.damagesource.DamageSource source, float amount) {
        if (inShell() && !source.is(net.minecraft.world.damagesource.DamageTypes.FELL_OUT_OF_WORLD)) {
            return false;
        }
        return super.hurtServer(level, source, amount);
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
            tickRecovery();
        }
    }

    /**
     * A shell left alone long enough stands back up.
     *
     * <p>Without this a stomped Koopa is permanently neutralised, and the shell stops being a
     * decision. In the reference it is one: kick it now, carry it, or leave it and it walks again.
     * That clock is what makes a room full of Koopas a situation rather than a checklist — clearing
     * them in the wrong order means the first one is back before the last one is down.
     *
     * <p>The wobble is not decoration. {@link #WOBBLE_TICKS} before it emerges the shell starts
     * rocking, which is the only warning a player standing on top of one gets. Without a tell this
     * is a shell that becomes an enemy under their feet, and the project's own rule is that a
     * hazard the player cannot read is a death they cannot learn from.
     */
    private void tickRecovery() {
        if (shellSince < 0 || shellSince < RECOVERY_TICKS) {
            return;
        }
        entityData.set(IN_SHELL, false);
        entityData.set(SLIDING, false);
        shellSince = -1;
        kickerUuid = null;
        shellCombo = 0;
        playSound(net.minecraft.sounds.SoundEvents.TURTLE_SHAMBLE, 0.7F, 1.4F);
    }

    /**
     * Whether the shell is visibly rocking, about to stand up.
     *
     * <p>Read by the renderer. Deliberately derived rather than synced: it is a pure function of a
     * counter the server already ticks, and a second synced boolean saying the same thing is the
     * kind of rival copy this codebase has been bitten by twice.
     */
    public boolean wobbling() {
        return inShell() && !sliding() && shellSince >= 0
                && shellSince >= RECOVERY_TICKS - WOBBLE_TICKS;
    }

    private void tickSlide() {
        if (level().isClientSide()) {
            return;
        }
        ServerLevel serverLevel = (ServerLevel) level();
        Vec3 velocity = getDeltaMovement();

        // Re-drive the shell to full speed every tick.
        //
        // It was kicked once and then left to the world's own friction, so it slowed to a stop
        // after a few blocks -- a shell that runs out of energy is a rolling ball, and a shell is
        // supposed to be a projectile that happens to be alive. Direction is kept and only the
        // magnitude is restored, so the bounce below still decides where it goes.
        if (velocity.horizontalDistanceSqr() > 1.0E-6) {
            Vec3 flat = new Vec3(velocity.x, 0.0D, velocity.z).normalize().scale(SHELL_SPEED);
            velocity = new Vec3(flat.x, velocity.y, flat.z);
            setDeltaMovement(velocity);
            hurtMarked = true;
        }

        if (horizontalCollision) {
            // Check for breakable bricks or question blocks in the path of the shell
            if (velocity.lengthSqr() > 1.0E-4) {
                Direction dir = Direction.getApproximateNearest(velocity.x, 0.0D, velocity.z);
                // One dispatcher for every non-head-bump impact, shared with the ground pound.
                // This was a hand-rolled copy of BrickBlock's own rules, and it had already
                // drifted: it knew nothing about coin blocks or rotating blocks.
                HitFromBelowBlock.impact(level(), blockPosition().relative(dir));
            }

            // Bounce off walls so a shell ricochets down a corridor instead of stalling.
            setDeltaMovement(-velocity.x, velocity.y, -velocity.z);
            setYRot(getYRot() + 180.0F);
        }

        // Anything the shell runs through is destroyed. This is the reward for kicking it.
        for (CourseEnemyEntity victim : level().getEntitiesOfClass(CourseEnemyEntity.class,
                getBoundingBox().inflate(0.2D), e -> e != this && e.isAlive())) {
            victim.hurtServer(serverLevel, damageSources().mobAttack(this), SHELL_DAMAGE);
            level().playSound(null, blockPosition(), ModSounds.ENEMY_DEFEAT.get(),
                    SoundSource.HOSTILE, 0.9F, 1.1F);

            if (kickerUuid != null) {
                ServerPlayer kicker = serverLevel.getServer().getPlayerList().getPlayer(kickerUuid);
                if (kicker != null && kicker.isAlive()) {
                    CourseScoringService.awardShellKill(kicker, victim.getX(), victim.getY(), victim.getZ(), shellCombo);
                    shellCombo++;
                }
            }
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
            // The retreat happens in onStomped, not here. Asking a second time whether that touch
            // was a stomp is what broke it: this class used a tighter band than the base class and
            // so disagreed with the decision that had already been made.
            super.playerTouch(player);
            return;
        }
        if (sliding()) {
            // Running into a moving shell hurts; stomping one stops it.
            // Same band as an ordinary stomp. It used its own tighter 0.25 and so had the same
            // failure the shell retreat did: landing squarely on a sliding shell would miss the
            // check and fall through to taking damage from it.
            if (serverPlayer.getBoundingBox().minY >= getBoundingBox().maxY - STOMP_BAND) {
                stopSlide();
            } else {
                serverPlayer.hurtServer((ServerLevel) level(), damageSources().mobAttack(this), 2.0F);
            }
            return;
        }
        kick(serverPlayer);
    }

    /** A survivable stomp retreats it rather than killing it. */
    @Override
    protected void onStomped(ServerPlayer player) {
        if (!inShell()) {
            enterShell();
        }
    }

    private void enterShell() {
        entityData.set(IN_SHELL, true);
        entityData.set(SLIDING, false);
        shellSince = 0;
        kickerUuid = null;
        shellCombo = 0;
        setDeltaMovement(0.0D, getDeltaMovement().y, 0.0D);
        goalSelector.removeAllGoals(g -> true);
        targetSelector.removeAllGoals(g -> true);
        level().playSound(null, blockPosition(), ModSounds.STOMP.get(), SoundSource.HOSTILE, 1.0F, 0.8F);
    }

    /**
     * Kick entry point for the spin attack.
     *
     * <p>{@link #kick} is private and guarded by a reach and grace check that only makes sense for
     * a player who has walked into the shell. A spin has its own reach, already checked by the
     * caller, so this exposes the launch without duplicating the rules — and returns whether it
     * actually happened, so the spin can fall through to ordinary damage when there was no shell
     * to kick.
     */
    public boolean kickFromSpin(ServerPlayer player) {
        if (!inShell() || sliding()) {
            return false;
        }
        kick(player);
        return sliding();
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

        this.kickerUuid = player.getUUID();
        this.shellCombo = 0;
        entityData.set(SLIDING, true);
        setDeltaMovement(direction.x, 0.0D, direction.z);
        hurtMarked = true;
        level().playSound(null, blockPosition(), ModSounds.STOMP.get(), SoundSource.HOSTILE, 1.0F, 1.6F);
    }

    private void stopSlide() {
        entityData.set(SLIDING, false);
        setDeltaMovement(0.0D, getDeltaMovement().y, 0.0D);
        shellSince = 0;
        kickerUuid = null;
        shellCombo = 0;
        level().playSound(null, blockPosition(), ModSounds.STOMP.get(), SoundSource.HOSTILE, 0.9F, 1.0F);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("InShell", inShell());
        output.putBoolean("Sliding", sliding());
        output.putBoolean("Red", isRed());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        boolean shell = input.getBooleanOr("InShell", false);
        entityData.set(IN_SHELL, shell);
        entityData.set(SLIDING, input.getBooleanOr("Sliding", false));
        setRed(input.getBooleanOr("Red", true));
        if (shell) {
            shellSince = KICK_GRACE_TICKS;
        }
    }

    /** Exposed so the shell can be told apart from a walker for rendering and shell collisions. */
    public AABB shellBox() {
        return getBoundingBox();
    }
}

