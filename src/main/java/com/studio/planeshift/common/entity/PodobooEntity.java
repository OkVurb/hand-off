package com.studio.planeshift.common.entity;

import com.studio.planeshift.common.registry.ModSounds;
import java.util.Set;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * A blob of lava that leaps out of the pit on a fixed cycle.
 *
 * <p>Exists because the LAVA theme already digs pits and nothing ever came out of them. A pit you
 * simply must not fall into is a wall lying down; a pit that periodically throws something at you
 * turns the same geometry into a timing problem, and the segment library gets that for free
 * everywhere it already places one.
 *
 * <p><b>Fixed cycle, not reactive.</b> The whole value of this enemy is that it is predictable — it
 * is a metronome the player reads and then walks through. An AI that leapt when the player got
 * close would be strictly worse: it would punish approaching, which is the one thing the player
 * has to do.
 */
public class PodobooEntity extends CourseEnemyEntity {

    /** Ticks between leaps. Two and a half seconds: long enough to read, short enough to matter. */
    private static final int CYCLE_TICKS = 50;

    /** Upward speed of a leap. Clears about five blocks, which is a lane's worth of headroom. */
    private static final double LEAP_SPEED = 0.92D;

    /** How far below its home the blob is allowed to sink before it is considered back in the pit. */
    private static final double HOME_TOLERANCE = 0.4D;

    private double homeY = Double.NaN;
    private int timer;

    public PodobooEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 10.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D)
                .add(Attributes.ATTACK_DAMAGE, 3.0D);
    }

    @Override
    protected void registerGoals() {
        // None. This creature does not pathfind, it oscillates.
    }

    @Override
    public Set<DefeatVector> answers() {
        return java.util.EnumSet.of(DefeatVector.STAR);
    }

    @Override
    public boolean isHazard() {
        // A ball of molten rock is weather, not an opponent. Flagged so the defeat-matrix GameTest
        // does not demand it be beatable with a stomp.
        return true;
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        // Star power aside, nothing touches it. Returning false rather than cancelling damage
        // upstream keeps the reason local to the creature that has it.
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            // Trailing embers, client-side so they cost the server nothing.
            if (random.nextInt(2) == 0) {
                level().addParticle(ParticleTypes.FLAME,
                        getX() + (random.nextDouble() - 0.5D) * 0.4D,
                        getY() + random.nextDouble() * 0.6D,
                        getZ() + (random.nextDouble() - 0.5D) * 0.4D,
                        0.0D, 0.02D, 0.0D);
            }
            return;
        }

        if (Double.isNaN(homeY)) {
            // Where it was placed is where it lives. Captured on the first tick rather than at
            // construction, because the generator sets the position after the entity exists.
            homeY = getY();
        }

        boolean home = getY() <= homeY + HOME_TOLERANCE;
        if (home && ++timer >= CYCLE_TICKS) {
            timer = 0;
            setPos(getX(), homeY, getZ());
            setDeltaMovement(0.0D, LEAP_SPEED, 0.0D);
            hurtMarked = true;
            if (level() instanceof ServerLevel server) {
                server.playSound(null, blockPosition(), ModSounds.FIREBALL.get(),
                        SoundSource.HOSTILE, 0.7F, 0.6F);
                server.sendParticles(ParticleTypes.LAVA, getX(), getY(), getZ(),
                        6, 0.2D, 0.05D, 0.2D, 0.02D);
            }
        }
        // Never drift sideways: it is a fountain, not a projectile.
        setDeltaMovement(0.0D, getDeltaMovement().y, 0.0D);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        if (!Double.isNaN(homeY)) {
            output.putDouble("HomeY", homeY);
        }
        output.putInt("CycleTimer", timer);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        homeY = input.getDoubleOr("HomeY", Double.NaN);
        timer = input.getIntOr("CycleTimer", 0);
    }
}
