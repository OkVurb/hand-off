package com.studio.planeshift.common.entity;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * Piranha Plant — "Stationary pipe lurker. Bites anything in reach."
 *
 * <p>Cycles up out of its pipe and back down on a fixed timer, so a player can learn the rhythm
 * and pass through on the beat. That predictability is the mechanic: an unpredictable plant would
 * be an unfair hazard rather than a puzzle.
 *
 * <p>It also will not emerge while the player is standing on or beside the pipe. Without that
 * rule a player who lands on a pipe is bitten with no possible counterplay, which is why the
 * original does the same.
 *
 * <p>Player lesson: not every threat can be stomped; some silhouettes must be avoided or
 * defeated from a distance.
 */
public class PiranhaPlantEntity extends CourseEnemyEntity {

    /** How far out of the pipe the plant rises, in blocks. */
    private static final double EMERGE_HEIGHT = 1.2D;
    /** Ticks fully retracted, fully extended, and spent moving between the two. */
    private static final int HIDDEN_TICKS = 40;
    private static final int RISING_TICKS = 20;
    private static final int EXPOSED_TICKS = 50;
    private static final int FALLING_TICKS = 20;
    private static final int CYCLE_TICKS = HIDDEN_TICKS + RISING_TICKS + EXPOSED_TICKS + FALLING_TICKS;

    /** Horizontal radius within which a player suppresses the plant entirely. */
    private static final double SUPPRESS_RADIUS = 1.6D;

    /** Extension from 0 (hidden) to 1 (fully out), synced so the client can render the rise. */
    private static final EntityDataAccessor<Float> EXTENSION =
            SynchedEntityData.defineId(PiranhaPlantEntity.class, EntityDataSerializers.FLOAT);

    private int cycle;
    private double baseY = Double.NaN;

    public PiranhaPlantEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 12.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D)
                .add(Attributes.ATTACK_DAMAGE, 3.0D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(EXTENSION, 0.0F);
    }

    /** 0 hidden, 1 fully emerged. Rendering and the hurt check both read this. */
    public float extension() {
        return entityData.get(EXTENSION);
    }


    /** A retracted plant is inside its pipe and cannot be touched. */
    @Override
    public boolean isPickable() {
        return extension() > 0.05F;
    }

    @Override
    public void playerTouch(Player player) {
        if (extension() <= 0.05F) {
            return;
        }
        super.playerTouch(player);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(2, new RandomLookAroundGoal(this));
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            return;
        }
        if (Double.isNaN(baseY)) {
            baseY = getY();
        }

        if (playerAdjacent()) {
            // Retract and hold. The cycle is also reset so the plant does not spring out the
            // instant the player steps off; they get the full hidden phase to move on.
            cycle = 0;
            applyExtension(0.0F);
            return;
        }

        cycle = (cycle + 1) % CYCLE_TICKS;
        applyExtension(extensionFor(cycle));
    }

    /** The extension curve for a point in the cycle. Package-private so it can be tested. */
    static float extensionFor(int tick) {
        int t = Math.floorMod(tick, CYCLE_TICKS);
        if (t < HIDDEN_TICKS) {
            return 0.0F;
        }
        t -= HIDDEN_TICKS;
        if (t < RISING_TICKS) {
            return t / (float) RISING_TICKS;
        }
        t -= RISING_TICKS;
        if (t < EXPOSED_TICKS) {
            return 1.0F;
        }
        t -= EXPOSED_TICKS;
        return 1.0F - (t / (float) FALLING_TICKS);
    }

    private void applyExtension(float value) {
        entityData.set(EXTENSION, value);
        setPos(getX(), baseY + value * EMERGE_HEIGHT, getZ());
    }

    /**
     * Whether a player is close enough to suppress the plant. Uses horizontal distance only, so
     * standing on top of the pipe counts just as much as standing beside it.
     */
    private boolean playerAdjacent() {
        Vec3 here = new Vec3(getX(), 0.0D, getZ());
        for (Player player : level().players()) {
            if (!player.isAlive() || player.isSpectator()) {
                continue;
            }
            Vec3 there = new Vec3(player.getX(), 0.0D, player.getZ());
            if (there.distanceToSqr(here) <= SUPPRESS_RADIUS * SUPPRESS_RADIUS) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("Cycle", cycle);
        if (!Double.isNaN(baseY)) {
            output.putDouble("BaseY", baseY);
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        cycle = input.getIntOr("Cycle", 0);
        baseY = input.getDoubleOr("BaseY", Double.NaN);
    }

    /** Anchored in its pipe. Being knocked over is not available to it. */
    @Override
    public boolean canBeStaggered() {
        return false;
    }


    /**
     * Anchored in its pipe and biting upward, so it cannot be landed on. A spin reaches it at
     * ground level, which is the answer the player always has; fire is the satisfying one.
     */
    @Override
    public java.util.Set<DefeatVector> answers() {
        return java.util.EnumSet.of(DefeatVector.SPIN, DefeatVector.SHELL,
                DefeatVector.FIRE, DefeatVector.ICE, DefeatVector.STAR);
    }

}
